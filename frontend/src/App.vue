<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { resetSimulation, startSimulation, stopSimulation } from '@/api/simulationApi'
import { StompClientError } from '@/api/httpClient'
import { useProductionTrendSocket } from '@/composables/useProductionTrendSocket'

const menuItems = [
  { to: '/dashboard', label: '대시보드' },
  { to: '/orders', label: '수주 및 생산 계획' },
  { to: '/monitoring', label: '실시간 공정 모니터링' },
  { to: '/quality', label: '품질 검사 및 리포트' },
]

const userRoleLabel = ref('Admin')
const simulationStatusLabel = ref('정지')
const simulationActionInProgress = ref(false)
const simulationStatusMessage = ref('')
const simulationStatusTone = ref('normal')
const themeMode = ref('light')
const THEME_STORAGE_KEY = 'mes-theme-mode'

const isSimulationRunning = computed(() => simulationStatusLabel.value === '가동 중')
const {
  connectionState: wsConnectionState,
  connect: connectProductionTrendSocket,
} = useProductionTrendSocket()

const wsConnectionStatusLabel = computed(() => {
  if (wsConnectionState.value === 'connecting') {
    return '연결 중'
  }
  if (wsConnectionState.value === 'connected') {
    return '연결됨'
  }
  if (wsConnectionState.value === 'reconnecting') {
    return '재연결 중'
  }
  return '끊김'
})

const wsConnectionStatusClassName = computed(() => {
  if (wsConnectionState.value === 'connected') {
    return 'app-header__status-value--running'
  }
  if (wsConnectionState.value === 'connecting' || wsConnectionState.value === 'reconnecting') {
    return 'app-header__status-value--warning'
  }
  return 'app-header__status-value--stopped'
})

onMounted(() => {
  const savedThemeMode = localStorage.getItem(THEME_STORAGE_KEY)
  if (savedThemeMode === 'light' || savedThemeMode === 'dark') {
    themeMode.value = savedThemeMode
  } else {
    themeMode.value = 'light'
  }
  document.documentElement.setAttribute('data-theme', themeMode.value)
  connectProductionTrendSocket()
})

const themeToggleLabel = computed(() => (themeMode.value === 'light' ? '다크 테마' : '라이트 테마'))

function toggleThemeMode() {
  themeMode.value = themeMode.value === 'light' ? 'dark' : 'light'
  document.documentElement.setAttribute('data-theme', themeMode.value)
  localStorage.setItem(THEME_STORAGE_KEY, themeMode.value)
}

function setSimulationStatusMessage(message, tone = 'normal') {
  simulationStatusMessage.value = message
  simulationStatusTone.value = tone
}

async function runSimulationAction(actionType) {
  if (simulationActionInProgress.value) {
    return
  }

  simulationActionInProgress.value = true
  setSimulationStatusMessage('')

  try {
    if (actionType === 'start') {
      await startSimulation()
      simulationStatusLabel.value = '가동 중'
      setSimulationStatusMessage('시뮬레이션 시작 요청을 완료했습니다.', 'normal')
      return
    }

    if (actionType === 'stop') {
      await stopSimulation()
      simulationStatusLabel.value = '정지'
      setSimulationStatusMessage('시뮬레이션 정지 요청을 완료했습니다.', 'warning')
      return
    }

    await resetSimulation()
    simulationStatusLabel.value = '초기화됨'
    setSimulationStatusMessage('시뮬레이션 초기화 요청을 완료했습니다.', 'warning')
  } catch (error) {
    const statusMessage =
      error instanceof StompClientError
        ? error.message
        : '시뮬레이션 제어 요청 중 오류가 발생했습니다.'
    setSimulationStatusMessage(statusMessage, 'danger')
  } finally {
    simulationActionInProgress.value = false
  }
}
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
        <div class="app-header__status">
          <p class="app-header__item"><strong>권한</strong>: {{ userRoleLabel }}</p>
          <p class="app-header__item">
            <strong>연결 상태</strong>:
            <span :class="['app-header__status-value', wsConnectionStatusClassName]">
              {{ wsConnectionStatusLabel }}
            </span>
          </p>
          <p class="app-header__item">
            <strong>시뮬레이션</strong>:
            <span
              :class="[
                'app-header__status-value',
                isSimulationRunning ? 'app-header__status-value--running' : 'app-header__status-value--stopped',
              ]"
            >
              {{ simulationStatusLabel }}
            </span>
          </p>
          <p
            v-if="simulationStatusMessage !== ''"
            :class="[
              'app-header__feedback',
              simulationStatusTone === 'danger'
                ? 'app-header__feedback--danger'
                : simulationStatusTone === 'warning'
                  ? 'app-header__feedback--warning'
                  : 'app-header__feedback--normal',
            ]"
          >
            {{ simulationStatusMessage }}
          </p>
        </div>

        <div class="app-header__actions">
          <button type="button" class="app-header__button" @click="toggleThemeMode">
            {{ themeToggleLabel }}
          </button>
          <button
            type="button"
            class="app-header__button"
            :disabled="simulationActionInProgress || isSimulationRunning"
            @click="runSimulationAction('start')"
          >
            시작
          </button>
          <button
            type="button"
            class="app-header__button"
            :disabled="simulationActionInProgress || !isSimulationRunning"
            @click="runSimulationAction('stop')"
          >
            정지
          </button>
          <button
            type="button"
            class="app-header__button"
            :disabled="simulationActionInProgress"
            @click="runSimulationAction('reset')"
          >
            초기화
          </button>
        </div>
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
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-md);
  padding: var(--space-md);
  border-bottom: 1px solid var(--color-border);
  background-color: var(--color-background);
}

.app-header__status {
  display: grid;
  gap: var(--space-2xs);
}

.app-header__item {
  margin: 0;
  font-size: var(--font-size-sm);
}

.app-header__status-value {
  font-weight: 700;
}

.app-header__status-value--running {
  color: var(--color-status-normal);
}

.app-header__status-value--warning {
  color: var(--color-status-warning);
}

.app-header__status-value--stopped {
  color: var(--color-status-warning);
}

.app-header__feedback {
  margin: 0;
  font-size: var(--font-size-sm);
}

.app-header__feedback--normal {
  color: var(--color-status-normal);
}

.app-header__feedback--warning {
  color: var(--color-status-warning);
}

.app-header__feedback--danger {
  color: var(--color-status-danger);
}

.app-header__actions {
  display: flex;
  gap: var(--space-xs);
}

.app-header__button {
  padding: var(--space-2xs) var(--space-sm);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background-color: var(--color-background);
  color: var(--color-text);
  cursor: pointer;
}

.app-header__button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

  .app-header__actions {
    width: 100%;
  }
}
</style>
