package com.ecotrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class RealDataService {

    private static final Logger logger = LoggerFactory.getLogger(RealDataService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aqicn.token}")
    private String aqicnToken;

    @Value("${aqicn.api.url}")
    private String aqicnApiUrl;

    @Value("${tomtom.api.key}")
    private String tomtomApiKey;

    @Value("${tomtom.traffic.api.url}")
    private String tomtomTrafficApiUrl;

    @Cacheable(value = "aqiData", key = "{#lat, #lon}", unless = "#result == null")
    public Map<String, Object> getRealTimeAQI(double lat, double lon) {
        try {
            String url = aqicnApiUrl
                    .replace("{lat}", String.valueOf(lat))
                    .replace("{lon}", String.valueOf(lon))
                    .replace("{token}", aqicnToken);

            logger.info("Fetching AQI data from: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.path("status").asText().equals("ok")) {
                JsonNode data = root.path("data");
                Map<String, Object> result = new HashMap<>();

                result.put("aqi", data.path("aqi").asInt());
                result.put("dominant_pollutant", data.path("dominentpol").asText());

                // Parse individual pollutants
                JsonNode iaqi = data.path("iaqi");
                if (iaqi.has("pm25")) result.put("pm25", iaqi.path("pm25").path("v").asDouble());
                if (iaqi.has("pm10")) result.put("pm10", iaqi.path("pm10").path("v").asDouble());
                if (iaqi.has("no2")) result.put("no2", iaqi.path("no2").path("v").asDouble());
                if (iaqi.has("o3")) result.put("o3", iaqi.path("o3").path("v").asDouble());
                if (iaqi.has("so2")) result.put("so2", iaqi.path("so2").path("v").asDouble());
                if (iaqi.has("co")) result.put("co", iaqi.path("co").path("v").asDouble());

                result.put("source", "aqicn");
                result.put("timestamp", System.currentTimeMillis());
                result.put("station", data.path("city").path("name").asText());

                logger.info("Successfully fetched AQI data: {}", result);
                return result;
            }
        } catch (Exception e) {
            logger.error("Failed to fetch AQI data: {}", e.getMessage());
        }
        return getFallbackAQI(lat, lon);
    }

    @Cacheable(value = "trafficData", key = "{#lat, #lon}", unless = "#result == null")
    public Map<String, Object> getTrafficData(double lat, double lon) {
        try {
            String url = tomtomTrafficApiUrl
                    .replace("{lat}", String.valueOf(lat))
                    .replace("{lon}", String.valueOf(lon))
                    .replace("{key}", tomtomApiKey);

            logger.info("Fetching traffic data from: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode segment = root.path("flowSegmentData");

            double currentSpeed = segment.path("currentSpeed").asDouble();
            double freeFlowSpeed = segment.path("freeFlowSpeed").asDouble();
            double confidence = segment.path("confidence").asDouble();

            // Calculate traffic level (0-1 scale)
            double trafficLevel = 1.0 - (currentSpeed / freeFlowSpeed);
            trafficLevel = Math.max(0.1, Math.min(1.0, trafficLevel));

            Map<String, Object> result = new HashMap<>();
            result.put("traffic_level", trafficLevel);
            result.put("current_speed", currentSpeed);
            result.put("free_flow_speed", freeFlowSpeed);
            result.put("confidence", confidence);
            result.put("road_type", segment.path("frc").asText());
            result.put("source", "tomtom");
            result.put("timestamp", System.currentTimeMillis());

            logger.info("Successfully fetched traffic data: {}", result);
            return result;

        } catch (Exception e) {
            logger.error("Failed to fetch traffic data: {}", e.getMessage());
            return getFallbackTrafficData();
        }
    }

    private Map<String, Object> getFallbackAQI(double lat, double lon) {
        Map<String, Object> fallback = new HashMap<>();

        // Simplified urban detection
        boolean isUrban = true;

        int baseAqi = isUrban ? 65 : 45;

        // Adjust for time of day (rush hours have higher pollution)
        int hour = java.time.LocalTime.now().getHour();
        if ((hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 19)) {
            baseAqi += 15;
        }

        fallback.put("aqi", baseAqi);
        fallback.put("pm25", baseAqi / 2.5);
        fallback.put("pm10", baseAqi / 2.2);
        fallback.put("no2", baseAqi / 3.0);
        fallback.put("source", "fallback");
        fallback.put("timestamp", System.currentTimeMillis());
        fallback.put("confidence", 0.5);

        return fallback;
    }

    private Map<String, Object> getFallbackTrafficData() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("traffic_level", 0.5);
        fallback.put("current_speed", 50.0);
        fallback.put("free_flow_speed", 80.0);
        fallback.put("confidence", 0.7);
        fallback.put("source", "fallback");
        fallback.put("timestamp", System.currentTimeMillis());
        return fallback;
    }
}