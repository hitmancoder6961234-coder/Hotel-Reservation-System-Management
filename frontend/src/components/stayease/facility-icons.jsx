/**
 * StayEase — facility name → icon mapping (lucide).
 * Used by room cards, room details, facilities section.
 */
import {
  Snowflake,
  Wifi,
  Tv,
  Coffee,
  Bell,
  Sun,
  Mountain,
  Wine,
  Waves,
  UtensilsCrossed,
  Dumbbell,
  Car,
  ConciergeBell,
  Sparkles,
  ShieldCheck,
  Bath,
  BedDouble,
} from "lucide-react";
const FACILITY_ICONS = {
  AC: Snowflake,
  WiFi: Wifi,
  TV: Tv,
  Breakfast: Coffee,
  "Room Service": ConciergeBell,
  Balcony: Sun,
  "Premium View": Mountain,
  "Mini Bar": Wine,
  Pool: Waves,
  Restaurant: UtensilsCrossed,
  Gym: Dumbbell,
  Parking: Car,
  Spa: Sparkles,
  Security: ShieldCheck,
  Bathtub: Bath,
  Bed: BedDouble,
  Bell: Bell,
};
/** Get the icon component for a facility name (falls back to Sparkles). */
export function getFacilityIcon(name) {
  return FACILITY_ICONS[name] ?? Sparkles;
}
