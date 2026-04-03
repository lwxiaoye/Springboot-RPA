<template>
  <div class="rpa-layout">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <div class="header-left">
        <div class="logo-area" @click="goToDashboard">
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
            <el-icon><DataLine /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="rpa">
            <el-icon><Monitor /></el-icon>
            <span>RPA运营管理</span>
          </el-menu-item>
          <el-menu-item index="system">
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <!-- 通知图标 -->
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
          <el-icon class="notification-icon" @click="goToNotifications"><Bell /></el-icon>
        </el-badge>
        <div class="user-info">
          <el-dropdown>
            <span class="user-dropdown">
              <el-icon><User /></el-icon>
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

          <!-- 队列与触发器 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'queue' }"
            @click="switchMenu('queue')"
          >
            <el-icon class="menu-icon"><Connection /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">队列与触发器</span>
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
import { ref, onMounted } from 'vue'
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
  Tools
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
  queue: '/rpa/queue',
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
  invoice: '/rpa/invoice'
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
  else if (path.includes('/rpa/queue')) activeMenu.value = 'queue'
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
  cursor: pointer;
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
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-badge {
  cursor: pointer;
}

.notification-icon {
  font-size: 20px;
  color: #ffffffa6;
  cursor: pointer;
}

.notification-icon:hover {
  color: #fff;
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

/* 主内容区域 */
.main-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  position: relative;
  transition: width .3s;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-toggle {
  position: absolute;
  right: -12px;
  top: 20px;
  width: 24px;
  height: 24px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  color: #8c8c8c;
  transition: all .2s;
}

.sidebar-toggle:hover {
  background: #f5f7fa;
  color: #1890ff;
}

.sidebar-menu {
  padding: 12px 8px;
  height: 100%;
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  color: #595961;
  cursor: pointer;
  transition: all .2s;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
}

.menu-item:hover {
  background: #f5f7fa;
  color: #1890ff;
}

.menu-item.active {
  background: #e6f4ff;
  color: #1890ff;
  font-weight: 500;
}

.menu-item.main-item {
  background: linear-gradient(135deg, #1677ff 0%, #409eff 100%);
  color: white;
  margin-bottom: 8px;
}

.menu-item.main-item:hover {
  background: linear-gradient(135deg, #409eff 0%, #1677ff 100%);
  color: white;
}

.menu-item.main-item.active {
  background: linear-gradient(135deg, #409eff 0%, #1677ff 100%);
  color: white;
}

.menu-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.menu-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.submenu-arrow {
  margin-left: auto;
  transition: transform 0.2s;
}

.submenu {
  padding-left: 46px;
  margin-top: 4px;
}

.submenu-item {
  font-size: 13px;
  padding: 10px 14px;
  gap: 10px;
}

.submenu-item .menu-icon {
  font-size: 16px;
}

.menu-divider {
  height: 1px;
  background: #e8e8e8;
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
}

/* 动画 */
.submenu-enter-active, .submenu-leave-active {
  transition: all 0.2s ease;
}

.submenu-enter-from, .submenu-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>