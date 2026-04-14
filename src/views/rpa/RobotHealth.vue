<template>
  <div class="health-page">
    <div class="page-header">
      <h2>机器人健康监控</h2>
      <p class="page-desc">实时监控机器人运行状态和资源使用情况</p>
    </div>

    <!-- 统计概览 -->
    <div class="stats-row">
      <div class="stat-card" @click="filterByStatus('healthy')">
        <div class="stat-icon healthy"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.healthy }}</span>
          <span class="stat-label">健康</span>
        </div>
      </div>
      <div class="stat-card" @click="filterByStatus('warning')">
        <div class="stat-icon warning"><el-icon><Warning /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.warning }}</span>
          <span class="stat-label">警告</span>
        </div>
      </div>
      <div class="stat-card" @click="filterByStatus('critical')">
        <div class="stat-icon critical"><el-icon><CloseBold /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.critical }}</span>
          <span class="stat-label">危险</span>
        </div>
      </div>
      <div class="stat-card" @click="filterByStatus('offline')">
        <div class="stat-icon offline"><el-icon><Switch /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.offline }}</span>
          <span class="stat-label">离线</span>
        </div>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索机器人名称..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="健康" value="healthy" />
        <el-option label="警告" value="warning" />
        <el-option label="危险" value="critical" />
        <el-option label="离线" value="offline" />
      </el-select>
      <el-select v-model="categoryFilter" placeholder="分类筛选" clearable style="width: 140px;">
        <el-option label="数据采集" value="DATA_COLLECT" />
        <el-option label="数据解析" value="DATA_PARSE" />
        <el-option label="数据加工" value="DATA_PROCESS" />
        <el-option label="通用执行" value="GENERAL" />
      </el-select>
      <el-button @click="loadRobotHealth">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>

    <!-- 机器人健康列表 -->
    <div class="health-list">
      <div v-for="robot in filteredRobots" :key="robot.id" class="health-card">
        <div class="health-header">
          <div class="health-title">
            <span class="robot-name">{{ robot.name }}</span>
            <el-tag :type="getStatusTagType(robot.healthStatus)" size="small">
              {{ getStatusText(robot.healthStatus) }}
            </el-tag>
          </div>
          <div class="health-score" :class="getScoreClass(robot.healthScore)">
            <span class="score-value">{{ robot.healthScore }}</span>
            <span class="score-label">分</span>
          </div>
        </div>

        <div class="health-metrics">
          <div class="metric-item">
            <div class="metric-label">CPU</div>
            <div class="metric-bar">
              <div class="metric-fill" :style="{ width: robot.cpuUsage + '%', background: getMetricColor(robot.cpuUsage) }"></div>
            </div>
            <div class="metric-value" :class="getMetricClass(robot.cpuUsage)">{{ robot.cpuUsage }}%</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">内存</div>
            <div class="metric-bar">
              <div class="metric-fill" :style="{ width: robot.memoryUsage + '%', background: getMetricColor(robot.memoryUsage) }"></div>
            </div>
            <div class="metric-value" :class="getMetricClass(robot.memoryUsage)">{{ robot.memoryUsage }}%</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">磁盘</div>
            <div class="metric-bar">
              <div class="metric-fill" :style="{ width: robot.diskUsage + '%', background: getMetricColor(robot.diskUsage) }"></div>
            </div>
            <div class="metric-value" :class="getMetricClass(robot.diskUsage)">{{ robot.diskUsage }}%</div>
          </div>
        </div>

        <div class="health-footer">
          <div class="health-info">
            <span class="info-item">分类: {{ getCategoryText(robot.robotCategory) }}</span>
            <span class="info-item">状态: {{ robot.status === 'idle' ? '空闲' : robot.status === 'busy' ? '忙碌' : '离线' }}</span>
          </div>
          <div class="health-actions">
            <el-button link type="primary" size="small" @click="viewDetail(robot)">详情</el-button>
            <el-button link type="primary" size="small" @click="viewHistory(robot)">历史</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`机器人详情 - ${currentRobot?.name}`" width="900px">
      <div v-if="currentRobot" class="detail-content">
        <el-tabs>
          <el-tab-pane label="基本信息">
            <div class="detail-grid">
              <div class="detail-item"><label>机器人名称:</label><span>{{ currentRobot.name }}</span></div>
              <div class="detail-item"><label>机器人分类:</label><span>{{ getCategoryText(currentRobot.robotCategory) }}</span></div>
              <div class="detail-item"><label>运行状态:</label><span>{{ currentRobot.status === 'idle' ? '空闲' : currentRobot.status === 'busy' ? '忙碌' : '离线' }}</span></div>
              <div class="detail-item"><label>健康状态:</label><span>{{ getStatusText(currentRobot.healthStatus) }}</span></div>
              <div class="detail-item"><label>最后心跳:</label><span>{{ currentRobot.lastHeartbeat }}</span></div>
              <div class="detail-item"><label>心跳超时:</label><span>{{ currentRobot.heartbeatTimeout ? '是' : '否' }}</span></div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="资源使用">
            <div class="metrics-grid">
              <div class="metric-card">
                <div class="metric-icon cpu"><el-icon><Cpu /></el-icon></div>
                <div class="metric-info">
                  <span class="metric-value">{{ currentRobot.cpuUsage }}%</span>
                  <span class="metric-label">CPU使用率</span>
                </div>
              </div>
              <div class="metric-card">
                <div class="metric-icon memory"><el-icon><Monitor /></el-icon></div>
                <div class="metric-info">
                  <span class="metric-value">{{ currentRobot.memoryUsage }}%</span>
                  <span class="metric-label">内存使用率</span>
                </div>
              </div>
              <div class="metric-card">
                <div class="metric-icon disk"><el-icon><Folder /></el-icon></div>
                <div class="metric-info">
                  <span class="metric-value">{{ currentRobot.diskUsage }}%</span>
                  <span class="metric-label">磁盘使用率</span>
                </div>
              </div>
              <div class="metric-card">
                <div class="metric-icon network"><el-icon><Connection /></el-icon></div>
                <div class="metric-info">
                  <span class="metric-value">{{ currentRobot.networkLatency || 0 }}ms</span>
                  <span class="metric-label">网络延迟</span>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="告警信息" v-if="currentRobot.warningItems?.length || currentRobot.criticalItems?.length">
            <div class="alert-list">
              <el-alert v-for="(item, idx) in currentRobot.warningItems" :key="'w'+idx" :title="item" type="warning" :closable="false" show-icon />
              <el-alert v-for="(item, idx) in currentRobot.criticalItems" :key="'c'+idx" :title="item" type="error" :closable="false" show-icon />
            </div>
            <div class="recommendation" v-if="currentRobot.recommendation">
              <h4>处理建议</h4>
              <p>{{ currentRobot.recommendation }}</p>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, CircleCheck, Warning, CloseBold, Switch, Cpu, Monitor, Folder, Connection } from '@element-plus/icons-vue'
import { apiGet } from '../../utils/api.js'

const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const categoryFilter = ref('')
const robots = ref([])
const detailVisible = ref(false)
const currentRobot = ref(null)

const stats = reactive({ healthy: 0, warning: 0, critical: 0, offline: 0 })

const filteredRobots = computed(() => {
  let data = robots.value
  if (statusFilter.value) data = data.filter(r => r.healthStatus === statusFilter.value)
  if (categoryFilter.value) data = data.filter(r => r.robotCategory === categoryFilter.value)
  if (searchKeyword.value) data = data.filter(r => r.name.includes(searchKeyword.value))
  return data
})

const getStatusTagType = (status) => {
  const map = { healthy: 'success', warning: 'warning', critical: 'danger', offline: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { healthy: '健康', warning: '警告', critical: '危险', offline: '离线' }
  return map[status] || '未知'
}

const getCategoryText = (cat) => {
  const map = { DATA_COLLECT: '数据采集', DATA_PARSE: '数据解析', DATA_PROCESS: '数据加工', GENERAL: '通用执行' }
  return map[cat] || cat
}

const getScoreClass = (score) => {
  if (score >= 80) return 'score-high'
  if (score >= 60) return 'score-mid'
  return 'score-low'
}

const getMetricColor = (value) => {
  if (value >= 90) return '#f56c6c'
  if (value >= 70) return '#e6a23c'
  return '#67c23a'
}

const getMetricClass = (value) => {
  if (value >= 90) return 'metric-high'
  if (value >= 70) return 'metric-mid'
  return ''
}

const filterByStatus = (status) => {
  statusFilter.value = statusFilter.value === status ? '' : status
}

const loadRobotHealth = async () => {
  loading.value = true
  try {
    const result = await apiGet('/robot/health')
    if (result.code === 0) {
      robots.value = result.data || []
      updateStats()
    }
  } catch (e) {
    console.error('加载失败:', e)
    robots.value = [
      { id: 1, name: 'Robot-Collector-01', robotCategory: 'DATA_COLLECT', status: 'idle', healthScore: 95, cpuUsage: 45, memoryUsage: 62, diskUsage: 30, healthStatus: 'healthy', lastHeartbeat: '2026-04-07 10:30:00', heartbeatTimeout: false },
      { id: 2, name: 'Robot-Parser-01', robotCategory: 'DATA_PARSE', status: 'busy', healthScore: 72, cpuUsage: 88, memoryUsage: 85, diskUsage: 45, healthStatus: 'warning', lastHeartbeat: '2026-04-07 10:29:00', heartbeatTimeout: false, warningItems: ['CPU使用率偏高', '内存使用率偏高'] },
      { id: 3, name: 'Robot-Processor-01', robotCategory: 'DATA_PROCESS', status: 'idle', healthScore: 45, cpuUsage: 95, memoryUsage: 92, diskUsage: 78, healthStatus: 'critical', lastHeartbeat: '2026-04-07 10:25:00', heartbeatTimeout: true, criticalItems: ['CPU过载', '内存即将耗尽'], recommendation: '建议立即扩容或清理资源' },
      { id: 4, name: 'Robot-General-01', robotCategory: 'GENERAL', status: 'offline', healthScore: 0, cpuUsage: 0, memoryUsage: 0, diskUsage: 0, healthStatus: 'offline', lastHeartbeat: '2026-04-07 08:00:00', heartbeatTimeout: true },
    ]
    updateStats()
  } finally {
    loading.value = false
  }
}

const updateStats = () => {
  stats.healthy = robots.value.filter(r => r.healthStatus === 'healthy').length
  stats.warning = robots.value.filter(r => r.healthStatus === 'warning').length
  stats.critical = robots.value.filter(r => r.healthStatus === 'critical').length
  stats.offline = robots.value.filter(r => r.healthStatus === 'offline').length
}

const viewDetail = (robot) => { currentRobot.value = robot; detailVisible.value = true }
const viewHistory = (robot) => { ElMessage.info(`查看 ${robot.name} 的历史记录`) }

onMounted(() => { loadRobotHealth() })
</script>

<style scoped>
.health-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: white; padding: 20px; border-radius: 12px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); cursor: pointer; transition: all 0.2s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 28px; color: white; }
.stat-icon.healthy { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-icon.warning { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.stat-icon.critical { background: linear-gradient(135deg, #f56c6c, #f78989); }
.stat-icon.offline { background: linear-gradient(135deg, #909399, #a6a9ad); }
.stat-value { font-size: 28px; font-weight: bold; color: #1e3a4a; }
.stat-label { font-size: 13px; color: #8c8c8c; }

.toolbar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; background: white; padding: 8px 12px; border-radius: 6px; border: 1px solid #dcdfe6; }
.search-box input { border: none; outline: none; width: 200px; }

.health-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(400px, 1fr)); gap: 16px; }
.health-card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.health-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.health-title { display: flex; align-items: center; gap: 10px; }
.robot-name { font-size: 16px; font-weight: 600; color: #1e3a4a; }
.health-score { display: flex; align-items: baseline; gap: 2px; }
.score-value { font-size: 32px; font-weight: bold; }
.score-label { font-size: 14px; color: #8c8c8c; }
.score-high .score-value { color: #67c23a; }
.score-mid .score-value { color: #e6a23c; }
.score-low .score-value { color: #f56c6c; }

.health-metrics { margin-bottom: 16px; }
.metric-item { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.metric-label { width: 40px; font-size: 12px; color: #8c8c8c; }
.metric-bar { flex: 1; height: 8px; background: #f0f2f5; border-radius: 4px; overflow: hidden; }
.metric-fill { height: 100%; border-radius: 4px; transition: width 0.3s; }
.metric-value { width: 45px; text-align: right; font-size: 12px; }
.metric-high { color: #f56c6c; font-weight: 600; }
.metric-mid { color: #e6a23c; }

.health-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 12px; border-top: 1px solid #ebeef5; }
.health-info { display: flex; gap: 16px; }
.info-item { font-size: 12px; color: #8c8c8c; }
.health-actions { display: flex; gap: 8px; }

.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.detail-item { display: flex; gap: 8px; }
.detail-item label { color: #8c8c8c; }
.detail-item span { color: #1e3a4a; }

.metrics-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.metric-card { display: flex; align-items: center; gap: 16px; padding: 20px; background: #f5f7fa; border-radius: 8px; }
.metric-icon { width: 48px; height: 48px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: white; }
.metric-icon.cpu { background: linear-gradient(135deg, #409eff, #66b1ff); }
.metric-icon.memory { background: linear-gradient(135deg, #67c23a, #85ce61); }
.metric-icon.disk { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.metric-icon.network { background: linear-gradient(135deg, #909399, #a6a9ad); }
.metric-info { display: flex; flex-direction: column; }
.metric-info .metric-value { font-size: 24px; font-weight: bold; color: #1e3a4a; }
.metric-info .metric-label { font-size: 12px; color: #8c8c8c; }

.alert-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px; }
.recommendation { background: #fdf6ec; padding: 16px; border-radius: 8px; }
.recommendation h4 { margin: 0 0 8px; color: #e6a23c; }
.recommendation p { margin: 0; color: #1e3a4a; }
</style>
