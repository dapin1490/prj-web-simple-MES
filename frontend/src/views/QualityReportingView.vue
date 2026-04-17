<script setup>
import { computed, ref } from 'vue'
import { useAsyncRequest } from '@/composables/useAsyncRequest'
import ColorDeScatterChart from '@/components/ColorDeScatterChart.vue'
import { getInspections, getReportByWorkOrderId } from '@/api/qualityApi'

const QUALITY_ALERT_COLOR_DE_THRESHOLD = 1.0

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

const colorDeScatterPoints = computed(() => {
  return inspectionList.value
    .map((inspectionRecord, index) => {
      const yValue = Number(inspectionRecord.color_de)
      if (!Number.isFinite(yValue)) {
        return null
      }
      let passValue = null
      if (inspectionRecord.pass_fail === true) {
        passValue = true
      } else if (inspectionRecord.pass_fail === false) {
        passValue = false
      }
      return {
        x: index,
        y: yValue,
        pass: passValue,
        label: String(inspectionRecord.wo_id ?? index),
      }
    })
    .filter((point) => point !== null)
})

const qualityAlertList = computed(() => {
  return inspectionList.value
    .map((inspectionRecord, index) => {
      const colorDeValue = Number(inspectionRecord.color_de)
      if (!Number.isFinite(colorDeValue) || colorDeValue < QUALITY_ALERT_COLOR_DE_THRESHOLD) {
        return null
      }
      return {
        id: String(inspectionRecord.insp_id ?? `quality-alert-${index}`),
        wo_id: String(inspectionRecord.wo_id ?? '-'),
        color_de: colorDeValue,
        message: `품질 이상 감지: color_de=${colorDeValue.toFixed(2)} (기준 ${QUALITY_ALERT_COLOR_DE_THRESHOLD.toFixed(1)} 이상)`,
      }
    })
    .filter((qualityAlertItem) => qualityAlertItem !== null)
})

const normalizedReport = computed(() => {
  const payload = reportData.value
  if (payload === null || typeof payload !== 'object' || Array.isArray(payload)) {
    return null
  }

  const hasUnsupportedReportKeys =
    'workOrder' in payload ||
    'productionLogs' in payload ||
    'woId' in payload ||
    'work_order_summary' in payload ||
    'logs' in payload ||
    'quality' in payload ||
    'inspection_result' in payload

  if (hasUnsupportedReportKeys) {
    return null
  }

  return {
    workOrderBlock: payload.work_order ?? null,
    productionLogsBlock: payload.production_logs ?? null,
    inspectionBlock: payload.inspection ?? null,
    topLevelWoId: payload.wo_id ?? null,
  }
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

function formatJsonBlock(value) {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
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
      <h3>품질 이상 알림</h3>
      <p class="feature-view__hint">
        기준: <code>color_de &gt;= {{ QUALITY_ALERT_COLOR_DE_THRESHOLD.toFixed(1) }}</code>
      </p>
      <p v-if="inspectionsLoading">품질 이상 알림 계산 중...</p>
      <p v-else-if="qualityAlertList.length === 0">감지된 품질 이상 알림이 없습니다.</p>
      <div v-else class="feature-view__table-wrap">
        <table class="feature-view__table">
          <thead>
            <tr>
              <th>wo_id</th>
              <th>color_de</th>
              <th>alert_type</th>
              <th>message</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="qualityAlert in qualityAlertList" :key="qualityAlert.id">
              <td>{{ qualityAlert.wo_id }}</td>
              <td>{{ qualityAlert.color_de.toFixed(2) }}</td>
              <td>
                <span class="feature-view__alert-badge feature-view__alert-badge--quality">QUALITY_COLOR_DE_HIGH</span>
              </td>
              <td>{{ qualityAlert.message }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="feature-view__panel">
      <h3>품질 분포 (color_de 산점도)</h3>
      <p class="feature-view__hint">
        가로축은 검사 목록의 순번, 세로축은 <code>color_de</code>입니다. 주황 점선은 DE 1.0 기준선입니다.
      </p>
      <p v-if="inspectionsLoading">품질 데이터 불러오는 중...</p>
      <p v-else-if="colorDeScatterPoints.length === 0" class="feature-view__muted">
        산점도를 그릴 수 있는 <code>color_de</code> 값이 없습니다.
      </p>
      <ColorDeScatterChart v-else :points="colorDeScatterPoints" />
    </section>

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
      <h3>공정 보고서 및 성적서 뷰어</h3>
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

      <div v-else-if="reportData !== null && typeof reportData === 'object'" class="report-viewer">
        <header class="report-viewer__header">
          <p class="report-viewer__title">
            생산 검사 성적서 (미리보기)
            <span v-if="normalizedReport?.topLevelWoId" class="report-viewer__wo">
              wo_id: {{ normalizedReport.topLevelWoId }}
            </span>
          </p>
          <button type="button" class="report-viewer__pdf" disabled title="PDF/HTML 내보내기 방식 확정 후 연동">
            PDF 내보내기
          </button>
        </header>

        <div class="report-viewer__sections">
          <section v-if="normalizedReport?.workOrderBlock !== null" class="report-viewer__section">
            <h4>작업 지시</h4>
            <pre class="report-viewer__pre">{{ formatJsonBlock(normalizedReport.workOrderBlock) }}</pre>
          </section>

          <section v-if="normalizedReport?.inspectionBlock !== null" class="report-viewer__section">
            <h4>품질 결과</h4>
            <pre class="report-viewer__pre">{{ formatJsonBlock(normalizedReport.inspectionBlock) }}</pre>
          </section>

          <section v-if="normalizedReport?.productionLogsBlock !== null" class="report-viewer__section">
            <h4>생산 로그</h4>
            <pre class="report-viewer__pre">{{ formatJsonBlock(normalizedReport.productionLogsBlock) }}</pre>
          </section>
        </div>

        <details class="report-viewer__raw">
          <summary>원본 JSON 전체</summary>
          <pre class="feature-view__json">{{ JSON.stringify(reportData, null, 2) }}</pre>
        </details>
      </div>

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

.feature-view__hint {
  margin: 0 0 var(--space-sm);
  font-size: var(--font-size-sm);
}

.feature-view__muted {
  margin: 0;
  opacity: 0.85;
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
  overflow: auto;
  max-height: 16rem;
}

.feature-view__alert-badge {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--feature-view-alert-badge-color);
  background: var(--feature-view-alert-badge-background);
}

.feature-view__alert-badge--quality {
  --feature-view-alert-badge-color: var(--color-alert-badge-quality-text);
  --feature-view-alert-badge-background: var(--color-alert-badge-quality-bg);
}

.report-viewer {
  margin-top: var(--space-md);
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-background-soft);
}

.report-viewer__header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.report-viewer__title {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: 700;
}

.report-viewer__wo {
  margin-left: var(--space-xs);
  font-weight: 400;
  font-size: var(--font-size-sm);
}

.report-viewer__pdf {
  opacity: 0.55;
  cursor: not-allowed;
}

.report-viewer__sections {
  display: grid;
  gap: var(--space-md);
}

.report-viewer__section h4 {
  margin: 0 0 var(--space-xs);
  font-size: var(--font-size-sm);
}

.report-viewer__pre {
  margin: 0;
  padding: var(--space-sm);
  font-size: 0.8rem;
  border-radius: var(--radius-sm);
  background: var(--color-background);
  overflow: auto;
  max-height: 14rem;
}

.report-viewer__raw {
  margin-top: var(--space-md);
}

.report-viewer__raw summary {
  cursor: pointer;
  font-size: var(--font-size-sm);
}
</style>
