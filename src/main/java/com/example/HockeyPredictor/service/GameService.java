package com.example.HockeyPredictor.service;

import com.example.HockeyPredictor.model.Game;
import com.example.HockeyPredictor.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    public Optional<Game> getGameById(Long id) {
        return gameRepository.findById(id);
    }

    public Game createGame(Game game) {
        return gameRepository.save(game);
    }

    public Game updateGame(Long id, Game updatedGame) {
        return gameRepository.findById(id)
                .map(game -> {
                    game.setHomeTeam(updatedGame.getHomeTeam());
                    game.setAwayTeam(updatedGame.getAwayTeam());
                    game.setHomeTeamGoals(updatedGame.getHomeTeamGoals());
                    game.setAwayTeamGoals(updatedGame.getAwayTeamGoals());
                    game.setDate(updatedGame.getDate());
                    return gameRepository.save(game);
                }).orElseThrow(() -> new RuntimeException("Game not found with id " + id));
    }

    public void deleteGame(Long id) {
        gameRepository.deleteById(id);
    }
}
