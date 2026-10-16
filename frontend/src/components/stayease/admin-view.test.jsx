import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AdminView } from "./admin-view";
import { jsonResponse } from "@/test/fixtures";

const BOOKINGS = [
  {
    id: 1,
    bookingRef: "STY-2026-001",
    guestName: "Ada Lovelace",
    email: "ada@example.com",
    mobile: "9876543210",
    roomId: 102,
    roomName: "Luxury Room",
    checkIn: "2026-10-08",
    checkOut: "2026-10-10",
    guests: 3,
    nights: 2,
    totalPrice: 14000,
    status: "Confirmed",
    createdAt: "2026-10-01T10:15:30Z",
  },
  {
    id: 2,
    bookingRef: "STY-2026-002",
    guestName: "Grace Hopper",
    email: "grace@example.com",
    mobile: "2536897570",
    roomId: 103,
    roomName: "Suite Room",
    checkIn: "2026-10-09",
    checkOut: "2026-10-11",
    guests: 3,
    nights: 2,
    totalPrice: 20000,
    status: "Confirmed",
    createdAt: "2026-10-02T09:00:00Z",
  },
];

/** Routes the admin API calls of one test. */
function stubApi({ me = null, login, bookings = { total: BOOKINGS.length, bookings: BOOKINGS } }) {
  const fetchMock = vi.fn((url) => {
    if (url === "/api/admin/me") return jsonResponse({ admin: me ? { username: me } : null });
    if (url === "/api/admin/login") return login();
    if (url === "/api/admin/bookings") return jsonResponse(bookings);
    if (url === "/api/admin/logout") return jsonResponse({ ok: true });
    return jsonResponse({ error: "unexpected " + url }, 500);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("AdminView", () => {
  it("shows the login form when nobody is signed in and the bookings are not requested", async () => {
    const fetchMock = stubApi({});
    render(<AdminView />);
    expect(await screen.findByRole("heading", { name: "Admin Login" })).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith("/api/admin/bookings", expect.anything());
  });

  it("shows the server's message for wrong credentials", async () => {
    stubApi({ login: () => jsonResponse({ error: "Invalid username or password." }, 401) });
    render(<AdminView />);
    await userEvent.type(await screen.findByLabelText("Admin ID"), "ham");
    await userEvent.type(screen.getByLabelText("Password"), "wrong");
    await userEvent.click(screen.getByRole("button", { name: /Sign in/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password.");
    expect(screen.queryByText(/Total bookings/)).not.toBeInTheDocument();
  });

  it("asks for both fields before calling the server", async () => {
    const fetchMock = stubApi({});
    render(<AdminView />);
    await userEvent.click(await screen.findByRole("button", { name: /Sign in/ }));
    expect(screen.getByRole("alert")).toHaveTextContent("Please enter your admin ID and password.");
    expect(fetchMock).not.toHaveBeenCalledWith("/api/admin/login", expect.anything());
  });

  it("signs in and lists every booking with the dashboard fields in order", async () => {
    const fetchMock = stubApi({ login: () => jsonResponse({ admin: { username: "ham" } }) });
    render(<AdminView />);
    await userEvent.type(await screen.findByLabelText("Admin ID"), "ham");
    await userEvent.type(screen.getByLabelText("Password"), "right");
    await userEvent.click(screen.getByRole("button", { name: /Sign in/ }));

    expect(await screen.findByRole("heading", { name: "Total bookings: 2" })).toBeInTheDocument();
    const loginCall = fetchMock.mock.calls.find(([url]) => url === "/api/admin/login");
    expect(JSON.parse(loginCall[1].body)).toEqual({ username: "ham", password: "right" });

    const first = screen.getByRole("region", { name: "Booking 1" });
    expect(first).toHaveTextContent("BOOKING 1");
    const labels = Array.from(first.querySelectorAll("dt")).map((n) => n.textContent);
    expect(labels).toEqual([
      "id", "bookingRef", "guestName", "email", "mobile", "roomId", "roomName", "checkIn",
      "checkOut", "guests", "nights", "totalPrice", "status", "createdAt",
    ]);
    expect(first).toHaveTextContent("STY-2026-001");
    expect(first).toHaveTextContent("Ada Lovelace");
    expect(first).toHaveTextContent("₹14,000");
    expect(screen.getByRole("region", { name: "Booking 2" })).toHaveTextContent("Grace Hopper");
    expect(screen.getByRole("button", { name: /Download Excel/ })).toBeInTheDocument();
  });

  it("goes straight to the dashboard when a session already exists, and logs out", async () => {
    const fetchMock = stubApi({ me: "ham" });
    render(<AdminView />);
    expect(await screen.findByRole("heading", { name: "Total bookings: 2" })).toBeInTheDocument();
    expect(screen.getByText("Signed in as ham")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: /Log out/ }));
    expect(await screen.findByRole("heading", { name: "Admin Login" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/admin/logout", expect.objectContaining({ method: "POST" }));
  });

  it("falls back to the login form when the session has expired", async () => {
    const fetchMock = vi.fn((url) =>
      url === "/api/admin/me"
        ? jsonResponse({ admin: { username: "ham" } })
        : jsonResponse({ error: "Admin login required." }, 401)
    );
    vi.stubGlobal("fetch", fetchMock);
    render(<AdminView />);
    expect(await screen.findByRole("heading", { name: "Admin Login" })).toBeInTheDocument();
  });

  it("shows an empty state when there are no bookings", async () => {
    stubApi({ me: "ham", bookings: { total: 0, bookings: [] } });
    render(<AdminView />);
    expect(await screen.findByText("There are no bookings yet.")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole("heading", { name: "Total bookings: 0" })).toBeInTheDocument());
  });
});
