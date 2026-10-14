/**
 * StayEase — Hash-based view router.
 * The app is served entirely from the "/" route; the five "pages"
 * of the plan (Home, Rooms, Room Details, Booking, Confirmation)
 * are hash-routed views, so browser back/forward and refresh both work.
 *
 * Examples:
 *   #/                       → home
 *   #/rooms                  → rooms list (optional ?checkIn=&checkOut=&guests=)
 *   #/rooms/101              → room details
 *   #/booking?room=101&...   → booking form
 *   #/confirmation/STY-…     → confirmation
 */
import { useCallback, useEffect, useState } from "react";
function readHash() {
  const raw = typeof window !== "undefined" ? window.location.hash.replace(/^#/, "") : "";
  const clean = raw || "/";
  const [pathPart, queryPart] = clean.split("?");
  const segments = pathPart.split("/").filter(Boolean);
  return {
    path: pathPart || "/",
    segments,
    query: new URLSearchParams(queryPart || ""),
  };
}
/** Programmatic navigation: navigate("/rooms/101"). */
export function navigate(to) {
  if (typeof window === "undefined") return;
  window.location.hash = to.startsWith("/") ? to : `/${to}`;
}
export function useHashRoute() {
  const [route, setRoute] = useState(() => readHash());
  useEffect(() => {
    const onChange = () => setRoute(readHash());
    window.addEventListener("hashchange", onChange);
    return () => window.removeEventListener("hashchange", onChange);
  }, []);
  const go = useCallback((to) => navigate(to), []);
  return { ...route, go };
}
