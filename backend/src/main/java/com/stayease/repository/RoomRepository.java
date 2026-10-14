package com.stayease.repository;

import com.stayease.model.Room;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for rooms. Every method runs on the connection supplied by the caller. */
public interface RoomRepository {

  List<Room> findAll(Connection connection) throws SQLException;

  Optional<Room> findById(Connection connection, int id) throws SQLException;

  /**
   * Reads the room row and locks it until the surrounding transaction ends. Used to serialise
   * concurrent bookings of the same room so that overlapping reservations cannot both succeed.
   */
  Optional<Room> findByIdForUpdate(Connection connection, int id) throws SQLException;

  /**
   * Locks every room row (always in id order, so transactions cannot deadlock) until the surrounding
   * transaction ends. Booking creation takes this lock: bookings are then created one at a time, which
   * makes both the overlap check and the next STY-YYYY-NNN reference race-free. A hotel has few rooms and
   * a booking takes milliseconds, so the serialisation is not a bottleneck.
   */
  List<Room> lockAllForUpdate(Connection connection) throws SQLException;
}
