# TODO → Branch → Commit → PR → DONE

심플 MES 저장소는 설계 문서(SRS v2.2, API 명세, 데이터 스키마 등)를 먼저 확정한 뒤 구현하는 **폭포수** 흐름을 전제로 한다. 아래 순서를 따른다.

## 1. TODO (이슈)

1. GitHub에서 **New issue** → **`[TODO] 설계 기반 구현`** 템플릿을 선택한다.
2. **참조 설계**에 문서 경로와 절 번호를 채운다.
3. **작업 범위**로 `backend/` / `frontend/` 를 구분한다.
4. **TODO 리스트**를 체크박스로 구체적으로 쓴다.
5. 이슈 번호를 기록해 둔다. (브랜치·커밋·PR에 사용)

## 2. Branch

로컬에서 이슈 번호를 포함한 브랜치를 만든다.

| 유형 | 패턴 | 예시 |
| --- | --- | --- |
| 기능 | `feature/<이슈번호>-<짧은-기능명>` | `feature/12-simulation-start-api` |
| 수정 | `fix/<이슈번호>-<짧은-설명>` | `fix/14-order-list-null-guard` |

- `기능명`은 영어 케밥 케이스를 권장한다.
- 한 이슈에 여러 사람이 나누어 쓸 때는 팀 규칙에 따라 브랜치를 쪼개거나 이슈를 쪼근다.

## 3. Commit

Conventional Commits 형태에 **영역 태그**와 **이슈 번호**를 넣는다.

형식:

```text
<태그>(<영역>): <한 줄 설명> #<이슈번호>
```

| 항목 | 설명 |
| --- | --- |
| 태그 | `feat`, `fix`, `docs`, `chore`, `refactor` 중 하나, 공통이면 우선하는 것 하나만 작성 |
| 영역 | `be` = backend, `fe` = frontend, `data` = data, 공통이면 우선하는 것 하나만 작성 |

예시:

```text
feat(be): 시뮬레이션 시작 API 구현 #12
fix(fe): 수주 목록 로딩 오류 수정 #14
```

## 4. PR

1. `.github/pull_request_template.md`에 맞춰 본문을 채운다.
2. **Closes #이슈번호** 를 반드시 넣어 머지 시 이슈가 닫히게 한다.
3. **DONE 리포트**에 이슈의 TODO를 복사해 완료 항목을 `[x]`로 표시한다.
4. **정합성 체크리스트**와 **테스트 증빙**을 채운다.

## 5. DONE

- PR이 기본 브랜치에 머지되고, 연결된 이슈가 닫히면 해당 작업은 DONE으로 본다.
- CI·리뷰 규칙은 팀의 브랜치 보호 설정을 따른다.

## 관련 파일

| 파일 | 용도 |
| --- | --- |
| `.github/ISSUE_TEMPLATE/todo.md` | TODO 이슈 템플릿 |
| `.github/pull_request_template.md` | PR·DONE 템플릿 |
| `COLLABORATION.md` | 모노레포 협업 요약 |
