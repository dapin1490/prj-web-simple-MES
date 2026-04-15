심플 MES(Simplified MES) 프로젝트의 백엔드와 프론트엔드 간 인터페이스를 정의한 API 상세 명세서입니다. 본 명세서는 사전 전처리된 통합 데이터셋을 기반으로 하며, 실시간 시뮬레이션 제어 및 데이터 조회를 위한 엔드포인트를 포함합니다.

## 심플 MES API 상세 명세서 (v1.0)

### 1. 공통 사항

- Base URL: `http://localhost:8080/api/v1` (배포 환경에서는 동일 경로 구조를 유지하고 호스트만 설정으로 치환).
- **경로 결합**: 아래 표의 엔드포인트는 Base URL 뒤에 이어 붙인다. 예: 제품 목록은 `GET http://localhost:8080/api/v1/products`.
- 통신 프로토콜: HTTP/REST (조회 및 제어), STOMP/WebSocket (실시간 트렌드).
- 데이터 포맷: JSON (UTF-8).
- **JSON 필드명**: 요청·응답 본문의 키는 `docs/data-schema-definition.md` §2의 **컬럼명과 동일한 스네이크 케이스**를 사용한다 (`wo_id`, `order_qty`, `pass_fail` 등). Java·TypeScript 코드 내부의 camelCase 매핑은 구현 관례에 따르되, **HTTP JSON 계약은 스키마 컬럼명을 기준**으로 한다.
- 응답 규격:
    - `success`: 성공 여부 (boolean).
    - `data`: 실제 데이터 객체 혹은 배열.
    - `message`: 오류 발생 시 원인 메시지(성공 시에는 빈 문자열 또는 생략 가능, 팀에서 통일).
- **HTTP 상태 코드**: 성공 시 주로 `200 OK`. 리소스 생성을 도입할 경우 `201 Created` 등을 사용할 수 있으며, 해당 엔드포인트 설명에 명시한다. 클라이언트 오류는 `4xx`, 서버 오류는 `5xx`이며, 본문은 위 응답 규격을 유지하고 `success: false`로 구분한다.
- **인증·인가**: SRS상 로그인·권한(Admin/Operator)은 요구사항에 포함되나, **REST API의 인증 헤더·토큰 형식은 구현 단계에서 본 문서에 추가**한다. 명세가 없는 동안에는 공개 엔드포인트로 가정하거나 개발용으로만 사용한다.

### 1.1. 목록·필터·페이지네이션

- `GET` 목록 API에 대한 쿼리 파라미터(`page`, `size`, 날짜 범위 등)는 **초기 명세에 고정하지 않는다.** 필요해지면 구현과 함께 본 문서에 표로 추가한다.

### 1.2. 시뮬레이션 제어 요청 본문

- `POST /simulation/start`, `POST /simulation/stop`, `POST /simulation/reset`은 **요청 본문 없음**(빈 JSON 또는 본문 생략)을 전제로 한다. 배속·파일 경로 등 옵션이 필요해지면 필드 정의와 함께 이 절에 추가한다.

### 2. 제품 및 수주 관리 API (Planning)

사전 전처리를 통해 동기화된 제품 마스터와 주문 정보를 조회합니다.

| 기능명 | 메서드 | 엔드포인트 | 설명 | 관련 테이블 |
| --- | --- | --- | --- | --- |
| 제품 목록 조회 | `GET` | `/products` | 등록된 모든 의류 제품군 리스트 반환. | `Products` |
| 수주 현황 조회 | `GET` | `/orders` | 전처리로 수치가 동기화된 일일 주문 현황 조회. | `SalesOrders` |
| 특정 제품 수주 조회 | `GET` | `/orders/{product_id}` | 특정 제품의 날짜별 주문 및 재고 현황 필터링. | `SalesOrders` |

### 3. 생산 및 작업 지시 API (Execution)

작업 지시 현황과 시뮬레이션으로 적재되는 실시간 로그를 관리합니다.

| 기능명 | 메서드 | 엔드포인트 | 설명 | 관련 테이블 |
| --- | --- | --- | --- | --- |
| 작업 지시 목록 | `GET` | `/work-orders` | 수주로부터 분할된 로트(LOT) 단위 작업 리스트. | `WorkOrders` |
| 실시간 로그 조회 | `GET` | `/production/logs/{wo_id}` | 특정 로트에서 현재까지 적재된 센서 데이터 전체 조회. | `ProductionLogs` |
| 진척률 정보 조회 | `GET` | `/production/progress` | 전 가동 라인의 목표 대비 현재 진척률($P$) 집계 정보. | `WorkOrders` |

### 4. 시뮬레이션 제어 API (Simulation)

백엔드 스케줄러의 동작을 제어하여 현장 데이터를 재현합니다.

| 기능명 | 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- | --- |
| 시뮬레이션 시작 | `POST` | `/simulation/start` | 스케줄러 가동 및 `ProductionLogs` 테이블 데이터 적재 개시. |
| 시뮬레이션 정지 | `POST` | `/simulation/stop` | 데이터 적재 중단 및 현재 상태 보존. |
| 시뮬레이션 초기화 | `POST` | `/simulation/reset` | `ProductionLogs` 테이블 초기화 및 포인터 리셋. |

### 5. 품질 및 리포트 API (Quality & Reporting)

품질 판정 결과와 최종 생산 보고서용 데이터를 제공합니다.

| 기능명 | 메서드 | 엔드포인트 | 설명 | 관련 테이블 |
| --- | --- | --- | --- | --- |
| 품질 검사 결과 | `GET` | `/inspections` | 로트별 색차(`color_de`) 및 합불(`pass_fail`) 결과 조회. | `Inspections` |
| 공정 보고서 데이터 | `GET` | `/reports/{wo_id}` | 특정 로트의 작업 사양, 생산 로그, 품질 결과 통합 데이터. | 통합 조인 |

### 6. WebSocket 실시간 스트리밍 (STOMP)

별도의 API 요청 없이 백엔드에서 프론트엔드로 실시간 푸시되는 이벤트입니다.

- Endpoint: `/ws-mes`
- Topic (Subscribe): `/topic/production-trend`
- 데이터 내용: `docs/data-schema-definition.md` §2.4 `ProductionLogs`와 동일한 스네이크 케이스 필드를 전제로 하며, 진척률 등 파생 값은 아래와 같이 포함할 수 있다.

    ```json
    {
      "wo_id": "F2205040066",
      "cr_temp": 70,
      "temp_sp": 70.0,
      "temp_pv": 69.5,
      "speed": 65,
      "timestamp": "2022-05-04 10:30:00",
      "progress": 45.2
    }
    ```

### 6.1. WebSocket 설비 이상 알림 (선택 구현)

설비 이상 감지 시 작업자 확인용 메시지를 **트렌드와 분리된 토픽**으로 보낼 수 있다.

- Topic (Subscribe): `/topic/equipment-alert`
- 데이터 내용(예시, 필드는 구현 시 확정):

    ```json
    {
      "machine_id": "R001",
      "wo_id": "F2205040066",
      "alert_type": "TEMP_HIGH",
      "message": "설비 R001 이상 감지: 온도 높음. 확인 요망.",
      "timestamp": "2022-05-04 10:30:00"
    }
    ```

`GET /production/logs/{wo_id}` 응답의 각 요소도 §2.4 컬럼과 동일한 키를 사용한다.