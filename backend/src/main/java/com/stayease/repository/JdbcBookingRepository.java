package com.stayease.repository;

import com.stayease.model.Availability.BookedRange;
import com.stayease.model.Availability.Conflict;
import com.stayease.model.AdminBooking;
import com.stayease.model.Booking;
import com.stayease.model.BookingDetail;
import com.stayease.model.NewBooking;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation. All SQL is parameterised with {@link PreparedStatement}. */
public class JdbcBookingRepository implements BookingRepository {

  @Override
  public List<Conflict> findOverlapping(Connection connection, int roomId, LocalDate checkIn, LocalDate checkOut)
      throws SQLException {
    String sql = "SELECT bookingRef, checkIn, checkOut FROM `Booking` "
        + "WHERE roomId = ? AND status = 'Confirmed' AND checkIn < ? AND checkOut > ? "
        + "ORDER BY checkIn ASC";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      statement.setObject(2, checkOut); // existing start before requested end
      statement.setObject(3, checkIn); //  existing end after requested start
      try (ResultSet rows = statement.executeQuery()) {
        List<Conflict> conflicts = new ArrayList<>();
        while (rows.next()) {
          conflicts.add(new Conflict(
              rows.getString("bookingRef"),
              rows.getObject("checkIn", LocalDate.class),
              rows.getObject("checkOut", LocalDate.class)));
        }
        return conflicts;
      }
    }
  }

  @Override
  public List<BookedRange> findConfirmedRanges(Connection connection, int roomId) throws SQLException {
    String sql = "SELECT checkIn, checkOut FROM `Booking` WHERE roomId = ? AND status = 'Confirmed' "
        + "ORDER BY checkIn ASC";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, roomId);
      try (ResultSet rows = statement.executeQuery()) {
        List<BookedRange> ranges = new ArrayList<>();
        while (rows.next()) {
          ranges.add(new BookedRange(
              rows.getObject("checkIn", LocalDate.class), rows.getObject("checkOut", LocalDate.class)));
        }
        return ranges;
      }
    }
  }

  @Override
  public int nextSequence(Connection connection, String prefix) throws SQLException {
    // "STY-2026-007" -> 7 ; escape LIKE wildcards in the prefix just in case.
    String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(bookingRef, ?) AS UNSIGNED)), 0) FROM `Booking` "
        + "WHERE bookingRef LIKE ? ESCAPE '\\\\'";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, prefix.length() + 1);
      statement.setString(2, escapeLike(prefix) + "%");
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        return rows.getInt(1) + 1;
      }
    }
  }

  @Override
  public Booking insert(Connection connection, NewBooking b) throws SQLException {
    String insert = "INSERT INTO `Booking` (bookingRef, guestName, email, mobile, roomId, checkIn, checkOut, "
        + "guests, nights, totalPrice, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    int id;
    try (PreparedStatement statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, b.bookingRef());
      statement.setString(2, b.guestName());
      statement.setString(3, b.email());
      statement.setString(4, b.mobile());
      statement.setInt(5, b.roomId());
      statement.setObject(6, b.checkIn());
      statement.setObject(7, b.checkOut());
      statement.setInt(8, b.guests());
      statement.setInt(9, b.nights());
      statement.setInt(10, b.totalPrice());
      statement.setString(11, b.status());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        keys.next();
        id = keys.getInt(1);
      }
    }
    // Read the row back so createdAt is exactly what the database stored.
    String select = "SELECT createdAt FROM `Booking` WHERE id = ?";
    try (PreparedStatement statement = connection.prepareStatement(select)) {
      statement.setInt(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        return new Booking(
            id, b.bookingRef(), b.guestName(), b.email(), b.mobile(), b.roomId(), b.checkIn(),
            b.checkOut(), b.guests(), b.nights(), b.totalPrice(), b.status(), readInstant(rows, "createdAt"));
      }
    }
  }

  @Override
  public Optional<BookingDetail> findDetailByRef(Connection connection, String bookingRef) throws SQLException {
    String sql = "SELECT b.bookingRef, b.guestName, b.email, b.mobile, b.roomId, r.name AS roomName, "
        + "r.image AS roomImage, b.checkIn, b.checkOut, b.guests, b.nights, b.totalPrice, b.status, "
        + "b.createdAt FROM `Booking` b JOIN `Room` r ON r.id = b.roomId WHERE b.bookingRef = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, bookingRef);
      try (ResultSet rows = statement.executeQuery()) {
        if (!rows.next()) {
          return Optional.empty();
        }
        return Optional.of(new BookingDetail(
            rows.getString("bookingRef"),
            rows.getString("guestName"),
            rows.getString("email"),
            rows.getString("mobile"),
            rows.getInt("roomId"),
            rows.getString("roomName"),
            rows.getString("roomImage"),
            rows.getObject("checkIn", LocalDate.class),
            rows.getObject("checkOut", LocalDate.class),
            rows.getInt("guests"),
            rows.getInt("nights"),
            rows.getInt("totalPrice"),
            rows.getString("status"),
            readInstant(rows, "createdAt")));
      }
    }
  }

  @Override
  public List<AdminBooking> findAllForAdmin(Connection connection) throws SQLException {
    String sql = "SELECT b.id, b.bookingRef, b.guestName, b.email, b.mobile, b.roomId, r.name AS roomName, "
        + "b.checkIn, b.checkOut, b.guests, b.nights, b.totalPrice, b.status, b.createdAt "
        + "FROM `Booking` b JOIN `Room` r ON r.id = b.roomId ORDER BY b.id ASC";
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet rows = statement.executeQuery()) {
      List<AdminBooking> all = new ArrayList<>();
      while (rows.next()) {
        all.add(new AdminBooking(
            rows.getInt("id"),
            rows.getString("bookingRef"),
            rows.getString("guestName"),
            rows.getString("email"),
            rows.getString("mobile"),
            rows.getInt("roomId"),
            rows.getString("roomName"),
            rows.getObject("checkIn", LocalDate.class),
            rows.getObject("checkOut", LocalDate.class),
            rows.getInt("guests"),
            rows.getInt("nights"),
            rows.getInt("totalPrice"),
            rows.getString("status"),
            readInstant(rows, "createdAt")));
      }
      return all;
    }
  }

  /** createdAt is stored as a UTC DATETIME(3). */
  private static Instant readInstant(ResultSet rows, String column) throws SQLException {
    LocalDateTime value = rows.getObject(column, LocalDateTime.class);
    return value == null ? null : value.toInstant(ZoneOffset.UTC);
  }

  static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
