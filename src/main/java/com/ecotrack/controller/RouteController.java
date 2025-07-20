package com.ecotrack.controller;

import com.ecotrack.dto.*;
import com.ecotrack.model.HealthProfile;
import com.ecotrack.repository.HealthProfileRepository;
import com.ecotrack.service.AqiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private HealthProfileRepository profileRepo;

    @Autowired
    private AqiService aqiService;

    @PostMapping
    public RouteResponse recommendRoutes(@RequestBody RouteRequest request) {
        HealthProfile profile = profileRepo.findByUserEmail(request.userEmail());
        if (profile == null) {
            throw new RuntimeException("Health profile not found for user: " + request.userEmail());
        }

        double factor = profile.getPollutionSensitivity();

        // Average location for AQI
        double centerLat = (request.originLat() + request.destLat()) / 2;
        double centerLon = (request.originLon() + request.destLon()) / 2;

        // Fetch AQI
        int aqi = aqiService.getAqi(centerLat, centerLon);

        //  Compute baseScore from AQI (lower AQI = better score)
        double baseScore = 100 - Math.min(aqi, 100);

        // Modifying base score using health profile factor
        List<RouteOption> routes = List.of(
                new RouteOption("routeA", score(baseScore, factor, 1.0), "abc123polyline", "green"),
                new RouteOption("routeB", score(baseScore, factor, 0.7), "def456polyline", "yellow"),
                new RouteOption("routeC", score(baseScore, factor, 0.4), "ghi789polyline", "red")
        );

        return new RouteResponse(routes);
    }

    private double score(double base, double factor, double modifier) {
        return Math.min(100, Math.max(0, base * modifier / factor));
    }
}
