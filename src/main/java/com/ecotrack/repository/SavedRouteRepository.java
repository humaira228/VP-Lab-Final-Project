package com.ecotrack.repository;

import com.ecotrack.model.SavedRoute;
import com.ecotrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedRouteRepository extends JpaRepository<SavedRoute, Long> {
    List<SavedRoute> findByUser(User user);
    List<SavedRoute> findByUserEmail(String email);
    void deleteByUserAndId(User user, Long id);
}