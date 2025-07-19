package com.ecotrack.controller;

import com.ecotrack.model.HealthProfile;
import com.ecotrack.repository.HealthProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class HealthProfileController {

    public HealthProfileController() {
        System.out.println("HealthProfileController initialized");
    }

    @Autowired
    private HealthProfileRepository repository;

    @PostMapping
    public HealthProfile saveProfile(@RequestBody HealthProfile profile) {
        return repository.save(profile);
    }

    @GetMapping
    public HealthProfile getProfile(@RequestParam String email) {
        return repository.findByUserEmail(email);
    }
}