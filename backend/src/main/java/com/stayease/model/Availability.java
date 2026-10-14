package com.stayease.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of GET /api/rooms/:id/availability.
 * Either the room is administratively unavailable ({@code available=false} + {@code reason})
 * or the date-range check result is returned (conflicts + all booked ranges).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "roomId", "checkIn", "checkOut", "available", "reason", "conflicts", "bookedRanges"
})
public record Availability(
    Integer roomId,
    LocalDate checkIn,
    LocalDate checkOut,
    boolean available,
    String reason,
    List<Conflict> conflicts,
    List<BookedRange> bookedRanges) {

  public static Availability roomUnavailable(String reason) {
    return new Availability(null, null, null, false, reason, null, null);
  }

  public static Availability forRange(
      int roomId,
      LocalDate checkIn,
      LocalDate checkOut,
      List<Conflict> conflicts,
      List<BookedRange> bookedRanges) {
    return new Availability(roomId, checkIn, checkOut, conflicts.isEmpty(), null, conflicts, bookedRanges);
  }

  /** An existing confirmed booking that overlaps the requested range. */
  @JsonPropertyOrder({"bookingRef", "checkIn", "checkOut"})
  public record Conflict(String bookingRef, LocalDate checkIn, LocalDate checkOut) {}

  /** A confirmed booked date range of the room. */
  @JsonPropertyOrder({"checkIn", "checkOut"})
  public record BookedRange(LocalDate checkIn, LocalDate checkOut) {}
}
