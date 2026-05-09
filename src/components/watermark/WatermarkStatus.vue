<template>
  <div class="watermark-status">
    <el-tag
      :type="isActive ? 'success' : 'warning'"
      :icon="isActive ? 'Check' : 'Close'"
      size="small"
    >
      {{ isActive ? '水印保护已启用' : '水印保护已关闭' }}
    </el-tag>

    <el-button
      v-if="isAdmin && isActive"
      type="text"
      size="small"
      @click="showDisableDialog"
      style="margin-left: 8px"
    >
      临时关闭
    </el-button>

    <el-button
      v-if="isAdmin && !isActive"
      type="text"
      size="small"
      @click="handleRestore"
      style="margin-left: 8px"
    >
      恢复水印
    </el-button>

    <!-- 临时关闭对话框 -->
    <el-dialog
      v-model="disableDialogVisible"
      title="临时关闭水印保护"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="disableForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="关闭原因" prop="reason">
          <el-input
            v-model="disableForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入关闭水印的原因（必填）"
          />
        </el-form-item>

        <el-form-item label="关闭时长" prop="duration">
          <el-select v-model="disableForm.duration" placeholder="请选择关闭时长">
            <el-option :value="30" label="30分钟" />
            <el-option :value="60" label="1小时" />
            <el-option :value="120" label="2小时" />
            <el-option :value="360" label="6小时" />
            <el-option :value="720" label="12小时" />
            <el-option :value="1440" label="24小时" />
          </el-select>
          <span style="margin-left: 10px; color: #909399;">
            水印将在1小时后自动恢复
          </span>
        </el-form-item>

        <el-alert
          type="warning"
          :closable="false"
          show-icon
          style="margin-top: 10px"
        >
          <template #title>
            <strong>注意：</strong>关闭水印保护将降低数据安全性。
            此操作将被记录到审计日志。
          </template>
        </el-alert>
      </el-form>

      <template #footer>
        <el-button @click="disableDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDisable" :loading="loading">
          确认关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import globalWatermarkManager from '../../utils/watermark/GlobalWatermarkManager.js'

const props = defineProps({
  isAdmin: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['status-change'])

const isActive = ref(true)
const disableDialogVisible = ref(false)
const loading = ref(false)
const formRef = ref(null)

const disableForm = ref({
  reason: '',
  duration: 60
})

const rules = {
  reason: [
    { required: true, message: '请输入关闭原因', trigger: 'blur' },
    { min: 5, max: 500, message: '原因长度在5-500个字符之间', trigger: 'blur' }
  ],
  duration: [
    { required: true, message: '请选择关闭时长', trigger: 'change' }
  ]
}

const status = computed(() => globalWatermarkManager.getStatus())

const showDisableDialog = () => {
  disableForm.value.reason = ''
  disableForm.value.duration = 60
  disableDialogVisible.value = true
}

const handleDisable = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true

  try {
    const result = await globalWatermarkManager.temporaryDisable(
      disableForm.value.reason,
      disableForm.value.duration * 60 * 1000
    )

    if (result.success) {
      ElMessage.success(`水印已临时关闭，将于${disableForm.value.duration}分钟后自动恢复`)
      isActive.value = false
      disableDialogVisible.value = false
      emit('status-change', false)

      // 通知全局水印系统和水印管理器
      window.dispatchEvent(new CustomEvent('watermarkStatusChanged', {
        detail: { enabled: false, activeDisableRecord: true }
      }))
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  } finally {
    loading.value = false
  }
}

const handleRestore = async () => {
  try {
    const result = await globalWatermarkManager.restoreWatermark()

    if (result.success) {
      ElMessage.success('水印已恢复')
      isActive.value = true
      emit('status-change', true)

      // 通知全局水印系统和水印管理器
      window.dispatchEvent(new CustomEvent('watermarkStatusChanged', {
        detail: { enabled: true }
      }))
    } else {
      ElMessage.error(result.message || '恢复失败')
    }
  } catch (e) {
    ElMessage.error('恢复失败：' + e.message)
  }
}

const checkStatus = () => {
  const status = globalWatermarkManager.getStatus()
  isActive.value = status.isActive
}

onMounted(() => {
  checkStatus()
})
</script>

<style scoped>
.watermark-status {
  display: inline-flex;
  align-items: center;
}
</style>
