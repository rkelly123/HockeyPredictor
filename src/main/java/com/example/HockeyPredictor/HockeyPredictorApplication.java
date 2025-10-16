package com.example.HockeyPredictor;

import com.example.HockeyPredictor.model.Team;
import com.example.HockeyPredictor.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HockeyPredictorApplication implements CommandLineRunner {

    @Autowired
    private TeamRepository teamRepository;

    public static void main(String[] args) {
        SpringApplication.run(HockeyPredictorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Create sample teams
        Team team1 = new Team(
                "Sharks",
                10, // wins
                5,  // losses
                2,  // overtimeLosses
                35, // goalsFor
                28, // goalsAgainst
                400, // corsiFor
                380, // fenwickFor
                300, // shotsFor
                150, // hits
                20,  // giveaways
                25   // takeaways
        );

        Team team2 = new Team(
                "Penguins",
                12,
                3,
                1,
                40,
                25,
                420,
                400,
                310,
                160,
                18,
                22
        );

        // Save teams to H2
        teamRepository.save(team1);
        teamRepository.save(team2);

        // Print all teams
        System.out.println("Teams saved:");
        teamRepository.findAll().forEach(t -> 
            System.out.println(
                t.getName() + " | W:" + t.getWins() + " L:" + t.getLosses() +
                " OTL:" + t.getOvertimeLosses() + " GF:" + t.getGoalsFor() +
                " GA:" + t.getGoalsAgainst() + " GD:" + t.getGoalDifferential()
            )
        );
    }
}
