<template>
  <div class="system-layout">
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
      <!-- 左侧菜单 - 根据当前顶部菜单动态显示 -->
      <aside class="sidebar">
        <!-- 系统管理菜单 -->
        <el-menu
          v-if="activeTopMenu === 'system'"
          :default-active="activeLeftMenu"
          class="sidebar-menu"
          background-color="#f5f7fa"
          text-color="#303133"
          active-text-color="#1677ff"
          router
        >
          <el-menu-item index="/system/profile">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="/system/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/system/roles">
            <el-icon><Management /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item index="/system/resources">
            <el-icon><FolderOpened /></el-icon>
            <span>资源管理</span>
          </el-menu-item>
        </el-menu>

        <!-- RPA运营管理菜单 -->
        <el-menu
          v-if="activeTopMenu === 'rpa'"
          :default-active="activeLeftMenu"
          class="sidebar-menu"
          background-color="#f5f7fa"
          text-color="#303133"
          active-text-color="#1677ff"
          router
        >
          <el-menu-item index="/rpa/tasks">
            <el-icon><List /></el-icon>
            <span>任务管理</span>
          </el-menu-item>
          <el-menu-item index="/rpa/robots">
            <el-icon><Monitor /></el-icon>
            <span>机器人管理</span>
          </el-menu-item>
          <el-menu-item index="/rpa/processes">
            <el-icon><Document /></el-icon>
            <span>流程定义</span>
          </el-menu-item>
          <el-menu-item index="/rpa/logs">
            <el-icon><Tickets /></el-icon>
            <span>日志管理</span>
          </el-menu-item>
        </el-menu>

        <!-- 首页菜单（如果需要） -->
        <el-menu
          v-if="activeTopMenu === 'dashboard'"
          :default-active="activeLeftMenu"
          class="sidebar-menu"
          background-color="#f5f7fa"
          text-color="#303133"
          active-text-color="#1677ff"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataLine /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 右侧内容区 - 子路由出口 -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataLine,
  Monitor,
  Setting,
  User,
  ArrowDown,
  Management,
  UserFilled,
  FolderOpened,
  List,
  Document,
  Tickets
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// 当前激活的顶部菜单
const activeTopMenu = ref('system')
// 当前激活的左侧菜单
const activeLeftMenu = ref('/system/profile')

const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  mobile: '13800138002',
  role: '系统管理员'
})

// 监听路由变化，自动更新顶部菜单和左侧菜单的激活状态
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
  if (index === 'dashboard') {
    router.push('/dashboard')
  } else if (index === 'rpa') {
    // 跳转到 RPA 运营管理的任务管理页面
    router.push('/rpa/tasks')
  } else if (index === 'system') {
    router.push('/system/profile')
  }
}

const goToProfile = () => {
  router.push('/system/profile')
}

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
})
</script>

<style scoped>
.system-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background-color: #f0f2f6;
}

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

.main-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.sidebar {
  width: 220px;
  background-color: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  flex-shrink: 0;
}

.sidebar-menu {
  border-right: none;
  background-color: #f5f7fa;
}

.sidebar-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 8px;
  border-radius: 8px;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #e6f2ff;
  color: #1677ff;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #eef2f6;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}
</style>