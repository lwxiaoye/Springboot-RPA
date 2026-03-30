<template>
  <div class="dashboard-view">
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
          <el-menu-item index="dashboard">首页</el-menu-item>
          <el-menu-item index="rpa">RPA运营管理</el-menu-item>
          <el-menu-item index="system">系统管理</el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <div class="user-info">
          <el-dropdown>
            <span class="user-dropdown">
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

    <!-- 仪表盘内容 -->
    <div class="dashboard-content">
      <!-- 欢迎卡片 - 蓝色渐变 -->
      <div class="welcome-section">
        <div class="welcome-left">
          <h2>欢迎回来，{{ currentUser.realName || currentUser.username }}！</h2>
          <p class="welcome-tip">今天是 {{ currentDate }}，祝您工作愉快</p>
        </div>
        <div class="welcome-right">
          <div class="quick-stats">
            <div class="quick-stat-item">
              <span class="stat-value success">{{ taskStatus.completed }}</span>
              <span class="stat-label">今日完成</span>
            </div>
            <div class="quick-stat-item">
              <span class="stat-value warning">{{ taskStatus.running }}</span>
              <span class="stat-label">进行中</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷入口和最近任务 - 两列布局，最近任务加长 -->
      <div class="dashboard-blocks">
        <!-- 快捷入口卡片 - 只保留4个核心功能 -->
        <div class="info-card shortcut-card">
          <div class="card-header">
            <h3>快捷入口</h3>
            <span class="card-more" @click="goToShortcutMore">查看详情 ></span>
          </div>
          <div class="shortcut-grid">
            <div 
              v-for="item in quickEntries" 
              :key="item.key" 
              class="shortcut-item"
              @click="goToShortcut(item)"
            >
              <div class="shortcut-icon" :class="item.iconClass">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="shortcut-content">
                <span class="shortcut-label">{{ item.label }}</span>
                <span class="shortcut-desc">{{ item.desc }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 最近任务卡片 - 加长版 -->
        <div class="info-card recent-tasks-card wide-card">
          <div class="card-header">
            <h3>最近任务</h3>
            <span class="card-more" @click="goToTasks">查看全部 ></span>
          </div>
          <el-table 
            :data="recentTasks" 
            style="width: 100%"
            :show-header="true"
            stripe
          >
            <el-table-column prop="name" label="任务编码" min-width="150" />
            <el-table-column prop="processName" label="流程名称" min-width="120" />
            <el-table-column prop="stockName" label="股票名称" min-width="120" />
            <el-table-column prop="status" label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
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
import { ArrowDown, Plus, Setting, Monitor, DataAnalysis } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 顶部菜单状态
const activeTopMenu = ref('dashboard')

// 用户信息
const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  role: 1
})

// 统计数据
const stats = ref({ tasks: 0, robots: 0, processes: 0, logs: 0 })
const statsChange = ref({ tasks: 12, robots: 5, processes: 8, logs: 15 })
const taskStatus = ref({ running: 0, pending: 0, completed: 0, failed: 0 })
const selectedPeriod = ref('近7天')

const modals = ref({ user: false, collect: false })
const editingUser = ref(null)
const editingCollect = ref(null)

const userForm = ref({ username: '', password: '', realName: '', email: '', phone: '', role: 0 })
const collectForm = ref({ name: '', sourceUrl: '', sourceType: 'web', selectorRules: '', headers: '', cookies: '', cronExpression: '' })
const loading = ref(false)

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
})

// 获取状态类型
const getStatusType = (status) => {
  const map = {
    running: 'warning',
    pending: 'info',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    running: '运行中',
    pending: '待执行',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

// ECharts 配置
const lineChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['完成任务', '新建任务'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '完成任务',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: [12, 15, 10, 18, 22, 19, 25],
      itemStyle: { color: '#67c23a' }
    },
    {
      name: '新建任务',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: [8, 12, 14, 10, 18, 20, 15],
      itemStyle: { color: '#409eff' }
    }
  ]
}))

const barChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['数量'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: { type: 'category', data: ['运行中', '待执行', '已完成', '失败'] },
  yAxis: { type: 'value' },
  series: [{
    name: '数量',
    type: 'bar',
    barWidth: '50%',
    data: [
      { value: taskStatus.value.running, itemStyle: { color: '#e6a23c' } },
      { value: taskStatus.value.pending, itemStyle: { color: '#909399' } },
      { value: taskStatus.value.completed, itemStyle: { color: '#67c23a' } },
      { value: taskStatus.value.failed, itemStyle: { color: '#f56c6c' } }
    ]
  }]
}))

// 顶部菜单选择
const handleTopMenuSelect = (index) => {
  if (index === 'dashboard') {
    // 已在首页
  } else if (index === 'rpa') {
    router.push('/rpa/tasks')
  } else if (index === 'system') {
    router.push('/system/profile')
  }
}

const goToShortcut = (item) => {
  if (item.path) {
    router.push(item.path)
  }
}

const goToShortcutMore = () => {
  router.push('/shortcuts')
}

const goToTasks = () => {
  router.push('/rpa/tasks')
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

// 数据加载
const loadUserFromStorage = () => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUser.value = { ...currentUser.value, ...user }
    } catch (e) {}
  }
}

const loadStats = async () => {
  try {
    const [taskRes, robotRes, processRes, logRes] = await Promise.all([
      fetch(`${API_BASE}/task`).catch(() => null),
      fetch(`${API_BASE}/robot`).catch(() => null),
      fetch(`${API_BASE}/process`).catch(() => null),
      fetch(`${API_BASE}/log`).catch(() => null)
    ])

    const tasks = taskRes ? (await taskRes.json()).data || [] : []
    const robots = robotRes ? (await robotRes.json()).data || [] : []
    const processes = processRes ? (await processRes.json()).data || [] : []
    const logs = logRes ? (await logRes.json()).data || [] : []

    stats.value = {
      tasks: tasks.length,
      robots: robots.length,
      processes: processes.length,
      logs: logs.length
    }

    taskStatus.value = {
      running: tasks.filter(t => t.status === 'running').length,
      pending: tasks.filter(t => t.status === 'pending').length,
      completed: tasks.filter(t => t.status === 'completed').length,
      failed: tasks.filter(t => t.status === 'failed').length
    }
  } catch (error) {
    stats.value = { tasks: 8, robots: 4, processes: 5, logs: 156 }
    taskStatus.value = { running: 2, pending: 3, completed: 10, failed: 1 }
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
      // 新建用户时，若未填写密码，默认使用 123456
      const userData = { ...userForm.value }
      if (!userData.password || userData.password.trim() === '') {
        userData.password = '123456'
      }
      if (!userData.username) {
        ElMessage.warning('请输入用户名')
        return
      }
      const response = await fetch(`${API_BASE}/user/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userData)
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
.dashboard-view {
  min-height: 100vh;
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

/* 仪表盘内容 */
.dashboard-content {
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

/* 欢迎卡片 - 蓝色渐变 */
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #1677ff 0%, #409eff 100%);
  color: white;
  padding: 28px 32px;
  border-radius: 20px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.3);
}

.welcome-left h2 {
  font-size: 24px;
  margin-bottom: 8px;
}

.welcome-tip {
  opacity: 0.9;
  font-size: 14px;
}

.quick-stats {
  display: flex;
  gap: 32px;
}

.quick-stat-item {
  text-align: center;
}

.quick-stat-item .stat-value {
  font-size: 32px;
  font-weight: bold;
}

.quick-stat-item .stat-value.success {
  color: #fff;
}

.quick-stat-item .stat-value.warning {
  color: #fff;
}

.quick-stat-item .stat-label {
  font-size: 14px;
  opacity: 0.8;
}

/* 快捷入口和最近任务布局 - 两列，最近任务占2/3宽度 */
.dashboard-blocks {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
  margin-bottom: 24px;
}

.info-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafbfc;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2f3d;
  margin: 0;
}

.card-more {
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  transition: color 0.2s;
}

.card-more:hover {
  color: #66b1ff;
  text-decoration: underline;
}

/* 快捷入口网格样式 - 4个入口，每个带图标、标题和描述 */
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 20px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  border: 1px solid #f0f0f0;
}

.shortcut-item:hover {
  background: #f5f7fa;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #e4e7ed;
}

.shortcut-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
}

.shortcut-icon.icon-create {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}
.shortcut-icon.icon-flow {
  background: linear-gradient(135deg, #36cfc9, #5cdbd3);
}
.shortcut-icon.icon-robot {
  background: linear-gradient(135deg, #722ed1, #9254de);
}
.shortcut-icon.icon-query {
  background: linear-gradient(135deg, #13c2c2, #36cfc9);
}

.shortcut-content {
  flex: 1;
}

.shortcut-label {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #1f2f3d;
  margin-bottom: 4px;
}

.shortcut-desc {
  display: block;
  font-size: 12px;
  color: #8c8f93;
  line-height: 1.4;
}

/* 最近任务卡片 */
.wide-card {
  grid-column: span 1;
}

.recent-tasks-card :deep(.el-table) {
  font-size: 12px;
}

.recent-tasks-card :deep(.el-table th) {
  background: #fafbfc;
  color: #4a5568;
  font-weight: 500;
  padding: 12px 0;
}

.recent-tasks-card :deep(.el-table td) {
  padding: 10px 0;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
}

.stat-card-primary { border-left: 4px solid #409eff; }
.stat-card-info { border-left: 4px solid #67c23a; }
.stat-card-success { border-left: 4px solid #e6a23c; }
.stat-card-warning { border-left: 4px solid #909399; }

.stat-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.stat-trend {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.stat-trend.up {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.stat-trend.down {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.stat-card .stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #1e3a4a;
  margin-bottom: 4px;
}

.stat-card .stat-label {
  color: #6b7280;
  font-size: 14px;
}

.stat-chart-mini {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 40px;
  margin-top: 16px;
}

.mini-bar {
  flex: 1;
  background: linear-gradient(to top, #409eff, #67c23a);
  border-radius: 4px 4px 0 0;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.mini-bar:hover {
  opacity: 1;
}

/* 图表区域 */
.charts-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e3a4a;
}

.chart-actions {
  display: flex;
  gap: 8px;
}

.period-btn {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.period-btn:hover {
  background: #f5f7fa;
}

.period-btn.active {
  background: #409eff;
  color: white;
}

.chart-body {
  width: 100%;
}

/* 任务状态概览 + 系统信息 并排布局 */
.status-system-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

/* 任务状态概览 - 2x2网格 */
.status-overview {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.status-card {
  background: white;
  padding: 20px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  text-align: center;
}

.status-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-header.running .status-dot { background: #e6a23c; }
.status-header.pending .status-dot { background: #909399; }
.status-header.completed .status-dot { background: #67c23a; }
.status-header.failed .status-dot { background: #f56c6c; }

.status-value {
  font-size: 36px;
  font-weight: bold;
  color: #1e3a4a;
}

/* 系统信息卡片 */
.system-info-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.system-info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.system-info-list {
  padding: 16px 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: #8c8f93;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #1f2f3d;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .dashboard-blocks {
    grid-template-columns: 1fr;
  }

  .charts-section {
    grid-template-columns: 1fr;
  }
  
  .status-system-wrapper {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>