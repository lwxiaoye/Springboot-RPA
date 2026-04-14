/**
 * RPA可视化流程设计器 - 主组件
 * 
 * 功能说明：
 * - 集成节点面板、流程画布、属性配置面板
 * - 支持流程的新建、编辑、保存
 * - 支持流程的运行和调试
 * - 支持流程版本管理
 */

<template>
  <div class="process-designer">
    <!-- 顶部工具栏 -->
    <div class="designer-toolbar">
      <div class="toolbar-left">
        <el-button @click="goBack" :icon="Back">返回</el-button>
        <el-divider direction="vertical" />
        <el-input v-model="processName" placeholder="请输入流程名称" class="process-name-input" />
        <el-tag :type="isDirty ? 'warning' : 'success'" size="small" v-if="!isNewProcess">
          {{ isDirty ? '已修改' : '已保存' }}
        </el-tag>
      </div>
      <div class="toolbar-center">
        <el-tooltip content="撤销 (Ctrl+Z)">
          <el-button @click="undo" :disabled="!canUndo" circle><el-icon><RefreshLeft /></el-icon></el-button>
        </el-tooltip>
        <el-tooltip content="重做 (Ctrl+Y)">
          <el-button @click="redo" :disabled="!canRedo" circle><el-icon><RefreshRight /></el-icon></el-button>
        </el-tooltip>
        <el-divider direction="vertical" />
        <el-tooltip content="适应画布">
          <el-button @click="fitView" circle><el-icon><FullScreen /></el-icon></el-button>
        </el-tooltip>
        <el-tooltip content="放大">
          <el-button @click="zoomIn" circle><el-icon><ZoomIn /></el-icon></el-button>
        </el-tooltip>
        <el-tooltip content="缩小">
          <el-button @click="zoomOut" circle><el-icon><ZoomOut /></el-icon></el-button>
        </el-tooltip>
      </div>
      <div class="toolbar-right">
        <el-button @click="validateProcess">校验</el-button>
        <el-button @click="saveProcess" type="primary" :loading="saving">
          <el-icon><Check /></el-icon> 保存
        </el-button>
        <el-button @click="saveAndPublish" type="success" :loading="publishing">
          <el-icon><Upload /></el-icon> 保存并发布
        </el-button>
        <el-dropdown @command="handleMoreCommand" trigger="click">
          <el-button>
            更多 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="run">运行流程</el-dropdown-item>
              <el-dropdown-item command="debug">调试运行</el-dropdown-item>
              <el-dropdown-item command="export">导出流程</el-dropdown-item>
              <el-dropdown-item command="import">导入流程</el-dropdown-item>
              <el-dropdown-item command="duplicate">复制流程</el-dropdown-item>
              <el-dropdown-item command="version" divided>版本历史</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="designer-main">
      <!-- 左侧节点面板 -->
      <NodePalette @drag-start="onDragStart" />

      <!-- 中间画布区域 -->
      <FlowCanvas
        ref="flowCanvasRef"
        v-model="flowData"
        @node-click="onNodeClick"
        @node-select="onNodeSelect"
        @change="onFlowChange"
      />

      <!-- 右侧属性面板 -->
      <PropertyPanel
        :selected-node="selectedNode"
        @update="onNodeUpdate"
        @delete="onNodeDelete"
      />
    </div>

    <!-- 底部状态栏 -->
    <div class="designer-statusbar">
      <div class="status-left">
        <span class="status-item">
          <el-icon><Clock /></el-icon>
          {{ currentTime }}
        </span>
        <span class="status-item" v-if="processId">
          <el-icon><Document /></el-icon>
          ID: {{ processId }}
        </span>
      </div>
      <div class="status-center">
        <el-tag size="small" :type="nodeCount > 0 ? 'success' : 'info'">
          {{ nodeCount }} 个节点
        </el-tag>
        <el-tag size="small" type="info" style="margin-left: 8px;">
          {{ edgeCount }} 条连线
        </el-tag>
      </div>
      <div class="status-right">
        <el-switch v-model="autoSave" active-text="自动保存" size="small" />
      </div>
    </div>

    <!-- 版本历史对话框 -->
    <el-dialog v-model="versionDialogVisible" title="版本历史" width="700px">
      <el-table :data="versionHistory" border>
        <el-table-column prop="version" label="版本号" width="100" />
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column prop="creator" label="发布人" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'released' ? 'success' : 'info'">
              {{ row.status === 'released' ? '正式' : '测试' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="版本说明" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewVersion(row)">查看</el-button>
            <el-button link type="success" size="small" @click="restoreVersion(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 运行调试对话框 -->
    <el-dialog v-model="runDialogVisible" title="运行流程" width="600px">
      <el-form :model="runConfig" label-width="100px">
        <el-form-item label="运行模式">
          <el-radio-group v-model="runConfig.mode">
            <el-radio label="normal">普通运行</el-radio>
            <el-radio label="debug">调试运行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="执行机器人">
          <el-select v-model="runConfig.robotId" placeholder="自动选择" clearable>
            <el-option label="自动选择最优机器人" value="" />
            <el-option v-for="robot in availableRobots" :key="robot.id" :label="robot.name" :value="robot.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="输入参数">
          <el-input v-model="runConfig.params" type="textarea" :rows="4" placeholder='{"key": "value"}' />
        </el-form-item>
        <el-form-item label="运行节点">
          <el-select v-model="runConfig.startNodeId" placeholder="从开始节点运行" clearable>
            <el-option v-for="node in flowNodes" :key="node.id" :label="node.data.name" :value="node.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="startRun" :loading="running">开始运行</el-button>
      </template>
    </el-dialog>

    <!-- 执行日志对话框 -->
    <el-dialog v-model="logDialogVisible" title="执行日志" width="900px" fullscreen>
      <div class="execution-log">
        <div class="log-header">
          <el-tag :type="executionStatus === 'running' ? 'primary' : executionStatus === 'success' ? 'success' : 'danger'">
            {{ getStatusText(executionStatus) }}
          </el-tag>
          <el-button @click="stopExecution" v-if="executionStatus === 'running'" type="danger" size="small">
            停止执行
          </el-button>
        </div>
        <el-steps :active="currentStep" finish-status="success" align-center class="execution-steps">
          <el-step v-for="(node, index) in flowNodes" :key="node.id" :title="node.data.name" @click="jumpToStep(index)" />
        </el-steps>
        <div class="log-content" ref="logContentRef">
          <div v-for="(log, index) in executionLogs" :key="index" class="log-item" :class="log.level">
            <span class="log-time">{{ log.time }}</span>
            <span class="log-node">[{{ log.node }}]</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Check, Upload, FullScreen, ZoomIn, ZoomOut, RefreshLeft, RefreshRight, Clock, Document, ArrowDown } from '@element-plus/icons-vue'

// 组件
import NodePalette from './components/designer/NodePalette.vue'
import FlowCanvas from './components/designer/FlowCanvas.vue'
import PropertyPanel from './components/designer/PropertyPanel.vue'

import { apiGet, apiPost, apiPut } from '../../utils/api.js'

const router = useRouter()
const route = useRoute()

// Refs
const flowCanvasRef = ref(null)

// 状态
const processId = ref(route.query.id || null)
const isNewProcess = computed(() => !processId.value)
const processName = ref('未命名流程')
const isDirty = ref(false)
const saving = ref(false)
const publishing = ref(false)

// 自动保存
const autoSave = ref(false)
let autoSaveTimer = null

// 流程数据
const flowData = ref({ nodes: [], edges: [] })

// 选中节点
const selectedNode = ref(null)

// 历史记录
const canUndo = ref(false)
const canRedo = ref(false)

// 时间
const currentTime = ref('')
let timeTimer = null

// 对话框状态
const versionDialogVisible = ref(false)
const runDialogVisible = ref(false)
const logDialogVisible = ref(false)

// 版本历史
const versionHistory = ref([])

// 运行配置
const runConfig = reactive({
  mode: 'normal',
  robotId: '',
  params: '{}',
  startNodeId: ''
})

// 可用机器人
const availableRobots = ref([])

// 执行状态
const executionStatus = ref('')
const currentStep = ref(0)
const executionLogs = ref([])

// 计算属性
const nodeCount = computed(() => flowData.value.nodes?.length || 0)
const edgeCount = computed(() => flowData.value.edges?.length || 0)
const flowNodes = computed(() => flowData.value.nodes || [])

// 监听自动保存
watch(autoSave, (val) => {
  if (val) {
    startAutoSave()
    ElMessage.success('已开启自动保存')
  } else {
    stopAutoSave()
  }
})

// 监听流程数据变化
watch(flowData, () => {
  isDirty.value = true
}, { deep: true })

// 监听流程名称变化
watch(processName, () => {
  isDirty.value = true
})

// 方法
const goBack = async () => {
  if (isDirty.value) {
    try {
      await ElMessageBox.confirm('有未保存的更改，是否保存？', '提示', {
        type: 'warning'
      })
      await saveProcess()
    } catch {
      // 用户取消
    }
  }
  router.back()
}

const onDragStart = (node) => {
  console.log('Drag start:', node)
}

const onNodeClick = (node) => {
  selectedNode.value = node
}

const onNodeSelect = (node) => {
  selectedNode.value = node
}

const onNodeUpdate = (node) => {
  // 节点已更新
  isDirty.value = true
}

const onNodeDelete = (node) => {
  selectedNode.value = null
  isDirty.value = true
}

const onFlowChange = () => {
  isDirty.value = true
  canUndo.value = flowCanvasRef.value?.canUndo
  canRedo.value = flowCanvasRef.value?.canRedo
}

const validateProcess = () => {
  const errors = []
  
  // 检查节点数量
  if (nodeCount.value === 0) {
    errors.push('流程中没有任何节点')
  }
  
  // 检查触发器
  const hasTrigger = flowNodes.value.some(n => n.type === 'trigger')
  if (!hasTrigger) {
    errors.push('流程没有开始节点（触发器）')
  }
  
  // 检查连线
  if (edgeCount.value === 0 && nodeCount.value > 1) {
    errors.push('存在未连接的节点')
  }
  
  // 检查孤立节点
  const connectedNodes = new Set()
  flowData.value.edges?.forEach(e => {
    connectedNodes.add(e.source)
    connectedNodes.add(e.target)
  })
  flowNodes.value.forEach(n => {
    if (!connectedNodes.has(n.id) && nodeCount.value > 1) {
      errors.push(`节点 "${n.data.name}" 未连接到流程`)
    }
  })
  
  if (errors.length === 0) {
    ElMessage.success('流程校验通过')
  } else {
    ElMessage.warning({
      message: `发现 ${errors.length} 个问题:\n${errors.join('\n')}`,
      duration: 5000
    })
  }
  
  return errors.length === 0
}

const saveProcess = async () => {
  if (!processName.value.trim()) {
    ElMessage.warning('请输入流程名称')
    return
  }
  
  if (!validateProcess()) {
    return
  }
  
  saving.value = true
  try {
    const data = {
      name: processName.value,
      designConfig: JSON.stringify(flowData.value),
      status: 'draft'
    }
    
    let result
    if (processId.value) {
      result = await apiPut(`/process/${processId.value}`, data)
    } else {
      result = await apiPost('/process', data)
      if (result.code === 0) {
        processId.value = result.data.id
        router.replace({ query: { id: processId.value } })
      }
    }
    
    if (result.code === 0) {
      isDirty.value = false
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(result.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const saveAndPublish = async () => {
  if (!validateProcess()) {
    return
  }
  
  await saveProcess()
  
  if (!isDirty.value) {
    publishProcess()
  }
}

const publishProcess = async () => {
  try {
    await ElMessageBox.confirm('确定要发布这个流程吗？发布后将可以被执行。', '确认发布', {
      type: 'success'
    })
    
    const result = await apiPut(`/process/${processId.value}/publish`)
    if (result.code === 0) {
      ElMessage.success('发布成功')
    } else {
      ElMessage.error(result.message || '发布失败')
    }
  } catch (e) {
    // 用户取消
  }
}

const handleMoreCommand = async (command) => {
  switch (command) {
    case 'run':
      runDialogVisible.value = true
      break
    case 'debug':
      runConfig.mode = 'debug'
      runDialogVisible.value = true
      break
    case 'export':
      exportProcess()
      break
    case 'import':
      importProcess()
      break
    case 'duplicate':
      duplicateProcess()
      break
    case 'version':
      await loadVersionHistory()
      versionDialogVisible.value = true
      break
  }
}

const exportProcess = () => {
  const data = {
    name: processName.value,
    version: '1.0',
    exportTime: new Date().toISOString(),
    flowData: flowData.value
  }
  
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${processName.value}.rpa`
  a.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('流程导出成功')
}

const importProcess = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.rpa,.json'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    
    try {
      const text = await file.text()
      const data = JSON.parse(text)
      
      if (data.flowData) {
        flowData.value = data.flowData
        processName.value = data.name || '导入的流程'
        isDirty.value = true
        ElMessage.success('流程导入成功')
      } else {
        ElMessage.error('无效的流程文件')
      }
    } catch {
      ElMessage.error('文件解析失败')
    }
  }
  input.click()
}

const duplicateProcess = async () => {
  processId.value = null
  processName.value = processName.value + ' (副本)'
  isDirty.value = true
  ElMessage.success('已创建副本，请手动保存')
}

const loadVersionHistory = async () => {
  if (!processId.value) return
  
  try {
    const result = await apiGet(`/process/${processId.value}/versions`)
    if (result.code === 0) {
      versionHistory.value = result.data || []
    }
  } catch {
    versionHistory.value = []
  }
}

const viewVersion = (version) => {
  try {
    const config = JSON.parse(version.designConfig || '{}')
    flowData.value = config
    ElMessage.success('已加载版本: ' + version.version)
    versionDialogVisible.value = false
  } catch {
    ElMessage.error('版本数据解析失败')
  }
}

const restoreVersion = async (version) => {
  try {
    await ElMessageBox.confirm('确定要恢复到版本 ' + version.version + ' 吗？', '确认恢复', {
      type: 'warning'
    })
    viewVersion(version)
    isDirty.value = true
  } catch {
    // 用户取消
  }
}

const loadProcess = async () => {
  if (!processId.value) return
  
  try {
    const result = await apiGet(`/process/${processId.value}`)
    if (result.code === 0) {
      const data = result.data
      processName.value = data.name || '未命名流程'
      
      if (data.designConfig) {
        try {
          flowData.value = JSON.parse(data.designConfig)
        } catch {
          flowData.value = { nodes: [], edges: [] }
        }
      }
    }
  } catch {
    ElMessage.error('加载流程失败')
  }
}

const loadRobots = async () => {
  try {
    const result = await apiGet('/robot')
    if (result.code === 0) {
      availableRobots.value = result.data || []
    }
  } catch {
    availableRobots.value = []
  }
}

const startRun = async () => {
  runDialogVisible.value = false
  logDialogVisible.value = true
  executionStatus.value = 'running'
  executionLogs.value = []
  currentStep.value = 0
  
  // 模拟执行日志
  const simulateExecution = async () => {
    for (let i = 0; i < flowNodes.value.length; i++) {
      const node = flowNodes.value[i]
      executionLogs.value.push({
        time: new Date().toLocaleTimeString(),
        node: node.data.name,
        level: 'info',
        message: '开始执行...'
      })
      
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      executionLogs.value.push({
        time: new Date().toLocaleTimeString(),
        node: node.data.name,
        level: 'success',
        message: '执行成功'
      })
      
      currentStep.value = i + 1
    }
    
    executionStatus.value = 'success'
    executionLogs.value.push({
      time: new Date().toLocaleTimeString(),
      node: '系统',
      level: 'success',
      message: '流程执行完成'
    })
  }
  
  simulateExecution()
}

const stopExecution = () => {
  executionStatus.value = 'stopped'
  executionLogs.value.push({
    time: new Date().toLocaleTimeString(),
    node: '系统',
    level: 'warning',
    message: '执行被用户停止'
  })
}

const getStatusText = (status) => {
  const map = {
    running: '执行中',
    success: '执行成功',
    failed: '执行失败',
    stopped: '已停止'
  }
  return map[status] || status
}

const jumpToStep = (index) => {
  currentStep.value = index
}

// 缩放控制
const fitView = () => flowCanvasRef.value?.fitView()
const zoomIn = () => flowCanvasRef.value?.zoomIn()
const zoomOut = () => flowCanvasRef.value?.zoomOut()
const undo = () => flowCanvasRef.value?.undo()
const redo = () => flowCanvasRef.value?.redo()

// 自动保存
const startAutoSave = () => {
  autoSaveTimer = setInterval(async () => {
    if (isDirty.value) {
      await saveProcess()
    }
  }, 60000) // 每分钟自动保存
}

const stopAutoSave = () => {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
}

// 更新时间
const updateTime = () => {
  currentTime.value = new Date().toLocaleString()
}

// 生命周期
onMounted(async () => {
  await loadRobots()
  
  if (processId.value) {
    await loadProcess()
  }
  
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  stopAutoSave()
  if (timeTimer) {
    clearInterval(timeTimer)
  }
})
</script>

<style scoped>
.process-designer {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: hidden;
}

/* 工具栏 */
.designer-toolbar {
  height: 56px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  flex-shrink: 0;
}

.toolbar-left,
.toolbar-center,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.process-name-input {
  width: 240px;
}

/* 主内容区 */
.designer-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 状态栏 */
.designer-statusbar {
  height: 28px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  font-size: 12px;
  color: #606266;
  flex-shrink: 0;
}

.status-left,
.status-center,
.status-right {
  display: flex;
  align-items: center;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 16px;
}

/* 执行日志 */
.execution-log {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.log-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.execution-steps {
  margin-bottom: 20px;
}

.log-content {
  flex: 1;
  overflow-y: auto;
  background: #1e1e1e;
  border-radius: 8px;
  padding: 12px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  color: #d4d4d4;
}

.log-item.success {
  color: #67c23a;
}

.log-item.error {
  color: #f56c6c;
}

.log-item.warning {
  color: #e6a23c;
}

.log-time {
  color: #909399;
  flex-shrink: 0;
}

.log-node {
  color: #409eff;
  flex-shrink: 0;
  min-width: 100px;
}

.log-message {
  flex: 1;
  word-break: break-all;
}
</style>
