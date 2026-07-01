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

    /**
     * A single tie. {@code topScore}/{@code bottomScore} are display strings
     * (e.g. "2" or "1 (4)" including a penalty-shootout count) and are empty when
     * the tie hasn't been played. {@code winner} is "TOP", "BOTTOM" or "".
     */
    public record Tie(String no, String top, String bottom,
                      String topScore, String bottomScore, String winner, boolean finished) {

        /** Framework tie with no result yet (positional/feeder labels). */
        public Tie(String no, String top, String bottom) {
            this(no, top, bottom, "", "", "", false);
        }
    }
}
