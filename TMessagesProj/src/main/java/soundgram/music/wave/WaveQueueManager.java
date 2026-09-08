package soundgram.music.wave;

import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import soundgram.music.ChannelMusicFragment;
import soundgram.music.MusicWebSearcher;
import soundgram.music.SmartRadioEngine;

public final class WaveQueueManager {

    private static final String TAG = "WaveQueueManager";
    private static final int BUFFER_TARGET_AHEAD = 8;
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
        this.executor = Executors.newFixedThreadPool(4, r -> new Thread(r, "wave-queue-worker"));
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
                        InnerTubeClient.NextResult nr = futures.get(i).get();
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

                // 2. Round-Robin интерливинг
                List<InnerTubeClient.RawTrack> initialPool = interleaveBatches(batches);
                addCandidatesFiltered(initialPool);

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
     * Round-Robin перемешивание пачек (берем первые до 8 позиций от каждого сида).
     */
    public static List<InnerTubeClient.RawTrack> interleaveBatches(List<List<InnerTubeClient.RawTrack>> batches) {
        List<InnerTubeClient.RawTrack> result = new ArrayList<>();
        if (batches == null || batches.isEmpty()) return result;

        int maxLen = 0;
        for (List<InnerTubeClient.RawTrack> b : batches) {
            if (b.size() > maxLen) maxLen = b.size();
        }

        int limit = Math.min(maxLen, 8);
        for (int i = 0; i < limit; i++) {
            for (List<InnerTubeClient.RawTrack> batch : batches) {
                if (i < batch.size()) {
                    result.add(batch.get(i));
                }
            }
        }
        return result;
    }

    /**
     * Фоновый цикл поддержания буфера воспроизведения (минимум BUFFER_TARGET_AHEAD треков вперед).
     */
    private void runBufferLoop() {
        while (isRunning && !cancelFlag.get()) {
            try {
                // Поддерживаем минимум BUFFER_TARGET_AHEAD треков вперед в очереди
                while (isRunning && !cancelFlag.get() && getRemainingAhead() < BUFFER_TARGET_AHEAD) {
                    InnerTubeClient.RawTrack candidate;
                    synchronized (candidatesPool) {
                        if (candidatesPool.isEmpty()) {
                            loadMoreCandidates();
                        }
                        candidate = candidatesPool.pollFirst();
                    }

                    if (candidate == null) {
                        // Нет кандидатов — небольшая пауза перед повтором
                        SystemClock.sleep(500);
                        break;
                    }

                    // Очистка названия и артиста через TrackSanitizer
                    String cleanTitle = TrackSanitizer.cleanTitle(candidate.title);
                    String cleanArtist = TrackSanitizer.cleanArtist(candidate.artist);
                    if (cleanTitle.isEmpty() || cleanArtist.isEmpty()) continue;

                    // 1. Проверка по сессионному кешу
                    String candidateKey = normalizeKey(cleanArtist, cleanTitle);
                    if (sessionPlayedKeys.contains(candidateKey)) {
                        FileLog.d(TAG + " skip session duplicate: " + cleanArtist + " - " + cleanTitle);
                        continue;
                    }

                    // 2. Проверка по живому плейлисту плеера в реальном времени
                    boolean inPlaylist = false;
                    ArrayList<MessageObject> curPl = MediaController.getInstance().getPlaylist();
                    if (curPl != null) {
                        for (MessageObject mo : curPl) {
                            if (mo != null && candidateKey.equals(normalizeKey(mo.getMusicAuthor(), mo.getMusicTitle()))) {
                                inPlaylist = true;
                                break;
                            }
                        }
                    }
                    if (inPlaylist) {
                        sessionPlayedKeys.add(candidateKey);
                        continue;
                    }

                    // 3. Проверка по SQLite истории (дедупликация 24 часа)
                    String hash = TrackSanitizer.generateHash(cleanArtist, cleanTitle);
                    if (historyDb.isRecentlyPlayed(hash)) {
                        sessionPlayedKeys.add(candidateKey);
                        FileLog.d(TAG + " skip recently played: " + cleanArtist + " - " + cleanTitle);
                        continue;
                    }

                    // Резолвинг трека через наш сервис музыки (Hitmos / Deezer)
                    MusicWebSearcher.WebTrack wt = MusicWebSearcher.searchTrackStrict(cleanArtist, cleanTitle, candidate.durationSec);
                    if (wt == null || wt.downloadUrl == null || wt.downloadUrl.isEmpty()) {
                        FileLog.d(TAG + " resolve 404: " + cleanArtist + " - " + cleanTitle);
                        continue;
                    }

                    // 4. Проверка найденного трека по сессионному кешу
                    String resolvedKey = normalizeKey(wt.artist, wt.title);
                    if (sessionPlayedKeys.contains(resolvedKey)) {
                        FileLog.d(TAG + " skip resolved session duplicate: " + wt.artist + " - " + wt.title);
                        continue;
                    }

                    int dur = ChannelMusicFragment.parseDuration(wt.duration);
                    MessageObject msg = ChannelMusicFragment.createWebMessageObject(
                            currentAccount, wt.title, wt.artist, wt.downloadUrl, dur,
                            -(long) wt.downloadUrl.hashCode(), wt.coverUrl);

                    // Запись в базу истории и сессионный набор (ОБА ключа)
                    sessionPlayedKeys.add(candidateKey);
                    sessionPlayedKeys.add(resolvedKey);
                    historyDb.markAsPlayed(hash);
                    historyDb.markAsPlayed(TrackSanitizer.generateHash(wt.artist, wt.title));

                    synchronized (playBuffer) {
                        playBuffer.addLast(msg);
                    }

                    // Мгновенный старт первого трека, либо добавление следующих
                    ArrayList<MessageObject> single = new ArrayList<>();
                    single.add(msg);

                    AndroidUtilities.runOnUIThread(() -> {
                        if (callback != null && !cancelFlag.get()) {
                            if (initialFired.compareAndSet(false, true)) {
                                callback.onTracksFound(single);
                            } else {
                                callback.onMoreTracksAdded(single);
                            }
                        }
                    });
                }

                // Пауза перед следующей проверкой буфера
                SystemClock.sleep(1500);

            } catch (Exception e) {
                FileLog.e(TAG + " runBufferLoop exception", e);
                SystemClock.sleep(2000);
            }
        }
    }

    private int getRemainingAhead() {
        if (!initialFired.get()) {
            synchronized (playBuffer) {
                return playBuffer.size();
            }
        }
        ArrayList<MessageObject> currentPlaylist = MediaController.getInstance().getPlaylist();
        MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            synchronized (playBuffer) {
                return playBuffer.size();
            }
        }
        int index = playing != null ? currentPlaylist.indexOf(playing) : -1;
        if (index >= 0) {
            return (currentPlaylist.size() - 1) - index;
        }
        return currentPlaylist.size();
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
                if (!candidatesPool.isEmpty()) {
                    return;
                }
            }
        }

        // Если continuation исчерпаны или отфильтрованы — фолбэк на Dynamic Re-seeding
        triggerDynamicReSeeding();
    }

    /**
     * Динамический дрифт (Dynamic Re-seeding):
     * Каждые 15 треков подмешиваем новые треки, строго сохраняя контекст волны
     * (по артисту, альбому или жанру).
     */
    private void triggerDynamicReSeeding() {
        if (cancelFlag.get() || !isRunning) return;

        // 1. Специфичное поведение по режимам: предотвращение расползания волны
        if (waveMode == MODE_ARTIST && !targetName.isEmpty()) {
            List<InnerTubeClient.RawTrack> artistSongs = InnerTubeClient.searchSongs(targetName + " лучшие треки", 20, cancelFlag);
            if (artistSongs.isEmpty()) {
                artistSongs = InnerTubeClient.searchSongs(targetName, 20, cancelFlag);
            }
            if (!artistSongs.isEmpty()) {
                Collections.shuffle(artistSongs);
                addCandidatesFiltered(artistSongs);
                return;
            }
        } else if (waveMode == MODE_ALBUM && !targetName.isEmpty()) {
            List<InnerTubeClient.RawTrack> albumArtistSongs = InnerTubeClient.searchSongs(targetName + " песни", 20, cancelFlag);
            if (!albumArtistSongs.isEmpty()) {
                Collections.shuffle(albumArtistSongs);
                addCandidatesFiltered(albumArtistSongs);
                return;
            }
        } else if (waveMode == MODE_GENRE && !targetName.isEmpty() && !targetName.equalsIgnoreCase("Любой жанр")) {
            List<String> genreSeeds = SeedSelector.selectSeedsByGenre(targetName, cancelFlag);
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
                return;
            }
        }

        // 2. Стандартный Re-seeding по последним прослушанным
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

        if (seedTracks.isEmpty()) return;

        List<String> newSeedIds = new ArrayList<>();
        for (MessageObject m : seedTracks) {
            String q = m.getMusicAuthor() + " " + m.getMusicTitle();
            List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q, 1, cancelFlag);
            if (!s.isEmpty() && s.get(0).videoId != null) {
                newSeedIds.add(s.get(0).videoId);
            }
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
            // Увеличиваем счетчик скипов у текущих сидов
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
