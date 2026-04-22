<template>
  <div class="system-layout">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <!-- Logo区域 -->
      <div class="header-left">
        <div class="logo-area">
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
      <!-- 左侧菜单 -->
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path v-if="sidebarCollapsed" d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
            <path v-else d="M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6 1.41-1.41z"/>
          </svg>
        </div>

        <nav class="sidebar-menu">
          <!-- 首页菜单 -->
          <template v-if="activeTopMenu === 'dashboard'">
            <div class="menu-item main-item" @click="switchMenu('/dashboard')">
              <el-icon class="menu-icon"><DataLine /></el-icon>
              <span class="menu-text">仪表盘</span>
            </div>
          </template>

          <!-- RPA运营管理菜单 -->
          <template v-if="activeTopMenu === 'rpa'">
            <div class="menu-item main-item" @click="switchMenu('/rpa/tasks')">
              <el-icon class="menu-icon"><DataLine /></el-icon>
              <span class="menu-text">工作台</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/tasks' }" @click="switchMenu('/rpa/tasks')">
              <el-icon class="menu-icon"><List /></el-icon>
              <span class="menu-text">任务调度</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/robots' }" @click="switchMenu('/rpa/robots')">
              <el-icon class="menu-icon"><Monitor /></el-icon>
              <span class="menu-text">机器人管理</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/processes' }" @click="switchMenu('/rpa/processes')">
              <el-icon class="menu-icon"><Document /></el-icon>
              <span class="menu-text">流程仓库</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/queues' }" @click="switchMenu('/rpa/queues')">
              <el-icon class="menu-icon"><Operation /></el-icon>
              <span class="menu-text">队列管理</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/triggers' }" @click="switchMenu('/rpa/triggers')">
              <el-icon class="menu-icon"><Timer /></el-icon>
              <span class="menu-text">触发器管理</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/logs' }" @click="switchMenu('/rpa/logs')">
              <el-icon class="menu-icon"><Tickets /></el-icon>
              <span class="menu-text">执行日志</span>
            </div>
            <div class="menu-divider"></div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/credentials' }" @click="switchMenu('/rpa/credentials')">
              <el-icon class="menu-icon"><Key /></el-icon>
              <span class="menu-text">凭据中心</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/reports' }" @click="switchMenu('/rpa/reports')">
              <el-icon class="menu-icon"><DataBoard /></el-icon>
              <span class="menu-text">报表分析</span>
            </div>
          </template>

          <!-- 系统管理菜单 -->
          <template v-if="activeTopMenu === 'system'">
            <div class="menu-item" :class="{ active: activeLeftMenu === '/system/profile' }" @click="switchMenu('/system/profile')">
              <el-icon class="menu-icon"><User /></el-icon>
              <span class="menu-text">个人信息</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/system/users' }" @click="switchMenu('/system/users')">
              <el-icon class="menu-icon"><UserFilled /></el-icon>
              <span class="menu-text">用户管理</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/system/roles' }" @click="switchMenu('/system/roles')">
              <el-icon class="menu-icon"><Management /></el-icon>
              <span class="menu-text">角色管理</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/system/resources' }" @click="switchMenu('/system/resources')">
              <el-icon class="menu-icon"><FolderOpened /></el-icon>
              <span class="menu-text">资源管理</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/settings' }" @click="switchMenu('/rpa/settings')">
              <el-icon class="menu-icon"><Tools /></el-icon>
              <span class="menu-text">系统设置</span>
            </div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/rpa/notifications' }" @click="switchMenu('/rpa/notifications')">
              <el-icon class="menu-icon"><Bell /></el-icon>
              <span class="menu-text">通知管理</span>
            </div>
          </template>
        </nav>
      </aside>

      <!-- 右侧内容区 -->
      <main class="content-area">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Odometer, VideoCamera, Setting, User, Bell, ArrowDown, SwitchButton,
  DataLine, List, Monitor, Document, Operation, Timer, Tickets, Key, DataBoard,
  UserFilled, Management, FolderOpened, Tools
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const sidebarCollapsed = ref(false)
const activeTopMenu = ref('system')
const activeLeftMenu = ref('/system/profile')
const unreadCount = ref(0)

const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  role: 1
})

const userName = computed(() => currentUser.value.realName || currentUser.value.username)
const userInitial = computed(() => userName.value.charAt(0).toUpperCase())
const userRole = computed(() => currentUser.value.role === 1 ? '管理员' : '用户')

// 监听路由变化
watch(
  () => route.path,
  (newPath) => {
    if (newPath.startsWith('/dashboard')) {
      activeTopMenu.value = 'dashboard'
      activeLeftMenu.value = '/dashboard'
    } else if (newPath.startsWith('/rpa')) {
      activeTopMenu.value = 'rpa'
      activeLeftMenu.value = newPath
    } else if (newPath.startsWith('/system')) {
      activeTopMenu.value = 'system'
      activeLeftMenu.value = newPath
    }
  },
  { immediate: true }
)

const handleTopMenuSelect = (index) => {
  activeTopMenu.value = index
  if (index === 'dashboard') {
    router.push('/dashboard')
  } else if (index === 'rpa') {
    router.push('/rpa/tasks')
  } else if (index === 'system') {
    router.push('/system/profile')
  }
}

const switchMenu = (path) => {
  activeLeftMenu.value = path
  router.push(path)
}

const goToProfile = () => {
  router.push('/system/profile')
}

const goToNotifications = () => {
  router.push('/rpa/notifications')
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出登录')
    router.push('/login')
  })
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

const loadUnreadCount = async () => {
  try {
    const { apiGet } = await import('../../utils/api.js')
    const result = await apiGet('/notification/stats')
    if (result?.code === 0 && result.data?.unreads) {
      const unreads = result.data.unreads
      unreadCount.value = (unreads.collect || 0) + (unreads.temp || 0) + (unreads.user || 0)
    }
  } catch (e) {}
}

onMounted(() => {
  loadUserInfo()
  loadUnreadCount()

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
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('storage', loadUserInfo)
  window.removeEventListener('avatarUpdated', loadUserInfo)
})
</script>

<style scoped>
/* ===== 统一设计系统 - 浅色主题 ===== */
.system-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background: var(--bg-primary, #f5f7fa);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* ===== 顶部导航栏 ===== */
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

/* ===== 主内容区域 ===== */
.main-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
  width: 100%;
}

/* ===== 侧边栏 ===== */
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
  background: #f9fafb;
  color: #409eff;
  border-color: #409eff;
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
  color: #409eff;
}

.menu-item.active {
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 100%);
  color: #409eff;
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

.menu-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 12px 14px;
}

/* ===== 内容区 ===== */
.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #f5f7fa;
}

/* ===== 滚动条 ===== */
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
