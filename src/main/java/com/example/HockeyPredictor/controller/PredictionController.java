package com.example.HockeyPredictor.controller;

import com.example.HockeyPredictor.dto.GamePredictionResult;
import com.example.HockeyPredictor.model.Game;
import com.example.HockeyPredictor.repository.GameRepository;
import com.example.HockeyPredictor.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class PredictionController {

    private final GameRepository gameRepository;
    private final PredictionService predictionService;

    public PredictionController(GameRepository gameRepository, PredictionService predictionService) {
        this.gameRepository = gameRepository;
        this.predictionService = predictionService;
    }

    /**
     * Predict all games for today.
     */
    @GetMapping("/api/predict")
    public List<GamePredictionResult> predictToday() {
        LocalDate today = LocalDate.now();
        List<Game> games = gameRepository.findByDate(today);
        return predictionService.predictGamesForDate(games);
    }
}
