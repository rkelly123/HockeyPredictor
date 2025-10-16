package com.example.HockeyPredictor.controller;

import com.example.HockeyPredictor.model.Game;
import com.example.HockeyPredictor.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    // GET /api/games
    @GetMapping
    public List<Game> getAllGames() {
        return gameService.getAllGames();
    }

    // GET /api/games/{id}
    @GetMapping("/{id}")
    public Game getGameById(@PathVariable Long id) {
        return gameService.getGameById(id)
                .orElseThrow(() -> new RuntimeException("Game not found with id " + id));
    }

    // POST /api/games
    @PostMapping
    public Game createGame(@RequestBody Game game) {
        return gameService.createGame(game);
    }

    // PUT /api/games/{id}
    @PutMapping("/{id}")
    public Game updateGame(@PathVariable Long id, @RequestBody Game game) {
        return gameService.updateGame(id, game);
    }

    // DELETE /api/games/{id}
    @DeleteMapping("/{id}")
    public void deleteGame(@PathVariable Long id) {
        gameService.deleteGame(id);
    }
}
