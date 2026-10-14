package com.stayease.db;

import java.sql.SQLException;

/** Gives the service layer a connection (read) or an all-or-nothing unit of work (transaction). */
public interface Db {

  /** Runs a read-only unit of work on a pooled connection. */
  <T> T read(SqlFunction<T> work) throws SQLException;

  /**
   * Runs a unit of work in one database transaction: committed when the work returns,
   * rolled back when it throws.
   */
  <T> T transaction(SqlFunction<T> work) throws SQLException;
}
