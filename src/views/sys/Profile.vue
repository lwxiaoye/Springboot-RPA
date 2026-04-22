<template>
  <div class="profile-wrapper">
    <div class="profile-layout">
      <!-- 左侧区域 -->
      <div class="profile-left">
        <!-- 头像卡片 -->
        <el-card class="avatar-card" shadow="never">
          <div class="card-header">
            <span class="header-title">我的头像</span>
          </div>
          <div class="avatar-content">
            <div class="avatar-circle" v-if="!avatarPreview && !currentUser.avatar">
              {{ userInitialsLarge }}
            </div>
            <div class="avatar-circle" v-else>
              <img 
                :src="getAvatarUrl(avatarPreview || currentUser.avatar)" 
                alt="头像" 
                @error="handleImageError"
              />
            </div>
            <el-button class="change-btn" @click="triggerAvatarUpload">
              <el-icon><Download /></el-icon>
              更换头像
            </el-button>
            <input 
              type="file" 
              ref="avatarInput" 
              style="display: none"
              accept="image/jpeg,png" 
              @change="handleAvatarChange" 
            />
          </div>
        </el-card>
  
        <!-- 基本信息卡片 -->
        <el-card class="info-card" shadow="never">
          <div class="card-header">
            <span class="header-title">
              <el-icon class="header-icon"><User /></el-icon>
              基本信息
            </span>
            <div class="header-actions">
              <el-button 
                v-if="!isEditing" 
                class="edit-btn" 
                type="primary" 
                size="small" 
                @click="startEditAll"
              >
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <div v-else class="action-buttons">
                <el-button size="small" @click="cancelEdit">
                  取消
                </el-button>
                <el-button size="small" type="primary" @click="saveAllInfo" :loading="saveLoading">
                  保存
                </el-button>
              </div>
            </div>
          </div>
          <el-form :model="editForm" label-width="90px" class="info-form">
            <el-form-item label="用户名">
              <div class="info-value">{{ currentUser.username }}</div>
            </el-form-item>
            <el-form-item label="真实姓名">
              <div v-if="!editingField.name" class="info-value">{{ editForm.realName || '未设置' }}</div>
              <el-input v-else v-model="editForm.realName" size="default" />
            </el-form-item>
            <el-form-item label="邮箱">
              <div v-if="!editingField.email" class="info-value">{{ editForm.email || '未设置' }}</div>
              <el-input v-else v-model="editForm.email" size="default" />
            </el-form-item>
            <el-form-item label="手机号">
              <div v-if="!editingField.mobile" class="info-value">{{ editForm.mobile || '未绑定' }}</div>
              <el-input v-else v-model="editForm.mobile" size="default" />
            </el-form-item>
            <el-form-item label="角色">
              <div class="info-value">{{ currentUser.role || '系统管理员' }}</div>
            </el-form-item>
            <el-form-item label="创建时间">
              <div class="info-value">{{ formatDate(currentUser.createTime) }}</div>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
  
      <!-- 右侧区域 -->
      <div class="profile-right">
        <!-- 账户安全卡片 -->
        <el-card class="security-card" shadow="never">
          <div class="card-header">
            <span class="header-title">
              <el-icon class="header-icon"><Lock /></el-icon>
              账户安全
            </span>
          </div>
          <div class="security-content">
            <div class="security-item">
              <div class="security-icon">
                <el-icon><Lock /></el-icon>
              </div>
              <div class="security-info">
                <div class="security-title">登录密码</div>
                <div class="security-desc">定期更换密码可以保护账户安全</div>
              </div>
              <el-button class="security-action" text type="primary" @click="activeTab = 'password'">
                修改
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </el-card>
  
        <!-- 账户统计卡片 -->
        <el-card class="stats-card" shadow="never">
          <div class="card-header">
            <span class="header-title">
              <el-icon class="header-icon"><DataAnalysis /></el-icon>
              账户统计
            </span>
          </div>
          <div class="stats-content">
            <div class="stat-item">
              <div class="stat-value">10</div>
              <div class="stat-label">账户天数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">今天</div>
              <div class="stat-label">最后登录</div>
            </div>
          </div>
        </el-card>
  
        <!-- 修改密码卡片 -->
        <el-card class="password-card" shadow="never" v-if="activeTab === 'password'">
          <div class="card-header">
            <span class="header-title">
              <el-icon class="header-icon"><Lock /></el-icon>
              修改密码
            </span>
          </div>
          <el-form :model="pwdModel" label-width="90px" class="pwd-form">
            <el-form-item label="原密码">
              <el-input 
                v-model="pwdModel.oldPassword" 
                type="password" 
                placeholder="请输入原密码"
                show-password
                @paste.prevent="preventPaste"
              />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input 
                v-model="pwdModel.newPassword" 
                type="password" 
                placeholder="8-24 位，包含字母、数字和特殊字符"
                show-password
                @paste.prevent="preventPaste"
              />
              <div v-if="pwdNewError" class="error-tip">{{ pwdNewError }}</div>
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input 
                v-model="pwdModel.confirmPassword" 
                type="password" 
                placeholder="请再次输入新密码"
                show-password
                @paste.prevent="preventPaste"
              />
              <div v-if="pwdConfirmError" class="error-tip">{{ pwdConfirmError }}</div>
            </el-form-item>
            <el-form-item>
              <el-button @click="resetPasswordForm">清空</el-button>
              <el-button type="primary" @click="changePassword" :loading="pwdLoading">
                更新密码
              </el-button>
            </el-form-item>
            <div class="pwd-tip">密码修改后需重新登录</div>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, User, Lock, DataAnalysis, ArrowRight, Edit } from '@element-plus/icons-vue'

import { apiGet, apiPost, apiPut, apiDelete, apiUpload } from '../../utils/api.js'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 当前用户信息
const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  mobile: '13800138002',
  role: '系统管理员',
  createTime: '2026-03-16T21:09:34',
  avatar: null,
  lastLoginTime: '2026-03-23 10:23:45'
})

const activeTab = ref('info')
const editForm = reactive({ realName: '', email: '', mobile: '' })
const editingField = reactive({ name: false, email: false, mobile: false })
const isEditing = ref(false)
const avatarPreview = ref(null)
const avatarInput = ref(null)
const saveLoading = ref(false)

const pwdModel = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdLoading = ref(false)
const pwdNewError = ref('')
const pwdConfirmError = ref('')

const userInitialsLarge = computed(() => 
  (currentUser.value.realName || currentUser.value.username).charAt(0).toUpperCase()
)

// 密码校验
watch(() => pwdModel.newPassword, (val) => {
  if (val) {
    // 长度验证
    if (val.length < 8 || val.length > 24) {
      pwdNewError.value = '密码长度需 8-24 位'
    } 
    // 复杂度验证：必须包含字母、数字和特殊字符
    else if (!/[a-zA-Z]/.test(val) || !/[0-9]/.test(val) || !/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(val)) {
      pwdNewError.value = '密码必须包含字母、数字和特殊字符'
    } else {
      pwdNewError.value = ''
    }
  } else {
    pwdNewError.value = ''
  }
  
  if (pwdModel.confirmPassword && pwdModel.confirmPassword !== val) {
    pwdConfirmError.value = '两次输入密码不一致'
  } else {
    pwdConfirmError.value = ''
  }
})

watch(() => pwdModel.confirmPassword, (val) => {
  if (val && val !== pwdModel.newPassword) {
    pwdConfirmError.value = '两次输入密码不一致'
  } else {
    pwdConfirmError.value = ''
  }
})

const preventPaste = (e) => {
  e.preventDefault()
  ElMessage.warning('出于安全考虑，禁止粘贴密码')
}

const initEditForm = () => {
  editForm.realName = currentUser.value.realName || ''
  editForm.email = currentUser.value.email || ''
  editForm.mobile = currentUser.value.mobile || ''
}

const startEditAll = () => {
  editingField.name = true
  editingField.email = true
  editingField.mobile = true
  isEditing.value = true
}

const cancelEdit = () => {
  editingField.name = false
  editingField.email = false
  editingField.mobile = false
  isEditing.value = false
  initEditForm()
  ElMessage.info('已取消编辑')
}

const startEdit = (field) => {
  if (field === 'name') editingField.name = true
  if (field === 'email') editingField.email = true
  if (field === 'mobile') editingField.mobile = true
}

const saveField = (field) => {
  if (field === 'name') editingField.name = false
  if (field === 'email') editingField.email = false
  if (field === 'mobile') editingField.mobile = false
}

const resetInfo = () => {
  initEditForm()
  ElMessage.info('已重置为当前用户信息')
}

const saveAllInfo = async () => {
  saveLoading.value = true
  try {
    const payload = { 
      realName: editForm.realName, 
      email: editForm.email, 
      mobile: editForm.mobile 
    }
    const result = await apiPut(`/user/${currentUser.value.id}`, payload)
    if (result.code === 0) {
      currentUser.value.realName = payload.realName
      currentUser.value.email = payload.email
      currentUser.value.mobile = payload.mobile
      const storedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
      storedUser.realName = payload.realName
      storedUser.email = payload.email
      storedUser.mobile = payload.mobile
      localStorage.setItem('userInfo', JSON.stringify(storedUser))
      ElMessage.success('基本信息更新成功')
      editingField.name = false
      editingField.email = false
      editingField.mobile = false
      isEditing.value = false
    } else {
      ElMessage.error(result.message || '保存失败')
    }
  } catch (err) {
    ElMessage.warning('后端未响应，模拟本地保存')
    currentUser.value.realName = editForm.realName
    currentUser.value.email = editForm.email
    currentUser.value.mobile = editForm.mobile
    const storedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
    storedUser.realName = editForm.realName
    storedUser.email = editForm.email
    storedUser.mobile = editForm.mobile
    localStorage.setItem('userInfo', JSON.stringify(storedUser))
    editingField.name = false
    editingField.email = false
    editingField.mobile = false
    isEditing.value = false
    ElMessage.success('本地更新成功')
  } finally {
    saveLoading.value = false
  }
}

const changePassword = async () => {
  if (!pwdModel.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!pwdModel.newPassword || pwdNewError.value) {
    ElMessage.warning('请填写符合要求的新密码')
    return
  }
  if (pwdModel.newPassword !== pwdModel.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  
  pwdLoading.value = true
  try {
    // 使用 apiPut 工具，自动携带 JWT Token
    const token = localStorage.getItem('token')
    console.log('当前 Token:', token ? token.substring(0, 20) + '...' : '无')
    console.log('请求参数:', {
      oldPassword: pwdModel.oldPassword,
      newPassword: pwdModel.newPassword
    })
    
    const result = await apiPut(`/user/password/${currentUser.value.id}`, {
      oldPassword: pwdModel.oldPassword,
      newPassword: pwdModel.newPassword
    })
    
    console.log('后端响应:', result)
    
    if (result.code === 0) {
      ElMessage.success('密码修改成功，请重新登录')
      resetPasswordForm()
      setTimeout(() => {
        ElMessageBox.confirm('密码已修改，建议重新登录', '提示', { confirmButtonText: '去登录' }).then(() => {
          localStorage.removeItem('userInfo')
          localStorage.removeItem('token')
          window.location.href = '/login'
        })
      }, 800)
    } else {
      ElMessage.error(result.message || '修改失败')
    }
  } catch (err) {
    console.error('密码修改失败:', err)
    ElMessage.error('网络错误，请确保后端服务可用')
  } finally {
    pwdLoading.value = false
  }
}

const resetPasswordForm = () => {
  pwdModel.oldPassword = ''
  pwdModel.newPassword = ''
  pwdModel.confirmPassword = ''
  pwdNewError.value = ''
  pwdConfirmError.value = ''
}

const triggerAvatarUpload = () => {
  avatarInput.value.click()
}

const handleAvatarChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  const reader = new FileReader()
  reader.onload = async (ev) => {
    avatarPreview.value = ev.target.result
    try {
      const formData = new FormData()
      formData.append('avatar', file)
      const response = await fetch(`${API_BASE}/user/avatar/${currentUser.value.id}`, { 
        method: 'POST', 
        body: formData 
      })
      const result = await response.json()
      if (result.code === 0) {
        // 使用后端返回的相对路径
        currentUser.value.avatar = result.data?.imageUrl || avatarPreview.value
        console.log('头像上传成功，新路径:', currentUser.value.avatar)
        
        // 同步更新 localStorage
        const storedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
        storedUser.avatar = currentUser.value.avatar
        localStorage.setItem('userInfo', JSON.stringify(storedUser))
        
        // 触发自定义事件，通知其他组件更新头像
        window.dispatchEvent(new Event('avatarUpdated'))
        
        ElMessage.success('头像更新成功')
      } else {
        ElMessage.error('上传失败')
      }
    } catch (err) {
      currentUser.value.avatar = avatarPreview.value
      ElMessage.success('头像已更换（本地模拟）')
    }
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

// 处理图片加载失败
const handleImageError = (e) => {
  const imgSrc = e.target.src
  console.error('❌ 头像加载失败:', imgSrc)
  console.error('当前用户头像路径:', currentUser.value.avatar)
  ElMessage.error('头像加载失败，请检查后端服务是否启动')
}

const formatDate = (isoString) => {
  if (!isoString) return '2026-03-16'
  return new Date(isoString).toLocaleString('zh-CN', { hour12: false })
}

// 获取头像完整 URL
const getAvatarUrl = (path) => {
  if (!path) {
    console.log('头像路径为空')
    return null
  }
  // 如果已经是完整 URL，直接返回
  if (path.startsWith('http://') || path.startsWith('https://')) {
    console.log('使用完整 URL:', path)
    return path
  }
  // 否则拼接 API 基础 URL
  const fullUrl = `${API_BASE}${path}`
  console.log('头像路径处理 - 原始路径:', path, '| 完整 URL:', fullUrl)
  return fullUrl
}

const loadUserFromStorage = () => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUser.value = { ...currentUser.value, ...user }
      
      console.log('从 localStorage 加载的用户信息:', currentUser.value)
      console.log('头像路径:', currentUser.value.avatar)
      
      // 修复旧的头像路径格式（去掉开头的 /api）
      if (currentUser.value.avatar && currentUser.value.avatar.startsWith('/api/')) {
        currentUser.value.avatar = currentUser.value.avatar.replace('/api/', '/')
        // 更新 localStorage
        user.avatar = currentUser.value.avatar
        localStorage.setItem('userInfo', JSON.stringify(user))
        console.log('已修复头像路径:', currentUser.value.avatar)
      }
      
      if (!currentUser.value.mobile) currentUser.value.mobile = '13800138002'
      if (!currentUser.value.role) currentUser.value.role = '系统管理员'
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  } else {
    console.warn('localStorage 中没有用户信息，请重新登录')
  }
  initEditForm()
}

// 从后端获取最新用户信息
const loadUserProfile = async () => {
  try {
    const result = await apiGet(`/user/${currentUser.value.id}`)
    if (result.code === 0 && result.data) {
      console.log('从后端获取的用户信息:', result.data)
      currentUser.value = { ...currentUser.value, ...result.data }
      console.log('更新后的头像路径:', currentUser.value.avatar)
      
      // 同时更新 localStorage
      const storedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
      storedUser.avatar = currentUser.value.avatar
      localStorage.setItem('userInfo', JSON.stringify(storedUser))
    }
  } catch (err) {
    console.error('获取用户信息失败:', err)
  }
}

onMounted(() => {
  loadUserFromStorage()
  // 从后端获取最新的用户信息，确保头像路径正确
  loadUserProfile()
})
</script>

<style scoped>
.profile-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f7fa;
}

.profile-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
}

.profile-left {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 卡片通用样式 */
.avatar-card,
.info-card,
.security-card,
.stats-card,
.password-card {
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

:deep(.el-card__body) {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
  color: #3b82f6;
}

/* 头像卡片 */
.avatar-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 0;
}

.avatar-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 600;
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  margin-bottom: 20px;
  overflow: hidden;
  border: 4px solid white;
}

.avatar-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.change-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: 1px dashed #d1d5db;
  background: white;
  color: #6b7280;
  border-radius: 6px;
  font-size: 13px;
}

.change-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #eff6ff;
}

/* 基本信息表单 */
.info-form {
  margin-top: 8px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
  width: 90px;
}

.info-value {
  font-size: 14px;
  color: #1f2937;
  padding: 8px 0;
  line-height: 1.5;
  font-weight: 500;
  border: none;
  background: transparent;
  width: 100%;
}

:deep(.el-input) {
  width: 100%;
}

:deep(.el-input__wrapper) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 0;
}

:deep(.el-input__inner) {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
  padding: 8px 0;
}

:deep(.el-input.is-disabled .el-input__inner) {
  color: #9ca3af;
  -webkit-text-fill-color: #9ca3af;
}

.edit-btn {
  padding: 6px 16px;
  font-size: 13px;
  border-radius: 6px;
  background: #3b82f6;
}

.edit-btn:hover {
  background: #2563eb;
}

/* 编辑模式下的输入框 */
:deep(.el-form-item .el-input__wrapper) {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 12px;
  background: white;
  transition: all 0.2s;
}

:deep(.el-form-item .el-input__wrapper:hover) {
  border-color: #3b82f6;
}

:deep(.el-form-item .el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

/* 账户安全 */
.security-content {
  padding: 8px 0;
}

.security-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.security-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.security-info {
  flex: 1;
}

.security-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.security-desc {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

.security-action {
  font-size: 13px;
  color: #3b82f6;
  display: flex;
  align-items: center;
  gap: 4px;
}

.security-action:hover {
  color: #2563eb;
}

/* 账户统计 */
.stats-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #3b82f6;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
}

/* 密码表单 */
.pwd-form {
  margin-top: 8px;
}

:deep(.pwd-form .el-form-item) {
  margin-bottom: 20px;
}

:deep(.pwd-form .el-input__wrapper) {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 12px;
  background: white;
  transition: all 0.2s;
}

:deep(.pwd-form .el-input__wrapper:hover) {
  border-color: #3b82f6;
}

:deep(.pwd-form .el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.error-tip {
  color: #ef4444;
  font-size: 12px;
  margin-top: 6px;
}

.pwd-tip {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
  margin-top: 16px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

:deep(.el-input__wrapper) {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 12px;
  background: white;
  transition: all 0.2s;
}

:deep(.el-input__wrapper:hover) {
  border-color: #3b82f6;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

:deep(.el-button--primary) {
  background: #3b82f6;
  border: none;
}

:deep(.el-button--primary:hover) {
  background: #2563eb;
}

/* 响应式 */
@media (max-width: 1024px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
  
  .profile-right {
    order: -1;
  }
}
</style>