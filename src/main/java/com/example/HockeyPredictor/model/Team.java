package com.example.HockeyPredictor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int wins;
    private int losses;
    private int overtimeLosses;
    private int points;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifferential;
    private int shotsFor;
    private int shotsAgainst;
    private int hits;
    private int powerplays;
    private int penalties;
    private double powerplayPercentage;
    private double penaltyKillPercentage;
    private double savePercentage;
    private int giveaways;
    private int takeaways;

    private int corsiFor;
    private int fenwickFor;
    private int corsiAgainst;
    private int fenwickAgainst;
    private int opponentsCorsiFor;
    private int opponentsFenwickFor;

    // Constructors
    public Team() {}

    public Team(String name, int wins, int losses, int overtimeLosses, int goalsFor, int goalsAgainst,
            int shotsFor, int shotsAgainst, int hits, int powerplays, int penalties,
            double powerplayPercentage, double penaltyKillPercentage, double savePercentage, int giveaways,
            int takeaways, int corsiFor, int fenwickFor, int corsiAgainst, int fenwickAgainst, int opponentsCorsiFor,
            int opponentsFenwickFor) {
        this.name = name;
        this.wins = wins;
        this.losses = losses;
        this.overtimeLosses = overtimeLosses;
        this.points = wins * 2 + overtimeLosses; // Points calculation
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
        this.goalDifferential = goalsFor - goalsAgainst;
        this.corsiFor = corsiFor;
        this.fenwickFor = fenwickFor;
        this.corsiAgainst = corsiAgainst;
        this.fenwickAgainst = fenwickAgainst;
        this.opponentsCorsiFor = opponentsCorsiFor;
        this.opponentsFenwickFor = opponentsFenwickFor;
        this.shotsFor = shotsFor;
        this.shotsAgainst = shotsAgainst;
        this.hits = hits;
        this.powerplays = powerplays;
        this.penalties = penalties;
        this.powerplayPercentage = powerplayPercentage;
        this.penaltyKillPercentage = penaltyKillPercentage;
        this.savePercentage = savePercentage;
        this.giveaways = giveaways;
        this.takeaways = takeaways;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getOvertimeLosses() { return overtimeLosses; }
    public void setOvertimeLosses(int overtimeLosses) { this.overtimeLosses = overtimeLosses; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getGoalsFor() { return goalsFor; }
    public void setGoalsFor(int goalsFor) { this.goalsFor = goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public void setGoalsAgainst(int goalsAgainst) { this.goalsAgainst = goalsAgainst; }
    public int getGoalDifferential() { return goalDifferential; }
    public void setGoalDifferential(int goalDifferential) { this.goalDifferential = goalDifferential; }
    public int getCorsiFor() { return corsiFor; }
    public void setCorsiFor(int corsiFor) { this.corsiFor = corsiFor; }
    public int getFenwickFor() { return fenwickFor; }
    public void setFenwickFor(int fenwickFor) { this.fenwickFor = fenwickFor; }
    public int getCorsiAgainst() { return corsiAgainst; }
    public void setCorsiAgainst(int corsiAgainst) { this.corsiAgainst = corsiAgainst; }
    public int getFenwickAgainst() { return fenwickAgainst; }
    public void setFenwickAgainst(int fenwickAgainst) { this.fenwickAgainst = fenwickAgainst; }
    public int getOpponentsCorsiFor() { return opponentsCorsiFor; }
    public void setOpponentsCorsiFor(int opponentCorsiFor) { this.opponentsCorsiFor = opponentCorsiFor; }
    public int getOpponentsFenwickFor() { return opponentsFenwickFor; }
    public void setOpponentsFenwickFor(int opponentsFenwickFor) { this.opponentsFenwickFor = opponentsFenwickFor; }
    public int getShotsFor() { return shotsFor; }
    public void setShotsFor(int shotsFor) { this.shotsFor = shotsFor; }
    public int getShotsAgainst() { return shotsAgainst; }
    public void setShotsAgainst(int shotsAgainst) { this.shotsAgainst = shotsAgainst; }
    public int getHits() { return hits; }
    public void setHits(int hits) { this.hits = hits; }
    public int getPowerplays() { return powerplays; }
    public void setPowerplays(int powerplays) { this.powerplays = powerplays; }
    public int getPenalties() { return penalties; }
    public void setPenalties(int penalties) { this.penalties = penalties; }
    public double getPowerplayPercentage() { return powerplayPercentage; }
    public void setPowerplayPercentage(double powerplayPercentage) { this.powerplayPercentage = powerplayPercentage; }
    public double getPenaltyKillPercentage() { return penaltyKillPercentage; }
    public void setPenaltyKillPercentage(double penaltyKillPercentage) { this.penaltyKillPercentage = penaltyKillPercentage; }
    public double getSavePercentage() { return savePercentage; }
    public void setSavePercentage(double savePercentage) { this.savePercentage = savePercentage; }
    public int getGiveaways() { return giveaways; }
    public void setGiveaways(int giveaways) { this.giveaways = giveaways; }
    public int getTakeaways() { return takeaways; }
    public void setTakeaways(int takeaways) { this.takeaways = takeaways; }
}
