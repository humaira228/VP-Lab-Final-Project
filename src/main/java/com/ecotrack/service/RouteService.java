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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteService {
    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);

    @Value("${ors.api.key}")
    private String orsApiKey;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    @Value("${ml.service.timeout}")
    private int mlServiceTimeout;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HealthProfileRepository healthProfileRepo;
    private final UserRepository userRepository;
    private final AqiService aqiService;
    private final TrafficService trafficService;
    private final MLIntegrationService mlIntegrationService;

    private static final double MAX_ALTERNATIVE_DISTANCE = 100000; // 100km
    private static final double SEGMENT_DISTANCE = 80000; // 80km segments

    public RouteService(RestTemplate restTemplate, ObjectMapper objectMapper,
                        HealthProfileRepository healthProfileRepo, UserRepository userRepository,
                        AqiService aqiService, TrafficService trafficService,
                        MLIntegrationService mlIntegrationService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.healthProfileRepo = healthProfileRepo;
        this.userRepository = userRepository;
        this.aqiService = aqiService;
        this.trafficService = trafficService;
        this.mlIntegrationService = mlIntegrationService;
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
            List<RouteOption> normalRoutes;

            if (totalDistance > MAX_ALTERNATIVE_DISTANCE) {
                logger.info("Long distance detected ({}m), using segmented routing", totalDistance);
                normalRoutes = getSegmentedRoutes(startLon, startLat, endLon, endLat, profile);
            } else {
                normalRoutes = getNormalRoutes(startLon, startLat, endLon, endLat, profile);

                if (normalRoutes.isEmpty()) {
                    return createFallbackRoutes(startLat, startLon, endLat, endLon, profile);
                }
            }

            for (int i = 0; i < normalRoutes.size(); i++) {
                RouteOption route = normalRoutes.get(i);
                System.out.println("Route " + i + " average AQI: " + route.aqi());
            }
            for (int i = 0; i < normalRoutes.size(); i++) {
                RouteOption route = normalRoutes.get(i);
                System.out.println("Route " + i + " average traffic: " + route.traffic());
            }

            // Check if all AQIs are the same
            boolean allAqiSame = normalRoutes.stream()
                    .map(RouteOption::aqi)
                    .distinct()
                    .count() == 1;

            RouteOption bestAqiRoute = allAqiSame && normalRoutes.size() >= 1
                    ? normalRoutes.get(0)   // Route 1 (index 0)
                    : normalRoutes.stream()
                    .min(Comparator.comparingInt(RouteOption::aqi))
                    .orElse(normalRoutes.get(0));

            // Check if all traffic values are the same
            boolean allTrafficSame = normalRoutes.stream()
                    .map(RouteOption::traffic)
                    .distinct()
                    .count() == 1;

            RouteOption bestTrafficRoute = allTrafficSame && normalRoutes.size() <= 2
                    ? normalRoutes.get(1)   // Route 2 (index 1)
                    : normalRoutes.stream()
                    .min(Comparator.comparingInt(RouteOption::traffic))
                    .orElse(normalRoutes.get(0));

            // Check if combined scores are all same
            boolean allCombinedSame = normalRoutes.stream()
                    .map(RouteOption::combinedScore)
                    .distinct()
                    .count() == 1;

            RouteOption bestCombinedRoute = allCombinedSame&&normalRoutes.size() <= 3
                    ? normalRoutes.get(2)   // Route 3 (index 2)
                    : normalRoutes.stream()
                    .filter(route -> !route.equals(bestTrafficRoute))
                    .max(Comparator.comparingDouble(RouteOption::combinedScore))
                    .orElse(normalRoutes.get(0));

            // Apply colors and offsets
            RouteOption bestAqiColored = new RouteOption(
                    bestAqiRoute.routeId(), bestAqiRoute.healthScore(),
                    offsetPolyline(bestAqiRoute.polyline(), 0), "#4CAF50",//green color
                    bestAqiRoute.distance(), bestAqiRoute.duration(),
                    bestAqiRoute.aqi(), bestAqiRoute.traffic(),
                    bestAqiRoute.combinedScore()
            );

            RouteOption bestTrafficColored = new RouteOption(
                    bestTrafficRoute.routeId(), bestTrafficRoute.healthScore(),
                    offsetPolyline(bestTrafficRoute.polyline(), 1), "#F44336", // Red color
                    bestTrafficRoute.distance(), bestTrafficRoute.duration(),
                    bestTrafficRoute.aqi(), bestTrafficRoute.traffic(),
                    bestTrafficRoute.combinedScore()
            );

            RouteOption bestCombinedColored = new RouteOption(
                    bestCombinedRoute.routeId(), bestCombinedRoute.healthScore(),
                    offsetPolyline(bestCombinedRoute.polyline(), 2), "#FFC107", // Yellow color
                    bestCombinedRoute.distance(), bestCombinedRoute.duration(),
                    bestCombinedRoute.aqi(), bestCombinedRoute.traffic(),
                    bestCombinedRoute.combinedScore()
            );

            return List.of(bestAqiColored, bestTrafficColored, bestCombinedColored);

        } catch (Exception ex) {
            logger.error("Failed to get route alternatives", ex);
            return createFallbackRoutes(startLat, startLon, endLat, endLon,
                    healthProfileRepo.findByUser(userRepository.findByEmail(userEmail)
                                    .orElseThrow(() -> new RuntimeException("User not found")))
                            .orElseGet(HealthProfile::new));
        }
    }


    private String offsetPolyline(String polyline, int offsetIndex) {
        try {
            List<List<Double>> coords = objectMapper.readValue(polyline, List.class);
            double delta = 0.00005 * offsetIndex;
            for (List<Double> point : coords) {
                point.set(0, point.get(0) + delta);
            }
            return objectMapper.writeValueAsString(coords);
        } catch (Exception e) {
            return polyline;
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
            requestBody.put("geometry_simplify", false);
            requestBody.put("instructions", true);
            requestBody.put("alternative_routes", Map.of(
                    "target_count", 3,
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
            logger.error("ORS API call failed. Falling back to straight-line routes.", e);
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
                        currentPoint[1], currentPoint[0],
                        nextPoint[1], nextPoint[0],
                        profile
                );
                allSegmentOptions.addAll(segmentOptions);
            }

            return combineSegmentsIntoRoutes(allSegmentOptions, segments.size() - 1, profile);

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

    private List<RouteOption> combineSegmentsIntoRoutes(List<RouteOption> segmentOptions, int numSegments, HealthProfile profile) {
        List<RouteOption> combinedRoutes = new ArrayList<>();

        combinedRoutes.add(createCombinedRoute(segmentOptions, "health-optimized", profile));
        combinedRoutes.add(createCombinedRoute(
                segmentOptions.stream()
                        .sorted(Comparator.comparingDouble(RouteOption::distance))
                        .collect(Collectors.toList()),
                "distance-optimized", profile
        ));
        combinedRoutes.add(createCombinedRoute(
                segmentOptions.stream()
                        .sorted((r1, r2) -> Integer.compare(r2.aqi(), r1.aqi()))
                        .collect(Collectors.toList()),
                "aqi-optimized", profile
        ));

        return combinedRoutes;
    }

    private RouteOption createCombinedRoute(List<RouteOption> segments, String routeType, HealthProfile profile) {
        double totalDistance = segments.stream().mapToDouble(RouteOption::distance).sum();
        double totalDuration = segments.stream().mapToDouble(RouteOption::duration).sum();
        int avgAqi = (int) segments.stream().mapToInt(RouteOption::aqi).average().orElse(50);
        int avgTraffic = (int) segments.stream().mapToInt(RouteOption::traffic).average().orElse(50);

        // Use ML-based health score calculation
        double healthScore = calculateMLHealthScore(totalDistance, totalDuration, avgAqi, avgTraffic, profile);
        double combinedScore = (healthScore * 0.7 + (100 - avgTraffic) * 0.3);
        combinedScore = Math.max(0, Math.min(100, combinedScore));

        return new RouteOption(
                "combined-" + routeType,
                healthScore,
                "combined-polyline",
                getColor(healthScore),
                totalDistance,
                totalDuration,
                avgAqi,
                avgTraffic,
                combinedScore
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
            if (features == null || features.isEmpty()) return options;

            for (int i = 0; i < features.size(); i++) {
                Map<String, Object> feature = features.get(i);
                Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
                Map<String, Object> properties = (Map<String, Object>) feature.get("properties");

                List<List<Double>> coordinates = (List<List<Double>>) geometry.get("coordinates");
                Map<String, Object> summary = (Map<String, Object>) properties.get("summary");

                double distance = ((Number) summary.get("distance")).doubleValue();
                double duration = ((Number) summary.get("duration")).doubleValue();

                int avgAqi = calculateRouteAQI(coordinates);
                int avgTraffic = calculateRouteTraffic(coordinates);

                String polyline = objectMapper.writeValueAsString(coordinates);

                // Use ML-based health score calculation
                double healthScore = calculateMLHealthScore(distance, duration, avgAqi, avgTraffic, profile);
                double combinedScore = (healthScore * 0.7 + (100 - avgTraffic) * 0.3);
                combinedScore = Math.max(0, Math.min(100, combinedScore));

                options.add(new RouteOption(
                        "route-" + i,
                        healthScore,
                        polyline,
                        getColor(healthScore),
                        distance,
                        duration,
                        avgAqi,
                        avgTraffic,
                        combinedScore
                ));
            }
        } catch (Exception e) {
            logger.error("Error processing routes", e);
        }
        return options;
    }

    private int calculateRouteAQI(List<List<Double>> coordinates) {
        try {
            List<Integer> aqiReadings = new ArrayList<>();
            for (int i = 0; i < coordinates.size(); i += 50) {
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

    private String getColor(double score) {
        if (score >= 80) return "#4CAF50";
        if (score >= 60) return "#8BC34A";
        if (score >= 40) return "#FFC107";
        if (score >= 20) return "#FF9800";
        return "#F44336";
    }

    // ML-based health score calculation with proper ML service integration
    private double calculateMLHealthScore(double distance, double duration, int aqi, int traffic, HealthProfile profile) {
        try {
            // Prepare features for ML model
            Map<String, Object> features = new HashMap<>();

            // Route characteristics
            features.put("route_distance", distance);
            features.put("route_duration", duration);
            features.put("aqi", aqi);
            features.put("traffic_level", traffic / 100.0); // Convert to 0-1 scale

            // User profile
            features.put("user_age", profile.getAge() != null ? profile.getAge() : 30);
            features.put("has_respiratory_issues", profile.getHasRespiratoryIssues() != null ? profile.getHasRespiratoryIssues() : false);
            features.put("has_cardio_issues", profile.getHasCardiovascularIssues() != null ? profile.getHasCardiovascularIssues() : false);
            features.put("is_pregnant", profile.getIsPregnant() != null ? profile.getIsPregnant() : false);
            features.put("has_allergies", profile.getHasAllergies() != null ? profile.getHasAllergies() : false);
            features.put("sensitivity_level", profile.getSensitivityLevel() != null ? profile.getSensitivityLevel().getMultiplier() : 2);
            features.put("preferred_max_aqi", profile.getPreferredMaxAqi() != null ? profile.getPreferredMaxAqi() : 100);
            features.put("prefer_green_routes", profile.getPreferGreenRoutes() != null ? profile.getPreferGreenRoutes() : true);

            // Time-based features
            int hour = LocalTime.now().getHour();
            DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
            features.put("hour_of_day", hour);
            features.put("is_weekend", dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
            features.put("is_rush_hour", (hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 19));

            // Call ML service with timeout
            Map<String, Object> mlResult = mlIntegrationService.getHealthPrediction(features);

            if (mlResult != null && mlResult.containsKey("score")) {
                return (Double) mlResult.get("score");
            }
        } catch (Exception e) {
            logger.warn("ML service call failed, using fallback calculation: {}", e.getMessage());
        }

        // Fallback to rule-based calculation if ML service fails
        return calculateHealthScore(distance, duration, aqi, profile);
    }

    private double calculateHealthScore(double distance, double duration, int aqi, HealthProfile profile) {
        double distanceScore = Math.max(0, 1 - (distance / 10000));
        double durationScore = Math.max(0, 1 - (duration / 3600));
        double aqiScore = Math.max(0, 1 - (aqi / 300.0));

        // Handle null sensitivity level
        double sensitivityMultiplier = 1.0;
        if (profile.getSensitivityLevel() != null) {
            sensitivityMultiplier = profile.getSensitivityLevel().getMultiplier();
        }

        double baseScore = (distanceScore * 0.3 + durationScore * 0.3 + aqiScore * 0.4) * 100;
        double adjustedScore = baseScore * sensitivityMultiplier;

        if (Boolean.TRUE.equals(profile.getHasRespiratoryIssues())) adjustedScore *= 0.9;
        if (Boolean.TRUE.equals(profile.getPreferGreenRoutes()) && aqi < 50) adjustedScore *= 1.1;

        return Math.max(0, Math.min(100, adjustedScore));
    }

    private List<RouteOption> createFallbackRoutes(double startLat, double startLon,
                                                   double endLat, double endLon, HealthProfile profile) {
        List<RouteOption> fallbackRoutes = new ArrayList<>();
        double distance = calculateHaversineDistance(startLat, startLon, endLat, endLon);

        // Create three distinct fallback routes with different characteristics
        for (int i = 0; i < 3; i++) {
            int estimatedAqi = 40 + (i * 30); // Vary AQI (40, 70, 100)
            int estimatedTraffic = 30 + (i * 25); // Vary traffic (30, 55, 80)
            double routeDistance = distance * (1 + (i * 0.15)); // Vary distance
            double routeDuration = (distance / 1000) * (2 + (i * 0.6)); // Vary duration

            // Use ML-based health score calculation
            double healthScore = calculateMLHealthScore(routeDistance, routeDuration, estimatedAqi, estimatedTraffic, profile);
            double combinedScore = (healthScore * 0.7 + (100 - estimatedTraffic) * 0.3);
            combinedScore = Math.max(0, Math.min(100, combinedScore));

            fallbackRoutes.add(new RouteOption(
                    "fallback-" + i,
                    healthScore,
                    generateSimplePolyline(startLat, startLon, endLat, endLon, i),
                    getColor(healthScore),
                    routeDistance,
                    routeDuration,
                    estimatedAqi,
                    estimatedTraffic,
                    combinedScore
            ));
        }
        return fallbackRoutes;
    }

    private String generateSimplePolyline(double startLat, double startLon,
                                          double endLat, double endLon, int variant) {
        if (variant == 0) {
            return String.format("[[%.6f,%.6f],[%.6f,%.6f]]", startLon, startLat, endLon, endLat);
        } else if (variant == 1) {
            double midLat = (startLat + endLat) / 2 + 0.01;
            double midLon = (startLon + endLon) / 2 - 0.01;
            return String.format("[[%.6f,%.6f],[%.6f,%.6f],[%.6f,%.6f]]",
                    startLon, startLat, midLon, midLat, endLon, endLat);
        } else {
            double midLat1 = (startLat + endLat) / 2 + 0.02;
            double midLon1 = (startLon + endLon) / 2 - 0.02;
            double midLat2 = (startLat + endLat) / 2 - 0.01;
            double midLon2 = (startLon + endLon) / 2 + 0.01;
            return String.format("[[%.6f,%.6f],[%.6f,%.6f],[%.6f,%.6f],[%.6f,%.6f]]",
                    startLon, startLat, midLon1, midLat1, midLon2, midLat2, endLon, endLat);
        }
    }

    private int calculateRouteTraffic(List<List<Double>> coordinates) {
        try {
            if (coordinates.size() < 2) return 50; // Not enough points

            int totalTraffic = 0;
            int segmentCount = 0;

            for (int i = 0; i < coordinates.size() - 1; i++) {
                List<Double> start = coordinates.get(i);
                List<Double> end = coordinates.get(i + 1);

                double startLat = start.get(1);
                double startLon = start.get(0);
                double endLat = end.get(1);
                double endLon = end.get(0);

                // Determine road type for this segment (simulated)
                String roadType = simulateRoadType(startLat, startLon, endLat, endLon);

                // Get traffic for this segment using your partner's TrafficService
                int traffic = trafficService.getTraffic(startLat, startLon, roadType);

                totalTraffic += traffic;
                segmentCount++;
            }

            return segmentCount > 0 ? totalTraffic / segmentCount : 50;

        } catch (Exception e) {
            logger.warn("Error calculating route traffic per segment, using fallback", e);
            return 50;
        }
    }

    // Simulate road type based on lat/lon or randomly for now
    private String simulateRoadType(double startLat, double startLon, double endLat, double endLon) {
        // Simple example: based on distance of segment
        double distance = calculateHaversineDistance(startLat, startLon, endLat, endLon);

        if (distance > 5000) return "highway";     // segments > 5km → highway
        if (distance > 2000) return "primary";     // 2–5 km → primary
        if (distance > 500) return "secondary";    // 0.5–2 km → secondary
        return "residential";                      // short segments → residential
    }
}