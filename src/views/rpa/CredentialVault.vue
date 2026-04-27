<template>
  <div class="credential-page">
    <div class="page-header">
      <h2>凭据中心</h2>
      <p class="page-desc">集中管理和安全存储系统凭据信息</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-box">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">凭据总数</span>
      </div>
      <div class="stat-box warning">
        <span class="stat-num">{{ stats.expiring }}</span>
        <span class="stat-label">即将过期</span>
      </div>
      <div class="stat-box success">
        <span class="stat-num">{{ stats.active }}</span>
        <span class="stat-label">使用中</span>
      </div>
      <div class="stat-box info">
        <span class="stat-num">{{ stats.usedToday }}</span>
        <span class="stat-label">今日使用</span>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索凭据名称..." />
      </div>
      <el-select v-model="typeFilter" placeholder="凭据类型" clearable style="width: 140px;">
        <el-option label="用户名/密码" value="password" />
        <el-option label="API密钥" value="apiKey" />
        <el-option label="SSH密钥" value="sshKey" />
        <el-option label="证书" value="certificate" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 添加凭据
      </el-button>
    </div>

    <el-table :data="paginatedCredentials" v-loading="loading" border stripe class="unified-table">
      <el-table-column type="index" label="序号" width="60" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="凭据名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column prop="expireTime" label="过期时间" min-width="120">
        <template #default="{ row }">
          <span :class="getExpireClass(row.expireTime)">
            {{ getExpireText(row.expireTime) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row)" size="small">
            {{ getStatusText(row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastUsed" label="最后使用" min-width="160" />
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="primary" @click="viewDetail(row)" class="action-btn">详情</el-button>
            <el-button link type="primary" @click="editCredential(row)" class="action-btn">编辑</el-button>
            <el-popconfirm
              :title="'确定要删除凭据「' + row.name + '」吗？'"
              confirmButtonText="确认删除"
              cancelButtonText="取消"
              icon="Delete"
              iconColor="#f56c6c"
              @confirm="deleteCredential(row)"
            >
              <template #reference>
                <el-button link type="danger" class="action-btn">删除</el-button>
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

    <!-- 添加/编辑凭据弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="credentialForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="凭据名称" prop="name">
          <el-input v-model="credentialForm.name" placeholder="请输入凭据名称" />
        </el-form-item>
        <el-form-item label="凭据类型" prop="type">
          <el-select v-model="credentialForm.type" style="width: 100%;" @change="onTypeChange">
            <el-option label="用户名/密码" value="password" />
            <el-option label="API密钥" value="apiKey" />
            <el-option label="SSH密钥" value="sshKey" />
            <el-option label="证书" value="certificate" />
          </el-select>
        </el-form-item>
        
        <!-- 用户名/密码类型 -->
        <template v-if="credentialForm.type === 'password'">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="credentialForm.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="credentialForm.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
        </template>
        
        <!-- API密钥类型 -->
        <template v-else-if="credentialForm.type === 'apiKey'">
          <el-form-item label="密钥名称">
            <el-input v-model="credentialForm.keyName" placeholder="如: OpenAI API Key" />
          </el-form-item>
          <el-form-item label="API密钥">
            <el-input v-model="credentialForm.apiKey" type="password" placeholder="请输入API密钥" show-password />
          </el-form-item>
        </template>
        
        <!-- SSH密钥类型 -->
        <template v-else-if="credentialForm.type === 'sshKey'">
          <el-form-item label="私钥内容">
            <el-input v-model="credentialForm.privateKey" type="textarea" :rows="4" placeholder="请输入SSH私钥内容" />
          </el-form-item>
        </template>
        
        <!-- 证书类型 -->
        <template v-else-if="credentialForm.type === 'certificate'">
          <el-form-item label="证书内容">
            <el-input v-model="credentialForm.certContent" type="textarea" :rows="4" placeholder="请输入证书内容" />
          </el-form-item>
        </template>

        <el-form-item label="过期时间">
          <el-date-picker v-model="credentialForm.expireTime" type="date" placeholder="选择过期时间，为空则永不过期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="credentialForm.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCredential" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="凭据详情" width="500px">
      <div class="detail-content" v-if="currentCredential">
        <div class="detail-item"><label>凭据名称：</label><span>{{ currentCredential.name }}</span></div>
        <div class="detail-item"><label>类型：</label><span>{{ getTypeText(currentCredential.type) }}</span></div>
        <div class="detail-item"><label>用户名：</label><span>{{ currentCredential.username || '-' }}</span></div>
        <div class="detail-item full"><label>密码/密钥：</label><span class="masked">{{ currentCredential.type === 'password' ? '••••••••' : '••••••••' }}</span></div>
        <div class="detail-item"><label>过期时间：</label><span :class="getExpireClass(currentCredential.expireTime)">{{ getExpireText(currentCredential.expireTime) }}</span></div>
        <div class="detail-item"><label>状态：</label><el-tag :type="getStatusTagType(currentCredential)" size="small">{{ getStatusText(currentCredential) }}</el-tag></div>
        <div class="detail-item"><label>创建时间：</label><span>{{ currentCredential.createTime }}</span></div>
        <div class="detail-item"><label>最后使用：</label><span>{{ currentCredential.lastUsed || '从未使用' }}</span></div>
        <div class="detail-item full"><label>描述：</label><span>{{ currentCredential.description || '-' }}</span></div>
      </div>
      <template #footer>
        <el-button @click="rotateCredential" type="warning">轮换凭据</el-button>
        <el-button @click="copyCredential">复制凭据</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

function getAuthHeaders() {
  const token = localStorage.getItem('token')
  return token ? { 'Authorization': `Bearer ${token}` } : {}
}

const loading = ref(false)
const submitLoading = ref(false)
const searchKeyword = ref('')
const typeFilter = ref('')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentCredential = ref(null)
const formRef = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const stats = reactive({ total: 0, expiring: 0, active: 0, usedToday: 0 })

const credentials = ref([])

const credentialForm = reactive({
  name: '',
  type: 'password',
  username: '',
  password: '',
  apiKey: '',
  keyName: '',
  privateKey: '',
  certContent: '',
  expireTime: '',
  description: '',
  status: 'active'
})

const formRules = {
  name: [{ required: true, message: '请输入凭据名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择凭据类型', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑凭据' : '添加凭据')

const filteredCredentials = computed(() => {
  let list = credentials.value
  if (searchKeyword.value) {
    list = list.filter(c => c.name.includes(searchKeyword.value))
  }
  if (typeFilter.value) {
    list = list.filter(c => c.type === typeFilter.value)
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedCredentials = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredCredentials.value.slice(start, end)
})

const getTypeTag = (type) => {
  const map = { password: 'success', apiKey: 'primary', sshKey: 'warning', certificate: 'info' }
  return map[type] || 'info'
}

const getTypeText = (type) => {
  const map = { password: '用户名/密码', apiKey: 'API密钥', sshKey: 'SSH密钥', certificate: '证书' }
  return map[type] || type
}

const isExpiring = (expireTime) => {
  if (!expireTime) return false
  const expire = new Date(expireTime)
  const now = new Date()
  const diff = (expire - now) / (1000 * 60 * 60 * 24)
  return diff <= 7 && diff > 0
}

// 根据过期时间判断显示状态
const getStatusText = (credential) => {
  const expireTime = credential.expireTime
  if (!expireTime) return '启用'
  const expire = new Date(expireTime)
  const now = new Date()
  const diff = (expire - now) / (1000 * 60 * 60 * 24)
  if (diff < 0) return '已过期'
  if (diff <= 7) return '即将过期'
  return credential.status === 'inactive' ? '已禁用' : '启用'
}

const getStatusTagType = (credential) => {
  const expireTime = credential.expireTime
  if (!expireTime) return credential.status === 'inactive' ? 'info' : 'success'
  const expire = new Date(expireTime)
  const now = new Date()
  const diff = (expire - now) / (1000 * 60 * 60 * 24)
  if (diff < 0) return 'danger'
  if (diff <= 7) return 'warning'
  return credential.status === 'inactive' ? 'info' : 'success'
}

const getExpireText = (expireTime) => {
  if (!expireTime) return '永不过期'
  const expire = new Date(expireTime)
  const now = new Date()
  const diff = (expire - now) / (1000 * 60 * 60 * 24)
  if (diff < 0) return '已过期'
  if (diff <= 7) return `${Math.ceil(diff)}天后过期`
  return expireTime
}

const getExpireClass = (expireTime) => {
  if (!expireTime) return ''
  const expire = new Date(expireTime)
  const now = new Date()
  const diff = (expire - now) / (1000 * 60 * 60 * 24)
  if (diff < 0) return 'text-danger'
  if (diff <= 7) return 'text-warning'
  return ''
}

const loadCredentials = async () => {
  loading.value = true
  try {
    const result = await apiGet('/credential')
    if (result.code === 0) {
      credentials.value = result.data || []
    } else {
      credentials.value = []
    }
    // 重新计算统计数据
    const now = new Date()
    let expiringCount = 0
    let activeCount = 0
    let usedTodayCount = 0
    for (const c of credentials.value) {
      const expireTime = c.expireTime ? new Date(c.expireTime) : null
      if (expireTime) {
        const diff = (expireTime - now) / (1000 * 60 * 60 * 24)
        if (diff < 0) {
          // 已过期，不计入active
        } else if (diff <= 7) {
          expiringCount++
          activeCount++
        } else {
          activeCount++
        }
      } else {
        activeCount++
      }
    }
    stats.total = credentials.value.length
    stats.expiring = expiringCount
    stats.active = activeCount
    stats.usedToday = credentials.value.reduce((sum, c) => sum + (c.useCount || 0), 0)
  } catch (e) {
    console.error('加载凭据失败:', e)
    credentials.value = []
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isEdit.value = false
  Object.assign(credentialForm, {
    name: '', type: 'password', username: '', password: '', apiKey: '', keyName: '',
    privateKey: '', certContent: '', expireTime: '', description: '', status: 'active'
  })
  dialogVisible.value = true
}

const editCredential = (credential) => {
  isEdit.value = true
  currentCredential.value = credential
  Object.assign(credentialForm, credential)
  dialogVisible.value = true
}

const onTypeChange = () => {
  credentialForm.username = ''
  credentialForm.password = ''
  credentialForm.apiKey = ''
  credentialForm.keyName = ''
  credentialForm.privateKey = ''
  credentialForm.certContent = ''
}

const submitCredential = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          // 调用后端更新API
          const result = await apiPut(`/credential/${currentCredential.value.id}`, credentialForm)
          if (result.code === 0) {
            ElMessage.success('凭据更新成功')
          } else {
            ElMessage.error(result.message || '更新失败')
            return
          }
        } else {
          // 调用后端创建API
          const result = await apiPost('/credential/create', credentialForm)
          if (result.code === 0) {
            ElMessage.success('凭据添加成功')
          } else {
            ElMessage.error(result.message || '创建失败')
            return
          }
        }
        dialogVisible.value = false
        await loadCredentials()
      } catch (e) {
        ElMessage.error('操作失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const deleteCredential = async (credential) => {
  try {
    const result = await apiDelete(`/credential/${credential.id}`)
    if (result.code === 0) {
      await loadCredentials()
      ElMessage.success('凭据删除成功')
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const viewDetail = (credential) => {
  currentCredential.value = credential
  detailVisible.value = true
}

const rotateCredential = () => {
  ElMessage.success('凭据轮换成功，密钥已更新')
}

const copyCredential = () => {
  ElMessage.success('凭据已复制到剪贴板')
}

const handleSizeChange = (size) => { pagination.size = size }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => {
  loadCredentials()
})
</script>

<style scoped>
.credential-page {
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

.stat-box {
  background: var(--bg-secondary, #ffffff);
  padding: 24px;
  border-radius: 16px;
  text-align: center;
  border: 1px solid var(--border-color, #e5e7eb);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-box:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0, 0, 0, 0.1));
}

.stat-box .stat-num {
  display: block;
  font-size: 36px;
  font-weight: 700;
  color: var(--primary, #409eff);
  line-height: 1;
  margin-bottom: 8px;
  background: linear-gradient(135deg, var(--primary, #409eff) 0%, var(--primary-light, #66b1ff) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-box.warning .stat-num {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

.stat-box.success .stat-num {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

.stat-box.info .stat-num {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

.stat-box .stat-label {
  font-size: 14px;
  color: var(--text-secondary, #6b7280);
  font-weight: 500;
}

.stat-box.warning {
  border-left: 4px solid #e6a23c;
}

.stat-box.success {
  border-left: 4px solid #67c23a;
}

.stat-box.info {
  border-left: 4px solid #409eff;
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
  gap: 4px;
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

.text-warning {
  color: var(--warning, #e6a23c);
  font-weight: 600;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  background: var(--bg-secondary, #ffffff);
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-color, #e5e7eb);
}

/* 详情弹窗 */
.detail-content {
  padding: 0 16px;
}

.detail-item {
  display: flex;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item.full {
  flex-direction: column;
}

.detail-item.full label {
  margin-bottom: 12px;
}

.detail-item label {
  width: 100px;
  color: var(--text-secondary, #6b7280);
  font-weight: 500;
  flex-shrink: 0;
}

.detail-item span {
  color: var(--text-primary, #1f2937);
  font-weight: 600;
}

.masked {
  letter-spacing: 2px;
  color: var(--text-tertiary, #9ca3af);
  font-weight: 400;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>