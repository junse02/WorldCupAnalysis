package sung.worldcupanalysis.service;

import org.springframework.stereotype.Service;
import sung.worldcupanalysis.model.Bracket.Round;
import sung.worldcupanalysis.model.Bracket.Tie;
import sung.worldcupanalysis.service.StandingsService.GroupTable;

import java.util.List;
import java.util.Map;

/**
 * The 2026 World Cup knockout bracket framework (48-team format), using the
 * official FIFA match numbers (M73–M104) and feeder mapping. Round-of-32 ties
 * (M73–M88) use the official positional pattern; every later round (M89–M104)
 * references the specific feeder matches it draws from. Positions/winners
 * resolve once the group stage finishes.
 */
@Service
public class BracketService {

    private final StandingsService standings;

    public BracketService(StandingsService standings) {
        this.standings = standings;
    }

    /**
     * The bracket with slots resolved to real team names where the group stage
     * already decides them (completed groups' 1st/2nd). Everything else stays as
     * its positional/feeder label.
     */
    public List<Round> rounds() {
        Map<String, GroupTable> tables = standings.tables();
        return rawRounds().stream()
                .map(round -> new Round(round.name(), round.dateLabel(),
                        round.ties().stream()
                                .map(tie -> new Tie(tie.no(),
                                        StandingsService.resolveSlot(tie.top(), tables),
                                        StandingsService.resolveSlot(tie.bottom(), tables)))
                                .toList()))
                .toList();
    }

    private List<Round> rawRounds() {
        return List.of(
                new Round("32강", "6.28 ~ 7.3", List.of(
                        new Tie("M73", "A조 2위", "B조 2위"),
                        new Tie("M74", "E조 1위", "A·B·C·D·F조 3위"),
                        new Tie("M75", "F조 1위", "C조 2위"),
                        new Tie("M76", "C조 1위", "F조 2위"),
                        new Tie("M77", "I조 1위", "C·D·F·G·H조 3위"),
                        new Tie("M78", "E조 2위", "I조 2위"),
                        new Tie("M79", "A조 1위", "C·E·F·H·I조 3위"),
                        new Tie("M80", "L조 1위", "E·H·I·J·K조 3위"),
                        new Tie("M81", "D조 1위", "B·E·F·I·J조 3위"),
                        new Tie("M82", "G조 1위", "A·E·H·I·J조 3위"),
                        new Tie("M83", "K조 2위", "L조 2위"),
                        new Tie("M84", "H조 1위", "J조 2위"),
                        new Tie("M85", "B조 1위", "E·F·G·I·J조 3위"),
                        new Tie("M86", "J조 1위", "H조 2위"),
                        new Tie("M87", "K조 1위", "D·E·I·J·L조 3위"),
                        new Tie("M88", "D조 2위", "G조 2위"))),
                new Round("16강", "7.4 ~ 7.7", List.of(
                        new Tie("M89", "M74 승자", "M77 승자"),
                        new Tie("M90", "M73 승자", "M75 승자"),
                        new Tie("M91", "M76 승자", "M78 승자"),
                        new Tie("M92", "M79 승자", "M80 승자"),
                        new Tie("M93", "M83 승자", "M84 승자"),
                        new Tie("M94", "M81 승자", "M82 승자"),
                        new Tie("M95", "M86 승자", "M88 승자"),
                        new Tie("M96", "M85 승자", "M87 승자"))),
                new Round("8강", "7.9 ~ 7.11", List.of(
                        new Tie("M97", "M89 승자", "M90 승자"),
                        new Tie("M98", "M93 승자", "M94 승자"),
                        new Tie("M99", "M91 승자", "M92 승자"),
                        new Tie("M100", "M95 승자", "M96 승자"))),
                new Round("준결승", "7.14 ~ 7.15", List.of(
                        new Tie("M101", "M97 승자", "M98 승자"),
                        new Tie("M102", "M99 승자", "M100 승자"))),
                new Round("3·4위전", "7.18", List.of(
                        new Tie("M103", "M101 패자", "M102 패자"))),
                new Round("결승", "7.19", List.of(
                        new Tie("M104", "M101 승자", "M102 승자"))));
    }
}
