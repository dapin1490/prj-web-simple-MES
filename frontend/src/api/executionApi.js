import { apiGet } from '@/api/httpClient'

/**
 * 작업 지시 목록 조회.
 * @returns {Promise<unknown>}
 */
export function getWorkOrders() {
  return apiGet('/work-orders')
}

/**
 * 전 가동 라인 진척률 조회.
 * @returns {Promise<unknown>}
 */
export function getProductionProgress() {
  return apiGet('/production/progress')
}

/**
 * 특정 작업 지시(wo_id) 실시간 로그 조회.
 * @param {string | number} workOrderId
 * @returns {Promise<unknown>}
 */
export function getProductionLogsByWorkOrderId(workOrderId) {
  return apiGet(`/production/logs/${workOrderId}`)
}
