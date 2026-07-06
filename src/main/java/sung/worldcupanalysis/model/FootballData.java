package sung.worldcupanalysis.model;

import java.util.List;

/**
 * Normalized, provider-agnostic match model used across the app and by the
 * bundled sample data ({@code sample/wc-matches.json}). {@code ApiFootballClient}
 * maps the live API-Football response onto these records.
 */
public final class FootballData {

    private FootballData() {
    }

    public record MatchesResponse(List<MatchDto> matches) {
    }

    public record MatchDto(
            long id,
            String utcDate,
            String status,
            String stage,
            String group,
            TeamRef homeTeam,
            TeamRef awayTeam,
            ScoreDto score,
            OddsDto odds) {
    }

    public record TeamRef(Long id, String name, String shortName, String tla, String crest) {
    }

    /**
     * Pre-match win/draw/loss probabilities (integer percent, summing to 100),
     * derived from bookmaker 1X2 odds with the margin removed. May be null when
     * no odds are available for a fixture.
     */
    public record OddsDto(Integer home, Integer draw, Integer away) {
    }

    public record ScoreDto(String winner, String duration, ScoreLine fullTime, ScoreLine penalties) {
    }

    public record ScoreLine(Integer home, Integer away) {
    }
}
