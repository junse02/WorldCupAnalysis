package sung.worldcupanalysis.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import sung.worldcupanalysis.client.GeminiClient;
import sung.worldcupanalysis.model.TeamAnalysis;

import java.util.Optional;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private static final String TEAM_SYSTEM = """
            당신은 축구 국가대표팀을 분석하는 전문 스카우트입니다.
            주어진 국가대표팀에 대해 핵심 에이스 선수와 팀의 전술적 성향을 분석합니다.
            최신 대표팀 구성에 대한 확신이 없더라도, 그 나라 축구의 일반적인 스타일과
            널리 알려진 스타 선수를 바탕으로 가장 합리적인 분석을 제시하세요.
            모든 출력은 자연스러운 한국어로 작성합니다.""";

    private static final String PREVIEW_SYSTEM = """
            당신은 축구 경기를 미리 분석하는 해설위원입니다.
            두 국가대표팀의 맞대결에 대해 관전 포인트, 예상되는 전술 흐름,
            승부를 가를 변수를 3~4문장의 한국어로 흥미롭게 풀어내세요.
            마크다운 기호 없이 평문으로만 작성합니다.""";

    private static final String REVIEW_SYSTEM = """
            당신은 이미 끝난 축구 경기를 리뷰하는 해설위원입니다.
            주어진 최종 스코어를 바탕으로 승부를 가른 요인, 잘한 팀과 아쉬운 팀,
            경기의 결정적 흐름을 3~4문장의 한국어로 흥미롭게 총평하세요.
            스코어와 모순되는 내용을 쓰지 말고, 마크다운 기호 없이 평문으로만 작성합니다.""";

    private final AnthropicClient client; // null when no Anthropic key configured
    private final String model;
    private final StaticAnalysisStore staticStore;
    private final GeminiClient gemini;

    public AnalysisService(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.model}") String model,
            StaticAnalysisStore staticStore,
            GeminiClient gemini) {
        this.model = model;
        this.staticStore = staticStore;
        this.gemini = gemini;
        if (apiKey != null && !apiKey.isBlank()) {
            this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        } else {
            this.client = null;
        }
        log.info("AI analysis source: {}", gemini.isEnabled() ? "Gemini"
                : (client != null ? "Claude" : "curated static data"));
    }

    public boolean isEnabled() {
        return gemini.isEnabled() || client != null;
    }

    @Cacheable("teamAnalysis")
    public TeamAnalysis analyzeTeam(String teamName) {
        if (gemini.isEnabled()) {
            Optional<TeamAnalysis> g = gemini.analyzeTeam(teamName);
            if (g.isPresent()) {
                return g.get();
            }
        }
        if (client == null) {
            return staticAnalysis(teamName);
        }
        try {
            String prompt = "다음 국가대표팀을 분석하세요: " + teamName;
            StructuredMessageCreateParams<TeamAnalysis> params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(16_000L)
                    .system(TEAM_SYSTEM)
                    .outputConfig(TeamAnalysis.class)
                    .addUserMessage(prompt)
                    .build();

            return client.messages().create(params).content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(typed -> typed.text())
                    .findFirst()
                    .orElseGet(() -> staticAnalysis(teamName));
        } catch (Exception e) {
            log.warn("Team analysis failed for '{}': {}", teamName, e.getMessage());
            return staticAnalysis(teamName);
        }
    }

    /** Curated bundled analysis, or a placeholder if the team isn't covered. */
    private TeamAnalysis staticAnalysis(String teamName) {
        return staticStore.find(teamName).orElseGet(() -> TeamAnalysis.unavailable(teamName));
    }

    @Cacheable("matchupPreview")
    public String matchupPreview(String homeTeam, String awayTeam) {
        if (gemini.isEnabled()) {
            Optional<String> p = gemini.matchupPreview(homeTeam, awayTeam);
            if (p.isPresent()) {
                return p.get();
            }
        }
        if (client == null) {
            return staticPreview(homeTeam, awayTeam);
        }
        try {
            String prompt = "%s 대 %s 경기의 관전 포인트를 분석해 주세요.".formatted(homeTeam, awayTeam);
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(2_000L)
                    .system(PREVIEW_SYSTEM)
                    .addUserMessage(prompt)
                    .build();

            Message message = client.messages().create(params);
            String text = message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(t -> t.text())
                    .reduce("", (a, b) -> a + b)
                    .trim();
            return text.isEmpty()
                    ? "맞대결 분석을 생성하지 못했습니다."
                    : text;
        } catch (Exception e) {
            log.warn("Matchup preview failed for '{}' vs '{}': {}", homeTeam, awayTeam, e.getMessage());
            return staticPreview(homeTeam, awayTeam);
        }
    }

    @Cacheable("matchReview")
    public String matchReview(String homeTeam, String awayTeam, String scoreLabel) {
        if (gemini.isEnabled()) {
            Optional<String> r = gemini.matchReview(homeTeam, awayTeam, scoreLabel);
            if (r.isPresent()) {
                return r.get();
            }
        }
        if (client == null) {
            return staticReview(homeTeam, awayTeam, scoreLabel);
        }
        try {
            String prompt = "%s 대 %s 경기가 %s로 끝났습니다. 이 결과를 바탕으로 경기 총평을 작성해 주세요."
                    .formatted(homeTeam, awayTeam, scoreLabel);
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(2_000L)
                    .system(REVIEW_SYSTEM)
                    .addUserMessage(prompt)
                    .build();

            Message message = client.messages().create(params);
            String text = message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(t -> t.text())
                    .reduce("", (a, b) -> a + b)
                    .trim();
            return text.isEmpty()
                    ? staticReview(homeTeam, awayTeam, scoreLabel)
                    : text;
        } catch (Exception e) {
            log.warn("Match review failed for '{}' vs '{}': {}", homeTeam, awayTeam, e.getMessage());
            return staticReview(homeTeam, awayTeam, scoreLabel);
        }
    }

    /** Composes a short result recap from the final score and both teams' curated styles. */
    private String staticReview(String homeTeam, String awayTeam, String scoreLabel) {
        String base = "%s 대 %s 경기는 %s로 마무리됐다.".formatted(homeTeam, awayTeam, scoreLabel);
        var home = staticStore.find(homeTeam);
        var away = staticStore.find(awayTeam);
        if (home.isEmpty() || away.isEmpty()) {
            return base + " (GEMINI_API_KEY 또는 ANTHROPIC_API_KEY를 설정하면 AI가 경기 총평을 생성합니다.)";
        }
        TeamAnalysis h = home.get();
        TeamAnalysis a = away.get();
        return base + " %s의 에이스 %s와(과) %s의 에이스 %s의 활약이 승부의 중심에 있었다. 두 팀의 '%s' 대 '%s' 스타일 대결이 결과에 그대로 드러난 경기였다."
                .formatted(homeTeam, h.acePlayer(), awayTeam, a.acePlayer(), h.playStyle(), a.playStyle());
    }

    /** Composes a short preview from the curated analyses of both teams. */
    private String staticPreview(String homeTeam, String awayTeam) {
        var home = staticStore.find(homeTeam);
        var away = staticStore.find(awayTeam);
        if (home.isEmpty() || away.isEmpty()) {
            return "두 팀의 에이스와 전술 성향은 아래 카드에서 확인할 수 있습니다. "
                    + "(GEMINI_API_KEY 또는 ANTHROPIC_API_KEY를 설정하면 AI가 맞대결 관전 포인트를 생성합니다.)";
        }
        TeamAnalysis h = home.get();
        TeamAnalysis a = away.get();
        return "%s의 에이스 %s와(과) %s의 에이스 %s의 맞대결이 관전 포인트다. %s는 '%s' 성향으로, %s는 '%s' 성향으로 맞선다. %s"
                .formatted(
                        homeTeam, h.acePlayer(), awayTeam, a.acePlayer(),
                        homeTeam, h.playStyle(), awayTeam, a.playStyle(),
                        "두 팀의 스타일이 정면으로 부딪히는 경기로, 중원 주도권과 에이스의 컨디션이 승부를 가를 전망이다.");
    }
}
