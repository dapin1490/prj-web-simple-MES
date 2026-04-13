<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAsyncRequest } from '@/composables/useAsyncRequest'
import { useProductionTrendSocket } from '@/composables/useProductionTrendSocket'
import {
  getProductionLogsByWorkOrderId,
  getProductionProgress,
  getWorkOrders,
} from '@/api/executionApi'

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
  lastErrorMessage: productionTrendErrorMessage,
  lastReceivedMessage: latestProductionTrendMessage,
  connect: connectProductionTrendSocket,
  disconnect: disconnectProductionTrendSocket,
} = useProductionTrendSocket()

const realtimeTrendMessageList = ref([])
const MAX_REALTIME_MESSAGE_COUNT = 50

onMounted(() => {
  connectProductionTrendSocket()
})

onBeforeUnmount(() => {
  disconnectProductionTrendSocket()
})

watch(latestProductionTrendMessage, (messagePayload) => {
  if (messagePayload === null) {
    return
  }

  realtimeTrendMessageList.value = [
    messagePayload,
    ...realtimeTrendMessageList.value,
  ].slice(0, MAX_REALTIME_MESSAGE_COUNT)
})

const filteredRealtimeTrendMessageList = computed(() => {
  if (selectedWorkOrderId.value === '') {
    return realtimeTrendMessageList.value
  }

  return realtimeTrendMessageList.value.filter((messageItem) => {
    if (typeof messageItem !== 'object' || messageItem === null) {
      return false
    }
    return String(messageItem.wo_id ?? '') === selectedWorkOrderId.value
  })
})

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
          :class="isProductionTrendConnected ? 'feature-view__status--ok' : 'feature-view__status--warn'"
        >
          {{ productionTrendConnectionState }}
        </strong>
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
      </div>
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
</style>
