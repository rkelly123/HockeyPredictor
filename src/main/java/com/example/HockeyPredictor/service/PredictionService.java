package com.example.HockeyPredictor.service;

import com.example.HockeyPredictor.model.Team;
import org.springframework.stereotype.Service;

@Service
public class PredictionService {

    public String predictGameWithScores(Team teamA, Team teamB) {
        double scoreA = calculateScore(teamA);
        double scoreB = calculateScore(teamB);

        // Round scores to 1 decimal
        scoreA = Math.round(scoreA * 10.0) / 10.0;
        scoreB = Math.round(scoreB * 10.0) / 10.0;

        String winner = predictWinner(teamA, teamB);

        return String.format("%s (%.1f vs. %.1f) - Predicted winner: %s",
                teamA.getName(), scoreA, scoreB, winner);
    }

    public String predictWinner(Team teamA, Team teamB) {
        double scoreA = calculateScore(teamA);
        double scoreB = calculateScore(teamB);

        if (scoreA > scoreB) {
            return teamA.getName();
        } else if (scoreB > scoreA) {
            return teamB.getName();
        } else {
            return "Tie / Too close to call";
        }
    }

    public double calculateScore(Team team) {
        double winPct = team.getWins() / (double)(team.getWins() + team.getLosses() + team.getOvertimeLosses());
        double goalDiff = team.getGoalDifferential() / 50.0;
        double shotsFactor = team.getShotsFor() / 2000.0;
        double corsiFactor = team.getCorsiFor() / 5000.0;

        return (winPct * 0.4) + (goalDiff * 0.3) + (shotsFactor * 0.2) + (corsiFactor * 0.1);
    }
}
