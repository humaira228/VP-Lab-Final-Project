package com.ecotrack.controller;

import com.ecotrack.service.MLIntegrationService;
import com.ecotrack.service.RealDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@CrossOrigin(origins = "*")
public class MLController {

    private final MLIntegrationService mlIntegrationService;
    private final RealDataService realDataService;

    public MLController(MLIntegrationService mlIntegrationService, RealDataService realDataService) {
        this.mlIntegrationService = mlIntegrationService;
        this.realDataService = realDataService;
    }

    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody Map<String, Object> payload) {
        try {
            // Validate required parameters
            if (!payload.containsKey("lat") || !payload.containsKey("lon")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Missing required parameters: lat and lon",
                        "timestamp", System.currentTimeMillis()
                ));
            }

            // Extract coordinates
            double lat = Double.parseDouble(payload.get("lat").toString());
            double lon = Double.parseDouble(payload.get("lon").toString());

            // Get real-time environmental data
            Map<String, Object> aqiData = realDataService.getRealTimeAQI(lat, lon);
            Map<String, Object> trafficData = realDataService.getTrafficData(lat, lon);

            // Combine all data for ML prediction
            Map<String, Object> features = new HashMap<>();
            features.putAll(payload);
            features.putAll(aqiData);
            features.putAll(trafficData);

            // Get prediction from ML service
            Map<String, Object> result = mlIntegrationService.getHealthPrediction(features);

            // Enhance response with additional metadata
            Map<String, Object> enhancedResponse = new HashMap<>();
            enhancedResponse.put("success", true);
            enhancedResponse.put("prediction", result);
            enhancedResponse.put("model_version", "2.0");
            enhancedResponse.put("model_type", "ensemble");
            enhancedResponse.put("timestamp", System.currentTimeMillis());
            enhancedResponse.put("data_sources", new String[]{"aqicn", "tomtom"});

            // Add health recommendations based on score
            double score = (Double) result.get("score");
            enhancedResponse.put("health_recommendations",
                    generateHealthRecommendations(score, payload));

            return ResponseEntity.ok(enhancedResponse);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Prediction failed",
                    "message", ex.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @PostMapping("/health-advice")
    public ResponseEntity<?> getHealthAdvice(@RequestBody Map<String, Object> request) {
        try {
            // For now, implement a simple rule-based approach since the ML service
            // doesn't have this specific method anymore
            Map<String, Object> userProfile = (Map<String, Object>) request.get("user_profile");
            Map<String, Object> routeData = (Map<String, Object>) request.get("route_data");
            Map<String, Object> realtimeAqi = (Map<String, Object>) request.get("realtime_aqi");

            // Create a combined feature set
            Map<String, Object> features = new HashMap<>();
            if (userProfile != null) features.putAll(userProfile);
            if (routeData != null) features.putAll(routeData);
            if (realtimeAqi != null) features.putAll(realtimeAqi);

            // Get prediction
            Map<String, Object> result = mlIntegrationService.getHealthPrediction(features);

            // Generate advice based on the prediction
            double score = (Double) result.get("score");
            Map<String, Object> adviceResponse = new HashMap<>();
            adviceResponse.put("score", score);
            adviceResponse.put("advice", generateHealthRecommendations(score, features));
            adviceResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(adviceResponse);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Health advice failed",
                    "message", ex.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @PostMapping("/pollution-alerts")
    public ResponseEntity<?> getPollutionAlerts(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> location = (Map<String, Object>) request.get("location");

            if (location == null || !location.containsKey("lat") || !location.containsKey("lon")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Missing location data with lat and lon",
                        "timestamp", System.currentTimeMillis()
                ));
            }

            double lat = Double.parseDouble(location.get("lat").toString());
            double lon = Double.parseDouble(location.get("lon").toString());

            // Get AQI data
            Map<String, Object> aqiData = realDataService.getRealTimeAQI(lat, lon);
            int aqi = (Integer) aqiData.get("aqi");

            // Generate alerts based on AQI levels
            Map<String, Object> alertResponse = new HashMap<>();
            alertResponse.put("aqi", aqi);
            alertResponse.put("alerts", generatePollutionAlerts(aqi));
            alertResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(alertResponse);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Pollution alerts failed",
                    "message", ex.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSystemStatus() {
        try {
            // Create a simple status response since we don't have the Python script check anymore
            Map<String, Object> status = new HashMap<>();
            status.put("ml_service", "active");
            status.put("api_services", "active");
            status.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(status);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Status check failed",
                    "message", ex.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "ML Service",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/api-status")
    public ResponseEntity<?> getApiStatus() {
        try {
            // Test API connectivity
            boolean aqicnAvailable = testAqicnApi();
            boolean tomtomAvailable = testTomTomApi();

            Map<String, Object> apiStatus = Map.of(
                    "aqicn_api", aqicnAvailable,
                    "tomtom_api", tomtomAvailable,
                    "ml_service", true,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(apiStatus);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "API status check failed",
                    "message", ex.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    private boolean testAqicnApi() {
        try {
            Map<String, Object> result = realDataService.getRealTimeAQI(40.7128, -74.0060);
            return result != null && !"fallback".equals(result.get("source"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testTomTomApi() {
        try {
            Map<String, Object> result = realDataService.getTrafficData(40.7128, -74.0060);
            return result != null && !"fallback".equals(result.get("source"));
        } catch (Exception e) {
            return false;
        }
    }

    private String[] generateHealthRecommendations(double score, Map<String, Object> input) {
        if (score < 30) {
            return new String[]{
                    "Avoid outdoor activities due to poor air quality",
                    "Consider using a N95 mask if going outside",
                    "Keep windows closed and use air purifiers"
            };
        } else if (score < 50) {
            return new String[]{
                    "Limit prolonged outdoor activities",
                    "Sensitive groups should reduce outdoor exposure"
            };
        } else if (score < 70) {
            return new String[]{
                    "Moderate air quality - acceptable for most people"
            };
        } else {
            return new String[]{
                    "Good air quality - enjoy your activities"
            };
        }
    }

    private String[] generatePollutionAlerts(int aqi) {
        if (aqi > 200) {
            return new String[]{
                    "VERY UNHEALTHY: Everyone should avoid outdoor exertion",
                    "Health warning of emergency conditions"
            };
        } else if (aqi > 150) {
            return new String[]{
                    "UNHEALTHY: Everyone may begin to experience health effects",
                    "Sensitive groups should avoid outdoor exertion"
            };
        } else if (aqi > 100) {
            return new String[]{
                    "UNHEALTHY FOR SENSITIVE GROUPS: People with respiratory issues should limit outdoor exertion"
            };
        } else if (aqi > 50) {
            return new String[]{
                    "MODERATE: Air quality is acceptable"
            };
        } else {
            return new String[]{
                    "GOOD: Air quality is satisfactory"
            };
        }
    }
}