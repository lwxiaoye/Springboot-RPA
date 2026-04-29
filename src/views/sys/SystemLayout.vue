<template>
  <div class="system-layout">
    <!-- 全局水印层 -->
    <WatermarkOverlay ref="watermarkRef" />

    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <!-- Logo区域 -->
      <div class="header-left">
        <div class="logo-area" @click="router.push('/rpa/collaboration')">
          <div class="logo-icon">
            <img src="/title.png" alt="logo" style="width:36px;height:36px;" />
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
        <!-- 聊天消息按钮 -->
        <el-badge :value="chatUnreadCount" :hidden="chatUnreadCount === 0" class="tool-badge" :max="99">
          <el-button class="tool-btn" @click="goToCollaboration">
            <el-icon><ChatLineSquare /></el-icon>
          </el-button>
        </el-badge>

        <!-- 通知按钮 -->
        <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="tool-badge" :max="99">
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
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
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
            <div class="menu-section-title">系统设置</div>
            <div class="menu-item" :class="{ active: activeLeftMenu === '/system/profile' }" @click="switchMenu('/system/profile')">
              <el-icon class="menu-icon"><User /></el-icon>
              <span class="menu-text">个人信息</span>
            </div>
            <div class="menu-section-title">用户权限</div>
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
import WatermarkOverlay from '../../components/watermark/WatermarkOverlay.vue'
import {
  Odometer, VideoCamera, Setting, User, Bell, ArrowDown, SwitchButton,
  DataLine, List, Monitor, Document, Operation, Timer, Tickets, Key, DataBoard,
  UserFilled, Management, FolderOpened, Tools, ChatLineSquare
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const watermarkRef = ref(null)

const sidebarCollapsed = ref(false)
const activeTopMenu = ref('system')
const activeLeftMenu = ref('/system/profile')
const notificationCount = ref(0)
const chatUnreadCount = ref(0)

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

// 跳转到协作中枢
const goToCollaboration = () => {
  router.push('/rpa/collaboration')
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    type: 'warning',
    confirmButtonPosition: 'right',
    distinguishCancelAndClose: true
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}


// 获取头像完整 URL
const getAvatarUrl = (path) => {
  if (!path) return null
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  // 修复旧的头像路径格式（去掉开头的 /api）
  if (path.startsWith('/api/')) {
    path = path.replace('/api/', '/')
  }
  // 拼接 API 基础 URL
  return `/api${path}`
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

    // 获取公告未读数（来自新公告系统）
    const announcementResult = await apiGet('/announcement/list')
    if (announcementResult?.code === 0 && announcementResult.data) {
      const unreadCount = announcementResult.data.filter(a => a.status === 'unread').length
      notificationCount.value = unreadCount
    }

    // 获取聊天未读数
    const chatResult = await apiGet('/chat/conversations?userId=1')
    if (chatResult?.code === 0 && chatResult.data) {
      const totalUnread = chatResult.data.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
      chatUnreadCount.value = totalUnread
    }
  } catch (e) {
    notificationCount.value = 0
    chatUnreadCount.value = 0
  }
}

// 监听聊天未读数更新事件
const handleChatUnreadUpdate = (event) => {
  chatUnreadCount.value = event.detail?.unread || 0
}

// 监听聊天未读数同步（来自聊天中枢）
const handleSyncChatUnread = (event) => {
  chatUnreadCount.value = event.detail.unread
}

// 监听通知未读数更新事件
const handleNotificationUpdate = (event) => {
  notificationCount.value = event.detail?.count || 0
}

// 监听路由变化
watch(
  () => route.path,
  (newPath) => {
    if (newPath.includes('/collaboration')) {
      // 进入聊天中枢，重置聊天未读数
      chatUnreadCount.value = 0
    } else if (newPath.includes('/notifications')) {
      // 进入通知页面，重置通知未读数
      notificationCount.value = 0
    } else {
      // 离开这些页面，重新加载未读数
      loadUnreadCount()
    }
  }
)

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

  // 监听聊天未读数更新事件
  window.addEventListener('chatUnreadUpdated', handleChatUnreadUpdate)
  // 监听聊天未读数同步（来自聊天中枢）
  window.addEventListener('syncChatUnread', handleSyncChatUnread)
  // 监听通知未读数更新事件
  window.addEventListener('notificationUpdated', handleNotificationUpdate)
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('storage', loadUserInfo)
  window.removeEventListener('avatarUpdated', loadUserInfo)
  window.removeEventListener('chatUnreadUpdated', handleChatUnreadUpdate)
  window.removeEventListener('syncChatUnread', handleSyncChatUnread)
  window.removeEventListener('notificationUpdated', handleNotificationUpdate)

  // 销毁水印
  if (watermarkRef.value) {
    watermarkRef.value.destroyWatermark()
  }
})
</script>

<style scoped>
/* ===== 统一设计系统 ===== */
.system-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background: var(--bg-primary, #f0f2f5);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* ===== 顶部导航栏 - 深色主题 ===== */
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
  background: linear-gradient(135deg, #00d4ff, #0077ff);
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

.menu-section-title {
  padding: 8px 16px 4px;
  font-size: 11px;
  font-weight: 600;
  color: #909399;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-top: 8px;
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

.menu-item.active .menu-icon {
  color: #2563eb;
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

.menu-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #e5e7eb 20%, #e5e7eb 80%, transparent);
  margin: 12px 14px;
}

/* ===== 内容区 ===== */
.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: linear-gradient(180deg, #f0f2f5 0%, #e8eaed 100%);
}

/* ===== 滚动条 ===== */
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

/* 下拉菜单样式 */
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

:deep(.el-badge__content) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}
</style>
