package com.stayease.web;

import com.stayease.service.BookingService;
import com.stayease.service.RoomService;

/** The services the servlets use, created once at start-up and stored in the ServletContext. */
public record AppServices(RoomService rooms, BookingService bookings) {

  public static final String ATTRIBUTE = AppServices.class.getName();
}
