package com.ecotrack.dto;

import java.util.List;

public class RouteRecommendationResponse {
    private List<RouteInfo> routes;

    public RouteRecommendationResponse(List<RouteInfo> routes) {
        this.routes = routes;
    }

    public List<RouteInfo> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteInfo> routes) {
        this.routes = routes;
    }

    public static class RouteInfo {
        private String routeId;
        private double healthScore;
        private String polyline;
        private String color;

        public RouteInfo(String routeId, double healthScore, String polyline, String color) {
            this.routeId = routeId;
            this.healthScore = healthScore;
            this.polyline = polyline;
            this.color = color;
        }

        public String getRouteId() { return routeId; }
        public double getHealthScore() { return healthScore; }
        public String getPolyline() { return polyline; }
        public String getColor() { return color; }

        public void setRouteId(String routeId) { this.routeId = routeId; }
        public void setHealthScore(double healthScore) { this.healthScore = healthScore; }
        public void setPolyline(String polyline) { this.polyline = polyline; }
        public void setColor(String color) { this.color = color; }
    }
}
