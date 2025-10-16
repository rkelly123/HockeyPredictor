package com.example.HockeyPredictor.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Team teamA;

    @ManyToOne
    private Team teamB;

    private LocalDate gameDate;

    // Constructors
    public Game() {}

    public Game(Team teamA, Team teamB, LocalDate gameDate) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.gameDate = gameDate;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public Team getTeamA() { return teamA; }
    public void setTeamA(Team teamA) { this.teamA = teamA; }
    public Team getTeamB() { return teamB; }
    public void setTeamB(Team teamB) { this.teamB = teamB; }
    public LocalDate getGameDate() { return gameDate; }
    public void setGameDate(LocalDate gameDate) { this.gameDate = gameDate; }
}
