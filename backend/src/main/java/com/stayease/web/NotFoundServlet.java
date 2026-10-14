package com.stayease.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Any other /api/... path: a JSON 404 instead of Tomcat's HTML error page. */
@WebServlet("/api/*")
public class NotFoundServlet extends HttpServlet {

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Json.error(response, 404, "Not found.");
  }
}
