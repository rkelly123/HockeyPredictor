package com.example.HockeyPredictor.repository;

import com.example.HockeyPredictor.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
    Optional<Team> findByNameContainingIgnoreCase(String name);
}
