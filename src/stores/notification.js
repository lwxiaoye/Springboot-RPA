import { reactive } from 'vue'

// 全局通知状态
export const notificationState = reactive({
  unreadCount: 0,
  stats: { total: 0, collect: 0, temp: 0, user: 0 }
})

// 更新未读数量
export function updateUnreadCount(count) {
  notificationState.unreadCount = count
}

// 更新统计数据
export function updateStats(stats) {
  notificationState.stats = stats
  // 同时更新未读数量
  notificationState.unreadCount = (stats.collect || 0) + (stats.temp || 0) + (stats.user || 0)
}

// 清除未读数量
export function clearUnreadCount() {
  notificationState.unreadCount = 0
}
