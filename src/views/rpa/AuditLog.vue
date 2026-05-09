<template>
  <div class="audit-page">
    <div class="page-header">
      <h2>{{ t('audit.title') }}</h2>
      <p class="page-desc">{{ t('audit.subtitle') }}</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-box">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">{{ t('audit.totalOperations') }}</span>
      </div>
      <div class="stat-box warning">
        <span class="stat-num">{{ stats.today }}</span>
        <span class="stat-label">{{ t('audit.todayOperations') }}</span>
      </div>
      <div class="stat-box danger">
        <span class="stat-num">{{ stats.highRisk }}</span>
        <span class="stat-label">{{ t('audit.highRiskOperations') }}</span>
      </div>
      <div class="stat-box info">
        <span class="stat-num">{{ stats.logins }}</span>
        <span class="stat-label">{{ t('audit.loginCount') }}</span>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" :placeholder="t('audit.searchPlaceholder')" />
      </div>
      <el-select v-model="typeFilter" :placeholder="t('audit.operationType')" clearable style="width: 140px;">
        <el-option :label="t('audit.login')" value="login" />
        <el-option :label="t('audit.create')" value="create" />
        <el-option :label="t('audit.update')" value="update" />
        <el-option :label="t('audit.delete')" value="delete" />
        <el-option :label="t('audit.export')" value="export" />
      </el-select>
      <el-select v-model="riskFilter" :placeholder="t('audit.riskLevel')" clearable style="width: 120px;">
        <el-option :label="t('audit.highRisk')" value="high" />
        <el-option :label="t('audit.mediumRisk')" value="medium" />
        <el-option :label="t('audit.lowRisk')" value="low" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" :range-separator="t('common.to')" :start-placeholder="t('common.startDate')" :end-placeholder="t('common.endDate')" style="width: 260px;" />
      <el-button @click="exportLogs"><el-icon><Download /></el-icon> {{ t('audit.exportReport') }}</el-button>
    </div>

    <el-table :data="paginatedLogs" v-loading="loading" border stripe class="unified-table" :default-sort="{ prop: 'time', order: 'descending' }">
      <el-table-column type="index" :label="t('audit.seq')" width="60" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="time" :label="t('audit.operationTime')" min-width="160" />
      <el-table-column prop="user" :label="t('audit.operationUser')" width="120" />
      <el-table-column prop="ip" :label="t('audit.ipAddress')" width="140" />
      <el-table-column prop="type" :label="t('audit.operationType')" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="module" :label="t('audit.module')" width="100" align="center">
        <template #default="{ row }">
          <span>{{ getModuleText(row.module) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" :label="t('audit.operationContent')" min-width="250" show-overflow-tooltip />
      <el-table-column prop="risk" :label="t('audit.risk')" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getRiskTag(row.risk)" size="small">{{ getRiskText(row.risk) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="100" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="primary" @click="viewDetail(row)" class="action-btn">{{ t('audit.detail') }}</el-button>
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
    <el-dialog v-model="detailVisible" :title="t('audit.operationDetail')" width="600px">
      <div class="detail-content" v-if="currentLog">
        <div class="detail-row">
          <label>{{ t('audit.operationTime') }}:</label>
          <span>{{ currentLog.time }}</span>
        </div>
        <div class="detail-row">
          <label>{{ t('audit.operationUser') }}:</label>
          <span>{{ currentLog.user }}</span>
        </div>
        <div class="detail-row">
          <label>{{ t('audit.userRole') }}:</label>
          <span>{{ currentLog.role }}</span>
        </div>
        <div class="detail-row">
          <label>{{ t('audit.ipAddress') }}:</label>
          <span>{{ currentLog.ip }}</span>
        </div>
        <div class="detail-row">
          <label>{{ t('audit.operationType') }}:</label>
          <el-tag :type="getTypeTag(currentLog.type)">{{ getTypeText(currentLog.type) }}</el-tag>
        </div>
        <div class="detail-row">
          <label>{{ t('audit.module') }}:</label>
          <span>{{ getModuleText(currentLog.module) }}</span>
        </div>
        <div class="detail-row">
          <label>{{ t('audit.riskLevel') }}:</label>
          <el-tag :type="getRiskTag(currentLog.risk)">{{ getRiskText(currentLog.risk) }}</el-tag>
        </div>
        <div class="detail-row full">
          <label>{{ t('audit.operationContent') }}:</label>
          <pre>{{ currentLog.content }}</pre>
        </div>
        <div class="detail-row full" v-if="currentLog.oldValue || currentLog.newValue">
          <label>{{ t('audit.changeDetail') }}:</label>
          <div class="change-detail">
            <div v-if="currentLog.oldValue">
              <span class="change-label">{{ t('audit.beforeChange') }}:</span>
              <pre>{{ currentLog.oldValue }}</pre>
            </div>
            <div v-if="currentLog.newValue">
              <span class="change-label">{{ t('audit.afterChange') }}:</span>
              <pre>{{ currentLog.newValue }}</pre>
            </div>
          </div>
        </div>
        <div class="detail-row full" v-if="currentLog.result">
          <label>{{ t('audit.executionResult') }}:</label>
          <span :class="currentLog.result === 'success' ? 'text-success' : 'text-danger'">
            {{ currentLog.result === 'success' ? t('audit.success') : t('audit.failed') }}
          </span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import { apiGet } from '../../utils/api.js'

const { t } = useI18n()

const loading = ref(false)
const searchKeyword = ref('')
const typeFilter = ref('')
const riskFilter = ref('')
const dateRange = ref([])
const detailVisible = ref(false)
const currentLog = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const stats = reactive({ total: 0, today: 0, highRisk: 0, logins: 0 })

const logs = ref([])

const filteredLogs = computed(() => {
  let list = logs.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(l => 
      l.content.toLowerCase().includes(kw) || 
      l.user.toLowerCase().includes(kw) ||
      l.ip.includes(kw)
    )
  }
  if (typeFilter.value) {
    list = list.filter(l => l.type === typeFilter.value)
  }
  if (riskFilter.value) {
    list = list.filter(l => l.risk === riskFilter.value)
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

const getTypeTag = (type) => {
  const map = { login: 'info', create: 'success', update: 'warning', delete: 'danger', export: 'primary' }
  return map[type] || 'info'
}

const getTypeText = (type) => {
  const map = { login: t('audit.login'), create: t('audit.create'), update: t('audit.update'), delete: t('audit.delete'), export: t('audit.export') }
  return map[type] || type
}

const getModuleText = (module) => {
  const map = { 
    user: t('audit.userManagement'), 
    robot: t('audit.robot'), 
    process: t('audit.processManagement'), 
    task: t('audit.taskScheduling'),
    log: t('audit.logManagement'),
    system: t('audit.systemSettings')
  }
  return map[module] || module
}

const getRiskTag = (risk) => {
  const map = { high: 'danger', medium: 'warning', low: 'info' }
  return map[risk] || 'info'
}

const getRiskText = (risk) => {
  const map = { high: t('audit.highRisk'), medium: t('audit.mediumRisk'), low: t('audit.lowRisk') }
  return map[risk] || risk
}

const loadLogs = async () => {
  loading.value = true
  try {
    const result = await apiGet('/audit')
    if (result.code === 0) {
      logs.value = (result.data || []).map(log => ({
        id: log.id,
        time: log.createTime,
        user: log.userName || '-',
        role: '',
        ip: log.ip || '-',
        type: log.action || '-',
        module: log.module || '-',
        content: log.description || '-',
        risk: log.riskLevel || 'low',
        result: log.status || 'success',
        oldValue: log.requestParams,
        newValue: log.responseResult,
        detail: log
      }))
    } else {
      logs.value = []
    }
    stats.total = logs.value.length
    const today = new Date().toISOString().split('T')[0]
    stats.today = logs.value.filter(l => l.time && l.time.startsWith(today)).length
    stats.highRisk = logs.value.filter(l => l.risk === 'high').length
    stats.logins = logs.value.filter(l => l.type === 'login').length
  } catch (e) {
    console.error(t('audit.loadFailed'), e)
    logs.value = []
  } finally {
    loading.value = false
  }
}

const viewDetail = (log) => {
  currentLog.value = log
  detailVisible.value = true
}

const exportLogs = () => {
  // 获取要导出的数据（当前筛选后的所有数据）
  const exportData = filteredLogs.value.map(log => ({
    [t('audit.operationTime')]: log.time,
    [t('audit.operationUser')]: log.user,
    [t('audit.ipAddress')]: log.ip,
    [t('audit.operationType')]: getTypeText(log.type),
    [t('audit.module')]: getModuleText(log.module),
    [t('audit.operationContent')]: log.content,
    [t('audit.riskLevel')]: getRiskText(log.risk),
    [t('audit.requestParams')]: log.params || '',
    [t('audit.responseResult')]: log.result || ''
  }))

  // 生成CSV内容
  const headers = Object.keys(exportData[0] || {})
  const csvContent = [
    headers.join(','),
    ...exportData.map(row => headers.map(h => `"${(row[h] || '').toString().replace(/"/g, '""')}"`).join(','))
  ].join('\n')

  // 添加BOM以支持Excel正确显示中文
  const bom = '\uFEFF'
  const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = `${t('audit.auditLog')}_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  URL.revokeObjectURL(url)
  ElMessage.success(t('audit.exportSuccess', { count: exportData.length }))
}

const handleSizeChange = (size) => { 
  pagination.size = size
  pagination.page = 1 // 重置到第一页
}
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.audit-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-box {
  flex: 1;
  background: white;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
}

.stat-box .stat-num {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #1e3a4a;
}

.stat-box .stat-label {
  font-size: 13px;
  color: #8c8c8c;
}

.stat-box.warning { border-left: 4px solid #e6a23c; }
.stat-box.danger { border-left: 4px solid #f56c6c; }
.stat-box.info { border-left: 4px solid #409eff; }

.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; flex-wrap: wrap; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }

.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; background: var(--bg-secondary, #ffffff); padding: 12px 16px; border-radius: 10px; border: 1px solid var(--border-color, #e5e7eb); }

/* 统一表格样式 */
.unified-table :deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  padding: 12px 0;
  border-bottom: 2px solid #e5e7eb;
}
.unified-table :deep(.el-table__body-wrapper td) {
  padding: 10px 0;
  vertical-align: middle;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}
.unified-table :deep(.el-table__header-wrapper th .cell),
.unified-table :deep(.el-table__body-wrapper td .cell) {
  padding: 0 6px;
}
.unified-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}
.unified-table :deep(.el-table__row:hover > td) {
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

.unified-table :deep(.el-table__row:hover .index-line) {
  height: 12px;
  opacity: 1;
}

.unified-table :deep(.el-table__row:hover .index-number) {
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

.detail-content { padding: 8px 0; }
.detail-row { display: flex; margin-bottom: 12px; align-items: center; }
.detail-row label { width: 90px; color: #8c8c8c; flex-shrink: 0; }
.detail-row span { color: #262626; font-weight: 500; }
.detail-row.full { flex-direction: column; align-items: flex-start; }
.detail-row.full label { margin-bottom: 8px; }
.detail-row pre { background: #f5f7fa; padding: 12px; border-radius: 4px; font-size: 13px; margin: 8px 0; width: 100%; overflow-x: auto; }
.change-detail { width: 100%; }
.change-label { font-weight: 500; color: #8c8c8c; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
</style>