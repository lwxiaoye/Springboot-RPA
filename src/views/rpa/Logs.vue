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

    <el-table :data="paginatedLogs" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
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
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
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

// 从信息中解析采集数据数量
const parseDataCount = (log) => {
  // 如果有直接的 dataCount 字段，优先使用
  if (log.dataCount !== undefined && log.dataCount !== null) {
    return log.dataCount
  }
  // 从 message 中解析
  if (log.message) {
    // 匹配 "采集数据: 123 条" 或 "处理数据: 123 条"
    const match = log.message.match(/采集数据[:：]\s*(\d+)|处理数据[:：]\s*(\d+)/)
    if (match) {
      return parseInt(match[1] || match[2])
    }
  }
  return 0
}

// 根据采集数据判断显示状态
const getDisplayStatus = (log) => {
  const dataCount = parseDataCount(log)
  // 如果信息中有"条"数据，视为正常
  if (dataCount > 0) {
    return '正常'
  }
  // 如果执行完成但没有数据，视为异常
  if (log.status === 'completed' || log.status === 'running') {
    if (log.message && (log.message.includes('完成') || log.message.includes('结束'))) {
      return '异常'
    }
  }
  // 其他情况使用原始状态
  return log.status
}

const getStatusText = (s) => {
  const map = {
    success: '成功',
    failed: '失败',
    running: '进行中',
    completed: '正常',
    completed_with_errors: '部分成功',
    abnormal: '异常',
    pending: '待执行',
    cancelled: '已取消',
    正常: '正常',
    异常: '异常'
  }
  return map[s] || s || '-'
}

const getStatusType = (s) => {
  const map = {
    success: 'success',
    completed: 'success',
    正常: 'success',
    failed: 'danger',
    abnormal: 'danger',
    异常: 'danger',
    running: 'warning',
    pending: 'info',
    completed_with_errors: 'warning',
    cancelled: 'info'
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
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
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
      // 不需要在这里设置 pagination.total，filteredLogs 会计算
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

// 导出数据为Excel
const exportData = async () => {
  try {
    const exportLogs = filteredLogs.value
    if (exportLogs.length === 0) {
      ElMessage.warning('没有可导出的数据')
      return
    }

    // 构建CSV内容
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

    // 转CSV
    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    ].join('\n')

    // 下载文件
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
.logs-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; flex-wrap: wrap; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }
.detail-content { padding: 8px 0; }
.detail-item { display: flex; margin-bottom: 12px; }
.detail-item label { width: 80px; color: #8c8c8c; }
.detail-item span { color: #262626; font-weight: 500; }
.detail-item.full { flex-direction: column; }
.detail-item.full label { margin-bottom: 8px; }
.log-message { background: #f5f7fa; padding: 12px; border-radius: 8px; font-size: 13px; line-height: 1.5; margin: 0; white-space: pre-wrap; word-break: break-all; }
.data-count-success { color: #67c23a; font-weight: 600; }
.data-count-zero { color: #f56c6c; }
</style>