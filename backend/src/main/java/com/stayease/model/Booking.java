package com.stayease.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.time.LocalDate;

/** A stored booking (field order matches the original Prisma/Next.js response). */
@JsonPropertyOrder({
  "id", "bookingRef", "guestName", "email", "mobile", "roomId", "checkIn", "checkOut",
  "guests", "nights", "totalPrice", "status", "createdAt"
})
public record Booking(
    int id,
    String bookingRef,
    String guestName,
    String email,
    String mobile,
    int roomId,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    int nights,
    int totalPrice,
    String status,
    Instant createdAt) {}
