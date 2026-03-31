<template>
  <div class="tasks-page">
    <div class="page-header">
      <h2>任务管理</h2>
      <p class="page-desc">创建和管理RPA自动化任务</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索任务名称..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px;">
        <el-option label="运行中" value="running" />
        <el-option label="待执行" value="pending" />
        <el-option label="已完成" value="completed" />
        <el-option label="失败" value="failed" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 新建任务
      </el-button>
    </div>

    <el-table :data="paginatedTasks" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="任务名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="processName" label="所属流程" min-width="140" />
      <el-table-column prop="robotName" label="执行机器人" min-width="120" />
      <el-table-column prop="priority" label="优先级" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getPriorityType(row.priority)" size="small">
            {{ getPriorityText(row.priority) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="primary" @click="editTask(row)">编辑</el-button>
          <el-popconfirm title="确认删除该任务吗？" @confirm="deleteTask(row)">
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

    <!-- 新建/编辑任务弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="taskForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="taskForm.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="所属流程" prop="processId">
          <el-select v-model="taskForm.processId" placeholder="请选择流程" style="width: 100%" @change="onProcessChange">
            <el-option v-for="p in processes" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行机器人" prop="robotId">
          <el-select v-model="taskForm.robotId" placeholder="请选择机器人" style="width: 100%">
            <el-option v-for="r in robots" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="taskForm.priority" placeholder="请选择优先级" style="width: 100%">
            <el-option label="低" value="low" />
            <el-option label="普通" value="normal" />
            <el-option label="高" value="high" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="taskForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="500px">
      <div class="detail-content">
        <div class="detail-item"><label>任务名称：</label><span>{{ currentTask.name }}</span></div>
        <div class="detail-item"><label>所属流程：</label><span>{{ currentTask.processName }}</span></div>
        <div class="detail-item"><label>执行机器人：</label><span>{{ currentTask.robotName }}</span></div>
        <div class="detail-item"><label>优先级：</label><span>{{ getPriorityText(currentTask.priority) }}</span></div>
        <div class="detail-item"><label>状态：</label><span>{{ getStatusText(currentTask.status) }}</span></div>
        <div class="detail-item"><label>创建时间：</label><span>{{ currentTask.createTime }}</span></div>
        <div class="detail-item full"><label>备注：</label><span>{{ currentTask.remark || '-' }}</span></div>
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
const tasks = ref([])
const processes = ref([])
const robots = ref([])

const searchKeyword = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentTask = ref({})
const currentEditId = ref(null)
const formRef = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const taskForm = reactive({
  name: '',
  processId: '',
  robotId: '',
  priority: 'normal',
  category: ''
})

const formRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  processId: [{ required: true, message: '请选择流程', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑任务' : '新建任务')

const getStatusText = (s) => {
  const map = { running: '运行中', pending: '待执行', completed: '已完成', failed: '失败' }
  return map[s] || s
}

const getStatusType = (s) => {
  const map = { running: 'warning', pending: 'info', completed: 'success', failed: 'danger' }
  return map[s] || 'info'
}

const getPriorityText = (p) => {
  const map = { high: '高', normal: '普通', low: '低' }
  return map[p] || p
}

const getPriorityType = (p) => {
  const map = { high: 'danger', normal: 'warning', low: 'info' }
  return map[p] || 'info'
}

const filteredTasks = computed(() => {
  let list = tasks.value
  if (searchKeyword.value) {
    list = list.filter(t => t.name.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(t => t.status === statusFilter.value)
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedTasks = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredTasks.value.slice(start, end)
})

const loadTasks = async () => {
  loading.value = true
  try {
    const result = await apiGet('/task')
    if (result.code === 0) {
      tasks.value = result.data || []
      // 不需要在这里设置 pagination.total，filteredTasks 会计算
    }
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

const loadProcesses = async () => {
  try {
    const result = await apiGet('/process')
    if (result.code === 0) {
      processes.value = result.data || []
    }
  } catch {
    processes.value = []
  }
}

const loadRobots = async () => {
  try {
    const result = await apiGet('/robot/idle')
    if (result.code === 0) {
      robots.value = result.data || []
    }
  } catch {
    robots.value = []
  }
}

const showCreateModal = () => {
  isEdit.value = false
  currentEditId.value = null
  Object.assign(taskForm, { name: '', processId: '', robotId: '', priority: 'normal', category: '' })
  dialogVisible.value = true
}

const editTask = (task) => {
  isEdit.value = true
  currentEditId.value = task.id
  // 先设置processId，再更新category
  taskForm.processId = task.processId || ''
  taskForm.category = task.category || task.processName || ''
  Object.assign(taskForm, {
    name: task.name,
    robotId: task.robotId || '',
    priority: task.priority || 'normal'
  })
  dialogVisible.value = true
}

const onProcessChange = (processId) => {
  const process = processes.value.find(p => p.id === processId)
  if (process) {
    taskForm.category = process.name
  }
}

const viewDetail = (task) => {
  currentTask.value = task
  detailVisible.value = true
}

const submitTask = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const process = processes.value.find(p => p.id === taskForm.processId)
        if (isEdit.value) {
          const result = await apiPut(`/task/${currentEditId.value}`, {
            name: taskForm.name,
            processId: taskForm.processId,
            processName: process?.name || taskForm.category,
            category: taskForm.category,
            priority: taskForm.priority,
            robotId: taskForm.robotId
          })
          if (result.code === 0) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            await loadTasks()
          } else {
            ElMessage.error(result.message || '更新失败')
          }
        } else {
          const result = await apiPost('/task', {
            name: taskForm.name,
            category: taskForm.category,
            priority: taskForm.priority,
            processId: taskForm.processId,
            processName: process?.name,
            assigneeId: taskForm.robotId
          })
          if (result.code === 0) {
            ElMessage.success('创建成功')
            dialogVisible.value = false
            await loadTasks()
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

const deleteTask = async (task) => {
  try {
    const result = await apiDelete(`/task/${task.id}`)
    if (result.code === 0) {
      const index = tasks.value.findIndex(t => t.id === task.id)
      if (index !== -1) {
        tasks.value.splice(index, 1)
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

onMounted(() => {
  loadTasks()
  loadProcesses()
  loadRobots()
})
</script>

<style scoped>
.tasks-page { max-width: 1400px; margin: 0 auto; }
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