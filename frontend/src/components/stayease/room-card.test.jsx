import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RoomCard } from "./room-card";
import { ROOMS } from "@/test/fixtures";

describe("RoomCard", () => {
  it("shows the room name, price, capacity and at most four facilities", () => {
    render(<RoomCard room={ROOMS[2]} />);
    expect(screen.getByRole("heading", { name: "Suite Room" })).toBeInTheDocument();
    expect(screen.getByText("₹10,000")).toBeInTheDocument();
    expect(screen.getByText("Up to 4 guests")).toBeInTheDocument();
    expect(screen.getByAltText("Suite Room at StayEase")).toHaveAttribute(
      "src",
      "/images/suite.jpg"
    );
    expect(screen.getAllByRole("listitem")).toHaveLength(4);
  });

  it("navigates to the details page and to the booking form with the search context", async () => {
    const user = userEvent.setup();
    render(<RoomCard room={ROOMS[0]} checkIn="2026-09-20" checkOut="2026-09-23" guests={2} />);
    await user.click(screen.getByRole("button", { name: "View Details" }));
    expect(window.location.hash).toBe("#/rooms/101");
    await user.click(screen.getByRole("button", { name: "Book Now" }));
    expect(window.location.hash).toBe(
      "#/booking?room=101&checkIn=2026-09-20&checkOut=2026-09-23&guests=2"
    );
  });
});
