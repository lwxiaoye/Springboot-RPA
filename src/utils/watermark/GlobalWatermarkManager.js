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
      return
    }

    // 获取当前路径
    const path = window.location.pathname

    // 实时监控页面跳过
    if (isMonitorPage(path)) {
      this.isInitialized = true
      return
    }

    // 检查是否需要水印
    if (!shouldApplyWatermark(path)) {
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
      // 只有明确的启用/禁用操作才处理
      if (e.detail && e.detail.enabled === false) {
        watermarkInstance.destroy()
        this.isExternallyDisabled = true
      } else if (e.detail && e.detail.enabled === true) {
        this.isExternallyDisabled = false
        this.refreshWatermark()
      }
    })

    // 覆盖 pushState 和 replaceState 以监听程序化路由跳转
    const originalPushState = window.history.pushState
    const originalReplaceState = window.history.replaceState

    window.history.pushState = (...args) => {
      originalPushState.apply(window.history, args)
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
      return
    }

    const path = newPath || window.location.pathname

    // 实时监控页面移除水印
    if (isMonitorPage(path)) {
      if (watermarkInstance.isActive()) {
        watermarkInstance.destroy()
      }
      return
    }

    // 检查是否需要水印
    if (!shouldApplyWatermark(path)) {
      if (watermarkInstance.isActive()) {
        watermarkInstance.destroy()
      }
      return
    }

    // 获取新的敏感级别
    const newSensitivity = getSensitivityByPath(path)

    // 如果敏感级别变化，更新水印
    if (newSensitivity !== this.currentSensitivity) {
      this.currentSensitivity = newSensitivity
      watermarkInstance.createWatermarkElement(newSensitivity)
    } else if (!watermarkInstance.isActive()) {
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
        this.refreshWatermark()
      }
    })

    // 监听自定义用户更新事件
    window.addEventListener('user-updated', () => {
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
          if (!this.isExternallyDisabled) {
            watermarkInstance.destroy()
            this.isExternallyDisabled = true
          }
        } else if (watermarkEnabled === true && this.isExternallyDisabled) {
          this.isExternallyDisabled = false
          this.refreshWatermark()
        }
      }
    } catch (e) {
      // 静默处理错误
    }
  }

  /**
   * 刷新水印
   */
  refreshWatermark() {
    const path = window.location.pathname

    if (isMonitorPage(path) || !shouldApplyWatermark(path)) {
      return
    }

    if (!this.currentSensitivity) {
      this.currentSensitivity = getSensitivityByPath(path)
    }

    if (this.isExternallyDisabled) {
      return
    }

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
          this.isExternallyDisabled = true
          watermarkInstance.destroy()
          this.setAutoRestoreTimer(duration)
          return { success: true, ...result.data }
        }
      }

      const error = await response.json()
      return { success: false, message: error.message }
    } catch (e) {
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
          this.isExternallyDisabled = false
          if (this.autoRestoreTimer) {
            clearTimeout(this.autoRestoreTimer)
            this.autoRestoreTimer = null
          }
          this.refreshWatermark()
          return { success: true }
        }
      }

      const error = await response.json()
      return { success: false, message: error.message }
    } catch (e) {
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
      this.restoreWatermark()
    }, duration)
  }

  /**
   * 销毁水印系统
   */
  destroy() {
    this.stopStatusCheck()

    if (this.autoRestoreTimer) {
      clearTimeout(this.autoRestoreTimer)
      this.autoRestoreTimer = null
    }

    watermarkInstance.destroy()
    printWatermarkInstance.destroy()

    this.isInitialized = false
    this.currentSensitivity = null
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
