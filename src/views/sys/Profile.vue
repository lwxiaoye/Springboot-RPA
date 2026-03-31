<template>
  <div class="profile-wrapper">
    <div class="content-header">
      <div class="header-left">
        <h2>个人信息</h2>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>系统管理</el-breadcrumb-item>
          <el-breadcrumb-item>个人信息</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
    </div>

    <!-- 左右各占一半的布局 -->
    <div class="profile-split">
      <!-- 左侧卡片：头像 + 基本信息 -->
      <div class="profile-sidebar-half">
        <el-card class="info-card" shadow="never">
          <div class="avatar-section">
            <div class="avatar-large" v-if="!avatarPreview && !currentUser.avatar">
              {{ userInitialsLarge }}
            </div>
            <div class="avatar-large" v-else>
              <img 
                :src="getAvatarUrl(avatarPreview || currentUser.avatar)" 
                alt="头像" 
                @error="handleImageError"
              />
            </div>
            <el-button class="change-avatar-btn" text @click="triggerAvatarUpload">
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
          <div class="user-basic">
            <h3>{{ currentUser.realName || currentUser.username }}</h3>
            <el-tag size="small" class="role-badge">{{ currentUser.role || '系统管理员' }}</el-tag>
            <p class="user-email">{{ currentUser.email }}</p>
          </div>
          <el-divider />
          <div class="info-detail-item">
            <div class="info-label">创建时间</div>
            <div class="info-value">{{ formatDate(currentUser.createTime) }}</div>
          </div>
          <div class="info-detail-item">
            <div class="info-label">最后登录时间</div>
            <div class="info-value">{{ currentUser.lastLoginTime || '2026-03-23 14:32:00' }}</div>
          </div>
          <div class="info-detail-item">
            <div class="info-label">用户名</div>
            <div class="info-value readonly">{{ currentUser.username }}</div>
          </div>
        </el-card>
      </div>

      <!-- 右侧卡片：基本信息 / 修改密码标签页 -->
      <div class="profile-content-half">
        <el-card class="content-card" shadow="never">
          <el-tabs v-model="activeTab" class="custom-tabs">
            <el-tab-pane label="基本信息" name="info">
              <el-form :model="editForm" label-width="80px" class="info-form">
                <el-form-item label="姓名">
                  <div v-if="!editingField.name" class="editable-value" @click="startEdit('name')">
                    {{ editForm.realName || '未设置' }}
                    <el-icon class="edit-icon"><Edit /></el-icon>
                  </div>
                  <el-input 
                    v-else 
                    v-model="editForm.realName" 
                    size="default"
                    @blur="saveField('name')" 
                    @keyup.enter="saveField('name')"
                    autofocus
                  />
                </el-form-item>
                <el-form-item label="用户名">
                  <div class="readonly-value">{{ currentUser.username }}</div>
                  <div class="field-tip">用户名不可修改</div>
                </el-form-item>
                <el-form-item label="邮箱">
                  <div v-if="!editingField.email" class="editable-value" @click="startEdit('email')">
                    {{ editForm.email || '未设置' }}
                    <el-icon class="edit-icon"><Edit /></el-icon>
                  </div>
                  <el-input 
                    v-else 
                    v-model="editForm.email" 
                    @blur="saveField('email')" 
                    @keyup.enter="saveField('email')"
                  />
                </el-form-item>
                <el-form-item label="手机号">
                  <div v-if="!editingField.mobile" class="editable-value" @click="startEdit('mobile')">
                    {{ editForm.mobile || '未绑定' }}
                    <el-icon class="edit-icon"><Edit /></el-icon>
                  </div>
                  <el-input 
                    v-else 
                    v-model="editForm.mobile" 
                    @blur="saveField('mobile')" 
                    @keyup.enter="saveField('mobile')"
                  />
                </el-form-item>
                <el-form-item label="角色">
                  <div class="readonly-value">{{ currentUser.role || '系统管理员' }}</div>
                </el-form-item>
                <el-form-item label="最后登录">
                  <div class="readonly-value">{{ currentUser.lastLoginTime || '2026-03-23 14:32:00' }}</div>
                </el-form-item>
                <el-form-item>
                  <el-button @click="resetInfo">重置</el-button>
                  <el-button type="primary" @click="saveAllInfo" :loading="saveLoading">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form :model="pwdModel" label-width="80px" class="pwd-form">
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
                    placeholder="6-20位字母/数字"
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
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'

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
  if (val && (val.length < 6 || val.length > 20)) {
    pwdNewError.value = '密码长度需6-20位'
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
    const loginCheck = await fetch(`${API_BASE}/user/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ 
        username: currentUser.value.username, 
        password: pwdModel.oldPassword 
      })
    })
    const loginRes = await loginCheck.json()
    if (loginRes.code !== 0) {
      ElMessage.error('原密码错误')
      pwdLoading.value = false
      return
    }
    
    const resetRes = await fetch(`${API_BASE}/user/reset-password/${currentUser.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newPassword: pwdModel.newPassword })
    })
    const result = await resetRes.json()
    if (result.code === 0) {
      ElMessage.success('密码修改成功，请重新登录')
      resetPasswordForm()
      setTimeout(() => {
        ElMessageBox.confirm('密码已修改，建议重新登录', '提示', { confirmButtonText: '去登录' }).then(() => {
          localStorage.removeItem('userInfo')
          window.location.href = '/login'
        })
      }, 800)
    } else {
      ElMessage.error(result.message || '修改失败')
    }
  } catch (err) {
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

.profile-split {
  display: flex;
  gap: 24px;
  align-items: stretch;
}

.profile-sidebar-half,
.profile-content-half {
  flex: 1;
  min-width: 0;
}

.info-card,
.content-card {
  height: 100%;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 20px;
  margin-bottom: 20px;
}

.avatar-large {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(145deg, #2a5f7a, #1e3a4a);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 600;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  margin-bottom: 16px;
  overflow: hidden;
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.change-avatar-btn {
  font-size: 14px;
  color: #1677ff;
}

.user-basic {
  text-align: center;
  margin-bottom: 20px;
}

.user-basic h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1e3a4a;
  margin-bottom: 8px;
}

.role-badge {
  background: #e6f2ff;
  color: #1677ff;
  border: none;
}

.user-email {
  font-size: 13px;
  color: #8aa5b8;
  margin-top: 8px;
}

.info-detail-item {
  margin-bottom: 16px;
}

.info-label {
  font-size: 13px;
  color: #8c9aa8;
  margin-bottom: 6px;
}

.info-value {
  font-size: 14px;
  color: #1e3a4a;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 8px;
  word-break: break-all;
}

.info-value.readonly {
  background: #fafbfc;
}

.editable-value {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editable-value:hover {
  background: #eef2f6;
}

.edit-icon {
  font-size: 14px;
  color: #8aa5b8;
}

.readonly-value {
  background: #fafbfc;
  padding: 8px 12px;
  border-radius: 8px;
  color: #4a6f86;
}

.field-tip {
  font-size: 12px;
  color: #8aa5b8;
  margin-top: 4px;
}

.error-tip {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

.pwd-tip {
  font-size: 12px;
  color: #8aa5b8;
  text-align: center;
  margin-top: 16px;
}

.custom-tabs {
  height: 100%;
}

.info-form,
.pwd-form {
  padding: 8px 0;
}

@media (max-width: 900px) {
  .profile-split {
    flex-direction: column;
  }
}
</style>