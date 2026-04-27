<template>
  <div class="rpa-layout">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <!-- Logo区域 -->
      <div class="header-left">
        <div class="logo-area" @click="goToDashboard">
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

      <!-- 导航菜单 -->
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

      <!-- 右侧工具栏 -->
      <div class="header-right">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="tool-badge">
          <el-button class="tool-btn" @click="goToNotifications">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>

        <el-dropdown trigger="click">
          <div class="user-avatar">
            <el-avatar :size="36" class="avatar-circle" v-if="!currentUser.avatar">
              {{ userInitial }}
            </el-avatar>
            <el-avatar :size="36" class="avatar-circle" v-else :src="getAvatarUrl(currentUser.avatar)" @error="handleAvatarError" />
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
    <div class="main-layout">
      <!-- 侧边栏 -->
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6 1.41-1.41z"/>
          </svg>
        </div>

        <nav class="sidebar-menu">
          <!-- 工作台 -->
          <div class="menu-item main-item"
            :class="{ active: activeMenu === 'dashboard' }"
            @click="switchMenu('dashboard')"
          >
            <el-icon class="menu-icon"><DataLine /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">工作台</span>
          </div>

          <!-- 任务调度中心 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'tasks' }"
            @click="switchMenu('tasks')"
          >
            <el-icon class="menu-icon"><List /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">任务调度中心</span>
          </div>

          <!-- 机器人管理 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'robots' }"
            @click="switchMenu('robots')"
          >
            <el-icon class="menu-icon"><Monitor /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">机器人管理</span>
          </div>

          <!-- 流程仓库 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'processes' }"
            @click="switchMenu('processes')"
          >
            <el-icon class="menu-icon"><Document /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">流程仓库</span>
          </div>

          <!-- 队列管理 -->
          <div class="menu-item submenu-item"
            :class="{ active: activeMenu === 'queues' }"
            @click="switchMenu('queues')"
          >
            <el-icon class="menu-icon"><Operation /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">队列管理</span>
          </div>

          <!-- 触发器管理 -->
          <div class="menu-item submenu-item"
            :class="{ active: activeMenu === 'triggers' }"
            @click="switchMenu('triggers')"
          >
            <el-icon class="menu-icon"><Timer /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">触发器管理</span>
          </div>

          <!-- 执行日志 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'logs' }"
            @click="switchMenu('logs')"
          >
            <el-icon class="menu-icon"><Tickets /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">执行日志</span>
          </div>

          <!-- 审计日志 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'audit' }"
            @click="switchMenu('audit')"
          >
            <el-icon class="menu-icon"><Clock /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">审计日志</span>
          </div>

          <!-- 凭据中心 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'credentials' }"
            @click="switchMenu('credentials')"
          >
            <el-icon class="menu-icon"><Key /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">凭据中心</span>
          </div>

          <!-- AI能力中心 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'ai' }"
            @click="switchMenu('ai')"
          >
            <el-icon class="menu-icon"><MagicStick /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">AI能力</span>
          </div>

          <!-- 智能录制器 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'recorder' }"
            @click="switchMenu('recorder')"
          >
            <el-icon class="menu-icon"><VideoPlay /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">智能录制</span>
          </div>

          <!-- 录屏管理 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'recording' }"
            @click="switchMenu('recording')"
          >
            <el-icon class="menu-icon"><VideoCamera /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">录屏管理</span>
          </div>

          <!-- 脚本执行器 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'script' }"
            @click="switchMenu('script')"
          >
            <el-icon class="menu-icon"><Promotion /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">脚本执行</span>
          </div>

          <!-- 数据脱敏 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'masking' }"
            @click="switchMenu('masking')"
          >
            <el-icon class="menu-icon"><View /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">数据脱敏</span>
          </div>

          <!-- 分布式锁 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'locks' }"
            @click="switchMenu('locks')"
          >
            <el-icon class="menu-icon"><Unlock /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">分布式锁</span>
          </div>

          <!-- 报表分析 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'reports' }"
            @click="switchMenu('reports')"
          >
            <el-icon class="menu-icon"><DataBoard /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">报表分析</span>
          </div>

          <!-- 数据管理（带子菜单） -->
          <div class="menu-group">
            <div class="menu-item has-submenu"
              :class="{ active: activeMenu.startsWith('data') || activeMenu === 'invoice' }"
              @click="toggleDataMenu"
            >
              <el-icon class="menu-icon"><DataAnalysis /></el-icon>
              <span class="menu-text" v-if="!sidebarCollapsed">数据管理</span>
              <span class="submenu-arrow" v-if="!sidebarCollapsed">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M7 10l5 5 5-5z"/>
                </svg>
              </span>
            </div>
            <Transition name="submenu">
              <div v-if="!sidebarCollapsed && showDataSubmenu" class="submenu">
                <div class="menu-item submenu-item"
                  :class="{ active: activeMenu === 'dataQuery' }"
                  @click="switchMenu('dataQuery')"
                >
                  <el-icon class="menu-icon"><Search /></el-icon>
                  <span class="menu-text">数据查询</span>
                </div>
              </div>
            </Transition>
          </div>

          <!-- 系统设置 -->
          <div class="menu-divider" v-if="!sidebarCollapsed"></div>
          <div class="menu-item"
            :class="{ active: activeMenu === 'settings' }"
            @click="switchMenu('settings')"
          >
            <el-icon class="menu-icon"><Tools /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">系统设置</span>
          </div>

          <!-- 通知管理 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'notifications' }"
            @click="switchMenu('notifications')"
          >
            <el-icon class="menu-icon"><Bell /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">通知管理</span>
          </div>
        </nav>
      </aside>

      <!-- 内容区 -->
      <main class="content-area">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataLine,
  Monitor,
  Setting,
  User,
  ArrowDown,
  List,
  Document,
  Tickets,
  Clock,
  Key,
  DataBoard,
  DataAnalysis,
  Download,
  Sort,
  Operation,
  Search,
  Bell,
  Connection,
  Tools,
  Timer,
  MagicStick,
  VideoCamera,
  Promotion,
  View,
  Unlock,
  VideoPlay,
  Odometer,
  SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const sidebarCollapsed = ref(false)
const showDataSubmenu = ref(true)
const activeTopMenu = ref('rpa')
const activeMenu = ref('tasks')
const unreadCount = ref(0)

const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  role: 1
})

const userName = computed(() => currentUser.value.realName || currentUser.value.username)
const userRole = computed(() => currentUser.value.role === 1 ? '管理员' : '普通用户')
const userInitial = computed(() => {
  const name = userName.value
  return name ? name.charAt(0).toUpperCase() : 'U'
})

// 顶部菜单选择
const handleTopMenuSelect = (index) => {
  if (index === 'dashboard') {
    router.push('/dashboard')
  } else if (index === 'system') {
    router.push('/system/profile')
  }
}

// 侧边栏菜单切换
const switchMenu = (menu) => {
  activeMenu.value = menu
const routeMap = {
  dashboard: '/dashboard',
  workbench: '/rpa/workbench',
  monitor: '/rpa/workbench',
  tasks: '/rpa/tasks',
  robots: '/rpa/robots',
  processes: '/rpa/processes',
  queues: '/rpa/queues',
  triggers: '/rpa/triggers',
  logs: '/rpa/logs',
  audit: '/rpa/audit',
  credentials: '/rpa/credentials',
  reports: '/rpa/reports',
  settings: '/rpa/settings',
  notifications: '/rpa/notifications',
  dataCollect: '/rpa/data-collect',
  dataParse: '/rpa/data-parse',
  dataProcess: '/rpa/data-process',
  dataQuery: '/rpa/data-query',
  invoice: '/rpa/invoice',
  ai: '/rpa/ai',
  recording: '/rpa/recording',
  script: '/rpa/script',
  masking: '/rpa/masking',
  locks: '/rpa/locks',
  recorder: '/rpa/recorder'
}
  router.push(routeMap[menu])
}

// 跳转到工作台
const goToDashboard = () => {
  router.push('/dashboard')
}

// 跳转到通知
const goToNotifications = () => {
  router.push('/rpa/notifications')
}

// 展开/收起数据管理子菜单
const toggleDataMenu = () => {
  showDataSubmenu.value = !showDataSubmenu.value
}

// 跳转到个人信息
const goToProfile = () => {
  router.push('/system/profile')
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出登录')
    router.push('/login')
  })
}

// 加载未读通知数
const loadUnreadCount = async () => {
  try {
    const { apiGet } = await import('../../utils/api.js')
    const result = await apiGet('/notification/stats')
    if (result?.code === 0 && result.data?.unreads) {
      const unreads = result.data.unreads
      unreadCount.value = (unreads.collect || 0) + (unreads.temp || 0) + (unreads.user || 0)
    }
  } catch (e) {
    // ignore
  }
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 获取头像完整 URL
const getAvatarUrl = (path) => {
  if (!path) return null
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return `${API_BASE}${path}`
}

// 处理头像加载失败
const handleAvatarError = (e) => {
  console.error('头像加载失败，回退到首字母显示')
  currentUser.value.avatar = null
}

// 加载用户信息
const loadUserInfo = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo)
      currentUser.value = { ...currentUser.value, ...user }
    } catch (e) {}
  }
}

onMounted(() => {
  loadUserInfo()

  // 监听 storage 事件，当其他页面修改 userInfo 时同步更新
  window.addEventListener('storage', (e) => {
    if (e.key === 'userInfo') {
      loadUserInfo()
    }
  })

  // 监听自定义的头像更新事件
  window.addEventListener('avatarUpdated', () => {
    loadUserInfo()
  })

  // 根据当前路由设置激活菜单
  const path = route.path
  if (path === '/dashboard' || path === '/') activeMenu.value = 'dashboard'
  else if (path.includes('/rpa/workbench') || path.includes('/rpa/monitor')) activeMenu.value = 'workbench'
  else if (path.includes('/rpa/tasks')) activeMenu.value = 'tasks'
  else if (path.includes('/rpa/robots')) activeMenu.value = 'robots'
  else if (path.includes('/rpa/processes')) activeMenu.value = 'processes'
  else if (path.includes('/rpa/queues')) activeMenu.value = 'queues'
  else if (path.includes('/rpa/triggers')) activeMenu.value = 'triggers'
  else if (path.includes('/rpa/logs')) activeMenu.value = 'logs'
  else if (path.includes('/rpa/audit')) activeMenu.value = 'audit'
  else if (path.includes('/rpa/credentials')) activeMenu.value = 'credentials'
  else if (path.includes('/rpa/reports')) activeMenu.value = 'reports'
  else if (path.includes('/rpa/settings')) activeMenu.value = 'settings'
  else if (path.includes('/rpa/notifications')) activeMenu.value = 'notifications'
  else if (path.includes('/rpa/data-collect')) activeMenu.value = 'dataCollect'
  else if (path.includes('/rpa/data-parse')) activeMenu.value = 'dataParse'
  else if (path.includes('/rpa/data-process')) activeMenu.value = 'dataProcess'
  else if (path.includes('/rpa/data-query')) activeMenu.value = 'dataQuery'
  else if (path.includes('/rpa/invoice')) activeMenu.value = 'invoice'

  else if (path.includes('/rpa/recorder')) activeMenu.value = 'recorder'

  // 加载未读通知数
  loadUnreadCount()
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('storage', loadUserInfo)
  window.removeEventListener('avatarUpdated', loadUserInfo)
})
</script>

<style scoped>
.rpa-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background: var(--bg-primary, #f0f2f5);
}

/* 顶部导航栏 - 深色主题 */
.dashboard-header {
  height: 58px;
  background: linear-gradient(135deg, #1a202c 0%, #2d3748 50%, #1a202c 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px 0 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  width: 100%;
  box-sizing: border-box;
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

.header-left::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, rgba(255,255,255,0.05) 0%, transparent 50%);
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

.logo-subtitle {
  color: rgba(255, 255, 255, 0.75);
  font-weight: 500;
  font-size: 12px;
  letter-spacing: 0.5px;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
  padding: 0 20px;
}

.top-menu {
  background-color: transparent;
  border-bottom: none;
  height: 60px;
}

.top-menu .el-menu-item {
  padding: 0 20px 0 14px;
  margin-left: 4px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  height: 60px;
  line-height: 60px;
  border-bottom: 2px solid transparent;
  transition: all 0.25s ease;
  border-radius: 8px 8px 0 0;
  position: relative;
}

.top-menu .el-menu-item::before {
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

.top-menu .el-menu-item:hover {
  color: rgba(255, 255, 255, 0.95);
  background-color: rgba(255, 255, 255, 0.08);
}

.top-menu .el-menu-item:hover::before {
  width: 60%;
}

.top-menu .el-menu-item.is-active {
  color: white;
  background-color: rgba(255, 255, 255, 0.1);
  font-weight: 600;
}

.top-menu .el-menu-item.is-active::before {
  width: 80%;
}

.top-menu .el-menu-item .el-icon {
  margin-right: 6px;
  font-size: 16px;
}

.header-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: auto;
  padding-right: 8px;
}

.tool-badge {
  cursor: pointer;
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
  transform: translateY(-1px);
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
  transform: translateY(-1px);
}

.avatar-circle {
  border: 2px solid rgba(255, 255, 255, 0.2);
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.user-meta {
  display: flex;
  flex-direction: column;
  white-space: nowrap;
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

.dropdown-arrow {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.user-avatar:hover .dropdown-arrow {
  transform: translateY(1px);
}

/* 主内容区域 */
.main-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
  width: 100%;
}

/* 侧边栏 - 优化过渡 */
.sidebar {
  width: 240px;
  background: #ffffff;
  border-right: 1px solid #e8eaed;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 4px 0 16px rgba(0, 0, 0, 0.06);
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-toggle {
  position: absolute;
  right: -12px;
  top: 24px;
  width: 24px;
  height: 24px;
  background: #ffffff;
  border: 1px solid #e0e3e8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  color: #6b7280;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.sidebar-toggle:hover {
  background: linear-gradient(135deg, #409eff 0%, #3178c6 100%);
  color: white;
  border-color: #409eff;
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.35);
}

.sidebar-menu {
  padding: 16px 12px;
  height: 100%;
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
  border-radius: 8px;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  margin-bottom: 2px;
  border: 1px solid transparent;
}

.menu-item:hover {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #2563eb;
  border-color: #e2e8f0;
}

.menu-item.active {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #2563eb;
  font-weight: 600;
  border: 1px solid #bfdbfe;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.1);
}

.menu-item.main-item {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #374151;
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
}

.menu-item.main-item:hover {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #1f2937;
  transform: translateX(2px);
  border-color: #d1d5db;
}

.menu-item.main-item.active {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #2563eb;
  border-color: #93c5fd;
}

.menu-icon {
  font-size: 17px;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.menu-item:hover .menu-icon {
  transform: scale(1.08);
  color: #2563eb;
}

.menu-item.active .menu-icon {
  color: #2563eb;
}

.menu-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.submenu-arrow {
  margin-left: auto;
  transition: transform 0.2s ease;
  color: #9ca3af;
  flex-shrink: 0;
}

.submenu-arrow svg {
  display: block;
}

.submenu {
  padding-left: 46px;
  margin-top: 2px;
}

.submenu-item {
  font-size: 13px;
  padding: 9px 14px;
  gap: 10px;
  color: #6b7280;
  border-radius: 6px;
}

.submenu-item:hover {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #2563eb;
}

.submenu-item.active {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #2563eb;
  font-weight: 600;
}

.submenu-item .menu-icon {
  font-size: 15px;
}

.menu-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #e5e7eb 20%, #e5e7eb 80%, transparent);
  margin: 12px 14px;
}

.menu-group {
  margin-top: 2px;
}

/* 内容区 - 统一背景 */
.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: linear-gradient(180deg, #f0f2f5 0%, #e8eaed 100%);
}

/* 动画 */
.submenu-enter-active,
.submenu-leave-active {
  transition: all 0.25s ease;
}

.submenu-enter-from,
.submenu-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* 滚动条样式 */
.sidebar-menu::-webkit-scrollbar,
.content-area::-webkit-scrollbar {
  width: 5px;
}

.sidebar-menu::-webkit-scrollbar-track,
.content-area::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-menu::-webkit-scrollbar-thumb,
.content-area::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.sidebar-menu::-webkit-scrollbar-thumb:hover,
.content-area::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
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

/* 响应式支持 */
@media (max-width: 1024px) {
  .header-left {
    width: 200px;
    min-width: 200px;
  }
  
  .top-menu .el-menu-item {
    padding: 0 14px;
    font-size: 13px;
  }
  
  .top-menu .el-menu-item .el-icon {
    margin-right: 4px;
  }
  
  .logo-text {
    font-size: 14px;
  }
}
</style>