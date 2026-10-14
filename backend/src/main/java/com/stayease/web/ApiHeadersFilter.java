package com.stayease.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Booking data is private: API responses must never be cached by browsers, proxies or the CDN. */
@WebFilter("/*")
public class ApiHeadersFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletResponse http = (HttpServletResponse) response;
    http.setHeader("Cache-Control", "no-store");
    http.setHeader("X-Content-Type-Options", "nosniff");
    chain.doFilter(request, response);
  }
}
