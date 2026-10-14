package com.stayease.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Error page for errors raised by the container itself (405 for unsupported methods, malformed
 * requests, ...): a short JSON body, never the server's HTML report with version information.
 */
@WebServlet("/error")
public class ErrorServlet extends HttpServlet {

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    int status = code instanceof Integer i ? i : 500;
    Json.error(response, status, messageFor(status));
  }

  static String messageFor(int status) {
    return switch (status) {
      case 400 -> "Bad request.";
      case 404 -> "Not found.";
      case 405 -> "Method not allowed.";
      case 413, 414 -> "Request too large.";
      default -> status >= 500 ? "Something went wrong." : "Request failed.";
    };
  }
}
