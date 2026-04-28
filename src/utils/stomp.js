/**
 * STOMP WebSocket 服务
 * 用于 RPA 协作中枢的实时消息推送
 */

// 使用原生 WebSocket + SockJS 兼容方式
let stompClient = null
let currentSubscription = null

// 连接函数
export const connect = (options = {}) => {
  const { url = '/ws/chat', onConnect, onDisconnect, onError } = options

  if (stompClient && stompClient.connected) {
    console.log('STOMP already connected')
    if (onConnect) onConnect()
    return stompClient
  }

  return new Promise((resolve, reject) => {
    // 动态导入 sockjs 和 stompjs
    Promise.all([
      import('sockjs-client'),
      import('@stomp/stompjs')
    ]).then(([{ default: SockJS }, { Client }]) => {
      const socket = new SockJS(url)

      stompClient = new Client({
        webSocketFactory: () => socket,
        debug: (str) => {
          // 空函数，避免 this.debug is not a function 错误
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 0,
        heartbeatOutgoing: 0,
        onConnect: (frame) => {
          console.log('STOMP connected')
          if (onConnect) onConnect()
          resolve(stompClient)
        },
        onDisconnect: (frame) => {
          console.log('STOMP disconnected')
          if (onDisconnect) onDisconnect()
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame)
          if (onError) onError(frame)
          reject(frame)
        }
      })

      stompClient.activate()
    }).catch(err => {
      console.error('Failed to load SockJS or stompjs:', err)
      reject(err)
    })
  })
}

// 断开连接
export const disconnect = (callback) => {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
    if (callback) callback()
  }
}

// 订阅会话
export const subscribeConversation = (conversationId, callback) => {
  if (!stompClient || !stompClient.connected) {
    console.warn('STOMP not connected, cannot subscribe')
    return null
  }
  
  // 取消之前的订阅
  if (currentSubscription) {
    currentSubscription.unsubscribe()
    currentSubscription = null
  }
  
  const destination = `/topic/conversation/${conversationId}`
  currentSubscription = stompClient.subscribe(destination, (message) => {
    try {
      const body = JSON.parse(message.body)
      callback(body)
    } catch (e) {
      callback(message.body)
    }
  })
  
  return currentSubscription
}

// 通用订阅
export const subscribe = (destination, callback) => {
  if (!stompClient || !stompClient.connected) {
    console.warn('STOMP not connected, cannot subscribe')
    return null
  }
  
  return stompClient.subscribe(destination, (message) => {
    try {
      const body = JSON.parse(message.body)
      callback(body)
    } catch (e) {
      callback(message.body)
    }
  })
}

// 取消订阅
export const unsubscribe = (subscription) => {
  if (subscription) {
    subscription.unsubscribe()
  }
}

// 发送消息
export const send = (destination, body) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({ destination, body: JSON.stringify(body) })
  } else {
    console.warn('STOMP not connected, cannot send message')
  }
}

// 检查连接状态
export const isConnected = () => {
  return stompClient && stompClient.connected
}
