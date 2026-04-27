<template>
  <div class="queue-page">
    <div class="page-header">
      <h2>队列与触发器</h2>
      <p class="page-desc">管理任务队列和自动化触发器配置</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-box" @click="currentTab = 'queue'">
        <span class="stat-num">{{ queueStats.total }}</span>
        <span class="stat-label">队列总数</span>
      </div>
      <div class="stat-box pending" @click="currentTab = 'queue'">
        <span class="stat-num">{{ queueStats.pending }}</span>
        <span class="stat-label">待处理任务</span>
      </div>
      <div class="stat-box running" @click="currentTab = 'queue'">
        <span class="stat-num">{{ queueStats.running }}</span>
        <span class="stat-label">执行中</span>
      </div>
      <div class="stat-box success" @click="currentTab = 'queue'">
        <span class="stat-num">{{ queueStats.completed }}</span>
        <span class="stat-label">已完成</span>
      </div>
    </div>

    <!-- Tab切换 -->
    <el-tabs v-model="currentTab" class="queue-tabs">
      <!-- 队列管理 -->
      <el-tab-pane label="队列管理" name="queue">
        <div class="toolbar">
          <div class="search-box">
            <el-icon><Search /></el-icon>
            <input v-model="searchKeyword" placeholder="搜索队列名称..." />
          </div>
          <el-button type="primary" @click="showCreateQueueModal">
            <el-icon><Plus /></el-icon> 创建队列
          </el-button>
        </div>

        <el-table :data="filteredQueues" v-loading="loading" border stripe>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="name" label="队列名称" min-width="150" />
          <el-table-column prop="type" label="队列类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type || '通用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="maxConcurrent" label="最大并发" width="100" align="center" />
          <el-table-column prop="priority" label="优先级" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.priority === 'high' ? 'danger' : row.priority === 'normal' ? 'warning' : 'info'" size="small">
                {{ row.priority === 'high' ? '高' : row.priority === 'normal' ? '普通' : '低' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.status" active-value="active" inactive-value="inactive" @change="toggleQueueStatus(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="taskCount" label="任务数" width="80" align="center" />
          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewQueueDetail(row)">详情</el-button>
              <el-button link type="primary" @click="editQueue(row)">编辑</el-button>
              <el-popconfirm title="确认删除该队列吗？" @confirm="deleteQueue(row)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 触发器管理 -->
      <el-tab-pane label="触发器管理" name="trigger">
        <div class="toolbar">
          <div class="search-box">
            <el-icon><Search /></el-icon>
            <input v-model="triggerSearchKeyword" placeholder="搜索触发器名称..." />
          </div>
          <el-button type="primary" @click="showCreateTriggerModal">
            <el-icon><Plus /></el-icon> 创建触发器
          </el-button>
        </div>

        <el-table :data="paginatedTriggers" v-loading="triggerLoading" border stripe class="unified-table">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="name" label="触发器名称" min-width="150" />
          <el-table-column prop="type" label="触发类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getTriggerTypeTag(row.type)" size="small">{{ getTriggerTypeText(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetQueue" label="目标队列" min-width="120" />
          <el-table-column prop="cron" label="Cron表达式" min-width="120" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.status" active-value="active" inactive-value="inactive" @change="toggleTriggerStatus(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="lastTrigger" label="最后触发" min-width="160" />
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="editTrigger(row)">编辑</el-button>
              <el-popconfirm title="确认删除该触发器吗？" @confirm="deleteTrigger(row)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 任务投递 -->
      <el-tab-pane label="任务投递" name="submit">
        <div class="submit-section">
          <el-form :model="taskForm" label-width="120px" style="max-width: 600px;">
            <el-form-item label="目标队列">
              <el-select v-model="taskForm.queueId" placeholder="请选择队列" style="width: 100%;">
                <el-option v-for="q in queues" :key="q.id" :label="q.name" :value="q.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="任务数据">
              <el-input v-model="taskForm.data" type="textarea" :rows="6" placeholder="请输入JSON格式的任务数据" />
            </el-form-item>
            <el-form-item label="优先级">
              <el-select v-model="taskForm.priority" style="width: 100%;">
                <el-option label="高" value="high" />
                <el-option label="普通" value="normal" />
                <el-option label="低" value="low" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitTask" :loading="submitLoading">提交任务</el-button>
              <el-button @click="batchImportVisible = true">批量导入</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建/编辑队列弹窗 -->
    <el-dialog v-model="queueDialogVisible" :title="queueDialogTitle" width="500px">
      <el-form :model="queueForm" :rules="queueRules" ref="queueFormRef" label-width="100px">
        <el-form-item label="队列名称" prop="name">
          <el-input v-model="queueForm.name" placeholder="请输入队列名称" />
        </el-form-item>
        <el-form-item label="队列类型">
          <el-select v-model="queueForm.type" style="width: 100%;">
            <el-option label="通用" value="general" />
            <el-option label="发票处理" value="invoice" />
            <el-option label="订单审核" value="order" />
            <el-option label="数据录入" value="dataEntry" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大并发">
          <el-input-number v-model="queueForm.maxConcurrent" :min="1" :max="100" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="queueForm.priority" style="width: 100%;">
            <el-option label="高" value="high" />
            <el-option label="普通" value="normal" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="queueForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="queueDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitQueue" :loading="queueSubmitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 创建/编辑触发器弹窗 -->
    <el-dialog v-model="triggerDialogVisible" :title="triggerDialogTitle" width="500px">
      <el-form :model="triggerForm" :rules="triggerRules" ref="triggerFormRef" label-width="100px">
        <el-form-item label="触发器名称" prop="name">
          <el-input v-model="triggerForm.name" placeholder="请输入触发器名称" />
        </el-form-item>
        <el-form-item label="触发类型" prop="type">
          <el-select v-model="triggerForm.type" style="width: 100%;">
            <el-option label="定时触发" value="cron" />
            <el-option label="事件触发" value="event" />
            <el-option label="手动触发" value="manual" />
            <el-option label="API触发" value="api" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标队列" prop="targetQueue">
          <el-select v-model="triggerForm.targetQueue" style="width: 100%;">
            <el-option v-for="q in queues" :key="q.id" :label="q.name" :value="q.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="Cron表达式" v-if="triggerForm.type === 'cron'">
          <el-input v-model="triggerForm.cron" placeholder="如: 0 0 * * * ?" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="triggerForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triggerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTrigger" :loading="triggerSubmitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="batchImportVisible" title="批量导入任务" width="600px">
      <el-form label-width="100px">
        <el-form-item label="目标队列">
          <el-select v-model="batchForm.queueId" style="width: 100%;">
            <el-option v-for="q in queues" :key="q.id" :label="q.name" :value="q.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="CSV文件">
          <el-upload ref="uploadRef" action="#" :auto-upload="false" :limit="1" accept=".csv">
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持CSV格式，每行一个任务数据</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchImportVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchImport">导入</el-button>
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
const triggerLoading = ref(false)
const submitLoading = ref(false)
const queueSubmitLoading = ref(false)
const triggerSubmitLoading = ref(false)

const currentTab = ref('queue')
const searchKeyword = ref('')
const triggerSearchKeyword = ref('')
const batchImportVisible = ref(false)

const queueDialogVisible = ref(false)
const triggerDialogVisible = ref(false)
const isEditQueue = ref(false)
const isEditTrigger = ref(false)
const currentQueueId = ref(null)
const currentTriggerId = ref(null)

const queues = ref([])
const triggers = ref([])
const uploadRef = ref(null)

const queueStats = reactive({ total: 0, pending: 0, running: 0, completed: 0 })

const queueForm = reactive({
  name: '',
  type: 'general',
  maxConcurrent: 5,
  priority: 'normal',
  description: ''
})

const triggerForm = reactive({
  name: '',
  type: 'cron',
  targetQueue: '',
  cron: '',
  description: ''
})

const taskForm = reactive({
  queueId: '',
  data: '',
  priority: 'normal'
})

const batchForm = reactive({
  queueId: '',
  file: null
})

const queueRules = {
  name: [{ required: true, message: '请输入队列名称', trigger: 'blur' }]
}

const triggerRules = {
  name: [{ required: true, message: '请输入触发器名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择触发类型', trigger: 'change' }],
  targetQueue: [{ required: true, message: '请选择目标队列', trigger: 'change' }]
}

const queueDialogTitle = computed(() => isEditQueue.value ? '编辑队列' : '创建队列')
const triggerDialogTitle = computed(() => isEditTrigger.value ? '编辑触发器' : '创建触发器')

const filteredQueues = computed(() => {
  if (!searchKeyword.value) return queues.value
  return queues.value.filter(q => q.name.includes(searchKeyword.value))
})

const filteredTriggers = computed(() => {
  if (!triggerSearchKeyword.value) return triggers.value
  return triggers.value.filter(t => t.name.includes(triggerSearchKeyword.value))
})

const getTriggerTypeTag = (type) => {
  const map = { cron: 'success', event: 'warning', manual: 'info', api: 'primary' }
  return map[type] || 'info'
}

const getTriggerTypeText = (type) => {
  const map = { cron: '定时触发', event: '事件触发', manual: '手动触发', api: 'API触发' }
  return map[type] || type
}

const loadQueues = async () => {
  loading.value = true
  try {
    // 模拟数据，实际应从后端获取
    queues.value = [
      { id: 1, name: '发票处理队列', type: 'invoice', maxConcurrent: 5, priority: 'high', status: 'active', taskCount: 23 },
      { id: 2, name: '订单审核队列', type: 'order', maxConcurrent: 3, priority: 'normal', status: 'active', taskCount: 15 },
      { id: 3, name: '数据录入队列', type: 'dataEntry', maxConcurrent: 10, priority: 'low', status: 'inactive', taskCount: 0 }
    ]
    queueStats.total = queues.value.length
    queueStats.pending = queues.value.reduce((sum, q) => sum + (q.taskCount || 0), 0)
    queueStats.running = Math.floor(queueStats.pending * 0.2)
    queueStats.completed = Math.floor(Math.random() * 100)
  } catch (e) {
    queues.value = []
  } finally {
    loading.value = false
  }
}

const loadTriggers = async () => {
  triggerLoading.value = true
  try {
    triggers.value = [
      { id: 1, name: '每日数据同步', type: 'cron', targetQueue: '数据录入队列', cron: '0 0 2 * * ?', status: 'active', lastTrigger: '2026-03-30 02:00:00' },
      { id: 2, name: '发票OCR识别', type: 'event', targetQueue: '发票处理队列', cron: '-', status: 'active', lastTrigger: '2026-03-30 10:30:00' },
      { id: 3, name: '订单自动审核', type: 'api', targetQueue: '订单审核队列', cron: '-', status: 'active', lastTrigger: '2026-03-30 11:45:00' }
    ]
  } catch (e) {
    triggers.value = []
  } finally {
    triggerLoading.value = false
  }
}

const showCreateQueueModal = () => {
  isEditQueue.value = false
  Object.assign(queueForm, { name: '', type: 'general', maxConcurrent: 5, priority: 'normal', description: '' })
  queueDialogVisible.value = true
}

const editQueue = (queue) => {
  isEditQueue.value = true
  currentQueueId.value = queue.id
  Object.assign(queueForm, queue)
  queueDialogVisible.value = true
}

const submitQueue = async () => {
  queueSubmitLoading.value = true
  try {
    if (isEditQueue.value) {
      const index = queues.value.findIndex(q => q.id === currentQueueId.value)
      if (index !== -1) {
        queues.value[index] = { ...queues.value[index], ...queueForm }
      }
      ElMessage.success('队列更新成功')
    } else {
      const newQueue = { id: Date.now(), ...queueForm, status: 'active', taskCount: 0 }
      queues.value.push(newQueue)
      ElMessage.success('队列创建成功')
    }
    queueDialogVisible.value = false
    await loadQueues()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    queueSubmitLoading.value = false
  }
}

const deleteQueue = async (queue) => {
  const index = queues.value.findIndex(q => q.id === queue.id)
  if (index !== -1) {
    queues.value.splice(index, 1)
    await loadQueues()
    ElMessage.success('队列删除成功')
  }
}

const toggleQueueStatus = async (queue) => {
  ElMessage.success(`队列${queue.status === 'active' ? '启用' : '禁用'}成功`)
}

const viewQueueDetail = (queue) => {
  ElMessage.info(`队列详情: ${queue.name}，当前任务数: ${queue.taskCount}`)
}

const showCreateTriggerModal = () => {
  isEditTrigger.value = false
  Object.assign(triggerForm, { name: '', type: 'cron', targetQueue: '', cron: '', description: '' })
  triggerDialogVisible.value = true
}

const editTrigger = (trigger) => {
  isEditTrigger.value = true
  currentTriggerId.value = trigger.id
  Object.assign(triggerForm, trigger)
  triggerDialogVisible.value = true
}

const submitTrigger = async () => {
  triggerSubmitLoading.value = true
  try {
    if (isEditTrigger.value) {
      const index = triggers.value.findIndex(t => t.id === currentTriggerId.value)
      if (index !== -1) {
        triggers.value[index] = { ...triggers.value[index], ...triggerForm }
      }
      ElMessage.success('触发器更新成功')
    } else {
      const newTrigger = { id: Date.now(), ...triggerForm, status: 'active', lastTrigger: '-' }
      triggers.value.push(newTrigger)
      ElMessage.success('触发器创建成功')
    }
    triggerDialogVisible.value = false
    await loadTriggers()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    triggerSubmitLoading.value = false
  }
}

const deleteTrigger = async (trigger) => {
  const index = triggers.value.findIndex(t => t.id === trigger.id)
  if (index !== -1) {
    triggers.value.splice(index, 1)
    ElMessage.success('触发器删除成功')
  }
}

const toggleTriggerStatus = async (trigger) => {
  ElMessage.success(`触发器${trigger.status === 'active' ? '启用' : '禁用'}成功`)
}

const submitTask = async () => {
  if (!taskForm.queueId) {
    ElMessage.warning('请选择目标队列')
    return
  }
  if (!taskForm.data) {
    ElMessage.warning('请输入任务数据')
    return
  }
  submitLoading.value = true
  try {
    ElMessage.success('任务提交成功')
    taskForm.data = ''
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

const handleBatchImport = () => {
  ElMessage.success('批量导入成功')
  batchImportVisible.value = false
}

onMounted(() => {
  loadQueues()
  loadTriggers()
})
</script>

<style scoped>
.queue-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-box {
  flex: 1;
  background: white;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.stat-box:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-box .stat-num {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #1e3a4a;
}

.stat-box .stat-label {
  font-size: 13px;
  color: #8c8c8c;
}

.stat-box.pending { border-left: 4px solid #e6a23c; }
.stat-box.running { border-left: 4px solid #409eff; }
.stat-box.success { border-left: 4px solid #67c23a; }

.queue-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }

.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; background: var(--bg-secondary, #ffffff); padding: 12px 16px; border-radius: 10px; border: 1px solid var(--border-color, #e5e7eb); }

/* 统一表格样式 */
.unified-table :deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  padding: 12px 0;
  border-bottom: 2px solid #e5e7eb;
}
.unified-table :deep(.el-table__body-wrapper td) {
  padding: 10px 0;
  vertical-align: middle;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}
.unified-table :deep(.el-table__header-wrapper th .cell),
.unified-table :deep(.el-table__body-wrapper td .cell) {
  padding: 0 6px;
}
.unified-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}
.unified-table :deep(.el-table__row:hover > td) {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.submit-section {
  background: white;
  padding: 24px;
  border-radius: 12px;
}
</style>