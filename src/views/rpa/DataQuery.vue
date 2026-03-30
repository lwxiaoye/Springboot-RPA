<template>
  <div class="data-query-page">
    <div class="page-header">
      <h2>数据查询</h2>
      <p class="page-desc">配置和管理数据查询任务</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索查询名称..." />
      </div>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 新建查询
      </el-button>
    </div>

    <el-table :data="filteredData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="查询名称" min-width="160" />
      <el-table-column prop="queryCondition" label="查询条件" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.queryCondition || '-' }}</template>
      </el-table-column>
      <el-table-column prop="resultCount" label="结果数量" width="100" align="center" />
      <el-table-column prop="lastQueryTime" label="查询时间" min-width="160">
        <template #default="{ row }">{{ row.lastQueryTime ? new Date(row.lastQueryTime).toLocaleString() : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="runQuery(row)">执行</el-button>
          <el-button link type="primary" @click="viewResult(row)">查看结果</el-button>
          <el-popconfirm title="确认删除该查询吗？" @confirm="deleteQuery(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
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

    <!-- 新建查询弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建查询" width="550px">
      <el-form :model="queryForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="查询名称" prop="name">
          <el-input v-model="queryForm.name" placeholder="请输入查询名称" />
        </el-form-item>
        <el-form-item label="数据表">
          <el-select v-model="queryForm.sourceTable" placeholder="请选择数据表" style="width: 100%">
            <el-option label="采集数据" value="collected_data" />
            <el-option label="处理数据" value="processed_data" />
            <el-option label="用户数据" value="users" />
          </el-select>
        </el-form-item>
        <el-form-item label="查询条件">
          <el-input v-model="queryForm.queryCondition" type="textarea" :rows="4" placeholder="请输入查询条件（SQL的WHERE子句），如：status = 'active'" />
        </el-form-item>
        <el-form-item label="返回字段">
          <el-input v-model="queryForm.queryColumns" placeholder="请输入返回字段，多个字段用逗号分隔，如：id,name,email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitQuery" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查询结果弹窗 -->
    <el-dialog v-model="resultVisible" title="查询结果" width="800px">
      <div class="result-header">
        <span>查询名称：{{ currentQuery.name }}</span>
        <span>结果数量：{{ queryResults.length }} 条</span>
      </div>
      <el-table :data="queryResults" border stripe max-height="400">
        <el-table-column v-for="col in resultColumns" :key="col" :prop="col" :label="col" min-width="120" />
      </el-table>
      <template #footer>
        <el-button @click="resultVisible = false">关闭</el-button>
        <el-button type="primary" @click="exportData">导出数据</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])
const searchKeyword = ref('')
const dialogVisible = ref(false)
const resultVisible = ref(false)
const formRef = ref(null)
const currentQuery = ref({})
const queryResults = ref([])
const resultColumns = ref([])

const pagination = reactive({ page: 1, size: 10, total: 0 })

const queryForm = reactive({
  name: '',
  sourceTable: '',
  queryCondition: '',
  queryColumns: ''
})

const formRules = {
  name: [{ required: true, message: '请输入查询名称', trigger: 'blur' }]
}

const filteredData = computed(() => {
  let list = dataList.value
  if (searchKeyword.value) {
    list = list.filter(d => d.name.includes(searchKeyword.value))
  }
  return list
})

const loadData = async () => {
  loading.value = true
  try {
    const result = await apiGet('/dataQuery')
    if (result.code === 0) {
      dataList.value = result.data || []
      pagination.total = dataList.value.length
    }
  } catch {
    dataList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  Object.assign(queryForm, { name: '', sourceTable: '', queryCondition: '', queryColumns: '' })
  dialogVisible.value = true
}

const submitQuery = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const result = await apiPost('/dataQuery', {
          name: queryForm.name,
          sourceTable: queryForm.sourceTable,
          queryCondition: queryForm.queryCondition,
          queryColumns: queryForm.queryColumns
        })
        if (result.code === 0) {
          ElMessage.success('创建成功')
          dialogVisible.value = false
          await loadData()
        } else {
          ElMessage.error(result.message || '创建失败')
        }
      } catch {
        ElMessage.error('请求失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const runQuery = async (item) => {
  try {
    const result = await apiPost(`/dataQuery/${item.id}/execute`)
    if (result.code === 0 || result.success) {
      ElMessage.success(`查询执行完成`)
      await loadData()
    } else {
      ElMessage.error(result.message || '执行失败')
    }
  } catch {
    ElMessage.error('请求失败')
  }
}

const viewResult = async (item) => {
  currentQuery.value = item
  try {
    const result = await apiGet(`/dataQuery/${item.id}`)
    if (result.code === 0 && result.data?.resultData) {
      try {
        const parsed = JSON.parse(result.data.resultData)
        if (Array.isArray(parsed) && parsed.length > 0) {
          queryResults.value = parsed
          resultColumns.value = Object.keys(parsed[0])
        } else {
          queryResults.value = []
          resultColumns.value = []
        }
      } catch {
        queryResults.value = []
        resultColumns.value = []
      }
    } else {
      queryResults.value = []
      resultColumns.value = []
    }
  } catch {
    queryResults.value = []
    resultColumns.value = []
  }
  resultVisible.value = true
}

const exportData = () => {
  ElMessage.success('导出功能开发中')
}

const deleteQuery = async (item) => {
  try {
    const result = await apiDelete(`/dataQuery/${item.id}`)
    if (result.code === 0) {
      const index = dataList.value.findIndex(d => d.id === item.id)
      if (index !== -1) {
        dataList.value.splice(index, 1)
        pagination.total--
      }
      ElMessage.success('删除成功')
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch {
    ElMessage.error('请求失败')
  }
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => { loadData() })
</script>

<style scoped>
.data-query-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; flex-wrap: wrap; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }
.result-header { display: flex; justify-content: space-between; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #e4e7ed; color: #666; }
</style>