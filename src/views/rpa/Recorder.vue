<template>
  <div class="recorder-container">
    <el-card class="recorder-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><VideoCamera /></el-icon> 智能录制器</span>
          <el-tag :type="recordingStatus === 'RECORDING' ? 'danger' : 'info'">
            {{ statusText }}
          </el-tag>
        </div>
      </template>

      <!-- 录制模式选择 -->
      <div class="mode-selector" v-if="!sessionId">
        <el-radio-group v-model="recordMode" size="large">
          <el-radio-button value="DESKTOP">
            <el-icon><Monitor /></el-icon> 桌面模式
          </el-radio-button>
          <el-radio-button value="BROWSER">
            <el-icon><ChromeFilled /></el-icon> 浏览器模式
          </el-radio-button>
          <el-radio-button value="IMAGE">
            <el-icon><Picture /></el-icon> 图像识别模式
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button
          v-if="!sessionId"
          type="primary"
          size="large"
          @click="startRecording"
          :loading="starting"
        >
          <el-icon><VideoPlay /></el-icon>
          开始录制
        </el-button>

        <el-button-group v-if="sessionId">
          <el-button
            type="danger"
            @click="stopRecording"
            :disabled="recordingStatus !== 'RECORDING' && recordingStatus !== 'PAUSED'"
          >
            <el-icon><VideoPause /></el-icon>
            停止
          </el-button>
          <el-button
            v-if="recordingStatus === 'RECORDING'"
            type="warning"
            @click="pauseRecording"
          >
            <el-icon><Pause /></el-icon>
            暂停
          </el-button>
          <el-button
            v-if="recordingStatus === 'PAUSED'"
            type="success"
            @click="resumeRecording"
          >
            <el-icon><VideoPlay /></el-icon>
            继续
          </el-button>
        </el-button-group>
      </div>

      <!-- 实时操作统计 -->
      <div class="stats-panel" v-if="sessionId">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ operationCount }}</div>
              <div class="stat-label">操作数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ duration }}</div>
              <div class="stat-label">时长(秒)</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ mouseCount }}</div>
              <div class="stat-label">鼠标操作</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ keyboardCount }}</div>
              <div class="stat-label">键盘操作</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 截图预览区域 -->
      <div class="screenshot-area">
        <el-button @click="captureScreen" :disabled="!sessionId">
          <el-icon><Camera /></el-icon>
          捕获屏幕
        </el-button>
        <el-button @click="captureRegion" :disabled="!sessionId">
          <el-icon><Crop /></el-icon>
          截取区域
        </el-button>
        <el-button @click="getScreenInfo" :disabled="!sessionId">
          <el-icon><Monitor /></el-icon>
          屏幕信息
        </el-button>
      </div>

      <!-- 操作记录列表 -->
      <div class="operations-list" v-if="sessionId">
        <div class="list-header">
          <span>操作记录</span>
          <el-button text size="small" @click="clearOperations">清空</el-button>
        </div>
        <el-table :data="operations" size="small" max-height="200">
          <el-table-column prop="index" label="#" width="50" />
          <el-table-column prop="type" label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="row.type === 'mouse' ? 'primary' : row.type === 'keyboard' ? 'success' : 'warning'" size="small">
                {{ row.type === 'mouse' ? '鼠标' : row.type === 'keyboard' ? '键盘' : '标注' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="action" label="动作" width="80" />
          <el-table-column prop="x" label="X" width="60" />
          <el-table-column prop="y" label="Y" width="60" />
          <el-table-column prop="elementInfo" label="元素" show-overflow-tooltip />
        </el-table>
      </div>

      <!-- 导入到流程 -->
      <div class="import-section" v-if="scriptPath">
        <el-divider>导入到流程设计器</el-divider>
        <div class="import-actions">
          <el-select v-model="targetProcessId" placeholder="选择目标流程" clearable>
            <el-option
              v-for="process in processes"
              :key="process.id"
              :label="process.name"
              :value="process.id"
            />
          </el-select>
          <el-button type="primary" @click="importToProcess" :disabled="!targetProcessId">
            <el-icon><Upload /></el-icon>
            导入
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const api = axios.create({
  baseURL: '/api/recorder',
  timeout: 30000
})

// 状态
const recordMode = ref('DESKTOP')
const sessionId = ref(null)
const recordingStatus = ref('IDLE')
const operations = ref([])
const operationCount = ref(0)
const duration = ref(0)
const mouseCount = ref(0)
const keyboardCount = ref(0)
const starting = ref(false)
const scriptPath = ref(null)
const targetProcessId = ref(null)
const processes = ref([])

// 计算属性
const statusText = computed(() => {
  const statusMap = {
    'IDLE': '空闲',
    'RECORDING': '录制中',
    'PAUSED': '已暂停',
    'STOPPED': '已停止'
  }
  return statusMap[recordingStatus.value] || '未知'
})

// 开始录制
async function startRecording() {
  starting.value = true
  try {
    const response = await api.post('/start', {
      mode: recordMode.value,
      targetApp: '',
      description: ''
    })

    if (response.data.success) {
      sessionId.value = response.data.data.sessionId
      recordingStatus.value = 'RECORDING'
      operations.value = []
      operationCount.value = 0
      duration.value = 0
      mouseCount.value = 0
      keyboardCount.value = 0
      ElMessage.success('录制已开始')
      startDurationTimer()
    } else {
      ElMessage.error(response.data.message || '启动录制失败')
    }
  } catch (error) {
    ElMessage.error('启动录制失败: ' + error.message)
  } finally {
    starting.value = false
  }
}

// 停止录制
async function stopRecording() {
  try {
    const response = await api.post('/stop', null, {
      params: { sessionId: sessionId.value }
    })

    if (response.data.success) {
      recordingStatus.value = 'STOPPED'
      scriptPath.value = response.data.data.scriptPath
      operationCount.value = response.data.data.operationCount
      duration.value = response.data.data.duration
      ElMessage.success('录制完成，共 ' + operationCount.value + ' 个操作')
    } else {
      ElMessage.error(response.data.message || '停止录制失败')
    }
  } catch (error) {
    ElMessage.error('停止录制失败: ' + error.message)
  }
}

// 暂停录制
async function pauseRecording() {
  try {
    const response = await api.post('/pause', null, {
      params: { sessionId: sessionId.value }
    })

    if (response.data.success) {
      recordingStatus.value = 'PAUSED'
      ElMessage.info('录制已暂停')
    }
  } catch (error) {
    ElMessage.error('暂停失败: ' + error.message)
  }
}

// 恢复录制
async function resumeRecording() {
  try {
    const response = await api.post('/resume', null, {
      params: { sessionId: sessionId.value }
    })

    if (response.data.success) {
      recordingStatus.value = 'RECORDING'
      ElMessage.info('录制已恢复')
    }
  } catch (error) {
    ElMessage.error('恢复失败: ' + error.message)
  }
}

// 捕获全屏
async function captureScreen() {
  try {
    const response = await api.get('/screenshot/full')
    if (response.data.success) {
      ElMessage.success('截图已保存')
      addOperation({
        type: 'screenshot',
        action: 'capture',
        x: 0, y: 0,
        elementInfo: response.data.data.path
      })
    }
  } catch (error) {
    ElMessage.error('截图失败: ' + error.message)
  }
}

// 捕获区域
async function captureRegion() {
  try {
    // 简单实现：使用固定区域
    const response = await api.post('/screenshot/region', {
      x: 100, y: 100, width: 800, height: 600
    })
    if (response.data.success) {
      ElMessage.success('区域截图已保存')
      addOperation({
        type: 'screenshot',
        action: 'capture-region',
        x: 100, y: 100,
        elementInfo: response.data.data.path
      })
    }
  } catch (error) {
    ElMessage.error('区域截图失败: ' + error.message)
  }
}

// 获取屏幕信息
async function getScreenInfo() {
  try {
    const response = await api.get('/screen/size')
    if (response.data.success) {
      ElMessage.info('屏幕尺寸: ' + response.data.data.width + ' x ' + response.data.data.height)
    }
  } catch (error) {
    ElMessage.error('获取屏幕信息失败: ' + error.message)
  }
}

// 添加操作记录
function addOperation(op) {
  operationCount.value++
  if (op.type === 'mouse') mouseCount.value++
  if (op.type === 'keyboard') keyboardCount.value++

  operations.value.push({
    ...op,
    index: operationCount.value
  })
}

// 清空操作列表
function clearOperations() {
  operations.value = []
  operationCount.value = 0
  mouseCount.value = 0
  keyboardCount.value = 0
}

// 导入到流程
async function importToProcess() {
  if (!targetProcessId.value || !scriptPath.value) return

  try {
    const response = await api.post('/import', {
      scriptPath: scriptPath.value,
      processId: targetProcessId.value
    })

    if (response.data.success) {
      ElMessage.success('已成功导入到流程设计器')
    } else {
      ElMessage.error('导入失败: ' + response.data.message)
    }
  } catch (error) {
    ElMessage.error('导入失败: ' + error.message)
  }
}

// 时长计时器
let durationTimer = null
function startDurationTimer() {
  durationTimer = setInterval(() => {
    if (recordingStatus.value === 'RECORDING') {
      duration.value++
    }
  }, 1000)
}

// 加载流程列表
async function loadProcesses() {
  try {
    const response = await axios.get('/api/process/list')
    if (response.data.success) {
      processes.value = response.data.data
    }
  } catch (error) {
    console.error('加载流程列表失败', error)
  }
}

onMounted(() => {
  loadProcesses()
})
</script>

<style scoped>
.recorder-container {
  padding: 20px;
}

.recorder-card {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
}

.mode-selector {
  margin-bottom: 20px;
  text-align: center;
}

.action-buttons {
  text-align: center;
  margin-bottom: 20px;
}

.stats-panel {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.screenshot-area {
  text-align: center;
  margin-bottom: 20px;
}

.screenshot-area .el-button {
  margin: 0 5px;
}

.operations-list {
  margin-bottom: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: bold;
}

.import-section {
  margin-top: 20px;
}

.import-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.import-actions .el-select {
  width: 300px;
}
</style>