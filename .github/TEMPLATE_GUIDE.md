# 이슈·PR 템플릿 배치 가이드

이 저장소는 GitHub 기본 경로에 템플릿을 두었다. **설계 기반 구현**은 `[TODO] 설계 기반 구현` 템플릿(파일 `todo.md`)과 PR 템플릿으로 **TODO → DONE** 흐름을 맞춘다. 상세 절차는 `.github/WORKFLOW.md`를 본다.

## GitHub

| 용도 | 경로 |
| --- | --- |
| 이슈 템플릿 | `.github/ISSUE_TEMPLATE/todo.md` |
| 이슈 템플릿 선택기 설정 | `.github/ISSUE_TEMPLATE/config.yml` |
| Pull Request 템플릿 | `.github/pull_request_template.md` 또는 `.github/PULL_REQUEST_TEMPLATE.md` (대소문자 무관) |
| 워크플로우 가이드 | `.github/WORKFLOW.md` |

저장소 설정에서 **Issues**가 켜져 있어야 템플릿이 보인다.

### 라벨

`todo.md`의 frontmatter에 `labels: todo`가 있으면, 저장소에 **`todo`** 라벨을 만들거나 frontmatter의 `labels` 줄을 팀 라벨에 맞게 수정한다.

## GitLab

GitLab은 기본 폴더명이 다르다. 동일 내용을 아래에 맞게 복사하면 된다.

| 용도 | GitLab 경로 |
| --- | --- |
| 이슈 템플릿 | `.gitlab/issue_templates/` |
| Merge Request 템플릿 | `.gitlab/merge_request_templates/` |

### 이슈

1. `.github/ISSUE_TEMPLATE/todo.md`를 `.gitlab/issue_templates/TODO_설계_기반_구현.md` 등으로 복사한다.
2. GitHub 전용 YAML frontmatter(`---` 블록)는 GitLab에서 그대로 본문에 노출될 수 있다. GitLab만 쓸 때는 frontmatter를 제거하거나 GitLab 이슈 템플릿 형식에 맞게 정리한다.

### Merge Request

1. `.github/pull_request_template.md` 내용을 `.gitlab/merge_request_templates/default.md` (또는 팀이 정한 이름)로 복사한다.
2. 제목의 "Pull Request"를 "Merge Request"로 바꿔도 된다.

## 한 저장소에 GitHub과 GitLab 둘 다 유지

원격이 하나라면 팀이 쓰는 플랫폼에 맞는 폴더만 유지하고, 나머지는 이 가이드로 동기화한다. 둘 다 쓰는 경우 `.github/`와 `.gitlab/`에 각각 맞는 사본을 두고, 내용 변경 시 두 곳을 함께 고친다.
