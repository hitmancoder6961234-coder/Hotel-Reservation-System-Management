package com.stayease.validation;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;

/** Date parsing/validation with the exact messages of the original application. */
public final class Dates {

  private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

  private Dates() {}

  /** Parses a strict YYYY-MM-DD string (rejects impossible dates such as 2026-02-30). */
  public static Optional<LocalDate> parse(String value) {
    if (value == null || !ISO_DATE.matcher(value).matches()) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.of(
          Integer.parseInt(value.substring(0, 4)),
          Integer.parseInt(value.substring(5, 7)),
          Integer.parseInt(value.substring(8, 10))));
    } catch (DateTimeException e) {
      return Optional.empty();
    }
  }

  /**
   * Same rules and messages as the original validateDates(): empty dates, invalid dates,
   * check-in in the past, check-out not after check-in. Returns the error message or null.
   */
  public static String validateDates(String checkIn, String checkOut, LocalDate today) {
    if (checkIn == null || checkIn.isEmpty()) {
      return "Please select check-in date.";
    }
    if (checkOut == null || checkOut.isEmpty()) {
      return "Please select check-out date.";
    }
    Optional<LocalDate> in = parse(checkIn);
    Optional<LocalDate> out = parse(checkOut);
    if (in.isEmpty() || out.isEmpty()) {
      return "Please select valid dates.";
    }
    if (in.get().isBefore(today)) {
      return "Check-in date cannot be in the past.";
    }
    if (!out.get().isAfter(in.get())) {
      return "Check-out must be after check-in.";
    }
    return null;
  }
}
