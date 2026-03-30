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
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px"
      />
    </div>

    <el-table :data="filteredLogs" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="robotName" label="执行机器人" min-width="120" />
      <el-table-column prop="action" label="操作" min-width="100" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
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
        <div class="detail-item"><label>状态：</label><el-tag :type="getStatusType(currentLog.status)" size="small">{{ getStatusText(currentLog.status) }}</el-tag></div>
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
import { Search } from '@element-plus/icons-vue'

import { apiGet } from '../../utils/api.js'

const loading = ref(false)
const logs = ref([])
const searchKeyword = ref('')
const statusFilter = ref('')
const dateRange = ref([])
const detailVisible = ref(false)
const currentLog = ref({})

const pagination = reactive({ page: 1, size: 10, total: 0 })

const getStatusText = (s) => {
  const map = { success: '成功', failed: '失败', running: '进行中' }
  return map[s] || s
}

const getStatusType = (s) => {
  const map = { success: 'success', failed: 'danger', running: 'warning' }
  return map[s] || 'info'
}

const filteredLogs = computed(() => {
  let list = logs.value
  if (searchKeyword.value) {
    list = list.filter(l => l.taskName?.includes(searchKeyword.value) || l.robotName?.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(l => l.status === statusFilter.value)
  }
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(l => {
      const time = new Date(l.startTime)
      return time >= start && time <= end
    })
  }
  return list
})

const loadLogs = async () => {
  loading.value = true
  try {
    const result = await apiGet('/log')
    if (result.code === 0) {
      logs.value = result.data || []
      pagination.total = logs.value.length
    }
  } catch {
    logs.value = []
    pagination.total = 0
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
</style>