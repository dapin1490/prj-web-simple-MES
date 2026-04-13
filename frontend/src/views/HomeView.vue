<script setup>
import TheWelcome from '../components/TheWelcome.vue'
import { useAsyncRequest } from '@/composables/useAsyncRequest'
import { apiGet } from '@/api/httpClient'

const {
  data: productsData,
  loading: productsLoading,
  error: productsError,
  run: loadProducts,
} = useAsyncRequest(() => apiGet('/products'), {
  immediate: true,
})
</script>

<template>
  <main>
    <section class="api-status" aria-live="polite">
      <h2 class="api-status__title">API 연동 확인 (개발용)</h2>
      <p v-if="productsLoading" class="api-status__loading">불러오는 중...</p>
      <p v-else-if="productsError" class="api-status__error" role="alert">
        {{ productsError.message }}
      </p>
      <pre
        v-else-if="productsData !== null"
        class="api-status__data"
      ><code>{{ JSON.stringify(productsData, null, 2) }}</code></pre>
      <p v-else class="api-status__empty">표시할 데이터가 없습니다.</p>
      <button
        type="button"
        class="api-status__retry"
        :disabled="productsLoading"
        @click="loadProducts()"
      >
        다시 불러오기
      </button>
    </section>
    <TheWelcome />
  </main>
</template>

<style scoped>
.api-status {
  margin-bottom: 1.5rem;
  padding: 1rem;
  border: 1px solid var(--color-border, #e0e0e0);
  border-radius: 8px;
}

.api-status__title {
  margin: 0 0 0.75rem;
  font-size: 1rem;
}

.api-status__loading {
  margin: 0;
  color: var(--color-text-muted, #666);
}

.api-status__error {
  margin: 0;
  color: #b00020;
}

.api-status__data {
  margin: 0 0 0.75rem;
  max-height: 12rem;
  overflow: auto;
  padding: 0.75rem;
  font-size: 0.85rem;
  background: var(--color-background-soft, #f5f5f5);
  border-radius: 4px;
}

.api-status__empty {
  margin: 0 0 0.75rem;
  color: var(--color-text-muted, #666);
}

.api-status__retry {
  padding: 0.35rem 0.75rem;
  cursor: pointer;
}

.api-status__retry:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
