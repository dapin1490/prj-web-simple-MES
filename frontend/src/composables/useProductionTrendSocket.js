import { computed, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { isDevFixturesMode } from '@/devFixtures/isDevFixturesMode'

const DEFAULT_MAX_REALTIME_MESSAGE_COUNT = 50
const DEFAULT_MAX_EQUIPMENT_ALERT_COUNT = 30
const DEFAULT_RECONNECT_DELAY_MS = 3000
const DEFAULT_WS_ENDPOINT = 'http://localhost:8080/ws-mes'
const DEFAULT_EQUIPMENT_ALERT_COOLDOWN_MS = 10000

function getWebSocketEndpoint() {
  const configuredEndpoint = import.meta.env.VITE_WS_ENDPOINT
  if (typeof configuredEndpoint === 'string' && configuredEndpoint.trim() !== '') {
    return configuredEndpoint
  }
  return DEFAULT_WS_ENDPOINT
}

function getProductionTrendTopic() {
  const configuredTopic = import.meta.env.VITE_WS_TOPIC
  if (typeof configuredTopic === 'string' && configuredTopic.trim() !== '') {
    return configuredTopic
  }
  return '/topic/production-trend'
}

function getEquipmentAlertTopic() {
  const configuredTopic = import.meta.env.VITE_WS_EQUIPMENT_ALERT_TOPIC
  if (typeof configuredTopic === 'string' && configuredTopic.trim() !== '') {
    return configuredTopic
  }
  return '/topic/equipment-alert'
}

function getReconnectDelayMs() {
  const configuredReconnectDelay = Number(import.meta.env.VITE_WS_RECONNECT_DELAY_MS)
  if (Number.isFinite(configuredReconnectDelay) && configuredReconnectDelay >= 0) {
    return configuredReconnectDelay
  }
  return DEFAULT_RECONNECT_DELAY_MS
}

function getEquipmentAlertCooldownMs() {
  const configuredCooldownMs = Number(import.meta.env.VITE_WS_EQUIPMENT_ALERT_COOLDOWN_MS)
  if (Number.isFinite(configuredCooldownMs) && configuredCooldownMs >= 0) {
    return configuredCooldownMs
  }
  return DEFAULT_EQUIPMENT_ALERT_COOLDOWN_MS
}

function getDevFixtureTrendIntervalMs() {
  const configuredInterval = Number(import.meta.env.VITE_DEV_FIXTURE_TREND_INTERVAL_MS)
  if (Number.isFinite(configuredInterval) && configuredInterval > 0) {
    return configuredInterval
  }
  return 1500
}

export function useProductionTrendSocket() {
  const connectionState = ref('disconnected')
  const lastErrorMessage = ref('')
  const lastReceivedMessage = ref(null)
  const receivedMessageList = ref([])
  const latestEquipmentAlert = ref(null)
  const equipmentAlertList = ref([])
  const reconnectAttemptCount = ref(0)
  const reconnectDelayMs = getReconnectDelayMs()
  const equipmentAlertCooldownMs = getEquipmentAlertCooldownMs()
  let stompClient = null
  let topicSubscription = null
  let equipmentAlertSubscription = null
  let manualDisconnectRequested = false
  /** @type {Map<string, number>} */
  const equipmentAlertLastSeenAtMap = new Map()
  /** @type {ReturnType<typeof setInterval> | null} */
  let devFixtureTrendIntervalId = null

  const isConnected = computed(() => connectionState.value === 'connected')
  const isAutoReconnectEnabled = computed(() => reconnectDelayMs > 0)

  function pushEquipmentAlert(rawAlert) {
    const normalizedAlert =
      typeof rawAlert === 'object' && rawAlert !== null
        ? {
            machine_id: rawAlert.machine_id ?? '-',
            wo_id: rawAlert.wo_id ?? '-',
            alert_type: rawAlert.alert_type ?? 'UNKNOWN',
            message: rawAlert.message ?? '설비 이상이 감지되었습니다.',
            timestamp: rawAlert.timestamp ?? new Date().toISOString().slice(0, 19).replace('T', ' '),
          }
        : {
            machine_id: '-',
            wo_id: '-',
            alert_type: 'UNKNOWN',
            message: String(rawAlert),
            timestamp: new Date().toISOString().slice(0, 19).replace('T', ' '),
          }

    const alertCooldownKey = [
      String(normalizedAlert.machine_id),
      String(normalizedAlert.wo_id),
      String(normalizedAlert.alert_type),
      String(normalizedAlert.message),
    ].join('|')
    const nowTimeMs = Date.now()
    const lastSeenAtMs = equipmentAlertLastSeenAtMap.get(alertCooldownKey) ?? 0
    if (equipmentAlertCooldownMs > 0 && nowTimeMs - lastSeenAtMs < equipmentAlertCooldownMs) {
      return
    }
    equipmentAlertLastSeenAtMap.set(alertCooldownKey, nowTimeMs)

    latestEquipmentAlert.value = normalizedAlert
    equipmentAlertList.value = [normalizedAlert, ...equipmentAlertList.value].slice(
      0,
      DEFAULT_MAX_EQUIPMENT_ALERT_COUNT,
    )
  }

  function disconnect() {
    manualDisconnectRequested = true
    if (isDevFixturesMode()) {
      if (devFixtureTrendIntervalId !== null) {
        clearInterval(devFixtureTrendIntervalId)
        devFixtureTrendIntervalId = null
      }
      connectionState.value = 'disconnected'
      return
    }
    if (!stompClient) {
      connectionState.value = 'disconnected'
      return
    }

    if (topicSubscription) {
      topicSubscription.unsubscribe()
      topicSubscription = null
    }
    if (equipmentAlertSubscription) {
      equipmentAlertSubscription.unsubscribe()
      equipmentAlertSubscription = null
    }

    const clientToClose = stompClient
    stompClient = null
    connectionState.value = 'disconnecting'
    void clientToClose.deactivate().finally(() => {
      connectionState.value = 'disconnected'
    })
  }

  function connect() {
    if (isDevFixturesMode()) {
      if (devFixtureTrendIntervalId !== null || connectionState.value === 'connecting') {
        return
      }

      manualDisconnectRequested = false
      lastErrorMessage.value = ''
      connectionState.value = 'connecting'

      queueMicrotask(() => {
        if (manualDisconnectRequested) {
          return
        }
        connectionState.value = 'connected'
        reconnectAttemptCount.value = 0
        let tickCounter = 0
        const intervalMs = getDevFixtureTrendIntervalMs()
        devFixtureTrendIntervalId = setInterval(() => {
          if (manualDisconnectRequested) {
            return
          }
          tickCounter += 1
          const wave = Math.sin(tickCounter / 5.0) * 2.0
          const parsedMessage = {
            wo_id: 'WO-220101-001',
            cr_temp: 70,
            temp_sp: 70.0,
            temp_pv: 69.0 + wave,
            speed: 62 + (tickCounter % 4),
            timestamp: new Date().toISOString().slice(0, 19).replace('T', ' '),
            progress: Math.min(99.9, 12.0 + tickCounter * 0.4),
          }
          lastReceivedMessage.value = parsedMessage
          receivedMessageList.value = [parsedMessage, ...receivedMessageList.value].slice(
            0,
            DEFAULT_MAX_REALTIME_MESSAGE_COUNT,
          )
          if (tickCounter % 10 === 0) {
            pushEquipmentAlert({
              machine_id: tickCounter % 20 === 0 ? 'M-02' : 'M-01',
              wo_id: 'WO-220101-001',
              alert_type: tickCounter % 20 === 0 ? 'TEMP_HIGH' : 'TEMP_DEVIATION',
              message:
                tickCounter % 20 === 0
                  ? '설비 M-02 이상 감지: 온도 상한 초과'
                  : '설비 M-01 이상 감지: 지시 대비 실측 온도 편차 발생',
              timestamp: new Date().toISOString().slice(0, 19).replace('T', ' '),
            })
          }
        }, intervalMs)
      })
      return
    }

    if (stompClient || connectionState.value === 'connecting') {
      return
    }

    manualDisconnectRequested = false
    lastErrorMessage.value = ''
    connectionState.value = 'connecting'

    const nextClient = new Client({
      reconnectDelay: reconnectDelayMs,
      webSocketFactory: () => new SockJS(getWebSocketEndpoint()),
      onConnect: () => {
        connectionState.value = 'connected'
        reconnectAttemptCount.value = 0
        topicSubscription = nextClient.subscribe(getProductionTrendTopic(), (message) => {
          let parsedMessage
          try {
            parsedMessage = JSON.parse(message.body)
          } catch {
            parsedMessage = message.body
          }

          lastReceivedMessage.value = parsedMessage
          receivedMessageList.value = [parsedMessage, ...receivedMessageList.value].slice(
            0,
            DEFAULT_MAX_REALTIME_MESSAGE_COUNT,
          )
        })
        equipmentAlertSubscription = nextClient.subscribe(getEquipmentAlertTopic(), (message) => {
          let parsedAlert
          try {
            parsedAlert = JSON.parse(message.body)
          } catch {
            parsedAlert = message.body
          }
          pushEquipmentAlert(parsedAlert)
        })
      },
      onStompError: (frame) => {
        lastErrorMessage.value = frame.headers.message ?? 'STOMP 오류가 발생했습니다.'
      },
      onWebSocketError: () => {
        lastErrorMessage.value = 'WebSocket 연결 오류가 발생했습니다.'
      },
      onWebSocketClose: () => {
        if (manualDisconnectRequested) {
          connectionState.value = 'disconnected'
          return
        }

        if (reconnectDelayMs > 0) {
          reconnectAttemptCount.value += 1
          connectionState.value = 'reconnecting'
          return
        }

        connectionState.value = 'disconnected'
      },
    })

    stompClient = nextClient
    stompClient.activate()
  }

  function clearReceivedMessages() {
    receivedMessageList.value = []
  }

  function clearEquipmentAlerts() {
    latestEquipmentAlert.value = null
    equipmentAlertList.value = []
    equipmentAlertLastSeenAtMap.clear()
  }

  return {
    connectionState,
    isConnected,
    isAutoReconnectEnabled,
    reconnectAttemptCount,
    reconnectDelayMs,
    lastErrorMessage,
    lastReceivedMessage,
    receivedMessageList,
    latestEquipmentAlert,
    equipmentAlertList,
    connect,
    disconnect,
    clearReceivedMessages,
    clearEquipmentAlerts,
  }
}
