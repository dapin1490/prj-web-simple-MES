<script setup>
import { computed } from 'vue'
import { useAsyncRequest } from '@/composables/useAsyncRequest'
import { getProductionProgress, getWorkOrders } from '@/api/executionApi'
import { getInspections } from '@/api/qualityApi'

const {
  data: productionProgressData,
  loading: productionProgressLoading,
  error: productionProgressError,
  run: loadProductionProgress,
} = useAsyncRequest(() => getProductionProgress(), { immediate: true })

const {
  data: inspectionsData,
  loading: inspectionsLoading,
  error: inspectionsError,
  run: loadInspections,
} = useAsyncRequest(() => getInspections(), { immediate: true })

const {
  data: workOrdersData,
  loading: workOrdersLoading,
  error: workOrdersError,
  run: loadWorkOrders,
} = useAsyncRequest(() => getWorkOrders(), { immediate: true })

const workOrderList = computed(() => (Array.isArray(workOrdersData.value) ? workOrdersData.value : []))
const inspectionList = computed(() => (Array.isArray(inspectionsData.value) ? inspectionsData.value : []))

const progressPercent = computed(() => {
  const progressPayload = productionProgressData.value
  if (typeof progressPayload === 'number') {
    return progressPayload.toFixed(1)
  }

  if (progressPayload && typeof progressPayload === 'object') {
    const candidateProgressValue = progressPayload.progress ?? progressPayload.progress_rate
    if (typeof candidateProgressValue === 'number') {
      return candidateProgressValue.toFixed(1)
    }
  }

  return '-'
})

const passRatePercent = computed(() => {
  if (inspectionList.value.length === 0) {
    return '-'
  }
  const passCount = inspectionList.value.filter((inspectionRecord) => inspectionRecord.pass_fail === true).length
  return ((passCount / inspectionList.value.length) * 100).toFixed(1)
})

function getDisplayValue(recordObject, candidateKeys) {
  for (const key of candidateKeys) {
    if (key in recordObject && recordObject[key] !== null && recordObject[key] !== undefined) {
      return String(recordObject[key])
    }
  }
  return '-'
}

function refreshDashboardData() {
  void loadProductionProgress()
  void loadInspections()
  void loadWorkOrders()
}
</script>

<template>
  <main class="dashboard-view">
    <header class="dashboard-view__header">
      <h2>메인 대시보드</h2>
      <button
        type="button"
        :disabled="productionProgressLoading || inspectionsLoading || workOrdersLoading"
        @click="refreshDashboardData"
      >
        다시 조회
      </button>
    </header>

    <section class="dashboard-view__kpi-grid">
      <article class="dashboard-view__kpi-card">
        <p class="dashboard-view__kpi-label">목표 대비 실적 (Progress)</p>
        <p class="dashboard-view__kpi-value">{{ progressPercent }}%</p>
        <p v-if="productionProgressError" class="dashboard-view__error">{{ productionProgressError.message }}</p>
      </article>

      <article class="dashboard-view__kpi-card">
        <p class="dashboard-view__kpi-label">품질 합격률 (Pass Rate)</p>
        <p class="dashboard-view__kpi-value">{{ passRatePercent }}%</p>
        <p v-if="inspectionsError" class="dashboard-view__error">{{ inspectionsError.message }}</p>
      </article>

      <article class="dashboard-view__kpi-card">
        <p class="dashboard-view__kpi-label">작업 지시 수</p>
        <p class="dashboard-view__kpi-value">{{ workOrderList.length }}</p>
        <p v-if="workOrdersError" class="dashboard-view__error">{{ workOrdersError.message }}</p>
      </article>
    </section>

    <section class="dashboard-view__panel">
      <h3>현장 현황판 (설비/LOT)</h3>
      <p v-if="workOrdersLoading">작업 지시 목록 불러오는 중...</p>
      <p v-else-if="workOrdersError" class="dashboard-view__error">{{ workOrdersError.message }}</p>
      <p v-else-if="workOrderList.length === 0">표시할 작업 지시 데이터가 없습니다.</p>
      <div v-else class="dashboard-view__table-wrap">
        <table class="dashboard-view__table">
          <thead>
            <tr>
              <th>machine_id</th>
              <th>wo_id</th>
              <th>planned_qty</th>
              <th>order_id</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="workOrder in workOrderList" :key="String(workOrder.wo_id ?? workOrder.id)">
              <td>{{ getDisplayValue(workOrder, ['machine_id', 'equipment_id']) }}</td>
              <td>{{ getDisplayValue(workOrder, ['wo_id', 'id']) }}</td>
              <td>{{ getDisplayValue(workOrder, ['planned_qty', 'qty']) }}</td>
              <td>{{ getDisplayValue(workOrder, ['order_id']) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>

<style scoped>
.dashboard-view {
  display: grid;
  gap: var(--space-md);
}

.dashboard-view__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dashboard-view__kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-md);
}

.dashboard-view__kpi-card {
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-background);
}

.dashboard-view__kpi-label {
  margin: 0 0 var(--space-xs);
  font-size: var(--font-size-sm);
}

.dashboard-view__kpi-value {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
}

.dashboard-view__panel {
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-background);
}

.dashboard-view__error {
  margin-top: var(--space-xs);
  color: var(--color-status-danger);
}

.dashboard-view__table-wrap {
  overflow-x: auto;
}

.dashboard-view__table {
  width: 100%;
  border-collapse: collapse;
}

.dashboard-view__table th,
.dashboard-view__table td {
  padding: var(--space-xs);
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}
</style>
