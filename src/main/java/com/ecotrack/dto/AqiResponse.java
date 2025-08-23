package com.ecotrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public record AqiResponse(
        String status,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            int aqi,
            City city,
            Time time
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record City(
            String name,
            double[] geo,
            String url
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Time(
            String s,
            String tz,
            long v
    ) {}
}