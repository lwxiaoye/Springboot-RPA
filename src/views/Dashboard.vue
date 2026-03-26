<template>
  <div class="app-layout">
    <div class="top-navbar">
      <div class="logo-area">
        <span class="logo-icon">⚙️</span>
        <span class="system-name">RPA 系统管理后台</span>
      </div>
      <div class="user-dropdown" @click.stop="toggleDropdown">
        <div class="avatar-circle">{{ userInitials }}</div>
        <span class="user-name-text">{{ currentUser.realName || currentUser.username }}</span>
        <span class="dropdown-arrow">▼</span>
        <div class="dropdown-menu" v-if="dropdownVisible" @click.stop>
          <div class="dropdown-item" @click="goToPersonal">
            <span>👤</span>
            <span>个人中心</span>
          </div>
          <div class="dropdown-item" @click="handleLogout">
            <span>🚪</span>
            <span>退出登录</span>
          </div>
        </div>
      </div>
    </div>

    <div class="main-container">
      <div class="sidebar">
        <div class="sidebar-menu">
          <div class="menu-item" :class="{active: activeMenu === 'dashboard'}" @click="switchMenu('dashboard')">
            <span class="menu-icon">📊</span>
            <span>仪表板</span>
          </div>
          
          <div class="menu-item" :class="{active: activeMenu === 'users'}" @click="switchMenu('users')">
            <span class="menu-icon">👥</span>
            <span>用户管理</span>
          </div>
          
          <div class="menu-item" :class="{active: activeMenu === 'tasks'}" @click="switchMenu('tasks')">
            <span class="menu-icon">📋</span>
            <span>任务管理</span>
          </div>
          
          <div class="menu-item" :class="{active: activeMenu === 'robots'}" @click="switchMenu('robots')">
            <span class="menu-icon">🤖</span>
            <span>机器人管理</span>
          </div>
          
          <div class="menu-item" :class="{active: activeMenu === 'processes'}" @click="switchMenu('processes')">
            <span class="menu-icon">🔧</span>
            <span>流程管理</span>
          </div>
          
          <div class="menu-item" :class="{active: activeMenu === 'logs'}" @click="switchMenu('logs')">
            <span class="menu-icon">📝</span>
            <span>执行日志</span>
          </div>
          
          <div class="menu-item" :class="{active: activeMenu.startsWith('data')}" @click="switchMenu('dataCollect')">
            <span class="menu-icon">💾</span>
            <span>数据管理</span>
          </div>
          <div class="submenu" v-if="activeMenu.startsWith('data')">
            <div class="menu-item" :class="{active: activeMenu === 'dataCollect'}" @click="switchMenu('dataCollect')">
              <span>数据采集</span>
            </div>
            <div class="menu-item" :class="{active: activeMenu === 'dataParse'}" @click="switchMenu('dataParse')">
              <span>数据解析</span>
            </div>
            <div class="menu-item" :class="{active: activeMenu === 'dataProcess'}" @click="switchMenu('dataProcess')">
              <span>数据加工</span>
            </div>
            <div class="menu-item" :class="{active: activeMenu === 'dataQuery'}" @click="switchMenu('dataQuery')">
              <span>数据查询</span>
            </div>
          </div>
        </div>
      </div>

      <div class="content-area">
        <!-- 仪表板 -->
        <div v-if="activeMenu === 'dashboard'">
          <div class="welcome-card">
            <h2>欢迎回来，{{ currentUser.realName || currentUser.username }}！</h2>
            <p>{{ currentDate }}</p>
          </div>

          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-icon purple">📋</div>
              <div class="stat-number">{{ stats.tasks }}</div>
              <div class="stat-label">总任务数</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon pink">🤖</div>
              <div class="stat-number">{{ stats.robots }}</div>
              <div class="stat-label">机器人总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon blue">🔧</div>
              <div class="stat-number">{{ stats.processes }}</div>
              <div class="stat-label">流程总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon green">📝</div>
              <div class="stat-number">{{ stats.logs }}</div>
              <div class="stat-label">日志总数</div>
            </div>
          </div>

          <div class="content-section">
            <div class="section-header">
              <h3 class="section-title">任务状态概览</h3>
            </div>
            <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem;">
              <div style="text-align: center;">
                <div style="font-size: 2.5rem; font-weight: bold; color: #f59e0b;">{{ taskStatus.running }}</div>
                <div style="color: #6b7280; font-size: 0.875rem;">运行中</div>
              </div>
              <div style="text-align: center;">
                <div style="font-size: 2.5rem; font-weight: bold; color: #6b7280;">{{ taskStatus.pending }}</div>
                <div style="color: #6b7280; font-size: 0.875rem;">待执行</div>
              </div>
              <div style="text-align: center;">
                <div style="font-size: 2.5rem; font-weight: bold; color: #10b981;">{{ taskStatus.completed }}</div>
                <div style="color: #6b7280; font-size: 0.875rem;">已完成</div>
              </div>
              <div style="text-align: center;">
                <div style="font-size: 2.5rem; font-weight: bold; color: #ef4444;">{{ taskStatus.failed }}</div>
                <div style="color: #6b7280; font-size: 0.875rem;">失败</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 用户管理 -->
        <div v-if="activeMenu === 'users'" class="content-section">
          <div class="section-header">
            <h2 class="section-title">用户管理</h2>
            <el-button type="primary" @click="showUserModal">+ 新建用户</el-button>
          </div>
          <el-table :data="users" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="realName" label="真实姓名">
              <template #default="{ row }">{{ row.realName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱">
              <template #default="{ row }">{{ row.email || '-' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="电话">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column prop="role" label="角色">
              <template #default="{ row }">{{ getRoleText(row.role) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280">
              <template #default="{ row }">
                <el-button size="small" @click="editUser(row)">编辑</el-button>
                <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleUserStatus(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click="deleteUser(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 任务管理 -->
        <div v-if="activeMenu === 'tasks'" class="content-section">
          <div class="section-header">
            <h2 class="section-title">任务管理</h2>
          </div>
          <el-table :data="tasks" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="任务名称" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small">查看</el-button>
                <el-button size="small" type="primary">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 机器人管理 -->
        <div v-if="activeMenu === 'robots'" class="content-section">
          <div class="section-header">
            <h2 class="section-title">机器人管理</h2>
          </div>
          <el-table :data="robots" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="机器人名称" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP地址" />
            <el-table-column prop="createTime" label="创建时间" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small">查看</el-button>
                <el-button size="small" type="primary">管理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 流程管理 -->
        <div v-if="activeMenu === 'processes'" class="content-section">
          <div class="section-header">
            <h2 class="section-title">流程管理</h2>
          </div>
          <el-table :data="processes" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="流程名称" />
            <el-table-column prop="version" label="版本" width="100" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small">查看</el-button>
                <el-button size="small" type="primary">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 执行日志 -->
        <div v-if="activeMenu === 'logs'" class="content-section">
          <div class="section-header">
            <h2 class="section-title">执行日志</h2>
          </div>
          <el-table :data="logs" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="taskName" label="任务名称" />
            <el-table-column prop="robotName" label="机器人" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" />
            <el-table-column prop="endTime" label="结束时间">
              <template #default="{ row }">{{ row.endTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="primary">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 数据采集 -->
        <div v-if="activeMenu === 'dataCollect'" class="content-section">
          <div class="section-header">
            <h2 class="section-title">数据采集</h2>
            <el-button type="primary" @click="showCollectModal()">+ 新建采集</el-button>
          </div>
          <el-table :data="dataCollects" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="采集名称" />
            <el-table-column prop="source" label="数据来源" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="collectTime" label="采集时间" />
            <el-table-column prop="count" label="数据量" width="100" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="executeCollect(row)">执行</el-button>
                <el-button size="small" @click="showCollectModal(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteCollect(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 数据解析 -->
        <div v-if="activeMenu === 'dataParse'" class="content-section">
          <div class="section-header">
            <h2 class="section-title">数据解析</h2>
          </div>
          <el-table :data="dataParses" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="解析名称" />
            <el-table-column prop="rule" label="解析规则" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="parseTime" label="解析时间" />
            <el-table-column prop="successRate" label="成功率" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="primary">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 数据加工 -->
        <div v-if="activeMenu === 'dataProcess'" class="content-section">
          <div class="section-header">
            <h2 class="section-title">数据加工</h2>
          </div>
          <el-table :data="dataProcesses" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="加工名称" />
            <el-table-column prop="method" label="加工方法" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="processTime" label="加工时间" />
            <el-table-column prop="processedCount" label="处理数量" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="primary">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 数据查询 -->
        <div v-if="activeMenu === 'dataQuery'" class="content-section">
          <div class="section-header">
            <h2 class="section-title">数据查询</h2>
          </div>
          <el-table :data="dataQueries" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="查询名称" />
            <el-table-column prop="condition" label="查询条件" />
            <el-table-column prop="queryTime" label="查询时间" />
            <el-table-column prop="resultCount" label="结果数量" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="primary">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 用户编辑弹窗 -->
    <el-dialog v-model="modals.user" :title="editingUser ? '编辑用户' : '新建用户'" width="500px">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="用户名" v-if="!editingUser">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" v-if="!editingUser">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="userForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role" style="width: 100%;">
            <el-option :label="'普通用户'" :value="0" />
            <el-option :label="'管理员'" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeModal('user')">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据采集编辑弹窗 -->
    <el-dialog v-model="modals.collect" :title="editingCollect ? '编辑采集配置' : '新建采集配置'" width="600px">
      <el-form :model="collectForm" label-width="100px">
        <el-form-item label="采集名称" required>
          <el-input v-model="collectForm.name" placeholder="请输入采集名称" />
        </el-form-item>
        <el-form-item label="数据来源URL" required>
          <el-input v-model="collectForm.sourceUrl" placeholder="请输入来源URL" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="collectForm.sourceType" style="width: 100%;">
            <el-option label="网页" value="web" />
            <el-option label="API" value="api" />
            <el-option label="文件" value="file" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择器规则">
          <el-input v-model="collectForm.selectorRules" type="textarea" :rows="3" placeholder="CSS选择器或XPath" />
        </el-form-item>
        <el-form-item label="请求头(Headers)">
          <el-input v-model="collectForm.headers" type="textarea" :rows="2" placeholder="JSON格式，如：{&quot;Authorization&quot;: &quot;Bearer xxx&quot;}" />
        </el-form-item>
        <el-form-item label="Cookies">
          <el-input v-model="collectForm.cookies" type="textarea" :rows="2" placeholder="Cookie字符串" />
        </el-form-item>
        <el-form-item label="定时表达式">
          <el-input v-model="collectForm.cronExpression" placeholder="如：0 0 * * * ?" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeModal('collect')">取消</el-button>
        <el-button type="primary" @click="saveCollect">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const activeMenu = ref('dashboard')
const dropdownVisible = ref(false)

const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com'
})

const stats = ref({ tasks: 0, robots: 0, processes: 0, logs: 0 })
const taskStatus = ref({ running: 0, pending: 0, completed: 0, failed: 0 })
const users = ref([])
const tasks = ref([])
const robots = ref([])
const processes = ref([])
const logs = ref([])
const dataCollects = ref([])
const dataParses = ref([])
const dataProcesses = ref([])
const dataQueries = ref([])

const modals = ref({ user: false, collect: false })
const editingUser = ref(null)
const editingCollect = ref(null)

const userForm = ref({ username: '', password: '', realName: '', email: '', phone: '', role: 0 })
const collectForm = ref({ name: '', sourceUrl: '', sourceType: 'web', selectorRules: '', headers: '', cookies: '', cronExpression: '' })
const loading = ref(false)

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', { 
    year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' 
  })
})

const userInitials = computed(() => {
  const name = currentUser.value.realName || currentUser.value.username
  return name.charAt(0).toUpperCase()
})

onMounted(() => {
  loadUserFromStorage()
  loadAllData()
})

const loadUserFromStorage = () => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUser.value = { ...currentUser.value, ...user }
    } catch(e) {}
  }
}

const loadAllData = async () => {
  await Promise.all([
    loadUsers(),
    loadTasks(),
    loadRobots(),
    loadProcesses(),
    loadLogs(),
    loadDataCollects(),
    loadDataParses(),
    loadDataProcesses(),
    loadDataQueries()
  ])
  updateStats()
}

const updateStats = () => {
  stats.value = {
    tasks: tasks.value.length,
    robots: robots.value.length,
    processes: processes.value.length,
    logs: logs.value.length
  }
  
  taskStatus.value = {
    running: tasks.value.filter(t => t.status === 'running').length,
    pending: tasks.value.filter(t => t.status === 'pending').length,
    completed: tasks.value.filter(t => t.status === 'completed').length,
    failed: tasks.value.filter(t => t.status === 'failed').length
  }
}

const getStatusText = (status) => {
  if (!status) return '未知'
  const s = status.toLowerCase()
  if (s === 'running') return '运行中'
  if (s === 'pending') return '待执行'
  if (s === 'completed') return '已完成'
  if (s === 'failed') return '失败'
  if (s === 'active') return '活跃'
  if (s === 'idle') return '空闲'
  return status
}

const getStatusType = (status) => {
  if (!status) return 'info'
  const s = status.toLowerCase()
  if (s === 'running' || s === 'active') return 'warning'
  if (s === 'pending' || s === 'idle') return 'info'
  if (s === 'completed') return 'success'
  if (s === 'failed') return 'danger'
  return 'info'
}

const getRoleText = (role) => {
  if (role === 1) return '管理员'
  return '普通用户'
}

const loadUsers = async () => {
  try {
    const response = await fetch(`${API_BASE}/user`)
    const result = await response.json()
    if (result.code === 0) {
      users.value = result.data || []
    }
  } catch (error) {
    console.error('加载用户失败:', error)
  }
}

const loadTasks = async () => {
  try {
    const response = await fetch(`${API_BASE}/task`)
    const result = await response.json()
    if (result.code === 0) {
      tasks.value = result.data || []
    }
  } catch (error) {
    console.error('加载任务失败:', error)
    tasks.value = [
      { id: 1, name: '数据同步任务', status: 'running', createTime: '2024-01-15 10:00:00' },
      { id: 2, name: '报表生成任务', status: 'pending', createTime: '2024-01-15 11:00:00' },
      { id: 3, name: '邮件发送任务', status: 'completed', createTime: '2024-01-15 09:00:00' },
      { id: 4, name: '文件下载任务', status: 'failed', createTime: '2024-01-14 15:00:00' }
    ]
  }
}

const loadRobots = async () => {
  try {
    const response = await fetch(`${API_BASE}/robot`)
    const result = await response.json()
    if (result.code === 0) {
      robots.value = result.data || []
    }
  } catch (error) {
    console.error('加载机器人失败:', error)
    robots.value = [
      { id: 1, name: 'Robot-001', status: 'active', ip: '192.168.1.101', createTime: '2024-01-10' },
      { id: 2, name: 'Robot-002', status: 'idle', ip: '192.168.1.102', createTime: '2024-01-11' },
      { id: 3, name: 'Robot-003', status: 'active', ip: '192.168.1.103', createTime: '2024-01-12' }
    ]
  }
}

const loadProcesses = async () => {
  try {
    const response = await fetch(`${API_BASE}/process`)
    const result = await response.json()
    if (result.code === 0) {
      processes.value = result.data || []
    }
  } catch (error) {
    console.error('加载流程失败:', error)
    processes.value = [
      { id: 1, name: '客户信息录入流程', version: '1.0', status: 'active', createTime: '2024-01-05' },
      { id: 2, name: '订单处理流程', version: '2.1', status: 'active', createTime: '2024-01-08' },
      { id: 3, name: '发票审核流程', version: '1.2', status: 'pending', createTime: '2024-01-12' }
    ]
  }
}

const loadLogs = async () => {
  try {
    const response = await fetch(`${API_BASE}/log`)
    const result = await response.json()
    if (result.code === 0) {
      logs.value = result.data || []
    }
  } catch (error) {
    console.error('加载日志失败:', error)
    logs.value = [
      { id: 1, taskName: '数据同步任务', robotName: 'Robot-001', status: 'completed', startTime: '2024-01-15 10:00:00', endTime: '2024-01-15 10:15:00' },
      { id: 2, taskName: '报表生成任务', robotName: 'Robot-002', status: 'running', startTime: '2024-01-15 11:00:00', endTime: null },
      { id: 3, taskName: '文件下载任务', robotName: 'Robot-003', status: 'failed', startTime: '2024-01-14 15:00:00', endTime: '2024-01-14 15:05:00' }
    ]
  }
}

const loadDataCollects = async () => {
  try {
    const response = await fetch(`${API_BASE}/dataCollect`)
    const result = await response.json()
    if (result.code === 0) {
      dataCollects.value = (result.data || []).map(item => ({
        id: item.id,
        name: item.name,
        source: item.sourceUrl || '-',
        status: item.status === 1 ? 'completed' : item.status === 2 ? 'failed' : 'pending',
        collectTime: item.lastCollectTime ? new Date(item.lastCollectTime).toLocaleString('zh-CN') : '-',
        count: item.dataCount || 0
      }))
    }
  } catch (error) {
    console.error('加载数据采集失败:', error)
    dataCollects.value = []
  }
}

const loadDataParses = async () => {
  dataParses.value = [
    { id: 1, name: '客户信息解析', rule: '正则表达式', status: 'completed', parseTime: '2024-01-15 09:30:00', successRate: '98.5%' },
    { id: 2, name: '订单详情解析', rule: 'JSON解析', status: 'running', parseTime: '2024-01-15 10:30:00', successRate: '95.2%' },
    { id: 3, name: '产品规格解析', rule: 'XML解析', status: 'completed', parseTime: '2024-01-14 16:00:00', successRate: '99.1%' }
  ]
}

const loadDataProcesses = async () => {
  dataProcesses.value = [
    { id: 1, name: '数据清洗', method: '去重+格式化', status: 'completed', processTime: '2024-01-15 10:00:00', processedCount: 1235 },
    { id: 2, name: '数据转换', method: '格式转换', status: 'running', processTime: '2024-01-15 11:00:00', processedCount: 500 },
    { id: 3, name: '数据聚合', method: '分组统计', status: 'pending', processTime: '2024-01-15 12:00:00', processedCount: 0 }
  ]
}

const loadDataQueries = async () => {
  dataQueries.value = [
    { id: 1, name: '客户信息查询', condition: '按地区筛选', queryTime: '2024-01-15 10:15:00', resultCount: 256 },
    { id: 2, name: '订单统计查询', condition: '按日期统计', queryTime: '2024-01-15 10:20:00', resultCount: 89 },
    { id: 3, name: '产品库存查询', condition: '按仓库查询', queryTime: '2024-01-15 10:25:00', resultCount: 1567 }
  ]
}

const showUserModal = () => {
  editingUser.value = null
  modals.value.user = true
  userForm.value = { username: '', password: '', realName: '', email: '', phone: '', role: 0 }
}

const closeModal = (type) => {
  modals.value[type] = false
}

// 数据采集相关方法
const showCollectModal = (collect) => {
  editingCollect.value = collect
  if (collect) {
    collectForm.value = {
      name: collect.name,
      sourceUrl: collect.source,
      sourceType: 'web',
      selectorRules: collect.selectorRules || '',
      headers: '',
      cookies: '',
      cronExpression: ''
    }
  } else {
    collectForm.value = { name: '', sourceUrl: '', sourceType: 'web', selectorRules: '', headers: '', cookies: '', cronExpression: '' }
  }
  modals.value.collect = true
}

const saveCollect = async () => {
  if (!collectForm.value.name || !collectForm.value.sourceUrl) {
    ElMessage.warning('请输入采集名称和来源URL')
    return
  }
  try {
    const url = editingCollect.value
      ? `${API_BASE}/dataCollect/${editingCollect.value.id}`
      : `${API_BASE}/dataCollect`
    const method = editingCollect.value ? 'PUT' : 'POST'

    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(collectForm.value)
    })
    const result = await response.json()
    if (result.code === 0) {
      ElMessage.success(editingCollect.value ? '更新成功' : '创建成功')
      closeModal('collect')
      loadDataCollects()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('网络错误')
  }
}

const executeCollect = async (collect) => {
  try {
    ElMessage.info('正在执行采集...')
    const response = await fetch(`${API_BASE}/dataCollect/${collect.id}/execute`, {
      method: 'POST'
    })
    const result = await response.json()
    if (result.success || result.code === 0) {
      ElMessage.success('采集执行成功')
      loadDataCollects()
    } else {
      ElMessage.error(result.message || '采集执行失败')
    }
  } catch (error) {
    ElMessage.error('网络错误')
  }
}

const deleteCollect = async (id) => {
  ElMessageBox.confirm('确定删除该采集配置吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await fetch(`${API_BASE}/dataCollect/${id}`, {
        method: 'DELETE'
      })
      const result = await response.json()
      if (result.code === 0) {
        ElMessage.success('删除成功')
        loadDataCollects()
      } else {
        ElMessage.error(result.message || '删除失败')
      }
    } catch (error) {
      ElMessage.error('网络错误')
    }
  })
}

const switchMenu = (menu) => {
  activeMenu.value = menu
}

const goToPersonal = () => {
  dropdownVisible.value = false
  activeMenu.value = 'dashboard'
  ElMessage.info('已返回首页')
}

const handleLogout = () => {
  dropdownVisible.value = false
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('userInfo')
    localStorage.removeItem('token')
    ElMessage.success('已退出登录')
    setTimeout(() => {
      router.push('/login')
    }, 500)
  })
}

const toggleDropdown = () => {
  dropdownVisible.value = !dropdownVisible.value
}

const saveUser = async () => {
  try {
    if (editingUser.value) {
      const response = await fetch(`${API_BASE}/user/${editingUser.value.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userForm.value)
      })
      const result = await response.json()
      if (result.code === 0) {
        ElMessage.success('更新成功')
      } else {
        ElMessage.error(result.message || '更新失败')
      }
    } else {
      if (!userForm.value.username || !userForm.value.password) {
        ElMessage.warning('请输入用户名和密码')
        return
      }
      const response = await fetch(`${API_BASE}/user/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userForm.value)
      })
      const result = await response.json()
      if (result.code === 0) {
        ElMessage.success('创建成功')
      } else {
        ElMessage.error(result.message || '创建失败')
      }
    }
    closeModal('user')
    await loadUsers()
  } catch (error) {
    ElMessage.error('网络错误')
  }
}

const editUser = (user) => {
  editingUser.value = user
  modals.value.user = true
  userForm.value = {
    username: user.username,
    password: '',
    realName: user.realName || '',
    email: user.email || '',
    phone: user.phone || '',
    role: user.role || 0
  }
}

const toggleUserStatus = async (user) => {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    const response = await fetch(`${API_BASE}/user/${user.id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    })
    const result = await response.json()
    if (result.code === 0) {
      ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
      await loadUsers()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('网络错误')
  }
}

const deleteUser = async (id) => {
  ElMessageBox.confirm('确定删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await fetch(`${API_BASE}/user/${id}`, {
        method: 'DELETE'
      })
      const result = await response.json()
      if (result.code === 0) {
        ElMessage.success('删除成功')
        await loadUsers()
      } else {
        ElMessage.error(result.message || '删除失败')
      }
    } catch (error) {
      ElMessage.error('网络错误')
    }
  })
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.top-navbar {
  background: #ffffff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid #e2edf5;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #1e3a4a 0%, #1f5270 100%);
  -webkit-background-clip: text;
  background: text;
  -webkit-text-fill-color: transparent;
}

.system-name {
  font-size: 20px;
  font-weight: 600;
  color: #1e3a4a;
  letter-spacing: 1px;
}

.user-dropdown {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8fbfe;
  padding: 6px 16px 6px 12px;
  border-radius: 40px;
  cursor: pointer;
  transition: all 0.2s;
}

.user-dropdown:hover {
  background: #eef3fa;
}

.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(145deg, #2a5f7a, #1e3a4a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 16px;
}

.user-name-text {
  font-size: 15px;
  font-weight: 500;
  color: #1e3a4a;
}

.dropdown-arrow {
  font-size: 14px;
  color: #7e9aae;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: white;
  border-radius: 20px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.15);
  min-width: 180px;
  overflow: hidden;
  z-index: 1000;
  border: 1px solid #e2ecf5;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  font-size: 14px;
  color: #2c5a70;
  cursor: pointer;
  transition: background 0.2s;
}

.dropdown-item:hover {
  background: #f0f6fc;
}

.main-container {
  display: flex;
  flex: 1;
}

.sidebar {
  width: 260px;
  background: #ffffff;
  border-right: 1px solid #e6eef5;
  padding: 28px 0;
  flex-shrink: 0;
  height: calc(100vh - 64px);
  position: sticky;
  top: 64px;
  overflow-y: auto;
}

.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 20px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 20px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 500;
  color: #466075;
  cursor: pointer;
  transition: all 0.2s;
}

.menu-item:hover {
  background: #f0f6fc;
  color: #1e3a4a;
}

.menu-item.active {
  background: linear-gradient(95deg, #eef3fc, #e6f0f8);
  color: #1e3a4a;
  font-weight: 600;
  border-left: 3px solid #1e3a4a;
  border-radius: 12px;
}

.menu-icon {
  font-size: 20px;
}

.submenu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 34px;
}

.submenu .menu-item {
  font-size: 14px;
  padding: 10px 16px;
}

.content-area {
  flex: 1;
  padding: 28px 32px;
  overflow-y: auto;
  background: #f0f4fa;
}

.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2rem;
  border-radius: 16px;
  margin-bottom: 2rem;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
}

.welcome-card h2 {
  font-size: 24px;
  margin-bottom: 8px;
}

.welcome-card p {
  opacity: 0.9;
  font-size: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  padding: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 1rem;
}

.stat-icon.purple { background: #ede9fe; color: #7c3aed; }
.stat-icon.pink { background: #fce7f3; color: #db2777; }
.stat-icon.blue { background: #dbeafe; color: #2563eb; }
.stat-icon.green { background: #d1fae5; color: #059669; }

.stat-number {
  font-size: 2rem;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.stat-label {
  color: #6b7280;
  font-size: 0.875rem;
}

.content-section {
  background: white;
  padding: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 2rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
}
</style>
