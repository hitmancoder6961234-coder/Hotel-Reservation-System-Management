package com.stayease.repository;

import com.stayease.model.Availability.BookedRange;
import com.stayease.model.Availability.Conflict;
import com.stayease.model.Booking;
import com.stayease.model.BookingDetail;
import com.stayease.model.NewBooking;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access for bookings. Every method runs on the connection supplied by the caller. */
public interface BookingRepository {

  /**
   * Confirmed bookings of the room that overlap [checkIn, checkOut).
   * Overlap rule (unchanged): existing.checkIn &lt; requested.checkOut AND existing.checkOut &gt; requested.checkIn.
   */
  List<Conflict> findOverlapping(Connection connection, int roomId, LocalDate checkIn, LocalDate checkOut)
      throws SQLException;

  /** Every confirmed booked range of the room, ordered by check-in. */
  List<BookedRange> findConfirmedRanges(Connection connection, int roomId) throws SQLException;

  /** The next free sequence number for a reference prefix such as "STY-2026-" (1 when none exist). */
  int nextSequence(Connection connection, String prefix) throws SQLException;

  Booking insert(Connection connection, NewBooking booking) throws SQLException;

  Optional<BookingDetail> findDetailByRef(Connection connection, String bookingRef) throws SQLException;
}
