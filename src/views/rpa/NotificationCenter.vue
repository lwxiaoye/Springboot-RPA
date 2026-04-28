<template>
  <div class="notification-center">
    <!-- 顶部导航 -->
    <div class="header-section">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon class="title-icon"><Bell /></el-icon>
          通知中心
        </h1>
        <p class="page-desc">管理通知模板、订阅配置和审批流程</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="refreshData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" @click="activeTab = 'template'">
        <div class="stat-icon template">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.templateCount }}</div>
          <div class="stat-label">通知模板</div>
        </div>
      </div>
      <div class="stat-card" @click="activeTab = 'subscription'">
        <div class="stat-icon subscription">
          <el-icon><Collection /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.subscriptionCount }}</div>
          <div class="stat-label">订阅配置</div>
        </div>
      </div>
      <div class="stat-card" @click="activeTab = 'approval'">
        <div class="stat-icon approval">
          <el-icon><Finished /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.pendingApproval }}</div>
          <div class="stat-label">待审批</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon channel">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.channelCount }}</div>
          <div class="stat-label">已配置渠道</div>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="main-tabs" @tab-change="handleTabChange">
      <!-- 通知模板 -->
      <el-tab-pane label="通知模板" name="template">
        <template #label>
          <span class="tab-label">
            <el-icon><Document /></el-icon>
            通知模板
          </span>
        </template>
        <div class="tab-content">
          <div class="toolbar">
            <el-input v-model="templateSearch" placeholder="搜索模板..." prefix-icon="Search" clearable class="search-input" />
            <div class="toolbar-actions">
              <el-select v-model="templateTypeFilter" placeholder="模板类型" clearable class="filter-select">
                <el-option label="任务完成" value="task_complete" />
                <el-option label="任务失败" value="task_failed" />
                <el-option label="订阅通知" value="subscription" />
                <el-option label="系统告警" value="alert" />
              </el-select>
              <el-button type="primary" @click="showTemplateDialog()">
                <el-icon><Plus /></el-icon>
                新建模板
              </el-button>
            </div>
          </div>

          <el-table :data="filteredTemplates" v-loading="loading" stripe class="data-table">
            <el-table-column prop="name" label="模板名称" min-width="150" />
            <el-table-column prop="code" label="模板编码" width="150">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.code }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="getTypeTagType(row.type)">{{ getTypeName(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="channels" label="渠道" width="150">
              <template #default="{ row }">
                <div class="channel-tags">
                  <el-tag v-for="ch in row.channels.split(',')" :key="ch" size="small" class="channel-tag">
                    {{ getChannelName(ch) }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="状态" width="100">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" @change="toggleTemplate(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="useCount" label="使用次数" width="100" align="center" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="previewTemplate(row)">预览</el-button>
                <el-button link type="primary" @click="editTemplate(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteTemplate(row)" :disabled="row.isDefault === 1">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 订阅管理 -->
      <el-tab-pane label="订阅管理" name="subscription">
        <template #label>
          <span class="tab-label">
            <el-icon><Collection /></el-icon>
            订阅管理
          </span>
        </template>
        <div class="tab-content">
          <div class="toolbar">
            <el-input v-model="subscriptionSearch" placeholder="搜索订阅..." prefix-icon="Search" clearable class="search-input" />
            <div class="toolbar-actions">
              <el-button type="primary" @click="showSubscriptionDialog()">
                <el-icon><Plus /></el-icon>
                新建订阅
              </el-button>
            </div>
          </div>

          <el-table :data="filteredSubscriptions" v-loading="loading" stripe class="data-table">
            <el-table-column prop="name" label="订阅名称" min-width="150" />
            <el-table-column prop="reportType" label="报表类型" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ getReportTypeName(row.reportType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="frequency" label="发送频率" width="100">
              <template #default="{ row }">
                {{ getFrequencyName(row.frequency) }}
              </template>
            </el-table-column>
            <el-table-column prop="channel" label="推送渠道" width="150">
              <template #default="{ row }">
                <div class="channel-tags">
                  <el-tag v-for="ch in row.channel.split(',')" :key="ch" size="small" type="success">
                    {{ getChannelName(ch) }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="recipients" label="接收人" min-width="150" show-overflow-tooltip />
            <el-table-column prop="approvalStatus" label="审批状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getApprovalTagType(row.approvalStatus)">
                  {{ getApprovalName(row.approvalStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="状态" width="80">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" @change="toggleSubscription(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="editSubscription(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteSubscription(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 审批管理 -->
      <el-tab-pane label="审批管理" name="approval">
        <template #label>
          <span class="tab-label">
            <el-icon><Finished /></el-icon>
            审批管理
            <el-badge :value="stats.pendingApproval" :hidden="stats.pendingApproval === 0" class="approval-badge" />
          </span>
        </template>
        <div class="tab-content">
          <el-table :data="approvals" v-loading="loading" stripe class="data-table">
            <el-table-column prop="subscriptionName" label="订阅名称" min-width="150" />
            <el-table-column prop="applicantName" label="申请人" width="120" />
            <el-table-column prop="createTime" label="申请时间" width="160">
              <template #default="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getApprovalTagType(row.status)">
                  {{ getApprovalName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="申请原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'pending'">
                  <el-button link type="success" @click="approveApproval(row)">通过</el-button>
                  <el-button link type="danger" @click="rejectApproval(row)">拒绝</el-button>
                </template>
                <el-button link type="info" @click="viewApprovalDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 渠道配置 -->
      <el-tab-pane label="渠道配置" name="channel">
        <template #label>
          <span class="tab-label">
            <el-icon><Connection /></el-icon>
            渠道配置
          </span>
        </template>
        <div class="tab-content">
          <div class="channel-grid">
            <div class="channel-card" v-for="ch in channels" :key="ch.code">
              <div class="channel-header">
                <div class="channel-icon" :class="ch.code">
                  <el-icon><component :is="ch.icon" /></el-icon>
                </div>
                <div class="channel-info">
                  <h3>{{ ch.name }}</h3>
                  <el-tag size="small" :type="ch.enabled ? 'success' : 'info'">
                    {{ ch.enabled ? '已配置' : '未配置' }}
                  </el-tag>
                </div>
              </div>
              <div class="channel-body">
                <p class="channel-desc">{{ ch.description }}</p>
                <el-button size="small" @click="configureChannel(ch)">
                  {{ ch.enabled ? '修改配置' : '立即配置' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 模板编辑对话框 -->
    <el-dialog v-model="templateDialogVisible" :title="templateDialogTitle" width="900px" class="custom-dialog">
      <el-form :model="templateForm" label-width="100px" class="template-form">
        <el-form-item label="模板名称" required>
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编码" required>
          <el-input v-model="templateForm.code" placeholder="请输入唯一编码" :disabled="!!templateForm.id" />
        </el-form-item>
        <el-form-item label="模板类型" required>
          <el-select v-model="templateForm.type" placeholder="请选择类型" class="full-width">
            <el-option label="任务完成" value="task_complete" />
            <el-option label="任务失败" value="task_failed" />
            <el-option label="订阅通知" value="subscription" />
            <el-option label="系统告警" value="alert" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="支持渠道">
          <el-checkbox-group v-model="templateForm.channelsList">
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="dingtalk">钉钉</el-checkbox>
            <el-checkbox label="wecom">企业微信</el-checkbox>
            <el-checkbox label="feishu">飞书</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="主题模板">
          <el-input v-model="templateForm.subjectTemplate" placeholder="如: 【RPA通知】${taskName}" />
        </el-form-item>
        <el-form-item label="模板内容" required>
          <el-input v-model="templateForm.content" type="textarea" :rows="12" placeholder="支持HTML，使用 ${变量名} 引用变量" />
        </el-form-item>
        <el-form-item label="可用变量">
          <div class="variable-tags">
            <el-tag v-for="v in availableVariables" :key="v" size="small" class="var-tag" @click="insertVariable(v)">
              {{ v }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="模板描述">
          <el-input v-model="templateForm.description" type="textarea" :rows="2" placeholder="请输入模板描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 模板预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="模板预览" width="700px" class="custom-dialog">
      <div class="preview-container" v-html="previewContent"></div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 订阅编辑对话框 -->
    <el-dialog v-model="subscriptionDialogVisible" :title="subscriptionDialogTitle" width="800px" class="custom-dialog">
      <el-form :model="subscriptionForm" label-width="100px" class="subscription-form">
        <el-form-item label="订阅名称" required>
          <el-input v-model="subscriptionForm.name" placeholder="请输入订阅名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="报表类型" required>
              <el-select v-model="subscriptionForm.reportType" placeholder="请选择" class="full-width">
                <el-option label="日报" value="daily" />
                <el-option label="周报" value="weekly" />
                <el-option label="月报" value="monthly" />
                <el-option label="机器人报表" value="robot" />
                <el-option label="ROI报表" value="roi" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发送频率" required>
              <el-select v-model="subscriptionForm.frequency" placeholder="请选择" class="full-width">
                <el-option label="每天" value="daily" />
                <el-option label="每周" value="weekly" />
                <el-option label="每月" value="monthly" />
                <el-option label="自定义" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="推送渠道" required>
          <el-checkbox-group v-model="subscriptionForm.channelsList">
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="dingtalk">钉钉</el-checkbox>
            <el-checkbox label="wecom">企业微信</el-checkbox>
            <el-checkbox label="feishu">飞书</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="接收人" required>
          <el-input v-model="subscriptionForm.recipients" placeholder="多个用逗号分隔" />
        </el-form-item>

        <!-- 推送时间配置 -->
        <el-divider content-position="left">推送时间配置</el-divider>
        <el-form-item label="时间类型">
          <el-radio-group v-model="subscriptionForm.scheduleType">
            <el-radio label="fixed">固定时间</el-radio>
            <el-radio label="custom">自定义</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="subscriptionForm.scheduleType === 'fixed'">
          <el-form-item label="固定时间">
            <el-time-picker v-model="subscriptionForm.fixedTime" format="HH:mm" placeholder="选择时间" class="full-width" />
          </el-form-item>
        </template>
        <template v-else>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Cron表达式">
                <el-input v-model="subscriptionForm.cronExpression" placeholder="如: 0 30 9 * * ?" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="推送星期">
                <el-select v-model="subscriptionForm.weekdays" multiple placeholder="选择星期" class="full-width">
                  <el-option label="周一" :value="1" />
                  <el-option label="周二" :value="2" />
                  <el-option label="周三" :value="3" />
                  <el-option label="周四" :value="4" />
                  <el-option label="周五" :value="5" />
                  <el-option label="周六" :value="6" />
                  <el-option label="周日" :value="7" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- 审批配置 -->
        <el-divider content-position="left">审批配置</el-divider>
        <el-form-item label="需要审批">
          <el-switch v-model="subscriptionForm.requireApproval" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="通知配置">
          <el-checkbox-group v-model="subscriptionForm.includeAttachment">
            <el-checkbox :label="1">包含附件</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subscriptionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSubscription">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, Document, Collection, Finished, Connection, Refresh, Plus, Search
} from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '@/utils/api'

// 状态
const activeTab = ref('template')
const loading = ref(false)
const templates = ref([])
const subscriptions = ref([])
const approvals = ref([])
const templateSearch = ref('')
const subscriptionSearch = ref('')
const templateTypeFilter = ref('')
const templateDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const subscriptionDialogVisible = ref(false)
const previewContent = ref('')
const templateDialogTitle = ref('新建模板')
const subscriptionDialogTitle = ref('新建订阅')

const templateForm = reactive({
  id: null,
  name: '',
  code: '',
  type: '',
  channels: '',
  channelsList: [],
  subjectTemplate: '',
  content: '',
  description: ''
})

const subscriptionForm = reactive({
  id: null,
  name: '',
  reportType: '',
  frequency: '',
  channel: '',
  channelsList: [],
  recipients: '',
  scheduleType: 'fixed',
  fixedTime: new Date(),
  cronExpression: '',
  weekdays: [],
  requireApproval: 0,
  includeAttachment: 1
})

const stats = reactive({
  templateCount: 0,
  subscriptionCount: 0,
  pendingApproval: 0,
  channelCount: 3
})

const channels = ref([
  { code: 'email', name: '邮件', icon: 'Message', enabled: true, description: '通过SMTP发送邮件通知，支持HTML格式' },
  { code: 'dingtalk', name: '钉钉', icon: 'ChatLineSquare', enabled: false, description: '通过钉钉群机器人发送消息' },
  { code: 'wecom', name: '企业微信', icon: 'ChatDotRound', enabled: false, description: '通过企业微信应用发送消息' },
  { code: 'feishu', name: '飞书', icon: 'ChatSquare', enabled: false, description: '通过飞书Webhook发送消息' }
])

const availableVariables = computed(() => {
  const type = templateForm.type
  if (type === 'task_complete' || type === 'task_failed') {
    return ['taskId', 'taskName', 'startTime', 'endTime', 'duration', 'errorMessage', 'reportUrl', 'robotName']
  } else if (type === 'subscription') {
    return ['subscriptionName', 'reportType', 'period', 'totalExecutions', 'successRate', 'failedCount', 'reportUrl']
  } else if (type === 'alert') {
    return ['alertTitle', 'alertContent', 'alertTime', 'level', 'source']
  }
  return ['taskId', 'taskName', 'createTime', 'reportUrl']
})

const filteredTemplates = computed(() => {
  return templates.value.filter(t => {
    const matchSearch = !templateSearch.value || t.name.includes(templateSearch.value) || t.code.includes(templateSearch.value)
    const matchType = !templateTypeFilter.value || t.type === templateTypeFilter.value
    return matchSearch && matchType
  })
})

const filteredSubscriptions = computed(() => {
  return subscriptions.value.filter(s => {
    return !subscriptionSearch.value || s.name.includes(subscriptionSearch.value)
  })
})

// 方法
const loadData = async () => {
  loading.value = true
  try {
    const [templateRes, subscriptionRes, approvalRes] = await Promise.all([
      apiGet('/notification-template'),
      apiGet('/report-analytics/subscriptions'),
      apiGet('/subscription-approval/pending')
    ])

    if (templateRes.code === 0) templates.value = templateRes.data || []
    if (subscriptionRes.code === 0) subscriptions.value = subscriptionRes.data || []
    if (approvalRes.code === 0) approvals.value = approvalRes.data || []

    stats.templateCount = templates.value.length
    stats.subscriptionCount = subscriptions.value.length
    stats.pendingApproval = approvals.value.filter(a => a.status === 'pending').length
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

const refreshData = () => loadData()

const handleTabChange = (tab) => {
  console.log('切换到:', tab)
}

// 模板操作
const showTemplateDialog = (row = null) => {
  if (row) {
    templateDialogTitle.value = '编辑模板'
    Object.assign(templateForm, {
      id: row.id,
      name: row.name,
      code: row.code,
      type: row.type,
      channels: row.channels,
      channelsList: row.channels ? row.channels.split(',') : [],
      subjectTemplate: row.subjectTemplate || '',
      content: row.content,
      description: row.description || ''
    })
  } else {
    templateDialogTitle.value = '新建模板'
    Object.assign(templateForm, {
      id: null, name: '', code: '', type: '', channels: '',
      channelsList: [], subjectTemplate: '', content: '', description: ''
    })
  }
  templateDialogVisible.value = true
}

const editTemplate = (row) => showTemplateDialog(row)

const saveTemplate = async () => {
  if (!templateForm.name || !templateForm.code || !templateForm.type || !templateForm.content) {
    ElMessage.warning('请填写必填项')
    return
  }
  templateForm.channels = templateForm.channelsList.join(',')

  try {
    const url = templateForm.id ? `/notification-template/${templateForm.id}` : '/notification-template'
    const method = templateForm.id ? 'put' : 'post'
    const res = await (method === 'put' ? apiPut(url, templateForm) : apiPost(url, templateForm))

    if (res.code === 0) {
      ElMessage.success('保存成功')
      templateDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const toggleTemplate = async (row) => {
  try {
    const res = await apiPut(`/notification-template/${row.id}/toggle`, {})
    if (res.code === 0) {
      ElMessage.success(row.enabled ? '模板已启用' : '模板已禁用')
    }
  } catch (error) {
    row.enabled = row.enabled ? 0 : 1
    ElMessage.error('操作失败')
  }
}

const deleteTemplate = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除此模板吗？', '提示', { type: 'warning' })
    const res = await apiDelete(`/notification-template/${row.id}`)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const previewTemplate = async (row) => {
  const demoVars = {
    taskId: '1001', taskName: '数据采集机器人',
    startTime: '2024-01-01 09:00', endTime: '2024-01-01 10:30',
    duration: '1小时30分钟', reportUrl: 'http://localhost:8080',
    robotName: 'RPA-Robot-01'
  }
  try {
    const res = await apiPost('/notification-template/preview', { content: row.content, variables: demoVars })
    if (res.code === 0) {
      previewContent.value = res.data.processed
      previewDialogVisible.value = true
    }
  } catch (error) {
    previewContent.value = row.content
    previewDialogVisible.value = true
  }
}

const insertVariable = (variable) => {
  templateForm.content += '${' + variable + '}'
}

// 订阅操作
const showSubscriptionDialog = (row = null) => {
  if (row) {
    subscriptionDialogTitle.value = '编辑订阅'
    subscriptionForm.id = row.id
    subscriptionForm.name = row.name
    subscriptionForm.reportType = row.reportType
    subscriptionForm.frequency = row.frequency
    subscriptionForm.channel = row.channel
    subscriptionForm.channelsList = row.channel ? row.channel.split(',') : []
    subscriptionForm.recipients = row.recipients
    subscriptionForm.scheduleType = row.scheduleType || 'fixed'
    subscriptionForm.cronExpression = row.cronExpression || ''
    subscriptionForm.requireApproval = row.requireApproval || 0
    subscriptionForm.includeAttachment = row.includeAttachment || 1
  } else {
    subscriptionDialogTitle.value = '新建订阅'
    Object.assign(subscriptionForm, {
      id: null, name: '', reportType: '', frequency: '', channel: '',
      channelsList: [], recipients: '', scheduleType: 'fixed',
      fixedTime: new Date(), cronExpression: '', weekdays: [],
      requireApproval: 0, includeAttachment: 1
    })
  }
  subscriptionDialogVisible.value = true
}

const editSubscription = (row) => showSubscriptionDialog(row)

const saveSubscription = async () => {
  if (!subscriptionForm.name || !subscriptionForm.reportType || !subscriptionForm.recipients) {
    ElMessage.warning('请填写必填项')
    return
  }
  subscriptionForm.channel = subscriptionForm.channelsList.join(',')

  try {
    const url = subscriptionForm.id ? `/report-analytics/subscription/${subscriptionForm.id}` : '/report-analytics/subscription'
    const method = subscriptionForm.id ? 'put' : 'post'
    const res = await (method === 'put' ? apiPut(url, subscriptionForm) : apiPost(url, subscriptionForm))

    if (res.code === 0) {
      ElMessage.success('保存成功')
      subscriptionDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const toggleSubscription = async (row) => {
  try {
    const res = await apiPut(`/report-analytics/subscription/${row.id}/toggle`, {})
    if (res.code === 0) {
      ElMessage.success(row.enabled ? '订阅已启用' : '订阅已禁用')
    }
  } catch (error) {
    row.enabled = row.enabled ? 0 : 1
    ElMessage.error('操作失败')
  }
}

const deleteSubscription = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除此订阅吗？', '提示', { type: 'warning' })
    const res = await apiDelete(`/report-analytics/subscription/${row.id}`)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

// 审批操作
const approveApproval = async (row) => {
  try {
    await ElMessageBox.confirm('确定要通过此订阅申请吗？', '审批确认', { type: 'success' })
    const res = await apiPost('/subscription-approval/approve', { approvalId: row.id, approverId: 1, remark: '' })
    if (res.code === 0) {
      ElMessage.success('审批已通过')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

const rejectApproval = async (row) => {
  try {
    await ElMessageBox.confirm('确定要拒绝此订阅申请吗？', '审批确认', { type: 'warning' })
    const res = await apiPost('/subscription-approval/reject', { approvalId: row.id, approverId: 1, remark: '' })
    if (res.code === 0) {
      ElMessage.success('已拒绝申请')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

const viewApprovalDetail = (row) => {
  ElMessage.info('详情功能开发中')
}

const configureChannel = (channel) => {
  ElMessage.info(`${channel.name}配置功能开发中`)
}

// 工具方法
const getTypeTagType = (type) => {
  const map = { task_complete: 'success', task_failed: 'danger', subscription: 'primary', alert: 'warning' }
  return map[type] || 'info'
}

const getTypeName = (type) => {
  const map = { task_complete: '任务完成', task_failed: '任务失败', subscription: '订阅', alert: '告警', custom: '自定义' }
  return map[type] || type
}

const getChannelName = (channel) => {
  const map = { email: '邮件', dingtalk: '钉钉', wecom: '企微', feishu: '飞书' }
  return map[channel] || channel
}

const getReportTypeName = (type) => {
  const map = { daily: '日报', weekly: '周报', monthly: '月报', robot: '机器人', roi: 'ROI' }
  return map[type] || type
}

const getFrequencyName = (freq) => {
  const map = { daily: '每天', weekly: '每周', monthly: '每月', custom: '自定义' }
  return map[freq] || freq
}

const getApprovalTagType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const getApprovalName = (status) => {
  const map = { pending: '待审批', approved: '已批准', rejected: '已拒绝' }
  return map[status] || status
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.notification-center {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100%;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  font-size: 28px;
  color: #667eea;
}

.page-desc {
  color: #666;
  margin: 0;
  font-size: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.template { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
.stat-icon.subscription { background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%); color: white; }
.stat-icon.approval { background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%); color: white; }
.stat-icon.channel { background: linear-gradient(135deg, #67c23a 0%, #95d475 100%); color: white; }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.main-tabs {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-content {
  padding: 20px 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.search-input {
  width: 280px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.filter-select {
  width: 150px;
}

.data-table {
  border-radius: 8px;
  overflow: hidden;
}

.channel-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.channel-tag {
  margin: 2px;
}

.approval-badge {
  margin-left: 4px;
}

.channel-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.channel-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
}

.channel-card:hover {
  background: #f0f2f5;
}

.channel-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.channel-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
}

.channel-icon.email { background: linear-gradient(135deg, #409eff, #66b1ff); }
.channel-icon.dingtalk { background: linear-gradient(135deg, #07c160, #25d47e); }
.channel-icon.wecom { background: linear-gradient(135deg, #07c160, #2eaf4b); }
.channel-icon.feishu { background: linear-gradient(135deg, #3370ff, #5d8aff); }

.channel-info h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: #1a1a2e;
}

.channel-desc {
  color: #666;
  font-size: 13px;
  margin: 0 0 16px 0;
}

.full-width {
  width: 100%;
}

.variable-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.var-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.var-tag:hover {
  transform: scale(1.05);
}

.preview-container {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  max-height: 500px;
  overflow-y: auto;
}

.custom-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: 0;
  padding: 20px;
}

.custom-dialog :deep(.el-dialog__title) {
  color: white;
  font-weight: 600;
}

.custom-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: white;
}

.custom-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.template-form, .subscription-form {
  max-height: 60vh;
  overflow-y: auto;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .channel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
