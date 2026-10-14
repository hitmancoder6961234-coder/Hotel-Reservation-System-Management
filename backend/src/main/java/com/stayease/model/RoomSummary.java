package com.stayease.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** The room fields embedded in a booking response. */
@JsonPropertyOrder({"id", "name", "price", "capacity", "image"})
public record RoomSummary(int id, String name, int price, int capacity, String image) {}
