심플 MES(Simplified MES) 프로젝트의 시스템 아키텍처 설계서입니다. 본 문서는 Java Spring Boot, Vue.js, MySQL 기술 스택을 기반으로, 정적 제조 데이터를 실시간 스트리밍 데이터로 변환하여 처리하는 논리적·물리적 구조를 정의합니다.

## 심플 MES 시스템 아키텍처 설계서 (v1.0)

### 1. 설계 개요

- 목적: 정적 CSV/XLSX 원천 데이터를 기반으로 실제 가동 중인 제조 현장과 유사한 실시간 데이터 흐름을 재현하고 관리함.
- 기본 원칙:
    - 관심사 분리: 프론트엔드(UI), 백엔드(Business Logic), 데이터베이스(Storage)의 역할을 명확히 분리함.
    - 실시간성 확보: 스케줄러와 웹소켓을 활용하여 새로고침 없는 데이터 갱신을 구현함.
    - 데이터 무결성: 정합성 지침에 따른 수치 동기화 로직을 백엔드에서 강제함.

### 2. 논리 아키텍처 (Layered Architecture)

### 2.1. Presentation Layer (Vue.js)

- Dashboard Component: 실시간 공정 트렌드(온도, 속도) 및 KPI(진척도, 품질 합격률) 시각화.
- Management UI: 수주 정보 확인, 작업 지시 발행 및 생산 이력 리포트 조회.
- WebSocket Client: STOMP 프로토콜을 통해 백엔드에서 푸시되는 실시간 로그 수신.

### 2.2. Service Layer (Spring Boot)

- Simulation Engine (Scheduler): 정해진 주기에 따라 정적 파일 데이터를 읽어 MySQL에 적재하고 이벤트를 발생시킴.
- Order & Production Service: 수주 데이터를 공정 데이터와 동기화하고 작업 지시를 관리하는 비즈니스 로직.
- Quality & Report Service: 품질 판정 로직 수행 및 최종 성적서 데이터 가공.

### 2.3. Persistence Layer (MySQL)

- RDBMS: 정규화된 스키마를 통해 제품 마스터, 수주, 작업 지시, 공정 로그, 품질 데이터를 관리.

### 3. 데이터 흐름 설계 (Data Flow)

1. 초기 적재 (Initial Loading): 에스윈텍(수주) 및 의류 공정(마스터) 데이터를 정합성 지침에 따라 전처리하여 MySQL 마스터 테이블에 저장함.
2. 생산 시뮬레이션 (Simulation Flow):
    - Spring Boot 스케줄러가 1분(가상 시간)마다 의류 공정 트렌드 파일의 레코드를 읽음.
    - 해당 데이터를 `ProductionLogs` 테이블에 Insert함.
    - 동시에 WebSocket을 통해 현재 생성된 로그를 프론트엔드로 전송함.
3. UI 갱신 (Real-time Update): Vue.js 대시보드가 수신된 데이터를 바탕으로 실시간 차트와 진척률($P$)을 즉각 업데이트함.

### 4. 핵심 컴포넌트 상세 설계

### 4.1. 시뮬레이션 엔진 (Simulator)

- 기술: Spring `@Scheduled` 및 `TaskScheduler`.
- 로직:
    - 파일 포인터를 유지하며 한 줄씩 읽어 실행 중인 `WorkOrder`와 매핑.
    - 실제 시간 대비 배속(Scaling) 기능을 제공하여 데모 속도 조절 가능.

### 4.2. 실시간 통신 (Messaging)

- 프로토콜: WebSocket / STOMP.
- Topic 구조: `/topic/production-trend` (`docs/api-details.md`와 동일. 페이로드에 `wo_id` 등이 포함됨).

### 5. 인프라 아키텍처

- Application Server: Embedded Tomcat (Spring Boot).
- Database Server: MySQL 8.x.
- Client: Web Browser (Chrome 최적화).