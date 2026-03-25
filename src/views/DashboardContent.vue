<template>
  <div class="dashboard-view">
    <!-- 欢迎卡片 -->
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

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card stat-card-primary">
        <div class="stat-header">
          <span class="stat-icon">📋</span>
          <span class="stat-trend up" v-if="statsChange.tasks > 0">+{{ statsChange.tasks }}%</span>
          <span class="stat-trend down" v-else-if="statsChange.tasks < 0">{{ statsChange.tasks }}%</span>
        </div>
        <div class="stat-value">{{ stats.tasks }}</div>
        <div class="stat-label">总任务数</div>
        <div class="stat-chart-mini">
          <div class="mini-bar" v-for="(val, idx) in [40, 60, 45, 70, 55, 80, 65]" :key="idx" :style="{ height: val + '%' }"></div>
        </div>
      </div>
      <div class="stat-card stat-card-info">
        <div class="stat-header">
          <span class="stat-icon">🤖</span>
          <span class="stat-trend up" v-if="statsChange.robots > 0">+{{ statsChange.robots }}%</span>
        </div>
        <div class="stat-value">{{ stats.robots }}</div>
        <div class="stat-label">机器人总数</div>
        <div class="stat-chart-mini">
          <div class="mini-bar" v-for="(val, idx) in [50, 70, 60, 80, 75, 90, 85]" :key="idx" :style="{ height: val + '%' }"></div>
        </div>
      </div>
      <div class="stat-card stat-card-success">
        <div class="stat-header">
          <span class="stat-icon">🔧</span>
          <span class="stat-trend up" v-if="statsChange.processes > 0">+{{ statsChange.processes }}%</span>
        </div>
        <div class="stat-value">{{ stats.processes }}</div>
        <div class="stat-label">流程总数</div>
        <div class="stat-chart-mini">
          <div class="mini-bar" v-for="(val, idx) in [60, 50, 75, 65, 85, 70, 90]" :key="idx" :style="{ height: val + '%' }"></div>
        </div>
      </div>
      <div class="stat-card stat-card-warning">
        <div class="stat-header">
          <span class="stat-icon">📝</span>
          <span class="stat-trend" v-if="statsChange.logs > 0">+{{ statsChange.logs }}%</span>
        </div>
        <div class="stat-value">{{ stats.logs }}</div>
        <div class="stat-label">日志总数</div>
        <div class="stat-chart-mini">
          <div class="mini-bar" v-for="(val, idx) in [45, 65, 55, 75, 60, 85, 70]" :key="idx" :style="{ height: val + '%' }"></div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <div class="chart-card chart-line">
        <div class="chart-header">
          <h3>任务执行趋势</h3>
          <div class="chart-actions">
            <span 
              v-for="period in ['近7天', '近30天', '近90天']" 
              :key="period"
              class="period-btn"
              :class="{ active: selectedPeriod === period }"
              @click="selectedPeriod = period"
            >{{ period }}</span>
          </div>
        </div>
        <div class="chart-body">
          <v-chart :option="lineChartOption" autoresize style="height: 300px;"></v-chart>
        </div>
      </div>
      <div class="chart-card chart-bar">
        <div class="chart-header">
          <h3>任务状态分布</h3>
        </div>
        <div class="chart-body">
          <v-chart :option="barChartOption" autoresize style="height: 300px;"></v-chart>
        </div>
      </div>
    </div>

    <!-- 任务状态概览 -->
    <div class="status-overview">
      <div class="status-card">
        <div class="status-header running">
          <span class="status-dot"></span>
          <span>运行中</span>
        </div>
        <div class="status-value">{{ taskStatus.running }}</div>
      </div>
      <div class="status-card">
        <div class="status-header pending">
          <span class="status-dot"></span>
          <span>待执行</span>
        </div>
        <div class="status-value">{{ taskStatus.pending }}</div>
      </div>
      <div class="status-card">
        <div class="status-header completed">
          <span class="status-dot"></span>
          <span>已完成</span>
        </div>
        <div class="status-value">{{ taskStatus.completed }}</div>
      </div>
      <div class="status-card">
        <div class="status-header failed">
          <span class="status-dot"></span>
          <span>失败</span>
        </div>
        <div class="status-value">{{ taskStatus.failed }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

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

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
})

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

onMounted(() => {
  loadUserFromStorage()
  loadStats()
})
</script>

<style scoped>
.dashboard-view {
  max-width: 1400px;
  margin: 0 auto;
}

.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 28px 32px;
  border-radius: 20px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
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

.stat-value {
  font-size: 32px;
  font-weight: bold;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-label {
  font-size: 14px;
  opacity: 0.8;
}

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
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.stat-icon {
  font-size: 24px;
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

.status-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .charts-section {
    grid-template-columns: 1fr;
  }
  
  .status-overview {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
