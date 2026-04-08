<template>
  <div class="queues-page">
    <div class="page-header">
      <h2>队列管理</h2>
      <p class="page-desc">管理任务队列，实现任务的统一调度和负载均衡</p>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon primary"><el-icon><Operation /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">队列总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon success"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.active }}</span>
          <span class="stat-label">运行中</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon warning"><el-icon><Timer /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.pendingTasks }}</span>
          <span class="stat-label">待处理任务</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon danger"><el-icon><Cpu /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.runningTasks }}</span>
          <span class="stat-label">执行中任务</span>
        </div>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索队列名称/编码..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="运行中" value="active" />
        <el-option label="已暂停" value="paused" />
        <el-option label="已停止" value="stopped" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 新建队列
      </el-button>
    </div>

    <el-table :data="paginatedQueues" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="队列名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="code" label="队列编码" min-width="120">
        <template #default="{ row }">
          <span class="code-text">{{ row.code }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="priorityLevel" label="优先级" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getPriorityType(row.priorityLevel)" size="small">
            {{ getPriorityText(row.priorityLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="任务统计" width="200" align="center">
        <template #default="{ row }">
          <div class="task-stats">
            <span class="stat-item pending">
              <el-icon><Clock /></el-icon>
              {{ row.currentPendingCount || 0 }}
            </span>
            <span class="stat-item running">
              <el-icon><VideoPlay /></el-icon>
              {{ row.currentRunningCount || 0 }}/{{ row.maxConcurrentTasks || 0 }}
            </span>
            <span class="stat-item completed">
              <el-icon><CircleCheck /></el-icon>
              {{ row.completedCount || 0 }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="requiredCategories" label="机器人分类" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.requiredCategories" class="category-tags">
            {{ formatCategories(row.requiredCategories) }}
          </span>
          <el-tag v-else type="info" size="small">全部</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="creator" label="创建人" width="100" align="center" />
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="warning" @click="toggleStatus(row)">
            {{ row.status === 'active' ? '暂停' : '启用' }}
          </el-button>
          <el-popconfirm title="确认删除该队列吗？" @confirm="deleteQueue(row)">
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

    <!-- 新建/编辑队列弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="queueForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="队列名称" prop="name">
          <el-input v-model="queueForm.name" placeholder="请输入队列名称" />
        </el-form-item>
        <el-form-item label="队列编码" prop="code">
          <el-input v-model="queueForm.code" placeholder="请输入队列编码，如：INVOICE_QUEUE" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="queueForm.priorityLevel" style="width: 100%">
            <el-option :value="1" label="低优先级" />
            <el-option :value="2" label="普通优先级" />
            <el-option :value="3" label="高优先级" />
            <el-option :value="4" label="紧急优先级" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大并发">
          <el-input-number v-model="queueForm.maxConcurrentTasks" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="机器人分类">
          <el-select v-model="queueForm.requiredCategories" multiple placeholder="选择需要的机器人分类" style="width: 100%">
            <el-option v-for="cat in categoryList" :key="cat.code" :label="cat.name" :value="cat.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="queueForm.description" type="textarea" :rows="3" placeholder="请输入队列描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitQueue" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="队列详情" width="650px">
      <div class="detail-content">
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <div class="detail-grid">
            <div class="detail-item">
              <label>队列名称：</label>
              <span>{{ currentQueue.name }}</span>
            </div>
            <div class="detail-item">
              <label>队列编码：</label>
              <span class="code-text">{{ currentQueue.code }}</span>
            </div>
            <div class="detail-item">
              <label>状态：</label>
              <el-tag :type="getStatusType(currentQueue.status)" size="small">{{ getStatusText(currentQueue.status) }}</el-tag>
            </div>
            <div class="detail-item">
              <label>优先级：</label>
              <el-tag :type="getPriorityType(currentQueue.priorityLevel)" size="small">{{ getPriorityText(currentQueue.priorityLevel) }}</el-tag>
            </div>
            <div class="detail-item">
              <label>最大并发：</label>
              <span>{{ currentQueue.maxConcurrentTasks }} 个任务</span>
            </div>
            <div class="detail-item">
              <label>创建人：</label>
              <span>{{ currentQueue.creator || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">实时统计</div>
          <div class="stats-grid">
            <div class="mini-stat-card pending">
              <div class="stat-icon"><el-icon><Clock /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentQueue.currentPendingCount || 0 }}</span>
                <span class="label">排队中</span>
              </div>
            </div>
            <div class="mini-stat-card running">
              <div class="stat-icon"><el-icon><VideoPlay /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentQueue.currentRunningCount || 0 }}</span>
                <span class="label">执行中</span>
              </div>
            </div>
            <div class="mini-stat-card success">
              <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentQueue.completedCount || 0 }}</span>
                <span class="label">已完成</span>
              </div>
            </div>
            <div class="mini-stat-card danger">
              <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentQueue.failedCount || 0 }}</span>
                <span class="label">已失败</span>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="currentQueue.description">
          <div class="section-title">描述</div>
          <div class="detail-desc">{{ currentQueue.description }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Operation, CircleCheck, Timer, Cpu, Clock, VideoPlay, CircleClose } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentQueue = ref({})
const formRef = ref(null)

const queues = ref([])
const categoryList = ref([
  { code: 'DATA_COLLECT', name: '数据采集' },
  { code: 'DATA_PARSE', name: '数据解析' },
  { code: 'DATA_PROCESS', name: '数据加工' },
  { code: 'DATA_STORE', name: '数据落库' },
  { code: 'GENERAL', name: '通用执行' }
])
const searchKeyword = ref('')
const statusFilter = ref('')

const pagination = reactive({ page: 1, size: 10, total: 0 })

const stats = reactive({
  total: 0,
  active: 0,
  pendingTasks: 0,
  runningTasks: 0
})

const queueForm = reactive({
  name: '',
  code: '',
  description: '',
  priorityLevel: 2,
  maxConcurrentTasks: 5,
  requiredCategories: [],
  creator: 'admin'
})

const formRules = {
  name: [{ required: true, message: '请输入队列名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入队列编码', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑队列' : '新建队列')

const filteredQueues = computed(() => {
  let list = queues.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(q => q.name?.toLowerCase().includes(kw) || q.code?.toLowerCase().includes(kw))
  }
  if (statusFilter.value) {
    list = list.filter(q => q.status === statusFilter.value)
  }
  pagination.total = list.length
  return list
})

const paginatedQueues = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredQueues.value.slice(start, end)
})

const updateStats = () => {
  stats.total = queues.value.length
  stats.active = queues.value.filter(q => q.status === 'active').length
  stats.pendingTasks = queues.value.reduce((sum, q) => sum + (q.currentPendingCount || 0), 0)
  stats.runningTasks = queues.value.reduce((sum, q) => sum + (q.currentRunningCount || 0), 0)
}

const getPriorityText = (level) => {
  const map = { 1: '低', 2: '普通', 3: '高', 4: '紧急' }
  return map[level] || '普通'
}

const getPriorityType = (level) => {
  const map = { 1: 'info', 2: '', 3: 'warning', 4: 'danger' }
  return map[level] || ''
}

const getStatusText = (s) => {
  const map = { active: '运行中', paused: '已暂停', stopped: '已停止' }
  return map[s] || s || '-'
}

const getStatusType = (s) => {
  const map = { active: 'success', paused: 'warning', stopped: 'danger' }
  return map[s] || 'info'
}

const formatCategories = (categories) => {
  if (!categories) return ''
  try {
    const arr = typeof categories === 'string' ? JSON.parse(categories) : categories
    return arr.map(c => {
      const cat = categoryList.value.find(x => x.code === c)
      return cat ? cat.name : c
    }).join(', ')
  } catch {
    return categories
  }
}

const loadQueues = async () => {
  loading.value = true
  try {
    const result = await apiGet('/queue')
    if (result.code === 0) {
      queues.value = result.data || []
      updateStats()
    }
  } catch {
    queues.value = []
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isEdit.value = false
  Object.assign(queueForm, {
    name: '',
    code: '',
    description: '',
    priorityLevel: 2,
    maxConcurrentTasks: 5,
    requiredCategories: []
  })
  dialogVisible.value = true
}

const viewDetail = (queue) => {
  currentQueue.value = queue
  detailVisible.value = true
}

const toggleStatus = async (queue) => {
  try {
    const newStatus = queue.status === 'active' ? 'paused' : 'active'
    const result = await apiPut(`/queue/${queue.id}/status`, { status: newStatus })
    if (result.code === 0) {
      ElMessage.success(`队列${newStatus === 'active' ? '已启用' : '已暂停'}`)
      await loadQueues()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const submitQueue = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const data = {
        ...queueForm,
        requiredCategories: JSON.stringify(queueForm.requiredCategories)
      }
      let result
      if (isEdit.value && currentQueue.value.id) {
        result = await apiPut(`/queue/${currentQueue.value.id}`, data)
      } else {
        result = await apiPost('/queue', data)
      }
      if (result.code === 0) {
        ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        await loadQueues()
      } else {
        ElMessage.error(result.message || '操作失败')
      }
    } catch {
      ElMessage.error('请求失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const deleteQueue = async (queue) => {
  try {
    const result = await apiDelete(`/queue/${queue.id}`)
    if (result.code === 0) {
      ElMessage.success('删除成功')
      await loadQueues()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => { loadQueues() })
</script>

<style scoped>
.queues-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 16px 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
}

.stat-icon.primary { background: linear-gradient(135deg, #409eff, #66b1ff); }
.stat-icon.success { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-icon.warning { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.stat-icon.danger { background: linear-gradient(135deg, #f56c6c, #f78989); }

.stat-content { display: flex; flex-direction: column; }
.stat-value { font-size: 22px; font-weight: bold; color: #1e3a4a; }
.stat-label { font-size: 12px; color: #8c8c8c; }

.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }

.code-text { font-family: 'Courier New', monospace; color: #409eff; background: #f0f7ff; padding: 2px 6px; border-radius: 4px; font-size: 12px; }

.task-stats { display: flex; gap: 8px; justify-content: center; }
.stat-item { display: flex; align-items: center; gap: 3px; font-size: 12px; }
.stat-item.pending { color: #e6a23c; }
.stat-item.running { color: #409eff; }
.stat-item.completed { color: #67c23a; }

.category-tags { color: #606266; font-size: 12px; }

/* 详情弹窗 */
.detail-content { padding: 8px 0; }
.detail-section { margin-bottom: 20px; }
.section-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5; }
.detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.detail-item { display: flex; gap: 8px; align-items: center; }
.detail-item label { color: #909399; font-size: 13px; min-width: 70px; }
.detail-item span { color: #303133; font-size: 13px; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.mini-stat-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.mini-stat-card .stat-icon { width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: white; }
.mini-stat-card.pending .stat-icon { background: #e6a23c; }
.mini-stat-card.running .stat-icon { background: #409eff; }
.mini-stat-card.success .stat-icon { background: #67c23a; }
.mini-stat-card.danger .stat-icon { background: #f56c6c; }
.mini-stat-card .stat-info { display: flex; flex-direction: column; }
.mini-stat-card .stat-info .value { font-size: 18px; font-weight: bold; color: #303133; }
.mini-stat-card .stat-info .label { font-size: 11px; color: #909399; }

.detail-desc { color: #606266; font-size: 13px; line-height: 1.6; }

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
