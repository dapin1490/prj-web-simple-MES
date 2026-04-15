import { apiPost } from '@/api/httpClient'

/**
 * 시뮬레이션 시작.
 * @returns {Promise<unknown>}
 */
export function startSimulation() {
  return apiPost('/simulation/start')
}

/**
 * 시뮬레이션 정지.
 * @returns {Promise<unknown>}
 */
export function stopSimulation() {
  return apiPost('/simulation/stop')
}

/**
 * 시뮬레이션 초기화.
 * @returns {Promise<unknown>}
 */
export function resetSimulation() {
  return apiPost('/simulation/reset')
}
