/**
 * 全局水印指令和组合式函数
 *
 * 提供两种使用方式：
 * 1. v-watermark 指令
 * 2. useWatermark() 组合式函数
 *
 * @author RPA Security Team
 * @version 1.0.0
 */

import { ref, onMounted, onUnmounted, watch } from 'vue'
import watermarkInstance, { SensitivityLevel } from './WatermarkUtils.js'
export { SensitivityLevel }
import {
  getSensitivityByPath,
  getWatermarkConfigByPath,
  shouldApplyWatermark,
  isMonitorPage,
  PAGE_SENSITIVITY_MAP
} from './watermarkConfig.js'

/**
 * Vue指令 - v-watermark
 *
 * 使用方式：
 * <template>
 *   <div v-watermark>内容</div>
 * </template>
 *
 * 或带参数：
 * <template>
 *   <div v-watermark:[sensitivity]="options">内容</div>
 * </template>
 */
export const watermarkDirective = {
  mounted(el, binding) {
    const sensitivity = binding.arg || SensitivityLevel.LOW
    const options = binding.value || {}

    // 检查是否应该添加水印
    if (!shouldApplyWatermark(window.location.pathname)) {
      console.log('[v-watermark] 当前页面不需要水印')
      return
    }

    // 启动水印
    watermarkInstance.startProtection(sensitivity)
    console.log(`[v-watermark] 水印指令已挂载，敏感级别: ${sensitivity}`)
  },

  unmounted() {
    // 不销毁水印，因为可能是全局水印
  }
}

/**
 * 组合式函数 - useWatermark
 *
 * 使用方式：
 * <script setup>
 * import { useWatermark } from '@/utils/watermark/index.js'
 *
 * const { initWatermark, destroyWatermark, currentSensitivity } = useWatermark()
 * initWatermark()
 * </script>
 */
export function useWatermark() {
  const isActive = ref(false)
  const currentSensitivity = ref(SensitivityLevel.LOW)
  const isInitialized = ref(false)

  /**
   * 初始化水印
   */
  const initWatermark = (sensitivity = null) => {
    if (isInitialized.value) {
      console.warn('[useWatermark] 水印已初始化')
      return
    }

    const path = window.location.pathname

    // 检查是否需要水印
    if (!shouldApplyWatermark(path)) {
      console.log('[useWatermark] 当前页面不需要水印')
      return
    }

    // 实时监控页面不添加水印
    if (isMonitorPage(path)) {
      console.log('[useWatermark] 实时监控页面不添加水印')
      return
    }

    // 确定敏感级别
    const level = sensitivity || getSensitivityByPath(path)
    currentSensitivity.value = level

    // 启动水印
    watermarkInstance.startProtection(level)
    isActive.value = true
    isInitialized.value = true

    console.log(`[useWatermark] 水印已初始化，敏感级别: ${level}`)
  }

  /**
   * 销毁水印
   */
  const destroyWatermark = () => {
    watermarkInstance.destroy()
    isActive.value = false
    isInitialized.value = false
    console.log('[useWatermark] 水印已销毁')
  }

  /**
   * 更新水印配置
   */
  const updateWatermark = (newSensitivity) => {
    destroyWatermark()
    currentSensitivity.value = newSensitivity
    initWatermark(newSensitivity)
  }

  /**
   * 临时关闭水印
   */
  const temporaryDisable = async (reason, duration = 3600000) => {
    const result = await watermarkInstance.temporaryDisable(reason, duration)
    if (result.success) {
      isActive.value = false
    }
    return result
  }

  /**
   * 获取当前敏感级别
   */
  const getCurrentSensitivity = () => {
    return currentSensitivity.value
  }

  /**
   * 检查水印是否激活
   */
  const checkActive = () => {
    return watermarkInstance.isActive()
  }

  return {
    isActive,
    currentSensitivity,
    isInitialized,
    initWatermark,
    destroyWatermark,
    updateWatermark,
    temporaryDisable,
    getCurrentSensitivity,
    checkActive,
    watermarkInstance
  }
}

/**
 * 获取所有敏感级别配置
 */
export function getAllSensitivityConfigs() {
  return Object.values(PAGE_SENSITIVITY_MAP).map(level => ({
    level,
    config: getWatermarkConfigByPath(Object.keys(PAGE_SENSITIVITY_MAP).find(key => PAGE_SENSITIVITY_MAP[key] === level))
  }))
}

/**
 * 根据页面路径检测敏感级别
 */
export function detectPageSensitivity(path) {
  return getSensitivityByPath(path)
}

/**
 * 检查页面是否需要水印
 */
export function checkPageRequiresWatermark(path) {
  return shouldApplyWatermark(path)
}

/**
 * 创建全局水印管理器
 * 用于在应用启动时初始化全局水印
 */
export function createGlobalWatermarkManager() {
  let instance = null

  const init = () => {
    const path = window.location.pathname

    // 实时监控页面跳过
    if (isMonitorPage(path)) {
      console.log('[GlobalWatermark] 实时监控页面，跳过水印初始化')
      return
    }

    // 检查是否需要水印
    if (!shouldApplyWatermark(path)) {
      console.log('[GlobalWatermark] 当前页面不需要水印')
      return
    }

    const sensitivity = getSensitivityByPath(path)
    instance = watermarkInstance
    instance.startProtection(sensitivity)
    console.log(`[GlobalWatermark] 全局水印已启动，敏感级别: ${sensitivity}`)
  }

  const destroy = () => {
    if (instance) {
      instance.destroy()
      instance = null
      console.log('[GlobalWatermark] 全局水印已销毁')
    }
  }

  const update = (newPath) => {
    destroy()
    const sensitivity = getSensitivityByPath(newPath)
    if (sensitivity && !isMonitorPage(newPath)) {
      instance = watermarkInstance
      instance.startProtection(sensitivity)
      console.log(`[GlobalWatermark] 水印已更新，新敏感级别: ${sensitivity}`)
    }
  }

  return {
    init,
    destroy,
    update,
    getInstance: () => instance
  }
}

// 导出所有API
export default {
  watermarkDirective,
  useWatermark,
  createGlobalWatermarkManager,
  getAllSensitivityConfigs,
  detectPageSensitivity,
  checkPageRequiresWatermark
}
