package sung.worldcupanalysis.model;

import java.util.List;

/**
 * Knockout bracket framework. Positions are placeholders (group winners /
 * runners-up / third-placed) that resolve once the group stage finishes.
 */
public final class Bracket {

    private Bracket() {
    }

    public record Round(String name, String dateLabel, List<Tie> ties) {
    }

    public record Tie(String no, String top, String bottom) {
    }
}
