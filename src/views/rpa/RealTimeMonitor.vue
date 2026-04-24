<template>
  <div class="monitor-page">
    <div class="monitor-header">
      <h2>实时监控</h2>
      <div class="header-actions">
        <el-button :type="isAutoRefresh ? 'primary' : 'default'" @click="toggleAutoRefresh">
          <span :class="['status-dot', { active: isAutoRefresh }]"></span>
          {{ isAutoRefresh ? '自动刷新中' : '已暂停' }}
        </el-button>
        <el-button @click="loadData">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 4v6h-6M1 20v-6h6"/>
            <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
          </svg>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 实时统计卡片 -->
    <div class="stats-row">
      <div class="stat-item running">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.running }}</span>
          <span class="stat-label">运行中</span>
        </div>
      </div>
      <div class="stat-item pending">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.pending }}</span>
          <span class="stat-label">等待中</span>
        </div>
      </div>
      <div class="stat-item success">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.success }}</span>
          <span class="stat-label">成功</span>
        </div>
      </div>
      <div class="stat-item failed">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.failed }}</span>
          <span class="stat-label">失败</span>
        </div>
      </div>
    </div>

    <!-- 监控内容区 -->
    <div class="monitor-content">
      <!-- 左侧: 实时任务流 -->
      <div class="panel task-stream">
        <div class="panel-header">
          <h3>实时任务流</h3>
          <el-tag size="small" type="success" effect="dark">
            <span class="live-dot"></span>
            LIVE
          </el-tag>
        </div>
        <div class="panel-body">
          <div class="stream-list" ref="streamListRef">
            <div
              v-for="task in taskStream"
              :key="task.id"
              class="stream-item"
              :class="task.status"
            >
              <div class="stream-icon">
                <svg v-if="task.status === 'running'" class="spin" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" stroke-dasharray="60" stroke-dashoffset="20"/>
                </svg>
                <svg v-else-if="task.status === 'success'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
                <svg v-else-if="task.status === 'failed'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                </svg>
              </div>
              <div class="stream-info">
                <span class="stream-name">{{ task.name }}</span>
                <span class="stream-robot">{{ task.robot }}</span>
              </div>
              <div class="stream-time">{{ task.time }}</div>
            </div>
            <div v-if="taskStream.length === 0" class="empty-stream">
              <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
              </svg>
              <span>暂无任务执行</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧: 机器人状态 -->
      <div class="panel robot-panel">
        <div class="panel-header">
          <h3>机器人状态</h3>
          <span class="robot-count">{{ robots.length }} 台</span>
        </div>
        <div class="panel-body">
          <div class="robot-list">
            <div
              v-for="robot in robots"
              :key="robot.id"
              class="robot-item"
              :class="robot.status"
            >
              <div class="robot-avatar">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="3" width="20" height="14" rx="2"/>
                  <line x1="8" y1="21" x2="16" y2="21"/>
                  <line x1="12" y1="17" x2="12" y2="21"/>
                </svg>
              </div>
              <div class="robot-info">
                <span class="robot-name">{{ robot.name }}</span>
                <span class="robot-task">{{ robot.currentTask || '空闲' }}</span>
              </div>
              <div class="robot-status">
                <span class="status-badge" :class="robot.status">
                  {{ getStatusText(robot.status) }}
                </span>
              </div>
            </div>
            <div v-if="robots.length === 0" class="empty-robots">
              <span>暂无机器人</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { apiGet } from '../../utils/api.js'

const isAutoRefresh = ref(true)
const streamListRef = ref(null)

const stats = reactive({
  running: 0,
  pending: 0,
  success: 0,
  failed: 0
})

const taskStream = ref([])
const robots = ref([])

const getStatusText = (status) => {
  const map = {
    idle: '空闲',
    busy: '忙碌',
    running: '运行中',
    offline: '离线',
    error: '异常'
  }
  return map[status] || status
}

const loadData = async () => {
  try {
    const [taskRes, robotRes] = await Promise.all([
      apiGet('/task').catch(() => ({ code: -1, data: [] })),
      apiGet('/robot').catch(() => ({ code: -1, data: [] }))
    ])

    if (taskRes.code === 0 && taskRes.data) {
      const tasks = taskRes.data
      stats.running = tasks.filter(t => t.status === 'running').length
      stats.pending = tasks.filter(t => t.status === 'pending' || t.status === 'assigned').length
      stats.success = tasks.filter(t => t.status === 'success' || t.status === 'completed').length
      stats.failed = tasks.filter(t => t.status === 'failed').length

      taskStream.value = tasks.slice(0, 20).map(t => ({
        id: t.id,
        name: t.name || t.processName || '未知任务',
        robot: t.robotName || '-',
        status: t.status,
        time: t.startTime ? new Date(t.startTime).toLocaleTimeString('zh-CN') : new Date().toLocaleTimeString('zh-CN')
      }))
    }

    if (robotRes.code === 0 && robotRes.data) {
      robots.value = robotRes.data.map(r => ({
        id: r.id,
        name: r.name || `Robot-${r.id}`,
        status: r.status || 'offline',
        currentTask: r.currentTask || ''
      }))
    }
  } catch (e) {
    console.error('加载监控数据失败:', e)
  }
}

const toggleAutoRefresh = () => {
  isAutoRefresh.value = !isAutoRefresh.value
}

let refreshTimer = null

onMounted(() => {
  loadData()
  refreshTimer = setInterval(() => {
    if (isAutoRefresh.value) {
      loadData()
    }
  }, 5000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.monitor-page {
  padding: 20px 24px;
  background: var(--bg-page);
  min-height: 100vh;
  color: var(--text-primary);
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.monitor-header h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions .el-button {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #64748b;
}

.status-dot.active {
  background: #22c55e;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.2); }
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-item {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-item.running .stat-icon { background: rgba(59, 130, 246, 0.15); color: #3b82f6; }
.stat-item.pending .stat-icon { background: rgba(245, 158, 11, 0.15); color: #f59e0b; }
.stat-item.success .stat-icon { background: rgba(34, 197, 94, 0.15); color: #22c55e; }
.stat-item.failed .stat-icon { background: rgba(239, 68, 68, 0.15); color: #ef4444; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.stat-item.running .stat-num { color: #3b82f6; }
.stat-item.pending .stat-num { color: #f59e0b; }
.stat-item.success .stat-num { color: #22c55e; }
.stat-item.failed .stat-num { color: #ef4444; }

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 内容区 */
.monitor-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 16px;
}

.panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border);
}

.panel-header h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0;
}

.live-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  margin-right: 4px;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.robot-count {
  font-size: 13px;
  color: var(--text-secondary);
}

.panel-body {
  padding: 12px;
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

/* 任务流 */
.stream-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stream-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--bg-hover);
  border-radius: 6px;
  border-left: 3px solid transparent;
}

.stream-item.running { border-left-color: #3b82f6; }
.stream-item.success { border-left-color: #22c55e; }
.stream-item.failed { border-left-color: #ef4444; }
.stream-item.pending, .stream-item.assigned { border-left-color: #f59e0b; }

.stream-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  flex-shrink: 0;
}

.stream-item.running .stream-icon { color: #3b82f6; }
.stream-item.success .stream-icon { color: #22c55e; }
.stream-item.failed .stream-icon { color: #ef4444; }
.stream-item.pending .stream-icon, .stream-item.assigned .stream-icon { color: #f59e0b; }

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.stream-info {
  flex: 1;
  min-width: 0;
}

.stream-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stream-robot {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.stream-time {
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
  font-family: 'Consolas', monospace;
}

.empty-stream, .empty-robots {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: var(--text-muted);
  gap: 10px;
}

.empty-stream svg {
  opacity: 0.3;
}

/* 机器人列表 */
.robot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.robot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-hover);
  border-radius: 6px;
  border: 1px solid transparent;
}

.robot-item:hover {
  border-color: var(--border);
}

.robot-item.idle { border-left: 3px solid #22c55e; }
.robot-item.busy, .robot-item.running { border-left: 3px solid #3b82f6; }
.robot-item.offline { border-left: 3px solid #64748b; }
.robot-item.error { border-left: 3px solid #ef4444; }

.robot-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
}

.robot-item.idle .robot-avatar { color: #22c55e; }
.robot-item.busy .robot-avatar, .robot-item.running .robot-avatar { color: #3b82f6; }
.robot-item.offline .robot-avatar { color: #64748b; }
.robot-item.error .robot-avatar { color: #ef4444; }

.robot-info {
  flex: 1;
  min-width: 0;
}

.robot-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
}

.robot-task {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-badge {
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.status-badge.idle { background: rgba(34, 197, 94, 0.15); color: #22c55e; }
.status-badge.busy, .status-badge.running { background: rgba(59, 130, 246, 0.15); color: #3b82f6; }
.status-badge.offline { background: rgba(100, 116, 139, 0.15); color: #64748b; }
.status-badge.error { background: rgba(239, 68, 68, 0.15); color: #ef4444; }

/* 滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #475569;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .monitor-content {
    grid-template-columns: 1fr;
  }
}
</style>
