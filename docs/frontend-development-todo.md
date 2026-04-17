# 심플 MES 프론트엔드 개발 TODO

Vue 3 기반 `frontend` 폴더 스캐폴드 생성 이후의 구현 순서와 체크리스트를 정리한다. 요구·화면·API의 정본은 아래 문서를 따른다.

- 기능·범위: `docs/functional-requirement-statement.md`
- UI·컴포넌트: `docs/ui-ux-component-specification.md`
- 아키텍처·실시간 흐름: `docs/system-architecture-design.md`
- STOMP/WebSocket 계약: `docs/api-details.md`
- 스키마·JSON 필드명: `docs/data-schema-definition.md`

---

## 0. 현재 스캐폴드 상태 (기준점)

- 기술: Vue 3, Vite, Vue Router, Pinia, ESLint·Oxlint.
- 기본 라우트: 대시보드, 수주·계획, 공정 모니터링, 품질·리포트 화면 기준으로 정리됨.
- 구현 완료(1~4절): 공통 STOMP 메시지 계층, 백엔드 STOMP 연동, 실시간 구독 처리, 메인 대시보드/수주·계획/공정 모니터링/품질·리포트 화면의 기본 동작.
- 후속 구현 범위(5절 이후): 권한 세부, 알림 UX 고도화, 테스트 확대, 미확정 기능(PDF·OEE·인증 명세 확정 항목) 정리.

---

## 1. 프로젝트 기반 및 공통 인프라

| 순서  | 항목         | 설명                                                                                                     |
| --- | ---------- | ------------------------------------------------------------------------------------------------------ |
| 1.1 | 환경 변수      | `VITE_WS_BASE_URL` 등으로 `docs/api-details.md`의 WebSocket 엔드포인트(`/ws-mes`)와 배포 호스트를 분리한다. |
| 1.2 | STOMP 클라이언트 래퍼 | `@stomp/stompjs` 기반 공통 메시지 함수를 두고 응답 규격(`success`, `data`, `message`)을 한곳에서 파싱한다. |
| 1.3 | 에러·로딩 UX   | 메시지 요청/응답 대기 상태와 실패 메시지 표시 패턴을 통일한다. |
| 1.4 | JSON 필드 매핑 | wire 상 스네이크 케이스 유지 여부를 팀 규칙으로 정하고, 필요 시 뷰 모델만 camelCase로 매핑한다.<br>프론트엔드는 API 응답/요청의 snake_case 필드명을 그대로 사용한다. camelCase 매핑은 도입하지 않는다. |
| 1.5 | 라우트 구조     | Sidebar 메뉴와 일치하도록 경로를 정한다. 예: 대시보드, 수주·계획, 공정 모니터링, 품질·리포트. `/about` 등 템플릿 전용 라우트는 제거하거나 개발용으로 격리한다.   |
| 1.6 | 앱 셸 레이아웃   | `ui-ux-component-specification.md`에 따라 Sidebar, Header(사용자 권한·시뮬레이션 상태), Content 영역을 구성한다.             |
| 1.7 | 디자인 토큰     | Green·Yellow·Red 상태색, 여백·타이포를 `base.css` 또는 CSS 변수로 정리해 현장 시인성을 맞춘다.                                   |

---

## 2. 백엔드 STOMP 연동 (명세별 destination)

`docs/api-details.md` §2~§5 순으로 클라이언트 함수와 화면을 묶어 구현한다.

| 영역 | Send/Subscribe(요약) | 프론트 작업 |
| --- | --- | --- |
| 제품·수주 | `/app/planning/*` ↔ `/user/queue/planning/*` | 수주 현황 테이블, 제품 필터 데이터 소스 |
| 작업·생산 | `/app/execution/*` ↔ `/user/queue/execution/*` | 로트 목록, 진척률·현황판 데이터, 히스토리 로그 조회 |
| 시뮬레이션 | `/app/simulation/*` ↔ `/user/queue/simulation/state` | Header 또는 전용 제어 패널에서 버튼 연동, 상태 표시 |
| 품질·리포트 | `/app/quality/*` ↔ `/user/queue/quality/*` | 합격률·산점도·성적서 뷰어 데이터 |

추가로 필요한 필터·페이지네이션은 요청 body 규격을 백엔드와 합의 후 `api-details.md`에 반영하고 클라이언트를 맞춘다.

---

## 3. WebSocket(STOMP) 실시간 연동

| 순서 | 항목 | 설명 |
| --- | --- | --- |
| 3.1 | 의존성 | `@stomp/stompjs` 및 필요 시 SockJS 클라이언트를 도입한다. 브라우저 환경에 맞게 번들 설정을 확인한다. |
| 3.2 | 연결 수명 | 앱 진입 시 연결, 라우트 이탈·앱 종료 시 구독 해제 및 disconnect 처리 규칙을 정한다. |
| 3.3 | 구독 | Endpoint `/ws-mes`, Topic `/topic/production-trend`를 구독하고 페이로드(`wo_id`, `cr_temp`, `temp_sp`, `temp_pv`, `speed`, `timestamp`, `progress` 등, `docs/api-details.md` §6)를 파싱한다. `progress`는 `SEQ_NO` 기반 대체 지표(0~100)로 처리한다. 설비 알림은 §6.1 토픽을 선택적으로 구독한다. |
| 3.4 | 차트·로그에 공급 | 동일 스트림을 실시간 꺾은선 차트와 로그 롤링 패널에 전달하는 store 또는 composable을 둔다. |
| 3.5 | 재연결 | 네트워크 끊김 시 사용자 알림 및 선택적 자동 재구독 정책을 정한다. |

---

## 4. 화면별 구현 (UI·UX 명세 매핑)

### 4.1. 메인 대시보드 (Executive Summary)

- KPI 카드: 목표 대비 실적(Progress), 품질 합격률(Pass Rate). Progress는 `SEQ_NO` 기반 대체 지표(수량 대체)로 표시하고 데이터 출처는 `/app/execution/production/progress`, `/app/quality/inspections` 등 명세와 백엔드 구현에 맞춘다.
- 현장 현황판: 설비별 LOT, 진척도 게이지. 식별자·진행률은 STOMP 조회 응답과 실시간 토픽을 조합해 채운다.
- 시뮬레이션 시작과 연동해 차트가 갱신되는지 E2E 수준으로 확인한다.

### 4.2. 수주 및 생산 계획 (Order & Planning)

- 수주 테이블: `SalesOrders` 기반 목록(제품명, 주문량, 납기 등 API 필드에 따름).
- 생산 트리거 위젯: `current_inven`과 `safety_stock` 비교 표시(스키마·API 필드명 확인).
- 작업 지시 발행: 수주 선택 후 모달에서 `WorkOrder` 생성 API가 명세에 추가되면 연동한다. 당시점에 생성 엔드포인트가 없으면 `api-details.md`에 먼저 기록하는 것을 전제로 스텁·목 처리한다.

### 4.3. 실시간 공정 모니터링 (Execution Monitoring)

- 실시간 트렌드: `temp_pv`(실측, ℃), `temp_sp`(지시), `cr_temp`(목표), `speed`(m/min), X축 시간. 라이브러리(예: Chart.js, ECharts) 선정 후 STOMP 데이터로 append 또는 윈도우 슬라이스 갱신.
- 로그 스트리밍 패널: 수신 레코드를 롤링 텍스트로 표시, 가독성을 위해 최대 줄 수·자동 스크롤 옵션을 둔다.

### 4.4. 품질 검사 및 리포트 (Quality & Reporting)

- 산점도: 로트별 `color_de` 시각화(`/app/quality/inspections` 응답).
- 성적서 뷰어: `/app/quality/reports` 응답 구조에 맞춘 표준 양식 레이아웃.
- PDF 내보내기: SRS상 목표 기능. 브라우저 인쇄·서버 생성 등 방식이 정해지면 구현하고, 미정이면 문서에 “방식 확정 후”로 남긴다.

---

## 5. 상태 관리·권한·시뮬레이션 UI

| 항목 | 설명 |
| --- | --- |
| Pinia store | 사용자 역할(Admin·Operator), 시뮬레이션 가동 여부, WebSocket 최신 스냅샷 등을 모듈별로 분리한다. |
| 권한 | SRS상 로그인·권한이 요구사항에 있긴 하지만 형식적으로만 구현한다. 혹은 로그인 없이 역할 선택 버튼만 만들기. |
| 시뮬레이션 | `Simulation Start`·Stop·Reset과 대시보드 실시간 갱신 연계, Header에 가동 상태 표시. |

---

## 6. 품질·설비 이상·알림 UX

- `color_de` 임계(기준 1.0 이상) 품질 이상은 품질 화면의 전용 알림 패널에서 `QUALITY_COLOR_DE_HIGH` 유형으로 표시한다.
- 공정 설비 이상은 `docs/api-details.md` §6.1 STOMP 메시지(예: `/topic/equipment-alert`)를 구독해 모니터링 화면의 전용 알림 패널로 표시한다. 품질 알림과 문구·유형을 구분한다.
- 알림 표시 채널은 패널 방식으로 통일한다. 설비 이상 중복 알림은 `machine_id|wo_id|alert_type|message` 키와 쿨다운(`VITE_WS_EQUIPMENT_ALERT_COOLDOWN_MS`, 기본 10000ms)으로 억제한다.
- 모니터링 화면의 실시간 트렌드 스트림과 설비 이상 알림 목록은 스크롤 영역(max-height)으로 제한하고, 최근 N건만 표시해 가독성과 성능을 유지한다.
- 실시간 트렌드 차트는 `machine_id` 필터를 제공해 설비별로 분리 확인한다. 필요 시 `wo_id` 필터와 함께 사용해 특정 설비/작업지시 기준으로 좁혀 본다.
- 앱 셸 Header에 라이트/다크 테마 전환을 제공하고, 선택된 테마를 로컬 저장소에 유지해 재진입 시 동일 테마를 복원한다.

---

## 7. 개발·배포·품질

| 항목 | 설명 |
| --- | --- |
| 프록시 | 로컬에서 CORS 회피를 위해 `vite.config.js`에 `/api`, `/ws-mes` 등 프록시 설정을 둘지 결정한다. |
| 빌드 | `npm run build` 산출물 경로와 정적 호스팅·리버스 프록시 규칙을 백엔드 팀과 맞춘다. |
| 접근성·반응형 | 현장용으로 글자 크기·터치 영역을 최소 기준 이상으로 유지한다. |
| 테스트 | 단기간 소규모 팀이기 때문에 별도의 테스트 코드는 두지 않고, 실행 시 정상 작동을 기준으로 한다. |

---

## 8. 명세와의 동기화 (필수 습관)

- 스키마·필드·엔드포인트 변경 시 `data-schema-definition.md`, `api-details.md`를 먼저 갱신한 뒤 프론트를 수정한다.
- SRS에만 있고 위 문서에 없는 기능(OEE, PDF/HTML 세부, 인증 헤더 등)은 문서 반영 후 구현한다.

### 8.1 통신 방식 전환(WebSocket 메인) 후속 작업

REST 중심 설계에서 STOMP/WebSocket 메인 설계로 바뀌면서, 기능 구현 완료와 별개로 아래 정리 작업을 추가 수행한다.

- 전환 범위 고정: 화면별로 "REST 잔존 호출 0건" 기준을 잡고, 남아 있는 REST 의존 코드와 환경 변수(`VITE_API_BASE_URL` 등) 사용 여부를 점검한다.
- destination 계약 점검: 화면에서 사용하는 `/app/...`, `/user/queue/...`, `/topic/...` 경로를 `docs/api-details.md`와 1:1로 맞추고, 불일치가 있으면 문서와 코드 중 하나를 즉시 정리한다.
- 연결 상태 UX 정리: 연결 중/연결됨/끊김/재연결 중 상태를 Header 또는 공통 배너에 통일해 노출한다.
- 재연결/재구독 안정화: 네트워크 단절, 서버 재기동, 브라우저 탭 복귀 시 자동 재구독 동작을 표준 시나리오로 검증한다.
- 요청-응답 타임아웃 기준 확정: STOMP 요청 후 응답 대기 시간, 실패 메시지 표준 문구, 재시도 정책(횟수/간격)을 팀 규칙으로 고정한다.
- SockJS fallback 운영 기준: fallback 사용 조건, 동작 확인 방법, 로컬/배포 환경별 설정 차이를 문서화한다.
- 로깅/디버깅 가이드: destination, subscription id, correlation key, 수신 시각을 남기는 디버그 로그 규칙을 공통 모듈에 반영한다.

#### 8.1.1 재연결/재구독 검증 시나리오 체크리스트

아래 3개 시나리오를 모두 통과해야 "재연결/재구독 안정화" 항목을 완료로 본다.

| 시나리오 | 재현 방법 | 기대 결과 | 확인 항목 |
| --- | --- | --- | --- |
| 네트워크 단절 | 브라우저 DevTools에서 Offline 전환 후 10초 대기, Online 복귀 | 상태가 `reconnecting`을 거쳐 `connected`로 복귀하고 실시간 메시지 수신 재개 | Header 연결 상태 문구, 재연결 시도 횟수 증가, 메시지 수신 시각 갱신 |
| 서버 재기동 | 백엔드 서버 중지 후 10초 대기, 서버 재실행 | 연결 끊김 이후 자동 재접속, 기존 구독 토픽(`/topic/production-trend`) 재구독 | 연결 상태 전이 기록, 재기동 이후 신규 메시지 수신 |
| 탭 비활성/복귀 | 탭을 1분 이상 백그라운드로 전환 후 복귀 | 복귀 직후 연결 상태가 유지되거나 자동 복구되고 수신 지속 | 복귀 시 상태 표기 정상, 로그 스트림/차트 업데이트 재개 |

검증 기록 예시(매 회차 기록 권장):

- 검증 일시: `YYYY-MM-DD HH:mm`
- 검증 환경: `local/dev/stage`, 브라우저 버전
- 시나리오 결과: `PASS/FAIL`
- 근거: 상태 캡처, 로그 캡처, 재현 영상 경로
- 후속 조치: 실패 시 원인/수정 PR 링크

---

## 9. 권장 구현 순서(요약)

현재 1~4절 구현 완료를 기준으로, 다음 작업은 아래 우선순위로 진행한다.

1. 통신 전환 안정화(§8.1)  
   - REST 잔존 호출/환경 변수 제거 점검  
   - destination(send/subscribe) 문서 정합성 점검  
   - 재연결·재구독·타임아웃·오류 처리 기준 확정  
2. 품질·설비 이상 알림 UX 완료(§6)  
   - 품질 이상과 설비 이상 알림 채널 분리  
   - 알림 표시 방식(토스트/배너) 및 중복 방지 규칙 확정  
3. 상태 관리·권한 UI 보강(§5)  
   - 연결 상태(연결/끊김/재연결) 공통 표기  
   - 권한 처리 방식(임시/정식) 고도화  
4. 개발·배포·품질 기준 고정(§7)  
   - 프록시/배포 규칙 정리  
   - 테스트 범위와 방식 확정  
5. 미확정 기능 착수 준비(§8, §10)  
   - PDF·인증·OEE 관련 명세 확정 후 구현 착수  

이 순서는 백엔드 준비 상황에 따라 일부 병행하거나 조정할 수 있다.

---

## 10. 체크리스트 (복사용)

- [x] `VITE_*` 환경 변수 및 WebSocket 연결 정보 적용  
- [x] 공통 STOMP 메시지 레이어 및 에러·로딩 처리  
- [x] Sidebar·Header·Content 레이아웃 및 라우트 메뉴  
- [x] 명세의 STOMP 조회/제어 destination 전 화면 연동  
- [x] `/app/simulation/*` 제어 및 UI 상태  
- [x] STOMP `/ws-mes`·`/topic/production-trend` 구독(필요 시 `/topic/equipment-alert`)  
- [x] 실시간 온도·속도 차트  
- [x] 로그 롤링 패널  
- [x] 대시보드 KPI·현황판  
- [x] 수주 테이블·재고 트리거·작업 지시 모달(API 준비 시)  
- [x] 품질 산점도·성적서 뷰어  
- [x] 품질 이상 알림 UX  
- [ ] PDF·인증·OEE 등 미확정 항목은 명세 갱신 후 구현  
- [x] REST 잔존 호출/환경 변수 사용 0건 점검 완료  
- [x] 화면별 destination(`send/subscribe`)과 `docs/api-details.md` 정합성 점검 완료  
- [x] 연결 상태 UX(연결/끊김/재연결) 공통 표기 반영  
- [ ] 재연결/재구독 시나리오(네트워크 단절/서버 재기동/탭 복귀) 검증 완료  
- [ ] STOMP 요청 타임아웃/재시도/오류 문구 팀 기준 확정  
- [ ] SockJS fallback 동작 및 환경별 설정 검증 완료  
- [ ] STOMP 디버그 로깅 규칙(destination, subscription id, correlation key) 반영  

문서 버전: 프론트 스캐폴드 기준 초안. 폴더 구조·스택 변경 시 본 문서의 §0과 §1을 함께 수정한다.
