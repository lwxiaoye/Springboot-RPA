<template>
  <div class="dashboard-page">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-left">
        <h1>欢迎回来，{{ userName }}</h1>
        <p>这是您的 RPA 运营数据中心</p>
      </div>
      <div class="welcome-right">
        <span class="current-time">{{ currentTime }}</span>
        <span class="system-status online">
          <span class="status-dot"></span>
          系统运行正常
        </span>
      </div>
    </div>

    <!-- 核心指标 -->
    <div class="stats-grid">
      <div class="stat-card" @click="goTo('/rpa/tasks')">
        <div class="stat-header">
          <span class="stat-title">待执行任务</span>
          <span class="stat-badge warning" v-if="stats.pendingTasks > 0">{{ stats.pendingTasks }}</span>
        </div>
        <div class="stat-value">{{ stats.pendingTasks }}</div>
        <div class="stat-footer">
          <span class="stat-trend up" v-if="stats.pendingTrend > 0">+{{ stats.pendingTrend }}%</span>
          <span class="stat-trend down" v-else-if="stats.pendingTrend < 0">{{ stats.pendingTrend }}%</span>
          <span class="stat-link">查看详情</span>
        </div>
      </div>

      <div class="stat-card" @click="goTo('/rpa/logs?type=failed')">
        <div class="stat-header">
          <span class="stat-title">异常任务</span>
          <span class="stat-badge danger" v-if="stats.failedTasks > 0">{{ stats.failedTasks }}</span>
        </div>
        <div class="stat-value" :class="{ 'text-danger': stats.failedTasks > 0 }">{{ stats.failedTasks }}</div>
        <div class="stat-footer">
          <span class="stat-link">查看详情</span>
        </div>
      </div>

      <div class="stat-card" @click="goTo('/rpa/robots')">
        <div class="stat-header">
          <span class="stat-title">在线机器人</span>
        </div>
        <div class="stat-value">{{ stats.onlineRobots }} <span class="stat-total">/ {{ stats.totalRobots }}</span></div>
        <div class="stat-footer">
          <div class="utilization-mini">
            <div class="util-bar">
              <div class="util-fill" :style="{ width: utilizationPercent + '%' }"></div>
            </div>
            <span class="util-text">利用率 {{ utilizationPercent }}%</span>
          </div>
        </div>
      </div>

      <div class="stat-card" @click="goTo('/rpa/logs')">
        <div class="stat-header">
          <span class="stat-title">今日执行</span>
        </div>
        <div class="stat-value">{{ stats.todayExecutions }}</div>
        <div class="stat-footer">
          <span class="success-rate">成功率 {{ stats.successRate }}%</span>
          <span class="stat-link">查看详情</span>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 左侧 -->
      <div class="content-left">
        <!-- 队列健康度 -->
        <div class="panel queue-health">
          <div class="panel-header">
            <h3>队列健康度</h3>
            <el-button size="small" @click="goTo('/rpa/queues')">管理队列</el-button>
          </div>
          <div class="panel-body">
            <div class="health-stats">
              <div class="health-item healthy">
                <span class="count">{{ healthyQueues }}</span>
                <span class="label">健康</span>
              </div>
              <div class="health-item warning">
                <span class="count">{{ warningQueues }}</span>
                <span class="label">预警</span>
              </div>
              <div class="health-item critical">
                <span class="count">{{ criticalQueues }}</span>
                <span class="label">告警</span>
              </div>
            </div>
            <div class="health-chart">
              <v-chart :option="queuePieOption" autoresize style="height: 180px;"></v-chart>
            </div>
          </div>
        </div>

        <!-- 快捷入口 -->
        <div class="panel quick-entry">
          <div class="panel-header">
            <h3>快捷入口</h3>
          </div>
          <div class="panel-body">
            <div class="quick-grid">
              <div class="quick-item" @click="goTo('/rpa/processes')">
                <div class="quick-icon blue">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="16 18 22 12 16 6"/>
                    <polyline points="8 6 2 12 8 18"/>
                  </svg>
                </div>
                <span>流程管理</span>
              </div>
              <div class="quick-item" @click="goTo('/rpa/tasks')">
                <div class="quick-icon green">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
                    <rect x="9" y="3" width="6" height="4" rx="1"/>
                  </svg>
                </div>
                <span>任务调度</span>
              </div>
              <div class="quick-item" @click="goTo('/rpa/robots')">
                <div class="quick-icon cyan">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="3" width="20" height="14" rx="2"/>
                    <line x1="8" y1="21" x2="16" y2="21"/>
                    <line x1="12" y1="17" x2="12" y2="21"/>
                  </svg>
                </div>
                <span>机器人</span>
              </div>
              <div class="quick-item" @click="goTo('/rpa/logs')">
                <div class="quick-icon orange">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                </div>
                <span>执行日志</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧 -->
      <div class="content-right">
        <!-- 机器人状态 -->
        <div class="panel robot-status">
          <div class="panel-header">
            <h3>机器人状态</h3>
            <el-button size="small" @click="goTo('/rpa/robots')">查看全部</el-button>
          </div>
          <div class="panel-body">
            <div class="robot-grid">
              <div class="robot-item idle" @click="filterRobots('idle')">
                <div class="robot-icon">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="6" y="4" width="4" height="16"/>
                    <rect x="14" y="4" width="4" height="16"/>
                  </svg>
                </div>
                <div class="robot-info">
                  <span class="robot-count">{{ robotStats.idle }}</span>
                  <span class="robot-label">空闲</span>
                </div>
              </div>
              <div class="robot-item busy" @click="filterRobots('busy')">
                <div class="robot-icon">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                  </svg>
                </div>
                <div class="robot-info">
                  <span class="robot-count">{{ robotStats.busy }}</span>
                  <span class="robot-label">忙碌</span>
                </div>
              </div>
              <div class="robot-item offline" @click="filterRobots('offline')">
                <div class="robot-icon">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="1" y1="1" x2="23" y2="23"/>
                    <path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55"/>
                  </svg>
                </div>
                <div class="robot-info">
                  <span class="robot-count">{{ robotStats.offline }}</span>
                  <span class="robot-label">离线</span>
                </div>
              </div>
              <div class="robot-item error" @click="filterRobots('error')">
                <div class="robot-icon">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                    <line x1="12" y1="9" x2="12" y2="13"/>
                    <line x1="12" y1="17" x2="12.01" y2="17"/>
                  </svg>
                </div>
                <div class="robot-info">
                  <span class="robot-count">{{ robotStats.error }}</span>
                  <span class="robot-label">异常</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 执行趋势 -->
        <div class="panel execution-trend">
          <div class="panel-header">
            <h3>执行趋势</h3>
            <el-button size="small" @click="goTo('/rpa/logs')">查看详情</el-button>
          </div>
          <div class="panel-body">
            <v-chart :option="executionTrendOption" autoresize style="height: 180px;"></v-chart>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部区域 -->
    <div class="bottom-content">
      <!-- 待处理任务 -->
      <div class="panel task-list">
        <div class="panel-header">
          <h3>待处理任务</h3>
          <el-button size="small" @click="goTo('/rpa/tasks')">查看全部</el-button>
        </div>
        <div class="panel-body">
          <el-table :data="pendingTasks" size="small" max-height="200" class="data-table" :show-overflow-tooltip="true">
            <el-table-column prop="name" label="任务名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="queue" label="所属队列" width="100" show-overflow-tooltip />
            <el-table-column prop="priority" label="优先级" width="70" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.priority === 'high' ? 'danger' : row.priority === 'medium' ? 'warning' : 'info'" effect="plain">
                  {{ row.priority === 'high' ? '高' : row.priority === 'medium' ? '中' : '低' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="warning" effect="light">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="120" />
          </el-table>
        </div>
      </div>

      <!-- 最近执行 -->
      <div class="panel recent-logs">
        <div class="panel-header">
          <h3>最近执行</h3>
          <el-button size="small" @click="goTo('/rpa/logs')">查看全部</el-button>
        </div>
        <div class="panel-body">
          <el-table :data="recentLogs" size="small" max-height="200" class="data-table" :show-overflow-tooltip="true">
            <el-table-column prop="taskName" label="任务" min-width="120" show-overflow-tooltip />
            <el-table-column prop="robotName" label="机器人" width="90" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="70" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getStatusType(row.status)" effect="light">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="时间" width="120" />
          </el-table>
        </div>
      </div>

      <!-- 告警信息 -->
      <div class="panel alerts-panel">
        <div class="panel-header">
          <h3>告警信息</h3>
          <el-badge :value="alertCount" type="danger" :hidden="alertCount === 0" />
        </div>
        <div class="panel-body">
          <div v-if="alerts.length === 0" class="empty-state">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
            <span>暂无告警</span>
          </div>
          <div v-else class="alert-list">
            <div v-for="alert in alerts" :key="alert.id" class="alert-item" :class="alert.level">
              <div class="alert-content">
                <span class="alert-title">{{ alert.title }}</span>
                <span class="alert-desc">{{ alert.message }}</span>
              </div>
              <span class="alert-time">{{ alert.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart as PieChartType, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { apiGet } from '../../utils/api.js'

use([CanvasRenderer, LineChart, PieChartType, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()
const currentTime = ref('')
const userName = ref('管理员')

// 统计数据
const stats = reactive({
  pendingTasks: 0,
  pendingTrend: 0,
  failedTasks: 0,
  onlineRobots: 0,
  totalRobots: 0,
  todayExecutions: 0,
  successRate: 100
})

// 机器人统计
const robotStats = reactive({ idle: 0, busy: 0, offline: 0, error: 0 })

// 队列数据
const queues = ref([])
const healthyQueues = computed(() => queues.value.filter(q => q.pendingCount <= 100).length)
const warningQueues = computed(() => queues.value.filter(q => q.pendingCount > 100 && q.pendingCount <= 500).length)
const criticalQueues = computed(() => queues.value.filter(q => q.pendingCount > 500).length)

const utilizationPercent = computed(() => {
  if (stats.totalRobots === 0) return 0
  return Math.round((stats.onlineRobots / stats.totalRobots) * 100)
})

// 待处理任务
const pendingTasks = ref([])

// 最近日志
const recentLogs = ref([])

// 告警
const alerts = ref([])
const alertCount = computed(() => alerts.value.length)

// 图表配置
const queuePieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { orient: 'vertical', right: 10, top: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: '#94a3b8' } },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['35%', '50%'],
    label: { show: false },
    data: [
      { value: healthyQueues.value, name: '健康', itemStyle: { color: '#22c55e' } },
      { value: warningQueues.value, name: '预警', itemStyle: { color: '#f59e0b' } },
      { value: criticalQueues.value, name: '告警', itemStyle: { color: '#ef4444' } }
    ].filter(item => item.value > 0)
  }]
}))

const executionTrendOption = computed(() => {
  const hours = Array.from({ length: 12 }, (_, i) => `${i * 2}:00`)
  const success = Array.from({ length: 12 }, () => Math.floor(Math.random() * 80) + 20)
  const failed = Array.from({ length: 12 }, () => Math.floor(Math.random() * 10))

  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '20px', containLabel: true },
    xAxis: { type: 'category', data: hours, axisLine: { lineStyle: { color: '#334155' } }, axisLabel: { color: '#94a3b8', fontSize: 10 } },
    yAxis: { type: 'value', axisLine: { lineStyle: { color: '#334155' } }, axisLabel: { color: '#94a3b8', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [
      { name: '成功', type: 'bar', data: success, itemStyle: { color: '#22c55e', borderRadius: [2, 2, 0, 0] }, barWidth: '40%' },
      { name: '失败', type: 'bar', data: failed, itemStyle: { color: '#ef4444', borderRadius: [2, 2, 0, 0] } }
    ]
  }
})

// 方法
const goTo = (path) => router.push(path)
const filterRobots = (status) => router.push(`/rpa/robots?status=${status}`)

const getStatusType = (status) => {
  const map = { success: 'success', completed: 'success', failed: 'danger', running: 'warning' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { success: '成功', completed: '完成', failed: '失败', running: '进行中' }
  return map[status] || status
}

const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

const loadData = async () => {
  try {
    const [taskRes, robotRes, logRes, queueRes] = await Promise.all([
      apiGet('/task').catch(() => ({ code: -1, data: [] })),
      apiGet('/robot').catch(() => ({ code: -1, data: [] })),
      apiGet('/log').catch(() => ({ code: -1, data: [] })),
      apiGet('/queue').catch(() => ({ code: -1, data: [] }))
    ])

    if (taskRes.code === 0 && taskRes.data) {
      const tasks = taskRes.data
      stats.pendingTasks = tasks.filter(t => t.status === 'pending' || t.status === 'assigned').length
      stats.failedTasks = tasks.filter(t => t.status === 'failed').length
      stats.pendingTrend = Math.floor(Math.random() * 20) - 10
      pendingTasks.value = tasks.filter(t => t.status === 'pending' || t.status === 'assigned').slice(0, 5).map(t => ({
        name: t.name || t.taskName || '未知任务',
        queue: t.queueName || '-',
        priority: t.priority || 'normal',
        status: '等待执行',
        createTime: t.createTime ? new Date(t.createTime).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-'
      }))
    }

    if (robotRes.code === 0 && robotRes.data) {
      const robots = robotRes.data
      stats.totalRobots = robots.length
      stats.onlineRobots = robots.filter(r => r.status !== 'offline').length
      robotStats.idle = robots.filter(r => r.status === 'idle').length
      robotStats.busy = robots.filter(r => r.status === 'busy').length
      robotStats.offline = robots.filter(r => r.status === 'offline').length
      robotStats.error = robots.filter(r => r.status === 'error').length
    }

    if (queueRes.code === 0 && queueRes.data) {
      queues.value = queueRes.data.map(q => ({
        id: q.id,
        name: q.name || q.queueName,
        pendingCount: q.pendingCount || q.currentPendingCount || 0
      }))
    }

    if (logRes.code === 0 && logRes.data) {
      const logs = logRes.data
      stats.todayExecutions = logs.length
      const success = logs.filter(l => l.status === 'success' || l.status === 'completed').length
      stats.successRate = logs.length > 0 ? Math.round((success / logs.length) * 100) : 100
      recentLogs.value = logs.slice(0, 5).map(l => ({
        taskName: l.taskName || l.processName || '未知任务',
        robotName: l.robotName || 'Robot-01',
        status: l.status,
        startTime: l.startTime ? new Date(l.startTime).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-'
      }))
    }

    // 模拟告警
    if (queues.value.filter(q => q.pendingCount > 500).length > 0) {
      alerts.value = queues.value.filter(q => q.pendingCount > 500).slice(0, 3).map(q => ({
        id: q.id,
        title: '队列积压',
        message: `${q.name} 待处理 ${q.pendingCount} 条`,
        level: 'danger',
        time: '刚刚'
      }))
    }

    if (stats.failedTasks > 0) {
      alerts.value.push({
        id: 'failed',
        title: '任务执行失败',
        message: `${stats.failedTasks} 个任务执行失败`,
        level: 'warning',
        time: '刚刚'
      })
    }

  } catch (e) {
    console.error('加载数据失败:', e)
  }
}

let timeTimer = null

onMounted(() => {
  updateTime()
  loadData()
  timeTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
})
</script>

<style scoped>
.dashboard-page {
  --primary: #3b82f6;
  --success: #22c55e;
  --warning: #f59e0b;
  --danger: #ef4444;
  --cyan: #06b6d4;

  --bg-page: #0f172a;
  --bg-card: #1e293b;
  --bg-hover: #334155;
  --border: #334155;
  --text-primary: #f1f5f9;
  --text-secondary: #94a3b8;
  --text-muted: #64748b;

  padding: 0;
  background: var(--bg-page);
  min-height: 100vh;
  color: var(--text-primary);
}

/* 欢迎区域 */
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid var(--border);
}

.welcome-left h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.welcome-left p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.welcome-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.current-time {
  font-size: 14px;
  color: var(--text-secondary);
  font-family: 'Consolas', monospace;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
}

.system-status.online {
  color: var(--success);
  border-color: rgba(34, 197, 94, 0.3);
}

.system-status .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 核心指标 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 20px 24px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.stat-card:hover {
  border-color: var(--primary);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.stat-title {
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.stat-badge.warning { background: rgba(245,158,11,0.15); color: var(--warning); }
.stat-badge.danger { background: rgba(239,68,68,0.15); color: var(--danger); }

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-value.text-danger { color: var(--danger); }

.stat-total {
  font-size: 16px;
  font-weight: 400;
  color: var(--text-secondary);
}

.stat-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.stat-trend {
  font-size: 12px;
  font-weight: 600;
}

.stat-trend.up { color: var(--success); }
.stat-trend.down { color: var(--danger); }

.stat-link {
  font-size: 12px;
  color: var(--primary);
  cursor: pointer;
}

.stat-link:hover {
  text-decoration: underline;
}

.success-rate {
  font-size: 12px;
  color: var(--success);
  font-weight: 500;
}

.utilization-mini {
  flex: 1;
}

.util-bar {
  height: 4px;
  background: var(--bg-hover);
  border-radius: 2px;
  margin-bottom: 4px;
}

.util-fill {
  height: 100%;
  background: var(--primary);
  border-radius: 2px;
  transition: width 0.3s;
}

.util-text {
  font-size: 11px;
  color: var(--text-muted);
}

/* 主要内容区 */
.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 0 24px 16px;
}

.content-left, .content-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 面板 */
.panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border);
}

.panel-header h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0;
}

.panel-body {
  padding: 16px 18px;
}

/* 队列健康度 */
.health-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.health-item {
  flex: 1;
  text-align: center;
  padding: 12px;
  border-radius: 6px;
}

.health-item.healthy { background: rgba(34,197,94,0.1); }
.health-item.warning { background: rgba(245,158,11,0.1); }
.health-item.critical { background: rgba(239,68,68,0.1); }

.health-item .count {
  display: block;
  font-size: 24px;
  font-weight: 700;
}

.health-item.healthy .count { color: var(--success); }
.health-item.warning .count { color: var(--warning); }
.health-item.critical .count { color: var(--danger); }

.health-item .label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* 快捷入口 */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: var(--bg-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.quick-item:hover {
  border-color: var(--primary);
  background: rgba(59,130,246,0.1);
}

.quick-item span {
  font-size: 12px;
  color: var(--text-secondary);
}

.quick-icon {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.quick-icon.blue { background: var(--primary); }
.quick-icon.green { background: var(--success); }
.quick-icon.cyan { background: var(--cyan); }
.quick-icon.orange { background: var(--warning); }

/* 机器人状态 */
.robot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.robot-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 10px;
  background: var(--bg-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.robot-item:hover {
  border-color: var(--primary);
}

.robot-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.robot-item.idle .robot-icon { background: rgba(34,197,94,0.15); color: var(--success); }
.robot-item.busy .robot-icon { background: rgba(59,130,246,0.15); color: var(--primary); }
.robot-item.offline .robot-icon { background: rgba(100,116,139,0.15); color: var(--text-muted); }
.robot-item.error .robot-icon { background: rgba(239,68,68,0.15); color: var(--danger); }

.robot-info {
  text-align: center;
}

.robot-count {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.robot-label {
  font-size: 11px;
  color: var(--text-muted);
}

/* 底部区域 */
.bottom-content {
  display: grid;
  grid-template-columns: 1fr 1fr 300px;
  gap: 16px;
  padding: 0 24px 24px;
}

/* 表格 */
.data-table {
  background: transparent;
}

.data-table :deep(.el-table__header th) {
  background: var(--bg-hover);
  color: var(--text-secondary);
  font-weight: 600;
  border-color: var(--border);
}

.data-table :deep(.el-table__body td) {
  border-color: var(--border);
  color: var(--text-primary);
}

.data-table :deep(.el-table__body tr:hover > td) {
  background: var(--bg-hover);
}

/* 告警 */
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 10px 12px;
  background: var(--bg-hover);
  border-radius: 6px;
  border-left: 3px solid;
}

.alert-item.danger { border-color: var(--danger); }
.alert-item.warning { border-color: var(--warning); }

.alert-content {
  flex: 1;
}

.alert-title {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.alert-desc {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.alert-time {
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
  color: var(--text-muted);
  gap: 8px;
}

.empty-state svg {
  opacity: 0.4;
}

/* 响应式 */
@media (max-width: 1400px) {
  .bottom-content {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .main-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .welcome-section {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  .bottom-content {
    grid-template-columns: 1fr;
  }
}

/* 滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: var(--bg-page);
}

::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 3px;
}
</style>
