package com.ecotrack.dto;

import java.util.List;

public record RouteResponse(List<RouteOption> routes) {
    // Add validation if needed
    public RouteResponse {
        if (routes == null) {
            throw new IllegalArgumentException("Routes cannot be null");
        }
    }
}