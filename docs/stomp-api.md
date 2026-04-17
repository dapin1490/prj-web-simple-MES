# Planning / Execution STOMP 계약 통합 문서

이 문서는 지금까지 구현한 **Planning**과 **Execution** 기능의 소켓 통신 계약을 하나로 묶은 통합 문서다.

- 통신 방식: **SockJS + STOMP**
- WebSocket endpoint: `/ws-mes`
- application destination prefix: `/app`
- subscribe prefix: `/user/queue`
- 공통 응답 규격: `success`, `data`, `message`
- JSON 필드명: 스키마 기준 `snake_case`

참고 문서:
- `docs/api-details.md`
- `docs/data-schema-definition.md`
- `docs/system-architecture-design.md`
- `docs/functional-requirement-statement.md`

---

## 1. 공통 WebSocket 설정

현재 서버 설정은 다음 전제를 따른다.

- 접속 URL: `ws://localhost:8080/ws-mes`
- STOMP 연결 사용
- SockJS 클라이언트 호환 지원
- 요청은 `/app/...` 로 전송
- 응답은 `/user/queue/...` 를 구독해서 수신

### 공통 connect header 예시

```json
{"accept-version":"1.2","host":"localhost"}
```

### 공통 응답 envelope

```json
{
  "success": true,
  "data": [],
  "message": null
}
```

---

## 2. Planning STOMP 계약

Planning 영역은 제품 마스터와 수주 데이터를 조회한다.

### 2.1 제품 목록 조회

- 요청 send destination: `/app/planning/products`
- subscribe destination: `/user/queue/planning/products`
- message content: `{}`

#### 기대 응답 예시

```json
{
  "success": true,
  "data": [
    {
      "product_id": "P-1001",
      "name": "High Stretch Poly Fabric",
      "category": "Dyeing",
      "safety_stock": 120
    }
  ],
  "message": null
}
```

### 2.2 수주 현황 조회

- 요청 send destination: `/app/planning/orders`
- subscribe destination: `/user/queue/planning/orders`
- message content: `{}`

#### 기대 응답 예시

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

### 2.3 특정 제품 수주 조회

- 요청 send destination: `/app/planning/orders/by-product`
- subscribe destination: `/user/queue/planning/orders/by-product`
- message content:

```json
{
  "product_id": "P-1001"
}
```

#### 실패 예시

`product_id`가 없으면 다음 응답이 올 수 있다.

```json
{
  "success": false,
  "data": null,
  "message": "product_id is required"
}
```

---

## 3. Execution STOMP 계약

Execution 영역은 작업 지시, 생산 로그, 진척률을 조회한다.

### 3.1 작업 지시 목록 조회

- 요청 send destination: `/app/execution/work-orders`
- subscribe destination: `/user/queue/execution/work-orders`
- message content: `{}`

#### 기대 응답 예시

```json
{
  "success": true,
  "data": [
    {
      "wo_id": "WO-220101-001",
      "order_id": "SO-20220101-001",
      "planned_qty": 400.0,
      "machine_id": "M-01"
    }
  ],
  "message": null
}
```

### 3.2 특정 작업 지시의 생산 로그 조회

- 요청 send destination: `/app/execution/production/logs/by-wo`
- subscribe destination: `/user/queue/execution/production/logs/by-wo`
- message content:

```json
{
  "wo_id": "WO-220101-001"
}
```

#### 기대 응답 예시

```json
{
  "success": true,
  "data": [
    {
      "log_id": 1,
      "wo_id": "WO-220101-001",
      "timestamp": "2022-01-01T09:00:00",
      "temp_pv": 68.4,
      "speed": 62
    }
  ],
  "message": null
}
```

#### 실패 예시

`wo_id`가 없으면 다음 응답이 올 수 있다.

```json
{
  "success": false,
  "data": null,
  "message": "wo_id is required"
}
```

### 3.3 진척률 조회

- 요청 send destination: `/app/execution/production/progress`
- subscribe destination: `/user/queue/execution/production/progress`
- message content: `{}`

#### 기대 응답 예시

```json
{
  "success": true,
  "data": {
    "progress": 0.6,
    "work_orders": [
      {
        "wo_id": "WO-220101-001",
        "progress": 0.8
      }
    ]
  },
  "message": null
}
```

### 3.4 진척률 계산 기준

현재 구현은 다음 기준으로 계산한다.

- 작업지시별 progress:
  - `t_current` = 해당 `wo_id`의 현재 적재 로그 건수
  - `t_total` = `WorkOrders.planned_qty`
  - `P = min(100, (t_current / t_total) * 100)`
  - 소수점 1자리 반올림
- 전체 progress:
  - 작업지시별 progress 평균

---

## 4. Postman 입력 예시

### 4.1 공통 입력값

- URL

```text
ws://localhost:8080/ws-mes
```

- Connect Type

```text
STOMP
```

- STOMP connect header

```json
{"accept-version":"1.2","host":"localhost"}
```

### 4.2 Planning 입력값

#### 제품 목록 조회

- subscribe destination: `/user/queue/planning/products`
- STOMP send header: `{}`
- STOMP send destination: `/app/planning/products`
- Message Content: `{}`

#### 수주 현황 조회

- subscribe destination: `/user/queue/planning/orders`
- STOMP send header: `{}`
- STOMP send destination: `/app/planning/orders`
- Message Content: `{}`

#### 특정 제품 수주 조회

- subscribe destination: `/user/queue/planning/orders/by-product`
- STOMP send header: `{}`
- STOMP send destination: `/app/planning/orders/by-product`
- Message Content:

```json
{"product_id":"P-1001"}
```

### 4.3 Execution 입력값

#### 작업 지시 목록 조회

- subscribe destination: `/user/queue/execution/work-orders`
- STOMP send header: `{}`
- STOMP send destination: `/app/execution/work-orders`
- Message Content: `{}`

#### 특정 작업 지시 로그 조회

- subscribe destination: `/user/queue/execution/production/logs/by-wo`
- STOMP send header: `{}`
- STOMP send destination: `/app/execution/production/logs/by-wo`
- Message Content:

```json
{"wo_id":"WO-220101-001"}
```

#### 진척률 조회

- subscribe destination: `/user/queue/execution/production/progress`
- STOMP send header: `{}`
- STOMP send destination: `/app/execution/production/progress`
- Message Content: `{}`

---

## 5. 구현 파일 매핑

### Planning

- `src/main/java/group1/be_mes_project/controller/PlanningStompController.java`
- `src/main/java/group1/be_mes_project/service/PlanningService.java`
- `src/main/java/group1/be_mes_project/service/impl/PlanningServiceImpl.java`
- `src/main/java/group1/be_mes_project/domain/repository/ProductRepository.java`
- `src/main/java/group1/be_mes_project/domain/repository/SalesOrderRepository.java`
- `src/main/java/group1/be_mes_project/config/PlanningSeedData.java`

### Execution

- `src/main/java/group1/be_mes_project/controller/ExecutionStompController.java`
- `src/main/java/group1/be_mes_project/service/ExecutionService.java`
- `src/main/java/group1/be_mes_project/service/impl/ExecutionServiceImpl.java`
- `src/main/java/group1/be_mes_project/domain/repository/WorkOrderRepository.java`
- `src/main/java/group1/be_mes_project/domain/repository/ProductionLogRepository.java`
- `src/main/java/group1/be_mes_project/dto/execution/ExecutionLogFilterDto.java`

### 공통

- `src/main/java/group1/be_mes_project/config/MesWebSocketConfig.java`
- `src/main/java/group1/be_mes_project/dto/ApiResponse.java`

---

## 6. 요약

- Planning과 Execution은 둘 다 STOMP로 통신한다.
- 둘 다 `/ws-mes`에 연결한다.
- 요청은 `/app/...`
- 응답은 `/user/queue/...`
- JSON 계약은 `success`, `data`, `message`를 유지한다.
- `snake_case` 필드명을 사용한다.

