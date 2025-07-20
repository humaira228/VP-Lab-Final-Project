package com.ecotrack.dto;

public record RouteOption(
        String routeId,
        double healthScore,
        String polyline, // dummy path or GeoJSON string
        String color
) {}
