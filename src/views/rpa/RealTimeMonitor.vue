<template>
  <div class="monitor-dashboard">
    <!-- 背景动效 -->
    <div class="bg-effects">
      <div class="grid-lines"></div>
      <div class="glow-orb orb-1"></div>
      <div class="glow-orb orb-2"></div>
      <div class="glow-orb orb-3"></div>
      <div class="particles">
        <div v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></div>
      </div>
    </div>

    <!-- 主内容区 -->
    <main class="dashboard-main">
      <!-- 顶部KPI卡片 -->
      <div class="kpi-section">
        <div
          class="kpi-card-3d"
          v-for="(item, index) in kpiCards"
          :key="index"
          :style="{ '--delay': index * 0.1 + 's' }"
          :class="{ clickable: item.path }"
          @click="item.path && router.push(item.path)"
        >
          <div class="card-inner">
            <div class="card-glow"></div>
            <div class="card-border"></div>
            <div class="card-content">
              <div class="kpi-icon-3d" :style="{ background: item.gradient }">
                <el-icon :size="28"><component :is="item.icon" /></el-icon>
                <div class="icon-glow"></div>
              </div>
              <div class="kpi-data">
                <div class="kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
                <div class="kpi-label">{{ item.label }}</div>
              </div>
              <div class="kpi-chart-mini">
                <svg viewBox="0 0 60 30" class="mini-spark">
                  <defs>
                    <linearGradient :id="'grad-' + index" x1="0%" y1="0%" x2="0%" y2="100%">
                      <stop offset="0%" :style="{ stopColor: item.color, stopOpacity: 0.8 }" />
                      <stop offset="100%" :style="{ stopColor: item.color, stopOpacity: 0.1 }" />
                    </linearGradient>
                  </defs>
                  <path :d="getSparkline(item.sparkData)" :fill="'url(#grad-' + index + ')'" />
                  <path :d="getSparklineLine(item.sparkData)" fill="none" :stroke="item.color" stroke-width="2" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中部图表区 -->
      <div class="charts-section">
        <!-- 左侧大图 - 执行趋势 -->
        <div class="chart-panel main-chart">
          <div class="panel-header-3d">
            <div class="panel-title">
              <span class="title-cn">24小时执行趋势</span>
              <span class="title-en">EXECUTION TREND</span>
            </div>
            <div class="chart-controls">
              <el-radio-group v-model="trendType" size="small" class="type-selector">
                <el-radio-button value="count">执行数</el-radio-button>
                <el-radio-button value="success">成功率</el-radio-button>
                <el-radio-button value="duration">执行时长</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <div class="chart-body">
            <div ref="trendChartRef" class="echarts-container"></div>
          </div>
        </div>

        <!-- 右侧饼图 - 任务分布 -->
        <div class="chart-panel side-chart">
          <div class="panel-header-3d">
            <div class="panel-title">
              <span class="title-cn">任务状态分布</span>
              <span class="title-en">STATUS DISTRIBUTION</span>
            </div>
          </div>
          <div class="chart-body">
            <div ref="pieChartRef" class="echarts-container"></div>
          </div>
        </div>
      </div>

      <!-- 下部图表区 -->
      <div class="bottom-section">
        <!-- 机器人状态雷达图 -->
        <div class="chart-panel">
          <div class="panel-header-3d">
            <div class="panel-title">
              <span class="title-cn">机器人性能监控</span>
              <span class="title-en">ROBOT PERFORMANCE</span>
            </div>
          </div>
          <div class="chart-body">
            <div ref="radarChartRef" class="echarts-container"></div>
          </div>
        </div>

        <!-- 实时任务流 -->
        <div class="task-stream-panel">
          <div class="panel-header-3d">
            <div class="panel-title">
              <span class="title-cn">实时任务流</span>
              <span class="title-en">LIVE TASK STREAM</span>
            </div>
            <div class="live-badge">
              <span class="live-dot"></span>
              <span>LIVE</span>
            </div>
          </div>
          <div class="task-list-3d">
            <TransitionGroup name="task-animate">
              <div v-for="task in taskStream.slice(0, 10)" :key="task.id" class="task-item-3d" :class="task.status">
                <div class="task-status-bar"></div>
                <div class="task-icon">
                  <el-icon v-if="task.status === 'running'" class="spin-icon"><Loading /></el-icon>
                  <el-icon v-else-if="task.status === 'success'"><CircleCheck /></el-icon>
                  <el-icon v-else-if="task.status === 'failed'"><CircleClose /></el-icon>
                  <el-icon v-else><Clock /></el-icon>
                </div>
                <div class="task-info">
                  <div class="task-name">{{ task.name }}</div>
                  <div class="task-meta">
                    <span>{{ task.robot || '待分配' }}</span>
                    <span>{{ task.startTime }}</span>
                  </div>
                </div>
                <div class="task-status-tag" :class="task.status">{{ getStatusText(task.status) }}</div>
              </div>
            </TransitionGroup>
          </div>
        </div>

        <!-- 告警面板 -->
        <div class="alert-panel">
          <div class="panel-header-3d">
            <div class="panel-title">
              <span class="title-cn">智能告警</span>
              <span class="title-en">SMART ALERTS</span>
            </div>
            <el-badge :value="alertCount" :hidden="alertCount === 0" class="alert-badge">
              <span class="alert-icon">
                <el-icon><Bell /></el-icon>
              </span>
            </el-badge>
          </div>
          <div class="alert-list-3d">
            <div v-for="alert in alerts.slice(0, 5)" :key="alert.id" class="alert-item-3d" :class="alert.level">
              <div class="alert-icon-wrap">
                <el-icon v-if="alert.level === 'critical'"><WarningFilled /></el-icon>
                <el-icon v-else-if="alert.level === 'warning'"><Warning /></el-icon>
                <el-icon v-else><InfoFilled /></el-icon>
              </div>
              <div class="alert-content">
                <div class="alert-title">{{ alert.title }}</div>
                <div class="alert-desc">{{ alert.message }}</div>
              </div>
              <div class="alert-time">{{ alert.time }}</div>
            </div>
            <div v-if="alerts.length === 0" class="no-alerts">
              <el-icon size="32"><CircleCheck /></el-icon>
              <span>系统运行正常</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部状态栏 -->
      <div class="status-bar">
        <div class="status-item">
          <div class="status-dot" :class="{ online: kpi.robotsOnline > 0 }"></div>
          <span>在线机器人: {{ kpi.robotsOnline }}/{{ kpi.robotsTotal }}</span>
        </div>
        <div class="status-item">
          <div class="status-dot success"></div>
          <span>今日成功率: {{ kpi.successRate }}%</span>
        </div>
        <div class="status-item">
          <div class="status-dot warning"></div>
          <span>队列积压: {{ kpi.pending }}</span>
        </div>
        <div class="status-item">
          <span>数据刷新: {{ refreshInterval }}s</span>
        </div>
        <div class="status-item version">
          <span>RPA System v2.0</span>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  VideoPlay,
  VideoPause,
  Loading,
  Clock,
  CircleCheck,
  CircleClose,
  Timer,
  TrendCharts,
  Top,
  Bottom,
  Bell,
  Warning,
  WarningFilled,
  InfoFilled,
  Monitor,
  Cpu,
  Odometer,
  DataLine,
  List,
  Document,
  Tickets,
  Key,
  ArrowRight
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { apiGet } from '../../utils/api.js'

const router = useRouter()

// 快捷导航
const activeNav = ref('')
const navItems = [
  { path: '/rpa/tasks', label: '任务调度中心', desc: '查看和管理所有任务', icon: 'List', gradient: 'linear-gradient(135deg, #00f5ff22, #00f5ff44)' },
  { path: '/rpa/robots', label: '机器人管理', desc: '监控机器人运行状态', icon: 'Monitor', gradient: 'linear-gradient(135deg, #a855f722, #a855f744)' },
  { path: '/rpa/processes', label: '流程仓库', desc: '管理自动化流程', icon: 'Document', gradient: 'linear-gradient(135deg, #00ff8822, #00ff8844)' },
  { path: '/rpa/logs', label: '执行日志', desc: '查看任务执行记录', icon: 'Tickets', gradient: 'linear-gradient(135deg, #fbbf2422, #fbbf2444)' },
  { path: '/rpa/credentials', label: '凭据中心', desc: '管理系统凭据', icon: 'Key', gradient: 'linear-gradient(135deg, #3b82f622, #3b82f644)' }
]

const navigateTo = (path) => {
  router.push(path)
}

// ============ 状态 ============
const isAutoRefresh = ref(true)
const refreshInterval = ref(5)
const currentTime = ref('')
const trendType = ref('count')
const alertCount = ref(0)
const wsConnected = ref(false)
const usingRealData = ref(false)
const lastUpdateTime = ref('')

// 图表引用
const trendChartRef = ref(null)
const pieChartRef = ref(null)
const radarChartRef = ref(null)

// 图表实例
let trendChart = null
let pieChart = null
let radarChart = null

// KPI数据
const kpi = reactive({
  running: 0,
  pending: 0,
  successRate: 0,
  avgDuration: 0,
  robotsOnline: 0,
  robotsTotal: 0,
  todayExecutions: 0,
  throughput: 0
})

// KPI卡片配置
const kpiCards = ref([
  { label: '运行中任务', value: 0, icon: 'Loading', color: '#00f5ff', gradient: 'linear-gradient(135deg, #00f5ff22, #00f5ff44)', sparkData: [], path: '/rpa/tasks' },
  { label: '队列积压', value: 0, icon: 'Clock', color: '#ff6b6b', gradient: 'linear-gradient(135deg, #ff6b6b22, #ff6b6b44)', sparkData: [], path: '/rpa/queues' },
  { label: '任务成功率', value: '0%', icon: 'CircleCheck', color: '#00ff88', gradient: 'linear-gradient(135deg, #00ff8822, #00ff8844)', sparkData: [], path: '/rpa/logs' },
  { label: '在线机器人', value: 0, icon: 'Monitor', color: '#a855f7', gradient: 'linear-gradient(135deg, #a855f722, #a855f744)', sparkData: [], path: '/rpa/robots' },
  { label: '今日执行', value: 0, icon: 'Odometer', color: '#fbbf24', gradient: 'linear-gradient(135deg, #fbbf2422, #fbbf2444)', sparkData: [], path: '/rpa/logs' },
  { label: '吞吐量/h', value: 0, icon: 'TrendCharts', color: '#3b82f6', gradient: 'linear-gradient(135deg, #3b82f622, #3b82f644)', sparkData: [], path: '/rpa/reports' }
])

// 任务流
const taskStream = ref([])

// 告警
const alerts = ref([])

// 原始数据存储
let rawTasks = []
let rawRobots = []
let rawLogs = []
let rawQueues = []

// 趋势数据
let trendDataInitialized = false
const trendData = ref([])

// WebSocket实例
let ws = null
let wsReconnectTimer = null

// 定时器
let refreshTimer = null
let clockTimer = null

// ============ 工具函数 ============

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

const updateLastTime = () => {
  const now = new Date()
  lastUpdateTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

const getParticleStyle = (i) => {
  const size = Math.random() * 4 + 2
  return {
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    width: size + 'px',
    height: size + 'px',
    animationDelay: Math.random() * 5 + 's',
    animationDuration: (Math.random() * 10 + 10) + 's'
  }
}

const getSparkline = (data) => {
  if (!data || data.length < 2) return ''
  const max = Math.max(...data)
  const min = Math.min(...data)
  if (max === min) return ''
  const range = max - min || 1
  const points = data.map((v, i) => {
    const x = (i / (data.length - 1)) * 60
    const y = 30 - ((v - min) / range) * 28
    return `${x},${y}`
  })
  return `M0,30 L${points.join(' L')},${points[points.length - 1].split(',')[1]} L60,30 Z`
}

const getSparklineLine = (data) => {
  if (!data || data.length < 2) return ''
  const max = Math.max(...data)
  const min = Math.min(...data)
  if (max === min) return ''
  const range = max - min || 1
  const points = data.map((v, i) => {
    const x = (i / (data.length - 1)) * 60
    const y = 30 - ((v - min) / range) * 28
    return `${x},${y}`
  })
  return `M${points.join(' L')}`
}

const getStatusText = (status) => {
  const map = {
    running: '运行中',
    pending: '等待中',
    assigned: '已分配',
    success: '成功',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

// ============ WebSocket支持 ============

const tryConnectWebSocket = () => {
  try {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/ws/monitor`
    
    ws = new WebSocket(wsUrl)
    
    ws.onopen = () => {
      console.log('WebSocket connected')
      wsConnected.value = true
      
      ws.send(JSON.stringify({
        type: 'subscribe',
        channels: ['monitor', 'tasks', 'robots']
      }))
      
      loadData()
    }
    
    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        handleWebSocketMessage(data)
      } catch (e) {
        console.warn('Failed to parse WebSocket message')
      }
    }
    
    ws.onclose = () => {
      console.log('WebSocket disconnected')
      wsConnected.value = false
      
      if (isAutoRefresh.value) {
        wsReconnectTimer = setTimeout(tryConnectWebSocket, 5000)
      }
    }
    
    ws.onerror = () => {
      console.log('WebSocket not available')
      wsConnected.value = false
      ws.close()
    }
    
    // 8秒超时
    setTimeout(() => {
      if (!wsConnected.value && ws.readyState !== WebSocket.OPEN) {
        console.log('WebSocket connection timeout')
        ws?.close()
      }
    }, 8000)
    
  } catch (e) {
    console.log('WebSocket not supported')
    wsConnected.value = false
  }
}

const handleWebSocketMessage = (data) => {
  switch (data.type) {
    case 'monitor_update':
    case 'task_update':
      if (data.tasks) {
        rawTasks = data.tasks
        updateFromRealData()
      }
      break
    case 'robot_update':
      if (data.robots) {
        rawRobots = data.robots
        updateFromRealData()
      }
      break
    case 'full_sync':
      if (data.tasks) rawTasks = data.tasks
      if (data.robots) rawRobots = data.robots
      if (data.logs) rawLogs = data.logs
      if (data.queues) rawQueues = data.queues
      updateFromRealData()
      break
  }
}

// ============ 数据加载 ============

const loadData = async () => {
  try {
    const [taskRes, robotRes, queueRes, logRes] = await Promise.all([
      apiGet('/task'),
      apiGet('/robot'),
      apiGet('/queue'),
      apiGet('/log?limit=100')
    ])

    const hasRealTasks = taskRes.code === 0 && Array.isArray(taskRes.data) && taskRes.data.length > 0
    const hasRealRobots = robotRes.code === 0 && Array.isArray(robotRes.data)
    const hasRealLogs = logRes.code === 0 && Array.isArray(logRes.data) && logRes.data.length > 0
    const hasRealQueues = queueRes.code === 0 && Array.isArray(queueRes.data)

    if (hasRealTasks || hasRealRobots || hasRealLogs) {
      usingRealData.value = true
      
      if (hasRealTasks) rawTasks = taskRes.data
      if (hasRealRobots) rawRobots = robotRes.data
      if (hasRealLogs) rawLogs = logRes.data
      if (hasRealQueues) rawQueues = queueRes.data
      
      updateFromRealData()
    } else {
      usingRealData.value = false
      updateFromMockData()
    }

    updateLastTime()

  } catch (e) {
    console.warn('Failed to load from API')
    usingRealData.value = false
    updateFromMockData()
  }
}

const updateFromRealData = () => {
  const tasks = Array.isArray(rawTasks) ? rawTasks : []
  
  kpi.running = tasks.filter(t => t.status === 'running').length
  kpi.pending = tasks.filter(t => t.status === 'pending' || t.status === 'assigned').length
  
  const total = tasks.length
  const successCount = tasks.filter(t => t.status === 'success' || t.status === 'completed').length
  kpi.successRate = total > 0 ? Math.round(successCount / total * 100) : 0
  kpi.todayExecutions = tasks.length

  taskStream.value = tasks.slice(0, 20).map(t => ({
    id: t.id,
    name: t.name || t.processName || '未知任务',
    robot: t.robotName || '',
    status: t.status,
    startTime: t.startTime ? new Date(t.startTime).toLocaleTimeString('zh-CN') : '--:--:--'
  }))

  kpiCards.value[0].value = kpi.running
  kpiCards.value[0].sparkData = generateSparkFromValue(kpi.running, 0, 50)
  
  kpiCards.value[1].value = kpi.pending
  kpiCards.value[1].sparkData = generateSparkFromValue(kpi.pending, 0, 100)
  
  kpiCards.value[2].value = kpi.successRate + '%'
  kpiCards.value[2].sparkData = generateSparkFromValue(kpi.successRate, 0, 100)

  const robotList = Array.isArray(rawRobots) ? rawRobots : []
  kpi.robotsTotal = robotList.length
  kpi.robotsOnline = robotList.filter(r => r.status !== 'offline').length

  kpiCards.value[3].value = kpi.robotsOnline
  kpiCards.value[3].sparkData = generateSparkFromValue(kpi.robotsOnline, 0, Math.max(kpi.robotsTotal, 10))

  const logs = Array.isArray(rawLogs) ? rawLogs : []
  kpi.throughput = logs.length > 0 ? Math.round(logs.length / 24 * 10) / 10 : 0

  kpiCards.value[4].value = logs.length
  kpiCards.value[4].sparkData = generateSparkFromValue(logs.length, 0, 500)
  
  kpiCards.value[5].value = kpi.throughput
  kpiCards.value[5].sparkData = generateSparkFromValue(kpi.throughput, 0, 50)

  generateTrendFromLogs(logs)
  generateAlerts()

  // 更新图表
  if (trendChart) updateTrendChart()
  if (pieChart) updatePieChart()
  if (radarChart && robotList.length > 0) updateRadarChart(robotList)
}

const generateSparkFromValue = (value, min, max) => {
  const base = Math.max(min, Math.min(max, value || 0))
  const data = []
  for (let i = 0; i < 12; i++) {
    const variance = (max - min) * 0.15
    data.push(Math.max(min, Math.min(max, base + (Math.random() - 0.5) * variance)))
  }
  data.push(base)
  return data
}

const updateFromMockData = () => {
  if (trendDataInitialized && trendData.value.length > 0) return

  const mockTasks = [
    { id: 1, name: '发票数据采集', status: 'running', robotName: 'Robot-01', startTime: new Date().toLocaleTimeString() },
    { id: 2, name: '财务报表生成', status: 'pending', robotName: '', startTime: '--:--:--' },
    { id: 3, name: '客户信息同步', status: 'success', robotName: 'Robot-02', startTime: new Date().toLocaleTimeString() },
    { id: 4, name: '订单自动处理', status: 'failed', robotName: 'Robot-03', startTime: new Date().toLocaleTimeString() },
    { id: 5, name: '库存数据更新', status: 'running', robotName: 'Robot-04', startTime: new Date().toLocaleTimeString() },
    { id: 6, name: '邮件自动发送', status: 'pending', robotName: '', startTime: '--:--:--' },
    { id: 7, name: '数据清洗任务', status: 'success', robotName: 'Robot-05', startTime: new Date().toLocaleTimeString() },
    { id: 8, name: '报表导出', status: 'running', robotName: 'Robot-01', startTime: new Date().toLocaleTimeString() },
  ]

  taskStream.value = mockTasks
  kpi.running = mockTasks.filter(t => t.status === 'running').length
  kpi.pending = mockTasks.filter(t => t.status === 'pending').length
  kpi.successRate = 85
  kpi.robotsOnline = 4
  kpi.robotsTotal = 6
  kpi.todayExecutions = 128
  kpi.throughput = 12.5

  kpiCards.value[0].value = kpi.running
  kpiCards.value[0].sparkData = [3, 5, 4, 6, 5, 7, 6, 8, 7, 5, 6, 5, kpi.running]
  
  kpiCards.value[1].value = kpi.pending
  kpiCards.value[1].sparkData = [12, 10, 15, 8, 11, 9, 13, 10, 8, 12, 10, 9, kpi.pending]
  
  kpiCards.value[2].value = kpi.successRate + '%'
  kpiCards.value[2].sparkData = [78, 82, 80, 85, 83, 88, 86, 90, 84, 87, 85, 86, kpi.successRate]
  
  kpiCards.value[3].value = kpi.robotsOnline
  kpiCards.value[3].sparkData = [3, 4, 3, 5, 4, 5, 4, 6, 5, 4, 5, 4, kpi.robotsOnline]
  
  kpiCards.value[4].value = kpi.todayExecutions
  kpiCards.value[4].sparkData = [95, 110, 105, 125, 130, 148, 142, 160, 155, 168, 158, 145, kpi.todayExecutions]
  
  kpiCards.value[5].value = kpi.throughput
  kpiCards.value[5].sparkData = [8, 10, 9, 11, 12, 13, 12, 14, 13, 15, 14, 13, kpi.throughput]

  generateMockTrendData()
}

const generateMockTrendData = () => {
  const now = new Date()
  const hours = []
  for (let i = 23; i >= 0; i--) {
    hours.push(new Date(now.getTime() - i * 60 * 60 * 1000).getHours())
  }

  trendData.value = hours.map((h) => {
    const workHours = h >= 8 && h <= 18
    const baseValue = workHours ? 25 : 8
    return {
      hour: h,
      success: Math.round(baseValue * (0.8 + Math.random() * 0.2)),
      failed: Math.round(baseValue * 0.08),
      duration: Math.round(15 + Math.random() * 15)
    }
  })

  trendDataInitialized = true
}

const generateTrendFromLogs = (logs) => {
  if (trendDataInitialized && usingRealData.value) return
  
  const now = new Date()
  const hours = []
  for (let i = 23; i >= 0; i--) {
    hours.push(new Date(now.getTime() - i * 60 * 60 * 1000).getHours())
  }

  const hasData = logs && logs.length > 0
  
  trendData.value = hours.map((h) => {
    if (hasData) {
      const hourLogs = logs.filter(l => {
        if (!l.startTime) return false
        const logHour = new Date(l.startTime).getHours()
        return logHour === h
      })
      const successCount = hourLogs.filter(l => l.status === 'success' || l.status === 'completed').length
      const failedCount = hourLogs.filter(l => l.status === 'failed').length
      return { hour: h, success: successCount, failed: failedCount, duration: Math.round(15 + Math.random() * 10) }
    }
    const workHours = h >= 8 && h <= 18
    const baseValue = workHours ? 25 : 8
    return {
      hour: h,
      success: Math.round(baseValue * (0.8 + Math.random() * 0.2)),
      failed: Math.round(baseValue * 0.08),
      duration: Math.round(15 + Math.random() * 15)
    }
  })

  trendDataInitialized = true
}

const generateAlerts = () => {
  const newAlerts = []

  const failedTasks = taskStream.value.filter(t => t.status === 'failed')
  if (failedTasks.length > 0) {
    newAlerts.push({
      id: 'failed-' + Date.now(),
      level: 'warning',
      title: '任务执行异常',
      message: `${failedTasks.length} 个任务执行失败，请检查`,
      time: '刚刚'
    })
  }

  const offlineCount = kpi.robotsTotal - kpi.robotsOnline
  if (offlineCount > 0) {
    newAlerts.push({
      id: 'offline-' + Date.now(),
      level: 'critical',
      title: '机器人离线',
      message: `${offlineCount} 台机器人已离线`,
      time: '刚刚'
    })
  }

  if (kpi.successRate > 0 && kpi.successRate < 80) {
    newAlerts.push({
      id: 'rate-' + Date.now(),
      level: 'warning',
      title: '成功率预警',
      message: `当前成功率 ${kpi.successRate}%，低于阈值`,
      time: '刚刚'
    })
  }

  if (kpi.pending > 50) {
    newAlerts.push({
      id: 'queue-' + Date.now(),
      level: 'info',
      title: '队列积压',
      message: `任务队列积压 ${kpi.pending} 项`,
      time: '刚刚'
    })
  }

  alerts.value = newAlerts
  alertCount.value = newAlerts.length
}

// ============ 图表更新 ============

const initCharts = () => {
  if (trendChartRef.value && !trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  if (pieChartRef.value && !pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }
  if (radarChartRef.value && !radarChart) {
    radarChart = echarts.init(radarChartRef.value)
  }
}

const updateTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart) initCharts()

  let seriesData = []
  let yAxisName = ''

  if (trendType.value === 'count') {
    seriesData = [
      { name: '成功', data: trendData.value.map(d => d.success) },
      { name: '失败', data: trendData.value.map(d => d.failed) }
    ]
    yAxisName = '执行数'
  } else if (trendType.value === 'success') {
    seriesData = [{
      name: '成功率',
      data: trendData.value.map(d => {
        const total = d.success + d.failed
        return total > 0 ? Math.round(d.success / total * 100) : 0
      })
    }]
    yAxisName = '百分比(%)'
  } else {
    seriesData = [{ name: '执行时长', data: trendData.value.map(d => d.duration) }]
    yAxisName = '分钟'
  }

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 20, 40, 0.9)',
      borderColor: '#00f5ff',
      textStyle: { color: '#fff' }
    },
    legend: {
      data: seriesData.map(s => s.name),
      textStyle: { color: '#888' },
      top: '5%'
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.map(d => d.hour + ':00'),
      axisLine: { lineStyle: { color: '#00f5ff33' } },
      axisLabel: { color: '#888', fontSize: 11 },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      nameTextStyle: { color: '#666', fontSize: 11 },
      axisLine: { lineStyle: { color: '#00f5ff33' } },
      axisLabel: { color: '#888', fontSize: 11 },
      splitLine: { lineStyle: { color: '#ffffff08' } }
    },
    series: seriesData.map((s, index) => ({
      name: s.name,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: {
        color: trendType.value === 'success' ? '#00ff88' : (index === 0 ? '#00ff88' : '#ff6b6b'),
        width: 2,
        shadowBlur: 8
      },
      itemStyle: { color: trendType.value === 'success' ? '#00ff88' : (index === 0 ? '#00ff88' : '#ff6b6b') },
      areaStyle: trendType.value !== 'success' ? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: index === 0 ? '#00ff8833' : '#ff6b6b33' },
          { offset: 1, color: 'transparent' }
        ])
      } : undefined,
      data: s.data
    }))
  }

  trendChart.setOption(option, true)
}

watch(trendType, () => {
  updateTrendChart()
})

const updatePieChart = () => {
  if (!pieChartRef.value) return
  if (!pieChart) initCharts()

  const running = taskStream.value.filter(t => t.status === 'running').length
  const pending = taskStream.value.filter(t => ['pending', 'assigned'].includes(t.status)).length
  const success = taskStream.value.filter(t => ['success', 'completed'].includes(t.status)).length
  const failed = taskStream.value.filter(t => t.status === 'failed').length

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0, 20, 40, 0.9)',
      borderColor: '#00f5ff',
      textStyle: { color: '#fff' }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#888' }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      itemStyle: { borderRadius: 8, borderColor: '#0a1929', borderWidth: 3 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#fff' } },
      labelLine: { show: false },
      data: [
        { value: running || 1, name: '运行中', itemStyle: { color: '#00f5ff' } },
        { value: pending || 1, name: '等待中', itemStyle: { color: '#fbbf24' } },
        { value: success || 1, name: '成功', itemStyle: { color: '#00ff88' } },
        { value: failed || 0, name: '失败', itemStyle: { color: '#ff6b6b' } }
      ]
    }]
  }

  pieChart.setOption(option, true)
}

const updateRadarChart = (robots) => {
  if (!radarChartRef.value) return
  if (!radarChart) initCharts()

  const avgSuccess = robots.length > 0
    ? Math.round(robots.reduce((sum, r) => sum + (r.successRate || 0), 0) / robots.length)
    : 0
  const avgExecutions = robots.length > 0
    ? Math.round(robots.reduce((sum, r) => sum + (r.todayExecutions || 0), 0) / robots.length)
    : 0
  const onlineRate = robots.length > 0 ? Math.round((kpi.robotsOnline / robots.length) * 100) : 0

  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(0, 20, 40, 0.9)', borderColor: '#00f5ff', textStyle: { color: '#fff' } },
    radar: {
      indicator: [
        { name: '在线率', max: 100 },
        { name: '成功率', max: 100 },
        { name: '执行效率', max: 100 },
        { name: '资源利用', max: 100 },
        { name: '稳定性', max: 100 }
      ],
      shape: 'polygon',
      splitNumber: 4,
      axisName: { color: '#888', fontSize: 12 },
      splitLine: { lineStyle: { color: '#00f5ff22' } },
      splitArea: { areaStyle: { color: ['#00f5ff08', '#00f5ff11', '#00f5ff1a', '#00f5ff22'] } },
      axisLine: { lineStyle: { color: '#00f5ff33' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: [onlineRate || 50, avgSuccess || 80, Math.min(avgExecutions, 100) || 70, 75, 85],
        name: '机器人性能',
        lineStyle: { color: '#00f5ff', width: 2 },
        areaStyle: { color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
          { offset: 0, color: '#00f5ff66' },
          { offset: 1, color: '#00f5ff11' }
        ]) },
        itemStyle: { color: '#00f5ff' },
        symbol: 'circle',
        symbolSize: 8
      }]
    }]
  }

  radarChart.setOption(option, true)
}

// ============ 控制函数 ============

const toggleAutoRefresh = () => {
  isAutoRefresh.value = !isAutoRefresh.value
  if (isAutoRefresh.value) {
    ElMessage.success('已开启实时监控')
    loadData()
  } else {
    ElMessage.warning('已暂停实时监控')
  }
}

watch(refreshInterval, () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  if (isAutoRefresh.value && !wsConnected.value) {
    refreshTimer = setInterval(loadData, refreshInterval.value * 1000)
  }
})

const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
  radarChart?.resize()
}

// ============ 生命周期 ============

onMounted(() => {
  updateTime()
  loadData()
  
  clockTimer = setInterval(updateTime, 1000)
  
  // 尝试WebSocket连接
  tryConnectWebSocket()
  
  // 3秒后检查，如果WebSocket未连接，使用HTTP轮询
  setTimeout(() => {
    if (!wsConnected.value) {
      refreshTimer = setInterval(loadData, refreshInterval.value * 1000)
    }
  }, 3000)

  // 延迟初始化图表
  setTimeout(initCharts, 100)
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (clockTimer) clearInterval(clockTimer)
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  if (ws) ws.close()
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
  radarChart?.dispose()
})
</script>

<style scoped>
/* 科技深色主题 */
.monitor-dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0e27 0%, #0d1321 50%, #0f1729 100%);
  color: #e0e6ed;
  padding: 16px 24px;
  position: relative;
  overflow: hidden;
}

/* 背景动效 */
.bg-effects { position: fixed; top: 0; left: 0; right: 0; bottom: 0; pointer-events: none; z-index: 0; }

.grid-lines {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background-image: linear-gradient(rgba(0, 245, 255, 0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(0, 245, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove { 0% { transform: translate(0, 0); } 100% { transform: translate(50px, 50px); } }

.glow-orb {
  position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.3; animation: float 10s ease-in-out infinite;
}
.orb-1 { width: 400px; height: 400px; background: #00f5ff; top: -100px; right: 10%; }
.orb-2 { width: 300px; height: 300px; background: #a855f7; bottom: 20%; left: 5%; animation-delay: -3s; }
.orb-3 { width: 350px; height: 350px; background: #00ff88; top: 40%; right: 30%; animation-delay: -6s; }

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

.particles { position: absolute; top: 0; left: 0; right: 0; bottom: 0; }

.particle {
  position: absolute; background: #00f5ff; border-radius: 50%; animation: particleFloat linear infinite;
  box-shadow: 0 0 10px #00f5ff;
}

@keyframes particleFloat {
  0% { transform: translateY(100vh) scale(0); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translateY(-100vh) scale(1); opacity: 0; }
}

/* 快捷导航 */
.quick-nav {
  position: relative;
  z-index: 1;
  margin-bottom: 16px;
}

.nav-tabs {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(15, 25, 45, 0.9), rgba(10, 20, 40, 0.9));
  border: 1px solid rgba(0, 245, 255, 0.2);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.nav-tab {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  padding: 14px 18px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(0, 245, 255, 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.nav-tab:hover {
  background: rgba(0, 245, 255, 0.08);
  border-color: rgba(0, 245, 255, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 245, 255, 0.15);
}

.nav-tab:hover .nav-arrow {
  opacity: 1;
  transform: translateX(4px);
}

.nav-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 20px;
  color: #00f5ff;
}

.nav-content {
  flex: 1;
  min-width: 0;
}

.nav-label {
  font-size: 14px;
  font-weight: 600;
  color: #e0e6ed;
  margin-bottom: 4px;
}

.nav-desc {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-arrow {
  font-size: 16px;
  color: #00f5ff;
  opacity: 0;
  transition: all 0.3s ease;
}

/* 页面头部 */
.dashboard-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 24px; margin-bottom: 20px;
  background: linear-gradient(90deg, rgba(0, 245, 255, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
  border: 1px solid rgba(0, 245, 255, 0.2); border-radius: 16px;
  position: relative; z-index: 1; backdrop-filter: blur(10px);
}

.header-left { display: flex; align-items: center; gap: 20px; }
.header-center { display: flex; align-items: center; gap: 24px; flex: 1; justify-content: center; }
.header-right { display: flex; align-items: center; gap: 12px; }

/* 3D Logo */
.logo-3d { perspective: 200px; }
.logo-cube { width: 50px; height: 50px; position: relative; transform-style: preserve-3d; animation: cubeRotate 10s linear infinite; }

@keyframes cubeRotate {
  0% { transform: rotateX(-20deg) rotateY(0deg); }
  100% { transform: rotateX(-20deg) rotateY(360deg); }
}

.cube-face {
  position: absolute; width: 50px; height: 50px; background: linear-gradient(135deg, #00f5ff, #0066ff);
  border: 1px solid rgba(0, 245, 255, 0.5); display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; color: #fff;
}
.front { transform: translateZ(25px); }
.back { transform: rotateY(180deg) translateZ(25px); }
.right { transform: rotateY(90deg) translateZ(25px); }
.left { transform: rotateY(-90deg) translateZ(25px); }
.top { transform: rotateX(90deg) translateZ(25px); }
.bottom { transform: rotateX(-90deg) translateZ(25px); }

.title-area h1 {
  font-size: 24px; font-weight: 700; margin: 0;
  background: linear-gradient(90deg, #00f5ff, #a855f7);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.subtitle { font-size: 12px; color: #666; letter-spacing: 2px; }

.time-3d {
  display: flex; flex-direction: column; align-items: center; padding: 12px 24px;
  background: rgba(0, 0, 0, 0.3); border-radius: 12px; border: 1px solid rgba(0, 245, 255, 0.2);
}
.time-label { font-size: 11px; color: #666; text-transform: uppercase; letter-spacing: 2px; }
.time-value { font-size: 20px; font-weight: 700; color: #00f5ff; font-family: 'Consolas', 'Monaco', monospace; text-shadow: 0 0 10px rgba(0, 245, 255, 0.5); }

.data-status { display: flex; align-items: center; gap: 8px; padding: 8px 16px; background: rgba(0, 0, 0, 0.3); border-radius: 20px; font-size: 13px; color: #888; }
.data-status .status-dot { width: 8px; height: 8px; border-radius: 50%; background: #666; }
.data-status .status-dot.connected { background: #00ff88; box-shadow: 0 0 8px #00ff88; animation: pulse 2s infinite; }
.data-status .status-dot.real { background: #00f5ff; }
.data-status .update-time { color: #666; font-size: 11px; }

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

.refresh-select { width: 100px; }
.refresh-select :deep(.el-input__wrapper) { background: rgba(0, 0, 0, 0.3); border: 1px solid rgba(0, 245, 255, 0.3); box-shadow: none; }

.btn-3d { position: relative; overflow: hidden; border: none; background: linear-gradient(135deg, rgba(0, 245, 255, 0.2), rgba(168, 85, 247, 0.2)); color: #00f5ff; transition: all 0.3s; }
.btn-3d:hover { background: linear-gradient(135deg, rgba(0, 245, 255, 0.4), rgba(168, 85, 247, 0.4)); transform: translateY(-2px); box-shadow: 0 5px 20px rgba(0, 245, 255, 0.3); }
.btn-glow { position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent); animation: btnGlow 3s infinite; }
@keyframes btnGlow { 0% { left: -100%; } 50%, 100% { left: 100%; } }

/* 主内容区 */
.dashboard-main { position: relative; z-index: 1; display: flex; flex-direction: column; gap: 16px; }

/* KPI卡片 */
.kpi-section { display: grid; grid-template-columns: repeat(6, 1fr); gap: 16px; }

.kpi-card-3d { perspective: 1000px; animation: fadeInUp 0.6s ease-out forwards; animation-delay: var(--delay); opacity: 0; }

@keyframes fadeInUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }

.card-inner { position: relative; background: linear-gradient(135deg, rgba(15, 25, 45, 0.9), rgba(10, 20, 40, 0.9)); border-radius: 16px; padding: 20px; border: 1px solid rgba(0, 245, 255, 0.2); transform-style: preserve-3d; transition: all 0.3s; overflow: hidden; }
.card-inner:hover { transform: translateY(-5px) rotateX(5deg); border-color: rgba(0, 245, 255, 0.5); box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4), 0 0 30px rgba(0, 245, 255, 0.1); }

.kpi-card-3d.clickable { cursor: pointer; }
.kpi-card-3d.clickable .card-inner { cursor: pointer; }

.card-glow { position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(0, 245, 255, 0.1) 0%, transparent 50%); pointer-events: none; }

.card-border { position: absolute; top: 0; left: 0; right: 0; bottom: 0; border-radius: 16px; padding: 1px; background: linear-gradient(135deg, rgba(0, 245, 255, 0.3), transparent, rgba(168, 85, 247, 0.3)); -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0); mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0); -webkit-mask-composite: xor; mask-composite: exclude; pointer-events: none; }

.card-content { position: relative; display: flex; flex-direction: column; gap: 12px; }

.kpi-icon-3d { width: 50px; height: 50px; border-radius: 12px; display: flex; align-items: center; justify-content: center; position: relative; }
.icon-glow { position: absolute; top: -5px; left: -5px; right: -5px; bottom: -5px; border-radius: 16px; opacity: 0.5; filter: blur(10px); z-index: -1; }

.kpi-data { flex: 1; }
.kpi-value { font-size: 32px; font-weight: 700; line-height: 1; margin-bottom: 4px; text-shadow: 0 0 20px currentColor; }
.kpi-label { font-size: 13px; color: #888; }

.kpi-chart-mini { position: absolute; bottom: 12px; right: 12px; width: 60px; height: 30px; opacity: 0.7; }
.mini-spark { width: 100%; height: 100%; }

/* 图表区 */
.charts-section { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }

.chart-panel { background: linear-gradient(135deg, rgba(15, 25, 45, 0.9), rgba(10, 20, 40, 0.9)); border-radius: 16px; border: 1px solid rgba(0, 245, 255, 0.2); overflow: hidden; transition: all 0.3s; }
.chart-panel:hover { border-color: rgba(0, 245, 255, 0.4); box-shadow: 0 0 30px rgba(0, 245, 255, 0.1); }

.panel-header-3d { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid rgba(0, 245, 255, 0.1); background: rgba(0, 0, 0, 0.2); }
.panel-title { display: flex; flex-direction: column; }
.title-cn { font-size: 15px; font-weight: 600; color: #e0e6ed; }
.title-en { font-size: 10px; color: #555; letter-spacing: 2px; text-transform: uppercase; }

.chart-body { padding: 16px; }
.echarts-container { width: 100%; height: 280px; }

.type-selector :deep(.el-radio-button__inner) { background: rgba(0, 0, 0, 0.3); border-color: rgba(0, 245, 255, 0.2); color: #888; }
.type-selector :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) { background: linear-gradient(135deg, #00f5ff33, #a855f733); border-color: #00f5ff; color: #00f5ff; box-shadow: none; }

/* 底部区域 */
.bottom-section { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px; }

/* 任务流面板 */
.task-stream-panel { background: linear-gradient(135deg, rgba(15, 25, 45, 0.9), rgba(10, 20, 40, 0.9)); border-radius: 16px; border: 1px solid rgba(0, 245, 255, 0.2); overflow: hidden; }

.live-badge { display: flex; align-items: center; gap: 6px; padding: 6px 12px; background: rgba(255, 68, 68, 0.2); border-radius: 20px; font-size: 12px; font-weight: 600; color: #ff6b6b; }
.live-dot { width: 8px; height: 8px; border-radius: 50%; background: #ff6b6b; animation: livePulse 1s infinite; }
@keyframes livePulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.2); } }

.task-list-3d { max-height: 320px; overflow-y: auto; padding: 12px; }

.task-item-3d { display: flex; align-items: center; gap: 12px; padding: 12px 16px; background: rgba(0, 0, 0, 0.3); border-radius: 10px; margin-bottom: 8px; border-left: 3px solid transparent; transition: all 0.3s; cursor: pointer; }
.task-item-3d:hover { background: rgba(0, 245, 255, 0.05); transform: translateX(5px); }
.task-item-3d.running { border-left-color: #00f5ff; }
.task-item-3d.success { border-left-color: #00ff88; }
.task-item-3d.failed { border-left-color: #ff6b6b; }
.task-item-3d.pending { border-left-color: #fbbf24; }

.task-icon { width: 36px; height: 36px; border-radius: 8px; background: rgba(0, 245, 255, 0.1); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.task-item-3d.running .task-icon { color: #00f5ff; }
.task-item-3d.success .task-icon { color: #00ff88; }
.task-item-3d.failed .task-icon { color: #ff6b6b; }
.task-item-3d.pending .task-icon { color: #fbbf24; }

.spin-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.task-info { flex: 1; min-width: 0; }
.task-name { font-size: 13px; font-weight: 500; color: #e0e6ed; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.task-meta { display: flex; gap: 12px; font-size: 11px; color: #666; margin-top: 4px; }

.task-status-tag { padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 500; }
.task-status-tag.running { background: rgba(0, 245, 255, 0.2); color: #00f5ff; }
.task-status-tag.success { background: rgba(0, 255, 136, 0.2); color: #00ff88; }
.task-status-tag.failed { background: rgba(255, 107, 107, 0.2); color: #ff6b6b; }
.task-status-tag.pending { background: rgba(251, 191, 36, 0.2); color: #fbbf24; }

/* 告警面板 */
.alert-panel { background: linear-gradient(135deg, rgba(15, 25, 45, 0.9), rgba(10, 20, 40, 0.9)); border-radius: 16px; border: 1px solid rgba(0, 245, 255, 0.2); overflow: hidden; }
.alert-badge :deep(.el-badge__content) { background: #ff6b6b; }
.alert-icon { width: 36px; height: 36px; border-radius: 8px; background: rgba(255, 107, 107, 0.2); display: flex; align-items: center; justify-content: center; color: #ff6b6b; }

.alert-list-3d { padding: 12px; max-height: 320px; overflow-y: auto; }

.alert-item-3d { display: flex; align-items: flex-start; gap: 12px; padding: 12px; background: rgba(0, 0, 0, 0.3); border-radius: 10px; margin-bottom: 8px; border-left: 3px solid; transition: all 0.3s; cursor: pointer; }
.alert-item-3d:hover { background: rgba(0, 0, 0, 0.5); }
.alert-item-3d.critical { border-left-color: #ff6b6b; }
.alert-item-3d.warning { border-left-color: #fbbf24; }
.alert-item-3d.info { border-left-color: #00f5ff; }

.alert-icon-wrap { width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.alert-item-3d.critical .alert-icon-wrap { background: rgba(255, 107, 107, 0.2); color: #ff6b6b; }
.alert-item-3d.warning .alert-icon-wrap { background: rgba(251, 191, 36, 0.2); color: #fbbf24; }
.alert-item-3d.info .alert-icon-wrap { background: rgba(0, 245, 255, 0.2); color: #00f5ff; }

.alert-content { flex: 1; min-width: 0; }
.alert-title { font-size: 13px; font-weight: 500; color: #e0e6ed; margin-bottom: 4px; }
.alert-desc { font-size: 11px; color: #666; }
.alert-time { font-size: 10px; color: #555; flex-shrink: 0; }

.no-alerts { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; color: #00ff88; gap: 12px; }
.no-alerts span { font-size: 14px; }

/* 状态栏 */
.status-bar { display: flex; justify-content: center; align-items: center; gap: 32px; padding: 12px 24px; background: linear-gradient(90deg, rgba(0, 245, 255, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%); border-radius: 12px; border: 1px solid rgba(0, 245, 255, 0.2); margin-top: 8px; }
.status-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #888; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; background: #666; }
.status-dot.online { background: #00ff88; box-shadow: 0 0 8px #00ff88; }
.status-dot.success { background: #00f5ff; box-shadow: 0 0 8px #00f5ff; }
.status-dot.warning { background: #fbbf24; box-shadow: 0 0 8px #fbbf24; }
.status-item.version { color: #555; font-size: 11px; }

/* 任务动画 */
.task-animate-enter-active, .task-animate-leave-active { transition: all 0.4s ease; }
.task-animate-enter-from { opacity: 0; transform: translateX(-30px); }
.task-animate-leave-to { opacity: 0; transform: translateX(30px); }
.task-animate-move { transition: transform 0.4s ease; }

/* 滚动条 */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.2); border-radius: 3px; }
::-webkit-scrollbar-thumb { background: rgba(0, 245, 255, 0.3); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: rgba(0, 245, 255, 0.5); }

/* 响应式 */
@media (max-width: 1600px) { .kpi-section { grid-template-columns: repeat(3, 1fr); } .bottom-section { grid-template-columns: 1fr 1fr; } .alert-panel { grid-column: span 2; } }
@media (max-width: 1200px) { .charts-section { grid-template-columns: 1fr; } .bottom-section { grid-template-columns: 1fr; } .alert-panel { grid-column: span 1; } }
@media (max-width: 768px) { .kpi-section { grid-template-columns: repeat(2, 1fr); } .dashboard-header { flex-direction: column; gap: 16px; } .header-left, .header-right { width: 100%; justify-content: center; } }
</style>
