package soundgram.music;

import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SharedConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import soundgram.music.wave.SeedSelector;
import soundgram.music.wave.WaveQueueManager;

/**
 * SmartRadioEngine — координатор движка «Моя Волна».
 * Делегирует всю логику SeedSelector (выбор 2–3 сидов)
 * и WaveQueueManager (параллельные запросы InnerTube, Round-Robin интерливинг,
 * локальная SQLite дедупликация, префетч 3+ треков и мгновенный старт).
 */
public final class SmartRadioEngine {

    private static final String TAG = "SmartRadioEngine";

    public interface WaveCallback {
        void onTracksFound(ArrayList<MessageObject> tracks);
        void onMoreTracksAdded(ArrayList<MessageObject> tracks);
        void onError();
    }

    public static class WaveConfig {
        public String query = "Любой жанр";
    }

    public static WaveConfig currentConfig = new WaveConfig();
    public static volatile boolean isWaveActive = false;
    public static volatile boolean isLoading = false;
    public static volatile int sCurrentMode = -1;
    public static volatile String sCurrentTarget = "";

    private static final ExecutorService sSeedExecutor = Executors.newSingleThreadExecutor();
    private static volatile WaveQueueManager sCurrentManager = null;
    private static final AtomicBoolean sCancelFlag = new AtomicBoolean(false);

    private SmartRadioEngine() {}

    /**
     * Проверка совпадения запущенной сессии с запрошенными параметрами.
     */
    public static boolean isCurrentSession(int mode, String target) {
        if (!isWaveActive) return false;
        if (sCurrentMode != mode) return false;
        if (target == null) return sCurrentTarget == null;
        return target.trim().equalsIgnoreCase(sCurrentTarget != null ? sCurrentTarget.trim() : "");
    }

    /**
     * Остановка текущей сессии волны.
     */
    public static synchronized void stopWaveSession() {
        isWaveActive = false;
        isLoading = false;
        sCurrentMode = -1;
        sCurrentTarget = "";
        sCancelFlag.set(true);
        if (sCurrentManager != null) {
            sCurrentManager.stop();
            sCurrentManager = null;
        }
    }

    /**
     * Запуск новой сессии волны по выбранному жанру/настройкам (Режим «По жанру»).
     * Запускается МГНОВЕННО за счет встроенного каталога маяков-сидов.
     */
    public static synchronized void generateWaveFromSettings(int currentAccount, WaveCallback callback) {
        stopWaveSession();
        sCancelFlag.set(false);
        isWaveActive = true;
        isLoading = true;

        String genre = currentConfig.query != null ? currentConfig.query : "default";
        sCurrentMode = WaveQueueManager.MODE_GENRE;
        sCurrentTarget = genre;

        sSeedExecutor.execute(() -> {
            try {
                List<String> seeds = SeedSelector.selectSeedsByGenre(genre, sCancelFlag);
                if (sCancelFlag.get() || !isWaveActive) return;

                synchronized (SmartRadioEngine.class) {
                    if (!isWaveActive) return;
                    WaveQueueManager manager = new WaveQueueManager(currentAccount, new WaveCallbackWrapper(callback));
                    sCurrentManager = manager;
                    manager.startSession(seeds, WaveQueueManager.MODE_GENRE, genre);
                }
            } catch (Exception e) {
                FileLog.e(TAG + " generateWaveFromSettings error", e);
                AndroidUtilities.runOnUIThread(() -> {
                    if (callback != null) callback.onError();
                });
            }
        });
    }

    /**
     * Запуск волны по исполнителю (Режим «По артисту»).
     */
    public static synchronized void generateWaveByArtist(String artistName, int currentAccount, WaveCallback callback) {
        stopWaveSession();
        sCancelFlag.set(false);
        isWaveActive = true;
        isLoading = true;

        sCurrentMode = WaveQueueManager.MODE_ARTIST;
        sCurrentTarget = artistName != null ? artistName.trim() : "";

        sSeedExecutor.execute(() -> {
            try {
                List<String> seeds = SeedSelector.selectSeedsByArtist(artistName, sCancelFlag);
                if (sCancelFlag.get() || !isWaveActive) return;

                synchronized (SmartRadioEngine.class) {
                    if (!isWaveActive) return;
                    WaveQueueManager manager = new WaveQueueManager(currentAccount, new WaveCallbackWrapper(callback));
                    sCurrentManager = manager;
                    manager.startSession(seeds, WaveQueueManager.MODE_ARTIST, artistName);
                }
            } catch (Exception e) {
                FileLog.e(TAG + " generateWaveByArtist error", e);
                AndroidUtilities.runOnUIThread(() -> {
                    if (callback != null) callback.onError();
                });
            }
        });
    }

    /**
     * Запуск волны по альбому (Режим «По альбому»).
     */
    public static synchronized void generateWaveByAlbum(String artistName, String albumName, int currentAccount, WaveCallback callback) {
        generateWaveByAlbum(artistName, albumName, null, currentAccount, callback);
    }

    public static synchronized void generateWaveByAlbum(String artistName, String albumName, ArrayList<MessageObject> albumTracks, int currentAccount, WaveCallback callback) {
        stopWaveSession();
        sCancelFlag.set(false);
        isWaveActive = true;
        isLoading = true;

        sCurrentMode = WaveQueueManager.MODE_ALBUM;
        sCurrentTarget = (artistName != null && !artistName.trim().isEmpty() ? artistName.trim() + " - " : "") + (albumName != null ? albumName.trim() : "");

        sSeedExecutor.execute(() -> {
            try {
                List<String> seeds = SeedSelector.selectSeedsByAlbum(artistName, albumName, albumTracks, sCancelFlag);
                if (sCancelFlag.get() || !isWaveActive) return;

                synchronized (SmartRadioEngine.class) {
                    if (!isWaveActive) return;
                    WaveQueueManager manager = new WaveQueueManager(currentAccount, new WaveCallbackWrapper(callback));
                    sCurrentManager = manager;
                    manager.startSession(seeds, WaveQueueManager.MODE_ALBUM, artistName, albumName, albumTracks);
                }
            } catch (Exception e) {
                FileLog.e(TAG + " generateWaveByAlbum error", e);
                AndroidUtilities.runOnUIThread(() -> {
                    if (callback != null) callback.onError();
                });
            }
        });
    }

    /**
     * Запуск волны по локальному плейлисту (Режим «По плейлисту»).
     */
    public static synchronized void generateWaveByPlaylist(ArrayList<MessageObject> originalPlaylist, int currentAccount, WaveCallback callback) {
        stopWaveSession();
        sCancelFlag.set(false);
        isWaveActive = true;
        isLoading = true;

        sCurrentMode = WaveQueueManager.MODE_PLAYLIST;
        sCurrentTarget = "playlist";

        sSeedExecutor.execute(() -> {
            try {
                List<String> seeds = SeedSelector.selectSeedsByPlaylist(originalPlaylist, sCancelFlag);
                if (sCancelFlag.get() || !isWaveActive) return;

                synchronized (SmartRadioEngine.class) {
                    if (!isWaveActive) return;
                    WaveQueueManager manager = new WaveQueueManager(currentAccount, new WaveCallbackWrapper(callback));
                    sCurrentManager = manager;
                    manager.startSession(seeds, WaveQueueManager.MODE_PLAYLIST, originalPlaylist);
                }
            } catch (Exception e) {
                FileLog.e(TAG + " generateWaveByPlaylist error", e);
                AndroidUtilities.runOnUIThread(() -> {
                    if (callback != null) callback.onError();
                });
            }
        });
    }

    /**
     * Фиксация скипа трека (< 20 сек).
     */
    public static void reportSkip(String artist, String title, long playedMs) {
        WaveQueueManager manager = sCurrentManager;
        if (manager != null) {
            manager.reportSkip(artist, title, playedMs);
        }
    }

    /**
     * Фиксация проигрывания трека в волне.
     */
    public static void reportTrackPlayed(MessageObject track) {
        WaveQueueManager manager = sCurrentManager;
        if (manager != null) {
            manager.reportTrackPlayed(track);
        }
    }

    /**
     * Добавление трека в библиотеку пользователя.
     */
    public static void reportLibraryAdd(String artist, String title) {
        // Boost artist/track in current session if needed
    }

    /**
     * Проверка и обновление сидов (no-op для совместимости со старыми вызовами).
     */
    public static void checkAndUpdateWaveSeeds(Context context) {
        // Seeds are dynamically selected or fetched via InnerTube
    }

    private static class WaveCallbackWrapper implements WaveCallback {
        private final WaveCallback delegate;

        WaveCallbackWrapper(WaveCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onTracksFound(ArrayList<MessageObject> tracks) {
            isLoading = false;
            if (!isWaveActive || sCancelFlag.get()) return;
            if (delegate != null) delegate.onTracksFound(tracks);
        }

        @Override
        public void onMoreTracksAdded(ArrayList<MessageObject> tracks) {
            isLoading = false;
            if (!isWaveActive || sCancelFlag.get()) return;
            if (delegate != null) delegate.onMoreTracksAdded(tracks);
        }

        @Override
        public void onError() {
            isLoading = false;
            if (!isWaveActive || sCancelFlag.get()) return;
            if (delegate != null) delegate.onError();
        }
    }
}
