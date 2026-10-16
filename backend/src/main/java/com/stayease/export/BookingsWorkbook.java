package com.stayease.export;

import com.stayease.model.AdminBooking;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChart;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Builds the Excel (.xlsx) export of all bookings:
 * <ul>
 *   <li>sheet "Bookings" - every booking, columns in the same order as the admin dashboard;</li>
 *   <li>sheet "Summary" - totals, bookings and revenue per room, bookings per status, with a pie chart
 *       (bookings per room) and a column chart (revenue per room).</li>
 * </ul>
 * Text goes in as plain string cells (never formulas), so a guest name like "=1+1" stays text.
 */
public final class BookingsWorkbook {

  private static final String[] HEADERS = {
    "id", "bookingRef", "guestName", "email", "mobile", "roomId", "roomName", "checkIn", "checkOut",
    "guests", "nights", "totalPrice", "status", "createdAt"
  };
  private static final int[] WIDTHS = {6, 15, 24, 32, 16, 8, 18, 12, 12, 8, 8, 12, 12, 20};

  private BookingsWorkbook() {}

  public static byte[] build(List<AdminBooking> bookings, LocalDateTime generatedAtUtc) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Styles styles = new Styles(workbook);
      writeBookings(workbook.createSheet("Bookings"), bookings, styles);
      writeSummary(workbook.createSheet("Summary"), bookings, generatedAtUtc, styles);
      workbook.write(out);
      return out.toByteArray();
    }
  }

  // ------------------------------------------------------------------ Bookings sheet

  private static void writeBookings(XSSFSheet sheet, List<AdminBooking> bookings, Styles styles) {
    Row head = sheet.createRow(0);
    for (int i = 0; i < HEADERS.length; i++) {
      head.createCell(i).setCellValue(HEADERS[i]);
      head.getCell(i).setCellStyle(styles.header);
      sheet.setColumnWidth(i, WIDTHS[i] * 256);
    }
    int rowIndex = 1;
    for (AdminBooking b : bookings) {
      Row row = sheet.createRow(rowIndex++);
      row.createCell(0).setCellValue(b.id());
      row.createCell(1).setCellValue(b.bookingRef());
      row.createCell(2).setCellValue(b.guestName());
      row.createCell(3).setCellValue(b.email());
      row.createCell(4).setCellValue(b.mobile());
      row.createCell(5).setCellValue(b.roomId());
      row.createCell(6).setCellValue(b.roomName());
      row.createCell(7).setCellValue(DateUtil.getExcelDate(b.checkIn().atStartOfDay()));
      row.getCell(7).setCellStyle(styles.date);
      row.createCell(8).setCellValue(DateUtil.getExcelDate(b.checkOut().atStartOfDay()));
      row.getCell(8).setCellStyle(styles.date);
      row.createCell(9).setCellValue(b.guests());
      row.createCell(10).setCellValue(b.nights());
      row.createCell(11).setCellValue(b.totalPrice());
      row.getCell(11).setCellStyle(styles.money);
      row.createCell(12).setCellValue(b.status());
      if (b.createdAt() != null) {
        row.createCell(13).setCellValue(DateUtil.getExcelDate(LocalDateTime.ofInstant(b.createdAt(), ZoneOffset.UTC)));
        row.getCell(13).setCellStyle(styles.dateTime);
      }
    }
    sheet.createFreezePane(0, 1);
    sheet.setAutoFilter(new CellRangeAddress(0, Math.max(bookings.size(), 1), 0, HEADERS.length - 1));
  }

  // ------------------------------------------------------------------ Summary sheet

  private static final class RoomTotals {
    final String name;
    int bookings;
    long revenue;

    RoomTotals(String name) {
      this.name = name;
    }
  }

  private static void writeSummary(
      XSSFSheet sheet, List<AdminBooking> bookings, LocalDateTime generatedAtUtc, Styles styles) {
    TreeMap<Integer, RoomTotals> perRoom = new TreeMap<>();
    Map<String, Integer> perStatus = new LinkedHashMap<>();
    long totalRevenue = 0;
    for (AdminBooking b : bookings) {
      RoomTotals room = perRoom.computeIfAbsent(b.roomId(), id -> new RoomTotals(b.roomName()));
      room.bookings++;
      perStatus.merge(b.status(), 1, Integer::sum);
      if (!"Cancelled".equalsIgnoreCase(b.status())) {
        room.revenue += b.totalPrice();
        totalRevenue += b.totalPrice();
      }
    }

    sheet.setColumnWidth(0, 22 * 256);
    sheet.setColumnWidth(1, 12 * 256);
    sheet.setColumnWidth(2, 16 * 256);

    Row head = sheet.createRow(0);
    String[] roomHeaders = {"Room", "Bookings", "Revenue (INR)"};
    for (int i = 0; i < roomHeaders.length; i++) {
      head.createCell(i).setCellValue(roomHeaders[i]);
      head.getCell(i).setCellStyle(styles.header);
    }
    int row = 1;
    for (RoomTotals room : perRoom.values()) {
      Row r = sheet.createRow(row++);
      r.createCell(0).setCellValue(room.name);
      r.createCell(1).setCellValue(room.bookings);
      r.createCell(2).setCellValue(room.revenue);
      r.getCell(2).setCellStyle(styles.money);
    }
    int lastRoomRow = row - 1; // 0 when there are no bookings

    row++;
    Row totalBookings = sheet.createRow(row++);
    totalBookings.createCell(0).setCellValue("Total bookings");
    totalBookings.createCell(1).setCellValue(bookings.size());
    Row totalMoney = sheet.createRow(row++);
    totalMoney.createCell(0).setCellValue("Total revenue (INR)");
    totalMoney.createCell(2).setCellValue(totalRevenue);
    totalMoney.getCell(2).setCellStyle(styles.money);
    Row generated = sheet.createRow(row++);
    generated.createCell(0).setCellValue("Generated (UTC)");
    generated.createCell(1).setCellValue(DateUtil.getExcelDate(generatedAtUtc));
    generated.getCell(1).setCellStyle(styles.dateTime);
    sheet.setColumnWidth(1, 20 * 256);

    row++;
    Row statusHead = sheet.createRow(row++);
    statusHead.createCell(0).setCellValue("Status");
    statusHead.getCell(0).setCellStyle(styles.header);
    statusHead.createCell(1).setCellValue("Bookings");
    statusHead.getCell(1).setCellStyle(styles.header);
    for (Map.Entry<String, Integer> entry : perStatus.entrySet()) {
      Row r = sheet.createRow(row++);
      r.createCell(0).setCellValue(entry.getKey());
      r.createCell(1).setCellValue(entry.getValue());
    }

    if (lastRoomRow >= 1) {
      XSSFDrawing drawing = sheet.createDrawingPatriarch();
      addPieChart(sheet, drawing, lastRoomRow);
      addColumnChart(sheet, drawing, lastRoomRow);
    } else {
      sheet.createRow(row + 1).createCell(0).setCellValue("No bookings yet - charts appear once there are bookings.");
    }
  }

  private static void addPieChart(XSSFSheet sheet, XSSFDrawing drawing, int lastRoomRow) {
    XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 1, 12, 19);
    XDDFChart chart = drawing.createChart(anchor);
    chart.setTitleText("Bookings per room");
    chart.setTitleOverlay(false);
    chart.getOrAddLegend().setPosition(LegendPosition.RIGHT);

    XDDFDataSource<String> rooms =
        XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(1, lastRoomRow, 0, 0));
    XDDFNumericalDataSource<Double> counts =
        XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(1, lastRoomRow, 1, 1));
    XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);
    data.setVaryColors(true);
    data.addSeries(rooms, counts);
    chart.plot(data);
  }

  private static void addColumnChart(XSSFSheet sheet, XSSFDrawing drawing, int lastRoomRow) {
    XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 21, 12, 39);
    XDDFChart chart = drawing.createChart(anchor);
    chart.setTitleText("Revenue per room (INR)");
    chart.setTitleOverlay(false);

    XDDFCategoryAxis bottom = chart.createCategoryAxis(AxisPosition.BOTTOM);
    XDDFValueAxis left = chart.createValueAxis(AxisPosition.LEFT);
    XDDFDataSource<String> rooms =
        XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(1, lastRoomRow, 0, 0));
    XDDFNumericalDataSource<Double> revenue =
        XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(1, lastRoomRow, 2, 2));
    XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottom, left);
    data.setBarDirection(BarDirection.COL);
    data.addSeries(rooms, revenue);
    chart.plot(data);
  }

  // ------------------------------------------------------------------ cell styles

  private static final class Styles {
    final CellStyle header;
    final CellStyle date;
    final CellStyle dateTime;
    final CellStyle money;

    Styles(XSSFWorkbook workbook) {
      DataFormat format = workbook.createDataFormat();
      Font bold = workbook.createFont();
      bold.setBold(true);
      header = workbook.createCellStyle();
      header.setFont(bold);
      header.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
      header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      header.setBorderBottom(BorderStyle.THIN);
      date = workbook.createCellStyle();
      date.setDataFormat(format.getFormat("yyyy-mm-dd"));
      dateTime = workbook.createCellStyle();
      dateTime.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm"));
      money = workbook.createCellStyle();
      money.setDataFormat(format.getFormat("#,##0"));
    }
  }
}
