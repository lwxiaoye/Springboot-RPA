<template>
  <div class="triggers-page">
    <div class="page-header">
      <h2>触发器管理</h2>
      <p class="page-desc">配置定时、文件、API、Webhook触发规则，自动化执行流程</p>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon primary"><el-icon><Timer /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">触发器总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon success"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.active }}</span>
          <span class="stat-label">启用中</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon warning"><el-icon><Clock /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.totalTriggers }}</span>
          <span class="stat-label">累计触发</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon danger"><el-icon><TrendCharts /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.successRate }}%</span>
          <span class="stat-label">触发成功率</span>
        </div>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索触发器名称/编码..." />
      </div>
      <el-select v-model="typeFilter" placeholder="类型筛选" clearable style="width: 120px;">
        <el-option label="定时触发" value="schedule" />
        <el-option label="文件触发" value="file" />
        <el-option label="API触发" value="api" />
        <el-option label="Webhook" value="webhook" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="启用" value="active" />
        <el-option label="暂停" value="paused" />
        <el-option label="禁用" value="disabled" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 新建触发器
      </el-button>
    </div>

    <el-table :data="paginatedTriggers" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="触发器名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="code" label="触发器编码" min-width="130">
        <template #default="{ row }">
          <span class="code-text">{{ row.code }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="triggerType" label="触发类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTriggerTypeTag(row.triggerType)" size="small">
            {{ getTriggerTypeText(row.triggerType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="processName" label="关联流程" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.processName" class="process-link">{{ row.processName }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="cron" label="执行规则" min-width="120">
        <template #default="{ row }">
          <span v-if="row.cron" class="cron-text">{{ row.cron }}</span>
          <span v-else-if="row.scheduleTime" class="schedule-text">每天 {{ row.scheduleTime }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="触发统计" width="140" align="center">
        <template #default="{ row }">
          <div class="trigger-stats">
            <span class="stat-total">{{ row.totalTriggers || 0 }}</span>
            <span class="stat-detail">
              <span class="success">{{ row.successTriggers || 0 }} 成功</span>
              <span class="failed">{{ row.failedTriggers || 0 }} 失败</span>
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="lastTriggerTime" label="最后触发" min-width="150">
        <template #default="{ row }">
          {{ formatDateTime(row.lastTriggerTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="success" @click="triggerNow(row)" :loading="triggeringId === row.id">触发</el-button>
          <el-button link type="primary" @click="editTrigger(row)">编辑</el-button>
          <el-button link type="info" @click="viewDetail(row)">详情</el-button>
          <el-button link type="warning" @click="toggleStatus(row)">
            {{ row.status === 'active' ? '暂停' : '启用' }}
          </el-button>
          <el-popconfirm title="确认删除该触发器吗？" @confirm="deleteTrigger(row)">
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

    <!-- 新建/编辑触发器弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" class="trigger-dialog">
      <el-form :model="triggerForm" :rules="formRules" ref="formRef" label-width="110px">
        <el-form-item label="触发器名称" prop="name">
          <el-input v-model="triggerForm.name" placeholder="请输入触发器名称" />
        </el-form-item>
        <el-form-item label="触发器编码" prop="code">
          <el-input v-model="triggerForm.code" placeholder="请输入触发器编码，如：DAILY_INVOICE" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="触发类型" prop="triggerType">
          <el-select v-model="triggerForm.triggerType" placeholder="选择触发类型" style="width: 100%" @change="onTriggerTypeChange">
            <el-option value="schedule" label="定时触发" />
            <el-option value="file" label="文件触发" />
            <el-option value="api" label="API触发" />
            <el-option value="webhook" label="Webhook触发" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联流程" prop="processId">
          <el-select v-model="triggerForm.processId" placeholder="选择关联的流程" style="width: 100%" filterable>
            <el-option v-for="p in processes" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联队列">
          <el-select v-model="triggerForm.queueId" placeholder="选择关联的队列（可选）" style="width: 100%" clearable filterable>
            <el-option v-for="q in queues" :key="q.id" :label="q.name" :value="q.id" />
          </el-select>
        </el-form-item>

        <!-- 定时触发配置 -->
        <template v-if="triggerForm.triggerType === 'schedule'">
          <el-form-item label="定时规则">
            <el-radio-group v-model="scheduleMode">
              <el-radio label="cron">Cron表达式</el-radio>
              <el-radio label="simple">简单配置</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="scheduleMode === 'cron'" label="Cron表达式">
            <el-input v-model="triggerForm.cron" placeholder="如：0 0 2 * * ? (每天凌晨2点)" />
            <div class="form-tip">格式：秒 分 时 日 月 周 年</div>
          </el-form-item>
          <el-form-item v-if="scheduleMode === 'simple'" label="执行时间">
            <el-time-picker v-model="triggerForm.scheduleTimeObj" format="HH:mm" value-format="HH:mm" placeholder="选择时间" style="width: 100%" />
          </el-form-item>
          <el-form-item v-if="scheduleMode === 'simple'" label="执行周期">
            <el-select v-model="triggerForm.scheduleType" style="width: 100%" @change="onScheduleTypeChange">
              <el-option value="day" label="每天" />
              <el-option value="week" label="每周" />
              <el-option value="month" label="每月" />
            </el-select>
          </el-form-item>
          <!-- 每周：选择星期几 -->
          <el-form-item v-if="scheduleMode === 'simple' && triggerForm.scheduleType === 'week'" label="选择星期">
            <el-checkbox-group v-model="selectedWeekDays">
              <el-checkbox label="1">周一</el-checkbox>
              <el-checkbox label="2">周二</el-checkbox>
              <el-checkbox label="3">周三</el-checkbox>
              <el-checkbox label="4">周四</el-checkbox>
              <el-checkbox label="5">周五</el-checkbox>
              <el-checkbox label="6">周六</el-checkbox>
              <el-checkbox label="7">周日</el-checkbox>
            </el-checkbox-group>
            <div class="form-tip">可选择多个星期</div>
          </el-form-item>
          <!-- 每月：选择日期 -->
          <el-form-item v-if="scheduleMode === 'simple' && triggerForm.scheduleType === 'month'" label="选择日期">
            <el-select v-model="selectedMonthDay" placeholder="选择日期" style="width: 100%">
              <el-option v-for="day in 31" :key="day" :label="day + '日'" :value="String(day)" />
            </el-select>
            <div class="form-tip">选择每月的具体日期（1-31）</div>
          </el-form-item>
        </template>

        <!-- 文件触发配置 -->
        <template v-if="triggerForm.triggerType === 'file'">
          <el-form-item label="监控目录">
            <el-input v-model="triggerForm.watchPath" placeholder="如：C:/invoices" />
          </el-form-item>
          <el-form-item label="文件匹配规则">
            <el-input v-model="triggerForm.filePattern" placeholder="如：*.pdf, *.jpg" />
          </el-form-item>
          <el-form-item label="监控子目录">
            <el-switch v-model="triggerForm.watchSubdirs" />
          </el-form-item>
        </template>

        <!-- API触发配置 -->
        <template v-if="triggerForm.triggerType === 'api'">
          <el-form-item label="API密钥">
            <el-input v-model="triggerForm.apiKey" placeholder="请输入API密钥" />
            <div class="form-tip">用于接口调用认证</div>
          </el-form-item>
          <el-form-item label="请求方法">
            <el-radio-group v-model="triggerForm.httpMethod">
              <el-radio label="POST">POST</el-radio>
              <el-radio label="GET">GET</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- Webhook配置 -->
        <template v-if="triggerForm.triggerType === 'webhook'">
          <el-form-item label="Webhook URL">
            <el-input v-model="triggerForm.webhookUrl" placeholder="请输入Webhook接收地址" />
          </el-form-item>
        </template>

        <el-form-item label="触发后自动启动">
          <el-switch v-model="triggerForm.autoStart" />
          <div class="form-tip">关闭后触发仅投递任务到队列，不自动执行</div>
        </el-form-item>

        <el-form-item label="最大并发">
          <el-input-number v-model="triggerForm.maxConcurrent" :min="1" :max="10" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="triggerForm.description" type="textarea" :rows="2" placeholder="请输入触发器描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTrigger" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="触发器详情" width="650px">
      <div class="detail-content">
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <div class="detail-grid">
            <div class="detail-item"><label>触发器名称：</label><span>{{ currentTrigger.name }}</span></div>
            <div class="detail-item"><label>触发器编码：</label><span class="code-text">{{ currentTrigger.code }}</span></div>
            <div class="detail-item"><label>触发类型：</label><el-tag :type="getTriggerTypeTag(currentTrigger.triggerType)" size="small">{{ getTriggerTypeText(currentTrigger.triggerType) }}</el-tag></div>
            <div class="detail-item"><label>状态：</label><el-tag :type="getStatusTag(currentTrigger.status)" size="small">{{ getStatusText(currentTrigger.status) }}</el-tag></div>
            <div class="detail-item"><label>关联流程：</label><span>{{ currentTrigger.processName || '-' }}</span></div>
            <div class="detail-item"><label>关联队列：</label><span>{{ currentTrigger.queueName || '-' }}</span></div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">执行配置</div>
          <div class="detail-grid">
            <div class="detail-item" v-if="currentTrigger.cron"><label>Cron表达式：</label><span class="cron-text">{{ currentTrigger.cron }}</span></div>
            <div class="detail-item" v-if="currentTrigger.scheduleTime"><label>执行时间：</label><span>每天 {{ currentTrigger.scheduleTime }}</span></div>
            <div class="detail-item"><label>自动启动：</label><span>{{ currentTrigger.autoStart ? '是' : '否' }}</span></div>
            <div class="detail-item"><label>最大并发：</label><span>{{ currentTrigger.maxConcurrent }} 个任务</span></div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">触发统计</div>
          <div class="stats-grid">
            <div class="mini-stat-card">
              <div class="stat-icon primary"><el-icon><Document /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentTrigger.totalTriggers || 0 }}</span>
                <span class="label">总触发次数</span>
              </div>
            </div>
            <div class="mini-stat-card success">
              <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentTrigger.successTriggers || 0 }}</span>
                <span class="label">成功次数</span>
              </div>
            </div>
            <div class="mini-stat-card danger">
              <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentTrigger.failedTriggers || 0 }}</span>
                <span class="label">失败次数</span>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">最近执行</div>
          <div class="detail-grid">
            <div class="detail-item"><label>最后触发：</label><span>{{ formatDateTime(currentTrigger.lastTriggerTime) }}</span></div>
            <div class="detail-item"><label>最后成功：</label><span>{{ formatDateTime(currentTrigger.lastSuccessTime) }}</span></div>
        <div class="detail-item"><label>最后失败：</label><span>{{ formatDateTime(currentTrigger.lastFailedTime) }}</span></div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Timer, CircleCheck, Clock, TrendCharts, Document, CircleClose } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const loading = ref(false)
const submitLoading = ref(false)
const triggeringId = ref(null)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentTrigger = ref({})
const formRef = ref(null)

const triggers = ref([])
const processes = ref([])
const queues = ref([])
const searchKeyword = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const scheduleMode = ref('cron')
const selectedWeekDays = ref([])
const selectedMonthDay = ref('')

const pagination = reactive({ page: 1, size: 10, total: 0 })

const stats = reactive({
  total: 0,
  active: 0,
  totalTriggers: 0,
  successRate: 0
})

const triggerForm = reactive({
  name: '',
  code: '',
  description: '',
  triggerType: 'schedule',
  processId: null,
  processName: '',
  queueId: null,
  queueName: '',
  cron: '',
  scheduleTimeObj: null,
  scheduleTime: '',
  scheduleType: 'day',
  watchPath: '',
  filePattern: '',
  watchSubdirs: false,
  apiKey: '',
  webhookUrl: '',
  httpMethod: 'POST',
  autoStart: true,
  maxConcurrent: 1,
  creator: 'admin'
})

const formRules = {
  name: [{ required: true, message: '请输入触发器名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入触发器编码', trigger: 'blur' }],
  triggerType: [{ required: true, message: '请选择触发类型', trigger: 'change' }],
  processId: [{ required: true, message: '请选择关联流程', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑触发器' : '新建触发器')

const filteredTriggers = computed(() => {
  let list = triggers.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(t => t.name?.toLowerCase().includes(kw) || t.code?.toLowerCase().includes(kw))
  }
  if (typeFilter.value) {
    list = list.filter(t => t.triggerType === typeFilter.value)
  }
  if (statusFilter.value) {
    list = list.filter(t => t.status === statusFilter.value)
  }
  pagination.total = list.length
  return list
})

const paginatedTriggers = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredTriggers.value.slice(start, end)
})

const updateStats = () => {
  stats.total = triggers.value.length
  stats.active = triggers.value.filter(t => t.status === 'active').length
  stats.totalTriggers = triggers.value.reduce((sum, t) => sum + (t.totalTriggers || 0), 0)
  const totalSuccess = triggers.value.reduce((sum, t) => sum + (t.successTriggers || 0), 0)
  const totalAll = triggers.value.reduce((sum, t) => sum + (t.totalTriggers || 0), 0)
  stats.successRate = totalAll > 0 ? Math.round((totalSuccess / totalAll) * 100) : 0
}

const getTriggerTypeText = (type) => {
  const map = { schedule: '定时', file: '文件', api: 'API', webhook: 'Webhook' }
  return map[type] || type || '-'
}

const getTriggerTypeTag = (type) => {
  const map = { schedule: 'primary', file: 'success', api: 'warning', webhook: 'danger' }
  return map[type] || 'info'
}

const getStatusText = (s) => {
  const map = { active: '启用', paused: '暂停', disabled: '禁用' }
  return map[s] || s || '-'
}

const getStatusTag = (s) => {
  const map = { active: 'success', paused: 'warning', disabled: 'danger' }
  return map[s] || 'info'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  // 处理 ISO 8601 格式 (2026-04-15T18:25:41)
  if (typeof dateTime === 'string' && dateTime.includes('T')) {
    return dateTime.replace('T', ' ')
  }
  return dateTime
}

const loadTriggers = async () => {
  loading.value = true
  try {
    const result = await apiGet('/trigger')
    if (result.code === 0) {
      triggers.value = result.data || []
      updateStats()
    }
  } catch {
    triggers.value = []
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

const loadQueues = async () => {
  try {
    const result = await apiGet('/queue')
    if (result.code === 0) {
      queues.value = result.data || []
    }
  } catch {
    queues.value = []
  }
}

const showCreateModal = () => {
  isEdit.value = false
  currentTrigger.value = {}
  Object.assign(triggerForm, {
    name: '',
    code: '',
    description: '',
    triggerType: 'schedule',
    processId: null,
    queueId: null,
    cron: '',
    scheduleTimeObj: null,
    scheduleType: 'day',
    scheduleDays: '',
    watchPath: '',
    filePattern: '',
    watchSubdirs: false,
    apiKey: '',
    webhookUrl: '',
    httpMethod: 'POST',
    autoStart: true,
    maxConcurrent: 1
  })
  selectedWeekDays.value = []
  selectedMonthDay.value = ''
  scheduleMode.value = 'cron'
  dialogVisible.value = true
}

const editTrigger = (trigger) => {
  isEdit.value = true
  currentTrigger.value = { ...trigger }
  
  // 填充表单数据
  Object.assign(triggerForm, {
    name: trigger.name || '',
    code: trigger.code || '',
    description: trigger.description || '',
    triggerType: trigger.triggerType || 'schedule',
    processId: trigger.processId || null,
    queueId: trigger.queueId || null,
    cron: trigger.cron || '',
    scheduleTimeObj: trigger.scheduleTime || null,
    scheduleTime: trigger.scheduleTime || '',
    scheduleType: trigger.scheduleType || 'day',
    scheduleDays: trigger.scheduleDays || '',
    watchPath: trigger.watchPath || '',
    filePattern: trigger.filePattern || '',
    watchSubdirs: trigger.watchSubdirs || false,
    apiKey: trigger.apiKey || '',
    webhookUrl: trigger.webhookUrl || '',
    httpMethod: trigger.httpMethod || 'POST',
    autoStart: trigger.autoStart !== false,
    maxConcurrent: trigger.maxConcurrent || 1
  })
  
  // 解析scheduleDays到选择器
  if (trigger.scheduleDays) {
    if (trigger.scheduleType === 'week') {
      // 周的格式："1,2,3" -> ['1','2','3']
      selectedWeekDays.value = trigger.scheduleDays.split(',').filter(d => d)
    } else if (trigger.scheduleType === 'month') {
      // 月的格式："15" -> "15"
      selectedMonthDay.value = trigger.scheduleDays
    }
  } else {
    selectedWeekDays.value = []
    selectedMonthDay.value = ''
  }
  
  // 根据是否有cron表达式设置定时模式
  scheduleMode.value = trigger.cron ? 'cron' : 'simple'
  dialogVisible.value = true
}

const onTriggerTypeChange = () => {
  triggerForm.cron = ''
  triggerForm.scheduleTimeObj = null
}

const onScheduleTypeChange = () => {
  // 切换周期类型时清空相关选择
  selectedWeekDays.value = []
  selectedMonthDay.value = ''
  triggerForm.scheduleDays = ''
}

const viewDetail = (trigger) => {
  currentTrigger.value = trigger
  detailVisible.value = true
}

const toggleStatus = async (trigger) => {
  try {
    const newStatus = trigger.status === 'active' ? 'paused' : 'active'
    const result = await apiPut(`/trigger/${trigger.id}/status`, { status: newStatus })
    if (result.code === 0) {
      ElMessage.success(`触发器${newStatus === 'active' ? '已启用' : '已暂停'}`)
      await loadTriggers()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const triggerNow = async (trigger) => {
  triggeringId.value = trigger.id
  try {
    const result = await apiPost(`/trigger/${trigger.id}/trigger`)
    if (result.code === 0) {
      ElMessage.success('触发成功')
      await loadTriggers()
    } else {
      ElMessage.error(result.message || '触发失败')
    }
  } catch {
    ElMessage.error('触发失败')
  } finally {
    triggeringId.value = null
  }
}

const submitTrigger = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const process = processes.value.find(p => p.id === triggerForm.processId)
      const queue = queues.value.find(q => q.id === triggerForm.queueId)

      // 处理scheduleDays字段
      let scheduleDaysValue = ''
      if (triggerForm.scheduleType === 'week' && selectedWeekDays.value.length > 0) {
        // 周：将数组转为逗号分隔的字符串，如 "1,2,3"
        scheduleDaysValue = selectedWeekDays.value.join(',')
      } else if (triggerForm.scheduleType === 'month' && selectedMonthDay.value) {
        // 月：直接使用选中的日期
        scheduleDaysValue = selectedMonthDay.value
      }

      const data = {
        ...triggerForm,
        processName: process?.name || '',
        queueName: queue?.name || '',
        scheduleTime: triggerForm.scheduleTimeObj,
        scheduleDays: scheduleDaysValue
      }

      let result
      if (isEdit.value && currentTrigger.value.id) {
        result = await apiPut('/trigger/' + currentTrigger.value.id, data)
      } else {
        result = await apiPost('/trigger', data)
      }

      if (result.code === 0) {
        ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        await loadTriggers()
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

const deleteTrigger = async (trigger) => {
  try {
    const result = await apiDelete(`/trigger/${trigger.id}`, {})
    if (result.code === 0) {
      ElMessage.success('删除成功')
      await loadTriggers()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => {
  loadTriggers()
  loadProcesses()
  loadQueues()
})
</script>

<style scoped>
.triggers-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.page-desc {
  font-size: 14px;
  color: var(--text-secondary, #6b7280);
  margin: 0;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-secondary, #ffffff);
  padding: 24px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--border-color, #e5e7eb);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0, 0, 0, 0.1));
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.stat-icon.primary { background: linear-gradient(135deg, #409eff, #66b1ff); }
.stat-icon.success { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-icon.warning { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.stat-icon.danger { background: linear-gradient(135deg, #f56c6c, #f78989); }

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary, #6b7280);
  font-weight: 500;
}

/* 工具栏 */
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: var(--bg-secondary, #ffffff);
  border-radius: 12px;
  border: 1px solid var(--border-color, #e5e7eb);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--bg-tertiary, #f9fafb);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 10px;
  flex: 1;
  max-width: 320px;
  transition: all 0.2s;
}

.search-box:focus-within {
  border-color: var(--primary, #409eff);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.search-box input {
  border: none;
  outline: none;
  flex: 1;
  background: transparent;
  font-size: 14px;
  color: var(--text-primary, #1f2937);
}

.search-box input::placeholder {
  color: var(--text-tertiary, #9ca3af);
}

/* 表格 */
:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color, #e5e7eb);
}

:deep(.el-table th) {
  background: var(--bg-tertiary, #f9fafb) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
}

:deep(.el-table td) {
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

:deep(.el-table__row:hover > td) {
  background: var(--bg-primary, #f5f7fa) !important;
}

.code-text {
  font-family: 'JetBrains Mono', 'Fira Code', 'Monaco', monospace;
  color: var(--primary, #409eff);
  background: rgba(64, 158, 255, 0.08);
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  letter-spacing: 0.5px;
}

.cron-text {
  font-family: 'JetBrains Mono', 'Fira Code', 'Monaco', monospace;
  color: var(--warning, #e6a23c);
  background: rgba(230, 162, 60, 0.1);
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.schedule-text {
  color: var(--success, #67c23a);
  font-weight: 500;
}

.process-link {
  color: var(--primary, #409eff);
  cursor: pointer;
  font-weight: 500;
}

.text-muted {
  color: var(--text-tertiary, #9ca3af);
}

.trigger-stats {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.trigger-stats .stat-total {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
}

.trigger-stats .stat-detail {
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.trigger-stats .success { color: var(--success, #67c23a); }
.trigger-stats .failed { color: var(--danger, #f56c6c); }

/* 分页 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  background: var(--bg-secondary, #ffffff);
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid var(--border-color, #e5e7eb);
}

/* 详情弹窗 */
.detail-content {
  padding: 0 16px;
}

.detail-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 6px 0;
}

.detail-item label {
  color: var(--text-secondary, #6b7280);
  font-size: 13px;
  min-width: 90px;
  flex-shrink: 0;
}

.detail-item span {
  color: var(--text-primary, #1f2937);
  font-size: 13px;
  font-weight: 500;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.mini-stat-card {
  background: var(--bg-tertiary, #f9fafb);
  border-radius: 10px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--border-color, #e5e7eb);
}

.mini-stat-card .stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.mini-stat-card .stat-icon.primary {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.mini-stat-card.success .stat-icon {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.mini-stat-card.danger .stat-icon {
  background: linear-gradient(135deg, #f56c6c, #f78989);
}

.mini-stat-card .stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mini-stat-card .stat-info .value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  line-height: 1;
}

.mini-stat-card .stat-info .label {
  font-size: 11px;
  color: var(--text-tertiary, #9ca3af);
  font-weight: 500;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
