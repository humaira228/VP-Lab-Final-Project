package com.ecotrack.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RouteRequest(
        @Min(-90) @Max(90) double originLat,
        @Min(-180) @Max(180) double originLon,
        @Min(-90) @Max(90) double destLat,
        @Min(-180) @Max(180) double destLon
) {}