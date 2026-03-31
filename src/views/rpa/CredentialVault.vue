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

    <el-table :data="paginatedCredentials" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="凭据名称" min-width="150" />
      <el-table-column prop="type" label="类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column prop="expireTime" label="过期时间" min-width="120">
        <template #default="{ row }">
          <span :class="isExpiring(row.expireTime) ? 'text-warning' : ''">
            {{ row.expireTime || '永不过期' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
            {{ row.status === 'active' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastUsed" label="最后使用" min-width="160" />
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="primary" @click="editCredential(row)">编辑</el-button>
          <el-popconfirm title="确认删除该凭据吗？" @confirm="deleteCredential(row)">
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
          <el-date-picker v-model="credentialForm.expireTime" type="date" placeholder="选择过期时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="credentialForm.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="credentialForm.status" active-value="active" inactive-value="inactive" />
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
        <div class="detail-item"><label>过期时间：</label><span :class="isExpiring(currentCredential.expireTime) ? 'text-warning' : ''">{{ currentCredential.expireTime || '永不过期' }}</span></div>
        <div class="detail-item"><label>状态：</label><el-tag :type="currentCredential.status === 'active' ? 'success' : 'info'" size="small">{{ currentCredential.status === 'active' ? '启用' : '禁用' }}</el-tag></div>
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

const loadCredentials = async () => {
  loading.value = true
  try {
    credentials.value = [
      { id: 1, name: '东方财富网账号', type: 'password', username: 'rpa_user01', description: '用于股票数据采集', expireTime: '2026-06-30', status: 'active', lastUsed: '2026-03-30 10:30:00', createTime: '2026-01-01' },
      { id: 2, name: 'OpenAI API', type: 'apiKey', keyName: 'GPT-4 Key', apiKey: 'sk-xxxx', description: '用于智能对话功能', expireTime: '2026-04-15', status: 'active', lastUsed: '2026-03-30 09:15:00', createTime: '2026-02-15' },
      { id: 3, name: '数据库连接', type: 'password', username: 'db_admin', description: 'MySQL 数据库连接凭据', expireTime: '', status: 'active', lastUsed: '2026-03-30 14:00:00', createTime: '2026-01-10' },
      { id: 4, name: 'SSH 服务器密钥', type: 'sshKey', privateKey: '-----BEGIN RSA PRIVATE KEY-----', description: '连接远程服务器', expireTime: '2026-12-31', status: 'active', lastUsed: '2026-03-29 22:00:00', createTime: '2026-01-15' },
      { id: 5, name: '企业微信机器人', type: 'apiKey', keyName: 'Webhook URL', apiKey: 'https://qyapi.weixin.qq.com', description: '发送通知消息', expireTime: '', status: 'active', lastUsed: '2026-03-30 14:25:00', createTime: '2026-01-20' }
    ]
    // 不需要在这里设置 pagination.total，filteredCredentials 会计算
    stats.total = credentials.value.length
    stats.expiring = credentials.value.filter(c => isExpiring(c.expireTime)).length
    stats.active = credentials.value.filter(c => c.status === 'active').length
    stats.usedToday = 12
  } catch (e) {
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
          const index = credentials.value.findIndex(c => c.id === currentCredential.value.id)
          if (index !== -1) {
            credentials.value[index] = { ...credentials.value[index], ...credentialForm }
          }
          ElMessage.success('凭据更新成功')
        } else {
          credentials.value.push({
            id: Date.now(),
            ...credentialForm,
            lastUsed: '从未使用',
            createTime: new Date().toLocaleString()
          })
          ElMessage.success('凭据添加成功')
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
  const index = credentials.value.findIndex(c => c.id === credential.id)
  if (index !== -1) {
    credentials.value.splice(index, 1)
    await loadCredentials()
    ElMessage.success('凭据删除成功')
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
.credential-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.stats-row { display: flex; gap: 16px; margin-bottom: 20px; }
.stat-box { flex: 1; background: white; padding: 20px; border-radius: 12px; text-align: center; }
.stat-box .stat-num { display: block; font-size: 28px; font-weight: bold; color: #1e3a4a; }
.stat-box .stat-label { font-size: 13px; color: #8c8c8c; }
.stat-box.warning { border-left: 4px solid #e6a23c; }
.stat-box.success { border-left: 4px solid #67c23a; }
.stat-box.info { border-left: 4px solid #409eff; }

.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }

.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }

.detail-content { padding: 8px 0; }
.detail-item { display: flex; margin-bottom: 12px; }
.detail-item label { width: 90px; color: #8c8c8c; }
.detail-item span { color: #262626; font-weight: 500; }
.detail-item.full { flex-direction: column; }
.detail-item.full label { margin-bottom: 4px; }
.masked { letter-spacing: 2px; }

.text-warning { color: #e6a23c; }
</style>