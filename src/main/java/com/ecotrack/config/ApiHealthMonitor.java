package com.ecotrack.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ApiHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ApiHealthMonitor.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, ApiStatus> apiStatuses = new HashMap<>();

    private static class ApiStatus {
        boolean healthy;
        long responseTime;
        String lastChecked;
        int errorCount;
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void monitorApis() {
        checkApiHealth("AQICN", "https://api.waqi.info/feed/geo:40.7128;-74.0060/?token=21c93d78e792c675f9daa4529cfddb2faab0977a");
        checkApiHealth("TomTom", "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json?point=40.7128,-74.0060&key=l4HsFmKVhcn1ptQV6Gw1xG6bkBlU1V4h");
        checkApiHealth("OpenRouteService", "https://api.openrouteservice.org/v2/directions/driving-car?api_key=eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjM3OGIxODQ0MDcwMTRkMmJiMjFkZDY1YjFmYTExMGZmIiwiaCI6Im11cm11cjY0In0=");
    }

    private void checkApiHealth(String apiName, String url) {
        try {
            long startTime = System.currentTimeMillis();
            var response = restTemplate.getForEntity(url, String.class);
            long responseTime = System.currentTimeMillis() - startTime;

            ApiStatus status = new ApiStatus();
            status.healthy = response.getStatusCode().is2xxSuccessful();
            status.responseTime = responseTime;
            status.lastChecked = java.time.Instant.now().toString();
            status.errorCount = 0;

            apiStatuses.put(apiName, status);

            logger.info("{} API health: {} ({}ms)", apiName, status.healthy, responseTime);

        } catch (Exception e) {
            ApiStatus status = apiStatuses.getOrDefault(apiName, new ApiStatus());
            status.healthy = false;
            status.errorCount++;
            status.lastChecked = java.time.Instant.now().toString();

            apiStatuses.put(apiName, status);
            logger.warn("{} API unhealthy: {}", apiName, e.getMessage());
        }
    }

    public Map<String, ApiStatus> getApiStatuses() {
        return new HashMap<>(apiStatuses);
    }

    public boolean isSystemHealthy() {
        return apiStatuses.values().stream()
                .filter(status -> !status.healthy)
                .count() < 2; // Allow one API to be down
    }
}