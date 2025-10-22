package com.example.HockeyPredictor.dto;

public class GamePredictionResult {
    private Long gameId;
    private String homeTeam;
    private String awayTeam;
    private String predictedWinner;
    private double probability;       // 0.0 - 1.0
    private String americanOdds;      // e.g. "-155" or "+210"
    private String notes;

    public GamePredictionResult() {}

    public GamePredictionResult(Long gameId, String homeTeam, String awayTeam, String predictedWinner,
                                double probability, String americanOdds, String notes) {
        this.gameId = gameId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.predictedWinner = predictedWinner;
        this.probability = probability;
        this.americanOdds = americanOdds;
        this.notes = notes;
    }

    // getters / setters

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }

    public String getPredictedWinner() { return predictedWinner; }
    public void setPredictedWinner(String predictedWinner) { this.predictedWinner = predictedWinner; }

    public double getProbability() { return probability; }
    public void setProbability(double probability) { this.probability = probability; }

    public String getAmericanOdds() { return americanOdds; }
    public void setAmericanOdds(String americanOdds) { this.americanOdds = americanOdds; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
