/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.database;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.dao.EditedMessageDao;
import com.radolyn.ayugram.database.dao.LastSeenDao;
import com.radolyn.ayugram.messages.AyuMessagesController;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.File;

public class AyuData {
    public static long dbSize, attachmentsSize, totalSize;
    private static AyuDatabase database;
    private static EditedMessageDao editedMessageDao;
    private static DeletedMessageDao deletedMessageDao;
    private static LastSeenDao lastSeenDao;

    private static final Migration MIGRATION_21_22 = new Migration(21, 22) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS index_deletedmessage_userId_dialogId_messageId ON deletedmessage(userId, dialogId, messageId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_deletedmessage_userId_dialogId_topicId_messageId ON deletedmessage(userId, dialogId, topicId, messageId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_deletedmessage_userId_dialogId_replyMessageId_messageId ON deletedmessage(userId, dialogId, replyMessageId, messageId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_deletedmessage_userId_dialogId_groupedId_messageId ON deletedmessage(userId, dialogId, groupedId, messageId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_deletedmessage_dialogId ON deletedmessage(dialogId)");

            database.execSQL("CREATE INDEX IF NOT EXISTS index_editedmessage_userId_dialogId_messageId_entityCreateDate ON editedmessage(userId, dialogId, messageId, entityCreateDate)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_editedmessage_userId_entityCreateDate ON editedmessage(userId, entityCreateDate)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_editedmessage_dialogId_messageId ON editedmessage(dialogId, messageId)");

            database.execSQL("CREATE INDEX IF NOT EXISTS index_deletedmessagereaction_deletedMessageId ON deletedmessagereaction(deletedMessageId)");
        }
    };

    private static final Migration MIGRATION_22_23 = new Migration(22, 23) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE DeletedMessage ADD COLUMN replyQuote INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE DeletedMessage ADD COLUMN replyQuoteText TEXT");
            database.execSQL("ALTER TABLE DeletedMessage ADD COLUMN replyQuoteEntities BLOB");
            database.execSQL("ALTER TABLE DeletedMessage ADD COLUMN replyFromSerialized BLOB");

            database.execSQL("ALTER TABLE EditedMessage ADD COLUMN replyQuote INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE EditedMessage ADD COLUMN replyQuoteText TEXT");
            database.execSQL("ALTER TABLE EditedMessage ADD COLUMN replyQuoteEntities BLOB");
            database.execSQL("ALTER TABLE EditedMessage ADD COLUMN replyFromSerialized BLOB");
        }
    };

    private static final Migration MIGRATION_23_24 = new Migration(23, 24) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE DeletedMessage ADD COLUMN replyMarkupSerialized BLOB");
            database.execSQL("ALTER TABLE EditedMessage ADD COLUMN replyMarkupSerialized BLOB");
        }
    };

    private static final Migration MIGRATION_24_25 = new Migration(24, 25) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE DeletedMessage ADD COLUMN forwards INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE EditedMessage ADD COLUMN forwards INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_25_26 = new Migration(25, 26) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS LastSeenEntity (userId INTEGER NOT NULL, lastSeen INTEGER NOT NULL, PRIMARY KEY(userId))");
        }
    };

    static {
        try {
            create();
        } catch (Throwable t) {
            FileLog.e("AyuData init error", t);
        }
    }

    public static synchronized void create() {
        if (ApplicationLoader.applicationContext == null) {
            return;
        }
        try {
            database = Room.databaseBuilder(ApplicationLoader.applicationContext, AyuDatabase.class, AyuConstants.AYU_DATABASE)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addMigrations(MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26)
                    .build();

            editedMessageDao = database.editedMessageDao();
            deletedMessageDao = database.deletedMessageDao();
            lastSeenDao = database.lastSeenDao();
        } catch (Throwable t) {
            FileLog.e("AyuData buildDatabase error", t);
        }
    }

    public static synchronized AyuDatabase getDatabase() {
        if (database == null) {
            create();
        }
        return database;
    }

    public static synchronized EditedMessageDao getEditedMessageDao() {
        if (editedMessageDao == null && getDatabase() != null) {
            editedMessageDao = database.editedMessageDao();
        }
        return editedMessageDao;
    }

    public static synchronized DeletedMessageDao getDeletedMessageDao() {
        if (deletedMessageDao == null && getDatabase() != null) {
            deletedMessageDao = database.deletedMessageDao();
        }
        return deletedMessageDao;
    }

    public static synchronized LastSeenDao getLastSeenDao() {
        if (lastSeenDao == null && getDatabase() != null) {
            lastSeenDao = database.lastSeenDao();
        }
        return lastSeenDao;
    }

    public static synchronized void clean() {
        if (database != null) {
            try {
                database.close();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        database = null;
        editedMessageDao = null;
        deletedMessageDao = null;
        lastSeenDao = null;

        ApplicationLoader.applicationContext.deleteDatabase(AyuConstants.AYU_DATABASE);
    }

    public static long getDatabaseSize() {
        long size = 0;
        try {
            File dbFile = ApplicationLoader.applicationContext.getDatabasePath(AyuConstants.AYU_DATABASE);
            File shmCacheFile = new File(dbFile.getAbsolutePath() + "-shm");
            File walCacheFile = new File(dbFile.getAbsolutePath() + "-wal");
            if (dbFile.exists()) {
                size = dbFile.length();
            }
            if (shmCacheFile.exists()) {
                size += shmCacheFile.length();
            }
            if (walCacheFile.exists()) {
                size += walCacheFile.length();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return size;
    }

    public static long getDirectorySize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                size += f.isDirectory() ? getDirectorySize(f) : f.length();
            }
        }
        return size;
    }

    public static long getAttachmentsDirSize() {
        long size = 0;
        try {
            if (AyuMessagesController.attachmentsPath.exists()) {
                size = getDirectorySize(AyuMessagesController.attachmentsPath);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return size;
    }

    public static void loadSizes(Runnable callback) {
        Utilities.globalQueue.postRunnable(() -> {
            dbSize = getDatabaseSize();
            attachmentsSize = getAttachmentsDirSize();
            totalSize = dbSize + attachmentsSize;
            if (callback != null) {
                AndroidUtilities.runOnUIThread(callback, 500);
            }
        });
    }
}
