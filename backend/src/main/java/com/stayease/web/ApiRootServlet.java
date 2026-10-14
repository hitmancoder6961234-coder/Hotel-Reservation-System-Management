package com.stayease.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/** GET /api - health ping (same response as the original app; used by the Docker health checks). */
@WebServlet(urlPatterns = {"/api", "/api/"})
public class ApiRootServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Json.write(response, 200, Map.of("message", "Hello, world!"));
  }
}
