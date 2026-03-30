<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>系统设置</h2>
      <p class="page-desc">配置系统全局参数和集成服务</p>
    </div>

    <el-tabs v-model="currentTab" class="settings-tabs">
      <!-- 通用设置 -->
      <el-tab-pane label="通用设置" name="general">
        <div class="settings-section">
          <h3>基本信息</h3>
          <el-form :model="generalForm" label-width="120px" style="max-width: 600px;">
            <el-form-item label="系统名称">
              <el-input v-model="generalForm.systemName" placeholder="请输入系统名称" />
            </el-form-item>
            <el-form-item label="系统Logo">
              <el-upload action="#" :auto-upload="false" :show-file-list="false">
                <el-button>选择图片</el-button>
                <span class="upload-tip">建议尺寸: 200x60px</span>
              </el-upload>
            </el-form-item>
            <el-form-item label="会话超时">
              <el-input-number v-model="generalForm.sessionTimeout" :min="5" :max="1440" />
              <span class="unit">分钟</span>
            </el-form-item>
            <el-form-item label="默认语言">
              <el-select v-model="generalForm.language" style="width: 200px;">
                <el-option label="简体中文" value="zh-CN" />
                <el-option label="English" value="en-US" />
              </el-select>
            </el-form-item>
            <el-form-item label="时区">
              <el-select v-model="generalForm.timezone" style="width: 200px;">
                <el-option label="北京时间 (UTC+8)" value="Asia/Shanghai" />
                <el-option label="东京时间 (UTC+9)" value="Asia/Tokyo" />
                <el-option label="UTC时间" value="UTC" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveGeneral">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 邮件/消息服务 -->
      <el-tab-pane label="消息服务" name="message">
        <div class="settings-section">
          <h3>SMTP邮件设置</h3>
          <el-form :model="smtpForm" label-width="120px" style="max-width: 600px;">
            <el-form-item label="SMTP服务器">
              <el-input v-model="smtpForm.host" placeholder="如: smtp.qq.com" />
            </el-form-item>
            <el-form-item label="端口">
              <el-input-number v-model="smtpForm.port" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="smtpForm.username" placeholder="请输入SMTP用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="smtpForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item label="使用SSL">
              <el-switch v-model="smtpForm.ssl" />
            </el-form-item>
            <el-form-item>
              <el-button @click="testSmtp">发送测试邮件</el-button>
              <el-button type="primary" @click="saveSmtp">保存设置</el-button>
            </el-form-item>
          </el-form>

          <h3>企业微信/钉钉机器人</h3>
          <el-form :model="webhookForm" label-width="120px" style="max-width: 600px;">
            <el-form-item label="机器人类型">
              <el-select v-model="webhookForm.type" style="width: 200px;">
                <el-option label="企业微信" value="wecom" />
                <el-option label="钉钉" value="dingtalk" />
                <el-option label="飞书" value="feishu" />
              </el-select>
            </el-form-item>
            <el-form-item label="Webhook地址">
              <el-input v-model="webhookForm.url" type="textarea" :rows="2" placeholder="请输入Webhook地址" />
            </el-form-item>
            <el-form-item>
              <el-button @click="testWebhook">发送测试消息</el-button>
              <el-button type="primary" @click="saveWebhook">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 存储配置 -->
      <el-tab-pane label="存储配置" name="storage">
        <div class="settings-section">
          <h3>文件存储</h3>
          <el-form :model="storageForm" label-width="120px" style="max-width: 600px;">
            <el-form-item label="存储类型">
              <el-select v-model="storageForm.type" style="width: 200px;">
                <el-option label="本地磁盘" value="local" />
                <el-option label="NFS" value="nfs" />
                <el-option label="S3兼容对象存储" value="s3" />
              </el-select>
            </el-form-item>
            <el-form-item label="存储路径" v-if="storageForm.type === 'local'">
              <el-input v-model="storageForm.path" placeholder="如: /data/rpa/storage" />
            </el-form-item>
            <el-form-item label="NFS地址" v-if="storageForm.type === 'nfs'">
              <el-input v-model="storageForm.nfsHost" placeholder="如: 192.168.1.100:/data" />
            </el-form-item>
            <el-form-item label="S3 Endpoint" v-if="storageForm.type === 's3'">
              <el-input v-model="storageForm.s3Endpoint" placeholder="如: https://s3.cn-north-1.amazonaws.com.cn" />
            </el-form-item>
            <el-form-item label="Access Key" v-if="storageForm.type === 's3'">
              <el-input v-model="storageForm.accessKey" placeholder="请输入Access Key" />
            </el-form-item>
            <el-form-item label="Secret Key" v-if="storageForm.type === 's3'">
              <el-input v-model="storageForm.secretKey" type="password" show-password placeholder="请输入Secret Key" />
            </el-form-item>
            <el-form-item label="存储配额">
              <el-input-number v-model="storageForm.quota" :min="1" :max="10000" />
              <span class="unit">GB</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveStorage">保存设置</el-button>
            </el-form-item>
          </el-form>

          <h3>使用统计</h3>
          <div class="storage-stats">
            <div class="stat-item">
              <span class="label">已使用:</span>
              <span class="value">{{ storageUsed }} GB</span>
            </div>
            <div class="stat-item">
              <span class="label">总配额:</span>
              <span class="value">{{ storageForm.quota }} GB</span>
            </div>
            <div class="stat-item">
              <span class="label">使用率:</span>
              <el-progress :percentage="storageUsage" :color="storageUsage > 80 ? '#f56c6c' : storageUsage > 60 ? '#e6a23c' : '#67c23a'" style="width: 200px;" />
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- OCR服务 -->
      <el-tab-pane label="OCR服务" name="ocr">
        <div class="settings-section">
          <h3>OCR服务配置</h3>
          <el-form :model="ocrForm" label-width="120px" style="max-width: 600px;">
            <el-form-item label="OCR服务商">
              <el-select v-model="ocrForm.provider" style="width: 200px;">
                <el-option label="百度OCR" value="baidu" />
                <el-option label="阿里云OCR" value="aliyun" />
                <el-option label="腾讯OCR" value="tencent" />
                <el-option label="Tesseract (本地)" value="tesseract" />
              </el-select>
            </el-form-item>
            <el-form-item label="AppID" v-if="ocrForm.provider !== 'tesseract'">
              <el-input v-model="ocrForm.appId" placeholder="请输入AppID" />
            </el-form-item>
            <el-form-item label="API Key" v-if="ocrForm.provider !== 'tesseract'">
              <el-input v-model="ocrForm.apiKey" type="password" show-password placeholder="请输入API Key" />
            </el-form-item>
            <el-form-item label="Secret Key" v-if="ocrForm.provider !== 'tesseract'">
              <el-input v-model="ocrForm.secretKey" type="password" show-password placeholder="请输入Secret Key" />
            </el-form-item>
            <el-form-item>
              <el-button @click="testOcr">测试OCR识别</el-button>
              <el-button type="primary" @click="saveOcr">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 许可证管理 -->
      <el-tab-pane label="许可证" name="license">
        <div class="settings-section">
          <h3>许可证信息</h3>
          <div class="license-info-card">
            <div class="license-status" :class="licenseValid ? 'valid' : 'invalid'">
              <el-icon v-if="licenseValid"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              <span>{{ licenseValid ? '许可证有效' : '许可证无效' }}</span>
            </div>
            <div class="license-details">
              <div class="detail-row"><label>许可证类型:</label><span>企业版</span></div>
              <div class="detail-row"><label>并发数:</label><span>{{ licenseInfo.concurrent }} 机器人</span></div>
              <div class="detail-row"><label>有效期:</label><span>{{ licenseInfo.expiry }}</span></div>
              <div class="detail-row"><label>授权给:</label><span>{{ licenseInfo.company }}</span></div>
            </div>
          </div>
          <el-form label-width="120px" style="max-width: 600px; margin-top: 20px;">
            <el-form-item label="上传许可证">
              <el-upload action="#" :auto-upload="false" :limit="1" accept=".lic,.key">
                <el-button>选择许可证文件</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary">激活许可证</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 集成中心 -->
      <el-tab-pane label="集成中心" name="integration">
        <div class="settings-section">
          <h3>大模型服务配置</h3>
          <el-form :model="llmForm" label-width="120px" style="max-width: 600px;">
            <el-form-item label="服务提供商">
              <el-select v-model="llmForm.provider" style="width: 200px;">
                <el-option label="OpenAI" value="openai" />
                <el-option label="通义千问" value="qwen" />
                <el-option label="智谱AI" value="zhipu" />
                <el-option label="本地部署" value="local" />
              </el-select>
            </el-form-item>
            <el-form-item label="API地址" v-if="llmForm.provider !== 'local'">
              <el-input v-model="llmForm.apiUrl" placeholder="如: https://api.openai.com/v1" />
            </el-form-item>
            <el-form-item label="API Key" v-if="llmForm.provider !== 'local'">
              <el-input v-model="llmForm.apiKey" type="password" show-password placeholder="请输入API Key" />
            </el-form-item>
            <el-form-item label="模型名称">
              <el-input v-model="llmForm.model" placeholder="如: gpt-4, qwen-turbo" />
            </el-form-item>
            <el-form-item>
              <el-button @click="testLlm">测试连接</el-button>
              <el-button type="primary" @click="saveLlm">保存设置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'

const currentTab = ref('general')

const generalForm = reactive({
  systemName: 'RPA运营管理系统',
  sessionTimeout: 30,
  language: 'zh-CN',
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

const saveGeneral = () => ElMessage.success('通用设置保存成功')
const saveSmtp = () => ElMessage.success('SMTP设置保存成功')
const saveWebhook = () => ElMessage.success('Webhook设置保存成功')
const saveStorage = () => ElMessage.success('存储设置保存成功')
const saveOcr = () => ElMessage.success('OCR设置保存成功')
const saveLlm = () => ElMessage.success('大模型设置保存成功')

const testSmtp = () => ElMessage.success('测试邮件发送成功')
const testWebhook = () => ElMessage.success('测试消息发送成功')
const testOcr = () => ElMessage.success('OCR识别测试成功')
const testLlm = () => ElMessage.success('大模型服务连接正常')
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

.license-info-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 24px;
  border-radius: 12px;
}

.license-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
}

.license-status.valid { color: #67c23a; }
.license-status.invalid { color: #f56c6c; }

.license-details .detail-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.license-details .detail-row:last-child {
  border-bottom: none;
}

.license-details .detail-row label {
  width: 100px;
  opacity: 0.8;
}

.license-details .detail-row span {
  font-weight: 500;
}
</style>