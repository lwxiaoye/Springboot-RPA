<template>
  <div class="tasks-page">
    <div class="page-header">
      <h2>{{ t('task.title') }}</h2>
      <p class="page-desc">{{ t('task.description') }}</p>
    </div>

    <div class="main-layout">
      <!-- 左侧：任务列表 -->
      <div class="task-list-panel">
        <div class="panel-header">
          <h3>{{ t('task.taskList') }}</h3>
          <el-button type="primary" size="small" @click="showCreateTask">
            <el-icon><Plus /></el-icon> {{ t('task.create') }}
          </el-button>
        </div>
        <div class="task-filter">
          <el-input v-model="taskSearch" :placeholder="t('task.searchPlaceholder')" size="small" clearable>
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
              <span class="task-process">{{ t('task.process') }}: {{ task.processName || '-' }}</span>
            </div>
            <div class="task-actions">
              <el-tag size="small" :type="getStatusType(task.status)">{{ getStatusText(task.status) }}</el-tag>
              <el-button link type="danger" size="small" @click.stop="deleteTask(task)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          <el-empty v-if="filteredTasks.length === 0" :description="t('task.noTasks')" />
        </div>
      </div>

      <!-- 中间：任务配置 -->
      <div class="config-panel">
        <div class="panel-header">
          <h3>{{ isEditTask ? t('task.editTask') : t('task.taskConfig') }}</h3>
        </div>

        <div class="config-content">
          <el-form :model="taskForm" :rules="formRules" ref="formRef" :label-width="t('task.labelWidth')">
            <el-form-item :label="t('task.taskName')" prop="name">
              <el-input v-model="taskForm.name" :placeholder="t('task.taskNamePlaceholder')" />
            </el-form-item>
            <el-form-item :label="t('task.bindProcess')" prop="processId">
              <div class="process-bind-list">
                <div v-for="(pid, index) in taskForm.processIds" :key="index" class="process-bind-item">
                  <el-select v-model="taskForm.processIds[index]" :placeholder="t('task.selectProcessPlaceholder')" style="flex: 1">
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
                  <el-icon><Plus /></el-icon> {{ t('task.addProcess') }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item :label="t('task.remark')">
              <el-input v-model="taskForm.remark" type="textarea" :rows="3" :placeholder="t('task.remarkPlaceholder')" />
            </el-form-item>
          </el-form>

          <div class="config-actions">
            <el-button @click="cancelEdit" v-if="isEditTask">{{ t('task.cancel') }}</el-button>
            <el-button type="primary" @click="saveTask" :loading="saveLoading">
              {{ isEditTask ? t('task.saveChanges') : t('task.createTask') }}
            </el-button>
            <el-button type="success" @click="executeTask" :disabled="!hasValidProcessIds" :loading="executeLoading">
              <el-icon><VideoPlay /></el-icon>
              {{ t('task.executeNow') }}
            </el-button>
            <el-button type="danger" @click="deleteTask(selectedTask)" v-if="isEditTask && selectedTask" :loading="deleteLoading">
              <el-icon><Delete /></el-icon>
              {{ t('task.deleteTask') }}
            </el-button>
          </div>
        </div>

        <!-- 已创建的流程（显示当前任务绑定的流程） -->
        <div class="process-list-section">
          <div class="section-header">
            <h4>{{ isEditTask ? t('task.boundProcesses') : t('task.processList') }}</h4>
          </div>
          <div class="process-list" v-if="isEditTask && selectedTask">
            <div v-for="process in boundProcesses" :key="process.id" class="process-item bound">
              <div class="process-info">
                <div class="process-name-row">
                  <span class="process-name">{{ process.name }}</span>
                  <el-tag size="small" type="success">{{ t('task.bound') }}</el-tag>
                </div>
                <span class="process-desc">{{ process.description || t('task.noDescription') }}</span>
                <span class="process-meta">{{ t('task.code') }}: {{ process.code }} | {{ t('task.version') }}: {{ process.version }}</span>
              </div>
              <div class="process-ops">
                <el-button link type="primary" size="small" @click="runProcess(process)">
                  <el-icon><VideoPlay /></el-icon> {{ t('task.execute') }}
                </el-button>
              </div>
            </div>
            <el-empty v-if="boundProcesses.length === 0" :description="t('task.noBoundProcess')" />
          </div>
          <div class="process-list" v-else>
            <div class="tip-text">
              {{ taskForm.processIds.some(pid => pid) ? t('task.processSelected') : t('task.selectTaskFirst') }}
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：执行记录 -->
      <div class="log-panel">
        <div class="panel-header">
          <h3>{{ t('task.executionLogs') }}</h3>
          <el-button size="small" @click="loadLogs">{{ t('task.refresh') }}</el-button>
        </div>
        <div class="log-list">
          <div v-for="log in recentLogs" :key="log.id" class="log-item">
            <div class="log-header">
              <span class="log-name">{{ log.taskName || log.name || t('task.task') }}</span>
              <el-tag size="small" :type="getStatusType(log.status)">{{ getStatusText(log.status) }}</el-tag>
            </div>
            <div class="log-info">
              <span>{{ log.startTime || log.createdAt || '-' }}</span>
              <span v-if="log.duration">{{ log.duration }}</span>
            </div>
            <div class="log-message" v-if="log.message">{{ log.message }}</div>
          </div>
          <el-empty v-if="recentLogs.length === 0" :description="t('task.noLogs')" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, VideoPlay, Plus, Delete } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const saveLoading = ref(false)
const executeLoading = ref(false)
const deleteLoading = ref(false)
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

// 校验任务名称是否重复
async function validateTaskName(rule, value, callback) {
  if (!value || value.trim() === '') {
    callback()
    return
  }
  try {
    const result = await apiPost('/task/check-name', {
      name: value.trim(),
      excludeId: selectedTask.value?.id || null
    })
    if (result.code === 0 && result.data === true) {
      callback(new Error(t('task.taskNameExists')))
    } else {
      callback()
    }
  } catch {
    callback()
  }
}

const formRules = computed(() => ({
  name: [
    { required: true, message: t('task.taskNameRequired'), trigger: 'blur' },
    { validator: validateTaskName, trigger: ['blur', 'change'] }
  ]
}))

const filteredTasks = computed(() => {
  if (!taskSearch.value) {
    return [...taskList.value].sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
  }
  const kw = taskSearch.value.toLowerCase()
  return taskList.value.filter(t => (t.name || '').toLowerCase().includes(kw))
})

// 是否有有效的流程绑定
const hasValidProcessIds = computed(() => {
  return taskForm.processIds.some(pid => pid) || selectedTask.value?.processId
})

// 获取当前任务/选择器绑定的流程详情
const boundProcesses = computed(() => {
  // 获取所有有效的流程ID
  const boundIds = selectedTask.value?.processIds
    ? ((() => {
        try {
          const parsed = JSON.parse(selectedTask.value.processIds)
          return Array.isArray(parsed) ? parsed : [selectedTask.value.processId]
        } catch {
          return selectedTask.value.processId ? [selectedTask.value.processId] : []
        }
      })())
    : taskForm.processIds.filter(pid => pid)

  if (!boundIds || boundIds.length === 0) return []

  // 查找对应的流程详情
  return boundIds
    .map(id => processes.value.find(p => String(p.id) === String(id)))
    .filter(p => p != null)
})

const getStatusText = (s) => {
  const map = {
    success: t('task.statusSuccess'),
    failed: t('task.statusFailed'),
    running: t('task.statusRunning'),
    pending: t('task.statusPending'),
    completed: t('task.statusCompleted')
  }
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
      ElMessage.warning(t('task.selectAtLeastOneProcess'))
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
          ElMessage.success(t('task.updateSuccess'))
          await loadTasks()
          cancelEdit()
        } else {
          ElMessage.error(result.message || t('task.updateFailed'))
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
          ElMessage.success(t('task.createSuccess'))
          await loadTasks()
          cancelEdit()
        } else {
          ElMessage.error(result.message || t('task.createFailed'))
        }
      }
    } catch {
      ElMessage.error(t('task.requestFailed'))
    } finally {
      saveLoading.value = false
    }
  })
}

const executeTask = async () => {
  const processIds = taskForm.processIds.filter(pid => pid)
  if (processIds.length === 0) {
    ElMessage.warning(t('task.selectProcessFirst'))
    return
  }
  executeLoading.value = true
  try {
    const taskName = taskForm.name || selectedTask.value?.name || t('task.task')
    // 批量执行多个流程
    for (const processId of processIds) {
      const process = processes.value.find(p => p.id === processId)
      const result = await apiPost(`/process/${processId}/execute`, {
        name: taskName + '-' + (process?.name || '')
      })
      if (result.code !== 0 && !result.success) {
        ElMessage.error(`${process?.name || t('task.process')}${t('task.executeFailed')}: ${result.message || t('task.unknownError')}`)
      }
    }
    ElMessage.success(t('task.startedExecution', { count: processIds.length }))
    await loadLogs()
  } catch {
    ElMessage.error(t('task.executeRequestFailed'))
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
      ElMessage.success(t('task.processExecutionStarted', { name: process.name }))
      await loadLogs()
    } else {
      ElMessage.error(result.message || t('task.executeFailed'))
    }
  } catch {
    ElMessage.error(t('task.executeRequestFailed'))
  } finally {
    executeLoading.value = false
  }
}

const deleteTask = async (task) => {
  if (!task) {
    ElMessage.warning(t('task.selectTaskFirst'))
    return
  }
  try {
    await ElMessageBox.confirm(
      t('task.confirmDelete', { name: task.name }),
      t('task.deleteConfirm'),
      {
        confirmButtonText: t('task.confirm'),
        cancelButtonText: t('task.cancel'),
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    deleteLoading.value = true
    const result = await apiDelete(`/task/${task.id}`)
    if (result.code === 0) {
      ElMessage.success(t('task.deleteSuccess'))
      cancelEdit()
      await loadTasks()
    } else {
      ElMessage.error(result.message || t('task.deleteFailed'))
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(t('task.deleteRequestFailed'))
    }
  } finally {
    deleteLoading.value = false
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
.task-info { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
.task-name { font-weight: 500; color: #262626; }
.task-process { font-size: 12px; color: #8c8c8c; }
.task-actions { display: flex; align-items: center; gap: 8px; margin-left: 8px; }

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
.process-item.bound { background: #f0f7ff; border: 1px solid #d9ecff; }
.process-item.bound:hover { background: #e6f0ff; }
.process-info { display: flex; flex-direction: column; gap: 2px; }
.process-name { font-weight: 500; color: #262626; }
.process-name-row { display: flex; align-items: center; gap: 8px; }
.process-desc { font-size: 12px; color: #8c8c8c; }
.process-meta { font-size: 11px; color: #999; }
.process-ops { display: flex; gap: 8px; }
.tip-text { color: #999; font-size: 13px; text-align: center; padding: 20px; }

.log-list { flex: 1; overflow-y: auto; padding: 12px; }
.log-item { padding: 12px 14px; background: #fafafa; border-radius: 8px; margin-bottom: 10px; }
.log-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.log-name { font-weight: 500; font-size: 13px; }
.log-info { display: flex; justify-content: space-between; font-size: 12px; color: #8c8c8c; }
.log-message { font-size: 12px; color: #666; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>