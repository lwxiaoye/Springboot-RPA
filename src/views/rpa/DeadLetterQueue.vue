<template>
  <div class="dead-letter-page">
    <div class="page-header">
      <h2>{{ t('dlq.title') }}</h2>
      <p class="page-desc">{{ t('dlq.subtitle') }}</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-box">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">{{ t('dlq.pending') }}</span>
      </div>
      <div class="stat-box warning">
        <span class="stat-num">{{ stats.retrying }}</span>
        <span class="stat-label">{{ t('dlq.retrying') }}</span>
      </div>
      <div class="stat-box danger">
        <span class="stat-num">{{ stats.resolved }}</span>
        <span class="stat-label">{{ t('dlq.resolved') }}</span>
      </div>
      <div class="stat-box info">
        <span class="stat-num">{{ stats.analyzing }}</span>
        <span class="stat-label">{{ t('dlq.analyzing') }}</span>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" :placeholder="t('dlq.searchPlaceholder')" />
      </div>
      <el-select v-model="statusFilter" :placeholder="t('dlq.statusFilter')" clearable style="width: 140px;">
        <el-option :label="t('dlq.pending')" value="pending" />
        <el-option :label="t('dlq.analyzing')" value="analysing" />
        <el-option :label="t('dlq.resolved')" value="resolved" />
        <el-option :label="t('dlq.manuallyClosed')" value="manually_closed" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" :range-separator="t('dlq.to')" :start-placeholder="t('dlq.startDate')" :end-placeholder="t('dlq.endDate')" style="width: 260px;" />
      <el-button type="primary" @click="loadDeadLetters">
        <el-icon><Search /></el-icon> {{ t('dlq.query') }}
      </el-button>
    </div>

    <!-- 死信队列列表 -->
    <el-table :data="paginatedData" v-loading="loading" border stripe class="unified-table" :default-sort="{ prop: 'createTime', order: 'descending' }">
      <el-table-column type="index" :label="t('dlq.seq')" width="60" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="taskName" :label="t('dlq.taskName')" min-width="180" show-overflow-tooltip />
      <el-table-column prop="processName" :label="t('dlq.process')" min-width="140" />
      <el-table-column prop="errorCode" :label="t('dlq.errorCode')" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="danger" size="small">{{ row.errorCode || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="retryCount" :label="t('dlq.retryCount')" width="90" align="center">
        <template #default="{ row }">
          <span :class="row.retryCount >= row.maxRetry ? 'text-danger' : 'text-warning'">
            {{ row.retryCount }}/{{ row.maxRetry }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="errorMessage" :label="t('dlq.errorMessage')" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" :label="t('task.status')" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="t('dlq.createTime')" min-width="160" />
      <el-table-column :label="t('common.actions')" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="primary" @click="viewDetail(row)" class="action-btn">{{ t('dlq.detail') }}</el-button>
            <el-button link type="success" @click="retryTask(row)" :disabled="row.status !== 'pending'" class="action-btn">{{ t('dlq.retry') }}</el-button>
            <el-popconfirm
              :title="t('dlq.confirmSkip')"
              :confirmButtonText="t('dlq.confirmSkipBtn')"
              :cancelButtonText="t('common.cancel')"
              @confirm="skipTask(row)"
            >
              <template #reference>
                <el-button link type="danger" :disabled="row.status !== 'pending'" class="action-btn">{{ t('dlq.skip') }}</el-button>
              </template>
            </el-popconfirm>
          </div>
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
    <el-dialog v-model="detailVisible" :title="t('dlq.detailTitle')" width="800px">
      <div class="detail-content" v-if="currentItem">
        <div class="detail-section">
          <h4>{{ t('dlq.basicInfo') }}</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>{{ t('dlq.taskName') }}:</label>
              <span>{{ currentItem.taskName }}</span>
            </div>
            <div class="detail-item">
              <label>{{ t('dlq.processName') }}:</label>
              <span>{{ currentItem.processName }}</span>
            </div>
            <div class="detail-item">
              <label>{{ t('dlq.robot') }}:</label>
              <span>{{ currentItem.robotName || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>{{ t('dlq.createTime') }}:</label>
              <span>{{ currentItem.createTime }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4>{{ t('dlq.errorInfo') }}</h4>
          <div class="error-info">
            <div class="error-row">
              <span class="error-label">{{ t('dlq.errorCode') }}:</span>
              <el-tag type="danger">{{ currentItem.errorCode || '-' }}</el-tag>
            </div>
            <div class="error-row">
              <span class="error-label">{{ t('dlq.errorMessage') }}:</span>
              <div class="error-message">{{ currentItem.errorMessage }}</div>
            </div>
            <div class="error-row">
              <span class="error-label">{{ t('dlq.errorStack') }}:</span>
              <pre class="error-stack">{{ currentItem.errorStack || t('dlq.noStack') }}</pre>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="currentItem.status === 'resolved'">
          <h4>{{ t('dlq.resolutionInfo') }}</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>{{ t('dlq.resolutionType') }}:</label>
              <span>{{ getResolutionText(currentItem.resolutionType) }}</span>
            </div>
            <div class="detail-item">
              <label>{{ t('dlq.handler') }}:</label>
              <span>{{ currentItem.resolvedBy }}</span>
            </div>
            <div class="detail-item">
              <label>{{ t('dlq.resolvedTime') }}:</label>
              <span>{{ currentItem.resolvedAt }}</span>
            </div>
            <div class="detail-item full">
              <label>{{ t('dlq.resolutionComment') }}:</label>
              <span>{{ currentItem.resolutionComment }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer v-if="currentItem && currentItem.status === 'pending'">
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
        <el-button type="warning" @click="analyzeTask(currentItem)">{{ t('dlq.analyze') }}</el-button>
        <el-button type="primary" @click="retryTask(currentItem)">{{ t('dlq.retry') }}</el-button>
        <el-button type="danger" @click="skipTask(currentItem)">{{ t('dlq.skip') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { apiGet, apiPost } from '../../utils/api.js'

const { t } = useI18n()

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
  const map = { pending: t('dlq.pending'), analysing: t('dlq.analyzing'), resolved: t('dlq.resolved'), manually_closed: t('dlq.manuallyClosed') }
  return map[status] || status
}

const getResolutionText = (type) => {
  const map = { retry: t('dlq.retry'), skip: t('dlq.skip'), manual_fix: t('dlq.manualFix') }
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
    console.error(t('dlq.loadFailed') + ':', e)
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
    await ElMessageBox.confirm(t('dlq.confirmRetry'), t('dlq.confirmRetryTitle'), { type: 'warning' })
    const result = await apiPost(`/dead-letter-queue/${item.id}/resolve`, {
      resolutionType: 'retry',
      comment: '手动重试'
    })
    if (result.code === 0) {
      ElMessage.success(t('dlq.taskResubmitted'))
      loadDeadLetters()
      detailVisible.value = false
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(t('dlq.operationFailed'))
    }
  }
}

const skipTask = async (item) => {
  try {
    await ElMessageBox.confirm(t('dlq.confirmSkipWarning'), t('dlq.confirmSkipTitle'), { type: 'warning' })
    const result = await apiPost(`/dead-letter-queue/${item.id}/resolve`, {
      resolutionType: 'skip',
      comment: '手动跳过'
    })
    if (result.code === 0) {
      ElMessage.success(t('dlq.taskSkipped'))
      loadDeadLetters()
      detailVisible.value = false
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(t('dlq.operationFailed'))
    }
  }
}

const analyzeTask = async (item) => {
  currentItem.value.status = 'analysing'
  ElMessage.info(t('dlq.analyzingError'))
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

/* 统一表格样式 */
:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color, #e5e7eb);
}

:deep(.el-table .el-table__header-wrapper th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  padding: 12px 0;
  border-bottom: 2px solid #e5e7eb;
}

:deep(.el-table .el-table__body-wrapper td) {
  padding: 10px 0;
  vertical-align: middle;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

:deep(.el-table .el-table__header-wrapper th .cell),
:deep(.el-table .el-table__body-wrapper td .cell) {
  padding: 0 6px;
}

:deep(.el-table .el-table__row) {
  transition: all 0.2s ease;
}

:deep(.el-table .el-table__row:hover > td) {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 序号单元格动画 */
.index-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  height: 100%;
  padding: 4px 0;
}

.index-number {
  font-weight: 700;
  font-size: 14px;
  color: var(--text-primary, #1f2937);
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.index-line {
  width: 2px;
  height: 0;
  background: linear-gradient(180deg, #00d4ff, #0077ff);
  border-radius: 1px;
  transition: height 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.6;
}

:deep(.el-table .el-table__row:hover .index-line) {
  height: 12px;
  opacity: 1;
}

:deep(.el-table .el-table__row:hover .index-number) {
  transform: scale(1.1);
  transition: transform 0.3s ease;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.2s ease;
  font-size: 13px;
}

.action-btn:hover {
  background: rgba(64, 158, 255, 0.1);
  transform: translateY(-1px);
}
</style>
