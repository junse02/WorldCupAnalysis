package sung.worldcupanalysis.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTOs mapping the subset of the API-Football (api-sports.io) v3 {@code /fixtures}
 * response we use. These are mapped onto our normalized {@link FootballData.MatchDto}
 * shape by the client, so the rest of the app is provider-agnostic.
 */
public final class ApiFootball {

    private ApiFootball() {
    }

    /**
     * {@code errors} is typed as {@code Object} because API-Football returns an empty
     * array {@code []} on success but an object {@code {"plan": "..."}} on failure;
     * {@code Object} lets Jackson map either shape (List vs Map) without failing.
     */
    public record FixturesResponse(List<FixtureItem> response, Object errors, int results) {
    }

    public record FixtureItem(Fixture fixture, League league, Teams teams, Goals goals) {
    }

    public record Fixture(long id, String date, Status status) {
    }

    public record Status(
            @JsonProperty("short") String shortCode,
            @JsonProperty("long") String longName) {
    }

    public record League(Long id, String name, Integer season, String round) {
    }

    public record Teams(TeamInfo home, TeamInfo away) {
    }

    public record TeamInfo(Long id, String name, String logo) {
    }

    public record Goals(Integer home, Integer away) {
    }
}
