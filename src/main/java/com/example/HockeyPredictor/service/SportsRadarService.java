package com.example.HockeyPredictor.service;

import com.example.HockeyPredictor.model.Team;
import com.example.HockeyPredictor.model.Game;
import com.example.HockeyPredictor.repository.TeamRepository;
import com.example.HockeyPredictor.repository.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Month;
import java.util.Iterator;
import java.util.Optional;

/**
 * SportsRadarService - fetches Teams, Team analytics, and Games from Sportradar
 * and populates Team and Game entities.
 *
 * NOTE: Ensure SPORTRADAR_API_KEY environment variable is set before running.
 */
@Service
public class SportsRadarService {

    private static final Logger log = LoggerFactory.getLogger(SportsRadarService.class);

    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl = "https://api.sportradar.com/nhl/trial/v7/en";

    // --- Rate-limit handling configuration ---
    private static final int MAX_RATE_LIMIT_RETRIES = 3;
    private static final long RATE_LIMIT_WAIT_MS = 60_000; // 60 seconds

    public SportsRadarService(TeamRepository teamRepository, GameRepository gameRepository) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        // Read API key from environment variable - fallback to null if not present
        String key = System.getenv("SPORTRADAR_API_KEY");
        if (key == null || key.isBlank()) {
            log.error("SPORTRADAR_API_KEY environment variable is not set. Set it before running the app.");
        }
        this.apiKey = key;
    }

    /** Season year calculation: July -> next June uses the earlier year */
    private int getSeasonYear() {
        LocalDate now = LocalDate.now();
        return (now.getMonthValue() >= Month.JULY.getValue()) ? now.getYear() : now.getYear() - 1;
    }

    /** GET helper returning JsonNode with rate-limit retry logic */
    private JsonNode getJson(String url) throws Exception {
        int retries = 0;

        while (true) {
            try {
                String resp = restTemplate.getForObject(url, String.class);
                return objectMapper.readTree(resp);

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    retries++;
                    if (retries >= MAX_RATE_LIMIT_RETRIES) {
                        log.error("Hit rate limit {} times consecutively. Terminating fetch for this request.", retries);
                        throw e;
                    }
                    log.warn("Rate limited on request ({}). Pausing to avoid rate limit... Retry {}/{}",
                            url, retries, MAX_RATE_LIMIT_RETRIES);
                    Thread.sleep(RATE_LIMIT_WAIT_MS);
                    continue;
                }
                // Other HTTP errors are not retried
                throw e;
            }
        }
    }

    /**
     * Fetch league teams and populate basic records (market + name -> Team.name)
     * then fetch per-team analytics and set stats for each Team.
     */
    public void fetchAndPopulateTeamsWithAnalytics() {
        if (apiKey == null) {
            log.error("API key missing; aborting fetchAndPopulateTeamsWithAnalytics.");
            return;
        }

        try {
            // Get all teams (league teams)
            String teamsUrl = String.format("%s/league/teams.json?api_key=%s", baseUrl, apiKey);
            JsonNode teamsRoot = getJson(teamsUrl);
            JsonNode teamsArray = teamsRoot.path("teams");
            if (!teamsArray.isArray()) {
                log.warn("teams array not found in teams response");
                return;
            }

            int seasonYear = getSeasonYear();

            // iterate league teams, create or update Team basic info
            for (JsonNode tnode : teamsArray) {
                String teamId = tnode.path("id").asText(null);
                String market = tnode.path("market").asText("");
                String name = tnode.path("name").asText("");
                String fullName = (market.isEmpty() ? name : market + " " + name);

                // find existing team by exact name; if not found, try contains ignoring case
                Optional<Team> opt = teamRepository.findByName(fullName);
                Team team = opt.orElseGet(() -> teamRepository.findByNameContainingIgnoreCase(name).orElse(new Team()));

                team.setName(fullName); // e.g., "Colorado Avalanche"

                // Get analytics for this team by teamId
                try {
                    String analyticsUrl = String.format("%s/seasons/%d/REG/teams/%s/analytics.json?api_key=%s",
                            baseUrl, seasonYear, teamId, apiKey);
                    JsonNode analyticsRoot = getJson(analyticsUrl);
                    String statisticsUrl = String.format("%s/seasons/%d/REG/teams/%s/statistics.json?api_key=%s",
                            baseUrl, seasonYear, teamId, apiKey);
                    JsonNode statisticsRoot = getJson(statisticsUrl);

                    // The Analytics API puts season stats under:
                    // own_record.statistics.total  (and opponents.statistics.total)
                    JsonNode analyticsOwnTotal = analyticsRoot.path("own_record").path("statistics").path("total");
                    JsonNode analyticsAgainstTotal = analyticsRoot.path("opponents").path("statistics").path("total");
                    if (analyticsOwnTotal.isMissingNode()) {
                        log.warn("No analytics own_record.statistics.total for team {} (id={})", fullName, teamId);
                    } else {
                        team.setCorsiFor(analyticsOwnTotal.path("corsi_for").asInt(team.getCorsiFor()));
                        team.setFenwickFor(analyticsOwnTotal.path("fenwick_for").asInt(team.getFenwickFor()));
                        team.setCorsiAgainst(analyticsOwnTotal.path("corsi_against").asInt(team.getCorsiFor()));
                        team.setFenwickAgainst(analyticsOwnTotal.path("fenwick_against").asInt(team.getFenwickFor()));
                        team.setOpponentsCorsiFor(analyticsAgainstTotal.path("corsi_for").asInt(team.getOpponentsCorsiFor()));
                        team.setOpponentsFenwickFor(analyticsAgainstTotal.path("fenwick_for").asInt(team.getOpponentsFenwickFor()));
                    }

                    // The Statistics API puts stats under own_record.statistics.total
                    JsonNode statisticsOwnTotal = statisticsRoot.path("own_record").path("statistics").path("total");
                    JsonNode statisticsPowerplayTotal = statisticsRoot.path("own_record").path("statistics").path("powerplay");
                    JsonNode statisticsPenaltyTotal = statisticsRoot.path("own_record").path("statistics").path("shorthanded");
                    JsonNode statisticsGoaltendingTotal = statisticsRoot.path("own_record").path("goaltending").path("total");
                    if (statisticsOwnTotal.isMissingNode()) {
                        log.warn("No statistics own_record.statistics.total for team {} (id={})", fullName, teamId);
                    } else{
                        team.setGoalsFor(statisticsOwnTotal.path("goals").asInt(team.getGoalsFor()));
                        team.setPenalties(statisticsOwnTotal.path("penalties").asInt(team.getPenalties()));
                        team.setPowerplays(statisticsOwnTotal.path("powerplays").asInt(team.getPowerplays()));
                        team.setHits(statisticsOwnTotal.path("hits").asInt(team.getHits()));
                        team.setGiveaways(statisticsOwnTotal.path("giveaways").asInt(team.getGiveaways()));
                        team.setTakeaways(statisticsOwnTotal.path("takeaways").asInt(team.getTakeaways()));
                        team.setShotsFor(statisticsOwnTotal.path("shots").asInt(team.getShotsFor()));
                        team.setPowerplayPercentage(statisticsPowerplayTotal.path("percentage").asDouble(team.getPowerplayPercentage()));
                        team.setPenaltyKillPercentage(statisticsPenaltyTotal.path("kill_pct").asDouble(team.getPenaltyKillPercentage()));
                        team.setWins(statisticsGoaltendingTotal.path("wins").asInt(team.getWins()));
                        team.setLosses(statisticsGoaltendingTotal.path("losses").asInt(team.getLosses()));
                        team.setOvertimeLosses(statisticsGoaltendingTotal.path("overtime_losses").asInt(team.getOvertimeLosses()));
                        team.setGoalsAgainst(statisticsGoaltendingTotal.path("goals_against").asInt(team.getGoalsAgainst()));
                        team.setShotsAgainst(statisticsGoaltendingTotal.path("shots_against").asInt(team.getShotsAgainst()));
                        team.setSavePercentage(statisticsGoaltendingTotal.path("saves_pct").asDouble(team.getSavePercentage()));

                        team.setGoalDifferential(team.getGoalsFor() - team.getGoalsAgainst());
                        team.setPoints(team.getWins() * 2 + team.getOvertimeLosses());
                    }

                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        log.warn("Rate limited while fetching analytics for team {}. Pausing before retry.", fullName);
                        try {
                            Thread.sleep(RATE_LIMIT_WAIT_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue; // will retry next iteration automatically
                    } else {
                        log.warn("Failed to fetch analytics for team {} (id={}): {}", fullName, teamId, e.getMessage());
                    }
                } catch (Exception ex) {
                    log.warn("Failed to fetch analytics for team {} (id={}): {}", fullName, teamId, ex.getMessage());
                }

                teamRepository.save(team);
            }

            log.info("Teams + analytics updated from SportsRadar.");
        } catch (Exception e) {
            log.error("Error fetching teams from SportsRadar: ", e);
        }
    }

    /** Fetch schedule for a specific date (default today) */
    public void fetchAndPopulateGamesForToday() {
        if (apiKey == null) {
            log.error("API key missing; aborting fetchAndPopulateGamesForToday.");
            return;
        }

        try {
            LocalDate today = LocalDate.now();
            int y = today.getYear();
            int m = today.getMonthValue();
            int d = today.getDayOfMonth();

            String scheduleUrl = String.format("%s/games/%d/%02d/%02d/schedule.json?api_key=%s",
                    baseUrl, y, m, d, apiKey);

            JsonNode scheduleRoot = getJson(scheduleUrl);
            JsonNode gamesNode = scheduleRoot.path("games");
            if (!gamesNode.isArray() || gamesNode.size() == 0) {
                log.info("No scheduled games found for {}", today);
                return;
            }

            Iterator<JsonNode> it = gamesNode.elements();
            while (it.hasNext()) {
                JsonNode g = it.next();
                JsonNode homeNode = g.path("home");
                JsonNode awayNode = g.path("away");

                String homeNameRaw = homeNode.path("name").asText(null);
                String awayNameRaw = awayNode.path("name").asText(null);

                Optional<Team> homeOpt = Optional.empty();
                Optional<Team> awayOpt = Optional.empty();

                if (homeNameRaw != null) {
                    homeOpt = teamRepository.findByName(homeNameRaw);
                    if (homeOpt.isEmpty()) homeOpt = teamRepository.findByNameContainingIgnoreCase(homeNameRaw);
                }

                if (awayNameRaw != null) {
                    awayOpt = teamRepository.findByName(awayNameRaw);
                    if (awayOpt.isEmpty()) awayOpt = teamRepository.findByNameContainingIgnoreCase(awayNameRaw);
                }

                if (homeOpt.isEmpty() || awayOpt.isEmpty()) {
                    log.warn("Could not match teams for game (home='{}', away='{}'). Skipping.", homeNameRaw, awayNameRaw);
                    continue;
                }

                Team home = homeOpt.get();
                Team away = awayOpt.get();

                Game game = new Game(home, away, 0, 0, today);
                gameRepository.save(game);
                log.debug("Saved game: {} vs {}", home.getName(), away.getName());
            }

            log.info("Games scheduled for {} processed.", LocalDate.now());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Rate limited while fetching game schedule. Pausing to avoid rate limit...");
                try {
                    Thread.sleep(RATE_LIMIT_WAIT_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                log.error("HTTP error fetching schedule: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error fetching schedule: ", e);
        }
    }

    /** Convenience method to update all relevant data. */
    public void updateAllFromSportsRadar() {
        fetchAndPopulateTeamsWithAnalytics();
        fetchAndPopulateGamesForToday();
    }
}
