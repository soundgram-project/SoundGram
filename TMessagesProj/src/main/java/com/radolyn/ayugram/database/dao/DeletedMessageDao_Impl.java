package com.radolyn.ayugram.database.dao;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.DeletedMessageFull;
import com.radolyn.ayugram.database.entities.DeletedMessageReaction;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import kotlin.Unit;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class DeletedMessageDao_Impl implements DeletedMessageDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<DeletedMessage> __insertAdapterOfDeletedMessage;

  private final EntityInsertAdapter<DeletedMessageReaction> __insertAdapterOfDeletedMessageReaction;

  public DeletedMessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfDeletedMessage = new EntityInsertAdapter<DeletedMessage>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `DeletedMessage` (`fakeId`,`userId`,`dialogId`,`groupedId`,`peerId`,`fromId`,`topicId`,`messageId`,`date`,`flags`,`editDate`,`views`,`forwards`,`fwdFlags`,`fwdFromId`,`fwdName`,`fwdDate`,`fwdPostAuthor`,`replyFlags`,`replyMessageId`,`replyPeerId`,`replyTopId`,`replyForumTopic`,`replyQuote`,`replyQuoteText`,`replyQuoteEntities`,`replyFromSerialized`,`entityCreateDate`,`text`,`textEntities`,`mediaPath`,`hqThumbPath`,`documentType`,`documentSerialized`,`thumbsSerialized`,`documentAttributesSerialized`,`mimeType`,`replyMarkupSerialized`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final DeletedMessage entity) {
        statement.bindLong(1, entity.fakeId);
        statement.bindLong(2, entity.userId);
        statement.bindLong(3, entity.dialogId);
        statement.bindLong(4, entity.groupedId);
        statement.bindLong(5, entity.peerId);
        statement.bindLong(6, entity.fromId);
        statement.bindLong(7, entity.topicId);
        statement.bindLong(8, entity.messageId);
        statement.bindLong(9, entity.date);
        statement.bindLong(10, entity.flags);
        statement.bindLong(11, entity.editDate);
        statement.bindLong(12, entity.views);
        statement.bindLong(13, entity.forwards);
        statement.bindLong(14, entity.fwdFlags);
        statement.bindLong(15, entity.fwdFromId);
        if (entity.fwdName == null) {
          statement.bindNull(16);
        } else {
          statement.bindText(16, entity.fwdName);
        }
        statement.bindLong(17, entity.fwdDate);
        if (entity.fwdPostAuthor == null) {
          statement.bindNull(18);
        } else {
          statement.bindText(18, entity.fwdPostAuthor);
        }
        statement.bindLong(19, entity.replyFlags);
        statement.bindLong(20, entity.replyMessageId);
        statement.bindLong(21, entity.replyPeerId);
        statement.bindLong(22, entity.replyTopId);
        final int _tmp = entity.replyForumTopic ? 1 : 0;
        statement.bindLong(23, _tmp);
        final int _tmp_1 = entity.replyQuote ? 1 : 0;
        statement.bindLong(24, _tmp_1);
        if (entity.replyQuoteText == null) {
          statement.bindNull(25);
        } else {
          statement.bindText(25, entity.replyQuoteText);
        }
        if (entity.replyQuoteEntities == null) {
          statement.bindNull(26);
        } else {
          statement.bindBlob(26, entity.replyQuoteEntities);
        }
        if (entity.replyFromSerialized == null) {
          statement.bindNull(27);
        } else {
          statement.bindBlob(27, entity.replyFromSerialized);
        }
        statement.bindLong(28, entity.entityCreateDate);
        if (entity.text == null) {
          statement.bindNull(29);
        } else {
          statement.bindText(29, entity.text);
        }
        if (entity.textEntities == null) {
          statement.bindNull(30);
        } else {
          statement.bindBlob(30, entity.textEntities);
        }
        if (entity.mediaPath == null) {
          statement.bindNull(31);
        } else {
          statement.bindText(31, entity.mediaPath);
        }
        if (entity.hqThumbPath == null) {
          statement.bindNull(32);
        } else {
          statement.bindText(32, entity.hqThumbPath);
        }
        statement.bindLong(33, entity.documentType);
        if (entity.documentSerialized == null) {
          statement.bindNull(34);
        } else {
          statement.bindBlob(34, entity.documentSerialized);
        }
        if (entity.thumbsSerialized == null) {
          statement.bindNull(35);
        } else {
          statement.bindBlob(35, entity.thumbsSerialized);
        }
        if (entity.documentAttributesSerialized == null) {
          statement.bindNull(36);
        } else {
          statement.bindBlob(36, entity.documentAttributesSerialized);
        }
        if (entity.mimeType == null) {
          statement.bindNull(37);
        } else {
          statement.bindText(37, entity.mimeType);
        }
        if (entity.replyMarkupSerialized == null) {
          statement.bindNull(38);
        } else {
          statement.bindBlob(38, entity.replyMarkupSerialized);
        }
      }
    };
    this.__insertAdapterOfDeletedMessageReaction = new EntityInsertAdapter<DeletedMessageReaction>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `DeletedMessageReaction` (`fakeReactionId`,`deletedMessageId`,`emoticon`,`documentId`,`isCustom`,`count`,`selfSelected`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          final DeletedMessageReaction entity) {
        statement.bindLong(1, entity.fakeReactionId);
        statement.bindLong(2, entity.deletedMessageId);
        if (entity.emoticon == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.emoticon);
        }
        statement.bindLong(4, entity.documentId);
        final int _tmp = entity.isCustom ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.count);
        final int _tmp_1 = entity.selfSelected ? 1 : 0;
        statement.bindLong(7, _tmp_1);
      }
    };
  }

  @Override
  public long insert(final DeletedMessage msg) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfDeletedMessage.insertAndReturnId(_connection, msg);
    });
  }

  @Override
  public void insertReaction(final DeletedMessageReaction reaction) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfDeletedMessageReaction.insert(_connection, reaction);
      return null;
    });
  }

  @Override
  public DeletedMessageFull getMessage(final long userId, final long dialogId,
      final int messageId) {
    final String _sql = "SELECT * FROM deletedmessage WHERE userId = ? AND dialogId = ? AND messageId = ?";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, messageId);
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final DeletedMessageFull _result;
        if (_stmt.step()) {
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _result = new DeletedMessageFull();
          _result.message = _tmpMessage;
          _result.reactions = _tmpReactionsCollection;
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public String getMediaPath(final long userId, final long dialogId, final int messageId) {
    final String _sql = "SELECT mediaPath FROM deletedmessage WHERE userId = ? AND dialogId = ? AND messageId = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, messageId);
        final String _result;
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null;
          } else {
            _result = _stmt.getText(0);
          }
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<DeletedMessageFull> getMessages(final long userId, final long dialogId,
      final long startId, final long endId, final int limit) {
    final String _sql = "SELECT * FROM deletedmessage WHERE userId = ? AND dialogId = ? AND ? <= messageId AND messageId <= ? ORDER BY messageId LIMIT ?";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, startId);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, endId);
        _argIndex = 5;
        _stmt.bindLong(_argIndex, limit);
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final List<DeletedMessageFull> _result = new ArrayList<DeletedMessageFull>();
        while (_stmt.step()) {
          final DeletedMessageFull _item;
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _item = new DeletedMessageFull();
          _item.message = _tmpMessage;
          _item.reactions = _tmpReactionsCollection;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<DeletedMessageFull> getTopicMessages(final long userId, final long dialogId,
      final long topicId, final long startId, final long endId, final int limit) {
    final String _sql = "SELECT * FROM deletedmessage WHERE userId = ? AND dialogId = ? AND topicId = ? AND ? <= messageId AND messageId <= ? ORDER BY messageId LIMIT ?";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, topicId);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, startId);
        _argIndex = 5;
        _stmt.bindLong(_argIndex, endId);
        _argIndex = 6;
        _stmt.bindLong(_argIndex, limit);
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final List<DeletedMessageFull> _result = new ArrayList<DeletedMessageFull>();
        while (_stmt.step()) {
          final DeletedMessageFull _item;
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _item = new DeletedMessageFull();
          _item.message = _tmpMessage;
          _item.reactions = _tmpReactionsCollection;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<DeletedMessageFull> getThreadMessages(final long userId, final long dialogId,
      final long threadMessageId, final long startId, final long endId, final int limit) {
    final String _sql = "SELECT * FROM deletedmessage WHERE userId = ? AND dialogId = ? AND replyMessageId = ? AND ? <= messageId AND messageId <= ? ORDER BY messageId LIMIT ?";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, threadMessageId);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, startId);
        _argIndex = 5;
        _stmt.bindLong(_argIndex, endId);
        _argIndex = 6;
        _stmt.bindLong(_argIndex, limit);
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final List<DeletedMessageFull> _result = new ArrayList<DeletedMessageFull>();
        while (_stmt.step()) {
          final DeletedMessageFull _item;
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _item = new DeletedMessageFull();
          _item.message = _tmpMessage;
          _item.reactions = _tmpReactionsCollection;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<DeletedMessageFull> getMessagesGrouped(final long userId, final long dialogId,
      final long groupedId) {
    final String _sql = "SELECT * FROM deletedmessage WHERE userId = ? AND dialogId = ? AND groupedId = ? ORDER BY messageId";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, groupedId);
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final List<DeletedMessageFull> _result = new ArrayList<DeletedMessageFull>();
        while (_stmt.step()) {
          final DeletedMessageFull _item;
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _item = new DeletedMessageFull();
          _item.message = _tmpMessage;
          _item.reactions = _tmpReactionsCollection;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<DeletedMessageFull> getMessagesGroupedIn(final long userId, final long dialogId,
      final List<Long> groupedIds) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT * FROM deletedmessage WHERE userId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND dialogId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND groupedId IN (");
    final int _inputSize = groupedIds == null ? 1 : groupedIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") ORDER BY messageId");
    final String _sql = _stringBuilder.toString();
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        if (groupedIds == null) {
          _stmt.bindNull(_argIndex);
        } else {
          for (Long _item : groupedIds) {
            if (_item == null) {
              _stmt.bindNull(_argIndex);
            } else {
              _stmt.bindLong(_argIndex, _item);
            }
            _argIndex++;
          }
        }
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final List<DeletedMessageFull> _result = new ArrayList<DeletedMessageFull>();
        while (_stmt.step()) {
          final DeletedMessageFull _item_1;
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _item_1 = new DeletedMessageFull();
          _item_1.message = _tmpMessage;
          _item_1.reactions = _tmpReactionsCollection;
          _result.add(_item_1);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<DeletedMessageFull> getMessagesByDialog(final long dialogId) {
    final String _sql = "SELECT * FROM deletedmessage WHERE dialogId = ?";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, dialogId);
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final List<DeletedMessageFull> _result = new ArrayList<DeletedMessageFull>();
        while (_stmt.step()) {
          final DeletedMessageFull _item;
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _item = new DeletedMessageFull();
          _item.message = _tmpMessage;
          _item.reactions = _tmpReactionsCollection;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<DeletedMessageFull> getLatestMessages(final long userId, final long dialogId,
      final int limit) {
    final String _sql = "SELECT * FROM deletedmessage WHERE userId = ? AND dialogId = ? ORDER BY messageId DESC LIMIT ?";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, limit);
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final List<DeletedMessageFull> _result = new ArrayList<DeletedMessageFull>();
        while (_stmt.step()) {
          final DeletedMessageFull _item;
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _item = new DeletedMessageFull();
          _item.message = _tmpMessage;
          _item.reactions = _tmpReactionsCollection;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<DeletedMessageFull> getOlderMessagesBefore(final long userId, final long dialogId,
      final int before, final int limit) {
    final String _sql = "SELECT * FROM deletedmessage WHERE userId = ? AND dialogId = ? AND messageId < ? ORDER BY messageId DESC LIMIT ?";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, before);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, limit);
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final List<DeletedMessageFull> _result = new ArrayList<DeletedMessageFull>();
        while (_stmt.step()) {
          final DeletedMessageFull _item;
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _item = new DeletedMessageFull();
          _item.message = _tmpMessage;
          _item.reactions = _tmpReactionsCollection;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<Integer> getExistingMessageIds(final long userId, final long dialogId,
      final List<Integer> messageIds) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT messageId FROM deletedmessage WHERE userId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND dialogId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND messageId IN (");
    final int _inputSize = messageIds == null ? 1 : messageIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        if (messageIds == null) {
          _stmt.bindNull(_argIndex);
        } else {
          for (Integer _item : messageIds) {
            if (_item == null) {
              _stmt.bindNull(_argIndex);
            } else {
              _stmt.bindLong(_argIndex, _item);
            }
            _argIndex++;
          }
        }
        final List<Integer> _result = new ArrayList<Integer>();
        while (_stmt.step()) {
          final Integer _item_1;
          if (_stmt.isNull(0)) {
            _item_1 = null;
          } else {
            _item_1 = (int) (_stmt.getLong(0));
          }
          _result.add(_item_1);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<DeletedMessageFull> getMessagesByIds(final long userId, final long dialogId,
      final List<Integer> messageIds) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT * FROM deletedmessage WHERE userId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND dialogId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND messageId IN (");
    final int _inputSize = messageIds == null ? 1 : messageIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        if (messageIds == null) {
          _stmt.bindNull(_argIndex);
        } else {
          for (Integer _item : messageIds) {
            if (_item == null) {
              _stmt.bindNull(_argIndex);
            } else {
              _stmt.bindLong(_argIndex, _item);
            }
            _argIndex++;
          }
        }
        final int _columnIndexOfFakeId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fakeId");
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfDialogId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "dialogId");
        final int _columnIndexOfGroupedId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "groupedId");
        final int _columnIndexOfPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "peerId");
        final int _columnIndexOfFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fromId");
        final int _columnIndexOfTopicId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "topicId");
        final int _columnIndexOfMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "messageId");
        final int _columnIndexOfDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date");
        final int _columnIndexOfFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "flags");
        final int _columnIndexOfEditDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "editDate");
        final int _columnIndexOfViews = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "views");
        final int _columnIndexOfForwards = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "forwards");
        final int _columnIndexOfFwdFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFlags");
        final int _columnIndexOfFwdFromId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdFromId");
        final int _columnIndexOfFwdName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdName");
        final int _columnIndexOfFwdDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdDate");
        final int _columnIndexOfFwdPostAuthor = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "fwdPostAuthor");
        final int _columnIndexOfReplyFlags = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFlags");
        final int _columnIndexOfReplyMessageId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMessageId");
        final int _columnIndexOfReplyPeerId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyPeerId");
        final int _columnIndexOfReplyTopId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyTopId");
        final int _columnIndexOfReplyForumTopic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyForumTopic");
        final int _columnIndexOfReplyQuote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuote");
        final int _columnIndexOfReplyQuoteText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteText");
        final int _columnIndexOfReplyQuoteEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyQuoteEntities");
        final int _columnIndexOfReplyFromSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyFromSerialized");
        final int _columnIndexOfEntityCreateDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "entityCreateDate");
        final int _columnIndexOfText = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "text");
        final int _columnIndexOfTextEntities = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "textEntities");
        final int _columnIndexOfMediaPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mediaPath");
        final int _columnIndexOfHqThumbPath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "hqThumbPath");
        final int _columnIndexOfDocumentType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentType");
        final int _columnIndexOfDocumentSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentSerialized");
        final int _columnIndexOfThumbsSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "thumbsSerialized");
        final int _columnIndexOfDocumentAttributesSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "documentAttributesSerialized");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mimeType");
        final int _columnIndexOfReplyMarkupSerialized = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "replyMarkupSerialized");
        final LongSparseArray<ArrayList<DeletedMessageReaction>> _collectionReactions = new LongSparseArray<ArrayList<DeletedMessageReaction>>();
        while (_stmt.step()) {
          final Long _tmpKey;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey != null) {
            if (!_collectionReactions.containsKey(_tmpKey)) {
              _collectionReactions.put(_tmpKey, new ArrayList<DeletedMessageReaction>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _collectionReactions);
        final List<DeletedMessageFull> _result = new ArrayList<DeletedMessageFull>();
        while (_stmt.step()) {
          final DeletedMessageFull _item_1;
          final DeletedMessage _tmpMessage;
          if (!(_stmt.isNull(_columnIndexOfFakeId) && _stmt.isNull(_columnIndexOfUserId) && _stmt.isNull(_columnIndexOfDialogId) && _stmt.isNull(_columnIndexOfGroupedId) && _stmt.isNull(_columnIndexOfPeerId) && _stmt.isNull(_columnIndexOfFromId) && _stmt.isNull(_columnIndexOfTopicId) && _stmt.isNull(_columnIndexOfMessageId) && _stmt.isNull(_columnIndexOfDate) && _stmt.isNull(_columnIndexOfFlags) && _stmt.isNull(_columnIndexOfEditDate) && _stmt.isNull(_columnIndexOfViews) && _stmt.isNull(_columnIndexOfForwards) && _stmt.isNull(_columnIndexOfFwdFlags) && _stmt.isNull(_columnIndexOfFwdFromId) && _stmt.isNull(_columnIndexOfFwdName) && _stmt.isNull(_columnIndexOfFwdDate) && _stmt.isNull(_columnIndexOfFwdPostAuthor) && _stmt.isNull(_columnIndexOfReplyFlags) && _stmt.isNull(_columnIndexOfReplyMessageId) && _stmt.isNull(_columnIndexOfReplyPeerId) && _stmt.isNull(_columnIndexOfReplyTopId) && _stmt.isNull(_columnIndexOfReplyForumTopic) && _stmt.isNull(_columnIndexOfReplyQuote) && _stmt.isNull(_columnIndexOfReplyQuoteText) && _stmt.isNull(_columnIndexOfReplyQuoteEntities) && _stmt.isNull(_columnIndexOfReplyFromSerialized) && _stmt.isNull(_columnIndexOfEntityCreateDate) && _stmt.isNull(_columnIndexOfText) && _stmt.isNull(_columnIndexOfTextEntities) && _stmt.isNull(_columnIndexOfMediaPath) && _stmt.isNull(_columnIndexOfHqThumbPath) && _stmt.isNull(_columnIndexOfDocumentType) && _stmt.isNull(_columnIndexOfDocumentSerialized) && _stmt.isNull(_columnIndexOfThumbsSerialized) && _stmt.isNull(_columnIndexOfDocumentAttributesSerialized) && _stmt.isNull(_columnIndexOfMimeType) && _stmt.isNull(_columnIndexOfReplyMarkupSerialized))) {
            _tmpMessage = new DeletedMessage();
            _tmpMessage.fakeId = _stmt.getLong(_columnIndexOfFakeId);
            _tmpMessage.userId = _stmt.getLong(_columnIndexOfUserId);
            _tmpMessage.dialogId = _stmt.getLong(_columnIndexOfDialogId);
            _tmpMessage.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
            _tmpMessage.peerId = _stmt.getLong(_columnIndexOfPeerId);
            _tmpMessage.fromId = _stmt.getLong(_columnIndexOfFromId);
            _tmpMessage.topicId = _stmt.getLong(_columnIndexOfTopicId);
            _tmpMessage.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
            _tmpMessage.date = (int) (_stmt.getLong(_columnIndexOfDate));
            _tmpMessage.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
            _tmpMessage.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
            _tmpMessage.views = (int) (_stmt.getLong(_columnIndexOfViews));
            _tmpMessage.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
            _tmpMessage.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
            _tmpMessage.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
            if (_stmt.isNull(_columnIndexOfFwdName)) {
              _tmpMessage.fwdName = null;
            } else {
              _tmpMessage.fwdName = _stmt.getText(_columnIndexOfFwdName);
            }
            _tmpMessage.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
            if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
              _tmpMessage.fwdPostAuthor = null;
            } else {
              _tmpMessage.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
            }
            _tmpMessage.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
            _tmpMessage.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
            _tmpMessage.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
            _tmpMessage.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
            final int _tmp;
            _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
            _tmpMessage.replyForumTopic = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
            _tmpMessage.replyQuote = _tmp_1 != 0;
            if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
              _tmpMessage.replyQuoteText = null;
            } else {
              _tmpMessage.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
            }
            if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
              _tmpMessage.replyQuoteEntities = null;
            } else {
              _tmpMessage.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
            }
            if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
              _tmpMessage.replyFromSerialized = null;
            } else {
              _tmpMessage.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
            }
            _tmpMessage.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
            if (_stmt.isNull(_columnIndexOfText)) {
              _tmpMessage.text = null;
            } else {
              _tmpMessage.text = _stmt.getText(_columnIndexOfText);
            }
            if (_stmt.isNull(_columnIndexOfTextEntities)) {
              _tmpMessage.textEntities = null;
            } else {
              _tmpMessage.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
            }
            if (_stmt.isNull(_columnIndexOfMediaPath)) {
              _tmpMessage.mediaPath = null;
            } else {
              _tmpMessage.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
            }
            if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
              _tmpMessage.hqThumbPath = null;
            } else {
              _tmpMessage.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
            }
            _tmpMessage.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
            if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
              _tmpMessage.documentSerialized = null;
            } else {
              _tmpMessage.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
            }
            if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
              _tmpMessage.thumbsSerialized = null;
            } else {
              _tmpMessage.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
            }
            if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
              _tmpMessage.documentAttributesSerialized = null;
            } else {
              _tmpMessage.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
            }
            if (_stmt.isNull(_columnIndexOfMimeType)) {
              _tmpMessage.mimeType = null;
            } else {
              _tmpMessage.mimeType = _stmt.getText(_columnIndexOfMimeType);
            }
            if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
              _tmpMessage.replyMarkupSerialized = null;
            } else {
              _tmpMessage.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
            }
          } else {
            _tmpMessage = null;
          }
          final ArrayList<DeletedMessageReaction> _tmpReactionsCollection;
          final Long _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfFakeId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfFakeId);
          }
          if (_tmpKey_1 != null) {
            _tmpReactionsCollection = _collectionReactions.get(_tmpKey_1);
          } else {
            _tmpReactionsCollection = new ArrayList<DeletedMessageReaction>();
          }
          _item_1 = new DeletedMessageFull();
          _item_1.message = _tmpMessage;
          _item_1.reactions = _tmpReactionsCollection;
          _result.add(_item_1);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int countByDialog(final long userId, final long dialogId) {
    final String _sql = "SELECT COUNT(*) FROM deletedmessage WHERE userId = ? AND dialogId = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        final int _result;
        if (_stmt.step()) {
          _result = (int) (_stmt.getLong(0));
        } else {
          _result = 0;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public boolean exists(final long userId, final long dialogId, final long topicId,
      final int msgId) {
    final String _sql = "SELECT EXISTS(SELECT * FROM deletedmessage WHERE userId = ? AND dialogId = ? AND topicId = ? AND messageId = ?)";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, topicId);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, msgId);
        final boolean _result;
        if (_stmt.step()) {
          final int _tmp;
          _tmp = (int) (_stmt.getLong(0));
          _result = _tmp != 0;
        } else {
          _result = false;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete(final long userId, final long dialogId, final int msgId) {
    final String _sql = "DELETE FROM deletedmessage WHERE userId = ? AND dialogId = ? AND messageId = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, msgId);
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete(final long dialogId) {
    final String _sql = "DELETE FROM deletedmessage WHERE dialogId = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, dialogId);
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void deleteMessages(final long userId, final long dialogId,
      final List<Integer> messageIds) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("DELETE FROM deletedmessage WHERE userId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND dialogId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND messageId IN (");
    final int _inputSize = messageIds == null ? 1 : messageIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        if (messageIds == null) {
          _stmt.bindNull(_argIndex);
        } else {
          for (Integer _item : messageIds) {
            if (_item == null) {
              _stmt.bindNull(_argIndex);
            } else {
              _stmt.bindLong(_argIndex, _item);
            }
            _argIndex++;
          }
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void updateMediaPathIfEmpty(final long userId, final long dialogId, final int messageId,
      final String newPath) {
    final String _sql = "UPDATE deletedmessage SET mediaPath = ? WHERE userId = ? AND dialogId = ? AND messageId = ? AND (mediaPath IS NULL OR mediaPath = '')";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (newPath == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, newPath);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, messageId);
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private void __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(
      @NonNull final SQLiteConnection _connection,
      @NonNull final LongSparseArray<ArrayList<DeletedMessageReaction>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > 999) {
      RelationUtil.recursiveFetchLongSparseArray(_map, true, (_tmpMap) -> {
        __fetchRelationshipDeletedMessageReactionAscomRadolynAyugramDatabaseEntitiesDeletedMessageReaction(_connection, _tmpMap);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT `fakeReactionId`,`deletedMessageId`,`emoticon`,`documentId`,`isCustom`,`count`,`selfSelected` FROM `DeletedMessageReaction` WHERE `deletedMessageId` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final SQLiteStatement _stmt = _connection.prepare(_sql);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    try {
      final int _itemKeyIndex = SQLiteStatementUtil.getColumnIndex(_stmt, "deletedMessageId");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _columnIndexOfFakeReactionId = 0;
      final int _columnIndexOfDeletedMessageId = 1;
      final int _columnIndexOfEmoticon = 2;
      final int _columnIndexOfDocumentId = 3;
      final int _columnIndexOfIsCustom = 4;
      final int _columnIndexOfCount = 5;
      final int _columnIndexOfSelfSelected = 6;
      while (_stmt.step()) {
        final long _tmpKey;
        _tmpKey = _stmt.getLong(_itemKeyIndex);
        final ArrayList<DeletedMessageReaction> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final DeletedMessageReaction _item_1;
          _item_1 = new DeletedMessageReaction();
          _item_1.fakeReactionId = _stmt.getLong(_columnIndexOfFakeReactionId);
          _item_1.deletedMessageId = _stmt.getLong(_columnIndexOfDeletedMessageId);
          if (_stmt.isNull(_columnIndexOfEmoticon)) {
            _item_1.emoticon = null;
          } else {
            _item_1.emoticon = _stmt.getText(_columnIndexOfEmoticon);
          }
          _item_1.documentId = _stmt.getLong(_columnIndexOfDocumentId);
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsCustom));
          _item_1.isCustom = _tmp != 0;
          _item_1.count = (int) (_stmt.getLong(_columnIndexOfCount));
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfSelfSelected));
          _item_1.selfSelected = _tmp_1 != 0;
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _stmt.close();
    }
  }
}
