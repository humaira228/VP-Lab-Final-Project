package com.ecotrack.dto;

public record RouteRequest(
        double originLat,
        double originLon,
        double destLat,
        double destLon,    // e.g., "12.9352,77.6146"
        String userEmail
) {}
