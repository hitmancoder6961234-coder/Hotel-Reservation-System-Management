package com.stayease.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.time.LocalDate;

/** One row of the admin dashboard / Excel export: a booking with its room name. */
@JsonPropertyOrder({
  "id", "bookingRef", "guestName", "email", "mobile", "roomId", "roomName", "checkIn", "checkOut",
  "guests", "nights", "totalPrice", "status", "createdAt"
})
public record AdminBooking(
    int id,
    String bookingRef,
    String guestName,
    String email,
    String mobile,
    int roomId,
    String roomName,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    int nights,
    int totalPrice,
    String status,
    Instant createdAt) {}
