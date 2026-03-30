<template>
  <div class="dashboard-view">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <div class="header-left">
        <div class="logo-area">
          <div class="logo-icon">RPA</div>
          <div class="logo-text">RPA运营管理系统</div>
        </div>
      </div>
      <div class="header-center">
        <el-menu
          :default-active="activeTopMenu"
          mode="horizontal"
          class="top-menu"
          :ellipsis="false"
          @select="handleTopMenuSelect"
        >
          <el-menu-item index="dashboard">首页</el-menu-item>
          <el-menu-item index="rpa">RPA运营管理</el-menu-item>
          <el-menu-item index="system">系统管理</el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <div class="user-info">
          <el-dropdown>
            <span class="user-dropdown">
              {{ currentUser.realName || currentUser.username }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">个人信息</el-dropdown-item>
                <el-dropdown-item divided>设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 仪表盘内容 -->
    <div class="dashboard-content">
      <!-- 欢迎卡片 - 蓝色渐变 -->
      <div class="welcome-section">
        <div class="welcome-left">
          <h2>欢迎回来，{{ currentUser.realName || currentUser.username }}！</h2>
          <p class="welcome-tip">今天是 {{ currentDate }}，祝您工作愉快</p>
        </div>
        <div class="welcome-right">
          <div class="quick-stats">
            <div class="quick-stat-item">
              <span class="stat-value success">{{ taskStatus.completed }}</span>
              <span class="stat-label">今日完成</span>
            </div>
            <div class="quick-stat-item">
              <span class="stat-value warning">{{ taskStatus.running }}</span>
              <span class="stat-label">进行中</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷入口和最近任务 - 两列布局，最近任务加长 -->
      <div class="dashboard-blocks">
        <!-- 快捷入口卡片 - 只保留4个核心功能 -->
        <div class="info-card shortcut-card">
          <div class="card-header">
            <h3>快捷入口</h3>
            <span class="card-more" @click="goToShortcutMore">查看详情 ></span>
          </div>
          <div class="shortcut-grid">
            <div 
              v-for="item in quickEntries" 
              :key="item.key" 
              class="shortcut-item"
              @click="goToShortcut(item)"
            >
              <div class="shortcut-icon" :class="item.iconClass">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="shortcut-content">
                <span class="shortcut-label">{{ item.label }}</span>
                <span class="shortcut-desc">{{ item.desc }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 最近任务卡片 - 加长版 -->
        <div class="info-card recent-tasks-card wide-card">
          <div class="card-header">
            <h3>最近任务</h3>
            <span class="card-more" @click="goToTasks">查看全部 ></span>
          </div>
          <el-table 
            :data="recentTasks" 
            style="width: 100%"
            :show-header="true"
            stripe
          >
            <el-table-column prop="name" label="任务编码" min-width="150" />
            <el-table-column prop="processName" label="流程名称" min-width="120" />
            <el-table-column prop="stockName" label="股票名称" min-width="120" />
            <el-table-column prop="status" label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="160" />
          </el-table>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card stat-card-primary">
          <div class="stat-header">
            <span class="stat-trend up" v-if="statsChange.tasks > 0">+{{ statsChange.tasks }}%</span>
            <span class="stat-trend down" v-else-if="statsChange.tasks < 0">{{ statsChange.tasks }}%</span>
          </div>
          <div class="stat-value">{{ stats.tasks }}</div>
          <div class="stat-label">总任务数</div>
          <div class="stat-chart-mini">
            <div class="mini-bar" v-for="(val, idx) in [40, 60, 45, 70, 55, 80, 65]" :key="idx" :style="{ height: val + '%' }"></div>
          </div>
        </div>
        <div class="stat-card stat-card-info">
          <div class="stat-header">
            <span class="stat-trend up" v-if="statsChange.robots > 0">+{{ statsChange.robots }}%</span>
          </div>
          <div class="stat-value">{{ stats.robots }}</div>
          <div class="stat-label">机器人总数</div>
          <div class="stat-chart-mini">
            <div class="mini-bar" v-for="(val, idx) in [50, 70, 60, 80, 75, 90, 85]" :key="idx" :style="{ height: val + '%' }"></div>
          </div>
        </div>
        <div class="stat-card stat-card-success">
          <div class="stat-header">
            <span class="stat-trend up" v-if="statsChange.processes > 0">+{{ statsChange.processes }}%</span>
          </div>
          <div class="stat-value">{{ stats.processes }}</div>
          <div class="stat-label">流程总数</div>
          <div class="stat-chart-mini">
            <div class="mini-bar" v-for="(val, idx) in [60, 50, 75, 65, 85, 70, 90]" :key="idx" :style="{ height: val + '%' }"></div>
          </div>
        </div>
        <div class="stat-card stat-card-warning">
          <div class="stat-header">
            <span class="stat-trend" v-if="statsChange.logs > 0">+{{ statsChange.logs }}%</span>
          </div>
          <div class="stat-value">{{ stats.logs }}</div>
          <div class="stat-label">日志总数</div>
          <div class="stat-chart-mini">
            <div class="mini-bar" v-for="(val, idx) in [45, 65, 55, 75, 60, 85, 70]" :key="idx" :style="{ height: val + '%' }"></div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-section">
        <div class="chart-card chart-line">
          <div class="chart-header">
            <h3>任务执行趋势</h3>
            <div class="chart-actions">
              <span 
                v-for="period in ['近7天', '近30天', '近90天']" 
                :key="period"
                class="period-btn"
                :class="{ active: selectedPeriod === period }"
                @click="selectedPeriod = period"
              >{{ period }}</span>
            </div>
          </div>
          <div class="chart-body">
            <v-chart :option="lineChartOption" autoresize style="height: 300px;"></v-chart>
          </div>
        </div>
        <div class="chart-card chart-bar">
          <div class="chart-header">
            <h3>任务状态分布</h3>
          </div>
          <div class="chart-body">
            <v-chart :option="barChartOption" autoresize style="height: 300px;"></v-chart>
          </div>
        </div>
      </div>

      <!-- 任务状态概览 + 系统信息 并排布局 -->
      <div class="status-system-wrapper">
        <!-- 任务状态概览 - 2x2网格 -->
        <div class="status-overview">
          <div class="status-card">
            <div class="status-header running">
              <span class="status-dot"></span>
              <span>运行中</span>
            </div>
            <div class="status-value">{{ taskStatus.running }}</div>
          </div>
          <div class="status-card">
            <div class="status-header pending">
              <span class="status-dot"></span>
              <span>待执行</span>
            </div>
            <div class="status-value">{{ taskStatus.pending }}</div>
          </div>
          <div class="status-card">
            <div class="status-header completed">
              <span class="status-dot"></span>
              <span>已完成</span>
            </div>
            <div class="status-value">{{ taskStatus.completed }}</div>
          </div>
          <div class="status-card">
            <div class="status-header failed">
              <span class="status-dot"></span>
              <span>失败</span>
            </div>
            <div class="status-value">{{ taskStatus.failed }}</div>
          </div>
        </div>

        <!-- 系统信息卡片 - 放在右侧 -->
        <div class="info-card system-info-card">
          <div class="card-header">
            <h3>系统信息</h3>
          </div>
          <div class="system-info-list">
            <div class="info-row">
              <span class="info-label">系统版本</span>
              <span class="info-value">{{ systemInfo.version }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">运行时间</span>
              <span class="info-value">{{ systemInfo.runTime }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">数据源</span>
              <span class="info-value">{{ systemInfo.dataSource }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">最后更新</span>
              <span class="info-value">{{ systemInfo.lastUpdate }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Plus, Setting, Monitor, DataAnalysis } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { apiGet } from '../utils/api.js'

use([CanvasRenderer, LineChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 顶部菜单状态
const activeTopMenu = ref('dashboard')

// 用户信息
const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  role: 1
})

// 统计数据
const stats = ref({ tasks: 0, robots: 0, processes: 0, logs: 0 })
const statsChange = ref({ tasks: 12, robots: 5, processes: 8, logs: 15 })
const taskStatus = ref({ running: 0, pending: 0, completed: 0, failed: 0 })
const selectedPeriod = ref('近7天')

// 快捷入口数据 - 只保留4个核心功能，带描述文字
const quickEntries = ref([
  { 
    key: 'createTask', 
    label: '创建任务', 
    desc: '快速创建新的RPA任务',
    icon: 'Plus', 
    path: '/rpa/tasks/create', 
    iconClass: 'icon-create' 
  },
  { 
    key: 'flowDefine', 
    label: '流程定义', 
    desc: '定义和管理RPA流程',
    icon: 'Setting', 
    path: '/rpa/processes', 
    iconClass: 'icon-flow' 
  },
  { 
    key: 'robotList', 
    label: '机器人列表', 
    desc: '查看和管理机器人',
    icon: 'Monitor', 
    path: '/rpa/robots', 
    iconClass: 'icon-robot' 
  },
  { 
    key: 'dataQuery', 
    label: '数据查询', 
    desc: '查询已处理的数据',
    icon: 'DataAnalysis', 
    path: '/data/query', 
    iconClass: 'icon-query' 
  }
])

// 最近任务数据
const recentTasks = ref([
  { id: 1, name: 'TASK_20260116_001', processName: '股票信息采集', stockName: '平安银行', status: 'running', createTime: '2026-01-16 14:20:00' },
  { id: 2, name: 'TASK_20260116_002', processName: '股票信息采集', stockName: '万科A', status: 'completed', createTime: '2026-01-16 14:15:00' },
  { id: 3, name: 'TASK_20260116_003', processName: '股票数据录入', stockName: '浦发银行', status: 'pending', createTime: '2026-01-16 14:10:00' },
  { id: 4, name: 'TASK_20260116_004', processName: '股票信息采集', stockName: '招商银行', status: 'completed', createTime: '2026-01-16 14:05:00' },
  { id: 5, name: 'TASK_20260116_005', processName: '股票信息采集', stockName: '中国平安', status: 'failed', createTime: '2026-01-16 14:00:00' }
])

const systemInfo = ref({
  version: 'v1.0.0',
  runTime: '15天8小时',
  dataSource: '东方财富网',
  lastUpdate: '2026-01-16 14:30:00'
})

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
})

// 获取状态类型
const getStatusType = (status) => {
  const map = {
    running: 'warning',
    pending: 'info',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    running: '运行中',
    pending: '待执行',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

// ECharts 配置
const lineChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['完成任务', '新建任务'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '完成任务',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: [12, 15, 10, 18, 22, 19, 25],
      itemStyle: { color: '#67c23a' }
    },
    {
      name: '新建任务',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: [8, 12, 14, 10, 18, 20, 15],
      itemStyle: { color: '#409eff' }
    }
  ]
}))

const barChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['数量'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: { type: 'category', data: ['运行中', '待执行', '已完成', '失败'] },
  yAxis: { type: 'value' },
  series: [{
    name: '数量',
    type: 'bar',
    barWidth: '50%',
    data: [
      { value: taskStatus.value.running, itemStyle: { color: '#e6a23c' } },
      { value: taskStatus.value.pending, itemStyle: { color: '#909399' } },
      { value: taskStatus.value.completed, itemStyle: { color: '#67c23a' } },
      { value: taskStatus.value.failed, itemStyle: { color: '#f56c6c' } }
    ]
  }]
}))

// 顶部菜单选择
const handleTopMenuSelect = (index) => {
  if (index === 'dashboard') {
    // 已在首页
  } else if (index === 'rpa') {
    router.push('/rpa/tasks')
  } else if (index === 'system') {
    router.push('/system/profile')
  }
}

const goToShortcut = (item) => {
  if (item.path) {
    router.push(item.path)
  }
}

const goToShortcutMore = () => {
  router.push('/shortcuts')
}

const goToTasks = () => {
  router.push('/rpa/tasks')
}

const goToProfile = () => {
  router.push('/system/profile')
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出登录')
    router.push('/login')
  })
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

// 获取统计数据
const loadStats = async () => {
  try {
    const [taskRes, robotRes, processRes, logRes] = await Promise.all([
      apiGet('/task'),
      apiGet('/robot'),
      apiGet('/process'),
      apiGet('/log')
    ])

    const tasks = taskRes?.data || []
    const robots = robotRes?.data || []
    const processes = processRes?.data || []
    const logs = logRes?.data || []

    stats.value = {
      tasks: tasks.length,
      robots: robots.length,
      processes: processes.length,
      logs: logs.length
    }

    taskStatus.value = {
      running: tasks.filter(t => t.status === 'running').length,
      pending: tasks.filter(t => t.status === 'pending').length,
      completed: tasks.filter(t => t.status === 'completed').length,
      failed: tasks.filter(t => t.status === 'failed').length
    }

    // 计算变化率（模拟）
    statsChange.value = {
      tasks: Math.floor(Math.random() * 20),
      robots: Math.floor(Math.random() * 15),
      processes: Math.floor(Math.random() * 10),
      logs: Math.floor(Math.random() * 25)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 获取最近任务
const loadRecentTasks = async () => {
  try {
    const result = await apiGet('/task')
    if (result?.code === 0 && result.data) {
      // 取最新的5条任务
      const recent = result.data.slice(0, 5).map(t => ({
        id: t.id,
        name: t.name || `任务_${t.id}`,
        processName: t.category || '未知流程',
        stockName: '-',
        status: t.status || 'pending',
        createTime: t.createTime || new Date().toLocaleString()
      }))
      recentTasks.value = recent
    }
  } catch (error) {
    console.error('加载最近任务失败:', error)
  }
}

onMounted(() => {
  loadUserFromStorage()
  loadStats()
  loadRecentTasks()
})
</script>

<style scoped>
.dashboard-view {
  min-height: 100vh;
  background-color: #f0f2f6;
}

/* 顶部导航栏 */
.top-header {
  height: 60px;
  background-color: #001529;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  flex-shrink: 0;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: #1677ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
}

.logo-text {
  color: white;
  font-weight: 500;
  font-size: 16px;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.top-menu {
  background-color: transparent;
  border-bottom: none;
}

.top-menu .el-menu-item {
  color: #ffffffa6;
  border-bottom: 2px solid transparent;
}

.top-menu .el-menu-item:hover {
  color: #fff;
  background-color: rgba(255, 255, 255, 0.1);
}

.top-menu .el-menu-item.is-active {
  color: #fff;
  border-bottom-color: #1677ff;
  background-color: transparent;
}

.header-right {
  flex-shrink: 0;
}

.user-info {
  color: #ffffffa6;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: white;
}

.user-dropdown:hover {
  color: #1677ff;
}

/* 仪表盘内容 */
.dashboard-content {
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

/* 欢迎卡片 - 蓝色渐变 */
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #1677ff 0%, #409eff 100%);
  color: white;
  padding: 28px 32px;
  border-radius: 20px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.3);
}

.welcome-left h2 {
  font-size: 24px;
  margin-bottom: 8px;
}

.welcome-tip {
  opacity: 0.9;
  font-size: 14px;
}

.quick-stats {
  display: flex;
  gap: 32px;
}

.quick-stat-item {
  text-align: center;
}

.quick-stat-item .stat-value {
  font-size: 32px;
  font-weight: bold;
}

.quick-stat-item .stat-value.success {
  color: #fff;
}

.quick-stat-item .stat-value.warning {
  color: #fff;
}

.quick-stat-item .stat-label {
  font-size: 14px;
  opacity: 0.8;
}

/* 快捷入口和最近任务布局 - 两列，最近任务占2/3宽度 */
.dashboard-blocks {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
  margin-bottom: 24px;
}

.info-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafbfc;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2f3d;
  margin: 0;
}

.card-more {
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  transition: color 0.2s;
}

.card-more:hover {
  color: #66b1ff;
  text-decoration: underline;
}

/* 快捷入口网格样式 - 4个入口，每个带图标、标题和描述 */
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 20px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  border: 1px solid #f0f0f0;
}

.shortcut-item:hover {
  background: #f5f7fa;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #e4e7ed;
}

.shortcut-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
}

.shortcut-icon.icon-create {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}
.shortcut-icon.icon-flow {
  background: linear-gradient(135deg, #36cfc9, #5cdbd3);
}
.shortcut-icon.icon-robot {
  background: linear-gradient(135deg, #722ed1, #9254de);
}
.shortcut-icon.icon-query {
  background: linear-gradient(135deg, #13c2c2, #36cfc9);
}

.shortcut-content {
  flex: 1;
}

.shortcut-label {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #1f2f3d;
  margin-bottom: 4px;
}

.shortcut-desc {
  display: block;
  font-size: 12px;
  color: #8c8f93;
  line-height: 1.4;
}

/* 最近任务卡片 */
.wide-card {
  grid-column: span 1;
}

.recent-tasks-card :deep(.el-table) {
  font-size: 12px;
}

.recent-tasks-card :deep(.el-table th) {
  background: #fafbfc;
  color: #4a5568;
  font-weight: 500;
  padding: 12px 0;
}

.recent-tasks-card :deep(.el-table td) {
  padding: 10px 0;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
}

.stat-card-primary { border-left: 4px solid #409eff; }
.stat-card-info { border-left: 4px solid #67c23a; }
.stat-card-success { border-left: 4px solid #e6a23c; }
.stat-card-warning { border-left: 4px solid #909399; }

.stat-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.stat-trend {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.stat-trend.up {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.stat-trend.down {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.stat-card .stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #1e3a4a;
  margin-bottom: 4px;
}

.stat-card .stat-label {
  color: #6b7280;
  font-size: 14px;
}

.stat-chart-mini {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 40px;
  margin-top: 16px;
}

.mini-bar {
  flex: 1;
  background: linear-gradient(to top, #409eff, #67c23a);
  border-radius: 4px 4px 0 0;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.mini-bar:hover {
  opacity: 1;
}

/* 图表区域 */
.charts-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e3a4a;
}

.chart-actions {
  display: flex;
  gap: 8px;
}

.period-btn {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.period-btn:hover {
  background: #f5f7fa;
}

.period-btn.active {
  background: #409eff;
  color: white;
}

.chart-body {
  width: 100%;
}

/* 任务状态概览 + 系统信息 并排布局 */
.status-system-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

/* 任务状态概览 - 2x2网格 */
.status-overview {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.status-card {
  background: white;
  padding: 20px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  text-align: center;
}

.status-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-header.running .status-dot { background: #e6a23c; }
.status-header.pending .status-dot { background: #909399; }
.status-header.completed .status-dot { background: #67c23a; }
.status-header.failed .status-dot { background: #f56c6c; }

.status-value {
  font-size: 36px;
  font-weight: bold;
  color: #1e3a4a;
}

/* 系统信息卡片 */
.system-info-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.system-info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.system-info-list {
  padding: 16px 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: #8c8f93;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #1f2f3d;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .dashboard-blocks {
    grid-template-columns: 1fr;
  }

  .charts-section {
    grid-template-columns: 1fr;
  }
  
  .status-system-wrapper {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>