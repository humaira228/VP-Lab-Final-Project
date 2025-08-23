package com.ecotrack.controller;

import com.ecotrack.service.MLService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@CrossOrigin(origins = "*")
public class MLController {

    private final MLService mlService;

    public MLController(MLService mlService) {
        this.mlService = mlService;
    }

    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> result = mlService.predict(payload);
            return ResponseEntity.ok(result);
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
            Map<String, Object> userProfile = (Map<String, Object>) request.get("user_profile");
            Map<String, Object> routeData = (Map<String, Object>) request.get("route_data");
            Map<String, Object> realtimeAqi = (Map<String, Object>) request.get("realtime_aqi");

            Map<String, Object> result = mlService.getHealthAdvice(userProfile, routeData, realtimeAqi);
            return ResponseEntity.ok(result);
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
            Map<String, Object> result = mlService.getPollutionAlerts(location);
            return ResponseEntity.ok(result);
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
            Map<String, Object> status = mlService.getSystemStatus();
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
}