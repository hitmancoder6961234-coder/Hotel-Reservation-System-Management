import "@testing-library/jest-dom/vitest";
import { afterEach, vi } from "vitest";
import { cleanup } from "@testing-library/react";

// jsdom does not implement these browser APIs used by the app.
window.scrollTo = vi.fn();
Element.prototype.scrollIntoView = vi.fn();

afterEach(() => {
  cleanup();
  window.location.hash = "";
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});
