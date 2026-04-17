/**
 * 로컬에서만: `.env.development.local` 등에 `VITE_USE_DEV_FIXTURES=true` 로 두면
 * STOMP 없이 픽스처 데이터로 화면을 돌린다. (저장소 `.gitignore`의 `.env*` 로 커밋 제외)
 */
export function isDevFixturesMode() {
  return import.meta.env.VITE_USE_DEV_FIXTURES === 'true'
}
