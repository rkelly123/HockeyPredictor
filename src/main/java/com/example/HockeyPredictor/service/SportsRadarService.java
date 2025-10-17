package com.example.HockeyPredictor.service;

import com.example.HockeyPredictor.model.Team;
import com.example.HockeyPredictor.model.Game;
import com.example.HockeyPredictor.repository.TeamRepository;
import com.example.HockeyPredictor.repository.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
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

    /** Simple GET helper returning JsonNode */
    private JsonNode getJson(String url) throws Exception {
        String resp = restTemplate.getForObject(url, String.class);
        return objectMapper.readTree(resp);
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
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy");

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

                // Get analytics (season stats) for this team by teamId
                try {
                    String analyticsUrl = String.format("%s/seasons/%d/REG/teams/%s/analytics.json?api_key=%s",
                            baseUrl, seasonYear, teamId, apiKey);
                    JsonNode analyticsRoot = getJson(analyticsUrl);

                    // The API puts season stats under:
                    // own_record.statistics.total  (and opponents.statistics.total etc)
                    JsonNode ownTotal = analyticsRoot.path("own_record").path("statistics").path("total");
                    if (ownTotal.isMissingNode()) {
                        // Some endpoints might have a slightly different shape; log and skip
                        log.warn("No own_record.statistics.total for team {} (id={})", fullName, teamId);
                    } else {
                        // Map available fields safely using opt-like methods
                        team.setWins(ownTotal.path("games_played").isMissingNode() ? team.getWins() : ownTotal.path("games_played").asInt(0)); 
                        // Many useful values are nested as e.g. 'corsi_for', 'fenwick_for', 'on_ice_shots_for', etc.
                        team.setCorsiFor(ownTotal.path("corsi_for").asInt(team.getCorsiFor()));
                        team.setFenwickFor(ownTotal.path("fenwick_for").asInt(team.getFenwickFor()));
                        team.setShotsFor(ownTotal.path("on_ice_shots_for").asInt(team.getShotsFor()));
                        team.setGoalsFor(ownTotal.path("goals_for").asInt(team.getGoalsFor()));
                        team.setGoalsAgainst(ownTotal.path("goals_against").asInt(team.getGoalsAgainst()));
                        team.setHits(ownTotal.path("hits").asInt(team.getHits()));
                        team.setGiveaways(ownTotal.path("giveaways").asInt(team.getGiveaways()));
                        team.setTakeaways(ownTotal.path("takeaways").asInt(team.getTakeaways()));

                        // Some endpoints return explicit wins / losses / ot values under 'stat' keys.
                        if (ownTotal.has("wins") || ownTotal.has("losses") || ownTotal.has("overtime_losses")) {
                            team.setWins(ownTotal.path("wins").asInt(team.getWins()));
                            team.setLosses(ownTotal.path("losses").asInt(team.getLosses()));
                            team.setOvertimeLosses(ownTotal.path("overtime_losses").asInt(team.getOvertimeLosses()));
                        } else {
                            // attempt to parse common alternate field names if present
                            team.setWins(ownTotal.path("wins").asInt(team.getWins()));
                            team.setLosses(ownTotal.path("losses").asInt(team.getLosses()));
                            team.setOvertimeLosses(ownTotal.path("ot").asInt(team.getOvertimeLosses()));
                        }
                    }

                    // After mapping, compute derived values:
                    team.setGoalDifferential(team.getGoalsFor() - team.getGoalsAgainst());
                    team.setPoints(team.getWins() * 2 + team.getOvertimeLosses());

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

    /**
     * Fetch schedule for a specific date (by default today's date)
     * Endpoint: /en/games/{year}/{month}/{day}/schedule.json
     * Populate Game entities (home/away team link and date). We do not store game scores
     * for upcoming games; only link to Team objects for prediction.
     */
    public void fetchAndPopulateGamesForToday() {
        if (apiKey == null) {
            log.error("API key missing; aborting fetchAndPopulateGamesForToday.");
            return;
        }

        try {
            LocalDate today = LocalDate.now();
            // build path components
            int y = today.getYear();
            int m = today.getMonthValue();
            int d = today.getDayOfMonth();

            // example: /en/games/2025/10/12/schedule.json
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
                // In your sample, the JSON shape includes "home" and "away" objects with id, name, alias, etc.
                JsonNode homeNode = g.path("home");
                JsonNode awayNode = g.path("away");

                String homeNameRaw = homeNode.path("name").asText(null);
                String awayNameRaw = awayNode.path("name").asText(null);

                // The "team name" we stored was "market + name" (e.g. "Pittsburgh Penguins")
                // The schedule's "name" may be just "Penguins" or may be full; try matching robustly
                Optional<Team> homeOpt = Optional.empty();
                Optional<Team> awayOpt = Optional.empty();

                if (homeNameRaw != null) {
                    // try exact match first, then contains-case-insensitive
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

                // Create game entity. For upcoming games, scores might be 0 or not included.
                // The Game constructor in your model expects (Team, Team, homeGoals, awayGoals, LocalDate)
                Game game = new Game(home, away, 0, 0, today);
                gameRepository.save(game);
                log.debug("Saved game: {} vs {}", home.getName(), away.getName());
            }

            log.info("Games scheduled for {} processed.", LocalDate.now());
        } catch (Exception e) {
            log.error("Error fetching schedule: ", e);
        }
    }

    /**
     * Convenience method to update all relevant data.
     */
    public void updateAllFromSportsRadar() {
        fetchAndPopulateTeamsWithAnalytics();
        fetchAndPopulateGamesForToday();
    }
}
