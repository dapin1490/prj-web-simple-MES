<script setup>
import { computed, nextTick, ref } from 'vue'
import { useAsyncRequest } from '@/composables/useAsyncRequest'
import {
  getOrders,
  getOrdersByProductId,
  getProducts,
} from '@/api/planningApi'

const selectedProductId = ref('')
const selectedOrderRow = ref(null)
const isIssueModalOpen = ref(false)

const triggerButtonRef = ref(null)
const modalContentRef = ref(null)

function getFocusableElements(container) {
  return Array.from(
    container.querySelectorAll(
      'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"]), [contenteditable="true"]',
    ),
  )
}

function onModalKeydown(event) {
  if (event.key === 'Escape') {
    closeIssueModal()
    return
  }
  if (event.key !== 'Tab' || !modalContentRef.value) return

  const focusable = getFocusableElements(modalContentRef.value)
  if (focusable.length === 0) return

  const firstEl = focusable[0]
  const lastEl = focusable[focusable.length - 1]

  if (event.shiftKey) {
    if (document.activeElement === firstEl) {
      event.preventDefault()
      lastEl.focus()
    }
  } else {
    if (document.activeElement === lastEl) {
      event.preventDefault()
      firstEl.focus()
    }
  }
}

const {
  data: productsData,
  loading: productsLoading,
  error: productsError,
  run: loadProducts,
} = useAsyncRequest(() => getProducts(), { immediate: true })

const {
  data: ordersData,
  loading: ordersLoading,
  error: ordersError,
  run: runOrderRequest,
} = useAsyncRequest(
  () => {
    if (selectedProductId.value === '') {
      return getOrders()
    }
    return getOrdersByProductId(selectedProductId.value)
  },
  { immediate: true },
)

const productList = computed(() =>
  Array.isArray(productsData.value) ? productsData.value : [],
)

const orderList = computed(() => (Array.isArray(ordersData.value) ? ordersData.value : []))

const productByIdMap = computed(() => {
  /** @type {Record<string, object>} */
  const map = {}
  for (const productRecord of productList.value) {
    const id = productRecord.product_id
    if (id !== null && id !== undefined) {
      map[String(id)] = productRecord
    }
  }
  return map
})

const ordersWithDisplayFields = computed(() => {
  return orderList.value.map((orderRecord) => {
    const productIdKey = orderRecord.product_id != null ? String(orderRecord.product_id) : ''
    const productRecord = productIdKey !== '' ? productByIdMap.value[productIdKey] : undefined
    const productName =
      productRecord && typeof productRecord.name === 'string' ? productRecord.name : '-'
    return {
      raw: orderRecord,
      productName,
    }
  })
})

/**
 * 안전재고 이하 후보. API에 current_inven이 없으면 목록이 비어 있을 수 있음.
 */
const lowStockProductRows = computed(() => {
  const rows = []
  for (const productRecord of productList.value) {
    const safetyStock = Number(productRecord.safety_stock)
    if (!Number.isFinite(safetyStock)) {
      continue
    }
    const rawCurrent = productRecord.current_inven
    if (rawCurrent === null || rawCurrent === undefined) {
      continue
    }
    const currentInventory = Number(rawCurrent)
    if (!Number.isFinite(currentInventory)) {
      continue
    }
    if (currentInventory <= safetyStock) {
      rows.push({
        product_id: productRecord.product_id,
        name: typeof productRecord.name === 'string' ? productRecord.name : '-',
        current_inven: currentInventory,
        safety_stock: safetyStock,
      })
    }
  }
  return rows
})

function getDisplayValue(orderRecord, candidateKeys) {
  for (const key of candidateKeys) {
    if (key in orderRecord && orderRecord[key] !== null && orderRecord[key] !== undefined) {
      return String(orderRecord[key])
    }
  }
  return '-'
}

function getProductOptionKey(productRecord, index) {
  return String(productRecord.product_id ?? productRecord.id ?? `product-${index}`)
}

function getProductOptionValue(productRecord) {
  if (productRecord.product_id !== null && productRecord.product_id !== undefined) {
    return String(productRecord.product_id)
  }
  if (productRecord.id !== null && productRecord.id !== undefined) {
    return String(productRecord.id)
  }
  return ''
}

function onProductFilterChange() {
  selectedOrderRow.value = null
  void runOrderRequest()
}

function reloadAll() {
  selectedOrderRow.value = null
  void loadProducts()
  void runOrderRequest()
}

function getOrderRowKey(orderRecord) {
  return String(orderRecord.order_id ?? orderRecord.id ?? '')
}

function selectOrderRow(orderRecord) {
  selectedOrderRow.value = orderRecord
}

function openIssueModal() {
  if (selectedOrderRow.value === null) {
    return
  }
  isIssueModalOpen.value = true
  nextTick(() => {
    if (modalContentRef.value) {
      const focusable = getFocusableElements(modalContentRef.value)
      if (focusable.length > 0) {
        focusable[0].focus()
      } else {
        modalContentRef.value.focus()
      }
    }
  })
}

function closeIssueModal() {
  isIssueModalOpen.value = false
  nextTick(() => {
    triggerButtonRef.value?.focus()
  })
}

function isRowSelected(orderRecord) {
  if (selectedOrderRow.value === null) {
    return false
  }
  return getOrderRowKey(orderRecord) === getOrderRowKey(selectedOrderRow.value)
}
</script>

<template>
  <main class="feature-view">
    <header class="feature-view__header">
      <h2>수주 및 생산 계획</h2>
      <button type="button" :disabled="productsLoading || ordersLoading" @click="reloadAll">
        다시 조회
      </button>
    </header>

    <section class="feature-view__panel">
      <h3>생산 트리거 (안전재고 이하)</h3>
      <p class="feature-view__hint">
        Products의 <code>current_inven</code>과 <code>safety_stock</code>을 비교합니다. 응답에
        <code>current_inven</code>이 없으면 목록이 비어 있을 수 있습니다.
      </p>
      <p v-if="productsLoading">제품 정보 불러오는 중...</p>
      <p v-else-if="lowStockProductRows.length === 0" class="feature-view__muted">
        안전재고 이하로 표시할 품목이 없습니다.
      </p>
      <div v-else class="feature-view__table-wrap">
        <table class="feature-view__table">
          <thead>
            <tr>
              <th>product_id</th>
              <th>name</th>
              <th>current_inven</th>
              <th>safety_stock</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in lowStockProductRows"
              :key="String(row.product_id)"
              class="feature-view__row--alert"
            >
              <td>{{ row.product_id }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.current_inven }}</td>
              <td>{{ row.safety_stock }}</td>
              <td><span class="feature-view__badge">재고 부족</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="feature-view__panel">
      <label class="feature-view__label" for="product-filter">제품 필터</label>
      <select
        id="product-filter"
        v-model="selectedProductId"
        :disabled="productsLoading || productsError !== null"
        @change="onProductFilterChange"
      >
        <option value="">전체 제품</option>
        <option
          v-for="(product, productIndex) in productList"
          :key="getProductOptionKey(product, productIndex)"
          :value="getProductOptionValue(product)"
        >
          {{ product.name ?? product.product_name ?? product.product_id ?? '이름 없음' }}
        </option>
      </select>
      <p v-if="productsLoading">제품 목록 불러오는 중...</p>
      <p v-else-if="productsError" class="feature-view__error">{{ productsError.message }}</p>
    </section>

    <section class="feature-view__panel">
      <div class="feature-view__toolbar">
        <h3>수주 목록</h3>
        <div class="feature-view__toolbar-actions">
          <button
            ref="triggerButtonRef"
            type="button"
            :disabled="selectedOrderRow === null || ordersLoading"
            @click="openIssueModal"
          >
            작업 지시 발행
          </button>
        </div>
      </div>
      <p v-if="ordersLoading">수주 목록 불러오는 중...</p>
      <p v-else-if="ordersError" class="feature-view__error">{{ ordersError.message }}</p>
      <p v-else-if="orderList.length === 0">조회된 수주 데이터가 없습니다.</p>
      <div v-else class="feature-view__table-wrap">
        <table class="feature-view__table">
          <thead>
            <tr>
              <th scope="col">선택</th>
              <th scope="col">order_id</th>
              <th scope="col">product_id</th>
              <th scope="col">name</th>
              <th scope="col">order_qty</th>
              <th scope="col">order_date</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in ordersWithDisplayFields"
              :key="getOrderRowKey(row.raw)"
              :class="{ 'feature-view__row--selected': isRowSelected(row.raw) }"
            >
              <td>
                <button type="button" class="feature-view__link-button" @click="selectOrderRow(row.raw)">
                  선택
                </button>
              </td>
              <td>{{ getDisplayValue(row.raw, ['order_id', 'id']) }}</td>
              <td>{{ getDisplayValue(row.raw, ['product_id']) }}</td>
              <td>{{ row.productName }}</td>
              <td>{{ getDisplayValue(row.raw, ['order_qty', 'qty']) }}</td>
              <td>{{ getDisplayValue(row.raw, ['order_date', 'due_date', 'date']) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <Teleport to="body">
      <div
        v-if="isIssueModalOpen"
        class="issue-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="issue-modal-title"
        @keydown="onModalKeydown"
      >
        <div class="issue-modal__backdrop" tabindex="-1" @click="closeIssueModal" />
        <div ref="modalContentRef" class="issue-modal__content" tabindex="-1">
          <h3 id="issue-modal-title">작업 지시 발행</h3>
          <p class="feature-view__hint">
            백엔드에 WorkOrder 생성 API가 <code>docs/api-details.md</code>에 반영되면 이 화면에서 연동합니다.
          </p>
          <pre v-if="selectedOrderRow !== null" class="issue-modal__preview">{{
            JSON.stringify(selectedOrderRow, null, 2)
          }}</pre>
          <div class="issue-modal__actions">
            <button type="button" class="issue-modal__primary" disabled>발행 요청 (준비 중)</button>
            <button type="button" @click="closeIssueModal">닫기</button>
          </div>
        </div>
      </div>
    </Teleport>
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
  color: var(--color-text);
}

.feature-view__muted {
  margin: 0;
  color: var(--color-text);
  opacity: 0.85;
}

.feature-view__error {
  color: var(--color-status-danger);
}

.feature-view__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}

.feature-view__toolbar h3 {
  margin: 0;
}

.feature-view__toolbar-actions {
  display: flex;
  gap: var(--space-xs);
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

.feature-view__row--selected {
  background: var(--color-brand-primary-soft);
}

.feature-view__row--alert {
  background: rgba(211, 47, 47, 0.06);
}

.feature-view__badge {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: 700;
  color: var(--color-status-danger);
  border: 1px solid var(--color-status-danger);
}

.feature-view__link-button {
  padding: 0;
  border: none;
  background: none;
  color: var(--color-brand-primary, #42b883);
  text-decoration: underline;
  cursor: pointer;
  font: inherit;
}

.issue-modal {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: var(--space-md);
}

.issue-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

.issue-modal__content {
  position: relative;
  z-index: 1;
  width: min(520px, 100%);
  max-height: min(80vh, 640px);
  overflow: auto;
  padding: var(--space-lg);
  border-radius: var(--radius-md);
  background: var(--color-background);
  border: 1px solid var(--color-border);
}

.issue-modal__preview {
  margin: var(--space-sm) 0;
  padding: var(--space-sm);
  font-size: 0.8rem;
  border-radius: var(--radius-sm);
  background: var(--color-background-soft);
  overflow: auto;
}

.issue-modal__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  justify-content: flex-end;
  margin-top: var(--space-md);
}

.issue-modal__primary {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
