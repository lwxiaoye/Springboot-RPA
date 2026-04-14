<template>
  <div class="tasks-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-section">
          <div class="title-icon">
            <el-icon><Schedule /></el-icon>
          </div>
          <div class="title-text">
            <h1 class="page-title">任务调度中心</h1>
            <p class="page-subtitle">编排自动化流程，监控任务执行状态</p>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showCreateTask" class="btn-primary">
          <el-icon><Plus /></el-icon>
          新建任务
        </el-button>
      </div>
    </div>

    <!-- 主内容区 - 三栏布局 -->
    <div class="main-content">
      <!-- 左侧：任务列表 -->
      <div class="panel panel-left">
        <div class="panel-header">
          <div class="panel-title">
            <span class="title-badge task-count">{{ filteredTasks.length }}</span>
            <span>任务列表</span>
          </div>
        </div>

        <div class="search-box">
          <el-input
            v-model="taskSearch"
            placeholder="搜索任务..."
            clearable
            class="search-input"
          >
            <template #prefix>
              <el-icon class="search-icon"><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="task-list">
          <div
            v-for="(task, index) in filteredTasks"
            :key="task.id"
            class="task-item"
            :class="{ active: selectedTask?.id === task.id }"
            @click="selectTask(task)"
          >
            <div class="task-number">{{ index + 1 }}</div>
            <div class="task-content">
              <div class="task-name">{{ task.name }}</div>
              <div class="task-info-row">
                <span class="process-tag">
                  <el-icon><Connection /></el-icon>
                  {{ task.processName || '未绑定' }}
                </span>
              </div>
            </div>
            <div class="task-status">
              <span class="status-dot" :class="getStatusClass(task.status)"></span>
            </div>
          </div>

          <div v-if="filteredTasks.length === 0" class="empty-state">
            <el-icon class="empty-icon"><Folder /></el-icon>
            <p>暂无任务</p>
          </div>
        </div>
      </div>

      <!-- 中间：任务配置 -->
      <div class="panel panel-center">
        <div class="panel-header">
          <div class="panel-title">
            <el-icon><EditPen /></el-icon>
            <span>{{ isEditTask ? '编辑任务' : '任务配置' }}</span>
          </div>
          <span class="panel-hint">{{ isEditTask ? '修改任务参数' : '创建新的自动化任务' }}</span>
        </div>

        <div class="config-area">
          <el-form :model="taskForm" :rules="formRules" ref="formRef" label-position="top" class="config-form">
            <div class="form-section">
              <div class="form-section-title">
                <el-icon><Calendar /></el-icon>
                基本信息
              </div>

              <el-form-item label="任务名称" prop="name" class="form-item-modern">
                <el-input
                  v-model="taskForm.name"
                  placeholder="输入任务名称"
                  size="large"
                  class="modern-input"
                >
                  <template #prefix>
                    <el-icon><Edit /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="任务备注" class="form-item-modern">
                <el-input
                  v-model="taskForm.remark"
                  type="textarea"
                  :rows="3"
                  placeholder="添加任务描述（可选）"
                  resize="none"
                  class="modern-input"
                />
              </el-form-item>
            </div>

            <div class="form-section">
              <div class="form-section-title">
                <el-icon><Link /></el-icon>
                流程绑定
                <el-tag size="small" type="info" class="section-tag">{{ taskForm.processIds.filter(p => p).length }} 个流程</el-tag>
              </div>

              <div class="process-chain">
                <div v-for="(pid, index) in taskForm.processIds" :key="index" class="process-node">
                  <div class="node-connector" v-if="index > 0">
                    <svg viewBox="0 0 24 24" width="20" height="20">
                      <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </div>
                  <div class="process-selector">
                    <el-select
                      v-model="taskForm.processIds[index]"
                      placeholder="选择流程"
                      size="large"
                      clearable
                      class="process-select"
                    >
                      <el-option
                        v-for="p in processes"
                        :key="p.id"
                        :label="p.name"
                        :value="p.id"
                      >
                        <div class="process-option">
                          <span class="option-name">{{ p.name }}</span>
                          <code class="option-code">{{ p.code }}</code>
                        </div>
                      </el-option>
                    </el-select>
                  </div>
                  <el-button
                    v-if="taskForm.processIds.length > 1"
                    circle
                    type="danger"
                    size="small"
                    @click="removeProcessBind(index)"
                    class="btn-remove"
                  >
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>

                <el-button type="primary" text @click="addProcessBind" class="btn-add-process">
                  <el-icon><Plus /></el-icon>
                  添加流程
                </el-button>
              </div>
            </div>
          </el-form>

          <div class="action-bar">
            <el-button @click="cancelEdit" v-if="isEditTask" plain class="btn-cancel">
              取消编辑
            </el-button>
            <el-button type="primary" @click="saveTask" :loading="saveLoading" class="btn-save">
              <el-icon><Check /></el-icon>
              {{ isEditTask ? '保存修改' : '创建任务' }}
            </el-button>
            <el-button type="success" @click="executeTask" :disabled="!hasValidProcessIds" :loading="executeLoading" class="btn-execute">
              <el-icon><VideoPlay /></el-icon>
              立即执行
            </el-button>
          </div>
        </div>

        <!-- 可用流程列表 -->
        <div class="process-list-section">
          <div class="section-header">
            <span class="section-title">可用流程</span>
            <span class="section-count">{{ processes.length }} 个</span>
          </div>
          <div class="process-grid">
            <div
              v-for="process in processes"
              :key="process.id"
              class="process-card"
              @click="runProcess(process)"
            >
              <div class="process-icon-box">
                <el-icon><Box /></el-icon>
              </div>
              <div class="process-details">
                <div class="process-name">{{ process.name }}</div>
                <code class="process-code">{{ process.code }}</code>
              </div>
              <div class="process-action">
                <el-icon><VideoPlay /></el-icon>
              </div>
            </div>

            <div v-if="processes.length === 0" class="empty-process">
              <el-icon class="empty-icon"><Document /></el-icon>
              <p>暂无可用流程</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：执行记录 -->
      <div class="panel panel-right">
        <div class="panel-header">
          <div class="panel-title">
            <el-icon><Clock /></el-icon>
            <span>执行记录</span>
          </div>
          <el-button size="small" text @click="loadLogs" class="btn-refresh">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>

        <div class="log-timeline">
          <div v-for="log in recentLogs" :key="log.id" class="log-entry">
            <div class="log-time-badge">
              <span class="time-text">{{ formatTime(log.startTime || log.createdAt) }}</span>
            </div>
            <div class="log-content">
              <div class="log-header">
                <span class="log-name">{{ log.taskName || log.name || '任务' }}</span>
                <span class="log-status" :class="getStatusClass(log.status)">
                  {{ getStatusText(log.status) }}
                </span>
              </div>
              <div class="log-info" v-if="log.duration">
                <el-icon><Timer /></el-icon>
                <span>{{ log.duration }}</span>
              </div>
              <div class="log-message" v-if="log.message">{{ log.message }}</div>
            </div>
          </div>

          <div v-if="recentLogs.length === 0" class="empty-state">
            <el-icon class="empty-icon"><List /></el-icon>
            <p>暂无执行记录</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, VideoPlay, Plus, Close, Check,
  Calendar, Edit, Link, Connection,
  Clock, Timer, Refresh, List, Box,
  Document, Folder, EditPen, Schedule
} from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut } from '../../utils/api.js'

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
  name: [{ required: true, message: '请输入任务��称', trigger: 'blur' }]
}

const filteredTasks = computed(() => {
  if (!taskSearch.value) return taskList.value
  const kw = taskSearch.value.toLowerCase()
  return taskList.value.filter(t => (t.name || '').toLowerCase().includes(kw))
})

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

const getStatusClass = (s) => {
  const map = { success: 'success', failed: 'danger', running: 'running', pending: 'pending', completed: 'success' }
  return map[s] || 'pending'
}

const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`
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

const addProcessBind = () => {
  taskForm.processIds.push('')
}

const removeProcessBind = (index) => {
  if (taskForm.processIds.length > 1) {
    taskForm.processIds.splice(index, 1)
  }
}

const getSelectedProcessNames = () => {
  return taskForm.processIds
    .filter(pid => pid)
    .map(pid => {
      const p = processes.value.find(proc => proc.id === pid)
      return p ? p.name : ''
    })
    .filter(name => name)
}

const getFirstProcessId = () => {
  const firstValid = taskForm.processIds.find(pid => pid)
  return firstValid || null
}

const saveTask = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

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
.tasks-container {
  max-width: 1600px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-secondary, #6b7280);
  margin: 0;
}

.header-actions .el-button {
  border-radius: var(--radius-md, 12px);
  font-weight: 600;
  padding: 12px 24px;
}

/* 主布局 */
.content-layout {
  display: grid;
  grid-template-columns: 320px 1fr 320px;
  gap: 24px;
  height: calc(100vh - 220px);
}

/* 面板通用样式 */
.panel-left,
.panel-center,
.panel-right {
  background: var(--bg-secondary, #ffffff);
  border-radius: var(--radius-lg, 16px);
  border: 1px solid var(--border-color, #e5e7eb);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
  background: var(--bg-tertiary, #fafbfc);
}

.panel-header h2,
.panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
}

.panel-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-tertiary, #9ca3af);
}

/* 左侧：任务列表 */
.search-section {
  padding: 16px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.search-input {
  width: 100%;
}

.task-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.task-card {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 12px;
  background: var(--bg-tertiary, #f9fafb);
}

.task-card:hover {
  background: var(--bg-primary, #f5f7fa);
  border-color: var(--primary-light, #66b1ff);
  transform: translateX(4px);
}

.task-card.active {
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 100%);
  border-color: var(--primary, #409eff);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.task-main {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.task-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.task-icon.primary { background: linear-gradient(135deg, #409eff, #66b1ff); color: white; }
.task-icon.success { background: linear-gradient(135deg, #67c23a, #85ce61); color: white; }
.task-icon.warning { background: linear-gradient(135deg, #e6a23c, #ebb563); color: white; }
.task-icon.danger { background: linear-gradient(135deg, #f56c6c, #f78989); color: white; }
.task-icon.info { background: linear-gradient(135deg, #909399, #a6a9ad); color: white; }

.task-info {
  flex: 1;
  min-width: 0;
}

.task-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.process-name {
  font-size: 12px;
  color: var(--text-secondary, #6b7280);
}

/* 中间：配置区 */
.config-form {
  padding: 24px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color, #e5e7eb);
}

.process-bind-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.process-bind-item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.process-bind-item .el-select {
  flex: 1;
}

.option-code {
  float: right;
  color: var(--text-tertiary, #9ca3af);
  font-size: 12px;
}

/* 流程区域 */
.process-section {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
}

.process-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.process-card {
  background: var(--bg-tertiary, #f9fafb);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 12px;
  padding: 20px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.process-card:hover {
  border-color: var(--primary, #409eff);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.process-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.process-card .process-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.process-code {
  font-size: 12px;
  color: var(--text-secondary, #6b7280);
  font-family: 'Monaco', 'Menlo', monospace;
  background: var(--bg-primary, #f5f7fa);
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
}

/* 右侧：执行记录 */
.log-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.log-item {
  padding: 14px;
  background: var(--bg-tertiary, #f9fafb);
  border-radius: 10px;
  margin-bottom: 12px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.log-item:hover {
  border-color: var(--primary-light, #66b1ff);
  background: var(--bg-primary, #f5f7fa);
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.log-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.log-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-tertiary, #9ca3af);
  margin-bottom: 6px;
}

.log-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.log-message {
  font-size: 12px;
  color: var(--text-secondary, #6b7280);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 空状态 */
.el-empty {
  padding: 40px 0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .content-layout {
    grid-template-columns: 280px 1fr 280px;
  }
}

@media (max-width: 1024px) {
  .content-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .panel-left,
  .panel-center,
  .panel-right {
    min-height: 400px;
  }
}

/* 滚动条 */
.task-list::-webkit-scrollbar,
.process-section::-webkit-scrollbar,
.log-list::-webkit-scrollbar {
  width: 6px;
}

.task-list::-webkit-scrollbar-track,
.process-section::-webkit-scrollbar-track,
.log-list::-webkit-scrollbar-track {
  background: transparent;
}

.task-list::-webkit-scrollbar-thumb,
.process-section::-webkit-scrollbar-thumb,
.log-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.task-list::-webkit-scrollbar-thumb:hover,
.process-section::-webkit-scrollbar-thumb:hover,
.log-list::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>