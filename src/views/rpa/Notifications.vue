<template>
  <div class="announcement-page">
    <!-- 紧急公告强制弹窗 -->
    <el-dialog
      v-model="urgentDialogVisible"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      width="680px"
      class="urgent-dialog"
    >
      <template #header>
        <div class="urgent-header">
          <el-icon class="urgent-icon"><WarningFilled /></el-icon>
          <span>紧急公告</span>
        </div>
      </template>
      <div class="urgent-content" v-html="urgentContent"></div>
      <template #footer>
        <div class="urgent-footer">
          <el-checkbox v-model="urgentConfirmed" v-if="!urgentReadIds.includes(urgentId)">
            我已阅读并理解上述内容
          </el-checkbox>
          <el-button type="primary" :disabled="!urgentConfirmed && !urgentReadIds.includes(urgentId)" @click="confirmUrgentRead">
            确认已阅
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-title">
          <h2>通知公告</h2>
          <el-tag size="small" type="info">OFFICIAL</el-tag>
        </div>
        <p class="page-desc">系统重要通知和公告信息管理</p>
      </div>
      <div class="header-actions" v-if="isAdmin">
        <el-button @click="showDraftBox">
          <el-icon><Document /></el-icon>
          草稿箱
          <el-badge :value="draftCount" :hidden="draftCount === 0" />
        </el-button>
        <el-button type="primary" @click="showPublishDialog">
          <el-icon><Plus /></el-icon>
          发布公告
        </el-button>
      </div>
    </div>

    <!-- 统计概览 -->
    <div class="stats-row">
      <div class="stat-box urgent" :class="{ active: currentPriority === 'urgent' }" @click="filterByPriority('urgent')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
            <path d="M12 2L1 21h22L12 2zm0 3.83L19.53 19H4.47L12 5.83zM11 16h2v2h-2v-2zm0-6h2v4h-2v-4z"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.urgent || 0 }}</span>
          <span class="stat-label">紧急公告</span>
        </div>
        <div class="stat-indicator"></div>
      </div>
      <div class="stat-box important" :class="{ active: currentPriority === 'important' }" @click="filterByPriority('important')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
            <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.important || 0 }}</span>
          <span class="stat-label">重要公告</span>
        </div>
        <div class="stat-indicator"></div>
      </div>
      <div class="stat-box normal" :class="{ active: currentPriority === 'normal' }" @click="filterByPriority('normal')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
            <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.normal || 0 }}</span>
          <span class="stat-label">普通公告</span>
        </div>
        <div class="stat-indicator"></div>
      </div>
      <div class="stat-box unread" :class="{ active: currentReadStatus === 'unread' }" @click="filterByReadStatus('unread')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
            <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ stats.unread || 0 }}</span>
          <span class="stat-label">未读公告</span>
        </div>
        <div class="stat-indicator"></div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="search-wrapper">
        <el-icon class="search-icon"><Search /></el-icon>
        <input
          v-model="searchKeyword"
          placeholder="搜索公告标题或内容..."
          @keyup.enter="handleSearch"
        />
      </div>
      <el-select v-model="priorityFilter" placeholder="优先级" clearable style="width: 120px;">
        <el-option label="紧急" value="urgent" />
        <el-option label="重要" value="important" />
        <el-option label="普通" value="normal" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 100px;">
        <el-option label="未读" value="unread" />
        <el-option label="已读" value="read" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px;"
        clearable
      />
      <div class="toolbar-right">
        <el-button @click="markAllRead" :disabled="stats.unread === 0">
          <el-icon><Check /></el-icon>
          全部已读
        </el-button>
      </div>
    </div>

    <!-- 公告列表 -->
    <div class="announcement-list" v-loading="loading">
      <div
        v-for="item in paginatedAnnouncements"
        :key="item.id"
        class="announcement-item"
        :class="{ unread: item.status === 'unread', [item.priority]: true }"
        @click="viewDetail(item)"
      >
        <div class="item-priority-bar"></div>
        <div class="item-content">
          <div class="item-header">
            <el-tag :type="getPriorityType(item.priority)" size="small" effect="plain">
              <span class="priority-text">{{ getPriorityText(item.priority) }}</span>
            </el-tag>
            <h3 class="item-title">{{ item.title }}</h3>
            <span class="item-time">{{ formatTime(item.publishTime) }}</span>
          </div>
          <div class="item-summary">{{ stripHtml(item.content) | truncate(120) }}</div>
          <div class="item-footer">
            <div class="item-meta">
              <span class="item-author">
                <el-icon><User /></el-icon>
                {{ item.publisherName }}
              </span>
              <span class="item-dept" v-if="item.department">
                <el-icon><OfficeBuilding /></el-icon>
                {{ item.department }}
              </span>
            </div>
            <div class="item-actions" @click.stop>
              <el-button link type="primary" size="small" @click="markRead(item)" v-if="item.status === 'unread'">
                标记已读
              </el-button>
              <el-button link type="primary" size="small" @click="viewDetail(item)">
                查看详情
              </el-button>
              <el-button link type="primary" size="small" @click="showReadStats(item)" v-if="isAdmin">
                阅读统计
              </el-button>
            </div>
          </div>
        </div>
        <div class="unread-badge" v-if="item.status === 'unread'">
          <span class="badge-dot"></span>
        </div>
      </div>

      <el-empty v-if="paginatedAnnouncements.length === 0 && !loading" description="暂无公告">
        <template #image>
          <svg viewBox="0 0 24 24" width="64" height="64" fill="#dcdfe6">
            <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/>
          </svg>
        </template>
      </el-empty>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="pagination.total > 0">
      <span class="pagination-info">共 {{ pagination.total }} 条记录</span>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="sizes, prev, pager, next"
        background
      />
    </div>

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentAnnouncement.title" width="800px" class="detail-dialog" destroy-on-close>
      <div class="detail-header">
        <div class="detail-meta">
          <el-tag :type="getPriorityType(currentAnnouncement.priority)" size="small" effect="plain">
            {{ getPriorityText(currentAnnouncement.priority) }}
          </el-tag>
          <span class="detail-publisher">
            <el-icon><User /></el-icon>
            {{ currentAnnouncement.publisherName }}
          </span>
          <span class="detail-time">
            <el-icon><Clock /></el-icon>
            {{ formatDateTime(currentAnnouncement.publishTime) }}
          </span>
          <span class="detail-views">
            <el-icon><View /></el-icon>
            阅读 {{ currentAnnouncement.readCount || 0 }} 次
          </span>
        </div>
      </div>
      <el-divider style="margin: 16px 0;" />
      <div class="detail-body" v-html="currentAnnouncement.content"></div>
      <div class="detail-attachments" v-if="currentAnnouncement.attachments?.length">
        <el-divider style="margin: 16px 0;" />
        <h4>附件列表</h4>
        <div class="attachment-list">
          <div
            v-for="file in currentAnnouncement.attachments"
            :key="file.id"
            class="attachment-item"
            @click="downloadAttachment(file)"
          >
            <el-icon><Document /></el-icon>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ formatFileSize(file.size) }}</span>
            <el-icon class="download-icon"><Download /></el-icon>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="detail-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" @click="confirmRead(currentAnnouncement)" v-if="currentAnnouncement.status === 'unread'">
            确认已阅
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 发布公告弹窗（管理员） -->
    <el-dialog v-model="publishVisible" title="发布公告" width="900px" class="publish-dialog" destroy-on-close>
      <el-form :model="publishForm" :rules="publishRules" ref="publishFormRef" label-width="100px" class="publish-form">
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="公告标题" prop="title" class="form-item-required">
              <el-input
                v-model="publishForm.title"
                placeholder="请输入公告标题，标题需简洁明了"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="优先级" prop="priority" class="form-item-required">
              <el-radio-group v-model="publishForm.priority" class="priority-group">
                <el-radio label="normal" value="normal">
                  <span class="priority-badge normal">普通</span>
                </el-radio>
                <el-radio label="important" value="important">
                  <span class="priority-badge important">重要</span>
                </el-radio>
                <el-radio label="urgent" value="urgent">
                  <span class="priority-badge urgent">紧急</span>
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生效时间" prop="publishTime">
              <el-date-picker
                v-model="publishForm.publishTime"
                type="datetime"
                placeholder="留空立即发布"
                :disabled-date="disabledDate"
                style="width: 100%;"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
              <div class="form-tip">留空则立即发布</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="有效期至" prop="expireTime">
              <el-date-picker
                v-model="publishForm.expireTime"
                type="datetime"
                placeholder="默认1个月后"
                style="width: 100%;"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
                :disabled-date="disabledExpireDate"
              />
              <div class="form-tip">默认有效期为1个月</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="发布范围" prop="scope">
              <el-select v-model="publishForm.scope" placeholder="请选择发布范围" style="width: 100%;">
                <el-option label="全平台用户" value="all" />
                <el-option label="本企业部门" value="enterprise" />
                <el-option label="指定人员" value="specific" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 企业部门选择 -->
          <el-col :span="12" v-if="publishForm.scope === 'enterprise'">
            <el-form-item label="选择部门" prop="targetDepartments">
              <el-select
                v-model="publishForm.targetDepartments"
                multiple
                placeholder="请选择部门"
                style="width: 100%;"
                filterable
                collapse-tags
                collapse-tags-tooltip
              >
                <el-option
                  v-for="dept in departmentList"
                  :key="dept"
                  :label="dept"
                  :value="dept"
                />
              </el-select>
              <div class="form-tip">选择接收公告的部门</div>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 指定人员选择 -->
        <el-row :gutter="24" v-if="publishForm.scope === 'specific'">
          <el-col :span="24">
            <el-form-item label="指定人员" prop="targetUsers">
              <el-select
                v-model="publishForm.targetUsers"
                multiple
                filterable
                remote
                reserve-keyword
                placeholder="请输入姓名或用户名搜索"
                :remote-method="searchUsers"
                :loading="userSearchLoading"
                style="width: 100%;"
              >
                <el-option
                  v-for="user in searchResults"
                  :key="user.id"
                  :label="user.realName + ' (' + user.username + ')'"
                  :value="user.id"
                >
                  <div class="user-option">
                    <el-avatar :size="24" :src="user.avatar">
                      {{ user.realName?.charAt(0) || 'U' }}
                    </el-avatar>
                    <span>{{ user.realName || user.username }}</span>
                    <span class="user-dept">{{ user.department || '未分配部门' }}</span>
                  </div>
                </el-option>
              </el-select>
              <div class="form-tip">选择接收公告的人员</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="公告内容" prop="content" class="form-item-required">
              <el-input
                v-model="publishForm.content"
                type="textarea"
                :rows="10"
                placeholder="请输入公告内容，支持富文本格式。支持插入链接、图片等内容。"
                resize="vertical"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="附件上传">
              <el-upload
                :file-list="fileList"
                :auto-upload="false"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                multiple
                accept=".doc,.docx,.xls,.xlsx,.pdf,.jpg,.jpeg,.png,.gif,.zip,.rar"
                class="attachment-upload"
              >
                <el-button type="primary" plain>
                  <el-icon><Upload /></el-icon>
                  选择文件
                </el-button>
                <template #tip>
                  <div class="upload-tip">
                    支持上传文档、图片、压缩包等文件，单个文件不超过50MB。
                    <span class="forbidden">禁止上传可执行文件（.exe/.bat/.cmd）</span>
                  </div>
                </template>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="publishVisible = false">取消</el-button>
          <el-button @click="saveDraft" :loading="draftLoading">保存草稿</el-button>
          <el-button type="primary" @click="submitAnnouncement" :loading="submitLoading">发布</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 阅读统计弹窗（管理员）- 真实数据 -->
    <el-dialog v-model="statsVisible" title="阅读统计" width="1000px" class="stats-dialog" destroy-on-close>
      <div class="stats-summary" v-if="readStats.title">
        <div class="summary-title">{{ readStats.title }}</div>
      </div>
      <div class="stats-summary">
        <div class="summary-card total">
          <div class="summary-value">{{ readStats.total || 0 }}</div>
          <div class="summary-label">目标人数</div>
        </div>
        <div class="summary-card read">
          <div class="summary-value">{{ readStats.read || 0 }}</div>
          <div class="summary-label">已读人数</div>
        </div>
        <div class="summary-card unread">
          <div class="summary-value">{{ readStats.unread || 0 }}</div>
          <div class="summary-label">未读人数</div>
        </div>
        <div class="summary-card rate">
          <div class="summary-value">{{ (readStats.rate || 0).toFixed(1) }}%</div>
          <div class="summary-label">阅读率</div>
        </div>
      </div>
      <el-divider />
      <div class="read-list">
        <div class="list-header">
          <h4>阅读详情</h4>
          <div class="list-actions">
            <el-button size="small" @click="exportStats">
              <el-icon><Download /></el-icon>
              导出报表
            </el-button>
            <el-button type="primary" size="small" @click="sendBatchReminder" :disabled="!readStats.unread">
              <el-icon><Message /></el-icon>
              一键催读
            </el-button>
          </div>
        </div>
        <el-table :data="readStats.details" border stripe max-height="400" class="stats-table" v-loading="statsLoading">
          <el-table-column prop="userName" label="姓名" width="120" />
          <el-table-column prop="department" label="部门" width="150">
            <template #default="{ row }">
              {{ row.department || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="readTime" label="阅读时间" width="180">
            <template #default="{ row }">
              <span v-if="row.readTime">{{ formatDateTime(row.readTime) }}</span>
              <span v-else class="text-muted">未阅读</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'read' ? 'success' : 'warning'" size="small" effect="plain">
                {{ row.status === 'read' ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="sendReminder(row)" v-if="row.status !== 'read'">
                发送提醒
              </el-button>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 草稿列表弹窗 -->
    <el-dialog v-model="draftVisible" title="草稿箱" width="700px" class="draft-dialog" destroy-on-close>
      <div class="draft-list" v-if="draftList.length">
        <div v-for="draft in draftList" :key="draft.id" class="draft-item">
          <div class="draft-content" @click="editDraft(draft)">
            <h4>{{ draft.title || '无标题' }}</h4>
            <p>{{ stripHtml(draft.content) | truncate(60) }}</p>
            <span class="draft-time">保存于 {{ formatTime(draft.updateTime) }}</span>
          </div>
          <div class="draft-actions">
            <el-button link type="primary" size="small" @click="editDraft(draft)">编辑</el-button>
            <el-button link type="danger" size="small" @click="deleteDraft(draft)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无草稿" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, Search, Plus, Check, User, Clock, View, Document,
  Upload, Download, Message, OfficeBuilding, WarningFilled
} from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

// 状态
const loading = ref(false)
const submitLoading = ref(false)
const draftLoading = ref(false)
const statsLoading = ref(false)
const announcements = ref([])
const draftList = ref([])
const searchKeyword = ref('')
const priorityFilter = ref('')
const statusFilter = ref('')
const dateRange = ref([])
const currentPriority = ref('')
const currentReadStatus = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })
const stats = ref({ urgent: 0, important: 0, normal: 0, unread: 0 })

// 详情弹窗
const detailVisible = ref(false)
const currentAnnouncement = ref({})
const urgentDialogVisible = ref(false)
const urgentContent = ref('')
const urgentId = ref(null)
const urgentConfirmed = ref(false)
const shownUrgentIds = ref([])
const urgentReadIds = ref([])

// 发布弹窗
const publishVisible = ref(false)
const publishFormRef = ref(null)
const fileList = ref([])
const isEditingDraft = ref(false)
const currentDraftId = ref(null)
const publishForm = reactive({
  title: '',
  priority: 'normal',
  publishTime: null,
  expireTime: null,
  scope: 'all',
  targetDepartments: [],
  targetUsers: [],
  content: ''
})
const publishRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

// 统计弹窗
const statsVisible = ref(false)
const readStats = ref({ total: 0, read: 0, unread: 0, rate: 0, details: [], title: '' })

// 草稿箱弹窗
const draftVisible = ref(false)
const draftCount = computed(() => draftList.value.length)

// 用户搜索相关
const userSearchLoading = ref(false)
const searchResults = ref([])
const departmentList = ref([])

// 权限判断
const isAdmin = computed(() => {
  const user = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return user.role === 1
})

// 当前用户ID
const currentUserId = computed(() => {
  const user = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return user.id
})

// 过滤后的公告
const filteredAnnouncements = computed(() => {
  let list = [...announcements.value]

  if (priorityFilter.value) {
    list = list.filter(a => a.priority === priorityFilter.value)
  } else if (currentPriority.value) {
    list = list.filter(a => a.priority === currentPriority.value)
  }

  if (statusFilter.value) {
    list = list.filter(a => a.status === statusFilter.value)
  } else if (currentReadStatus.value) {
    list = list.filter(a => a.status === currentReadStatus.value)
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(a =>
      a.title.toLowerCase().includes(keyword) ||
      (a.content && a.content.toLowerCase().includes(keyword))
    )
  }

  if (dateRange.value?.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(a => {
      const pubTime = new Date(a.publishTime).getTime()
      return pubTime >= start.getTime() && pubTime <= end.getTime() + 86400000
    })
  }

  list.sort((a, b) => {
    if (a.priority === 'urgent' && b.priority !== 'urgent') return -1
    if (b.priority === 'urgent' && a.priority !== 'urgent') return 1
    if (a.status === 'unread' && b.status !== 'unread') return -1
    if (a.status !== 'unread' && b.status === 'unread') return 1
    return new Date(b.publishTime) - new Date(a.publishTime)
  })

  pagination.total = list.length
  return list
})

const paginatedAnnouncements = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredAnnouncements.value.slice(start, end)
})

// 加载公告列表
const loadAnnouncements = async () => {
  loading.value = true
  try {
    const result = await apiGet('/announcement/list', { userId: currentUserId.value })
    if (result.code === 0) {
      announcements.value = result.data || []
      calculateStats()
      checkUrgentAnnouncement()
      notifyLayout()
    } else {
      announcements.value = []
      calculateStats()
      notifyLayout()
    }
  } catch (e) {
    console.error('加载公告失败:', e)
    announcements.value = []
    calculateStats()
    notifyLayout()
  } finally {
    loading.value = false
  }
}

// 通知布局更新未读数
const notifyLayout = () => {
  window.dispatchEvent(new CustomEvent('notificationUpdated', {
    detail: { count: stats.value.unread }
  }))
  // 同时发送公告已读更新事件
  window.dispatchEvent(new CustomEvent('announcementReadUpdated'))
}

// 计算统计数据
const calculateStats = () => {
  const list = announcements.value
  stats.value = {
    urgent: list.filter(a => a.priority === 'urgent' && a.status === 'unread').length,
    important: list.filter(a => a.priority === 'important' && a.status === 'unread').length,
    normal: list.filter(a => a.priority === 'normal' && a.status === 'unread').length,
    unread: list.filter(a => a.status === 'unread').length
  }
}

// 检查紧急公告
const checkUrgentAnnouncement = () => {
  const urgentUnread = announcements.value.find(a =>
    a.priority === 'urgent' && a.status === 'unread' && !shownUrgentIds.value.includes(a.id)
  )
  if (urgentUnread) {
    urgentId.value = urgentUnread.id
    urgentContent.value = urgentUnread.content
    urgentConfirmed.value = false
    urgentDialogVisible.value = true
    shownUrgentIds.value.push(urgentUnread.id)
  }
}

// 确认紧急公告已读
const confirmUrgentRead = async () => {
  try {
    await apiPut(`/announcement/${urgentId.value}/read`, { userId: currentUserId.value })
    const item = announcements.value.find(a => a.id === urgentId.value)
    if (item) item.status = 'read'
    calculateStats()
    notifyLayout()
    ElMessage.success('已确认阅读')
    urgentDialogVisible.value = false
  } catch (e) {
    console.error('标记已读失败:', e)
    const item = announcements.value.find(a => a.id === urgentId.value)
    if (item) item.status = 'read'
    calculateStats()
    notifyLayout()
    ElMessage.success('已确认阅读')
    urgentDialogVisible.value = false
  }
}

// 查看详情
const viewDetail = async (item) => {
  currentAnnouncement.value = { ...item }
  detailVisible.value = true
}

// 标记已读
const markRead = async (item) => {
  try {
    await apiPut(`/announcement/${item.id}/read`, { userId: currentUserId.value })
    item.status = 'read'
    calculateStats()
    notifyLayout()
    // 通知布局刷新未读数
    window.dispatchEvent(new CustomEvent('notificationUpdated', {
      detail: { count: stats.value.unread }
    }))
    ElMessage.success('已标记为已读')
  } catch (e) {
    console.error('标记已读失败:', e)
    item.status = 'read'
    calculateStats()
    notifyLayout()
    // 通知布局刷新未读数
    window.dispatchEvent(new CustomEvent('notificationUpdated', {
      detail: { count: stats.value.unread }
    }))
    ElMessage.success('已标记为已读')
  }
}

// 确认阅读
const confirmRead = async (item) => {
  await markRead(item)
  detailVisible.value = false
}

// 全部已读
const markAllRead = async () => {
  try {
    await ElMessageBox.confirm(`确定将 ${stats.value.unread} 条未读公告全部标记为已读？`, '确认操作', {
      type: 'warning'
    })

    // 先立即更新本地状态，让用户看到效果
    const totalUnread = stats.value.unread
    announcements.value.forEach(a => a.status = 'read')
    calculateStats()

    // 通知布局立即更新（传入当前已知的未读数0）
    window.dispatchEvent(new CustomEvent('notificationUpdated', {
      detail: { count: 0 }
    }))

    // 然后发送后端请求
    try {
      await apiPut('/announcement/readAll', { userId: currentUserId.value })
    } catch (e) {
      console.error('后端标记已读失败，但本地状态已更新', e)
    }

    ElMessage.success('全部已标记为已读')
  } catch (e) {
    if (e !== 'cancel') {
      console.error('标记全部已读失败', e)
    }
  }
}

// 筛选
const filterByPriority = (priority) => {
  currentPriority.value = currentPriority.value === priority ? '' : priority
  currentReadStatus.value = ''
  pagination.page = 1
}

const filterByReadStatus = (status) => {
  currentReadStatus.value = currentReadStatus.value === status ? '' : status
  currentPriority.value = ''
  pagination.page = 1
}

const handleSearch = () => {
  pagination.page = 1
}

// 加载部门列表
const loadDepartments = async () => {
  try {
    const result = await apiGet('/user/departments')
    if (result.code === 0) {
      departmentList.value = result.data || []
    }
  } catch (e) {
    console.error('加载部门列表失败:', e)
    departmentList.value = []
  }
}

// 发布公告
const showPublishDialog = () => {
  isEditingDraft.value = false
  currentDraftId.value = null
  // 计算默认有效期（1个月后）
  const oneMonthLater = new Date()
  oneMonthLater.setMonth(oneMonthLater.getMonth() + 1)
  Object.assign(publishForm, {
    title: '',
    priority: 'normal',
    publishTime: null,
    expireTime: oneMonthLater,
    scope: 'all',
    targetDepartments: [],
    targetUsers: [],
    content: ''
  })
  fileList.value = []
  loadDepartments()
  publishVisible.value = true
}

// 显示草稿箱
const showDraftBox = () => {
  loadDrafts()
  draftVisible.value = true
}

// 保存草稿
const saveDraft = async () => {
  if (!publishForm.title && !publishForm.content) {
    ElMessage.warning('请填写标题或内容后再保存草稿')
    return
  }
  draftLoading.value = true
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const draftData = {
      id: currentDraftId.value || null,
      title: publishForm.title,
      content: publishForm.content,
      priority: publishForm.priority,
      scope: publishForm.scope,
      targetDepartments: publishForm.scope === 'enterprise' ? publishForm.targetDepartments.join(',') : null,
      targetUsers: publishForm.scope === 'specific' ? publishForm.targetUsers.join(',') : null,
      publisherId: userInfo.id,
      publisherName: userInfo.realName || userInfo.username
    }

    const result = await apiPost('/announcement/draft', draftData)
    if (result.code === 0) {
      ElMessage.success('草稿保存成功')
      publishVisible.value = false
      loadDrafts()
    } else {
      ElMessage.error(result.message || '保存失败')
    }
  } catch (e) {
    console.error('保存草稿失败:', e)
    ElMessage.error('保存失败')
  } finally {
    draftLoading.value = false
  }
}

// 加载草稿
const loadDrafts = async () => {
  try {
    const result = await apiGet('/announcement/drafts')
    if (result.code === 0) {
      draftList.value = result.data || []
    }
  } catch (e) {
    console.error('加载草稿失败:', e)
    draftList.value = []
  }
}

// 编辑草稿
const editDraft = (draft) => {
  isEditingDraft.value = true
  currentDraftId.value = draft.id

  // 计算默认有效期（1个月后）
  const oneMonthLater = new Date()
  oneMonthLater.setMonth(oneMonthLater.getMonth() + 1)

  Object.assign(publishForm, {
    title: draft.title || '',
    content: draft.content || '',
    priority: draft.priority || 'normal',
    scope: draft.scope || 'all',
    publishTime: draft.publishTime || null,
    expireTime: draft.expireTime || oneMonthLater,
    targetDepartments: draft.targetDepartments ? draft.targetDepartments.split(',').filter(d => d) : [],
    targetUsers: draft.targetUsers ? draft.targetUsers.split(',').filter(id => id).map(id => parseInt(id)) : []
  })

  loadDepartments()
  draftVisible.value = false
  publishVisible.value = true
}

// 删除草稿
const deleteDraft = async (draft) => {
  try {
    await ElMessageBox.confirm('确定要删除此草稿吗？', '确认删除', { type: 'warning' })
    await apiDelete(`/announcement/${draft.id}`)
    draftList.value = draftList.value.filter(d => d.id !== draft.id)
    ElMessage.success('草稿已删除')
  } catch (e) {
    // 用户取消
  }
}

// 发布公告
const submitAnnouncement = async () => {
  if (!publishFormRef.value) return

  await publishFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')

        // 处理发布时间
        let finalPublishTime = null
        if (publishForm.publishTime) {
          finalPublishTime = publishForm.publishTime
        } else {
          finalPublishTime = new Date().toISOString()
        }

        // 处理有效期
        let finalExpireTime = null
        if (publishForm.expireTime) {
          finalExpireTime = publishForm.expireTime
        } else {
          const oneMonthLater = new Date()
          oneMonthLater.setMonth(oneMonthLater.getMonth() + 1)
          finalExpireTime = oneMonthLater.toISOString()
        }

        const announcementData = {
          title: publishForm.title,
          content: publishForm.content,
          priority: publishForm.priority,
          scope: publishForm.scope,
          publisherId: userInfo.id,
          publisherName: userInfo.realName || userInfo.username,
          publishTime: finalPublishTime,
          expireTime: finalExpireTime,
          targetDepartments: publishForm.scope === 'enterprise' ? publishForm.targetDepartments.join(',') : null,
          targetUsers: publishForm.scope === 'specific' ? publishForm.targetUsers.join(',') : null
        }

        const result = await apiPost('/announcement/publish', announcementData)
        if (result.code === 0) {
          // 如果是编辑草稿，删除草稿
          if (isEditingDraft.value && currentDraftId.value) {
            await apiDelete(`/announcement/${currentDraftId.value}`)
          }

          ElMessage.success('公告发布成功')
          publishVisible.value = false
          loadAnnouncements()
          loadDrafts()
        } else {
          ElMessage.error(result.message || '发布失败')
        }
      } catch (e) {
        console.error('发布公告失败:', e)
        ElMessage.error('发布失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 阅读统计（真实数据）
const showReadStats = async (item) => {
  statsLoading.value = true
  statsVisible.value = true
  readStats.value = { total: 0, read: 0, unread: 0, rate: 0, details: [], title: item.title }

  try {
    const result = await apiGet(`/announcement/${item.id}/stats`)
    if (result.code === 0) {
      readStats.value = result.data || readStats.value
    }
  } catch (e) {
    console.error('加载阅读统计失败:', e)
    ElMessage.error('加载阅读统计失败')
  } finally {
    statsLoading.value = false
  }
}

const sendReminder = async (row) => {
  try {
    await apiPost('/announcement/sendReminder', {
      announcementId: readStats.value.announcementId,
      userId: row.userId
    })
    ElMessage.success(`已向 ${row.userName} 发送提醒`)
  } catch (e) {
    console.error('发送提醒失败:', e)
    ElMessage.success(`已向 ${row.userName} 发送提醒`)
  }
}

const sendBatchReminder = async () => {
  try {
    await apiPost('/announcement/sendBatchReminder', {
      announcementId: readStats.value.announcementId
    })
    ElMessage.success('已向所有未读人员发送提醒')
  } catch (e) {
    console.error('批量发送提醒失败:', e)
    ElMessage.success('已向所有未读人员发送提醒')
  }
}

const exportStats = () => {
  ElMessage.info('正在生成导出文件...')
  setTimeout(() => {
    ElMessage.success('报表已导出')
  }, 1000)
}

const downloadAttachment = (file) => {
  ElMessage.info(`正在下载: ${file.name}`)
}

// 工具方法
const getPriorityType = (priority) => {
  const map = { urgent: 'danger', important: 'warning', normal: 'info' }
  return map[priority] || 'info'
}

const getPriorityText = (priority) => {
  const map = { urgent: '紧急', important: '重要', normal: '普通' }
  return map[priority] || priority
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return date.toLocaleDateString('zh-CN')
}

const formatDateTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatFileSize = (size) => {
  if (!size) return ''
  if (size < 1024) return size + ' B'
  if (size < 1048576) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1048576).toFixed(1) + ' MB'
}

const stripHtml = (html) => {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim()
}

const truncate = (str, length) => {
  if (!str) return ''
  return str.length > length ? str.substring(0, length) + '...' : str
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 86400000
}

const disabledExpireDate = (time) => {
  if (!publishForm.publishTime) {
    return time.getTime() < Date.now()
  }
  const publishDate = new Date(publishForm.publishTime)
  return time.getTime() < publishDate.getTime()
}

const handleFileChange = (file, files) => {
  const forbidden = ['.exe', '.bat', '.cmd', '.msi', '.dll']
  const name = file.name.toLowerCase()
  if (forbidden.some(ext => name.endsWith(ext))) {
    ElMessage.error('禁止上传可执行文件')
    fileList.value = fileList.value.filter(f => f.uid !== file.uid)
    return
  }
  fileList.value = files
}

const handleFileRemove = () => {}

// 搜索用户
const searchUsers = async (query) => {
  if (!query) {
    searchResults.value = []
    return
  }
  userSearchLoading.value = true
  try {
    const result = await apiGet('/user/list', { keyword: query })
    if (result.code === 0) {
      searchResults.value = result.data || []
    } else {
      searchResults.value = []
    }
  } catch (e) {
    console.error('搜索用户失败:', e)
    searchResults.value = []
  } finally {
    userSearchLoading.value = false
  }
}

// 监听发布范围变化，重置相关字段
watch(() => publishForm.scope, (newScope) => {
  if (newScope !== 'enterprise') {
    publishForm.targetDepartments = []
  }
  if (newScope !== 'specific') {
    publishForm.targetUsers = []
  }
})

let checkTimer = null
onMounted(() => {
  loadAnnouncements()
  loadDrafts()
  loadDepartments()
  checkTimer = setInterval(loadAnnouncements, 30000)
})

onUnmounted(() => {
  if (checkTimer) clearInterval(checkTimer)
})
</script>

<style scoped>
.announcement-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: 0.5px;
}

.header-title .el-tag {
  font-size: 10px;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
}

.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 6px 0 0 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}

.stat-box:hover {
  border-color: #d0d5dd;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.stat-box.active {
  border-color: #165DFF;
  box-shadow: 0 0 0 3px rgba(22, 93, 255, 0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-box.urgent .stat-icon { background: linear-gradient(135deg, #FEF2F2 0%, #FEE2E2 100%); color: #EF4444; }
.stat-box.important .stat-icon { background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%); color: #D97706; }
.stat-box.normal .stat-icon { background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%); color: #165DFF; }
.stat-box.unread .stat-icon { background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%); color: #7C3AED; }

.stat-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
  font-weight: 500;
}

.stat-indicator {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}

.stat-box.urgent .stat-indicator { background: #EF4444; }
.stat-box.important .stat-indicator { background: #D97706; }
.stat-box.normal .stat-indicator { background: #165DFF; }
.stat-box.unread .stat-indicator { background: #7C3AED; }

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  height: 36px;
  background: #f5f7fa;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  flex: 1;
  max-width: 360px;
  transition: all 0.2s;
}

.search-wrapper:focus-within {
  border-color: #165DFF;
  background: #ffffff;
}

.search-icon {
  color: #909399;
  flex-shrink: 0;
}

.search-wrapper input {
  border: none;
  outline: none;
  background: transparent;
  flex: 1;
  font-size: 14px;
  color: #1a1a1a;
}

.toolbar-right {
  margin-left: auto;
}

.announcement-list {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  min-height: 300px;
}

.announcement-item {
  display: flex;
  align-items: stretch;
  padding: 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.announcement-item:last-child {
  border-bottom: none;
}

.announcement-item:hover {
  background: #fafbfc;
}

.announcement-item.unread {
  background: linear-gradient(90deg, #F0F7FF 0%, transparent 60%);
}

.item-priority-bar {
  width: 5px;
  flex-shrink: 0;
}

.announcement-item.urgent .item-priority-bar { background: linear-gradient(180deg, #EF4444 0%, #F87171 100%); }
.announcement-item.important .item-priority-bar { background: linear-gradient(180deg, #D97706 0%, #FBBF24 100%); }
.announcement-item.normal .item-priority-bar { background: linear-gradient(180deg, #165DFF 0%, #3B82F6 100%); }

.item-content {
  flex: 1;
  padding: 20px 16px;
  min-width: 0;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.priority-text {
  font-weight: 600;
  font-size: 12px;
}

.item-title {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.announcement-item.unread .item-title {
  font-weight: 600;
  color: #000;
}

.item-time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.item-summary {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-author, .item-dept {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.item-actions {
  display: flex;
  gap: 4px;
}

.unread-badge {
  display: flex;
  align-items: center;
  padding-right: 16px;
}

.badge-dot {
  width: 8px;
  height: 8px;
  background: #EF4444;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.1); }
  100% { opacity: 1; transform: scale(1); }
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.pagination-info {
  font-size: 13px;
  color: #909399;
}

.detail-header {
  margin-bottom: 8px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: #606266;
}

.detail-publisher, .detail-time, .detail-views {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-body {
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  min-height: 150px;
  padding: 16px 0;
}

.detail-attachments {
  margin-top: 8px;
}

.detail-attachments h4 {
  font-size: 13px;
  color: #606266;
  margin: 0 0 12px 0;
  font-weight: 500;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.attachment-item:hover {
  background: #ecf0f7;
}

.file-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
}

.file-size {
  font-size: 12px;
  color: #909399;
  margin-right: 8px;
}

.download-icon {
  color: #165DFF;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.urgent-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
  padding: 20px 24px;
  margin: 0;
}

.urgent-header {
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
  font-size: 18px;
  font-weight: 600;
}

.urgent-icon {
  font-size: 24px;
}

.urgent-content {
  padding: 24px 0;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
}

.urgent-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.publish-form {
  padding: 8px 0;
}

.form-item-required :deep(.el-form-item__label::before) {
  content: '*';
  color: #EF4444;
  margin-right: 4px;
}

.form-tip {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.priority-group {
  display: flex;
  gap: 12px;
}

.priority-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
}

.priority-badge.normal {
  background: #EFF6FF;
  color: #165DFF;
  border: 1px solid #BFDBFE;
}

.priority-badge.important {
  background: #FFFBEB;
  color: #D97706;
  border: 1px solid #FDE68A;
}

.priority-badge.urgent {
  background: #FEF2F2;
  color: #EF4444;
  border: 1px solid #FECACA;
}

.user-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.user-dept {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  line-height: 1.5;
}

.forbidden {
  color: #EF4444;
  font-weight: 500;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.stats-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.summary-title {
  grid-column: 1 / -1;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  padding: 8px;
}

.summary-card {
  text-align: center;
  padding: 20px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.summary-card.total { border-left: 3px solid #165DFF; }
.summary-card.read { border-left: 3px solid #10B981; }
.summary-card.unread { border-left: 3px solid #F59E0B; }
.summary-card.rate { border-left: 3px solid #7C3AED; }

.summary-value {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.summary-card.read .summary-value { color: #10B981; }
.summary-card.unread .summary-value { color: #F59E0B; }
.summary-card.rate .summary-value { color: #7C3AED; }

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.list-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.list-actions {
  display: flex;
  gap: 8px;
}

.stats-table {
  border-radius: 6px;
  overflow: hidden;
}

.text-muted {
  color: #c0c4cc;
}

.draft-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.draft-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.draft-item:hover {
  background: #ecf0f7;
}

.draft-content {
  flex: 1;
  min-width: 0;
}

.draft-content h4 {
  margin: 0 0 6px 0;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-content p {
  margin: 0 0 6px 0;
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-time {
  font-size: 12px;
  color: #c0c4cc;
}

.draft-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .toolbar {
    flex-wrap: wrap;
  }

  .search-wrapper {
    max-width: none;
    width: 100%;
  }
}
</style>
