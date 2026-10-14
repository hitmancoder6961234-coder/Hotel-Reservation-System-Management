import { useState } from "react";
import { Hotel, Menu, X } from "lucide-react";
import { navigate } from "@/lib/use-hash-route";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
const LINKS = [
  { label: "Home", target: "/" },
  { label: "Rooms", target: "/rooms" },
  { label: "About", target: "/#about" },
  { label: "Contact", target: "/#contact" },
];
export function Navbar({ path }) {
  const [open, setOpen] = useState(false);
  const go = (target) => {
    setOpen(false);
    // Section link, e.g. "/#about"
    if (target.includes("#")) {
      const [route, section] = target.split("#");
      if (path === route || (route === "/" && path === "/")) {
        document.getElementById(section)?.scrollIntoView({ behavior: "smooth" });
        return;
      }
      navigate(route || "/");
      setTimeout(() => {
        document.getElementById(section)?.scrollIntoView({ behavior: "smooth" });
      }, 120);
      return;
    }
    navigate(target);
  };
  const isActive = (target) => {
    if (target === "/") return path === "/";
    if (target === "/rooms") return path.startsWith("/rooms");
    return false;
  };
  return (
    <header className="sticky top-0 z-50 border-b border-border/70 bg-[oklch(0.99_0.006_85/0.85)] backdrop-blur-md">
      <nav
        className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6"
        aria-label="Main navigation"
      >
        {/* Logo */}
        <button
          onClick={() => go("/")}
          className="flex items-center gap-2.5 outline-none"
          aria-label="StayEase home"
        >
          <span className="flex size-9 items-center justify-center rounded-[15px] bg-gradient-to-br from-[oklch(0.75_0.17_70)] to-[oklch(0.63_0.19_45)] text-white shadow-soft">
            <Hotel className="size-5" />
          </span>
          <span className="font-heading text-xl font-semibold tracking-tight text-foreground">
            Stay<span className="text-primary">Ease</span>
          </span>
        </button>

        {/* Desktop links */}
        <ul className="hidden items-center gap-1 md:flex">
          {LINKS.map((link) => (
            <li key={link.label}>
              <button
                onClick={() => go(link.target)}
                className={cn(
                  "rounded-[25px] px-4 py-2 text-sm font-medium transition-colors",
                  isActive(link.target)
                    ? "bg-secondary text-secondary-foreground"
                    : "text-muted-foreground hover:bg-secondary/70 hover:text-foreground"
                )}
              >
                {link.label}
              </button>
            </li>
          ))}
        </ul>

        <div className="hidden md:block">
          <Button className="btn-gradient h-10 px-6" onClick={() => go("/rooms")}>
            Book Now
          </Button>
        </div>

        {/* Mobile toggle */}
        <button
          className="flex size-10 items-center justify-center rounded-[15px] text-foreground hover:bg-secondary md:hidden"
          onClick={() => setOpen((v) => !v)}
          aria-label={open ? "Close menu" : "Open menu"}
          aria-expanded={open}
        >
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </nav>

      {/* Mobile menu */}
      {open && (
        <div className="border-t border-border/70 bg-[oklch(0.99_0.006_85/0.97)] px-4 pb-5 pt-2 backdrop-blur-md md:hidden">
          <ul className="flex flex-col gap-1">
            {LINKS.map((link) => (
              <li key={link.label}>
                <button
                  onClick={() => go(link.target)}
                  className={cn(
                    "w-full rounded-[15px] px-4 py-3 text-left text-sm font-medium transition-colors",
                    isActive(link.target)
                      ? "bg-secondary text-secondary-foreground"
                      : "text-muted-foreground hover:bg-secondary/70 hover:text-foreground"
                  )}
                >
                  {link.label}
                </button>
              </li>
            ))}
            <li className="pt-2">
              <Button className="btn-gradient h-11 w-full" onClick={() => go("/rooms")}>
                Book Now
              </Button>
            </li>
          </ul>
        </div>
      )}
    </header>
  );
}
