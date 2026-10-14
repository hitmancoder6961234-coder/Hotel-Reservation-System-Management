package com.stayease.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stayease.model.Availability;
import com.stayease.model.BookingResponse;
import com.stayease.model.Room;
import com.stayease.testsupport.InMemory;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T08:00:00Z"), ZoneId.of("Asia/Kolkata"));

  private InMemory.Rooms rooms;
  private InMemory.Bookings bookings;
  private BookingService service;
  private RoomService roomService;

  @BeforeEach
  void setUp() {
    rooms = new InMemory.Rooms(InMemory.seedRooms());
    bookings = new InMemory.Bookings(rooms);
    InMemory.FakeDb db = new InMemory.FakeDb();
    service = new BookingService(db, rooms, bookings, CLOCK);
    roomService = new RoomService(db, rooms, bookings, CLOCK);
  }

  private BookingRequest request(String in, String out, double guests) {
    return new BookingRequest("Ahmed Khan", "ahmed@example.com", "+91 98765 43210", 101, in, out, guests);
  }

  private ApiException failure(BookingRequest request) {
    return assertThrows(ApiException.class, () -> service.create(request));
  }

  @Test
  void createsAConfirmedBookingWithServerSideNightsAndTotal() throws SQLException {
    BookingResponse created = service.create(request("2026-09-20", "2026-09-23", 2));

    assertEquals("STY-2026-001", created.booking().bookingRef());
    assertEquals(3, created.booking().nights());
    assertEquals(12_000, created.booking().totalPrice()); // 3 x 4000
    assertEquals("Confirmed", created.booking().status());
    assertEquals("Deluxe Room", created.room().name());
  }

  @Test
  void referencesIncreaseSequentially() throws SQLException {
    assertEquals("STY-2026-001", service.create(request("2026-09-20", "2026-09-21", 1)).booking().bookingRef());
    assertEquals("STY-2026-002", service.create(request("2026-09-22", "2026-09-23", 1)).booking().bookingRef());
  }

  @Test
  void blocksOverlappingBookingsButAllowsAdjacentDates() throws SQLException {
    service.create(request("2026-09-20", "2026-09-23", 2));

    ApiException overlap = failure(request("2026-09-22", "2026-09-25", 2));
    assertEquals(409, overlap.status());
    assertEquals("This room is not available for the selected dates.", overlap.getMessage());

    // [20,23) and [23,25) share no night: allowed. Same for [18,20).
    service.create(request("2026-09-23", "2026-09-25", 2));
    service.create(request("2026-09-18", "2026-09-20", 2));
    assertEquals(3, bookings.stored.size());
  }

  @Test
  void aDifferentRoomCanBeBookedForTheSameDates() throws SQLException {
    service.create(request("2026-09-20", "2026-09-23", 2));
    BookingRequest other = new BookingRequest("Ahmed Khan", "ahmed@example.com", "+91 98765 43210", 102,
        "2026-09-20", "2026-09-23", 2);
    assertEquals(21_000, service.create(other).booking().totalPrice());
  }

  @Test
  void validatesGuestFieldsWithTheOriginalMessages() {
    assertEquals("Please enter your name.", failure(new BookingRequest("A", "a@b.co", "9876543210", 101,
        "2026-09-20", "2026-09-21", 1)).getMessage());
    assertEquals("Please enter a valid email.", failure(new BookingRequest("Ahmed", "nope", "9876543210", 101,
        "2026-09-20", "2026-09-21", 1)).getMessage());
    assertEquals("Please enter a valid mobile number.", failure(new BookingRequest("Ahmed", "a@b.co", "123", 101,
        "2026-09-20", "2026-09-21", 1)).getMessage());
    assertEquals(400, failure(new BookingRequest("", "a@b.co", "9876543210", 101, "", "", 1)).status());
  }

  @Test
  void validatesRoomBeforeDatesLikeTheOriginal() {
    BookingRequest missingRoom = new BookingRequest("Ahmed", "a@b.co", "9876543210", Double.NaN, "", "", 0);
    assertEquals("Please select a room.", failure(missingRoom).getMessage());

    BookingRequest unknownRoom = new BookingRequest("Ahmed", "a@b.co", "9876543210", 999, "", "", 0);
    ApiException notFound = failure(unknownRoom);
    assertEquals(404, notFound.status());
    assertEquals("Room not found.", notFound.getMessage());
  }

  @Test
  void rejectsUnavailableRooms() {
    rooms.rooms.set(0, new Room(101, "Deluxe Room", "d", 4000, 2, List.of(), List.of(), "/images/deluxe.jpg", false));
    ApiException e = failure(request("2026-09-20", "2026-09-21", 1));
    assertEquals(409, e.status());
    assertEquals("This room is not available for booking.", e.getMessage());
  }

  @Test
  void validatesDatesAndGuests() {
    assertEquals("Please select check-in date.", failure(request("", "2026-09-21", 1)).getMessage());
    assertEquals("Please select check-out date.", failure(request("2026-09-20", "", 1)).getMessage());
    assertEquals("Please select valid dates.", failure(request("2026-02-30", "2026-03-02", 1)).getMessage());
    assertEquals("Check-in date cannot be in the past.", failure(request("2026-09-01", "2026-09-05", 1)).getMessage());
    assertEquals("Check-out must be after check-in.", failure(request("2026-09-20", "2026-09-20", 1)).getMessage());
    assertEquals("Please select number of guests.", failure(request("2026-09-20", "2026-09-21", 0)).getMessage());
    assertEquals("Please select number of guests.", failure(request("2026-09-20", "2026-09-21", 1.5)).getMessage());
    assertEquals("Number of guests exceeds room capacity (max 2).",
        failure(request("2026-09-20", "2026-09-21", 3)).getMessage());
  }

  @Test
  void usesTheConfiguredTimezoneForToday() throws SQLException {
    // 2026-09-10T08:00Z is already 13:30 on 10 Sep in Asia/Kolkata: booking for "today" works.
    assertEquals(1, service.create(request("2026-09-10", "2026-09-11", 1)).booking().nights());
  }

  @Test
  void returnsBookingDetailsByReference() throws SQLException {
    service.create(request("2026-09-20", "2026-09-23", 2));
    var detail = service.getBooking(" STY-2026-001 ");
    assertEquals("Deluxe Room", detail.roomName());
    assertEquals("/images/deluxe.jpg", detail.roomImage());
    assertEquals(12_000, detail.totalPrice());

    ApiException missing = assertThrows(ApiException.class, () -> service.getBooking("STY-2026-999"));
    assertEquals(404, missing.status());
    assertEquals("Booking not found.", missing.getMessage());
  }

  // ---------------------------------------------------------------- availability

  @Test
  void availabilityListsConflictsAndBookedRanges() throws SQLException {
    service.create(request("2026-09-20", "2026-09-23", 2));

    Availability free = roomService.checkAvailability(101, "2026-09-23", "2026-09-25");
    assertTrue(free.available());
    assertEquals(0, free.conflicts().size());
    assertEquals(1, free.bookedRanges().size());

    Availability busy = roomService.checkAvailability(101, "2026-09-21", "2026-09-22");
    assertEquals(false, busy.available());
    assertEquals("STY-2026-001", busy.conflicts().get(0).bookingRef());
  }

  @Test
  void availabilityValidatesInTheOriginalOrder() {
    ApiException badId = assertThrows(ApiException.class, () -> roomService.checkAvailability(Double.NaN, "", ""));
    assertEquals(400, badId.status());
    assertEquals("Invalid room id.", badId.getMessage());

    // dates are validated before the room lookup: an unknown room with bad dates reports the date error
    ApiException badDates = assertThrows(ApiException.class, () -> roomService.checkAvailability(999, "", ""));
    assertEquals("Please select check-in date.", badDates.getMessage());

    ApiException unknown = assertThrows(ApiException.class,
        () -> roomService.checkAvailability(999, "2026-09-20", "2026-09-21"));
    assertEquals(404, unknown.status());
    assertEquals("Room not found.", unknown.getMessage());
  }

  @Test
  void unavailableRoomIsReportedWithAReason() throws SQLException {
    rooms.rooms.set(0, new Room(101, "Deluxe Room", "d", 4000, 2, List.of(), List.of(), "/images/deluxe.jpg", false));
    Availability result = roomService.checkAvailability(101, "2026-09-20", "2026-09-21");
    assertEquals(false, result.available());
    assertEquals("This room is currently unavailable for booking.", result.reason());
  }
}
