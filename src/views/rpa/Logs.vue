<template>
  <div class="logs-page">
    <div class="page-header">
      <h2>执行日志</h2>
      <p class="page-desc">查看RPA任务执行记录</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索任务名称/机器人..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="成功" value="success" />
        <el-option label="失败" value="failed" />
        <el-option label="进行中" value="running" />
        <el-option label="正常" value="正常" />
        <el-option label="异常" value="异常" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px"
      />
      <el-button type="success" @click="exportData">
        <el-icon><Download /></el-icon> 导出
      </el-button>
    </div>

    <el-table :data="paginatedLogs" v-loading="loading" border stripe class="unified-table">
      <el-table-column type="index" label="序号" width="60" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="action" label="操作" min-width="100" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(getDisplayStatus(row))" size="small">
            {{ getDisplayStatus(row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="采集数据" width="90" align="center">
        <template #default="{ row }">
          <span :class="parseDataCount(row) > 0 ? 'data-count-success' : 'data-count-zero'">
            {{ parseDataCount(row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="信息" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.message || '-' }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" min-width="160" />
      <el-table-column prop="endTime" label="结束时间" min-width="160">
        <template #default="{ row }">{{ row.endTime || '-' }}</template>
      </el-table-column>
      <el-table-column prop="duration" label="耗时" width="90" align="center" />
      <el-table-column label="操作" width="80" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="primary" @click="viewDetail(row)" class="action-btn">
              <span>详情</span>
            </el-button>
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
    <el-dialog v-model="detailVisible" title="日志详情" width="550px">
      <div class="detail-content">
        <div class="detail-item"><label>任务名称：</label><span>{{ currentLog.taskName }}</span></div>
        <div class="detail-item"><label>执行机器人：</label><span>{{ currentLog.robotName }}</span></div>
        <div class="detail-item"><label>操作：</label><span>{{ currentLog.action }}</span></div>
        <div class="detail-item"><label>状态：</label>
          <el-tag :type="getStatusType(getDisplayStatus(currentLog))" size="small">{{ getDisplayStatus(currentLog) }}</el-tag>
        </div>
        <div class="detail-item"><label>采集数据：</label><span :class="parseDataCount(currentLog) > 0 ? 'data-count-success' : 'data-count-zero'">{{ parseDataCount(currentLog) }} 条</span></div>
        <div class="detail-item"><label>开始时间：</label><span>{{ currentLog.startTime }}</span></div>
        <div class="detail-item"><label>结束时间：</label><span>{{ currentLog.endTime || '-' }}</span></div>
        <div class="detail-item"><label>耗时：</label><span>{{ currentLog.duration || '-' }}</span></div>
        <div class="detail-item full"><label>详细信息：</label><pre class="log-message">{{ currentLog.message || '-' }}</pre></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Download } from '@element-plus/icons-vue'
import { apiGet } from '../../utils/api.js'

const loading = ref(false)
const logs = ref([])
const searchKeyword = ref('')
const statusFilter = ref('')
const dateRange = ref([])
const detailVisible = ref(false)
const currentLog = ref({})

const pagination = reactive({ page: 1, size: 10, total: 0 })

const parseDataCount = (log) => {
  if (log.dataCount !== undefined && log.dataCount !== null) {
    return log.dataCount
  }
  if (log.message) {
    const match = log.message.match(/采集数据[:：]\s*(\d+)|处理数据[:：]\s*(\d+)/)
    if (match) {
      return parseInt(match[1] || match[2])
    }
  }
  return 0
}

const getDisplayStatus = (log) => {
  const dataCount = parseDataCount(log)
  if (dataCount > 0) {
    return '正常'
  }
  if (log.status === 'completed' || log.status === 'running') {
    if (log.message && (log.message.includes('完成') || log.message.includes('结束'))) {
      return '异常'
    }
  }
  return log.status
}

const getStatusType = (s) => {
  const map = {
    success: 'success',
    failed: 'danger',
    running: 'warning',
    completed: 'success',
    completed_with_errors: 'warning',
    abnormal: 'danger',
    pending: 'info',
    cancelled: 'info',
    正常: 'success',
    异常: 'danger'
  }
  return map[s] || 'info'
}

const filteredLogs = computed(() => {
  let list = logs.value
  if (searchKeyword.value) {
    list = list.filter(l => l.taskName?.includes(searchKeyword.value) || l.robotName?.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(l => {
      const displayStatus = getDisplayStatus(l)
      return displayStatus === statusFilter.value
    })
  }
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(l => {
      const time = new Date(l.startTime)
      return time >= start && time <= end
    })
  }
  pagination.total = list.length
  return list
})

const paginatedLogs = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredLogs.value.slice(start, end)
})

const loadLogs = async () => {
  loading.value = true
  try {
    const result = await apiGet('/log')
    if (result.code === 0) {
      logs.value = result.data || []
    }
  } catch {
    logs.value = []
  } finally {
    loading.value = false
  }
}

const viewDetail = (log) => {
  currentLog.value = log
  detailVisible.value = true
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

const exportData = async () => {
  try {
    const exportLogs = filteredLogs.value
    if (exportLogs.length === 0) {
      ElMessage.warning('没有可导出的数据')
      return
    }

    const headers = ['序号', '任务名称', '操作', '状态', '采集数据', '信息', '开始时间', '结束时间', '耗时']
    const rows = exportLogs.map((log, index) => [
      index + 1,
      log.taskName || '-',
      log.action || '-',
      getDisplayStatus(log),
      parseDataCount(log),
      (log.message || '-').replace(/[\n\r]/g, ' '),
      log.startTime || '-',
      log.endTime || '-',
      log.duration || '-'
    ])

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    ].join('\n')

    const BOM = '\uFEFF'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `执行日志_${new Date().toISOString().split('T')[0]}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    ElMessage.success(`已导出 ${exportLogs.length} 条记录`)
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(() => { loadLogs() })
</script>

<style scoped>
.logs-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.page-desc {
  font-size: 14px;
  color: var(--text-secondary, #6b7280);
  margin: 0;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: var(--bg-secondary, #ffffff);
  border-radius: 12px;
  border: 1px solid var(--border-color, #e5e7eb);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--bg-tertiary, #f9fafb);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 10px;
  flex: 1;
  max-width: 320px;
  transition: all 0.2s;
}

.search-box:focus-within {
  border-color: var(--primary, #409eff);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.search-box input {
  border: none;
  outline: none;
  flex: 1;
  background: transparent;
  font-size: 14px;
  color: var(--text-primary, #1f2937);
}

.search-box input::placeholder {
  color: var(--text-tertiary, #9ca3af);
}

/* 表格样式 */
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
  gap: 2px;
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

.data-count-success {
  color: var(--success, #67c23a);
  font-weight: 700;
}

.data-count-zero {
  color: var(--danger, #f56c6c);
  font-weight: 700;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  background: var(--bg-secondary, #ffffff);
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid var(--border-color, #e5e7eb);
}

.detail-content {
  padding: 0 16px;
}

.detail-item {
  display: flex;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item.full {
  flex-direction: column;
}

.detail-item.full label {
  margin-bottom: 8px;
}

.detail-item label {
  width: 100px;
  color: var(--text-secondary, #6b7280);
  font-weight: 500;
  flex-shrink: 0;
}

.detail-item span {
  color: var(--text-primary, #1f2937);
  font-weight: 600;
}

.log-message {
  background: var(--bg-tertiary, #f9fafb);
  padding: 12px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-primary, #1f2937);
  white-space: pre-wrap;
  word-break: break-all;
  margin: 8px 0 0 0;
}
</style>
