# GitHub Copilot: 심플 MES 프로젝트 지시문

이 저장소는 **심플 MES**용 모노레포이다. 제안하는 코드와 리팩터링은 아래 규범을 벗어나지 않아야 한다. 우선순위가 겹치면 **기능 요구 명세서(SRS v2.2), 데이터 스키마 정의서, 데이터 생성 리포트, 시스템 아키텍처, API 명세, UI/UX 명세**에 적힌 내용을 따른다.

---

## 1. 시스템 아키텍처 및 기술 스택

- **구성**: `backend/`는 **Java 17 이상, Spring Boot 3.x, MySQL 8.x**. `frontend/`는 **Vue.js 3.x**.
- **모노레포**: 백엔드와 프론트엔드는 루트 아래 디렉터리로 분리되어 있다. 한쪽만을 가정한 전역 설정이나 경로 가정은 피한다.
- **실시간 데이터**: HTTP로 조회·제어하고, **실시간 푸시는 STOMP 기반 WebSocket만** 사용한다.
  - 엔드포인트: `/ws-mes`
  - 구독 토픽: `/topic/production-trend`
  - 폴링이나 별도 소켓으로 동일 목적을 대체하는 제안은 하지 않는다. 상세는 `docs/api-details.md`를 따른다.

---

## 2. 데이터 처리 제약 (필수)

- **런타임 전처리 금지**: 백엔드에서 원천 데이터를 재가공·재집계·동기화하는 **전처리 로직을 새로 제안하지 않는다**. SRS와 데이터 생성 리포트에 명시된 대로, 데이터는 **이미 전처리된 통합 데이터셋**을 전제로 한다.
- **시뮬레이터 중심**: 구현 초점은 **전처리된 CSV(및 명세에 정한 파일)를 순차적으로 읽어 MySQL에 INSERT하는 시뮬레이터**이다. Spring Boot `@Scheduled`로 1분 주기 적재 패턴을 유지한다. 파일명 예: `PRODUCTION_TREND_preprocessed.csv` (실제 경로는 데이터 배치 정책에 따름).
- **기간 전제**: 도메인 데이터는 **2022년 1월부터 10월까지** 실측·정합성 조정이 끝난 범위를 사용한다. 임의로 다른 연도나 가상 구간을 생성하는 제안은 하지 않는다.
- 상세 전처리·검증 규칙은 `docs/data-generation-report.md`, 요구사항 전제는 `docs/functional-requirement-statement.md`를 참고한다.

---

## 3. 데이터 모델 및 스키마 준수

- **규범 문서**: 테이블·컬럼명·타입은 **`docs/data-schema-definition.md`**에 정의된 내용을 따른다. 코드 생성 시 이 파일과 **필드명을 일치**시킨다.
- **주요 테이블**: `Products`, `SalesOrders`, `WorkOrders`, `ProductionLogs`, `Inspections` 등 명세에 나온 이름과 PK, FK 관계를 존중한다. 엔티티·DTO·JSON 필드명은 스키마의 스네이크 케이스 컬럼과 프로젝트 컨벤션(camelCase 매핑 등)에 맞춘다.

### 3.1 `Products` 테이블과 에스윈텍·의류 매핑 (코드 제안 시 반드시 고려)

`Products`는 에스윈텍 주문 마스터와 의류 공정 데이터를 **이미 매핑한 결과**를 담는다. 새로 매핑 알고리즘을 짜지 말고, 아래 **출처 규칙**을 설명·반영한다.

- **`product_id`**: 에스윈텍 `code`와 의류 `공정코드`를 연결한다. 에스윈텍 쪽 값이 비어 있으면 의류 데이터의 `공정코드`로 대치한 값이 쓰인다.
- **`name`**: 에스윈텍 `name`과 의류 `작업명`을 연결한다. 에스윈텍 쪽이 비어 있으면 의류 `작업명`으로 대치한다.
- **`category`**: 에스윈텍 `hierarchy`(제품 계층·분류).
- **`safety_stock`**: 에스윈텍 `safety_stock`.

이 매핑은 **데이터 생성 단계에서 확정**된 것이므로, 애플리케이션에서 소스별로 다시 조합하는 로직을 추가하지 않는다.

---

## 4. 비즈니스 로직 및 품질 기준

### 4.1 진척률

- 실시간 진척률 $P$는 다음 공식을 따른다.

$$P = \frac{t_{\mathrm{current}}}{t_{\mathrm{total}}} \times 100$$

- $t_{\mathrm{current}}$, $t_{\mathrm{total}}$의 의미와 데이터 출처는 SRS와 구현된 도메인 모델에 맞춘다. 임의의 다른 진행률 정의로 바꾸지 않는다.

### 4.2 품질 판정

- **염색 색차 DE**(`color_de` 등 스키마상 동일 의미 필드)가 **1.0 미만**이면 **합격(Pass)**, **1.0 이상**이면 불합격(Fail) 처리하는 것을 기본으로 한다.
- SRS의 표현과 일치시키며, 임계값을 임의로 완화하거나 다른 지표로 대체하지 않는다.

---

## 5. 코딩 스타일 및 API 인터페이스

### 5.1 Vue.js

- **Vue 3**, **Composition API**, **`<script setup>`** 형식을 사용한다. Options API 위주 템플릿이나 구식 패턴으로 새 코드를 제안하지 않는다.

### 5.2 REST 응답 규격

- 백엔드 REST 응답은 공통으로 **`success`**, **`data`**, **`message`** 필드를 포함하는 형태를 유지한다 (`docs/api-details.md`).
- 프론트엔드에서는 이 규격을 처리하는 **공통 Axios 인터셉터** 패턴을 따른다.

### 5.3 스타일 가이드

- Java: Google Java Style에 가깝게.
- JavaScript/Vue: Airbnb 스타일을 기본으로, 식별자는 **camelCase**.

---

## 6. 참고 문서 경로 (저장소 기준)

| 문서 | 경로 |
| --- | --- |
| SRS v2.2 | `docs/functional-requirement-statement.md` |
| 데이터 스키마 | `docs/data-schema-definition.md` (PDF `데이터_스키마_정의서.pdf`와 동일 역할) |
| 데이터 생성 리포트 | `docs/data-generation-report.md` |
| 시스템 아키텍처 | `docs/system-architecture-design.md` |
| API 상세 | `docs/api-details.md` |
| UI/UX | `docs/ui-ux-component-specification.md` |

코드 제안 전에 관련 문서를 읽을 수 있으면 읽고, 불확실하면 위 규범과 충돌하는 추측 코드를 만들지 않는다.
