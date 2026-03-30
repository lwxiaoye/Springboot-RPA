<template>
  <div class="data-collect-page">
    <div class="page-header">
      <h2>数据采集</h2>
      <p class="page-desc">配置和管理数据采集任务</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索采集名称..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="成功" value="success" />
        <el-option label="运行中" value="running" />
        <el-option label="待执行" value="pending" />
        <el-option label="失败" value="failed" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 新建采集
      </el-button>
    </div>

    <el-table :data="filteredData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="采集名称" min-width="160" />
      <el-table-column prop="sourceUrl" label="数据来源" min-width="160" show-overflow-tooltip />
      <el-table-column prop="sourceType" label="类型" width="100" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="dataCount" label="数据量" width="90" align="center" />
      <el-table-column prop="lastCollectTime" label="最后采集" min-width="160">
        <template #default="{ row }">{{ row.lastCollectTime ? new Date(row.lastCollectTime).toLocaleString() : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="runTask(row)">执行</el-button>
          <el-button link type="primary" @click="editTask(row)">编辑</el-button>
          <el-popconfirm title="确认删除该采集任务吗？" @confirm="deleteTask(row)">
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

    <!-- 新建/编辑采集弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="dataForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="采集名称" prop="name">
          <el-input v-model="dataForm.name" placeholder="请输入采集名称" />
        </el-form-item>
        <el-form-item label="来源URL" prop="sourceUrl">
          <el-input v-model="dataForm.sourceUrl" placeholder="请输入数据来源URL，如：https://example.com/api" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="dataForm.sourceType" placeholder="请选择来源类型" style="width: 100%">
            <el-option label="CRM系统" value="CRM系统" />
            <el-option label="ERP系统" value="ERP系统" />
            <el-option label="电商平台" value="电商平台" />
            <el-option label="外部API" value="外部API" />
            <el-option label="网页" value="网页" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择器规则">
          <el-input v-model="dataForm.selectorRules" type="textarea" :rows="3" placeholder="请输入CSS选择器或XPath规则" />
        </el-form-item>
        <el-form-item label="请求头">
          <el-input v-model="dataForm.headers" type="textarea" :rows="2" placeholder="如：Content-Type: application/json" />
        </el-form-item>
        <el-form-item label="Cookie">
          <el-input v-model="dataForm.cookies" type="textarea" :rows="2" placeholder="如有登录Cookie，请在此输入" />
        </el-form-item>
        <el-form-item label="定时规则">
          <el-input v-model="dataForm.cronExpression" placeholder="Cron表达式，如：0 0 * * * (每天)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask" :loading="submitLoading">确定</el-button>
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
const statusFilter = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentEditId = ref(null)
const formRef = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const dataForm = reactive({
  name: '',
  sourceUrl: '',
  sourceType: '',
  selectorRules: '',
  headers: '',
  cookies: '',
  cronExpression: ''
})

const formRules = {
  name: [{ required: true, message: '请输入采集名称', trigger: 'blur' }],
  sourceUrl: [{ required: true, message: '请输入数据来源URL', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑采集' : '新建采集')

const getStatusText = (s) => {
  const map = { success: '成功', running: '运行中', pending: '待执行', failed: '失败' }
  return map[s] || s
}

const getStatusType = (s) => {
  const map = { success: 'success', running: 'warning', pending: 'info', failed: 'danger' }
  return map[s] || 'info'
}

const filteredData = computed(() => {
  let list = dataList.value
  if (searchKeyword.value) {
    list = list.filter(d => d.name.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(d => d.status === statusFilter.value)
  }
  return list
})

const loadData = async () => {
  loading.value = true
  try {
    const result = await apiGet('/dataCollect')
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
  isEdit.value = false
  currentEditId.value = null
  Object.assign(dataForm, { name: '', sourceUrl: '', sourceType: '', selectorRules: '', headers: '', cookies: '', cronExpression: '' })
  dialogVisible.value = true
}

const editTask = (item) => {
  isEdit.value = true
  currentEditId.value = item.id
  Object.assign(dataForm, {
    name: item.name,
    sourceUrl: item.sourceUrl,
    sourceType: item.sourceType,
    selectorRules: item.selectorRules,
    headers: item.headers,
    cookies: item.cookies,
    cronExpression: item.cronExpression
  })
  dialogVisible.value = true
}

const submitTask = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          const result = await apiPut(`/dataCollect/${currentEditId.value}`, {
            name: dataForm.name,
            sourceUrl: dataForm.sourceUrl,
            sourceType: dataForm.sourceType,
            selectorRules: dataForm.selectorRules,
            headers: dataForm.headers,
            cookies: dataForm.cookies,
            cronExpression: dataForm.cronExpression
          })
          if (result.code === 0) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            await loadData()
          } else {
            ElMessage.error(result.message || '更新失败')
          }
        } else {
          const result = await apiPost('/dataCollect', {
            name: dataForm.name,
            sourceUrl: dataForm.sourceUrl,
            sourceType: dataForm.sourceType,
            selectorRules: dataForm.selectorRules,
            headers: dataForm.headers,
            cookies: dataForm.cookies,
            cronExpression: dataForm.cronExpression
          })
          if (result.code === 0) {
            ElMessage.success('创建成功')
            dialogVisible.value = false
            await loadData()
          } else {
            ElMessage.error(result.message || '创建失败')
          }
        }
      } catch {
        ElMessage.error('请求失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const runTask = async (item) => {
  try {
    const result = await apiPost(`/dataCollect/${item.id}/execute`)
    if (result.code === 0 || result.success) {
      ElMessage.success(`采集任务已启动: ${item.name}`)
      await loadData()
    } else {
      ElMessage.error(result.message || '执行失败')
    }
  } catch {
    ElMessage.error('请求失败')
  }
}

const deleteTask = async (item) => {
  try {
    const result = await apiDelete(`/dataCollect/${item.id}`)
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
.data-collect-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; flex-wrap: wrap; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }
</style>