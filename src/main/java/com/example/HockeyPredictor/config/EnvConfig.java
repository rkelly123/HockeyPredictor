package com.example.HockeyPredictor.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    public static String getSportsRadarApiKey() {
        return dotenv.get("SPORTRADAR_API_KEY");
    }
}
