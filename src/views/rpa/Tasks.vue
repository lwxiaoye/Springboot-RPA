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
            <el-form-item label="绑定流程" prop="processId">
              <div class="process-bind-list">
                <div v-for="(pid, index) in taskForm.processIds" :key="index" class="process-bind-item">
                  <el-select v-model="taskForm.processIds[index]" placeholder="请选择流程" style="flex: 1">
                    <el-option v-for="p in processes" :key="p.id" :label="p.name" :value="p.id">
                      <span>{{ p.name }}</span>
                      <el-tag size="small" type="info" style="margin-left: 8px;">{{ p.code }}</el-tag>
                    </el-option>
                  </el-select>
                  <el-button link type="danger" @click="removeProcessBind(index)" :disabled="taskForm.processIds.length <= 1">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                <el-button type="primary" link @click="addProcessBind">
                  <el-icon><Plus /></el-icon> 添加流程
                </el-button>
              </div>
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
            <el-button type="success" @click="executeTask" :disabled="!hasValidProcessIds" :loading="executeLoading">
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
import { Search, VideoPlay, Plus, Delete } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const saveLoading = ref(false)
const executeLoading = ref(false)
const taskList = ref([])
const processes = ref([])
const selectedTask = ref(null)
const taskSearch = ref('')
const recentLogs = ref([])
const formRef = ref(null)
const isEditTask = ref(false)

const taskForm = reactive({
  name: '',
  processIds: [''],
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

// 是否有有效的流程绑定
const hasValidProcessIds = computed(() => {
  return taskForm.processIds.some(pid => pid) || selectedTask.value?.processId
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
  try {
    const result = await apiGet('/process')
    if (result.code === 0) {
      processes.value = result.data || []
    }
  } catch {
    processes.value = []
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
  // 解析多个流程ID
  if (task.processIds) {
    try {
      const parsed = JSON.parse(task.processIds)
      taskForm.processIds = Array.isArray(parsed) && parsed.length > 0 ? parsed : ['']
    } catch {
      taskForm.processIds = task.processId ? [task.processId] : ['']
    }
  } else {
    taskForm.processIds = task.processId ? [task.processId] : ['']
  }
  taskForm.remark = task.remark || ''
}

const showCreateTask = () => {
  selectedTask.value = null
  isEditTask.value = false
  taskForm.name = ''
  taskForm.processIds = ['']
  taskForm.remark = ''
}

const cancelEdit = () => {
  selectedTask.value = null
  isEditTask.value = false
  taskForm.name = ''
  taskForm.processIds = ['']
  taskForm.remark = ''
}

// 添加流程绑定
const addProcessBind = () => {
  taskForm.processIds.push('')
}

// 删除流程绑定
const removeProcessBind = (index) => {
  if (taskForm.processIds.length > 1) {
    taskForm.processIds.splice(index, 1)
  }
}

// 获取已选择的流程名称列表
const getSelectedProcessNames = () => {
  return taskForm.processIds
    .filter(pid => pid) // 过滤空值
    .map(pid => {
      const p = processes.value.find(proc => proc.id === pid)
      return p ? p.name : ''
    })
    .filter(name => name)
}

// 获取第一个有效的流程ID
const getFirstProcessId = () => {
  const firstValid = taskForm.processIds.find(pid => pid)
  return firstValid || null
}

const saveTask = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    // 过滤空值
    const validProcessIds = taskForm.processIds.filter(pid => pid)
    if (validProcessIds.length === 0) {
      ElMessage.warning('请至少选择一个流程')
      return
    }

    saveLoading.value = true
    try {
      const processNames = getSelectedProcessNames()

      if (isEditTask.value && selectedTask.value) {
        const result = await apiPut(`/task/${selectedTask.value.id}`, {
          name: taskForm.name,
          processId: getFirstProcessId(),
          processIds: validProcessIds,
          processName: processNames[0] || '',
          processNames: processNames,
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
          processId: getFirstProcessId(),
          processIds: validProcessIds,
          processName: processNames[0] || '',
          processNames: processNames,
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
  const processIds = taskForm.processIds.filter(pid => pid)
  if (processIds.length === 0) {
    ElMessage.warning('请先选择要执行的流程')
    return
  }
  executeLoading.value = true
  try {
    const taskName = taskForm.name || selectedTask.value?.name || '任务'
    // 批量执行多个流程
    for (const processId of processIds) {
      const process = processes.value.find(p => p.id === processId)
      const result = await apiPost(`/process/${processId}/execute`, {
        name: taskName + '-' + (process?.name || '')
      })
      if (result.code !== 0 && !result.success) {
        ElMessage.error(`${process?.name || '流程'}执行失败: ${result.message || '未知错误'}`)
      }
    }
    ElMessage.success(`已启动执行 ${processIds.length} 个流程`)
    await loadLogs()
  } catch {
    ElMessage.error('执行请求失败')
  } finally {
    executeLoading.value = false
  }
}

const runProcess = async (process) => {
  executeLoading.value = true
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
    executeLoading.value = false
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
.process-bind-list { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.process-bind-item { display: flex; gap: 8px; align-items: center; }
.process-bind-item .el-select { flex: 1; }

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