import { useEffect, useState } from "react";
import { CheckCircle2, Loader2, Copy, Check, Home, CalendarPlus, SearchX } from "lucide-react";
import { formatDate, formatINR } from "@/lib/stayease";
import { navigate } from "@/lib/use-hash-route";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
export function ConfirmationView({ bookingRef }) {
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [copied, setCopied] = useState(false);
  const { toast } = useToast();
  useEffect(() => {
    let cancelled = false;
    fetch(`/api/bookings/${encodeURIComponent(bookingRef)}`)
      .then(async (res) => {
        const data = await res.json();
        if (cancelled) return;
        if (!res.ok) {
          setNotFound(true);
          return;
        }
        setBooking(data.booking);
      })
      .catch(() => !cancelled && setNotFound(true))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [bookingRef]);
  const copyRef = async () => {
    if (!booking) return;
    try {
      await navigator.clipboard.writeText(booking.bookingRef);
      setCopied(true);
      toast({
        title: "Copied",
        description: `Booking ID ${booking.bookingRef} copied to clipboard.`,
      });
      setTimeout(() => setCopied(false), 2000);
    } catch {
      toast({
        title: "Could not copy",
        description: `Your booking ID is ${booking.bookingRef}.`,
        variant: "destructive",
      });
    }
  };
  // ---------- Loading ----------
  if (loading) {
    return (
      <div className="flex min-h-[60vh] w-full flex-col items-center justify-center gap-4 px-4">
        <Loader2 className="size-10 animate-spin text-primary" aria-hidden />
        <p className="text-sm text-muted-foreground">Loading your booking…</p>
      </div>
    );
  }
  // ---------- Not found ----------
  if (notFound || !booking) {
    return (
      <div className="mx-auto flex w-full max-w-6xl flex-col items-center px-4 py-24 text-center sm:px-6">
        <span className="flex size-16 items-center justify-center rounded-[20px] bg-secondary text-primary">
          <SearchX className="size-8" aria-hidden />
        </span>
        <h1 className="mt-6 font-heading text-3xl font-semibold">Booking Not Found</h1>
        <p className="mt-3 max-w-md text-muted-foreground">
          We could not find a booking with the ID{" "}
          <span className="font-semibold text-foreground">{bookingRef}</span>. Please check the
          reference and try again.
        </p>
        <Button
          className="btn-gradient mt-8 h-12 border-0 px-8 text-base"
          onClick={() => navigate("/")}
        >
          Back To Home
        </Button>
      </div>
    );
  }
  // ---------- Success ----------
  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-12 sm:px-6 sm:py-16">
      {/* Headline */}
      <div className="text-center">
        <span className="mx-auto flex size-20 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 shadow-soft">
          <CheckCircle2 className="size-11" aria-hidden />
        </span>
        <h1 className="mt-6 font-heading text-3xl font-semibold sm:text-4xl">Booking Confirmed</h1>
        <p className="mt-3 text-muted-foreground">
          Thank you, {booking.guestName.split(" ")[0]}! Your room is reserved. A confirmation
          summary is shown below.
        </p>

        {/* Booking ID pill */}
        <button
          onClick={copyRef}
          className="mt-6 inline-flex items-center gap-2.5 rounded-full bg-secondary px-5 py-2.5 font-mono text-sm font-semibold text-primary shadow-soft transition-colors hover:bg-accent"
          title="Copy booking ID"
        >
          {booking.bookingRef}
          {copied ? (
            <Check className="size-4 text-emerald-600" aria-hidden />
          ) : (
            <Copy className="size-4 opacity-60" aria-hidden />
          )}
        </button>
      </div>

      {/* Details card */}
      <div className="mt-10 overflow-hidden rounded-[25px] bg-card shadow-soft-lg">
        <div className="flex items-center gap-5 border-b border-border bg-secondary/50 p-6">
          <img
            src={booking.roomImage}
            alt={booking.roomName}
            className="hidden h-20 w-28 rounded-[15px] object-cover sm:block"
          />
          <div className="flex-1">
            <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
              Your Room
            </p>
            <h2 className="mt-1 font-heading text-xl font-semibold">{booking.roomName}</h2>
            <p className="mt-0.5 text-sm text-muted-foreground">
              {formatINR(booking.totalPrice / booking.nights)} per night
            </p>
          </div>
          <span className="rounded-full bg-emerald-50 px-4 py-1.5 text-sm font-semibold text-emerald-700">
            {booking.status}
          </span>
        </div>

        <dl className="grid grid-cols-2 gap-x-6 gap-y-5 p-6 sm:grid-cols-3 sm:p-8">
          {[
            { label: "Guest", value: booking.guestName },
            { label: "Check-in", value: formatDate(booking.checkIn) },
            { label: "Check-out", value: formatDate(booking.checkOut) },
            { label: "Guests", value: String(booking.guests) },
            { label: "Nights", value: String(booking.nights) },
            { label: "Email", value: booking.email, small: true },
          ].map((item) => (
            <div key={item.label}>
              <dt className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
                {item.label}
              </dt>
              <dd className={`mt-1.5 font-medium ${item.small ? "text-sm break-all" : ""}`}>
                {item.value}
              </dd>
            </div>
          ))}
        </dl>

        <div className="border-t border-border bg-secondary/50 px-6 py-5 sm:px-8">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <span className="text-sm text-muted-foreground">
              {formatINR(booking.totalPrice / booking.nights)} × {booking.nights}{" "}
              {booking.nights === 1 ? "night" : "nights"}
            </span>
            <span className="font-heading text-2xl font-semibold text-primary">
              Total: {formatINR(booking.totalPrice)}
            </span>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="mt-9 flex flex-wrap justify-center gap-3">
        <Button className="btn-gradient h-12 border-0 px-8 text-base" onClick={() => navigate("/")}>
          <Home className="size-4" aria-hidden /> Back To Home
        </Button>
        <Button
          variant="outline"
          className="h-12 rounded-[25px] px-8 text-base"
          onClick={() => navigate("/rooms")}
        >
          <CalendarPlus className="size-4" aria-hidden /> Book Another Stay
        </Button>
      </div>
    </div>
  );
}
