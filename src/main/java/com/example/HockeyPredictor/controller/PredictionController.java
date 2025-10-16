package com.example.HockeyPredictor.controller;

import com.example.HockeyPredictor.model.Team;
import com.example.HockeyPredictor.repository.TeamRepository;
import com.example.HockeyPredictor.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

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
            @RequestParam Long teamBId
    ) {
        Optional<Team> teamAOpt = teamRepository.findById(teamAId);
        Optional<Team> teamBOpt = teamRepository.findById(teamBId);

        if (teamAOpt.isEmpty() || teamBOpt.isEmpty()) {
            return "One or both team IDs not found.";
        }

        Team teamA = teamAOpt.get();
        Team teamB = teamBOpt.get();

        String winner = predictionService.predictWinner(teamA, teamB);
        return "Predicted winner: " + winner;
    }
}
