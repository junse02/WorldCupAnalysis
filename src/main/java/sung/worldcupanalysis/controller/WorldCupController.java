package sung.worldcupanalysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sung.worldcupanalysis.client.ApiFootballClient;
import sung.worldcupanalysis.model.FootballData.MatchDto;
import sung.worldcupanalysis.model.MatchView;
import sung.worldcupanalysis.service.AnalysisService;
import sung.worldcupanalysis.service.BracketService;
import sung.worldcupanalysis.service.MatchService;
import sung.worldcupanalysis.service.StandingsService;

import java.util.Optional;

@Controller
public class WorldCupController {

    private final MatchService matchService;
    private final AnalysisService analysisService;
    private final ApiFootballClient apiFootballClient;
    private final BracketService bracketService;
    private final StandingsService standingsService;

    public WorldCupController(MatchService matchService,
                              AnalysisService analysisService,
                              ApiFootballClient apiFootballClient,
                              BracketService bracketService,
                              StandingsService standingsService) {
        this.matchService = matchService;
        this.analysisService = analysisService;
        this.apiFootballClient = apiFootballClient;
        this.bracketService = bracketService;
        this.standingsService = standingsService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("matchesByDate", matchService.matchesByDate());
        // matchesByDate() above triggers the fetch, so the source is now accurate.
        model.addAttribute("liveData",
                apiFootballClient.getLastSource() == ApiFootballClient.DataSource.LIVE);
        model.addAttribute("aiEnabled", analysisService.isEnabled());
        return "index";
    }

    @GetMapping("/matches/{id}")
    public String matchDetail(@PathVariable long id, Model model, RedirectAttributes redirect) {
        Optional<MatchDto> match = matchService.findMatch(id);
        if (match.isEmpty()) {
            return "redirect:/";
        }

        MatchView view = matchService.toView(match.get());
        String home = view.homeName();
        String away = view.awayName();

        model.addAttribute("match", view);
        model.addAttribute("homeAnalysis", analysisService.analyzeTeam(home));
        model.addAttribute("awayAnalysis", analysisService.analyzeTeam(away));
        model.addAttribute("preview", analysisService.matchupPreview(home, away));
        model.addAttribute("aiEnabled", analysisService.isEnabled());
        return "match";
    }

    @GetMapping("/bracket")
    public String bracket(Model model) {
        model.addAttribute("rounds", bracketService.rounds());
        return "bracket";
    }

    @GetMapping("/standings")
    public String standings(Model model) {
        var tables = standingsService.tables();
        boolean allComplete = !tables.isEmpty()
                && tables.values().stream().allMatch(t -> t.complete());
        model.addAttribute("tables", tables);
        model.addAttribute("thirds", StandingsService.thirdPlaceRanking(tables));
        model.addAttribute("thirdsFinal", allComplete);
        return "standings";
    }

    @GetMapping("/teams/{name}")
    public String team(@PathVariable String name, Model model) {
        model.addAttribute("teamName", name);
        model.addAttribute("analysis", analysisService.analyzeTeam(name));
        model.addAttribute("aiEnabled", analysisService.isEnabled());
        return "team";
    }
}
