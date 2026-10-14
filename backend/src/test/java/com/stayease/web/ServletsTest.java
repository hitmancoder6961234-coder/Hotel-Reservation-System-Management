package com.stayease.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.stayease.db.Db;
import com.stayease.model.Room;
import com.stayease.service.BookingService;
import com.stayease.service.RoomService;
import com.stayease.testsupport.InMemory;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HTTP-level tests: servlet routing, JSON shapes, status codes and error handling. */
class ServletsTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T08:00:00Z"), ZoneId.of("Asia/Kolkata"));

  private ServletContext context;
  private InMemory.Rooms rooms;
  private RoomsServlet roomsServlet;
  private BookingsServlet bookingsServlet;

  /** A captured HTTP response. */
  private static final class Out {
    int status = 200;
    String contentType;
    final ByteArrayOutputStream body = new ByteArrayOutputStream();
    HttpServletResponse response = mock(HttpServletResponse.class);

    JsonNode json() throws Exception {
      return Json.MAPPER.readTree(body.toByteArray());
    }
  }

  private Out newOut() throws Exception {
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

  private HttpServletRequest request(String method, String pathInfo, String body, Map<String, String> params)
      throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn(method);
    when(request.getProtocol()).thenReturn("HTTP/1.1");
    when(request.getPathInfo()).thenReturn(pathInfo);
    when(request.getRequestURI()).thenReturn("/api/test");
    params.forEach((k, v) -> when(request.getParameter(k)).thenReturn(v));
    byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
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
    return request;
  }

  private void init(HttpServlet servlet) throws Exception {
    ServletConfig config = mock(ServletConfig.class);
    when(config.getServletContext()).thenReturn(context);
    servlet.init(config);
  }

  @BeforeEach
  void setUp() throws Exception {
    rooms = new InMemory.Rooms(InMemory.seedRooms());
    InMemory.Bookings bookings = new InMemory.Bookings(rooms);
    Db db = new InMemory.FakeDb();
    context = mock(ServletContext.class);
    when(context.getAttribute(AppServices.ATTRIBUTE)).thenReturn(
        new AppServices(new RoomService(db, rooms, bookings, CLOCK), new BookingService(db, rooms, bookings, CLOCK)));
    roomsServlet = new RoomsServlet();
    bookingsServlet = new BookingsServlet();
    init(roomsServlet);
    init(bookingsServlet);
  }

  private static List<String> keys(JsonNode node) {
    List<String> names = new java.util.ArrayList<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }

  // ---------------------------------------------------------------- rooms

  @Test
  void listRoomsReturnsTheOriginalJsonShape() throws Exception {
    Out out = newOut();
    roomsServlet.service(request("GET", null, null, Map.of()), out.response);

    assertEquals(200, out.status);
    assertEquals("application/json", out.contentType);
    JsonNode list = out.json().get("rooms");
    assertEquals(3, list.size());
    JsonNode first = list.get(0);
    assertEquals(101, first.get("id").asInt());
    assertEquals("Deluxe Room", first.get("name").asText());
    assertEquals(4000, first.get("price").asInt());
    assertEquals("AC", first.get("facilities").get(0).asText());
    assertTrue(first.get("available").asBoolean());
    assertEquals(List.of("id", "name", "description", "price", "capacity", "facilities", "features", "image", "available"),
        keys(first));
  }

  @Test
  void roomDetailsAndErrors() throws Exception {
    Out ok = newOut();
    roomsServlet.service(request("GET", "/102", null, Map.of()), ok.response);
    assertEquals("Luxury Room", ok.json().get("room").get("name").asText());

    Out bad = newOut();
    roomsServlet.service(request("GET", "/abc", null, Map.of()), bad.response);
    assertEquals(400, bad.status);
    assertEquals("Invalid room id.", bad.json().get("error").asText());

    Out missing = newOut();
    roomsServlet.service(request("GET", "/999", null, Map.of()), missing.response);
    assertEquals(404, missing.status);
    assertEquals("Room not found.", missing.json().get("error").asText());
  }

  @Test
  void availabilityEndpoint() throws Exception {
    Out ok = newOut();
    roomsServlet.service(request("GET", "/101/availability", null,
        Map.of("checkIn", "2026-09-20", "checkOut", "2026-09-23")), ok.response);
    JsonNode json = ok.json();
    assertEquals(200, ok.status);
    assertTrue(json.get("available").asBoolean());
    assertEquals(101, json.get("roomId").asInt());
    assertEquals("2026-09-20", json.get("checkIn").asText());
    assertEquals(0, json.get("conflicts").size());
    assertEquals(0, json.get("bookedRanges").size());

    Out noDates = newOut();
    roomsServlet.service(request("GET", "/101/availability", null, Map.of()), noDates.response);
    assertEquals(400, noDates.status);
    assertEquals("Please select check-in date.", noDates.json().get("error").asText());
  }

  @Test
  void unknownRoomSubPathIs404() throws Exception {
    Out out = newOut();
    roomsServlet.service(request("GET", "/101/photos", null, Map.of()), out.response);
    assertEquals(404, out.status);
    assertEquals("Not found.", out.json().get("error").asText());
  }

  // ---------------------------------------------------------------- bookings

  private static final String VALID_BOOKING = """
      {"guestName":"Priya Mehta","email":"priya@example.com","mobile":"+91 98200 12345",
       "roomId":102,"checkIn":"2026-09-20","checkOut":"2026-09-23","guests":2}""";

  @Test
  void createBookingReturns201WithTheOriginalShape() throws Exception {
    Out out = newOut();
    bookingsServlet.service(request("POST", null, VALID_BOOKING, Map.of()), out.response);

    assertEquals(201, out.status);
    JsonNode booking = out.json().get("booking");
    assertEquals("STY-2026-001", booking.get("bookingRef").asText());
    assertEquals(3, booking.get("nights").asInt());
    assertEquals(21_000, booking.get("totalPrice").asInt());
    assertEquals("Confirmed", booking.get("status").asText());
    assertEquals("2026-09-20", booking.get("checkIn").asText());
    assertEquals("Luxury Room", booking.get("room").get("name").asText());
    assertEquals(
        List.of("id", "bookingRef", "guestName", "email", "mobile", "roomId", "checkIn", "checkOut", "guests",
            "nights", "totalPrice", "status", "createdAt", "room"),
        keys(booking));
  }

  @Test
  void doubleBookingIs409() throws Exception {
    bookingsServlet.service(request("POST", null, VALID_BOOKING, Map.of()), newOut().response);
    Out second = newOut();
    bookingsServlet.service(request("POST", null, VALID_BOOKING, Map.of()), second.response);
    assertEquals(409, second.status);
    assertEquals("This room is not available for the selected dates.", second.json().get("error").asText());
  }

  @Test
  void malformedAndNonObjectBodiesAre400() throws Exception {
    for (String body : new String[] {"{not json", "[1,2]", "\"text\"", "", "null"}) {
      Out out = newOut();
      bookingsServlet.service(request("POST", null, body, Map.of()), out.response);
      assertEquals(400, out.status, body);
      assertEquals("Invalid request body.", out.json().get("error").asText());
    }
  }

  @Test
  void numbersGivenAsStringsAreAcceptedLikeTheOriginal() throws Exception {
    String body = VALID_BOOKING.replace("\"roomId\":102", "\"roomId\":\"102\"").replace("\"guests\":2", "\"guests\":\"2\"");
    Out out = newOut();
    bookingsServlet.service(request("POST", null, body, Map.of()), out.response);
    assertEquals(201, out.status);
  }

  @Test
  void missingRoomIdIsReported() throws Exception {
    Out out = newOut();
    bookingsServlet.service(request("POST", null,
        "{\"guestName\":\"Priya\",\"email\":\"p@x.com\",\"mobile\":\"9820012345\"}", Map.of()), out.response);
    assertEquals(400, out.status);
    assertEquals("Please select a room.", out.json().get("error").asText());
  }

  @Test
  void getBookingByReference() throws Exception {
    bookingsServlet.service(request("POST", null, VALID_BOOKING, Map.of()), newOut().response);

    Out ok = newOut();
    bookingsServlet.service(request("GET", "/STY-2026-001", null, Map.of()), ok.response);
    JsonNode booking = ok.json().get("booking");
    assertEquals("Luxury Room", booking.get("roomName").asText());
    assertEquals("/images/luxury.jpg", booking.get("roomImage").asText());
    assertEquals(102, booking.get("roomId").asInt());
    assertEquals(
        List.of("bookingRef", "guestName", "email", "mobile", "roomId", "roomName", "roomImage", "checkIn",
            "checkOut", "guests", "nights", "totalPrice", "status", "createdAt"),
        keys(booking));

    Out missing = newOut();
    bookingsServlet.service(request("GET", "/STY-2026-404", null, Map.of()), missing.response);
    assertEquals(404, missing.status);
    assertEquals("Booking not found.", missing.json().get("error").asText());
  }

  @Test
  void unsupportedMethodsAreRejectedWith405() throws Exception {
    Out getList = newOut();
    bookingsServlet.service(request("GET", null, null, Map.of()), getList.response);
    verify(getList.response).sendError(org.mockito.ArgumentMatchers.eq(405), org.mockito.ArgumentMatchers.any());

    Out postRef = newOut();
    bookingsServlet.service(request("POST", "/STY-2026-001", VALID_BOOKING, Map.of()), postRef.response);
    verify(postRef.response).sendError(org.mockito.ArgumentMatchers.eq(405), org.mockito.ArgumentMatchers.any());
  }

  // ---------------------------------------------------------------- error handling

  private void breakDatabase(Throwable failure) throws Exception {
    Db broken = new Db() {
      @Override
      public <T> T read(com.stayease.db.SqlFunction<T> work) throws SQLException {
        if (failure instanceof SQLException e) throw e;
        throw (RuntimeException) failure;
      }

      @Override
      public <T> T transaction(com.stayease.db.SqlFunction<T> work) throws SQLException {
        return read(work);
      }
    };
    when(context.getAttribute(AppServices.ATTRIBUTE)).thenReturn(
        new AppServices(new RoomService(broken, rooms, new InMemory.Bookings(rooms), CLOCK),
            new BookingService(broken, rooms, new InMemory.Bookings(rooms), CLOCK)));
  }

  @Test
  void databaseOutageIs503AndHidesTechnicalDetails() throws Exception {
    breakDatabase(new SQLTransientConnectionException("Connection to 10.0.0.5:3306 refused, password=secret"));
    Out out = newOut();
    roomsServlet.service(request("GET", null, null, Map.of()), out.response);
    assertEquals(503, out.status);
    String text = out.body.toString(StandardCharsets.UTF_8);
    assertEquals("Service temporarily unavailable. Please try again.", out.json().get("error").asText());
    assertFalse(text.contains("10.0.0.5") || text.contains("secret"));
  }

  @Test
  void unexpectedErrorsAre500WithAGenericMessage() throws Exception {
    breakDatabase(new SQLException("Syntax error near 'FROM Room' at line 1"));
    Out out = newOut();
    bookingsServlet.service(request("POST", null, VALID_BOOKING, Map.of()), out.response);
    assertEquals(500, out.status);
    assertEquals("Failed to create booking.", out.json().get("error").asText());
    assertFalse(out.body.toString(StandardCharsets.UTF_8).contains("Syntax"));
  }

  @Test
  void rootPingAndNotFoundServlets() throws Exception {
    Out ping = newOut();
    new ApiRootServlet().service(request("GET", null, null, Map.of()), ping.response);
    assertEquals("Hello, world!", ping.json().get("message").asText());

    Out nf = newOut();
    new NotFoundServlet().service(request("GET", "/whatever", null, Map.of()), nf.response);
    assertEquals(404, nf.status);
    assertEquals("Not found.", nf.json().get("error").asText());
    assertNull(nf.json().get("stack"));
  }
}
