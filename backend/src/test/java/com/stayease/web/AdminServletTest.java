package com.stayease.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.stayease.db.Db;
import com.stayease.security.AdminAuth;
import com.stayease.security.PasswordHasher;
import com.stayease.service.BookingService;
import com.stayease.service.RoomService;
import com.stayease.testsupport.InMemory;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HTTP-level tests of the administrator endpoints. */
class AdminServletTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T08:00:00Z"), ZoneId.of("Asia/Kolkata"));
  private static final String HASH = PasswordHasher.hash("s3cret-pass", 1000);
  private static final String VALID_BOOKING =
      "{\"guestName\":\"Ada Lovelace\",\"email\":\"ada@example.com\",\"mobile\":\"9876543210\","
          + "\"roomId\":102,\"checkIn\":\"2026-10-08\",\"checkOut\":\"2026-10-10\",\"guests\":2}";

  private ServletContext context;
  private AdminServlet admin;
  private BookingsServlet bookingsServlet;

  private static final class Out {
    int status = 200;
    String contentType;
    final ByteArrayOutputStream body = new ByteArrayOutputStream();
    final HttpServletResponse response = mock(HttpServletResponse.class);

    JsonNode json() throws Exception {
      return Json.MAPPER.readTree(body.toByteArray());
    }
  }

  private static Out newOut() throws Exception {
    Out out = new Out();
    when(out.response.getOutputStream()).thenReturn(new ServletOutputStream() {
      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setWriteListener(WriteListener listener) {}

      @Override
      public void write(int b) {
        out.body.write(b);
      }
    });
    org.mockito.Mockito.doAnswer(i -> {
      out.status = i.getArgument(0);
      return null;
    }).when(out.response).setStatus(anyInt());
    org.mockito.Mockito.doAnswer(i -> {
      out.contentType = i.getArgument(0);
      return null;
    }).when(out.response).setContentType(org.mockito.ArgumentMatchers.anyString());
    return out;
  }

  private static HttpServletRequest request(String method, String pathInfo, String body, HttpSession session) {
    HttpSession fresh = session == null ? mock(HttpSession.class) : session; // created before any stubbing
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn(method);
    when(request.getProtocol()).thenReturn("HTTP/1.1");
    when(request.getPathInfo()).thenReturn(pathInfo);
    when(request.getRequestURI()).thenReturn("/api/admin/test");
    when(request.getSession(false)).thenReturn(session);
    when(request.getSession(true)).thenReturn(fresh);
    ByteArrayInputStream in = new ByteArrayInputStream(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
    try {
      when(request.getInputStream()).thenReturn(new ServletInputStream() {
        @Override
        public boolean isFinished() {
          return in.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {}

        @Override
        public int read() {
          return in.read();
        }
      });
    } catch (java.io.IOException impossible) {
      throw new IllegalStateException(impossible);
    }
    return request;
  }

  private static HttpSession loggedInSession() {
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(AdminServlet.SESSION_USER)).thenReturn("admin@example");
    return session;
  }

  private static String login(String user, String password) {
    return "{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}";
  }

  private void configure(AdminAuth auth) throws Exception {
    ServletConfig config = mock(ServletConfig.class);
    when(config.getServletContext()).thenReturn(context);
    when(context.getAttribute(AdminAuth.ATTRIBUTE)).thenReturn(auth);
    admin = new AdminServlet();
    admin.init(config);
  }

  @BeforeEach
  void setUp() throws Exception {
    InMemory.Rooms rooms = new InMemory.Rooms(InMemory.seedRooms());
    InMemory.Bookings bookings = new InMemory.Bookings(rooms);
    Db db = new InMemory.FakeDb();
    context = mock(ServletContext.class);
    when(context.getAttribute(AppServices.ATTRIBUTE)).thenReturn(
        new AppServices(new RoomService(db, rooms, bookings, CLOCK), new BookingService(db, rooms, bookings, CLOCK)));
    configure(new AdminAuth("admin@example", HASH, CLOCK));

    ServletConfig config = mock(ServletConfig.class);
    when(config.getServletContext()).thenReturn(context);
    bookingsServlet = new BookingsServlet();
    bookingsServlet.init(config);
  }

  @Test
  void loginWithTheRightCredentialsStartsASession() throws Exception {
    HttpSession session = mock(HttpSession.class);
    Out out = newOut();
    admin.service(request("POST", "/login", login("admin@example", "s3cret-pass"), session), out.response);

    assertEquals(200, out.status);
    assertEquals("admin@example", out.json().get("admin").get("username").asText());
    verify(session).setAttribute(AdminServlet.SESSION_USER, "admin@example");
    verify(session).setMaxInactiveInterval(30 * 60);
  }

  @Test
  void loginWithWrongCredentialsIs401AndStartsNoSession() throws Exception {
    HttpSession session = mock(HttpSession.class);
    Out out = newOut();
    admin.service(request("POST", "/login", login("admin@example", "nope"), session), out.response);

    assertEquals(401, out.status);
    assertEquals("Invalid username or password.", out.json().get("error").asText());
    verify(session, never()).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void tooManyFailuresAre429() throws Exception {
    for (int i = 0; i < 5; i++) {
      admin.service(request("POST", "/login", login("admin@example", "bad"), null), newOut().response);
    }
    Out out = newOut();
    admin.service(request("POST", "/login", login("admin@example", "s3cret-pass"), null), out.response);
    assertEquals(429, out.status);
  }

  @Test
  void loginIsUnavailableWhenNotConfigured() throws Exception {
    configure(new AdminAuth("", "", CLOCK));
    Out out = newOut();
    admin.service(request("POST", "/login", login("a", "b"), null), out.response);
    assertEquals(503, out.status);
  }

  @Test
  void malformedLoginBodyIs400() throws Exception {
    Out out = newOut();
    admin.service(request("POST", "/login", "not json", null), out.response);
    assertEquals(400, out.status);
  }

  @Test
  void meReportsTheSessionState() throws Exception {
    Out anonymous = newOut();
    admin.service(request("GET", "/me", null, null), anonymous.response);
    assertEquals(200, anonymous.status);
    assertTrue(anonymous.json().get("admin").isNull());

    Out signedIn = newOut();
    admin.service(request("GET", "/me", null, loggedInSession()), signedIn.response);
    assertEquals("admin@example", signedIn.json().get("admin").get("username").asText());
  }

  @Test
  void bookingsRequireAnAdminSession() throws Exception {
    Out denied = newOut();
    admin.service(request("GET", "/bookings", null, null), denied.response);
    assertEquals(401, denied.status);

    Out deniedExcel = newOut();
    admin.service(request("GET", "/bookings.xlsx", null, null), deniedExcel.response);
    assertEquals(401, deniedExcel.status);
  }

  @Test
  void adminSeesEveryBookingInTheDashboardFieldOrder() throws Exception {
    bookingsServlet.service(request("POST", null, VALID_BOOKING, null), newOut().response);

    Out out = newOut();
    admin.service(request("GET", "/bookings", null, loggedInSession()), out.response);
    assertEquals(200, out.status);
    assertEquals(1, out.json().get("total").asInt());
    JsonNode first = out.json().get("bookings").get(0);
    assertEquals("STY-2026-001", first.get("bookingRef").asText());
    assertEquals("Luxury Room", first.get("roomName").asText());
    java.util.List<String> names = new java.util.ArrayList<>();
    first.fieldNames().forEachRemaining(names::add);
    assertEquals(
        java.util.List.of("id", "bookingRef", "guestName", "email", "mobile", "roomId", "roomName", "checkIn",
            "checkOut", "guests", "nights", "totalPrice", "status", "createdAt"),
        names);
  }

  @Test
  void excelDownloadIsAnXlsxFile() throws Exception {
    bookingsServlet.service(request("POST", null, VALID_BOOKING, null), newOut().response);

    Out out = newOut();
    admin.service(request("GET", "/bookings.xlsx", null, loggedInSession()), out.response);
    assertEquals(200, out.status);
    assertEquals(AdminServlet.XLSX, out.contentType);
    byte[] bytes = out.body.toByteArray();
    assertTrue(bytes.length > 1000);
    assertEquals('P', bytes[0]); // .xlsx is a zip archive
    assertEquals('K', bytes[1]);
    verify(out.response).setHeader(
        org.mockito.ArgumentMatchers.eq("Content-Disposition"),
        org.mockito.ArgumentMatchers.argThat(v -> v.startsWith("attachment; filename=\"stayease-bookings-")));
  }

  @Test
  void logoutEndsTheSession() throws Exception {
    HttpSession session = loggedInSession();
    Out out = newOut();
    admin.service(request("POST", "/logout", null, session), out.response);
    assertEquals(200, out.status);
    verify(session).invalidate();
  }

  @Test
  void unknownPathsAre404() throws Exception {
    Out out = newOut();
    admin.service(request("GET", "/nothing", null, null), out.response);
    assertEquals(404, out.status);
  }
}
