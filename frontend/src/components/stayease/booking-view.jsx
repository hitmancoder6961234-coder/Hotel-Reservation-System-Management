import { useEffect, useMemo, useState } from "react";
import {
  Users,
  CalendarDays,
  Loader2,
  CheckCircle2,
  XCircle,
  BedDouble,
  ArrowLeft,
} from "lucide-react";
import {
  calcNights,
  calcTotal,
  formatDate,
  formatINR,
  todayStr,
  validateBookingForm,
  validateDates,
} from "@/lib/stayease";
import { navigate } from "@/lib/use-hash-route";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
export function BookingView({ rooms, roomId, initialCheckIn, initialCheckOut, initialGuests }) {
  const room = rooms.find((r) => r.id === roomId);
  const { toast } = useToast();
  const today = todayStr();
  const [form, setForm] = useState({
    guestName: "",
    email: "",
    mobile: "",
    checkIn: initialCheckIn ?? "",
    checkOut: initialCheckOut ?? "",
    guests: initialGuests && room && initialGuests <= room.capacity ? initialGuests : 1,
  });
  const [errors, setErrors] = useState({});
  const [availability, setAvailability] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState("");
  // ---------- Live calculation (plan §12/§13) ----------
  const nights = useMemo(
    () => calcNights(form.checkIn, form.checkOut),
    [form.checkIn, form.checkOut]
  );
  const total = room ? calcTotal(room.price, nights) : 0;
  const datesValid = validateDates(form.checkIn, form.checkOut) === null;
  // Derived availability display: "checking" while the debounced request for
  // the CURRENT dates is pending or missing; otherwise the stored result.
  const currentResult =
    availability && availability.checkIn === form.checkIn && availability.checkOut === form.checkOut
      ? availability.result
      : null;
  const availabilityDisplay = !datesValid
    ? "idle"
    : currentResult
      ? currentResult.status
      : "checking";
  // ---------- Availability check, debounced (plan §11) ----------
  useEffect(() => {
    if (!room || !datesValid) return;
    const ci = form.checkIn;
    const co = form.checkOut;
    const timer = setTimeout(async () => {
      try {
        const res = await fetch(`/api/rooms/${room.id}/availability?checkIn=${ci}&checkOut=${co}`);
        const data = await res.json();
        if (!res.ok) {
          setAvailability({
            checkIn: ci,
            checkOut: co,
            result: { status: "error", message: data.error ?? "Availability check failed." },
          });
          return;
        }
        if (data.available) {
          setAvailability({ checkIn: ci, checkOut: co, result: { status: "available" } });
        } else {
          setAvailability({
            checkIn: ci,
            checkOut: co,
            result: {
              status: "unavailable",
              message: data.reason ?? "This room is not available for the selected dates.",
            },
          });
        }
      } catch {
        setAvailability({
          checkIn: ci,
          checkOut: co,
          result: { status: "error", message: "Could not check availability — please try again." },
        });
      }
    }, 450);
    return () => clearTimeout(timer);
  }, [form.checkIn, form.checkOut, room?.id, datesValid]);
  // ---------- No room selected ----------
  if (!room) {
    return (
      <div className="mx-auto flex w-full max-w-6xl flex-col items-center px-4 py-24 text-center sm:px-6">
        <span className="flex size-16 items-center justify-center rounded-[20px] bg-secondary text-primary">
          <BedDouble className="size-8" aria-hidden />
        </span>
        <h1 className="mt-6 font-heading text-3xl font-semibold">Select a Room First</h1>
        <p className="mt-3 max-w-md text-muted-foreground">
          To make a reservation, choose a room from our rooms page — the booking form will open with
          the room already selected.
        </p>
        <Button
          className="btn-gradient mt-8 h-12 border-0 px-8 text-base"
          onClick={() => navigate("/rooms")}
        >
          Browse Rooms
        </Button>
      </div>
    );
  }
  const setField = (key, value) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: undefined }));
    setServerError("");
  };
  // ---------- Submit (FUNCTION 6 + 7) ----------
  const onSubmit = async (e) => {
    e.preventDefault();
    const validation = validateBookingForm(form, room);
    if (Object.keys(validation).length > 0) {
      setErrors(validation);
      return;
    }
    if (availabilityDisplay === "unavailable" || availabilityDisplay === "error") {
      toast({
        title: "Room unavailable",
        description:
          currentResult && "message" in currentResult
            ? currentResult.message
            : "This room is not available for the selected dates.",
        variant: "destructive",
      });
      return;
    }
    setSubmitting(true);
    setServerError("");
    try {
      const res = await fetch("/api/bookings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          guestName: form.guestName,
          email: form.email,
          mobile: form.mobile,
          roomId: room.id,
          checkIn: form.checkIn,
          checkOut: form.checkOut,
          guests: form.guests,
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        setServerError(data.error ?? "Booking failed. Please try again.");
        toast({
          title: "Booking failed",
          description: data.error ?? "Booking failed. Please try again.",
          variant: "destructive",
        });
        setSubmitting(false);
        return;
      }
      toast({
        title: "Booking confirmed",
        description: `Your booking reference is ${data.booking.bookingRef}.`,
      });
      navigate(`/confirmation/${encodeURIComponent(data.booking.bookingRef)}`);
    } catch {
      setServerError("Network error — please check your connection and try again.");
      toast({
        title: "Booking failed",
        description: "Network error — please try again.",
        variant: "destructive",
      });
      setSubmitting(false);
    }
  };
  const canSubmit =
    datesValid &&
    Object.values(errors).every((v) => !v) &&
    currentResult?.status === "available" &&
    !submitting;
  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-10 sm:px-6 sm:py-14">
      {/* Back + heading */}
      <button
        onClick={() => navigate(`/rooms/${room.id}`)}
        className="flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
      >
        <ArrowLeft className="size-4" aria-hidden /> Back to {room.name}
      </button>
      <header className="mt-6">
        <p className="text-sm font-semibold uppercase tracking-widest text-primary">Almost There</p>
        <h1 className="mt-2 font-heading text-3xl font-semibold sm:text-4xl">
          Complete Your Booking
        </h1>
        <p className="mt-3 max-w-xl text-muted-foreground">
          Enter your details and pick your dates — the total updates live and we verify availability
          before confirming.
        </p>
      </header>

      <form onSubmit={onSubmit} className="mt-10 grid gap-8 lg:grid-cols-[1fr_380px]" noValidate>
        {/* ============ Left: form card ============ */}
        <div className="rounded-[25px] bg-card p-6 shadow-soft sm:p-8">
          <h2 className="font-heading text-xl font-semibold">Guest Details</h2>

          <div className="mt-6 grid gap-5">
            {/* Name */}
            <label className="grid gap-1.5 text-sm font-medium">
              Guest Name
              <input
                type="text"
                value={form.guestName}
                onChange={(e) => setField("guestName", e.target.value)}
                placeholder="e.g. Ahmed Khan"
                className="h-12 rounded-[15px] border border-input bg-background px-4 text-sm outline-none transition placeholder:text-muted-foreground/60 focus:border-ring focus:ring-[3px] focus:ring-ring/30"
              />
              {errors.guestName && (
                <span className="text-xs font-medium text-destructive">{errors.guestName}</span>
              )}
            </label>

            {/* Email + Mobile */}
            <div className="grid gap-5 sm:grid-cols-2">
              <label className="grid gap-1.5 text-sm font-medium">
                Email
                <input
                  type="email"
                  value={form.email}
                  onChange={(e) => setField("email", e.target.value)}
                  placeholder="you@example.com"
                  className="h-12 rounded-[15px] border border-input bg-background px-4 text-sm outline-none transition placeholder:text-muted-foreground/60 focus:border-ring focus:ring-[3px] focus:ring-ring/30"
                />
                {errors.email && (
                  <span className="text-xs font-medium text-destructive">{errors.email}</span>
                )}
              </label>
              <label className="grid gap-1.5 text-sm font-medium">
                Mobile Number
                <input
                  type="tel"
                  value={form.mobile}
                  onChange={(e) => setField("mobile", e.target.value)}
                  placeholder="+91 98765 43210"
                  className="h-12 rounded-[15px] border border-input bg-background px-4 text-sm outline-none transition placeholder:text-muted-foreground/60 focus:border-ring focus:ring-[3px] focus:ring-ring/30"
                />
                {errors.mobile && (
                  <span className="text-xs font-medium text-destructive">{errors.mobile}</span>
                )}
              </label>
            </div>

            {/* Dates */}
            <div className="grid gap-5 sm:grid-cols-2">
              <label className="grid gap-1.5 text-sm font-medium">
                Check-in Date
                <input
                  type="date"
                  value={form.checkIn}
                  min={today}
                  onChange={(e) => setField("checkIn", e.target.value)}
                  className="h-12 rounded-[15px] border border-input bg-background px-4 text-sm outline-none transition focus:border-ring focus:ring-[3px] focus:ring-ring/30"
                />
                {errors.checkIn && (
                  <span className="text-xs font-medium text-destructive">{errors.checkIn}</span>
                )}
              </label>
              <label className="grid gap-1.5 text-sm font-medium">
                Check-out Date
                <input
                  type="date"
                  value={form.checkOut}
                  min={form.checkIn || today}
                  onChange={(e) => setField("checkOut", e.target.value)}
                  className="h-12 rounded-[15px] border border-input bg-background px-4 text-sm outline-none transition focus:border-ring focus:ring-[3px] focus:ring-ring/30"
                />
                {errors.checkOut && (
                  <span className="text-xs font-medium text-destructive">{errors.checkOut}</span>
                )}
              </label>
            </div>

            {/* Guests */}
            <label className="grid gap-1.5 text-sm font-medium sm:max-w-xs">
              Number of Guests
              <select
                value={form.guests}
                onChange={(e) => setField("guests", Number(e.target.value))}
                className="h-12 rounded-[15px] border border-input bg-background px-4 text-sm outline-none transition focus:border-ring focus:ring-[3px] focus:ring-ring/30"
              >
                {Array.from({ length: room.capacity }, (_, i) => i + 1).map((n) => (
                  <option key={n} value={n}>
                    {n} {n === 1 ? "Guest" : "Guests"}
                  </option>
                ))}
              </select>
              <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <Users className="size-3.5" aria-hidden /> This room hosts up to {room.capacity}{" "}
                guests
              </span>
              {errors.guests && (
                <span className="text-xs font-medium text-destructive">{errors.guests}</span>
              )}
            </label>
          </div>

          {serverError && (
            <p
              className="mt-6 rounded-[15px] bg-destructive/10 px-4 py-3 text-sm font-medium text-destructive"
              role="alert"
            >
              {serverError}
            </p>
          )}
        </div>

        {/* ============ Right: summary card ============ */}
        <aside
          className="h-fit rounded-[25px] bg-card p-6 shadow-soft lg:sticky lg:top-24"
          aria-label="Booking summary"
        >
          <div className="flex items-center gap-4">
            <img src={room.image} alt={room.name} className="size-20 rounded-[15px] object-cover" />
            <div>
              <h2 className="font-heading text-lg font-semibold">{room.name}</h2>
              <p className="mt-0.5 flex items-center gap-1.5 text-xs text-muted-foreground">
                <Users className="size-3.5" aria-hidden /> Up to {room.capacity} guests
              </p>
            </div>
          </div>

          <dl className="mt-6 space-y-3 text-sm">
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">Price per night</dt>
              <dd className="font-medium">{formatINR(room.price)}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">Check-in</dt>
              <dd className="font-medium">{form.checkIn ? formatDate(form.checkIn) : "—"}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">Check-out</dt>
              <dd className="font-medium">{form.checkOut ? formatDate(form.checkOut) : "—"}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">Guests</dt>
              <dd className="font-medium">{form.guests}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-muted-foreground">Nights</dt>
              <dd className="font-medium">
                {datesValid && nights > 0 ? (
                  <span className="flex items-center gap-1.5">
                    {nights}
                    <span className="text-xs font-normal text-muted-foreground">
                      ({formatINR(room.price)} × {nights})
                    </span>
                  </span>
                ) : (
                  "—"
                )}
              </dd>
            </div>
          </dl>

          {/* Availability indicator */}
          <div className="mt-5" aria-live="polite">
            {availabilityDisplay === "checking" && (
              <p className="flex items-center gap-2 rounded-[15px] bg-secondary/70 px-4 py-2.5 text-sm text-muted-foreground">
                <Loader2 className="size-4 animate-spin" aria-hidden /> Checking availability…
              </p>
            )}
            {availabilityDisplay === "available" && (
              <p className="flex items-center gap-2 rounded-[15px] bg-emerald-50 px-4 py-2.5 text-sm font-medium text-emerald-700">
                <CheckCircle2 className="size-4 shrink-0" aria-hidden />
                Available for your selected dates
              </p>
            )}
            {(availabilityDisplay === "unavailable" || availabilityDisplay === "error") &&
              currentResult &&
              "message" in currentResult && (
                <p className="flex items-center gap-2 rounded-[15px] bg-destructive/10 px-4 py-2.5 text-sm font-medium text-destructive">
                  <XCircle className="size-4 shrink-0" aria-hidden /> {currentResult.message}
                </p>
              )}
            {availabilityDisplay === "idle" && (
              <p className="flex items-center gap-2 rounded-[15px] bg-secondary/70 px-4 py-2.5 text-sm text-muted-foreground">
                <CalendarDays className="size-4" aria-hidden /> Select valid dates to check
                availability
              </p>
            )}
          </div>

          <div className="mt-6 border-t border-border pt-5">
            <div className="flex items-baseline justify-between">
              <span className="text-sm font-medium text-muted-foreground">Total Price</span>
              <span className="font-heading text-3xl font-semibold text-primary">
                {datesValid && nights > 0 ? formatINR(total) : "—"}
              </span>
            </div>
          </div>

          <Button
            type="submit"
            className="btn-gradient mt-6 h-13 w-full border-0 py-3.5 text-base"
            disabled={!canSubmit}
          >
            {submitting ? (
              <>
                <Loader2 className="size-5 animate-spin" aria-hidden /> Confirming…
              </>
            ) : (
              "Confirm Booking"
            )}
          </Button>
          <p className="mt-4 text-center text-xs text-muted-foreground">
            You will see your booking confirmation with a unique booking ID.
          </p>
        </aside>
      </form>
    </div>
  );
}
