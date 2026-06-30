package sung.worldcupanalysis.model;

/**
 * Presentation-ready view of a match: dates already parsed/localized and labels
 * translated to Korean, so the templates stay logic-free.
 */
public record MatchView(
        long id,
        String homeName,
        String homeCrest,
        String homeTla,
        String awayName,
        String awayCrest,
        String awayTla,
        String stageLabel,
        String groupLabel,
        String statusLabel,
        boolean live,
        boolean finished,
        String dateLabel,
        String timeLabel,
        String scoreLabel) {
}
