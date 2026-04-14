/**
 * RPA可视化流程设计器 - 流程画布组件
 * 
 * 功能说明：
 * - 基于Vue Flow实现流程可视化编辑
 * - 支持拖拽节点到画布
 * - 支持节点连线
 * - 支持节点选中、复制、删除
 * - 支持缩放、平移
 */

<template>
  <div 
    class="flow-canvas" 
    ref="canvasContainer"
    @drop="onDrop"
    @dragover.prevent
    @dragenter.prevent
  >
    <VueFlow
      v-model:nodes="nodes"
      v-model:edges="edges"
      :default-viewport="{ zoom: 1, x: 0, y: 0 }"
      :snap-to-grid="true"
      :snap-grid="[15, 15]"
      :connect-on-click="true"
      fit-view-on-init
      @node-click="onNodeClick"
      @edge-click="onEdgeClick"
      @connect="onConnect"
      @pane-click="onPaneClick"
    >
      <template #node-default="{ data }">
        <div class="custom-node" :class="[data.category, { selected: selectedNodeId === data.id }]">
          <div class="node-icon">{{ data.icon }}</div>
          <div class="node-label">{{ data.name }}</div>
        </div>
      </template>
    </VueFlow>

    <!-- 画布工具栏 -->
    <div class="canvas-toolbar">
      <el-tooltip content="适应画布" placement="left">
        <el-button circle size="small" @click="fitView">
          <el-icon><FullScreen /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip content="放大" placement="left">
        <el-button circle size="small" @click="zoomIn">
          <el-icon><ZoomIn /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip content="缩小" placement="left">
        <el-button circle size="small" @click="zoomOut">
          <el-icon><ZoomOut /></el-icon>
        </el-button>
      </el-tooltip>
      <el-divider direction="vertical" />
      <el-tooltip content="撤销" placement="left">
        <el-button circle size="small" @click="undo" :disabled="!canUndo">
          <el-icon><RefreshLeft /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip content="重做" placement="left">
        <el-button circle size="small" @click="redo" :disabled="!canRedo">
          <el-icon><RefreshRight /></el-icon>
        </el-button>
      </el-tooltip>
    </div>

    <!-- 节点统计 -->
    <div class="node-stats-bar">
      <span class="stat-item">
        <el-icon><Connection /></el-icon>
        {{ nodes.length }} 个节点
      </span>
      <span class="stat-item">
        <el-icon><Link /></el-icon>
        {{ edges.length }} 条连线
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { VueFlow, useVueFlow, Position } from '@vue-flow/core'
import { ElMessage } from 'element-plus'
import { FullScreen, ZoomIn, ZoomOut, RefreshLeft, RefreshRight, Connection, Link } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({ nodes: [], edges: [] })
  }
})

const emit = defineEmits(['update:modelValue', 'node-click', 'node-select', 'change'])

const canvasContainer = ref(null)
const selectedNodeId = ref(null)

// 历史记录
const history = ref([])
const historyIndex = ref(-1)

const { 
  zoomIn: flowZoomIn, 
  zoomOut: flowZoomOut, 
  fitView: flowFitView,
  onConnect: onFlowConnect,
  addNodes
} = useVueFlow()

const nodes = ref([])
const edges = ref([])

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    nodes.value = newVal.nodes || []
    edges.value = newVal.edges || []
  }
}, { immediate: true, deep: true })

watch([nodes, edges], () => {
  emit('update:modelValue', { nodes: nodes.value, edges: edges.value })
  emit('change', { nodes: nodes.value, edges: edges.value })
  saveHistory()
}, { deep: true })

const saveHistory = () => {
  const current = JSON.stringify({ nodes: nodes.value, edges: edges.value })
  if (historyIndex.value < history.value.length - 1) {
    history.value = history.value.slice(0, historyIndex.value + 1)
  }
  history.value.push(current)
  historyIndex.value = history.value.length - 1
  if (history.value.length > 50) {
    history.value.shift()
    historyIndex.value--
  }
}

const undo = () => {
  if (canUndo.value) {
    historyIndex.value--
    const state = JSON.parse(history.value[historyIndex.value])
    nodes.value = state.nodes
    edges.value = state.edges
  }
}

const redo = () => {
  if (canRedo.value) {
    historyIndex.value++
    const state = JSON.parse(history.value[historyIndex.value])
    nodes.value = state.nodes
    edges.value = state.edges
  }
}

const onDrop = (event) => {
  const jsonData = event.dataTransfer.getData('application/json')
  if (!jsonData) return

  try {
    const nodeData = JSON.parse(jsonData)
    const { left, top } = canvasContainer.value.getBoundingClientRect()
    
    const position = {
      x: event.clientX - left,
      y: event.clientY - top
    }

    addNodeFromPalette(nodeData, position)
  } catch (e) {
    console.error('Failed to parse node data:', e)
  }
}

const addNodeFromPalette = (nodeData, position) => {
  const id = `${nodeData.type}_${Date.now()}`
  const newNode = {
    id,
    type: 'default',
    position,
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    data: {
      id,
      type: nodeData.type,
      name: nodeData.name,
      icon: nodeData.icon,
      category: nodeData.category,
      description: nodeData.description,
      config: getDefaultConfig(nodeData.type),
      status: 'idle'
    }
  }

  nodes.value.push(newNode)
  selectedNodeId.value = id
  emit('node-select', newNode)
  ElMessage.success(`已添加节点: ${nodeData.name}`)
}

const getDefaultConfig = (type) => {
  const configs = {
    browser_open: { browser: 'chrome', headless: false, timeout: 30 },
    browser_navigate: { url: '', waitLoad: true, timeout: 30 },
    browser_click: { selector: '', index: 0, timeout: 10 },
    browser_input: { selector: '', value: '', clear: true },
    excel_open: { filePath: '', visible: true, readOnly: false },
    excel_read: { cell: 'A1', sheetName: '' },
    excel_write: { cell: 'A1', value: '', sheetName: '' },
    file_read: { filePath: '', encoding: 'UTF-8' },
    file_write: { filePath: '', content: '', encoding: 'UTF-8' },
    db_connect: { connectionString: '', provider: 'mysql' },
    db_query: { sql: '', timeout: 30 },
    ai_ocr: { imagePath: '', language: 'chi_sim+eng', confidence: 0.9 },
    logic_if: { condition: '' },
    logic_loop: { times: 1 },
    script_python: { code: '# Python代码\nprint("Hello RPA")', timeout: 60 },
    script_js: { code: '// JavaScript\nconsole.log("Hello")', timeout: 60 },
    script_http: { url: '', method: 'GET', headers: {}, body: '' }
  }
  return configs[type] || {}
}

const onNodeClick = (event) => {
  const node = event.node
  if (node) {
    selectedNodeId.value = node.id
    emit('node-click', node)
    emit('node-select', node)
  }
}

const onEdgeClick = () => {
  selectedNodeId.value = null
}

const onConnect = (params) => {
  const newEdge = {
    id: `edge_${params.source}_${params.target}_${Date.now()}`,
    source: params.source,
    target: params.target,
    type: 'smoothstep',
    animated: true,
    style: { stroke: '#409eff', strokeWidth: 2 }
  }
  edges.value.push(newEdge)
}

const onPaneClick = () => {
  selectedNodeId.value = null
}

const zoomIn = () => flowZoomIn()
const zoomOut = () => flowZoomOut()
const fitView = () => flowFitView({ padding: 0.2 })

// 暴露方法给父组件
defineExpose({ fitView, zoomIn, zoomOut, undo, redo, canUndo, canRedo })

saveHistory()
</script>

<style scoped>
.flow-canvas {
  flex: 1;
  height: 100%;
  position: relative;
  background: #f5f7fa;
  background-image: radial-gradient(circle, #dcdfe6 1px, transparent 1px);
  background-size: 20px 20px;
}

.canvas-toolbar {
  position: absolute;
  right: 16px;
  top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: white;
  padding: 8px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.node-stats-bar {
  position: absolute;
  left: 16px;
  bottom: 16px;
  display: flex;
  gap: 16px;
  background: white;
  padding: 8px 16px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #606266;
}

/* 自定义节点样式 */
:deep(.custom-node) {
  padding: 0;
  border-radius: 8px;
  min-width: 120px;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s;
  overflow: hidden;
}

:deep(.custom-node.selected) {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
  transform: scale(1.05);
}

:deep(.custom-node.trigger) {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

:deep(.custom-node.browser) {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

:deep(.custom-node.excel) {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

:deep(.custom-node.file) {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}

:deep(.custom-node.database) {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

:deep(.custom-node.ai) {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

:deep(.custom-node.logic) {
  background: linear-gradient(135deg, #e6a23c 0%, #f5c68a 100%);
}

:deep(.custom-node.script) {
  background: linear-gradient(135deg, #4a6cf7 0%, #6b8cff 100%);
}

:deep(.custom-node.default) {
  background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
}

:deep(.node-icon) {
  padding: 8px;
  text-align: center;
  font-size: 18px;
  background: rgba(0, 0, 0, 0.1);
}

:deep(.node-label) {
  padding: 6px 12px;
  font-size: 12px;
  text-align: center;
  white-space: nowrap;
}

:deep(.vue-flow__handle) {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  border: 2px solid white;
}

:deep(.vue-flow__edge-path) {
  stroke-width: 2;
}
</style>
