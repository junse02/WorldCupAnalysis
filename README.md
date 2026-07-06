# WorldCupAnalysis ⚽

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?logo=thymeleaf&logoColor=white)
[![Last commit](https://img.shields.io/github/last-commit/junse02/WorldCupAnalysis?logo=github)](https://github.com/junse02/WorldCupAnalysis/commits/main)
![Top language](https://img.shields.io/github/languages/top/junse02/WorldCupAnalysis)
![Repo size](https://img.shields.io/github/repo-size/junse02/WorldCupAnalysis)

2026 북중미 월드컵 경기 일정을 가져와 각 팀의 **에이스 선수**와 **전술 성향**을 AI로 분석해 주는
Spring Boot + Thymeleaf 웹사이트입니다.

- **데이터**: [API-Football](https://www.api-football.com/) (api-sports.io) v3 `/fixtures` (World Cup = `league=1`)
- **분석**: AI 생성 (구조화 JSON 출력). 우선순위 **Gemini(`gemini-2.5-flash`) → Claude(`claude-opus-4-8`) → 큐레이션 정적 데이터**
- **배당**: 경기별 **승·무·패 확률** 막대. 넉아웃은 실제 북메이커 1X2 배당(마진 제거) 환산, 조별리그는 팀 전력 기반 추정 — 라벨로 구분
- **화면**: Thymeleaf 서버 렌더링

API 키가 없어도 **번들 샘플 데이터**와 **자리표시자 분석**으로 사이트가 그대로 동작합니다.

## 화면 미리보기

| 경기 일정 (홈) | 조별리그 순위 |
|:---:|:---:|
| [![홈 화면 — 날짜별 경기 일정과 스코어](docs/screenshots/home.png)](docs/screenshots/home.png) | [![조별리그 순위 — 12개 조 + 3위 와일드카드](docs/screenshots/standings.png)](docs/screenshots/standings.png) |

**토너먼트 대진표** — 32강~결승 대진 틀. **32강 대진이 모두 확정**되어 실제 매치업(3위 배정 포함)과
완료 경기의 **스코어·승부차기·승자**(금색 강조)가 채워지며, 아직 안 치른 라운드는 피더 라벨로 남습니다.

[![토너먼트 대진표](docs/screenshots/bracket.png)](docs/screenshots/bracket.png)

**경기 분석** — 양 팀 에이스·강점/약점·포메이션·전술 요약과 맞대결 관전 포인트. 이미 끝난 경기는
관전 포인트 대신 최종 스코어 기반 **경기 총평**(📝)으로 자동 전환됩니다.

[![경기 분석 화면](docs/screenshots/match.png)](docs/screenshots/match.png)

**승·무·패 확률** — 일정 카드와 경기 상세에 홈승/무/원정승 확률을 3분할 막대로 표시합니다.
넉아웃 24경기는 **실제 북메이커 1X2 배당**(FanDuel·DraftKings 등)을 마진 제거 후 환산했고,
조별리그 72경기는 **팀 전력 기반 추정**입니다. 카드/상세의 라벨("북메이커 배당 기준" vs
"전력 기반 추정")로 출처를 구분하며, 전 96경기에 확률이 붙어 있습니다.

**득점·도움 순위** — 골든부트 레이스(득점 → 도움 순)와 플레이메이커 레이스(도움 → 골 순)를 나란히
보여줍니다. 국기와 함께 상위 3위를 강조합니다.

[![득점·도움 순위](docs/screenshots/players.png)](docs/screenshots/players.png)

> 모든 국가는 **국기 이미지**(일정·순위·대진표·경기 상세)와 함께 표시됩니다. API-Football 크레스트가 없을 때는
> [flagcdn](https://flagcdn.com) 국기로 폴백합니다. 위 화면은 API 키 없이 번들 샘플 데이터(2026 조별리그
> 72경기 실제 결과)로 렌더링한 모습입니다.

## 실행

```bash
# 모든 키는 선택 사항 — 없으면 샘플/정적 데이터로 동작
export APIFOOTBALL_API_KEY=...   # api-sports.io (API-Football) 키 — 일정 데이터
export GEMINI_API_KEY=...        # Google Gemini 키 — AI 분석 (우선)
export ANTHROPIC_API_KEY=...     # Anthropic 키 — AI 분석 (Gemini 미설정 시)

./gradlew bootRun
```

Windows PowerShell:

```powershell
$env:APIFOOTBALL_API_KEY = "..."
$env:GEMINI_API_KEY = "..."
.\gradlew.bat bootRun --no-daemon   # --no-daemon: 데몬이 환경변수를 캐시하는 문제 회피
```

> **시즌 주의**: API-Football *무료* 플랜은 2022~2024 시즌만 접근됩니다. 실제 2026 월드컵
> 데이터는 유료 플랜이 필요합니다. `application.properties`의 `apifootball.season` 기본값은
> **2022**(카타르)이며, 무료 키로 실제 데이터를 바로 확인할 수 있습니다. 플랜이 지원되면
> `apifootball.season=2026`으로 바꾸세요.

브라우저에서 <http://localhost:8080> 접속.

- `/` — 날짜별 경기 일정 (조별리그/토너먼트 단계, 스코어/킥오프, 경기 상태, 승·무·패 확률 막대)
- `/matches/{id}` — 양 팀 에이스·강점/약점·포메이션·전술 요약 + 맞대결 관전 포인트(끝난 경기는 경기 총평) + 승·무·패 확률
- `/standings` — 조별리그 순위 + 3위 와일드카드 순위
- `/bracket` — 32강~결승 토너먼트 대진표
- `/players` — 득점 순위(골든부트 레이스) + 도움 순위(플레이메이커 레이스)

## 구조

```
client/ApiFootballClient     API-Football 호출·정규화 (+ 샘플 데이터 폴백, 실시간/폴백 출처 추적)
service/MatchService        경기 조회·캐싱, 화면용 MatchView 변환(KST·한국어 라벨, 승·무·패 확률)
client/GeminiClient          Gemini 호출(팀 분석 JSON 스키마, 맞대결 프리뷰, 경기 총평) — REST 직접 호출
service/AnalysisService     AI 분석 오케스트레이션(팀 분석·프리뷰·경기 총평, Gemini→Claude→정적) + 캐싱
service/StaticAnalysisStore  큐레이션 정적 분석 로딩(폴백)
controller/WorldCupController  /, /matches/{id}, /bracket, /standings, /players 라우팅
service/StandingsService     조별 순위 계산(승점·득실·다득점) + 대진표 슬롯 치환
service/BracketService       32강~결승 토너먼트 대진 틀(공식 M73~M104 피더, 확정 슬롯 자동 치환)
service/PlayerStatsService   선수 스탯 로딩 + 득점 순위(득점→도움)·도움 순위(도움→득점) 정렬
model/                      football-data DTO(MatchDto·OddsDto 등), TeamAnalysis(분석 스키마), MatchView
config/CacheConfig          in-memory 캐시 (분석 결과 재사용 → API 비용 절감)
resources/templates/        index.html, match.html, fragments/
resources/sample/           wc-matches.json (API 미연동 시 폴백 데이터, 경기별 결과·odds 포함)
```

## 동작 메모

- **캐싱**: 팀 분석/맞대결 프리뷰/경기 총평은 캐시되어 페이지를 새로고침해도 AI를 다시 호출하지 않습니다.
  경기 일정 캐시는 10분마다 갱신됩니다(스코어 반영).
- **폴백**: API-Football 키가 없거나 호출이 실패/0건이면 `sample/wc-matches.json`을 사용합니다.
  Anthropic 키가 없으면(또는 호출 실패 시) 48개 본선 팀의 **큐레이션된 정적 분석**
  (`sample/team-analysis.json`, 에이스·전술 성향)을 사용합니다. 키가 있으면 Claude가 실시간 생성합니다.
- **출처 표시**: 홈 상단 배지가 실제 호출 성공 여부를 반영합니다 — "실시간 데이터 (API-Football)"
  또는 "샘플 데이터 (폴백)". 키가 있어도 플랜·시즌 제한으로 0건이면 폴백으로 표시됩니다.
- **배당(승·무·패 확률)**: `OddsDto`(정수 %, 합계 100 + `estimated` 플래그)로 모델링합니다. 넉아웃은
  실제 1X2 배당을 마진 제거 후 환산, 조별리그는 Elo 스타일 전력 레이팅 기반 추정이며 `estimated=true`로
  구분합니다. `odds`가 없는 경기는 막대가 숨겨집니다. 라이브 API 경로에는 배당이 없어 폴백 데이터에만 담겨 있습니다.
