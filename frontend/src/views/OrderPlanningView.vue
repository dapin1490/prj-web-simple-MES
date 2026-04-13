<script setup>
import { computed, ref } from 'vue'
import { useAsyncRequest } from '@/composables/useAsyncRequest'
import {
  getOrders,
  getOrdersByProductId,
  getProducts,
} from '@/api/planningApi'

const selectedProductId = ref('')

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

const orderList = computed(() =>
  Array.isArray(ordersData.value) ? ordersData.value : [],
)

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
  void runOrderRequest()
}

function reloadAll() {
  void loadProducts()
  void runOrderRequest()
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
      <h3>수주 목록</h3>
      <p v-if="ordersLoading">수주 목록 불러오는 중...</p>
      <p v-else-if="ordersError" class="feature-view__error">{{ ordersError.message }}</p>
      <p v-else-if="orderList.length === 0">조회된 수주 데이터가 없습니다.</p>
      <div v-else class="feature-view__table-wrap">
        <table class="feature-view__table">
          <thead>
            <tr>
              <th>order_id</th>
              <th>product_id</th>
              <th>order_qty</th>
              <th>due_date</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orderList" :key="String(order.order_id ?? order.id)">
              <td>{{ getDisplayValue(order, ['order_id', 'id']) }}</td>
              <td>{{ getDisplayValue(order, ['product_id']) }}</td>
              <td>{{ getDisplayValue(order, ['order_qty', 'qty']) }}</td>
              <td>{{ getDisplayValue(order, ['due_date', 'order_date', 'date']) }}</td>
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
</style>
