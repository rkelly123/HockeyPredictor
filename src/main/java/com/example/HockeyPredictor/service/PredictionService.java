package com.example.HockeyPredictor.service;

import com.example.HockeyPredictor.dto.GamePredictionResult;
import com.example.HockeyPredictor.model.Game;
import com.example.HockeyPredictor.model.Team;
import com.example.HockeyPredictor.util.PredictionFileWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PredictionService {

    // Tunable category weights (derived from analytics and hockey metrics studies)
    private static final double WIN_PCT_WEIGHT = 0.30;       // Overall success rate
    private static final double GOAL_DIFF_WEIGHT = 0.12;     // Strong predictor of true talent
    private static final double SAVE_PCT_WEIGHT = 0.08;      // Goaltending stability
    private static final double SPECIAL_TEAMS_WEIGHT = 0.12; // Power play & PK effect
    private static final double SHOTS_FOR_WEIGHT = 0.03;     // Offensive pressure
    private static final double SHOTS_AGAINST_WEIGHT = 0.03; // Defensive consistency
    private static final double CORSI_DIFF_WEIGHT = 0.06;    // Possession control
    private static final double FENWICK_DIFF_WEIGHT = 0.06;  // Shot quality proxy
    private static final double HITS_PEN_WEIGHT = 0.06;      // Physicality vs. discipline
    private static final double TURNOVERS_WEIGHT = 0.08;     // Puck management

    private static final double HOME_ADVANTAGE = 0.06;       // ~5% home edge
    private static final double LOGISTIC_K = 1.25;           // Reasonable steepness for diff→prob

    public List<GamePredictionResult> predictGamesForDate(List<Game> gamesForDate) {
        List<GamePredictionResult> results = new ArrayList<>();

        for (Game g : gamesForDate) {
            Team home = g.getHomeTeam();
            Team away = g.getAwayTeam();
            if (home == null || away == null) continue;

            double ratingHome = computeTeamRating(home, true);
            double ratingAway = computeTeamRating(away, false);

            double diff = ratingHome - ratingAway;
            double probabilityHome = logisticProbability(diff);
            double probabilityAway = 1.0 - probabilityHome;

            String predictedWinner = (probabilityHome > probabilityAway)
                    ? home.getName()
                    : away.getName();

            double winnerProb = Math.max(probabilityHome, probabilityAway);
            String americanOdds = convertProbabilityToAmericanOdds(winnerProb);

            String notes = generateNotes(home, away, ratingHome, ratingAway, probabilityHome, probabilityAway);

            GamePredictionResult res = new GamePredictionResult(
                    g.getId(),
                    home.getName(),
                    away.getName(),
                    predictedWinner,
                    Math.round(winnerProb * 1000.0) / 1000.0,
                    americanOdds,
                    notes
            );
            results.add(res);
        }

        try {
            PredictionFileWriter.writeDailyPredictions(results, LocalDate.now());
        } catch (IOException e) {
            System.err.println("Failed to write prediction file: " + e.getMessage());
        }

        return results;
    }

    private double computeTeamRating(Team t, boolean isHome) {
        int gamesPlayed = Math.max(1, t.getWins() + t.getLosses() + t.getOvertimeLosses());
        double averageGoalDiff = 0.6;
        double averageSavePct = 0.900;
        double averageShots = 35.0;
        double magnitudeScaler = 10.0;
        double halfMagnitudeScaler = 5.0;

        // Per-game metrics to stabilize early in season
        double goalsPerGameDiff = (double) t.getGoalDifferential() / gamesPlayed;
        double shotsForPerGame = (double) t.getShotsFor() / gamesPlayed;
        double shotsAgainstPerGame = (double) t.getShotsAgainst() / gamesPlayed;
        double corsiDiffPerGame = (double) (t.getCorsiFor() - t.getCorsiAgainst()) / gamesPlayed;
        double fenwickDiffPerGame = (double) (t.getFenwickFor() - t.getFenwickAgainst()) / gamesPlayed;
        double hitsPerGame = (double) t.getHits() / gamesPlayed;
        double penaltiesPerGame = (double) t.getPenalties() / gamesPlayed;
        double turnoversPerGame = (double) (t.getTakeaways() - t.getGiveaways()) / gamesPlayed;

        // Normalize to approximate real hockey scale
        double winPct = safeRatio(t.getWins(), t.getWins() + t.getLosses() + t.getOvertimeLosses());
        double goalDiff = goalsPerGameDiff / averageGoalDiff;
        double savePct = (t.getSavePercentage() - averageSavePct) / 0.04;
        double specialTeams = ((t.getPowerplayPercentage() + t.getPenaltyKillPercentage()) / 2.0) / 100.0;
        double shotsFor = shotsForPerGame / averageShots;
        double shotsAgainst = ((1.5 * averageShots) - shotsAgainstPerGame) / averageShots; // Get 'half-points' at average
        double corsiDiff = corsiDiffPerGame / magnitudeScaler;
        double fenwickDiff = fenwickDiffPerGame / magnitudeScaler;
        double hitsPen = (hitsPerGame / (penaltiesPerGame + 1.0)) / halfMagnitudeScaler;
        double turnovers = turnoversPerGame / magnitudeScaler;

        double rating =
                WIN_PCT_WEIGHT * winPct +
                GOAL_DIFF_WEIGHT * goalDiff +
                SAVE_PCT_WEIGHT * savePct +
                SPECIAL_TEAMS_WEIGHT * specialTeams +
                SHOTS_FOR_WEIGHT * shotsFor +
                SHOTS_AGAINST_WEIGHT * shotsAgainst +
                CORSI_DIFF_WEIGHT * corsiDiff +
                FENWICK_DIFF_WEIGHT * fenwickDiff +
                HITS_PEN_WEIGHT * hitsPen +
                TURNOVERS_WEIGHT * turnovers;

        if (isHome) rating += HOME_ADVANTAGE;

        double progressionFactor = Math.min(1.0, (1 + (gamesPlayed / 10)) / 3.0); // full weight after 30 games, avoid early overreactions
        rating *= progressionFactor;

        // Keep rating in sensible range
        return Math.max(-2, Math.min(2, rating));
    }

    private double safeRatio(int num, int denom) {
        return denom == 0 ? 0.5 : (double) num / denom;
    }

    private double logisticProbability(double diff) {
        return 1.0 / (1.0 + Math.exp(-LOGISTIC_K * diff));
    }

    private String convertProbabilityToAmericanOdds(double p) {
        p = Math.max(0.001, Math.min(0.999, p));
        double odds = (p > 0.5)
                ? -100 * (p / (1 - p))
                : 100 * ((1 - p) / p);
        return String.format("%+d", Math.round(odds));
    }

    private String generateNotes(Team home, Team away, double ratingHome, double ratingAway,
                                 double probHome, double probAway) {
        return String.format(
                "Home rating: %.3f, Away rating: %.3f. HomeProb: %.3f, AwayProb: %.3f.",
                ratingHome, ratingAway, probHome, probAway
        );
    }
}
