package org.telegram.messenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SoundGramVault extends SQLiteOpenHelper {
    private static SoundGramVault instance;

    public static SoundGramVault getInstance() {
        if (instance == null) {
            instance = new SoundGramVault(ApplicationLoader.applicationContext);
        }
        return instance;
    }

    public SoundGramVault(Context context) {
        super(context, "soundgram_vault.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE removed (dialog_id INTEGER NOT NULL, message_id INTEGER NOT NULL, PRIMARY KEY (dialog_id, message_id))");
        db.execSQL("CREATE TABLE revisions (dialog_id INTEGER NOT NULL, message_id INTEGER NOT NULL, idx INTEGER NOT NULL, text TEXT NOT NULL DEFAULT '', PRIMARY KEY (dialog_id, message_id, idx))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS removed");
        db.execSQL("DROP TABLE IF EXISTS revisions");
        onCreate(db);
    }

    public void wipe() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM removed");
        db.execSQL("DELETE FROM revisions");
    }

    public void markRemoved(long dialogId, int messageId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("dialog_id", dialogId);
        values.put("message_id", messageId);
        db.insertWithOnConflict("removed", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean isRemoved(long dialogId, int messageId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM removed WHERE dialog_id = ? AND message_id = ?",
                new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public void pushRevision(long dialogId, int messageId, String text) {
        if (text == null || text.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(idx) FROM revisions WHERE dialog_id = ? AND message_id = ?", new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
        int nextIdx = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            nextIdx = cursor.getInt(0) + 1;
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("dialog_id", dialogId);
        values.put("message_id", messageId);
        values.put("idx", nextIdx);
        values.put("text", text);
        db.insertWithOnConflict("revisions", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }
}
