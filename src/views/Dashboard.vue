<template>
  <div class="dashboard-pro">
    <!-- 顶部导航 - 浅色主题 -->
    <header class="dashboard-header">
      <div class="header-inner">
        <!-- Logo -->
        <div class="logo-section">
          <div class="logo-mark">RPA</div>
          <div class="logo-title">运营管理系统</div>
        </div>

        <!-- 主导航 -->
        <nav class="main-nav">
          <el-menu
            :default-active="activeTopMenu"
            mode="horizontal"
            :ellipsis="false"
            @select="handleTopMenuSelect"
            class="nav-menu"
          >
            <el-menu-item index="dashboard">
              <el-icon><Odometer /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="rpa">
              <el-icon><VideoCamera /></el-icon>
              <span>RPA运营管理</span>
            </el-menu-item>
            <el-menu-item index="system">
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </el-menu-item>
          </el-menu>
        </nav>

        <!-- 右侧工具栏 -->
        <div class="header-tools">
          <!-- 通知 -->
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="tool-badge">
            <el-button class="tool-btn" @click="goToNotifications">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>

          <!-- 用户信息 -->
          <el-dropdown trigger="click">
            <div class="user-avatar">
              <el-avatar :size="36" :src="userAvatar" class="avatar-circle">
                {{ userInitial }}
              </el-avatar>
              <div class="user-meta">
                <div class="user-name">{{ userName }}</div>
                <div class="user-role">{{ userRole }}</div>
              </div>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">
                  <el-icon><User /></el-icon>
                  个人信息
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="dashboard-main">
      <div class="content-wrapper">
        <!-- 页面标题区 -->
        <div class="page-header">
          <div class="header-text">
            <h1 class="page-title">运营概览</h1>
            <p class="page-subtitle">{{ currentDate }}</p>
          </div>
          <div class="header-actions">
            <el-button type="primary" plain @click="refreshData">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </div>

        <!-- KPI 指标卡片 - Bento Grid 风格 -->
        <div class="kpi-section">
          <div class="section-title">
            <span class="title-icon"><el-icon><TrendCharts /></el-icon></span>
            <span>关键指标</span>
          </div>
          <div class="kpi-grid">
            <!-- 任务统计 -->
            <div class="kpi-card" @click="goToTasks">
              <div class="kpi-header">
                <div class="kpi-icon-wrapper" style="background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);">
                  <el-icon color="#1976d2"><List /></el-icon>
                </div>
                <div class="kpi-trend" :class="statsChange.tasks >= 0 ? 'trend-up' : 'trend-down'">
                  <el-icon><CaretTop /></el-icon>
                  {{ Math.abs(statsChange.tasks) }}%
                </div>
              </div>
              <div class="kpi-value">{{ stats.tasks }}</div>
              <div class="kpi-label">总任务数</div>
              <div class="kpi-desc">较上周期</div>
            </div>

            <!-- 成功率 -->
            <div class="kpi-card" @click="goToTasks">
              <div class="kpi-header">
                <div class="kpi-icon-wrapper" style="background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);">
                  <el-icon color="#388e3c"><CircleCheck /></el-icon>
                </div>
                <div class="kpi-trend trend-up">
                  <el-icon><CaretTop /></el-icon>
                  {{ successRate }}%
                </div>
              </div>
              <div class="kpi-value">{{ stats.tasks - stats.failedTasks }}</div>
              <div class="kpi-label">成功任务</div>
              <div class="kpi-desc">执行成功率</div>
            </div>

            <!-- 在线机器人 -->
            <div class="kpi-card" @click="goToRobots">
              <div class="kpi-header">
                <div class="kpi-icon-wrapper" style="background: linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 100%);">
                  <el-icon color="#0097a7"><Monitor /></el-icon>
                </div>
                <div class="kpi-trend" :class="statsChange.robots >= 0 ? 'trend-up' : 'trend-down'">
                  <el-icon><CaretTop /></el-icon>
                  {{ Math.abs(statsChange.robots) }}%
                </div>
              </div>
              <div class="kpi-value">{{ activeRobots }}</div>
              <div class="kpi-label">在线机器人</div>
              <div class="kpi-desc">总数: {{ stats.robots }}</div>
            </div>

            <!-- 队列状态 -->
            <div class="kpi-card" @click="goToQueue">
              <div class="kpi-header">
                <div class="kpi-icon-wrapper" style="background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);">
                  <el-icon color="#f57c00"><Warning /></el-icon>
                </div>
              </div>
              <div class="kpi-value">{{ queueBacklog }}</div>
              <div class="kpi-label">队列积压</div>
              <div class="kpi-desc">待处理任务</div>
            </div>

            <!-- 响应时间 -->
            <div class="kpi-card">
              <div class="kpi-header">
                <div class="kpi-icon-wrapper" style="background: linear-gradient(135deg, #fce4ec 0%, #f8bbd0 100%);">
                  <el-icon color="#c2185b"><Timer /></el-icon>
                </div>
              </div>
              <div class="kpi-value">{{ avgResponseTime }}</div>
              <div class="kpi-label">平均响应时间</div>
              <div class="kpi-desc">单位: ms</div>
            </div>
          </div>
        </div>

        <!-- 主图表区 -->
        <div class="main-grid">
          <!-- 趋势图卡片 -->
          <div class="chart-card-large">
            <div class="card-header">
              <div class="header-left">
                <span class="card-title"><el-icon><DataLine /></el-icon> 任务执行趋势</span>
                <span class="card-subtitle">近30天执行情况</span>
              </div>
              <div class="period-selector">
                <el-radio-group v-model="selectedPeriod" size="small">
                  <el-radio-button value="7">近7天</el-radio-button>
                  <el-radio-button value="30">近30天</el-radio-button>
                  <el-radio-button value="90">近90天</el-radio-button>
                </el-radio-group>
              </div>
            </div>
            <div class="chart-container">
              <v-chart :option="lineChartOption" autoresize />
            </div>
          </div>

          <!-- 右侧栏 -->
          <div class="side-panel">
            <!-- 机器人状态 -->
            <div class="status-card">
              <div class="card-header">
                <span class="card-title"><el-icon><Monitor /></el-icon> 机器人状态</span>
                <el-link type="primary" @click="goToRobots">查看全部</el-link>
              </div>
              <div class="robot-list">
                <div v-for="robot in robotStatusList.slice(0, 5)" :key="robot.id" class="robot-item">
                  <div class="robot-info">
                    <el-avatar :size="32" icon="User" class="robot-avatar" />
                    <div class="robot-details">
                      <div class="robot-name">{{ robot.name }}</div>
                      <div class="robot-ip">{{ robot.ip || '192.168.1.100' }}</div>
                    </div>
                    <el-tag :type="getRobotStatusType(robot.status)" size="small" effect="plain">
                      {{ getRobotStatusText(robot.status) }}
                    </el-tag>
                  </div>
                  <div class="resource-bars">
                    <div class="resource-bar">
                      <span class="resource-label">CPU {{ robot.cpuUsage || 0 }}%</span>
                      <el-progress :percentage="robot.cpuUsage || 0" :color="getProgressColor(robot.cpuUsage)" :stroke-width="6" :show-text="false" />
                    </div>
                    <div class="resource-bar">
                      <span class="resource-label">内存 {{ robot.memoryUsage || 0 }}%</span>
                      <el-progress :percentage="robot.memoryUsage || 0" :color="getProgressColor(robot.memoryUsage)" :stroke-width="6" :show-text="false" />
                    </div>
                  </div>
                </div>
                <el-empty v-if="robotStatusList.length === 0" description="暂无数据" :image-size="80" />
              </div>
            </div>

            <!-- 许可证使用 -->
            <div class="license-card">
              <div class="card-header">
                <span class="card-title"><el-icon><Key /></el-icon> 许可证</span>
              </div>
              <div class="license-content">
                <el-progress
                  type="dashboard"
                  :percentage="licenseUsage"
                  :color="getLicenseColor(licenseUsage)"
                  :width="100"
                >
                  <template #default>
                    <span class="license-percent">{{ licenseUsage }}%</span>
                    <span class="license-label">使用率</span>
                  </template>
                </el-progress>
                <div class="license-details">
                  <div class="license-item">
                    <span class="label">已用</span>
                    <span class="value">{{ usedLicenses }}</span>
                  </div>
                  <div class="license-item">
                    <span class="label">总数</span>
                    <span class="value">{{ totalLicenses }}</span>
                  </div>
                  <div class="license-item">
                    <span class="label">到期</span>
                    <span class="value">{{ licenseExpiry }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部信息区 -->
        <div class="bottom-grid">
          <!-- 告警消息 -->
          <div class="alert-card">
            <div class="card-header">
              <span class="card-title"><el-icon><BellFilled /></el-icon> 告警消息</span>
              <el-link type="primary" @click="goToNotifications">查看全部</el-link>
            </div>
            <div class="alert-list">
              <div v-for="alert in recentAlerts.slice(0, 4)" :key="alert.id" class="alert-item" @click="viewAlert(alert)">
                <div class="alert-icon" :class="alert.type">
                  <el-icon><WarningFilled /></el-icon>
                </div>
                <div class="alert-content">
                  <div class="alert-title">{{ alert.title }}</div>
                  <div class="alert-time">{{ formatTime(alert.createTime) }}</div>
                </div>
                <el-tag :type="getAlertType(alert.type)" size="small" effect="plain">
                  {{ getAlertText(alert.type) }}
                </el-tag>
              </div>
              <el-empty v-if="recentAlerts.length === 0" description="暂无告警" :image-size="60" />
            </div>
          </div>

          <!-- 热门流程 -->
          <div class="process-card">
            <div class="card-header">
              <span class="card-title"><el-icon><StarFilled /></el-icon> 热门流程</span>
              <el-link type="primary" @click="goToProcesses">流程仓库</el-link>
            </div>
            <div class="process-list">
              <div v-for="(process, index) in hotProcesses.slice(0, 5)" :key="process.id" class="process-item" @click="goToProcessDetail(process)">
                <div class="process-rank" :class="'rank-' + (index + 1)">
                  {{ index + 1 }}
                </div>
                <div class="process-info">
                  <div class="process-name">{{ process.name }}</div>
                  <div class="process-meta">
                    <span class="process-count">执行 {{ process.execCount }} 次</span>
                  </div>
                </div>
                <el-button type="primary" link @click.stop="goToProcessDetail(process)">
                  查看
                </el-button>
              </div>
              <el-empty v-if="hotProcesses.length === 0" description="暂无流程" :image-size="60" />
            </div>
          </div>

          <!-- 快速操作 -->
          <div class="quick-actions-card">
            <div class="card-header">
              <span class="card-title"><el-icon><MagicStick /></el-icon> 快速操作</span>
            </div>
            <div class="action-grid">
              <div class="action-item" @click="goToProcesses">
                <div class="action-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                  <el-icon><Document /></el-icon>
                </div>
                <span>创建流程</span>
              </div>
              <div class="action-item" @click="goToRobots">
                <div class="action-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                  <el-icon><Cpu /></el-icon>
                </div>
                <span>机器人管理</span>
              </div>
              <div class="action-item" @click="goToTasks">
                <div class="action-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
                  <el-icon><VideoPlay /></el-icon>
                </div>
                <span>任务监控</span>
              </div>
              <div class="action-item" @click="goToSystemSettings">
                <div class="action-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
                  <el-icon><Setting /></el-icon>
                </div>
                <span>系统设置</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, ArrowDown, Odometer, VideoCamera, Setting, Refresh, User,
  SwitchButton, List, CircleCheck, Monitor, Warning, Timer, Star, Key,
  DataLine, BellFilled, WarningFilled, MagicStick, Document, Cpu, VideoPlay,
  CaretTop, TrendCharts
} from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()
const activeTopMenu = ref('dashboard')
const selectedPeriod = ref('30')

// 用户信息
const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  role: 1,
  avatar: ''
})

const userName = computed(() => currentUser.value.realName || currentUser.value.username)
const userInitial = computed(() => userName.value.charAt(0).toUpperCase())
const userAvatar = computed(() => currentUser.value.avatar || '')
const userRole = computed(() => currentUser.value.role === 1 ? '管理员' : '用户')

const unreadCount = ref(0)

// 统计数据
const stats = ref({ tasks: 0, robots: 0, failedTasks: 0 })
const statsChange = ref({ tasks: 12, robots: 5 })
const robotStatusList = ref([])
const recentAlerts = ref([])
const hotProcesses = ref([])
const usedLicenses = ref(3)
const totalLicenses = ref(10)
const licenseExpiry = ref('2026-12-31')
const queueBacklog = ref(5)
const avgResponseTime = ref(245)

// 计算属性
const successRate = computed(() => {
  if (stats.value.tasks === 0) return 0
  return Math.round(((stats.value.tasks - stats.value.failedTasks) / stats.value.tasks) * 100)
})

const activeRobots = computed(() => {
  return robotStatusList.value.filter(r => r.status === 'idle' || r.status === 'active').length
})

const licenseUsage = computed(() => Math.round((usedLicenses.value / totalLicenses.value) * 100))

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

// ECharts 配置 - 浅色主题
const lineChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#e4e7ed',
    textStyle: { color: '#303133' },
    shadowBlur: 10,
    shadowColor: 'rgba(0, 0, 0, 0.1)'
  },
  legend: {
    data: ['成功', '失败', '总计'],
    bottom: 0,
    textStyle: { color: '#606266' }
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '15%',
    top: '10%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    axisLine: { lineStyle: { color: '#e4e7ed' } },
    axisLabel: { color: '#606266' }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisLabel: { color: '#606266' },
    splitLine: { lineStyle: { color: '#f0f0f0' } }
  },
  series: [
    {
      name: '成功',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      showSymbol: false,
      lineStyle: { width: 3, color: '#67c23a' },
      areaStyle: { color: 'rgba(103, 194, 58, 0.15)' },
      data: [120, 132, 101, 134, 190, 230, 210]
    },
    {
      name: '失败',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      showSymbol: false,
      lineStyle: { width: 3, color: '#f56c6c' },
      areaStyle: { color: 'rgba(245, 108, 108, 0.15)' },
      data: [10, 12, 8, 15, 8, 5, 3]
    },
    {
      name: '总计',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      showSymbol: false,
      lineStyle: { width: 3, color: '#409eff', type: 'dashed' },
      data: [130, 144, 109, 149, 198, 235, 213]
    }
  ]
}))

// 辅助函数
const getRobotStatusType = (s) => {
  const map = { idle: 'success', busy: 'warning', offline: 'info', active: 'success' }
  return map[s] || 'info'
}

const getRobotStatusText = (s) => {
  const map = { idle: '空闲', busy: '忙碌', offline: '离线', active: '活跃' }
  return map[s] || s
}

const getProgressColor = (v) => {
  if (v >= 80) return '#f56c6c'
  if (v >= 60) return '#e6a23c'
  return '#67c23a'
}

const getLicenseColor = (v) => {
  if (v >= 80) return '#f56c6c'
  if (v >= 60) return '#e6a23c'
  return '#67c23a'
}

const getAlertType = (type) => {
  const map = { danger: 'danger', warning: 'warning', info: 'info', success: 'success' }
  return map[type] || 'info'
}

const getAlertText = (type) => {
  const map = { danger: '紧急', warning: '警告', info: '提示', success: '成功' }
  return map[type] || type
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 导航函数
const goToNotifications = () => router.push('/rpa/notifications')
const goToTasks = () => router.push('/rpa/tasks')
const goToRobots = () => router.push('/rpa/robots')
const goToProcesses = () => router.push('/rpa/processes')
const goToQueue = () => router.push('/rpa/queue')
const goToProfile = () => router.push('/system/profile')
const goToProcessDetail = (process) => router.push('/rpa/processes')
const goToSystemSettings = () => router.push('/system/settings')

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出登录')
    router.push('/login')
  })
}

const handleTopMenuSelect = (index) => {
  if (index === 'dashboard') {
    router.push('/dashboard')
  } else if (index === 'rpa') {
    router.push('/rpa/tasks')
  } else if (index === 'system') {
    router.push('/system/profile')
  }
}

const refreshData = () => {
  ElMessage.success('数据已刷新')
  loadStats()
  loadNotifications()
  loadHotProcesses()
}

const viewAlert = (alert) => {
  ElMessageBox.alert(alert.content || alert.title, alert.title, { confirmButtonText: '确定' })
}

// 数据加载
const loadUserFromStorage = () => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUser.value = { ...currentUser.value, ...user }
    } catch (e) {}
  }
}

const loadStats = async () => {
  try {
    const { apiGet } = await import('../utils/api.js')
    const [taskRes, robotRes] = await Promise.all([
      apiGet('/task'),
      apiGet('/robot')
    ])

    const tasks = taskRes?.data || []
    const robots = robotRes?.data || []

    stats.value = {
      tasks: tasks.length,
      robots: robots.length,
      failedTasks: tasks.filter(t => t.status === 'failed').length
    }

    robotStatusList.value = robots
    statsChange.value = {
      tasks: Math.floor(Math.random() * 20),
      robots: Math.floor(Math.random() * 15)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadNotifications = async () => {
  try {
    const { apiGet } = await import('../utils/api.js')
    const result = await apiGet('/notification/stats')
    if (result?.code === 0 && result.data?.unreads) {
      const unreads = result.data.unreads
      unreadCount.value = (unreads.collect || 0) + (unreads.temp || 0) + (unreads.user || 0)
    }

    const notifResult = await apiGet('/notification?type=temp&size=5')
    if (notifResult?.code === 0) {
      recentAlerts.value = (notifResult.data || []).map(n => ({
        ...n,
        type: n.type === 'user' ? 'warning' : 'info'
      }))
    }
  } catch (error) {
    console.error('加载通知失败:', error)
  }
}

const loadHotProcesses = async () => {
  try {
    const { apiGet } = await import('../utils/api.js')
    const result = await apiGet('/process')
    if (result?.code === 0) {
      hotProcesses.value = (result.data || [])
        .slice(0, 5)
        .map((p, i) => ({
          ...p,
          execCount: Math.floor(Math.random() * 500) + 100
        }))
    }
  } catch (error) {
    console.error('加载热门流程失败:', error)
  }
}

onMounted(() => {
  loadUserFromStorage()
  loadStats()
  loadNotifications()
  loadHotProcesses()
})
</script>

<style scoped>
/* ===== 设计系统 - 浅色主题 ===== */
.dashboard-pro {
  --primary: #409eff;
  --primary-light: #66b1ff;
  --primary-dark: #3a8ee6;
  --success: #67c23a;
  --warning: #e6a23c;
  --danger: #f56c6c;
  --info: #909399;

  --bg-primary: #f5f7fa;
  --bg-secondary: #ffffff;
  --bg-tertiary: #fafbfc;

  --text-primary: #1f2937;
  --text-secondary: #6b7280;
  --text-tertiary: #9ca3af;

  --border-color: #e5e7eb;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.12);

  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;

  min-height: 100vh;
  background: var(--bg-primary);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* ===== Header - 浅色主题 ===== */
.dashboard-header {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
  box-shadow: var(--shadow-sm);
}

.header-inner {
  max-width: 1440px;
  margin: 0 auto;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-mark {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 14px;
}

.logo-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.main-nav {
  flex: 1;
  display: flex;
  justify-content: center;
}

.nav-menu {
  border-bottom: none;
  background: transparent;
}

.nav-menu .el-menu-item {
  padding: 0 24px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-secondary);
  height: 64px;
  line-height: 64px;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
}

.nav-menu .el-menu-item:hover {
  color: var(--primary);
  background: rgba(64, 158, 255, 0.05);
}

.nav-menu .el-menu-item.is-active {
  color: var(--primary);
  border-bottom-color: var(--primary);
  background: transparent;
}

.nav-menu .el-menu-item .el-icon {
  margin-right: 6px;
  font-size: 18px;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 16px;
}

.tool-badge {
  cursor: pointer;
}

.tool-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s;
}

.tool-btn:hover {
  background: var(--bg-tertiary);
  color: var(--primary);
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px 6px 6px;
  border-radius: 24px;
  transition: all 0.2s;
}

.user-avatar:hover {
  background: var(--bg-tertiary);
}

.avatar-circle {
  border: 2px solid var(--border-color);
}

.user-meta {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.user-role {
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.2;
}

.dropdown-arrow {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* ===== Main Content ===== */
.dashboard-main {
  padding: 24px;
  max-width: 1440px;
  margin: 0 auto;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ===== Page Header ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.header-actions .el-button {
  border-radius: var(--radius-sm);
  font-weight: 500;
}

/* ===== KPI Section ===== */
.kpi-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.title-icon {
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.kpi-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--primary) 0%, var(--primary-light) 100%);
  opacity: 0;
  transition: opacity 0.3s;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.kpi-card:hover::before {
  opacity: 1;
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.kpi-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 12px;
}

.trend-up {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.trend-down {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.kpi-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  margin-bottom: 8px;
  letter-spacing: -1px;
}

.kpi-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.kpi-desc {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* ===== Main Grid ===== */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
}

.chart-card-large {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-title .el-icon {
  color: var(--primary);
  font-size: 18px;
}

.card-subtitle {
  font-size: 13px;
  color: var(--text-tertiary);
}

.period-selector {
  display: flex;
}

.period-selector :deep(.el-radio-button__inner) {
  padding: 6px 16px;
  font-size: 13px;
  border-radius: var(--radius-sm) !important;
}

.chart-container {
  height: 320px;
}

/* ===== Side Panel ===== */
.side-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.status-card,
.license-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-title .el-icon {
  color: var(--primary);
  font-size: 16px;
}

.robot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 320px;
  overflow-y: auto;
}

.robot-item {
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  transition: all 0.2s;
}

.robot-item:hover {
  background: var(--bg-primary);
  border-color: var(--primary);
}

.robot-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.robot-avatar {
  flex-shrink: 0;
}

.robot-details {
  flex: 1;
  min-width: 0;
}

.robot-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.robot-ip {
  font-size: 12px;
  color: var(--text-tertiary);
}

.resource-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resource-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resource-label {
  font-size: 12px;
  color: var(--text-secondary);
  width: 60px;
  flex-shrink: 0;
}

.resource-bar :deep(.el-progress) {
  flex: 1;
}

.resource-bar :deep(.el-progress__bar) {
  border-radius: 4px;
}

/* License Card */
.license-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.license-percent {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.license-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.license-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.license-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.license-item .label {
  color: var(--text-secondary);
}

.license-item .value {
  color: var(--text-primary);
  font-weight: 600;
}

/* ===== Bottom Grid ===== */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 24px;
}

.alert-card,
.process-card,
.quick-actions-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.alert-list,
.process-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.alert-item:hover {
  background: var(--bg-primary);
  border-color: var(--primary);
  transform: translateX(4px);
}

.alert-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.alert-icon.danger {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.alert-icon.warning {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.alert-icon.info {
  background: rgba(144, 147, 153, 0.1);
  color: #909399;
}

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.alert-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.process-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.process-item:hover {
  background: var(--bg-primary);
  border-color: var(--primary);
  transform: translateX(4px);
}

.process-rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.rank-1 { background: linear-gradient(135deg, #ffd700 0%, #ffb700 100%); }
.rank-2 { background: linear-gradient(135deg, #c0c0c0 0%, #a0a0a0 100%); }
.rank-3 { background: linear-gradient(135deg, #cd7f32 0%, #b87333 100%); }
.rank-4, .rank-5 { background: #8c8c8c; }

.process-info {
  flex: 1;
  min-width: 0;
}

.process-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.process-meta {
  margin-top: 2px;
}

.process-count {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* ===== Quick Actions ===== */
.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 12px;
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.action-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.action-item span {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .main-grid {
    grid-template-columns: 1fr;
  }
  .bottom-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .header-inner {
    padding: 0 16px;
  }

  .logo-title {
    display: none;
  }

  .nav-menu .el-menu-item span {
    display: none;
  }

  .nav-menu .el-menu-item {
    padding: 0 12px;
  }

  .user-meta {
    display: none;
  }

  .dashboard-main {
    padding: 16px;
  }

  .page-title {
    font-size: 24px;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .kpi-card {
    padding: 16px;
  }

  .kpi-value {
    font-size: 24px;
  }

  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .main-grid {
    gap: 16px;
  }

  .chart-card-large {
    padding: 16px;
  }

  .chart-container {
    height: 260px;
  }
}

@media (max-width: 480px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
