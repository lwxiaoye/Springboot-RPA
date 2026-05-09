<template>
  <div class="credential-page">
    <div class="page-header">
      <h2>{{ t('credential.title') }}</h2>
      <p class="page-desc">{{ t('credential.pageDesc') }}</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-box">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">{{ t('credential.totalCount') }}</span>
      </div>
      <div class="stat-box warning">
        <span class="stat-num">{{ stats.expiring }}</span>
        <span class="stat-label">{{ t('credential.expiring') }}</span>
      </div>
      <div class="stat-box success">
        <span class="stat-num">{{ stats.active }}</span>
        <span class="stat-label">{{ t('credential.active') }}</span>
      </div>
      <div class="stat-box info">
        <span class="stat-num">{{ stats.usedToday }}</span>
        <span class="stat-label">{{ t('credential.usedToday') }}</span>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" :placeholder="t('credential.searchPlaceholder')" />
      </div>
      <el-select v-model="typeFilter" :placeholder="t('credential.typePlaceholder')" clearable style="width: 140px;">
        <el-option :label="t('credential.typePassword')" value="password" />
        <el-option :label="t('credential.typeApiKey')" value="apiKey" />
        <el-option :label="t('credential.typeSshKey')" value="sshKey" />
        <el-option :label="t('credential.typeCertificate')" value="certificate" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> {{ t('credential.add') }}
      </el-button>
    </div>

    <el-table :data="paginatedCredentials" v-loading="loading" border stripe class="unified-table">
      <el-table-column type="index" :label="t('credential.index')" width="60" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" :label="t('credential.name')" min-width="150" show-overflow-tooltip />
      <el-table-column prop="type" :label="t('credential.type')" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="username" :label="t('credential.username')" min-width="120" />
      <el-table-column prop="description" :label="t('credential.description')" min-width="150" show-overflow-tooltip />
      <el-table-column prop="expireTime" :label="t('credential.expireTime')" min-width="120">
        <template #default="{ row }">
          <span :class="getExpireClass(row.expireTime)">
            {{ getExpireText(row.expireTime) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="t('credential.status')" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row)" size="small">
            {{ getStatusText(row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastUsed" :label="t('credential.lastUsedLabel')" min-width="160" />
      <el-table-column :label="t('credential.actions')" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="primary" @click="viewDetail(row)" class="action-btn">{{ t('credential.detail') }}</el-button>
            <el-button link type="primary" @click="editCredential(row)" class="action-btn">{{ t('credential.editBtn') }}</el-button>
            <el-popconfirm
              :title="t('credential.confirmDelete', { name: row.name })"
              :confirmButtonText="t('credential.confirmDeleteBtn')"
              :cancelButtonText="t('credential.cancelBtn')"
              icon="Delete"
              iconColor="#f56c6c"
              @confirm="deleteCredential(row)"
            >
              <template #reference>
                <el-button link type="danger" class="action-btn">{{ t('credential.delete') }}</el-button>
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
        <el-form-item :label="t('credential.name')" prop="name">
          <el-input v-model="credentialForm.name" :placeholder="t('credential.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('credential.type')" prop="type">
          <el-select v-model="credentialForm.type" style="width: 100%;" @change="onTypeChange">
            <el-option :label="t('credential.typePassword')" value="password" />
            <el-option :label="t('credential.typeApiKey')" value="apiKey" />
            <el-option :label="t('credential.typeSshKey')" value="sshKey" />
            <el-option :label="t('credential.typeCertificate')" value="certificate" />
          </el-select>
        </el-form-item>
        
        <!-- 用户名/密码类型 -->
        <template v-if="credentialForm.type === 'password'">
          <el-form-item :label="t('credential.username')" prop="username">
            <el-input v-model="credentialForm.username" :placeholder="t('credential.usernamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('credential.password')" prop="password">
            <el-input v-model="credentialForm.password" type="password" :placeholder="t('credential.passwordPlaceholder')" show-password />
          </el-form-item>
        </template>
        
        <!-- API密钥类型 -->
        <template v-else-if="credentialForm.type === 'apiKey'">
          <el-form-item :label="t('credential.keyName')">
            <el-input v-model="credentialForm.keyName" :placeholder="t('credential.keyNamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('credential.apiKey')">
            <el-input v-model="credentialForm.apiKey" type="password" :placeholder="t('credential.apiKeyPlaceholder')" show-password />
          </el-form-item>
        </template>
        
        <!-- SSH密钥类型 -->
        <template v-else-if="credentialForm.type === 'sshKey'">
          <el-form-item :label="t('credential.privateKey')">
            <el-input v-model="credentialForm.privateKey" type="textarea" :rows="4" :placeholder="t('credential.privateKeyPlaceholder')" />
          </el-form-item>
        </template>
        
        <!-- 证书类型 -->
        <template v-else-if="credentialForm.type === 'certificate'">
          <el-form-item :label="t('credential.certContent')">
            <el-input v-model="credentialForm.certContent" type="textarea" :rows="4" :placeholder="t('credential.certContentPlaceholder')" />
          </el-form-item>
        </template>

        <el-form-item :label="t('credential.expireTime')">
          <el-date-picker v-model="credentialForm.expireTime" type="date" :placeholder="t('credential.expireTimePlaceholder')" style="width: 100%;" />
        </el-form-item>
        <el-form-item :label="t('credential.description')">
          <el-input v-model="credentialForm.description" type="textarea" :rows="2" :placeholder="t('credential.descriptionPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('credential.cancel') }}</el-button>
        <el-button type="primary" @click="submitCredential" :loading="submitLoading">{{ t('credential.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="t('credential.detailTitle')" width="500px">
      <div class="detail-content" v-if="currentCredential">
        <div class="detail-item"><label>{{ t('credential.name') }}：</label><span>{{ currentCredential.name }}</span></div>
        <div class="detail-item"><label>{{ t('credential.typeLabel') }}</label><span>{{ getTypeText(currentCredential.type) }}</span></div>
        <div class="detail-item"><label>{{ t('credential.usernameLabel') }}</label><span>{{ currentCredential.username || '-' }}</span></div>
        <div class="detail-item full"><label>{{ t('credential.passwordKey') }}</label><span class="masked">••••••••</span></div>
        <div class="detail-item"><label>{{ t('credential.expireTimeLabel') }}</label><span :class="getExpireClass(currentCredential.expireTime)">{{ getExpireText(currentCredential.expireTime) }}</span></div>
        <div class="detail-item"><label>{{ t('credential.statusLabel') }}</label><el-tag :type="getStatusTagType(currentCredential)" size="small">{{ getStatusText(currentCredential) }}</el-tag></div>
        <div class="detail-item"><label>{{ t('credential.createTimeLabel') }}</label><span>{{ currentCredential.createTime }}</span></div>
        <div class="detail-item"><label>{{ t('credential.lastUsedLabel') }}</label><span>{{ currentCredential.lastUsed || t('credential.neverUsed') }}</span></div>
        <div class="detail-item full"><label>{{ t('credential.descriptionLabel') }}</label><span>{{ currentCredential.description || '-' }}</span></div>
      </div>
      <template #footer>
        <el-button @click="rotateCredential" type="warning">{{ t('credential.rotateCredential') }}</el-button>
        <el-button @click="copyCredential">{{ t('credential.copyCredential') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const { t } = useI18n()

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

const formRules = computed(() => ({
  name: [{ required: true, message: t('credential.inputName'), trigger: 'blur' }],
  type: [{ required: true, message: t('credential.selectType'), trigger: 'change' }],
  username: [{ required: true, message: t('credential.inputUsername'), trigger: 'blur' }],
  password: [{ required: true, message: t('credential.inputPassword'), trigger: 'blur' }]
}))

const dialogTitle = computed(() => isEdit.value ? t('credential.edit') : t('credential.add'))

const filteredCredentials = computed(() => {
  let list = credentials.value
  if (searchKeyword.value) {
    list = list.filter(c => c.name.includes(searchKeyword.value))
  }
  if (typeFilter.value) {
    list = list.filter(c => c.type === typeFilter.value)
  }
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
  const map = { password: t('credential.typePassword'), apiKey: t('credential.typeApiKey'), sshKey: t('credential.typeSshKey'), certificate: t('credential.typeCertificate') }
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
  if (!expireTime) return t('credential.statusEnabled')
  const expire = new Date(expireTime)
  const now = new Date()
  const diff = (expire - now) / (1000 * 60 * 60 * 24)
  if (diff < 0) return t('credential.statusExpired')
  if (diff <= 7) return t('credential.statusExpiring')
  return credential.status === 'inactive' ? t('credential.statusDisabled') : t('credential.statusEnabled')
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
  if (!expireTime) return t('credential.neverExpire')
  const expire = new Date(expireTime)
  const now = new Date()
  const diff = (expire - now) / (1000 * 60 * 60 * 24)
  if (diff < 0) return t('credential.expired')
  if (diff <= 7) return t('credential.expireAfter', { days: Math.ceil(diff) })
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
    console.error('Load credential failed:', e)
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
            ElMessage.success(t('credential.updateSuccess'))
          } else {
            ElMessage.error(result.message || t('credential.updateFailed'))
            return
          }
        } else {
          // 调用后端创建API
          const result = await apiPost('/credential/create', credentialForm)
          if (result.code === 0) {
            ElMessage.success(t('credential.addSuccess'))
          } else {
            ElMessage.error(result.message || t('credential.createFailed'))
            return
          }
        }
        dialogVisible.value = false
        await loadCredentials()
      } catch (e) {
        ElMessage.error(t('credential.operationFailed'))
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
      ElMessage.success(t('credential.deleteSuccess'))
    } else {
      ElMessage.error(result.message || t('credential.deleteFailed'))
    }
  } catch (e) {
    ElMessage.error(t('credential.deleteFailed'))
  }
}

const viewDetail = (credential) => {
  currentCredential.value = credential
  detailVisible.value = true
}

const rotateCredential = () => {
  ElMessage.success(t('credential.rotateSuccess'))
}

const copyCredential = () => {
  ElMessage.success(t('credential.copied'))
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
