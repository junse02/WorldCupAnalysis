package sung.worldcupanalysis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sung.worldcupanalysis.model.TeamAnalysis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates team analyses and matchup previews with Google's Gemini API
 * (generativelanguage v1beta /generateContent). Uses structured JSON output
 * (responseSchema) for analyses. Calls the REST API directly via RestClient so
 * no extra SDK dependency is needed. Returns {@code Optional.empty()} on any
 * failure so callers can fall back.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String TEAM_SYSTEM = """
            당신은 축구 국가대표팀을 분석하는 전문 스카우트입니다.
            주어진 국가대표팀에 대해 핵심 에이스 선수와 팀의 전술적 성향을 분석합니다.
            최신 대표팀 구성에 확신이 없더라도 그 나라 축구의 일반적인 스타일과
            널리 알려진 스타 선수를 바탕으로 가장 합리적인 분석을 제시하세요.
            모든 출력은 자연스러운 한국어로 작성합니다.""";

    private static final String PREVIEW_SYSTEM = """
            당신은 축구 경기를 미리 분석하는 해설위원입니다.
            두 국가대표팀의 맞대결에 대해 관전 포인트, 예상되는 전술 흐름,
            승부를 가를 변수를 3~4문장의 한국어로 흥미롭게 풀어내세요.
            마크다운 기호 없이 평문으로만 작성합니다.""";

    private final boolean enabled;
    private final RestClient rest;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiClient(
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model}") String model) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.model = model;

        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (enabled) {
            builder.defaultHeader("x-goog-api-key", apiKey);
        }
        this.rest = builder.build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<TeamAnalysis> analyzeTeam(String teamName) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("system_instruction", parts(TEAM_SYSTEM));
            body.put("contents", List.of(parts("다음 국가대표팀을 분석하세요: " + teamName)));
            body.put("generationConfig", Map.of(
                    "responseMimeType", "application/json",
                    "responseSchema", teamSchema()));

            String text = generate(body);
            if (text == null || text.isBlank()) {
                return Optional.empty();
            }
            TeamAnalysis parsed = mapper.readValue(text, TeamAnalysis.class);
            log.info("Gemini generated analysis for '{}'.", teamName);
            // Keep the caller's team name for consistency with the schedule; flag as real.
            return Optional.of(new TeamAnalysis(
                    teamName, parsed.acePlayer(), parsed.acePlayerDescription(), parsed.playStyle(),
                    parsed.formation(), parsed.strengths(), parsed.weaknesses(), parsed.tacticalSummary(), true));
        } catch (Exception e) {
            log.warn("Gemini team analysis failed for '{}': {}", teamName, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> matchupPreview(String homeTeam, String awayTeam) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("system_instruction", parts(PREVIEW_SYSTEM));
            body.put("contents", List.of(parts(
                    "%s 대 %s 경기의 관전 포인트를 분석해 주세요.".formatted(homeTeam, awayTeam))));

            String text = generate(body);
            return (text == null || text.isBlank()) ? Optional.empty() : Optional.of(text.trim());
        } catch (Exception e) {
            log.warn("Gemini matchup preview failed for '{}' vs '{}': {}", homeTeam, awayTeam, e.getMessage());
            return Optional.empty();
        }
    }

    private String generate(Map<String, Object> body) {
        GenResponse response = rest.post()
                .uri("/models/{model}:generateContent", model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GenResponse.class);
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return null;
        }
        return candidate.content().parts().get(0).text();
    }

    private static Map<String, Object> parts(String text) {
        return Map.of("parts", List.of(Map.of("text", text)));
    }

    private static Map<String, Object> teamSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("teamName", Map.of("type", "STRING"));
        props.put("acePlayer", Map.of("type", "STRING"));
        props.put("acePlayerDescription", Map.of("type", "STRING"));
        props.put("playStyle", Map.of("type", "STRING"));
        props.put("formation", Map.of("type", "STRING"));
        props.put("strengths", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));
        props.put("weaknesses", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));
        props.put("tacticalSummary", Map.of("type", "STRING"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", props);
        schema.put("required", List.of("teamName", "acePlayer", "acePlayerDescription", "playStyle",
                "formation", "strengths", "weaknesses", "tacticalSummary"));
        return schema;
    }

    // --- Minimal response mapping (records; unknown fields ignored by Jackson) ---
    record GenResponse(List<Candidate> candidates) {
    }

    record Candidate(Content content, String finishReason) {
    }

    record Content(List<Part> parts) {
    }

    record Part(String text) {
    }
}
