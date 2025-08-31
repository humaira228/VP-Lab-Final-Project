
package com.ecotrack.dto;

public record RouteOption(
        String routeId,
        double healthScore,
        String polyline,
        String color,
        double distance,
        double duration,
        int aqi,
        int traffic,          // NEW: average traffic score for the route
        double combinedScore  // NEW: balanced score (health + traffic)
) {
    public RouteOption {
        if (healthScore < 0 || healthScore > 100) {
            throw new IllegalArgumentException("Health score must be between 0 and 100");
        }
        if (combinedScore < 0 || combinedScore > 100) {
            throw new IllegalArgumentException("Combined score must be between 0 and 100");
        }
    }
}