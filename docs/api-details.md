심플 MES(Simplified MES) 프로젝트의 백엔드와 프론트엔드 간 메시지 인터페이스를 정의한 상세 명세서입니다. 본 명세서는 사전 전처리된 통합 데이터셋을 기반으로 하며, 조회·제어·실시간 스트리밍을 STOMP/WebSocket으로 통일합니다. Planning·Execution 관련 요청·응답 예시와 클라이언트 테스트 절차는 본 문서 부록에 수록한다.

## 심플 MES 메시지 상세 명세서 (v1.2)

### 1. 공통 사항

- WebSocket Endpoint: `/ws-mes`
- STOMP Application Prefix: `/app`
- Broker Prefix: `/topic`, `/queue`
- User Destination Prefix: `/user`
- 통신 프로토콜: STOMP/WebSocket (SockJS fallback 포함)
- 데이터 포맷: JSON (UTF-8).
- **JSON 필드명**: 요청·응답 본문의 키는 `docs/data-schema-definition.md` 절 2의 컬럼명과 동일한 스네이크 케이스를 사용한다 (`wo_id`, `order_qty`, `pass_fail` 등).
- 응답 규격:
    - `success`: 성공 여부 (boolean).
    - `data`: 실제 데이터 객체 혹은 배열.
    - `message`: 오류 발생 시 원인 메시지(성공 시에는 빈 문자열 또는 생략 가능, 팀에서 통일).
- 오류 처리: 실패는 `success: false`와 `message`로 전달하며, 필요 시 `/user/queue/errors`를 구독해 에러 메시지를 수신한다.
- 인증·인가: SRS상 로그인·권한(Admin/Operator) 플로우는 시연용 클라이언트 로컬 역할 선택으로만 형식적으로 표현하며, 이번 프로젝트 범위에서는 서버 측 인증·인가 및 REST/STOMP/WebSocket 접근 제어를 구현하지 않는다.

개발 환경에서의 접속 URL 예시는 `ws://{호스트}:{포트}/ws-mes` 형태이며, 로컬에서 흔히 `ws://localhost:8080/ws-mes` 로 연결한다. 호스트·포트는 배포 환경에 맞게 바꾼다.

### 1.1. 목록·필터·페이지네이션

- 목록 조회의 필터·페이지 조건은 STOMP 요청 본문으로 전달한다.
- `page`, `size`, 날짜 범위 등은 초기 명세에 고정하지 않으며 필요 시 표로 확정한다.

### 1.2. 시뮬레이션 제어 요청 본문

- `/app/simulation/start`, `/app/simulation/stop`, `/app/simulation/reset`은 요청 본문 없음(빈 JSON)을 기본으로 한다.
- 배속·파일 경로 등 옵션이 필요해지면 요청 본문 필드를 본 절에 추가한다.

### 2. 제품 및 수주 관리 API (Planning)

사전 전처리를 통해 동기화된 제품 마스터와 주문 정보를 조회합니다.

| 기능명 | Send Destination | Subscribe Destination | 설명 | 관련 테이블 |
| --- | --- | --- | --- | --- |
| 제품 목록 조회 | `/app/planning/products` | `/user/queue/planning/products` | 등록된 모든 의류 제품군 리스트 반환. | `Products` |
| 수주 현황 조회 | `/app/planning/orders` | `/user/queue/planning/orders` | 전처리로 수치가 동기화된 일일 주문 현황 조회. | `SalesOrders` |
| 특정 제품 수주 조회 | `/app/planning/orders/by-product` | `/user/queue/planning/orders/by-product` | 요청 본문 `product_id`로 특정 제품 주문 조회. | `SalesOrders` |

### 3. 생산 및 작업 지시 API (Execution)

작업 지시 현황과 시뮬레이션으로 적재되는 실시간 로그를 관리합니다.

| 기능명 | Send Destination | Subscribe Destination | 설명 | 관련 테이블 |
| --- | --- | --- | --- | --- |
| 작업 지시 목록 | `/app/execution/work-orders` | `/user/queue/execution/work-orders` | 수주로부터 분할된 로트(LOT) 단위 작업 리스트. | `WorkOrders` |
| 실시간 로그 조회 | `/app/execution/production/logs` | `/user/queue/execution/production/logs` | 요청 본문 `wo_id`로 특정 로트 로그 조회. | `ProductionLogs` |
| 진척률 정보 조회 | `/app/execution/production/progress` | `/user/queue/execution/production/progress` | 전 가동 라인의 목표 대비 현재 진척률 집계 정보. | `WorkOrders` |

### 4. 시뮬레이션 제어 API (Simulation)

백엔드 스케줄러의 동작을 제어하여 현장 데이터를 재현합니다.

| 기능명 | Send Destination | Subscribe Destination | 설명 |
| --- | --- | --- | --- |
| 시뮬레이션 시작 | `/app/simulation/start` | `/user/queue/simulation/state` | 스케줄러 가동 및 `ProductionLogs` 데이터 적재 개시. |
| 시뮬레이션 정지 | `/app/simulation/stop` | `/user/queue/simulation/state` | 데이터 적재 중단 및 현재 상태 보존. |
| 시뮬레이션 초기화 | `/app/simulation/reset` | `/user/queue/simulation/state` | `ProductionLogs` 초기화 및 포인터 리셋. |

### 5. 품질 및 리포트 API (Quality & Reporting)

품질 판정 결과와 최종 생산 보고서용 데이터를 제공합니다.

| 기능명 | Send Destination | Subscribe Destination | 설명 | 관련 테이블 |
| --- | --- | --- | --- | --- |
| 품질 검사 결과 | `/app/quality/inspections` | `/user/queue/quality/inspections` | 로트별 색차(`color_de`) 및 합불(`pass_fail`) 결과 조회. | `Inspections` |
| 공정 보고서 데이터 | `/app/quality/reports` | `/user/queue/quality/reports` | 요청 본문 `wo_id` 기반 통합 보고서 조회. | 통합 조인 |

### 6. 실시간 스트리밍 및 알림 (STOMP)

별도 요청 없이 백엔드에서 프론트엔드로 푸시되는 이벤트 채널입니다.

- Endpoint: `/ws-mes` (SockJS fallback 허용)
- Topic (Subscribe): `/topic/production-trend`
- 데이터 내용: `docs/data-schema-definition.md` 절 2.4 `ProductionLogs`와 동일한 스네이크 케이스 필드를 전제로 하며, 진척률 등 파생 값은 아래와 같이 포함할 수 있다.

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

### 6.1. 설비 이상 알림

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

`/user/queue/execution/production/logs` 응답의 각 요소도 `data-schema-definition.md` 절 2.4 컬럼과 동일한 키를 사용한다.

---

## 부록

### 부록 A. STOMP 연결·공통 envelope 예시

클라이언트가 STOMP `CONNECT` 시 보낼 수 있는 헤더 예시는 다음과 같다.

```json
{"accept-version":"1.2","host":"localhost"}
```

공통 응답 envelope 예시는 다음과 같다.

```json
{
  "success": true,
  "data": [],
  "message": null
}
```

### 부록 B. Planning·Execution 요청·응답 예시

아래는 절 2·3의 destination과 맞춘 예시이며, 필드 값은 `data-schema-definition.md` 절 2와 일치하도록 한다.

#### B.1 제품 목록 조회

- Send: `/app/planning/products`, Subscribe: `/user/queue/planning/products`, 본문: `{}`

```json
{
  "success": true,
  "data": [
    {
      "product_id": "P-1001",
      "name": "고신축 폴리 원단",
      "category": "Dyeing",
      "safety_stock": 120
    }
  ],
  "message": null
}
```

#### B.2 수주 현황 조회

- Send: `/app/planning/orders`, Subscribe: `/user/queue/planning/orders`, 본문: `{}`

```json
{
  "success": true,
  "data": [
    {
      "order_id": "SO-20220101-001",
      "product_id": "P-1001",
      "order_date": "2022-01-01",
      "order_qty": 400.0
    }
  ],
  "message": null
}
```

#### B.3 특정 제품 수주 조회

- Send: `/app/planning/orders/by-product`, Subscribe: `/user/queue/planning/orders/by-product`

```json
{
  "product_id": "P-1001"
}
```

`product_id`가 없으면 예를 들어 다음과 같은 응답이 올 수 있다.

```json
{
  "success": false,
  "data": null,
  "message": "product_id is required"
}
```

#### B.4 작업 지시 목록 조회

- Send: `/app/execution/work-orders`, Subscribe: `/user/queue/execution/work-orders`, 본문: `{}`

```json
{
  "success": true,
  "data": [
    {
      "wo_id": "F2205040066",
      "order_id": "SO-20220101-001",
      "planned_qty": 400.0,
      "machine_id": "M-01"
    }
  ],
  "message": null
}
```

#### B.5 특정 작업 지시 생산 로그 조회

- Send: `/app/execution/production/logs`, Subscribe: `/user/queue/execution/production/logs`

```json
{
  "wo_id": "F2205040066"
}
```

`data` 배열 원소는 `data-schema-definition.md` 절 2.4 `ProductionLogs` 컬럼을 갖춘다. 예시는 다음과 같다.

```json
{
  "success": true,
  "data": [
    {
      "log_id": 1,
      "wo_id": "F2205040066",
      "timestamp": "2022-05-04 10:30:00",
      "cr_temp": 70,
      "temp_sp": 70.0,
      "temp_pv": 68.4,
      "speed": 62
    }
  ],
  "message": null
}
```

`wo_id`가 없으면 예를 들어 다음과 같은 응답이 올 수 있다.

```json
{
  "success": false,
  "data": null,
  "message": "wo_id is required"
}
```

#### B.6 진척률 조회

- Send: `/app/execution/production/progress`, Subscribe: `/user/queue/execution/production/progress`, 본문: `{}`

```json
{
  "success": true,
  "data": {
    "progress": 0.6,
    "work_orders": [
      {
        "wo_id": "F2205040066",
        "progress": 0.8
      }
    ]
  },
  "message": null
}
```

### 부록 C. 진척률 산출 참고

절 3의 진척률 집계를 구현할 때 참고할 수 있는 산출 예시는 다음과 같다. 배포·구현에 따라 다를 수 있으며 본문 표(절 3)가 우선한다.

- 작업지시별 progress:
  - `t_current` = 해당 `wo_id`의 현재 적재 로그 건수
  - `t_total` = `WorkOrders.planned_qty`
  - `P = min(100, (t_current / t_total) * 100)` (소수점 한 자리 반올림)
- 전체 progress: 작업지시별 progress 평균

### 부록 D. 클라이언트 도구 입력 예시 (Postman 등)

- URL 예시: `ws://localhost:8080/ws-mes`
- Connect Type: `STOMP`
- STOMP connect header: 부록 A와 동일 JSON 사용 가능

Planning:

| 동작 | Subscribe | Send destination | Message content |
| --- | --- | --- | --- |
| 제품 목록 | `/user/queue/planning/products` | `/app/planning/products` | `{}` |
| 수주 현황 | `/user/queue/planning/orders` | `/app/planning/orders` | `{}` |
| 특정 제품 수주 | `/user/queue/planning/orders/by-product` | `/app/planning/orders/by-product` | `{"product_id":"P-1001"}` |

Execution:

| 동작 | Subscribe | Send destination | Message content |
| --- | --- | --- | --- |
| 작업 지시 목록 | `/user/queue/execution/work-orders` | `/app/execution/work-orders` | `{}` |
| 생산 로그 | `/user/queue/execution/production/logs` | `/app/execution/production/logs` | `{"wo_id":"F2205040066"}` |
| 진척률 | `/user/queue/execution/production/progress` | `/app/execution/production/progress` | `{}` |

### 부록 E. 관련 문서

- `docs/data-schema-definition.md`
- `docs/system-architecture-design.md`
- `docs/functional-requirement-statement.md`

### 부록 F. 구현 코드 위치 (참고)

다음 경로는 과거 백엔드 초안을 정리한 것이다. 저장소 최신 트리와 다를 수 있으므로, 실제 클래스·패키지는 코드 검색으로 확인한다.

Planning:

- `src/main/java/group1/be_mes_project/controller/PlanningStompController.java`
- `src/main/java/group1/be_mes_project/service/PlanningService.java`
- `src/main/java/group1/be_mes_project/service/impl/PlanningServiceImpl.java`
- `src/main/java/group1/be_mes_project/domain/repository/ProductRepository.java`
- `src/main/java/group1/be_mes_project/domain/repository/SalesOrderRepository.java`
- `src/main/java/group1/be_mes_project/config/PlanningSeedData.java`

Execution:

- `src/main/java/group1/be_mes_project/controller/ExecutionStompController.java`
- `src/main/java/group1/be_mes_project/service/ExecutionService.java`
- `src/main/java/group1/be_mes_project/service/impl/ExecutionServiceImpl.java`
- `src/main/java/group1/be_mes_project/domain/repository/WorkOrderRepository.java`
- `src/main/java/group1/be_mes_project/domain/repository/ProductionLogRepository.java`
- `src/main/java/group1/be_mes_project/dto/execution/ExecutionLogFilterDto.java`

공통:

- `src/main/java/group1/be_mes_project/config/MesWebSocketConfig.java`
- `src/main/java/group1/be_mes_project/dto/ApiResponse.java`
