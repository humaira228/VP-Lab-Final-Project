package com.ecotrack.controller;

import com.ecotrack.dto.RouteOption;
import com.ecotrack.dto.RouteResponse;
import com.ecotrack.service.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/route")
public class RouteController {
    private static final Logger logger = LoggerFactory.getLogger(RouteController.class);
    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/recommend")
    public ResponseEntity<?> recommendRoutes(
            @RequestParam double startLon,
            @RequestParam double startLat,
            @RequestParam double endLon,
            @RequestParam double endLat
    ) {
        try {
            logger.info("Fetching routes from {}:{} to {}:{}", startLat, startLon, endLat, endLon);
            List<RouteOption> routes = routeService.getRouteAlternatives(startLon, startLat, endLon, endLat);
            return ResponseEntity.ok(new RouteResponse(routes));
        } catch (Exception e) {
            logger.error("Error in recommendRoutes", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}