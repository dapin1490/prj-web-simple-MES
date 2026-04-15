# 심플 MES (Simplified Manufacturing Execution System)

![License](https://img.shields.io/badge/license-Project-blue)
![Backend](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)
![Frontend](https://img.shields.io/badge/Vue.js-3.x-4FC08D?logo=vuedotjs&logoColor=white)
![DB](https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql&logoColor=white)

에스윈텍 주문 데이터와 의류 염색 공정 실측 데이터를 통합해 구축한 **실무형 MES** 저장소입니다. 현장에서 흔히 기대하는 “실시간 원천 데이터 가공”이 아니라, **사전 전처리된 정합 데이터셋**을 바탕으로 **시뮬레이션(스케줄 적재, WebSocket 스트리밍)** 으로 운영 화면을 재현하는 것을 핵심 아키텍처로 둡니다.

| 문서 | 경로 |
| --- | --- |
| 신규 팀원 온보딩 | `docs/onboarding-guide.md` |
| SRS | `docs/functional-requirement-statement.md` |
| 아키텍처 | `docs/system-architecture-design.md` |
| 데이터 생성 | `docs/data-generation-report.md` |
| API | `docs/api-details.md` |
| UI/UX | `docs/ui-ux-component-specification.md` |
| 스키마 | `docs/data-schema-definition.md` |
| 정합성 지침 | `docs/데이터 정합성 처리 지침서.md` |

---

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| **Backend** | Java 17, Spring Boot 3.x, MySQL 8.x |
| **Frontend** | Vue.js 3.x, Pinia, Axios |
| **통신** | REST API (조회·제어), **STOMP WebSocket** (`/ws-mes`, 토픽 `/topic/production-trend`, 선택 `/topic/equipment-alert`는 `docs/api-details.md` §6.1) |

실시간 공정 트렌드는 REST 폴링이 아니라 **STOMP 기반 WebSocket**으로 전달하는 것을 전제로 합니다. 상세는 `docs/api-details.md`를 따릅니다.

---

## 주요 기능

| 영역 | 내용 |
| --- | --- |
| **수주 및 계획** | 재고·안전재고 개념과 연계한 수주 잔량 관리, 작업 지시(Work Order) 발행 및 계획 연동 |
| **실시간 모니터링** | 1분 주기 공정 트렌드(온도, 속도 등) **시뮬레이션** 및 **WebSocket 스트리밍**으로 대시보드 갱신 |
| **품질 및 리포트** | 염색 색차 **DE** 기반 자동 판정, 로트·공정 데이터를 묶은 **표준 생산 성적서** 출력 |

---

## 프로젝트 구조 (모노레포)

단일 Git 저장소에서 백엔드와 프론트엔드를 분리해 관리하는 것을 목표로 합니다. 현재는 Spring Boot 백엔드가 `be-mes-project/`에 있으며, Vue 프론트엔드 루트 디렉터리는 도입 시 추가합니다.

```
.
├── be-mes-project/   # Spring Boot API (Gradle), 스케줄러·STOMP·DB 연동 예정
├── docs/             # SRS, 스키마, API, 정합성 지침 등 설계 Single Source of Truth
├── scripts/          # 데이터 탐색·전처리 스크립트
├── .github/          # Copilot 등 저장소 수준 지시문
├── .cursorrules      # Cursor AI 가이드
├── COLLABORATION.md  # 팀 협업 프로토콜
└── README.md
```

협업 규칙은 루트의 `COLLABORATION.md`를 참고합니다.

---

## 시작 가이드

### 1) 데이터 적재 준비

`docs/data-generation-report.md`에 따라 **전처리가 끝난 CSV 또는 XLSX** 산출물을 준비합니다. Spring Boot 관례에 맞게 예를 들면 다음 위치에 두고 애플리케이션에서 classpath 또는 설정된 경로로 읽습니다.

| 단계 | 안내 |
| --- | --- |
| 산출물 확인 | `scripts/data_preprocess.py` 기준 출력 디렉터리 `data/backend_ready/`의 CSV(예: `ProductionLogs.csv`, `WorkOrders.csv` 등). 통합 입력은 `data/`의 `integrated_manufacture_data.csv`·`MES_customer_order.csv`를 전제로 한다. |
| 배치 위치 예시 | `be-mes-project/src/main/resources/data/` 등 (팀에서 정한 경로와 `application.properties` 또는 `application.yml`의 리소스 설정과 일치시킴) |
| 주의 | 런타임에 원천 데이터를 다시 전처리하지 않고, **이미 정제된 파일을 순차 적재**하는 시뮬레이터에 맞춤 |

파일명·필드 매핑은 `docs/data-schema-definition.md`, `docs/data-generation-report.md` §2.3을 함께 확인합니다.

### 2) 백엔드 실행 (Gradle)

`be-mes-project` 디렉터리에서:

```bash
cd be-mes-project
./gradlew bootRun
```

Windows에서는 `gradlew.bat`을 사용합니다. 빌드만 할 때는 `./gradlew build`입니다. JDK 버전은 `be-mes-project/build.gradle`의 toolchain을 따릅니다.

### 3) 프론트엔드 실행 (npm)

프론트엔드 앱이 저장소에 추가되면 해당 디렉터리에서 `npm install` 및 `npm run dev`를 실행한다. API Base URL과 WebSocket 엔드포인트는 `docs/api-details.md`의 로컬 기준값(`http://localhost:8080/api/v1`, `/ws-mes`)에 맞춰 환경 변수 또는 프록시를 설정한다.

---

## 데이터 정합성 규칙 (요약)

아래는 `docs/데이터 정합성 처리 지침서.md`의 핵심만 옮긴 것입니다. 구현·검증 시 해당 문서를 기준으로 합니다.

| 항목 | 내용 |
| --- | --- |
| **기간** | 2022년 1월 ~ 10월, 의류 공정 실측이 있는 **약 231일** 범위 |
| **수량 동기화 (Overwrite)** | 의류 공정을 일자별로 묶어 `염색 가동 길이` 합계를 산출하고, 그 값을 에스윈텍 주문량(`day_order`)에 **덮어씀**으로써 주문·계획·실적 수치를 맞춤 |
| **날짜 정렬** | 에스윈텍 주문일을 의류 실측일(`INSRT_DT` 등)에 맞게 치환하고, 실측이 없는 기간의 주문은 제거해 정합성 유지 |
| **제품 정보** | 비공개 `code`를 의류 `JOB_CD` 등으로 치환하고, 제품명은 공정 특성에 맞는 가상 명칭으로 부여. 분류는 에스윈텍 `hierarchy` 유지 |
| **품질 연계** | 전체 로트 중 측정값이 있는 로트(지침서 기준 434개 로트)에 `염색 색차 DE` 연결. 스키마의 `pass_fail`은 **DE < 1.0**을 합격, **1.0 이상**을 불합격으로 정의함 (`docs/data-schema-definition.md`와 동일) |
| **검증** | 수주 총량과 작업 지시 계획량 합의 일치, 주문 시각과 로그 시각의 선후 관계, `ProductionLogs`가 유효한 `wo_id`만 참조. 로그 행은 스키마에 정의된 `cr_temp`, `temp_sp`, `temp_pv`, `speed` 등을 포함한다. |

---

## 라이선스 및 문의

프로젝트 라이선스와 담당자 정보는 팀 정책에 맞게 이 섹션을 채웁니다.
