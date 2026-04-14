<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h2>RPA工作台</h2>
      <p class="page-desc">实时监控RPA系统运行状态，快速处理待办事项</p>
    </div>

    <!-- 核心统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" @click="goTo('/rpa/tasks')">
        <div class="stat-icon primary"><el-icon><Ticket /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.pendingTasks }}</span>
          <span class="stat-label">待执行任务</span>
        </div>
        <div class="stat-trend warning" v-if="stats.pendingTasks > 0">
          <el-icon><Warning /></el-icon> 待处理
        </div>
      </div>

      <div class="stat-card" @click="goTo('/rpa/logs')">
        <div class="stat-icon danger"><el-icon><CircleClose /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.failedTasks }}</span>
          <span class="stat-label">异常任务</span>
        </div>
        <div class="stat-trend danger" v-if="stats.failedTasks > 0">
          <el-icon><Bell /></el-icon> 需要处理
        </div>
      </div>

      <div class="stat-card" @click="goTo('/rpa/robots')">
        <div class="stat-icon success"><el-icon><Monitor /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.onlineRobots }}</span>
          <span class="stat-label">在线机器人</span>
        </div>
        <div class="stat-sub">{{ stats.totalRobots }} 台总量</div>
      </div>

      <div class="stat-card" @click="goTo('/rpa/logs')">
        <div class="stat-icon info"><el-icon><Document /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.todayExecutions }}</span>
          <span class="stat-label">今日执行</span>
        </div>
        <div class="stat-sub">成功率 {{ stats.successRate }}%</div>
      </div>
    </div>

    <!-- 数据统计 -->
    <div class="stats-row secondary">
      <div class="stat-card-mini">
        <div class="mini-icon blue"><el-icon><DataAnalysis /></el-icon></div>
        <div class="mini-content">
          <span class="mini-value">{{ stats.todayDataCollected }}</span>
          <span class="mini-label">今日采集数据</span>
        </div>
      </div>

      <div class="stat-card-mini">
        <div class="mini-icon green"><el-icon><List /></el-icon></div>
        <div class="mini-content">
          <span class="mini-value">{{ stats.activeProcesses }}</span>
          <span class="mini-label">活跃流程</span>
        </div>
      </div>

      <div class="stat-card-mini">
        <div class="mini-icon orange"><el-icon><Clock /></el-icon></div>
        <div class="mini-content">
          <span class="mini-value">{{ stats.todayRuntime }}</span>
          <span class="mini-label">运行时长</span>
        </div>
      </div>

      <div class="stat-card-mini">
        <div class="mini-icon purple"><el-icon><Bell /></el-icon></div>
        <div class="mini-content">
          <span class="mini-value">{{ stats.pendingAlerts }}</span>
          <span class="mini-label">待处理告警</span>
        </div>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧：待办事项 -->
      <div class="panel todo-panel">
        <div class="panel-header">
          <h3><el-icon><List /></el-icon> 待办事项</h3>
          <el-badge :value="todoItems.length" :hidden="todoItems.length === 0">
            <el-button size="small" text>全部</el-button>
          </el-badge>
        </div>
        <div class="panel-body">
          <div v-if="todoItems.length === 0" class="empty-state">
            <el-icon><CircleCheck /></el-icon>
            <span>暂无待办事项</span>
          </div>
          <div v-else class="todo-list">
            <div v-for="item in todoItems" :key="item.id" class="todo-item" @click="handleTodoItem(item)">
              <div class="todo-icon" :class="item.type">
                <el-icon v-if="item.type === 'task'"><Ticket /></el-icon>
                <el-icon v-else-if="item.type === 'error'"><CircleClose /></el-icon>
                <el-icon v-else-if="item.type === 'robot'"><Monitor /></el-icon>
                <el-icon v-else><Bell /></el-icon>
              </div>
              <div class="todo-content">
                <div class="todo-title">{{ item.title }}</div>
                <div class="todo-desc">{{ item.description }}</div>
              </div>
              <div class="todo-action">
                <el-button size="small" type="primary" :link="true">{{ item.actionText }}</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间：执行概览 -->
      <div class="panel execution-panel">
        <div class="panel-header">
          <h3><el-icon><TrendCharts /></el-icon> 执行概览</h3>
          <el-select v-model="executionPeriod" size="small" style="width: 100px;">
            <el-option label="今日" value="today" />
            <el-option label="本周" value="week" />
            <el-option label="本月" value="month" />
          </el-select>
        </div>
        <div class="panel-body">
          <div class="chart-wrapper">
            <v-chart :option="executionChartOption" autoresize style="height: 200px;"></v-chart>
          </div>
          <div class="execution-summary">
            <div class="summary-item">
              <span class="label">总执行</span>
              <span class="value">{{ executionStats.total }}</span>
            </div>
            <div class="summary-item success">
              <span class="label">成功</span>
              <span class="value">{{ executionStats.success }}</span>
            </div>
            <div class="summary-item danger">
              <span class="label">失败</span>
              <span class="value">{{ executionStats.failed }}</span>
            </div>
            <div class="summary-item warning">
              <span class="label">进行中</span>
              <span class="value">{{ executionStats.running }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：机器人状态 -->
      <div class="panel robot-panel">
        <div class="panel-header">
          <h3><el-icon><Cpu /></el-icon> 机器人状态</h3>
          <el-button size="small" text @click="goTo('/rpa/robots')">查看全部</el-button>
        </div>
        <div class="panel-body">
          <div class="robot-summary">
            <div class="robot-status-item">
              <div class="status-dot online"></div>
              <span>空闲 {{ robotStats.idle }} 台</span>
            </div>
            <div class="robot-status-item">
              <div class="status-dot busy"></div>
              <span>忙碌 {{ robotStats.busy }} 台</span>
            </div>
            <div class="robot-status-item">
              <div class="status-dot offline"></div>
              <span>离线 {{ robotStats.offline }} 台</span>
            </div>
          </div>
          <div class="robot-list">
            <div v-for="robot in robotList" :key="robot.id" class="robot-item">
              <div class="robot-info">
                <el-icon class="robot-icon"><Monitor /></el-icon>
                <span class="robot-name">{{ robot.name }}</span>
              </div>
              <el-tag size="small" :type="getRobotStatusType(robot.status)">
                {{ getRobotStatusText(robot.status) }}
              </el-tag>
            </div>
            <div v-if="robotList.length === 0" class="empty-state small">
              <span>暂无机器人</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="secondary-content">
      <!-- 最近执行日志 -->
      <div class="panel logs-panel">
        <div class="panel-header">
          <h3><el-icon><Document /></el-icon> 最近执行日志</h3>
          <el-button size="small" text @click="goTo('/rpa/logs')">查看全部</el-button>
        </div>
        <div class="panel-body">
          <el-table :data="recentLogs" size="small" border stripe max-height="250">
            <el-table-column type="index" label="序号" width="50" align="center" />
            <el-table-column prop="taskName" label="任务名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getLogStatusType(row.status)">
                  {{ getLogStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dataCount" label="数据" width="70" align="center" />
            <el-table-column prop="startTime" label="开始时间" min-width="150" />
            <el-table-column prop="duration" label="耗时" width="80" align="center" />
          </el-table>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="panel quick-entry-panel">
        <div class="panel-header">
          <h3><el-icon><Grid /></el-icon> 快捷入口</h3>
        </div>
        <div class="panel-body">
          <div class="quick-entry-grid">
            <div class="quick-entry-item" @click="goTo('/rpa/processes')">
              <div class="entry-icon blue"><el-icon><Operation /></el-icon></div>
              <span>流程管理</span>
            </div>
            <div class="quick-entry-item" @click="goTo('/rpa/tasks')">
              <div class="entry-icon green"><el-icon><Ticket /></el-icon></div>
              <span>任务调度</span>
            </div>
            <div class="quick-entry-item" @click="goTo('/rpa/robots')">
              <div class="entry-icon orange"><el-icon><Monitor /></el-icon></div>
              <span>机器人管理</span>
            </div>
            <div class="quick-entry-item" @click="goTo('/rpa/queues')">
              <div class="entry-icon purple"><el-icon><List /></el-icon></div>
              <span>队列管理</span>
            </div>
            <div class="quick-entry-item" @click="goTo('/rpa/triggers')">
              <div class="entry-icon red"><el-icon><Timer /></el-icon></div>
              <span>触发器</span>
            </div>
            <div class="quick-entry-item" @click="goTo('/rpa/logs')">
              <div class="entry-icon cyan"><el-icon><Document /></el-icon></div>
              <span>执行日志</span>
            </div>
            <div class="quick-entry-item" @click="goTo('/rpa/report')">
              <div class="entry-icon yellow"><el-icon><DataAnalysis /></el-icon></div>
              <span>报表分析</span>
            </div>
            <div class="quick-entry-item" @click="goTo('/rpa/audit')">
              <div class="entry-icon gray"><el-icon><Key /></el-icon></div>
              <span>审计日志</span>
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
import { ElMessage } from 'element-plus'
import {
  Ticket, CircleClose, Monitor, Document, List, TrendCharts, Cpu,
  Clock, Bell, DataAnalysis, Grid, Operation, CircleCheck, Warning, Key
} from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart as PieChartType } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { apiGet } from '../../utils/api.js'

use([CanvasRenderer, LineChart, PieChartType, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()
const executionPeriod = ref('today')

// 统计数据
const stats = reactive({
  pendingTasks: 0,
  failedTasks: 0,
  onlineRobots: 0,
  totalRobots: 0,
  todayExecutions: 0,
  successRate: 0,
  todayDataCollected: 0,
  activeProcesses: 0,
  todayRuntime: '0h',
  pendingAlerts: 0
})

// 机器人统计
const robotStats = reactive({
  idle: 0,
  busy: 0,
  offline: 0
})

// 机器人列表
const robotList = ref([])

// 执行统计
const executionStats = reactive({
  total: 0,
  success: 0,
  failed: 0,
  running: 0
})

// 执行趋势图配置
const executionChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
  },
  yAxis: { type: 'value', name: '次数' },
  series: [
    {
      name: '成功',
      type: 'line',
      data: [5, 0, 0, 12, 25, 30, 15],
      smooth: true,
      areaStyle: { color: 'rgba(103, 194, 58, 0.2)' },
      itemStyle: { color: '#67c23a' }
    },
    {
      name: '失败',
      type: 'line',
      data: [0, 0, 0, 2, 1, 3, 1],
      smooth: true,
      itemStyle: { color: '#f56c6c' }
    }
  ]
}))

// 待办事项
const todoItems = ref([])

// 最近日志
const recentLogs = ref([])

// 跳转
const goTo = (path) => {
  router.push(path)
}

// 加载仪表盘数据
const loadDashboard = async () => {
  try {
    // 加载任务数据
    const taskResult = await apiGet('/task')
    if (taskResult.code === 0) {
      const tasks = taskResult.data || []
      stats.pendingTasks = tasks.filter(t => t.status === 'pending' || t.status === 'assigned').length
      stats.failedTasks = tasks.filter(t => t.status === 'failed').length

      // 生成待办事项
      const pending = tasks.filter(t => t.status === 'pending').slice(0, 3)
      const failed = tasks.filter(t => t.status === 'failed').slice(0, 2)
      todoItems.value = [
        ...pending.map(t => ({
          id: t.id,
          type: 'task',
          title: t.name || '待执行任务',
          description: `关联流程: ${t.processName || '-'}`,
          actionText: '立即执行'
        })),
        ...failed.map(t => ({
          id: t.id,
          type: 'error',
          title: `任务失败: ${t.name || ''}`,
          description: t.errorMessage || '执行异常，请检查',
          actionText: '查看详情'
        }))
      ]
    }

    // 加载机器人数据
    const robotResult = await apiGet('/robot')
    if (robotResult.code === 0) {
      const robots = robotResult.data || []
      stats.totalRobots = robots.length
      stats.onlineRobots = robots.filter(r => r.status !== 'offline').length
      robotStats.idle = robots.filter(r => r.status === 'idle').length
      robotStats.busy = robots.filter(r => r.status === 'busy').length
      robotStats.offline = robots.filter(r => r.status === 'offline').length
      robotList.value = robots.slice(0, 5)
    }

    // 加载执行日志
    const logResult = await apiGet('/log')
    if (logResult.code === 0) {
      const logs = logResult.data || []
      stats.todayExecutions = logs.length
      executionStats.total = logs.length
      executionStats.success = logs.filter(l => l.status === 'success' || l.status === 'completed').length
      executionStats.failed = logs.filter(l => l.status === 'failed').length
      executionStats.running = logs.filter(l => l.status === 'running').length
      stats.successRate = executionStats.total > 0
        ? Math.round((executionStats.success / executionStats.total) * 100)
        : 0

      recentLogs.value = logs.slice(0, 8).map(l => ({
        ...l,
        dataCount: l.dataCount || 0
      }))
    }

    // 加载流程数据
    const processResult = await apiGet('/process')
    if (processResult.code === 0) {
      const processes = processResult.data || []
      stats.activeProcesses = processes.filter(p => p.status === 'active').length
    }

    // 加载队列数据
    const queueResult = await apiGet('/queue')
    if (queueResult.code === 0) {
      const queues = queueResult.data || []
      stats.pendingTasks += queues.reduce((sum, q) => sum + (q.currentPendingCount || 0), 0)
      stats.pendingAlerts = queues.filter(q => q.currentPendingCount > 0).length
    }

    // 加载触发器数据
    const triggerResult = await apiGet('/trigger')
    if (triggerResult.code === 0) {
      const triggers = triggerResult.data || []
      const failedTriggers = triggers.filter(t => t.failedTriggers > 0)
      if (failedTriggers.length > 0 && todoItems.value.length < 8) {
        failedTriggers.slice(0, 2).forEach(t => {
          todoItems.value.push({
            id: t.id,
            type: 'trigger',
            title: `触发器异常: ${t.name}`,
            description: `失败次数: ${t.failedTriggers}`,
            actionText: '查看详情'
          })
        })
      }
    }

  } catch (e) {
    console.error('加载仪表盘数据失败:', e)
  }
}

// 处理待办事项
const handleTodoItem = (item) => {
  switch (item.type) {
    case 'task':
      router.push('/rpa/tasks')
      break
    case 'error':
      router.push('/rpa/logs')
      break
    case 'robot':
      router.push('/rpa/robots')
      break
    default:
      break
  }
}

// 状态映射
const getRobotStatusType = (status) => {
  const map = { idle: 'success', busy: 'warning', offline: 'info' }
  return map[status] || 'info'
}

const getRobotStatusText = (status) => {
  const map = { idle: '空闲', busy: '忙碌', offline: '离线' }
  return map[status] || status
}

const getLogStatusType = (status) => {
  const map = { success: 'success', completed: 'success', failed: 'danger', running: 'warning' }
  return map[status] || 'info'
}

const getLogStatusText = (status) => {
  const map = { success: '成功', completed: '完成', failed: '失败', running: '进行中' }
  return map[status] || status
}

// 定时刷新
let refreshTimer = null

onMounted(() => {
  loadDashboard()
  refreshTimer = setInterval(loadDashboard, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.dashboard-page { max-width: 1600px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stats-row.secondary {
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-icon.primary { background: linear-gradient(135deg, #409eff, #66b1ff); }
.stat-icon.success { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-icon.danger { background: linear-gradient(135deg, #f56c6c, #f78989); }
.stat-icon.info { background: linear-gradient(135deg, #909399, #a6a9ad); }

.stat-content { display: flex; flex-direction: column; flex: 1; }
.stat-value { font-size: 26px; font-weight: bold; color: #1e3a4a; }
.stat-label { font-size: 13px; color: #8c8c8c; margin-top: 2px; }
.stat-trend { position: absolute; top: 12px; right: 12px; font-size: 11px; display: flex; align-items: center; gap: 4px; }
.stat-trend.warning { color: #e6a23c; }
.stat-trend.danger { color: #f56c6c; }
.stat-sub { font-size: 11px; color: #909399; margin-top: 4px; }

/* 小型统计卡片 */
.stat-card-mini {
  background: white;
  padding: 14px 16px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.mini-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: white;
}

.mini-icon.blue { background: #409eff; }
.mini-icon.green { background: #67c23a; }
.mini-icon.orange { background: #e6a23c; }
.mini-icon.purple { background: #909399; }

.mini-content { display: flex; flex-direction: column; }
.mini-value { font-size: 18px; font-weight: bold; color: #1e3a4a; }
.mini-label { font-size: 11px; color: #8c8c8c; }

/* 主内容区 */
.main-content {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: #1e3a4a;
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-body { padding: 12px 16px; }

/* 待办事项 */
.todo-list { display: flex; flex-direction: column; gap: 10px; }
.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.todo-item:hover { background: #f0f0f0; }
.todo-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}
.todo-icon.task { background: #409eff; }
.todo-icon.error { background: #f56c6c; }
.todo-icon.robot { background: #67c23a; }

.todo-content { flex: 1; min-width: 0; }
.todo-title { font-size: 13px; font-weight: 500; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.todo-desc { font-size: 11px; color: #909399; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 执行概览 */
.chart-wrapper { margin-bottom: 12px; }
.execution-summary {
  display: flex;
  justify-content: space-around;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.summary-item { text-align: center; }
.summary-item .label { display: block; font-size: 11px; color: #909399; margin-bottom: 4px; }
.summary-item .value { font-size: 18px; font-weight: bold; color: #303133; }
.summary-item.success .value { color: #67c23a; }
.summary-item.danger .value { color: #f56c6c; }
.summary-item.warning .value { color: #e6a23c; }

/* 机器人状态 */
.robot-summary {
  display: flex;
  justify-content: space-around;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.robot-status-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.status-dot.online { background: #67c23a; }
.status-dot.busy { background: #e6a23c; }
.status-dot.offline { background: #909399; }

.robot-list { display: flex; flex-direction: column; gap: 8px; }
.robot-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  background: #fafafa;
  border-radius: 6px;
}

.robot-info {
  display: flex;
  align-items: center;
  gap: 6px;
}
.robot-icon { color: #409eff; }
.robot-name { font-size: 12px; color: #303133; }

/* 次级内容区 */
.secondary-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
  color: #909399;
  gap: 8px;
}
.empty-state .el-icon { font-size: 32px; }
.empty-state.small { padding: 20px; font-size: 12px; }
.empty-state.small .el-icon { font-size: 24px; }

/* 快捷入口 */
.quick-entry-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick-entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  background: #fafafa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-entry-item:hover {
  background: #f0f0f0;
  transform: translateY(-2px);
}

.quick-entry-item .entry-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: white;
}

.entry-icon.blue { background: #409eff; }
.entry-icon.green { background: #67c23a; }
.entry-icon.orange { background: #e6a23c; }
.entry-icon.purple { background: #9c27b0; }
.entry-icon.red { background: #f56c6c; }
.entry-icon.cyan { background: #00bcd4; }
.entry-icon.yellow { background: #ffc107; }
.entry-icon.gray { background: #607d8b; }

.quick-entry-item span { font-size: 12px; color: #606266; }

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .main-content { grid-template-columns: 1fr; }
  .secondary-content { grid-template-columns: 1fr; }
}
</style>
