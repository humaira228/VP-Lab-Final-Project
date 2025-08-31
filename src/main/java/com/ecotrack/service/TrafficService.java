package com.ecotrack.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

@Service
public class TrafficService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Your TomTom API key
    private final String API_KEY = "PMZT3MWjkM4TqhgfDUbCnMtdlxOOfPdV";

    // Snap-to-road API URL
    private final String SNAP_URL = "https://api.tomtom.com/map/1/match?key=%s";

    // Traffic API URL
    private final String TRAFFIC_URL = "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json?point=%f,%f&key=%s";

    /**
     * Snaps the given coordinates to the nearest road using TomTom's Snap-to-Road API.
     * Returns a list [lat, lon].
     */
    public List<Double> snapToRoad(double lat, double lon) {
        try {
            String body = String.format("{\"points\":[{\"latitude\":%f,\"longitude\":%f}]}", lat, lon);
            Map response = restTemplate.postForObject(String.format(SNAP_URL, API_KEY), body, Map.class);

            List<Map<String, Object>> matchedPoints = (List<Map<String, Object>>) response.get("matchedPoints");
            if (matchedPoints != null && !matchedPoints.isEmpty()) {
                Map<String, Object> point = matchedPoints.get(0);
                double snappedLat = ((Number) point.get("latitude")).doubleValue();
                double snappedLon = ((Number) point.get("longitude")).doubleValue();
                return List.of(snappedLat, snappedLon);
            }
        } catch (HttpClientErrorException e) {
            System.out.println("Snap-to-road API error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error in snapToRoad: " + e.getMessage());
        }
        // fallback: return original coordinates
        return List.of(lat, lon);
    }

    /**
     * Get traffic severity (0–100) for given coordinates.
     * 0 = free, 100 = heavy traffic
     */
    public int getTraffic(double lat, double lon) {
        try {
            // Snap to road first
            List<Double> snapped = snapToRoad(lat, lon);
            double snappedLat = snapped.get(0);
            double snappedLon = snapped.get(1);

            String url = String.format(TRAFFIC_URL, snappedLat, snappedLon, API_KEY);
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("flowSegmentData")) {
                Map<String, Object> flowData = (Map<String, Object>) response.get("flowSegmentData");
                double currentSpeed = ((Number) flowData.get("currentSpeed")).doubleValue();
                double freeFlowSpeed = ((Number) flowData.get("freeFlowSpeed")).doubleValue();

                int severity = (int) Math.max(0, Math.min(100, 100 - (currentSpeed / freeFlowSpeed) * 100));
                return severity;
            }
        } catch (HttpClientErrorException e) {
            System.out.println("Traffic API error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error in getTraffic: " + e.getMessage());
        }
        // fallback severity
        return 50;
    }
}