<template>
  <div class="rpa-layout">
    <!-- 全局水印层 -->
    <WatermarkOverlay ref="watermarkRef" />

    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <!-- Logo区域 -->
      <div class="header-left">
        <div class="logo-area" @click="goToDashboard">
          <div class="logo-icon">
            <img src="/logo.png" alt="logo" style="width:36px;height:36px;" />
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
            <span>{{ t('menu.dashboard') }}</span>
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

      <!-- 右侧工具栏 -->
      <div class="header-right">
        <!-- 聊天消息按钮 -->
        <el-badge :value="chatUnreadCount" :hidden="chatUnreadCount === 0" class="tool-badge chat-badge" :max="99">
          <el-button class="tool-btn" @click="goToCollaboration">
            <el-icon><ChatLineSquare /></el-icon>
          </el-button>
        </el-badge>

        <!-- 通知按钮 -->
        <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="tool-badge notification-badge" :max="99">
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
          <el-menu
            :default-active="activeMenu"
            class="sidebar-el-menu"
            @select="handleMenuSelect"
          >
            <!-- 首页 -->
            <el-menu-item index="dashboard">
              <el-icon><Odometer /></el-icon>
              <span>{{ t('menu.dashboard') }}</span>
            </el-menu-item>

            <!-- 任务中心 -->
            <el-sub-menu index="task-center">
              <template #title>
                <el-icon><List /></el-icon>
                <span>{{ t('menu.taskCenter') }}</span>
              </template>
              <el-menu-item index="tasks">
                <el-icon><Timer /></el-icon>
                <span>{{ t('menu.tasks') }}</span>
              </el-menu-item>
              <el-menu-item index="queues">
                <el-icon><Operation /></el-icon>
                <span>{{ t('menu.queues') }}</span>
              </el-menu-item>
              <el-menu-item index="triggers">
                <el-icon><Switch /></el-icon>
                <span>{{ t('menu.triggers') }}</span>
              </el-menu-item>
              <el-menu-item index="script">
                <el-icon><Promotion /></el-icon>
                <span>{{ t('menu.script') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 资源中心 -->
            <el-sub-menu index="resource-center">
              <template #title>
                <el-icon><FolderOpened /></el-icon>
                <span>{{ t('menu.resourceCenter') }}</span>
              </template>
              <el-menu-item index="robots">
                <el-icon><Monitor /></el-icon>
                <span>{{ t('menu.robots') }}</span>
              </el-menu-item>
              <el-menu-item index="processes">
                <el-icon><Document /></el-icon>
                <span>{{ t('menu.processes') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 日志中心 -->
            <el-sub-menu index="log-center">
              <template #title>
                <el-icon><Tickets /></el-icon>
                <span>{{ t('menu.logCenter') }}</span>
              </template>
              <el-menu-item index="logs">
                <el-icon><Document /></el-icon>
                <span>{{ t('menu.executionLogs') }}</span>
              </el-menu-item>
              <el-menu-item index="audit">
                <el-icon><Clock /></el-icon>
                <span>{{ t('menu.auditLog') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 数据中心 -->
            <el-sub-menu index="data-center">
              <template #title>
                <el-icon><DataAnalysis /></el-icon>
                <span>{{ t('menu.dataCenter') }}</span>
              </template>
              <el-menu-item index="dataQuery">
                <el-icon><Search /></el-icon>
                <span>{{ t('menu.dataQuery') }}</span>
              </el-menu-item>
              <el-menu-item index="reports">
                <el-icon><DataBoard /></el-icon>
                <span>{{ t('menu.reports') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 凭据中心 -->
            <el-menu-item index="credentials">
              <el-icon><Key /></el-icon>
              <span>{{ t('menu.credentials') }}</span>
            </el-menu-item>

            <!-- AI能力 -->
            <el-menu-item index="ai">
              <el-icon><MagicStick /></el-icon>
              <span>{{ t('menu.aiCenter') }}</span>
            </el-menu-item>

            <!-- 系统管理 -->
            <el-sub-menu index="system-admin">
              <template #title>
                <el-icon><Setting /></el-icon>
                <span>{{ t('menu.systemManagement') }}</span>
              </template>
              <el-menu-item index="settings">
                <el-icon><Tools /></el-icon>
                <span>{{ t('menu.settings') }}</span>
              </el-menu-item>
              <el-menu-item index="masking">
                <el-icon><View /></el-icon>
                <span>{{ t('menu.dataMasking') }}</span>
              </el-menu-item>
              <el-menu-item index="locks">
                <el-icon><Unlock /></el-icon>
                <span>{{ t('menu.distributedLocks') }}</span>
              </el-menu-item>
              <el-menu-item index="watermark" v-if="isAdmin">
                <el-icon><Key /></el-icon>
                <span>{{ t('menu.watermark') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 协作中枢 -->
            <el-menu-item index="collaboration">
              <el-icon><ChatLineSquare /></el-icon>
              <span>{{ t('menu.collaboration') }}</span>
            </el-menu-item>
          </el-menu>
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
import { ref, onMounted, computed, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import WatermarkOverlay from '../../components/watermark/WatermarkOverlay.vue'
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
  ChatLineSquare,
  Connection,
  Tools,
  Timer,
  MagicStick,
  Promotion,
  View,
  Unlock,
  Odometer,
  SwitchButton,
  Key as WatermarkIcon,
  FolderOpened,
  Switch
} from '@element-plus/icons-vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const watermarkRef = ref(null)

const sidebarCollapsed = ref(false)
const showDataSubmenu = ref(true)
const activeTopMenu = ref('rpa')
const activeMenu = ref('tasks')
const unreadCount = ref(0)
const notificationCount = ref(0)
const chatUnreadCount = ref(0)

// 公告已读缓存（用于快速更新未读数）
const announcementReadCache = ref(new Set())

// 从本地缓存加载已读公告ID
const loadAnnouncementReadCache = () => {
  try {
    const cached = localStorage.getItem('announcementReadCache')
    if (cached) {
      announcementReadCache.value = new Set(JSON.parse(cached))
    }
  } catch (e) {
    console.error('加载公告已读缓存失败', e)
  }
}

// 保存已读公告ID到本地缓存
const saveAnnouncementReadCache = () => {
  try {
    localStorage.setItem('announcementReadCache', JSON.stringify([...announcementReadCache.value]))
  } catch (e) {
    console.error('保存公告已读缓存失败', e)
  }
}

// 添加已读公告
const addReadAnnouncement = (id) => {
  announcementReadCache.value.add(id)
  saveAnnouncementReadCache()
}

// 添加多个已读公告
const addReadAnnouncements = (ids) => {
  ids.forEach(id => announcementReadCache.value.add(id))
  saveAnnouncementReadCache()
}

// 标记公告已读（更新本地缓存和通知组件）
const markAnnouncementAsRead = (announcementId) => {
  addReadAnnouncement(announcementId)
  // 减少未读数（至少为0）
  if (notificationCount.value > 0) {
    notificationCount.value = notificationCount.value - 1
  }
  console.log('[RpaLayout] 公告已读标记:', announcementId, '剩余未读:', notificationCount.value)
}

const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  role: 1
})

const userName = computed(() => currentUser.value.realName || currentUser.value.username)
const userRole = computed(() => currentUser.value.role === 1 ? t('user.admin') : t('user.normal'))
const isAdmin = computed(() => currentUser.value.role === 1)
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

// 路由映射
const routeMap = {
  dashboard: '/dashboard',
  tasks: '/rpa/tasks',
  queues: '/rpa/queues',
  triggers: '/rpa/triggers',
  logs: '/rpa/logs',
  audit: '/rpa/audit',
  credentials: '/rpa/credentials',
  reports: '/rpa/reports',
  settings: '/rpa/settings',
  notifications: '/rpa/notifications',
  collaboration: '/rpa/collaboration',
  dataQuery: '/rpa/data-query',
  robots: '/rpa/robots',
  processes: '/rpa/processes',
  ai: '/rpa/ai',
  script: '/rpa/script',
  masking: '/rpa/masking',
  locks: '/rpa/locks',
  watermark: '/rpa/watermark-settings'
}

// 菜单选择处理
const handleMenuSelect = (index) => {
  activeMenu.value = index
  if (routeMap[index]) {
    router.push(routeMap[index])
  }
}

// 侧边栏菜单切换（兼容旧代码）
const switchMenu = (menu) => {
  activeMenu.value = menu
  if (routeMap[menu]) {
    router.push(routeMap[menu])
  }
}

// 跳转到工作台
const goToDashboard = () => {
  router.push('/dashboard')
}

// 跳转到协作中枢
// 跳转到协作中枢
const goToCollaboration = () => {
  switchMenu('collaboration')
  router.push('/rpa/collaboration')
}

// 跳转到通知页面
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
  ElMessageBox.confirm(t('confirm.logout'), t('common.warning'), {
    type: 'warning',
    confirmButtonPosition: 'right',
    distinguishCancelAndClose: true
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success(t('common.success'))
    router.push('/login')
  }).catch(() => {})
}

// 加载未读消息数
const loadUnreadCount = async () => {
  try {
    const { apiGet } = await import('../../utils/api.js')

    // 获取当前用户ID
    const user = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const userId = user.id || 1

    // 获取公告未读数
    const announcementResult = await apiGet('/announcement/list', { userId })
    if (announcementResult?.code === 0 && announcementResult.data) {
      // 过滤出后端返回的未读公告
      const unreadAnnouncements = announcementResult.data.filter(a => a.status === 'unread')
      notificationCount.value = unreadAnnouncements.length
      console.log('[RpaLayout] 公告未读数:', notificationCount.value, '总计:', announcementResult.data.length)
    }

    // 获取聊天未读数
    const chatResult = await apiGet(`/chat/conversations?userId=${userId}`)
    if (chatResult?.code === 0 && chatResult.data) {
      const totalUnread = chatResult.data.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
      chatUnreadCount.value = totalUnread
    }
  } catch (e) {
    console.error('[RpaLayout] 加载未读数失败:', e)
    notificationCount.value = 0
    chatUnreadCount.value = 0
  }
}

// 监听聊天未读数更新事件
const handleChatUnreadUpdate = (event) => {
  chatUnreadCount.value = event.detail?.unread || 0
}

// 监听通知未读数更新事件
const handleNotificationUpdate = (event) => {
  // 更新未读数
  notificationCount.value = event.detail?.count ?? notificationCount.value

  // 如果传入了已读公告ID列表，更新缓存并同步未读数
  if (event.detail?.readIds && event.detail.readIds.length > 0) {
    addReadAnnouncements(event.detail.readIds)
    // 如果传入了明确的count，使用它；否则减少对应数量
    if (event.detail?.count === undefined) {
      notificationCount.value = Math.max(0, notificationCount.value - event.detail.readIds.length)
    }
  }
}

// 监听路由变化
watch(
  () => route.path,
  (newPath, oldPath) => {
    if (newPath.includes('/collaboration')) {
      // 进入聊天中枢，重置聊天未读数
      chatUnreadCount.value = 0
    } else if (newPath.includes('/notifications')) {
      // 进入通知页面，强制重新加载未读数以获取最新状态
      setTimeout(() => loadUnreadCount(), 100)
    } else if (oldPath?.includes('/notifications') || oldPath?.includes('/collaboration')) {
      // 从通知/聊天页面离开，重新加载未读数
      loadUnreadCount()
    }
  }
)

// 监听来自通知页面的已读更新事件
const handleAnnouncementReadUpdate = () => {
  // 当通知页面标记已读后，通知布局刷新未读数
  loadUnreadCount()
}

// 监听来自水印设置页面的水印状态变化
const handleWatermarkStatusChange = (event) => {
  console.log('[RpaLayout] 水印状态变化:', event.detail)

  // 只有明确的水印启用/禁用操作才刷新未读数，公告操作不需要
  if (event.detail && event.detail.type === 'announcement') {
    // 公告相关操作不需要刷新未读数（已经在Notifications中处理了）
    return
  }

  // 刷新未读数
  loadUnreadCount()
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

onMounted(() => {
  loadUserInfo()
  loadAnnouncementReadCache() // 加载公告已读缓存
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

  // 监听通知未读数更新事件
  window.addEventListener('notificationUpdated', handleNotificationUpdate)

  // 监听公告已读更新事件
  window.addEventListener('announcementReadUpdated', handleAnnouncementReadUpdate)

  // 监听水印状态变化事件
  window.addEventListener('watermarkStatusChanged', handleWatermarkStatusChange)

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
  else if (path.includes('/rpa/collaboration')) activeMenu.value = 'collaboration'
  else if (path.includes('/rpa/data-collect')) activeMenu.value = 'dataCollect'
  else if (path.includes('/rpa/data-parse')) activeMenu.value = 'dataParse'
  else if (path.includes('/rpa/data-process')) activeMenu.value = 'dataProcess'
  else if (path.includes('/rpa/data-query')) activeMenu.value = 'dataQuery'
  else if (path.includes('/rpa/invoice')) activeMenu.value = 'invoice'
  else if (path.includes('/rpa/watermark')) activeMenu.value = 'watermark'
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('storage', loadUserInfo)
  window.removeEventListener('avatarUpdated', loadUserInfo)
  window.removeEventListener('chatUnreadUpdated', handleChatUnreadUpdate)
  window.removeEventListener('notificationUpdated', handleNotificationUpdate)
  window.removeEventListener('announcementReadUpdated', handleAnnouncementReadUpdate)
  window.removeEventListener('watermarkStatusChanged', handleWatermarkStatusChange)

  // 销毁水印
  if (watermarkRef.value) {
    watermarkRef.value.destroyWatermark()
  }
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

/* 通知徽章样式 - 科技风格脉冲动画 */
.notification-badge {
  position: relative;
}

.notification-badge :deep(.el-badge__content) {
  background: linear-gradient(135deg, #ff6b6b 0%, #ff4444 100%);
  border: none;
  box-shadow: 0 0 10px rgba(255, 68, 68, 0.5);
  animation: notificationPulse 2s ease-in-out infinite;
  right: 8px;
  top: 2px;
}

@keyframes notificationPulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 10px rgba(255, 68, 68, 0.5);
  }
  50% {
    transform: scale(1.1);
    box-shadow: 0 0 20px rgba(255, 68, 68, 0.8);
  }
}

/* 通知图标激活状态 */
.notification-badge .tool-btn:hover {
  background: rgba(255, 68, 68, 0.15);
  border-color: rgba(255, 68, 68, 0.3);
  color: #ff6b6b;
}

/* 通知图标未读时的微光效果 */
.tool-btn:has(~ .notification-badge .el-badge__content:not([hidden])) {
  animation: bellGlow 2s ease-in-out infinite;
}

@keyframes bellGlow {
  0%, 100% {
    box-shadow: none;
  }
  50% {
    box-shadow: 0 0 15px rgba(255, 68, 68, 0.4);
  }
}

/* 聊天徽章样式 */
.chat-badge {
  position: relative;
}

.chat-badge :deep(.el-badge__content) {
  background: linear-gradient(135deg, #00f5ff 0%, #0099ff 100%);
  border: none;
  box-shadow: 0 0 10px rgba(0, 245, 255, 0.5);
  animation: chatPulse 2s ease-in-out infinite;
  right: 8px;
  top: 2px;
}

@keyframes chatPulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 10px rgba(0, 245, 255, 0.5);
  }
  50% {
    transform: scale(1.1);
    box-shadow: 0 0 20px rgba(0, 245, 255, 0.8);
  }
}

/* 聊天图标激活状态 */
.chat-badge .tool-btn:hover {
  background: rgba(0, 245, 255, 0.15);
  border-color: rgba(0, 245, 255, 0.3);
  color: #00f5ff;
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
  height: 100%;
  overflow-y: auto;
  padding: 12px 0;
}

/* Element Plus 菜单样式覆盖 */
.sidebar-el-menu {
  background: transparent;
  border-right: none;
}

.sidebar-el-menu:not(.el-menu--collapse) {
  width: 100%;
}

.sidebar-el-menu .el-menu-item,
.sidebar-el-menu .el-sub-menu__title {
  height: 48px;
  line-height: 48px;
  color: #4b5563;
  padding-left: 20px !important;
  transition: all 0.2s ease;
}

.sidebar-el-menu .el-menu-item:hover,
.sidebar-el-menu .el-sub-menu__title:hover {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #2563eb;
}

.sidebar-el-menu .el-menu-item.is-active,
.sidebar-el-menu .el-menu-item.is-active:hover {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #2563eb;
  font-weight: 600;
  border-right: 3px solid #2563eb;
}

.sidebar-el-menu .el-sub-menu .el-menu-item {
  height: 42px;
  line-height: 42px;
  padding-left: 48px !important;
  font-size: 13px;
}

.sidebar-el-menu .el-sub-menu .el-menu-item:hover {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #2563eb;
}

.sidebar-el-menu .el-sub-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #2563eb;
  font-weight: 600;
  border-right: 3px solid #2563eb;
}

.sidebar-el-menu .el-sub-menu__title {
  color: #374151;
  font-weight: 500;
}

.sidebar-el-menu .el-sub-menu__title:hover {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #2563eb;
}

.sidebar-el-menu .el-sub-menu .el-sub-menu__title {
  padding-left: 20px !important;
}

.sidebar-el-menu .el-sub-menu .el-sub-menu__title:hover {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #2563eb;
}

.sidebar-el-menu .el-icon {
  margin-right: 12px;
  font-size: 18px;
}

.sidebar-el-menu .el-sub-menu__icon-arrow {
  color: #9ca3af;
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