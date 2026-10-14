import { X, SearchX } from "lucide-react";
import { formatDateShort } from "@/lib/stayease";
import { navigate } from "@/lib/use-hash-route";
import { RoomCard } from "./room-card";
import { Button } from "@/components/ui/button";
export function RoomsView({ rooms, checkIn, checkOut, guests }) {
  const hasSearch = Boolean(checkIn && checkOut && guests);
  const filtered = hasSearch ? rooms.filter((r) => r.capacity >= (guests ?? 1)) : rooms;
  const clearSearch = () => navigate("/rooms");
  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-10 sm:px-6 sm:py-14">
      {/* Header */}
      <header className="text-center">
        <p className="text-sm font-semibold uppercase tracking-widest text-primary">Stay With Us</p>
        <h1 className="mt-2 font-heading text-3xl font-semibold sm:text-4xl">Our Rooms</h1>
        <p className="mx-auto mt-3 max-w-xl text-muted-foreground">
          Every room includes daily housekeeping, complimentary WiFi and access to all hotel
          facilities. Prices include all taxes.
        </p>
      </header>

      {/* Active search banner */}
      {hasSearch && (
        <div className="mt-8 flex flex-wrap items-center justify-between gap-4 rounded-[20px] bg-secondary/70 px-5 py-4">
          <p className="text-sm text-secondary-foreground">
            Showing rooms for{" "}
            <span className="font-semibold">
              {guests} {guests === 1 ? "guest" : "guests"}
            </span>
            {checkIn && checkOut && (
              <>
                {" "}
                · <span className="font-semibold">{formatDateShort(checkIn)}</span> →{" "}
                <span className="font-semibold">{formatDateShort(checkOut)}</span>
              </>
            )}
          </p>
          <button
            onClick={clearSearch}
            className="flex items-center gap-1.5 rounded-full bg-card px-4 py-2 text-xs font-medium text-foreground shadow-soft transition-colors hover:bg-secondary"
          >
            <X className="size-3.5" aria-hidden /> Clear search
          </button>
        </div>
      )}

      {/* Grid */}
      {filtered.length > 0 ? (
        <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((room) => (
            <RoomCard
              key={room.id}
              room={room}
              checkIn={checkIn}
              checkOut={checkOut}
              guests={guests}
            />
          ))}
        </div>
      ) : (
        <div className="mt-10 flex flex-col items-center rounded-[25px] bg-card p-12 text-center shadow-soft">
          <span className="flex size-14 items-center justify-center rounded-[20px] bg-secondary text-primary">
            <SearchX className="size-7" aria-hidden />
          </span>
          <h2 className="mt-5 font-heading text-2xl font-semibold">No rooms match your search</h2>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            We could not find a room for {guests} guests. Try clearing the search or reducing the
            number of guests.
          </p>
          <Button variant="outline" className="mt-6 h-11 rounded-[25px] px-6" onClick={clearSearch}>
            Show all rooms
          </Button>
        </div>
      )}
    </div>
  );
}
