package com.ecotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_routes")
public class SavedRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double startLat;
    private Double startLon;
    private Double endLat;
    private Double endLon;

    @Column(columnDefinition = "text")
    private String polyline;

    private Double healthScore;
    private Double distance; // meters
    private Double duration; // seconds
    private Integer avgAqi;

    private LocalDateTime calculatedAt = LocalDateTime.now();
    private String routeName;

    // Constructors
    public SavedRoute() {}

    public SavedRoute(User user, Double startLat, Double startLon, Double endLat, Double endLon,
                      String polyline, Double healthScore, Double distance, Double duration, Integer avgAqi) {
        this.user = user;
        this.startLat = startLat;
        this.startLon = startLon;
        this.endLat = endLat;
        this.endLon = endLon;
        this.polyline = polyline;
        this.healthScore = healthScore;
        this.distance = distance;
        this.duration = duration;
        this.avgAqi = avgAqi;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Double getStartLat() { return startLat; }
    public void setStartLat(Double startLat) { this.startLat = startLat; }
    public Double getStartLon() { return startLon; }
    public void setStartLon(Double startLon) { this.startLon = startLon; }
    public Double getEndLat() { return endLat; }
    public void setEndLat(Double endLat) { this.endLat = endLat; }
    public Double getEndLon() { return endLon; }
    public void setEndLon(Double endLon) { this.endLon = endLon; }
    public String getPolyline() { return polyline; }
    public void setPolyline(String polyline) { this.polyline = polyline; }
    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }
    public Integer getAvgAqi() { return avgAqi; }
    public void setAvgAqi(Integer avgAqi) { this.avgAqi = avgAqi; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
}