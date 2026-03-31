<template>
  <div class="notifications-page">
    <div class="page-header">
      <h2>通知管理</h2>
      <p class="page-desc">管理和查看系统通知消息</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-box" :class="{ active: currentType === '' }" @click="filterByType('')">
        <span class="stat-num">{{ stats.total || 0 }}</span>
        <span class="stat-label">全部通知</span>
      </div>
      <div class="stat-box collect" :class="{ active: currentType === 'collect' }" @click="filterByType('collect')">
        <span class="stat-num">{{ stats.collect || 0 }}</span>
        <span class="stat-label">采集通知</span>
      </div>
      <div class="stat-box temp" :class="{ active: currentType === 'temp' }" @click="filterByType('temp')">
        <span class="stat-num">{{ stats.temp || 0 }}</span>
        <span class="stat-label">临时通知</span>
      </div>
      <div class="stat-box user" :class="{ active: currentType === 'user' }" @click="filterByType('user')">
        <span class="stat-num">{{ stats.user || 0 }}</span>
        <span class="stat-label">操作通知</span>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索通知标题..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="未读" value="unread" />
        <el-option label="已读" value="read" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 发送通知
      </el-button>
      <el-button @click="markAllRead" :disabled="!hasUnread">全部已读</el-button>
    </div>

    <el-table :data="paginatedNotifications" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="type" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)" size="small">
            {{ getTypeText(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
      <el-table-column prop="creatorName" label="发送人" width="100" align="center" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'unread' ? 'warning' : 'info'" size="small">
            {{ row.status === 'unread' ? '未读' : '已读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="primary" @click="markRead(row)" v-if="row.status === 'unread'">已读</el-button>
          <el-popconfirm title="确认删除该通知吗？" @confirm="deleteNotification(row)">
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

    <!-- 发送通知弹窗 -->
    <el-dialog v-model="dialogVisible" title="发送通知" width="520px">
      <el-form :model="notificationForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="通知类型" prop="type">
          <el-select v-model="notificationForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="采集通知" value="collect" />
            <el-option label="临时通知" value="temp" />
            <el-option label="操作通知" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="notificationForm.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="notificationForm.content" type="textarea" :rows="4" placeholder="请输入通知内容" />
        </el-form-item>
        <el-form-item label="接收者">
          <el-input v-model="notificationForm.receiverId" placeholder="留空表示全员通知" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNotification" :loading="submitLoading">发送</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="通知详情" width="500px">
      <div class="detail-content">
        <div class="detail-item"><label>通知类型：</label><span>{{ getTypeText(currentNotification.type) }}</span></div>
        <div class="detail-item"><label>标题：</label><span>{{ currentNotification.title }}</span></div>
        <div class="detail-item full"><label>内容：</label><span>{{ currentNotification.content || '-' }}</span></div>
        <div class="detail-item"><label>发送人：</label><span>{{ currentNotification.creatorName || '-' }}</span></div>
        <div class="detail-item"><label>状态：</label><span>{{ currentNotification.status === 'unread' ? '未读' : '已读' }}</span></div>
        <div class="detail-item"><label>创建时间：</label><span>{{ currentNotification.createTime }}</span></div>
        <div class="detail-item" v-if="currentNotification.readTime"><label>阅读时间：</label><span>{{ currentNotification.readTime }}</span></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const loading = ref(false)
const submitLoading = ref(false)
const notifications = ref([])
const searchKeyword = ref('')
const statusFilter = ref('')
const currentType = ref('')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const currentNotification = ref({})
const formRef = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const stats = ref({ total: 0, collect: 0, temp: 0, user: 0 })

const notificationForm = reactive({
  type: 'temp',
  title: '',
  content: '',
  receiverId: null
})

const formRules = {
  type: [{ required: true, message: '请选择通知类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入通知标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入通知内容', trigger: 'blur' }]
}

const getTypeText = (type) => {
  const map = { collect: '采集通知', temp: '临时通知', user: '操作通知' }
  return map[type] || type
}

const getTypeTag = (type) => {
  const map = { collect: 'success', temp: 'warning', user: 'info' }
  return map[type] || 'info'
}

const filteredNotifications = computed(() => {
  let list = notifications.value
  if (searchKeyword.value) {
    list = list.filter(n => n.title?.includes(searchKeyword.value) || n.content?.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(n => n.status === statusFilter.value)
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedNotifications = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredNotifications.value.slice(start, end)
})

const hasUnread = computed(() => notifications.value.some(n => n.status === 'unread'))

const loadNotifications = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (currentType.value) params.append('type', currentType.value)
    // 不使用后端的分页参数，前端自己处理分页
    // params.append('page', pagination.page)
    // params.append('size', pagination.size)
    
    const result = await apiGet(`/notification?${params.toString()}`)
    if (result.code === 0) {
      notifications.value = result.data || []
      // 不需要在这里设置 pagination.total，filteredNotifications 会计算
    }
  } catch {
    notifications.value = []
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const result = await apiGet('/notification/stats')
    if (result.code === 0 && result.data) {
      const totals = result.data.totals || {}
      stats.value = {
        total: (totals.collect || 0) + (totals.temp || 0) + (totals.user || 0),
        collect: totals.collect || 0,
        temp: totals.temp || 0,
        user: totals.user || 0
      }
    }
  } catch {
    stats.value = { total: 0, collect: 0, temp: 0, user: 0 }
  }
}

const filterByType = (type) => {
  currentType.value = type
  pagination.page = 1
  loadNotifications()
}

const showCreateModal = () => {
  Object.assign(notificationForm, { type: 'temp', title: '', content: '', receiverId: null })
  dialogVisible.value = true
}

const viewDetail = (notification) => {
  currentNotification.value = notification
  detailVisible.value = true
}

const markRead = async (notification) => {
  try {
    const result = await apiPut(`/notification/${notification.id}/read`)
    if (result.code === 0) {
      notification.status = 'read'
      ElMessage.success('已标记为已读')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const markAllRead = async () => {
  try {
    const type = currentType.value || undefined
    const result = await apiPut(`/notification/readAll?type=${type || ''}`)
    if (result.code === 0) {
      notifications.value.forEach(n => n.status = 'read')
      loadStats()
      ElMessage.success('全部已标记为已读')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const submitNotification = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const result = await apiPost('/notification', {
          type: notificationForm.type,
          title: notificationForm.title,
          content: notificationForm.content,
          receiverId: notificationForm.receiverId ? parseInt(notificationForm.receiverId) : null,
          creatorId: JSON.parse(localStorage.getItem('userInfo') || '{}').id,
          creatorName: JSON.parse(localStorage.getItem('userInfo') || '{}').realName || JSON.parse(localStorage.getItem('userInfo') || '{}').username
        })
        if (result.code === 0) {
          ElMessage.success('通知发送成功')
          dialogVisible.value = false
          await loadNotifications()
          await loadStats()
        } else {
          ElMessage.error(result.message || '发送失败')
        }
      } catch {
        ElMessage.error('请求失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const deleteNotification = async (notification) => {
  try {
    const result = await apiDelete(`/notification/${notification.id}`)
    if (result.code === 0) {
      const index = notifications.value.findIndex(n => n.id === notification.id)
      if (index !== -1) {
        notifications.value.splice(index, 1)
        pagination.total--
      }
      await loadStats()
      ElMessage.success('删除成功')
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch {
    ElMessage.error('请求失败')
  }
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1; loadNotifications() }
const handleCurrentChange = (page) => { pagination.page = page; loadNotifications() }

onMounted(() => {
  loadNotifications()
  loadStats()
})
</script>

<style scoped>
.notifications-page { max-width: 1400px; margin: 0 auto; }
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

.stat-box.active {
  border-color: #409eff;
  background: #ecf5ff;
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

.stat-box.collect { border-left: 4px solid #67c23a; }
.stat-box.temp { border-left: 4px solid #e6a23c; }
.stat-box.user { border-left: 4px solid #909399; }

.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }

.detail-content { padding: 8px 0; }
.detail-item { display: flex; margin-bottom: 12px; }
.detail-item label { width: 80px; color: #8c8c8c; }
.detail-item span { color: #262626; font-weight: 500; }
.detail-item.full { flex-direction: column; }
.detail-item.full label { margin-bottom: 4px; }
</style>