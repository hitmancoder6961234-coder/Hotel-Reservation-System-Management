import { useCallback, useEffect, useState } from "react";
import { Download, Loader2, LockKeyhole, LogOut, RefreshCw } from "lucide-react";
import {
  AdminSessionError,
  downloadBookingsExcel,
  fetchAdmin,
  fetchAdminBookings,
  loginAdmin,
  logoutAdmin,
} from "@/lib/admin-api";
import { formatINR } from "@/lib/stayease";
import { Button } from "@/components/ui/button";

const INPUT =
  "h-12 rounded-[15px] border border-input bg-background px-4 text-sm outline-none transition placeholder:text-muted-foreground/60 focus:border-ring focus:ring-[3px] focus:ring-ring/30";

/** Admin area: sign-in form, then the bookings dashboard. Reached at #/admin (no public link). */
export function AdminView() {
  // undefined = still checking the session, null = signed out, string = signed-in username
  const [admin, setAdmin] = useState(undefined);
  const [checkError, setCheckError] = useState("");
  const signedOut = useCallback(() => setAdmin(null), []);

  useEffect(() => {
    let cancelled = false;
    fetchAdmin()
      .then((username) => !cancelled && setAdmin(username))
      .catch((e) => {
        if (cancelled) return;
        setCheckError(e.message);
        setAdmin(null);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (admin === undefined) {
    return (
      <div className="flex justify-center px-4 py-28" role="status" aria-label="Checking session">
        <Loader2 className="size-6 animate-spin text-primary" aria-hidden />
      </div>
    );
  }
  if (admin === null) {
    return <LoginForm initialError={checkError} onSignedIn={setAdmin} />;
  }
  return <Dashboard username={admin} onSignedOut={signedOut} />;
}

function LoginForm({ initialError, onSignedIn }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(initialError);
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    if (!username.trim() || !password) {
      setError("Please enter your admin ID and password.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      onSignedIn(await loginAdmin(username.trim(), password));
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-md px-4 py-16 sm:px-6 sm:py-24">
      <form onSubmit={submit} className="rounded-[25px] bg-card p-8 shadow-soft" noValidate>
        <div className="flex flex-col items-center text-center">
          <span className="flex size-12 items-center justify-center rounded-[15px] bg-secondary text-primary">
            <LockKeyhole className="size-6" aria-hidden />
          </span>
          <h1 className="mt-4 font-heading text-2xl font-semibold">Admin Login</h1>
          <p className="mt-1 text-sm text-muted-foreground">Sign in to view the reservations.</p>
        </div>
        <div className="mt-7 grid gap-4">
          <label className="grid gap-1.5 text-sm font-medium">
            Admin ID
            <input
              className={INPUT}
              type="text"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </label>
          <label className="grid gap-1.5 text-sm font-medium">
            Password
            <input
              className={INPUT}
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
        </div>
        {error && (
          <p role="alert" className="mt-4 rounded-[15px] bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </p>
        )}
        <Button type="submit" className="btn-gradient mt-6 h-12 w-full border-0 text-base" disabled={busy}>
          {busy ? <Loader2 className="size-4 animate-spin" aria-hidden /> : null} Sign in
        </Button>
      </form>
    </div>
  );
}

// The dashboard lists each booking with the fields in this order (id ... createdAt).
function fieldRows(b) {
  return [
    ["id", b.id],
    ["bookingRef", b.bookingRef],
    ["guestName", b.guestName],
    ["email", b.email],
    ["mobile", b.mobile],
    ["roomId", b.roomId],
    ["roomName", b.roomName],
    ["checkIn", b.checkIn],
    ["checkOut", b.checkOut],
    ["guests", b.guests],
    ["nights", b.nights],
    ["totalPrice", formatINR(b.totalPrice)],
    ["status", b.status],
    ["createdAt", b.createdAt ? new Date(b.createdAt).toLocaleString("en-GB") : ""],
  ];
}

function Dashboard({ username, onSignedOut }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await fetchAdminBookings());
    } catch (e) {
      if (e instanceof AdminSessionError) onSignedOut();
      else setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [onSignedOut]);

  useEffect(() => {
    load();
  }, [load]);

  const signOut = async () => {
    try {
      await logoutAdmin();
    } finally {
      onSignedOut();
    }
  };

  const exportExcel = async () => {
    setExporting(true);
    setError("");
    try {
      await downloadBookingsExcel();
    } catch (e) {
      if (e instanceof AdminSessionError) onSignedOut();
      else setError(e.message);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-10 sm:px-6 sm:py-14">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold uppercase tracking-widest text-primary">Admin Dashboard</p>
          <h1 className="mt-2 font-heading text-3xl font-semibold">
            Total bookings: {data ? data.total : "…"}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">Signed in as {username}</p>
        </div>
        <div className="flex flex-wrap gap-3">
          <Button variant="outline" className="h-10 rounded-[25px] px-5" onClick={load} disabled={loading}>
            <RefreshCw className="size-4" aria-hidden /> Refresh
          </Button>
          <Button className="btn-gradient h-10 border-0 px-5" onClick={exportExcel} disabled={exporting}>
            {exporting ? <Loader2 className="size-4 animate-spin" aria-hidden /> : <Download className="size-4" aria-hidden />}{" "}
            Download Excel
          </Button>
          <Button variant="outline" className="h-10 rounded-[25px] px-5" onClick={signOut}>
            <LogOut className="size-4" aria-hidden /> Log out
          </Button>
        </div>
      </header>

      {error && (
        <p role="alert" className="mt-6 rounded-[15px] bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </p>
      )}

      {loading && !data ? (
        <div className="flex justify-center py-20" role="status" aria-label="Loading bookings">
          <Loader2 className="size-6 animate-spin text-primary" aria-hidden />
        </div>
      ) : data && data.bookings.length === 0 ? (
        <p className="mt-10 rounded-[20px] bg-secondary/70 px-6 py-10 text-center text-muted-foreground">
          There are no bookings yet.
        </p>
      ) : (
        <div className="mt-8 grid gap-6 md:grid-cols-2">
          {data?.bookings.map((booking, index) => (
            <section
              key={booking.id}
              aria-label={`Booking ${index + 1}`}
              className="rounded-[20px] bg-card p-6 shadow-soft"
            >
              <h2 className="font-heading text-lg font-semibold">BOOKING {index + 1}</h2>
              <dl className="mt-4 grid grid-cols-[8.5rem_1fr] gap-x-4 gap-y-2 text-sm">
                {fieldRows(booking).map(([label, value]) => (
                  <div key={label} className="contents">
                    <dt className="font-medium text-muted-foreground">{label}</dt>
                    <dd className="break-words">{String(value)}</dd>
                  </div>
                ))}
              </dl>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
