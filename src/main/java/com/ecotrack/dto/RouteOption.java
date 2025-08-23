package com.ecotrack.dto;

public record RouteOption(
        String routeId,
        double healthScore,
        String polyline,
        String color,
        double distance,
        double duration,
        int aqi
) {
    public RouteOption {
        if (healthScore < 0 || healthScore > 100) {
            throw new IllegalArgumentException("Health score must be between 0 and 100");
        }
    }
}
