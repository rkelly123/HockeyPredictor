package com.example.HockeyPredictor.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="home_team_id")
    private Team homeTeam;

    @ManyToOne
    @JoinColumn(name="away_team_id")
    private Team awayTeam;

    private int homeTeamGoals;
    private int awayTeamGoals;

    private LocalDate date;

    public Game() {}

    public Game(Team homeTeam, Team awayTeam, int homeTeamGoals, int awayTeamGoals, LocalDate date) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeTeamGoals = homeTeamGoals;
        this.awayTeamGoals = awayTeamGoals;
        this.date = date;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Team getHomeTeam() { return homeTeam; }
    public void setHomeTeam(Team homeTeam) { this.homeTeam = homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public void setAwayTeam(Team awayTeam) { this.awayTeam = awayTeam; }
    public int getHomeTeamGoals() { return homeTeamGoals; }
    public void setHomeTeamGoals(int goals) { this.homeTeamGoals = goals; }
    public int getAwayTeamGoals() { return awayTeamGoals; }
    public void setAwayTeamGoals(int goals) { this.awayTeamGoals = goals; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
