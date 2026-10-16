package com.stayease.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.stayease.export.BookingsWorkbook;
import com.stayease.model.AdminBooking;
import com.stayease.security.AdminAuth;
import com.stayease.service.ApiException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administrator endpoints (session cookie: HttpOnly, Secure, SameSite=Strict - see META-INF/context.xml):
 * <ul>
 *   <li>POST /api/admin/login {username, password} - starts the admin session</li>
 *   <li>POST /api/admin/logout - ends it</li>
 *   <li>GET  /api/admin/me - {"admin": {"username"}} or {"admin": null}</li>
 *   <li>GET  /api/admin/bookings - every booking (401 without a session)</li>
 *   <li>GET  /api/admin/bookings.xlsx - the same data as an Excel workbook with charts (401 without a session)</li>
 * </ul>
 */
@WebServlet("/api/admin/*")
public class AdminServlet extends ApiServlet {

  static final String SESSION_USER = "adminUser";
  static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final int SESSION_SECONDS = 30 * 60;

  private AdminAuth auth() {
    return (AdminAuth) getServletContext().getAttribute(AdminAuth.ATTRIBUTE);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String[] parts = segments(request);
    if (parts.length == 1 && parts[0].equals("login")) {
      run(request, response, "Login failed.", () -> login(request, response));
    } else if (parts.length == 1 && parts[0].equals("logout")) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
      Json.write(response, 200, Map.of("ok", true));
    } else if (parts.length == 1 && (parts[0].equals("me") || parts[0].startsWith("bookings"))) {
      super.doPost(request, response); // 405
    } else {
      Json.error(response, 404, "Not found.");
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String[] parts = segments(request);
    if (parts.length != 1) {
      Json.error(response, 404, "Not found.");
      return;
    }
    switch (parts[0]) {
      case "me" -> {
        Map<String, Object> body = new HashMap<>();
        String admin = currentAdmin(request);
        body.put("admin", admin == null ? null : Map.of("username", admin));
        Json.write(response, 200, body);
      }
      case "bookings" -> {
        if (!requireAdmin(request, response)) {
          return;
        }
        run(request, response, "Failed to load bookings.", () -> {
          List<AdminBooking> all = services().bookings().listAll();
          Json.write(response, 200, Map.of("total", all.size(), "bookings", all));
        });
      }
      case "bookings.xlsx" -> {
        if (!requireAdmin(request, response)) {
          return;
        }
        run(request, response, "Failed to create the Excel file.", () -> {
          List<AdminBooking> all = services().bookings().listAll();
          byte[] bytes = BookingsWorkbook.build(all, LocalDateTime.now(ZoneOffset.UTC));
          response.setStatus(200);
          response.setContentType(XLSX);
          response.setHeader(
              "Content-Disposition", "attachment; filename=\"stayease-bookings-" + LocalDate.now(ZoneOffset.UTC) + ".xlsx\"");
          response.setContentLength(bytes.length);
          response.getOutputStream().write(bytes);
        });
      }
      default -> Json.error(response, 404, "Not found.");
    }
  }

  private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
    JsonNode body = Json.readObject(request);
    if (body == null) {
      throw new ApiException(400, "Invalid request body.");
    }
    AdminAuth auth = auth();
    if (auth == null || !auth.isConfigured()) {
      throw new ApiException(503, "Admin login is not configured on this server.");
    }
    if (auth.isLocked()) {
      throw new ApiException(429, "Too many failed attempts. Please try again later.");
    }
    if (!auth.authenticate(text(body, "username"), text(body, "password"))) {
      throw new ApiException(401, "Invalid username or password.");
    }
    HttpSession old = request.getSession(false);
    if (old != null) {
      old.invalidate(); // a fresh session id after login (no session fixation)
    }
    HttpSession session = request.getSession(true);
    session.setAttribute(SESSION_USER, auth.username());
    session.setMaxInactiveInterval(SESSION_SECONDS);
    Json.write(response, 200, Map.of("admin", Map.of("username", auth.username())));
  }

  private static boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (currentAdmin(request) == null) {
      Json.error(response, 401, "Admin login required.");
      return false;
    }
    return true;
  }

  private static String currentAdmin(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object user = session == null ? null : session.getAttribute(SESSION_USER);
    return user instanceof String name ? name : null;
  }

  private static String text(JsonNode body, String field) {
    JsonNode node = body.get(field);
    return node != null && node.isTextual() ? node.textValue() : "";
  }
}
