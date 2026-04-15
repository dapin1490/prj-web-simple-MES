/**
 * REST 호출 공통 모듈. 응답 본문은 docs/api-details.md 규격(success, data, message)을 따른다.
 * Base URL은 VITE_API_BASE_URL(예: http://localhost:8080/api/v1)을 사용한다.
 */

export class ApiClientError extends Error {
  /**
   * @param {string} message
   * @param {{ httpStatus?: number, payload?: unknown }} [options]
   */
  constructor(message, options = {}) {
    super(message)
    this.name = 'ApiClientError'
    this.httpStatus = options.httpStatus
    this.payload = options.payload
  }
}

/**
 * @returns {string}
 */
function getApiBaseUrl() {
  const baseUrl = import.meta.env.VITE_API_BASE_URL
  if (typeof baseUrl !== 'string' || baseUrl.trim() === '') {
    throw new ApiClientError(
      'VITE_API_BASE_URL이 설정되지 않았습니다. frontend/.env.development를 확인하세요.',
    )
  }
  return baseUrl.replace(/\/+$/, '')
}

/**
 * @param {string} baseUrl
 * @param {string} path
 * @returns {string}
 */
function joinBaseAndPath(baseUrl, path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${baseUrl}${normalizedPath}`
}

/**
 * @param {unknown} value
 * @returns {value is { success?: boolean, data?: unknown, message?: string }}
 */
function isApiEnvelope(value) {
  return typeof value === 'object' && value !== null && 'success' in value
}

/**
 * @param {string} method
 * @param {string} path API 경로. 예: "/products", "/orders/1" (Base URL에 /api/v1 포함 가정)
 * @param {{ body?: unknown, headers?: Record<string, string> }} [options]
 * @returns {Promise<unknown>}
 */
export async function apiRequest(method, path, options = {}) {
  const baseUrl = getApiBaseUrl()
  const url = joinBaseAndPath(baseUrl, path)

  /** @type {RequestInit} */
  const init = {
    method,
    headers: {
      Accept: 'application/json',
      ...options.headers,
    },
  }

  if (options.body !== undefined) {
    init.headers = {
      ...init.headers,
      'Content-Type': 'application/json',
    }
    init.body = JSON.stringify(options.body)
  }

  let response
  try {
    response = await fetch(url, init)
  } catch (cause) {
    throw new ApiClientError('네트워크 요청에 실패했습니다.', {
      payload: { cause },
    })
  }

  const contentType = response.headers.get('Content-Type') ?? ''
  const isJson = contentType.includes('application/json')
  /** @type {unknown} */
  let parsedBody = null

  if (isJson) {
    const text = await response.text()
    if (text.trim() !== '') {
      try {
        parsedBody = JSON.parse(text)
      } catch {
        throw new ApiClientError('서버 응답 JSON을 해석할 수 없습니다.', {
          httpStatus: response.status,
        })
      }
    }
  }

  if (!response.ok) {
    const message =
      isApiEnvelope(parsedBody) && typeof parsedBody.message === 'string'
        ? parsedBody.message
        : `HTTP ${response.status}`
    throw new ApiClientError(message, {
      httpStatus: response.status,
      payload: parsedBody,
    })
  }

  if (parsedBody === null) {
    return undefined
  }

  if (!isApiEnvelope(parsedBody)) {
    return parsedBody
  }

  if (parsedBody.success === false) {
    const message =
      typeof parsedBody.message === 'string' && parsedBody.message !== ''
        ? parsedBody.message
        : '요청이 실패했습니다.'
    throw new ApiClientError(message, {
      httpStatus: response.status,
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
