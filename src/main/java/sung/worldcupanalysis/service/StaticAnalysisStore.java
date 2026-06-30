package sung.worldcupanalysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import sung.worldcupanalysis.model.TeamAnalysis;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads bundled, curated team analyses from {@code sample/team-analysis.json}.
 * Used as the analysis source when the Claude API is not configured (or a live
 * call fails), so the scouting reports are populated without an API key.
 */
@Component
public class StaticAnalysisStore {

    private static final Logger log = LoggerFactory.getLogger(StaticAnalysisStore.class);
    private static final String RESOURCE = "sample/team-analysis.json";

    private final Map<String, TeamAnalysis> byTeam = new HashMap<>();

    public StaticAnalysisStore() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            AnalysesFile file = new ObjectMapper().readValue(in, AnalysesFile.class);
            for (TeamAnalysis a : file.analyses()) {
                byTeam.put(a.teamName(), a.asGenerated());
            }
            log.info("Loaded {} curated team analyses.", byTeam.size());
        } catch (Exception e) {
            log.error("Failed to load curated team analyses from {}", RESOURCE, e);
        }
    }

    public Optional<TeamAnalysis> find(String teamName) {
        return Optional.ofNullable(byTeam.get(teamName));
    }

    private record AnalysesFile(List<TeamAnalysis> analyses) {
    }
}
