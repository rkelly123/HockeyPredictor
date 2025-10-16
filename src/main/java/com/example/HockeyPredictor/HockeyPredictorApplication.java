package com.example.HockeyPredictor;

import com.example.HockeyPredictor.model.Team;
import com.example.HockeyPredictor.model.Game;
import com.example.HockeyPredictor.repository.TeamRepository;
import com.example.HockeyPredictor.repository.GameRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

@SpringBootApplication
public class HockeyPredictorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HockeyPredictorApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(TeamRepository teamRepository, GameRepository gameRepository) {
        return args -> {
            // Sample teams with all required fields
            Team sharks = new Team(
                    "San Jose Sharks", 5, 2, 1, 20, 15, 200, 180, 150, 100, 50, 40);
            Team penguins = new Team(
                    "Pittsburgh Penguins", 4, 3, 1, 18, 17, 190, 170, 160, 110, 45, 38);
            Team bruins = new Team(
                    "Boston Bruins", 6, 1, 1, 22, 14, 210, 185, 155, 120, 48, 42);
            Team mapleLeafs = new Team(
                    "Toronto Maple Leafs", 3, 4, 1, 17, 19, 180, 160, 148, 105, 52, 36);

            // Save teams
            teamRepository.save(sharks);
            teamRepository.save(penguins);
            teamRepository.save(bruins);
            teamRepository.save(mapleLeafs);

            // Sample games
            Game game1 = new Game(sharks, penguins, 3, 2, LocalDate.of(2025, 10, 15));
            Game game2 = new Game(bruins, mapleLeafs, 1, 4, LocalDate.of(2025, 10, 16));
            Game game3 = new Game(penguins, bruins, 2, 2, LocalDate.of(2025, 10, 17));

            // Save games
            gameRepository.save(game1);
            gameRepository.save(game2);
            gameRepository.save(game3);

            System.out.println("Sample data loaded!");
        };
    }
}
