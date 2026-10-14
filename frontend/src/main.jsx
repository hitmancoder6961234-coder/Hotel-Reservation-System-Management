import { createRoot } from "react-dom/client";
// Fonts: same families as the original (Inter + Playfair Display), self-hosted.
import "@fontsource-variable/inter";
import "@fontsource-variable/playfair-display";
import "@fontsource-variable/playfair-display/wght-italic.css";
import "./styles/fonts.css";
import "./styles/globals.css";
import { StayEaseApp } from "@/components/stayease/stayease-app";
import { Toaster } from "@/components/ui/toaster";

// The original layout rendered <StayEaseApp /> followed by <Toaster />,
// without React StrictMode (reactStrictMode: false) - kept identical.
createRoot(document.getElementById("root")).render(
  <>
    <StayEaseApp />
    <Toaster />
  </>
);
