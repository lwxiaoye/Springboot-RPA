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
        <el-badge :value="unreadNotifications" :hidden="unreadNotifications === 0" class="notif-badge">
          <el-icon class="notif-icon" @click="goToNotifications"><Bell /></el-icon>
        </el-badge>
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

      <!-- 关键指标卡片 -->
      <div class="stats-grid">
        <div class="stat-card stat-card-primary" @click="goToTasks">
          <div class="stat-icon"><el-icon><List /></el-icon></div>
          <div class="stat-header">
            <span class="stat-trend up" v-if="statsChange.tasks > 0">+{{ statsChange.tasks }}%</span>
          </div>
          <div class="stat-value">{{ stats.tasks }}</div>
          <div class="stat-label">总任务数</div>
        </div>
        <div class="stat-card stat-card-success" @click="goToTasks">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-header">
            <span class="stat-trend up">{{ successRate }}%</span>
          </div>
          <div class="stat-value">{{ stats.tasks - stats.failedTasks }}</div>
          <div class="stat-label">成功任务</div>
        </div>
        <div class="stat-card stat-card-info" @click="goToRobots">
          <div class="stat-icon"><el-icon><Monitor /></el-icon></div>
          <div class="stat-header">
            <span class="stat-trend up" v-if="statsChange.robots > 0">+{{ statsChange.robots }}%</span>
          </div>
          <div class="stat-value">{{ activeRobots }}</div>
          <div class="stat-label">在线机器人</div>
        </div>
        <div class="stat-card stat-card-warning" @click="goToQueue">
          <div class="stat-icon"><el-icon><Warning /></el-icon></div>
          <div class="stat-header">
            <span class="stat-trend">{{ queueBacklog }}</span>
          </div>
          <div class="stat-value">{{ queueBacklog }}</div>
          <div class="stat-label">队列积压</div>
        </div>
        <div class="stat-card stat-card-danger">
          <div class="stat-icon"><el-icon><Timer /></el-icon></div>
          <div class="stat-header">
            <span class="stat-trend">ms</span>
          </div>
          <div class="stat-value">{{ avgResponseTime }}</div>
          <div class="stat-label">平均响应时间</div>
        </div>
      </div>

      <!-- 图表和机器人状态 -->
      <div class="main-charts-row">
        <!-- 成功率趋势图 -->
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
            <v-chart :option="lineChartOption" autoresize style="height: 280px;"></v-chart>
          </div>
        </div>

        <!-- 机器人状态 -->
        <div class="robot-status-card">
          <div class="card-header">
            <h3>机器人状态</h3>
            <span class="card-more" @click="goToRobots">查看全部 ></span>
          </div>
          <div class="robot-list">
            <div v-for="robot in robotStatusList.slice(0, 5)" :key="robot.id" class="robot-item">
              <div class="robot-info">
                <span class="robot-name">{{ robot.name }}</span>
                <el-tag :type="getRobotStatusType(robot.status)" size="small">
                  {{ getRobotStatusText(robot.status) }}
                </el-tag>
              </div>
              <div class="robot-resource">
                <div class="resource-item">
                  <span class="resource-label">CPU</span>
                  <el-progress :percentage="robot.cpuUsage || 0" :color="getProgressColor(robot.cpuUsage)" :stroke-width="4" />
                </div>
                <div class="resource-item">
                  <span class="resource-label">内存</span>
                  <el-progress :percentage="robot.memoryUsage || 0" :color="getProgressColor(robot.memoryUsage)" :stroke-width="4" />
                </div>
              </div>
            </div>
            <div v-if="robotStatusList.length === 0" class="empty-tip">暂无机器人数据</div>
          </div>
        </div>
      </div>

      <!-- 告警消息和热门流程 -->
      <div class="bottom-row">
        <!-- 告警消息滚动条 -->
        <div class="alert-card">
          <div class="card-header">
            <h3><el-icon><Bell /></el-icon> 告警消息</h3>
            <span class="card-more" @click="goToNotifications">查看全部 ></span>
          </div>
          <div class="alert-scroll" v-if="recentAlerts.length > 0">
            <div 
              v-for="alert in recentAlerts" 
              :key="alert.id" 
              class="alert-item"
              :class="alert.type"
              @click="viewAlert(alert)"
            >
              <span class="alert-time">{{ formatTime(alert.createTime) }}</span>
              <span class="alert-title">{{ alert.title }}</span>
              <el-tag :type="getAlertType(alert.type)" size="small">{{ getAlertText(alert.type) }}</el-tag>
            </div>
          </div>
          <div v-else class="empty-tip">暂无告警消息</div>
        </div>

        <!-- 热门流程排行 -->
        <div class="hot-process-card">
          <div class="card-header">
            <h3><el-icon><Star /></el-icon> 热门流程排行</h3>
            <span class="card-more" @click="goToProcesses">流程仓库 ></span>
          </div>
          <div class="process-rank-list">
            <div 
              v-for="(process, index) in hotProcesses" 
              :key="process.id" 
              class="process-rank-item"
              @click="goToProcessDetail(process)"
            >
              <span class="rank-num" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              <span class="process-name">{{ process.name }}</span>
              <span class="process-count">{{ process.execCount }}次</span>
            </div>
            <div v-if="hotProcesses.length === 0" class="empty-tip">暂无流程数据</div>
          </div>
        </div>

        <!-- 许可证使用率 -->
        <div class="license-card">
          <div class="card-header">
            <h3><el-icon><Key /></el-icon> 许可证使用率</h3>
          </div>
          <div class="license-content">
            <el-progress 
              type="circle" 
              :percentage="licenseUsage" 
              :color="licenseUsage > 80 ? '#f56c6c' : licenseUsage > 60 ? '#e6a23c' : '#67c23a'"
              :width="120"
            >
              <template #default>
                <span class="license-text">{{ usedLicenses }}/{{ totalLicenses }}</span>
                <span class="license-label">并发数</span>
              </template>
            </el-progress>
            <div class="license-info">
              <div class="info-item">
                <span class="label">已用:</span>
                <span class="value">{{ usedLicenses }}</span>
              </div>
              <div class="info-item">
                <span class="label">总数:</span>
                <span class="value">{{ totalLicenses }}</span>
              </div>
              <div class="info-item">
                <span class="label">到期时间:</span>
                <span class="value">{{ licenseExpiry }}</span>
              </div>
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
import { ArrowDown, Bell, List, CircleCheck, Monitor, Warning, Timer, Star, Key } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()
const activeTopMenu = ref('dashboard')

// 用户信息
const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  role: 1
})

const unreadNotifications = ref(0)

// 统计数据
const stats = ref({ tasks: 0, robots: 0, processes: 0, logs: 0, failedTasks: 0 })
const statsChange = ref({ tasks: 12, robots: 5, processes: 8, logs: 15 })
const taskStatus = ref({ running: 0, pending: 0, completed: 0, failed: 0 })
const selectedPeriod = ref('近7天')

// 机器人状态列表
const robotStatusList = ref([])

// 告警消息
const recentAlerts = ref([])

// 热门流程
const hotProcesses = ref([])

// 许可证
const usedLicenses = ref(3)
const totalLicenses = ref(10)
const licenseExpiry = ref('2026-12-31')

const successRate = computed(() => {
  if (stats.value.tasks === 0) return 0
  return Math.round(((stats.value.tasks - stats.value.failedTasks) / stats.value.tasks) * 100)
})

const activeRobots = computed(() => {
  return robotStatusList.value.filter(r => r.status === 'idle' || r.status === 'active').length
})

const queueBacklog = ref(5)
const avgResponseTime = ref(245)
const licenseUsage = computed(() => Math.round((usedLicenses.value / totalLicenses.value) * 100))

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
})

// ECharts 配置
const lineChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['成功', '失败', '总计'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '成功',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: [120, 132, 101, 134, 190, 230, 210],
      itemStyle: { color: '#67c23a' }
    },
    {
      name: '失败',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: [10, 12, 8, 15, 8, 5, 3],
      itemStyle: { color: '#f56c6c' }
    },
    {
      name: '总计',
      type: 'line',
      smooth: true,
      data: [130, 144, 109, 149, 198, 235, 213],
      itemStyle: { color: '#409eff' }
    }
  ]
}))

const getRobotStatusType = (s) => {
  const map = { idle: 'success', busy: 'warning', offline: 'info', active: 'success' }
  return map[s] || 'info'
}

const getRobotStatusText = (s) => {
  const map = { idle: '空闲', busy: '忙碌', offline: '离线', active: '活跃' }
  return map[s] || s
}

const getProgressColor = (v) => v >= 80 ? '#f56c6c' : v >= 60 ? '#e6a23c' : '#67c23a'

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
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const goToNotifications = () => router.push('/rpa/notifications')
const goToTasks = () => router.push('/rpa/tasks')
const goToRobots = () => router.push('/rpa/robots')
const goToProcesses = () => router.push('/rpa/processes')
const goToQueue = () => router.push('/rpa/queue')
const goToProfile = () => router.push('/system/profile')
const goToProcessDetail = (process) => router.push('/rpa/processes')

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出登录')
    router.push('/login')
  })
}

const viewAlert = (alert) => {
  ElMessageBox.alert(alert.content || alert.title, alert.title, { confirmButtonText: '确定' })
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
      logs: logs.length,
      failedTasks: tasks.filter(t => t.status === 'failed').length
    }

    taskStatus.value = {
      running: tasks.filter(t => t.status === 'running').length,
      pending: tasks.filter(t => t.status === 'pending').length,
      completed: tasks.filter(t => t.status === 'completed').length,
      failed: tasks.filter(t => t.status === 'failed').length
    }

    robotStatusList.value = robots
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

const loadNotifications = async () => {
  try {
    const { apiGet } = await import('../utils/api.js')
    const result = await apiGet('/notification/stats')
    if (result?.code === 0 && result.data?.unreads) {
      const unreads = result.data.unreads
      unreadNotifications.value = (unreads.collect || 0) + (unreads.temp || 0) + (unreads.user || 0)
    }

    // 加载最近告警
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
.dashboard-view {
  min-height: 100vh;
  background-color: #f0f2f6;
}

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

.header-left { flex-shrink: 0; }

.logo-area { display: flex; align-items: center; gap: 8px; }

.logo-icon {
  width: 32px; height: 32px; background: #1677ff; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; color: white; font-weight: bold;
}

.logo-text { color: white; font-weight: 500; font-size: 16px; }

.header-center { flex: 1; display: flex; justify-content: center; }

.top-menu { background-color: transparent; border-bottom: none; }
.top-menu .el-menu-item { color: #ffffffa6; border-bottom: 2px solid transparent; }
.top-menu .el-menu-item:hover { color: #fff; background-color: rgba(255, 255, 255, 0.1); }
.top-menu .el-menu-item.is-active { color: #fff; border-bottom-color: #1677ff; background-color: transparent; }

.header-right { flex-shrink: 0; display: flex; align-items: center; gap: 20px; }

.notif-badge { cursor: pointer; }
.notif-icon { font-size: 20px; color: #ffffffa6; cursor: pointer; }
.notif-icon:hover { color: #fff; }

.user-info { color: #ffffffa6; }
.user-dropdown { display: flex; align-items: center; gap: 8px; cursor: pointer; color: white; }
.user-dropdown:hover { color: #1677ff; }

.dashboard-content { max-width: 1600px; margin: 0 auto; padding: 24px; }

.welcome-section {
  display: flex; justify-content: space-between; align-items: center;
  background: linear-gradient(135deg, #1677ff 0%, #409eff 100%);
  color: white; padding: 28px 32px; border-radius: 20px; margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.3);
}
.welcome-left h2 { font-size: 24px; margin-bottom: 8px; }
.welcome-tip { opacity: 0.9; font-size: 14px; }
.quick-stats { display: flex; gap: 32px; }
.quick-stat-item { text-align: center; }
.quick-stat-item .stat-value { font-size: 32px; font-weight: bold; }
.quick-stat-item .stat-label { font-size: 14px; opacity: 0.8; }

.stats-grid {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 20px; margin-bottom: 24px;
}

.stat-card {
  background: white; padding: 20px; border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); cursor: pointer;
  transition: all 0.2s; position: relative; overflow: hidden;
}
.stat-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12); }
.stat-card-primary { border-left: 4px solid #409eff; }
.stat-card-success { border-left: 4px solid #67c23a; }
.stat-card-info { border-left: 4px solid #36cfc9; }
.stat-card-warning { border-left: 4px solid #e6a23c; }
.stat-card-danger { border-left: 4px solid #f56c6c; }

.stat-icon {
  position: absolute; top: 16px; right: 16px; font-size: 32px; opacity: 0.2;
}

.stat-header { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.stat-trend { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.stat-trend.up { background: rgba(103, 194, 58, 0.1); color: #67c23a; }

.stat-card .stat-value { font-size: 28px; font-weight: bold; color: #1e3a4a; margin-bottom: 4px; }
.stat-card .stat-label { color: #6b7280; font-size: 14px; }

.main-charts-row { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 24px; }

.chart-card {
  background: white; border-radius: 16px; padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.chart-header h3 { font-size: 16px; font-weight: 600; color: #1e3a4a; }
.chart-actions { display: flex; gap: 8px; }
.period-btn {
  padding: 4px 12px; border-radius: 12px; font-size: 12px; color: #666; cursor: pointer; transition: all 0.2s;
}
.period-btn:hover { background: #f5f7fa; }
.period-btn.active { background: #409eff; color: white; }
.chart-body { width: 100%; }

.robot-status-card {
  background: white; border-radius: 16px; padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }
.card-header h3 { font-size: 16px; font-weight: 600; color: #1e3a4a; display: flex; align-items: center; gap: 8px; }
.card-more { font-size: 12px; color: #409eff; cursor: pointer; }
.card-more:hover { color: #66b1ff; text-decoration: underline; }

.robot-list { max-height: 280px; overflow-y: auto; }
.robot-item {
  padding: 12px 0; border-bottom: 1px solid #f5f7fa;
}
.robot-item:last-child { border-bottom: none; }
.robot-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.robot-name { font-weight: 500; color: #1e3a4a; font-size: 14px; }
.robot-resource { display: flex; flex-direction: column; gap: 4px; }
.resource-item { display: flex; align-items: center; gap: 8px; }
.resource-label { font-size: 12px; color: #8c8c8c; width: 30px; }
.resource-item :deep(.el-progress) { flex: 1; }

.empty-tip { text-align: center; color: #8c8c8c; padding: 20px 0; font-size: 14px; }

.bottom-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 20px; }

.alert-card {
  background: white; border-radius: 16px; padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.alert-scroll { max-height: 300px; overflow-y: auto; }
.alert-item {
  display: flex; align-items: center; gap: 12px; padding: 10px;
  border-radius: 8px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s;
}
.alert-item:hover { background: #f5f7fa; }
.alert-item.danger { border-left: 3px solid #f56c6c; }
.alert-item.warning { border-left: 3px solid #e6a23c; }
.alert-item.info { border-left: 3px solid #909399; }
.alert-time { font-size: 12px; color: #8c8c8c; white-space: nowrap; }
.alert-title { flex: 1; font-size: 14px; color: #1e3a4a; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.hot-process-card {
  background: white; border-radius: 16px; padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.process-rank-list {}
.process-rank-item {
  display: flex; align-items: center; gap: 12px; padding: 10px;
  border-radius: 8px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s;
}
.process-rank-item:hover { background: #f5f7fa; }
.rank-num {
  width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: bold; color: white;
}
.rank-num.rank-1 { background: linear-gradient(135deg, #ffd700, #ffb700); }
.rank-num.rank-2 { background: linear-gradient(135deg, #c0c0c0, #a0a0a0); }
.rank-num.rank-3 { background: linear-gradient(135deg, #cd7f32, #b87333); }
.rank-num.rank-4, .rank-num.rank-5 { background: #8c8c8c; }
.process-name { flex: 1; font-size: 14px; color: #1e3a4a; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.process-count { font-size: 12px; color: #8c8c8c; }

.license-card {
  background: white; border-radius: 16px; padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.license-content { display: flex; flex-direction: column; align-items: center; gap: 20px; }
.license-text { font-size: 24px; font-weight: bold; color: #1e3a4a; }
.license-label { font-size: 12px; color: #8c8c8c; }
.license-info { width: 100%; }
.info-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f5f7fa; }
.info-item:last-child { border-bottom: none; }
.info-item .label { color: #8c8c8c; font-size: 14px; }
.info-item .value { color: #1e3a4a; font-weight: 500; font-size: 14px; }

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(3, 1fr); }
  .main-charts-row { grid-template-columns: 1fr; }
  .bottom-row { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>