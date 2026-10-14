import { describe, it, expect } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useHashRoute, navigate } from "@/lib/use-hash-route";

describe("useHashRoute", () => {
  it("defaults to the home route", () => {
    window.location.hash = "";
    const { result } = renderHook(() => useHashRoute());
    expect(result.current.path).toBe("/");
    expect(result.current.segments).toEqual([]);
  });

  it("parses path segments and query parameters", () => {
    window.location.hash = "#/booking?room=101&guests=2";
    const { result } = renderHook(() => useHashRoute());
    expect(result.current.path).toBe("/booking");
    expect(result.current.segments).toEqual(["booking"]);
    expect(result.current.query.get("room")).toBe("101");
    expect(result.current.query.get("guests")).toBe("2");
  });

  it("reacts to hashchange (back/forward and programmatic navigation)", async () => {
    window.location.hash = "";
    const { result } = renderHook(() => useHashRoute());
    await act(async () => {
      navigate("/rooms/102");
      window.dispatchEvent(new HashChangeEvent("hashchange"));
    });
    expect(result.current.segments).toEqual(["rooms", "102"]);
  });
});
