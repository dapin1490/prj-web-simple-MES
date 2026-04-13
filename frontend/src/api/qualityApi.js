import { apiGet } from '@/api/httpClient'

/**
 * 품질 검사 결과 목록 조회.
 * @returns {Promise<unknown>}
 */
export function getInspections() {
  return apiGet('/inspections')
}

/**
 * 특정 작업 지시(wo_id) 공정 보고서 조회.
 * @param {string | number} workOrderId
 * @returns {Promise<unknown>}
 */
export function getReportByWorkOrderId(workOrderId) {
  return apiGet(`/reports/${workOrderId}`)
}
