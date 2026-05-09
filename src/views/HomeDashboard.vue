<template>
  <div class="home-layout">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="logo-area" @click="goToHome">
          <div class="logo-icon">
            <div class="hexagon">
              <svg viewBox="0 0 100 100" width="36" height="36">
                <defs>
                  <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                    <stop offset="50%" style="stop-color:#0077ff;stop-opacity:1" />
                    <stop offset="100%" style="stop-color:#0055cc;stop-opacity:1" />
                  </linearGradient>
                </defs>
                <polygon points="50,5 95,27.5 95,72.5 50,95 5,72.5 5,27.5" fill="none" stroke="url(#logoGradient)" stroke-width="3"/>
                <path d="M30 50 L45 35 L45 45 L70 45 L70 55 L45 55 L45 65 Z" fill="url(#logoGradient)" opacity="0.9"/>
                <circle cx="70" cy="35" r="4" fill="#00ffcc" opacity="0.8"/>
              </svg>
            </div>
          </div>
          <div class="logo-text">
            <span class="logo-title">RPA</span>
            <span class="logo-subtitle">Enterprise Platform</span>
          </div>
          <div class="logo-accent"></div>
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
          <el-menu-item index="dashboard">
            <el-icon><Odometer /></el-icon>
            <span>{{ t('menu.dashboard') }}</span>
          </el-menu-item>
          <el-menu-item index="monitor">
            <el-icon><DataLine /></el-icon>
            <span>{{ t('menu.monitor') }}</span>
          </el-menu-item>
          <el-menu-item index="rpa">
            <el-icon><VideoCamera /></el-icon>
            <span>{{ t('menu.rpaManagement') }}</span>
          </el-menu-item>
          <el-menu-item index="system">
            <el-icon><Setting /></el-icon>
            <span>{{ t('menu.systemManagement') }}</span>
          </el-menu-item>
        </el-menu>
      </div>

      <div class="header-right">
        <div class="ws-indicator" :class="{ connected: wsConnected }" :title="wsConnected ? '实时数据已连接' : '实时数据未连接'">
          <span class="ws-dot"></span>
        </div>

        <el-badge :value="alertCount" :hidden="alertCount === 0" class="tool-badge">
          <el-button class="tool-btn" @click="showAlerts = true">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>

        <el-dropdown trigger="click">
          <div class="user-avatar">
            <el-avatar :size="36" class="avatar-circle">
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
                {{ t('menu.profile') }}
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                {{ t('menu.logout') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主体内容区域 -->
    <main class="content-area">
      <!-- 核心指标卡片 -->
      <section class="stats-section">
        <div class="stats-grid">
          <div class="stat-card primary" @click="goTo('/rpa/tasks')">
            <div class="stat-bg"></div>
            <div class="stat-icon">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
                <rect x="9" y="3" width="6" height="4" rx="1"/>
                <path d="M9 14l2 2 4-4"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.pendingTasks }}</div>
              <div class="stat-label">{{ t('dashboard.pendingTasks') }}</div>
            </div>
          </div>

          <div class="stat-card success" @click="goTo('/rpa/robots')">
            <div class="stat-bg"></div>
            <div class="stat-icon">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
                <line x1="12" y1="17" x2="12" y2="21"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">
                {{ stats.onlineRobots }}
                <span class="stat-total">/ {{ stats.totalRobots }}</span>
              </div>
              <div class="stat-label">{{ t('dashboard.onlineRobots') }}</div>
            </div>
          </div>

          <div class="stat-card info" @click="goTo('/rpa/logs')">
            <div class="stat-bg"></div>
            <div class="stat-icon">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="5 3 19 12 5 21 5 3"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(stats.todayExecutions) }}</div>
              <div class="stat-label">{{ t('dashboard.todayExecutions') }}</div>
              <div class="stat-sub success">{{ stats.successRate }}% {{ t('dashboard.successRate') }}</div>
            </div>
          </div>

          <div class="stat-card" :class="{ danger: stats.failedTasks > 0 }" @click="goTo('/rpa/logs?type=failed')">
            <div class="stat-bg"></div>
            <div class="stat-icon">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.failedTasks }}</div>
              <div class="stat-label">{{ t('dashboard.failedTasks') }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 最近执行日志 -->
      <section class="logs-section">
        <div class="section-header">
          <h3>{{ t('dashboard.recentExecutions') }}</h3>
          <el-button size="small" @click="goTo('/rpa/logs')">{{ t('dashboard.viewAll') }}</el-button>
        </div>
        <div class="logs-table">
          <el-table :data="recentLogs" size="small" max-height="200">
            <el-table-column prop="taskName" :label="t('task.taskName')" min-width="120" show-overflow-tooltip />
            <el-table-column prop="robotName" :label="t('monitor.robotName')" width="100" show-overflow-tooltip />
            <el-table-column prop="status" :label="t('common.status')" width="80" align="center">
              <template #default="{ row }">
                <span class="status-badge" :class="row.status">{{ getStatusText(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" :label="t('task.startTime')" width="150" />
          </el-table>
        </div>
      </section>
    </main>

    <!-- 告警弹窗 -->
    <el-dialog v-model="showAlerts" :title="t('dashboard.alertDetails')" width="500px">
      <div class="alert-list">
        <div v-for="alert in alerts" :key="alert.id" class="alert-item" :class="alert.level">
          <div class="alert-content">
            <span class="alert-title">{{ alert.title }}</span>
            <span class="alert-message">{{ alert.message }}</span>
          </div>
          <el-button size="small" type="primary" @click="handleAlert(alert)">{{ t('dashboard.handle') }}</el-button>
        </div>
        <div v-if="alerts.length === 0" class="empty-alerts">
          {{ t('dashboard.noAlerts') }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Odometer, DataLine, VideoCamera, Setting, Bell, User, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { apiGet } from '../utils/api.js'

const router = useRouter()
const { t } = useI18n()
const showAlerts = ref(false)
const activeTopMenu = ref('dashboard')
const wsConnected = ref(true)

const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  role: 1
})

const userName = computed(() => currentUser.value.realName || currentUser.value.username)
const userRole = computed(() => currentUser.value.role === 1 ? t('user.admin') : t('user.normalUser'))
const userInitial = computed(() => {
  const name = userName.value
  return name ? name.charAt(0).toUpperCase() : 'U'
})

const stats = reactive({
  pendingTasks: 0,
  failedTasks: 0,
  onlineRobots: 0,
  totalRobots: 0,
  todayExecutions: 0,
  successRate: 100
})

const alerts = ref([])
const alertCount = computed(() => alerts.value.length)
const recentLogs = ref([])

const goTo = (path) => router.push(path)
const goToHome = () => router.push('/')
const goToProfile = () => router.push('/system/profile')

const handleTopMenuSelect = (index) => {
  activeTopMenu.value = index
  if (index === 'dashboard') router.push('/')
  else if (index === 'monitor') router.push('/rpa/monitor')
  else if (index === 'rpa') router.push('/rpa/tasks')
  else if (index === 'system') router.push('/system/profile')
}

const getStatusText = (status) => {
  const map = {
    success: t('task.statusSuccess'),
    completed: t('task.statusCompleted'),
    failed: t('task.statusFailed'),
    running: t('task.statusRunning'),
    pending: t('task.statusPending')
  }
  return map[status] || status
}

const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const handleLogout = () => {
  ElMessageBox.confirm(t('confirm.logout'), t('common.tips'), {
    type: 'warning',
    confirmButtonPosition: 'right',
    distinguishCancelAndClose: true
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success(t('login.logoutSuccess'))
    router.push('/login')
  }).catch(() => {})
}

const handleAlert = (alert) => {
  alerts.value = alerts.value.filter(a => a.id !== alert.id)
  ElMessage.success(t('dashboard.alertHandled'))
}

const loadData = async () => {
  try {
    const [taskRes, robotRes, logRes] = await Promise.all([
      apiGet('/task').catch(() => ({ code: -1, data: [] })),
      apiGet('/robot').catch(() => ({ code: -1, data: [] })),
      apiGet('/log').catch(() => ({ code: -1, data: [] }))
    ])

    if (taskRes.code === 0 && taskRes.data) {
      const tasks = taskRes.data
      stats.pendingTasks = tasks.filter(t => t.status === 'pending' || t.status === 'assigned').length
      stats.failedTasks = tasks.filter(t => t.status === 'failed').length
    }

    if (robotRes.code === 0 && robotRes.data) {
      const robots = robotRes.data
      stats.totalRobots = robots.length
      stats.onlineRobots = robots.filter(r => r.status !== 'offline').length
    }

    if (logRes.code === 0 && logRes.data) {
      const logs = logRes.data
      stats.todayExecutions = logs.length
      const success = logs.filter(l => l.status === 'success' || l.status === 'completed').length
      stats.successRate = logs.length > 0 ? Math.round((success / logs.length) * 100) : 100
      recentLogs.value = logs.slice(0, 5).map(l => ({
        taskName: l.taskName || l.processName || '未知任务',
        robotName: l.robotName || 'Robot-01',
        status: l.status,
        startTime: l.startTime || '-'
      }))
    }
  } catch (e) {
    console.error('加载数据失败:', e)
  }
}

onMounted(() => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUser.value = { ...currentUser.value, ...user }
    } catch (e) {}
  }
  loadData()
})
</script>

<style scoped>
.home-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f0f2f5;
}

.dashboard-header {
  height: 58px;
  background: linear-gradient(135deg, #1a202c 0%, #2d3748 50%, #1a202c 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 0 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.header-left {
  flex-shrink: 0;
  width: 220px;
  min-width: 220px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2d3748 0%, #1a202c 100%);
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
}

.header-left::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.15) 0%, transparent 60%);
  pointer-events: none;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
}

.logo-area:hover {
  transform: scale(1.02);
}

.logo-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
}

.hexagon {
  position: relative;
  animation: logoFloat 3s ease-in-out infinite;
}

@keyframes logoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-title {
  color: white;
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 2px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  font-family: 'Orbitron', 'Roboto Mono', monospace;
}

.logo-subtitle {
  color: rgba(0, 212, 255, 0.85);
  font-weight: 500;
  font-size: 10px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.logo-accent {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #00d4ff, transparent);
  animation: accentPulse 2s ease-in-out infinite;
}

@keyframes accentPulse {
  0%, 100% { opacity: 0.5; width: 60%; }
  50% { opacity: 1; width: 80%; }
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 20px;
}

.top-menu {
  background-color: transparent;
  border-bottom: none;
  height: 58px;
}

.top-menu :deep(.el-menu-item) {
  padding: 0 20px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  height: 58px;
  line-height: 58px;
  border-bottom: 2px solid transparent;
  transition: all 0.25s ease;
  border-radius: 8px 8px 0 0;
  position: relative;
}

.top-menu :deep(.el-menu-item)::before {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #409eff, #66b1ff);
  transition: width 0.3s ease;
}

.top-menu :deep(.el-menu-item:hover) {
  color: rgba(255, 255, 255, 0.95);
  background-color: rgba(255, 255, 255, 0.08);
}

.top-menu :deep(.el-menu-item:hover)::before {
  width: 60%;
}

.top-menu :deep(.el-menu-item.is-active) {
  color: white;
  background-color: rgba(255, 255, 255, 0.1);
  font-weight: 600;
}

.top-menu :deep(.el-menu-item.is-active)::before {
  width: 80%;
}

.header-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 8px;
}

.ws-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transition: background 0.3s ease;
}

.ws-indicator.connected {
  background: #22c55e;
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.5);
}

.tool-btn {
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.75);
  padding: 8px;
  border-radius: 8px;
  transition: all 0.25s ease;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.tool-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border-color: rgba(255, 255, 255, 0.2);
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px 6px 6px;
  border-radius: 10px;
  transition: all 0.25s ease;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.user-avatar:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.18);
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  line-height: 1.2;
}

.user-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.2;
}

.content-area {
  flex: 1;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

.stats-section {
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  cursor: pointer;
  transition: all 0.25s;
  position: relative;
  overflow: hidden;
}

.stat-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-card.danger {
  border-color: rgba(239, 68, 68, 0.3);
}

.stat-bg {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  opacity: 0.08;
}

.stat-card.primary .stat-bg { background: #409eff; }
.stat-card.success .stat-bg { background: #22c55e; }
.stat-card.info .stat-bg { background: #06b6d4; }
.stat-card.danger .stat-bg { background: #ef4444; }

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card.primary .stat-icon { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.stat-card.success .stat-icon { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
.stat-card.info .stat-icon { background: rgba(6, 182, 212, 0.1); color: #06b6d4; }
.stat-card.danger .stat-icon { background: rgba(239, 68, 68, 0.1); color: #ef4444; }

.stat-content { flex: 1; }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.1;
}

.stat-total {
  font-size: 16px;
  font-weight: 400;
  color: #6b7280;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

.stat-sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.stat-sub.success { color: #22c55e; font-weight: 500; }

.logs-section {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.status-badge {
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.success, .status-badge.completed { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
.status-badge.failed { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.status-badge.running { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.status-badge.pending { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  border-left: 3px solid;
}

.alert-item.danger { border-color: #ef4444; }
.alert-item.warning { border-color: #f59e0b; }

.alert-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.alert-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.alert-message {
  font-size: 12px;
  color: #6b7280;
}

.empty-alerts {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .header-left { width: auto; min-width: auto; }
}

/* 头像样式 */
.avatar-circle {
  border: 2px solid rgba(255, 255, 255, 0.2);
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.dropdown-arrow {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

/* 暗色导航栏下拉菜单覆盖样式 */
:deep(.el-dropdown-menu) {
  background: #ffffff;
  border: 1px solid #e8eaed;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  padding: 6px;
}

:deep(.el-dropdown-menu__item) {
  border-radius: 6px;
  padding: 10px 14px;
  font-size: 13px;
  color: #4b5563;
  transition: all 0.2s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #2563eb;
}

:deep(.el-dropdown-menu__item.is-divided) {
  margin-top: 6px;
  border-top: 1px solid #e5e7eb;
  padding-top: 10px;
}

/* 暗色主题下的徽章 */
:deep(.el-badge__content) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}
</style>
