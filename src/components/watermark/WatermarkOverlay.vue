<template>
  <!-- 水印容器 - 通过JS动态控制，不在模板中渲染 -->
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import watermarkInstance, { SensitivityLevel } from '../../utils/watermark/WatermarkUtils.js'
import {
  getSensitivityByPath,
  getWatermarkConfigByPath,
  shouldApplyWatermark,
  isMonitorPage,
  ANTI_TAMPER_CONFIG
} from '../../utils/watermark/watermarkConfig.js'

const route = useRoute()

const props = defineProps({
  /**
   * 是否强制启用水印（用于测试）
   */
  forceEnable: {
    type: Boolean,
    default: false
  },
  /**
   * 自定义敏感级别（会覆盖自动检测）
   */
  customSensitivity: {
    type: String,
    default: null
  }
})

const emit = defineEmits([
  'watermark-ready',
  'watermark-tampering',
  'watermark-updated'
])

const isActive = ref(false)
const currentSensitivity = ref(SensitivityLevel.LOW)
const isInitialized = ref(false)
// 水印是否被外部禁用
let isExternallyDisabled = false
// 防抖计时器
let routeChangeTimer = null
// 事件监听是否已注册的标志
let eventListenersRegistered = false

/**
 * 获取当前页面的敏感级别
 */
const getCurrentPageSensitivity = () => {
  // 优先使用自定义敏感级别
  if (props.customSensitivity) {
    return props.customSensitivity
  }

  const path = route.path
  return getSensitivityByPath(path)
}

/**
 * 初始化水印
 */
const initWatermark = () => {
  const path = route.path

  // 检查是否需要水印
  if (!shouldApplyWatermark(path)) {
    console.log(`[WatermarkOverlay] 页面 ${path} 不需要水印`)
    return
  }

  // 实时监控页面不添加水印
  if (isMonitorPage(path)) {
    console.log(`[WatermarkOverlay] 实时监控页面不添加水印`)
    return
  }

  // 如果水印被外部禁用，不重新启用
  // 注意：外部禁用状态需要通过 watermarkStatusChanged 事件来恢复
  if (isExternallyDisabled) {
    console.log(`[WatermarkOverlay] 水印已被禁用，跳过初始化`)
    return
  }

  // 获取敏感级别
  const sensitivity = getCurrentPageSensitivity()

  // 获取配置
  const config = getWatermarkConfigByPath(path)
  if (config) {
    currentSensitivity.value = sensitivity
    isActive.value = true
    isInitialized.value = true

    // 启动水印保护
    watermarkInstance.startProtection(sensitivity)

    console.log(`[WatermarkOverlay] 水印已初始化，敏感级别: ${config.levelName}`)
    emit('watermark-ready', { sensitivity, config })
  }
}

/**
 * 更新水印
 */
const updateWatermark = () => {
  // 如果水印被外部禁用，不更新
  if (isExternallyDisabled) {
    console.log(`[WatermarkOverlay] 水印已被禁用，跳过更新`)
    return
  }

  const newSensitivity = getCurrentPageSensitivity()

  // 敏感级别变化时才重新创建
  if (newSensitivity !== currentSensitivity.value) {
    currentSensitivity.value = newSensitivity
    watermarkInstance.createWatermarkElement(newSensitivity)
    emit('watermark-updated', { sensitivity: newSensitivity })
  }
}

// 监听路由变化 - 使用 requestAnimationFrame 优化
watch(
  () => route.path,
  (newPath, oldPath) => {
    if (newPath !== oldPath) {
      console.log(`[WatermarkOverlay] 路由变化: ${oldPath} -> ${newPath}`)

      // 使用 requestAnimationFrame 延迟到下一帧更新，避免阻塞
      if (routeChangeTimer) {
        cancelAnimationFrame(routeChangeTimer)
      }
      routeChangeTimer = requestAnimationFrame(() => {
        routeChangeTimer = null
        updateWatermark()
      })
    }
  }
)

// 监听props变化
watch(
  () => props.forceEnable,
  (newVal) => {
    if (newVal && !isInitialized.value) {
      initWatermark()
    }
  }
)

watch(
  () => props.customSensitivity,
  () => {
    if (isInitialized.value) {
      updateWatermark()
    }
  }
)

// 生命周期
onMounted(() => {
  initWatermark()
})

// 监听水印状态变化事件
const handleWatermarkStatusChange = (event) => {
  console.log('[WatermarkOverlay] 收到水印状态变化事件:', event.detail)

  // 只有明确的启用/禁用操作才处理，不处理公告相关的事件
  if (event.detail && event.detail.enabled === false) {
    // 水印被禁用，移除水印
    watermarkInstance.destroy()
    isActive.value = false
    isExternallyDisabled = true
    console.log('[WatermarkOverlay] 水印已被外部禁用')
  } else if (event.detail && event.detail.enabled === true) {
    // 水印被启用，重新初始化
    isExternallyDisabled = false
    initWatermark()
  }
  // 忽略其他类型的事件（如公告相关事件）
}

// 注册事件监听（只执行一次）
const registerEventListeners = () => {
  if (eventListenersRegistered) return

  window.addEventListener('watermarkStatusChanged', handleWatermarkStatusChange)
  eventListenersRegistered = true
  console.log('[WatermarkOverlay] 事件监听已注册')
}

// 在 setup 阶段注册事件监听
registerEventListeners()

onUnmounted(() => {
  // 清除动画帧
  if (routeChangeTimer) {
    cancelAnimationFrame(routeChangeTimer)
    routeChangeTimer = null
  }
  // 移除事件监听
  window.removeEventListener('watermarkStatusChanged', handleWatermarkStatusChange)
  eventListenersRegistered = false
})

// 暴露方法给父组件
defineExpose({
  updateWatermark,
  getCurrentSensitivity: () => currentSensitivity.value,
  isWatermarkActive: () => isActive.value,
  destroyWatermark: () => {
    watermarkInstance.destroy()
    isActive.value = false
  }
})
</script>

<style scoped>
/* 水印样式由JS动态控制，这里仅提供基础样式 */
.watermark-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 1;
}
</style>
