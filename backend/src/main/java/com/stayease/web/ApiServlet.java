package com.stayease.web;

import com.stayease.service.ApiException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of the API servlets: one place that turns exceptions into the JSON error responses
 * {@code {"error": "..."}}. Internal details (stack traces, SQL messages) go to the log, never to the client.
 */
public abstract class ApiServlet extends HttpServlet {

  private static final Logger log = LoggerFactory.getLogger(ApiServlet.class);

  @FunctionalInterface
  protected interface Handler {
    void handle() throws IOException, SQLException;
  }

  protected AppServices services() {
    return (AppServices) getServletContext().getAttribute(AppServices.ATTRIBUTE);
  }

  /** Runs the handler; {@code failureMessage} is what the client sees on an unexpected error. */
  protected final void run(
      HttpServletRequest request, HttpServletResponse response, String failureMessage, Handler handler)
      throws IOException {
    try {
      if (services() == null) {
        throw new IllegalStateException("Application services are not initialised");
      }
      handler.handle();
    } catch (ApiException e) {
      Json.error(response, e.status(), e.getMessage());
    } catch (SQLTransientException | SQLRecoverableException e) {
      // e.g. the database is restarting / unreachable: not the client's fault, and retrying may work.
      log.warn("{} {} - database temporarily unavailable: {}", request.getMethod(), request.getRequestURI(), e.toString());
      Json.error(response, 503, "Service temporarily unavailable. Please try again.");
    } catch (SQLException | RuntimeException e) {
      log.error("{} {} failed", request.getMethod(), request.getRequestURI(), e);
      Json.error(response, 500, failureMessage);
    }
  }

  /** The non-empty segments of the part of the URL after the servlet mapping, e.g. "/101/availability". */
  protected static String[] segments(HttpServletRequest request) {
    String path = request.getPathInfo();
    if (path == null || path.isBlank()) {
      return new String[0];
    }
    return java.util.Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
  }
}
