package soundgram.music.wave;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

public final class WaveHistoryDao extends SQLiteOpenHelper {

    private static final String TAG = "WaveHistoryDao";
    private static final String DB_NAME = "soundgram_wave_history.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_NAME = "wave_history";
    private static final String COL_HASH = "track_hash";
    private static final String COL_PLAYED_AT = "played_at";

    private static final long EXPIRATION_WINDOW_MS = 24L * 60 * 60 * 1000; // 24 hours

    private static volatile WaveHistoryDao sInstance;

    public static WaveHistoryDao getInstance() {
        if (sInstance == null) {
            synchronized (WaveHistoryDao.class) {
                if (sInstance == null) {
                    Context ctx = ApplicationLoader.applicationContext;
                    sInstance = new WaveHistoryDao(ctx);
                }
            }
        }
        return sInstance;
    }

    private WaveHistoryDao(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + COL_HASH + " TEXT PRIMARY KEY, "
                + COL_PLAYED_AT + " INTEGER NOT NULL);");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_wave_played_at ON " + TABLE_NAME + " (" + COL_PLAYED_AT + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * Checks if the track hash was played within the last 24 hours.
     */
    public boolean isRecentlyPlayed(String hash) {
        if (hash == null || hash.isEmpty()) return false;
        long cutoff = System.currentTimeMillis() - EXPIRATION_WINDOW_MS;
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(
                    TABLE_NAME,
                    new String[]{COL_PLAYED_AT},
                    COL_HASH + " = ? AND " + COL_PLAYED_AT + " > ?",
                    new String[]{hash, String.valueOf(cutoff)},
                    null, null, null, "1"
            );
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            FileLog.e(TAG + " isRecentlyPlayed error: " + e);
            return false;
        } finally {
            if (cursor != null) {
                try { cursor.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Marks track hash as played right now.
     */
    public void markAsPlayed(String hash) {
        if (hash == null || hash.isEmpty()) return;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COL_HASH, hash);
            cv.put(COL_PLAYED_AT, System.currentTimeMillis());
            db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            FileLog.e(TAG + " markAsPlayed error: " + e);
        }
    }

    /**
     * Removes records older than 24 hours to keep the database slim.
     */
    public void pruneExpired() {
        try {
            long cutoff = System.currentTimeMillis() - EXPIRATION_WINDOW_MS;
            SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_NAME, COL_PLAYED_AT + " <= ?", new String[]{String.valueOf(cutoff)});
        } catch (Exception e) {
            FileLog.e(TAG + " pruneExpired error: " + e);
        }
    }
}
