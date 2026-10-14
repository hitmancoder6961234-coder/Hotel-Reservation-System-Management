package com.stayease.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/** JSON helpers shared by all servlets. */
public final class Json {

  /** Booking requests are tiny; anything larger is rejected as an invalid body. */
  private static final int MAX_BODY_BYTES = 16 * 1024;

  public static final ObjectMapper MAPPER = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private Json() {}

  public static void write(HttpServletResponse response, int status, Object body) throws IOException {
    byte[] bytes = MAPPER.writeValueAsBytes(body); // serialise first so a failure cannot leave a half-written body
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setContentLength(bytes.length);
    response.getOutputStream().write(bytes);
  }

  public static void error(HttpServletResponse response, int status, String message) throws IOException {
    write(response, status, Map.of("error", message));
  }

  /** Reads the request body as a JSON object; returns null when it is missing, too large or not an object. */
  public static JsonNode readObject(HttpServletRequest request) throws IOException {
    try (InputStream in = request.getInputStream()) {
      byte[] bytes = in.readNBytes(MAX_BODY_BYTES + 1);
      if (bytes.length == 0 || bytes.length > MAX_BODY_BYTES) {
        return null;
      }
      JsonNode node = MAPPER.readTree(bytes);
      return node != null && node.isObject() ? node : null;
    } catch (com.fasterxml.jackson.core.JsonProcessingException malformed) {
      return null;
    }
  }
}
