<template>
  <div class="triggers-page">
    <div class="page-header">
      <h2>{{ t('trigger.title') }}</h2>
      <p class="page-desc">{{ t('trigger.pageDesc') }}</p>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon primary"><el-icon><Timer /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">{{ t('trigger.totalCount') }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon success"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.active }}</span>
          <span class="stat-label">{{ t('trigger.active') }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon warning"><el-icon><Clock /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.totalTriggers }}</span>
          <span class="stat-label">{{ t('trigger.totalTriggers') }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon danger"><el-icon><TrendCharts /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.successRate }}%</span>
          <span class="stat-label">{{ t('trigger.successRate') }}</span>
        </div>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" :placeholder="t('trigger.searchPlaceholder')" />
      </div>
      <el-select v-model="typeFilter" :placeholder="t('trigger.typeFilter')" clearable style="width: 120px;">
        <el-option :label="t('trigger.typeSchedule')" value="schedule" />
        <el-option :label="t('trigger.typeFile')" value="file" />
        <el-option :label="t('trigger.typeApi')" value="api" />
        <el-option :label="t('trigger.typeWebhook')" value="webhook" />
      </el-select>
      <el-select v-model="statusFilter" :placeholder="t('trigger.statusFilter')" clearable style="width: 120px;">
        <el-option :label="t('trigger.statusEnabled')" value="active" />
        <el-option :label="t('trigger.statusPaused')" value="paused" />
        <el-option :label="t('trigger.statusDisabled')" value="disabled" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> {{ t('trigger.create') }}
      </el-button>
    </div>

    <el-table :data="paginatedTriggers" v-loading="loading" border stripe class="unified-table" :default-sort="{ prop: 'lastTriggerTime', order: 'descending' }">
      <el-table-column type="index" :label="t('trigger.index')" width="60" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" :label="t('trigger.name')" min-width="150" show-overflow-tooltip />
      <el-table-column prop="code" :label="t('trigger.code')" min-width="130">
        <template #default="{ row }">
          <span class="code-text">{{ row.code }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="triggerType" :label="t('trigger.triggerType')" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTriggerTypeTag(row.triggerType)" size="small">
            {{ getTriggerTypeText(row.triggerType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="processName" :label="t('trigger.relatedProcess')" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.processName" class="process-link">{{ row.processName }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="cron" :label="t('trigger.executionRule')" min-width="120">
        <template #default="{ row }">
          <span v-if="row.cron" class="cron-text">{{ row.cron }}</span>
          <span v-else-if="row.scheduleTime" class="schedule-text">{{ t('trigger.scheduleEveryday') }} {{ row.scheduleTime }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="t('trigger.status')" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('trigger.triggerStats')" width="140" align="center">
        <template #default="{ row }">
          <div class="trigger-stats">
            <span class="stat-total">{{ row.totalTriggers || 0 }}</span>
            <span class="stat-detail">
              <span class="success">{{ row.successTriggers || 0 }} {{ t('trigger.success') }}</span>
              <span class="failed">{{ row.failedTriggers || 0 }} {{ t('trigger.failed') }}</span>
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="lastTriggerTime" :label="t('trigger.lastTrigger')" min-width="150">
        <template #default="{ row }">
          {{ formatDateTime(row.lastTriggerTime) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('trigger.actions')" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="success" @click="triggerNow(row)" :loading="triggeringId === row.id" class="action-btn">
              <span>{{ t('trigger.triggerNow') }}</span>
            </el-button>
            <el-button link type="primary" @click="editTrigger(row)" class="action-btn">
              <span>{{ t('trigger.edit') }}</span>
            </el-button>
            <el-button link type="warning" @click="toggleStatus(row)" class="action-btn">
              <span>{{ row.status === 'active' ? t('trigger.pause') : t('trigger.enable') }}</span>
            </el-button>
            <el-popconfirm
              :title="t('trigger.confirmDelete', { name: row.name })"
              :confirmButtonText="t('trigger.confirmDeleteBtn')"
              :cancelButtonText="t('trigger.cancel')"
              icon="Delete"
              iconColor="#f56c6c"
              @confirm="deleteTrigger(row)"
            >
              <template #reference>
                <el-button link type="danger" class="action-btn">
                  <span>{{ t('trigger.delete') }}</span>
                </el-button>
              </template>
            </el-popconfirm>
          </div>
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
        <el-form-item :label="t('trigger.name')" prop="name">
          <el-input v-model="triggerForm.name" :placeholder="t('trigger.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('trigger.code')" prop="code">
          <el-input v-model="triggerForm.code" :placeholder="t('trigger.codePlaceholder')" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('trigger.type')" prop="triggerType">
          <el-select v-model="triggerForm.triggerType" :placeholder="t('trigger.typePlaceholder')" style="width: 100%" @change="onTriggerTypeChange">
            <el-option value="schedule" :label="t('trigger.typeSchedule')" />
            <el-option value="file" :label="t('trigger.typeFile')" />
            <el-option value="api" :label="t('trigger.typeApi')" />
            <el-option value="webhook" :label="t('trigger.typeWebhook')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('trigger.bindProcess')" prop="processId">
          <el-select v-model="triggerForm.processId" :placeholder="t('trigger.bindProcessPlaceholder')" style="width: 100%" filterable>
            <el-option v-for="p in processes" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('trigger.bindQueue')">
          <el-select v-model="triggerForm.queueId" :placeholder="t('trigger.bindQueuePlaceholder')" style="width: 100%" clearable filterable>
            <el-option v-for="q in queues" :key="q.id" :label="q.name" :value="q.id" />
          </el-select>
        </el-form-item>

        <!-- 定时触发配置 -->
        <template v-if="triggerForm.triggerType === 'schedule'">
          <el-form-item :label="t('trigger.scheduleRule')">
            <el-radio-group v-model="scheduleMode">
              <el-radio label="cron">{{ t('trigger.cronRadio') }}</el-radio>
              <el-radio label="simple">{{ t('trigger.simpleRadio') }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="scheduleMode === 'cron'" :label="t('trigger.cronExpression')">
            <el-input v-model="triggerForm.cron" :placeholder="t('trigger.cronExpressionPlaceholder')" />
            <div class="form-tip">{{ t('trigger.cronFormat') }}</div>
          </el-form-item>
          <el-form-item v-if="scheduleMode === 'simple'" :label="t('trigger.executeTime')">
            <el-time-picker v-model="triggerForm.scheduleTimeObj" format="HH:mm" value-format="HH:mm" :placeholder="t('trigger.executeTime')" style="width: 100%" />
          </el-form-item>
          <el-form-item v-if="scheduleMode === 'simple'" :label="t('trigger.executePeriod')">
            <el-select v-model="triggerForm.scheduleType" style="width: 100%" @change="onScheduleTypeChange">
              <el-option value="day" :label="t('trigger.periodDay')" />
              <el-option value="week" :label="t('trigger.periodWeek')" />
              <el-option value="month" :label="t('trigger.periodMonth')" />
            </el-select>
          </el-form-item>
          <!-- 每周：选择星期几 -->
          <el-form-item v-if="scheduleMode === 'simple' && triggerForm.scheduleType === 'week'" :label="t('trigger.selectWeekday')">
            <el-checkbox-group v-model="selectedWeekDays">
              <el-checkbox label="1">{{ t('trigger.weekdayMonday') }}</el-checkbox>
              <el-checkbox label="2">{{ t('trigger.weekdayTuesday') }}</el-checkbox>
              <el-checkbox label="3">{{ t('trigger.weekdayWednesday') }}</el-checkbox>
              <el-checkbox label="4">{{ t('trigger.weekdayThursday') }}</el-checkbox>
              <el-checkbox label="5">{{ t('trigger.weekdayFriday') }}</el-checkbox>
              <el-checkbox label="6">{{ t('trigger.weekdaySaturday') }}</el-checkbox>
              <el-checkbox label="7">{{ t('trigger.weekdaySunday') }}</el-checkbox>
            </el-checkbox-group>
            <div class="form-tip">{{ t('trigger.selectWeekdayTip') }}</div>
          </el-form-item>
          <!-- 每月：选择日期 -->
          <el-form-item v-if="scheduleMode === 'simple' && triggerForm.scheduleType === 'month'" :label="t('trigger.selectDate')">
            <el-select v-model="selectedMonthDay" :placeholder="t('trigger.selectDate')" style="width: 100%">
              <el-option v-for="day in 31" :key="day" :label="day + '日'" :value="String(day)" />
            </el-select>
            <div class="form-tip">{{ t('trigger.selectDateTip') }}</div>
          </el-form-item>
        </template>

        <!-- 文件触发配置 -->
        <template v-if="triggerForm.triggerType === 'file'">
          <el-form-item :label="t('trigger.watchPath')">
            <el-input v-model="triggerForm.watchPath" :placeholder="t('trigger.watchPathPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('trigger.filePattern')">
            <el-input v-model="triggerForm.filePattern" :placeholder="t('trigger.filePatternPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('trigger.watchSubdirs')">
            <el-switch v-model="triggerForm.watchSubdirs" />
          </el-form-item>
        </template>

        <!-- API触发配置 -->
        <template v-if="triggerForm.triggerType === 'api'">
          <el-form-item :label="t('trigger.apiKey')">
            <el-input v-model="triggerForm.apiKey" :placeholder="t('trigger.apiKeyPlaceholder')" />
            <div class="form-tip">{{ t('trigger.apiKeyTip') }}</div>
          </el-form-item>
          <el-form-item :label="t('trigger.requestMethod')">
            <el-radio-group v-model="triggerForm.httpMethod">
              <el-radio label="POST">POST</el-radio>
              <el-radio label="GET">GET</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- Webhook配置 -->
        <template v-if="triggerForm.triggerType === 'webhook'">
          <el-form-item label="Webhook URL">
            <el-input v-model="triggerForm.webhookUrl" :placeholder="t('trigger.webhookUrlPlaceholder')" />
          </el-form-item>
        </template>

        <el-form-item :label="t('trigger.autoStart')">
          <el-switch v-model="triggerForm.autoStart" />
          <div class="form-tip">{{ t('trigger.autoStartTip') }}</div>
        </el-form-item>

        <el-form-item :label="t('trigger.maxConcurrent')">
          <el-input-number v-model="triggerForm.maxConcurrent" :min="1" :max="10" />
        </el-form-item>

        <el-form-item :label="t('trigger.description')">
          <el-input v-model="triggerForm.description" type="textarea" :rows="2" :placeholder="t('trigger.descriptionPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('trigger.cancel') }}</el-button>
        <el-button type="primary" @click="submitTrigger" :loading="submitLoading">{{ t('trigger.submit') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="t('trigger.detail')" width="650px">
      <div class="detail-content">
        <div class="detail-section">
          <div class="section-title">{{ t('trigger.basicInfo') }}</div>
          <div class="detail-grid">
            <div class="detail-item"><label>{{ t('trigger.name') }}：</label><span>{{ currentTrigger.name }}</span></div>
            <div class="detail-item"><label>{{ t('trigger.code') }}：</label><span class="code-text">{{ currentTrigger.code }}</span></div>
            <div class="detail-item"><label>{{ t('trigger.triggerType') }}：</label><el-tag :type="getTriggerTypeTag(currentTrigger.triggerType)" size="small">{{ getTriggerTypeText(currentTrigger.triggerType) }}</el-tag></div>
            <div class="detail-item"><label>{{ t('trigger.status') }}：</label><el-tag :type="getStatusTag(currentTrigger.status)" size="small">{{ getStatusText(currentTrigger.status) }}</el-tag></div>
            <div class="detail-item"><label>{{ t('trigger.relatedProcess') }}：</label><span>{{ currentTrigger.processName || '-' }}</span></div>
            <div class="detail-item"><label>{{ t('trigger.bindQueue') }}：</label><span>{{ currentTrigger.queueName || '-' }}</span></div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">{{ t('trigger.executionConfig') }}</div>
          <div class="detail-grid">
            <div class="detail-item" v-if="currentTrigger.cron"><label>{{ t('trigger.cronExpression') }}：</label><span class="cron-text">{{ currentTrigger.cron }}</span></div>
            <div class="detail-item" v-if="currentTrigger.scheduleTime"><label>{{ t('trigger.executeTime') }}：</label><span>{{ t('trigger.scheduleEveryday') }} {{ currentTrigger.scheduleTime }}</span></div>
            <div class="detail-item"><label>{{ t('trigger.autoStart') }}：</label><span>{{ currentTrigger.autoStart ? t('trigger.yes') : t('trigger.no') }}</span></div>
            <div class="detail-item"><label>{{ t('trigger.maxConcurrent') }}：</label><span>{{ currentTrigger.maxConcurrent }} {{ t('trigger.tasks') }}</span></div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">{{ t('trigger.triggerStats2') }}</div>
          <div class="stats-grid">
            <div class="mini-stat-card">
              <div class="stat-icon primary"><el-icon><Document /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentTrigger.totalTriggers || 0 }}</span>
                <span class="label">{{ t('trigger.totalTriggers2') }}</span>
              </div>
            </div>
            <div class="mini-stat-card success">
              <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentTrigger.successTriggers || 0 }}</span>
                <span class="label">{{ t('trigger.successCount') }}</span>
              </div>
            </div>
            <div class="mini-stat-card danger">
              <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
              <div class="stat-info">
                <span class="value">{{ currentTrigger.failedTriggers || 0 }}</span>
                <span class="label">{{ t('trigger.failedCount') }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">{{ t('trigger.recentExecution') }}</div>
          <div class="detail-grid">
            <div class="detail-item"><label>{{ t('trigger.lastTrigger') }}：</label><span>{{ formatDateTime(currentTrigger.lastTriggerTime) }}</span></div>
            <div class="detail-item"><label>{{ t('trigger.lastSuccess') }}：</label><span>{{ formatDateTime(currentTrigger.lastSuccessTime) }}</span></div>
        <div class="detail-item"><label>{{ t('trigger.lastFailed') }}：</label><span>{{ formatDateTime(currentTrigger.lastFailedTime) }}</span></div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Plus, Timer, CircleCheck, Clock, TrendCharts, Document, CircleClose } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const { t } = useI18n()

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

const formRules = computed(() => ({
  name: [{ required: true, message: t('trigger.inputName'), trigger: 'blur' }],
  code: [{ required: true, message: t('trigger.inputCode'), trigger: 'blur' }],
  triggerType: [{ required: true, message: t('trigger.selectType'), trigger: 'change' }],
  processId: [{ required: true, message: t('trigger.selectProcess'), trigger: 'change' }]
}))

const dialogTitle = computed(() => isEdit.value ? t('trigger.edit') : t('trigger.create'))

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
  const map = { schedule: t('trigger.typeSchedule'), file: t('trigger.typeFile'), api: t('trigger.typeApi'), webhook: t('trigger.typeWebhook') }
  return map[type] || type || '-'
}

const getTriggerTypeTag = (type) => {
  const map = { schedule: 'primary', file: 'success', api: 'warning', webhook: 'danger' }
  return map[type] || 'info'
}

const getStatusText = (s) => {
  const map = { active: t('trigger.statusEnabled'), paused: t('trigger.statusPaused'), disabled: t('trigger.statusDisabled') }
  return map[s] || s || '-'
}

const getStatusTag = (s) => {
  const map = { active: 'success', paused: 'warning', disabled: 'danger' }
  return map[s] || 'info'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
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
  
  if (trigger.scheduleDays) {
    if (trigger.scheduleType === 'week') {
      selectedWeekDays.value = trigger.scheduleDays.split(',').filter(d => d)
    } else if (trigger.scheduleType === 'month') {
      selectedMonthDay.value = trigger.scheduleDays
    }
  } else {
    selectedWeekDays.value = []
    selectedMonthDay.value = ''
  }
  
  scheduleMode.value = trigger.cron ? 'cron' : 'simple'
  dialogVisible.value = true
}

const onTriggerTypeChange = () => {
  triggerForm.cron = ''
  triggerForm.scheduleTimeObj = null
}

const onScheduleTypeChange = () => {
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
      ElMessage.success(newStatus === 'active' ? t('trigger.enabled') : t('trigger.paused'))
      await loadTriggers()
    } else {
      ElMessage.error(result.message || t('trigger.operationFailed'))
    }
  } catch {
    ElMessage.error(t('trigger.operationFailed'))
  }
}

const triggerNow = async (trigger) => {
  triggeringId.value = trigger.id
  try {
    const result = await apiPost(`/trigger/${trigger.id}/trigger`)
    if (result.code === 0) {
      ElMessage.success(t('trigger.triggerSuccess'))
      await loadTriggers()
    } else {
      ElMessage.error(result.message || t('trigger.triggerFailed'))
    }
  } catch {
    ElMessage.error(t('trigger.triggerFailed'))
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

      let scheduleDaysValue = ''
      if (triggerForm.scheduleType === 'week' && selectedWeekDays.value.length > 0) {
        scheduleDaysValue = selectedWeekDays.value.join(',')
      } else if (triggerForm.scheduleType === 'month' && selectedMonthDay.value) {
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
        ElMessage.success(isEdit.value ? t('trigger.updateSuccess') : t('trigger.createSuccess'))
        dialogVisible.value = false
        await loadTriggers()
      } else {
        ElMessage.error(result.message || t('trigger.operationFailed'))
      }
    } catch {
      ElMessage.error(t('trigger.requestFailed'))
    } finally {
      submitLoading.value = false
    }
  })
}

const deleteTrigger = async (trigger) => {
  try {
    const result = await apiDelete(`/trigger/${trigger.id}`, {})
    if (result.code === 0) {
      ElMessage.success(t('trigger.deleteSuccess'))
      await loadTriggers()
    } else {
      ElMessage.error(result.message || t('trigger.deleteFailed'))
    }
  } catch {
    ElMessage.error(t('trigger.deleteFailed'))
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
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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

/* 序号单元格动画 */
.index-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  height: 100%;
  padding: 4px 0;
}

.index-number {
  font-weight: 700;
  font-size: 14px;
  color: var(--text-primary, #1f2937);
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.index-line {
  width: 2px;
  height: 0;
  background: linear-gradient(180deg, #00d4ff, #0077ff);
  border-radius: 1px;
  transition: height 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.6;
}

.unified-table :deep(.el-table__row:hover .index-line) {
  height: 12px;
  opacity: 1;
}

.unified-table :deep(.el-table__row:hover .index-number) {
  transform: scale(1.1);
  transition: transform 0.3s ease;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.2s ease;
  font-size: 13px;
}

.action-btn:hover {
  background: rgba(64, 158, 255, 0.1);
  transform: translateY(-1px);
}

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
