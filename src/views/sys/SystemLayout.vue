<template>
  <div class="system-layout">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <!-- Logo区域 -->
      <div class="header-left">
        <div class="logo-area" @click="router.push('/rpa/collaboration')">
          <div class="logo-icon">
            <img src="/title.png" alt="logo" style="width:32px;height:32px;" />
          </div>
          <div class="logo-text">
            <span class="logo-title">RPA</span>
            <span class="logo-subtitle">Enterprise</span>
          </div>
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
        <el-badge :value="unreadCount + chatUnreadCount" :hidden="unreadCount + chatUnreadCount === 0" class="tool-badge">
          <el-button class="tool-btn" @click="goToNotifications">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>

        <el-dropdown trigger="click">
          <div class="user-avatar">
            <el-avatar :size="34" class="avatar-circle" v-if="!currentUser.avatar">
              {{ userInitial }}
            </el-avatar>
            <el-avatar :size="34" class="avatar-circle" v-else :src="getAvatarUrl(currentUser.avatar)" @error="handleAvatarError" />
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
    const result = await apiGet('/notification/stats')
    if (result?.code === 0 && result.data?.unreads) {
      const unreads = result.data.unreads
      unreadCount.value = (unreads.collect || 0) + (unreads.temp || 0) + (unreads.user || 0)
    }
  } catch (e) {}
}

// 监听聊天未读数更新事件
const handleChatUnreadUpdate = (event) => {
  // 收到聊天未读更新时，只在非聊天中枢页面显示
  // 进入聊天中枢后，由路由监听器处理
}

// 监听聊天未读数同步（来自聊天中枢）
const handleSyncChatUnread = (event) => {
  // 更新全局未读数
  chatUnreadCount.value = event.detail.unread
}

// 监听路由变化，当进入聊天中枢时重置未读数
watch(
  () => route.path,
  (newPath) => {
    if (newPath.includes('/collaboration')) {
      // 进入聊天中枢，重置未读数（因为聊天中枢有自己的未读显示）
      unreadCount.value = 0
      chatUnreadCount.value = 0
    } else {
      // 离开聊天中枢，显示系统通知和聊天未读的总和
      loadUnreadCount()
      if (chatUnreadCount.value > 0) {
        unreadCount.value += chatUnreadCount.value
      }
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
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('storage', loadUserInfo)
  window.removeEventListener('avatarUpdated', loadUserInfo)
  window.removeEventListener('chatUnreadUpdated', handleChatUnreadUpdate)
  window.removeEventListener('syncChatUnread', handleSyncChatUnread)
})
</script>

<style scoped>
/* ===== 统一设计系统 ===== */
.system-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background: #f5f7fa;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* ===== 顶部导航栏 ===== */
.dashboard-header {
  height: 56px;
  background: linear-gradient(135deg, #1e3a5f 0%, #2d5a87 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  padding: 0 16px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.header-left {
  flex-shrink: 0;
  width: 220px;
  height: 100%;
  display: flex;
  align-items: center;
  padding-left: 16px;
  background: linear-gradient(135deg, #1e3a5f 0%, #2d5a87 100%);
  position: relative;
}

.header-left::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 32px;
  background: rgba(255, 255, 255, 0.15);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.logo-area:hover {
  transform: translateX(2px);
}

.logo-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
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
  color: #ffffff;
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 2px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  font-family: 'Orbitron', 'Roboto Mono', monospace;
}

.logo-subtitle {
  color: rgba(0, 212, 255, 0.9);
  font-weight: 500;
  font-size: 11px;
  letter-spacing: 1px;
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
  height: 56px;
}

.top-menu :deep(.el-menu-item) {
  padding: 0 20px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  height: 56px;
  line-height: 56px;
  border-bottom: 3px solid transparent;
  transition: all 0.25s ease;
  position: relative;
}

.top-menu :deep(.el-menu-item:hover) {
  color: #ffffff;
  background-color: rgba(255, 255, 255, 0.1);
}

.top-menu :deep(.el-menu-item.is-active) {
  color: #ffffff;
  background-color: rgba(255, 255, 255, 0.15);
  font-weight: 600;
}

.top-menu :deep(.el-menu-item.is-active)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 3px;
  background: linear-gradient(90deg, #00d4ff, #0077ff);
  border-radius: 3px 3px 0 0;
}

.header-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 16px;
}

.tool-badge {
  cursor: pointer;
}

.tool-btn {
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.85);
  padding: 8px;
  border-radius: 8px;
  transition: all 0.25s ease;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.tool-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.2);
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 24px;
  transition: all 0.25s ease;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.user-avatar:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.25);
}

.avatar-circle {
  border: 2px solid rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
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
  color: #ffffff;
  line-height: 1.2;
}

.user-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.2;
}

.dropdown-arrow {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
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
  border-right: 1px solid #e4e7ed;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
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
  top: 16px;
  width: 24px;
  height: 24px;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  color: #909399;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.sidebar-toggle:hover {
  background: linear-gradient(135deg, #00d4ff 0%, #0077ff 100%);
  color: #ffffff;
  border-color: #0077ff;
  transform: scale(1.1);
}

.sidebar-menu {
  padding: 12px 8px;
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
  gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  margin-bottom: 2px;
}

.menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.menu-item.active {
  background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
  color: #409eff;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1);
}

.menu-item.active .menu-icon {
  color: #409eff;
}

.menu-item.main-item {
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f4ff 100%);
  color: #1e3a5f;
  margin-bottom: 12px;
  border: 1px solid #d9ecff;
}

.menu-item.main-item:hover {
  background: linear-gradient(135deg, #e6f4ff 0%, #d9ecff 100%);
  transform: translateX(2px);
}

.menu-item.main-item.active {
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  color: #ffffff;
  border-color: #409eff;
}

.menu-item.main-item.active .menu-icon {
  color: #ffffff;
}

.menu-icon {
  font-size: 18px;
  flex-shrink: 0;
  transition: all 0.2s ease;
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
  background: linear-gradient(90deg, transparent, #e4e7ed 20%, #e4e7ed 80%, transparent);
  margin: 12px 14px;
}

/* ===== 内容区 ===== */
.content-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f5f7fa;
}

/* ===== 滚动条 ===== */
.sidebar-menu::-webkit-scrollbar,
.content-area::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-track,
.content-area::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-menu::-webkit-scrollbar-thumb,
.content-area::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 2px;
}

.sidebar-menu::-webkit-scrollbar-thumb:hover,
.content-area::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

/* 下拉菜单样式 */
:deep(.el-dropdown-menu) {
  background: #ffffff;
  border: 1px solid #ebeef5;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  padding: 4px;
}

:deep(.el-dropdown-menu__item) {
  border-radius: 6px;
  padding: 10px 16px;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: #f5f7fa;
  color: #409eff;
}

:deep(.el-dropdown-menu__item.is-divided) {
  margin-top: 4px;
  border-top: 1px solid #ebeef5;
  padding-top: 10px;
}

:deep(.el-badge__content) {
  background: linear-gradient(135deg, #f56c6c 0%, #e64a4a 100%);
  border: none;
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.4);
}
</style>
