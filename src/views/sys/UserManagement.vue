<template>
  <div class="user-management-content">

    <div class="section-header">
      <h2 class="section-title">{{ t('user.title') }}</h2>
      <el-button type="primary" @click="showUserModal">
        <span style="margin-right: 4px;">+</span> {{ t('user.add') }}
      </el-button>
    </div>

    <!-- 查询表单 -->
    <div class="search-form">
      <el-form :model="searchForm" inline>
        <el-form-item :label="t('user.username') + '：'">
          <el-input
            v-model="searchForm.username"
            :placeholder="t('common.input')"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item :label="t('user.realName') + '：'">
          <el-input
            v-model="searchForm.realName"
            :placeholder="t('common.input')"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item :label="t('user.role') + '：'">
          <el-select
            v-model="searchForm.roleId"
            :placeholder="t('common.select')"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in roles"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('user.search') }}</el-button>
          <el-button @click="resetSearch">{{ t('user.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 用户表格 -->
    <el-table :data="paginatedUsers" style="width: 100%" v-loading="loading" border stripe class="unified-table" :default-sort="{ prop: 'createTime', order: 'descending' }">
      <el-table-column type="index" :label="t('user.index')" width="80" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.current - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="username" :label="t('user.username')" min-width="120" />
      <el-table-column prop="realName" :label="t('user.realName')" min-width="100">
        <template #default="{ row }">{{ row.realName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="email" :label="t('user.email')" min-width="180">
        <template #default="{ row }">{{ row.email || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" :label="t('user.phone')" min-width="130">
        <template #default="{ row }">{{ row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column prop="role" :label="t('user.role')" width="120">
        <template #default="{ row }">
          <el-tag :type="row.role === 1 ? 'primary' : 'info'" size="small">
            {{ getRoleText(row.role) }}
          </el-tag>
          <el-tag v-if="getRoleNameById(row.roleId)" type="success" size="small" style="margin-left: 4px;">
            {{ getRoleNameById(row.roleId) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="t('user.status')" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? t('user.enabled') : t('user.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="t('user.createTime')" min-width="160">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column :label="t('user.actions')" fixed="right" width="260">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-tooltip :content="t('user.editUser')" placement="top" :show-after="300">
              <el-button link type="primary" size="small" @click="editUser(row)" class="action-btn">
                <el-icon class="action-icon"><Edit /></el-icon>
                <span>{{ t('user.edit') }}</span>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="t('user.resetPwd')" placement="top" :show-after="300">
              <el-button link type="warning" size="small" @click="resetPassword(row)" class="action-btn warning-btn">
                <el-icon class="action-icon"><Key /></el-icon>
                <span>{{ t('user.reset') }}</span>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="row.status === 1 ? t('user.disableUser') : t('user.enableUser')" placement="top" :show-after="300">
              <el-popconfirm
                :title="t('user.confirmStatus', { action: row.status === 1 ? t('user.disable') : t('user.enable'), username: row.username })"
                :confirmButtonText="t('common.confirm')"
                :cancelButtonText="t('common.cancel')"
                @confirm="toggleUserStatus(row)"
              >
                <template #reference>
                  <el-button link :type="row.status === 1 ? 'info' : 'success'" size="small" class="action-btn">
                    <el-icon class="action-icon"><component :is="row.status === 1 ? Lock : Unlock" /></el-icon>
                    <span>{{ row.status === 1 ? t('user.disable') : t('user.enable') }}</span>
                  </el-button>
                </template>
              </el-popconfirm>
            </el-tooltip>
            <el-tooltip :content="t('user.deleteUser')" placement="top" :show-after="300">
              <el-popconfirm
                :title="t('user.confirmDelete', { username: row.username })"
                :confirmButtonText="t('user.confirmDeleteBtn')"
                :cancelButtonText="t('common.cancel')"
                icon="Delete"
                iconColor="#f56c6c"
                @confirm="deleteUser(row)"
              >
                <template #reference>
                  <el-button link type="danger" size="small" class="action-btn danger-btn">
                    <el-icon class="action-icon"><Delete /></el-icon>
                    <span>{{ t('user.delete') }}</span>
                  </el-button>
                </template>
              </el-popconfirm>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="closeDialog"
    >
      <el-form :model="userForm" :rules="formRules" ref="userFormRef" label-width="80px">
        <el-form-item :label="t('user.username')" prop="username" v-if="!isEdit">
          <el-input v-model="userForm.username" :placeholder="t('user.inputUsername')" />
        </el-form-item>
        <el-form-item :label="t('user.password')" prop="password" v-if="!isEdit">
          <el-input
            v-model="userForm.password"
            type="password"
            :placeholder="t('user.inputPassword')"
            show-password
          />
        </el-form-item>
        <el-form-item :label="t('user.realName')" prop="realName">
          <el-input v-model="userForm.realName" :placeholder="t('user.inputRealName')" />
        </el-form-item>
        <el-form-item :label="t('user.email')" prop="email">
          <el-input v-model="userForm.email" :placeholder="t('user.inputEmail')" />
        </el-form-item>
        <el-form-item :label="t('user.phone')" prop="phone">
          <el-input v-model="userForm.phone" :placeholder="t('user.inputPhone')" />
        </el-form-item>
        <el-form-item :label="t('user.systemRole')" prop="role">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option :label="t('user.admin')" :value="1" />
            <el-option :label="t('user.normalUser')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('user.assignedRole')" prop="roleId">
          <el-select v-model="userForm.roleId" :placeholder="t('user.selectAssignedRole')" clearable style="width: 100%">
            <el-option
              v-for="item in roles"
              :key="item.id"
              :label="item.name + (item.description ? ' - ' + item.description : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveUser" :loading="submitLoading">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" :title="t('user.resetPwd')" width="400px">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px">
        <el-form-item :label="t('user.newPassword')" prop="password">
          <el-input
            v-model="pwdForm.password"
            type="password"
            :placeholder="t('user.inputNewPassword')"
            show-password
          />
        </el-form-item>
        <el-form-item :label="t('user.confirmPassword')" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            :placeholder="t('user.confirmNewPassword')"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmResetPassword" :loading="pwdLoading">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Edit, Key, Lock, Unlock, Delete } from '@element-plus/icons-vue'

const { t } = useI18n()

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

// 数据状态
const loading = ref(false)
const submitLoading = ref(false)
const pwdLoading = ref(false)
const users = ref([])
const roles = ref([]) // 角色列表

// 搜索表单
const searchForm = reactive({
  username: '',
  realName: '',
  role: null,
  roleId: null // 搜索的角色ID
})

// 分页参数
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 弹窗控制
const dialogVisible = ref(false)
const pwdDialogVisible = ref(false)
const isEdit = ref(false)
const currentUserId = ref(null)
const currentResetUser = ref(null)

// 表单引用
const userFormRef = ref(null)
const pwdFormRef = ref(null)

// 用户表单
const userForm = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  role: 0,
  roleId: null // 选择的角色ID
})

// 密码表单
const pwdForm = reactive({
  password: '',
  confirmPassword: ''
})

// 表单验证规则
const formRules = {
  username: [
    { required: true, message: () => t('user.validate.inputUsername'), trigger: 'blur' },
    { min: 2, max: 20, message: () => t('user.validate.usernameLength'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: () => t('user.validate.inputPassword'), trigger: 'blur' },
    { min: 6, max: 20, message: () => t('user.validate.passwordLength'), trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: () => t('user.validate.correctEmail'), trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: () => t('user.validate.correctPhone'), trigger: 'blur' }
  ]
}

const pwdRules = {
  password: [
    { required: true, message: () => t('user.validate.inputNewPassword'), trigger: 'blur' },
    { min: 6, max: 20, message: () => t('user.validate.passwordLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: () => t('user.validate.confirmPasswordAgain'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.password) {
          callback(new Error(t('user.validate.passwordMismatch')))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 计算属性
const dialogTitle = computed(() => isEdit.value ? t('user.editUser') : t('user.add'))

// 过滤后的用户列表
const filteredUsers = computed(() => {
  let list = users.value
  if (searchForm.username) {
    list = list.filter(u => u.username.includes(searchForm.username))
  }
  if (searchForm.realName) {
    list = list.filter(u => u.realName?.includes(searchForm.realName))
  }
  // 按角色ID搜索（支持管理员/普通用户的基础角色筛选）
  if (searchForm.role !== null && searchForm.role !== undefined) {
    list = list.filter(u => u.role === searchForm.role)
  }
  // 按具体角色ID搜索
  if (searchForm.roleId !== null && searchForm.roleId !== undefined) {
    list = list.filter(u => u.roleId === searchForm.roleId)
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedUsers = computed(() => {
  const start = (pagination.current - 1) * pagination.size
  const end = start + pagination.size
  return filteredUsers.value.slice(start, end)
})

// 获取角色文本
const getRoleText = (role) => {
  return role === 1 ? t('user.admin') : t('user.normalUser')
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  try {
    const result = await apiGet('/user')
    if (result.code === 0) {
      users.value = result.data || []
    }
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

// 加载角色列表
const loadRoles = async () => {
  try {
    const result = await apiGet('/role')
    if (result.code === 0) {
      roles.value = result.data || []
    }
  } catch {
    roles.value = []
  }
}

// 获取用户已分配的角色ID
const getUserRoleId = (user) => {
  // 如果用户有 roleId 字段，直接返回
  if (user.roleId) return user.roleId
  // 否则根据 role (0=普通用户, 1=管理员) 映射
  // 假设 roleId 为 1 时是管理员角色（系统管理员），roleId 为 2 时是普通用户角色
  return user.role === 1 ? 1 : 2
}

// 获取角色名称
const getRoleNameById = (roleId) => {
  const role = roles.value.find(r => r.id === roleId)
  return role ? role.name : ''
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  filteredUsers.value // 触发重新计算
}

// 重置搜索
const resetSearch = () => {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.role = null
  searchForm.roleId = null
  pagination.current = 1
}

// 分页变化
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
}

const handleCurrentChange = (page) => {
  pagination.current = page
}

// 显示新增用户弹窗
const showUserModal = () => {
  isEdit.value = false
  currentUserId.value = null
  Object.assign(userForm, {
    username: '',
    password: '',
    realName: '',
    email: '',
    phone: '',
    role: 0,
    roleId: null
  })
  dialogVisible.value = true
}

// 编辑用户
const editUser = (user) => {
  isEdit.value = true
  currentUserId.value = user.id
  Object.assign(userForm, {
    username: user.username,
    password: '',
    realName: user.realName || '',
    email: user.email || '',
    phone: user.phone || '',
    role: user.role,
    roleId: user.roleId || null
  })
  dialogVisible.value = true
}

// 关闭弹窗
const closeDialog = () => {
  userFormRef.value?.resetFields()
}

// 保存用户
const saveUser = async () => {
  if (!userFormRef.value) return

  await userFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (isEdit.value) {
        const payload = {
          realName: userForm.realName,
          email: userForm.email,
          phone: userForm.phone,
          role: userForm.role,
          roleId: userForm.roleId
        }
        const result = await apiPut(`/user/${currentUserId.value}`, payload)
        if (result.code === 0) {
          const index = users.value.findIndex(u => u.id === currentUserId.value)
          if (index !== -1) {
            users.value[index] = { ...users.value[index], ...payload }
          }
          ElMessage.success(t('common.updateSuccess'))
        } else {
          ElMessage.error(result.message || t('common.updateFailed'))
        }
      } else {
        const result = await apiPost('/user/register', {
          username: userForm.username,
          password: userForm.password,
          realName: userForm.realName,
          email: userForm.email,
          phone: userForm.phone,
          role: userForm.role,
          roleId: userForm.roleId
        })
        if (result.code === 0) {
          users.value.unshift(result.data)
          ElMessage.success(t('common.createSuccess'))
        } else {
          ElMessage.error(result.message || t('common.createFailed'))
        }
      }
      dialogVisible.value = false
    } finally {
      submitLoading.value = false
    }
  })
}

// 重置密码
const resetPassword = (user) => {
  currentResetUser.value = user
  pwdForm.password = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

// 确认重置密码
const confirmResetPassword = async () => {
  if (!pwdFormRef.value) return

  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return

    pwdLoading.value = true
    try {
      const result = await apiPut(`/user/reset-password/${currentResetUser.value.id}`, { newPassword: pwdForm.password })
      if (result.code === 0) {
        ElMessage.success(t('user.passwordResetSuccess'))
        pwdDialogVisible.value = false
      } else {
        ElMessage.error(result.message || t('user.passwordResetFailed'))
      }
    } catch {
      ElMessage.error(t('common.requestFailed'))
    } finally {
      pwdLoading.value = false
    }
  })
}

// 切换用户状态
const toggleUserStatus = async (user) => {
  const action = user.status === 1 ? t('user.disable') : t('user.enable')

  ElMessageBox.confirm(t('user.confirmStatusTitle', { action, username: user.username }), t('common.tips'), {
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
    type: 'warning'
  }).then(async () => {
    try {
      const newStatus = user.status === 1 ? 0 : 1
      const result = await apiPut(`/user/${user.id}/status`, { status: newStatus })
      if (result.code === 0) {
        user.status = newStatus
        ElMessage.success(t('user.statusChangeSuccess', { action }))
      } else {
        ElMessage.error(result.message || t('user.statusChangeFailed', { action }))
      }
    } catch {
      ElMessage.error(t('common.requestFailed'))
    }
  })
}

// 删除用户
const deleteUser = (user) => {
  ElMessageBox.confirm(t('user.confirmDeleteTitle', { username: user.username }), t('common.warning'), {
    confirmButtonText: t('user.confirmDeleteBtn'),
    cancelButtonText: t('common.cancel'),
    type: 'error'
  }).then(async () => {
    try {
      const result = await apiDelete(`/user/${user.id}`)
      if (result.code === 0) {
        const index = users.value.findIndex(u => u.id === user.id)
        if (index !== -1) {
          users.value.splice(index, 1)
        }
        ElMessage.success(t('common.deleteSuccess'))
      } else {
        ElMessage.error(result.message || t('common.deleteFailed'))
      }
    } catch {
      ElMessage.error(t('common.requestFailed'))
    }
  })
}

// 初始化
onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>

<style scoped>
.user-management-content {
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.search-form {
  background: #fff;
  padding: 18px 16px;
  border-radius: 4px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  background: var(--bg-secondary, #ffffff);
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-color, #e5e7eb);
}

/* 统一表格样式 */
.unified-table :deep(.el-table) {
  border: none !important;
  border-radius: 12px;
  overflow: hidden;
}

.unified-table :deep(.el-table::before) {
  display: none;
}

.unified-table :deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  padding: 14px 0;
  border-bottom: 2px solid #e5e7eb !important;
}

.unified-table :deep(.el-table__body-wrapper td) {
  padding: 12px 0;
  vertical-align: middle;
  border-bottom: 1px solid #f0f0f0 !important;
  transition: all 0.2s ease;
}

.unified-table :deep(.el-table__header-wrapper th .cell),
.unified-table :deep(.el-table__body-wrapper td .cell) {
  padding: 0 16px;
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
  gap: 6px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 8px;
  transition: all 0.2s ease;
  font-size: 13px;
  font-weight: 500;
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(255,255,255,0.5) 50%, transparent 100%);
  transform: translateX(-100%);
  transition: transform 0.4s ease;
}

.action-btn:hover::before {
  transform: translateX(100%);
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-icon {
  font-size: 14px;
  transition: transform 0.2s ease;
}

.action-btn:hover .action-icon {
  transform: scale(1.15);
}

/* 编辑按钮 */
.action-btn[type="primary"]:hover {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.15) 0%, rgba(64, 158, 255, 0.05) 100%);
  color: #409eff;
}

/* 重置密码按钮 */
.warning-btn {
  background: rgba(230, 162, 60, 0.08);
  border: none;
}

.warning-btn:hover {
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.15) 0%, rgba(230, 162, 60, 0.05) 100%);
  color: #e6a23c;
}

/* 禁用/启用按钮 */
.action-btn[type="info"]:hover {
  background: linear-gradient(135deg, rgba(144, 147, 153, 0.15) 0%, rgba(144, 147, 153, 0.05) 100%);
  color: #909399;
}

.action-btn[type="success"]:hover {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.15) 0%, rgba(103, 194, 58, 0.05) 100%);
  color: #67c23a;
}

/* 删除按钮 */
.danger-btn {
  background: rgba(245, 108, 108, 0.08);
  border: none;
}

.danger-btn:hover {
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.15) 0%, rgba(245, 108, 108, 0.05) 100%);
  color: #f56c6c;
}
</style>
