# 모노레포 협업 프로토콜 (심플 MES)

팀이 동일한 규범으로 백엔드와 프론트엔드를 나누어 개발할 때 따르는 협업 규칙이다. (현재 저장소에는 Spring Boot 프로젝트가 `be-mes-project/`에 있으며, 프론트엔드 루트 디렉터리는 `frontend/`에 있음.)

## 1. Single Source of Truth

설계와 데이터 정의는 저장소 루트의 `docs/`에 있는 명세서가 최종 기준이다.

| 문서 | 용도 |
| --- | --- |
| `docs/functional-requirement-statement.md` | 기능 범위와 요구 |
| `docs/data-schema-definition.md` | DB 및 필드명, 타입 |
| `docs/data-generation-report.md` | 전처리 데이터와 검증 전제 |
| `docs/system-architecture-design.md` | 계층과 데이터 흐름 |
| `docs/api-details.md` | STOMP/WebSocket 경로, 응답 형식, 토픽/큐 규약 |
| `docs/ui-ux-component-specification.md` | 화면과 컴포넌트 |

명세와 충돌하는 코드는 명세를 먼저 갱신한 뒤 구현한다.

## 2. 모노레포 구조와 책임

### 2.1 디렉터리

- `be-mes-project/`: Spring Boot(Gradle), DB 연동, 스케줄러, STOMP WebSocket 서버(구현 시). 기술 버전은 저장소의 `build.gradle`을 따른다.
- `frontend/`: Vue 3, Composition API, `<script setup>`, 대시보드 및 관리 UI.

### 2.2 권한과 소유 범위 (협업 기준)

- **백엔드**: `be-mes-project/` 전역, 서버 설정, DB 마이그레이션·엔티티, STOMP 컨트롤러, 스케줄러, WebSocket 설정. 스키마 변경 시 `docs/data-schema-definition.md`와의 일치를 PR 설명에 명시한다.
- **프론트엔드**: `frontend/` 전역, 라우팅, 컴포넌트, STOMP 메시지 계층, STOMP 클라이언트. API 계약은 `docs/api-details.md`를 따른다.
- **공통 문서**: `docs/` 변경은 기획 및 설계 담당자가 제안하고, 스키마나 API가 바뀌면 백엔드와 프론트엔드 모두 리뷰어에 포함한다.

필요 시 저장소에 `CODEOWNERS`를 두어 디렉터리별 승인 규칙을 자동화할 수 있다.

## 3. 데이터와 시뮬레이션 원칙

- **스키마 준수**: DB 컬럼과 STOMP JSON 키는 `docs/data-schema-definition.md` 및 `docs/api-details.md`의 스네이크 케이스 규칙을 따른다. Java 등 코드 내부의 camelCase는 프로젝트 관례에 맞춘다.
- **전처리 재구현 금지**: 백엔드에서 CSV를 임의로 변환·보정하는 로직을 두지 않는다. `docs/data-generation-report.md`에 기술된 전처리된 산출물을 로드해 시뮬레이션한다.
- **우선 과제**: 전처리 산출 `data/backend_ready/ProductionLogs.csv`(등)를 읽어 1분 주기로 MySQL에 적재하는 스케줄러를 먼저 안정화하고, 시뮬레이션 시작·정지·초기화 API와 연동한다. 필드 정의는 `docs/data-schema-definition.md` §2.4 및 `docs/data-generation-report.md` §2.3과 일치시킨다.

## 4. 메시지 채널 규약

- **메인 통신**: 조회·제어·스트리밍 모두 STOMP WebSocket을 사용한다.
- **연결/경로**: 엔드포인트 `/ws-mes`, 요청 prefix `/app`, 구독 `/topic`, `/queue`, `/user/queue`.
- **실시간**: 공정 트렌드는 `/topic/production-trend`, 설비 이상 알림은 `docs/api-details.md` §6.1 별도 토픽(예: `/topic/equipment-alert`)을 사용한다.

## 5. 프론트엔드 메시지 계층

- STOMP 메시지 계층에서 응답 규격(`success`, `data`, `message`)을 일관되게 파싱하고, 오류 시 `message`를 사용자 피드백 또는 로깅에 연결한다.
- Send/Subscribe destination은 `docs/api-details.md`와 동기화한다.

## 6. 코딩 컨벤션

- **Java**: Google Java Style
- **JavaScript/Vue**: Airbnb 스타일 기준, 식별자는 camelCase
- 프로젝트에 포매터(예: Spotless, ESLint, Prettier)가 있으면 CI와 동일한 설정을 로컬에서 맞춘다.

## 7. Git 전략

- **브랜치**: 기본적으로 `main`(또는 `develop`)에서 기능 브랜치를 분리하고, 짧은 수명의 브랜치를 권장한다.
- **커밋**: [Conventional Commits](https://www.conventionalcommits.org/)를 사용한다.
  - 예: `feat(be): 생산 로그 시뮬레이션 스케줄러 추가`
  - 예: `fix(fe): Axios 응답 인터셉터 오류 처리`
  - 접두어로 영역을 구분한다: `be` 백엔드, `fe` 프론트엔드, `docs` 문서, 그 외 `chore` 등

## 8. Pull Request 관습

- 제목에 Conventional Commits 형식을 적용해도 좋다.
- 스키마, API, WebSocket 계약이 바뀌면 관련 `docs/` 링크와 마이그레이션·프론트 영향 범위를 본문에 적는다.
- 리뷰는 최소 한 명 이상의 해당 영역 담당자 승인을 받는다.

## 9. AI 보조 도구 (Cursor)

- 프로젝트 루트 `.cursorrules`에 AI가 따라야 할 기술 스택, 문서 우선순위, 시뮬레이터·WebSocket 규칙이 정리되어 있다. 새 작업 시 이 파일과 본 문서를 함께 참고한다.
