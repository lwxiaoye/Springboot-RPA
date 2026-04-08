<template>
  <div class="recording-page">
    <div class="page-header">
      <h2>录屏管理</h2>
      <p class="page-desc">管理RPA执行过程的屏幕录制，满足金融行业合规审计要求</p>
    </div>

    <!-- 统计信息 -->
    <div class="stats-row">
      <div class="stat-box">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">录制总数</span>
      </div>
      <div class="stat-box primary">
        <span class="stat-num">{{ stats.recording }}</span>
        <span class="stat-label">录制中</span>
      </div>
      <div class="stat-box success">
        <span class="stat-num">{{ stats.completed }}</span>
        <span class="stat-label">已完成</span>
      </div>
      <div class="stat-box warning">
        <span class="stat-num">{{ formatSize(stats.storageUsed) }}</span>
        <span class="stat-label">存储使用</span>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索任务名称..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px;">
        <el-option label="录制中" value="RECORDING" />
        <el-option label="已暂停" value="PAUSED" />
        <el-option label="已完成" value="STOPPED" />
      </el-select>
      <el-button @click="refreshList">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>

    <!-- 活跃录制 -->
    <el-card v-if="activeSessions.length > 0" class="active-recording-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="live-indicator">
            <span class="dot"></span> 活跃录制
          </span>
          <el-tag type="danger">{{ activeSessions.length }} 个</el-tag>
        </div>
      </template>
      <div class="active-sessions">
        <div v-for="session in activeSessions" :key="session.sessionId" class="session-item">
          <div class="session-info">
            <span class="session-id">ID: {{ session.sessionId.substring(0, 8) }}...</span>
            <span class="session-robot">机器人: {{ session.robotId }}</span>
            <span class="session-task">任务: {{ session.taskId }}</span>
          </div>
          <div class="session-status">
            <el-tag :type="session.status === 'RECORDING' ? 'success' : 'warning'" size="small">
              {{ session.status }}
            </el-tag>
            <span class="session-duration">{{ formatDuration(session) }}</span>
            <span class="session-frames">{{ session.frameCount }} 帧</span>
          </div>
          <div class="session-actions">
            <el-button v-if="session.status === 'RECORDING'" size="small" @click="pauseRecording(session.sessionId)">
              暂停
            </el-button>
            <el-button v-if="session.status === 'PAUSED'" size="small" type="success" @click="resumeRecording(session.sessionId)">
              恢复
            </el-button>
            <el-button size="small" type="danger" @click="stopRecording(session.sessionId)">
              停止
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 录制列表 -->
    <el-table :data="paginatedRecordings" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="sessionId" label="录制ID" width="150">
        <template #default="{ row }">
          {{ row.sessionId.substring(0, 8) }}...
        </template>
      </el-table-column>
      <el-table-column prop="robotId" label="机器人" width="120" />
      <el-table-column prop="taskId" label="关联任务" width="120" />
      <el-table-column prop="startTime" label="开始时间" width="160">
        <template #default="{ row }">
          {{ formatDate(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="时长" width="100" align="center">
        <template #default="{ row }">
          {{ formatSeconds(row.duration || 0) }}
        </template>
      </el-table-column>
      <el-table-column prop="frameCount" label="帧数" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="videoPath" label="文件" min-width="150">
        <template #default="{ row }">
          <span v-if="row.videoPath">{{ row.videoPath.split('/').pop() }}</span>
          <span v-else class="text-muted">处理中...</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="playRecording(row)">播放</el-button>
          <el-button link type="primary" @click="downloadRecording(row)">下载</el-button>
          <el-popconfirm title="确认删除该录制吗？" @confirm="deleteRecording(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 开始录制对话框 -->
    <el-dialog v-model="startDialogVisible" title="开始录制" width="450px">
      <el-form :model="startForm" label-width="100px">
        <el-form-item label="机器人" required>
          <el-select v-model="startForm.robotId" placeholder="请选择机器人" style="width: 100%">
            <el-option v-for="robot in robots" :key="robot.id" :label="robot.name" :value="robot.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联任务">
          <el-select v-model="startForm.taskId" placeholder="请选择任务（可选）" clearable style="width: 100%">
            <el-option v-for="task in tasks" :key="task.id" :label="task.name" :value="task.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="屏幕">
          <el-select v-model="startForm.monitorIndex" style="width: 100%">
            <el-option label="主屏幕" :value="0" />
            <el-option label="屏幕 2" :value="1" />
            <el-option label="屏幕 3" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="帧率">
          <el-select v-model="startForm.fps" style="width: 100%">
            <el-option label="5 fps（低）" :value="5" />
            <el-option label="10 fps（标准）" :value="10" />
            <el-option label="15 fps（高清）" :value="15" />
            <el-option label="30 fps（流畅）" :value="30" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStartRecording" :loading="starting">开始录制</el-button>
      </template>
    </el-dialog>

    <!-- 播放对话框 -->
    <el-dialog v-model="playDialogVisible" title="录制回放" width="900px">
      <div class="player-container">
        <div v-if="currentRecording" class="player-info">
          <span>录制时间: {{ formatDate(currentRecording.startTime) }}</span>
          <span>时长: {{ formatSeconds(currentRecording.duration || 0) }}</span>
          <span>帧数: {{ currentRecording.frameCount }}</span>
        </div>
        <div class="player-placeholder">
          <el-icon size="60"><VideoPlay /></el-icon>
          <p>视频播放器</p>
          <p class="text-muted">录制文件将显示在这里</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, VideoPlay } from '@element-plus/icons-vue'

const apiBase = '/api'
const token = localStorage.getItem('token') || ''

const loading = ref(false)
const starting = ref(false)
const activeSessions = ref([])
const recordings = ref([])
const robots = ref([])
const tasks = ref([])
const startDialogVisible = ref(false)
const playDialogVisible = ref(false)
const currentRecording = ref(null)

const searchKeyword = ref('')
const statusFilter = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })
const stats = reactive({ total: 0, recording: 0, completed: 0, storageUsed: 0 })

const startForm = reactive({
  robotId: '',
  taskId: '',
  monitorIndex: 0,
  fps: 10
})

const fetchActiveSessions = async () => {
  try {
    const res = await fetch(apiBase + '/enterprise/recording/active', {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()
    if (data.code === 0) {
      activeSessions.value = data.data || []
      stats.recording = activeSessions.value.filter(s => s.status === 'RECORDING').length
    }
  } catch (e) {
    console.error('获取活跃会话失败', e)
  }
}

const fetchRecordings = async () => {
  loading.value = true
  try {
    // 模拟数据
    recordings.value = [
      { sessionId: 'rec-001', robotId: 'robot-001', taskId: 'task-001', startTime: Date.now() - 3600000, duration: 1800, frameCount: 18000, status: 'STOPPED', videoPath: '/recordings/rec-001.mp4' },
      { sessionId: 'rec-002', robotId: 'robot-002', taskId: 'task-002', startTime: Date.now() - 7200000, duration: 3600, frameCount: 36000, status: 'STOPPED', videoPath: '/recordings/rec-002.mp4' },
    ]
    pagination.total = recordings.value.length
    stats.total = recordings.value.length
    stats.completed = recordings.value.filter(r => r.status === 'STOPPED').length
    stats.storageUsed = recordings.value.length * 500 * 1024 * 1024 // 模拟
  } finally {
    loading.value = false
  }
}

const fetchRobots = async () => {
  robots.value = [
    { id: 'robot-001', name: '财务机器人' },
    { id: 'robot-002', name: 'HR机器人' },
  ]
}

const startRecording = async () => {
  startDialogVisible.value = true
}

const handleStartRecording = async () => {
  if (!startForm.robotId) {
    ElMessage.warning('请选择机器人')
    return
  }
  starting.value = true
  try {
    const res = await fetch(apiBase + '/enterprise/recording/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(startForm)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('开始录制')
      startDialogVisible.value = false
      fetchActiveSessions()
    } else {
      ElMessage.error(data.message || '启动失败')
    }
  } catch (e) {
    ElMessage.error('启动失败')
  } finally {
    starting.value = false
  }
}

const pauseRecording = async (sessionId) => {
  try {
    const res = await fetch(apiBase + `/enterprise/recording/pause/${sessionId}`, { method: 'POST' })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('已暂停')
      fetchActiveSessions()
    }
  } catch (e) {
    ElMessage.error('暂停失败')
  }
}

const resumeRecording = async (sessionId) => {
  try {
    const res = await fetch(apiBase + `/enterprise/recording/resume/${sessionId}`, { method: 'POST' })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('已恢复')
      fetchActiveSessions()
    }
  } catch (e) {
    ElMessage.error('恢复失败')
  }
}

const stopRecording = async (sessionId) => {
  try {
    const res = await fetch(apiBase + `/enterprise/recording/stop/${sessionId}`, { method: 'POST' })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('录制已停止')
      fetchActiveSessions()
      fetchRecordings()
    }
  } catch (e) {
    ElMessage.error('停止失败')
  }
}

const playRecording = (row) => {
  currentRecording.value = row
  playDialogVisible.value = true
}

const downloadRecording = (row) => {
  ElMessage.info('下载功能开发中')
}

const deleteRecording = (row) => {
  recordings.value = recordings.value.filter(r => r.sessionId !== row.sessionId)
  ElMessage.success('已删除')
}

const refreshList = () => {
  fetchActiveSessions()
  fetchRecordings()
}

const formatDate = (timestamp) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN')
}

const formatSeconds = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return h > 0 ? `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}` : `${m}:${s.toString().padStart(2, '0')}`
}

const formatDuration = (session) => {
  if (!session.startTime) return '0:00'
  const seconds = Math.floor((Date.now() - session.startTime) / 1000)
  return formatSeconds(seconds)
}

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getStatusType = (status) => {
  const types = { RECORDING: 'success', PAUSED: 'warning', STOPPED: 'info' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { RECORDING: '录制中', PAUSED: '已暂停', STOPPED: '已完成' }
  return texts[status] || status
}

const handleSizeChange = () => {}
const handleCurrentChange = () => {}

const paginatedRecordings = computed(() => {
  let result = recordings.value
  if (searchKeyword.value) {
    result = result.filter(r => r.robotId.includes(searchKeyword.value) || r.taskId.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    result = result.filter(r => r.status === statusFilter.value)
  }
  return result.slice((pagination.page - 1) * pagination.size, pagination.page * pagination.size)
})

let pollTimer = null

onMounted(() => {
  fetchActiveSessions()
  fetchRecordings()
  fetchRobots()
  
  // 每5秒刷新活跃会话
  pollTimer = setInterval(fetchActiveSessions, 5000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.recording-page {
  padding: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
}

.page-desc {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.stats-row {
  display: flex;
  gap: 20px;
  margin: 20px 0;
}

.stat-box {
  flex: 1;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  border: 1px solid #eee;
}

.stat-num {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  display: block;
  margin-top: 5px;
  color: #666;
  font-size: 13px;
}

.stat-box.primary .stat-num { color: #409eff; }
.stat-box.success .stat-num { color: #67c23a; }
.stat-box.warning .stat-num { color: #e6a23c; }

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.search-box {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 0 10px;
}

.search-box input {
  border: none;
  outline: none;
  padding: 8px;
  width: 200px;
}

.active-recording-card {
  margin-bottom: 20px;
  border: 2px solid #f56c6c;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.live-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

.dot {
  width: 10px;
  height: 10px;
  background: #f56c6c;
  border-radius: 50%;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.active-sessions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.session-info {
  display: flex;
  gap: 20px;
}

.session-info span {
  color: #666;
}

.session-id {
  font-family: monospace;
}

.session-status {
  display: flex;
  align-items: center;
  gap: 15px;
}

.session-actions {
  display: flex;
  gap: 10px;
}

.player-container {
  background: #000;
  height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.player-info {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 20px;
  background: rgba(0,0,0,0.5);
  padding: 10px 15px;
  border-radius: 4px;
  font-size: 13px;
}

.player-placeholder {
  text-align: center;
}

.text-muted {
  color: #999;
}
</style>