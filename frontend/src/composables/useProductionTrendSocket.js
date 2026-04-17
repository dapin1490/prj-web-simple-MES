import { computed, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { isDevFixturesMode } from '@/devFixtures/isDevFixturesMode'

const DEFAULT_MAX_REALTIME_MESSAGE_COUNT = 50
const DEFAULT_RECONNECT_DELAY_MS = 3000
const DEFAULT_WS_ENDPOINT = 'http://localhost:8080/ws-mes'

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

function getReconnectDelayMs() {
  const configuredReconnectDelay = Number(import.meta.env.VITE_WS_RECONNECT_DELAY_MS)
  if (Number.isFinite(configuredReconnectDelay) && configuredReconnectDelay >= 0) {
    return configuredReconnectDelay
  }
  return DEFAULT_RECONNECT_DELAY_MS
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
  const reconnectAttemptCount = ref(0)
  const reconnectDelayMs = getReconnectDelayMs()
  let stompClient = null
  let topicSubscription = null
  let manualDisconnectRequested = false
  /** @type {ReturnType<typeof setInterval> | null} */
  let devFixtureTrendIntervalId = null

  const isConnected = computed(() => connectionState.value === 'connected')
  const isAutoReconnectEnabled = computed(() => reconnectDelayMs > 0)

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

  return {
    connectionState,
    isConnected,
    isAutoReconnectEnabled,
    reconnectAttemptCount,
    reconnectDelayMs,
    lastErrorMessage,
    lastReceivedMessage,
    receivedMessageList,
    connect,
    disconnect,
    clearReceivedMessages,
  }
}
