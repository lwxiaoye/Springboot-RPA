<template>
  <div class="rpa-layout">
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
          <!-- 任务管理 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'tasks' }"
            @click="switchMenu('tasks')"
          >
            <el-icon class="menu-icon"><List /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">任务管理</span>
          </div>
          
          <!-- 机器人管理 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'robots' }"
            @click="switchMenu('robots')"
          >
            <el-icon class="menu-icon"><Monitor /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">机器人管理</span>
          </div>
          
          <!-- 流程管理 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'processes' }"
            @click="switchMenu('processes')"
          >
            <el-icon class="menu-icon"><Document /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">流程管理</span>
          </div>
          
          <!-- 执行日志 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'logs' }"
            @click="switchMenu('logs')"
          >
            <el-icon class="menu-icon"><Tickets /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">执行日志</span>
          </div>

          <!-- 通知管理 -->
          <div class="menu-item"
            :class="{ active: activeMenu === 'notifications' }"
            @click="switchMenu('notifications')"
          >
            <el-icon class="menu-icon"><Bell /></el-icon>
            <span class="menu-text" v-if="!sidebarCollapsed">通知管理</span>
          </div>
          
          <!-- 数据管理（带子菜单） -->
          <div class="menu-group">
            <div class="menu-item has-submenu"
              :class="{ active: activeMenu.startsWith('data') }"
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
                  :class="{ active: activeMenu === 'dataCollect' }"
                  @click="switchMenu('dataCollect')"
                >
                  <el-icon class="menu-icon"><Download /></el-icon>
                  <span class="menu-text">数据采集</span>
                </div>
                <div class="menu-item submenu-item"
                  :class="{ active: activeMenu === 'dataParse' }"
                  @click="switchMenu('dataParse')"
                >
                  <el-icon class="menu-icon"><Sort /></el-icon>
                  <span class="menu-text">数据解析</span>
                </div>
                <div class="menu-item submenu-item"
                  :class="{ active: activeMenu === 'dataProcess' }"
                  @click="switchMenu('dataProcess')"
                >
                  <el-icon class="menu-icon"><Operation /></el-icon>
                  <span class="menu-text">数据加工</span>
                </div>
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
  DataAnalysis,
  Download,
  Sort,
  Operation,
  Search,
  Bell
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const sidebarCollapsed = ref(false)
const showDataSubmenu = ref(true)
const activeTopMenu = ref('rpa')
const activeMenu = ref('tasks')

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
    tasks: '/rpa/tasks',
    robots: '/rpa/robots',
    processes: '/rpa/processes',
    logs: '/rpa/logs',
    notifications: '/rpa/notifications',
    dataCollect: '/rpa/data-collect',
    dataParse: '/rpa/data-parse',
    dataProcess: '/rpa/data-process',
    dataQuery: '/rpa/data-query'
  }
  router.push(routeMap[menu])
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
  if (path.includes('/rpa/tasks')) activeMenu.value = 'tasks'
  else if (path.includes('/rpa/robots')) activeMenu.value = 'robots'
  else if (path.includes('/rpa/processes')) activeMenu.value = 'processes'
  else if (path.includes('/rpa/logs')) activeMenu.value = 'logs'
  else if (path.includes('/rpa/notifications')) activeMenu.value = 'notifications'
  else if (path.includes('/rpa/data-collect')) activeMenu.value = 'dataCollect'
  else if (path.includes('/rpa/data-parse')) activeMenu.value = 'dataParse'
  else if (path.includes('/rpa/data-process')) activeMenu.value = 'dataProcess'
  else if (path.includes('/rpa/data-query')) activeMenu.value = 'dataQuery'
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