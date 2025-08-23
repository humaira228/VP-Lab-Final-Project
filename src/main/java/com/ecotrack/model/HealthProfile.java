package com.ecotrack.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "health_profiles")
public class HealthProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore // Prevent infinite loop when serializing
    private User user;

    private Integer age;

    @Enumerated(EnumType.STRING)
    private PollutionSensitivityLevel sensitivityLevel = PollutionSensitivityLevel.MODERATE;

    @ElementCollection
    @CollectionTable(name = "health_conditions", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "condition")
    private List<String> healthConditions = new ArrayList<>();

    private Boolean hasRespiratoryIssues = false;
    private Boolean hasCardiovascularIssues = false;
    private Boolean isPregnant = false;
    private Boolean hasAllergies = false;

    private Integer preferredMaxAqi = 100;
    private Boolean avoidOutbreakZones = true;
    private Boolean preferGreenRoutes = true;

    // Getters and Setters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public PollutionSensitivityLevel getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(PollutionSensitivityLevel sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }
    public List<String> getHealthConditions() { return healthConditions; }
    public void setHealthConditions(List<String> healthConditions) { this.healthConditions = healthConditions; }
    public Boolean getHasRespiratoryIssues() { return hasRespiratoryIssues; }
    public void setHasRespiratoryIssues(Boolean hasRespiratoryIssues) { this.hasRespiratoryIssues = hasRespiratoryIssues; }
    public Boolean getHasCardiovascularIssues() { return hasCardiovascularIssues; }
    public void setHasCardiovascularIssues(Boolean hasCardiovascularIssues) { this.hasCardiovascularIssues = hasCardiovascularIssues; }
    public Boolean getIsPregnant() { return isPregnant; }
    public void setIsPregnant(Boolean isPregnant) { this.isPregnant = isPregnant; }
    public Boolean getHasAllergies() { return hasAllergies; }
    public void setHasAllergies(Boolean hasAllergies) { this.hasAllergies = hasAllergies; }
    public Integer getPreferredMaxAqi() { return preferredMaxAqi; }
    public void setPreferredMaxAqi(Integer preferredMaxAqi) { this.preferredMaxAqi = preferredMaxAqi; }
    public Boolean getAvoidOutbreakZones() { return avoidOutbreakZones; }
    public void setAvoidOutbreakZones(Boolean avoidOutbreakZones) { this.avoidOutbreakZones = avoidOutbreakZones; }
    public Boolean getPreferGreenRoutes() { return preferGreenRoutes; }
    public void setPreferGreenRoutes(Boolean preferGreenRoutes) { this.preferGreenRoutes = preferGreenRoutes; }

    public enum PollutionSensitivityLevel {
        LOW(1.0), MODERATE(1.2), HIGH(1.5), EXTREME(2.0);

        private final double multiplier;

        PollutionSensitivityLevel(double multiplier) {
            this.multiplier = multiplier;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }
}
