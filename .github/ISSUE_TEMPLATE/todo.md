---
name: "[TODO] 설계 기반 구현"
about: 설계 문서와 연결된 작업 범위를 선언하고, 구현할 세부 항목을 체크리스트로 적습니다.
title: "[TODO] "
labels: todo
---

## 목적

<!-- 이 이슈에서 달성할 구현 목표를 한두 문단으로 적습니다. (폭포수 단계에서 설계가 끝난 뒤 착수하는 작업임을 명시해도 됩니다.) -->

## 참조 설계

<!--
아래 형식으로 **문서 파일 경로**와 **섹션(절)** 을 구체적으로 적습니다. 여러 개면 목록으로 나열합니다.

예시:

- SRS v2.2: `docs/functional-requirement-statement.md` ? 5.2절 실시간 공정 감시 모듈
- API: `docs/api-details.md` ? 4절 시뮬레이션 제어 API
- UI/UX: `docs/ui-ux-component-specification.md` ? (해당 화면/컴포넌트 절)
- 데이터 스키마: `docs/data-schema-definition.md` ? 2.4 공정 실행 로그
- 데이터 생성: `docs/data-generation-report.md` ? (관련 절)
- 아키텍처: `docs/system-architecture-design.md` ? (관련 절)
- 정합성: `docs/데이터 정합성 처리 지침서.md` ? (필요 시)
-->

**이 이슈의 참조 설계 (필수):**

| 문서 | 경로 | 섹션 또는 표 |
| --- | --- | --- |
| | | |
| | | |

## 작업 범위

<!-- 해당하는 항목에 체크합니다. (둘 다 걸치면 둘 다 체크) -->

- [ ] `backend/` (Java, Spring Boot, DB, 시뮬레이터, STOMP 서버 등)
- [ ] `frontend/` (Vue 3, Pinia, Axios, STOMP 클라이언트 등)

## TODO 리스트

<!-- 구현할 세부 항목을 **체크박스**로 나열합니다. PR에서는 이 항목을 그대로 옮겨 완료 시 `[x]`로 표시합니다. -->

- [ ]
- [ ]
- [ ]

## 완료의 정의

<!--
(선택사항)
병합 조건을 한 줄 이상으로 적습니다. (예: 특정 API 동작, 화면 동선, 시뮬레이터 동작 여부)
-->

## 비고

브랜치명·커밋 규칙은 `.github/WORKFLOW.md`를 따릅니다.
