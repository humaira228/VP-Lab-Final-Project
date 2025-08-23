package com.ecotrack.controller;

import com.ecotrack.model.HealthProfile;
import com.ecotrack.model.User;
import com.ecotrack.repository.HealthProfileRepository;
import com.ecotrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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

        // Set the user for the health profile
        healthProfile.setUser(user);

        // Save the health profile
        HealthProfile savedProfile = healthProfileRepository.save(healthProfile);

        return ResponseEntity.ok(savedProfile);
    }

    @PostMapping("/update")
    public ResponseEntity<HealthProfile> updateProfile(Principal principal,
                                                       @RequestBody HealthProfile healthProfile) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        HealthProfile existingProfile = healthProfileRepository.findByUser(user)
                .orElse(new HealthProfile());

        existingProfile.setHasRespiratoryIssues(healthProfile.getHasRespiratoryIssues());
        existingProfile.setHasCardiovascularIssues(healthProfile.getHasCardiovascularIssues());
        existingProfile.setIsPregnant(healthProfile.getIsPregnant());
        existingProfile.setHasAllergies(healthProfile.getHasAllergies());
        existingProfile.setSensitivityLevel(healthProfile.getSensitivityLevel());
        existingProfile.setPreferredMaxAqi(healthProfile.getPreferredMaxAqi());
        existingProfile.setAvoidOutbreakZones(healthProfile.getAvoidOutbreakZones());
        existingProfile.setPreferGreenRoutes(healthProfile.getPreferGreenRoutes());
        existingProfile.setUser(user);

        HealthProfile savedProfile = healthProfileRepository.save(existingProfile);

        return ResponseEntity.ok(savedProfile);
    }
}
