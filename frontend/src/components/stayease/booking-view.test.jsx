import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BookingView } from "./booking-view";
import { Toaster } from "@/components/ui/toaster";
import { ROOMS, jsonResponse } from "@/test/fixtures";

function future(days) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

function setup(props = {}) {
  return render(
    <>
      <BookingView rooms={ROOMS} roomId={102} {...props} />
      <Toaster />
    </>
  );
}

describe("BookingView", () => {
  it("asks the guest to pick a room first when none is selected", () => {
    render(<BookingView rooms={ROOMS} />);
    expect(screen.getByText("Select a Room First")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Browse Rooms" })).toBeInTheDocument();
  });

  it("disables 'Confirm Booking' and shows the idle hint until valid dates are chosen", () => {
    setup();
    expect(screen.getByRole("button", { name: "Confirm Booking" })).toBeDisabled();
    expect(screen.getByText("Select valid dates to check availability")).toBeInTheDocument();
  });

  it("shows field-level messages when the form is submitted empty", async () => {
    const { container } = setup();
    fireEvent.submit(container.querySelector("form"));
    expect(await screen.findByText("Please enter your name.")).toBeInTheDocument();
    expect(screen.getByText("Please enter your email.")).toBeInTheDocument();
    expect(screen.getByText("Please enter your mobile number.")).toBeInTheDocument();
    expect(screen.getByText("Please select check-in date.")).toBeInTheDocument();
  });

  it("checks availability, calculates the total live and submits the booking", async () => {
    const checkIn = future(10);
    const checkOut = future(13);
    const fetchMock = vi.fn((url, options) => {
      if (String(url).includes("/availability")) return jsonResponse({ available: true });
      if (url === "/api/bookings" && options?.method === "POST") {
        return jsonResponse({ booking: { bookingRef: "STY-2026-001" } }, 201);
      }
      return jsonResponse({ error: "unexpected" }, 500);
    });
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    setup({ initialCheckIn: checkIn, initialCheckOut: checkOut, initialGuests: 2 });

    await user.type(screen.getByPlaceholderText("e.g. Ahmed Khan"), "Priya Mehta");
    await user.type(screen.getByPlaceholderText("you@example.com"), "priya@example.com");
    await user.type(screen.getByPlaceholderText("+91 98765 43210"), "+91 98200 12345");

    expect(await screen.findByText("Available for your selected dates")).toBeInTheDocument();
    expect(screen.getAllByText("₹21,000").length).toBeGreaterThan(0); // 3 nights x ₹7,000

    const confirm = screen.getByRole("button", { name: "Confirm Booking" });
    await waitFor(() => expect(confirm).toBeEnabled());
    await user.click(confirm);

    await waitFor(() => expect(window.location.hash).toBe("#/confirmation/STY-2026-001"));
    const post = fetchMock.mock.calls.find(([u]) => u === "/api/bookings");
    expect(JSON.parse(post[1].body)).toEqual({
      guestName: "Priya Mehta",
      email: "priya@example.com",
      mobile: "+91 98200 12345",
      roomId: 102,
      checkIn,
      checkOut,
      guests: 2,
    });
  });

  it("shows the unavailable message returned by the API and keeps the form locked", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => jsonResponse({ available: false, reason: "This room is currently unavailable for booking." }))
    );
    setup({ initialCheckIn: future(10), initialCheckOut: future(12), initialGuests: 1 });
    expect(
      await screen.findByText("This room is currently unavailable for booking.")
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Confirm Booking" })).toBeDisabled();
  });
});
