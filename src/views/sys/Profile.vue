<template>
  <div class="profile-wrapper">
    <!-- 顶部欢迎区域 -->
    <div class="profile-header">
      <div class="header-content">
        <h1 class="page-title">{{ t('profile.title') }}</h1>
        <p class="page-subtitle">{{ t('profile.subtitle') }}</p>
      </div>
    </div>

    <div class="profile-layout">
      <!-- 左侧主内容区 -->
      <div class="profile-main">
        <!-- 基本信息卡片 -->
        <el-card class="info-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon"><User /></el-icon>
                <span class="header-title">{{ t('profile.basicInfo') }}</span>
              </div>
              <div class="header-actions">
                <el-button
                  v-if="!isEditing"
                  type="primary"
                  size="small"
                  @click="startEdit"
                >
                  <el-icon><Edit /></el-icon>
                  {{ t('profile.editProfile') }}
                </el-button>
                <div v-else class="action-buttons">
                  <el-button size="small" @click="cancelEdit">{{ t('common.cancel') }}</el-button>
                  <el-button size="small" type="primary" @click="saveAllInfo" :loading="saveLoading">
                    {{ t('profile.save') }}
                  </el-button>
                </div>
              </div>
            </div>
          </template>

          <div class="profile-info-section">
            <!-- 头像区域 -->
            <div class="avatar-section">
              <div class="avatar-wrapper">
                <div class="avatar-circle" v-if="!avatarPreview && !currentUser.avatar">
                  {{ userInitialsLarge }}
                </div>
                <div class="avatar-circle" v-else>
                  <img
                    :src="getAvatarUrl(avatarPreview || currentUser.avatar)"
                    :alt="t('profile.avatar')"
                    @error="handleImageError"
                  />
                </div>
                <div class="avatar-overlay" @click="triggerAvatarUpload">
                  <el-icon><Camera /></el-icon>
                  <span>{{ t('profile.changeAvatar') }}</span>
                </div>
              </div>
              <input
                type="file"
                ref="avatarInput"
                style="display: none"
                accept="image/jpeg,image/png"
                @change="handleAvatarChange"
              />
            </div>

            <!-- 信息表单 -->
            <div class="info-form-section">
              <el-form :model="editForm" label-width="100px" class="info-form">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item :label="t('user.username')">
                      <div class="info-value readonly">{{ currentUser.username }}</div>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item :label="t('user.role')">
                      <div class="info-value readonly">
                        <el-tag size="small" type="success">{{ currentUser.role === 1 ? t('profile.admin') : currentUser.role === 2 ? t('profile.normalUser') : t('user.admin') }}</el-tag>
                      </div>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item :label="t('profile.realName')">
                      <div v-if="!isEditing" class="info-value">{{ editForm.realName || t('profile.notSet') }}</div>
                      <el-input v-else v-model="editForm.realName" :placeholder="t('profile.inputRealName')" clearable />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item :label="t('profile.email')">
                      <div v-if="!isEditing" class="info-value">{{ editForm.email || t('profile.notSet') }}</div>
                      <el-input v-else v-model="editForm.email" :placeholder="t('profile.inputEmail')" clearable />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item :label="t('profile.mobile')">
                      <div v-if="!isEditing" class="info-value">{{ editForm.mobile || t('profile.notBound') }}</div>
                      <el-input v-else v-model="editForm.mobile" :placeholder="t('profile.inputMobile')" clearable />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item :label="t('common.createTime')">
                      <div class="info-value readonly">{{ formatDate(currentUser.createTime) }}</div>
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </div>
          </div>
        </el-card>

        <!-- 账户统计卡片 -->
        <el-card class="stats-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon"><DataAnalysis /></el-icon>
                <span class="header-title">{{ t('profile.accountStats') }}</span>
              </div>
            </div>
          </template>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ accountDays }}</div>
                <div class="stat-label">{{ t('profile.accountDays') }}</div>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ t('profile.today') }}</div>
                <div class="stat-label">{{ t('profile.lastLogin') }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧边栏 -->
      <div class="profile-sidebar">
        <!-- 账户安全卡片 -->
        <el-card class="security-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon"><Lock /></el-icon>
                <span class="header-title">{{ t('profile.security') }}</span>
              </div>
            </div>
          </template>
          <div class="security-list">
            <div class="security-item" @click="activeTab = 'password'">
              <div class="security-info">
                <div class="security-title">{{ t('profile.loginPassword') }}</div>
                <div class="security-desc">{{ t('profile.passwordTip') }}</div>
              </div>
              <el-button text type="primary">
                {{ t('profile.modify') }}
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 修改密码卡片 -->
        <el-card class="password-card" shadow="hover" v-if="activeTab === 'password'">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon"><Key /></el-icon>
                <span class="header-title">{{ t('profile.changePassword') }}</span>
              </div>
              <el-button text @click="activeTab = ''">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </template>
          <el-form :model="pwdModel" label-position="top" class="pwd-form">
            <el-form-item :label="t('profile.oldPassword')">
              <el-input
                v-model="pwdModel.oldPassword"
                type="password"
                :placeholder="t('profile.inputOldPassword')"
                show-password
                @paste.prevent="preventPaste"
                clearable
              />
            </el-form-item>
            <el-form-item :label="t('profile.newPassword')">
              <el-input
                v-model="pwdModel.newPassword"
                type="password"
                :placeholder="t('profile.newPasswordTip')"
                show-password
                @paste.prevent="preventPaste"
                clearable
              />
              <div v-if="pwdNewError" class="error-tip">{{ pwdNewError }}</div>
            </el-form-item>
            <el-form-item :label="t('profile.confirmPassword')">
              <el-input
                v-model="pwdModel.confirmPassword"
                type="password"
                :placeholder="t('profile.inputConfirmPassword')"
                show-password
                @paste.prevent="preventPaste"
                clearable
              />
              <div v-if="pwdConfirmError" class="error-tip">{{ pwdConfirmError }}</div>
            </el-form-item>
            <div class="pwd-actions">
              <el-button @click="resetPasswordForm" plain>{{ t('profile.reset') }}</el-button>
              <el-button type="primary" @click="changePassword" :loading="pwdLoading">
                {{ t('profile.updatePassword') }}
              </el-button>
            </div>
            <el-alert
              :title="t('profile.passwordChangeTip')"
              type="info"
              :closable="false"
              show-icon
            />
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, User, Lock, DataAnalysis, ArrowRight, Edit, Camera, Calendar, Clock, Key, Close } from '@element-plus/icons-vue'

import { apiGet, apiPost, apiPut, apiDelete, apiUpload } from '../../utils/api.js'

const { t } = useI18n()


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

// 计算账户天数
const accountDays = computed(() => {
  if (!currentUser.value.createTime) return 0
  const createDate = new Date(currentUser.value.createTime)
  const now = new Date()
  const diffTime = Math.abs(now - createDate)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays
})

// 密码校验
watch(() => pwdModel.newPassword, (val) => {
  if (val) {
    // 长度验证
    if (val.length < 8 || val.length > 24) {
      pwdNewError.value = t('profile.pwdLengthTip')
    }
    // 复杂度验证：必须包含字母、数字和特殊字符
    else if (!/[a-zA-Z]/.test(val) || !/[0-9]/.test(val) || !/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(val)) {
      pwdNewError.value = t('profile.pwdComplexTip')
    } else {
      pwdNewError.value = ''
    }
  } else {
    pwdNewError.value = ''
  }

  if (pwdModel.confirmPassword && pwdModel.confirmPassword !== val) {
    pwdConfirmError.value = t('profile.pwdMismatch')
  } else {
    pwdConfirmError.value = ''
  }
})

watch(() => pwdModel.confirmPassword, (val) => {
  if (val && val !== pwdModel.newPassword) {
    pwdConfirmError.value = t('profile.pwdMismatch')
  } else {
    pwdConfirmError.value = ''
  }
})

const preventPaste = (e) => {
  e.preventDefault()
  ElMessage.warning(t('profile.forbidPaste'))
}

const initEditForm = () => {
  editForm.realName = currentUser.value.realName || ''
  editForm.email = currentUser.value.email || ''
  editForm.mobile = currentUser.value.mobile || ''
}

const startEdit = () => {
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  initEditForm()
  ElMessage.info(t('profile.editCanceled'))
}



const resetInfo = () => {
  initEditForm()
  ElMessage.info(t('profile.resetToCurrent'))
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
      ElMessage.success(t('profile.basicInfoSuccess'))
      isEditing.value = false
    } else {
      ElMessage.error(result.message || t('profile.saveFailed'))
    }
  } catch (err) {
    ElMessage.warning(t('profile.noResponse'))
    currentUser.value.realName = editForm.realName
    currentUser.value.email = editForm.email
    currentUser.value.mobile = editForm.mobile
    const storedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
    storedUser.realName = editForm.realName
    storedUser.email = editForm.email
    storedUser.mobile = editForm.mobile
    localStorage.setItem('userInfo', JSON.stringify(storedUser))
    isEditing.value = false
    ElMessage.success(t('profile.localSaveSuccess'))
  } finally {
    saveLoading.value = false
  }
}

const changePassword = async () => {
  if (!pwdModel.oldPassword) {
    ElMessage.warning(t('profile.inputOldPassword'))
    return
  }
  if (!pwdModel.newPassword || pwdNewError.value) {
    ElMessage.warning(t('profile.inputNewPassword'))
    return
  }
  if (pwdModel.newPassword !== pwdModel.confirmPassword) {
    ElMessage.warning(t('profile.pwdMismatch'))
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
      ElMessage.success(t('profile.pwdSuccess'))
      resetPasswordForm()
      setTimeout(() => {
        ElMessageBox.confirm(t('profile.confirmReLogin'), t('common.tips'), { confirmButtonText: t('menu.logout') }).then(() => {
          localStorage.removeItem('userInfo')
          localStorage.removeItem('token')
          window.location.href = '/login'
        })
      }, 800)
    } else {
      ElMessage.error(result.message || t('profile.pwdFailed'))
    }
  } catch (err) {
    console.error(t('profile.pwdFailed') + ':', err)
    ElMessage.error(t('profile.pwdFailed'))
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
      const response = await fetch(`/api/user/avatar/${currentUser.value.id}`, { 
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
        
        ElMessage.success(t('profile.avatarUpdateSuccess'))
      } else {
        ElMessage.error(t('profile.uploadFailed'))
      }
    } catch (err) {
      currentUser.value.avatar = avatarPreview.value
      ElMessage.success(t('profile.avatarChanged'))
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
  ElMessage.error(t('profile.avatarLoadFailed'))
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
  const fullUrl = `/api${path}`
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
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 56px);
}

/* 顶部欢迎区域 */
.profile-header {
  margin-bottom: 24px;
  padding: 24px 32px;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2);
}

.header-content {
  color: white;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  letter-spacing: 0.5px;
}

.page-subtitle {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

/* 主布局 */
.profile-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
}

.profile-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 卡片通用样式 */
.info-card,
.stats-card,
.security-card,
.password-card {
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.info-card:hover,
.stats-card:hover,
.security-card:hover,
.password-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

:deep(.el-card__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafbfc;
}

:deep(.el-card__body) {
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
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
}

.header-icon {
  font-size: 20px;
  color: #409eff;
}

/* 头像区域 */
.profile-info-section {
  display: flex;
  gap: 32px;
}

.avatar-section {
  flex-shrink: 0;
}

.avatar-wrapper {
  position: relative;
  width: 140px;
  height: 140px;
  cursor: pointer;
}

.avatar-circle {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  font-weight: 600;
  color: white;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.3);
  overflow: hidden;
  border: 4px solid white;
  transition: all 0.3s ease;
}

.avatar-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: all 0.3s ease;
  color: white;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-wrapper:hover .avatar-circle {
  transform: scale(1.05);
}

.avatar-overlay .el-icon {
  font-size: 32px;
}

.avatar-overlay span {
  font-size: 13px;
  font-weight: 500;
}

/* 信息表单 */
.info-form-section {
  flex: 1;
}

.info-form {
  margin-top: 8px;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-form-item__label) {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #303133;
  padding: 8px 0;
  line-height: 1.6;
  font-weight: 500;
}

.info-value.readonly {
  color: #909399;
}

:deep(.el-input) {
  width: 100%;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 10px 14px;
  transition: all 0.2s;
}

:deep(.el-input__wrapper:hover) {
  border-color: #409eff;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

:deep(.el-tag) {
  border-radius: 6px;
  font-weight: 500;
}

/* 账户统计 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f4ff 100%);
  border-radius: 10px;
  border: 1px solid #d9ecff;
  transition: all 0.2s;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.stat-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

/* 账户安全 */
.security-list {
  padding: 4px 0;
}

.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.2s;
}

.security-item:hover {
  background: #f3f4f6;
  border-color: #409eff;
  transform: translateX(4px);
}

.security-info {
  flex: 1;
}

.security-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 6px;
}

.security-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.security-item :deep(.el-button) {
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
  background: white !important;
  color: #409eff !important;
  border: 1px solid #409eff !important;
}

.security-item :deep(.el-button:hover) {
  background: #ecf5ff !important;
  color: #3a8ee6 !important;
  border-color: #3a8ee6 !important;
}

/* 密码表单 */
.pwd-form {
  margin-top: 8px;
}

:deep(.pwd-form .el-form-item) {
  margin-bottom: 20px;
}

:deep(.pwd-form .el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

:deep(.pwd-form .el-input__wrapper) {
  border-radius: 8px;
  padding: 10px 14px;
  transition: all 0.2s;
}

:deep(.pwd-form .el-input__wrapper:hover) {
  border-color: #409eff;
}

:deep(.pwd-form .el-input__wrapper.is-focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.error-tip {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.pwd-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

:deep(.el-alert) {
  margin-top: 16px;
  border-radius: 8px;
}

/* 按钮样式 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
  border-radius: 8px;
  font-weight: 500;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #3a8ee6 0%, #3375b9 100%);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
}

/* 响应式 */
@media (max-width: 1200px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
  
  .profile-sidebar {
    order: -1;
  }
}

@media (max-width: 768px) {
  .profile-wrapper {
    padding: 16px;
  }
  
  .profile-header {
    padding: 20px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
  .profile-info-section {
    flex-direction: column;
    align-items: center;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>