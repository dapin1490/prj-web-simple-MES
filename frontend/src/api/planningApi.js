import { apiGet } from '@/api/httpClient'

/**
 * 제품 목록 조회.
 * @returns {Promise<unknown>}
 */
export function getProducts() {
  return apiGet('/products')
}

/**
 * 전체 수주 목록 조회.
 * @returns {Promise<unknown>}
 */
export function getOrders() {
  return apiGet('/orders')
}

/**
 * 특정 제품 수주 목록 조회.
 * @param {string | number} productId
 * @returns {Promise<unknown>}
 */
export function getOrdersByProductId(productId) {
  return apiGet(`/orders/${productId}`)
}
