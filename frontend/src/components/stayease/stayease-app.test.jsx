import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { StayEaseApp } from "./stayease-app";
import { ROOMS, jsonResponse } from "@/test/fixtures";

describe("StayEaseApp", () => {
  it("loads the rooms from GET /api/rooms and renders the home page", async () => {
    const fetchMock = vi.fn(() => jsonResponse({ rooms: ROOMS }));
    vi.stubGlobal("fetch", fetchMock);
    render(<StayEaseApp />);
    expect(await screen.findByRole("heading", { name: "Featured Rooms" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/rooms");
    expect(screen.getByRole("heading", { name: "Deluxe Room" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Luxury Room" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Suite Room" })).toBeInTheDocument();
    expect(screen.getByText(/A Warm Stay,/)).toBeInTheDocument();
  });

  it("shows the original error screen and retries", async () => {
    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => jsonResponse({ error: "Failed to load rooms." }, 500))
      .mockImplementation(() => jsonResponse({ rooms: ROOMS }));
    vi.stubGlobal("fetch", fetchMock);
    render(<StayEaseApp />);
    expect(await screen.findByText("Something went wrong")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /Retry/ }));
    expect(await screen.findByRole("heading", { name: "Featured Rooms" })).toBeInTheDocument();
  });

  it("renders the rooms page filtered by guest capacity", async () => {
    window.location.hash = "#/rooms?checkIn=2026-09-20&checkOut=2026-09-23&guests=3";
    vi.stubGlobal(
      "fetch",
      vi.fn(() => jsonResponse({ rooms: ROOMS }))
    );
    render(<StayEaseApp />);
    expect(await screen.findByRole("heading", { name: "Our Rooms" })).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole("heading", { name: "Deluxe Room" })).not.toBeInTheDocument()
    );
    expect(screen.getByRole("heading", { name: "Luxury Room" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Suite Room" })).toBeInTheDocument();
  });

  it("shows 'Room not found' for an unknown room id", async () => {
    window.location.hash = "#/rooms/999";
    vi.stubGlobal(
      "fetch",
      vi.fn(() => jsonResponse({ rooms: ROOMS }))
    );
    render(<StayEaseApp />);
    expect(await screen.findByText("Room not found")).toBeInTheDocument();
  });
});
