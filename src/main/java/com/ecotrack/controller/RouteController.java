package com.ecotrack.controller;

import com.ecotrack.dto.RouteOption;
import com.ecotrack.dto.RouteRequest;
import com.ecotrack.dto.RouteResponse;
import com.ecotrack.service.RouteService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/recommend")
    public ResponseEntity<?> recommendRoutes(
            @RequestBody @Valid RouteRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            // Ensure user is authenticated before logging sensitive info
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Authentication required"));
            }

            logger.info("Route request from {}:{} to {}:{} by user {}",
                    request.originLat(), request.originLon(),
                    request.destLat(), request.destLon(),
                    userDetails.getUsername());

            List<RouteOption> routes = routeService.getRouteAlternatives(
                    request.originLon(), request.originLat(),
                    request.destLon(), request.destLat(),
                    userDetails.getUsername()
            );

            return ResponseEntity.ok(new RouteResponse(routes));

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Route calculation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false,
                            "message", "Failed to calculate routes",
                            "details", e.getMessage())
            );
        }
    }
}
