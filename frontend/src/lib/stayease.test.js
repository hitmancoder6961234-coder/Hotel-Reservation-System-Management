import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  parseDate,
  calcNights,
  calcTotal,
  formatINR,
  formatDate,
  formatDateShort,
  validateDates,
  validateBookingForm,
  bookingUrl,
} from "@/lib/stayease";
import { ROOMS } from "@/test/fixtures";

describe("date helpers", () => {
  it("parses only real YYYY-MM-DD dates", () => {
    expect(parseDate("2026-09-20")).toBeInstanceOf(Date);
    expect(parseDate("2026-02-30")).toBeNull();
    expect(parseDate("20-09-2026")).toBeNull();
    expect(parseDate("")).toBeNull();
  });

  it("counts nights between two dates", () => {
    expect(calcNights("2026-09-20", "2026-09-23")).toBe(3);
    expect(calcNights("2026-09-20", "2026-09-21")).toBe(1);
    expect(calcNights("bad", "2026-09-21")).toBe(0);
  });

  it("formats dates like the original UI", () => {
    expect(formatDate("2026-09-20")).toBe("20 September 2026");
    // Newer CLDR data (en-GB) abbreviates September as "Sept"; older data as "Sep".
    expect(formatDateShort("2026-09-20")).toMatch(/^20 Sept? 2026$/);
  });
});

describe("price helpers", () => {
  it("calculates the total price", () => {
    expect(calcTotal(4000, 3)).toBe(12000);
    expect(calcTotal(4000, -1)).toBe(0);
  });
  it("formats Indian rupees", () => {
    expect(formatINR(12000)).toBe("₹12,000");
    expect(formatINR(100000)).toBe("₹1,00,000");
  });
});

describe("validateDates", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 8, 10, 12, 0, 0)); // 10 Sep 2026
  });
  afterEach(() => vi.useRealTimers());

  it("uses the exact messages of the original app", () => {
    expect(validateDates("", "2026-09-20")).toBe("Please select check-in date.");
    expect(validateDates("2026-09-20", "")).toBe("Please select check-out date.");
    expect(validateDates("2026-02-30", "2026-03-01")).toBe("Please select valid dates.");
    expect(validateDates("2026-09-01", "2026-09-20")).toBe("Check-in date cannot be in the past.");
    expect(validateDates("2026-09-20", "2026-09-20")).toBe("Check-out must be after check-in.");
    expect(validateDates("2026-09-20", "2026-09-23")).toBeNull();
  });
});

describe("validateBookingForm", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 8, 10, 12, 0, 0));
  });
  afterEach(() => vi.useRealTimers());

  const valid = {
    guestName: "Ahmed Khan",
    email: "ahmed@example.com",
    mobile: "+91 98765 43210",
    checkIn: "2026-09-20",
    checkOut: "2026-09-23",
    guests: 2,
  };

  it("accepts a valid form", () => {
    expect(validateBookingForm(valid, ROOMS[0])).toEqual({});
  });

  it("reports every invalid field with the original wording", () => {
    const errors = validateBookingForm(
      { guestName: "A", email: "nope", mobile: "123", checkIn: "", checkOut: "", guests: 0 },
      ROOMS[0]
    );
    expect(errors.guestName).toBe("Name must be at least 2 characters.");
    expect(errors.email).toBe("Please enter a valid email.");
    expect(errors.mobile).toBe("Please enter a valid mobile number.");
    expect(errors.checkIn).toBe("Please select check-in date.");
    expect(errors.guests).toBe("Please select number of guests.");
  });

  it("rejects more guests than the room capacity", () => {
    const errors = validateBookingForm({ ...valid, guests: 3 }, ROOMS[0]);
    expect(errors.guests).toBe("Number of guests exceeds room capacity (max 2).");
  });
});

describe("bookingUrl", () => {
  it("keeps the search context", () => {
    expect(bookingUrl(101, "2026-09-20", "2026-09-23", 2)).toBe(
      "/booking?room=101&checkIn=2026-09-20&checkOut=2026-09-23&guests=2"
    );
    expect(bookingUrl(101)).toBe("/booking?room=101");
  });
});
