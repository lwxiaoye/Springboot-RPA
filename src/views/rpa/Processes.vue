<template>
  <div class="processes-page">
    <div class="page-header">
      <h2>流程管理</h2>
      <p class="page-desc">管理RPA自动化流程</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索流程名称/编码..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="已发布" value="active" />
        <el-option label="草稿" value="draft" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 新建流程
      </el-button>
    </div>

    <el-table :data="paginatedProcesses" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="流程名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="code" label="流程编码" min-width="140" />
      <el-table-column prop="version" label="版本" width="80" align="center" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '-' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
            {{ row.status === 'active' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="creatorName" label="创建人" width="100" align="center" />
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="primary" @click="editProcess(row)">编辑</el-button>
          <el-popconfirm title="确认删除该流程吗？" @confirm="deleteProcess(row)">
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

    <!-- 新建/编辑流程弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="processForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="流程名称" prop="name">
          <el-input v-model="processForm.name" placeholder="请输入流程名称" />
        </el-form-item>
        <el-form-item label="流程编码" prop="code">
          <el-input v-model="processForm.code" placeholder="请输入流程编码，如：ORDER_PROCESS" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="processForm.version" placeholder="请输入版本号，如：1.0.0" />
        </el-form-item>
        <el-form-item label="流程描述">
          <el-input v-model="processForm.description" type="textarea" :rows="3" placeholder="请输入流程描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="processForm.status">
            <el-radio label="draft">草稿</el-radio>
            <el-radio label="active">已发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcess" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="流程详情" width="500px">
      <div class="detail-content">
        <div class="detail-item"><label>流程名称：</label><span>{{ currentProcess.name }}</span></div>
        <div class="detail-item"><label>流程编码：</label><span>{{ currentProcess.code }}</span></div>
        <div class="detail-item"><label>版本号：</label><span>{{ currentProcess.version }}</span></div>
        <div class="detail-item"><label>状态：</label><span>{{ currentProcess.status === 'active' ? '已发布' : '草稿' }}</span></div>
        <div class="detail-item"><label>创建人：</label><span>{{ currentProcess.creatorName }}</span></div>
        <div class="detail-item"><label>创建时间：</label><span>{{ currentProcess.createTime }}</span></div>
        <div class="detail-item full"><label>描述：</label><span>{{ currentProcess.description || '-' }}</span></div>
      </div>
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
const processes = ref([])
const searchKeyword = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentProcess = ref({})
const currentEditId = ref(null)
const formRef = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const processForm = reactive({
  name: '',
  code: '',
  version: '1.0.0',
  description: '',
  status: 'draft'
})

const formRules = {
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入流程编码', trigger: 'blur' }],
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑流程' : '新建流程')

const filteredProcesses = computed(() => {
  let list = processes.value
  if (searchKeyword.value) {
    list = list.filter(p => p.name.includes(searchKeyword.value) || p.code?.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(p => p.status === statusFilter.value)
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedProcesses = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredProcesses.value.slice(start, end)
})

const loadProcesses = async () => {
  loading.value = true
  try {
    const result = await apiGet('/process')
    if (result.code === 0) {
      processes.value = result.data || []
      // 不需要在这里设置 pagination.total，filteredProcesses 会计算
    }
  } catch {
    processes.value = []
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isEdit.value = false
  currentEditId.value = null
  Object.assign(processForm, { name: '', code: '', version: '1.0.0', description: '', status: 'draft' })
  dialogVisible.value = true
}

const editProcess = (process) => {
  isEdit.value = true
  currentEditId.value = process.id
  Object.assign(processForm, {
    name: process.name,
    code: process.code,
    version: process.version,
    description: process.description,
    status: process.status
  })
  dialogVisible.value = true
}

const viewDetail = (process) => {
  currentProcess.value = process
  detailVisible.value = true
}

const submitProcess = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          const result = await apiPut(`/process/${currentEditId.value}`, {
            name: processForm.name,
            code: processForm.code,
            description: processForm.description,
            version: processForm.version,
            status: processForm.status
          })
          if (result.code === 0) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            await loadProcesses()
          } else {
            ElMessage.error(result.message || '更新失败')
          }
        } else {
          const result = await apiPost('/process', {
            name: processForm.name,
            code: processForm.code,
            description: processForm.description,
            version: processForm.version,
            status: processForm.status
          })
          if (result.code === 0) {
            ElMessage.success('创建成功')
            dialogVisible.value = false
            await loadProcesses()
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

const deleteProcess = async (process) => {
  try {
    const result = await apiDelete(`/process/${process.id}`)
    if (result.code === 0) {
      const index = processes.value.findIndex(p => p.id === process.id)
      if (index !== -1) {
        processes.value.splice(index, 1)
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

onMounted(() => { loadProcesses() })
</script>

<style scoped>
.processes-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }
.detail-content { padding: 8px 0; }
.detail-item { display: flex; margin-bottom: 12px; }
.detail-item label { width: 80px; color: #8c8c8c; }
.detail-item span { color: #262626; font-weight: 500; }
.detail-item.full { flex-direction: column; }
.detail-item.full label { margin-bottom: 4px; }
</style>