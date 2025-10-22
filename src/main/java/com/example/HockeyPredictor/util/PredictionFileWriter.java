package com.example.HockeyPredictor.util;

import com.example.HockeyPredictor.dto.GamePredictionResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PredictionFileWriter {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final String FOLDER_NAME = "GamePredictions";

    public static void writeDailyPredictions(List<GamePredictionResult> results, LocalDate date) throws IOException {
        File folder = new File(FOLDER_NAME);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String filename = FILE_DATE_FORMAT.format(date) + ".txt";
        File out = new File(folder, filename);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out, false))) {
            for (GamePredictionResult r : results) {
                writer.write(String.format("%s Vs. %s", r.getHomeTeam(), r.getAwayTeam()));
                writer.newLine();
                writer.write(String.format("Winner: %s (%s)", r.getPredictedWinner(), r.getAmericanOdds()));
                writer.newLine();
                if (r.getNotes() != null && !r.getNotes().isEmpty()) {
                    writer.write("Notes: " + r.getNotes());
                    writer.newLine();
                }
                writer.write("");
                writer.newLine();
            }
        }
    }
}
