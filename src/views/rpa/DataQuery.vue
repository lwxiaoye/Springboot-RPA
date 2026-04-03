<template>
  <div class="data-query-page">
    <div class="page-header">
      <h2>发票数据查询</h2>
      <p class="page-desc">查询和管理发票数据</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索企业名称、发票号..." />
      </div>
      <el-select v-model="filterStatus" placeholder="发票状态" style="width: 140px" clearable>
        <el-option label="全部" value="" />
        <el-option label="正常" value="正常" />
        <el-option label="作废" value="作废" />
        <el-option label="红冲" value="红冲" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" clearable />
      <el-button type="primary" @click="loadInvoiceData">
        <el-icon><Search /></el-icon> 查询
      </el-button>
      <el-button @click="resetFilters">
        <el-icon><Refresh /></el-icon> 重置
      </el-button>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-value">{{ statistics.total }}</div>
        <div class="stat-label">总发票数</div>
      </div>
      <div class="stat-card success">
        <div class="stat-value">{{ statistics.normal }}</div>
        <div class="stat-label">正常发票</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-value">{{ statistics.cancelled }}</div>
        <div class="stat-label">作废发票</div>
      </div>
      <div class="stat-card info">
        <div class="stat-value">¥{{ statistics.totalAmount.toLocaleString() }}</div>
        <div class="stat-label">价税合计</div>
      </div>
    </div>

    <el-table :data="paginatedData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="invoiceNo" label="发票号码" width="130" />
      <el-table-column prop="invoiceType" label="发票类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="info">{{ row.invoiceType || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="invoiceStatus" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.invoiceStatus === '正常' ? 'success' : 'danger'">{{ row.invoiceStatus || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="companyName" label="企业名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="creditCode" label="统一社会信用代码" width="180" show-overflow-tooltip />
      <el-table-column prop="invoiceDate" label="开票日期" width="120" align="center" />
      <el-table-column prop="taxExclusiveAmount" label="不含税金额" width="120" align="right">
        <template #default="{ row }">¥{{ Number(row.taxExclusiveAmount || 0).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="taxAmount" label="税额" width="100" align="right">
        <template #default="{ row }">¥{{ Number(row.taxAmount || 0).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="价税合计" width="120" align="right">
        <template #default="{ row }"><b>¥{{ Number(row.totalAmount || 0).toLocaleString() }}</b></template>
      </el-table-column>
      <el-table-column prop="collectTime" label="采集时间" width="160">
        <template #default="{ row }">{{ row.collectTime ? new Date(row.collectTime).toLocaleString() : '-' }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { apiGet } from '../../utils/api.js'

const loading = ref(false)
const invoiceList = ref([])
const searchKeyword = ref('')
const filterStatus = ref('')
const dateRange = ref(null)

const pagination = reactive({ page: 1, size: 20, total: 0 })

const statistics = computed(() => {
  const list = filteredData.value
  return {
    total: list.length,
    normal: list.filter(d => d.invoiceStatus === '正常').length,
    cancelled: list.filter(d => d.invoiceStatus === '作废' || d.invoiceStatus === '红冲').length,
    totalAmount: list.reduce((sum, d) => sum + Number(d.totalAmount || 0), 0)
  }
})

const filteredData = computed(() => {
  let list = invoiceList.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(d =>
      (d.companyName || '').toLowerCase().includes(kw) ||
      (d.invoiceNo || '').toLowerCase().includes(kw) ||
      (d.creditCode || '').toLowerCase().includes(kw)
    )
  }
  if (filterStatus.value) {
    list = list.filter(d => d.invoiceStatus === filterStatus.value)
  }
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(d => {
      if (!d.invoiceDate) return true
      const date = new Date(d.invoiceDate)
      return date >= start && date <= end
    })
  }
  return list
})

const paginatedData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  pagination.total = filteredData.value.length
  return filteredData.value.slice(start, end)
})

const loadInvoiceData = async () => {
  loading.value = true
  try {
    const result = await apiGet('/invoice')
    if (result.code === 0) {
      invoiceList.value = result.data || []
    } else {
      invoiceList.value = []
    }
  } catch {
    invoiceList.value = []
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  searchKeyword.value = ''
  filterStatus.value = ''
  dateRange.value = null
  pagination.page = 1
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => { loadInvoiceData() })
</script>

<style scoped>
.data-query-page { max-width: 1600px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; flex-wrap: wrap; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.stats-cards { display: flex; gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; padding: 20px 24px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); min-width: 140px; }
.stat-card.success { border-left: 4px solid #52c41a; }
.stat-card.warning { border-left: 4px solid #faad14; }
.stat-card.info { border-left: 4px solid #1890ff; }
.stat-value { font-size: 28px; font-weight: 600; color: #1a1a1a; }
.stat-label { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }
</style>