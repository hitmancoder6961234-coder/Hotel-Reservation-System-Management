package com.stayease.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stayease.db.DataSourceDb;
import com.stayease.db.Db;
import com.stayease.model.BookingResponse;
import com.stayease.service.ApiException;
import com.stayease.service.BookingRequest;
import com.stayease.service.BookingService;
import com.stayease.service.RoomService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Integration tests against a REAL MySQL (schema from database/init/01-schema.sql and the seeded rooms).
 * Skipped unless TEST_DB_URL is set, e.g.
 *   TEST_DB_URL=jdbc:mysql://127.0.0.1:3307/stayease?connectionTimeZone=UTC&amp;forceConnectionTimeZoneToSession=true
 *   TEST_DB_USER=stayease_app TEST_DB_PASSWORD=...
 * The tests DELETE all rows of the Booking table: point them at a throw-away database only.
 */
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class MySqlIntegrationTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T08:00:00Z"), ZoneId.of("Asia/Kolkata"));

  private static HikariDataSource dataSource;
  private static Db db;
  private static BookingService bookings;
  private static RoomService rooms;

  @BeforeAll
  static void connect() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(System.getenv("TEST_DB_URL"));
    config.setUsername(System.getenv("TEST_DB_USER"));
    config.setPassword(System.getenv("TEST_DB_PASSWORD"));
    config.setMaximumPoolSize(16);
    dataSource = new HikariDataSource(config);
    db = new DataSourceDb(dataSource);
    JdbcRoomRepository roomRepo = new JdbcRoomRepository();
    JdbcBookingRepository bookingRepo = new JdbcBookingRepository();
    bookings = new BookingService(db, roomRepo, bookingRepo, CLOCK);
    rooms = new RoomService(db, roomRepo, bookingRepo, CLOCK);
  }

  @AfterAll
  static void close() {
    dataSource.close();
  }

  @BeforeEach
  void cleanBookings() throws SQLException {
    // Needs DELETE privilege (the application user has it).
    try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
      s.executeUpdate("DELETE FROM `Booking`");
    }
  }

  private static BookingRequest request(int room, String in, String out, int guests, String name) {
    return new BookingRequest(name, "guest@example.com", "+91 98200 12345", room, in, out, guests);
  }

  @Test
  void seededRoomsAreReadWithListsSplitFromCsv() throws SQLException {
    var all = rooms.listRooms();
    assertEquals(3, all.size());
    assertEquals(101, all.get(0).id());
    assertEquals(List.of("AC", "WiFi", "TV"), all.get(0).facilities());
    assertEquals(4000, all.get(0).price());
    assertTrue(all.get(0).available());
    assertEquals("Suite Room", rooms.getRoom(103).name());
  }

  @Test
  void createsAndReadsBackABooking() throws SQLException {
    BookingResponse created = bookings.create(request(102, "2026-09-20", "2026-09-23", 2, "Priya Mehta"));
    assertEquals("STY-2026-001", created.booking().bookingRef());
    assertEquals(21_000, created.booking().totalPrice());
    assertEquals(LocalDate.of(2026, 9, 20), created.booking().checkIn());
    assertTrue(created.booking().createdAt() != null);

    var detail = bookings.getBooking("STY-2026-001");
    assertEquals("Priya Mehta", detail.guestName());
    assertEquals("Luxury Room", detail.roomName());
    assertEquals(LocalDate.of(2026, 9, 23), detail.checkOut());
    assertEquals("Confirmed", detail.status());
    // createdAt is "now" (UTC): within a minute of the system clock
    assertTrue(Math.abs(detail.createdAt().getEpochSecond() - Instant.now().getEpochSecond()) < 60);
  }

  @Test
  void unicodeGuestNamesSurvive() throws SQLException {
    bookings.create(request(101, "2026-09-20", "2026-09-21", 1, "Zoë Müller — 李雷"));
    assertEquals("Zoë Müller — 李雷", bookings.getBooking("STY-2026-001").guestName());
  }

  @Test
  void overlapRuleMatchesTheOriginalAndAdjacentDatesAreAllowed() throws SQLException {
    bookings.create(request(101, "2026-09-20", "2026-09-23", 2, "First Guest"));
    ApiException overlap = assertThrows(ApiException.class,
        () -> bookings.create(request(101, "2026-09-22", "2026-09-24", 2, "Second Guest")));
    assertEquals(409, overlap.status());
    assertEquals("This room is not available for the selected dates.", overlap.getMessage());

    bookings.create(request(101, "2026-09-23", "2026-09-25", 2, "Third Guest")); // starts on check-out day
    bookings.create(request(101, "2026-09-18", "2026-09-20", 2, "Fourth Guest")); // ends on check-in day

    var availability = rooms.checkAvailability(101, "2026-09-21", "2026-09-22");
    assertFalse(availability.available());
    assertEquals("STY-2026-001", availability.conflicts().get(0).bookingRef());
    assertEquals(3, availability.bookedRanges().size());
    assertEquals(LocalDate.of(2026, 9, 18), availability.bookedRanges().get(0).checkIn()); // ordered by check-in
  }

  @Test
  void concurrentRequestsForTheSameRoomAndDatesCreateExactlyOneBooking() throws Exception {
    int threads = 12;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<String>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      int n = i;
      Callable<String> task = () -> {
        start.await();
        try {
          return bookings.create(request(103, "2026-10-01", "2026-10-05", 2, "Racer " + n)).booking().bookingRef();
        } catch (ApiException e) {
          return "HTTP " + e.status();
        }
      };
      results.add(pool.submit(task));
    }
    start.countDown();
    int created = 0;
    int conflicts = 0;
    for (Future<String> f : results) {
      String r = f.get();
      if (r.startsWith("STY-")) created++;
      else if (r.equals("HTTP 409")) conflicts++;
    }
    pool.shutdown();
    assertEquals(1, created, "exactly one booking may win the room");
    assertEquals(threads - 1, conflicts);
    try (Connection c = dataSource.getConnection(); var s = c.createStatement();
        var rs = s.executeQuery("SELECT COUNT(*) FROM `Booking` WHERE roomId = 103")) {
      rs.next();
      assertEquals(1, rs.getInt(1));
    }
  }

  @Test
  void concurrentBookingsOfDifferentRoomsGetUniqueReferences() throws Exception {
    int threads = 12;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<String>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      int n = i;
      // different (room, dates) pairs: no business conflict, but all want the next STY-2026-NNN number
      int room = 101 + (n % 3);
      String in = LocalDate.of(2026, 11, 1).plusDays(n * 3L).toString();
      String out = LocalDate.of(2026, 11, 1).plusDays(n * 3L + 2).toString();
      results.add(pool.submit(() -> {
        start.await();
        return bookings.create(request(room, in, out, 1, "Guest " + n)).booking().bookingRef();
      }));
    }
    start.countDown();
    Set<String> refs = new HashSet<>();
    for (Future<String> f : results) {
      refs.add(f.get());
    }
    pool.shutdown();
    assertEquals(threads, refs.size(), "every booking needs its own reference: " + refs);
    assertTrue(refs.contains("STY-2026-001"));
    assertTrue(refs.contains("STY-2026-012"));
  }

  @Test
  void databaseConstraintsAreASecondLineOfDefence() throws SQLException {
    try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
      SQLException badDates = assertThrows(SQLException.class, () -> s.executeUpdate(
          "INSERT INTO `Booking` (bookingRef, guestName, email, mobile, roomId, checkIn, checkOut, guests, nights, "
              + "totalPrice) VALUES ('X-1','A','a@b.co','9876543210',101,'2026-09-20','2026-09-20',1,1,4000)"));
      assertTrue(badDates.getMessage().toLowerCase().contains("check"));
      SQLException badRoom = assertThrows(SQLException.class, () -> s.executeUpdate(
          "INSERT INTO `Booking` (bookingRef, guestName, email, mobile, roomId, checkIn, checkOut, guests, nights, "
              + "totalPrice) VALUES ('X-2','A','a@b.co','9876543210',999,'2026-09-20','2026-09-21',1,1,4000)"));
      assertTrue(badRoom.getMessage().toLowerCase().contains("foreign key"));
    }
  }

  @Test
  void aFailedTransactionLeavesNothingBehind() throws SQLException {
    assertThrows(IllegalStateException.class, () -> db.transaction(c -> {
      new JdbcBookingRepository().insert(c, new com.stayease.model.NewBooking("STY-2026-777", "Temp Guest",
          "t@x.co", "9876543210", 101, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2), 1, 1, 4000, "Confirmed"));
      throw new IllegalStateException("boom");
    }));
    assertThrows(ApiException.class, () -> bookings.getBooking("STY-2026-777"));
  }
}
