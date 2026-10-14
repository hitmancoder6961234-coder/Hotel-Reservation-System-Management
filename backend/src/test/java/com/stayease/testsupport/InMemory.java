package com.stayease.testsupport;

import com.stayease.db.Db;
import com.stayease.db.SqlFunction;
import com.stayease.model.Availability.BookedRange;
import com.stayease.model.Availability.Conflict;
import com.stayease.model.Booking;
import com.stayease.model.BookingDetail;
import com.stayease.model.NewBooking;
import com.stayease.model.Room;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.RoomRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** In-memory stand-ins for the database layer, used by the fast unit tests. */
public final class InMemory {

  private InMemory() {}

  public static List<Room> seedRooms() {
    return new ArrayList<>(List.of(
        new Room(101, "Deluxe Room", "d", 4000, 2, List.of("AC", "WiFi", "TV"), List.of("Queen-size bed"),
            "/images/deluxe.jpg", true),
        new Room(102, "Luxury Room", "d", 7000, 3, List.of("AC", "WiFi", "Breakfast", "Room Service"),
            List.of("King-size bed"), "/images/luxury.jpg", true),
        new Room(103, "Suite Room", "d", 10000, 4, List.of("Balcony"), List.of("Private balcony"),
            "/images/suite.jpg", true)));
  }

  /** A "database" with no real connection: the connection argument is always null. */
  public static class FakeDb implements Db {
    public int transactions;

    @Override
    public <T> T read(SqlFunction<T> work) throws SQLException {
      return work.apply((Connection) null);
    }

    @Override
    public synchronized <T> T transaction(SqlFunction<T> work) throws SQLException {
      transactions++;
      return work.apply((Connection) null);
    }
  }

  public static class Rooms implements RoomRepository {
    public final List<Room> rooms;

    public Rooms(List<Room> rooms) {
      this.rooms = rooms;
    }

    @Override
    public List<Room> findAll(Connection c) {
      return rooms.stream().sorted(Comparator.comparingInt(Room::id)).toList();
    }

    @Override
    public Optional<Room> findById(Connection c, int id) {
      return rooms.stream().filter(r -> r.id() == id).findFirst();
    }

    @Override
    public Optional<Room> findByIdForUpdate(Connection c, int id) {
      return findById(c, id);
    }

    @Override
    public List<Room> lockAllForUpdate(Connection c) {
      return findAll(c);
    }
  }

  public static class Bookings implements BookingRepository {
    public final List<Booking> stored = new ArrayList<>();
    private final Rooms rooms;

    public Bookings(Rooms rooms) {
      this.rooms = rooms;
    }

    @Override
    public List<Conflict> findOverlapping(Connection c, int roomId, LocalDate in, LocalDate out) {
      return stored.stream()
          .filter(b -> b.roomId() == roomId && b.status().equals("Confirmed"))
          .filter(b -> b.checkIn().isBefore(out) && b.checkOut().isAfter(in))
          .sorted(Comparator.comparing(Booking::checkIn))
          .map(b -> new Conflict(b.bookingRef(), b.checkIn(), b.checkOut()))
          .toList();
    }

    @Override
    public List<BookedRange> findConfirmedRanges(Connection c, int roomId) {
      return stored.stream()
          .filter(b -> b.roomId() == roomId && b.status().equals("Confirmed"))
          .sorted(Comparator.comparing(Booking::checkIn))
          .map(b -> new BookedRange(b.checkIn(), b.checkOut()))
          .toList();
    }

    @Override
    public int nextSequence(Connection c, String prefix) {
      return stored.stream()
              .map(Booking::bookingRef)
              .filter(r -> r.startsWith(prefix))
              .mapToInt(r -> Integer.parseInt(r.substring(prefix.length())))
              .max()
              .orElse(0)
          + 1;
    }

    @Override
    public Booking insert(Connection c, NewBooking b) {
      Booking saved = new Booking(stored.size() + 1, b.bookingRef(), b.guestName(), b.email(), b.mobile(),
          b.roomId(), b.checkIn(), b.checkOut(), b.guests(), b.nights(), b.totalPrice(), b.status(), Instant.now());
      stored.add(saved);
      return saved;
    }

    @Override
    public Optional<BookingDetail> findDetailByRef(Connection c, String ref) {
      return stored.stream().filter(b -> b.bookingRef().equals(ref)).findFirst().map(b -> {
        Room room = rooms.findById(c, b.roomId()).orElseThrow();
        return new BookingDetail(b.bookingRef(), b.guestName(), b.email(), b.mobile(), b.roomId(), room.name(),
            room.image(), b.checkIn(), b.checkOut(), b.guests(), b.nights(), b.totalPrice(), b.status(), b.createdAt());
      });
    }
  }
}
