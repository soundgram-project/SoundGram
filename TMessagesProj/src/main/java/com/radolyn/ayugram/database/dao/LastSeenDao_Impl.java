package com.radolyn.ayugram.database.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.radolyn.ayugram.database.entities.LastSeenEntity;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class LastSeenDao_Impl implements LastSeenDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<LastSeenEntity> __insertAdapterOfLastSeenEntity;

  public LastSeenDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfLastSeenEntity = new EntityInsertAdapter<LastSeenEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `LastSeenEntity` (`userId`,`lastSeen`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final LastSeenEntity entity) {
        statement.bindLong(1, entity.userId);
        statement.bindLong(2, entity.lastSeen);
      }
    };
  }

  @Override
  public void upsertAll(final List<LastSeenEntity> entities) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfLastSeenEntity.insert(_connection, entities);
      return null;
    });
  }

  @Override
  public List<LastSeenEntity> getAll() {
    final String _sql = "SELECT * FROM LastSeenEntity";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfUserId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "userId");
        final int _columnIndexOfLastSeen = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastSeen");
        final List<LastSeenEntity> _result = new ArrayList<LastSeenEntity>();
        while (_stmt.step()) {
          final LastSeenEntity _item;
          _item = new LastSeenEntity();
          _item.userId = _stmt.getLong(_columnIndexOfUserId);
          _item.lastSeen = (int) (_stmt.getLong(_columnIndexOfLastSeen));
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void deleteOlderThan(final int cutoff) {
    final String _sql = "DELETE FROM LastSeenEntity WHERE lastSeen < ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoff);
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
}
