import { Users, Check, ArrowLeft, CalendarCheck2, ShieldCheck, BadgeCheck } from "lucide-react";
import { formatINR, bookingUrl } from "@/lib/stayease";
import { navigate } from "@/lib/use-hash-route";
import { getFacilityIcon } from "./facility-icons";
import { Button } from "@/components/ui/button";
export function RoomDetailsView({ room, checkIn, checkOut, guests }) {
  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-10 sm:px-6 sm:py-14">
      {/* Back link */}
      <button
        onClick={() => navigate("/rooms")}
        className="flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
      >
        <ArrowLeft className="size-4" aria-hidden /> All Rooms
      </button>

      <div className="mt-6 grid gap-10 lg:grid-cols-2">
        {/* Left: image + facilities */}
        <div>
          <div className="overflow-hidden rounded-[20px] shadow-soft">
            <img
              src={room.image}
              alt={`${room.name} at StayEase`}
              className="aspect-[4/3] w-full object-cover"
            />
          </div>

          <h2 className="mt-8 font-heading text-xl font-semibold">Room Facilities</h2>
          <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
            {room.facilities.map((facility) => {
              const Icon = getFacilityIcon(facility);
              return (
                <div
                  key={facility}
                  className="flex items-center gap-2.5 rounded-[15px] bg-secondary/70 px-4 py-3"
                >
                  <Icon className="size-5 shrink-0 text-primary" aria-hidden />
                  <span className="text-sm font-medium">{facility}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right: details */}
        <div>
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="font-heading text-3xl font-semibold sm:text-4xl">{room.name}</h1>
              <p className="mt-2 flex items-center gap-1.5 text-sm text-muted-foreground">
                <Users className="size-4" aria-hidden /> Maximum {room.capacity} guests
              </p>
            </div>
            <span className="rounded-full bg-secondary px-4 py-2 text-base font-semibold text-primary">
              {formatINR(room.price)}
              <span className="text-sm font-normal text-muted-foreground"> / night</span>
            </span>
          </div>

          <p className="mt-6 leading-relaxed text-muted-foreground">{room.description}</p>

          {/* Features */}
          <h2 className="mt-8 font-heading text-xl font-semibold">Room Features</h2>
          <ul className="mt-4 grid gap-2.5 sm:grid-cols-2">
            {room.features.map((feature) => (
              <li key={feature} className="flex items-start gap-2.5 text-sm">
                <Check className="mt-0.5 size-4 shrink-0 text-emerald-600" aria-hidden />
                <span>{feature}</span>
              </li>
            ))}
          </ul>

          {/* CTA card */}
          <div className="mt-9 rounded-[25px] bg-card p-6 shadow-soft">
            <div className="flex items-baseline justify-between">
              <span className="text-sm font-medium text-muted-foreground">Price per night</span>
              <span className="font-heading text-2xl font-semibold text-primary">
                {formatINR(room.price)}
              </span>
            </div>
            <Button
              className="btn-gradient mt-5 h-13 w-full border-0 py-3.5 text-base"
              onClick={() => navigate(bookingUrl(room.id, checkIn, checkOut, guests))}
            >
              <CalendarCheck2 className="size-5" aria-hidden /> Book This Room
            </Button>
            <ul className="mt-5 space-y-2 text-xs text-muted-foreground">
              <li className="flex items-center gap-2">
                <BadgeCheck className="size-4 text-emerald-600" aria-hidden />
                Best price guaranteed — no hidden charges at check-in
              </li>
              <li className="flex items-center gap-2">
                <ShieldCheck className="size-4 text-emerald-600" aria-hidden />
                Free cancellation up to 24 hours before check-in
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
