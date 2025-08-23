package com.ecotrack.repository;

import com.ecotrack.model.HealthProfile;
import com.ecotrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {
    Optional<HealthProfile> findByUser(User user);
    Optional<HealthProfile> findByUserEmail(String email);
}