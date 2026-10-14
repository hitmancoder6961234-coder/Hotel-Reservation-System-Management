package com.stayease.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.time.LocalDate;

/** Response of GET /api/bookings/:ref (booking flattened together with its room name/image). */
@JsonPropertyOrder({
  "bookingRef", "guestName", "email", "mobile", "roomId", "roomName", "roomImage", "checkIn",
  "checkOut", "guests", "nights", "totalPrice", "status", "createdAt"
})
public record BookingDetail(
    String bookingRef,
    String guestName,
    String email,
    String mobile,
    int roomId,
    String roomName,
    String roomImage,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    int nights,
    int totalPrice,
    String status,
    Instant createdAt) {}
