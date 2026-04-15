# 신규 팀원 온보딩 가이드

이 문서는 저장소에 합류한 팀원이 **문서를 어떤 순서로 읽으면** 프로젝트 맥락과 규범을 빠르게 잡을 수 있는지 정리한 것입니다. 설계와 데이터 정의의 기준은 루트 `docs/`이며, 상세는 `COLLABORATION.md`의 Single Source of Truth 표와 같습니다.

---

## 1. 읽기 순서 (권장)

아래는 **한 번에 끝내기보다**, 단계마다 메모하며 진행하는 것을 권장합니다.

### 1단계: 저장소 개요와 협업 규칙

| 순서 | 문서 | 읽는 이유 |
| --- | --- | --- |
| 1 | [`README.md`](../README.md) | 프로젝트 목적, 기술 스택, 문서 인덱스, 로컬 실행 흐름의 큰 그림 |
| 2 | [`COLLABORATION.md`](../COLLABORATION.md) | `docs/`를 최종 기준으로 두는 이유, 모노레포 책임 구분, API와 WebSocket 규약, PR 시 문서 연동 |

이후 작업은 **명세와 충돌하면 명세를 먼저 갱신한 뒤 구현**한다는 전제를 공유합니다.

### 2단계: 무엇을 만들고, 데이터는 어디서 오는가

| 순서 | 문서 | 읽는 이유 |
| --- | --- | --- |
| 3 | [`functional-requirement-statement.md`](functional-requirement-statement.md) | 기능 범위, 모듈(수주·실행·품질), 시뮬레이터 전제 등 SRS 수준의 요구 |
| 4 | [`데이터 정합성 처리 지침서.md`](데이터%20정합성%20처리%20지침서.md) | 에스윈텍 주문과 의류 공정 데이터를 맞추는 규칙(기간, 수량 및 날짜 동기화, 품질 연계). **도메인 숫자와 일자가 왜 이렇게 보이는지** 설명 |
| 5 | [`data-generation-report.md`](data-generation-report.md) | 전처리 산출물, 파일명, 검증 전제. **백엔드가 로드할 입력**이 무엇인지 |

### 3단계: DB와 필드 (구현 시 자주 참조)

| 순서 | 문서 | 읽는 이유 |
| --- | --- | --- |
| 6 | [`data-schema-definition.md`](data-schema-definition.md) | 테이블, 컬럼, 출처 매핑, ER 다이어그램. 엔티티와 API 필드명의 기준 |
| 7 | (선택) [`data_info/tables.md`](data_info/tables.md) | 원천 데이터셋 쪽 테이블 요약이 필요할 때 보조 참고 |

### 4단계: 구조, API, 실시간

| 순서 | 문서 | 읽는 이유 |
| --- | --- | --- |
| 8 | [`system-architecture-design.md`](system-architecture-design.md) | 레이어, 데이터 흐름, 시뮬레이터와 WebSocket 역할 |
| 9 | [`api-details.md`](api-details.md) | REST Base URL, 응답 형식, 엔드포인트 목록, STOMP 엔드포인트와 토픽 |

### 5단계: 프론트엔드 담당자만

| 순서 | 문서 | 읽는 이유 |
| --- | --- | --- |
| 10 | [`ui-ux-component-specification.md`](ui-ux-component-specification.md) | 화면 구조, 컴포넌트, API 명세와의 연결 |

백엔드만 담당하면 10번은 나중에 읽어도 됩니다.

---

## 2. 역할별 최소 경로

팀 일정에 맞춰 **아래만 먼저** 읽고 들어가도 됩니다.

| 역할 | 최소 순서 (문서 번호는 위 표의 순서와 동일) |
| --- | --- |
| **백엔드** | 1 → 2 → 3 → 4 → 5 → 6 → 8 → 9 |
| **프론트엔드** | 1 → 2 → 3 → 8 → 9 → 10 (`docs/ui-ux-component-specification.md`). 스키마는 6번을 훑어두면 API 필드 이해에 도움이 됨 |
| **데이터·전처리** | 1 → 2 → 4 → 5 → 6 → (필요 시 7) |
| **기획·문서 정리** | 1 → 2 → 3 → 4 → 5 |

---

## 3. 보조 참고 (필수는 아님)

| 위치 | 용도 |
| --- | --- |
| [`.cursorrules`](../.cursorrules) | Cursor 사용 시 AI가 따를 저장소 수준 규칙 |
| [`.github/WORKFLOW.md`](../.github/WORKFLOW.md) | 브랜치·이슈·PR 워크플로 요약 |
| [`.github/copilot-instructions.md`](../.github/copilot-instructions.md) | Copilot용 기술·문서 우선순위 |
| `docs/ref/` | 에스윈텍·의류 공정 원천 데이터 가이드 PDF (배경 이해용) |
| `scripts/` | 데이터 탐색·통합 전처리 스크립트 (파이프라인 파악용) |

---

## 4. 온보딩 체크리스트 (선택)

작업 착수 전에 스스로 확인할 수 있는 질문입니다.

- [ ] 이 시스템이 런타임에 원천 CSV를 새로 전처리하지 않는 이유를 말할 수 있다.
- [ ] `Products`, `SalesOrders`, `WorkOrders`, `ProductionLogs`, `Inspections`의 역할과 대략적인 관계를 말할 수 있다.
- [ ] REST Base URL과 WebSocket 엔드포인트, 구독 토픽을 `api-details.md`에서 찾을 수 있다.
- [ ] 스키마나 API를 바꿀 때 어떤 문서를 함께 수정해야 하는지 안다 (`COLLABORATION.md`).
- [ ] `ProductionLogs`의 온도·속도 필드(`cr_temp`, `temp_sp`, `temp_pv`, `speed`)가 스키마 §2.4와 전처리 매핑(`data-generation-report.md` §2.3)에서 어떻게 정의되는지 안다.

---

## 5. 문서 중복에 대해

`be-mes-project/docs/` 아래에 동일 제목의 파일이 있을 수 있습니다. **루트 `docs/`를 우선**하고, 팀에서 하나로 맞추는 중이라면 리뷰 시 최신 쪽을 확인합니다.
