package com.stayease.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ValidationTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 10);

  @Test
  void parsesOnlyRealIsoDates() {
    assertEquals(LocalDate.of(2026, 9, 20), Dates.parse("2026-09-20").orElseThrow());
    assertTrue(Dates.parse("2026-02-30").isEmpty());
    assertTrue(Dates.parse("20-09-2026").isEmpty());
    assertTrue(Dates.parse("2026-9-2").isEmpty());
    assertTrue(Dates.parse("").isEmpty());
    assertTrue(Dates.parse(null).isEmpty());
  }

  @Test
  void dateMessagesMatchTheOriginalApplication() {
    assertEquals("Please select check-in date.", Dates.validateDates("", "2026-09-20", TODAY));
    assertEquals("Please select check-out date.", Dates.validateDates("2026-09-20", "", TODAY));
    assertEquals("Please select valid dates.", Dates.validateDates("2026-02-30", "2026-03-01", TODAY));
    assertEquals("Check-in date cannot be in the past.", Dates.validateDates("2026-09-09", "2026-09-20", TODAY));
    assertEquals("Check-out must be after check-in.", Dates.validateDates("2026-09-20", "2026-09-20", TODAY));
    assertEquals("Check-out must be after check-in.", Dates.validateDates("2026-09-20", "2026-09-19", TODAY));
    assertNull(Dates.validateDates("2026-09-10", "2026-09-11", TODAY)); // today is allowed
  }

  @Test
  void emailAndMobileRulesAreUnchanged() {
    assertTrue(Rules.EMAIL.matcher("ahmed@example.com").matches());
    assertFalse(Rules.EMAIL.matcher("ahmed@example").matches());
    assertFalse(Rules.EMAIL.matcher("a b@example.com").matches());
    assertTrue(Rules.MOBILE.matcher("+91 98765 43210").matches());
    assertTrue(Rules.MOBILE.matcher("9876543210").matches());
    assertFalse(Rules.MOBILE.matcher("12345").matches());
    assertFalse(Rules.MOBILE.matcher("98765abcde1").matches());
  }

  @Test
  void numberCoercionEmulatesJavaScriptNumber() {
    assertTrue(Double.isNaN(Rules.toNumber(null, false))); // missing field
    assertEquals(0, Rules.toNumber(null, true)); // null
    assertEquals(0, Rules.toNumber("", true));
    assertEquals(101, Rules.toNumber("101", true));
    assertEquals(101, Rules.toNumber(101, true));
    assertTrue(Double.isNaN(Rules.toNumber("abc", true)));
    assertTrue(Rules.isInteger(101));
    assertFalse(Rules.isInteger(101.5));
    assertFalse(Rules.isInteger(Double.NaN));
    assertFalse(Rules.isInteger(Double.POSITIVE_INFINITY));
  }
}
