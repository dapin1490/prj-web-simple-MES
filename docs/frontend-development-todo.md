# 심플 MES 프론트엔드 개발 TODO

Vue 3 기반 `frontend` 폴더 스캐폴드 생성 이후의 구현 순서와 체크리스트를 정리한다. 요구·화면·API의 정본은 아래 문서를 따른다.

- 기능·범위: `docs/functional-requirement-statement.md`
- UI·컴포넌트: `docs/ui-ux-component-specification.md`
- 아키텍처·실시간 흐름: `docs/system-architecture-design.md`
- REST·WebSocket 계약: `docs/api-details.md`
- 스키마·JSON 필드명: `docs/data-schema-definition.md`

---

## 0. 현재 스캐폴드 상태 (기준점)

- 기술: Vue 3, Vite, Vue Router, Pinia, ESLint·Oxlint.
- 기본 라우트: `/`, `/about` (템플릿 예제 수준).
- 미포함(구현 예정): 백엔드 연동, STOMP 클라이언트, 차트·레이아웃·도메인 화면.

---

## 1. 프로젝트 기반 및 공통 인프라

| 순서  | 항목         | 설명                                                                                                     |
| --- | ---------- | ------------------------------------------------------------------------------------------------------ |
| 1.1 | 환경 변수      | `VITE_API_BASE_URL` 등으로 `docs/api-details.md`의 Base URL(`http://localhost:8080/api/v1`)과 배포 호스트를 분리한다. |
| 1.2 | HTTP 클라이언트 | `fetch` 래퍼 또는 Axios 등으로 공통 요청 함수를 둔다. 응답 규격(`success`, `data`, `message`)을 한곳에서 파싱한다.                  |
| 1.3 | 에러·로딩 UX   | 목록·상세·제어 API 호출 시 로딩 상태와 실패 메시지 표시 패턴을 통일한다.                                                           |
| 1.4 | JSON 필드 매핑 | wire 상 스네이크 케이스 유지 여부를 팀 규칙으로 정하고, 필요 시 뷰 모델만 camelCase로 매핑한다.<br>프론트엔드는 API 응답/요청의 snake_case 필드명을 그대로 사용한다. camelCase 매핑은 도입하지 않는다. |
| 1.5 | 라우트 구조     | Sidebar 메뉴와 일치하도록 경로를 정한다. 예: 대시보드, 수주·계획, 공정 모니터링, 품질·리포트. `/about` 등 템플릿 전용 라우트는 제거하거나 개발용으로 격리한다.   |
| 1.6 | 앱 셸 레이아웃   | `ui-ux-component-specification.md`에 따라 Sidebar, Header(사용자 권한·시뮬레이션 상태), Content 영역을 구성한다.             |
| 1.7 | 디자인 토큰     | Green·Yellow·Red 상태색, 여백·타이포를 `base.css` 또는 CSS 변수로 정리해 현장 시인성을 맞춘다.                                   |

---

## 2. 백엔드 REST 연동 (명세별 엔드포인트)

`docs/api-details.md` §2~§5 순으로 클라이언트 함수와 화면을 묶어 구현한다.

| 영역 | 엔드포인트(요약) | 프론트 작업 |
| --- | --- | --- |
| 제품·수주 | `GET /products`, `GET /orders`, `GET /orders/{product_id}` | 수주 현황 테이블, 제품 필터 데이터 소스 |
| 작업·생산 | `GET /work-orders`, `GET /production/logs/{wo_id}`, `GET /production/progress` | 로트 목록, 진척률·현황판 데이터, (필요 시) 히스토리 로그 조회 |
| 시뮬레이션 | `POST /simulation/start`, `stop`, `reset` | Header 또는 전용 제어 패널에서 버튼 연동, 상태 표시 |
| 품질·리포트 | `GET /inspections`, `GET /reports/{wo_id}` | 합격률·산점도·성적서 뷰어 데이터 |

추가로 필요한 쿼리 파라미터·페이지네이션은 백엔드와 합의 후 `api-details.md`에 반영하고 클라이언트를 맞춘다.

---

## 3. WebSocket(STOMP) 실시간 연동

| 순서 | 항목 | 설명 |
| --- | --- | --- |
| 3.1 | 의존성 | `@stomp/stompjs` 및 필요 시 SockJS 클라이언트를 도입한다. 브라우저 환경에 맞게 번들 설정을 확인한다. |
| 3.2 | 연결 수명 | 앱 진입 시 연결, 라우트 이탈·앱 종료 시 구독 해제 및 disconnect 처리 규칙을 정한다. |
| 3.3 | 구독 | Endpoint `/ws-mes`, Topic `/topic/production-trend`를 구독하고 페이로드(`wo_id`, `temp_pv`, `speed`, `timestamp`, `progress` 등)를 파싱한다. |
| 3.4 | 차트·로그에 공급 | 동일 스트림을 실시간 꺾은선 차트와 로그 롤링 패널에 전달하는 store 또는 composable을 둔다. |
| 3.5 | 재연결 | 네트워크 끊김 시 사용자 알림 및 선택적 자동 재구독 정책을 정한다. |

---

## 4. 화면별 구현 (UI·UX 명세 매핑)

### 4.1. 메인 대시보드 (Executive Summary)

- KPI 카드: 목표 대비 실적(Progress), 품질 합격률(Pass Rate). 데이터 출처는 `GET /production/progress`, `GET /inspections` 등 명세와 백엔드 구현에 맞춘다.
- 현장 현황판: 설비별 LOT, 진척도 게이지. 식별자·진행률은 WebSocket과 `GET /work-orders`·`/production/progress` 조합으로 채운다.
- 시뮬레이션 시작과 연동해 차트가 갱신되는지 E2E 수준으로 확인한다.

### 4.2. 수주 및 생산 계획 (Order & Planning)

- 수주 테이블: `SalesOrders` 기반 목록(제품명, 주문량, 납기 등 API 필드에 따름).
- 생산 트리거 위젯: `current_inven`과 `safety_stock` 비교 표시(스키마·API 필드명 확인).
- 작업 지시 발행: 수주 선택 후 모달에서 `WorkOrder` 생성 API가 명세에 추가되면 연동한다. 당시점에 생성 엔드포인트가 없으면 `api-details.md`에 먼저 기록하는 것을 전제로 스텁·목 처리한다.

### 4.3. 실시간 공정 모니터링 (Execution Monitoring)

- 실시간 트렌드: `temp_pv`(℃), `speed`(m/min), X축 시간. 라이브러리(예: Chart.js, ECharts) 선정 후 STOMP 데이터로 append 또는 윈도우 슬라이스 갱신.
- 로그 스트리밍 패널: 수신 레코드를 롤링 텍스트로 표시, 가독성을 위해 최대 줄 수·자동 스크롤 옵션을 둔다.

### 4.4. 품질 검사 및 리포트 (Quality & Reporting)

- 산점도: 로트별 `color_de` 시각화(`GET /inspections`).
- 성적서 뷰어: `GET /reports/{wo_id}` 응답 구조에 맞춘 표준 양식 레이아웃.
- PDF 내보내기: SRS상 목표 기능. 브라우저 인쇄·서버 생성 등 방식이 정해지면 구현하고, 미정이면 문서에 “방식 확정 후”로 남긴다.

---

## 5. 상태 관리·권한·시뮬레이션 UI

| 항목 | 설명 |
| --- | --- |
| Pinia store | 사용자 역할(Admin·Operator), 시뮬레이션 가동 여부, WebSocket 최신 스냅샷 등을 모듈별로 분리한다. |
| 권한 | SRS상 로그인·권한은 요구되나 REST 인증 형식은 `api-details.md`에 추가 전제. 추가 전까지는 라우트 가드 스텁 또는 개발용 토글만 둔다. |
| 시뮬레이션 | `Simulation Start`·Stop·Reset과 대시보드 실시간 갱신 연계, Header에 가동 상태 표시. |

---

## 6. 품질 이상·알림 UX

- `color_de` 임계(예: 1.0 이상) 시 설비 카드 강조·알림은 `ui-ux-component-specification.md` §3 표에 맞춰 구현한다.
- 브라우저 알림·토스트·배너 중 하나로 통일하고, 동일 이벤트 중복 알림을 줄이는 쿨다운을 검토한다.

---

## 7. 개발·배포·품질

| 항목 | 설명 |
| --- | --- |
| 프록시 | 로컬에서 CORS 회피를 위해 `vite.config.js`에 `/api`, `/ws-mes` 등 프록시 설정을 둘지 결정한다. |
| 빌드 | `npm run build` 산출물 경로와 정적 호스팅·리버스 프록시 규칙을 백엔드 팀과 맞춘다. |
| 접근성·반응형 | 현장용으로 글자 크기·터치 영역을 최소 기준 이상으로 유지한다. |
| 테스트 | 핵심 composable·API 래퍼에 단위 테스트 도입 여부를 팀에서 결정한다. |

---

## 8. 명세와의 동기화 (필수 습관)

- 스키마·필드·엔드포인트 변경 시 `data-schema-definition.md`, `api-details.md`를 먼저 갱신한 뒤 프론트를 수정한다.
- SRS에만 있고 위 문서에 없는 기능(OEE, PDF/HTML 세부, 인증 헤더 등)은 문서 반영 후 구현한다.

---

## 9. 권장 구현 순서(요약)

1. 환경 변수·HTTP 클라이언트·앱 셸 레이아웃·메뉴 라우팅  
2. 시뮬레이션 제어 API + 상태 표시  
3. 대시보드용 조회 API + KPI·진척률 카드  
4. STOMP 연결 + 실시간 차트·로그 패널  
5. 수주·작업 지시 화면  
6. 품질·리포트 화면 및 성적서 뷰어  
7. 권한·알림·내보내기 등 명세 확정 항목 마무리  

이 순서는 백엔드 준비 상황에 따라 일부 병행하거나 조정할 수 있다.

---

## 10. 체크리스트 (복사용)

- [ ] `VITE_*` 환경 변수 및 API Base URL 적용  
- [ ] 공통 HTTP 레이어 및 에러·로딩 처리  
- [ ] Sidebar·Header·Content 레이아웃 및 라우트 메뉴  
- [ ] `GET` 계열 REST 전 화면 연동  
- [ ] `POST /simulation/*` 제어 및 UI 상태  
- [ ] STOMP `/ws-mes`·`/topic/production-trend` 구독  
- [ ] 실시간 온도·속도 차트  
- [ ] 로그 롤링 패널  
- [ ] 대시보드 KPI·현황판  
- [ ] 수주 테이블·재고 트리거·작업 지시 모달(API 준비 시)  
- [ ] 품질 산점도·성적서 뷰어  
- [ ] 품질 이상 알림 UX  
- [ ] PDF·인증·OEE 등 미확정 항목은 명세 갱신 후 구현  

문서 버전: 프론트 스캐폴드 기준 초안. 폴더 구조·스택 변경 시 본 문서의 §0과 §1을 함께 수정한다.
