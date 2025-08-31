package com.ecotrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

@Service
public class MLIntegrationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    @Value("${ml.service.timeout}")
    private int mlServiceTimeout;

    public Map<String, Object> getHealthPrediction(Map<String, Object> features) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(features, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlServiceUrl + "/predict",
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
        } catch (HttpClientErrorException e) {
            System.err.println("HTTP error when calling ML service: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            System.err.println("Connection timeout or error accessing ML service: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error when calling ML service: " + e.getMessage());
        }

        // Fallback prediction if ML service is unavailable
        return getFallbackPrediction(features);
    }

    private Map<String, Object> getFallbackPrediction(Map<String, Object> features) {
        Map<String, Object> fallback = new HashMap<>();

        // Simple rule-based fallback
        double baseScore = 70.0;

        // Adjust based on AQI if available
        if (features.containsKey("aqi")) {
            int aqi = (Integer) features.get("aqi");
            if (aqi > 150) baseScore -= 30;
            else if (aqi > 100) baseScore -= 20;
            else if (aqi > 50) baseScore -= 10;
        }

        // Adjust based on traffic if available
        if (features.containsKey("traffic_level")) {
            double trafficLevel = (Double) features.get("traffic_level");
            baseScore -= trafficLevel * 15;
        }

        // Ensure score is within bounds
        baseScore = Math.max(0, Math.min(100, baseScore));

        fallback.put("score", baseScore);
        fallback.put("confidence", 0.6);
        fallback.put("method", "fallback");
        fallback.put("timestamp", System.currentTimeMillis());

        return fallback;
    }
}