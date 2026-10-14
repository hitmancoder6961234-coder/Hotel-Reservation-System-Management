import { Hotel, MapPin, Phone, Mail, Clock } from "lucide-react";
import { navigate } from "@/lib/use-hash-route";
export function Footer() {
  return (
    <footer className="mt-auto bg-[oklch(0.27_0.04_50)] text-[oklch(0.92_0.02_85)]">
      <div className="mx-auto grid max-w-6xl gap-10 px-4 py-14 sm:px-6 md:grid-cols-2 lg:grid-cols-4">
        {/* Brand */}
        <div>
          <div className="flex items-center gap-2.5">
            <span className="flex size-9 items-center justify-center rounded-[15px] bg-gradient-to-br from-[oklch(0.75_0.17_70)] to-[oklch(0.63_0.19_45)] text-white">
              <Hotel className="size-5" />
            </span>
            <span className="font-heading text-xl font-semibold">
              Stay<span className="text-[oklch(0.8_0.15_75)]">Ease</span>
            </span>
          </div>
          <p className="mt-4 text-sm leading-relaxed text-[oklch(0.75_0.03_80)]">
            A modern stay experience with warm rooms, honest pricing and an easy booking flow —
            reserve your room in under a minute.
          </p>
        </div>

        {/* Quick links */}
        <nav aria-label="Footer navigation">
          <h3 className="font-heading text-base font-semibold text-white">Quick Links</h3>
          <ul className="mt-4 space-y-2.5 text-sm">
            {[
              { label: "Home", target: "/" },
              { label: "Rooms", target: "/rooms" },
              { label: "About", target: "/#about" },
              { label: "Contact", target: "/#contact" },
              { label: "Book a Stay", target: "/rooms" },
            ].map((link) => (
              <li key={link.label}>
                <button
                  onClick={() => navigate(link.target)}
                  className="text-[oklch(0.75_0.03_80)] transition-colors hover:text-white"
                >
                  {link.label}
                </button>
              </li>
            ))}
          </ul>
        </nav>

        {/* Contact */}
        <div>
          <h3 className="font-heading text-base font-semibold text-white">Contact</h3>
          <ul className="mt-4 space-y-3 text-sm text-[oklch(0.75_0.03_80)]">
            <li className="flex items-start gap-2.5">
              <MapPin className="mt-0.5 size-4 shrink-0" />
              <span>12 Palm Avenue, Marine Drive, Mumbai 400020</span>
            </li>
            <li className="flex items-center gap-2.5">
              <Phone className="size-4 shrink-0" />
              <span>+91 98200 12345</span>
            </li>
            <li className="flex items-center gap-2.5">
              <Mail className="size-4 shrink-0" />
              <span>hello@stayease.example</span>
            </li>
          </ul>
        </div>

        {/* Hours */}
        <div>
          <h3 className="font-heading text-base font-semibold text-white">Front Desk</h3>
          <ul className="mt-4 space-y-3 text-sm text-[oklch(0.75_0.03_80)]">
            <li className="flex items-center gap-2.5">
              <Clock className="size-4 shrink-0" />
              <span>Reception open 24 × 7</span>
            </li>
            <li className="flex items-center gap-2.5">
              <Clock className="size-4 shrink-0" />
              <span>Check-in from 2:00 PM</span>
            </li>
            <li className="flex items-center gap-2.5">
              <Clock className="size-4 shrink-0" />
              <span>Check-out until 11:00 AM</span>
            </li>
          </ul>
        </div>
      </div>

      <div className="border-t border-white/10">
        <p className="mx-auto max-w-6xl px-4 py-5 text-center text-xs text-[oklch(0.68_0.03_80)] sm:px-6">
          © 2026 StayEase Hotel. All rights reserved. Prices shown in Indian Rupees (₹).
        </p>
      </div>
    </footer>
  );
}
