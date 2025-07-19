package com.ecotrack.repository;

import com.ecotrack.model.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {
    HealthProfile findByUserEmail(String email);
}