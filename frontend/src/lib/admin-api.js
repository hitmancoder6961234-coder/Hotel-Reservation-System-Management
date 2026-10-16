/**
 * StayEase admin API client. The login is an HttpOnly session cookie set by the backend, so every
 * call is same-origin and the browser sends the cookie by itself (no token is kept in JavaScript).
 */

/** Thrown when the server says the admin session is missing or expired (HTTP 401). */
export class AdminSessionError extends Error {}

async function readJson(res) {
  try {
    return await res.json();
  } catch {
    return null;
  }
}

/** The signed-in admin's username, or null when nobody is signed in. */
export async function fetchAdmin() {
  const res = await fetch("/api/admin/me", { credentials: "same-origin" });
  const data = await readJson(res);
  if (!res.ok) throw new Error(data?.error ?? "Could not reach the server.");
  return data?.admin?.username ?? null;
}

export async function loginAdmin(username, password) {
  const res = await fetch("/api/admin/login", {
    method: "POST",
    credentials: "same-origin",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  const data = await readJson(res);
  if (!res.ok) throw new Error(data?.error ?? "Login failed. Please try again.");
  return data.admin.username;
}

export async function logoutAdmin() {
  await fetch("/api/admin/logout", { method: "POST", credentials: "same-origin" });
}

/** { total, bookings: [...] } with the fields in dashboard order. */
export async function fetchAdminBookings() {
  const res = await fetch("/api/admin/bookings", { credentials: "same-origin" });
  if (res.status === 401) throw new AdminSessionError("Please sign in again.");
  const data = await readJson(res);
  if (!res.ok) throw new Error(data?.error ?? "Failed to load bookings.");
  return data;
}

/** Downloads the Excel workbook (bookings sheet + summary sheet with charts). */
export async function downloadBookingsExcel() {
  const res = await fetch("/api/admin/bookings.xlsx", { credentials: "same-origin" });
  if (res.status === 401) throw new AdminSessionError("Please sign in again.");
  if (!res.ok) throw new Error("Failed to create the Excel file.");
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `stayease-bookings-${new Date().toISOString().slice(0, 10)}.xlsx`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
