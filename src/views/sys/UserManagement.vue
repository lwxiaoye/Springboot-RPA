<template>
  <div class="user-management-content">

    <div class="section-header">
      <h2 class="section-title">用户管理</h2>
      <el-button type="primary" @click="showUserModal">
        <span style="margin-right: 4px;">+</span> 新增用户
      </el-button>
    </div>

    <!-- 查询表单 -->
    <div class="search-form">
      <el-form :model="searchForm" inline>
        <el-form-item label="用户名：">
          <el-input 
            v-model="searchForm.username" 
            placeholder="请输入" 
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="姓名：">
          <el-input 
            v-model="searchForm.realName" 
            placeholder="请输入" 
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="角色：">
          <el-select 
            v-model="searchForm.role" 
            placeholder="请选择" 
            clearable
            style="width: 140px"
          >
            <el-option label="管理员" :value="1" />
            <el-option label="普通用户" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 用户表格 -->
    <el-table :data="paginatedUsers" style="width: 100%" v-loading="loading">
      <el-table-column type="index" label="序号" width="80">
        <template #default="{ $index }">
          {{ (pagination.current - 1) * pagination.size + $index + 1 }}
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100">
        <template #default="{ row }">{{ row.realName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180">
        <template #default="{ row }">{{ row.email || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="130">
        <template #default="{ row }">{{ row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 1 ? 'primary' : 'info'" size="small">
            {{ getRoleText(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="editUser(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="resetPassword(row)">重置密码</el-button>
          <el-button 
            size="small" 
            :type="row.status === 1 ? 'danger' : 'success'" 
            @click="toggleUserStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="deleteUser(row)">删除</el-button>
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
        <el-form-item label="用户名" prop="username" v-if="!isEdit">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input 
            v-model="userForm.password" 
            type="password" 
            placeholder="请输入密码" 
            show-password 
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="管理员" :value="1" />
            <el-option label="普通用户" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px">
        <el-form-item label="新密码" prop="password">
          <el-input 
            v-model="pwdForm.password" 
            type="password" 
            placeholder="请输入新密码" 
            show-password 
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="pwdForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码" 
            show-password 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResetPassword" :loading="pwdLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

// 数据状态
const loading = ref(false)
const submitLoading = ref(false)
const pwdLoading = ref(false)
const users = ref([])

// 搜索表单
const searchForm = reactive({
  username: '',
  realName: '',
  role: null
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
  role: 0
})

// 密码表单
const pwdForm = reactive({
  password: '',
  confirmPassword: ''
})

// 表单验证规则
const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')

// 过滤后的用户列表
const filteredUsers = computed(() => {
  let list = users.value
  if (searchForm.username) {
    list = list.filter(u => u.username.includes(searchForm.username))
  }
  if (searchForm.realName) {
    list = list.filter(u => u.realName?.includes(searchForm.realName))
  }
  if (searchForm.role !== null && searchForm.role !== undefined) {
    list = list.filter(u => u.role === searchForm.role)
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
  return role === 1 ? '管理员' : '普通用户'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
}

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  try {
    const result = await apiGet('/user')
    if (result.code === 0) {
      users.value = result.data || []
      // 不需要在这里设置 pagination.total，filteredUsers 会计算
    }
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadUsers()
}

// 重置搜索
const resetSearch = () => {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.role = null
  pagination.current = 1
  loadUsers()
}

// 分页变化
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  loadUsers()
}

const handleCurrentChange = (page) => {
  pagination.current = page
  loadUsers()
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
    role: 0
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
    role: user.role
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
          role: userForm.role
        }
        const result = await apiPut(`/user/${currentUserId.value}`, payload)
        if (result.code === 0) {
          const index = users.value.findIndex(u => u.id === currentUserId.value)
          if (index !== -1) {
            users.value[index] = { ...users.value[index], ...payload }
          }
          ElMessage.success('更新成功')
        } else {
          ElMessage.error(result.message || '更新失败')
        }
      } else {
        const result = await apiPost('/user/register', {
          username: userForm.username,
          password: userForm.password,
          realName: userForm.realName,
          email: userForm.email,
          phone: userForm.phone,
          role: userForm.role
        })
        if (result.code === 0) {
          users.value.unshift(result.data)
          pagination.total++
          ElMessage.success('创建成功')
        } else {
          ElMessage.error(result.message || '创建失败')
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
        ElMessage.success('密码重置成功')
        pwdDialogVisible.value = false
      } else {
        ElMessage.error(result.message || '重置失败')
      }
    } catch {
      ElMessage.error('请求失败')
    } finally {
      pwdLoading.value = false
    }
  })
}

// 切换用户状态
const toggleUserStatus = async (user) => {
  const action = user.status === 1 ? '禁用' : '启用'
  
  ElMessageBox.confirm(`确定要${action}用户 "${user.username}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const newStatus = user.status === 1 ? 0 : 1
      const result = await apiPut(`/user/${user.id}/status`, { status: newStatus })
      if (result.code === 0) {
        user.status = newStatus
        ElMessage.success(`${action}成功`)
      } else {
        ElMessage.error(result.message || `${action}失败`)
      }
    } catch {
      ElMessage.error('请求失败')
    }
  })
}

// 删除用户
const deleteUser = (user) => {
  ElMessageBox.confirm(`确定要删除用户 "${user.username}" 吗？删除后无法恢复！`, '警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      const result = await apiDelete(`/user/${user.id}`)
      if (result.code === 0) {
        const index = users.value.findIndex(u => u.id === user.id)
        if (index !== -1) {
          users.value.splice(index, 1)
          pagination.total--
        }
        ElMessage.success('删除成功')
      } else {
        ElMessage.error(result.message || '删除失败')
      }
    } catch {
      ElMessage.error('请求失败')
    }
  })
}

// 初始化
onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management-content {
  max-width: 1400px;
  margin: 0 auto;
}

.content-header {
  margin-bottom: 20px;
  background: white;
  padding: 16px 24px;
  border-radius: 12px;
}

.content-header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 500;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem 1.5rem;
  background: white;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.search-form {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 16px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  background: white;
  padding: 12px 20px;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-table th) {
  background-color: #f8fafc;
  color: #1e293b;
  font-weight: 600;
}

:deep(.el-table .el-table__row:hover) {
  background-color: #f8fafc;
}
</style>