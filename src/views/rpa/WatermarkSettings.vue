<template>
  <div class="watermark-settings">
    <div class="page-header">
      <div class="header-left">
        <div class="header-title">
          <h2>{{ t('watermark.title') }}</h2>
          <el-tag size="small" type="success">SECURITY</el-tag>
        </div>
        <p class="page-desc">{{ t('watermark.description') }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadConfigs">
          <el-icon><Refresh /></el-icon>
          {{ t('watermark.refresh') }}
        </el-button>
      </div>
    </div>

    <!-- 水印状态卡片 -->
    <div class="status-cards">
      <div class="status-card" :class="{ 'is-disabled': !watermarkEnabled }">
        <div class="status-icon">
          <el-icon :size="40" :color="watermarkEnabled ? '#67C23A' : '#909399'">
            <component :is="watermarkEnabled ? 'CircleCheck' : 'Close'" />
          </el-icon>
        </div>
        <div class="status-info">
          <span class="status-label">{{ t('watermark.watermarkStatus') }}</span>
          <span class="status-value">{{ watermarkEnabled ? t('watermark.enabled') : t('watermark.disabled') }}</span>
        </div>
        <div class="status-actions" v-if="isAdmin">
          <el-button
            :type="watermarkEnabled ? 'danger' : 'success'"
            size="small"
            @click="toggleWatermark"
          >
            {{ watermarkEnabled ? t('watermark.temporaryClose') : t('watermark.enableNow') }}
          </el-button>
        </div>
      </div>

      <div class="status-card" v-if="activeDisableRecord">
        <div class="status-icon warning">
          <el-icon :size="40" color="#E6A23C">
            <Clock />
          </el-icon>
        </div>
        <div class="status-info">
          <span class="status-label">{{ t('watermark.remainingTime') }}</span>
          <span class="status-value">{{ remainingTime }}</span>
        </div>
      </div>
    </div>

    <!-- 临时关闭对话框 -->
    <el-dialog v-model="disableDialogVisible" :title="t('watermark.temporaryDisable')" width="500px">
      <el-form :model="disableForm" label-width="100px">
        <el-form-item :label="t('watermark.closeReason')">
          <el-input
            v-model="disableForm.reason"
            type="textarea"
            :rows="3"
            :placeholder="t('watermark.reasonPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('watermark.closeDuration')">
          <el-select v-model="disableForm.duration" style="width: 100%;">
            <el-option :label="t('watermark.minutes30')" :value="30" />
            <el-option :label="t('watermark.hour1')" :value="60" />
            <el-option :label="t('watermark.hours2')" :value="120" />
            <el-option :label="t('watermark.hours4')" :value="240" />
            <el-option :label="t('watermark.hours8')" :value="480" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disableDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmDisable" :loading="disableLoading">
          {{ t('watermark.confirmClose') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 水印配置列表 -->
    <div class="config-section" v-if="isAdmin">
      <div class="section-header">
        <h3>{{ t('watermark.sensitivityConfig') }}</h3>
        <p class="section-desc">{{ t('watermark.sensitivityDesc') }}</p>
      </div>

      <div class="config-cards">
        <div
          v-for="config in configs"
          :key="config.id"
          class="config-card"
          :class="[config.sensitivityLevel]"
        >
          <div class="config-header">
            <div class="config-level">
              <span class="level-badge" :class="config.sensitivityLevel">
                {{ getLevelName(config.sensitivityLevel) }}
              </span>
            </div>
            <el-tag size="small" :type="config.enabled ? 'success' : 'info'">
              {{ config.enabled ? t('watermark.enabled') : t('watermark.disabled') }}
            </el-tag>
          </div>

          <div class="config-body">
            <div class="config-item">
              <span class="item-label">{{ t('watermark.opacity') }}</span>
              <span class="item-value">{{ (config.opacity * 100).toFixed(0) }}%</span>
            </div>
            <div class="config-item">
              <span class="item-label">{{ t('watermark.fontSize') }}</span>
              <span class="item-value">{{ config.fontSize }}px</span>
            </div>
            <div class="config-item">
              <span class="item-label">{{ t('watermark.watermarkColor') }}</span>
              <span class="item-value">
                <span class="color-preview" :style="{ backgroundColor: config.color }"></span>
                {{ config.color }}
              </span>
            </div>
            <div class="config-item">
              <span class="item-label">{{ t('watermark.rotationAngle') }}</span>
              <span class="item-value">{{ config.rotation }}°</span>
            </div>
          </div>

          <div class="config-pages">
            <span class="pages-label">{{ t('watermark.applicablePages') }}</span>
            <span class="pages-list">{{ getPagesText(config.pages) }}</span>
          </div>

          <div class="config-actions">
            <el-button size="small" @click="editConfig(config)">{{ t('watermark.edit') }}</el-button>
            <el-button
              size="small"
              :type="config.enabled ? 'warning' : 'success'"
              @click="toggleConfig(config)"
            >
              {{ config.enabled ? t('watermark.disableConfig') : t('watermark.enableConfig') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑配置对话框 -->
    <el-dialog v-model="editDialogVisible" :title="t('watermark.edit') + ' - ' + editingConfig?.sensitivityName" width="600px">
      <el-form v-if="editingConfig" :model="editForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('watermark.opacity')">
              <el-slider
                v-model="editForm.opacity"
                :min="0.05"
                :max="1"
                :step="0.05"
                :format-tooltip="val => (val * 100).toFixed(0) + '%'"
              />
              <span class="slider-value">{{ (editForm.opacity * 100).toFixed(0) }}%</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('watermark.fontSize')">
              <el-input-number v-model="editForm.fontSize" :min="10" :max="30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('watermark.watermarkColor')">
              <el-color-picker v-model="editForm.color" />
              <span class="color-value">{{ editForm.color }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('watermark.rotationAngle')">
              <el-input-number v-model="editForm.rotation" :min="-90" :max="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('watermark.tileWidth')">
              <el-input-number v-model="editForm.tileWidth" :min="100" :max="800" :step="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('watermark.tileHeight')">
              <el-input-number v-model="editForm.tileHeight" :min="100" :max="800" :step="50" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveConfig" :loading="saveLoading">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 水印预览 -->
    <div class="preview-section">
      <div class="section-header">
        <h3>{{ t('watermark.previewSection') }}</h3>
        <el-button size="small" @click="refreshPreview">{{ t('watermark.refreshPreview') }}</el-button>
      </div>
      <div class="preview-container">
        <div class="preview-box" :style="previewStyle">
          <p class="preview-text">{{ t('watermark.sampleContent') }}</p>
          <p class="preview-text">{{ t('watermark.sampleText') }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, CircleCheck, Close, Clock } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut } from '../../utils/api.js'

const { t } = useI18n()

const isAdmin = computed(() => {
  const user = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return user.role === 1
})

// 水印状态
const watermarkEnabled = ref(true)
const activeDisableRecord = ref(null)
const remainingTime = ref('')
let countdownTimer = null

// 配置列表
const configs = ref([])

// 禁用对话框
const disableDialogVisible = ref(false)
const disableForm = ref({
  reason: '',
  duration: 60
})
const disableLoading = ref(false)

// 编辑对话框
const editDialogVisible = ref(false)
const editingConfig = ref(null)
const editForm = ref({})
const saveLoading = ref(false)

// 加载配置
const loadConfigs = async () => {
  try {
    const result = await apiGet('/watermark/config')
    if (result.code === 0) {
      configs.value = result.data || []
    }
  } catch (e) {
    console.error(t('watermark.loadFailed'), e)
  }
}

// 加载水印状态
const loadStatus = async () => {
  try {
    const result = await apiGet('/watermark/status')
    if (result.code === 0) {
      watermarkEnabled.value = result.data.watermarkEnabled
      activeDisableRecord.value = result.data.activeDisableRecord
    }
  } catch (e) {
    console.error(t('watermark.loadFailed'), e)
  }
}

// 切换水印状态
const toggleWatermark = async () => {
  if (watermarkEnabled.value) {
    // 关闭水印
    disableDialogVisible.value = true
  } else {
    // 启用水印
    try {
      await apiPost('/watermark/restore')
      watermarkEnabled.value = true
      activeDisableRecord.value = null
      remainingTime.value = ''
      ElMessage.success(t('watermark.enabled'))

      // 通知全局水印系统和水印管理器
      window.dispatchEvent(new CustomEvent('watermarkStatusChanged', {
        detail: { enabled: true }
      }))
    } catch (e) {
      ElMessage.error(t('watermark.enableFailed'))
    }
  }
}

// 确认关闭水印
const confirmDisable = async () => {
  if (!disableForm.value.reason.trim()) {
    ElMessage.warning(t('watermark.fillReason'))
    return
  }

  disableLoading.value = true
  try {
    await apiPost('/watermark/temporary-disable', {
      reason: disableForm.value.reason,
      duration: disableForm.value.duration
    })
    ElMessage.success(t('watermark.temporaryClose'))
    disableDialogVisible.value = false
    watermarkEnabled.value = false
    activeDisableRecord.value = true
    startCountdown()

    // 通知全局水印系统和水印管理器
    window.dispatchEvent(new CustomEvent('watermarkStatusChanged', {
      detail: { enabled: false, activeDisableRecord: true }
    }))
    window.dispatchEvent(new CustomEvent('announcementReadUpdated'))
  } catch (e) {
    ElMessage.error(e.message || t('watermark.disableFailed'))
  } finally {
    disableLoading.value = false
  }
}

// 编辑配置
const editConfig = (config) => {
  editingConfig.value = config
  editForm.value = { ...config }
  editDialogVisible.value = true
}

// 保存配置
const saveConfig = async () => {
  saveLoading.value = true
  try {
    await apiPut(`/watermark/config/${editingConfig.value.id}`, editForm.value)
    ElMessage.success(t('watermark.configSaved'))
    editDialogVisible.value = false
    loadConfigs()
  } catch (e) {
    ElMessage.error(e.message || t('watermark.saveFailed'))
  } finally {
    saveLoading.value = false
  }
}

// 切换单个配置
const toggleConfig = async (config) => {
  const action = config.enabled ? t('watermark.disableConfig') : t('watermark.enableConfig')
  try {
    await ElMessageBox.confirm(`${t('collaboration.confirmDeleteText')}?`, t('common.warning'), { type: 'warning' })
    await apiPut(`/watermark/config/${config.id}`, {
      ...config,
      enabled: !config.enabled
    })
    ElMessage.success(action)
    loadConfigs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(`${action}`)
    }
  }
}

// 获取级别名称
const getLevelName = (sensitivity) => {
  const names = {
    extreme: t('watermark.extremeSensitive'),
    high: t('watermark.highSensitive'),
    medium: t('watermark.mediumSensitive'),
    low: t('watermark.lowSensitive')
  }
  return names[sensitivity] || sensitivity
}

// 获取页面列表文本
const getPagesText = (pages) => {
  if (!pages) return t('watermark.notConfigured')
  return pages.split(',').slice(0, 3).join('、') + (pages.split(',').length > 3 ? '...' : '')
}

// 预览样式
const previewStyle = computed(() => {
  const config = configs.value.find(c => c.enabled) || configs.value[0] || {}
  return {
    backgroundImage: `url(${generatePreviewWatermark(config)})`,
    backgroundRepeat: 'repeat',
    backgroundPosition: '0 0',
    backgroundSize: `${config.tileWidth || 300}px ${config.tileHeight || 200}px`
  }
})

// 生成预览水印
const generatePreviewWatermark = (config) => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  const width = config.tileWidth || 300
  const height = config.tileHeight || 200

  canvas.width = width
  canvas.height = height

  ctx.clearRect(0, 0, width, height)
  ctx.globalAlpha = config.opacity || 0.5
  ctx.fillStyle = config.color || '#000000'
  ctx.font = `${config.fontSize || 14}px KaiTi, 楷体, Microsoft YaHei`
  ctx.rotate(config.rotation * Math.PI / 180 || -0.785)

  const text = `${localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')).realName || '测试用户' : '测试用户'} 测试水印`
  ctx.fillText(text, 20, height / 2)
  ctx.fillText(new Date().toLocaleString('zh-CN'), 20, height / 2 + 25)

  return canvas.toDataURL('image/png')
}

const refreshPreview = () => {
  // 强制刷新预览
  const index = configs.value.findIndex(c => c.enabled)
  if (index >= 0) {
    configs.value[index] = { ...configs.value[index] }
  }
}

// 倒计时
const startCountdown = () => {
  if (countdownTimer) clearInterval(countdownTimer)

  // 如果没有活跃的禁用记录，清除倒计时
  if (!activeDisableRecord.value) {
    remainingTime.value = ''
    return
  }

  // 计算剩余时间
  const updateRemaining = () => {
    if (!activeDisableRecord.value) {
      remainingTime.value = ''
      if (countdownTimer) clearInterval(countdownTimer)
      return
    }

    const expireTime = new Date(activeDisableRecord.value.expireTime).getTime()
    const now = Date.now()
    const diff = expireTime - now

    if (diff <= 0) {
      // 已过期，恢复水印
      remainingTime.value = t('watermark.expired')
      watermarkEnabled.value = true
      activeDisableRecord.value = null
      if (countdownTimer) clearInterval(countdownTimer)
      ElMessage.info(t('watermark.autoRestored'))
      return
    }

    // 计算时分秒
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)

    if (hours > 0) {
      remainingTime.value = `${hours}小时${minutes}分${seconds}秒`
    } else if (minutes > 0) {
      remainingTime.value = `${minutes}分${seconds}秒`
    } else {
      remainingTime.value = `${seconds}秒`
    }
  }

  // 立即更新一次
  updateRemaining()
  // 每秒更新
  countdownTimer = setInterval(updateRemaining, 1000)
}

onMounted(() => {
  loadConfigs()
  loadStatus()
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.watermark-settings {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 6px 0 0 0;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.status-card.is-disabled {
  border-color: #f56c6c;
  background: #fef0f0;
}

.status-icon {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 12px;
}

.status-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.status-label {
  font-size: 13px;
  color: #909399;
}

.status-value {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-top: 4px;
}

.config-section, .preview-section {
  background: #ffffff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid #e8e8e8;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.section-desc {
  font-size: 13px;
  color: #909399;
  margin: 4px 0 0 0;
}

.config-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.config-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: #fafbfc;
}

.config-card.extreme { border-left: 3px solid #f56c6c !important; }
.config-card.high { border-left: 3px solid #e6a23c !important; }
.config-card.medium { border-left: 3px solid #409eff !important; }
.config-card.low { border-left: 3px solid #67c23a !important; }

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.level-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
}

.level-badge.extreme { background: #fef0f0; color: #f56c6c; }
.level-badge.high { background: #fdf6ec; color: #e6a23c; }
.level-badge.medium { background: #ecf5ff; color: #409eff; }
.level-badge.low { background: #f0f9eb; color: #67c23a; }

.config-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.config-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 6px 0;
}

.item-label {
  color: #909399;
}

.item-value {
  color: #1a1a1a;
  font-weight: 500;
}

.color-preview {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 3px;
  margin-right: 4px;
  vertical-align: middle;
  border: 1px solid #dcdfe6;
}

.config-pages {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.pages-list {
  color: #606266;
}

.config-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.slider-value, .color-value {
  margin-left: 12px;
  color: #606266;
}

.preview-container {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  min-height: 200px;
}

.preview-box {
  background: #ffffff;
  border-radius: 8px;
  padding: 24px;
  min-height: 180px;
  position: relative;
}

.preview-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  margin: 0;
}
</style>
