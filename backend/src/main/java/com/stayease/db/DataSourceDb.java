package com.stayease.db;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/** {@link Db} backed by a (pooled) {@link DataSource}. Connections are always closed. */
public class DataSourceDb implements Db {

  private final DataSource dataSource;

  public DataSourceDb(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public <T> T read(SqlFunction<T> work) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      return work.apply(connection);
    }
  }

  @Override
  public <T> T transaction(SqlFunction<T> work) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      boolean previousAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        T result = work.apply(connection);
        connection.commit();
        return result;
      } catch (SQLException | RuntimeException e) {
        try {
          connection.rollback();
        } catch (SQLException rollbackFailure) {
          e.addSuppressed(rollbackFailure);
        }
        throw e;
      } finally {
        connection.setAutoCommit(previousAutoCommit);
      }
    }
  }
}
