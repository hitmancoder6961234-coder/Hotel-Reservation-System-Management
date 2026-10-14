/**
 * StayEase — Shared date/price helpers and validation rules.
 * (The room serialisation that used to live here now happens in the Java backend.)
 * Mirrors the project plan: date calculation, price calculation,
 * guest validation, date validation, form validation (§4, §13, §14).
 */
// ============================================================
// Date helpers (all dates are YYYY-MM-DD strings)
// ============================================================
/** Parse a YYYY-MM-DD string into a local Date (no timezone surprises). */
export function parseDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
  const [y, m, d] = value.split("-").map(Number);
  const date = new Date(y, m - 1, d);
  if (date.getFullYear() !== y || date.getMonth() !== m - 1 || date.getDate() !== d) return null;
  return date;
}
/** Today's date as YYYY-MM-DD (local). */
export function todayStr() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}
/**
 * FUNCTION 2 — Date calculation.
 * Nights between check-in and check-out, e.g. Sep 20 → Sep 23 = 3 nights.
 */
export function calcNights(checkIn, checkOut) {
  const a = parseDate(checkIn);
  const b = parseDate(checkOut);
  if (!a || !b) return 0;
  const diff = b.getTime() - a.getTime();
  return Math.round(diff / (1000 * 60 * 60 * 24));
}
/** Format a YYYY-MM-DD date for display, e.g. "20 September 2026". */
export function formatDate(value) {
  const date = parseDate(value);
  if (!date) return value;
  return date.toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" });
}
/** Short display, e.g. "20 Sep 2026". */
export function formatDateShort(value) {
  const date = parseDate(value);
  if (!date) return value;
  return date.toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });
}
// ============================================================
// Price helpers
// ============================================================
/** Format a number as Indian Rupees, e.g. 12000 → "₹12,000". */
export function formatINR(amount) {
  return `₹${new Intl.NumberFormat("en-IN").format(amount)}`;
}
/**
 * FUNCTION 3 — Price calculation. ₹4,000 × 3 nights = ₹12,000.
 */
export function calcTotal(price, nights) {
  return price * Math.max(nights, 0);
}
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MOBILE_RE = /^[+]?[\d\s-]{10,15}$/;
/** FUNCTION 5 — Date validation. Returns an error message or null. */
export function validateDates(checkIn, checkOut) {
  if (!checkIn) return "Please select check-in date.";
  if (!checkOut) return "Please select check-out date.";
  const a = parseDate(checkIn);
  const b = parseDate(checkOut);
  if (!a || !b) return "Please select valid dates.";
  if (checkIn < todayStr()) return "Check-in date cannot be in the past.";
  if (checkOut <= checkIn) return "Check-out must be after check-in.";
  return null;
}
/**
 * FUNCTION 4 + 6 — Guest & form validation.
 * Validates the whole booking form against the selected room.
 */
export function validateBookingForm(data, room) {
  const errors = {};
  if (!data.guestName.trim()) errors.guestName = "Please enter your name.";
  else if (data.guestName.trim().length < 2)
    errors.guestName = "Name must be at least 2 characters.";
  if (!data.email.trim()) errors.email = "Please enter your email.";
  else if (!EMAIL_RE.test(data.email.trim())) errors.email = "Please enter a valid email.";
  if (!data.mobile.trim()) errors.mobile = "Please enter your mobile number.";
  else if (!MOBILE_RE.test(data.mobile.trim()))
    errors.mobile = "Please enter a valid mobile number.";
  const dateError = validateDates(data.checkIn, data.checkOut);
  if (dateError) {
    if (!data.checkIn) errors.checkIn = "Please select check-in date.";
    else if (!data.checkOut) errors.checkOut = "Please select check-out date.";
    else if (data.checkOut <= data.checkIn) errors.checkOut = "Check-out must be after check-in.";
    else errors.checkIn = dateError;
  }
  if (!data.guests || data.guests < 1) errors.guests = "Please select number of guests.";
  else if (data.guests > room.capacity)
    errors.guests = `Number of guests exceeds room capacity (max ${room.capacity}).`;
  return errors;
}
/** Generate a booking link preserving the current search context. */
export function bookingUrl(roomId, checkIn, checkOut, guests) {
  const params = new URLSearchParams();
  params.set("room", String(roomId));
  if (checkIn) params.set("checkIn", checkIn);
  if (checkOut) params.set("checkOut", checkOut);
  if (guests) params.set("guests", String(guests));
  return `/booking?${params.toString()}`;
}
