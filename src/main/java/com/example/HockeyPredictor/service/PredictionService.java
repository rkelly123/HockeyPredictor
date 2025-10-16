package com.example.HockeyPredictor.service;

import com.example.HockeyPredictor.model.Team;
import org.springframework.stereotype.Service;

@Service
public class PredictionService {

    /**
     * Simple prediction logic based on win percentage
     * Returns the team most likely to win
     */
    public String predictWinner(Team teamA, Team teamB) {
        double winPctA = teamA.getWins() / (double) (teamA.getWins() + teamA.getLosses() + teamA.getOvertimeLosses());
        double winPctB = teamB.getWins() / (double) (teamB.getWins() + teamB.getLosses() + teamB.getOvertimeLosses());

        if (winPctA > winPctB) {
            return teamA.getName();
        } else if (winPctB > winPctA) {
            return teamB.getName();
        } else {
            return "Tie / Too close to call";
        }
    }
}
