<template>
  <div class="invoice-data-container">
    <el-card class="header-card">
      <div class="header-content">
        <div class="header-left">
          <h2>
            <el-icon><Document /></el-icon>
            企业发票数据
          </h2>
          <p class="subtitle">从目标网页采集并解析的企业发票数据</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="handleCollect" :loading="collecting">
            <el-icon><Refresh /></el-icon>
            采集数据
          </el-button>
          <el-button @click="loadData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="danger" @click="handleClear" :disabled="tableData.length === 0">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-if="stats.totalCount > 0">
      <el-card class="stat-card">
        <div class="stat-icon collect">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalCount }}</div>
          <div class="stat-label">发票总数</div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon amount">
          <el-icon><Money /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">¥ {{ formatNumber(stats.taxExclusiveTotal) }}</div>
          <div class="stat-label">不含税总额</div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon tax">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">¥ {{ formatNumber(stats.taxTotal) }}</div>
          <div class="stat-label">税额总额</div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon total">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">¥ {{ formatNumber(stats.amountTotal) }}</div>
          <div class="stat-label">价税合计</div>
        </div>
      </el-card>
    </div>

    <!-- 企业信息 -->
    <el-card class="company-card" v-if="companyInfo">
      <template #header>
        <div class="card-header">
          <span>
            <el-icon><OfficeBuilding /></el-icon>
            企业基本信息
          </span>
        </div>
      </template>
      <div class="company-grid">
        <div class="company-item">
          <span class="label">纳税人识别号：</span>
          <span class="value code">{{ companyInfo.taxNo }}</span>
        </div>
        <div class="company-item">
          <span class="label">统一社会信用代码：</span>
          <span class="value code">{{ companyInfo.creditCode }}</span>
        </div>
        <div class="company-item">
          <span class="label">企业名称：</span>
          <span class="value">{{ companyInfo.companyName }}</span>
        </div>
        <div class="company-item">
          <span class="label">企业类型：</span>
          <el-tag type="info">{{ companyInfo.companyType }}</el-tag>
        </div>
        <div class="company-item">
          <span class="label">申请日期：</span>
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
            发票明细
            <el-tag type="primary" size="small">{{ tableData.length }} 条</el-tag>
          </span>
        </div>
      </template>
      
      <el-table :data="tableData" stripe border :max-height="600" v-loading="loading">
        <el-table-column type="index" width="60" label="#" align="center" />
        <el-table-column prop="invoiceType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.invoiceType === '销项' ? 'success' : 'warning'" size="small">
              {{ row.invoiceType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceStatus" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.invoiceStatus)" size="small">
              {{ row.invoiceStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceDate" label="日期" width="120" align="center" />
        <el-table-column prop="invoiceNo" label="发票号码" width="140" align="center">
          <template #default="{ row }">
            <span class="invoice-no">{{ row.invoiceNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="taxExclusiveAmount" label="不含税金额" width="140" align="right">
          <template #default="{ row }">
            <span class="amount">¥ {{ formatNumber(row.taxExclusiveAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="taxAmount" label="税额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount tax">¥ {{ formatNumber(row.taxAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="价税合计" width="140" align="right">
          <template #default="{ row }">
            <span class="amount total">¥ {{ formatNumber(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="collectTime" label="采集时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDate(row.collectTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-if="!loading && tableData.length === 0" description="暂无发票数据">
      <el-button type="primary" @click="handleCollect">
        <el-icon><Refresh /></el-icon>
        立即采集
      </el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, Refresh, Delete, List, Money, Coin, 
  TrendCharts, OfficeBuilding
} from '@element-plus/icons-vue'

const loading = ref(false)
const collecting = ref(false)
const tableData = ref([])
const companyInfo = ref(null)
const stats = reactive({
  totalCount: 0,
  taxExclusiveTotal: 0,
  taxTotal: 0,
  amountTotal: 0
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
        
        // 从第一条数据获取企业信息
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
      }
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
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
      ElMessage.success('数据采集完成')
      await loadData()
    } else {
      ElMessage.error(result.message || '采集失败')
    }
  } catch (error) {
    ElMessage.error('采集失败，请检查目标网页是否可访问')
  } finally {
    collecting.value = false
  }
}

// 清空数据
const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有发票数据吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const result = await apiDelete('/invoice/clear')
    if (result.code === 0) {
      ElMessage.success('数据已清空')
      tableData.value = []
      companyInfo.value = null
      stats.totalCount = 0
      stats.taxExclusiveTotal = 0
      stats.taxTotal = 0
      stats.amountTotal = 0
    } else {
      ElMessage.error(result.message || '清空失败')
    }
  } catch {
    // 用户取消
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
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.stat-icon.collect {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-icon.amount {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.stat-icon.tax {
  background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%);
  color: white;
}

.stat-icon.total {
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
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
</style>
