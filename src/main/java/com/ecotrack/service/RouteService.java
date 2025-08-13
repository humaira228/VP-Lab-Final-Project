package com.ecotrack.service;

import com.ecotrack.dto.RouteOption;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class RouteService {

    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    // TODO: Move to application.properties
    private final String API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjM3OGIxODQ0MDcwMTRkMmJiMjFkZDY1YjFmYTExMGZmIiwiaCI6Im11cm11cjY0In0=";

    public List<RouteOption> getRouteAlternatives(double startLon, double startLat, double endLon, double endLat) {
        String url = "https://api.openrouteservice.org/v2/directions/foot-walking/geojson";

        Map<String, Object> body = new HashMap<>();
        body.put("coordinates", Arrays.asList(
                Arrays.asList(startLon, startLat),
                Arrays.asList(endLon, endLat)
        ));

        Map<String, Object> alternatives = new HashMap<>();
        alternatives.put("share_factor", 0.6);
        alternatives.put("target_count", 3);
        body.put("alternative_routes", alternatives);
        body.put("instructions", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", API_KEY);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("ORS API returned non-200 status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to fetch routes from ORS");
            }

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                logger.error("Empty response body from ORS API");
                throw new RuntimeException("Empty response from routing service");
            }

            List<Map<String, Object>> features = (List<Map<String, Object>>) responseBody.get("features");
            if (features == null || features.isEmpty()) {
                logger.warn("No route features found in ORS response");
                return Collections.emptyList();
            }

            List<RouteOption> options = new ArrayList<>();

            for (int i = 0; i < features.size(); i++) {
                Map<String, Object> feature = features.get(i);
                Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
                Map<String, Object> properties = (Map<String, Object>) feature.get("properties");

                if (geometry == null || properties == null) {
                    logger.warn("Skipping route {} due to missing geometry or properties", i);
                    continue;
                }

                List<List<Double>> coordinates = (List<List<Double>>) geometry.get("coordinates");
                Map<String, Object> summary = (Map<String, Object>) properties.get("summary");

                if (coordinates == null || summary == null) {
                    logger.warn("Skipping route {} due to missing coordinates or summary", i);
                    continue;
                }

                String polyline = serializeCoordinates(coordinates);
                double distance = parseDouble(summary.get("distance"));  // meters
                double duration = parseDouble(summary.get("duration"));  // seconds

                // Convert to km and minutes for scoring
                double distanceKm = distance / 1000.0;
                double durationMinutes = duration / 60.0;

                // New health score formula (0-100 scale)
                double healthScore = 100 - (distanceKm * 0.5) - (durationMinutes * 0.1);
                healthScore = Math.max(0, Math.min(100, healthScore)); // Clamp to 0-100

                int aqi = new Random().nextInt(101);
                String color = getColor(healthScore);

                options.add(new RouteOption(
                        "route-" + i,
                        healthScore,
                        polyline,
                        color,
                        distance,
                        duration,
                        aqi
                ));
            }

            return options;

        } catch (Exception e) {
            logger.error("Error fetching routes from ORS API", e);
            throw new RuntimeException("Error processing routing request", e);
        }
    }

    private double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private String serializeCoordinates(List<List<Double>> coordinates) {
        try {
            return objectMapper.writeValueAsString(coordinates);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize coordinates", e);
            return "[]";
        }
    }

    private String getColor(double score) {
        if (score >= 70) return "#4CAF50";    // Green
        else if (score >= 40) return "#FFC107"; // Yellow
        else return "#F44336";                 // Red
    }
}
