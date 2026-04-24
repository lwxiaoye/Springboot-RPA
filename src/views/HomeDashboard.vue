<template>
  <div class="home-layout">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="logo-area" @click="goToHome">
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
          <el-menu-item index="dashboard">
            <el-icon><Odometer /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="monitor">
            <el-icon><DataLine /></el-icon>
            <span>实时监控</span>
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
              <div class="stat-label">待执行任务</div>
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
              <div class="stat-label">在线机器人</div>
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
              <div class="stat-label">今日执行</div>
              <div class="stat-sub success">{{ stats.successRate }}% 成功率</div>
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
              <div class="stat-label">异常任务</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 最近执行日志 -->
      <section class="logs-section">
        <div class="section-header">
          <h3>最近执行</h3>
          <el-button size="small" @click="goTo('/rpa/logs')">查看全部</el-button>
        </div>
        <div class="logs-table">
          <el-table :data="recentLogs" size="small" max-height="200">
            <el-table-column prop="taskName" label="任务" min-width="120" show-overflow-tooltip />
            <el-table-column prop="robotName" label="机器人" width="100" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <span class="status-badge" :class="row.status">{{ getStatusText(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="时间" width="150" />
          </el-table>
        </div>
      </section>
    </main>

    <!-- 告警弹窗 -->
    <el-dialog v-model="showAlerts" title="告警详情" width="500px">
      <div class="alert-list">
        <div v-for="alert in alerts" :key="alert.id" class="alert-item" :class="alert.level">
          <div class="alert-content">
            <span class="alert-title">{{ alert.title }}</span>
            <span class="alert-message">{{ alert.message }}</span>
          </div>
          <el-button size="small" type="primary" @click="handleAlert(alert)">处理</el-button>
        </div>
        <div v-if="alerts.length === 0" class="empty-alerts">
          暂无告警
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Odometer, DataLine, VideoCamera, Setting, Bell, User, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { apiGet } from '../utils/api.js'

const router = useRouter()
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
const userRole = computed(() => currentUser.value.role === 1 ? '管理员' : '普通用户')
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
  const map = { success: '成功', completed: '完成', failed: '失败', running: '进行中', pending: '等待' }
  return map[status] || status
}

const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出登录')
    router.push('/login')
  })
}

const handleAlert = (alert) => {
  alerts.value = alerts.value.filter(a => a.id !== alert.id)
  ElMessage.success('告警已处理')
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
  background: #f5f7fa;
}

.dashboard-header {
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.header-left {
  flex-shrink: 0;
  width: 280px;
  min-width: 280px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.logo-icon {
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

.logo-text {
  color: #1f2937;
  font-weight: 600;
  font-size: 18px;
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
  padding: 0 24px;
  font-size: 15px;
  font-weight: 500;
  color: #6b7280;
  height: 64px;
  line-height: 64px;
}

.top-menu .el-menu-item:hover {
  color: #409eff;
  background-color: rgba(64, 158, 255, 0.05);
}

.top-menu .el-menu-item.is-active {
  color: #409eff;
  border-bottom-color: #409eff;
}

.header-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.ws-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d1d5db;
}

.ws-indicator.connected {
  background: #22c55e;
}

.tool-btn {
  border: none;
  background: transparent;
  color: #6b7280;
  padding: 8px;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px 6px 6px;
  border-radius: 24px;
}

.user-avatar:hover {
  background: #f9fafb;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.user-role {
  font-size: 12px;
  color: #9ca3af;
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
</style>
