import { Users } from "lucide-react";
import { formatINR, bookingUrl } from "@/lib/stayease";
import { navigate } from "@/lib/use-hash-route";
import { getFacilityIcon } from "./facility-icons";
import { Button } from "@/components/ui/button";
export function RoomCard({ room, checkIn, checkOut, guests }) {
  const facilityIcons = room.facilities.slice(0, 4).map((f) => ({
    name: f,
    Icon: getFacilityIcon(f),
  }));
  return (
    <article className="group flex flex-col overflow-hidden rounded-[20px] bg-card shadow-soft transition-all duration-300 hover:-translate-y-1.5 hover:shadow-soft-lg">
      {/* Room image */}
      <div className="relative overflow-hidden">
        <img
          src={room.image}
          alt={`${room.name} at StayEase`}
          className="aspect-[4/3] w-full object-cover transition-transform duration-500 group-hover:scale-105"
          loading="lazy"
        />
        <span className="absolute right-4 top-4 rounded-full bg-white/90 px-3.5 py-1.5 text-sm font-semibold text-primary shadow-soft backdrop-blur-sm">
          {formatINR(room.price)} <span className="font-normal text-muted-foreground">/ night</span>
        </span>
      </div>

      {/* Content */}
      <div className="flex flex-1 flex-col p-5">
        <div className="flex items-start justify-between gap-3">
          <h3 className="font-heading text-xl font-semibold text-foreground">{room.name}</h3>
        </div>

        <p className="mt-1.5 flex items-center gap-1.5 text-sm text-muted-foreground">
          <Users className="size-4" aria-hidden />
          Up to {room.capacity} guests
        </p>

        <ul className="mt-4 flex flex-wrap gap-2" aria-label={`${room.name} facilities`}>
          {facilityIcons.map(({ name, Icon }) => (
            <li
              key={name}
              className="flex items-center gap-1.5 rounded-full bg-secondary px-3 py-1.5 text-xs font-medium text-secondary-foreground"
            >
              <Icon className="size-3.5 text-primary" aria-hidden />
              {name}
            </li>
          ))}
          {room.facilities.length > 4 && (
            <li className="flex items-center rounded-full bg-secondary px-3 py-1.5 text-xs font-medium text-secondary-foreground">
              +{room.facilities.length - 4} more
            </li>
          )}
        </ul>

        {/* Actions */}
        <div className="mt-auto flex gap-3 pt-5">
          <Button
            variant="outline"
            className="h-11 flex-1 rounded-[25px] border-border text-sm hover:bg-secondary"
            onClick={() => navigate(`/rooms/${room.id}`)}
          >
            View Details
          </Button>
          <Button
            className="btn-gradient h-11 flex-1 border-0 text-sm"
            onClick={() => navigate(bookingUrl(room.id, checkIn, checkOut, guests))}
          >
            Book Now
          </Button>
        </div>
      </div>
    </article>
  );
}
