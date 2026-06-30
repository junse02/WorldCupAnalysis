package sung.worldcupanalysis.service;

import org.junit.jupiter.api.Test;
import sung.worldcupanalysis.model.FootballData.MatchDto;
import sung.worldcupanalysis.model.FootballData.ScoreDto;
import sung.worldcupanalysis.model.FootballData.ScoreLine;
import sung.worldcupanalysis.model.FootballData.TeamRef;
import sung.worldcupanalysis.service.StandingsService.GroupTable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingsServiceTest {

    private static MatchDto finished(String group, String home, int hg, int ag, String away) {
        return new MatchDto(0, "2026-06-20T00:00:00Z", "FINISHED", "GROUP_STAGE", group,
                new TeamRef(null, home, home, null, null),
                new TeamRef(null, away, away, null, null),
                new ScoreDto(null, "REGULAR", new ScoreLine(hg, ag)));
    }

    private static MatchDto upcoming(String group, String home, String away) {
        return new MatchDto(0, "2026-06-20T00:00:00Z", "TIMED", "GROUP_STAGE", group,
                new TeamRef(null, home, home, null, null),
                new TeamRef(null, away, away, null, null),
                new ScoreDto(null, "REGULAR", new ScoreLine(null, null)));
    }

    private static final List<MatchDto> COMPLETE_GROUP_A = List.of(
            finished("GROUP_A", "Mexico", 2, 0, "South Africa"),
            finished("GROUP_A", "South Korea", 2, 1, "Czechia"),
            finished("GROUP_A", "Czechia", 1, 1, "South Africa"),
            finished("GROUP_A", "Mexico", 1, 1, "South Korea"),
            finished("GROUP_A", "South Africa", 0, 2, "South Korea"),
            finished("GROUP_A", "Czechia", 0, 1, "Mexico"));

    @Test
    void resolvesGroupPositionsWhenComplete() {
        Map<String, GroupTable> tables = StandingsService.computeTables(COMPLETE_GROUP_A);

        assertTrue(tables.get("A").complete());
        // Korea & Mexico both 7 pts / +3 GD; Korea wins on goals-for (5 vs 4).
        assertEquals("South Korea", tables.get("A").rows().get(0).team());
        assertEquals("Mexico", tables.get("A").rows().get(1).team());
        assertEquals("Czechia", tables.get("A").rows().get(2).team());

        assertEquals("South Korea", StandingsService.resolveSlot("A조 1위", tables));
        assertEquals("Mexico", StandingsService.resolveSlot("A조 2위", tables));
        // Unknown / incomplete group stays a label.
        assertEquals("B조 2위", StandingsService.resolveSlot("B조 2위", tables));
        // Third-place and knockout feeder labels are never resolved here.
        assertEquals("C·E·F·H·I조 3위", StandingsService.resolveSlot("C·E·F·H·I조 3위", tables));
        assertEquals("M74 승자", StandingsService.resolveSlot("M74 승자", tables));
    }

    @Test
    void leavesSlotsUnresolvedWhenGroupIncomplete() {
        // Only one match played -> group not complete.
        Map<String, GroupTable> tables = StandingsService.computeTables(
                List.of(finished("GROUP_A", "Mexico", 2, 0, "South Africa"),
                        upcoming("GROUP_A", "South Korea", "Czechia")));

        assertFalse(tables.get("A").complete());
        assertEquals("A조 1위", StandingsService.resolveSlot("A조 1위", tables));
    }
}
