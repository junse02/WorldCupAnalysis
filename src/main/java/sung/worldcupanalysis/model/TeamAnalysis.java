package sung.worldcupanalysis.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * AI-generated scouting report for a national team. This shape is handed to the
 * Anthropic SDK as a structured-output schema, so the field descriptions double
 * as instructions to the model.
 */
@JsonClassDescription("축구 국가대표팀에 대한 스카우팅 리포트")
public record TeamAnalysis(
        @JsonPropertyDescription("팀(국가) 이름") String teamName,
        @JsonPropertyDescription("팀의 핵심 에이스 선수 이름") String acePlayer,
        @JsonPropertyDescription("에이스 선수의 포지션과 특징을 2~3문장으로 설명") String acePlayerDescription,
        @JsonPropertyDescription("팀의 전반적인 플레이 스타일과 전술 성향을 한 문장으로") String playStyle,
        @JsonPropertyDescription("예상 주력 포메이션, 예: 4-3-3") String formation,
        @JsonPropertyDescription("팀의 강점 2~4가지") List<String> strengths,
        @JsonPropertyDescription("팀의 약점 2~4가지") List<String> weaknesses,
        @JsonPropertyDescription("종합 전술 요약 한 문단") String tacticalSummary,
        @JsonPropertyDescription("AI가 실제로 생성한 분석이면 true, 자리표시자면 false") boolean generated) {

    /** Returns a copy flagged as real analysis content (used for curated static data). */
    public TeamAnalysis asGenerated() {
        return new TeamAnalysis(teamName, acePlayer, acePlayerDescription, playStyle,
                formation, strengths, weaknesses, tacticalSummary, true);
    }

    /** Placeholder used when no API key is configured or a call fails. */
    public static TeamAnalysis unavailable(String teamName) {
        return new TeamAnalysis(
                teamName,
                "-",
                "AI 분석을 생성하려면 ANTHROPIC_API_KEY 환경변수를 설정하세요.",
                "분석 준비 중",
                "-",
                List.of("AI 분석 비활성화됨"),
                List.of("AI 분석 비활성화됨"),
                "Claude API 키가 설정되면 이 팀의 에이스 선수와 전술 성향에 대한 상세 분석이 표시됩니다.",
                false);
    }
}
