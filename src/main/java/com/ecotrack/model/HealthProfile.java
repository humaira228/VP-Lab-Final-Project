package com.ecotrack.model;

import jakarta.persistence.*;

@Entity
public class HealthProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;

    private double pollutionSensitivity;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public double getPollutionSensitivity() {
        return pollutionSensitivity;
    }

    public void setPollutionSensitivity(double pollutionSensitivity) {
        this.pollutionSensitivity = pollutionSensitivity;
    }
}