package com.ecotrack.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

@Service
public class TrafficService {
    private static final Logger logger = LoggerFactory.getLogger(TrafficService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${tomtom.api.key}")
    private String apiKey;

    private final String SNAP_URL = "https://api.tomtom.com/map/1/match?key=%s";
    private final String TRAFFIC_URL = "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json?point=%f,%f&key=%s";

    public List<Double> snapToRoad(double lat, double lon) {
        try {
            String body = String.format("{\"points\":[{\"latitude\":%f,\"longitude\":%f}]}", lat, lon);
            Map response = restTemplate.postForObject(String.format(SNAP_URL, apiKey), body, Map.class);

            List<Map<String, Object>> matchedPoints = (List<Map<String, Object>>) response.get("matchedPoints");
            if (matchedPoints != null && !matchedPoints.isEmpty()) {
                Map<String, Object> point = matchedPoints.get(0);
                double snappedLat = ((Number) point.get("latitude")).doubleValue();
                double snappedLon = ((Number) point.get("longitude")).doubleValue();
                return List.of(snappedLat, snappedLon);
            }
        } catch (Exception e) {
            logger.warn("Snap-to-road failed for lat={}, lon={}: {}", lat, lon, e.getMessage());
        }
        return List.of(lat, lon);
    }

    public int getTraffic(double lat, double lon) {
        try {
            List<Double> snapped = snapToRoad(lat, lon);
            double snappedLat = snapped.get(0);
            double snappedLon = snapped.get(1);

            String url = String.format(TRAFFIC_URL, snappedLat, snappedLon, apiKey);
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("flowSegmentData")) {
                Map<String, Object> flowData = (Map<String, Object>) response.get("flowSegmentData");
                double currentSpeed = ((Number) flowData.get("currentSpeed")).doubleValue();
                double freeFlowSpeed = ((Number) flowData.get("freeFlowSpeed")).doubleValue();

                return (int) Math.max(0, Math.min(100, 100 - (currentSpeed / freeFlowSpeed) * 100));
            }
        } catch (Exception e) {
            logger.warn("Traffic API failed for lat={}, lon={}: {}", lat, lon, e.getMessage());
        }
        return 50;
    }
}