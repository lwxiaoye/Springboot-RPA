<template>
  <div class="robot-detail-page">
    <div class="page-header">
      <div class="header-content">
        <h2>{{ robot.name || t('robot.loading') }} - {{ t('robot.detail') }}</h2>
        <div class="header-tags">
          <el-tag :type="getCategoryType(robot.robotCategory)" size="large">
            {{ getCategoryText(robot.robotCategory) }}
          </el-tag>
          <el-tag :type="getStatusType(robot.status)" size="large">
            {{ getStatusText(robot.status) }}
          </el-tag>
        </div>
      </div>
      <el-button @click="goBack">
        <el-icon><Back /></el-icon> {{ t('robot.backToList') }}
      </el-button>
    </div>

    <!-- 基本信息卡片 -->
    <el-card class="info-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><InfoFilled /></el-icon> {{ t('robot.basicInfo') }}</span>
          <el-button type="primary" size="small" @click="loadRobotDetail">
            <el-icon><Refresh /></el-icon> {{ t('common.refresh') }}
          </el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('robot.name')">{{ robot.name || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('robot.category')">
          <el-tag :type="getCategoryType(robot.robotCategory)" size="small">
            {{ getCategoryText(robot.robotCategory) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('robot.ip')">{{ robot.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('robot.hostname')">{{ robot.hostname || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('robot.port')">{{ robot.port || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('robot.currentStatus')">
          <el-tag :type="getStatusType(robot.status)" size="small">
            {{ getStatusText(robot.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('robot.cpuUsage')">
          <el-progress :percentage="robot.cpuUsage || 0" :color="getProgressColor(robot.cpuUsage)" :stroke-width="8" style="width: 120px;" />
        </el-descriptions-item>
        <el-descriptions-item :label="t('robot.memoryUsage')">
          <el-progress :percentage="robot.memoryUsage || 0" :color="getProgressColor(robot.memoryUsage)" :stroke-width="8" style="width: 120px;" />
        </el-descriptions-item>
        <el-descriptions-item :label="t('robot.lastHeartbeat')">{{ formatTime(robot.lastHeartbeat) }}</el-descriptions-item>
        <el-descriptions-item :label="t('robot.createTime')">{{ formatTime(robot.createTime) }}</el-descriptions-item>
        <el-descriptions-item :label="t('robot.boundProcess')" :span="2">
          <el-tag v-if="robot.boundProcessName" type="info">{{ robot.boundProcessName }}</el-tag>
          <span v-else class="text-muted">{{ t('robot.notBound') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('robot.capabilities')" :span="2">
          {{ robot.capabilities || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('robot.description')" :span="2">
          {{ robot.description || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 流程执行统计卡片 -->
    <el-card class="info-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Operation /></el-icon> {{ t('robot.processStats') }}</span>
          <el-button type="primary" size="small" @click="refreshStats">
            <el-icon><Refresh /></el-icon> {{ t('common.refresh') }}
          </el-button>
        </div>
      </template>
      
      <div class="process-stats">
        <div class="stat-item success">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.collectSuccess }}</div>
            <div class="stat-label">{{ t('robot.collectSuccess') }}</div>
          </div>
        </div>
        <div class="stat-item danger">
          <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.collectFailed }}</div>
            <div class="stat-label">{{ t('robot.collectFailed') }}</div>
          </div>
        </div>
        <div class="stat-item success">
          <div class="stat-icon"><el-icon><SuccessFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.parseSuccess }}</div>
            <div class="stat-label">{{ t('robot.parseSuccess') }}</div>
          </div>
        </div>
        <div class="stat-item danger">
          <div class="stat-icon"><el-icon><CloseBold /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.parseFailed }}</div>
            <div class="stat-label">{{ t('robot.parseFailed') }}</div>
          </div>
        </div>
        <div class="stat-item success">
          <div class="stat-icon"><el-icon><Select /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.processSuccess }}</div>
            <div class="stat-label">{{ t('robot.processSuccess') }}</div>
          </div>
        </div>
        <div class="stat-item danger">
          <div class="stat-icon"><el-icon><Delete /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.processFailed }}</div>
            <div class="stat-label">{{ t('robot.processFailed') }}</div>
          </div>
        </div>
      </div>

      <!-- 执行记录表格 -->
      <el-divider content-position="left">{{ t('robot.recentExecution') }}</el-divider>
      <el-table :data="executionLogs" v-loading="loading" border stripe size="small" max-height="300">
        <el-table-column type="index" :label="t('robot.seq')" width="60" align="center" />
        <el-table-column prop="taskName" :label="t('robot.taskName')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="action" :label="t('robot.action')" width="120" />
        <el-table-column prop="status" :label="t('task.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'success' ? t('task.statusSuccess') : row.status === 'failed' ? t('task.statusFailed') : t('task.statusRunning') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" :label="t('robot.message')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="duration" :label="t('robot.duration')" width="100" align="center" />
        <el-table-column prop="createTime" :label="t('robot.executeTime')" width="160" />
      </el-table>
    </el-card>

    <!-- 机器人执行代码卡片 -->
    <el-card class="info-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Cpu /></el-icon> {{ t('robot.executionCode') }}</span>
          <el-tag type="warning" size="small">{{ t('robot.editable') }}</el-tag>
        </div>
      </template>
      
      <div v-if="!editingCode" class="code-display">
        <pre class="code-block">{{ robot.robotCode || t('robot.noCode') }}</pre>
        <div class="code-actions">
          <el-button type="primary" @click="editCode">
            <el-icon><Edit /></el-icon> {{ t('robot.editCode') }}
          </el-button>
        </div>
      </div>
      
      <div v-else class="code-edit">
        <el-input
          v-model="codeForm"
          type="textarea"
          :rows="15"
          :placeholder="t('robot.enterCode')"
          class="code-textarea"
        />
        <div class="code-actions">
          <el-button @click="cancelEditCode">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" @click="saveCode" :loading="saveLoading">
            <el-icon><Check /></el-icon> {{ t('robot.saveCode') }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { 
  Back, InfoFilled, Operation, CircleCheck, CircleClose, 
  SuccessFilled, CloseBold, Select, Delete, Cpu, Edit, Check, Refresh
} from '@element-plus/icons-vue'
import { apiGet, apiPut } from '../../utils/api.js'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()

const robot = ref({})
const executionLogs = ref([])
const loading = ref(false)
const saveLoading = ref(false)
const editingCode = ref(false)
const codeForm = ref('')

const stats = reactive({
  collectSuccess: 0,
  collectFailed: 0,
  parseSuccess: 0,
  parseFailed: 0,
  processSuccess: 0,
  processFailed: 0
})

const getCategoryText = (category) => {
  const map = {
    'DATA_COLLECT': t('robot.categoryCollect'),
    'DATA_PARSE': t('robot.categoryParse'),
    'DATA_PROCESS': t('robot.categoryProcess'),
    'GENERAL': t('robot.categoryGeneral')
  }
  return map[category] || category || t('robot.categoryGeneral')
}

const getCategoryType = (category) => {
  const map = {
    'DATA_COLLECT': 'primary',
    'DATA_PARSE': 'success',
    'DATA_PROCESS': 'warning',
    'GENERAL': 'info'
  }
  return map[category] || 'info'
}

const getStatusText = (status) => {
  const map = { idle: t('robot.idle'), busy: t('robot.busy'), offline: t('robot.offline') }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { idle: 'success', busy: 'warning', offline: 'danger' }
  return map[status] || 'info'
}

const getProgressColor = (value) => {
  if (value >= 80) return '#f56c6c'
  if (value >= 60) return '#e6a23c'
  return '#67c23a'
}

const formatTime = (time) => {
  if (!time) return '-'
  return time
}

const goBack = () => {
  router.push('/rpa/robots')
}

const loadRobotDetail = async () => {
  loading.value = true
  try {
    const robotId = route.params.id
    const result = await apiGet(`/robot/${robotId}`)
    if (result.code === 0) {
      robot.value = result.data || {}
    } else {
      ElMessage.error(result.message || t('robot.loadFailed'))
    }
  } catch {
    ElMessage.error(t('robot.loadFailed'))
  } finally {
    loading.value = false
  }
}

const loadExecutionLogs = async () => {
  try {
    const robotId = route.params.id
    const result = await apiGet(`/log?robotId=${robotId}`)
    if (result.code === 0) {
      executionLogs.value = result.data || []
      calculateStats(result.data || [])
    } else {
      executionLogs.value = []
    }
  } catch {
    executionLogs.value = []
  }
}

const calculateStats = (logs) => {
  stats.collectSuccess = 0
  stats.collectFailed = 0
  stats.parseSuccess = 0
  stats.parseFailed = 0
  stats.processSuccess = 0
  stats.processFailed = 0
  
  logs.forEach(log => {
    const action = log.action || ''
    const status = log.status || ''
    
    if (action.includes('采集') || action.includes('Collect')) {
      if (status === 'success') stats.collectSuccess++
      else if (status === 'failed') stats.collectFailed++
    } else if (action.includes('解析') || action.includes('Parse')) {
      if (status === 'success') stats.parseSuccess++
      else if (status === 'failed') stats.parseFailed++
    } else if (action.includes('加工') || action.includes('Process')) {
      if (status === 'success') stats.processSuccess++
      else if (status === 'failed') stats.processFailed++
    }
  })
}

const refreshStats = () => {
  loadExecutionLogs()
  ElMessage.success(t('robot.statsRefreshed'))
}

const editCode = () => {
  codeForm.value = robot.value.robotCode || ''
  editingCode.value = true
}

const cancelEditCode = () => {
  editingCode.value = false
  codeForm.value = ''
}

const saveCode = async () => {
  saveLoading.value = true
  try {
    const result = await apiPut(`/robot/${robot.value.id}`, {
      robotCode: codeForm.value
    })
    if (result.code === 0) {
      ElMessage.success(t('robot.codeSaved'))
      robot.value.robotCode = codeForm.value
      editingCode.value = false
    } else {
      ElMessage.error(result.message || t('robot.saveFailed'))
    }
  } catch {
    ElMessage.error(t('robot.saveFailed'))
  } finally {
    saveLoading.value = false
  }
}

// 监听路由变化，当从列表页返回并重新进入时刷新数据
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadRobotDetail()
    loadExecutionLogs()
  }
})

onMounted(() => {
  loadRobotDetail()
  loadExecutionLogs()
})
</script>

<style scoped>
.robot-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.header-tags {
  display: flex;
  gap: 8px;
}

.info-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.text-muted {
  color: #909399;
  font-style: italic;
}

.process-stats {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid;
}

.stat-item.success {
  border-left-color: #67c23a;
}

.stat-item.danger {
  border-left-color: #f56c6c;
}

.stat-icon {
  font-size: 24px;
}

.stat-item.success .stat-icon {
  color: #67c23a;
}

.stat-item.danger .stat-icon {
  color: #f56c6c;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.code-display {
  position: relative;
}

.code-block {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 8px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.6;
  overflow-x: auto;
  margin: 0;
  max-height: 400px;
  overflow-y: auto;
}

.code-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.code-textarea {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.code-textarea :deep(textarea) {
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: inherit;
}

@media (max-width: 768px) {
  .process-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
