<template>
  <div class="rpa-layout">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <!-- Logo区域 -->
      <div class="header-left">
        <div class="logo-area" @click="goToDashboard">
          <div class="logo-icon">RPA</div>
          <div class="logo-text">RPA运营管理系统</div>
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

          <!-- AI流程助手 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'ai-assistant' }"
            @click="switchMenu('ai-assistant')"
          >
            <el-icon class="menu-icon"><ChatLineSquare /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">AI流程助手</span>
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
import { ref, onMounted, computed } from 'vue'
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
  ChatLineSquare,
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
  'ai-assistant': '/rpa/ai-assistant',
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

onMounted(() => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo)
      currentUser.value = { ...currentUser.value, ...user }
    } catch (e) {}
  }

  // 根据当前路由设置激活菜单
  const path = route.path
  if (path === '/dashboard' || path === '/') activeMenu.value = 'dashboard'
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

  else if (path.includes('/rpa/ai-assistant')) activeMenu.value = 'ai-assistant'
  else if (path.includes('/rpa/recorder')) activeMenu.value = 'recorder'

  // 加载未读通知数
  loadUnreadCount()
})
</script>

<style scoped>
.rpa-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background: var(--bg-primary, #f5f7fa);
}

/* 顶部导航栏 */
.dashboard-header {
  height: 64px;
  background: var(--bg-secondary, #ffffff);
  border-bottom: 1px solid var(--border-color, #e5e7eb);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
  width: 100%;
  box-sizing: border-box;
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
  transition: opacity 0.2s;
}

.logo-area:hover {
  opacity: 0.85;
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
  flex-shrink: 0;
}

.logo-text {
  color: var(--text-primary, #1f2937);
  font-weight: 600;
  font-size: 18px;
  white-space: nowrap;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
}

.top-menu {
  background-color: transparent;
  border-bottom: none;
}

.top-menu .el-menu-item {
  padding: 0 24px 0 14px;
  margin-left: 10px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-secondary, #6b7280);
  height: 64px;
  line-height: 64px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
}

.top-menu .el-menu-item:hover {
  color: var(--primary, #409eff);
  background-color: rgba(64, 158, 255, 0.05);
}

.top-menu .el-menu-item.is-active {
  color: var(--primary, #409eff);
  border-bottom-color: var(--primary, #409eff);
  background-color: transparent;
}

.top-menu .el-menu-item .el-icon {
  margin-right: 6px;
  font-size: 18px;
}

.header-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: auto;
}

.tool-badge {
  cursor: pointer;
}

.tool-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary, #6b7280);
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s;
}

.tool-btn:hover {
  background: var(--bg-tertiary, #f9fafb);
  color: var(--primary, #409eff);
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
  background: var(--bg-tertiary, #f9fafb);
}

.avatar-circle {
  border: 2px solid var(--border-color, #e5e7eb);
  flex-shrink: 0;
}

.user-meta {
  display: flex;
  flex-direction: column;
  white-space: nowrap;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  line-height: 1.2;
}

.user-role {
  font-size: 12px;
  color: var(--text-tertiary, #9ca3af);
  line-height: 1.2;
}

.dropdown-arrow {
  font-size: 12px;
  color: var(--text-tertiary, #9ca3af);
  flex-shrink: 0;
}

/* 主内容区域 */
.main-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
  width: 100%;
}

/* 侧边栏 */
.sidebar {
  width: 240px;
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  position: relative;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
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
  border: 1px solid #e5e7eb;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  color: #6b7280;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.sidebar-toggle:hover {
  background: var(--bg-tertiary, #f9fafb);
  color: var(--primary, #409eff);
  border-color: var(--primary, #409eff);
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
  padding: 12px 14px;
  border-radius: 10px;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.menu-item:hover {
  background: #f3f4f6;
  color: var(--primary, #409eff);
  border-color: transparent;
}

.menu-item.active {
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 100%);
  color: var(--primary, #409eff);
  font-weight: 600;
  border: 1px solid rgba(64, 158, 255, 0.2);
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.08);
}

.menu-item.main-item {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.menu-item.main-item:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.menu-item.main-item.active {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

.menu-icon {
  font-size: 18px;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.menu-item:hover .menu-icon {
  transform: scale(1.1);
}

.menu-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.submenu-arrow {
  margin-left: auto;
  transition: transform 0.2s;
  color: #9ca3af;
}

.submenu-arrow svg {
  display: block;
}

.submenu {
  padding-left: 46px;
  margin-top: 4px;
}

.submenu-item {
  font-size: 13px;
  padding: 10px 14px;
  gap: 10px;
  color: #6b7280;
  border-radius: 8px;
}

.submenu-item:hover {
  background: #f3f4f6;
  color: var(--primary, #409eff);
}

.submenu-item.active {
  background: #e6f4ff;
  color: var(--primary, #409eff);
  font-weight: 600;
}

.submenu-item .menu-icon {
  font-size: 16px;
}

.menu-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 12px 14px;
}

.menu-group {
  margin-top: 4px;
}

/* 内容区 */
.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: var(--bg-primary, #f5f7fa);
}

/* 动画 */
.submenu-enter-active,
.submenu-leave-active {
  transition: all 0.2s ease;
}

.submenu-enter-from,
.submenu-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 滚动条样式 */
.sidebar-menu::-webkit-scrollbar,
.content-area::-webkit-scrollbar {
  width: 6px;
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
</style>