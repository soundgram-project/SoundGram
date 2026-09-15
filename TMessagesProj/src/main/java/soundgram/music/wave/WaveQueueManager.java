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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
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
    private static final int RE_SEED_INTERVAL = 10;
    private static final Random sRandom = new Random();

    public static final int MODE_GENRE = 0;
    public static final int MODE_ARTIST = 1;
    public static final int MODE_ALBUM = 2;
    public static final int MODE_PLAYLIST = 3;

    private int waveMode = MODE_GENRE;
    private String targetName = "";

    // Контекст альбома
    private String albumContextArtist = "";
    private String albumContextTitle = "";
    private final List<MessageObject> albumContextTracks = Collections.synchronizedList(new ArrayList<>());

    // Контекст плейлиста
    private final List<MessageObject> playlistContextTracks = Collections.synchronizedList(new ArrayList<>());
    private final List<String> playlistContextArtists = Collections.synchronizedList(new ArrayList<>());

    private final int currentAccount;
    private final SmartRadioEngine.WaveCallback callback;
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    private final Deque<InnerTubeClient.RawTrack> candidatesPool = new ArrayDeque<>();
    private final Deque<MessageObject> playBuffer = new ArrayDeque<>();

    // Сессионная дедупликация: предотвращает повторение треков в одной сессии
    private final Set<String> sessionPlayedKeys = Collections.synchronizedSet(new HashSet<>());

    // Сиды и continuation-токены
    private final List<String> activeSeedIds = Collections.synchronizedList(new ArrayList<>());
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
     * Старт сессии волны по альбому с сохранением контекста альбома и артиста.
     */
    public void startSession(List<String> seedVideoIds, int mode, String artist, String album, List<MessageObject> albumTracks) {
        this.waveMode = mode;
        this.albumContextArtist = artist != null ? artist.trim() : "";
        this.albumContextTitle = album != null ? album.trim() : "";
        this.targetName = (!this.albumContextArtist.isEmpty() ? this.albumContextArtist + " - " : "") + this.albumContextTitle;
        if (albumTracks != null) {
            this.albumContextTracks.addAll(albumTracks);
        }
        startSession(seedVideoIds);
    }

    /**
     * Старт сессии волны по плейлисту с сохранением треков и артистов плейлиста.
     */
    public void startSession(List<String> seedVideoIds, int mode, List<MessageObject> playlistTracks) {
        this.waveMode = mode;
        this.targetName = "playlist";
        if (playlistTracks != null) {
            this.playlistContextTracks.addAll(playlistTracks);
            Set<String> artSet = new HashSet<>();
            for (MessageObject m : playlistTracks) {
                if (m != null && m.getMusicAuthor() != null && !m.getMusicAuthor().trim().isEmpty()) {
                    artSet.add(m.getMusicAuthor().trim());
                }
            }
            this.playlistContextArtists.addAll(artSet);
        }
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

        executor.execute(() -> {
            try {
                // 1. Быстрый параллельный запрос радио по сидам (InnerTube)
                CompletionService<InnerTubeClient.NextResult> cs = new ExecutorCompletionService<>(executor);
                int submitted = 0;
                for (String seedId : seedVideoIds) {
                    try {
                        cs.submit(() -> InnerTubeClient.fetchRadioBatch(seedId, null, cancelFlag));
                        submitted++;
                    } catch (Exception ignored) {}
                }

                // Ждем первую готовую пачку треков с таймаутом (до 1.8 с) для мгновенного старта
                long deadline = SystemClock.uptimeMillis() + 1800L;
                List<List<InnerTubeClient.RawTrack>> batches = new ArrayList<>();
                for (int i = 0; i < submitted; i++) {
                    long remaining = deadline - SystemClock.uptimeMillis();
                    if (remaining <= 0 && !batches.isEmpty()) {
                        break;
                    }
                    try {
                        Future<InnerTubeClient.NextResult> f = cs.poll(Math.max(100, remaining), TimeUnit.MILLISECONDS);
                        if (f != null) {
                            InnerTubeClient.NextResult nr = f.get();
                            if (nr != null && !nr.tracks.isEmpty()) {
                                batches.add(nr.tracks);
                                if (nr.continuationToken != null && i < seedVideoIds.size()) {
                                    seedContinuationTokens.put(seedVideoIds.get(i), nr.continuationToken);
                                }
                                if (batches.size() >= 1) {
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(TAG + " error awaiting batch: " + e);
                    }
                }

                if (cancelFlag.get() || !isRunning) return;

                List<InnerTubeClient.RawTrack> initialPool = interleaveBatches(batches);
                addCandidatesFiltered(initialPool);

                // Если кандидатов нет, сразу запускаем re-seeding
                if (candidatesPool.isEmpty()) {
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

                // Фоновый сбор оставшихся радио-батчей
                final int remainingSubmits = submitted - batches.size();
                if (remainingSubmits > 0) {
                    executor.execute(() -> {
                        for (int i = 0; i < remainingSubmits; i++) {
                            if (cancelFlag.get() || !isRunning) break;
                            try {
                                Future<InnerTubeClient.NextResult> f = cs.poll(3, TimeUnit.SECONDS);
                                if (f != null) {
                                    InnerTubeClient.NextResult nr = f.get();
                                    if (nr != null && !nr.tracks.isEmpty()) {
                                        addCandidatesFiltered(nr.tracks);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    });
                }

                // 2. Запуск буферизации очереди (трек #1 стартует сразу!)
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

        // В режимах Альбома и Артиста НЕ фильтруем историю (пользователь явно выбрал этот альбом/артиста)!
        // В жанре и плейлисте проверяем историю только если кандидатов достаточно (>= 15), чтобы не было голодания
        if (waveMode != MODE_ALBUM && waveMode != MODE_ARTIST) {
            String hash = TrackSanitizer.generateHash(cleanArtist, cleanTitle);
            boolean poolPlentiful;
            synchronized (candidatesPool) {
                poolPlentiful = candidatesPool.size() >= 15;
            }
            if (poolPlentiful && historyDb.isRecentlyPlayed(hash)) {
                sessionPlayedKeys.add(candidateKey);
                return null;
            }
        }

        // Поиск через музыкальный сервис (Hitmos / Deezer) с чистым фолбэком
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
        historyDb.markAsPlayed(TrackSanitizer.generateHash(cleanArtist, cleanTitle));
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
            if (candidatesPool.size() < 20) {
                executor.execute(this::loadMoreCandidates);
            }
            int take = Math.min(candidatesPool.size(), Math.max(targetCount * 3, targetCount == 1 ? 6 : 12));
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

        CompletionService<MessageObject> cs = new ExecutorCompletionService<>(executor);
        int submitted = 0;
        for (InnerTubeClient.RawTrack track : toProcess) {
            try {
                cs.submit(() -> resolveSingleCandidate(track));
                submitted++;
            } catch (Exception ignored) {}
        }

        for (int i = 0; i < submitted; i++) {
            if (cancelFlag.get() || !isRunning || !SmartRadioEngine.isWaveActive) break;
            try {
                Future<MessageObject> f = cs.poll(4, TimeUnit.SECONDS);
                if (f != null) {
                    MessageObject msg = f.get();
                    if (msg != null) {
                        resolved.add(msg);
                        // Мгновенный запуск первого трека сессии!
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
        // 1. Сверхбыстрый старт: находим ровно 1 трек (< 1 сек) и сразу запускаем воспроизведение
        List<MessageObject> firstTrack = resolveBatch(1);
        if (firstTrack.isEmpty() && !initialFired.get()) {
            triggerDynamicReSeeding();
            firstTrack = resolveBatch(1);
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

        // 2. В фоне догружаем остаток первичной очереди (5-7 треков), пока играет первый трек
        List<MessageObject> remainingInitial = resolveBatch(BATCH_SIZE - 1);
        if (remainingInitial.isEmpty()) {
            triggerDynamicReSeeding();
            remainingInitial = resolveBatch(BATCH_SIZE - 1);
        }
        if (!remainingInitial.isEmpty()) {
            ArrayList<MessageObject> toAdd = new ArrayList<>(remainingInitial);
            synchronized (playBuffer) {
                playBuffer.addAll(toAdd);
            }
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null && !cancelFlag.get() && SmartRadioEngine.isWaveActive) {
                    callback.onMoreTracksAdded(toAdd);
                }
            });
        }

        // 3. Бесконечный цикл поддержания очереди воспроизведения
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
                            triggerDynamicReSeeding();
                            List<MessageObject> retryBatch = resolveBatch(need);
                            if (!retryBatch.isEmpty()) {
                                emptyBatchesCount = 0;
                                ArrayList<MessageObject> toAdd = new ArrayList<>(retryBatch);
                                synchronized (playBuffer) {
                                    playBuffer.addAll(toAdd);
                                }
                                AndroidUtilities.runOnUIThread(() -> {
                                    if (callback != null && !cancelFlag.get() && SmartRadioEngine.isWaveActive) {
                                        callback.onMoreTracksAdded(toAdd);
                                    }
                                });
                            }
                        }
                    }
                }

                synchronized (candidatesPool) {
                    if (candidatesPool.size() < 20) {
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

        // 1. Попытка использовать continuation token наименее скипанного сида
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
                if (candidatesPool.size() >= 20) {
                    return;
                }
            } else {
                seedContinuationTokens.remove(bestSeed);
            }
        }

        // 2. Если continuation исчерпаны или кандидатов мало — запускаем динамический re-seeding
        triggerDynamicReSeeding();
    }

    private List<MessageObject> getRecentSessionTracks(int count) {
        List<MessageObject> list = new ArrayList<>();
        synchronized (completedSessionTracks) {
            int sz = completedSessionTracks.size();
            for (int i = sz - 1; i >= 0 && list.size() < count; i--) {
                MessageObject m = completedSessionTracks.get(i);
                if (m != null) list.add(m);
            }
        }
        if (list.size() < count) {
            synchronized (playBuffer) {
                List<MessageObject> buf = new ArrayList<>(playBuffer);
                for (int i = buf.size() - 1; i >= 0 && list.size() < count; i--) {
                    MessageObject m = buf.get(i);
                    if (m != null && !list.contains(m)) list.add(m);
                }
            }
        }
        return list;
    }

    /**
     * Динамический дрифт (Dynamic Re-seeding):
     * Пополняет кандидатов строго сохраняя контекст волны
     * (по артисту, альбому, плейлисту или жанру).
     */
    private void triggerDynamicReSeeding() {
        if (cancelFlag.get() || !isRunning) return;

        // ── 1. РЕЖИМ ЖАНРА ──
        if (waveMode == MODE_GENRE) {
            String g = !targetName.isEmpty() ? targetName : "default";

            // А. Ротация разнообразных поисковых запросов по жанру
            String[] genreQueryPatterns = {
                    g + " хиты",
                    g + " лучшие песни",
                    g + " новинки",
                    g + " топ треки",
                    g + " радио",
                    g + " популярное",
                    g + " сборник",
                    g + " playlist",
                    "русский " + g,
                    g + " classics hits"
            };
            int qIdx = Math.abs(playedTracksCount.get() + sRandom.nextInt(5)) % genreQueryPatterns.length;
            List<InnerTubeClient.RawTrack> searchTracks = InnerTubeClient.searchSongs(genreQueryPatterns[qIdx], 20, cancelFlag);
            if (!searchTracks.isEmpty()) {
                Collections.shuffle(searchTracks, sRandom);
                addCandidatesFiltered(searchTracks);
            }

            // Б. Радио по недавно проигранным трекам текущей сессии
            List<MessageObject> recentTracks = getRecentSessionTracks(2);
            for (MessageObject m : recentTracks) {
                String author = m.getMusicAuthor();
                String title = m.getMusicTitle();
                String q = (author != null ? author : "") + " " + (title != null ? title : "");
                if (!q.trim().isEmpty()) {
                    List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q.trim(), 1, cancelFlag);
                    if (!s.isEmpty() && s.get(0).videoId != null && !activeSeedIds.contains(s.get(0).videoId)) {
                        String vid = s.get(0).videoId;
                        activeSeedIds.add(vid);
                        InnerTubeClient.NextResult nr = InnerTubeClient.fetchRadioBatch(vid, null, cancelFlag);
                        if (nr != null && !nr.tracks.isEmpty()) {
                            addCandidatesFiltered(nr.tracks);
                            if (nr.continuationToken != null) {
                                seedContinuationTokens.put(vid, nr.continuationToken);
                            }
                        }
                    }
                }
            }

            // В. Маяки жанра
            List<String> allGenrePool = SeedSelector.getAllGenreBeaconIds(g);
            List<String> shuffledPool = new ArrayList<>(allGenrePool);
            Collections.shuffle(shuffledPool, sRandom);
            List<String> pickedSeeds = new ArrayList<>();
            for (String sid : shuffledPool) {
                if (!activeSeedIds.contains(sid)) {
                    pickedSeeds.add(sid);
                    if (pickedSeeds.size() >= 2) break;
                }
            }
            for (String sid : pickedSeeds) {
                activeSeedIds.add(sid);
                InnerTubeClient.NextResult nr = InnerTubeClient.fetchRadioBatch(sid, null, cancelFlag);
                if (nr != null && !nr.tracks.isEmpty()) {
                    addCandidatesFiltered(nr.tracks);
                    if (nr.continuationToken != null) {
                        seedContinuationTokens.put(sid, nr.continuationToken);
                    }
                }
            }

            // Г. Deezer фолбэк при малом числе кандидатов
            if (candidatesPool.size() < 15) {
                ArrayList<MusicWebSearcher.WebTrack> deezerTracks = MusicWebSearcher.getGenreTracksSync(g);
                if (deezerTracks != null && !deezerTracks.isEmpty()) {
                    List<InnerTubeClient.RawTrack> dRaw = new ArrayList<>();
                    for (MusicWebSearcher.WebTrack wt : deezerTracks) {
                        dRaw.add(new InnerTubeClient.RawTrack(null, wt.title, wt.artist, ChannelMusicFragment.parseDuration(wt.duration)));
                    }
                    Collections.shuffle(dRaw, sRandom);
                    addCandidatesFiltered(dRaw);
                }
            }
            return;
        }

        // ── 2. РЕЖИМ ПЛЕЙЛИСТА ──
        if (waveMode == MODE_PLAYLIST) {
            // А. Неиспользованные треки из плейлиста
            if (!playlistContextTracks.isEmpty()) {
                List<InnerTubeClient.RawTrack> pTracks = new ArrayList<>();
                for (MessageObject m : playlistContextTracks) {
                    if (m != null && m.getMusicAuthor() != null && m.getMusicTitle() != null) {
                        pTracks.add(new InnerTubeClient.RawTrack(null, m.getMusicTitle(), m.getMusicAuthor(), (int) m.getDuration()));
                    }
                }
                Collections.shuffle(pTracks, sRandom);
                addCandidatesFiltered(pTracks);
            }

            // Б. Артисты плейлиста
            if (!playlistContextArtists.isEmpty()) {
                List<String> artistsPool = new ArrayList<>(playlistContextArtists);
                Collections.shuffle(artistsPool, sRandom);
                for (String artist : artistsPool.subList(0, Math.min(2, artistsPool.size()))) {
                    List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(artist + " популярные треки", 10, cancelFlag);
                    if (!s.isEmpty()) {
                        addCandidatesFiltered(s);
                    }
                }
            }

            // В. Радио по недавним трекам сессии или плейлиста
            List<MessageObject> seedPool = new ArrayList<>(getRecentSessionTracks(2));
            if (seedPool.isEmpty() && !playlistContextTracks.isEmpty()) {
                seedPool.addAll(playlistContextTracks);
                Collections.shuffle(seedPool, sRandom);
            }
            for (MessageObject m : seedPool.subList(0, Math.min(2, seedPool.size()))) {
                String q = (m.getMusicAuthor() != null ? m.getMusicAuthor() + " " : "") + (m.getMusicTitle() != null ? m.getMusicTitle() : "");
                if (q.trim().isEmpty()) continue;
                List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q.trim(), 1, cancelFlag);
                if (!s.isEmpty() && s.get(0).videoId != null && !activeSeedIds.contains(s.get(0).videoId)) {
                    String vid = s.get(0).videoId;
                    activeSeedIds.add(vid);
                    InnerTubeClient.NextResult nr = InnerTubeClient.fetchRadioBatch(vid, null, cancelFlag);
                    if (nr != null && !nr.tracks.isEmpty()) {
                        addCandidatesFiltered(nr.tracks);
                        if (nr.continuationToken != null) {
                            seedContinuationTokens.put(vid, nr.continuationToken);
                        }
                        break;
                    }
                }
            }
            return;
        }

        // ── 3. РЕЖИМ АРТИСТА ──
        if (waveMode == MODE_ARTIST && !targetName.isEmpty()) {
            String[] artistQueries = {
                    targetName + " популярные треки",
                    targetName + " лучшие песни",
                    targetName + " альбомы",
                    targetName + " синглы",
                    targetName + " feat",
                    targetName + " радио",
                    targetName + " remix",
                    targetName
            };
            int qIdx = Math.abs(playedTracksCount.get() + sRandom.nextInt(4)) % artistQueries.length;
            List<InnerTubeClient.RawTrack> artistSongs = InnerTubeClient.searchSongs(artistQueries[qIdx], 20, cancelFlag);
            if (!artistSongs.isEmpty()) {
                Collections.shuffle(artistSongs, sRandom);
                addCandidatesFiltered(artistSongs);
            }

            // Радио по недавним трекам сессии или трекам артиста
            List<MessageObject> recent = getRecentSessionTracks(2);
            if (!recent.isEmpty()) {
                for (MessageObject m : recent) {
                    String q = m.getMusicAuthor() + " " + m.getMusicTitle();
                    List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q.trim(), 1, cancelFlag);
                    if (!s.isEmpty() && s.get(0).videoId != null && !activeSeedIds.contains(s.get(0).videoId)) {
                        String vid = s.get(0).videoId;
                        activeSeedIds.add(vid);
                        InnerTubeClient.NextResult nr = InnerTubeClient.fetchRadioBatch(vid, null, cancelFlag);
                        if (nr != null && !nr.tracks.isEmpty()) {
                            addCandidatesFiltered(nr.tracks);
                            if (nr.continuationToken != null) {
                                seedContinuationTokens.put(vid, nr.continuationToken);
                            }
                            break;
                        }
                    }
                }
            } else if (!artistSongs.isEmpty()) {
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

            if (candidatesPool.size() < 15) {
                ArrayList<MusicWebSearcher.WebTrack> deezer = MusicWebSearcher.getGenreTracksSync(targetName);
                if (deezer != null && !deezer.isEmpty()) {
                    List<InnerTubeClient.RawTrack> dRaw = new ArrayList<>();
                    for (MusicWebSearcher.WebTrack wt : deezer) {
                        dRaw.add(new InnerTubeClient.RawTrack(null, wt.title, wt.artist, ChannelMusicFragment.parseDuration(wt.duration)));
                    }
                    addCandidatesFiltered(dRaw);
                }
            }
            return;
        }

        // ── 4. РЕЖИМ АЛЬБОМА ──
        if (waveMode == MODE_ALBUM) {
            String art = !albumContextArtist.isEmpty() ? albumContextArtist : targetName;
            String alb = albumContextTitle;

            // А. Треки самого альбома
            if (!albumContextTracks.isEmpty()) {
                List<InnerTubeClient.RawTrack> fromAlbum = new ArrayList<>();
                for (MessageObject m : albumContextTracks) {
                    if (m != null && m.getMusicAuthor() != null && m.getMusicTitle() != null) {
                        fromAlbum.add(new InnerTubeClient.RawTrack(null, m.getMusicTitle(), m.getMusicAuthor(), (int) m.getDuration()));
                    }
                }
                addCandidatesFiltered(fromAlbum);
            }

            // Б. Поиск треков альбома, исполнителя и родственных релизов
            if (!art.isEmpty()) {
                String[] albumQueries = {
                        (art + " " + alb).trim() + " треки",
                        (art + " " + alb).trim(),
                        art + " популярные треки",
                        art + " лучшие песни",
                        art + " альбомы",
                        art + " синглы"
                };
                int qIdx = Math.abs(playedTracksCount.get() + sRandom.nextInt(3)) % albumQueries.length;
                List<InnerTubeClient.RawTrack> found = InnerTubeClient.searchSongs(albumQueries[qIdx], 20, cancelFlag);
                if (!found.isEmpty()) {
                    Collections.shuffle(found, sRandom);
                    addCandidatesFiltered(found);
                }
            }

            // В. Радио по неиспользованным трекам альбома или сессии
            List<MessageObject> seedPool = new ArrayList<>();
            if (!albumContextTracks.isEmpty()) {
                seedPool.addAll(albumContextTracks);
            }
            seedPool.addAll(getRecentSessionTracks(2));
            Collections.shuffle(seedPool, sRandom);
            for (MessageObject m : seedPool) {
                String author = m.getMusicAuthor();
                String title = m.getMusicTitle();
                String q = (author != null ? author + " - " : "") + (title != null ? title : "");
                if (q.trim().isEmpty()) continue;
                List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q.trim(), 1, cancelFlag);
                if (!s.isEmpty() && s.get(0).videoId != null && !activeSeedIds.contains(s.get(0).videoId)) {
                    String vid = s.get(0).videoId;
                    activeSeedIds.add(vid);
                    InnerTubeClient.NextResult nr = InnerTubeClient.fetchRadioBatch(vid, null, cancelFlag);
                    if (nr != null && !nr.tracks.isEmpty()) {
                        addCandidatesFiltered(nr.tracks);
                        if (nr.continuationToken != null) {
                            seedContinuationTokens.put(vid, nr.continuationToken);
                        }
                        break;
                    }
                }
            }

            if (candidatesPool.size() < 15 && !art.isEmpty()) {
                ArrayList<MusicWebSearcher.WebTrack> deezer = MusicWebSearcher.getGenreTracksSync(art);
                if (deezer != null && !deezer.isEmpty()) {
                    List<InnerTubeClient.RawTrack> dRaw = new ArrayList<>();
                    for (MusicWebSearcher.WebTrack wt : deezer) {
                        dRaw.add(new InnerTubeClient.RawTrack(null, wt.title, wt.artist, ChannelMusicFragment.parseDuration(wt.duration)));
                    }
                    addCandidatesFiltered(dRaw);
                }
            }
            return;
        }

        // ── 5. ФОЛБЭК: Re-seeding по последним сыгранным трекам сессии или дефолтным маякам ──
        List<MessageObject> seedTracks = getRecentSessionTracks(2);
        List<String> newSeedIds = new ArrayList<>();
        for (MessageObject m : seedTracks) {
            String q = m.getMusicAuthor() + " " + m.getMusicTitle();
            List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q.trim(), 1, cancelFlag);
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
            synchronized (activeSeedIds) {
                for (String seedId : activeSeedIds) {
                    AtomicInteger c = seedSkipCounters.get(seedId);
                    if (c != null) c.incrementAndGet();
                }
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
        albumContextTracks.clear();
        playlistContextTracks.clear();
        playlistContextArtists.clear();
    }
}
