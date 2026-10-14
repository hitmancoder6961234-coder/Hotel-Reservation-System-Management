package com.stayease.service;

/**
 * Raw booking request fields. Numbers arrive as doubles because the original API coerced JSON values
 * with JavaScript's Number() (see {@link com.stayease.validation.Rules#toNumber}).
 */
public record BookingRequest(
    String guestName,
    String email,
    String mobile,
    double roomId,
    String checkIn,
    String checkOut,
    double guests) {}
