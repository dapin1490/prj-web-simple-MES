<script setup>
import { RouterLink, RouterView } from 'vue-router'

const menuItems = [
  { to: '/dashboard', label: '대시보드' },
  { to: '/orders', label: '수주 및 생산 계획' },
  { to: '/monitoring', label: '실시간 공정 모니터링' },
  { to: '/quality', label: '품질 검사 및 리포트' },
]
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <h1 class="app-title">심플 MES 프론트엔드</h1>
      <nav class="app-nav" aria-label="주요 메뉴">
        <RouterLink
          v-for="menuItem in menuItems"
          :key="menuItem.to"
          :to="menuItem.to"
          class="app-nav__link"
        >
          {{ menuItem.label }}
        </RouterLink>
      </nav>
    </header>

    <RouterView />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  padding: 1rem;
  border-bottom: 1px solid var(--color-border, #e0e0e0);
}

.app-title {
  margin: 0 0 0.75rem;
  font-size: 1.125rem;
}

.app-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.app-nav__link {
  display: inline-block;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border, #d0d0d0);
  border-radius: 6px;
  text-decoration: none;
  color: inherit;
}

.app-nav__link.router-link-exact-active {
  font-weight: 700;
  border-color: #42b883;
}
</style>
