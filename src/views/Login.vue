<template>
  <div class="login-page">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <span class="nav-platform">RPA运营管理系统</span>
    </div>

    <!-- 英文标题区域 -->
    <div class="english-title-section">
      <div class="english-title-wrapper">
        <div class="english-title">Robotic Process Automation Operation Management System</div>
      </div>
    </div>

    <div class="login-container">
      <!-- 左侧区域 - 外层容器 -->
      <div class="left-hero">
        <div class="left-content">
          <div class="left-overlay-text">
            <span>智能运营 · 高效管理</span>
            <p>RPA自动化流程解决方案</p>
          </div>
        </div>
      </div>

      <!-- 右侧卡片区域 -->
      <div class="right-card-area">
        <div class="login-card">
          <div class="card-welcome">欢迎登录</div>

          <!-- 密码登录表单 -->
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-position="top"
            @keyup.enter="handlePasswordLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="passwordForm.username"
                placeholder="请输入用户名"
                size="large"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                placeholder="密码"
                size="large"
                show-password
                autocomplete="new-password"
                @paste.prevent="preventCopyPaste"
                @copy.prevent="preventCopyPaste"
              />
            </el-form-item>

            <div class="password-tip">
              <span>请输入用户名和密码</span>
            </div>

            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <span class="forgot-text" @click="openForget">忘记密码？</span>
            </div>

            <el-button
              type="primary"
              :loading="loading"
              class="login-button"
              @click="handlePasswordLogin"
            >
              登录
            </el-button>
          </el-form>

          <div class="default-account-tip">
            欢迎登录
          </div>
        </div>
      </div>
    </div>

    <!-- 忘记密码弹窗 -->
    <el-dialog
      v-model="forgetVisible"
      title="重置密码"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form :model="resetForm" label-width="80px" style="margin-top:20px">
        <el-form-item label="手机号">
          <el-input v-model="resetForm.phone" placeholder="请输入注册手机号" />
        </el-form-item>
        <el-form-item label="验证码">
          <div style="display:flex;gap:10px">
            <el-input
              v-model="resetForm.code"
              placeholder="6 位验证码"
              style="flex:1"
              maxlength="6"
            />
            <el-button 
              size="default" 
              @click="getResetCode"
              :disabled="!resetForm.phone || countdown > 0"
            >
              {{ countdown > 0 ? `${countdown}秒后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            placeholder="输入新密码（6-20 位）"
            show-password
            maxlength="20"
          />
        </el-form-item>
      </el-form>
      <div style="text-align:right;margin-top:20px">
        <el-button @click="forgetVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="resetPassword">
          确认重置
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

// 后端接口地址
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 响应式数据
const loading = ref(false)
const rememberMe = ref(false)
const loginFailCount = ref(0)
const isLocked = ref(false)

// 密码登录表单
const passwordFormRef = ref(null)
const passwordForm = reactive({
  username: '',
  password: ''
})

// 忘记密码弹窗
const forgetVisible = ref(false)
const resetForm = reactive({
  phone: '',
  code: '',
  newPassword: ''
})
const resetCode = ref('')
const countdown = ref(0) // 倒计时

// 验证规则
const passwordRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 禁止密码复制粘贴
const preventCopyPaste = (e) => {
  e.preventDefault()
  ElMessage.warning('禁止复制/粘贴密码！')
}

// 密码登录 - 对接后端真实接口
const handlePasswordLogin = async () => {
  if (isLocked.value) {
    ElMessage.error('账号已临时锁定，请稍后再试！')
    return
  }

  if (!passwordFormRef.value) return

  passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true

    try {
      const response = await fetch(`${API_BASE_URL}/user/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          username: passwordForm.username,
          password: passwordForm.password
        })
      })

      const result = await response.json()

      // 后端返回格式: code: 0表示成功, -1表示失败
      if (result.code === 0) {
        // 登录成功
        ElMessage.success(result.message || '登录成功！')
      
        // 保存用户信息到本地存储
        const userInfo = {
          id: result.data?.id,
          username: result.data?.username,
          realName: result.data?.realName,
          role: result.data?.role,
          status: result.data?.status,  // 保存用户状态
          avatar: result.data?.avatar,  // 保存头像信息（相对路径）
          loginTime: new Date().toISOString()
        }
        
        // 如果后端返回的头像路径包含 /api/前缀，去掉它
        if (userInfo.avatar && userInfo.avatar.startsWith('/api/')) {
          userInfo.avatar = userInfo.avatar.replace('/api/', '/')
        }
        
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
      
        // 保存 token
        if (result.data?.token) {
          localStorage.setItem('token', result.data.token)
        }
      
        if (rememberMe.value) {
          localStorage.setItem('rememberedUser', passwordForm.username)
        } else {
          localStorage.removeItem('rememberedUser')
        }
      
        // 重置失败计数
        loginFailCount.value = 0
      
        // 跳转到仪表盘页面
        router.push('/dashboard')
      } else {
        // 登录失败
        loginFailCount.value++
        const remainingAttempts = 5 - loginFailCount.value
        if (loginFailCount.value >= 5) {
          isLocked.value = true
          ElMessage.error('连续失败5次，账号已锁定1分钟！')
          setTimeout(() => {
            isLocked.value = false
            loginFailCount.value = 0
          }, 60000)
        } else {
          ElMessage.error(result.message || `用户名或密码错误，还剩 ${remainingAttempts} 次机会`)
        }
      }
    } catch (error) {
      console.error('登录请求失败:', error)
      ElMessage.error('网络错误，请检查后端服务是否启动')
    } finally {
      loading.value = false
    }
  })
}

// 忘记密码 - 重置密码
const openForget = () => {
  forgetVisible.value = true
  resetForm.phone = ''
  resetForm.code = ''
  resetForm.newPassword = ''
  countdown.value = 0
}

// 获取验证码
const getResetCode = async () => {
  if (!resetForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }

  // 验证手机号格式
  const phoneRegex = /^[0-9]{11}$/
  if (!phoneRegex.test(resetForm.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }

  try {
    // 调用后端发送验证码接口
    const response = await fetch(`${API_BASE_URL}/user/send-reset-code`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        account: resetForm.phone
      })
    })

    const result = await response.json()

    if (result.code === 0) {
      ElMessage.success(result.message || '验证码已发送')
      // 如果后端返回验证码，可以存储起来
      if (result.data?.code) {
        resetCode.value = result.data.code
      }
      // 开始倒计时
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } else {
      ElMessage.error(result.message || '发送验证码失败')
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 重置密码
const resetPassword = async () => {
  if (!resetForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!resetForm.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (resetForm.code !== resetCode.value) {
    ElMessage.error('验证码错误')
    return
  }
  if (!resetForm.newPassword || resetForm.newPassword.length < 6 || resetForm.newPassword.length > 20) {
    ElMessage.warning('新密码长度必须在 6-20 位之间')
    return
  }

  loading.value = true
  try {
    // 调用后端重置密码接口
    const response = await fetch(`${API_BASE_URL}/user/reset-password-by-code`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        account: resetForm.phone,
        code: resetForm.code,
        newPassword: resetForm.newPassword
      })
    })

    const result = await response.json()

    if (result.code === 0) {
      ElMessage.success('密码重置成功！')
      forgetVisible.value = false
    } else {
      ElMessage.error(result.message || '重置失败')
    }
  } catch (error) {
    console.error('重置密码失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 加载记住的用户名
const loadRememberedUser = () => {
  const remembered = localStorage.getItem('rememberedUser')
  if (remembered) {
    passwordForm.username = remembered
    rememberMe.value = true
  }
}

// 检查是否已登录
const checkLoginStatus = () => {
  const token = localStorage.getItem('token')
  const userInfo = localStorage.getItem('userInfo')
  if (token && userInfo) {
    // 已登录，跳转到仪表盘页面
    router.push('/dashboard')
  }
}

onMounted(() => {
  loadRememberedUser()
  checkLoginStatus()
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-page {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  min-height: 100vh;
  overflow-x: hidden;
  background: #f0f4fa;
  padding: 5px;
}

/* 顶部导航栏 */
.top-nav {
  top: 0;
  left: 0;
  width: 100%;
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f4fa;
  z-index: 1000;
  backdrop-filter: blur(10px);
  position: relative;
}

.nav-platform {
  font-size: 40px;
  font-weight: 600;
  color: #1e3a4a;
  letter-spacing: 2.5px;
  background: linear-gradient(135deg, #1e3a4a 0%, #1f5270 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  position: relative;
  padding: 0 32px;
}

.nav-platform::before,
.nav-platform::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #8aa8c8, transparent);
}

.nav-platform::before {
  left: -20px;
  transform: translateY(-50%);
}

.nav-platform::after {
  right: -20px;
  transform: translateY(-50%);
}

/* 英文标题区域 */
.english-title-section {
  width: 100%;
  position: relative;
  padding: 5px 0 40px 0;
  margin-top: -10px;
  overflow: hidden;
}

.english-title-section::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -5%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(106, 154, 186, 0.05) 0%, rgba(106, 154, 186, 0) 70%);
  border-radius: 50%;
  pointer-events: none;
}

.english-title-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 40px;
  text-align: right;
  position: relative;
  z-index: 1;
}

.english-title {
  font-size: 22px;
  font-weight: 600;
  font-family: 'Orbitron', 'Poppins', 'Playfair Display', monospace;
  letter-spacing: 3px;
  position: relative;
  display: inline-block;
  padding: 12px 28px 12px 20px;
  background: linear-gradient(135deg, #1e3a4a 0%, #2a5f7a 50%, #1e3a4a 100%);
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 2px 2px 4px rgba(30, 58, 74, 0.2);
  transition: all 0.4s ease;
  cursor: default;
}

.english-title:hover {
  background-position: right center;
  letter-spacing: 4px;
}

.english-title::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #6a9aba, #1e3a4a, #6a9aba, transparent);
  transform: scaleX(0.8);
  transition: transform 0.3s ease;
}

.english-title:hover::before {
  transform: scaleX(1);
}

.english-title::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 30px;
  background: linear-gradient(180deg, #6a9aba, #1e3a4a, #6a9aba);
  border-radius: 2px;
  opacity: 0.6;
  transition: height 0.3s ease, opacity 0.3s ease;
}

.english-title:hover::after {
  height: 40px;
  opacity: 1;
}

/* 主容器左右结构 */
.login-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  background: #f0f4fa;
  position: relative;
  top: 0;
}

/* 左侧区域 */
.left-hero {
  flex: 3;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  position: relative;
  margin-left: 30px;
  margin-right: 30px;
}

.left-content {
  width: 100%;
  height: 80vh;
  min-height: 350px;
  max-height: 600px;
  background: url('/src/assets/background.png') no-repeat center center;
  background-size: cover;
  position: relative;
  clip-path: polygon(0% 0%, 100% 0%, 92% 100%, 0% 100%);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
}

.left-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(30, 58, 74, 0.2) 0%, rgba(30, 58, 74, 0.05) 100%);
  pointer-events: none;
  z-index: 1;
}

.left-content::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0) 70%);
  pointer-events: none;
  z-index: 1;
}

.left-overlay-text {
  position: absolute;
  bottom: 40px;
  left: 30px;
  color: white;
  z-index: 2;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  font-size: 24px;
  font-weight: 500;
  background: linear-gradient(90deg, rgba(30, 58, 74, 0.7), rgba(30, 58, 74, 0));
  padding: 12px 24px;
  border-radius: 8px;
  backdrop-filter: blur(5px);
}

.left-overlay-text p {
  font-size: 14px;
  margin-top: 8px;
  opacity: 0.9;
}

/* 右侧区域 */
.right-card-area {
  flex: 2;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.login-card {
  width: 100%;
  max-width: 440px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 32px;
  box-shadow: 0 25px 50px -8px rgba(0, 40, 60, 0.25);
  padding: 32px 32px 40px 32px;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(200, 220, 240, 0.5);
}

.card-welcome {
  text-align: center;
  margin-bottom: 24px;
  font-size: 28px;
  font-weight: 600;
  color: #1e3a4a;
  letter-spacing: 1px;
  position: relative;
}

.card-welcome::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 50%;
  transform: translateX(-50%);
  width: 50px;
  height: 3px;
  background: linear-gradient(90deg, #1e3a4a, #6a9aba);
  border-radius: 3px;
}

.el-form-item {
  margin-bottom: 24px;
}

.el-form-item__label {
  font-weight: 500;
  color: #2a4a5a;
  font-size: 14px;
  line-height: 1.8;
}

:deep(.el-input__wrapper) {
  background-color: #f8fcff;
  border-radius: 12px;
  padding: 4px 16px;
  border: 1px solid #c9d8e8;
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) {
  border-color: #8aa8c8;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #1e3a4a;
  box-shadow: 0 0 0 3px rgba(30, 58, 74, 0.1);
}

:deep(.el-input__inner) {
  height: 46px;
  color: #1e3a4a;
  font-size: 15px;
}

.password-tip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: -12px;
  margin-bottom: 16px;
  font-size: 12px;
  color: #6a7a8a;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.forgot-text {
  color: #1e3a4a;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: opacity 0.2s;
}

.forgot-text:hover {
  opacity: 0.7;
  text-decoration: underline;
}

.login-button {
  width: 100%;
  height: 50px;
  font-size: 18px;
  font-weight: 500;
  border-radius: 25px;
  background: linear-gradient(135deg, #1e3a4a 0%, #2a4a6a 100%);
  border: none;
  color: white;
  letter-spacing: 1px;
  transition: all 0.25s;
  cursor: pointer;
  border: 1px solid #1e3a4a;
}

.login-button:hover {
  background: linear-gradient(135deg, #14303e 0%, #1e405e 100%);
  transform: translateY(-2px);
  box-shadow: 0 12px 20px -10px #1e3a4a80;
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.default-account-tip {
  text-align: center;
  font-size: 14px;
  color: #4a6a7a;
  background: #f5faff;
  padding: 12px 16px;
  border-radius: 30px;
  border: 1px dashed #b8cde0;
  margin-top: 24px;
}

/* 动画效果 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.left-content {
  animation: fadeInUp 0.6s ease-out;
}

.english-title-section {
  animation: fadeInUp 0.5s ease-out 0.2s both;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
  }
  .left-hero {
    flex: 1;
    min-height: auto;
    padding: 15px;
  }
  .left-content {
    height: 40vh;
    min-height: 280px;
    clip-path: polygon(0% 0%, 100% 0%, 100% 90%, 0% 100%);
  }
  .right-card-area {
    flex: 3;
  }
  .top-nav {
    height: 70px;
  }
  .nav-platform {
    font-size: 28px;
  }
  .card-welcome {
    font-size: 24px;
    margin-bottom: 20px;
  }
  .left-overlay-text {
    font-size: 18px;
    bottom: 20px;
    left: 20px;
    padding: 8px 16px;
  }
  .left-overlay-text p {
    font-size: 12px;
  }
  .english-title-wrapper {
    padding: 0 20px;
  }
  .english-title {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 24px 20px 30px 20px;
  }
  .card-welcome {
    font-size: 22px;
    margin-bottom: 16px;
  }
  .nav-platform {
    font-size: 22px;
  }
  .nav-platform::before,
  .nav-platform::after {
    width: 25px;
  }
  .left-content {
    height: 35vh;
  }
  .left-overlay-text {
    font-size: 16px;
    bottom: 15px;
    left: 15px;
  }
  .english-title {
    font-size: 12px;
  }
}
</style>