package sung.worldcupanalysis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sung.worldcupanalysis.model.ApiFootball.FixtureItem;
import sung.worldcupanalysis.model.ApiFootball.FixturesResponse;
import sung.worldcupanalysis.model.ApiFootball.TeamInfo;
import sung.worldcupanalysis.model.FootballData.MatchDto;
import sung.worldcupanalysis.model.FootballData.MatchesResponse;
import sung.worldcupanalysis.model.FootballData.ScoreLine;
import sung.worldcupanalysis.model.FootballData.ScoreDto;
import sung.worldcupanalysis.model.FootballData.TeamRef;

import java.io.InputStream;
import java.util.List;

/**
 * Talks to API-Football (api-sports.io) for the World Cup ({@code league=1}) and
 * maps the response onto our normalized {@link MatchDto} shape. Falls back to
 * bundled sample data when no API key is set or the call fails / returns nothing,
 * so the app is always demonstrable.
 */
@Component
public class ApiFootballClient {

    private static final Logger log = LoggerFactory.getLogger(ApiFootballClient.class);
    private static final String SAMPLE_RESOURCE = "sample/wc-matches.json";

    /** Where the most recently served match list actually came from. */
    public enum DataSource { LIVE, SAMPLE }

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final boolean apiEnabled;
    private final int leagueId;
    private final int season;
    private volatile DataSource lastSource = DataSource.SAMPLE;

    public ApiFootballClient(
            @Value("${apifootball.base-url}") String baseUrl,
            @Value("${apifootball.api-key:}") String apiKey,
            @Value("${apifootball.league}") int leagueId,
            @Value("${apifootball.season}") int season) {
        this.apiEnabled = apiKey != null && !apiKey.isBlank();
        this.leagueId = leagueId;
        this.season = season;

        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (apiEnabled) {
            builder.defaultHeader("x-apisports-key", apiKey);
        }
        this.restClient = builder.build();
    }

    public boolean isApiEnabled() {
        return apiEnabled;
    }

    public DataSource getLastSource() {
        return lastSource;
    }

    /** Returns all World Cup matches, or sample data if the live API is unavailable. */
    public List<MatchDto> getWorldCupMatches() {
        if (!apiEnabled) {
            log.info("APIFOOTBALL_API_KEY not set — serving bundled sample matches.");
            return sample();
        }
        try {
            FixturesResponse response = restClient.get()
                    .uri(uri -> uri.path("/fixtures")
                            .queryParam("league", leagueId)
                            .queryParam("season", season)
                            .build())
                    .retrieve()
                    .body(FixturesResponse.class);

            if (response == null) {
                log.warn("API-Football returned an empty body — falling back to sample data.");
            } else if (hasErrors(response.errors())) {
                log.warn("API-Football returned errors {} — falling back to sample data. "
                        + "On the free plan the World Cup or season {} may not be included.",
                        response.errors(), season);
            } else if (response.response() == null || response.response().isEmpty()) {
                log.warn("API-Football returned 0 fixtures for league {} season {} — falling back to sample data.",
                        leagueId, season);
            } else {
                List<MatchDto> matches = response.response().stream().map(this::toMatch).toList();
                log.info("Loaded {} World Cup matches from API-Football.", matches.size());
                lastSource = DataSource.LIVE;
                return matches;
            }
        } catch (Exception e) {
            log.warn("API-Football request failed ({}); falling back to sample data.", e.getMessage());
        }
        return sample();
    }

    private boolean hasErrors(Object errors) {
        // Success is an empty array []; failure is a non-empty object/array.
        if (errors instanceof java.util.Map<?, ?> map) {
            return !map.isEmpty();
        }
        if (errors instanceof java.util.List<?> list) {
            return !list.isEmpty();
        }
        return false;
    }

    private MatchDto toMatch(FixtureItem item) {
        String round = item.league() != null ? item.league().round() : null;
        return new MatchDto(
                item.fixture().id(),
                item.fixture().date(),
                mapStatus(item.fixture().status() != null ? item.fixture().status().shortCode() : null),
                mapStage(round),
                mapGroup(round),
                toTeamRef(item.teams() != null ? item.teams().home() : null),
                toTeamRef(item.teams() != null ? item.teams().away() : null),
                toScore(item));
    }

    private TeamRef toTeamRef(TeamInfo t) {
        if (t == null) {
            return new TeamRef(null, null, null, null, null);
        }
        return new TeamRef(t.id(), t.name(), t.name(), tla(t.name()), t.logo());
    }

    private static String tla(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String trimmed = name.trim();
        return (trimmed.length() >= 3 ? trimmed.substring(0, 3) : trimmed).toUpperCase();
    }

    private ScoreDto toScore(FixtureItem item) {
        Integer home = item.goals() != null ? item.goals().home() : null;
        Integer away = item.goals() != null ? item.goals().away() : null;
        return new ScoreDto(null, "REGULAR", new ScoreLine(home, away));
    }

    /** API-Football status short codes -> the normalized status codes MatchService understands. */
    private String mapStatus(String shortCode) {
        if (shortCode == null) {
            return "TIMED";
        }
        return switch (shortCode) {
            case "TBD", "NS" -> "TIMED";
            case "1H", "2H", "ET", "BT", "P", "LIVE", "INT" -> "IN_PLAY";
            case "HT" -> "PAUSED";
            case "FT", "AET", "PEN" -> "FINISHED";
            case "PST" -> "POSTPONED";
            case "SUSP" -> "SUSPENDED";
            case "CANC", "ABD", "AWD", "WO" -> "CANCELLED";
            default -> "TIMED";
        };
    }

    private String mapStage(String round) {
        if (round == null) {
            return "";
        }
        String r = round.toLowerCase();
        if (r.contains("group")) {
            return "GROUP_STAGE";
        }
        if (r.contains("round of 16") || r.contains("16")) {
            return "LAST_16";
        }
        if (r.contains("quarter")) {
            return "QUARTER_FINALS";
        }
        if (r.contains("semi")) {
            return "SEMI_FINALS";
        }
        if (r.contains("3rd place") || r.contains("third place")) {
            return "THIRD_PLACE";
        }
        if (r.contains("final")) {
            return "FINAL";
        }
        return round;
    }

    /** Extracts a group letter from rounds like "Group A - 1" -> "GROUP_A". */
    private String mapGroup(String round) {
        if (round == null) {
            return null;
        }
        String r = round.trim();
        if (r.toLowerCase().startsWith("group ") && r.length() >= 7) {
            char letter = Character.toUpperCase(r.charAt(6));
            if (Character.isLetter(letter)) {
                return "GROUP_" + letter;
            }
        }
        return null;
    }

    private List<MatchDto> sample() {
        lastSource = DataSource.SAMPLE;
        return loadSampleMatches();
    }

    private List<MatchDto> loadSampleMatches() {
        try (InputStream in = new ClassPathResource(SAMPLE_RESOURCE).getInputStream()) {
            MatchesResponse response = objectMapper.readValue(in, MatchesResponse.class);
            return response.matches();
        } catch (Exception e) {
            log.error("Failed to load sample matches from {}", SAMPLE_RESOURCE, e);
            return List.of();
        }
    }
}
