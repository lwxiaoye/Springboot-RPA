<template>
  <div class="robot-detail-page">
    <div class="page-header">
      <div class="header-content">
        <h2>{{ robot.name || '加载中...' }} - 详情</h2>
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
        <el-icon><Back /></el-icon> 返回列表
      </el-button>
    </div>

    <!-- 基本信息卡片 -->
    <el-card class="info-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><InfoFilled /></el-icon> 机器人基本信息</span>
          <el-button type="primary" size="small" @click="loadRobotDetail">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="机器人名称">{{ robot.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="机器人分类">
          <el-tag :type="getCategoryType(robot.robotCategory)" size="small">
            {{ getCategoryText(robot.robotCategory) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ robot.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="主机名">{{ robot.hostname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="端口">{{ robot.port || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="getStatusType(robot.status)" size="small">
            {{ getStatusText(robot.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="CPU使用率">
          <el-progress :percentage="robot.cpuUsage || 0" :color="getProgressColor(robot.cpuUsage)" :stroke-width="8" style="width: 120px;" />
        </el-descriptions-item>
        <el-descriptions-item label="内存使用率">
          <el-progress :percentage="robot.memoryUsage || 0" :color="getProgressColor(robot.memoryUsage)" :stroke-width="8" style="width: 120px;" />
        </el-descriptions-item>
        <el-descriptions-item label="最后心跳时间">{{ formatTime(robot.lastHeartbeat) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(robot.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="绑定的流程" :span="2">
          <el-tag v-if="robot.boundProcessName" type="info">{{ robot.boundProcessName }}</el-tag>
          <span v-else class="text-muted">未绑定（由流程仓库进行绑定）</span>
        </el-descriptions-item>
        <el-descriptions-item label="能力描述" :span="2">
          {{ robot.capabilities || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="机器人描述" :span="2">
          {{ robot.description || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 流程执行统计卡片 -->
    <el-card class="info-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Operation /></el-icon> 流程执行统计</span>
          <el-button type="primary" size="small" @click="refreshStats">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      
      <div class="process-stats">
        <div class="stat-item success">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.collectSuccess }}</div>
            <div class="stat-label">采集成功</div>
          </div>
        </div>
        <div class="stat-item danger">
          <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.collectFailed }}</div>
            <div class="stat-label">采集失败</div>
          </div>
        </div>
        <div class="stat-item success">
          <div class="stat-icon"><el-icon><SuccessFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.parseSuccess }}</div>
            <div class="stat-label">解析成功</div>
          </div>
        </div>
        <div class="stat-item danger">
          <div class="stat-icon"><el-icon><CloseBold /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.parseFailed }}</div>
            <div class="stat-label">解析失败</div>
          </div>
        </div>
        <div class="stat-item success">
          <div class="stat-icon"><el-icon><Select /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.processSuccess }}</div>
            <div class="stat-label">加工成功</div>
          </div>
        </div>
        <div class="stat-item danger">
          <div class="stat-icon"><el-icon><Delete /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.processFailed }}</div>
            <div class="stat-label">加工失败</div>
          </div>
        </div>
      </div>

      <!-- 执行记录表格 -->
      <el-divider content-position="left">最近执行记录</el-divider>
      <el-table :data="executionLogs" v-loading="loading" border stripe size="small" max-height="300">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="action" label="执行动作" width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'success' ? '成功' : row.status === 'failed' ? '失败' : '运行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
        <el-table-column prop="duration" label="耗时" width="100" align="center" />
        <el-table-column prop="createTime" label="执行时间" width="160" />
      </el-table>
    </el-card>

    <!-- 机器人执行代码卡片 -->
    <el-card class="info-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Cpu /></el-icon> 机器人执行代码</span>
          <el-tag type="warning" size="small">可编辑</el-tag>
        </div>
      </template>
      
      <div v-if="!editingCode" class="code-display">
        <pre class="code-block">{{ robot.robotCode || '// 暂无执行代码，请在编辑模式下添加机器人的执行代码' }}</pre>
        <div class="code-actions">
          <el-button type="primary" @click="editCode">
            <el-icon><Edit /></el-icon> 编辑代码
          </el-button>
        </div>
      </div>
      
      <div v-else class="code-edit">
        <el-input
          v-model="codeForm"
          type="textarea"
          :rows="15"
          placeholder="请输入机器人执行代码..."
          class="code-textarea"
        />
        <div class="code-actions">
          <el-button @click="cancelEditCode">取消</el-button>
          <el-button type="primary" @click="saveCode" :loading="saveLoading">
            <el-icon><Check /></el-icon> 保存代码
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Back, InfoFilled, Operation, CircleCheck, CircleClose, 
  SuccessFilled, CloseBold, Select, Delete, Cpu, Edit, Check, Refresh
} from '@element-plus/icons-vue'
import { apiGet, apiPut } from '../../utils/api.js'

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
    'DATA_COLLECT': '数据采集',
    'DATA_PARSE': '数据解析',
    'DATA_PROCESS': '数据加工',
    'GENERAL': '通用执行'
  }
  return map[category] || category || '通用执行'
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
  const map = { idle: '空闲', busy: '忙碌', offline: '离线' }
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
      ElMessage.error(result.message || '获取机器人详情失败')
    }
  } catch {
    ElMessage.error('获取机器人详情失败')
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
  ElMessage.success('统计数据已刷新')
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
      ElMessage.success('代码保存成功')
      robot.value.robotCode = codeForm.value
      editingCode.value = false
    } else {
      ElMessage.error(result.message || '保存失败')
    }
  } catch {
    ElMessage.error('保存失败')
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
