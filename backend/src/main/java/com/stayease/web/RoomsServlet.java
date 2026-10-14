package com.stayease.web;

import com.stayease.validation.Rules;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Room endpoints (paths and JSON unchanged from the original API):
 * <ul>
 *   <li>GET /api/rooms</li>
 *   <li>GET /api/rooms/:id</li>
 *   <li>GET /api/rooms/:id/availability?checkIn=YYYY-MM-DD&amp;checkOut=YYYY-MM-DD</li>
 * </ul>
 */
@WebServlet("/api/rooms/*")
public class RoomsServlet extends ApiServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String[] parts = segments(request);

    if (parts.length == 0) {
      run(request, response, "Failed to load rooms.",
          () -> Json.write(response, 200, Map.of("rooms", services().rooms().listRooms())));
    } else if (parts.length == 1) {
      run(request, response, "Failed to load room.",
          () -> Json.write(response, 200, Map.of("room", services().rooms().getRoom(Rules.toNumber(parts[0], true)))));
    } else if (parts.length == 2 && parts[1].equals("availability")) {
      run(request, response, "Failed to check availability.", () -> {
        String checkIn = request.getParameter("checkIn");
        String checkOut = request.getParameter("checkOut");
        Json.write(response, 200, services().rooms().checkAvailability(
            Rules.toNumber(parts[0], true), checkIn == null ? "" : checkIn, checkOut == null ? "" : checkOut));
      });
    } else {
      Json.error(response, 404, "Not found.");
    }
  }
}
