package com.ecotrack.controller;

import com.ecotrack.model.HealthProfile;
import com.ecotrack.model.User;
import com.ecotrack.repository.HealthProfileRepository;
import com.ecotrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<HealthProfile> getProfile(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        HealthProfile profile = healthProfileRepository.findByUser(user)
                .orElse(new HealthProfile());

        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<HealthProfile> saveProfile(Principal principal,
                                                     @RequestBody HealthProfile healthProfile) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if profile already exists for this user
        Optional<HealthProfile> existingProfile = healthProfileRepository.findByUser(user);

        if (existingProfile.isPresent()) {
            // Update existing profile
            HealthProfile profileToUpdate = existingProfile.get();
            updateProfileFields(profileToUpdate, healthProfile);
            HealthProfile savedProfile = healthProfileRepository.save(profileToUpdate);
            return ResponseEntity.ok(savedProfile);
        } else {
            // Create new profile
            healthProfile.setUser(user);
            HealthProfile savedProfile = healthProfileRepository.save(healthProfile);
            return ResponseEntity.ok(savedProfile);
        }
    }

    // Helper method to update profile fields
    private void updateProfileFields(HealthProfile existing, HealthProfile updated) {
        if (updated.getAge() != null) {
            existing.setAge(updated.getAge());
        }
        if (updated.getSensitivityLevel() != null) {
            existing.setSensitivityLevel(updated.getSensitivityLevel());
        }
        if (updated.getHealthConditions() != null) {
            existing.setHealthConditions(updated.getHealthConditions());
        }
        if (updated.getHasRespiratoryIssues() != null) {
            existing.setHasRespiratoryIssues(updated.getHasRespiratoryIssues());
        }
        if (updated.getHasCardiovascularIssues() != null) {
            existing.setHasCardiovascularIssues(updated.getHasCardiovascularIssues());
        }
        if (updated.getIsPregnant() != null) {
            existing.setIsPregnant(updated.getIsPregnant());
        }
        if (updated.getHasAllergies() != null) {
            existing.setHasAllergies(updated.getHasAllergies());
        }
        if (updated.getPreferredMaxAqi() != null) {
            existing.setPreferredMaxAqi(updated.getPreferredMaxAqi());
        }
        if (updated.getAvoidOutbreakZones() != null) {
            existing.setAvoidOutbreakZones(updated.getAvoidOutbreakZones());
        }
        if (updated.getPreferGreenRoutes() != null) {
            existing.setPreferGreenRoutes(updated.getPreferGreenRoutes());
        }
    }

    // You can remove the /update endpoint as it's redundant now
}