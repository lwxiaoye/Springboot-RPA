<template>
  <div class="invoice-data-container">
    <el-card class="header-card">
      <div class="header-content">
        <div class="header-left">
          <h2>
            <el-icon><Document /></el-icon>
            {{ t('invoice.title') }}
          </h2>
          <p class="subtitle">{{ t('invoice.subtitle') }}</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="handleCollect" :loading="collecting">
            <el-icon><Refresh /></el-icon>
            {{ t('invoice.collectData') }}
          </el-button>
          <el-button @click="loadData">
            <el-icon><Refresh /></el-icon>
            {{ t('invoice.refresh') }}
          </el-button>
          <el-button type="danger" @click="handleClear" :disabled="tableData.length === 0">
            <el-icon><Delete /></el-icon>
            {{ t('invoice.clear') }}
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-if="stats.totalCount > 0">
      <el-card class="stat-card total-count" shadow="hover">
        <div class="stat-card-inner">
          <div class="stat-icon-wrapper">
            <div class="stat-icon collect">
              <el-icon><Document /></el-icon>
            </div>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalCount }}</div>
            <div class="stat-label">{{ t('invoice.totalInvoices') }}</div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon>
              <span>{{ t('invoice.allValid') }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card normal-count" shadow="hover">
        <div class="stat-card-inner">
          <div class="stat-icon-wrapper">
            <div class="stat-icon amount">
              <el-icon><Money /></el-icon>
            </div>
          </div>
          <div class="stat-content">
            <div class="stat-value success">¥ {{ formatNumber(stats.taxExclusiveTotal) }}</div>
            <div class="stat-label">{{ t('invoice.taxExclusiveTotal') }}</div>
            <div class="stat-sub">{{ t('invoice.taxExclusiveTotalSub') }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card tax-count" shadow="hover">
        <div class="stat-card-inner">
          <div class="stat-icon-wrapper">
            <div class="stat-icon tax">
              <el-icon><Coin /></el-icon>
            </div>
          </div>
          <div class="stat-content">
            <div class="stat-value warning">¥ {{ formatNumber(stats.taxTotal) }}</div>
            <div class="stat-label">{{ t('invoice.taxTotal') }}</div>
            <div class="stat-sub">{{ t('invoice.taxTotalSub') }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card amount-total" shadow="hover">
        <div class="stat-card-inner">
          <div class="stat-icon-wrapper highlight">
            <div class="stat-icon total">
              <el-icon><TrendCharts /></el-icon>
            </div>
          </div>
          <div class="stat-content">
            <div class="stat-value highlight">¥ {{ formatNumber(stats.amountTotal) }}</div>
            <div class="stat-label">{{ t('invoice.totalAmount') }}</div>
            <div class="stat-trend">
              <el-progress :percentage="stats.totalCount > 0 ? Math.round((stats.normalCount / stats.totalCount) * 100) : 0" :stroke-width="4" :show-text="false" size="small" />
              <span class="trend-text">{{ stats.normalCount }} {{ t('invoice.normalCount') }} / {{ stats.totalCount - stats.normalCount }} {{ t('invoice.voidedCount') }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 企业信息 -->
    <el-card class="company-card" v-if="companyInfo">
      <template #header>
        <div class="card-header">
          <span>
            <el-icon><OfficeBuilding /></el-icon>
            {{ t('invoice.companyInfo') }}
          </span>
        </div>
      </template>
      <div class="company-grid">
        <div class="company-item">
          <span class="label">{{ t('invoice.taxNo') }}：</span>
          <span class="value code">{{ companyInfo.taxNo }}</span>
        </div>
        <div class="company-item">
          <span class="label">{{ t('invoice.creditCode') }}：</span>
          <span class="value code">{{ companyInfo.creditCode }}</span>
        </div>
        <div class="company-item">
          <span class="label">{{ t('invoice.companyName') }}：</span>
          <span class="value">{{ companyInfo.companyName }}</span>
        </div>
        <div class="company-item">
          <span class="label">{{ t('invoice.companyType') }}：</span>
          <el-tag type="info">{{ companyInfo.companyType }}</el-tag>
        </div>
        <div class="company-item">
          <span class="label">{{ t('invoice.applyDate') }}：</span>
          <span class="value">{{ companyInfo.applyDate }}</span>
        </div>
      </div>
    </el-card>

    <!-- 发票明细表格 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>
            <el-icon><List /></el-icon>
            {{ t('invoice.invoiceDetail') }}
            <el-tag type="primary" size="small">{{ tableData.length }} {{ t('invoice.records') }}</el-tag>
          </span>
        </div>
      </template>

      <el-table :data="tableData" stripe border :max-height="600" v-loading="loading">
        <el-table-column type="index" width="60" label="#" align="center" />
        <el-table-column prop="invoiceNo" :label="t('invoice.invoiceNo')" width="140" align="center">
          <template #default="{ row }">
            <span class="invoice-no">{{ row.invoiceNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceType" :label="t('invoice.invoiceType')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.invoiceType === '销项' ? 'success' : 'warning'" size="small">
              {{ row.invoiceType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceStatus" :label="t('invoice.invoiceStatus')" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.invoiceStatus)" size="small">
              {{ row.invoiceStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="companyName" :label="t('invoice.companyName')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="creditCode" :label="t('invoice.creditCode')" width="200" align="center">
          <template #default="{ row }">
            <span class="code">{{ row.creditCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceDate" :label="t('invoice.invoiceDate')" width="120" align="center" />
        <el-table-column prop="taxExclusiveAmount" :label="t('invoice.taxExclusiveAmount')" width="140" align="right">
          <template #default="{ row }">
            <span class="amount">¥ {{ formatNumber(row.taxExclusiveAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="taxAmount" :label="t('invoice.taxAmount')" width="120" align="right">
          <template #default="{ row }">
            <span class="amount tax">¥ {{ formatNumber(row.taxAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" :label="t('invoice.totalAmount')" width="140" align="right">
          <template #default="{ row }">
            <span class="amount total">¥ {{ formatNumber(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="collectTime" :label="t('invoice.collectTime')" width="160" align="center">
          <template #default="{ row }">
            {{ formatDate(row.collectTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-if="!loading && tableData.length === 0" :description="t('invoice.noData')">
      <el-button type="primary" @click="handleCollect">
        <el-icon><Refresh /></el-icon>
        {{ t('invoice.collectNow') }}
      </el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, Refresh, Delete, List, Money, Coin,
  TrendCharts, OfficeBuilding, Top
} from '@element-plus/icons-vue'

import { apiGet, apiPost, apiDelete } from '../../utils/api.js'

const { t } = useI18n()

const loading = ref(false)
const collecting = ref(false)
const tableData = ref([])
const companyInfo = ref(null)
const stats = reactive({
  totalCount: 0,
  taxExclusiveTotal: 0,
  taxTotal: 0,
  amountTotal: 0,
  normalCount: 0
})

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleString('zh-CN')
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    '正常': 'success',
    '作废': 'danger',
    '红冲': 'warning'
  }
  return typeMap[status] || 'info'
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const result = await apiGet('/invoice')
    if (result.code === 0) {
      tableData.value = result.data || []
      stats.totalCount = tableData.value.length
      if (stats.totalCount > 0) {
        stats.taxExclusiveTotal = tableData.value.reduce((sum, item) => sum + (item.taxExclusiveAmount || 0), 0)
        stats.taxTotal = tableData.value.reduce((sum, item) => sum + (item.taxAmount || 0), 0)
        stats.amountTotal = tableData.value.reduce((sum, item) => sum + (item.totalAmount || 0), 0)
        // 统计正常发票数量
        stats.normalCount = tableData.value.filter(item => '正常' === item.invoiceStatus).length

        // 从第一条数据获取企业信息（最近采集的数据）
        const first = tableData.value[0]
        companyInfo.value = {
          taxNo: first.taxNo,
          creditCode: first.creditCode,
          companyName: first.companyName,
          companyType: first.companyType,
          applyDate: first.applyDate
        }
      } else {
        companyInfo.value = null
        stats.normalCount = 0
      }
    }
  } catch (error) {
    ElMessage.error(t('invoice.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 采集数据
const handleCollect = async () => {
  collecting.value = true
  try {
    const result = await apiPost('/invoice/collect', { collectName: '企业发票数据采集' })
    if (result.code === 0) {
      ElMessage.success(t('invoice.collectComplete'))
      await loadData()
    } else {
      ElMessage.error(result.message || t('invoice.collectFailed'))
    }
  } catch (error) {
    ElMessage.error(t('invoice.checkTarget'))
  } finally {
    collecting.value = false
  }
}

// 清空数据
const handleClear = async () => {
  try {
    await ElMessageBox.confirm(t('invoice.confirmClear'), t('invoice.warning'), {
      confirmButtonText: t('invoice.confirm'),
      cancelButtonText: t('invoice.cancel'),
      type: 'warning'
    })

    const result = await apiDelete('/invoice/clear')
    if (result.code === 0) {
      ElMessage.success(t('invoice.dataCleared'))
      tableData.value = []
      companyInfo.value = null
      stats.totalCount = 0
      stats.taxExclusiveTotal = 0
      stats.taxTotal = 0
      stats.amountTotal = 0
      stats.normalCount = 0
    } else {
      ElMessage.error(result.message || t('invoice.clearFailed'))
    }
  } catch {
    // 用户取消
  }
}

// 导出数据为CSV
const exportData = async () => {
  try {
    const exportData = tableData.value
    if (exportData.length === 0) {
      ElMessage.warning(t('invoice.noExportData'))
      return
    }

    // 构建CSV内容
    const headers = [t('invoice.index'), t('invoice.invoiceNo'), t('invoice.invoiceType'), t('invoice.invoiceStatus'), t('invoice.companyName'), t('invoice.creditCode'), t('invoice.invoiceDate'), t('invoice.taxExclusiveAmount'), t('invoice.taxAmount'), t('invoice.totalAmount'), t('invoice.collectTime')]
    const rows = exportData.map((item, index) => [
      index + 1,
      item.invoiceNo || '-',
      item.invoiceType || '-',
      item.invoiceStatus || '-',
      item.companyName || '-',
      item.creditCode || '-',
      formatDate(item.invoiceDate) || '-',
      formatNumber(item.taxExclusiveAmount),
      formatNumber(item.taxAmount),
      formatNumber(item.totalAmount),
      formatDate(item.collectTime)
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
    link.download = `${t('invoice.title')}_${new Date().toISOString().split('T')[0]}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    ElMessage.success(t('invoice.exported', { count: exportData.length }))
  } catch {
    ElMessage.error(t('invoice.exportFailed'))
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.invoice-data-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left h2 {
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-card-inner {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.stat-icon-wrapper {
  flex-shrink: 0;
}

.stat-icon-wrapper.highlight {
  position: relative;
}

.stat-icon-wrapper.highlight::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, rgba(250, 139, 32, 0.2) 0%, rgba(247, 183, 51, 0.1) 100%);
  border-radius: 50%;
  z-index: -1;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-icon.collect {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.amount {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-icon.tax {
  background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%);
}

.stat-icon.total {
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
  font-size: 28px;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-value.success {
  color: #11998e;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-value.highlight {
  color: #fc4a1a;
  font-size: 24px;
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.stat-sub {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

.stat-trend {
  margin-top: 8px;
  font-size: 12px;
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-trend.up {
  color: #67c23a;
}

.stat-trend.down {
  color: #f56c6c;
}

.trend-text {
  color: #909399;
  margin-left: 4px;
}

/* 卡片特定样式 */
.stat-card.total-count::before {
  content: '';
  position: absolute;
  top: -20px;
  right: -20px;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.05) 100%);
  border-radius: 50%;
}

.stat-card.amount-total::before {
  content: '';
  position: absolute;
  top: -30px;
  right: -30px;
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, rgba(252, 74, 26, 0.08) 0%, rgba(247, 183, 51, 0.04) 100%);
  border-radius: 50%;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.company-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.company-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.company-item {
  display: flex;
  align-items: center;
}

.company-item .label {
  color: #909399;
  white-space: nowrap;
}

.company-item .value {
  color: #303133;
}

.company-item .code {
  font-family: monospace;
  font-weight: 600;
  color: #409eff;
}

.table-card {
  margin-bottom: 20px;
}

.invoice-no {
  font-family: monospace;
  color: #606266;
}

.amount {
  font-family: monospace;
  color: #303133;
}

.amount.tax {
  color: #e6a23c;
}

.amount.total {
  color: #409eff;
  font-weight: 600;
}

.code {
  font-family: monospace;
  color: #409eff;
  font-weight: 600;
}
</style>
