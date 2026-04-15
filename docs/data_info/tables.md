'심플 MES' 프로젝트의 정합성을 최우선으로 하여, 상위 계획(수주)과 하위 실행(생산/품질) 데이터가 유기적으로 연결된 통합 데이터 스키마를 제안합니다. 에스윈텍의 주문 데이터를 수주 원천으로 삼고, 이를 의류 공정의 구체적인 작업 지시와 실행 로그로 상세화하는 구조입니다.

---

## 통합 MES 데이터 스키마 설계안

### 1. 마스터 데이터 (Master Data)

시스템의 기준이 되는 정보입니다. 에스윈텍의 제품 코드(`code`)를 의류 품목으로 재정의하여 사용합니다.

| 테이블명 | 주요 컬럼 (Fields) | 설명 | 출처 및 매핑 |
| --- | --- | --- | --- |
| Products | `product_id`(PK), `name`, `category`, `safety_stock` | 제품 마스터 정보 | 에스윈텍 `code`, `name`, `safety_stock`  |
| Machines | `machine_id`(PK), `name`, `type`, `location` | 설비 마스터 정보 | 의류 공정 `RESOURCE_CD` 기반  |

---

### 2. 생산 계획 및 수주 (Planning & Orders)

에스윈텍의 일별 주문 데이터를 기반으로 구성합니다.

| 테이블명 | 주요 컬럼 (Fields) | 설명 | 출처 및 매핑 |
| --- | --- | --- | --- |
| SalesOrders | `order_id`(PK), `product_id`(FK), `order_date`, `qty` | 고객사 수주 정보 | 에스윈텍 `date`, `day_order`  |
| WorkOrders | `wo_id`(PK), `order_id`(FK), `product_id`(FK), `planned_qty`, `status` | 생산 작업 지시서 | 의류 공정 `PRODT_ORDER_NO`  |

---

### 3. 현장 실행 및 공정 로그 (Execution & Logs)

의류 공정의 1분 주기 시계열 데이터를 작업 지시와 연결하여 실시간 진척도를 산출합니다.

| 테이블명 | 주요 컬럼 (Fields) | 설명 | 출처 및 매핑 |
| --- | --- | --- | --- |
| ProductionLogs | `log_id`(PK), `wo_id`(FK), `timestamp`, `cr_temp`, `temp_sp`, `temp_pv`, `speed` | 실시간 공정 수집 데이터 | 의류 공정 `CR_TEMP`, `TRD_TEMP_SP`, `TRD_TEMP_PV`, `TRD_SPEED1`  |
| ProcessStatus | `wo_id`(PK/FK), `current_seq`, `total_seq`, `progress_rate` | 실시간 공정 진척도 현황 | 의류 공정 `SEQ_NO` 기반 연산  |

---

### 4. 품질 및 결과 보고 (Quality & Reports)

공정 완료 후 발생한 측정값을 기반으로 표준화된 보고서를 생성합니다.

| 테이블명 | 주요 컬럼 (Fields) | 설명 | 출처 및 매핑 |
| --- | --- | --- | --- |
| Inspections | `insp_id`(PK), `wo_id`(FK), `color_de`, `pass_fail` | 최종 품질 검사 결과 | 의류 공정 `염색 색차 DE`  |

---

## 데이터 통합 및 정합성 확보 전략

1. 데이터 밀도 차이 해결: 에스윈텍 데이터는 일별(Daily) 단위이며, 의류 공정 데이터는 분별(Minute) 단위입니다. 따라서 하나의 `SalesOrder`를 여러 개의 `WorkOrder`(LOT 단위)로 분할하여 관리하는 1:N 관계를 설정하는 것이 정합성에 맞습니다.
2. ID 체계 통합: 에스윈텍의 비공개 모델명(`code`)을 의류 공정의 `product_id`로 치환하고, 의류 공정의 `LOT_NO`를 MES 내의 `wo_id`로 사용하여 물리적 데이터를 연결합니다.
3. 실시간 진척도 계산: `WorkOrders`의 목표 수량 대비 `ProductionLogs`에 기록된 가장 최신의 `SEQ_NO` 비중을 계산하여 실시간 대시보드 기능을 구현합니다.