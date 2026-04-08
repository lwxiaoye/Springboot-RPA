<template>
  <div class="dead-letter-page">
    <div class="page-header">
      <h2>死信队列</h2>
      <p class="page-desc">任务执行失败的记录，需要人工介入处理</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-box">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">待处理</span>
      </div>
      <div class="stat-box warning">
        <span class="stat-num">{{ stats.retrying }}</span>
        <span class="stat-label">重试中</span>
      </div>
      <div class="stat-box danger">
        <span class="stat-num">{{ stats.resolved }}</span>
        <span class="stat-label">已解决</span>
      </div>
      <div class="stat-box info">
        <span class="stat-num">{{ stats.analyzing }}</span>
        <span class="stat-label">分析中</span>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索任务名称/流程名称..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px;">
        <el-option label="待处理" value="pending" />
        <el-option label="分析中" value="analysing" />
        <el-option label="已解决" value="resolved" />
        <el-option label="人工关闭" value="manually_closed" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px;" />
      <el-button type="primary" @click="loadDeadLetters">
        <el-icon><Search /></el-icon> 查询
      </el-button>
    </div>

    <!-- 死信队列列表 -->
    <el-table :data="paginatedData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="processName" label="流程" min-width="140" />
      <el-table-column prop="errorCode" label="错误码" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="danger" size="small">{{ row.errorCode || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="retryCount" label="重试次数" width="90" align="center">
        <template #default="{ row }">
          <span :class="row.retryCount >= row.maxRetry ? 'text-danger' : 'text-warning'">
            {{ row.retryCount }}/{{ row.maxRetry }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="primary" @click="retryTask(row)" :disabled="row.status !== 'pending'">重试</el-button>
          <el-button link type="danger" @click="skipTask(row)" :disabled="row.status !== 'pending'">跳过</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="死信详情" width="800px">
      <div class="detail-content" v-if="currentItem">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>任务名称:</label>
              <span>{{ currentItem.taskName }}</span>
            </div>
            <div class="detail-item">
              <label>流程名称:</label>
              <span>{{ currentItem.processName }}</span>
            </div>
            <div class="detail-item">
              <label>机器人:</label>
              <span>{{ currentItem.robotName || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>创建时间:</label>
              <span>{{ currentItem.createTime }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4>错误信息</h4>
          <div class="error-info">
            <div class="error-row">
              <span class="error-label">错误码:</span>
              <el-tag type="danger">{{ currentItem.errorCode || '-' }}</el-tag>
            </div>
            <div class="error-row">
              <span class="error-label">错误信息:</span>
              <div class="error-message">{{ currentItem.errorMessage }}</div>
            </div>
            <div class="error-row">
              <span class="error-label">错误堆栈:</span>
              <pre class="error-stack">{{ currentItem.errorStack || '无' }}</pre>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="currentItem.status === 'resolved'">
          <h4>解决信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>解决方式:</label>
              <span>{{ getResolutionText(currentItem.resolutionType) }}</span>
            </div>
            <div class="detail-item">
              <label>处理人:</label>
              <span>{{ currentItem.resolvedBy }}</span>
            </div>
            <div class="detail-item">
              <label>解决时间:</label>
              <span>{{ currentItem.resolvedAt }}</span>
            </div>
            <div class="detail-item full">
              <label>解决说明:</label>
              <span>{{ currentItem.resolutionComment }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer v-if="currentItem && currentItem.status === 'pending'">
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="warning" @click="analyzeTask(currentItem)">分析</el-button>
        <el-button type="primary" @click="retryTask(currentItem)">重试</el-button>
        <el-button type="danger" @click="skipTask(currentItem)">跳过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../utils/api.js'

const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const dateRange = ref([])
const deadLetters = ref([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const detailVisible = ref(false)
const currentItem = ref(null)

const stats = reactive({
  total: 0,
  retrying: 0,
  resolved: 0,
  analyzing: 0
})

const filteredData = computed(() => {
  let data = deadLetters.value
  if (statusFilter.value) {
    data = data.filter(item => item.status === statusFilter.value)
  }
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    data = data.filter(item =>
      item.taskName?.toLowerCase().includes(keyword) ||
      item.processName?.toLowerCase().includes(keyword) ||
      item.errorMessage?.toLowerCase().includes(keyword)
    )
  }
  return data
})

const paginatedData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredData.value.slice(start, end)
})

const getStatusType = (status) => {
  const map = { pending: 'warning', analysing: 'info', resolved: 'success', manually_closed: '' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待处理', analysing: '分析中', resolved: '已解决', manually_closed: '人工关闭' }
  return map[status] || status
}

const getResolutionText = (type) => {
  const map = { retry: '重试', skip: '跳过', manual_fix: '人工修复' }
  return map[type] || '-'
}

const loadDeadLetters = async () => {
  loading.value = true
  try {
    const result = await apiGet('/dead-letter-queue')
    if (result.code === 0) {
      deadLetters.value = result.data || []
      pagination.total = deadLetters.value.length

      // 统计
      stats.total = deadLetters.value.filter(d => d.status === 'pending').length
      stats.retrying = deadLetters.value.filter(d => d.status === 'retrying').length
      stats.resolved = deadLetters.value.filter(d => d.status === 'resolved').length
      stats.analyzing = deadLetters.value.filter(d => d.status === 'analysing').length
    }
  } catch (e) {
    console.error('加载死信队列失败:', e)
    // 模拟数据
    deadLetters.value = [
      { id: 1, taskName: '发票采集任务', processName: '发票采集流程', errorCode: 'NET_TIMEOUT', errorMessage: '网络连接超时', retryCount: 3, maxRetry: 3, status: 'pending', robotName: 'Robot-01', createTime: '2026-04-07 10:30:00' },
      { id: 2, taskName: '数据同步任务', processName: '数据同步流程', errorCode: 'AUTH_FAILED', errorMessage: '认证失败', retryCount: 2, maxRetry: 3, status: 'pending', robotName: 'Robot-02', createTime: '2026-04-07 09:15:00' },
    ]
    pagination.total = deadLetters.value.length
    stats.total = 2
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  currentItem.value = { ...row }
  detailVisible.value = true
}

const retryTask = async (item) => {
  try {
    await ElMessageBox.confirm('确定要重试该任务吗？', '确认重试', { type: 'warning' })
    const result = await apiPost(`/dead-letter-queue/${item.id}/resolve`, {
      resolutionType: 'retry',
      comment: '手动重试'
    })
    if (result.code === 0) {
      ElMessage.success('任务已重新提交到队列')
      loadDeadLetters()
      detailVisible.value = false
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const skipTask = async (item) => {
  try {
    await ElMessageBox.confirm('确定要跳过该任务吗？此操作不可恢复。', '确认跳过', { type: 'warning' })
    const result = await apiPost(`/dead-letter-queue/${item.id}/resolve`, {
      resolutionType: 'skip',
      comment: '手动跳过'
    })
    if (result.code === 0) {
      ElMessage.success('任务已跳过')
      loadDeadLetters()
      detailVisible.value = false
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const analyzeTask = async (item) => {
  currentItem.value.status = 'analysing'
  ElMessage.info('正在分析错误原因...')
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
}

const handleCurrentChange = (page) => {
  pagination.page = page
}

onMounted(() => {
  loadDeadLetters()
})
</script>

<style scoped>
.dead-letter-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-box {
  background: white;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-num {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #1e3a4a;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
}

.stat-box.warning .stat-num { color: #e6a23c; }
.stat-box.danger .stat-num { color: #f56c6c; }
.stat-box.info .stat-num { color: #409eff; }

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
}

.search-box input {
  border: none;
  outline: none;
  width: 200px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-content { padding: 0 10px; }
.detail-section { margin-bottom: 20px; }
.detail-section h4 { font-size: 14px; color: #1e3a4a; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.detail-item { display: flex; gap: 8px; }
.detail-item.full { grid-column: span 2; }
.detail-item label { color: #8c8c8c; min-width: 80px; }
.detail-item span { color: #1e3a4a; }

.error-info { background: #fef0f0; padding: 16px; border-radius: 8px; }
.error-row { margin-bottom: 12px; }
.error-row:last-child { margin-bottom: 0; }
.error-label { color: #f56c6c; font-weight: 500; }
.error-message { margin-top: 4px; color: #1e3a4a; }
.error-stack { background: #f5f7fa; padding: 12px; border-radius: 4px; font-size: 12px; max-height: 200px; overflow: auto; margin-top: 8px; }

.text-danger { color: #f56c6c; font-weight: 600; }
.text-warning { color: #e6a23c; font-weight: 600; }
</style>
