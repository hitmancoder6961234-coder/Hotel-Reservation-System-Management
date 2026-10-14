package com.stayease.repository;

import com.stayease.model.Room;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** JDBC implementation. All SQL is parameterised with {@link PreparedStatement}. */
public class JdbcRoomRepository implements RoomRepository {

  private static final String SELECT_COLUMNS =
      "SELECT id, name, description, price, capacity, facilities, features, image, available FROM `Room`";

  @Override
  public List<Room> findAll(Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(SELECT_COLUMNS + " ORDER BY id ASC");
        ResultSet rows = statement.executeQuery()) {
      List<Room> rooms = new ArrayList<>();
      while (rows.next()) {
        rooms.add(map(rows));
      }
      return rooms;
    }
  }

  @Override
  public Optional<Room> findById(Connection connection, int id) throws SQLException {
    return findOne(connection, SELECT_COLUMNS + " WHERE id = ?", id);
  }

  @Override
  public Optional<Room> findByIdForUpdate(Connection connection, int id) throws SQLException {
    return findOne(connection, SELECT_COLUMNS + " WHERE id = ? FOR UPDATE", id);
  }

  @Override
  public List<Room> lockAllForUpdate(Connection connection) throws SQLException {
    try (PreparedStatement statement =
            connection.prepareStatement(SELECT_COLUMNS + " ORDER BY id ASC FOR UPDATE");
        ResultSet rows = statement.executeQuery()) {
      List<Room> rooms = new ArrayList<>();
      while (rows.next()) {
        rooms.add(map(rows));
      }
      return rooms;
    }
  }

  private Optional<Room> findOne(Connection connection, String sql, int id) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(map(rows)) : Optional.empty();
      }
    }
  }

  private Room map(ResultSet rows) throws SQLException {
    return new Room(
        rows.getInt("id"),
        rows.getString("name"),
        rows.getString("description"),
        rows.getInt("price"),
        rows.getInt("capacity"),
        splitCsv(rows.getString("facilities")),
        splitCsv(rows.getString("features")),
        rows.getString("image"),
        rows.getBoolean("available"));
  }

  /** facilities/features are stored as comma-separated text (unchanged from the original schema). */
  static List<String> splitCsv(String csv) {
    if (csv == null) {
      return List.of();
    }
    return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }
}
