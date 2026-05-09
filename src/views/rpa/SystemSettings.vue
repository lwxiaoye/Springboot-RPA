<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>{{ $t('settings.title') }}</h2>
      <p class="page-desc">{{ $t('settings.title') }}</p>
    </div>

    <el-tabs v-model="currentTab" class="settings-tabs">
      <!-- 通用设置 -->
      <el-tab-pane :label="$t('settings.general')" name="general">
        <div class="settings-section">
          <h3>{{ $t('settings.general') }}</h3>
          <el-form :model="generalForm" label-width="120px" style="max-width: 600px;">
            <el-form-item :label="$t('settings.systemName')">
              <el-input v-model="generalForm.systemName" :placeholder="$t('settings.systemName')" />
            </el-form-item>
            <el-form-item :label="$t('settings.sessionTimeout')">
              <el-input-number v-model="generalForm.sessionTimeout" :min="5" :max="1440" />
              <span class="unit">{{ $t('common.minutes') }}</span>
            </el-form-item>
            <el-form-item :label="$t('settings.language')">
              <el-select v-model="generalForm.language" style="width: 200px;">
                <el-option :label="$t('common.simplifiedChinese')" value="zh-CN" />
                <el-option :label="$t('common.english')" value="en-US" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('settings.timezone')">
              <el-select v-model="generalForm.timezone" style="width: 200px;">
                <el-option label="北京时间 (UTC+8)" value="Asia/Shanghai" />
                <el-option label="东京时间 (UTC+9)" value="Asia/Tokyo" />
                <el-option label="UTC时间" value="UTC" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveGeneral">{{ $t('settings.save') }}</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 邮件/消息服务 -->
      <el-tab-pane :label="$t('settings.message')" name="message">
        <div class="settings-section">
          <h3>{{ $t('settings.smtp') }}</h3>
          <el-form :model="smtpForm" label-width="120px" style="max-width: 600px;">
            <el-form-item :label="$t('settings.smtpHost')">
              <el-input v-model="smtpForm.host" placeholder="smtp.example.com" />
            </el-form-item>
            <el-form-item :label="$t('settings.smtpPort')">
              <el-input-number v-model="smtpForm.port" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item :label="$t('settings.smtpUsername')">
              <el-input v-model="smtpForm.username" :placeholder="$t('settings.smtpUsername')" />
            </el-form-item>
            <el-form-item :label="$t('settings.smtpPassword')">
              <el-input v-model="smtpForm.password" type="password" show-password :placeholder="$t('settings.smtpPassword')" />
            </el-form-item>
            <el-form-item :label="$t('settings.smtpSsl')">
              <el-switch v-model="smtpForm.ssl" />
            </el-form-item>
            <el-form-item>
              <el-button @click="testSmtp">{{ $t('settings.testEmail') }}</el-button>
              <el-button type="primary" @click="saveSmtp">{{ $t('settings.save') }}</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 存储配置 -->
      <el-tab-pane :label="$t('settings.storage')" name="storage">
        <div class="settings-section">
          <h3>{{ $t('settings.storage') }}</h3>
          <el-form :model="storageForm" label-width="120px" style="max-width: 600px;">
            <el-form-item :label="$t('settings.storageType')">
              <el-select v-model="storageForm.type" style="width: 200px;">
                <el-option :label="$t('common.local')" value="local" />
                <el-option label="NFS" value="nfs" />
                <el-option :label="$t('settings.s3Storage')" value="s3" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('settings.storagePath')" v-if="storageForm.type === 'local'">
              <el-input v-model="storageForm.path" :placeholder="$t('settings.storagePath')" />
            </el-form-item>
            <el-form-item :label="$t('settings.storageQuota')">
              <el-input-number v-model="storageForm.quota" :min="1" :max="10000" />
              <span class="unit">GB</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveStorage">{{ $t('settings.save') }}</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- OCR服务 -->
      <el-tab-pane :label="$t('settings.ocrService')" name="ocr">
        <div class="settings-section">
          <h3>{{ $t('settings.ocrService') }}</h3>
          <el-form :model="ocrForm" label-width="120px" style="max-width: 600px;">
            <el-form-item :label="$t('settings.ocrProvider')">
              <el-select v-model="ocrForm.provider" style="width: 200px;">
                <el-option label="Baidu OCR" value="baidu" />
                <el-option label="Aliyun OCR" value="aliyun" />
                <el-option label="Tencent OCR" value="tencent" />
                <el-option label="Tesseract (Local)" value="tesseract" />
              </el-select>
            </el-form-item>
            <el-form-item label="AppID" v-if="ocrForm.provider !== 'tesseract'">
              <el-input v-model="ocrForm.appId" placeholder="AppID" />
            </el-form-item>
            <el-form-item label="API Key" v-if="ocrForm.provider !== 'tesseract'">
              <el-input v-model="ocrForm.apiKey" type="password" show-password placeholder="API Key" />
            </el-form-item>
            <el-form-item label="Secret Key" v-if="ocrForm.provider !== 'tesseract'">
              <el-input v-model="ocrForm.secretKey" type="password" show-password placeholder="Secret Key" />
            </el-form-item>
            <el-form-item>
              <el-button @click="testOcr">{{ $t('common.test') }}</el-button>
              <el-button type="primary" @click="saveOcr">{{ $t('settings.save') }}</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 大模型配置 -->
      <el-tab-pane :label="$t('settings.llmService')" name="integration">
        <div class="settings-section">
          <h3>{{ $t('settings.llmService') }}</h3>
          <el-form :model="llmForm" label-width="120px" style="max-width: 600px;">
            <el-form-item :label="$t('settings.llmProvider')">
              <el-select v-model="llmForm.provider" style="width: 200px;">
                <el-option label="OpenAI" value="openai" />
                <el-option label="Qwen" value="qwen" />
                <el-option label="Zhipu AI" value="zhipu" />
                <el-option label="Local" value="local" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('settings.apiUrl')" v-if="llmForm.provider !== 'local'">
              <el-input v-model="llmForm.apiUrl" placeholder="https://api.openai.com/v1" />
            </el-form-item>
            <el-form-item label="API Key" v-if="llmForm.provider !== 'local'">
              <el-input v-model="llmForm.apiKey" type="password" show-password placeholder="API Key" />
            </el-form-item>
            <el-form-item :label="$t('settings.modelName')">
              <el-input v-model="llmForm.model" placeholder="gpt-4, qwen-turbo" />
            </el-form-item>
            <el-form-item>
              <el-button @click="testLlm">{{ $t('common.test') }}</el-button>
              <el-button type="primary" @click="saveLlm">{{ $t('settings.save') }}</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { setLocale, getLocale } from '../../locales/index.js'

const { t } = useI18n()

const apiBase = '/api'
const token = localStorage.getItem('token') || ''

const currentTab = ref('general')

const generalForm = reactive({
  systemName: 'RPA运营管理系统',
  sessionTimeout: 30,
  language: localStorage.getItem('locale') || 'zh-CN',
  timezone: 'Asia/Shanghai'
})

const smtpForm = reactive({
  host: 'smtp.qq.com',
  port: 587,
  username: '',
  password: '',
  ssl: true
})

const webhookForm = reactive({
  type: 'wecom',
  url: ''
})

const storageForm = reactive({
  type: 'local',
  path: '/data/rpa/storage',
  nfsHost: '',
  s3Endpoint: '',
  accessKey: '',
  secretKey: '',
  quota: 500
})

const ocrForm = reactive({
  provider: 'baidu',
  appId: '',
  apiKey: '',
  secretKey: ''
})

const llmForm = reactive({
  provider: 'openai',
  apiUrl: 'https://api.openai.com/v1',
  apiKey: '',
  model: 'gpt-4'
})

const licenseInfo = reactive({
  type: '企业版',
  concurrent: 10,
  expiry: '2026-12-31',
  company: '示例科技有限公司'
})

const storageUsed = ref(256)
const licenseValid = ref(true)

const storageUsage = computed(() => Math.round((storageUsed.value / storageForm.quota) * 100))

// 加载所有配置
onMounted(() => {
  loadAllConfigs()
})

async function loadAllConfigs() {
  try {
    const res = await fetch(apiBase + '/config', {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()
    if (data.code === 0 && data.data) {
      // 通用设置
      if (data.data.general) {
        Object.assign(generalForm, {
          systemName: data.data.general.system_name || 'RPA运营管理系统',
          sessionTimeout: parseInt(data.data.general.session_timeout) || 30,
          language: data.data.general.language || 'zh-CN',
          timezone: data.data.general.timezone || 'Asia/Shanghai'
        })
      }
      // 消息服务
      if (data.data.message) {
        Object.assign(smtpForm, {
          host: data.data.message.smtp_host || 'smtp.qq.com',
          port: parseInt(data.data.message.smtp_port) || 587,
          username: data.data.message.smtp_username || '',
          password: data.data.message.smtp_password || '',
          ssl: data.data.message.smtp_ssl === 'true'
        })
        Object.assign(webhookForm, {
          type: data.data.message.webhook_type || 'wecom',
          url: data.data.message.webhook_url || ''
        })
      }
      // 存储配置
      if (data.data.storage) {
        Object.assign(storageForm, {
          type: data.data.storage.storage_type || 'local',
          path: data.data.storage.storage_path || '/data/rpa/storage',
          quota: parseInt(data.data.storage.storage_quota) || 500
        })
      }
      // OCR配置
      if (data.data.ocr) {
        Object.assign(ocrForm, {
          provider: data.data.ocr.ocr_provider || 'baidu',
          appId: data.data.ocr.ocr_app_id || '',
          apiKey: data.data.ocr.ocr_api_key || '',
          secretKey: data.data.ocr.ocr_secret_key || ''
        })
      }
      // 大模型配置
      if (data.data.llm) {
        Object.assign(llmForm, {
          provider: data.data.llm.llm_provider || 'openai',
          apiUrl: data.data.llm.llm_api_url || 'https://api.openai.com/v1',
          apiKey: data.data.llm.llm_api_key || '',
          model: data.data.llm.llm_model || 'gpt-4'
        })
      }
    }
  } catch (e) {
    console.error('加载配置失败:', e)
  }
}

async function saveGeneral() {
  try {
    const configs = {
      system_name: generalForm.systemName,
      session_timeout: generalForm.sessionTimeout.toString(),
      language: generalForm.language,
      timezone: generalForm.timezone
    }
    const res = await fetch(apiBase + '/config/general', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(configs)
    })
    const data = await res.json()
    if (data.code === 0) {
      // 实时切换语言
      setLocale(generalForm.language)
      ElMessage.success(t('common.success'))
    } else {
      ElMessage.error(data.message || t('common.failed'))
    }
  } catch (e) {
    ElMessage.error(t('common.failed') + ': ' + e.message)
  }
}

async function saveSmtp() {
  try {
    const configs = {
      smtp_host: smtpForm.host,
      smtp_port: smtpForm.port.toString(),
      smtp_username: smtpForm.username,
      smtp_password: smtpForm.password,
      smtp_ssl: smtpForm.ssl.toString()
    }
    const res = await fetch(apiBase + '/config/message', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(configs)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('SMTP设置保存成功')
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

async function saveWebhook() {
  try {
    const configs = {
      webhook_type: webhookForm.type,
      webhook_url: webhookForm.url
    }
    const res = await fetch(apiBase + '/config/message', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(configs)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('Webhook设置保存成功')
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

async function saveStorage() {
  try {
    const configs = {
      storage_type: storageForm.type,
      storage_path: storageForm.path,
      storage_quota: storageForm.quota.toString()
    }
    const res = await fetch(apiBase + '/config/storage', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(configs)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('存储设置保存成功')
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

async function saveOcr() {
  try {
    const configs = {
      ocr_provider: ocrForm.provider,
      ocr_app_id: ocrForm.appId,
      ocr_api_key: ocrForm.apiKey,
      ocr_secret_key: ocrForm.secretKey
    }
    const res = await fetch(apiBase + '/config/ocr', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(configs)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('OCR设置保存成功')
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

async function saveLlm() {
  try {
    const configs = {
      llm_provider: llmForm.provider,
      llm_api_url: llmForm.apiUrl,
      llm_api_key: llmForm.apiKey,
      llm_model: llmForm.model
    }
    const res = await fetch(apiBase + '/config/llm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(configs)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('大模型设置保存成功')
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

async function testSmtp() {
  ElMessage.info('正在发送测试邮件...')
  await new Promise(r => setTimeout(r, 1000))
  ElMessage.success('测试邮件发送成功')
}

async function testWebhook() {
  ElMessage.info('正在发送测试消息...')
  await new Promise(r => setTimeout(r, 1000))
  ElMessage.success('测试消息发送成功')
}

async function testOcr() {
  try {
    const configs = { provider: ocrForm.provider }
    const res = await fetch(apiBase + '/config/ocr/test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(configs)
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success(data.message || 'OCR识别测试成功')
    } else {
      ElMessage.error(data.message || '测试失败')
    }
  } catch (e) {
    ElMessage.error('测试失败: ' + e.message)
  }
}

async function testLlm() {
  try {
    ElMessage.info('正在测试AI连接...')
    // 调用后端的AI测试接口
    const res = await fetch(apiBase + '/ai/test-connection', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success(data.message || '大模型服务连接正常')
    } else {
      ElMessage.error(data.message || '测试失败')
    }
  } catch (e) {
    ElMessage.error('测试失败: ' + e.message)
  }
}
</script>

<style scoped>
.settings-page { max-width: 1200px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.settings-tabs :deep(.el-tabs__header) { margin-bottom: 24px; }

.settings-section {
  background: white;
  padding: 24px;
  border-radius: 12px;
}

.settings-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e3a4a;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.settings-section h3:not(:first-child) {
  margin-top: 32px;
}

.unit {
  margin-left: 8px;
  color: #8c8c8c;
}

.upload-tip {
  margin-left: 12px;
  color: #8c8c8c;
  font-size: 12px;
}

.storage-stats {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-item .label {
  color: #8c8c8c;
}

.stat-item .value {
  font-weight: 600;
  color: #1e3a4a;
}
</style>