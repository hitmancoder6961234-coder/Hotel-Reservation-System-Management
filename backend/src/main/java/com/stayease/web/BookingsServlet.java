package com.stayease.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.stayease.service.ApiException;
import com.stayease.service.BookingRequest;
import com.stayease.validation.Rules;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Booking endpoints (paths and JSON unchanged from the original API):
 * <ul>
 *   <li>POST /api/bookings - create a booking</li>
 *   <li>GET /api/bookings/:ref - booking details by reference, e.g. STY-2026-001</li>
 * </ul>
 */
@WebServlet("/api/bookings/*")
public class BookingsServlet extends ApiServlet {

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    if (segments(request).length != 0) {
      super.doPost(request, response); // 405: POST is only allowed on the collection
      return;
    }
    run(request, response, "Failed to create booking.", () -> {
      JsonNode body = Json.readObject(request);
      if (body == null) {
        throw new ApiException(400, "Invalid request body.");
      }
      BookingRequest booking = new BookingRequest(
          text(body, "guestName"), text(body, "email"), text(body, "mobile"), number(body, "roomId"),
          text(body, "checkIn"), text(body, "checkOut"), number(body, "guests"));
      Json.write(response, 201, Map.of("booking", services().bookings().create(booking)));
    });
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String[] parts = segments(request);
    if (parts.length == 0) {
      super.doGet(request, response); // 405: there is no "list all bookings" endpoint
      return;
    }
    if (parts.length > 1) {
      Json.error(response, 404, "Not found.");
      return;
    }
    run(request, response, "Failed to load booking.",
        () -> Json.write(response, 200, Map.of("booking", services().bookings().getBooking(parts[0]))));
  }

  /** String(body[field] ?? "") */
  private static String text(JsonNode body, String field) {
    JsonNode node = body.get(field);
    if (node == null || node.isNull()) {
      return "";
    }
    return node.isValueNode() ? node.asText() : node.toString();
  }

  /** Number(body[field]) */
  private static double number(JsonNode body, String field) {
    JsonNode node = body.get(field);
    if (node == null) {
      return Rules.toNumber(null, false);
    }
    if (node.isNull()) {
      return Rules.toNumber(null, true);
    }
    if (node.isNumber()) {
      return node.doubleValue();
    }
    if (node.isBoolean()) {
      return Rules.toNumber(node.booleanValue(), true);
    }
    return Rules.toNumber(node.isTextual() ? node.textValue() : node.toString(), true);
  }
}
