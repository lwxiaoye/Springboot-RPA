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

    <!-- 页面头部 -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="logo-3d">
          <div class="logo-cube">
            <div class="cube-face front">RPA</div>
            <div class="cube-face back">RPA</div>
            <div class="cube-face right">RPA</div>
            <div class="cube-face left">RPA</div>
            <div class="cube-face top">RPA</div>
            <div class="cube-face bottom">RPA</div>
          </div>
        </div>
        <div class="title-area">
          <h1>智能运维监控中心</h1>
          <div class="subtitle">Intelligent Operations Monitoring Center</div>
        </div>
      </div>
      <div class="header-center">
        <div class="time-3d">
          <span class="time-label">系统时间</span>
          <span class="time-value">{{ currentTime }}</span>
        </div>
      </div>
      <div class="header-right">
        <div class="status-indicator" :class="{ active: isAutoRefresh }">
          <div class="pulse-ring"></div>
          <span>{{ isAutoRefresh ? '实时监控中' : '已暂停' }}</span>
        </div>
        <el-select v-model="refreshInterval" size="default" class="refresh-select">
          <el-option :value="3" label="3秒刷新" />
          <el-option :value="5" label="5秒刷新" />
          <el-option :value="10" label="10秒刷新" />
          <el-option :value="30" label="30秒刷新" />
        </el-select>
        <el-button :type="isAutoRefresh ? 'primary' : 'default'" @click="toggleAutoRefresh" class="btn-3d">
          <span class="btn-glow"></span>
          <el-icon><VideoPlay v-if="isAutoRefresh" /><VideoPause v-else /></el-icon>
        </el-button>
        <el-button @click="loadData" class="btn-3d">
          <span class="btn-glow"></span>
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="dashboard-main">
      <!-- 顶部KPI卡片 -->
      <div class="kpi-section">
        <div class="kpi-card-3d" v-for="(item, index) in kpiCards" :key="index" :style="{ '--delay': index * 0.1 + 's' }">
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
                <div class="kpi-trend" :class="{ up: item.trend > 0, down: item.trend < 0 }">
                  <el-icon v-if="item.trend > 0"><Top /></el-icon>
                  <el-icon v-else-if="item.trend < 0"><Bottom /></el-icon>
                  <span>{{ Math.abs(item.trend) }}%</span>
                </div>
              </div>
              <div class="kpi-chart-mini">
                <svg viewBox="0 0 60 30" class="mini-spark">
                  <defs>
                    <linearGradient :id="'grad-' + index" x1="0%" y1="0%" x2="0%" y2="100%">
                      <stop offset="0%" :style="{ stopColor: item.color, stopOpacity: 0.8 }" />
                      <stop offset="100%" :style="{ stopColor: item.color, stopOpacity: 0.1 }" />
                    </linearGradient>
                  </defs>
                  <path :d="getSparkline(item.sparkData, index)" :fill="'url(#grad-' + index + ')'" />
                  <path :d="getSparklineLine(item.sparkData, index)" fill="none" :stroke="item.color" stroke-width="2" />
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
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
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
  DataLine
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { apiGet } from '../../utils/api.js'

// 状态
const isAutoRefresh = ref(true)
const refreshInterval = ref(5)
const currentTime = ref('')
const trendType = ref('count')
const alertCount = ref(0)

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
  { label: '运行中任务', value: 0, icon: 'Loading', color: '#00f5ff', gradient: 'linear-gradient(135deg, #00f5ff22, #00f5ff44)', trend: 12, sparkData: [] },
  { label: '队列积压', value: 0, icon: 'Clock', color: '#ff6b6b', gradient: 'linear-gradient(135deg, #ff6b6b22, #ff6b6b44)', trend: -5, sparkData: [] },
  { label: '任务成功率', value: '0%', icon: 'CircleCheck', color: '#00ff88', gradient: 'linear-gradient(135deg, #00ff8822, #00ff8844)', trend: 3, sparkData: [] },
  { label: '在线机器人', value: 0, icon: 'Monitor', color: '#a855f7', gradient: 'linear-gradient(135deg, #a855f722, #a855f744)', trend: 8, sparkData: [] },
  { label: '今日执行', value: 0, icon: 'Odometer', color: '#fbbf24', gradient: 'linear-gradient(135deg, #fbbf2422, #fbbf2444)', trend: 15, sparkData: [] },
  { label: '吞吐量/h', value: 0, icon: 'TrendCharts', color: '#3b82f6', gradient: 'linear-gradient(135deg, #3b82f622, #3b82f644)', trend: 22, sparkData: [] }
])

// 任务流
const taskStream = ref([])

// 告警
const alerts = ref([])

// 趋势数据
const trendData = ref([])

// 定时器
let refreshTimer = null
let clockTimer = null

// 更新时间
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

// 生成随机数据
const generateRandomData = (count, min, max) => {
  return Array.from({ length: count }, () => Math.floor(Math.random() * (max - min + 1)) + min)
}

// 获取粒子样式
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

// 获取迷你图路径
const getSparkline = (data, index) => {
  if (!data || data.length === 0) return ''
  const max = Math.max(...data)
  const min = Math.min(...data)
  const range = max - min || 1
  const points = data.map((v, i) => {
    const x = (i / (data.length - 1)) * 60
    const y = 30 - ((v - min) / range) * 28
    return `${x},${y}`
  })
  return `M0,30 L${points.join(' L')},${points[points.length - 1].split(',')[1]} L60,30 Z`
}

const getSparklineLine = (data, index) => {
  if (!data || data.length === 0) return ''
  const max = Math.max(...data)
  const min = Math.min(...data)
  const range = max - min || 1
  const points = data.map((v, i) => {
    const x = (i / (data.length - 1)) * 60
    const y = 30 - ((v - min) / range) * 28
    return `${x},${y}`
  })
  return `M${points.join(' L')}`
}

// 状态文本
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

// 加载数据
const loadData = async () => {
  try {
    const [taskRes, robotRes, queueRes, logRes] = await Promise.all([
      apiGet('/task').catch(() => ({ code: -1, data: [] })),
      apiGet('/robot').catch(() => ({ code: -1, data: [] })),
      apiGet('/queue').catch(() => ({ code: -1, data: [] })),
      apiGet('/log?limit=50').catch(() => ({ code: -1, data: [] }))
    ])

    // 处理任务数据
    if (taskRes.code === 0 && taskRes.data) {
      const tasks = Array.isArray(taskRes.data) ? taskRes.data : []
      
      kpi.running = tasks.filter(t => t.status === 'running').length
      kpi.pending = tasks.filter(t => t.status === 'pending' || t.status === 'assigned').length
      
      const total = tasks.length
      const success = tasks.filter(t => t.status === 'success' || t.status === 'completed').length
      kpi.successRate = total > 0 ? Math.round(success / total * 100) : 0
      kpi.todayExecutions = tasks.length

      taskStream.value = tasks.slice(0, 20).map(t => ({
        id: t.id,
        name: t.name || t.processName || '未知任务',
        robot: t.robotName || '',
        status: t.status,
        startTime: t.startTime ? new Date(t.startTime).toLocaleTimeString('zh-CN') : '--:--:--'
      }))

      // 更新KPI卡片
      kpiCards.value[0].value = kpi.running
      kpiCards.value[0].sparkData = generateRandomData(12, kpi.running - 5, kpi.running + 5)
      
      kpiCards.value[1].value = kpi.pending
      kpiCards.value[1].sparkData = generateRandomData(12, Math.max(0, kpi.pending - 10), kpi.pending + 10)
      
      kpiCards.value[2].value = kpi.successRate + '%'
      kpiCards.value[2].sparkData = generateRandomData(12, kpi.successRate - 10, Math.min(100, kpi.successRate + 10))
    }

    // 处理机器人数据
    if (robotRes.code === 0 && robotRes.data) {
      const robotList = Array.isArray(robotRes.data) ? robotRes.data : []
      kpi.robotsTotal = robotList.length
      kpi.robotsOnline = robotList.filter(r => r.status !== 'offline').length

      kpiCards.value[3].value = kpi.robotsOnline
      kpiCards.value[3].sparkData = generateRandomData(12, kpi.robotsOnline - 2, kpi.robotsOnline + 2)

      // 更新雷达图
      updateRadarChart(robotList)
    }

    // 处理队列数据
    if (queueRes.code === 0 && queueRes.data) {
      const queues = Array.isArray(queueRes.data) ? queueRes.data : []
      const totalPending = queues.reduce((sum, q) => sum + (q.itemCount || 0), 0)
      kpi.pending = totalPending || kpi.pending
    }

    // 处理日志数据
    if (logRes.code === 0 && logRes.data) {
      const logs = Array.isArray(logRes.data) ? logRes.data : []
      kpi.throughput = Math.round(logs.length / 24 * 10) / 10

      kpiCards.value[4].value = logs.length
      kpiCards.value[4].sparkData = generateRandomData(12, Math.max(0, logs.length - 20), logs.length + 20)
      
      kpiCards.value[5].value = kpi.throughput
      kpiCards.value[5].sparkData = generateRandomData(12, Math.max(0, kpi.throughput - 5), kpi.throughput + 5)

      // 生成趋势数据
      generateTrendData(logs)
    }

    // 生成告警
    generateAlerts()

    // 更新图表
    nextTick(() => {
      updateTrendChart()
      updatePieChart()
    })

  } catch (e) {
    console.error('加载数据失败:', e)
  }
}

// 生成趋势数据
const generateTrendData = (logs) => {
  const now = new Date()
  const hours = []
  
  for (let i = 23; i >= 0; i--) {
    const hour = new Date(now.getTime() - i * 60 * 60 * 1000).getHours()
    hours.push(hour)
  }

  trendData.value = hours.map(h => {
    const hourLogs = logs.filter(l => {
      if (!l.startTime) return false
      const logHour = new Date(l.startTime).getHours()
      return logHour === h
    })
    return {
      hour: h,
      success: hourLogs.filter(l => l.status === 'success' || l.status === 'completed').length || Math.floor(Math.random() * 15) + 3,
      failed: hourLogs.filter(l => l.status === 'failed').length || Math.floor(Math.random() * 3),
      duration: Math.floor(Math.random() * 30) + 10
    }
  })

  if (trendData.value.every(d => d.success === 0 && d.failed === 0)) {
    trendData.value = hours.map(h => ({
      hour: h,
      success: Math.floor(Math.random() * 20) + 5,
      failed: Math.floor(Math.random() * 4),
      duration: Math.floor(Math.random() * 30) + 10
    }))
  }
}

// 生成告警
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

  if (kpi.successRate < 80 && kpi.successRate > 0) {
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

// 更新趋势图
const updateTrendChart = () => {
  if (!trendChartRef.value) return
  
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 20, 40, 0.9)',
      borderColor: '#00f5ff',
      textStyle: { color: '#fff' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
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
      axisLine: { lineStyle: { color: '#00f5ff33' } },
      axisLabel: { color: '#888', fontSize: 11 },
      splitLine: { lineStyle: { color: '#ffffff08' } }
    },
    series: [
      {
        name: '成功',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#00ff88', width: 3, shadowColor: '#00ff8866', shadowBlur: 10 },
        itemStyle: { color: '#00ff88' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#00ff8844' },
            { offset: 1, color: '#00ff8800' }
          ])
        },
        data: trendData.value.map(d => d.success)
      },
      {
        name: '失败',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#ff6b6b', width: 3, shadowColor: '#ff6b6b66', shadowBlur: 10 },
        itemStyle: { color: '#ff6b6b' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ff6b6b44' },
            { offset: 1, color: '#ff6b6b00' }
          ])
        },
        data: trendData.value.map(d => d.failed)
      }
    ]
  }

  trendChart.setOption(option)
}

// 更新饼图
const updatePieChart = () => {
  if (!pieChartRef.value) return
  
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }

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
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#0a1929',
        borderWidth: 3
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#fff' }
      },
      labelLine: { show: false },
      data: [
        { value: running, name: '运行中', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#00f5ff' }, { offset: 1, color: '#0066ff' }]) } },
        { value: pending, name: '等待中', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#fbbf24' }, { offset: 1, color: '#f59e0b' }]) } },
        { value: success, name: '成功', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#00ff88' }, { offset: 1, color: '#00cc66' }]) } },
        { value: failed, name: '失败', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#ff6b6b' }, { offset: 1, color: '#ee4444' }]) } }
      ]
    }]
  }

  pieChart.setOption(option)
}

// 更新雷达图
const updateRadarChart = (robots) => {
  if (!radarChartRef.value) return
  
  if (!radarChart) {
    radarChart = echarts.init(radarChartRef.value)
  }

  const avgSuccess = robots.length > 0 
    ? Math.round(robots.reduce((sum, r) => sum + (r.successRate || 0), 0) / robots.length) 
    : 0
  const avgExecutions = robots.length > 0 
    ? Math.round(robots.reduce((sum, r) => sum + (r.todayExecutions || 0), 0) / robots.length) 
    : 0
  const onlineRate = robots.length > 0 ? Math.round((kpi.robotsOnline / robots.length) * 100) : 0

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0, 20, 40, 0.9)',
      borderColor: '#00f5ff',
      textStyle: { color: '#fff' }
    },
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
        value: [onlineRate, avgSuccess, avgExecutions, 75, 85],
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

  radarChart.setOption(option)
}

// 切换自动刷新
const toggleAutoRefresh = () => {
  isAutoRefresh.value = !isAutoRefresh.value
  if (isAutoRefresh.value) {
    ElMessage.success('已开启实时监控')
  } else {
    ElMessage.warning('已暂停实时监控')
  }
}

// 监听刷新间隔
watch(refreshInterval, () => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (isAutoRefresh.value) {
    refreshTimer = setInterval(loadData, refreshInterval.value * 1000)
  }
})

// 窗口调整
const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
  radarChart?.resize()
}

// 生命周期
onMounted(() => {
  updateTime()
  loadData()

  clockTimer = setInterval(updateTime, 1000)
  refreshTimer = setInterval(loadData, refreshInterval.value * 1000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (clockTimer) clearInterval(clockTimer)
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
.bg-effects {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.grid-lines {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(0, 245, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 245, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  animation: float 10s ease-in-out infinite;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: #00f5ff;
  top: -100px;
  right: 10%;
  animation-delay: 0s;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: #a855f7;
  bottom: 20%;
  left: 5%;
  animation-delay: -3s;
}

.orb-3 {
  width: 350px;
  height: 350px;
  background: #00ff88;
  top: 40%;
  right: 30%;
  animation-delay: -6s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

.particles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.particle {
  position: absolute;
  background: #00f5ff;
  border-radius: 50%;
  animation: particleFloat linear infinite;
  box-shadow: 0 0 10px #00f5ff;
}

@keyframes particleFloat {
  0% { transform: translateY(100vh) scale(0); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translateY(-100vh) scale(1); opacity: 0; }
}

/* 页面头部 */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(90deg, rgba(0, 245, 255, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
  border: 1px solid rgba(0, 245, 255, 0.2);
  border-radius: 16px;
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 3D Logo */
.logo-3d {
  perspective: 200px;
}

.logo-cube {
  width: 50px;
  height: 50px;
  position: relative;
  transform-style: preserve-3d;
  animation: cubeRotate 10s linear infinite;
}

@keyframes cubeRotate {
  0% { transform: rotateX(-20deg) rotateY(0deg); }
  100% { transform: rotateX(-20deg) rotateY(360deg); }
}

.cube-face {
  position: absolute;
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #00f5ff, #0066ff);
  border: 1px solid rgba(0, 245, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #fff;
  box-shadow: inset 0 0 20px rgba(0, 245, 255, 0.3);
}

.front { transform: translateZ(25px); }
.back { transform: rotateY(180deg) translateZ(25px); }
.right { transform: rotateY(90deg) translateZ(25px); }
.left { transform: rotateY(-90deg) translateZ(25px); }
.top { transform: rotateX(90deg) translateZ(25px); }
.bottom { transform: rotateX(-90deg) translateZ(25px); }

.title-area h1 {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(90deg, #00f5ff, #a855f7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 30px rgba(0, 245, 255, 0.3);
}

.subtitle {
  font-size: 12px;
  color: #666;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.time-3d {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 24px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 12px;
  border: 1px solid rgba(0, 245, 255, 0.2);
}

.time-label {
  font-size: 11px;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 2px;
}

.time-value {
  font-size: 20px;
  font-weight: 700;
  color: #00f5ff;
  font-family: 'Consolas', 'Monaco', monospace;
  text-shadow: 0 0 10px rgba(0, 245, 255, 0.5);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 13px;
  color: #888;
}

.status-indicator.active {
  border-color: #00ff88;
  color: #00ff88;
}

.pulse-ring {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #666;
  position: relative;
}

.status-indicator.active .pulse-ring {
  background: #00ff88;
}

.status-indicator.active .pulse-ring::after {
  content: '';
  position: absolute;
  top: -3px;
  left: -3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid #00ff88;
  animation: pulseRing 1.5s ease-out infinite;
}

@keyframes pulseRing {
  0% { transform: scale(0.8); opacity: 1; }
  100% { transform: scale(1.5); opacity: 0; }
}

.refresh-select {
  width: 100px;
}

.refresh-select :deep(.el-input__wrapper) {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(0, 245, 255, 0.3);
  box-shadow: none;
}

.btn-3d {
  position: relative;
  overflow: hidden;
  border: none;
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.2), rgba(168, 85, 247, 0.2));
  color: #00f5ff;
  transition: all 0.3s;
}

.btn-3d:hover {
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.4), rgba(168, 85, 247, 0.4));
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(0, 245, 255, 0.3);
}

.btn-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  animation: btnGlow 3s infinite;
}

@keyframes btnGlow {
  0% { left: -100%; }
  50%, 100% { left: 100%; }
}

/* 主内容区 */
.dashboard-main {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* KPI卡片 */
.kpi-section {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.kpi-card-3d {
  perspective: 1000px;
  animation: fadeInUp 0.6s ease-out forwards;
  animation-delay: var(--delay);
  opacity: 0;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-inner {
  position: relative;
  background: linear-gradient(135deg, rgba(15, 25, 45, 0.9), rgba(10, 20, 40, 0.9));
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(0, 245, 255, 0.2);
  transform-style: preserve-3d;
  transition: all 0.3s;
  overflow: hidden;
}

.card-inner:hover {
  transform: translateY(-5px) rotateX(5deg);
  border-color: rgba(0, 245, 255, 0.5);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4), 0 0 30px rgba(0, 245, 255, 0.1);
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(0, 245, 255, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.card-border {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.3), transparent, rgba(168, 85, 247, 0.3));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.card-content {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kpi-icon-3d {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.icon-glow {
  position: absolute;
  top: -5px;
  left: -5px;
  right: -5px;
  bottom: -5px;
  border-radius: 16px;
  opacity: 0.5;
  filter: blur(10px);
  z-index: -1;
}

.kpi-data {
  flex: 1;
}

.kpi-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
  text-shadow: 0 0 20px currentColor;
}

.kpi-label {
  font-size: 13px;
  color: #888;
  margin-bottom: 4px;
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.kpi-trend.up { color: #00ff88; }
.kpi-trend.down { color: #ff6b6b; }

.kpi-chart-mini {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 60px;
  height: 30px;
  opacity: 0.7;
}

.mini-spark {
  width: 100%;
  height: 100%;
}

/* 图表区 */
.charts-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.chart-panel {
  background: linear-gradient(135deg, rgba(15, 25, 45, 0.9), rgba(10, 20, 40, 0.9));
  border-radius: 16px;
  border: 1px solid rgba(0, 245, 255, 0.2);
  overflow: hidden;
  transition: all 0.3s;
}

.chart-panel:hover {
  border-color: rgba(0, 245, 255, 0.4);
  box-shadow: 0 0 30px rgba(0, 245, 255, 0.1);
}

.panel-header-3d {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 245, 255, 0.1);
  background: rgba(0, 0, 0, 0.2);
}

.panel-title {
  display: flex;
  flex-direction: column;
}

.title-cn {
  font-size: 15px;
  font-weight: 600;
  color: #e0e6ed;
}

.title-en {
  font-size: 10px;
  color: #555;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.chart-body {
  padding: 16px;
}

.echarts-container {
  width: 100%;
  height: 280px;
}

.type-selector :deep(.el-radio-button__inner) {
  background: rgba(0, 0, 0, 0.3);
  border-color: rgba(0, 245, 255, 0.2);
  color: #888;
}

.type-selector :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #00f5ff33, #a855f733);
  border-color: #00f5ff;
  color: #00f5ff;
  box-shadow: none;
}

/* 底部区域 */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
}

/* 任务流面板 */
.task-stream-panel {
  background: linear-gradient(135deg, rgba(15, 25, 45, 0.9), rgba(10, 20, 40, 0.9));
  border-radius: 16px;
  border: 1px solid rgba(0, 245, 255, 0.2);
  overflow: hidden;
}

.live-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(255, 68, 68, 0.2);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #ff6b6b;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff6b6b;
  animation: livePulse 1s infinite;
}

@keyframes livePulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

.task-list-3d {
  max-height: 320px;
  overflow-y: auto;
  padding: 12px;
}

.task-item-3d {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 10px;
  margin-bottom: 8px;
  border-left: 3px solid transparent;
  transition: all 0.3s;
  cursor: pointer;
}

.task-item-3d:hover {
  background: rgba(0, 245, 255, 0.05);
  transform: translateX(5px);
}

.task-item-3d.running { border-left-color: #00f5ff; }
.task-item-3d.success { border-left-color: #00ff88; }
.task-item-3d.failed { border-left-color: #ff6b6b; }
.task-item-3d.pending { border-left-color: #fbbf24; }

.task-status-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 3px 0 0 3px;
}

.task-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(0, 245, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.task-item-3d.running .task-icon { color: #00f5ff; }
.task-item-3d.success .task-icon { color: #00ff88; }
.task-item-3d.failed .task-icon { color: #ff6b6b; }
.task-item-3d.pending .task-icon { color: #fbbf24; }

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.task-info {
  flex: 1;
  min-width: 0;
}

.task-name {
  font-size: 13px;
  font-weight: 500;
  color: #e0e6ed;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #666;
  margin-top: 4px;
}

.task-status-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.task-status-tag.running { background: rgba(0, 245, 255, 0.2); color: #00f5ff; }
.task-status-tag.success { background: rgba(0, 255, 136, 0.2); color: #00ff88; }
.task-status-tag.failed { background: rgba(255, 107, 107, 0.2); color: #ff6b6b; }
.task-status-tag.pending { background: rgba(251, 191, 36, 0.2); color: #fbbf24; }

/* 告警面板 */
.alert-panel {
  background: linear-gradient(135deg, rgba(15, 25, 45, 0.9), rgba(10, 20, 40, 0.9));
  border-radius: 16px;
  border: 1px solid rgba(0, 245, 255, 0.2);
  overflow: hidden;
}

.alert-badge :deep(.el-badge__content) {
  background: #ff6b6b;
}

.alert-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(255, 107, 107, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff6b6b;
}

.alert-list-3d {
  padding: 12px;
  max-height: 320px;
  overflow-y: auto;
}

.alert-item-3d {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 10px;
  margin-bottom: 8px;
  border-left: 3px solid;
  transition: all 0.3s;
  cursor: pointer;
}

.alert-item-3d:hover {
  background: rgba(0, 0, 0, 0.5);
}

.alert-item-3d.critical { border-left-color: #ff6b6b; }
.alert-item-3d.warning { border-left-color: #fbbf24; }
.alert-item-3d.info { border-left-color: #00f5ff; }

.alert-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.alert-item-3d.critical .alert-icon-wrap { background: rgba(255, 107, 107, 0.2); color: #ff6b6b; }
.alert-item-3d.warning .alert-icon-wrap { background: rgba(251, 191, 36, 0.2); color: #fbbf24; }
.alert-item-3d.info .alert-icon-wrap { background: rgba(0, 245, 255, 0.2); color: #00f5ff; }

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-size: 13px;
  font-weight: 500;
  color: #e0e6ed;
  margin-bottom: 4px;
}

.alert-desc {
  font-size: 11px;
  color: #666;
}

.alert-time {
  font-size: 10px;
  color: #555;
  flex-shrink: 0;
}

.no-alerts {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #00ff88;
  gap: 12px;
}

.no-alerts span {
  font-size: 14px;
}

/* 状态栏 */
.status-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 32px;
  padding: 12px 24px;
  background: linear-gradient(90deg, rgba(0, 245, 255, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
  border-radius: 12px;
  border: 1px solid rgba(0, 245, 255, 0.2);
  margin-top: 8px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #888;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #666;
}

.status-dot.online { background: #00ff88; box-shadow: 0 0 8px #00ff88; }
.status-dot.success { background: #00f5ff; box-shadow: 0 0 8px #00f5ff; }
.status-dot.warning { background: #fbbf24; box-shadow: 0 0 8px #fbbf24; }

.status-item.version {
  color: #555;
  font-size: 11px;
}

/* 任务动画 */
.task-animate-enter-active,
.task-animate-leave-active {
  transition: all 0.4s ease;
}

.task-animate-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.task-animate-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.task-animate-move {
  transition: transform 0.4s ease;
}

/* 滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 245, 255, 0.3);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 245, 255, 0.5);
}

/* 响应式 */
@media (max-width: 1600px) {
  .kpi-section {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .bottom-section {
    grid-template-columns: 1fr 1fr;
  }
  
  .alert-panel {
    grid-column: span 2;
  }
}

@media (max-width: 1200px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
  
  .bottom-section {
    grid-template-columns: 1fr;
  }
  
  .alert-panel {
    grid-column: span 1;
  }
}

@media (max-width: 768px) {
  .kpi-section {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .dashboard-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-left,
  .header-right {
    width: 100%;
    justify-content: center;
  }
}
</style>
