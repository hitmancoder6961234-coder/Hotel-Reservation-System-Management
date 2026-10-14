package com.stayease.service;

import com.stayease.db.Db;
import com.stayease.model.BookingDetail;
import com.stayease.model.BookingResponse;
import com.stayease.model.NewBooking;
import com.stayease.model.Room;
import com.stayease.model.RoomSummary;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.RoomRepository;
import com.stayease.validation.Dates;
import com.stayease.validation.Rules;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and reads bookings. Server-side responsibilities (unchanged from the original):
 * validate the request, check availability (no double-booking), calculate nights and total on the
 * server, persist the booking and return the confirmation.
 */
public class BookingService {

  private static final Logger log = LoggerFactory.getLogger(BookingService.class);
  private static final int MYSQL_DUPLICATE_ENTRY = 1062;
  private static final int MAX_REFERENCE_ATTEMPTS = 5;

  private final Db db;
  private final RoomRepository rooms;
  private final BookingRepository bookings;
  private final Clock clock;

  public BookingService(Db db, RoomRepository rooms, BookingRepository bookings, Clock clock) {
    this.db = db;
    this.rooms = rooms;
    this.bookings = bookings;
    this.clock = clock;
  }

  public BookingResponse create(BookingRequest request) throws SQLException {
    // ---------- Guest validation ----------
    String guestName = trim(request.guestName());
    String email = trim(request.email());
    String mobile = trim(request.mobile());
    if (guestName.length() < 2) {
      throw new ApiException(400, "Please enter your name.");
    }
    if (guestName.length() > Rules.MAX_NAME_LENGTH) {
      throw new ApiException(400, "Name must be at most " + Rules.MAX_NAME_LENGTH + " characters.");
    }
    if (email.length() > Rules.MAX_EMAIL_LENGTH || !Rules.EMAIL.matcher(email).matches()) {
      throw new ApiException(400, "Please enter a valid email.");
    }
    if (!Rules.MOBILE.matcher(mobile).matches()) {
      throw new ApiException(400, "Please enter a valid mobile number.");
    }

    // ---------- Room validation ----------
    if (!Rules.isInteger(request.roomId()) || Math.abs(request.roomId()) > Integer.MAX_VALUE) {
      throw new ApiException(400, "Please select a room.");
    }
    int roomId = (int) request.roomId();
    Room room = db.read(c -> rooms.findById(c, roomId)).orElseThrow(() -> new ApiException(404, "Room not found."));
    if (!room.available()) {
      throw new ApiException(409, "This room is not available for booking.");
    }

    // ---------- Date validation ----------
    String checkInText = trim(request.checkIn());
    String checkOutText = trim(request.checkOut());
    if (checkInText.isEmpty()) {
      throw new ApiException(400, "Please select check-in date.");
    }
    if (checkOutText.isEmpty()) {
      throw new ApiException(400, "Please select check-out date.");
    }
    String dateError = Dates.validateDates(checkInText, checkOutText, LocalDate.now(clock));
    if (dateError != null) {
      throw new ApiException(400, dateError);
    }
    LocalDate checkIn = Dates.parse(checkInText).orElseThrow();
    LocalDate checkOut = Dates.parse(checkOutText).orElseThrow();

    if (!Rules.isInteger(request.guests()) || request.guests() < 1) {
      throw new ApiException(400, "Please select number of guests.");
    }
    if (request.guests() > room.capacity()) {
      throw new ApiException(400, "Number of guests exceeds room capacity (max " + room.capacity() + ").");
    }
    int guests = (int) request.guests();

    // ---------- Booking calculation (server side) ----------
    long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
    long total = room.price() * nights;
    if (total > Integer.MAX_VALUE) {
      throw new ApiException(400, "Please select valid dates.");
    }

    String prefix = "STY-" + LocalDate.now(clock).getYear() + "-";
    for (int attempt = 1; ; attempt++) {
      try {
        return db.transaction(c -> {
          // Lock the room rows (in id order): bookings are processed one at a time, so neither the
          // overlap check below nor the next reference number can be raced by another request.
          Room locked = rooms.lockAllForUpdate(c).stream()
              .filter(r -> r.id() == roomId)
              .findFirst()
              .orElseThrow(() -> new ApiException(404, "Room not found."));
          if (!locked.available()) {
            throw new ApiException(409, "This room is not available for booking.");
          }
          if (!bookings.findOverlapping(c, roomId, checkIn, checkOut).isEmpty()) {
            throw new ApiException(409, "This room is not available for the selected dates.");
          }
          String bookingRef = prefix + String.format("%03d", bookings.nextSequence(c, prefix));
          var saved = bookings.insert(c, new NewBooking(
              bookingRef, guestName, email, mobile, roomId, checkIn, checkOut, guests, (int) nights,
              (int) total, "Confirmed"));
          return new BookingResponse(
              saved, new RoomSummary(locked.id(), locked.name(), locked.price(), locked.capacity(), locked.image()));
        });
      } catch (SQLException e) {
        // Safety net: if a reference number is still taken (e.g. by an external writer), retry.
        if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY && attempt < MAX_REFERENCE_ATTEMPTS) {
          log.warn("Booking reference collision (attempt {}), retrying", attempt);
          continue;
        }
        throw e;
      }
    }
  }

  public BookingDetail getBooking(String reference) throws SQLException {
    String bookingRef = trim(reference);
    return db.read(c -> bookings.findDetailByRef(c, bookingRef))
        .orElseThrow(() -> new ApiException(404, "Booking not found."));
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
