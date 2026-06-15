/**
 * WebSocket 工具类
 * 用于建立和管理WebSocket连接，支持自动重连
 */

class WebSocketClient {
  constructor(url, options = {}) {
    this.url = url
    this.options = {
      reconnectInterval: options.reconnectInterval || 3000,
      maxReconnectAttempts: options.maxReconnectAttempts || 10,
      heartbeatInterval: options.heartbeatInterval || 30000,
      onMessage: options.onMessage || (() => {}),
      onOpen: options.onOpen || (() => {}),
      onClose: options.onClose || (() => {}),
      onError: options.onError || (() => {})
    }
    
    this.ws = null
    this.reconnectAttempts = 0
    this.heartbeatTimer = null
    this.reconnectTimer = null
    this.isManualClose = false
  }

  connect() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      return
    }

    try {
      this.ws = new WebSocket(this.url)

      this.ws.onopen = (event) => {
        this.reconnectAttempts = 0
        this.isManualClose = false
        this.startHeartbeat()
        this.options.onOpen(event)
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.options.onMessage(data)
        } catch (e) {
          // 静默处理解析错误
        }
      }

      this.ws.onclose = (event) => {
        this.stopHeartbeat()
        this.options.onClose(event)
        
        if (!this.isManualClose && this.reconnectAttempts < this.options.maxReconnectAttempts) {
          this.scheduleReconnect()
        }
      }

      this.ws.onerror = (error) => {
        this.options.onError(error)
      }
    } catch (e) {
      this.scheduleReconnect()
    }
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(typeof data === 'string' ? data : JSON.stringify(data))
      return true
    }
    return false
  }

  close() {
    this.isManualClose = true
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  scheduleReconnect() {
    if (this.reconnectTimer) return
    
    this.reconnectAttempts++
    
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, this.options.reconnectInterval)
  }

  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.send({ type: 'ping' })
      }
    }, this.options.heartbeatInterval)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

// WebSocket管理器
class MonitorWebSocketManager {
  constructor() {
    this.client = null
    this.listeners = new Map()
    this.connected = ref(false)
  }

  connect() {
    if (this.client && this.client.isConnected()) {
      return
    }

    const wsUrl = this.getWebSocketUrl()
    
    this.client = new WebSocketClient(wsUrl, {
      reconnectInterval: 3000,
      maxReconnectAttempts: 10,
      heartbeatInterval: 25000,
      onOpen: () => {
        this.connected.value = true
        this.emit('connected', {})
        
        // 订阅监控数据
        this.subscribe()
      },
      onClose: () => {
        this.connected.value = false
        this.emit('disconnected', {})
      },
      onError: (error) => {
        this.emit('error', error)
      },
      onMessage: (data) => {
        this.handleMessage(data)
      }
    })

    this.client.connect()
  }

  getWebSocketUrl() {
    // 根据当前页面协议和主机生成WebSocket URL
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    
    // 从环境变量或使用当前主机
    const wsBase = import.meta.env.VITE_WS_BASE_URL || ''
    
    if (wsBase) {
      return wsBase.startsWith('ws') ? wsBase : `${protocol}//${wsBase}`
    }
    
    // 直接使用当前主机的 /ws/monitor 端点
    return `${protocol}//${host}/ws/monitor`
  }

  handleMessage(data) {
    // 根据消息类型分发到不同的监听器
    switch (data.type) {
      case 'monitor_update':
        this.emit('monitor_update', data.data)
        break
      case 'task_update':
        this.emit('task_update', data.data)
        break
      case 'robot_update':
        this.emit('robot_update', data.data)
        break
      case 'alert':
        this.emit('alert', data.data)
        break
      case 'pong':
        // 心跳响应，忽略
        break
      default:
        // 静默处理未知消息类型
        break
    }
  }

  subscribe() {
    // 发送订阅消息
    this.send({
      type: 'subscribe',
      channels: ['monitor', 'tasks', 'robots', 'alerts']
    })
  }

  unsubscribe() {
    this.send({
      type: 'unsubscribe',
      channels: ['monitor', 'tasks', 'robots', 'alerts']
    })
  }

  send(data) {
    if (this.client) {
      this.client.send(data)
    }
  }

  close() {
    if (this.client) {
      this.unsubscribe()
      this.client.close()
      this.client = null
    }
    this.connected.value = false
  }

  // 事件监听器管理
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
    return () => {
      const callbacks = this.listeners.get(event)
      if (callbacks) {
        const index = callbacks.indexOf(callback)
        if (index > -1) {
          callbacks.splice(index, 1)
        }
      }
    }
  }

  off(event, callback) {
    if (!callback) {
      this.listeners.delete(event)
    } else {
      const callbacks = this.listeners.get(event)
      if (callbacks) {
        const index = callbacks.indexOf(callback)
        if (index > -1) {
          callbacks.splice(index, 1)
        }
      }
    }
  }

  emit(event, data) {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(data)
        } catch (e) {
          // 静默处理
        }
      })
    }
  }
}

// 创建全局单例
import { ref } from 'vue'
const monitorWsManager = new MonitorWebSocketManager()

export { monitorWsManager, WebSocketClient, MonitorWebSocketManager }
