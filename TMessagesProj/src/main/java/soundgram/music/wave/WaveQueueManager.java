package soundgram.music.wave;

import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SharedConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import soundgram.music.ChannelMusicFragment;
import soundgram.music.MusicWebSearcher;
import soundgram.music.SmartRadioEngine;

public final class WaveQueueManager {

    private static final String TAG = "WaveQueueManager";
    private static final int BUFFER_TARGET_AHEAD = 16;
    private static final int BUFFER_REFILL_THRESHOLD = 12;
    private static final int BATCH_SIZE = 8;
    private static final int RE_SEED_INTERVAL = 15; // Dynamic Re-seeding every 15 played tracks

    public static final int MODE_GENRE = 0;
    public static final int MODE_ARTIST = 1;
    public static final int MODE_ALBUM = 2;
    public static final int MODE_PLAYLIST = 3;

    private int waveMode = MODE_GENRE;
    private String targetName = "";

    private final int currentAccount;
    private final SmartRadioEngine.WaveCallback callback;
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    private final Deque<InnerTubeClient.RawTrack> candidatesPool = new ArrayDeque<>();
    private final Deque<MessageObject> playBuffer = new ArrayDeque<>();

    // Сессионная дедупликация: предотвращает повторение треков в одной сессии
    private final Set<String> sessionPlayedKeys = Collections.synchronizedSet(new HashSet<>());

    // Сиды и continuation-токены
    private final List<String> activeSeedIds = new ArrayList<>();
    private final Map<String, String> seedContinuationTokens = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> seedSkipCounters = new ConcurrentHashMap<>();

    // Сессионные треки для Dynamic Re-seeding
    private final List<MessageObject> completedSessionTracks = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger playedTracksCount = new AtomicInteger(0);
    private final AtomicBoolean initialFired = new AtomicBoolean(false);

    private final WaveHistoryDao historyDb;
    private final ExecutorService executor;
    private volatile boolean isRunning = false;

    public WaveQueueManager(int account, SmartRadioEngine.WaveCallback callback) {
        this.currentAccount = account;
        this.callback = callback;
        this.historyDb = WaveHistoryDao.getInstance();
        this.executor = Executors.newFixedThreadPool(8, r -> new Thread(r, "wave-queue-worker"));
    }

    /**
     * Нормализация ключа трека для надежного сравнения и дедупликации
     */
    public static String normalizeKey(String artist, String title) {
        if (artist == null) artist = "";
        if (title == null) title = "";
        String a = artist.toLowerCase().replaceAll("(?i)(feat\\.|ft\\.|&|,|\\+).*$", "").replaceAll("[^a-zа-я0-9]", "");
        String t = title.toLowerCase().replaceAll("(?i)(\\(.*?\\)|\\[.*?\\]|-.*?(remix|edit|mix|version)).*$", "").replaceAll("[^a-zа-я0-9]", "");
        return a + "___" + t;
    }

    /**
     * Старт сессии волны с переданным списком seedVideoIds, режимом и целевым именем.
     */
    public void startSession(List<String> seedVideoIds, int mode, String target) {
        this.waveMode = mode;
        this.targetName = target != null ? target.trim() : "";
        startSession(seedVideoIds);
    }

    /**
     * Старт сессии волны с переданным списком seedVideoIds (2–3 трека).
     */
    public void startSession(List<String> seedVideoIds) {
        if (seedVideoIds == null || seedVideoIds.isEmpty()) {
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) callback.onError();
            });
            return;
        }

        isRunning = true;
        cancelFlag.set(false);
        activeSeedIds.clear();
        activeSeedIds.addAll(seedVideoIds);
        for (String id : seedVideoIds) {
            seedSkipCounters.put(id, new AtomicInteger(0));
        }

        // Заполняем sessionPlayedKeys треками, которые уже есть в текущем плейлисте плеера
        ArrayList<MessageObject> currentPl = MediaController.getInstance().getPlaylist();
        if (currentPl != null) {
            for (MessageObject mo : currentPl) {
                if (mo != null) {
                    sessionPlayedKeys.add(normalizeKey(mo.getMusicAuthor(), mo.getMusicTitle()));
                }
            }
        }

        executor.execute(() -> {
            try {
                // 1. Параллельный запрос радио по всем сидам (InnerTube)
                List<Future<InnerTubeClient.NextResult>> futures = new ArrayList<>();
                for (String seedId : seedVideoIds) {
                    futures.add(executor.submit(() -> InnerTubeClient.fetchRadioBatch(seedId, null, cancelFlag)));
                }

                List<List<InnerTubeClient.RawTrack>> batches = new ArrayList<>();
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        InnerTubeClient.NextResult nr = futures.get(i).get(8, TimeUnit.SECONDS);
                        if (nr != null && !nr.tracks.isEmpty()) {
                            batches.add(nr.tracks);
                            String seed = seedVideoIds.get(i);
                            if (nr.continuationToken != null) {
                                seedContinuationTokens.put(seed, nr.continuationToken);
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(TAG + " error awaiting batch: " + e);
                    }
                }

                if (cancelFlag.get() || !isRunning) return;

                // 2. Round-Robin интерливинг всех полученных треков
                List<InnerTubeClient.RawTrack> initialPool = interleaveBatches(batches);
                addCandidatesFiltered(initialPool);

                if (candidatesPool.size() < 25) {
                    FileLog.d(TAG + " candidatesPool low after radio batches (" + candidatesPool.size() + "), triggering re-seeding");
                    triggerDynamicReSeeding();
                }

                if (candidatesPool.isEmpty()) {
                    FileLog.w(TAG + " candidatesPool is still empty after re-seeding");
                    AndroidUtilities.runOnUIThread(() -> {
                        if (callback != null && !initialFired.get() && !cancelFlag.get()) {
                            callback.onError();
                        }
                    });
                    stop();
                    return;
                }

                // 3. Запуск буферизации очереди
                runBufferLoop();

            } catch (Exception e) {
                FileLog.e(TAG + " startSession error", e);
                AndroidUtilities.runOnUIThread(() -> {
                    if (callback != null && !initialFired.get()) {
                        callback.onError();
                    }
                });
            }
        });
    }

    private void addCandidatesFiltered(List<InnerTubeClient.RawTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) return;
        synchronized (candidatesPool) {
            for (InnerTubeClient.RawTrack t : tracks) {
                if (t == null) continue;
                String k = normalizeKey(t.artist, t.title);
                if (!sessionPlayedKeys.contains(k)) {
                    candidatesPool.addLast(t);
                }
            }
        }
    }

    /**
     * Round-Robin перемешивание пачек (объединяем все полученные треки).
     */
    public static List<InnerTubeClient.RawTrack> interleaveBatches(List<List<InnerTubeClient.RawTrack>> batches) {
        List<InnerTubeClient.RawTrack> result = new ArrayList<>();
        if (batches == null || batches.isEmpty()) return result;

        int maxLen = 0;
        for (List<InnerTubeClient.RawTrack> b : batches) {
            if (b.size() > maxLen) maxLen = b.size();
        }

        for (int i = 0; i < maxLen; i++) {
            for (List<InnerTubeClient.RawTrack> batch : batches) {
                if (i < batch.size()) {
                    result.add(batch.get(i));
                }
            }
        }
        return result;
    }

    /**
     * Резолвинг одного кандидата в MessageObject с полной дедупликацией.
     */
    private MessageObject resolveSingleCandidate(InnerTubeClient.RawTrack candidate) {
        if (candidate == null || cancelFlag.get() || !isRunning || !SmartRadioEngine.isWaveActive) {
            return null;
        }

        String cleanTitle = TrackSanitizer.cleanTitle(candidate.title);
        String cleanArtist = TrackSanitizer.cleanArtist(candidate.artist);
        if (cleanTitle.isEmpty() || cleanArtist.isEmpty()) {
            return null;
        }

        String candidateKey = normalizeKey(cleanArtist, cleanTitle);
        if (sessionPlayedKeys.contains(candidateKey)) {
            return null;
        }

        String hash = TrackSanitizer.generateHash(cleanArtist, cleanTitle);
        if (historyDb.isRecentlyPlayed(hash)) {
            sessionPlayedKeys.add(candidateKey);
            return null;
        }

        // Поиск через музыкальный сервис (Hitmos / Deezer)
        MusicWebSearcher.WebTrack wt = MusicWebSearcher.searchTrackStrict(cleanArtist, cleanTitle, candidate.durationSec);
        if (wt == null || wt.downloadUrl == null || wt.downloadUrl.isEmpty()) {
            return null;
        }

        String resolvedKey = normalizeKey(wt.artist, wt.title);
        if (sessionPlayedKeys.contains(resolvedKey)) {
            return null;
        }

        int dur = ChannelMusicFragment.parseDuration(wt.duration);
        MessageObject msg = ChannelMusicFragment.createWebMessageObject(
                currentAccount, wt.title, wt.artist, wt.downloadUrl, dur,
                -(long) wt.downloadUrl.hashCode(), wt.coverUrl);

        sessionPlayedKeys.add(candidateKey);
        sessionPlayedKeys.add(resolvedKey);
        historyDb.markAsPlayed(hash);
        historyDb.markAsPlayed(TrackSanitizer.generateHash(wt.artist, wt.title));

        return msg;
    }

    /**
     * Параллельный резолвинг пачки кандидатов.
     */
    private List<MessageObject> resolveBatch(int targetCount) {
        List<MessageObject> resolved = new ArrayList<>();
        if (cancelFlag.get() || !isRunning || !SmartRadioEngine.isWaveActive) {
            return resolved;
        }

        List<InnerTubeClient.RawTrack> toProcess = new ArrayList<>();
        synchronized (candidatesPool) {
            if (candidatesPool.size() < 25) {
                executor.execute(this::loadMoreCandidates);
            }
            // Берем кандидатов с запасом для компенсации 404
            int take = Math.min(candidatesPool.size(), targetCount * 2);
            for (int i = 0; i < take; i++) {
                InnerTubeClient.RawTrack c = candidatesPool.pollFirst();
                if (c != null) {
                    toProcess.add(c);
                }
            }
        }

        if (toProcess.isEmpty()) {
            return resolved;
        }

        List<Future<MessageObject>> futures = new ArrayList<>();
        for (InnerTubeClient.RawTrack track : toProcess) {
            futures.add(executor.submit(() -> resolveSingleCandidate(track)));
        }

        for (Future<MessageObject> f : futures) {
            if (cancelFlag.get() || !isRunning || !SmartRadioEngine.isWaveActive) break;
            try {
                MessageObject msg = f.get(8, TimeUnit.SECONDS);
                if (msg != null) {
                    resolved.add(msg);
                    // Мгновенный запуск первого трека сессии
                    if (initialFired.compareAndSet(false, true)) {
                        ArrayList<MessageObject> first = new ArrayList<>();
                        first.add(msg);
                        synchronized (playBuffer) {
                            playBuffer.addLast(msg);
                        }
                        AndroidUtilities.runOnUIThread(() -> {
                            if (callback != null && !cancelFlag.get() && SmartRadioEngine.isWaveActive) {
                                callback.onTracksFound(first);
                            }
                        });
                    }
                    if (resolved.size() >= targetCount) {
                        break;
                    }
                }
            } catch (Exception e) {
                FileLog.e(TAG + " resolve future error: " + e);
            }
        }

        return resolved;
    }

    /**
     * Фоновый цикл поддержания буфера воспроизведения пачками треков.
     */
    private void runBufferLoop() {
        // 1. Первичная пачка треков (резолвится параллельно, первый трек стартует сразу)
        List<MessageObject> initialBatch = resolveBatch(BATCH_SIZE);
        if (initialBatch.isEmpty() && !initialFired.get()) {
            triggerDynamicReSeeding();
            initialBatch = resolveBatch(BATCH_SIZE);
        }

        if (!initialFired.get()) {
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null && !cancelFlag.get()) {
                    callback.onError();
                }
            });
            stop();
            return;
        }

        // Отправляем остаток первичной пачки, чтобы очередь сразу была наполнена
        if (initialBatch.size() > 1) {
            ArrayList<MessageObject> remainingInitial = new ArrayList<>();
            for (int i = 1; i < initialBatch.size(); i++) {
                MessageObject m = initialBatch.get(i);
                remainingInitial.add(m);
                synchronized (playBuffer) {
                    playBuffer.addLast(m);
                }
            }
            if (!remainingInitial.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (callback != null && !cancelFlag.get() && SmartRadioEngine.isWaveActive) {
                        callback.onMoreTracksAdded(remainingInitial);
                    }
                });
            }
        }

        // 2. Цикл непрерывной догрузки новыми пачками треков
        int emptyBatchesCount = 0;
        while (isRunning && !cancelFlag.get() && SmartRadioEngine.isWaveActive) {
            try {
                if (MediaController.getInstance().getPlaylistMergeDialogId() != MediaController.WAVE_CONTEXT_ID) {
                    SystemClock.sleep(1000);
                    continue;
                }

                int ahead = getRemainingAhead();
                if (ahead < BUFFER_REFILL_THRESHOLD) {
                    int need = Math.min(BATCH_SIZE, BUFFER_TARGET_AHEAD - ahead);
                    if (need > 0) {
                        List<MessageObject> batch = resolveBatch(need);
                        if (!batch.isEmpty()) {
                            emptyBatchesCount = 0;
                            ArrayList<MessageObject> toAdd = new ArrayList<>(batch);
                            synchronized (playBuffer) {
                                playBuffer.addAll(toAdd);
                            }
                            AndroidUtilities.runOnUIThread(() -> {
                                if (callback != null && !cancelFlag.get() && SmartRadioEngine.isWaveActive) {
                                    callback.onMoreTracksAdded(toAdd);
                                }
                            });
                        } else {
                            emptyBatchesCount++;
                            if (emptyBatchesCount >= 2) {
                                triggerDynamicReSeeding();
                            }
                        }
                    }
                }

                synchronized (candidatesPool) {
                    if (candidatesPool.size() < 25) {
                        executor.execute(this::loadMoreCandidates);
                    }
                }

                SystemClock.sleep(ahead < BUFFER_REFILL_THRESHOLD ? 1500 : 2500);

            } catch (Exception e) {
                FileLog.e(TAG + " runBufferLoop exception", e);
                SystemClock.sleep(2000);
            }
        }

        if (!initialFired.get() && !cancelFlag.get()) {
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null && !cancelFlag.get()) {
                    callback.onError();
                }
            });
        }
    }

    private int getRemainingAhead() {
        if (!initialFired.get()) {
            return 0;
        }
        if (MediaController.getInstance().getPlaylistMergeDialogId() != MediaController.WAVE_CONTEXT_ID) {
            return 0;
        }
        ArrayList<MessageObject> currentPlaylist = MediaController.getInstance().getPlaylist();
        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            return 0;
        }
        int num = MediaController.getInstance().getPlayingMessageObjectNum();
        if (num < 0 || num >= currentPlaylist.size()) {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            num = playing != null ? currentPlaylist.indexOf(playing) : -1;
        }
        if (num >= 0 && num < currentPlaylist.size()) {
            return SharedConfig.playOrderReversed ? Math.max(0, currentPlaylist.size() - 1 - num) : num;
        }
        return 0;
    }

    /**
     * Дозагрузка кандидатов по continuationToken лучшего сида.
     */
    private void loadMoreCandidates() {
        if (cancelFlag.get() || !isRunning) return;

        // Выбираем сид с наименьшим количеством скипов
        String bestSeed = null;
        int minSkips = Integer.MAX_VALUE;
        for (String seedId : activeSeedIds) {
            String tok = seedContinuationTokens.get(seedId);
            if (tok != null && !tok.isEmpty()) {
                AtomicInteger counter = seedSkipCounters.get(seedId);
                int skips = counter != null ? counter.get() : 0;
                if (skips < minSkips) {
                    minSkips = skips;
                    bestSeed = seedId;
                }
            }
        }

        if (bestSeed != null) {
            String continuation = seedContinuationTokens.get(bestSeed);
            InnerTubeClient.NextResult next = InnerTubeClient.fetchRadioBatch(null, continuation, cancelFlag);
            if (next != null && !next.tracks.isEmpty()) {
                if (next.continuationToken != null) {
                    seedContinuationTokens.put(bestSeed, next.continuationToken);
                } else {
                    seedContinuationTokens.remove(bestSeed);
                }
                addCandidatesFiltered(next.tracks);
                if (candidatesPool.size() >= 25) {
                    return;
                }
            } else {
                seedContinuationTokens.remove(bestSeed);
            }
        }

        // Если continuation исчерпаны или отфильтрованы — фолбэк на Dynamic Re-seeding
        triggerDynamicReSeeding();
    }

    /**
     * Динамический дрифт (Dynamic Re-seeding):
     * Пополняет кандидатов строго сохраняя контекст волны
     * (по артисту, альбому или жанру).
     */
    private void triggerDynamicReSeeding() {
        if (cancelFlag.get() || !isRunning) return;

        if (waveMode == MODE_GENRE) {
            String g = !targetName.isEmpty() ? targetName : "default";
            List<String> genreSeeds = SeedSelector.selectSeedsByGenre(g, cancelFlag);
            if (!genreSeeds.isEmpty()) {
                List<List<InnerTubeClient.RawTrack>> newBatches = new ArrayList<>();
                for (String sid : genreSeeds) {
                    InnerTubeClient.NextResult nr = InnerTubeClient.fetchRadioBatch(sid, null, cancelFlag);
                    if (nr != null && !nr.tracks.isEmpty()) {
                        newBatches.add(nr.tracks);
                        activeSeedIds.add(sid);
                        seedSkipCounters.put(sid, new AtomicInteger(0));
                        if (nr.continuationToken != null) {
                            seedContinuationTokens.put(sid, nr.continuationToken);
                        }
                    }
                }
                List<InnerTubeClient.RawTrack> fresh = interleaveBatches(newBatches);
                addCandidatesFiltered(fresh);
            }
            if (candidatesPool.size() < 25) {
                List<InnerTubeClient.RawTrack> direct = SeedSelector.searchGenreSongs(g, 30, cancelFlag);
                if (!direct.isEmpty()) {
                    Collections.shuffle(direct);
                    addCandidatesFiltered(direct);
                }
            }
            return;
        }

        if (waveMode == MODE_ARTIST && !targetName.isEmpty()) {
            List<InnerTubeClient.RawTrack> artistSongs = InnerTubeClient.searchSongs(targetName + " лучшие треки", 30, cancelFlag);
            if (artistSongs.isEmpty()) {
                artistSongs = InnerTubeClient.searchSongs(targetName, 30, cancelFlag);
            }
            if (!artistSongs.isEmpty()) {
                Collections.shuffle(artistSongs);
                addCandidatesFiltered(artistSongs);
                for (InnerTubeClient.RawTrack t : artistSongs) {
                    if (t.videoId != null && !activeSeedIds.contains(t.videoId)) {
                        activeSeedIds.add(t.videoId);
                        InnerTubeClient.NextResult nr = InnerTubeClient.fetchRadioBatch(t.videoId, null, cancelFlag);
                        if (nr != null && !nr.tracks.isEmpty()) {
                            addCandidatesFiltered(nr.tracks);
                            if (nr.continuationToken != null) {
                                seedContinuationTokens.put(t.videoId, nr.continuationToken);
                            }
                            break;
                        }
                    }
                }
            }
            return;
        }

        if (waveMode == MODE_ALBUM && !targetName.isEmpty()) {
            List<InnerTubeClient.RawTrack> albumSongs = InnerTubeClient.searchSongs(targetName + " песни", 30, cancelFlag);
            if (!albumSongs.isEmpty()) {
                Collections.shuffle(albumSongs);
                addCandidatesFiltered(albumSongs);
                for (InnerTubeClient.RawTrack t : albumSongs) {
                    if (t.videoId != null && !activeSeedIds.contains(t.videoId)) {
                        activeSeedIds.add(t.videoId);
                        InnerTubeClient.NextResult nr = InnerTubeClient.fetchRadioBatch(t.videoId, null, cancelFlag);
                        if (nr != null && !nr.tracks.isEmpty()) {
                            addCandidatesFiltered(nr.tracks);
                            if (nr.continuationToken != null) {
                                seedContinuationTokens.put(t.videoId, nr.continuationToken);
                            }
                            break;
                        }
                    }
                }
            }
            return;
        }

        // Re-seeding по последним прослушанным или активным сидам
        List<MessageObject> seedTracks = new ArrayList<>();
        synchronized (completedSessionTracks) {
            int size = completedSessionTracks.size();
            if (size >= 2) {
                seedTracks.add(completedSessionTracks.get(size - 1));
                seedTracks.add(completedSessionTracks.get(size - 2));
            } else if (size == 1) {
                seedTracks.add(completedSessionTracks.get(0));
            }
        }

        List<String> newSeedIds = new ArrayList<>();
        for (MessageObject m : seedTracks) {
            String q = m.getMusicAuthor() + " " + m.getMusicTitle();
            List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q, 1, cancelFlag);
            if (!s.isEmpty() && s.get(0).videoId != null) {
                newSeedIds.add(s.get(0).videoId);
            }
        }

        if (newSeedIds.isEmpty()) {
            newSeedIds.addAll(SeedSelector.selectSeedsByGenre("default", cancelFlag));
        }

        if (!newSeedIds.isEmpty()) {
            List<List<InnerTubeClient.RawTrack>> newBatches = new ArrayList<>();
            for (String sid : newSeedIds) {
                InnerTubeClient.NextResult nr = InnerTubeClient.fetchRadioBatch(sid, null, cancelFlag);
                if (nr != null && !nr.tracks.isEmpty()) {
                    newBatches.add(nr.tracks);
                    activeSeedIds.add(sid);
                    seedSkipCounters.put(sid, new AtomicInteger(0));
                    if (nr.continuationToken != null) {
                        seedContinuationTokens.put(sid, nr.continuationToken);
                    }
                }
            }
            List<InnerTubeClient.RawTrack> fresh = interleaveBatches(newBatches);
            addCandidatesFiltered(fresh);
            FileLog.d(TAG + " Dynamic re-seeding completed with " + fresh.size() + " fresh candidates.");
        }
    }

    /**
     * Фиксация полностью прослушанного трека.
     */
    public void reportTrackPlayed(MessageObject track) {
        if (track == null) return;
        completedSessionTracks.add(track);
        int total = playedTracksCount.incrementAndGet();
        if (total % RE_SEED_INTERVAL == 0) {
            executor.execute(this::triggerDynamicReSeeding);
        }
    }

    /**
     * Фиксация скипа трека пользователем (< 20 сек).
     */
    public void reportSkip(String artist, String title, long playedMs) {
        if (playedMs < 20_000L && !activeSeedIds.isEmpty()) {
            for (String seedId : activeSeedIds) {
                AtomicInteger c = seedSkipCounters.get(seedId);
                if (c != null) c.incrementAndGet();
            }
        }
    }

    /**
     * Остановка сессии.
     */
    public void stop() {
        isRunning = false;
        cancelFlag.set(true);
        executor.shutdownNow();
        synchronized (candidatesPool) { candidatesPool.clear(); }
        synchronized (playBuffer) { playBuffer.clear(); }
        activeSeedIds.clear();
        seedContinuationTokens.clear();
        seedSkipCounters.clear();
        completedSessionTracks.clear();
    }
}
