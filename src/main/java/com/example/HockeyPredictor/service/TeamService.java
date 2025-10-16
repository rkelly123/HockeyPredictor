package com.example.HockeyPredictor.service;

import com.example.HockeyPredictor.model.Team;
import com.example.HockeyPredictor.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Optional<Team> getTeamById(Long id) {
        return teamRepository.findById(id);
    }

    public Team createTeam(Team team) {
        return teamRepository.save(team);
    }

    public Team updateTeam(Long id, Team updatedTeam) {
        return teamRepository.findById(id)
                .map(team -> {
                    team.setName(updatedTeam.getName());
                    team.setWins(updatedTeam.getWins());
                    team.setLosses(updatedTeam.getLosses());
                    team.setOvertimeLosses(updatedTeam.getOvertimeLosses());
                    team.setGoalsFor(updatedTeam.getGoalsFor());
                    team.setGoalsAgainst(updatedTeam.getGoalsAgainst());
                    team.setGoalDifferential(updatedTeam.getGoalDifferential());
                    team.setCorsiFor(updatedTeam.getCorsiFor());
                    team.setFenwickFor(updatedTeam.getFenwickFor());
                    team.setShotsFor(updatedTeam.getShotsFor());
                    team.setHits(updatedTeam.getHits());
                    team.setGiveaways(updatedTeam.getGiveaways());
                    team.setTakeaways(updatedTeam.getTakeaways());
                    return teamRepository.save(team);
                }).orElseThrow(() -> new RuntimeException("Team not found with id " + id));
    }

    public void deleteTeam(Long id) {
        teamRepository.deleteById(id);
    }
}
