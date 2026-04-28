/**
 * 聊天WebSocket管理器
 * 处理实时聊天消息推送
 */

import { ref, reactive } from 'vue'

class ChatWebSocketManager {
  constructor() {
    this.stompClient = null
    this.socket = null
    this.connected = ref(false)
    this.subscriptions = new Map()
    this.listeners = reactive({})
    this.currentUserId = null
    this.currentConversationId = null
    this.reconnectTimer = null
    this.reconnectInterval = 3000
    this.maxReconnectAttempts = 10
    this.reconnectAttempts = 0
  }

  getWebSocketUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    return `${protocol}//${host}/ws/chat`
  }

  connect(userId) {
    if (this.connected.value) {
      console.log('WebSocket already connected')
      return Promise.resolve()
    }

    this.currentUserId = userId

    return new Promise((resolve, reject) => {
      try {
        // 使用原生WebSocket连接
        const wsUrl = this.getWebSocketUrl()
        console.log('Connecting to chat WebSocket:', wsUrl)

        this.socket = new WebSocket(wsUrl)

        this.socket.onopen = (event) => {
          console.log('Chat WebSocket connected')
          this.connected.value = true
          this.reconnectAttempts = 0
          this.emit('connected', { userId })
          this.resolveConnection()
          resolve()
        }

        this.socket.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data)
            this.handleMessage(data)
          } catch (e) {
            console.warn('Failed to parse message:', e)
          }
        }

        this.socket.onclose = (event) => {
          console.log('Chat WebSocket closed:', event.code, event.reason)
          this.connected.value = false
          this.emit('disconnected', { userId })
          this.scheduleReconnect()
        }

        this.socket.onerror = (error) => {
          console.error('Chat WebSocket error:', error)
          this.emit('error', error)
          reject(error)
        }

      } catch (e) {
        console.error('Failed to connect:', e)
        this.scheduleReconnect()
        reject(e)
      }
    })
  }

  resolveConnection() {
    // 使用STOMP协议
    if (typeof STOMP !== 'undefined') {
      this.stompClient = STOMP.over(this.socket)
      this.stompClient.connect({}, (frame) => {
        console.log('STOMP connected')
        this.setupSubscriptions()
      }, (error) => {
        console.error('STOMP error:', error)
      })
    }
  }

  setupSubscriptions() {
    if (!this.stompClient) return

    // 订阅用户消息队列
    this.stompClient.subscribe('/user/queue/messages', (message) => {
      const data = JSON.parse(message.body)
      this.emit('message', data)
    })

    // 订阅未读消息数更新
    this.stompClient.subscribe('/user/queue/unread', (message) => {
      const data = JSON.parse(message.body)
      this.emit('unread_update', data)
    })
  }

  subscribeConversation(conversationId) {
    if (!this.stompClient) return

    // 取消之前的会话订阅
    this.unsubscribeConversation()

    this.currentConversationId = conversationId

    // 订阅新会话
    const sub = this.stompClient.subscribe(
      `/topic/conversation/${conversationId}`,
      (message) => {
        const data = JSON.parse(message.body)
        this.emit('conversation_message', data)
      }
    )

    this.subscriptions.set('conversation', sub)
  }

  unsubscribeConversation() {
    const sub = this.subscriptions.get('conversation')
    if (sub) {
      sub.unsubscribe()
      this.subscriptions.delete('conversation')
    }
  }

  handleMessage(data) {
    // 处理不同类型的消息
    switch (data.type) {
      case 'new_message':
        this.emit('new_message', data.message)
        break
      case 'unread_update':
        this.emit('unread_update', data)
        break
      case 'user_status':
        this.emit('user_status', data)
        break
      case 'system_notice':
        this.emit('system_notice', data)
        break
      default:
        console.log('Unknown message type:', data.type)
    }
  }

  sendMessage(conversationId, message) {
    if (this.stompClient) {
      this.stompClient.send('/app/chat/send', {}, JSON.stringify({
        conversationId,
        ...message
      }))
      return true
    }
    return false
  }

  joinConversation(conversationId) {
    if (this.stompClient) {
      this.stompClient.send('/app/chat/join', {}, JSON.stringify({
        conversationId,
        userId: this.currentUserId
      }))
    }
  }

  leaveConversation(conversationId) {
    if (this.stompClient) {
      this.stompClient.send('/app/chat/leave', {}, JSON.stringify({
        conversationId,
        userId: this.currentUserId
      }))
    }
    this.unsubscribeConversation()
  }

  disconnect() {
    this.stopReconnect()
    if (this.stompClient) {
      this.stompClient.disconnect()
      this.stompClient = null
    }
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
    this.connected.value = false
    this.subscriptions.clear()
  }

  scheduleReconnect() {
    if (this.reconnectTimer) return
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('Max reconnect attempts reached')
      return
    }

    this.reconnectAttempts++
    console.log(`Scheduling reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      if (this.currentUserId) {
        this.connect(this.currentUserId)
      }
    }, this.reconnectInterval)
  }

  stopReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  // 事件处理
  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = []
    }
    this.listeners[event].push(callback)

    // 返回取消订阅函数
    return () => {
      const callbacks = this.listeners[event]
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
      this.listeners[event] = []
    } else {
      const callbacks = this.listeners[event]
      if (callbacks) {
        const index = callbacks.indexOf(callback)
        if (index > -1) {
          callbacks.splice(index, 1)
        }
      }
    }
  }

  emit(event, data) {
    const callbacks = this.listeners[event]
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(data)
        } catch (e) {
          console.error('Error in listener:', e)
        }
      })
    }
  }

  isConnected() {
    return this.connected.value
  }
}

// 创建全局单例
const chatWsManager = new ChatWebSocketManager()

export { chatWsManager, ChatWebSocketManager }
