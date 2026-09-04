package com.radolyn.ayugram.database;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenDelegate;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.SQLite;
import androidx.sqlite.SQLiteConnection;
import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.dao.DeletedMessageDao_Impl;
import com.radolyn.ayugram.database.dao.EditedMessageDao;
import com.radolyn.ayugram.database.dao.EditedMessageDao_Impl;
import com.radolyn.ayugram.database.dao.LastSeenDao;
import com.radolyn.ayugram.database.dao.LastSeenDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class AyuDatabase_Impl extends AyuDatabase {
  private volatile EditedMessageDao _editedMessageDao;

  private volatile DeletedMessageDao _deletedMessageDao;

  private volatile LastSeenDao _lastSeenDao;

  @Override
  @NonNull
  protected RoomOpenDelegate createOpenDelegate() {
    final RoomOpenDelegate _openDelegate = new RoomOpenDelegate(26, "7f649e4fe5c484d5efde9f2e0c89fa0a", "60a2cf5d11115b1416146b7fec683474") {
      @Override
      public void createAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `EditedMessage` (`fakeId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `dialogId` INTEGER NOT NULL, `groupedId` INTEGER NOT NULL, `peerId` INTEGER NOT NULL, `fromId` INTEGER NOT NULL, `topicId` INTEGER NOT NULL, `messageId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `flags` INTEGER NOT NULL, `editDate` INTEGER NOT NULL, `views` INTEGER NOT NULL, `forwards` INTEGER NOT NULL, `fwdFlags` INTEGER NOT NULL, `fwdFromId` INTEGER NOT NULL, `fwdName` TEXT, `fwdDate` INTEGER NOT NULL, `fwdPostAuthor` TEXT, `replyFlags` INTEGER NOT NULL, `replyMessageId` INTEGER NOT NULL, `replyPeerId` INTEGER NOT NULL, `replyTopId` INTEGER NOT NULL, `replyForumTopic` INTEGER NOT NULL, `replyQuote` INTEGER NOT NULL, `replyQuoteText` TEXT, `replyQuoteEntities` BLOB, `replyFromSerialized` BLOB, `entityCreateDate` INTEGER NOT NULL, `text` TEXT, `textEntities` BLOB, `mediaPath` TEXT, `hqThumbPath` TEXT, `documentType` INTEGER NOT NULL, `documentSerialized` BLOB, `thumbsSerialized` BLOB, `documentAttributesSerialized` BLOB, `mimeType` TEXT, `replyMarkupSerialized` BLOB)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_EditedMessage_userId_dialogId_messageId_entityCreateDate` ON `EditedMessage` (`userId`, `dialogId`, `messageId`, `entityCreateDate`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_EditedMessage_userId_entityCreateDate` ON `EditedMessage` (`userId`, `entityCreateDate`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_EditedMessage_dialogId_messageId` ON `EditedMessage` (`dialogId`, `messageId`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `DeletedMessage` (`fakeId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `dialogId` INTEGER NOT NULL, `groupedId` INTEGER NOT NULL, `peerId` INTEGER NOT NULL, `fromId` INTEGER NOT NULL, `topicId` INTEGER NOT NULL, `messageId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `flags` INTEGER NOT NULL, `editDate` INTEGER NOT NULL, `views` INTEGER NOT NULL, `forwards` INTEGER NOT NULL, `fwdFlags` INTEGER NOT NULL, `fwdFromId` INTEGER NOT NULL, `fwdName` TEXT, `fwdDate` INTEGER NOT NULL, `fwdPostAuthor` TEXT, `replyFlags` INTEGER NOT NULL, `replyMessageId` INTEGER NOT NULL, `replyPeerId` INTEGER NOT NULL, `replyTopId` INTEGER NOT NULL, `replyForumTopic` INTEGER NOT NULL, `replyQuote` INTEGER NOT NULL, `replyQuoteText` TEXT, `replyQuoteEntities` BLOB, `replyFromSerialized` BLOB, `entityCreateDate` INTEGER NOT NULL, `text` TEXT, `textEntities` BLOB, `mediaPath` TEXT, `hqThumbPath` TEXT, `documentType` INTEGER NOT NULL, `documentSerialized` BLOB, `thumbsSerialized` BLOB, `documentAttributesSerialized` BLOB, `mimeType` TEXT, `replyMarkupSerialized` BLOB)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_DeletedMessage_userId_dialogId_messageId` ON `DeletedMessage` (`userId`, `dialogId`, `messageId`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_DeletedMessage_userId_dialogId_topicId_messageId` ON `DeletedMessage` (`userId`, `dialogId`, `topicId`, `messageId`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_DeletedMessage_userId_dialogId_replyMessageId_messageId` ON `DeletedMessage` (`userId`, `dialogId`, `replyMessageId`, `messageId`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_DeletedMessage_userId_dialogId_groupedId_messageId` ON `DeletedMessage` (`userId`, `dialogId`, `groupedId`, `messageId`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_DeletedMessage_dialogId` ON `DeletedMessage` (`dialogId`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `DeletedMessageReaction` (`fakeReactionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `deletedMessageId` INTEGER NOT NULL, `emoticon` TEXT, `documentId` INTEGER NOT NULL, `isCustom` INTEGER NOT NULL, `count` INTEGER NOT NULL, `selfSelected` INTEGER NOT NULL)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_DeletedMessageReaction_deletedMessageId` ON `DeletedMessageReaction` (`deletedMessageId`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `LastSeenEntity` (`userId` INTEGER NOT NULL, `lastSeen` INTEGER NOT NULL, PRIMARY KEY(`userId`))");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        SQLite.execSQL(connection, "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7f649e4fe5c484d5efde9f2e0c89fa0a')");
      }

      @Override
      public void dropAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `EditedMessage`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `DeletedMessage`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `DeletedMessageReaction`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `LastSeenEntity`");
      }

      @Override
      public void onCreate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      public void onOpen(@NonNull final SQLiteConnection connection) {
        internalInitInvalidationTracker(connection);
      }

      @Override
      public void onPreMigrate(@NonNull final SQLiteConnection connection) {
        DBUtil.dropFtsSyncTriggers(connection);
      }

      @Override
      public void onPostMigrate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      @NonNull
      public RoomOpenDelegate.ValidationResult onValidateSchema(
          @NonNull final SQLiteConnection connection) {
        final Map<String, TableInfo.Column> _columnsEditedMessage = new HashMap<String, TableInfo.Column>(38);
        _columnsEditedMessage.put("fakeId", new TableInfo.Column("fakeId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("userId", new TableInfo.Column("userId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("dialogId", new TableInfo.Column("dialogId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("groupedId", new TableInfo.Column("groupedId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("peerId", new TableInfo.Column("peerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("fromId", new TableInfo.Column("fromId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("topicId", new TableInfo.Column("topicId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("messageId", new TableInfo.Column("messageId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("flags", new TableInfo.Column("flags", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("editDate", new TableInfo.Column("editDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("views", new TableInfo.Column("views", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("forwards", new TableInfo.Column("forwards", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("fwdFlags", new TableInfo.Column("fwdFlags", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("fwdFromId", new TableInfo.Column("fwdFromId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("fwdName", new TableInfo.Column("fwdName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("fwdDate", new TableInfo.Column("fwdDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("fwdPostAuthor", new TableInfo.Column("fwdPostAuthor", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyFlags", new TableInfo.Column("replyFlags", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyMessageId", new TableInfo.Column("replyMessageId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyPeerId", new TableInfo.Column("replyPeerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyTopId", new TableInfo.Column("replyTopId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyForumTopic", new TableInfo.Column("replyForumTopic", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyQuote", new TableInfo.Column("replyQuote", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyQuoteText", new TableInfo.Column("replyQuoteText", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyQuoteEntities", new TableInfo.Column("replyQuoteEntities", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyFromSerialized", new TableInfo.Column("replyFromSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("entityCreateDate", new TableInfo.Column("entityCreateDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("text", new TableInfo.Column("text", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("textEntities", new TableInfo.Column("textEntities", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("mediaPath", new TableInfo.Column("mediaPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("hqThumbPath", new TableInfo.Column("hqThumbPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("documentType", new TableInfo.Column("documentType", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("documentSerialized", new TableInfo.Column("documentSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("thumbsSerialized", new TableInfo.Column("thumbsSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("documentAttributesSerialized", new TableInfo.Column("documentAttributesSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("mimeType", new TableInfo.Column("mimeType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEditedMessage.put("replyMarkupSerialized", new TableInfo.Column("replyMarkupSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysEditedMessage = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesEditedMessage = new HashSet<TableInfo.Index>(3);
        _indicesEditedMessage.add(new TableInfo.Index("index_EditedMessage_userId_dialogId_messageId_entityCreateDate", false, Arrays.asList("userId", "dialogId", "messageId", "entityCreateDate"), Arrays.asList("ASC", "ASC", "ASC", "ASC")));
        _indicesEditedMessage.add(new TableInfo.Index("index_EditedMessage_userId_entityCreateDate", false, Arrays.asList("userId", "entityCreateDate"), Arrays.asList("ASC", "ASC")));
        _indicesEditedMessage.add(new TableInfo.Index("index_EditedMessage_dialogId_messageId", false, Arrays.asList("dialogId", "messageId"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoEditedMessage = new TableInfo("EditedMessage", _columnsEditedMessage, _foreignKeysEditedMessage, _indicesEditedMessage);
        final TableInfo _existingEditedMessage = TableInfo.read(connection, "EditedMessage");
        if (!_infoEditedMessage.equals(_existingEditedMessage)) {
          return new RoomOpenDelegate.ValidationResult(false, "EditedMessage(com.radolyn.ayugram.database.entities.EditedMessage).\n"
                  + " Expected:\n" + _infoEditedMessage + "\n"
                  + " Found:\n" + _existingEditedMessage);
        }
        final Map<String, TableInfo.Column> _columnsDeletedMessage = new HashMap<String, TableInfo.Column>(38);
        _columnsDeletedMessage.put("fakeId", new TableInfo.Column("fakeId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("userId", new TableInfo.Column("userId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("dialogId", new TableInfo.Column("dialogId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("groupedId", new TableInfo.Column("groupedId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("peerId", new TableInfo.Column("peerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("fromId", new TableInfo.Column("fromId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("topicId", new TableInfo.Column("topicId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("messageId", new TableInfo.Column("messageId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("flags", new TableInfo.Column("flags", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("editDate", new TableInfo.Column("editDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("views", new TableInfo.Column("views", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("forwards", new TableInfo.Column("forwards", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("fwdFlags", new TableInfo.Column("fwdFlags", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("fwdFromId", new TableInfo.Column("fwdFromId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("fwdName", new TableInfo.Column("fwdName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("fwdDate", new TableInfo.Column("fwdDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("fwdPostAuthor", new TableInfo.Column("fwdPostAuthor", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyFlags", new TableInfo.Column("replyFlags", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyMessageId", new TableInfo.Column("replyMessageId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyPeerId", new TableInfo.Column("replyPeerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyTopId", new TableInfo.Column("replyTopId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyForumTopic", new TableInfo.Column("replyForumTopic", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyQuote", new TableInfo.Column("replyQuote", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyQuoteText", new TableInfo.Column("replyQuoteText", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyQuoteEntities", new TableInfo.Column("replyQuoteEntities", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyFromSerialized", new TableInfo.Column("replyFromSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("entityCreateDate", new TableInfo.Column("entityCreateDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("text", new TableInfo.Column("text", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("textEntities", new TableInfo.Column("textEntities", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("mediaPath", new TableInfo.Column("mediaPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("hqThumbPath", new TableInfo.Column("hqThumbPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("documentType", new TableInfo.Column("documentType", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("documentSerialized", new TableInfo.Column("documentSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("thumbsSerialized", new TableInfo.Column("thumbsSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("documentAttributesSerialized", new TableInfo.Column("documentAttributesSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("mimeType", new TableInfo.Column("mimeType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessage.put("replyMarkupSerialized", new TableInfo.Column("replyMarkupSerialized", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysDeletedMessage = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesDeletedMessage = new HashSet<TableInfo.Index>(5);
        _indicesDeletedMessage.add(new TableInfo.Index("index_DeletedMessage_userId_dialogId_messageId", false, Arrays.asList("userId", "dialogId", "messageId"), Arrays.asList("ASC", "ASC", "ASC")));
        _indicesDeletedMessage.add(new TableInfo.Index("index_DeletedMessage_userId_dialogId_topicId_messageId", false, Arrays.asList("userId", "dialogId", "topicId", "messageId"), Arrays.asList("ASC", "ASC", "ASC", "ASC")));
        _indicesDeletedMessage.add(new TableInfo.Index("index_DeletedMessage_userId_dialogId_replyMessageId_messageId", false, Arrays.asList("userId", "dialogId", "replyMessageId", "messageId"), Arrays.asList("ASC", "ASC", "ASC", "ASC")));
        _indicesDeletedMessage.add(new TableInfo.Index("index_DeletedMessage_userId_dialogId_groupedId_messageId", false, Arrays.asList("userId", "dialogId", "groupedId", "messageId"), Arrays.asList("ASC", "ASC", "ASC", "ASC")));
        _indicesDeletedMessage.add(new TableInfo.Index("index_DeletedMessage_dialogId", false, Arrays.asList("dialogId"), Arrays.asList("ASC")));
        final TableInfo _infoDeletedMessage = new TableInfo("DeletedMessage", _columnsDeletedMessage, _foreignKeysDeletedMessage, _indicesDeletedMessage);
        final TableInfo _existingDeletedMessage = TableInfo.read(connection, "DeletedMessage");
        if (!_infoDeletedMessage.equals(_existingDeletedMessage)) {
          return new RoomOpenDelegate.ValidationResult(false, "DeletedMessage(com.radolyn.ayugram.database.entities.DeletedMessage).\n"
                  + " Expected:\n" + _infoDeletedMessage + "\n"
                  + " Found:\n" + _existingDeletedMessage);
        }
        final Map<String, TableInfo.Column> _columnsDeletedMessageReaction = new HashMap<String, TableInfo.Column>(7);
        _columnsDeletedMessageReaction.put("fakeReactionId", new TableInfo.Column("fakeReactionId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessageReaction.put("deletedMessageId", new TableInfo.Column("deletedMessageId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessageReaction.put("emoticon", new TableInfo.Column("emoticon", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessageReaction.put("documentId", new TableInfo.Column("documentId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessageReaction.put("isCustom", new TableInfo.Column("isCustom", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessageReaction.put("count", new TableInfo.Column("count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedMessageReaction.put("selfSelected", new TableInfo.Column("selfSelected", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysDeletedMessageReaction = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesDeletedMessageReaction = new HashSet<TableInfo.Index>(1);
        _indicesDeletedMessageReaction.add(new TableInfo.Index("index_DeletedMessageReaction_deletedMessageId", false, Arrays.asList("deletedMessageId"), Arrays.asList("ASC")));
        final TableInfo _infoDeletedMessageReaction = new TableInfo("DeletedMessageReaction", _columnsDeletedMessageReaction, _foreignKeysDeletedMessageReaction, _indicesDeletedMessageReaction);
        final TableInfo _existingDeletedMessageReaction = TableInfo.read(connection, "DeletedMessageReaction");
        if (!_infoDeletedMessageReaction.equals(_existingDeletedMessageReaction)) {
          return new RoomOpenDelegate.ValidationResult(false, "DeletedMessageReaction(com.radolyn.ayugram.database.entities.DeletedMessageReaction).\n"
                  + " Expected:\n" + _infoDeletedMessageReaction + "\n"
                  + " Found:\n" + _existingDeletedMessageReaction);
        }
        final Map<String, TableInfo.Column> _columnsLastSeenEntity = new HashMap<String, TableInfo.Column>(2);
        _columnsLastSeenEntity.put("userId", new TableInfo.Column("userId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLastSeenEntity.put("lastSeen", new TableInfo.Column("lastSeen", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysLastSeenEntity = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesLastSeenEntity = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLastSeenEntity = new TableInfo("LastSeenEntity", _columnsLastSeenEntity, _foreignKeysLastSeenEntity, _indicesLastSeenEntity);
        final TableInfo _existingLastSeenEntity = TableInfo.read(connection, "LastSeenEntity");
        if (!_infoLastSeenEntity.equals(_existingLastSeenEntity)) {
          return new RoomOpenDelegate.ValidationResult(false, "LastSeenEntity(com.radolyn.ayugram.database.entities.LastSeenEntity).\n"
                  + " Expected:\n" + _infoLastSeenEntity + "\n"
                  + " Found:\n" + _existingLastSeenEntity);
        }
        return new RoomOpenDelegate.ValidationResult(true, null);
      }
    };
    return _openDelegate;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final Map<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final Map<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "EditedMessage", "DeletedMessage", "DeletedMessageReaction", "LastSeenEntity");
  }

  @Override
  public void clearAllTables() {
    super.performClear(false, "EditedMessage", "DeletedMessage", "DeletedMessageReaction", "LastSeenEntity");
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final Map<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(EditedMessageDao.class, EditedMessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DeletedMessageDao.class, DeletedMessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LastSeenDao.class, LastSeenDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final Set<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public EditedMessageDao editedMessageDao() {
    if (_editedMessageDao != null) {
      return _editedMessageDao;
    } else {
      synchronized(this) {
        if(_editedMessageDao == null) {
          _editedMessageDao = new EditedMessageDao_Impl(this);
        }
        return _editedMessageDao;
      }
    }
  }

  @Override
  public DeletedMessageDao deletedMessageDao() {
    if (_deletedMessageDao != null) {
      return _deletedMessageDao;
    } else {
      synchronized(this) {
        if(_deletedMessageDao == null) {
          _deletedMessageDao = new DeletedMessageDao_Impl(this);
        }
        return _deletedMessageDao;
      }
    }
  }

  @Override
  public LastSeenDao lastSeenDao() {
    if (_lastSeenDao != null) {
      return _lastSeenDao;
    } else {
      synchronized(this) {
        if(_lastSeenDao == null) {
          _lastSeenDao = new LastSeenDao_Impl(this);
        }
        return _lastSeenDao;
      }
    }
  }
}
