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

    // Tunable coefficients — chosen by combining domain intuition + analytic indications.
    // Comments explain why each weight has this relative importance.

    private static final double WIN_PCT_WEIGHT      = 0.25;  // Win % is a strong fundamental indicator of team quality.
    private static final double GOAL_DIFF_WEIGHT    = 0.20;  // Goal differential has been shown to explain ~90% of team winning variance. :contentReference[oaicite:0]{index=0}
    private static final double SAVE_PCT_WEIGHT    = 0.10;  // Goalie / team save percentage matters for preventing goals.
    private static final double SPECIAL_TEAMS_WEIGHT = 0.15; // Power play + penalty kill performance influences tight games significantly.
    private static final double SHOTS_FOR_WEIGHT   = 0.06;  // Shots for proxies offensive generation.
    private static final double CORSI_DIFF_WEIGHT  = 0.10;  // Possession metrics (Corsi/Fenwick) indicate control of play. :contentReference[oaicite:1]{index=1}
    private static final double HITS_PEN_WEIGHT    = 0.04;  // Hits vs. penalties reflect physicality/discipline – lower predictive power but usable.
    private static final double SHOTS_AGAINST_WEIGHT = 0.04;// Allowing fewer shots is part of defensive performance.
    private static final double TURNOVERS_WEIGHT   = 0.06;  // Winning the turnover battle imples strong control of the game.

    // Home ice advantage
    private static final double HOME_ADVANTAGE      = 0.05;  // Approx +5% baseline advantage for home team.

    // logistic scale factor for difference -> probability mapping
    private static final double LOGISTIC_K          = 3.5;  // moderate steepness

    /**
     * Predicts all games in the list for a given date, writes to file and returns the results.
     */
    public List<GamePredictionResult> predictGamesForDate(List<Game> gamesForDate) {
        List<GamePredictionResult> results = new ArrayList<>();
        for (Game g : gamesForDate) {
            Team home = g.getHomeTeam();
            Team away = g.getAwayTeam();
            if (home == null || away == null) {
                continue;
            }

            double ratingHome = computeTeamRating(home, /*isHome=*/ true);
            double ratingAway = computeTeamRating(away, /*isHome=*/ false);

            double diff = ratingHome - ratingAway;
            double probabilityHome = logisticProbability(diff);
            double probabilityAway = 1.0 - probabilityHome;

            String predictedWinner;
            double winnerProb;
            if (probabilityHome > probabilityAway) {
                predictedWinner = home.getName();
                winnerProb = probabilityHome;
            } else if (probabilityAway > probabilityHome) {
                predictedWinner = away.getName();
                winnerProb = probabilityAway;
            } else {
                predictedWinner = "Too close to call";
                winnerProb = Math.max(probabilityHome, probabilityAway);
            }

            String americanOdds = convertProbabilityToAmericanOdds(winnerProb);

            String notes = generateNotes(home, away, ratingHome, ratingAway, probabilityHome, probabilityAway, g);

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

    /**
     * Composite team rating. 
     *    Many stats normalized by season-typical values so newer seasons/games still compare.
     */
    private double computeTeamRating(Team t, boolean isHome) {
        int gamesPlayed = t.getWins() + t.getLosses() + t.getOvertimeLosses();
        int totalGames = 84;
        double averageCorsi = 5000.0;
        double averageFenwick = 3000.0;
        double averageShots = 2000.0;
        double averageSavePct = 0.900;
        double averageGoalDifferential = 50.0;

        // Win percentage
        double winPct = safeRatio(t.getWins(), (t.getWins() + t.getLosses() + t.getOvertimeLosses()));

        // Goal differential: scale by ~50 goals per season (for top teams). Hence divide by 50.0.
        // If teams are early in the season, this still normalizes to comparable scale.
        double goalDiff   = t.getGoalDifferential() / ((averageGoalDifferential / totalGames) * gamesPlayed);

        // Save percentage: typically around .900; shift by subtracting .900 then divide by .020 to get ~0–1 scale.
        double savePct    = (t.getSavePercentage() - averageSavePct) / 0.020;

        // Special teams: combine powerplay % and penalty kill %; assume values ~0.15–0.90 scale; average.
        double specialTeams = ((t.getPowerplayPercentage()) + (t.getPenaltyKillPercentage())) / 200.0;

        // Shots for: typical season ~2000 shots; normalize
        double shotsFor   = t.getShotsFor() / ((averageShots / totalGames) * gamesPlayed);

        // Shots against: typical season shots against ~2000; we invert to reward fewer allowed shots
        double shotsAgainst = (((averageShots / totalGames) * gamesPlayed) - t.getShotsAgainst()) / ((averageShots / totalGames) * gamesPlayed);

        // Corsi differential: (CorsiFor – CorsiAgainst) normalized by ~5000 (season attempts)
        double corsiDiff   = ((double)(t.getCorsiFor() - t.getCorsiAgainst())) / ((averageCorsi / totalGames) * gamesPlayed);

        // Fenwick differential: (FenwickFor – FenwickAgainst) normalized by ~3000 attempts
        double fenwickDiff = ((double)(t.getFenwickFor() - t.getFenwickAgainst())) / ((averageFenwick / totalGames) * gamesPlayed);

        // Hits vs penalties: we assume more hits is positive (physical dominance), more penalties is negative:
        double hitsPen    = ((double)t.getHits() / (t.getPenalties() + 1.0)) / 100.0;

        // Opponent Corsi for: fewer opponent Corsi better: normalize ~5000 attempts
        double oppCorsiFor = ((double)(t.getOpponentsCorsiFor())) / ((averageCorsi / totalGames) * gamesPlayed);

        double netTurnovers = ((double)(t.getTakeaways() - t.getGiveaways()));

        // Now build weighted rating
        double rating = 0.0;
        rating += WIN_PCT_WEIGHT         * winPct;
        rating += GOAL_DIFF_WEIGHT       * goalDiff;
        rating += SAVE_PCT_WEIGHT        * savePct;
        rating += SPECIAL_TEAMS_WEIGHT   * specialTeams;
        rating += SHOTS_FOR_WEIGHT       * shotsFor;
        rating += SHOTS_AGAINST_WEIGHT   * shotsAgainst;
        rating += CORSI_DIFF_WEIGHT      * corsiDiff;
        rating += CORSI_DIFF_WEIGHT      * fenwickDiff;
        rating += HITS_PEN_WEIGHT        * hitsPen;
        rating += CORSI_DIFF_WEIGHT      * (1.0 - oppCorsiFor);
        rating += TURNOVERS_WEIGHT       * netTurnovers;

        if (isHome) {
            rating += HOME_ADVANTAGE;
        }

        return rating;
    }

    private double safeRatio(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.5; // unknown -> neutral 0.5
        }
        return ((double) numerator) / denominator;
    }

    private double logisticProbability(double diff) {
        return 1.0 / (1.0 + Math.exp(-LOGISTIC_K * diff));
    }

    private String convertProbabilityToAmericanOdds(double p) {
        if (p <= 0.0) p = 0.001;
        if (p >= 1.0) p = 0.999;
        if (p > 0.5) {
            double odds = (p / (1.0 - p)) * 100.0;
            return String.format("-%d", Math.round(odds));
        } else {
            double odds = ((1.0 - p) / p) * 100.0;
            return String.format("+%d", Math.round(odds));
        }
    }

    private String generateNotes(Team home, Team away, double ratingHome, double ratingAway,
                                 double probHome, double probAway, Game g) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Home rating: %.3f, Away rating: %.3f. ", ratingHome, ratingAway));
        sb.append(String.format("HomeProb: %.3f, AwayProb: %.3f. ", probHome, probAway));
        if (Math.abs(ratingHome - ratingAway) > 0.30) {
            sb.append("Large rating gap. ");
        }
        // Can add extra notes here if desired
        // sb.append(String.format("Season shotsFor home: %d vs away: %d. ", home.getShotsFor(), away.getShotsFor()));
        return sb.toString();
    }
}
