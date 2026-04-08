<template>
  <div class="canvas-designer">
    <!-- 工具栏 -->
    <div class="canvas-toolbar">
      <div class="toolbar-left">
        <span class="toolbar-title">流程画布</span>
        <el-divider direction="vertical" />
        <el-button size="small" @click="addProcessNode" type="primary">
          <el-icon><Plus /></el-icon> 添加流程节点
        </el-button>
        <el-button size="small" @click="addConditionNode" type="warning">
          <el-icon><Plus /></el-icon> 添加条件节点
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button size="small" @click="autoLayout" text>
          <el-icon><Grid /></el-icon> 自动布局
        </el-button>
        <el-button size="small" @click="clearCanvas" text type="danger">
          <el-icon><Delete /></el-icon> 清空画布
        </el-button>
      </div>
    </div>

    <!-- 画布区域 -->
    <div class="canvas-container" ref="canvasContainer">
      <svg 
        class="canvas-svg" 
        :width="canvasWidth" 
        :height="canvasHeight"
        @mousedown="handleCanvasMouseDown"
        @mousemove="handleCanvasMouseMove"
        @mouseup="handleCanvasMouseUp"
        @mouseleave="handleCanvasMouseUp"
      >
        <!-- 网格背景 -->
        <defs>
          <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
            <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#e8e8e8" stroke-width="1"/>
          </pattern>
          <!-- 箭头标记 - refX设置为箭头尖端位置 -->
          <marker 
            id="arrowhead" 
            markerWidth="12" 
            markerHeight="8" 
            refX="11" 
            refY="4" 
            orient="auto"
          >
            <polygon points="0 0, 12 4, 0 8, 2 4" fill="#409eff" />
          </marker>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid)" />

        <!-- 连接线 -->
        <g class="edges-layer">
          <g v-for="edge in edges" :key="edge.id" class="edge-group">
            <!-- 透明粗线用于增加点击区域 -->
            <path
              :d="getEdgePath(edge)"
              class="edge-hit-area"
              @click="selectEdge(edge)"
            />
            <!-- 实际显示的线 -->
            <path
              :d="getEdgePath(edge)"
              class="edge-path"
              :class="{ 'selected': selectedEdgeId === edge.id }"
              marker-end="url(#arrowhead)"
            />
          </g>
        </g>

        <!-- 节点 -->
        <g class="nodes-layer">
          <g
            v-for="node in nodes"
            :key="node.id"
            :transform="`translate(${node.x}, ${node.y})`"
            class="node-group"
            :class="{ 'selected': selectedNodeId === node.id }"
            @mousedown="handleNodeMouseDown($event, node)"
          >
            <!-- 流程节点（方框） -->
            <g v-if="node.type === 'process'">
              <rect
                x="-80"
                y="-30"
                width="160"
                height="60"
                rx="8"
                ry="8"
                class="node-rect process-node"
                :class="{ 'has-robot': node.robotId }"
              />
              <text x="0" y="-8" text-anchor="middle" class="node-title">{{ node.name }}</text>
              <text x="0" y="12" text-anchor="middle" class="node-subtitle">
                {{ node.robotName || '未绑定机器人' }}
              </text>
              <!-- 连接点 - 上 -->
              <circle cx="0" cy="-30" r="5" class="connection-point top" @mousedown.stop="startConnection($event, node, 'top')" />
              <!-- 连接点 - 下 -->
              <circle cx="0" cy="30" r="5" class="connection-point bottom" @mousedown.stop="startConnection($event, node, 'bottom')" />
              <!-- 连接点 - 左 -->
              <circle cx="-80" cy="0" r="5" class="connection-point left" @mousedown.stop="startConnection($event, node, 'left')" />
              <!-- 连接点 - 右 -->
              <circle cx="80" cy="0" r="5" class="connection-point right" @mousedown.stop="startConnection($event, node, 'right')" />
            </g>

            <!-- 条件节点（菱形） -->
            <g v-if="node.type === 'condition'">
              <polygon
                points="0,-50 80,0 0,50 -80,0"
                class="node-polygon condition-node"
              />
              <text x="0" y="-10" text-anchor="middle" class="node-title">{{ node.name }}</text>
              <text x="0" y="10" text-anchor="middle" class="node-subtitle">
                {{ node.condition || '设置条件' }}
              </text>
              <!-- 连接点 - 上 -->
              <circle cx="0" cy="-50" r="5" class="connection-point top" @mousedown.stop="startConnection($event, node, 'top')" />
              <!-- 连接点 - 下 -->
              <circle cx="0" cy="50" r="5" class="connection-point bottom" @mousedown.stop="startConnection($event, node, 'bottom')" />
              <!-- 连接点 - 左 -->
              <circle cx="-80" cy="0" r="5" class="connection-point left" @mousedown.stop="startConnection($event, node, 'left')" />
              <!-- 连接点 - 右 -->
              <circle cx="80" cy="0" r="5" class="connection-point right" @mousedown.stop="startConnection($event, node, 'right')" />
            </g>
          </g>
        </g>

        <!-- 正在绘制的连接线 -->
        <g v-if="drawingEdge">
          <path
            :d="getDrawingEdgePath()"
            class="edge-path drawing"
          />
        </g>
      </svg>

      <!-- 空状态提示 -->
      <div v-if="nodes.length === 0" class="empty-canvas">
        <el-empty description="暂无节点，请添加工序节点或条件节点">
          <el-button type="primary" @click="addProcessNode">添加第一个节点</el-button>
        </el-empty>
      </div>
    </div>

    <!-- 节点属性编辑面板 -->
    <div v-if="selectedNodeId" class="properties-panel">
      <div class="panel-header">
        <span class="panel-title">节点属性</span>
        <el-button link @click="closeProperties">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="panel-body">
        <el-form label-width="80px" size="small">
          <el-form-item label="节点名称">
            <el-input v-model="selectedNode.name" placeholder="请输入节点名称" />
          </el-form-item>

          <!-- 流程节点属性 -->
          <template v-if="selectedNode.type === 'process'">
            <el-form-item label="节点类型">
              <el-select v-model="selectedNode.stepType" placeholder="选择步骤类型" clearable>
                <el-option value="collect" label="数据采集" />
                <el-option value="parse" label="数据解析" />
                <el-option value="process" label="数据加工" />
                <el-option value="query" label="数据查询" />
                <el-option value="transform" label="数据转换" />
                <el-option value="output" label="数据输出" />
                <el-option value="validate" label="数据校验" />
              </el-select>
            </el-form-item>
            <el-form-item label="机器人分类">
              <el-select v-model="selectedNode.category" placeholder="选择分类" clearable @change="onCategoryChange">
                <el-option v-for="cat in robotCategories" :key="cat.code" :value="cat.code" :label="cat.name" />
              </el-select>
            </el-form-item>
            <el-form-item label="执行机器人">
              <el-select 
                v-model="selectedNode.robotId" 
                placeholder="选择机器人" 
                filterable 
                clearable
                :disabled="!selectedNode.category"
              >
                <el-option
                  v-for="robot in getFilteredRobots(selectedNode.category)"
                  :key="robot.id"
                  :value="robot.id"
                  :label="robot.name"
                >
                  <div class="robot-option">
                    <span>{{ robot.name }}</span>
                    <el-tag size="small" :type="robot.status === 'idle' ? 'success' : robot.status === 'busy' ? 'warning' : 'info'">
                      {{ robot.status === 'idle' ? '空闲' : robot.status === 'busy' ? '忙碌' : '离线' }}
                    </el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </template>

          <!-- 条件节点属性 -->
          <template v-if="selectedNode.type === 'condition'">
            <el-form-item label="条件表达式">
              <el-input 
                v-model="selectedNode.condition" 
                type="textarea" 
                :rows="3"
                placeholder="例如: data.count > 100" 
              />
            </el-form-item>
            <el-form-item label="条件说明">
              <el-input v-model="selectedNode.description" type="textarea" :rows="2" placeholder="条件描述" />
            </el-form-item>
          </template>

          <el-form-item>
            <el-button type="danger" size="small" @click="deleteSelectedNode" style="width: 100%">
              <el-icon><Delete /></el-icon> 删除节点
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 边属性编辑面板 -->
    <div v-if="selectedEdgeId" class="properties-panel">
      <div class="panel-header">
        <span class="panel-title">连接线属性</span>
        <el-button link @click="closeProperties">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="panel-body">
        <el-form label-width="80px" size="small">
          <el-form-item label="源节点">
            <el-input :value="getSourceNodeName()" disabled />
          </el-form-item>
          <el-form-item label="目标节点">
            <el-input :value="getTargetNodeName()" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="danger" size="small" @click="deleteSelectedEdge" style="width: 100%">
              <el-icon><Delete /></el-icon> 删除连接线
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { Plus, Delete, Grid, Close } from '@element-plus/icons-vue'
import { apiGet } from '../../utils/api.js'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({ nodes: [], edges: [] })
  },
  robots: {
    type: Array,
    default: () => []
  },
  robotCategories: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

// 画布相关
const canvasContainer = ref(null)
const canvasWidth = ref(2000)
const canvasHeight = ref(1500)

// 节点和边
const nodes = ref([])
const edges = ref([])

// 选中状态
const selectedNodeId = ref(null)
const selectedEdgeId = ref(null)

// 拖拽和连线
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const isDrawingConnection = ref(false)
const connectionStart = ref(null)
const drawingEdge = ref(null)

// 选中的节点
const selectedNode = computed(() => {
  return nodes.value.find(n => n.id === selectedNodeId.value)
})

// 选中的边
const selectedEdge = computed(() => {
  return edges.value.find(e => e.id === selectedEdgeId.value)
})

// 加载数据
const loadData = (data) => {
  try {
    const parsed = typeof data === 'string' ? JSON.parse(data) : data
    nodes.value = parsed.nodes || []
    edges.value = parsed.edges || []
  } catch (e) {
    console.error('加载数据失败:', e)
    nodes.value = []
    edges.value = []
  }
}

// 初始化数据 - 只在外部传入新对象时加载，避免内部修改触发的循环
let isInternalUpdate = false

watch(() => props.modelValue, (newVal) => {
  // 如果是内部更新导致的，跳过处理
  if (isInternalUpdate) {
    isInternalUpdate = false
    return
  }
  
  // 检查是否是有效的画布数据对象
  if (newVal && typeof newVal === 'object' && (newVal.nodes !== undefined || newVal.edges !== undefined)) {
    loadData(newVal)
  }
}, { immediate: true })

// 保存数据
const saveData = () => {
  const data = {
    nodes: nodes.value,
    edges: edges.value
  }
  isInternalUpdate = true  // 标记为内部更新
  emit('update:modelValue', data)
  return data
}

// 添加流程节点
const addProcessNode = () => {
  const node = {
    id: `node_${Date.now()}`,
    type: 'process',
    name: '新流程节点',
    stepType: '',
    category: '',
    robotId: null,
    robotName: '',
    x: 200 + Math.random() * 100,
    y: 200 + Math.random() * 100
  }
  nodes.value.push(node)
  selectNode(node.id)
  saveData()
}

// 添加条件节点
const addConditionNode = () => {
  const node = {
    id: `node_${Date.now()}`,
    type: 'condition',
    name: '新条件节点',
    condition: '',
    description: '',
    x: 200 + Math.random() * 100,
    y: 200 + Math.random() * 100
  }
  nodes.value.push(node)
  selectNode(node.id)
  saveData()
}

// 选择节点
const selectNode = (nodeId) => {
  selectedNodeId.value = nodeId
  selectedEdgeId.value = null
}

// 关闭属性面板
const closeProperties = () => {
  selectedNodeId.value = null
  selectedEdgeId.value = null
}

// 删除选中节点
const deleteSelectedNode = () => {
  if (!selectedNodeId.value) return
  
  // 删除相关边
  edges.value = edges.value.filter(e => 
    e.source !== selectedNodeId.value && e.target !== selectedNodeId.value
  )
  
  // 删除节点
  nodes.value = nodes.value.filter(n => n.id !== selectedNodeId.value)
  
  selectedNodeId.value = null
  saveData()
}

// 删除选中边
const deleteSelectedEdge = () => {
  if (!selectedEdgeId.value) return
  
  edges.value = edges.value.filter(e => e.id !== selectedEdgeId.value)
  selectedEdgeId.value = null
  saveData()
}

// 获取源节点名称
const getSourceNodeName = () => {
  if (!selectedEdge.value) return ''
  const node = nodes.value.find(n => n.id === selectedEdge.value.source)
  return node ? node.name : '未知节点'
}

// 获取目标节点名称
const getTargetNodeName = () => {
  if (!selectedEdge.value) return ''
  const node = nodes.value.find(n => n.id === selectedEdge.value.target)
  return node ? node.name : '未知节点'
}

// 分类变更
const onCategoryChange = () => {
  if (selectedNode.value) {
    selectedNode.value.robotId = null
    selectedNode.value.robotName = ''
  }
}

// 获取过滤后的机器人列表
const getFilteredRobots = (category) => {
  if (!category) return []
  return props.robots.filter(r => r.robotCategory === category)
}

// 更新机器人名称
watch(() => selectedNode.value?.robotId, (newRobotId) => {
  if (selectedNode.value && newRobotId) {
    const robot = props.robots.find(r => r.id === newRobotId)
    if (robot) {
      selectedNode.value.robotName = robot.name
    }
  }
})

// 开始连线
const startConnection = (event, node, port) => {
  event.stopPropagation()
  isDrawingConnection.value = true
  connectionStart.value = { node, port }
  
  const rect = canvasContainer.value.getBoundingClientRect()
  drawingEdge.value = {
    source: node.id,
    sourcePort: port,
    targetX: event.clientX - rect.left,
    targetY: event.clientY - rect.top
  }
}

// 画布鼠标事件
const handleCanvasMouseDown = (event) => {
  // 取消选中
  if (event.target.tagName === 'svg') {
    selectedNodeId.value = null
    selectedEdgeId.value = null
  }
}

const handleCanvasMouseMove = (event) => {
  const rect = canvasContainer.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  // 绘制连线
  if (isDrawingConnection.value && drawingEdge.value) {
    drawingEdge.value.targetX = x
    drawingEdge.value.targetY = y
  }
  
  // 拖拽节点
  if (isDragging.value && selectedNodeId.value) {
    const node = nodes.value.find(n => n.id === selectedNodeId.value)
    if (node) {
      node.x = x - dragOffset.value.x
      node.y = y - dragOffset.value.y
    }
  }
}

const handleCanvasMouseUp = (event) => {
  // 完成连线
  if (isDrawingConnection.value) {
    // 检查是否释放到另一个节点上
    const rect = canvasContainer.value.getBoundingClientRect()
    const x = event.clientX - rect.left
    const y = event.clientY - rect.top
    
    const targetNode = nodes.value.find(n => {
      const dx = Math.abs(n.x - x)
      const dy = Math.abs(n.y - y)
      return dx < 80 && dy < 50 && n.id !== connectionStart.value.node.id
    })
    
    if (targetNode) {
      // 创建边
      const edge = {
        id: `edge_${Date.now()}`,
        source: connectionStart.value.node.id,
        sourcePort: connectionStart.value.port,
        target: targetNode.id,
        targetPort: 'top' // 默认连接到顶部
      }
      edges.value.push(edge)
      saveData()
    }
    
    isDrawingConnection.value = false
    connectionStart.value = null
    drawingEdge.value = null
  }
  
  isDragging.value = false
}

// 节点鼠标事件
const handleNodeMouseDown = (event, node) => {
  event.stopPropagation()
  selectNode(node.id)
  
  const rect = canvasContainer.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  isDragging.value = true
  dragOffset.value = {
    x: x - node.x,
    y: y - node.y
  }
}

// 选择边
const selectEdge = (edge) => {
  selectedEdgeId.value = edge.id
  selectedNodeId.value = null
}

// 获取边的路径 - 使用纯直线
const getEdgePath = (edge) => {
  const sourceNode = nodes.value.find(n => n.id === edge.source)
  const targetNode = nodes.value.find(n => n.id === edge.target)
  
  if (!sourceNode || !targetNode) return ''
  
  const sourcePoint = getNodePortPoint(sourceNode, edge.sourcePort)
  const targetPoint = getNodePortPoint(targetNode, edge.targetPort)
  
  // 直接使用直线连接，marker-end的refX会自动处理箭头位置
  return `M ${sourcePoint.x} ${sourcePoint.y} L ${targetPoint.x} ${targetPoint.y}`
}

// 获取节点连接点坐标
const getNodePortPoint = (node, port) => {
  let x = node.x
  let y = node.y
  
  switch (port) {
    case 'top':
      y -= node.type === 'condition' ? 50 : 30
      break
    case 'bottom':
      y += node.type === 'condition' ? 50 : 30
      break
    case 'left':
      x -= 80
      break
    case 'right':
      x += 80
      break
  }
  
  return { x, y }
}

// 获取正在绘制的边的路径 - 使用直线
const getDrawingEdgePath = () => {
  if (!drawingEdge.value) return ''
  
  const sourceNode = nodes.value.find(n => n.id === drawingEdge.value.source)
  if (!sourceNode) return ''
  
  const sourcePoint = getNodePortPoint(sourceNode, drawingEdge.value.sourcePort)
  const targetX = drawingEdge.value.targetX
  const targetY = drawingEdge.value.targetY
  
  // 使用直线
  return `M ${sourcePoint.x} ${sourcePoint.y} L ${targetX} ${targetY}`
}

// 自动布局
const autoLayout = () => {
  const spacing = 200
  const startX = 150
  const startY = 100
  
  nodes.value.forEach((node, index) => {
    const row = Math.floor(index / 3)
    const col = index % 3
    node.x = startX + col * spacing
    node.y = startY + row * spacing
  })
  
  saveData()
}

// 清空画布
const clearCanvas = () => {
  nodes.value = []
  edges.value = []
  selectedNodeId.value = null
  selectedEdgeId.value = null
  saveData()
}

// 暴露方法供父组件调用
defineExpose({
  saveData,
  loadData
})
</script>

<style scoped>
.canvas-designer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
  position: relative;
}

/* 工具栏 */
.canvas-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: white;
  border-bottom: 1px solid #e8ecef;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

/* 画布容器 */
.canvas-container {
  flex: 1;
  position: relative;
  overflow: auto;
  background: #fafafa;
}

.canvas-svg {
  display: block;
  cursor: default;
  min-height: 600px;
}

/* 空状态 */
.empty-canvas {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
}

/* 节点样式 */
.node-group {
  cursor: move;
  transition: filter 0.2s;
}

.node-group:hover {
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.15));
}

.node-group.selected .node-rect,
.node-group.selected .node-polygon {
  stroke: #409eff;
  stroke-width: 3;
}

/* 流程节点 */
.node-rect.process-node {
  fill: white;
  stroke: #409eff;
  stroke-width: 2;
  transition: all 0.3s;
}

.node-rect.process-node.has-robot {
  fill: #ecf5ff;
  stroke: #67c23a;
}

/* 条件节点 */
.node-polygon.condition-node {
  fill: white;
  stroke: #e6a23c;
  stroke-width: 2;
  transition: all 0.3s;
}

/* 节点文本 */
.node-title {
  font-size: 14px;
  font-weight: 600;
  fill: #2c3e50;
  pointer-events: none;
}

.node-subtitle {
  font-size: 11px;
  fill: #909399;
  pointer-events: none;
}

/* 连接点 */
.connection-point {
  fill: white;
  stroke: #409eff;
  stroke-width: 2;
  cursor: crosshair;
  opacity: 0;
  transition: opacity 0.2s;
}

.node-group:hover .connection-point {
  opacity: 1;
}

.connection-point:hover {
  fill: #409eff;
  r: 7;
}

/* 连接线 */
.edge-hit-area {
  fill: none;
  stroke: transparent;
  stroke-width: 15;
  cursor: pointer;
}

.edge-path {
  fill: none;
  stroke: #409eff;
  stroke-width: 2;
  pointer-events: none;
  transition: stroke 0.2s, stroke-width 0.2s;
}

.edge-group:hover .edge-path,
.edge-path.selected {
  stroke: #f56c6c;
  stroke-width: 3;
}

.edge-path.drawing {
  stroke: #409eff;
  stroke-dasharray: 5, 5;
  opacity: 0.6;
}

/* 属性面板 */
.properties-panel {
  position: absolute;
  right: 0;
  top: 60px;
  width: 320px;
  height: calc(100% - 60px);
  background: white;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  z-index: 100;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e8ecef;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.robot-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
