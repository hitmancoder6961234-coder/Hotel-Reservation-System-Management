package com.stayease.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stayease.model.AdminBooking;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class BookingsWorkbookTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 16, 8, 0);

  private static AdminBooking booking(int id, String name, int roomId, String room, int price, String status) {
    return new AdminBooking(id, "STY-2026-00" + id, name, "g" + id + "@example.com", "9876543210", roomId, room,
        LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 10), 2, 2, price, status,
        Instant.parse("2026-10-01T10:15:30Z"));
  }

  private static XSSFWorkbook open(byte[] bytes) throws Exception {
    return new XSSFWorkbook(new ByteArrayInputStream(bytes));
  }

  @Test
  void bookingsSheetHasTheDashboardColumnsAndOneRowPerBooking() throws Exception {
    byte[] bytes = BookingsWorkbook.build(
        List.of(booking(1, "Ada", 101, "Deluxe Room", 8000, "Confirmed"),
            booking(2, "=1+1", 102, "Luxury Room", 14000, "Confirmed")),
        NOW);

    try (XSSFWorkbook workbook = open(bytes)) {
      XSSFSheet sheet = workbook.getSheet("Bookings");
      assertEquals("id", sheet.getRow(0).getCell(0).getStringCellValue());
      assertEquals("bookingRef", sheet.getRow(0).getCell(1).getStringCellValue());
      assertEquals("createdAt", sheet.getRow(0).getCell(13).getStringCellValue());
      assertEquals(3, sheet.getPhysicalNumberOfRows());
      assertEquals("STY-2026-001", sheet.getRow(1).getCell(1).getStringCellValue());
      assertEquals(14000, sheet.getRow(2).getCell(11).getNumericCellValue());
      // a guest name that looks like a formula stays plain text
      assertEquals(CellType.STRING, sheet.getRow(2).getCell(2).getCellType());
      assertEquals("=1+1", sheet.getRow(2).getCell(2).getStringCellValue());
      assertEquals(LocalDate.of(2026, 10, 8), sheet.getRow(1).getCell(7).getLocalDateTimeCellValue().toLocalDate());
    }
  }

  @Test
  void summarySheetHasTotalsAndTwoCharts() throws Exception {
    byte[] bytes = BookingsWorkbook.build(
        List.of(booking(1, "Ada", 101, "Deluxe Room", 8000, "Confirmed"),
            booking(2, "Bob", 101, "Deluxe Room", 4000, "Confirmed"),
            booking(3, "Cy", 102, "Luxury Room", 14000, "Cancelled")),
        NOW);

    try (XSSFWorkbook workbook = open(bytes)) {
      XSSFSheet summary = workbook.getSheet("Summary");
      assertEquals("Deluxe Room", summary.getRow(1).getCell(0).getStringCellValue());
      assertEquals(2, summary.getRow(1).getCell(1).getNumericCellValue());
      assertEquals(12000, summary.getRow(1).getCell(2).getNumericCellValue());
      assertEquals("Luxury Room", summary.getRow(2).getCell(0).getStringCellValue());
      assertEquals(1, summary.getRow(2).getCell(1).getNumericCellValue());
      assertEquals(0, summary.getRow(2).getCell(2).getNumericCellValue()); // cancelled: no revenue
      assertNotNull(summary.getDrawingPatriarch());
      assertEquals(2, summary.getDrawingPatriarch().getCharts().size());
    }
  }

  @Test
  void emptyExportStillOpensAndHasNoCharts() throws Exception {
    byte[] bytes = BookingsWorkbook.build(List.of(), NOW);
    try (XSSFWorkbook workbook = open(bytes)) {
      assertEquals(1, workbook.getSheet("Bookings").getPhysicalNumberOfRows());
      XSSFSheet summary = workbook.getSheet("Summary");
      assertTrue(summary.getDrawingPatriarch() == null || summary.getDrawingPatriarch().getCharts().isEmpty());
    }
  }
}
