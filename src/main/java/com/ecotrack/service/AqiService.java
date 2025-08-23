package com.ecotrack.service;

import com.ecotrack.dto.AqiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@Service
public class AqiService {
    private static final Logger logger = LoggerFactory.getLogger(AqiService.class);

    @Value("${aqicn.token}")
    private String token;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, CachedAqi> aqiCache = new ConcurrentHashMap<>();

    private static class CachedAqi {
        int aqi;
        long timestamp;
        String source;

        CachedAqi(int aqi, String source) {
            this.aqi = aqi;
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 1800000; // 30 minutes
        }
    }

    @Cacheable(value = "aqiData", key = "#lat + ',' + #lon")
    public int getAqi(double lat, double lon) {
        String cacheKey = String.format("%.4f,%.4f", lat, lon);

        // Check manual cache
        CachedAqi cached = aqiCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Using cached AQI data from {}", cached.source);
            return cached.aqi;
        }

        try {
            String url = String.format(
                    "https://api.waqi.info/feed/geo:%s;%s/?token=%s",
                    lat, lon, token
            );

            AqiResponse response = restTemplate.getForObject(url, AqiResponse.class);

            if (response != null && "ok".equalsIgnoreCase(response.status())) {
                int aqi = response.data().aqi();
                aqiCache.put(cacheKey, new CachedAqi(aqi, "WAQI"));
                return aqi;
            }
        } catch (Exception e) {
            logger.warn("WAQI API failed for {}:{}, using estimation", lat, lon, e);
        }

        int estimatedAqi = estimateAqiFromLocationType(lat, lon);
        aqiCache.put(cacheKey, new CachedAqi(estimatedAqi, "ESTIMATION"));
        return estimatedAqi;
    }

    private int estimateAqiFromLocationType(double lat, double lon) {
        if (isDenseUrbanArea(lat, lon)) return 80 + (int)(Math.random() * 40);
        if (isUrbanArea(lat, lon)) return 50 + (int)(Math.random() * 30);
        if (isSuburbanArea(lat, lon)) return 30 + (int)(Math.random() * 20);
        return 20 + (int)(Math.random() * 10);
    }

    private boolean isDenseUrbanArea(double lat, double lon) {
        // Example: major city centers
        return (lat > 40.4 && lat < 41.0 && lon > -74.1 && lon < -73.7); // NYC
    }

    private boolean isUrbanArea(double lat, double lon) { return false; }

    private boolean isSuburbanArea(double lat, double lon) { return false; }

    public Map<String, Object> getAqiAnalysis(double lat, double lon) {
        int aqi = getAqi(lat, lon);
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("aqi", aqi);
        analysis.put("level", getAqiLevel(aqi));
        analysis.put("healthImplications", getHealthImplications(aqi));
        analysis.put("recommendations", getRecommendations(aqi));
        analysis.put("timestamp", System.currentTimeMillis());
        return analysis;
    }

    private String getAqiLevel(int aqi) {
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    private String getHealthImplications(int aqi) {
        if (aqi <= 50) return "Air quality is satisfactory";
        if (aqi <= 100) return "Acceptable air quality";
        if (aqi <= 150) return "Sensitive groups may experience effects";
        if (aqi <= 200) return "Everyone may experience health effects";
        if (aqi <= 300) return "Health alert: serious effects possible";
        return "Emergency conditions: health warnings";
    }

    private List<String> getRecommendations(int aqi) {
        List<String> recommendations = new ArrayList<>();
        if (aqi > 100) {
            recommendations.add("Limit outdoor activities");
            recommendations.add("Keep windows closed");
        }
        if (aqi > 150) {
            recommendations.add("Wear a mask outdoors");
            recommendations.add("Use air purifiers if available");
        }
        if (aqi > 200) {
            recommendations.add("Avoid outdoor activities");
            recommendations.add("Stay indoors with filtered air");
        }
        return recommendations;
    }

    @Scheduled(fixedRate = 1800000)
    @CacheEvict(value = "aqiData", allEntries = true)
    public void clearAqiCache() {
        logger.info("Clearing AQI cache");
        aqiCache.clear();
    }
}
