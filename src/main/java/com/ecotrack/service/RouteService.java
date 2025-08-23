package com.ecotrack.service;

import com.ecotrack.dto.RouteOption;
import com.ecotrack.model.HealthProfile;
import com.ecotrack.model.User;
import com.ecotrack.repository.HealthProfileRepository;
import com.ecotrack.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteService {
    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);

    @Value("${ors.api.key}")
    private String orsApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HealthProfileRepository healthProfileRepo;
    private final UserRepository userRepository;
    private final AqiService aqiService;

    private static final double MAX_ALTERNATIVE_DISTANCE = 100000; // 100km
    private static final double SEGMENT_DISTANCE = 80000; // 80km segments

    public RouteService(RestTemplate restTemplate, ObjectMapper objectMapper,
                        HealthProfileRepository healthProfileRepo, UserRepository userRepository,
                        AqiService aqiService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.healthProfileRepo = healthProfileRepo;
        this.userRepository = userRepository;
        this.aqiService = aqiService;
    }

    public List<RouteOption> getRouteAlternatives(
            double startLon, double startLat,
            double endLon, double endLat,
            String userEmail
    ) {
        try {
            validateCoordinates(startLat, startLon);
            validateCoordinates(endLat, endLon);

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            HealthProfile profile = healthProfileRepo.findByUser(user)
                    .orElseGet(HealthProfile::new);

            double totalDistance = calculateHaversineDistance(startLat, startLon, endLat, endLon);

            if (totalDistance > MAX_ALTERNATIVE_DISTANCE) {
                logger.info("Long distance detected ({}m), using segmented routing", totalDistance);
                return getSegmentedRoutes(startLon, startLat, endLon, endLat, profile);
            } else {
                return getNormalRoutes(startLon, startLat, endLon, endLat, profile);
            }

        } catch (Exception e) {
            logger.error("Failed to get route alternatives", e);
            throw new RuntimeException("Route calculation failed: " + e.getMessage());
        }
    }

    private List<RouteOption> getNormalRoutes(double startLon, double startLat,
                                              double endLon, double endLat,
                                              HealthProfile profile) {
        try {
            String profileType = determineProfileType(profile);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("coordinates", Arrays.asList(
                    Arrays.asList(startLon, startLat),
                    Arrays.asList(endLon, endLat)
            ));
            requestBody.put("instructions", false);
            requestBody.put("alternative_routes", Map.of(
                    "target_count", 5,
                    "share_factor", 0.7
            ));

            HttpHeaders headers = createHeaders();

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.openrouteservice.org/v2/directions/" + profileType + "/geojson",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("ORS API returned status: " + response.getStatusCode());
            }

            return processRoutes(response.getBody(), profile, startLat, startLon, endLat, endLon);

        } catch (Exception e) {
            logger.warn("Normal routing failed, using fallback: {}", e.getMessage());
            return createFallbackRoutes(startLat, startLon, endLat, endLon, profile);
        }
    }

    private List<RouteOption> getSegmentedRoutes(double startLon, double startLat,
                                                 double endLon, double endLat,
                                                 HealthProfile profile) {
        try {
            List<double[]> segments = calculateRouteSegments(startLat, startLon, endLat, endLon);
            List<RouteOption> allSegmentOptions = new ArrayList<>();

            for (int i = 0; i < segments.size() - 1; i++) {
                double[] currentPoint = segments.get(i);
                double[] nextPoint = segments.get(i + 1);

                List<RouteOption> segmentOptions = getNormalRoutes(
                        currentPoint[1], currentPoint[0], // lon, lat
                        nextPoint[1], nextPoint[0],       // lon, lat
                        profile
                );
                allSegmentOptions.addAll(segmentOptions);
            }

            return combineSegmentsIntoRoutes(allSegmentOptions, segments.size() - 1);

        } catch (Exception e) {
            logger.warn("Segmented routing failed, using fallback: {}", e.getMessage());
            return createFallbackRoutes(startLat, startLon, endLat, endLon, profile);
        }
    }

    private List<double[]> calculateRouteSegments(double startLat, double startLon,
                                                  double endLat, double endLon) {
        List<double[]> segments = new ArrayList<>();
        segments.add(new double[]{startLat, startLon});

        double totalDistance = calculateHaversineDistance(startLat, startLon, endLat, endLon);
        int numSegments = (int) Math.ceil(totalDistance / SEGMENT_DISTANCE);

        if (numSegments <= 1) {
            segments.add(new double[]{endLat, endLon});
            return segments;
        }

        for (int i = 1; i < numSegments; i++) {
            double fraction = (double) i / numSegments;
            double intermediateLat = startLat + fraction * (endLat - startLat);
            double intermediateLon = startLon + fraction * (endLon - startLon);
            segments.add(new double[]{intermediateLat, intermediateLon});
        }

        segments.add(new double[]{endLat, endLon});
        return segments;
    }

    private List<RouteOption> combineSegmentsIntoRoutes(List<RouteOption> segmentOptions, int numSegments) {
        List<RouteOption> combinedRoutes = new ArrayList<>();

        combinedRoutes.add(createCombinedRoute(segmentOptions, "health-optimized"));

        combinedRoutes.add(createCombinedRoute(
                segmentOptions.stream()
                        .sorted(Comparator.comparingDouble(RouteOption::distance))
                        .collect(Collectors.toList()),
                "distance-optimized"
        ));

        combinedRoutes.add(createCombinedRoute(
                segmentOptions.stream()
                        .sorted((r1, r2) -> Integer.compare(r2.aqi(), r1.aqi()))
                        .collect(Collectors.toList()),
                "aqi-optimized"
        ));

        return combinedRoutes;
    }

    private RouteOption createCombinedRoute(List<RouteOption> segments, String routeType) {
        double totalDistance = segments.stream().mapToDouble(RouteOption::distance).sum();
        double totalDuration = segments.stream().mapToDouble(RouteOption::duration).sum();
        int avgAqi = (int) segments.stream().mapToInt(RouteOption::aqi).average().orElse(50);

        double healthScore = calculateHealthScore(totalDistance, totalDuration, avgAqi, new HealthProfile());

        return new RouteOption(
                "combined-" + routeType,
                healthScore,
                "combined-polyline",
                getColor(healthScore),
                totalDistance,
                totalDuration,
                avgAqi
        );
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", orsApiKey);
        return headers;
    }

    private void validateCoordinates(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid coordinates provided");
        }
    }

    private String determineProfileType(HealthProfile profile) {
        if (Boolean.TRUE.equals(profile.getPreferGreenRoutes())) {
            return "foot-walking";
        }
        return "driving-car";
    }

    private List<RouteOption> processRoutes(Map<String, Object> responseBody, HealthProfile profile,
                                            double startLat, double startLon, double endLat, double endLon) {
        List<RouteOption> options = new ArrayList<>();
        try {
            List<Map<String, Object>> features = (List<Map<String, Object>>) responseBody.get("features");
            if (features == null || features.isEmpty()) {
                return options;
            }

            for (int i = 0; i < features.size(); i++) {
                Map<String, Object> feature = features.get(i);
                Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
                Map<String, Object> properties = (Map<String, Object>) feature.get("properties");

                List<List<Double>> coordinates = (List<List<Double>>) geometry.get("coordinates");
                Map<String, Object> summary = (Map<String, Object>) properties.get("summary");

                double distance = ((Number) summary.get("distance")).doubleValue();
                double duration = ((Number) summary.get("duration")).doubleValue();
                int avgAqi = calculateRouteAQI(coordinates);

                if (shouldAvoidRoute(avgAqi, profile)) continue;

                String polyline = objectMapper.writeValueAsString(coordinates);
                double healthScore = calculateHealthScore(distance, duration, avgAqi, profile);

                options.add(new RouteOption(
                        "route-" + i,
                        healthScore,
                        polyline,
                        getColor(healthScore),
                        distance,
                        duration,
                        avgAqi
                ));
            }
        } catch (Exception e) {
            logger.error("Error processing routes", e);
        }

        return options.stream()
                .sorted((r1, r2) -> Double.compare(r2.healthScore(), r1.healthScore()))
                .limit(3)
                .collect(Collectors.toList());
    }

    private int calculateRouteAQI(List<List<Double>> coordinates) {
        try {
            List<Integer> aqiReadings = new ArrayList<>();
            for (int i = 0; i < coordinates.size(); i += 5) {
                List<Double> coord = coordinates.get(i);
                double lat = coord.get(1);
                double lon = coord.get(0);
                int aqi = aqiService.getAqi(lat, lon);
                if (aqi > 0) aqiReadings.add(aqi);
            }
            if (aqiReadings.isEmpty()) return 50;
            return (int) aqiReadings.stream().mapToInt(Integer::intValue).average().orElse(50);
        } catch (Exception e) {
            logger.warn("Error calculating route AQI, using fallback", e);
            return 50;
        }
    }

    private boolean shouldAvoidRoute(int aqi, HealthProfile profile) {
        return profile.getPreferredMaxAqi() != null && aqi > profile.getPreferredMaxAqi();
    }

    private double calculateHealthScore(double distance, double duration, int aqi, HealthProfile profile) {
        double distanceScore = Math.max(0, 1 - (distance / 10000));
        double durationScore = Math.max(0, 1 - (duration / 3600));
        double aqiScore = Math.max(0, 1 - (aqi / 300.0));

        double sensitivityMultiplier = profile.getSensitivityLevel() != null
                ? profile.getSensitivityLevel().getMultiplier() : 1.0;

        double baseScore = (distanceScore * 0.3 + durationScore * 0.3 + aqiScore * 0.4) * 100;
        double adjustedScore = baseScore * sensitivityMultiplier;

        if (profile.getHasRespiratoryIssues()) adjustedScore *= 0.9;
        if (Boolean.TRUE.equals(profile.getPreferGreenRoutes()) && aqi < 50) adjustedScore *= 1.1;

        return Math.max(0, Math.min(100, adjustedScore));
    }

    private List<RouteOption> createFallbackRoutes(double startLat, double startLon,
                                                   double endLat, double endLon, HealthProfile profile) {
        List<RouteOption> fallbackRoutes = new ArrayList<>();
        double distance = calculateHaversineDistance(startLat, startLon, endLat, endLon);

        for (int i = 0; i < 3; i++) {
            int estimatedAqi = 50 + (i * 25);
            double routeDistance = distance * (1 + (i * 0.1));
            double routeDuration = (distance / 1000) * (2 + (i * 0.5));

            double healthScore = calculateHealthScore(routeDistance, routeDuration, estimatedAqi, profile);

            fallbackRoutes.add(new RouteOption(
                    "fallback-" + i,
                    healthScore,
                    generateSimplePolyline(startLat, startLon, endLat, endLon, i),
                    getColor(healthScore),
                    routeDistance,
                    routeDuration,
                    estimatedAqi
            ));
        }
        return fallbackRoutes;
    }

    private String generateSimplePolyline(double startLat, double startLon,
                                          double endLat, double endLon, int variant) {
        if (variant == 0) {
            return String.format("[[%.6f,%.6f],[%.6f,%.6f]]", startLon, startLat, endLon, endLat);
        } else {
            double midLat = (startLat + endLat) / 2 + variant * 0.01;
            double midLon = (startLon + endLon) / 2 - variant * 0.01;
            return String.format("[[%.6f,%.6f],[%.6f,%.6f],[%.6f,%.6f]]",
                    startLon, startLat, midLon, midLat, endLon, endLat);
        }
    }

    private String getColor(double score) {
        if (score >= 80) return "#4CAF50";
        if (score >= 60) return "#8BC34A";
        if (score >= 40) return "#FFC107";
        if (score >= 20) return "#FF9800";
        return "#F44336";
    }
}
