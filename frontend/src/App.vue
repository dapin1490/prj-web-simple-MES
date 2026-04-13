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
    <aside class="app-sidebar">
      <h1 class="app-title">심플 MES</h1>
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
    </aside>

    <section class="app-main">
      <header class="app-header">
        <p class="app-header__item"><strong>권한</strong>: Admin</p>
        <p class="app-header__item"><strong>시뮬레이션</strong>: 정지</p>
      </header>

      <main class="app-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--layout-sidebar-width) 1fr;
  background-color: var(--color-background-soft);
}

.app-sidebar {
  padding: var(--space-md);
  border-right: 1px solid var(--color-border);
  background-color: var(--color-background);
}

.app-main {
  display: grid;
  grid-template-rows: auto 1fr;
}

.app-header {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-md);
  padding: var(--space-md);
  border-bottom: 1px solid var(--color-border);
  background-color: var(--color-background);
}

.app-header__item {
  margin: 0;
  font-size: var(--font-size-sm);
}

.app-title {
  margin: 0 0 var(--space-sm);
  font-size: var(--font-size-lg);
}

.app-nav {
  display: grid;
  gap: var(--space-xs);
}

.app-nav__link {
  display: block;
  padding: var(--space-2xs) var(--space-sm);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  text-decoration: none;
  color: inherit;
  background-color: var(--color-background);
}

.app-nav__link.router-link-exact-active {
  font-weight: 700;
  border-color: var(--color-brand-primary);
  background-color: var(--color-brand-primary-soft);
  color: var(--color-brand-primary-contrast);
}

.app-content {
  padding: var(--space-md);
}

@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }

  .app-sidebar {
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .app-nav {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }

  .app-header {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
