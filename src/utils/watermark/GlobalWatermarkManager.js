/**
 * 全局水印和截图增强初始化模块
 *
 * 功能：
 * 1. 应用启动时初始化全局水印
 * 2. 设置截图检测
 * 3. 设置打印拦截
 * 4. 监听路由变化自动更新水印
 *
 * @author RPA Security Team
 * @version 1.0.0
 */

import watermarkInstance from './WatermarkUtils.js'
import printWatermarkInstance from './PrintWatermark.js'
import {
  getSensitivityByPath,
  shouldApplyWatermark,
  isMonitorPage,
  PERMISSION_CONFIG
} from './watermarkConfig.js'

/**
 * 全局水印管理器
 */
class GlobalWatermarkManager {
  constructor() {
    this.isInitialized = false
    this.currentSensitivity = null
    this.autoRestoreTimer = null
    this.statusCheckInterval = null
    // 水印是否被外部禁用
    this.isExternallyDisabled = false
  }

  /**
   * 初始化全局水印
   */
  init() {
    if (this.isInitialized) {
      console.warn('[GlobalWatermark] 水印系统已初始化')
      return
    }

    // 获取当前路径
    const path = window.location.pathname

    // 实时监控页面跳过
    if (isMonitorPage(path)) {
      console.log('[GlobalWatermark] 实时监控页面，跳过水印初始化')
      this.isInitialized = true
      return
    }

    // 检查是否需要水印
    if (!shouldApplyWatermark(path)) {
      console.log('[GlobalWatermark] 当前页面不需要水印')
      this.isInitialized = true
      return
    }

    // 获取敏感级别并启动水印
    const sensitivity = getSensitivityByPath(path)
    this.currentSensitivity = sensitivity

    // 启动水印保护
    watermarkInstance.startProtection(sensitivity)

    // 设置打印拦截
    printWatermarkInstance.setupPrintInterception()

    // 监听路由变化
    this.setupRouteListener()

    // 监听用户变化（重新加载用户信息后刷新水印）
    this.setupUserChangeListener()

    // 检查是否有临时关闭记录
    this.checkTemporaryDisableStatus()

    // 定期检查水印状态（每30秒）
    this.startStatusCheck()

    this.isInitialized = true
    console.log(`[GlobalWatermark] 全局水印系统已初始化，敏感级别: ${sensitivity}`)
  }

  /**
   * 启动定期状态检查
   */
  startStatusCheck() {
    // 每30秒检查一次水印状态
    this.statusCheckInterval = setInterval(() => {
      this.checkTemporaryDisableStatus()
    }, 30000)
  }

  /**
   * 停止定期状态检查
   */
  stopStatusCheck() {
    if (this.statusCheckInterval) {
      clearInterval(this.statusCheckInterval)
      this.statusCheckInterval = null
    }
  }

  /**
   * 设置路由监听
   */
  setupRouteListener() {
    // 监听 popstate 事件（浏览器前进后退）
    window.addEventListener('popstate', () => {
      this.handleRouteChange()
    })

    // 监听自定义路由事件（如果有）
    window.addEventListener('route-changed', (e) => {
      if (e.detail && e.detail.path) {
        this.handleRouteChange(e.detail.path)
      }
    })

    // 监听水印状态变化事件
    window.addEventListener('watermarkStatusChanged', (e) => {
      console.log('[GlobalWatermark] 收到水印状态变化事件:', e.detail)

      // 只有明确的启用/禁用操作才处理，不处理公告相关的事件
      if (e.detail && e.detail.enabled === false) {
        // 水印被禁用，移除水印
        watermarkInstance.destroy()
        this.isExternallyDisabled = true
        console.log('[GlobalWatermark] 水印已被禁用')
      } else if (e.detail && e.detail.enabled === true) {
        // 水印被启用，重新启动
        this.isExternallyDisabled = false
        console.log('[GlobalWatermark] 水印被启用，准备刷新')
        this.refreshWatermark()
      }
      // 忽略其他类型的事件（如公告相关事件）
    })

    // 覆盖 pushState 和 replaceState 以监听程序化路由跳转
    const originalPushState = window.history.pushState
    const originalReplaceState = window.history.replaceState

    window.history.pushState = (...args) => {
      originalPushState.apply(window.history, args)
      // 使用 requestAnimationFrame 延迟执行，避免阻塞
      requestAnimationFrame(() => this.handleRouteChange())
    }

    window.history.replaceState = (...args) => {
      originalReplaceState.apply(window.history, args)
      requestAnimationFrame(() => this.handleRouteChange())
    }
  }

  /**
   * 处理路由变化
   */
  handleRouteChange(newPath) {
    // 如果水印被外部禁用，不处理路由变化
    if (this.isExternallyDisabled) {
      console.log('[GlobalWatermark] 水印已被禁用，跳过路由变化处理')
      return
    }

    const path = newPath || window.location.pathname

    // 实时监控页面移除水印
    if (isMonitorPage(path)) {
      if (watermarkInstance.isActive()) {
        console.log('[GlobalWatermark] 进入实时监控页面，移除水印')
        watermarkInstance.destroy()
      }
      return
    }

    // 检查是否需要水印
    if (!shouldApplyWatermark(path)) {
      if (watermarkInstance.isActive()) {
        console.log('[GlobalWatermark] 进入不需要水印的页面，移除水印')
        watermarkInstance.destroy()
      }
      return
    }

    // 获取新的敏感级别
    const newSensitivity = getSensitivityByPath(path)

    // 如果敏感级别变化，更新水印（只更新配置，不销毁重建）
    if (newSensitivity !== this.currentSensitivity) {
      console.log(`[GlobalWatermark] 敏感级别变化: ${this.currentSensitivity} -> ${newSensitivity}`)
      this.currentSensitivity = newSensitivity
      watermarkInstance.createWatermarkElement(newSensitivity)
    } else if (!watermarkInstance.isActive()) {
      // 如果水印未激活，重新启动
      watermarkInstance.startProtection(newSensitivity)
    }
  }

  /**
   * 设置用户变化监听
   */
  setupUserChangeListener() {
    // 监听 localStorage 变化
    window.addEventListener('storage', (e) => {
      if (e.key === 'userInfo') {
        console.log('[GlobalWatermark] 用户信息已变更，刷新水印')
        this.refreshWatermark()
      }
    })

    // 监听自定义用户更新事件
    window.addEventListener('user-updated', () => {
      console.log('[GlobalWatermark] 收到用户更新事件，刷新水印')
      this.refreshWatermark()
    })
  }

  /**
   * 检查临时关闭状态
   */
  async checkTemporaryDisableStatus() {
    try {
      const response = await fetch('/api/watermark/status', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      })

      if (response.ok) {
        const result = await response.json()
        const watermarkEnabled = result.data?.watermarkEnabled

        if (watermarkEnabled === false) {
          // 水印被禁用
          if (!this.isExternallyDisabled) {
            console.log('[GlobalWatermark] 检测到水印被禁用，移除水印')
            watermarkInstance.destroy()
            this.isExternallyDisabled = true
          }
        } else if (watermarkEnabled === true && this.isExternallyDisabled) {
          // 水印被启用但当前被本地禁用（可能已自动恢复）
          console.log('[GlobalWatermark] 检测到水印已恢复')
          this.isExternallyDisabled = false
          this.refreshWatermark()
        }
      }
    } catch (e) {
      console.error('[GlobalWatermark] 检查临时关闭状态失败', e)
    }
  }

  /**
   * 刷新水印（当用户信息变化时或手动启用时）
   */
  refreshWatermark() {
    // 如果页面不需要水印，跳过
    const path = window.location.pathname

    if (isMonitorPage(path) || !shouldApplyWatermark(path)) {
      return
    }

    if (!this.currentSensitivity) {
      this.currentSensitivity = getSensitivityByPath(path)
    }

    // 如果水印被外部禁用且不是正在启用，跳过
    if (this.isExternallyDisabled) {
      console.log('[GlobalWatermark] 水印已被禁用，跳过刷新')
      return
    }

    // 销毁现有水印并重新启动
    if (watermarkInstance.isActive()) {
      watermarkInstance.destroy()
    }

    watermarkInstance.startProtection(this.currentSensitivity)
  }

  /**
   * 临时关闭水印
   */
  async temporaryDisable(reason, duration = 3600000) {
    try {
      const response = await fetch('/api/watermark/temporary-disable', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ reason, duration })
      })

      if (response.ok) {
        const result = await response.json()
        if (result.code === 0) {
          // 设置禁用标志
          this.isExternallyDisabled = true
          // 销毁水印
          watermarkInstance.destroy()

          // 设置自动恢复定时器
          this.setAutoRestoreTimer(duration)

          return { success: true, ...result.data }
        }
      }

      const error = await response.json()
      return { success: false, message: error.message }
    } catch (e) {
      console.error('[GlobalWatermark] 临时关闭失败', e)
      return { success: false, message: e.message }
    }
  }

  /**
   * 手动恢复水印
   */
  async restoreWatermark() {
    try {
      const response = await fetch('/api/watermark/restore', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      })

      if (response.ok) {
        const result = await response.json()
        if (result.code === 0) {
          // 清除禁用标志
          this.isExternallyDisabled = false
          // 清除自动恢复定时器
          if (this.autoRestoreTimer) {
            clearTimeout(this.autoRestoreTimer)
            this.autoRestoreTimer = null
          }

          // 重新启动水印
          this.refreshWatermark()

          return { success: true }
        }
      }

      const error = await response.json()
      return { success: false, message: error.message }
    } catch (e) {
      console.error('[GlobalWatermark] 恢复水印失败', e)
      return { success: false, message: e.message }
    }
  }

  /**
   * 设置自动恢复定时器
   */
  setAutoRestoreTimer(duration) {
    if (this.autoRestoreTimer) {
      clearTimeout(this.autoRestoreTimer)
    }

    this.autoRestoreTimer = setTimeout(() => {
      console.log('[GlobalWatermark] 水印自动恢复时间到达')
      this.restoreWatermark()
    }, duration)
  }

  /**
   * 销毁水印系统
   */
  destroy() {
    // 停止定期检查
    this.stopStatusCheck()

    if (this.autoRestoreTimer) {
      clearTimeout(this.autoRestoreTimer)
      this.autoRestoreTimer = null
    }

    watermarkInstance.destroy()
    printWatermarkInstance.destroy()

    this.isInitialized = false
    this.currentSensitivity = null

    console.log('[GlobalWatermark] 全局水印系统已销毁')
  }

  /**
   * 获取当前状态
   */
  getStatus() {
    return {
      isInitialized: this.isInitialized,
      isActive: watermarkInstance.isActive(),
      currentSensitivity: this.currentSensitivity
    }
  }
}

// 创建全局单例
const globalWatermarkManager = new GlobalWatermarkManager()

// 导出
export default globalWatermarkManager
export { GlobalWatermarkManager }
