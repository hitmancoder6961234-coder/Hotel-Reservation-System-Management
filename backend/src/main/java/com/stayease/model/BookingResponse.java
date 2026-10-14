package com.stayease.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** The "booking" object returned by POST /api/bookings: all booking fields plus a "room" summary. */
public record BookingResponse(@JsonUnwrapped Booking booking, RoomSummary room) {}
