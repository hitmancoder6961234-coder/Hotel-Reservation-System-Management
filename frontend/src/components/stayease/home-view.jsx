import { useState } from "react";
import {
  Star,
  ArrowRight,
  Waves,
  UtensilsCrossed,
  Dumbbell,
  Car,
  Wifi,
  ConciergeBell,
  MapPin,
  Phone,
  Mail,
} from "lucide-react";
import { todayStr, validateDates } from "@/lib/stayease";
import { navigate } from "@/lib/use-hash-route";
import { RoomCard } from "./room-card";
import { Button } from "@/components/ui/button";
const FACILITIES = [
  {
    Icon: Waves,
    title: "Rooftop Pool",
    text: "Temperature-controlled pool with skyline views and poolside service.",
  },
  {
    Icon: UtensilsCrossed,
    title: "Restaurant",
    text: "Multi-cuisine dining from 7 AM to 11 PM, every day of the week.",
  },
  {
    Icon: Dumbbell,
    title: "Modern Gym",
    text: "Fully-equipped fitness centre open round the clock for guests.",
  },
  {
    Icon: Car,
    title: "Free Parking",
    text: "Complimentary secure on-site parking for every in-house guest.",
  },
  {
    Icon: Wifi,
    title: "High-Speed WiFi",
    text: "Fast, free fibre internet in every room and all public areas.",
  },
  {
    Icon: ConciergeBell,
    title: "24 × 7 Room Service",
    text: "Our front desk and room service never sleep — just call down.",
  },
];
const REVIEWS = [
  {
    quote:
      "We booked the Luxury Room for our anniversary and loved every minute. The booking took two minutes on the website and the breakfast was excellent.",
    name: "Priya & Arjun Mehta",
    place: "Mumbai",
    initials: "PM",
  },
  {
    quote:
      "StayEase made a short work trip feel effortless. Clean rooms, honest pricing on the site, and the date-based availability check actually works.",
    name: "Daniel D'Souza",
    place: "Goa",
    initials: "DD",
  },
  {
    quote:
      "The Suite Room balcony view is stunning at sunset. Booking was simple, the confirmation came instantly, and check-in was smooth.",
    name: "Sneha Kulkarni",
    place: "Pune",
    initials: "SK",
  },
];
export function HomeView({ rooms }) {
  const today = todayStr();
  const [checkIn, setCheckIn] = useState("");
  const [checkOut, setCheckOut] = useState("");
  const [guests, setGuests] = useState(2);
  const [error, setError] = useState("");
  const onSearch = () => {
    const dateError = validateDates(checkIn, checkOut);
    if (dateError) {
      setError(dateError);
      return;
    }
    setError("");
    const params = new URLSearchParams({ checkIn, checkOut, guests: String(guests) });
    navigate(`/rooms?${params.toString()}`);
  };
  return (
    <div className="mx-auto w-full max-w-6xl px-4 sm:px-6">
      {/* ============ HERO ============ */}
      <section
        className="relative mt-4 overflow-hidden rounded-[25px] shadow-soft-lg sm:mt-6"
        aria-label="Welcome to StayEase"
      >
        <img
          src="/images/hero.jpg"
          alt="StayEase hotel exterior with a lit swimming pool at dusk"
          className="h-[440px] w-full object-cover md:h-[540px]"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[oklch(0.2_0.04_50/0.92)] via-[oklch(0.25_0.04_50/0.45)] to-transparent" />
        <div className="absolute inset-x-0 bottom-0 p-6 sm:p-10 md:p-14">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-4 py-1.5 text-sm font-medium text-white backdrop-blur-sm">
            <Star className="size-4 fill-amber-400 text-amber-400" aria-hidden />
            4.9 Guest Rating · Marine Drive, Mumbai
          </span>
          <h1 className="mt-5 max-w-2xl font-heading text-4xl font-semibold leading-tight text-white sm:text-5xl md:text-6xl">
            A Warm Stay, <span className="italic text-[oklch(0.85_0.13_80)]">Made Easy</span>
          </h1>
          <p className="mt-4 max-w-xl text-base leading-relaxed text-white/85 sm:text-lg">
            Comfortable rooms, honest prices and a booking that takes less than a minute — pick your
            dates, choose a room, and we will keep it ready.
          </p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Button className="btn-gradient h-12 px-8 text-base" onClick={() => navigate("/rooms")}>
              Explore Rooms <ArrowRight className="size-4" />
            </Button>
            <Button
              variant="outline"
              className="h-12 rounded-[25px] border-white/50 bg-white/10 px-8 text-base text-white backdrop-blur-sm hover:bg-white/20 hover:text-white"
              onClick={() =>
                document.getElementById("about")?.scrollIntoView({ behavior: "smooth" })
              }
            >
              About the Hotel
            </Button>
          </div>
        </div>
      </section>

      {/* ============ SEARCH ============ */}
      <section className="relative z-10 -mt-12 sm:-mt-14" aria-label="Search available rooms">
        <div className="rounded-[25px] bg-card p-5 shadow-soft-lg sm:p-6">
          <div className="grid gap-4 md:grid-cols-4">
            <label className="flex flex-col gap-1.5 text-sm font-medium text-foreground">
              Check-in
              <input
                type="date"
                value={checkIn}
                min={today}
                onChange={(e) => setCheckIn(e.target.value)}
                className="h-12 rounded-[15px] border border-input bg-background px-4 text-sm text-foreground shadow-sm outline-none transition focus:border-ring focus:ring-[3px] focus:ring-ring/30"
              />
            </label>
            <label className="flex flex-col gap-1.5 text-sm font-medium text-foreground">
              Check-out
              <input
                type="date"
                value={checkOut}
                min={checkIn || today}
                onChange={(e) => setCheckOut(e.target.value)}
                className="h-12 rounded-[15px] border border-input bg-background px-4 text-sm text-foreground shadow-sm outline-none transition focus:border-ring focus:ring-[3px] focus:ring-ring/30"
              />
            </label>
            <label className="flex flex-col gap-1.5 text-sm font-medium text-foreground">
              Guests
              <select
                value={guests}
                onChange={(e) => setGuests(Number(e.target.value))}
                className="h-12 rounded-[15px] border border-input bg-background px-4 text-sm text-foreground shadow-sm outline-none transition focus:border-ring focus:ring-[3px] focus:ring-ring/30"
              >
                {[1, 2, 3, 4].map((n) => (
                  <option key={n} value={n}>
                    {n} {n === 1 ? "Guest" : "Guests"}
                  </option>
                ))}
              </select>
            </label>
            <div className="flex items-end">
              <Button className="btn-gradient h-12 w-full border-0 text-base" onClick={onSearch}>
                Search Rooms
              </Button>
            </div>
          </div>
          {error && (
            <p className="mt-3 text-sm font-medium text-destructive" role="alert">
              {error}
            </p>
          )}
        </div>
      </section>

      {/* ============ FEATURED ROOMS ============ */}
      <section className="py-16 sm:py-20" aria-label="Featured rooms">
        <div className="text-center">
          <p className="text-sm font-semibold uppercase tracking-widest text-primary">
            Stay With Us
          </p>
          <h2 className="mt-2 font-heading text-3xl font-semibold sm:text-4xl">Featured Rooms</h2>
          <p className="mx-auto mt-3 max-w-xl text-muted-foreground">
            Three room styles, one promise of comfort — every room includes daily housekeeping and
            complimentary high-speed WiFi.
          </p>
        </div>
        <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {rooms.map((room) => (
            <RoomCard key={room.id} room={room} />
          ))}
        </div>
        <div className="mt-10 text-center">
          <Button
            variant="outline"
            className="h-12 rounded-[25px] px-8 text-base"
            onClick={() => navigate("/rooms")}
          >
            View All Rooms <ArrowRight className="size-4" />
          </Button>
        </div>
      </section>

      {/* ============ FACILITIES ============ */}
      <section className="rounded-[25px] bg-secondary/70 p-6 sm:p-10" aria-label="Hotel facilities">
        <div className="text-center">
          <p className="text-sm font-semibold uppercase tracking-widest text-primary">
            Hotel Facilities
          </p>
          <h2 className="mt-2 font-heading text-3xl font-semibold sm:text-4xl">
            Everything You Need
          </h2>
        </div>
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {FACILITIES.map(({ Icon, title, text }) => (
            <div
              key={title}
              className="rounded-[20px] bg-card p-6 shadow-soft transition-all duration-300 hover:-translate-y-1 hover:shadow-soft-lg"
            >
              <span className="flex size-12 items-center justify-center rounded-[15px] bg-gradient-to-br from-[oklch(0.75_0.17_70)] to-[oklch(0.63_0.19_45)] text-white shadow-soft">
                <Icon className="size-6" aria-hidden />
              </span>
              <h3 className="mt-4 font-heading text-lg font-semibold">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ============ ABOUT ============ */}
      <section id="about" className="scroll-mt-24 py-16 sm:py-20" aria-label="About StayEase">
        <div className="grid items-center gap-10 lg:grid-cols-2">
          <div className="relative">
            <img
              src="/images/lobby.jpg"
              alt="The warm, elegantly lit StayEase hotel lobby lounge"
              className="aspect-[4/3] w-full rounded-[20px] object-cover shadow-soft"
              loading="lazy"
            />
            <div className="absolute -bottom-5 -right-3 rounded-[20px] bg-card px-6 py-4 shadow-soft-lg sm:-right-5">
              <p className="font-heading text-3xl font-semibold text-primary">10+</p>
              <p className="text-sm text-muted-foreground">Years of warm hospitality</p>
            </div>
          </div>
          <div>
            <p className="text-sm font-semibold uppercase tracking-widest text-primary">
              About StayEase
            </p>
            <h2 className="mt-2 font-heading text-3xl font-semibold sm:text-4xl">
              A Small Hotel With a Big Heart
            </h2>
            <p className="mt-4 leading-relaxed text-muted-foreground">
              StayEase began in 2016 as a family-run guest house on Marine Drive and grew into a
              3-floor boutique hotel without losing its personal touch. Every room is cleaned and
              inspected daily, and our front desk answers around the clock — whether you need an
              extra pillow at midnight or a taxi at dawn.
            </p>
            <p className="mt-4 leading-relaxed text-muted-foreground">
              We believe booking a room should be as relaxing as staying in one. That is why our
              website shows live availability, calculates your total before you confirm, and never
              adds hidden charges at check-in.
            </p>
            <div className="mt-7 grid grid-cols-3 gap-4">
              {[
                { value: "4.9", label: "Guest rating" },
                { value: "24 × 7", label: "Front desk" },
                { value: "2 min", label: "Average booking" },
              ].map((stat) => (
                <div key={stat.label} className="rounded-[20px] bg-secondary/70 p-4 text-center">
                  <p className="font-heading text-2xl font-semibold text-primary">{stat.value}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{stat.label}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ============ REVIEWS ============ */}
      <section className="pb-16 sm:pb-20" aria-label="Customer reviews">
        <div className="text-center">
          <p className="text-sm font-semibold uppercase tracking-widest text-primary">
            Guest Stories
          </p>
          <h2 className="mt-2 font-heading text-3xl font-semibold sm:text-4xl">
            What Our Guests Say
          </h2>
        </div>
        <div className="mt-10 grid gap-6 md:grid-cols-3">
          {REVIEWS.map((review) => (
            <figure
              key={review.name}
              className="flex flex-col rounded-[20px] bg-card p-6 shadow-soft transition-all duration-300 hover:-translate-y-1 hover:shadow-soft-lg"
            >
              <div className="flex gap-1" aria-label="5 out of 5 stars">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Star key={i} className="size-4 fill-amber-400 text-amber-400" aria-hidden />
                ))}
              </div>
              <blockquote className="mt-4 flex-1 text-sm leading-relaxed text-foreground/90">
                “{review.quote}”
              </blockquote>
              <figcaption className="mt-5 flex items-center gap-3">
                <span className="flex size-10 items-center justify-center rounded-full bg-gradient-to-br from-[oklch(0.75_0.17_70)] to-[oklch(0.63_0.19_45)] text-sm font-semibold text-white">
                  {review.initials}
                </span>
                <span>
                  <span className="block text-sm font-semibold">{review.name}</span>
                  <span className="block text-xs text-muted-foreground">{review.place}</span>
                </span>
              </figcaption>
            </figure>
          ))}
        </div>
      </section>

      {/* ============ CONTACT ============ */}
      <section
        id="contact"
        className="scroll-mt-24 pb-16 sm:pb-20"
        aria-label="Contact information"
      >
        <div className="rounded-[25px] bg-[oklch(0.27_0.04_50)] p-6 sm:p-10">
          <div className="text-center">
            <p className="text-sm font-semibold uppercase tracking-widest text-[oklch(0.8_0.15_75)]">
              Contact Us
            </p>
            <h2 className="mt-2 font-heading text-3xl font-semibold text-white sm:text-4xl">
              We Are Here For You
            </h2>
            <p className="mx-auto mt-3 max-w-lg text-sm text-[oklch(0.75_0.03_80)]">
              Questions about rooms, dates or group bookings? Reach us any time — the front desk
              never closes.
            </p>
          </div>
          <div className="mt-9 grid gap-5 md:grid-cols-3">
            {[
              {
                Icon: Phone,
                title: "Call Us",
                line1: "+91 98200 12345",
                line2: "24 × 7 reception",
              },
              {
                Icon: Mail,
                title: "Email Us",
                line1: "hello@stayease.example",
                line2: "Replies within a few hours",
              },
              {
                Icon: MapPin,
                title: "Visit Us",
                line1: "12 Palm Avenue, Marine Drive",
                line2: "Mumbai 400020",
              },
            ].map(({ Icon, title, line1, line2 }) => (
              <div
                key={title}
                className="rounded-[20px] bg-white/5 p-6 text-center transition-colors hover:bg-white/10"
              >
                <span className="mx-auto flex size-12 items-center justify-center rounded-[15px] bg-gradient-to-br from-[oklch(0.75_0.17_70)] to-[oklch(0.63_0.19_45)] text-white">
                  <Icon className="size-6" aria-hidden />
                </span>
                <h3 className="mt-4 font-heading text-lg font-semibold text-white">{title}</h3>
                <p className="mt-2 text-sm text-[oklch(0.85_0.02_80)]">{line1}</p>
                <p className="mt-1 text-xs text-[oklch(0.68_0.03_80)]">{line2}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
