package com.stayease.validation;

import java.util.regex.Pattern;

/** Field rules, identical to the original validation (plus length limits that match the schema). */
public final class Rules {

  public static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  public static final Pattern MOBILE = Pattern.compile("^[+]?[\\d\\s-]{10,15}$");

  public static final int MAX_NAME_LENGTH = 100;
  public static final int MAX_EMAIL_LENGTH = 254;

  private Rules() {}

  /**
   * Emulates JavaScript's {@code Number(value)} for the JSON values the original API accepted:
   * missing -> NaN, null/"" -> 0, numeric text -> its value, anything else -> NaN.
   */
  public static double toNumber(Object raw, boolean present) {
    if (!present) {
      return Double.NaN;
    }
    if (raw == null) {
      return 0;
    }
    if (raw instanceof Number n) {
      return n.doubleValue();
    }
    if (raw instanceof Boolean b) {
      return b ? 1 : 0;
    }
    String text = raw.toString().trim();
    if (text.isEmpty()) {
      return 0;
    }
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }

  /** {@code Number.isInteger} */
  public static boolean isInteger(double value) {
    return !Double.isNaN(value) && !Double.isInfinite(value) && value == Math.floor(value);
  }
}
