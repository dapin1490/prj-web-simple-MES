<script setup>
import { computed, ref } from 'vue'
import { useAsyncRequest } from '@/composables/useAsyncRequest'
import { getInspections, getReportByWorkOrderId } from '@/api/qualityApi'

const selectedWorkOrderId = ref('')

const {
  data: inspectionsData,
  loading: inspectionsLoading,
  error: inspectionsError,
  run: loadInspections,
} = useAsyncRequest(() => getInspections(), { immediate: true })

const {
  data: reportData,
  loading: reportLoading,
  error: reportError,
  run: loadReport,
  reset: resetReport,
} = useAsyncRequest(
  () => {
    if (selectedWorkOrderId.value === '') {
      return null
    }
    return getReportByWorkOrderId(selectedWorkOrderId.value)
  },
  { immediate: false },
)

const inspectionList = computed(() =>
  Array.isArray(inspectionsData.value) ? inspectionsData.value : [],
)

const selectableWorkOrderList = computed(() => {
  const workOrderIdSet = new Set()
  for (const inspectionRecord of inspectionList.value) {
    if (inspectionRecord.wo_id !== null && inspectionRecord.wo_id !== undefined) {
      workOrderIdSet.add(String(inspectionRecord.wo_id))
    }
  }
  return Array.from(workOrderIdSet)
})

function getFirstDefinedValue(recordObject, candidateKeys) {
  for (const key of candidateKeys) {
    if (key in recordObject && recordObject[key] !== null && recordObject[key] !== undefined) {
      return String(recordObject[key])
    }
  }
  return '-'
}

function getPassFailLabel(inspectionRecord) {
  if (inspectionRecord.pass_fail === true) {
    return 'PASS'
  }
  if (inspectionRecord.pass_fail === false) {
    return 'FAIL'
  }
  return '-'
}

function getPassFailClass(inspectionRecord) {
  if (inspectionRecord.pass_fail === true) {
    return 'feature-view__result--pass'
  }
  if (inspectionRecord.pass_fail === false) {
    return 'feature-view__result--fail'
  }
  return ''
}

function onWorkOrderChange() {
  if (selectedWorkOrderId.value === '') {
    resetReport()
    return
  }
  void loadReport()
}

function refreshQualityData() {
  void loadInspections()
  if (selectedWorkOrderId.value !== '') {
    void loadReport()
  }
}
</script>

<template>
  <main class="feature-view">
    <header class="feature-view__header">
      <h2>품질 검사 및 리포트</h2>
      <button type="button" :disabled="inspectionsLoading || reportLoading" @click="refreshQualityData">
        다시 조회
      </button>
    </header>

    <section class="feature-view__panel">
      <h3>품질 검사 결과 목록</h3>
      <p v-if="inspectionsLoading">품질 검사 결과 불러오는 중...</p>
      <p v-else-if="inspectionsError" class="feature-view__error">{{ inspectionsError.message }}</p>
      <p v-else-if="inspectionList.length === 0">조회된 품질 검사 결과가 없습니다.</p>
      <div v-else class="feature-view__table-wrap">
        <table class="feature-view__table">
          <thead>
            <tr>
              <th>insp_id</th>
              <th>wo_id</th>
              <th>color_de</th>
              <th>pass_fail</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="inspectionRecord in inspectionList"
              :key="String(inspectionRecord.insp_id ?? inspectionRecord.id)"
            >
              <td>{{ getFirstDefinedValue(inspectionRecord, ['insp_id', 'id']) }}</td>
              <td>{{ getFirstDefinedValue(inspectionRecord, ['wo_id']) }}</td>
              <td>{{ getFirstDefinedValue(inspectionRecord, ['color_de']) }}</td>
              <td :class="getPassFailClass(inspectionRecord)">
                {{ getPassFailLabel(inspectionRecord) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="feature-view__panel">
      <h3>공정 보고서 조회</h3>
      <label class="feature-view__label" for="report-work-order-filter">작업 지시(wo_id) 선택</label>
      <select
        id="report-work-order-filter"
        v-model="selectedWorkOrderId"
        :disabled="inspectionsLoading || selectableWorkOrderList.length === 0"
        @change="onWorkOrderChange"
      >
        <option value="">작업 지시 선택</option>
        <option v-for="workOrderId in selectableWorkOrderList" :key="workOrderId" :value="workOrderId">
          {{ workOrderId }}
        </option>
      </select>

      <p v-if="reportLoading">공정 보고서 불러오는 중...</p>
      <p v-else-if="reportError" class="feature-view__error">{{ reportError.message }}</p>
      <p v-else-if="selectedWorkOrderId === ''">작업 지시를 선택하면 보고서를 조회합니다.</p>
      <pre v-else-if="reportData !== null" class="feature-view__json">{{ JSON.stringify(reportData, null, 2) }}</pre>
      <p v-else>보고서 데이터가 없습니다.</p>
    </section>
  </main>
</template>

<style scoped>
.feature-view {
  display: grid;
  gap: var(--space-md);
}

.feature-view__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.feature-view__panel {
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-background);
}

.feature-view__label {
  display: block;
  margin-bottom: var(--space-xs);
}

.feature-view__error {
  color: var(--color-status-danger);
}

.feature-view__table-wrap {
  overflow-x: auto;
}

.feature-view__table {
  width: 100%;
  border-collapse: collapse;
}

.feature-view__table th,
.feature-view__table td {
  padding: var(--space-xs);
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}

.feature-view__result--pass {
  color: var(--color-status-normal);
  font-weight: 700;
}

.feature-view__result--fail {
  color: var(--color-status-danger);
  font-weight: 700;
}

.feature-view__json {
  margin-top: var(--space-xs);
  padding: var(--space-sm);
  border-radius: var(--radius-sm);
  background: var(--color-background-soft);
}
</style>
