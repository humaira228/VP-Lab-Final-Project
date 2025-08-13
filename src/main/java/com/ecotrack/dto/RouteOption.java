package com.ecotrack.dto;

public class RouteOption {
    private String routeId;
    private double healthScore;
    private String polyline;
    private String color;
    private double distance;
    private double duration;
    private int aqi;

    public RouteOption() {}

    public RouteOption(String routeId, double healthScore, String polyline, String color,
                       double distance, double duration, int aqi) {
        this.routeId = routeId;
        this.healthScore = healthScore;
        this.polyline = polyline;
        this.color = color;
        this.distance = distance;
        this.duration = duration;
        this.aqi = aqi;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(double healthScore) {
        this.healthScore = healthScore;
    }

    public String getPolyline() {
        return polyline;
    }

    public void setPolyline(String polyline) {
        this.polyline = polyline;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getAqi() {
        return aqi;
    }

    public void setAqi(int aqi) {
        this.aqi = aqi;
    }
}
