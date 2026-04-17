<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useAsyncRequest } from '@/composables/useAsyncRequest'
import { useProductionTrendSocket } from '@/composables/useProductionTrendSocket'
import ProductionTrendChart from '@/components/ProductionTrendChart.vue'
import {
  getProductionLogsByWorkOrderId,
  getProductionProgress,
  getWorkOrders,
} from '@/api/executionApi'

const CHART_MAX_POINTS = 80
const LOG_MAX_LINES = 40

const selectedWorkOrderId = ref('')

const {
  data: workOrdersData,
  loading: workOrdersLoading,
  error: workOrdersError,
  run: loadWorkOrders,
} = useAsyncRequest(() => getWorkOrders(), { immediate: true })

const {
  data: productionProgressData,
  loading: productionProgressLoading,
  error: productionProgressError,
  run: loadProductionProgress,
} = useAsyncRequest(() => getProductionProgress(), { immediate: true })

const {
  data: productionLogsData,
  loading: productionLogsLoading,
  error: productionLogsError,
  run: loadProductionLogs,
} = useAsyncRequest(
  () => {
    if (selectedWorkOrderId.value === '') {
      return []
    }
    return getProductionLogsByWorkOrderId(selectedWorkOrderId.value)
  },
  { immediate: false },
)

const workOrderList = computed(() =>
  Array.isArray(workOrdersData.value) ? workOrdersData.value : [],
)

const productionLogList = computed(() =>
  Array.isArray(productionLogsData.value) ? productionLogsData.value : [],
)

const {
  connectionState: productionTrendConnectionState,
  isConnected: isProductionTrendConnected,
  isAutoReconnectEnabled,
  reconnectAttemptCount: productionTrendReconnectAttemptCount,
  reconnectDelayMs: productionTrendReconnectDelayMs,
  lastErrorMessage: productionTrendErrorMessage,
  receivedMessageList: productionTrendMessageList,
  connect: connectProductionTrendSocket,
  disconnect: disconnectProductionTrendSocket,
  clearReceivedMessages: clearProductionTrendMessages,
} = useProductionTrendSocket()

const filteredRealtimeTrendMessageList = computed(() => {
  if (selectedWorkOrderId.value === '') {
    return productionTrendMessageList.value
  }

  return productionTrendMessageList.value.filter((messageItem) => {
    if (typeof messageItem !== 'object' || messageItem === null) {
      return false
    }
    return String(messageItem.wo_id ?? '') === selectedWorkOrderId.value
  })
})

const logPanelRef = ref(null)

const chartSeriesPoints = computed(() => {
  const parsedRows = []
  for (const messageItem of filteredRealtimeTrendMessageList.value) {
    if (typeof messageItem !== 'object' || messageItem === null) {
      continue
    }
    const temperatureValue = Number(messageItem.temp_pv)
    const speedValue = Number(messageItem.speed)
    if (!Number.isFinite(temperatureValue) || !Number.isFinite(speedValue)) {
      continue
    }
    const sortKey = parseTimestampToSortKey(messageItem.timestamp)
    if (!Number.isFinite(sortKey)) {
      continue
    }
    parsedRows.push({
      sortKey,
      label: formatChartAxisLabel(messageItem.timestamp),
      temp_pv: temperatureValue,
      speed: speedValue,
    })
  }
  parsedRows.sort((firstRow, secondRow) => firstRow.sortKey - secondRow.sortKey)
  const trimmedRows = parsedRows.slice(-CHART_MAX_POINTS)
  return trimmedRows.map(({ label, temp_pv, speed }) => ({ label, temp_pv, speed }))
})

const logStreamLines = computed(() => {
  const newestFirst = filteredRealtimeTrendMessageList.value
  const windowItems = newestFirst.slice(0, LOG_MAX_LINES).reverse()
  return windowItems.map((messageItem) => {
    const workOrderLabel = getFirstDefinedValue(messageItem, ['wo_id'])
    const timestampLabel = getFirstDefinedValue(messageItem, ['timestamp'])
    const temperatureLabel = getFirstDefinedValue(messageItem, ['temp_pv'])
    const speedLabel = getFirstDefinedValue(messageItem, ['speed'])
    const progressLabel = getFirstDefinedValue(messageItem, ['progress'])
    return `[${timestampLabel}] wo_id=${workOrderLabel} temp_pv=${temperatureLabel} speed=${speedLabel} progress=${progressLabel}`
  })
})

watch(logStreamLines, async () => {
  await nextTick()
  const panelElement = logPanelRef.value
  if (panelElement) {
    panelElement.scrollTop = panelElement.scrollHeight
  }
})

function parseTimestampToSortKey(timestampValue) {
  if (timestampValue === null || timestampValue === undefined) {
    return NaN
  }
  const text = typeof timestampValue === 'string' ? timestampValue : String(timestampValue)
  const normalized = text.includes('T') ? text : text.replace(' ', 'T')
  const parsedTime = Date.parse(normalized)
  return Number.isFinite(parsedTime) ? parsedTime : NaN
}

function formatChartAxisLabel(timestampValue) {
  const text = typeof timestampValue === 'string' ? timestampValue : String(timestampValue)
  if (text.length >= 19) {
    return text.slice(11, 19)
  }
  return text
}

function getFirstDefinedValue(recordObject, candidateKeys) {
  for (const key of candidateKeys) {
    if (key in recordObject && recordObject[key] !== null && recordObject[key] !== undefined) {
      return String(recordObject[key])
    }
  }
  return '-'
}

function getWorkOrderOptionKey(workOrderRecord, index) {
  return String(workOrderRecord.wo_id ?? workOrderRecord.id ?? `work-order-${index}`)
}

function getWorkOrderOptionValue(workOrderRecord) {
  if (workOrderRecord.wo_id !== null && workOrderRecord.wo_id !== undefined) {
    return String(workOrderRecord.wo_id)
  }
  if (workOrderRecord.id !== null && workOrderRecord.id !== undefined) {
    return String(workOrderRecord.id)
  }
  return ''
}

function onWorkOrderChange() {
  if (selectedWorkOrderId.value === '') {
    return
  }
  void loadProductionLogs()
}

function refreshMonitoringData() {
  void loadWorkOrders()
  void loadProductionProgress()
  if (selectedWorkOrderId.value !== '') {
    void loadProductionLogs()
  }
}
</script>

<template>
  <main class="feature-view">
    <header class="feature-view__header">
      <h2>실시간 공정 모니터링</h2>
      <button
        type="button"
        :disabled="workOrdersLoading || productionProgressLoading || productionLogsLoading"
        @click="refreshMonitoringData"
      >
        다시 조회
      </button>
    </header>

    <section class="feature-view__panel">
      <h3>실시간 연결 상태</h3>
      <p>
        STOMP 연결 상태:
        <strong
          :class="
            isProductionTrendConnected
              ? 'feature-view__status--ok'
              : productionTrendConnectionState === 'reconnecting'
                ? 'feature-view__status--danger'
                : 'feature-view__status--warn'
          "
        >
          {{ productionTrendConnectionState }}
        </strong>
      </p>
      <p v-if="productionTrendConnectionState === 'reconnecting'" class="feature-view__status-hint">
        재연결 시도 중... (시도 {{ productionTrendReconnectAttemptCount }}회, 간격
        {{ productionTrendReconnectDelayMs }}ms)
      </p>
      <p v-else-if="!isAutoReconnectEnabled" class="feature-view__status-hint">
        자동 재연결 비활성화 상태입니다.
      </p>
      <p v-if="productionTrendErrorMessage !== ''" class="feature-view__error">
        {{ productionTrendErrorMessage }}
      </p>
      <div class="feature-view__actions">
        <button type="button" :disabled="isProductionTrendConnected" @click="connectProductionTrendSocket">
          연결
        </button>
        <button type="button" :disabled="!isProductionTrendConnected" @click="disconnectProductionTrendSocket">
          해제
        </button>
        <button type="button" :disabled="productionTrendMessageList.length === 0" @click="clearProductionTrendMessages">
          메시지 비우기
        </button>
      </div>
    </section>

    <section class="feature-view__panel">
      <h3>실시간 트렌드 차트</h3>
      <p v-if="chartSeriesPoints.length === 0">
        차트에 표시할 수 있는 데이터가 없습니다. (temp_pv, speed, timestamp 필요)
      </p>
      <ProductionTrendChart v-else :points="chartSeriesPoints" />
    </section>

    <section class="feature-view__panel">
      <h3>로그 스트리밍</h3>
      <p v-if="logStreamLines.length === 0">수신 로그가 없습니다.</p>
      <pre v-else ref="logPanelRef" class="feature-view__log">{{ logStreamLines.join('\n') }}</pre>
    </section>

    <section class="feature-view__panel">
      <h3>실시간 트렌드 스트림</h3>
      <p v-if="filteredRealtimeTrendMessageList.length === 0">
        수신된 실시간 메시지가 없습니다.
      </p>
      <div v-else class="feature-view__table-wrap">
        <table class="feature-view__table">
          <thead>
            <tr>
              <th>wo_id</th>
              <th>timestamp</th>
              <th>temp_pv</th>
              <th>speed</th>
              <th>progress</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(messageItem, messageIndex) in filteredRealtimeTrendMessageList" :key="messageIndex">
              <td>{{ getFirstDefinedValue(messageItem, ['wo_id']) }}</td>
              <td>{{ getFirstDefinedValue(messageItem, ['timestamp']) }}</td>
              <td>{{ getFirstDefinedValue(messageItem, ['temp_pv']) }}</td>
              <td>{{ getFirstDefinedValue(messageItem, ['speed']) }}</td>
              <td>{{ getFirstDefinedValue(messageItem, ['progress']) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="feature-view__panel">
      <h3>진척률 요약</h3>
      <p v-if="productionProgressLoading">진척률 정보 불러오는 중...</p>
      <p v-else-if="productionProgressError" class="feature-view__error">
        {{ productionProgressError.message }}
      </p>
      <pre v-else-if="productionProgressData !== null" class="feature-view__json">{{
        JSON.stringify(productionProgressData, null, 2)
      }}</pre>
      <p v-else>진척률 정보가 없습니다.</p>
    </section>

    <section class="feature-view__panel">
      <h3>작업 지시 목록</h3>
      <p v-if="workOrdersLoading">작업 지시 목록 불러오는 중...</p>
      <p v-else-if="workOrdersError" class="feature-view__error">{{ workOrdersError.message }}</p>
      <p v-else-if="workOrderList.length === 0">작업 지시 데이터가 없습니다.</p>
      <div v-else class="feature-view__table-wrap">
        <table class="feature-view__table">
          <thead>
            <tr>
              <th>wo_id</th>
              <th>order_id</th>
              <th>planned_qty</th>
              <th>machine_id</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="workOrder in workOrderList" :key="String(workOrder.wo_id ?? workOrder.id)">
              <td>{{ getFirstDefinedValue(workOrder, ['wo_id', 'id']) }}</td>
              <td>{{ getFirstDefinedValue(workOrder, ['order_id']) }}</td>
              <td>{{ getFirstDefinedValue(workOrder, ['planned_qty', 'qty']) }}</td>
              <td>{{ getFirstDefinedValue(workOrder, ['machine_id', 'equipment_id']) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="feature-view__panel">
      <h3>로트 로그 조회</h3>
      <label class="feature-view__label" for="work-order-filter">작업 지시(wo_id) 선택</label>
      <select
        id="work-order-filter"
        v-model="selectedWorkOrderId"
        :disabled="workOrdersLoading || workOrderList.length === 0"
        @change="onWorkOrderChange"
      >
        <option value="">작업 지시 선택</option>
        <option
          v-for="(workOrder, workOrderIndex) in workOrderList"
          :key="getWorkOrderOptionKey(workOrder, workOrderIndex)"
          :value="getWorkOrderOptionValue(workOrder)"
        >
          {{ getFirstDefinedValue(workOrder, ['wo_id', 'id']) }}
        </option>
      </select>

      <p v-if="productionLogsLoading">로그 불러오는 중...</p>
      <p v-else-if="productionLogsError" class="feature-view__error">{{ productionLogsError.message }}</p>
      <p v-else-if="selectedWorkOrderId === ''">작업 지시를 선택하면 로그를 조회합니다.</p>
      <p v-else-if="productionLogList.length === 0">조회된 로그 데이터가 없습니다.</p>
      <div v-else class="feature-view__table-wrap">
        <table class="feature-view__table">
          <thead>
            <tr>
              <th>timestamp</th>
              <th>temp_pv</th>
              <th>speed</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(productionLog, logIndex) in productionLogList" :key="String(productionLog.log_id ?? logIndex)">
              <td>{{ getFirstDefinedValue(productionLog, ['timestamp', 'created_at']) }}</td>
              <td>{{ getFirstDefinedValue(productionLog, ['temp_pv']) }}</td>
              <td>{{ getFirstDefinedValue(productionLog, ['speed']) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
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

.feature-view__actions {
  display: flex;
  gap: var(--space-xs);
}

.feature-view__status--ok {
  color: var(--color-status-normal);
}

.feature-view__status--warn {
  color: var(--color-status-warning);
}

.feature-view__status--danger {
  color: var(--color-status-danger);
}

.feature-view__status-hint {
  margin-top: var(--space-xs);
  color: var(--color-text);
}

.feature-view__error {
  color: var(--color-status-danger);
}

.feature-view__json {
  margin-top: var(--space-xs);
  padding: var(--space-sm);
  border-radius: var(--radius-sm);
  background: var(--color-background-soft);
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

.feature-view__log {
  max-height: 12rem;
  overflow: auto;
  margin: 0;
  padding: var(--space-sm);
  font-size: 0.8rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: var(--radius-sm);
  background: var(--color-background-soft);
}
</style>
