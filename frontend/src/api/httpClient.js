import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

/**
 * STOMP 호출 공통 모듈.
 * 응답 본문은 docs/api-details.md 규격(success, data, message)을 따른다.
 */

export class StompClientError extends Error {
  /**
   * @param {string} message
   * @param {{ payload?: unknown }} [options]
   */
  constructor(message, options = {}) {
    super(message)
    this.name = 'StompClientError'
    this.payload = options.payload
  }
}

const DEFAULT_WS_ENDPOINT = 'http://localhost:8080/ws-mes'
const DEFAULT_STOMP_REQUEST_TIMEOUT_MS = 10000
const RECONNECT_DELAY_MS = 3000

const REQUEST_DESTINATION_BY_METHOD_AND_PATH = {
  'GET /products': '/app/planning/products',
  'GET /orders': '/app/planning/orders',
  'GET /orders/:productId': '/app/planning/orders/by-product',
  'GET /work-orders': '/app/execution/work-orders',
  'GET /production/progress': '/app/execution/production/progress',
  'GET /production/logs/:workOrderId': '/app/execution/production/logs',
  'GET /inspections': '/app/quality/inspections',
  'GET /reports/:workOrderId': '/app/quality/reports',
  'POST /simulation/start': '/app/simulation/start',
  'POST /simulation/stop': '/app/simulation/stop',
  'POST /simulation/reset': '/app/simulation/reset',
}

const RESPONSE_DESTINATION_BY_METHOD_AND_PATH = {
  'GET /products': '/user/queue/planning/products',
  'GET /orders': '/user/queue/planning/orders',
  'GET /orders/:productId': '/user/queue/planning/orders/by-product',
  'GET /work-orders': '/user/queue/execution/work-orders',
  'GET /production/progress': '/user/queue/execution/production/progress',
  'GET /production/logs/:workOrderId': '/user/queue/execution/production/logs',
  'GET /inspections': '/user/queue/quality/inspections',
  'GET /reports/:workOrderId': '/user/queue/quality/reports',
  'POST /simulation/start': '/user/queue/simulation/state',
  'POST /simulation/stop': '/user/queue/simulation/state',
  'POST /simulation/reset': '/user/queue/simulation/state',
}

/** @type {Client | null} */
let stompClient = null
/** @type {Promise<void> | null} */
let connectPromise = null

/** @type {Map<string, import('@stomp/stompjs').StompSubscription>} */
const subscriptionByDestination = new Map()
/** @type {Map<string, Array<{ resolve: (value: unknown) => void, reject: (reason?: unknown) => void, timeoutId: ReturnType<typeof setTimeout> }>>} */
const pendingQueueByDestination = new Map()

/**
 * @param {unknown} value
 * @returns {value is { success?: boolean, data?: unknown, message?: string }}
 */
function isApiEnvelope(value) {
  return typeof value === 'object' && value !== null && 'success' in value
}

/**
 * @returns {string}
 */
function getWebSocketEndpoint() {
  const configuredEndpoint = import.meta.env.VITE_WS_ENDPOINT
  if (typeof configuredEndpoint === 'string' && configuredEndpoint.trim() !== '') {
    return configuredEndpoint
  }
  return DEFAULT_WS_ENDPOINT
}

/**
 * @param {string} method
 * @param {string} path
 * @returns {string}
 */
function normalizeMethodAndPathKey(method, path) {
  if (method === 'GET' && /^\/orders\/[^/]+$/.test(path)) {
    return 'GET /orders/:productId'
  }
  if (method === 'GET' && /^\/production\/logs\/[^/]+$/.test(path)) {
    return 'GET /production/logs/:workOrderId'
  }
  return `${method} ${path}`
}

/**
 * @param {string} method
 * @param {string} path
 * @returns {{ requestDestination: string, responseDestination: string }}
 */
function resolveDestinationPair(method, path) {
  const key = normalizeMethodAndPathKey(method, path)
  const requestDestination = REQUEST_DESTINATION_BY_METHOD_AND_PATH[key]
  const responseDestination = RESPONSE_DESTINATION_BY_METHOD_AND_PATH[key]
  if (!requestDestination || !responseDestination) {
    throw new StompClientError(`지원하지 않는 STOMP 경로입니다: ${key}`)
  }
  return { requestDestination, responseDestination }
}

/**
 * @param {string} method
 * @param {string} path
 * @returns {unknown}
 */
function createRequestBody(method, path) {
  if (method === 'GET' && /^\/orders\/[^/]+$/.test(path)) {
    const productId = path.split('/')[2]
    return { product_id: productId }
  }
  if (method === 'GET' && /^\/production\/logs\/[^/]+$/.test(path)) {
    const workOrderId = path.split('/')[3]
    return { wo_id: workOrderId }
  }
  if (method === 'GET') {
    return {}
  }
  return {}
}

function ensureStompConnection() {
  if (stompClient?.connected) {
    return Promise.resolve()
  }
  if (connectPromise) {
    return connectPromise
  }

  connectPromise = new Promise((resolve, reject) => {
    const nextClient = new Client({
      reconnectDelay: RECONNECT_DELAY_MS,
      webSocketFactory: () => new SockJS(getWebSocketEndpoint()),
      onConnect: () => {
        stompClient = nextClient
        connectPromise = null
        resolve()
      },
      onStompError: (frame) => {
        connectPromise = null
        reject(new StompClientError(frame.headers.message ?? 'STOMP 오류가 발생했습니다.', { payload: frame }))
      },
      onWebSocketError: () => {
        connectPromise = null
        reject(new StompClientError('WebSocket 연결 오류가 발생했습니다.'))
      },
    })

    nextClient.activate()
  })

  return connectPromise
}

/**
 * @param {string} responseDestination
 * @returns {Promise<unknown>}
 */
function waitForResponse(responseDestination) {
  return new Promise((resolve, reject) => {
    const timeoutId = setTimeout(() => {
      const currentQueue = pendingQueueByDestination.get(responseDestination) ?? []
      const nextQueue = currentQueue.filter((pendingItem) => pendingItem.timeoutId !== timeoutId)
      pendingQueueByDestination.set(responseDestination, nextQueue)
      reject(new StompClientError('STOMP 응답 대기 시간이 초과되었습니다.'))
    }, DEFAULT_STOMP_REQUEST_TIMEOUT_MS)

    const currentQueue = pendingQueueByDestination.get(responseDestination) ?? []
    currentQueue.push({ resolve, reject, timeoutId })
    pendingQueueByDestination.set(responseDestination, currentQueue)
  })
}

/**
 * @param {string} responseDestination
 */
function ensureResponseSubscription(responseDestination) {
  if (subscriptionByDestination.has(responseDestination)) {
    return
  }
  if (!stompClient) {
    throw new StompClientError('STOMP 클라이언트가 초기화되지 않았습니다.')
  }

  const subscription = stompClient.subscribe(responseDestination, (message) => {
    const pendingQueue = pendingQueueByDestination.get(responseDestination) ?? []
    if (pendingQueue.length === 0) {
      return
    }

    const nextPendingItem = pendingQueue.shift()
    pendingQueueByDestination.set(responseDestination, pendingQueue)
    if (!nextPendingItem) {
      return
    }

    clearTimeout(nextPendingItem.timeoutId)

    try {
      const parsedBody = JSON.parse(message.body)
      nextPendingItem.resolve(parsedBody)
    } catch {
      nextPendingItem.reject(new StompClientError('서버 응답 JSON을 해석할 수 없습니다.'))
    }
  })

  subscriptionByDestination.set(responseDestination, subscription)
}

/**
 * @param {string} method
 * @param {string} path STOMP 경로 별칭. 예: "/products", "/orders/PRODUCT_1"
 * @param {{ body?: unknown, headers?: Record<string, string> }} [options]
 * @returns {Promise<unknown>}
 */
export async function apiRequest(method, path, options = {}) {
  const { requestDestination, responseDestination } = resolveDestinationPair(method, path)
  await ensureStompConnection()
  ensureResponseSubscription(responseDestination)

  const requestBody = options.body !== undefined ? options.body : createRequestBody(method, path)
  const responseBodyPromise = waitForResponse(responseDestination)

  if (!stompClient) {
    throw new StompClientError('STOMP 클라이언트가 초기화되지 않았습니다.')
  }

  stompClient.publish({
    destination: requestDestination,
    body: JSON.stringify(requestBody),
    headers: options.headers,
  })

  const parsedBody = await responseBodyPromise

  if (!isApiEnvelope(parsedBody)) {
    return parsedBody
  }

  if (parsedBody.success === false) {
    const message =
      typeof parsedBody.message === 'string' && parsedBody.message !== ''
        ? parsedBody.message
        : '요청이 실패했습니다.'
    throw new StompClientError(message, {
      payload: parsedBody,
    })
  }

  return parsedBody.data
}

/**
 * @param {string} path
 * @param {{ headers?: Record<string, string> }} [options]
 * @returns {Promise<unknown>}
 */
export function apiGet(path, options) {
  return apiRequest('GET', path, { headers: options?.headers })
}

/**
 * @param {string} path
 * @param {unknown} [body]
 * @param {{ headers?: Record<string, string> }} [options]
 * @returns {Promise<unknown>}
 */
export function apiPost(path, body, options) {
  return apiRequest('POST', path, {
    body,
    headers: options?.headers,
  })
}
