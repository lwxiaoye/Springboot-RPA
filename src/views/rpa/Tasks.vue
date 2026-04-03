<template>
  <div class="tasks-page">
    <div class="page-header">
      <h2>任务调度中心</h2>
      <p class="page-desc">创建任务并绑定业务流程进行自动化执行</p>
    </div>

    <div class="main-layout">
      <!-- 左侧：任务列表 -->
      <div class="task-list-panel">
        <div class="panel-header">
          <h3>任务列表</h3>
          <el-button type="primary" size="small" @click="showCreateTask">
            <el-icon><Plus /></el-icon> 新建
          </el-button>
        </div>
        <div class="task-filter">
          <el-input v-model="taskSearch" placeholder="搜索任务..." size="small" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="task-list">
          <div
            v-for="task in filteredTasks"
            :key="task.id"
            class="task-item"
            :class="{ active: selectedTask?.id === task.id }"
            @click="selectTask(task)"
          >
            <div class="task-info">
              <span class="task-name">{{ task.name }}</span>
              <span class="task-process">流程: {{ task.processName || '-' }}</span>
            </div>
            <el-tag size="small" :type="getStatusType(task.status)">{{ getStatusText(task.status) }}</el-tag>
          </div>
          <el-empty v-if="filteredTasks.length === 0" description="暂无任务" />
        </div>
      </div>

      <!-- 中间：任务配置 -->
      <div class="config-panel">
        <div class="panel-header">
          <h3>{{ isEditTask ? '编辑任务' : '任务配置' }}</h3>
        </div>

        <div class="config-content">
          <el-form :model="taskForm" :rules="formRules" ref="formRef" label-width="100px">
            <el-form-item label="任务名称" prop="name">
              <el-input v-model="taskForm.name" placeholder="请输入任务名称" />
            </el-form-item>
            <el-form-item label="绑定流程">
              <el-select v-model="taskForm.processId" placeholder="请选择绑定的流程" style="width: 100%" clearable>
                <el-option v-for="p in processes" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="taskForm.remark" type="textarea" :rows="3" placeholder="任务备注（可选）" />
            </el-form-item>
          </el-form>

          <div class="config-actions">
            <el-button @click="cancelEdit" v-if="isEditTask">取消</el-button>
            <el-button type="primary" @click="saveTask" :loading="saveLoading">
              {{ isEditTask ? '保存修改' : '创建任务' }}
            </el-button>
            <el-button type="success" @click="executeTask" :disabled="!selectedTask?.processId && !taskForm.processId">
              <el-icon><VideoPlay /></el-icon>
              立即执行
            </el-button>
          </div>
        </div>

        <!-- 已创建的流程 -->
        <div class="process-list-section">
          <div class="section-header">
            <h4>已创建的流程</h4>
          </div>
          <div class="process-list">
            <div v-for="process in processes" :key="process.id" class="process-item">
              <div class="process-info">
                <span class="process-name">{{ process.name }}</span>
                <span class="process-desc">{{ process.description || '暂无描述' }}</span>
              </div>
              <div class="process-ops">
                <el-button link type="primary" size="small" @click="runProcess(process)">
                  <el-icon><VideoPlay /></el-icon> 执行
                </el-button>
              </div>
            </div>
            <el-empty v-if="processes.length === 0" description="暂无流程" />
          </div>
        </div>
      </div>

      <!-- 右侧：执行记录 -->
      <div class="log-panel">
        <div class="panel-header">
          <h3>执行记录</h3>
          <el-button size="small" @click="loadLogs">刷新</el-button>
        </div>
        <div class="log-list">
          <div v-for="log in recentLogs" :key="log.id" class="log-item">
            <div class="log-header">
              <span class="log-name">{{ log.taskName || log.name || '任务' }}</span>
              <el-tag size="small" :type="getStatusType(log.status)">{{ getStatusText(log.status) }}</el-tag>
            </div>
            <div class="log-info">
              <span>{{ log.startTime || log.createdAt || '-' }}</span>
              <span v-if="log.duration">{{ log.duration }}</span>
            </div>
            <div class="log-message" v-if="log.message">{{ log.message }}</div>
          </div>
          <el-empty v-if="recentLogs.length === 0" description="暂无执行记录" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, VideoPlay, Plus } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const loading = ref(false)
const saveLoading = ref(false)
const executing = ref(false)
const taskList = ref([])
const processes = ref([])
const selectedTask = ref(null)
const taskSearch = ref('')
const recentLogs = ref([])
const formRef = ref(null)
const isEditTask = ref(false)

const taskForm = reactive({
  name: '',
  processId: '',
  remark: ''
})

const formRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }]
}

const filteredTasks = computed(() => {
  if (!taskSearch.value) return taskList.value
  const kw = taskSearch.value.toLowerCase()
  return taskList.value.filter(t => (t.name || '').toLowerCase().includes(kw))
})

const getStatusText = (s) => {
  const map = { success: '成功', failed: '失败', running: '执行中', pending: '待执行', completed: '已完成' }
  return map[s] || s || '-'
}

const getStatusType = (s) => {
  const map = { success: 'success', failed: 'danger', running: 'warning', pending: 'info', completed: 'success' }
  return map[s] || 'info'
}

const loadTasks = async () => {
  try {
    const result = await apiGet('/task')
    if (result.code === 0) {
      taskList.value = result.data || []
    }
  } catch {
    taskList.value = []
  }
}

const loadProcesses = async () => {
  loading.value = true
  try {
    const result = await apiGet('/process')
    if (result.code === 0) {
      processes.value = result.data || []
    }
  } catch {
    processes.value = []
  } finally {
    loading.value = false
  }
}

const loadLogs = async () => {
  try {
    const result = await apiGet('/log?limit=20')
    if (result.code === 0) {
      recentLogs.value = result.data || []
    }
  } catch {
    recentLogs.value = []
  }
}

const selectTask = (task) => {
  selectedTask.value = task
  isEditTask.value = true
  taskForm.name = task.name
  taskForm.processId = task.processId || ''
  taskForm.remark = task.remark || ''
}

const showCreateTask = () => {
  selectedTask.value = null
  isEditTask.value = false
  taskForm.name = ''
  taskForm.processId = ''
  taskForm.remark = ''
}

const cancelEdit = () => {
  selectedTask.value = null
  isEditTask.value = false
  taskForm.name = ''
  taskForm.processId = ''
  taskForm.remark = ''
}

const saveTask = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saveLoading.value = true
    try {
      const process = processes.value.find(p => p.id === taskForm.processId)
      if (isEditTask.value && selectedTask.value) {
        const result = await apiPut(`/task/${selectedTask.value.id}`, {
          name: taskForm.name,
          processId: taskForm.processId,
          processName: process?.name || '',
          remark: taskForm.remark
        })
        if (result.code === 0) {
          ElMessage.success('任务更新成功')
          await loadTasks()
          cancelEdit()
        } else {
          ElMessage.error(result.message || '更新失败')
        }
      } else {
        const result = await apiPost('/task', {
          name: taskForm.name,
          processId: taskForm.processId,
          processName: process?.name || '',
          remark: taskForm.remark,
          status: 'pending'
        })
        if (result.code === 0) {
          ElMessage.success('任务创建成功')
          await loadTasks()
          cancelEdit()
        } else {
          ElMessage.error(result.message || '创建失败')
        }
      }
    } catch {
      ElMessage.error('请求失败')
    } finally {
      saveLoading.value = false
    }
  })
}

const executeTask = async () => {
  const processId = taskForm.processId || selectedTask.value?.processId
  if (!processId) {
    ElMessage.warning('请先为任务绑定一个流程')
    return
  }
  executing.value = true
  try {
    const taskName = taskForm.name || selectedTask.value?.name || '任务'
    const result = await apiPost(`/process/${processId}/execute`, {
      name: taskName
    })
    if (result.code === 0 || result.success) {
      ElMessage.success('流程执行已启动')
      await loadLogs()
    } else {
      ElMessage.error(result.message || '执行失败')
    }
  } catch {
    ElMessage.error('执行请求失败')
  } finally {
    executing.value = false
  }
}

const runProcess = async (process) => {
  executing.value = true
  try {
    const result = await apiPost(`/process/${process.id}/execute`, {
      name: process.name
    })
    if (result.code === 0 || result.success) {
      ElMessage.success(`流程"${process.name}"执行已启动`)
      await loadLogs()
    } else {
      ElMessage.error(result.message || '执行失败')
    }
  } catch {
    ElMessage.error('执行请求失败')
  } finally {
    executing.value = false
  }
}

onMounted(() => {
  loadTasks()
  loadProcesses()
  loadLogs()
})
</script>

<style scoped>
.tasks-page { max-width: 1600px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.main-layout { display: flex; gap: 20px; height: calc(100vh - 180px); }

.task-list-panel { width: 280px; background: #fff; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.config-panel { flex: 1; background: #fff; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.log-panel { width: 300px; background: #fff; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }

.panel-header { padding: 16px 20px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.panel-header h3 { font-size: 16px; font-weight: 600; margin: 0; }

.task-filter { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; }
.task-list { flex: 1; overflow-y: auto; padding: 12px; }
.task-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-radius: 8px; cursor: pointer; margin-bottom: 8px; transition: all 0.2s; border: 1px solid transparent; }
.task-item:hover { background: #f5f7fa; }
.task-item.active { background: #e6f4ff; border-color: #1890ff; }
.task-info { display: flex; flex-direction: column; gap: 2px; }
.task-name { font-weight: 500; color: #262626; }
.task-process { font-size: 12px; color: #8c8c8c; }

.config-content { padding: 20px; border-bottom: 1px solid #f0f0f0; }
.config-actions { display: flex; gap: 12px; margin-top: 20px; }

.process-list-section { flex: 1; overflow-y: auto; padding: 16px 20px; }
.section-header { margin-bottom: 12px; }
.section-header h4 { font-size: 14px; font-weight: 600; margin: 0; }
.process-list { display: flex; flex-direction: column; gap: 10px; }
.process-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; background: #fafafa; border-radius: 8px; }
.process-item:hover { background: #f0f0f0; }
.process-info { display: flex; flex-direction: column; gap: 2px; }
.process-name { font-weight: 500; color: #262626; }
.process-desc { font-size: 12px; color: #8c8c8c; }
.process-ops { display: flex; gap: 8px; }

.log-list { flex: 1; overflow-y: auto; padding: 12px; }
.log-item { padding: 12px 14px; background: #fafafa; border-radius: 8px; margin-bottom: 10px; }
.log-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.log-name { font-weight: 500; font-size: 13px; }
.log-info { display: flex; justify-content: space-between; font-size: 12px; color: #8c8c8c; }
.log-message { font-size: 12px; color: #666; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>