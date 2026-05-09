<template>
  <div class="process-detail-page">
    <!-- 顶部导航栏 -->
    <div class="detail-nav">
      <div class="nav-left">
        <el-button @click="goBack" text class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          {{ t('process.backToList') }}
        </el-button>
        <el-divider direction="vertical" />
        <span class="nav-title">{{ t('process.detail') }}</span>
      </div>
      <div class="nav-actions">
        <el-button @click="openDesigner" type="primary">
          <el-icon><Edit /></el-icon>
          {{ t('process.design') }}
        </el-button>
        <el-button @click="handleExecute" type="success" :loading="executing">
          <el-icon><VideoPlay /></el-icon>
          {{ t('process.execute') }}
        </el-button>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="detail-container">
      <!-- 左侧信息卡片 -->
      <div class="left-panel">
        <!-- 基本信息卡片 -->
        <div class="info-card">
          <div class="card-header">
            <div class="card-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="card-title">
              <h3>{{ t('process.basicInfo') }}</h3>
              <p>{{ t('process.basicInfoDesc') }}</p>
            </div>
            <el-tag :type="process.status === 'active' ? 'success' : 'info'" effect="dark">
              {{ process.status === 'active' ? t('process.published') : t('process.draft') }}
            </el-tag>
          </div>
          <div class="card-body">
            <div class="info-item">
              <label>{{ t('process.name') }}</label>
              <div class="info-value primary">{{ process.name }}</div>
            </div>
            <div class="info-item">
              <label>{{ t('process.code') }}</label>
              <div class="info-value code">{{ process.code }}</div>
            </div>
            <div class="info-item">
              <label>{{ t('process.versionNumber') }}</label>
              <div class="info-value">
                <el-tag size="small" effect="plain">v{{ process.version }}</el-tag>
              </div>
            </div>
            <div class="info-item">
              <label>{{ t('process.creator') }}</label>
              <div class="info-value">{{ process.creatorName || '-' }}</div>
            </div>
            <div class="info-item">
              <label>{{ t('process.createTime') }}</label>
              <div class="info-value">{{ process.createTime || '-' }}</div>
            </div>
            <div class="info-item full">
              <label>{{ t('process.description') }}</label>
              <div class="info-value desc">{{ process.description || t('process.noDescription') }}</div>
            </div>
          </div>
        </div>

        <!-- 流程步骤卡片 -->
        <div class="steps-card">
          <div class="card-header">
            <div class="card-icon purple">
              <el-icon><List /></el-icon>
            </div>
            <div class="card-title">
              <h3>{{ t('process.processSteps') }}</h3>
              <p>{{ t('process.stepsCount', { count: steps.length }) }}</p>
            </div>
            <el-button size="small" @click="refreshSteps" text>
              <el-icon><Refresh /></el-icon>
              {{ t('common.refresh') }}
            </el-button>
          </div>
          <div class="card-body">
            <div v-if="steps.length > 0" class="steps-list">
              <div v-for="(step, index) in steps" :key="step.id" class="step-item" :class="{ 'has-robot': step.robotId }">
                <div class="step-number">{{ index + 1 }}</div>
                <div class="step-content">
                  <div class="step-header">
                    <span class="step-name">{{ step.name || t('process.noNamedStep') }}</span>
                    <el-tag v-if="step.type" :type="getStepTypeTag(step.type)" size="small" effect="light">
                      {{ getStepTypeLabel(step.type) }}
                    </el-tag>
                  </div>
                  <div v-if="step.description" class="step-desc">{{ step.description }}</div>
                  <div class="step-robot">
                    <template v-if="step.robotId">
                      <el-tag :type="getRobotStatus(step.robotId).type" size="small">
                        <el-icon><Monitor /></el-icon>
                        {{ getRobotStatus(step.robotId).name }}
                      </el-tag>
                      <span class="robot-status">{{ getRobotStatus(step.robotId).statusText }}</span>
                    </template>
                    <template v-else>
                      <el-tag type="info" size="small" effect="plain">
                        <el-icon><Warning /></el-icon>
                        {{ t('process.unboundRobot') }}
                      </el-tag>
                    </template>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-steps">
              <el-empty :description="t('process.noSteps')" :image-size="60">
                <el-button type="primary" size="small" @click="openDesigner">{{ t('process.goDesign') }}</el-button>
              </el-empty>
            </div>
          </div>
        </div>

        <!-- 执行结果数据卡片 -->
        <div class="result-card" v-if="latestExecutionResult">
          <div class="card-header">
            <div class="card-icon blue">
              <el-icon><Tickets /></el-icon>
            </div>
            <div class="card-title">
              <h3>{{ t('process.executionResultData') }}</h3>
              <p>{{ t('process.executionResultDataDesc') }}</p>
            </div>
            <el-tag :type="latestExecutionResult.status === 'completed' ? 'success' : 'warning'" effect="dark">
              {{ latestExecutionResult.status === 'completed' ? t('task.statusCompleted') : t('task.statusRunning') }}
            </el-tag>
          </div>
          <div class="card-body">
            <div v-if="latestExecutionResult.steps && latestExecutionResult.steps.length > 0" class="result-list">
              <div v-for="(stepResult, index) in latestExecutionResult.steps" :key="index" 
                   class="result-item" :class="stepResult.status">
                <div class="result-icon">
                  <el-icon v-if="stepResult.status === 'success'" color="#67c23a"><Check /></el-icon>
                  <el-icon v-else color="#f56c6c"><Close /></el-icon>
                </div>
                <div class="result-content">
                  <div class="result-header">
                    <span class="result-step">{{ stepResult.stepIndex }}. {{ stepResult.stepName || t('process.step') + stepResult.stepIndex }}</span>
                    <el-tag v-if="stepResult.stepType" size="small" :type="getStepTypeTag(stepResult.stepType)">
                      {{ getStepTypeLabel(stepResult.stepType) }}
                    </el-tag>
                  </div>
                  <div class="result-details">
                    <div class="result-robot">
                      <el-icon><Monitor /></el-icon>
                      {{ stepResult.robotName || t('process.unknownRobot') }}
                    </div>
                    <div v-if="stepResult.result" class="result-info">
                      <span v-if="stepResult.result.dataCount" class="result-count">
                        {{ t('process.processedData') }}：{{ stepResult.result.dataCount }} {{ t('process.items') }}
                      </span>
                      <span v-if="stepResult.result.message" class="result-message">
                        {{ stepResult.result.message }}
                      </span>
                    </div>
                    <div v-if="stepResult.error" class="result-error">
                      {{ stepResult.error }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-result">
              <el-empty :description="t('process.noExecutionResult')" :image-size="60" />
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧执行日志卡片 -->
      <div class="right-panel">
        <!-- 执行进度卡片 -->
        <div class="progress-card" v-if="executing || executionProgress.length > 0 || showVisualizer">
          <div class="card-header">
            <div class="card-icon orange">
              <el-icon><VideoPlay /></el-icon>
            </div>
            <div class="card-title">
              <h3>{{ t('process.executionProgress') }}</h3>
              <p>{{ executing ? t('process.executing') : t('process.lastExecution') }}</p>
            </div>
            <div class="progress-actions">
              <el-button 
                size="small" 
                :type="showVisualizer ? 'primary' : 'default'" 
                @click="showVisualizer = !showVisualizer"
                :disabled="!executing && executionProgress.length === 0"
              >
                <el-icon><Monitor /></el-icon>
                {{ showVisualizer ? t('process.hideMonitor') : t('process.visualization') }}
              </el-button>
              <el-button size="small" @click="clearProgress" text v-if="!executing">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          
          <!-- 可视化监控面板 -->
          <div v-if="showVisualizer" class="visualizer-panel">
            <div class="visualizer-header">
              <span class="viz-title">{{ t('process.realtimeMonitor') }}</span>
              <div class="viz-status">
                <span class="status-indicator" :class="{ active: executing }"></span>
                <span>{{ executing ? t('process.running') : t('process.stopped') }}</span>
              </div>
            </div>
            <div class="visualizer-body">
              <div class="viz-robot-info" v-if="currentExecutingRobot">
                <el-icon size="20"><Monitor /></el-icon>
                <span>{{ currentExecutingRobot }}</span>
              </div>
              <div class="viz-steps-flow">
                <div 
                  v-for="(step, index) in steps" 
                  :key="step.id"
                  class="viz-step"
                  :class="{ 
                    'completed': index < currentStepIndex,
                    'active': index === currentStepIndex && executing,
                    'pending': index > currentStepIndex
                  }"
                >
                  <div class="viz-step-dot">
                    <el-icon v-if="index < currentStepIndex"><Check /></el-icon>
                    <span v-else>{{ index + 1 }}</span>
                  </div>
                  <div class="viz-step-name">{{ step.name || t('process.step') + (index + 1) }}</div>
                </div>
              </div>
              <div class="viz-console">
                <div class="console-header">
                  <span>{{ t('process.consoleOutput') }}</span>
                  <span class="console-blink" v-if="executing">▌</span>
                </div>
                <div class="console-body" ref="consoleContainer">
                  <div 
                    v-for="(log, index) in realtimeLogs" 
                    :key="index"
                    class="console-line"
                    :class="log.type"
                  >
                    <span class="console-time">{{ log.time }}</span>
                    <span class="console-text">{{ log.message }}</span>
                  </div>
                  <div v-if="realtimeLogs.length === 0" class="console-empty">
                    {{ t('process.waitingRobotOutput') }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="progress-body" v-if="!showVisualizer">
            <div v-if="executionProgress.length > 0" class="progress-list">
              <div v-for="(item, index) in executionProgress" :key="index" 
                   class="progress-item" :class="item.type">
                <div class="progress-dot" :class="{ 'pulse': item.type === 'info' && executing }"></div>
                <div class="progress-content">
                  <div class="progress-message">{{ item.message }}</div>
                  <div class="progress-time">{{ item.time }}</div>
                </div>
              </div>
            </div>
            <div v-else class="empty-progress">
              <el-empty :description="t('process.noExecutionProgress')" :image-size="40" />
            </div>
          </div>
        </div>

        <!-- 执行日志卡片 -->
        <div class="log-card">
          <div class="card-header">
            <div class="card-icon green">
              <el-icon><Tickets /></el-icon>
            </div>
            <div class="card-title">
              <h3>{{ t('process.executionLog') }}</h3>
              <p>{{ t('process.executionLogDesc') }}</p>
            </div>
            <div class="log-actions">
              <el-button size="small" @click="loadLogs" text>
                <el-icon><Refresh /></el-icon>
                {{ t('common.refresh') }}
              </el-button>
            </div>
          </div>
          <div class="log-body" ref="logContainer">
            <div v-if="executeLogs.length === 0" class="empty-logs">
              <div class="empty-icon">
                <el-icon><Document /></el-icon>
              </div>
              <p>{{ t('process.noExecutionLog') }}</p>
              <span>{{ t('process.clickExecuteToStart') }}</span>
            </div>
            <div v-else class="log-timeline">
              <div v-for="(log, index) in executeLogs" :key="index" class="log-item" :class="getLogType(log.status)">
                <div class="log-dot"></div>
                <div class="log-content">
                  <div class="log-header">
                    <span class="log-time">{{ formatTime(log.createTime) }}</span>
                    <el-tag size="small" :type="getStatusTagType(log.status)" effect="plain">
                      {{ getStatusText(log.status) }}
                    </el-tag>
                  </div>
                  <div class="log-message">{{ log.message || log.action || t('process.executionCompleted') }}</div>
                  <div v-if="log.duration" class="log-duration">{{ t('process.duration') }}: {{ log.duration }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 统计信息卡片 -->
        <div class="stats-card">
          <div class="card-header">
            <div class="card-icon cyan">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="card-title">
              <h3>{{ t('process.executionStats') }}</h3>
              <p>{{ t('process.executionStatsDesc') }}</p>
            </div>
          </div>
          <div class="stats-body">
            <div class="stat-item">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">{{ t('process.totalExecutions') }}</div>
            </div>
            <div class="stat-item success">
              <div class="stat-value">{{ stats.completed }}</div>
              <div class="stat-label">{{ t('task.statusCompleted') }}</div>
            </div>
            <div class="stat-item danger">
              <div class="stat-value">{{ stats.failed }}</div>
              <div class="stat-label">{{ t('task.statusFailed') }}</div>
            </div>
            <div class="stat-item warning">
              <div class="stat-value">{{ stats.running }}</div>
              <div class="stat-label">{{ t('task.statusRunning') }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 流程设计器弹窗 - 画布模式 -->
    <el-dialog v-model="designerVisible" :title="`${t('process.designTitle')} - ${process.name || ''}`" width="90%" top="5vh" class="process-designer-dialog-canvas" :close-on-click-modal="false">
      <div class="canvas-designer-wrapper">
        <CanvasDesigner
          ref="canvasDesignerRef"
          v-model="canvasData"
          :robots="robots"
          :robot-categories="robotCategories"
          :process-id="process.id"
          @save="saveDesign"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft, Document, List, Monitor, Warning, Edit, VideoPlay,
  Check, Delete, Rank, Plus, Setting, Tickets, Refresh, DataAnalysis,
  Operation, Close
} from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import CanvasDesigner from './CanvasDesigner.vue'
import { apiGet, apiPost } from '../../utils/api.js'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const process = ref({})
const steps = ref([])
const canvasData = ref({ nodes: [], edges: [] })
const canvasDesignerRef = ref(null)
const robots = ref([])
const robotCategories = ref([])
const executeLogs = ref([])
const executionProgress = ref([])
const latestExecutionResult = ref(null)
const stats = reactive({ total: 0, completed: 0, failed: 0, running: 0 })
const designerVisible = ref(false)
const savingDesign = ref(false)
const executing = ref(false)
const logContainer = ref(null)
const consoleContainer = ref(null)
const showVisualizer = ref(false)
const realtimeLogs = ref([])
const currentExecutingRobot = ref('')
const currentStepIndex = ref(-1)
let pollInterval = null

// 加载流程详情
const loadProcess = async () => {
  try {
    const id = route.params.id
    const result = await apiGet(`/process/${id}`)
    if (result.code === 0) {
      process.value = result.data || {}
      await loadSteps()
    }
  } catch {
    ElMessage.error(t('process.loadFailed'))
  }
}

// 加载步骤
const loadSteps = async () => {
  try {
    const result = await apiGet(`/process/${process.value.id}/design`)
    if (result.code === 0 && result.data) {
      try {
        const parsed = JSON.parse(result.data)
        // 判断是新的画布格式还是旧的步骤格式
        if (parsed.nodes && parsed.edges) {
          // 新格式
          steps.value = convertCanvasToOldSteps(parsed)
        } else {
          // 旧格式
          steps.value = parsed
        }
      } catch {
        steps.value = []
      }
    } else {
      steps.value = []
    }
  } catch {
    steps.value = []
  }
}

// 将画布格式转换为旧步骤格式（用于执行）
const convertCanvasToOldSteps = (canvasData) => {
  const { nodes, edges } = canvasData
  
  // 只保留流程节点，按连接顺序排序
  const processNodes = nodes.filter(n => n.type === 'process')
  
  // 简单的拓扑排序，按照边的连接顺序
  const sorted = []
  const visited = new Set()
  
  // 找到起始节点（没有被其他节点指向的节点）
  const targetIds = new Set(edges.map(e => e.target))
  const startNodes = processNodes.filter(n => !targetIds.has(n.id))
  
  // BFS遍历
  const queue = startNodes.length > 0 ? startNodes : [processNodes[0]]
  
  while (queue.length > 0) {
    const node = queue.shift()
    if (visited.has(node.id)) continue
    
    visited.add(node.id)
    sorted.push(node)
    
    // 找到下一个节点
    const outgoingEdges = edges.filter(e => e.source === node.id)
    outgoingEdges.forEach(edge => {
      const nextNode = processNodes.find(n => n.id === edge.target)
      if (nextNode && !visited.has(nextNode.id)) {
        queue.push(nextNode)
      }
    })
  }
  
  // 添加未访问的节点
  processNodes.forEach(node => {
    if (!visited.has(node.id)) {
      sorted.push(node)
    }
  })
  
  return sorted
}

// 加载机器人列表
const loadRobots = async () => {
  try {
    const result = await apiGet('/robot')
    if (result.code === 0) {
      robots.value = result.data || []
    }
  } catch {
    robots.value = []
  }
}

// 加载机器人分类列表
const loadRobotCategories = async () => {
  try {
    const result = await apiGet('/robot/category/list')
    if (result.code === 0) {
      robotCategories.value = result.data || []
    }
  } catch {
    robotCategories.value = []
  }
}

// 加载执行日志
const loadLogs = async () => {
  try {
    const result = await apiGet(`/log?processId=${process.value.id}`)
    if (result.code === 0) {
      const logs = result.data || []
      executeLogs.value = logs
      // 统计
      stats.total = logs.length
      stats.completed = logs.filter(l => l.status === 'completed').length
      stats.failed = logs.filter(l => l.status === 'failed' || l.status === 'completed_with_errors').length
      stats.running = logs.filter(l => l.status === 'running').length
      
      // 加载最新一次执行的详细结果
      if (logs.length > 0) {
        const latestLog = logs[0]
        if (latestLog.resultData) {
          try {
            latestExecutionResult.value = {
              status: latestLog.status,
              steps: JSON.parse(latestLog.resultData)
            }
          } catch {
            latestExecutionResult.value = null
          }
        }
      }
    }
  } catch {
    executeLogs.value = []
  }
}

// 获取机器人状态
const getRobotStatus = (robotId) => {
  const robot = robots.value.find(r => r.id === robotId)
  if (!robot) return { name: '', statusText: '', type: 'info' }
  const statusMap = {
    idle: { text: t('robot.idle'), type: 'success' },
    busy: { text: t('robot.busy'), type: 'warning' },
    offline: { text: t('robot.offline'), type: 'info' }
  }
  const status = statusMap[robot.status] || { text: robot.status, type: 'info' }
  return { name: robot.name, statusText: status.text, type: status.type }
}

// 步骤类型标签映射
const stepTypeMap = {
  collect: { label: t('process.stepTypeCollect'), tag: 'primary' },
  parse: { label: t('process.stepTypeParse'), tag: 'success' },
  process: { label: t('process.stepTypeProcess'), tag: 'warning' },
  query: { label: t('process.stepTypeQuery'), tag: 'info' },
  transform: { label: t('process.stepTypeTransform'), tag: '' },
  output: { label: t('process.stepTypeOutput'), tag: 'danger' },
  validate: { label: t('process.stepTypeValidate'), tag: '' },
  default: { label: t('process.stepTypeDefault'), tag: 'info' }
}

// 获取步骤类型标签
const getStepTypeLabel = (type) => {
  return stepTypeMap[type]?.label || stepTypeMap.default.label
}

// 获取步骤类型标签颜色
const getStepTypeTag = (type) => {
  return stepTypeMap[type]?.tag || 'info'
}

// 刷新步骤数据
const refreshSteps = async () => {
  await loadSteps()
  ElMessage.success(t('process.refreshed'))
}

// 真实执行流程
const handleExecute = async () => {
  if (steps.value.length === 0) {
    ElMessage.warning(t('process.pleaseDesignFirst'))
    return
  }

  const unboundSteps = steps.value.filter(s => !s.robotId)
  if (unboundSteps.length > 0) {
    ElMessage.warning(`有 ${unboundSteps.length} 个步骤未绑定机器人，请先完善流程设计`)
    return
  }

  executing.value = true
  executionProgress.value = []
  realtimeLogs.value = []
  latestExecutionResult.value = null
  currentStepIndex.value = -1
  currentExecutingRobot.value = ''

  // 添加开始日志
  addProgressLog('info', `开始执行流程: ${process.value.name}`)
  addRealtimeLog(`=== 开始执行流程: ${process.value.name} ===`, 'info')

  try {
    const result = await apiPost(`/process/${process.value.id}/execute`)

    if (result.code === 0) {
      addProgressLog('success', `执行请求已提交，ID: ${result.data.executionId}`)
      addRealtimeLog(`执行ID: ${result.data.executionId}`, 'success')
      
      // 开始轮询执行状态
      startPolling(result.data.logId)
      
      // 自动显示可视化监控
      if (!showVisualizer.value) {
        showVisualizer.value = true
      }
    } else {
      addProgressLog('error', result.message || t('process.executionFailed'))
      addRealtimeLog(`${t('process.executionFailed')}: ${result.message || t('process.unknownError')}`, 'error')
      ElMessage.error(result.message || t('process.executionFailed'))
      executing.value = false
    }
  } catch {
    addProgressLog('error', t('process.executionError'))
    addRealtimeLog(t('process.executionErrorHint'), 'error')
    ElMessage.error(t('process.executionError'))
    executing.value = false
  }
}

// 开始轮询执行状态
const startPolling = (logId) => {
  // 如果已有轮询在运行，先停止
  if (pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
  
  let pollCount = 0
  const maxPolls = 150 // 最多轮询5分钟（150次 * 2秒）
  
  pollInterval = setInterval(async () => {
    pollCount++
    
    // 超时处理
    if (pollCount > maxPolls) {
      clearInterval(pollInterval)
      executing.value = false
        addRealtimeLog(t('process.pollTimeout'), 'warning')
      return
    }
    
    try {
      const result = await apiGet(`/log/${logId}`)
      
      // 如果请求失败，尝试几次后停止
      if (result === null || result === undefined) {
        console.warn('轮询请求返回空')
        return
      }
      
      if (result.code === 0 && result.data) {
        const log = result.data
        
        // 更新进度日志和实时监控
        if (log.message) {
          const lines = log.message.split('\n').filter(line => line.trim())
          
          // 只显示新的日志行（避免重复显示）
          if (lines.length > executionProgress.value.length) {
            const newLines = lines.slice(executionProgress.value.length)
            newLines.forEach(line => {
              let type = 'info'
              if (line.includes('[成功]') || line.includes('完成') || line.includes('成功')) type = 'success'
              else if (line.includes('[错误]') || line.includes('[失败]')) type = 'error'
              else if (line.includes('>>>') || line.includes('开始执行') || line.includes('执行中')) type = 'warning'
              else if (line.includes('>')) type = 'info' // 浏览器自动化日志
              
              executionProgress.value.push({ message: line, time: '', type })
              addRealtimeLog(line, type)
              
              // 解析当前执行步骤
              if (line.includes('>>> 开始执行步骤') || line.includes('开始执行步骤')) {
                const match = line.match(/步骤?\s*(\d+)/)
                if (match) {
                  currentStepIndex.value = parseInt(match[1]) - 1
                }
              }
              
              // 解析机器人名称
              if (line.includes('机器人') || line.includes('浏览器')) {
                const match = line.match(/机器人[：:]\s*([^\n]+)/)
                if (match) {
                  currentExecutingRobot.value = match[1].trim()
                }
              }
            })
          }
        }
        
        // 如果执行完成
        if (log.status === 'completed' || log.status === 'completed_with_errors' || log.status === 'failed') {
          clearInterval(pollInterval)
          pollInterval = null
          executing.value = false
          
          // 加载结果数据
          if (log.resultData) {
            try {
              latestExecutionResult.value = {
                status: log.status,
                steps: JSON.parse(log.resultData)
              }
            } catch {
              latestExecutionResult.value = null
            }
          }
          
          // 刷新日志列表和统计
          await loadLogs()
          
          addProgressLog(log.status === 'completed' ? 'success' : 'warning', t('process.executionCompleted'))
          addRealtimeLog('=== ' + t('process.executionDone') + ' ===', 'success')
          ElMessage.success(log.status === 'completed' ? t('process.executionSuccess') : t('process.executionCompletedWithErrors'))
        }
      } else {
        // 日志不存在或其他错误
        console.warn('获取日志失败:', result.message)
      }
    } catch (error) {
      console.warn('轮询错误:', error)
      // 忽略网络错误，继续轮询
    }
  }, 2000)
}

// 添加实时日志
const addRealtimeLog = (message, type = 'info') => {
  const time = new Date().toLocaleTimeString()
  realtimeLogs.value.push({ message, type, time })
  
  // 限制日志数量
  if (realtimeLogs.value.length > 100) {
    realtimeLogs.value = realtimeLogs.value.slice(-50)
  }
  
  // 自动滚动到底部
  nextTick(() => {
    if (consoleContainer.value) {
      consoleContainer.value.scrollTop = consoleContainer.value.scrollHeight
    }
  })
}

// 清除进度
const clearProgress = () => {
  executionProgress.value = []
  realtimeLogs.value = []
  currentStepIndex.value = -1
  currentExecutingRobot.value = ''
}

// 添加进度日志
const addProgressLog = (type, message) => {
  executionProgress.value.push({
    message,
    time: new Date().toLocaleTimeString(),
    type
  })
}

// 获取日志类型
const getLogType = (status) => {
  if (status === 'completed') return 'success'
  if (status === 'completed_with_errors') return 'warning'
  if (status === 'failed') return 'error'
  return 'info'
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  if (status === 'completed') return 'success'
  if (status === 'completed_with_errors') return 'warning'
  if (status === 'failed') return 'danger'
  if (status === 'running') return ''
  return 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    pending: t('task.statusPending'),
    running: t('task.statusRunning'),
    completed: t('task.statusCompleted'),
    completed_with_errors: t('task.statusCompletedWithErrors'),
    failed: t('task.statusFailed'),
    cancelled: t('task.statusCancelled')
  }
  return map[status] || status || t('process.unknown')
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString()
}

// 打开设计器
const openDesigner = async () => {
  await loadRobots()
  await loadRobotCategories()
  
  // 加载当前流程的设计数据
  try {
    const result = await apiGet(`/process/${process.value.id}/design`)
    if (result.code === 0 && result.data) {
      try {
        const parsed = JSON.parse(result.data)
        // 判断是新的画布格式还是旧的步骤格式
        if (parsed.nodes && parsed.edges) {
          // 新格式
          canvasData.value = parsed
        } else {
          // 旧格式，转换为新格式
          canvasData.value = convertOldStepsToCanvas(parsed)
        }
      } catch {
        canvasData.value = { nodes: [], edges: [] }
      }
    } else {
      canvasData.value = { nodes: [], edges: [] }
    }
  } catch {
    canvasData.value = { nodes: [], edges: [] }
  }
  
  designerVisible.value = true
}

// 将旧格式的步骤转换为画布格式
const convertOldStepsToCanvas = (oldSteps) => {
  const nodes = oldSteps.map((step, index) => ({
    id: step.id || `node_${index}`,
    type: 'process',
    name: step.name || t('process.noNamedStep'),
    stepType: step.type || '',
    category: step.category || '',
    robotId: step.robotId || null,
    robotName: step.robotName || '',
    x: 200,
    y: 100 + index * 150
  }))
  
  // 创建连接线
  const edges = []
  for (let i = 0; i < nodes.length - 1; i++) {
    edges.push({
      id: `edge_${i}`,
      source: nodes[i].id,
      sourcePort: 'bottom',
      target: nodes[i + 1].id,
      targetPort: 'top'
    })
  }
  
  return { nodes, edges }
}

// 保存设计
const saveDesign = async () => {
  savingDesign.value = true
  try {
    // 从画布组件获取数据
    const data = canvasDesignerRef.value?.saveData() || canvasData.value
    const stepsData = JSON.stringify(data)
    const result = await apiPost(`/process/${process.value.id}/design`, { steps: stepsData })
    if (result.code === 0) {
      ElMessage.success(t('process.saveSuccess'))
      await loadSteps()
      designerVisible.value = false
    } else {
      ElMessage.error(result.message || t('process.saveFailed'))
    }
  } catch {
    ElMessage.error(t('process.requestFailed'))
  } finally {
    savingDesign.value = false
  }
}

// 返回列表
const goBack = () => {
  router.push('/rpa/processes')
}

onMounted(async () => {
  await loadProcess()
  loadRobots()
  loadLogs()
})

onUnmounted(() => {
  if (pollInterval) {
    clearInterval(pollInterval)
  }
})
</script>

<style scoped>
.process-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.detail-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 32px;
  background: white;
  border-bottom: 1px solid #e8ecef;
  box-shadow: 0 1px 3px rgba(44, 62, 80, 0.04);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #7f8c8d;
  font-size: 14px;
  padding: 8px 14px;
  border-radius: 6px;
  transition: all 0.25s ease;
}

.back-btn:hover {
  color: #2c3e50;
  background: #f5f7fa;
}

.nav-title {
  font-size: 17px;
  font-weight: 600;
  color: #2c3e50;
}

.nav-actions {
  display: flex;
  gap: 12px;
}

.nav-actions :deep(.el-button) {
  border-radius: 6px;
  padding: 9px 18px;
  font-size: 14px;
  transition: all 0.25s ease;
}

.nav-actions :deep(.el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(44, 62, 80, 0.12);
}

.detail-container {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 20px;
  padding: 20px 32px;
  max-width: 1600px;
  margin: 0 auto;
}

.info-card, .steps-card, .log-card, .stats-card, .result-card, .progress-card {
  background: white;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(44, 62, 80, 0.06);
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid #eaeff3;
}

.info-card:hover, .steps-card:hover {
  box-shadow: 0 4px 16px rgba(44, 62, 80, 0.1);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #ecf0f3 0%, #dfe6ed 100%);
  border-bottom: 1px solid #dae1e8;
}

.steps-card .card-header {
  background: linear-gradient(135deg, #e8f4f8 0%, #d4e9f0 100%);
  border-bottom: 1px solid #cce5ed;
}

.log-card .card-header {
  background: linear-gradient(135deg, #eaf2f8 0%, #d8e8f2 100%);
  border-bottom: 1px solid #cce0eb;
}

.stats-card .card-header {
  background: linear-gradient(135deg, #e8f5e9 0%, #d4edd9 100%);
  border-bottom: 1px solid #cce5d3;
}

.result-card .card-header {
  background: linear-gradient(135deg, #f3e8f8 0%, #e8d4f0 100%);
  border-bottom: 1px solid #e5cce8;
}

.progress-card .card-header {
  background: linear-gradient(135deg, #fff3e8 0%, #ffe8d4 100%);
  border-bottom: 1px solid #ffe0c9;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(44, 62, 80, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #5a6c7d;
}

.steps-card .card-icon { color: #3498db; }
.log-card .card-icon { color: #2980b9; }
.stats-card .card-icon { color: #27ae60; }
.result-card .card-icon { color: #9b59b6; }
.progress-card .card-icon { color: #e67e22; }

.card-title h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  letter-spacing: 0.3px;
}

.card-title p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #7f8c8d;
}

.card-body {
  padding: 20px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.info-item.full {
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
  padding-top: 14px;
  border-top: 1px solid #f0f2f5;
}

.info-item label {
  min-width: 70px;
  color: #95a5a6;
  font-size: 13px;
  font-weight: 500;
  text-align: right;
  line-height: 24px;
}

.info-value {
  color: #34495e;
  font-size: 13px;
  flex: 1;
  line-height: 24px;
}

.info-value.primary {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
}

.info-value.code {
  font-family: 'JetBrains Mono', 'Monaco', monospace;
  font-size: 12px;
  color: #3498db;
  background: #ebf5fb;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid #d4e6f1;
}

.info-value.desc {
  color: #7f8c8d;
  line-height: 1.7;
}

.steps-list, .result-list, .progress-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.step-item, .result-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #ecf0f3;
  transition: all 0.25s ease;
}

.step-item:hover {
  background: #f0f7fc;
  border-color: #b8d4e8;
  transform: translateX(4px);
}

.step-item.has-robot {
  background: linear-gradient(135deg, #f0fdf4 0%, #e8f8f0 100%);
  border-color: #a3d9b8;
}

.result-item.success {
  background: linear-gradient(135deg, #f0fdf4 0%, #e8f8f0 100%);
  border-color: #a3d9b8;
}

.result-item.failed {
  background: linear-gradient(135deg, #fef5f5 0%, #fde8e8 100%);
  border-color: #f5b8b8;
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #bdc3c7 0%, #95a5a6 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 12px;
  flex-shrink: 0;
}

.step-item.has-robot .step-number {
  background: linear-gradient(135deg, #27ae60 0%, #229954 100%);
}

.result-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.result-item.success .result-icon {
  background: #e8f8f0;
}

.result-item.failed .result-icon {
  background: #fde8e8;
}

.step-content, .result-content {
  flex: 1;
  min-width: 0;
}

.step-header, .result-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.step-name, .result-step {
  font-weight: 600;
  color: #2c3e50;
  font-size: 13px;
}

.step-desc {
  font-size: 12px;
  color: #7f8c8d;
  margin-bottom: 8px;
}

.step-robot, .result-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.result-robot {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #7f8c8d;
}

.result-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.result-count {
  font-size: 13px;
  color: #27ae60;
  font-weight: 500;
}

.result-message {
  font-size: 12px;
  color: #606266;
}

.result-error {
  font-size: 12px;
  color: #e74c3c;
}

.robot-status {
  font-size: 11px;
  color: #bdc3c7;
}

.empty-steps, .empty-result {
  padding: 40px 20px;
  background: #fafbfc;
  border-radius: 8px;
  border: 2px dashed #e8ecef;
}

.right-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.log-card, .progress-card {
  flex: 1;
  min-height: 300px;
  display: flex;
  flex-direction: column;
}

.log-card .card-header, .progress-card .card-header {
  flex-shrink: 0;
}

.progress-actions {
  margin-left: auto;
  display: flex;
  gap: 6px;
}

.log-actions {
  margin-left: auto;
}

.log-actions :deep(.el-button) {
  color: #5a6c7d;
  font-size: 12px;
  background: rgba(44, 62, 80, 0.06);
  border: none;
}

.log-body, .progress-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  max-height: 350px;
  background: #fafbfc;
}

.empty-logs, .empty-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 20px;
  color: #bdc3c7;
}

.empty-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #ecf0f3;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: #bdc3c7;
  margin-bottom: 14px;
}

.empty-logs p {
  margin: 0;
  font-size: 13px;
  color: #95a5a6;
}

.empty-logs span {
  font-size: 11px;
  margin-top: 6px;
  color: #bdc3c7;
}

.log-timeline {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-item {
  display: flex;
  gap: 10px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(-8px); }
  to { opacity: 1; transform: translateX(0); }
}

.log-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #bdc3c7;
  margin-top: 5px;
  flex-shrink: 0;
}

.log-item.success .log-dot { background: #27ae60; }
.log-item.error .log-dot { background: #e74c3c; }
.log-item.warning .log-dot { background: #f39c12; }
.log-item.info .log-dot { background: #3498db; }

.log-content {
  flex: 1;
  min-width: 0;
}

.log-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.log-time {
  font-size: 10px;
  color: #bdc3c7;
  font-family: 'JetBrains Mono', 'Monaco', monospace;
}

.log-message {
  font-size: 12px;
  color: #5a6c7d;
  line-height: 1.5;
  white-space: pre-wrap;
}

.log-duration {
  font-size: 10px;
  color: #bdc3c7;
  margin-top: 4px;
}

.progress-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #ecf0f3;
}

.progress-item:last-child {
  border-bottom: none;
}

.progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.progress-item.info .progress-dot { background: #3498db; }
.progress-item.success .progress-dot { background: #27ae60; }
.progress-item.error .progress-dot { background: #e74c3c; }
.progress-item.warning .progress-dot { background: #f39c12; }

.progress-dot.pulse {
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}

/* 可视化监控面板 */
.visualizer-panel {
  border-top: 1px solid #ffe0c9;
  background: #1e1e1e;
  max-height: 400px;
  overflow: hidden;
}

.visualizer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #2d2d2d;
  border-bottom: 1px solid #3d3d3d;
}

.viz-title {
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}

.viz-status {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #888;
  font-size: 11px;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #666;
}

.status-indicator.active {
  background: #67c23a;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.visualizer-body {
  padding: 12px 14px;
  max-height: 350px;
  overflow-y: auto;
}

.viz-robot-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #252526;
  border-radius: 6px;
  color: #4fc3f7;
  font-size: 12px;
  margin-bottom: 12px;
}

.viz-steps-flow {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  overflow-x: auto;
  padding: 4px 0;
}

.viz-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 70px;
}

.viz-step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: #3d3d3d;
  color: #888;
  border: 2px solid #555;
  transition: all 0.3s ease;
}

.viz-step.completed .viz-step-dot {
  background: #67c23a;
  color: white;
  border-color: #67c23a;
}

.viz-step.active .viz-step-dot {
  background: #e6a23c;
  color: white;
  border-color: #e6a23c;
  animation: stepPulse 1s infinite;
}

@keyframes stepPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(230, 162, 60, 0); }
}

.viz-step.pending .viz-step-dot {
  background: #3d3d3d;
  color: #666;
  border-color: #444;
}

.viz-step-name {
  font-size: 10px;
  color: #888;
  text-align: center;
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.viz-step.active .viz-step-name {
  color: #e6a23c;
  font-weight: 500;
}

.viz-step.completed .viz-step-name {
  color: #67c23a;
}

.viz-console {
  background: #0d0d0d;
  border-radius: 6px;
  overflow: hidden;
}

.console-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #252526;
  color: #888;
  font-size: 11px;
  border-bottom: 1px solid #3d3d3d;
}

.console-blink {
  color: #4fc3f7;
  animation: consoleBlink 1s infinite;
}

@keyframes consoleBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.console-body {
  padding: 8px 10px;
  max-height: 180px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', monospace;
}

.console-line {
  display: flex;
  gap: 8px;
  padding: 2px 0;
  font-size: 11px;
  line-height: 1.4;
}

.console-line.info .console-text { color: #4fc3f7; }
.console-line.success .console-text { color: #67c23a; }
.console-line.error .console-text { color: #f56c6c; }
.console-line.warning .console-text { color: #e6a23c; }

.console-time {
  color: #666;
  flex-shrink: 0;
}

.console-text {
  color: #ddd;
  word-break: break-all;
}

.console-empty {
  color: #555;
  font-size: 11px;
  text-align: center;
  padding: 20px;
}

.progress-content {
  flex: 1;
}

.progress-message {
  font-size: 12px;
  color: #5a6c7d;
  line-height: 1.5;
}

.progress-time {
  font-size: 10px;
  color: #bdc3c7;
  margin-top: 4px;
}

.stats-card .card-body {
  padding: 16px;
}

.stats-body {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.stat-item {
  text-align: center;
  padding: 16px 10px;
  background: #fafbfc;
  border-radius: 8px;
  transition: all 0.25s ease;
  border: 1px solid #ecf0f3;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(44, 62, 80, 0.06);
  background: white;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1;
}

.stat-item.success .stat-value { color: #27ae60; }
.stat-item.danger .stat-value { color: #e74c3c; }
.stat-item.warning .stat-value { color: #f39c12; }

.stat-label {
  font-size: 11px;
  color: #95a5a6;
  margin-top: 6px;
}

/* 设计器样式 */
.process-designer-dialog :deep(.el-dialog__header) {
  padding: 0;
  border-bottom: none;
}

.process-designer-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 70vh;
  overflow: hidden;
}

.designer-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
}

.designer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #1a1f36 0%, #2d3748 100%);
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 24px;
  color: rgba(255, 255, 255, 0.9);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: white;
}

.header-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.header-actions :deep(.el-button) {
  padding: 8px 14px;
  font-size: 13px;
  border-radius: 6px;
}

.steps-container {
  flex: 1;
  overflow-y: auto;
  padding: 18px 24px;
  background: #f5f7fa;
}

.steps-list-design {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.step-card {
  background: white;
  border-radius: 8px;
  padding: 12px 16px;
  box-shadow: 0 1px 3px rgba(44, 62, 80, 0.04);
  border: 1px solid #e8ecef;
  transition: all 0.25s ease;
}

.step-card:hover {
  border-color: #b8d4e8;
  box-shadow: 0 3px 10px rgba(52, 152, 219, 0.1);
  transform: translateY(-2px);
}

.step-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.step-drag-handle {
  cursor: move;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #bdc3c7;
}

.step-card:hover .step-drag-handle {
  background: #ecf0f3;
  color: #3498db;
}

.step-number {
  flex-shrink: 0;
}

.number-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #bdc3c7 0%, #95a5a6 100%);
  color: white;
  font-weight: 600;
  font-size: 12px;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.field-label {
  font-size: 12px;
  color: #7f8c8d;
  font-weight: 500;
  min-width: 85px;
  text-align: right;
}

.field-input, .field-select {
  flex: 1;
}

.robot-display {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.step-actions {
  flex-shrink: 0;
}

.delete-btn:hover {
  background: #fdf2f2;
  border-radius: 4px;
}

.empty-steps-design {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 20px;
  background: white;
  border-radius: 10px;
  border: 2px dashed #e8ecef;
}

.empty-steps-design:hover {
  border-color: #b8d4e8;
  background: #f5fbff;
}

.designer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
  background: white;
  border-top: 1px solid #e8ecef;
}

.step-count {
  font-size: 12px;
  color: #7f8c8d;
}

.footer-buttons {
  display: flex;
  gap: 10px;
}

.footer-buttons :deep(.el-button) {
  min-width: 90px;
  border-radius: 6px;
}

.robot-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.robot-name {
  flex: 1;
  font-weight: 500;
}

/* 画布设计器弹窗样式 */
.process-designer-dialog-canvas :deep(.el-dialog__body) {
  padding: 0;
  height: 80vh;
  overflow: hidden;
}

.canvas-designer-wrapper {
  height: 100%;
  overflow: hidden;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 24px;
  border-top: 1px solid #e8ecef;
}

@media (max-width: 1200px) {
  .detail-container {
    grid-template-columns: 1fr;
  }
}
</style>
