package com.stayease.service;

import com.stayease.db.Db;
import com.stayease.model.Availability;
import com.stayease.model.Room;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.RoomRepository;
import com.stayease.validation.Dates;
import com.stayease.validation.Rules;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Room listing, room details and availability checks. */
public class RoomService {

  private final Db db;
  private final RoomRepository rooms;
  private final BookingRepository bookings;
  private final Clock clock;

  public RoomService(Db db, RoomRepository rooms, BookingRepository bookings, Clock clock) {
    this.db = db;
    this.rooms = rooms;
    this.bookings = bookings;
    this.clock = clock;
  }

  public List<Room> listRooms() throws SQLException {
    return db.read(rooms::findAll);
  }

  public Room getRoom(double rawId) throws ApiException, SQLException {
    int id = requireRoomId(rawId);
    return db.read(c -> rooms.findById(c, id)).orElseThrow(() -> new ApiException(404, "Room not found."));
  }

  public Availability checkAvailability(double rawId, String checkIn, String checkOut)
      throws ApiException, SQLException {
    int id = requireRoomId(rawId);

    // Dates are validated before the room is looked up (same order as the original API).
    String dateError = Dates.validateDates(checkIn, checkOut, today());
    if (dateError != null) {
      throw new ApiException(400, dateError);
    }

    Availability result = db.read(c -> {
      Room room = rooms.findById(c, id).orElse(null);
      if (room == null) {
        return null;
      }
      if (!room.available()) {
        return Availability.roomUnavailable("This room is currently unavailable for booking.");
      }
      LocalDate in = Dates.parse(checkIn).orElseThrow();
      LocalDate out = Dates.parse(checkOut).orElseThrow();
      return Availability.forRange(
          id, in, out, bookings.findOverlapping(c, id, in, out), bookings.findConfirmedRanges(c, id));
    });
    if (result == null) {
      throw new ApiException(404, "Room not found.");
    }
    return result;
  }

  private static int requireRoomId(double raw) throws ApiException {
    if (!Rules.isInteger(raw) || raw > Integer.MAX_VALUE || raw < Integer.MIN_VALUE) {
      throw new ApiException(400, "Invalid room id.");
    }
    return (int) raw;
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }
}
