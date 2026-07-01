package sung.worldcupanalysis.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sung.worldcupanalysis.client.ApiFootballClient;
import sung.worldcupanalysis.model.FootballData.MatchDto;
import sung.worldcupanalysis.model.MatchView;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private static final Locale KO = Locale.KOREAN;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ApiFootballClient client;
    private final ZoneId displayZone;

    public MatchService(ApiFootballClient client,
                        @Value("${app.display-zone}") String displayZone) {
        this.client = client;
        this.displayZone = ZoneId.of(displayZone);
    }

    @Cacheable("worldCupMatches")
    public List<MatchDto> getAllMatches() {
        return client.getWorldCupMatches();
    }

    /** Refresh cached fixtures periodically so scores stay reasonably current. */
    @CacheEvict(value = "worldCupMatches", allEntries = true)
    @Scheduled(fixedRateString = "PT10M")
    public void refreshMatches() {
        // cache eviction only
    }

    public Optional<MatchDto> findMatch(long id) {
        return getAllMatches().stream().filter(m -> m.id() == id).findFirst();
    }

    /** Matches grouped by localized date label, ordered chronologically. */
    public Map<String, List<MatchView>> matchesByDate() {
        return getAllMatches().stream()
                .sorted(Comparator.comparing(this::kickoff))
                .map(this::toView)
                .collect(Collectors.groupingBy(
                        MatchView::dateLabel,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    public MatchView toView(MatchDto m) {
        OffsetDateTime kickoff = kickoff(m);
        var local = kickoff.atZoneSameInstant(displayZone);

        boolean finished = "FINISHED".equalsIgnoreCase(m.status());
        boolean live = "IN_PLAY".equalsIgnoreCase(m.status()) || "PAUSED".equalsIgnoreCase(m.status());

        String dateLabel = "%d년 %d월 %d일 (%s)".formatted(
                local.getYear(), local.getMonthValue(), local.getDayOfMonth(),
                local.getDayOfWeek().getDisplayName(TextStyle.SHORT, KO));

        return new MatchView(
                m.id(),
                name(m.homeTeam()),
                crest(m.homeTeam()),
                tla(m.homeTeam()),
                name(m.awayTeam()),
                crest(m.awayTeam()),
                tla(m.awayTeam()),
                stageLabel(m.stage()),
                groupLabel(m.group()),
                statusLabel(m.status()),
                live,
                finished,
                dateLabel,
                local.format(TIME_FMT),
                scoreLabel(m));
    }

    private OffsetDateTime kickoff(MatchDto m) {
        return OffsetDateTime.parse(m.utcDate());
    }

    private static String name(sung.worldcupanalysis.model.FootballData.TeamRef t) {
        if (t == null || t.name() == null) {
            return "미정";
        }
        return t.name();
    }

    private static String tla(sung.worldcupanalysis.model.FootballData.TeamRef t) {
        return t != null && t.tla() != null ? t.tla() : "?";
    }

    private static String crest(sung.worldcupanalysis.model.FootballData.TeamRef t) {
        return t != null ? t.crest() : null;
    }

    private String scoreLabel(MatchDto m) {
        if (m.score() == null || m.score().fullTime() == null) {
            return null;
        }
        Integer home = m.score().fullTime().home();
        Integer away = m.score().fullTime().away();
        if (home == null || away == null) {
            return null;
        }
        String label = home + " : " + away;
        var pens = m.score().penalties();
        if (pens != null && pens.home() != null && pens.away() != null) {
            label += " (승부차기 " + pens.home() + ":" + pens.away() + ")";
        }
        return label;
    }

    private String stageLabel(String stage) {
        if (stage == null) {
            return "";
        }
        return switch (stage) {
            case "GROUP_STAGE" -> "조별리그";
            case "LAST_32" -> "32강";
            case "LAST_16" -> "16강";
            case "QUARTER_FINALS" -> "8강";
            case "SEMI_FINALS" -> "준결승";
            case "THIRD_PLACE" -> "3·4위전";
            case "FINAL" -> "결승";
            default -> stage;
        };
    }

    private String groupLabel(String group) {
        if (group == null || group.isBlank()) {
            return "";
        }
        // e.g. "GROUP_A" -> "A조"
        String letter = group.replace("GROUP_", "").trim();
        return letter.isEmpty() ? "" : letter + "조";
    }

    private String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "SCHEDULED", "TIMED" -> "예정";
            case "IN_PLAY" -> "경기 중";
            case "PAUSED" -> "하프타임";
            case "FINISHED" -> "종료";
            case "POSTPONED" -> "연기";
            case "SUSPENDED" -> "중단";
            case "CANCELLED" -> "취소";
            default -> status;
        };
    }
}
