package sung.worldcupanalysis.service;

import org.springframework.stereotype.Service;
import sung.worldcupanalysis.model.FootballData.MatchDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computes group standings from finished matches and resolves bracket slot
 * labels ("X조 1위" / "X조 2위") to actual team names once a group is complete.
 * The computation is a pure static function so it is easy to unit-test.
 *
 * Tiebreakers: points, then goal difference, then goals for, then name
 * (a simplification of FIFA's full tiebreaker list).
 */
@Service
public class StandingsService {

    private static final Pattern SLOT = Pattern.compile("^([A-L])조 ([12])위$");

    public record Row(String team, int played, int points, int goalsFor, int goalsAgainst, int goalDiff) {
    }

    public record GroupTable(List<Row> rows, boolean complete) {
    }

    /** A group's third-placed team and its standing in the cross-group wildcard race. */
    public record ThirdPlace(String group, Row row, int rank, boolean qualified) {
    }

    private final MatchService matchService;

    public StandingsService(MatchService matchService) {
        this.matchService = matchService;
    }

    public Map<String, GroupTable> tables() {
        return computeTables(matchService.getAllMatches());
    }

    /** Builds a standings table per group letter from the given matches. */
    public static Map<String, GroupTable> computeTables(List<MatchDto> matches) {
        Map<String, List<MatchDto>> byGroup = new java.util.TreeMap<>(); // A..L order
        for (MatchDto m : matches) {
            String g = m.group();
            if (g == null || !g.startsWith("GROUP_")) {
                continue;
            }
            byGroup.computeIfAbsent(g.substring("GROUP_".length()), k -> new ArrayList<>()).add(m);
        }

        Map<String, GroupTable> result = new LinkedHashMap<>();
        byGroup.forEach((letter, groupMatches) -> {
            boolean complete = !groupMatches.isEmpty() && groupMatches.stream().allMatch(StandingsService::isFinished);

            Map<String, int[]> stat = new LinkedHashMap<>(); // team -> [played, points, gf, ga]
            for (MatchDto m : groupMatches) {
                if (!isFinished(m)) {
                    continue;
                }
                String home = m.homeTeam().name();
                String away = m.awayTeam().name();
                int hg = m.score().fullTime().home();
                int ag = m.score().fullTime().away();

                int[] h = stat.computeIfAbsent(home, k -> new int[4]);
                int[] a = stat.computeIfAbsent(away, k -> new int[4]);
                h[0]++; a[0]++;
                h[2] += hg; h[3] += ag;
                a[2] += ag; a[3] += hg;
                if (hg > ag) {
                    h[1] += 3;
                } else if (hg < ag) {
                    a[1] += 3;
                } else {
                    h[1]++; a[1]++;
                }
            }

            List<Row> rows = new ArrayList<>();
            stat.forEach((team, s) -> rows.add(new Row(team, s[0], s[1], s[2], s[3], s[2] - s[3])));
            rows.sort(Comparator.comparingInt(Row::points).reversed()
                    .thenComparing(Comparator.comparingInt(Row::goalDiff).reversed())
                    .thenComparing(Comparator.comparingInt(Row::goalsFor).reversed())
                    .thenComparing(Row::team));

            result.put(letter, new GroupTable(rows, complete));
        });
        return result;
    }

    /**
     * Ranks each group's third-placed team across all groups (points, then goal
     * difference, goals for, name). The top 8 are flagged as qualifying — the
     * 2026 format advances the 8 best third-placed teams.
     */
    public static List<ThirdPlace> thirdPlaceRanking(Map<String, GroupTable> tables) {
        record GroupThird(String group, Row row) {
        }
        List<GroupThird> thirds = new ArrayList<>();
        tables.forEach((letter, table) -> {
            if (table.rows().size() >= 3) {
                thirds.add(new GroupThird(letter, table.rows().get(2)));
            }
        });
        thirds.sort(Comparator.comparingInt((GroupThird x) -> x.row().points()).reversed()
                .thenComparing(Comparator.comparingInt((GroupThird x) -> x.row().goalDiff()).reversed())
                .thenComparing(Comparator.comparingInt((GroupThird x) -> x.row().goalsFor()).reversed())
                .thenComparing(x -> x.row().team()));

        List<ThirdPlace> result = new ArrayList<>();
        for (int i = 0; i < thirds.size(); i++) {
            result.add(new ThirdPlace(thirds.get(i).group(), thirds.get(i).row(), i + 1, i < 8));
        }
        return result;
    }

    /**
     * Resolves a bracket slot label to a team name if determinable, else returns
     * the label unchanged. Only "X조 1위" / "X조 2위" for a completed group resolve;
     * third-place slots and knockout "Mxx 승자" labels are left as-is.
     */
    public static String resolveSlot(String label, Map<String, GroupTable> tables) {
        Matcher matcher = SLOT.matcher(label);
        if (!matcher.matches()) {
            return label;
        }
        GroupTable table = tables.get(matcher.group(1));
        int position = Integer.parseInt(matcher.group(2)); // 1 or 2
        if (table != null && table.complete() && table.rows().size() >= position) {
            return table.rows().get(position - 1).team();
        }
        return label;
    }

    private static boolean isFinished(MatchDto m) {
        return "FINISHED".equalsIgnoreCase(m.status())
                && m.score() != null && m.score().fullTime() != null
                && m.score().fullTime().home() != null && m.score().fullTime().away() != null;
    }
}
