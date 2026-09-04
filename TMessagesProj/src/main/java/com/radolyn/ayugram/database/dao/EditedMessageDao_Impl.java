package com.radolyn.ayugram.database.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteConnectionUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.SQLiteStatement;
import com.radolyn.ayugram.database.entities.EditedMessage;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class EditedMessageDao_Impl implements EditedMessageDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<EditedMessage> __insertAdapterOfEditedMessage;

  public EditedMessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfEditedMessage = new EntityInsertAdapter<EditedMessage>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `EditedMessage` (`fakeId`,`userId`,`dialogId`,`groupedId`,`peerId`,`fromId`,`topicId`,`messageId`,`date`,`flags`,`editDate`,`views`,`forwards`,`fwdFlags`,`fwdFromId`,`fwdName`,`fwdDate`,`fwdPostAuthor`,`replyFlags`,`replyMessageId`,`replyPeerId`,`replyTopId`,`replyForumTopic`,`replyQuote`,`replyQuoteText`,`replyQuoteEntities`,`replyFromSerialized`,`entityCreateDate`,`text`,`textEntities`,`mediaPath`,`hqThumbPath`,`documentType`,`documentSerialized`,`thumbsSerialized`,`documentAttributesSerialized`,`mimeType`,`replyMarkupSerialized`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final EditedMessage entity) {
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
  }

  @Override
  public void insert(final EditedMessage revision) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfEditedMessage.insert(_connection, revision);
      return null;
    });
  }

  @Override
  public List<EditedMessage> getAllRevisions(final long userId, final long dialogId,
      final long messageId) {
    final String _sql = "SELECT * FROM editedmessage WHERE userId = ? AND dialogId = ? AND messageId = ? ORDER BY entityCreateDate";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
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
        final List<EditedMessage> _result = new ArrayList<EditedMessage>();
        while (_stmt.step()) {
          final EditedMessage _item;
          _item = new EditedMessage();
          _item.fakeId = _stmt.getLong(_columnIndexOfFakeId);
          _item.userId = _stmt.getLong(_columnIndexOfUserId);
          _item.dialogId = _stmt.getLong(_columnIndexOfDialogId);
          _item.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
          _item.peerId = _stmt.getLong(_columnIndexOfPeerId);
          _item.fromId = _stmt.getLong(_columnIndexOfFromId);
          _item.topicId = _stmt.getLong(_columnIndexOfTopicId);
          _item.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
          _item.date = (int) (_stmt.getLong(_columnIndexOfDate));
          _item.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
          _item.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
          _item.views = (int) (_stmt.getLong(_columnIndexOfViews));
          _item.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
          _item.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
          _item.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
          if (_stmt.isNull(_columnIndexOfFwdName)) {
            _item.fwdName = null;
          } else {
            _item.fwdName = _stmt.getText(_columnIndexOfFwdName);
          }
          _item.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
          if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
            _item.fwdPostAuthor = null;
          } else {
            _item.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
          }
          _item.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
          _item.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
          _item.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
          _item.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
          _item.replyForumTopic = _tmp != 0;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
          _item.replyQuote = _tmp_1 != 0;
          if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
            _item.replyQuoteText = null;
          } else {
            _item.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
          }
          if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
            _item.replyQuoteEntities = null;
          } else {
            _item.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
          }
          if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
            _item.replyFromSerialized = null;
          } else {
            _item.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
          }
          _item.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
          if (_stmt.isNull(_columnIndexOfText)) {
            _item.text = null;
          } else {
            _item.text = _stmt.getText(_columnIndexOfText);
          }
          if (_stmt.isNull(_columnIndexOfTextEntities)) {
            _item.textEntities = null;
          } else {
            _item.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
          }
          if (_stmt.isNull(_columnIndexOfMediaPath)) {
            _item.mediaPath = null;
          } else {
            _item.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
          }
          if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
            _item.hqThumbPath = null;
          } else {
            _item.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
          }
          _item.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
          if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
            _item.documentSerialized = null;
          } else {
            _item.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
          }
          if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
            _item.thumbsSerialized = null;
          } else {
            _item.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
          }
          if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
            _item.documentAttributesSerialized = null;
          } else {
            _item.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
          }
          if (_stmt.isNull(_columnIndexOfMimeType)) {
            _item.mimeType = null;
          } else {
            _item.mimeType = _stmt.getText(_columnIndexOfMimeType);
          }
          if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
            _item.replyMarkupSerialized = null;
          } else {
            _item.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
          }
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public EditedMessage getLastRevision(final long userId, final long dialogId,
      final long messageId) {
    final String _sql = "SELECT * FROM editedmessage WHERE userId = ? AND dialogId = ? AND messageId = ? ORDER BY entityCreateDate DESC LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
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
        final EditedMessage _result;
        if (_stmt.step()) {
          _result = new EditedMessage();
          _result.fakeId = _stmt.getLong(_columnIndexOfFakeId);
          _result.userId = _stmt.getLong(_columnIndexOfUserId);
          _result.dialogId = _stmt.getLong(_columnIndexOfDialogId);
          _result.groupedId = _stmt.getLong(_columnIndexOfGroupedId);
          _result.peerId = _stmt.getLong(_columnIndexOfPeerId);
          _result.fromId = _stmt.getLong(_columnIndexOfFromId);
          _result.topicId = _stmt.getLong(_columnIndexOfTopicId);
          _result.messageId = (int) (_stmt.getLong(_columnIndexOfMessageId));
          _result.date = (int) (_stmt.getLong(_columnIndexOfDate));
          _result.flags = (int) (_stmt.getLong(_columnIndexOfFlags));
          _result.editDate = (int) (_stmt.getLong(_columnIndexOfEditDate));
          _result.views = (int) (_stmt.getLong(_columnIndexOfViews));
          _result.forwards = (int) (_stmt.getLong(_columnIndexOfForwards));
          _result.fwdFlags = (int) (_stmt.getLong(_columnIndexOfFwdFlags));
          _result.fwdFromId = _stmt.getLong(_columnIndexOfFwdFromId);
          if (_stmt.isNull(_columnIndexOfFwdName)) {
            _result.fwdName = null;
          } else {
            _result.fwdName = _stmt.getText(_columnIndexOfFwdName);
          }
          _result.fwdDate = (int) (_stmt.getLong(_columnIndexOfFwdDate));
          if (_stmt.isNull(_columnIndexOfFwdPostAuthor)) {
            _result.fwdPostAuthor = null;
          } else {
            _result.fwdPostAuthor = _stmt.getText(_columnIndexOfFwdPostAuthor);
          }
          _result.replyFlags = (int) (_stmt.getLong(_columnIndexOfReplyFlags));
          _result.replyMessageId = (int) (_stmt.getLong(_columnIndexOfReplyMessageId));
          _result.replyPeerId = _stmt.getLong(_columnIndexOfReplyPeerId);
          _result.replyTopId = (int) (_stmt.getLong(_columnIndexOfReplyTopId));
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfReplyForumTopic));
          _result.replyForumTopic = _tmp != 0;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfReplyQuote));
          _result.replyQuote = _tmp_1 != 0;
          if (_stmt.isNull(_columnIndexOfReplyQuoteText)) {
            _result.replyQuoteText = null;
          } else {
            _result.replyQuoteText = _stmt.getText(_columnIndexOfReplyQuoteText);
          }
          if (_stmt.isNull(_columnIndexOfReplyQuoteEntities)) {
            _result.replyQuoteEntities = null;
          } else {
            _result.replyQuoteEntities = _stmt.getBlob(_columnIndexOfReplyQuoteEntities);
          }
          if (_stmt.isNull(_columnIndexOfReplyFromSerialized)) {
            _result.replyFromSerialized = null;
          } else {
            _result.replyFromSerialized = _stmt.getBlob(_columnIndexOfReplyFromSerialized);
          }
          _result.entityCreateDate = (int) (_stmt.getLong(_columnIndexOfEntityCreateDate));
          if (_stmt.isNull(_columnIndexOfText)) {
            _result.text = null;
          } else {
            _result.text = _stmt.getText(_columnIndexOfText);
          }
          if (_stmt.isNull(_columnIndexOfTextEntities)) {
            _result.textEntities = null;
          } else {
            _result.textEntities = _stmt.getBlob(_columnIndexOfTextEntities);
          }
          if (_stmt.isNull(_columnIndexOfMediaPath)) {
            _result.mediaPath = null;
          } else {
            _result.mediaPath = _stmt.getText(_columnIndexOfMediaPath);
          }
          if (_stmt.isNull(_columnIndexOfHqThumbPath)) {
            _result.hqThumbPath = null;
          } else {
            _result.hqThumbPath = _stmt.getText(_columnIndexOfHqThumbPath);
          }
          _result.documentType = (int) (_stmt.getLong(_columnIndexOfDocumentType));
          if (_stmt.isNull(_columnIndexOfDocumentSerialized)) {
            _result.documentSerialized = null;
          } else {
            _result.documentSerialized = _stmt.getBlob(_columnIndexOfDocumentSerialized);
          }
          if (_stmt.isNull(_columnIndexOfThumbsSerialized)) {
            _result.thumbsSerialized = null;
          } else {
            _result.thumbsSerialized = _stmt.getBlob(_columnIndexOfThumbsSerialized);
          }
          if (_stmt.isNull(_columnIndexOfDocumentAttributesSerialized)) {
            _result.documentAttributesSerialized = null;
          } else {
            _result.documentAttributesSerialized = _stmt.getBlob(_columnIndexOfDocumentAttributesSerialized);
          }
          if (_stmt.isNull(_columnIndexOfMimeType)) {
            _result.mimeType = null;
          } else {
            _result.mimeType = _stmt.getText(_columnIndexOfMimeType);
          }
          if (_stmt.isNull(_columnIndexOfReplyMarkupSerialized)) {
            _result.replyMarkupSerialized = null;
          } else {
            _result.replyMarkupSerialized = _stmt.getBlob(_columnIndexOfReplyMarkupSerialized);
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
  public boolean hasAnyRevisions(final long userId, final long dialogId, final long messageId) {
    final String _sql = "SELECT EXISTS(SELECT * FROM editedmessage WHERE userId = ? AND dialogId = ? AND messageId = ?)";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, userId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, messageId);
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
  public String getMediaPathByFakeId(final long fakeId) {
    final String _sql = "SELECT mediaPath FROM editedmessage WHERE fakeId = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, fakeId);
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
  public void updateAttachmentForRevisionsBetweenDates(final long userId, final long dialogId,
      final long messageId, final String oldPath, final String newPath) {
    final String _sql = "UPDATE editedmessage SET mediaPath = ? WHERE userId = ? AND dialogId = ? AND messageId = ? AND mediaPath = ?";
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
        _argIndex = 5;
        if (oldPath == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, oldPath);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete(final long dialogId) {
    final String _sql = "DELETE FROM editedmessage WHERE dialogId = ?";
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
  public void deleteByDialogIdAndMessageIds(final long dialogId, final List<Integer> messageIds) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("DELETE FROM editedmessage WHERE dialogId = ");
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
        _stmt.bindLong(_argIndex, dialogId);
        _argIndex = 2;
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
  public int deleteByFakeId(final long fakeId) {
    final String _sql = "DELETE FROM editedmessage WHERE fakeId = ?";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, fakeId);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
