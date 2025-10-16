package com.example.HockeyPredictor.controller;

import com.example.HockeyPredictor.model.Team;
import com.example.HockeyPredictor.repository.TeamRepository;
import com.example.HockeyPredictor.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PredictionController {

    private final TeamRepository teamRepository;
    private final PredictionService predictionService;

    public PredictionController(TeamRepository teamRepository, PredictionService predictionService) {
        this.teamRepository = teamRepository;
        this.predictionService = predictionService;
    }

    @GetMapping("/api/predict")
    public String predictGame(
            @RequestParam Long teamAId,
            @RequestParam Long teamBId) {
        Team teamA = teamRepository.findById(teamAId).orElse(null);
        Team teamB = teamRepository.findById(teamBId).orElse(null);

        if (teamA == null || teamB == null) {
            return "One or both team IDs not found.";
        }

        // Delegate all logic to the service
        return predictionService.predictGameWithScores(teamA, teamB);
    }

}
