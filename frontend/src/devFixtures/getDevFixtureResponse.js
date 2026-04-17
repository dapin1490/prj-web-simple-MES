/**
 * api-details.md 및 백엔드 DTO(snake_case)에 맞춘 개발용 `data` 페이로드.
 */

const PRODUCTS = [
  { product_id: 'P-1001', name: 'High Stretch Poly Fabric', category: 'Dyeing', safety_stock: 120 },
  { product_id: 'P-1002', name: 'Cotton Blend Fabric', category: 'Dyeing', safety_stock: 150 },
]

const ORDERS = [
  { order_id: 'SO-20220101-001', product_id: 'P-1001', order_date: '2022-01-01', order_qty: 400 },
  { order_id: 'SO-20220102-001', product_id: 'P-1001', order_date: '2022-01-02', order_qty: 350 },
  { order_id: 'SO-20220101-002', product_id: 'P-1002', order_date: '2022-01-01', order_qty: 500 },
]

const WORK_ORDERS = [
  { wo_id: 'WO-220101-001', order_id: 'SO-20220101-001', planned_qty: 400, machine_id: 'M-01' },
  { wo_id: 'WO-220102-001', order_id: 'SO-20220102-001', planned_qty: 350, machine_id: 'M-02' },
  { wo_id: 'WO-220101-002', order_id: 'SO-20220101-002', planned_qty: 500, machine_id: 'M-03' },
]

const PRODUCTION_LOGS_BY_WO = {
  'WO-220101-001': [
    {
      log_id: 1,
      wo_id: 'WO-220101-001',
      timestamp: '2022-01-01T09:00:00',
      cr_temp: 70,
      temp_sp: 70.0,
      temp_pv: 68.4,
      speed: 62,
    },
    {
      log_id: 2,
      wo_id: 'WO-220101-001',
      timestamp: '2022-01-01T09:01:00',
      cr_temp: 70,
      temp_sp: 70.0,
      temp_pv: 69.1,
      speed: 64,
    },
    {
      log_id: 3,
      wo_id: 'WO-220101-001',
      timestamp: '2022-01-01T09:02:00',
      cr_temp: 70,
      temp_sp: 70.0,
      temp_pv: 69.7,
      speed: 65,
    },
  ],
  'WO-220102-001': [
    {
      log_id: 4,
      wo_id: 'WO-220102-001',
      timestamp: '2022-01-02T10:00:00',
      cr_temp: 69,
      temp_sp: 69.0,
      temp_pv: 67.8,
      speed: 58,
    },
  ],
}

const INSPECTIONS = [
  { insp_id: 'INSP-WO-220101-001', wo_id: 'WO-220101-001', color_de: 0.72, pass_fail: true },
  { insp_id: 'INSP-WO-220102-001', wo_id: 'WO-220102-001', color_de: 1.12, pass_fail: false },
  { insp_id: 'INSP-WO-220101-002', wo_id: 'WO-220101-002', color_de: 0.55, pass_fail: true },
]

function buildReport(woId) {
  const workOrder = WORK_ORDERS.find((row) => row.wo_id === woId)
  if (!workOrder) {
    return null
  }
  const productionLogs = PRODUCTION_LOGS_BY_WO[woId] ?? []
  const inspection = INSPECTIONS.find((row) => row.wo_id === woId) ?? {
    insp_id: `INSP-${woId}`,
    wo_id: woId,
    color_de: 0.8,
    pass_fail: true,
  }
  return {
    wo_id: woId,
    work_order: workOrder,
    production_logs: productionLogs,
    inspection,
  }
}

const PROGRESS = {
  progress: 38.5,
  work_orders: [
    { wo_id: 'WO-220101-001', progress: 42.0 },
    { wo_id: 'WO-220102-001', progress: 35.0 },
    { wo_id: 'WO-220101-002', progress: 38.5 },
  ],
}

let simulationRunning = false
let simulationPointer = 0

/**
 * @param {string} method
 * @param {string} path
 * @param {{ body?: unknown }} options
 * @returns {unknown}
 */
function normalizeMethodAndPathKey(method, path) {
  if (method === 'GET' && /^\/orders\/[^/]+$/.test(path)) {
    return 'GET /orders/:productId'
  }
  if (method === 'GET' && /^\/production\/logs\/[^/]+$/.test(path)) {
    return 'GET /production/logs/:workOrderId'
  }
  if (method === 'GET' && /^\/reports\/[^/]+$/.test(path)) {
    return 'GET /reports/:workOrderId'
  }
  return `${method} ${path}`
}

/**
 * @param {string} method
 * @param {string} path
 * @param {{ body?: unknown }} [options]
 * @returns {Promise<unknown>}
 */
export async function getDevFixtureResponse(method, path, _options = {}) {
  await Promise.resolve()
  const key = normalizeMethodAndPathKey(method, path)

  switch (key) {
    case 'GET /products':
      return PRODUCTS
    case 'GET /orders':
      return ORDERS
    case 'GET /orders/:productId': {
      const productId = path.split('/')[2]
      return ORDERS.filter((row) => String(row.product_id) === String(productId))
    }
    case 'GET /work-orders':
      return WORK_ORDERS
    case 'GET /production/progress':
      return PROGRESS
    case 'GET /production/logs/:workOrderId': {
      const workOrderId = path.split('/')[3]
      return PRODUCTION_LOGS_BY_WO[workOrderId] ?? []
    }
    case 'GET /inspections':
      return INSPECTIONS
    case 'GET /reports/:workOrderId': {
      const workOrderId = path.split('/')[2]
      const report = buildReport(workOrderId)
      if (!report) {
        throw new Error(`픽스처: wo_id를 찾을 수 없습니다: ${workOrderId}`)
      }
      return report
    }
    case 'POST /simulation/start':
      simulationRunning = true
      return { running: simulationRunning, pointer: simulationPointer }
    case 'POST /simulation/stop':
      simulationRunning = false
      return { running: simulationRunning, pointer: simulationPointer }
    case 'POST /simulation/reset':
      simulationRunning = false
      simulationPointer = 0
      return { running: simulationRunning, pointer: simulationPointer }
    default:
      throw new Error(`픽스처에 정의되지 않은 요청입니다: ${key}`)
  }
}
