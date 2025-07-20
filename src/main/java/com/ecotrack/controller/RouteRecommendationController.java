package com.ecotrack.controller;

import com.ecotrack.dto.RouteRecommendationResponse;
import com.ecotrack.dto.RouteRecommendationResponse.RouteInfo;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/routes")
public class RouteRecommendationController {

    @GetMapping("/recommend")
    public RouteRecommendationResponse getRecommendedRoutes(Authentication authentication,
                                                            @RequestParam String origin,
                                                            @RequestParam String destination) {
        String email = authentication.getName(); // we can later use this for personalization
        System.out.println("Generating routes for: " + email);

        // Simulated ML logic (placeholder)
        List<RouteInfo> routes = new ArrayList<>();
        routes.add(new RouteInfo("routeA", 83.33, "abc123polyline", "green"));
        routes.add(new RouteInfo("routeB", 58.33, "def456polyline", "yellow"));
        routes.add(new RouteInfo("routeC", 33.33, "ghi789polyline", "red"));

        return new RouteRecommendationResponse(routes);
    }
}
