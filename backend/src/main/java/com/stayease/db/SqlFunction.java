package com.stayease.db;

import java.sql.Connection;
import java.sql.SQLException;

/** A unit of work that runs against one JDBC connection. */
@FunctionalInterface
public interface SqlFunction<T> {
  T apply(Connection connection) throws SQLException;
}
