package com.stayease.model;

import java.util.List;

/** A room as returned by the API (same JSON shape as the original Next.js API). */
public record Room(
    int id,
    String name,
    String description,
    int price,
    int capacity,
    List<String> facilities,
    List<String> features,
    String image,
    boolean available) {}
