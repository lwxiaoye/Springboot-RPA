<template>
  <div class="data-process-page">
    <div class="page-header">
      <h2>数据加工</h2>
      <p class="page-desc">配置和管理数据加工任务</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索加工名称..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="成功" value="success" />
        <el-option label="运行中" value="running" />
        <el-option label="待执行" value="pending" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 新建加工
      </el-button>
    </div>

    <el-table :data="filteredData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="加工名称" min-width="160" />
      <el-table-column prop="method" label="加工方法" min-width="140" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="processedCount" label="处理数量" width="100" align="center" />
      <el-table-column prop="processTime" label="处理时间" min-width="160">
        <template #default="{ row }">{{ row.processTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="runTask(row)">执行</el-button>
          <el-button link type="primary" @click="editTask(row)">编辑</el-button>
          <el-popconfirm title="确认删除该加工任务吗？" @confirm="deleteTask(row)">
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

    <!-- 新建/编辑加工弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="dataForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="加工名称" prop="name">
          <el-input v-model="dataForm.name" placeholder="请输入加工名称" />
        </el-form-item>
        <el-form-item label="加工方法" prop="method">
          <el-select v-model="dataForm.method" placeholder="请选择加工方法" style="width: 100%">
            <el-option label="数据清洗" value="数据清洗" />
            <el-option label="数据转换" value="数据转换" />
            <el-option label="数据聚合" value="数据聚合" />
            <el-option label="数据脱敏" value="数据脱敏" />
          </el-select>
        </el-form-item>
        <el-form-item label="加工配置">
          <el-input v-model="dataForm.config" type="textarea" :rows="4" placeholder="请输入加工配置" />
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

const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])
const searchKeyword = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentEditId = ref(null)
const formRef = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const dataForm = reactive({
  name: '',
  method: '',
  config: ''
})

const formRules = {
  name: [{ required: true, message: '请输入加工名称', trigger: 'blur' }],
  method: [{ required: true, message: '请选择加工方法', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑加工' : '新建加工')

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
  setTimeout(() => {
    dataList.value = [
      { id: 1, name: '数据清洗', method: '数据清洗', status: 'success', processedCount: 1235, processTime: '2026-03-26 10:00:00', config: '{"rules":["去重","格式化"]}' },
      { id: 2, name: '数据转换', method: '数据转换', status: 'running', processedCount: 500, processTime: '2026-03-26 11:00:00', config: '{"format":"JSON->CSV"}' },
      { id: 3, name: '数据聚合', method: '数据聚合', status: 'pending', processedCount: 0, processTime: null, config: '{"groupBy":"category","aggregate":"sum"}' }
    ]
    pagination.total = dataList.value.length
    loading.value = false
  }, 300)
}

const showCreateModal = () => {
  isEdit.value = false
  currentEditId.value = null
  Object.assign(dataForm, { name: '', method: '', config: '' })
  dialogVisible.value = true
}

const editTask = (item) => {
  isEdit.value = true
  currentEditId.value = item.id
  Object.assign(dataForm, {
    name: item.name,
    method: item.method,
    config: item.config
  })
  dialogVisible.value = true
}

const submitTask = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      setTimeout(() => {
        if (isEdit.value) {
          const index = dataList.value.findIndex(d => d.id === currentEditId.value)
          if (index !== -1) {
            dataList.value[index] = { ...dataList.value[index], ...dataForm }
          }
          ElMessage.success('更新成功')
        } else {
          dataList.value.unshift({
            id: Date.now(),
            ...dataForm,
            status: 'pending',
            processedCount: 0,
            processTime: null
          })
          pagination.total++
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        submitLoading.value = false
      }, 500)
    }
  })
}

const runTask = (item) => {
  ElMessage.success(`已启动加工任务: ${item.name}`)
  const index = dataList.value.findIndex(d => d.id === item.id)
  if (index !== -1) {
    dataList.value[index].status = 'running'
  }
}

const deleteTask = (item) => {
  const index = dataList.value.findIndex(d => d.id === item.id)
  if (index !== -1) {
    dataList.value.splice(index, 1)
    pagination.total--
  }
  ElMessage.success('删除成功')
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => { loadData() })
</script>

<style scoped>
.data-process-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; flex-wrap: wrap; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }
</style>