심플 MES(Simplified MES) 프로젝트의 백엔드와 프론트엔드 간 인터페이스를 정의한 API 상세 명세서입니다. 본 명세서는 사전 전처리된 통합 데이터셋을 기반으로 하며, 실시간 시뮬레이션 제어 및 데이터 조회를 위한 엔드포인트를 포함합니다 .

## 심플 MES API 상세 명세서 (v1.0)

### 1. 공통 사항

- Base URL: `http://localhost:8080/api/v1`.
- 통신 프로토콜: HTTP/REST (조회 및 제어), STOMP/WebSocket (실시간 트렌드).
- 데이터 포맷: JSON (UTF-8).
- 응답 규격:
    - `success`: 성공 여부 (boolean).
    - `data`: 실제 데이터 객체 혹은 배열.
    - `message`: 오류 발생 시 원인 메시지.

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

백엔드 스케줄러의 동작을 제어하여 현장 데이터를 재현합니다 .

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
- 데이터 내용:

    ```json
    {
      "wo_id": "F2205040066",
      "temp_pv": 69.5,
      "speed": 65,
      "timestamp": "2022-05-04 10:30:00",
      "progress": 45.2
    }
    ```