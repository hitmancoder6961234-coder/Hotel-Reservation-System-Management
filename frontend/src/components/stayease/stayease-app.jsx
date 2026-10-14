import { useEffect, useRef, useState } from "react";
import { RefreshCw } from "lucide-react";
import { useHashRoute } from "@/lib/use-hash-route";
import { Navbar } from "./navbar";
import { Footer } from "./footer";
import { HomeView } from "./home-view";
import { RoomsView } from "./rooms-view";
import { RoomDetailsView } from "./room-details-view";
import { BookingView } from "./booking-view";
import { ConfirmationView } from "./confirmation-view";
import { Button } from "@/components/ui/button";
function ViewSkeleton() {
  return (
    <div className="mx-auto w-full max-w-6xl animate-pulse px-4 py-10 sm:px-6" aria-hidden>
      <div className="h-[300px] rounded-[25px] bg-secondary/70 sm:h-[420px]" />
      <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {[0, 1, 2].map((i) => (
          <div key={i} className="overflow-hidden rounded-[20px] bg-secondary/70">
            <div className="aspect-[4/3]" />
            <div className="space-y-3 p-5">
              <div className="h-5 w-1/2 rounded-full bg-secondary" />
              <div className="h-4 w-1/3 rounded-full bg-secondary" />
              <div className="h-10 rounded-[25px] bg-secondary" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
function LoadError({ onRetry }) {
  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col items-center px-4 py-28 text-center sm:px-6">
      <h1 className="font-heading text-2xl font-semibold">Something went wrong</h1>
      <p className="mt-3 max-w-md text-muted-foreground">
        We could not load the rooms from the server. Please try again.
      </p>
      <Button className="btn-gradient mt-7 h-11 border-0 px-6" onClick={onRetry}>
        <RefreshCw className="size-4" aria-hidden /> Retry
      </Button>
    </div>
  );
}
export function StayEaseApp() {
  const route = useHashRoute();
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const prevPath = useRef(null);
  // ---------- Load rooms (GET /api/rooms) ----------
  useEffect(() => {
    let cancelled = false;
    fetch("/api/rooms")
      .then(async (res) => {
        const data = await res.json();
        if (cancelled) return;
        if (!res.ok) throw new Error(data.error ?? "Failed to load rooms");
        setRooms(data.rooms);
      })
      .catch(() => !cancelled && setError(true))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);
  const retryLoad = () => {
    setLoading(true);
    setError(false);
    setReloadKey((k) => k + 1);
  };
  // ---------- Scroll to top on view change ----------
  useEffect(() => {
    if (prevPath.current !== null && prevPath.current !== route.path) {
      window.scrollTo({ top: 0, behavior: "instant" });
    }
    prevPath.current = route.path;
  }, [route.path]);
  // ---------- Resolve the current view ----------
  const { segments, query } = route;
  let view;
  if (segments[0] === "confirmation" && segments[1]) {
    // Confirmation is independent of the rooms list. Keyed by bookingRef so a
    // new confirmation remounts the component fresh.
    view = (
      <ConfirmationView
        key={decodeURIComponent(segments[1])}
        bookingRef={decodeURIComponent(segments[1])}
      />
    );
  } else if (loading) {
    view = <ViewSkeleton />;
  } else if (error) {
    view = <LoadError onRetry={retryLoad} />;
  } else if (segments[0] === "rooms" && segments.length === 1) {
    view = (
      <RoomsView
        rooms={rooms}
        checkIn={query.get("checkIn") ?? undefined}
        checkOut={query.get("checkOut") ?? undefined}
        guests={Number(query.get("guests")) || undefined}
      />
    );
  } else if (segments[0] === "rooms" && segments.length === 2) {
    const roomId = Number(segments[1]);
    const room = rooms.find((r) => r.id === roomId);
    if (room) {
      view = (
        <RoomDetailsView
          room={room}
          checkIn={query.get("checkIn") ?? undefined}
          checkOut={query.get("checkOut") ?? undefined}
          guests={Number(query.get("guests")) || undefined}
        />
      );
    } else {
      view = <RoomNotFound />;
    }
  } else if (segments[0] === "booking") {
    view = (
      <BookingView
        rooms={rooms}
        roomId={Number(query.get("room")) || undefined}
        initialCheckIn={query.get("checkIn") ?? undefined}
        initialCheckOut={query.get("checkOut") ?? undefined}
        initialGuests={Number(query.get("guests")) || undefined}
      />
    );
  } else {
    view = <HomeView rooms={rooms} />;
  }
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar path={route.path} />
      <main className="flex-1">{view}</main>
      <Footer />
    </div>
  );
}
function RoomNotFound() {
  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col items-center px-4 py-28 text-center sm:px-6">
      <h1 className="font-heading text-2xl font-semibold">Room not found</h1>
      <p className="mt-3 max-w-md text-muted-foreground">
        The room you are looking for does not exist or is no longer listed.
      </p>
      <Button
        variant="outline"
        className="mt-7 h-11 rounded-[25px] px-6"
        onClick={() => (window.location.hash = "/rooms")}
      >
        View all rooms
      </Button>
    </div>
  );
}
