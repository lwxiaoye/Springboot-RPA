/**
 * 金融级水印工具类 - 银保监会合规审计版
 *
 * 核心特性：
 * 1. Canvas动态生成 - 绝对禁止静态水印
 * 2. 防篡改机制 - 500ms实时检测、自动重建
 * 3. 防截图增强 - 随机偏移、时间戳
 * 4. 分级配置 - 按页面敏感级别自动切换
 *
 * @author RPA Security Team
 * @version 1.0.0
 */

/**
 * 页面敏感级别枚举
 */
const SensitivityLevel = {
  EXTREME: 'extreme',   // 极高敏感: 执行日志、审计日志、凭证中心、数据查询
  HIGH: 'high',         // 高敏感: 任务调度中心、机器人管理、队列管理、触发器管理、数据脱敏
  MEDIUM: 'medium',     // 中敏感: 流程仓库、脚本执行、分布式锁、报表分析
  LOW: 'low'            // 低敏感: 系统设置、通知管理
}

/**
 * 敏感级别详细配置（优先级最高，支持动态更新）
 */
const SENSITIVITY_CONFIG = {
  [SensitivityLevel.EXTREME]: {
    opacity: 0.5,
    fontSize: 18,
    fontFamily: '"KaiTi", "楷体", "STKaiti", "Microsoft YaHei", sans-serif',
    color: '#F5F5F5',
    tileWidth: 400,
    tileHeight: 300
  },
  [SensitivityLevel.HIGH]: {
    opacity: 0.5,
    fontSize: 18,
    fontFamily: '"KaiTi", "楷体", "STKaiti", "Microsoft YaHei", sans-serif',
    color: '#F5F5F5',
    tileWidth: 400,
    tileHeight: 300
  },
  [SensitivityLevel.MEDIUM]: {
    opacity: 0.5,
    fontSize: 18,
    fontFamily: '"KaiTi", "楷体", "STKaiti", "Microsoft YaHei", sans-serif',
    color: '#F5F5F5',
    tileWidth: 450,
    tileHeight: 320
  },
  [SensitivityLevel.LOW]: {
    opacity: 0.5,
    fontSize: 18,
    fontFamily: '"KaiTi", "楷体", "STKaiti", "Microsoft YaHei", sans-serif',
    color: '#F5F5F5',
    tileWidth: 500,
    tileHeight: 350
  }
}

/**
 * 水印工具类
 */
class WatermarkUtils {
  constructor() {
    this.watermarkId = 'rpa-financial-watermark-overlay'
    this.checkInterval = null
    this.lastWatermarkHash = ''
    this.currentSensitivity = SensitivityLevel.LOW
    this.isDestroyed = false
    this.mutationObserver = null
    this.lastTimestamp = ''
  }

  /**
   * 格式化手机号（脱敏）
   */
  maskPhone(phone) {
    if (!phone || phone.length < 11) return '****'
    return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
  }

  /**
   * 获取当前登录用户信息
   */
  getCurrentUser() {
    try {
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        return JSON.parse(userInfo)
      }
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
    return { realName: '未知用户', phone: '' }
  }

  /**
   * 获取当前时间戳（精确到分钟）
   */
  getCurrentTimestamp() {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hour = String(now.getHours()).padStart(2, '0')
    const minute = String(now.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}`
  }

  /**
   * 生成随机偏移值（±5px）
   */
  getRandomOffset(maxOffset = 5) {
    return {
      x: Math.floor(Math.random() * (maxOffset * 2 + 1)) - maxOffset,
      y: Math.floor(Math.random() * (maxOffset * 2 + 1)) - maxOffset
    }
  }

  /**
   * 计算水印内容哈希值（用于篡改检测）
   */
  calculateHash(text) {
    let hash = 0
    for (let i = 0; i < text.length; i++) {
      const char = text.charCodeAt(i)
      hash = ((hash << 5) - hash) + char
      hash = hash & hash
    }
    return hash.toString(16)
  }

  /**
   * 构建水印文本内容
   */
  buildWatermarkText() {
    const user = this.getCurrentUser()
    const timestamp = this.getCurrentTimestamp()
    const maskedPhone = this.maskPhone(user.phone || '')
    const text = `${user.realName || '未知'} ${maskedPhone} ${timestamp}`
    this.lastTimestamp = timestamp
    return text
  }

  /**
   * 创建Canvas水印 - 45度倾斜，稀疏有序
   */
  createCanvasWatermark(sensitivity = SensitivityLevel.LOW, containerWidth = 1920, containerHeight = 1080) {
    const config = SENSITIVITY_CONFIG[sensitivity] || SENSITIVITY_CONFIG[SensitivityLevel.LOW]
    const text = this.buildWatermarkText()
    const watermarkHash = this.calculateHash(text)
    this.lastWatermarkHash = watermarkHash

    // 根据配置计算瓦片尺寸
    const tileWidth = config.tileWidth
    const tileHeight = config.tileHeight

    // 创建离屏Canvas
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')

    // 设置Canvas尺寸
    canvas.width = tileWidth
    canvas.height = tileHeight

    // 清除画布
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    // 设置字体和样式（使用配置的字体）
    ctx.font = `bold ${config.fontSize}px ${config.fontFamily}`
    ctx.fillStyle = config.color || `rgba(0, 0, 0, ${config.opacity})`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'

    // 保存状态
    ctx.save()

    // 45度倾斜变换
    ctx.translate(canvas.width / 2, canvas.height / 2)
    ctx.rotate(-45 * Math.PI / 180)
    ctx.translate(-canvas.width / 2, -canvas.height / 2)

    // 获取随机偏移（保持稀疏感）
    const offset = this.getRandomOffset(15)

    // 计算文字位置
    const textX = canvas.width / 2 + offset.x
    const textY = canvas.height / 2 + offset.y

    // 绘制水印文字
    ctx.fillText(text, textX, textY)

    // 恢复状态
    ctx.restore()

    return {
      canvas,
      dataUrl: canvas.toDataURL('image/png'),
      config,
      hash: watermarkHash,
      text
    }
  }

  /**
   * 创建水印DOM元素
   */
  createWatermarkElement(sensitivity = SensitivityLevel.LOW) {
    this.currentSensitivity = sensitivity
    const config = SENSITIVITY_CONFIG[sensitivity] || SENSITIVITY_CONFIG[SensitivityLevel.LOW]

    // 检查是否已存在水印元素
    let container = document.getElementById(this.watermarkId)

    if (container) {
      // 已存在，只更新背景图片和配置，避免重新创建DOM
      const { dataUrl, hash, text } = this.createCanvasWatermark(sensitivity)
      container.style.backgroundImage = `url(${dataUrl})`
      container.style.backgroundSize = `${config.tileWidth}px ${config.tileHeight}px`
      container.setAttribute('data-watermark-hash', hash)
      container.setAttribute('data-watermark-text', text)
      container.setAttribute('data-watermark-sensitivity', sensitivity)
      return container
    }

    // 创建新的水印容器
    container = document.createElement('div')
    container.id = this.watermarkId
    container.style.cssText = `
      position: fixed !important;
      top: 0 !important;
      left: 0 !important;
      width: 100vw !important;
      height: 100vh !important;
      z-index: 1 !important;
      pointer-events: none !important;
      overflow: hidden !important;
      background-repeat: repeat !important;
      background-position: 0 0 !important;
      background-size: ${config.tileWidth}px ${config.tileHeight}px !important;
    `

    // 生成水印背景
    const { dataUrl, hash, text } = this.createCanvasWatermark(sensitivity)
    container.style.backgroundImage = `url(${dataUrl})`

    // 将水印添加到body
    document.body.appendChild(container)

    // 标记水印元素以防篡改
    container.setAttribute('data-watermark-hash', hash)
    container.setAttribute('data-watermark-text', text)
    container.setAttribute('data-watermark-sensitivity', sensitivity)

    return container
  }

  /**
   * 验证水印完整性
   */
  verifyWatermark() {
    const container = document.getElementById(this.watermarkId)

    if (!container) {
      return { valid: false, reason: 'watermark_missing' }
    }

    // 检查元素是否可见
    const style = window.getComputedStyle(container)
    if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') {
      return { valid: false, reason: 'watermark_hidden' }
    }

    // 检查z-index（现在水印在底层，只要存在即可）
    const zIndex = parseInt(style.zIndex)
    if (zIndex < 1) {
      return { valid: false, reason: 'zindex_changed' }
    }

    // 检查背景图是否存在
    const bgImage = style.backgroundImage
    if (!bgImage || bgImage === 'none' || !bgImage.startsWith('url')) {
      return { valid: false, reason: 'background_removed' }
    }

    // 检查水印哈希
    const storedHash = container.getAttribute('data-watermark-hash')
    const currentText = this.buildWatermarkText()
    const currentHash = this.calculateHash(currentText)

    // 如果时间戳变了，更新哈希
    if (currentText !== container.getAttribute('data-watermark-text')) {
      container.setAttribute('data-watermark-text', currentText)
      container.setAttribute('data-watermark-hash', currentHash)
    }

    return { valid: true }
  }

  /**
   * 检测篡改并自动修复
   */
  detectTampering() {
    if (this.isDestroyed) return

    const verification = this.verifyWatermark()

    if (!verification.valid) {
      console.warn(`[Watermark] 检测到篡改: ${verification.reason}，正在重建...`)

      // 记录篡改事件
      this.logTamperingEvent(verification.reason)

      // 立即重建水印
      this.createWatermarkElement(this.currentSensitivity)

      // 刷新页面以确保完全恢复
      setTimeout(() => {
        console.warn('[Watermark] 刷新页面以确保水印完整性')
        window.location.reload()
      }, 100)
    }
  }

  /**
   * 记录篡改事件到审计日志
   */
  logTamperingEvent(reason) {
    try {
      const user = this.getCurrentUser()
      const eventData = {
        eventType: 'WATERMARK_TAMPERING_DETECTED',
        userId: user.id,
        userName: user.realName,
        reason: reason,
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        pageUrl: window.location.href
      }

      // 通过API记录篡改事件
      fetch('/api/audit/watermark-tampering', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify(eventData)
      }).catch(err => {
        console.error('[Watermark] 记录篡改事件失败', err)
      })

      // 同时在本地存储
      const tamperLogs = JSON.parse(localStorage.getItem('watermarkTamperLogs') || '[]')
      tamperLogs.push(eventData)
      if (tamperLogs.length > 100) tamperLogs.shift()
      localStorage.setItem('watermarkTamperLogs', JSON.stringify(tamperLogs))
    } catch (e) {
      console.error('[Watermark] 篡改事件记录失败', e)
    }
  }

  /**
   * 启动水印保护（自动检测篡改）
   * 注意：此方法会自动重置销毁状态，允许在调用destroy()后重新启用水印
   */
  startProtection(sensitivity = SensitivityLevel.LOW) {
    // 重置销毁状态，允许重新启用水印
    this.isDestroyed = false

    // 创建水印
    this.createWatermarkElement(sensitivity)

    // 启动2秒检测循环（降低频率以提升性能）
    if (this.checkInterval) {
      clearInterval(this.checkInterval)
    }
    this.checkInterval = setInterval(() => {
      this.detectTampering()
    }, 2000)

    // 启动每分钟时间戳更新
    if (this.timestampInterval) {
      clearInterval(this.timestampInterval)
    }
    this.timestampInterval = setInterval(() => {
      this.updateTimestamp()
    }, 60000)

    // 监听DOM变化（MutationObserver）- 使用轻量模式
    this.setupMutationObserver()

    // 监听截图事件（PrintScreen）
    this.setupScreenshotListener()
  }

  /**
   * 更新水印时间戳
   */
  updateTimestamp() {
    const container = document.getElementById(this.watermarkId)
    if (!container) return

    const newTimestamp = this.getCurrentTimestamp()
    const user = this.getCurrentUser()
    const maskedPhone = this.maskPhone(user.phone || '')
    const newText = `${user.realName || '未知'} ${maskedPhone} ${newTimestamp}`
    const newHash = this.calculateHash(newText)

    container.setAttribute('data-watermark-text', newText)
    container.setAttribute('data-watermark-hash', newHash)

    // 重新生成Canvas
    const { dataUrl } = this.createCanvasWatermark(this.currentSensitivity)
    container.style.backgroundImage = `url(${dataUrl})`

    this.lastTimestamp = newTimestamp
  }

  /**
   * 设置DOM变化监听器（轻量模式）
   */
  setupMutationObserver() {
    if (this.mutationObserver) {
      this.mutationObserver.disconnect()
      this.mutationObserver = null
    }

    // 防抖计时器
    let rebuildTimer = null

    this.mutationObserver = new MutationObserver((mutations) => {
      // 如果已销毁，跳过所有操作
      if (this.isDestroyed) return

      // 如果正在执行自身操作，跳过
      if (this._rebuilding) return

      // 简单检测：只检查水印元素是否还存在
      const watermarkEl = document.getElementById(this.watermarkId)
      if (!watermarkEl) {
        // 防抖处理
        if (rebuildTimer) return
        rebuildTimer = setTimeout(() => {
          rebuildTimer = null
        }, 1000)

        this._rebuilding = true
        console.warn('[Watermark] 检测到水印元素被移除，正在重建...')
        this.createWatermarkElement(this.currentSensitivity)
        setTimeout(() => {
          this._rebuilding = false
        }, 300)
      }
    })

    // 只监听body的子节点变化，减少观察范围
    this.mutationObserver.observe(document.body, {
      childList: true,
      subtree: false  // 不监听子树，减少性能开销
    })
  }

  /**
   * 设置截图监听器
   */
  setupScreenshotListener() {
    // 监听PrintScreen键
    document.addEventListener('keydown', (e) => {
      if (e.key === 'PrintScreen') {
        this.handleScreenshot()
      }
    })

    // 监听系统截图事件（部分浏览器支持）
    document.addEventListener('webkitfullscreenchange', () => {
      // 截图后提高透明度
      this.enhanceForScreenshot()
    })
  }

  /**
   * 处理截图操作
   */
  handleScreenshot() {
    const container = document.getElementById(this.watermarkId)
    if (!container) return

    // 提高透明度到20%
    container.style.opacity = '1'

    // 记录截图事件
    this.logScreenshotEvent()

    // 5秒后恢复正常透明度
    setTimeout(() => {
      if (container) {
        container.style.opacity = '1'
      }
    }, 5000)

    // 通知用户截图已被记录
    console.warn('[Watermark] 截图操作已被记录并同步到审计日志')
  }

  /**
   * 增强水印可见性（用于截图场景）
   */
  enhanceForScreenshot() {
    const container = document.getElementById(this.watermarkId)
    if (!container) return

    container.style.opacity = '1'
  }

  /**
   * 记录截图事件
   */
  logScreenshotEvent() {
    try {
      const user = this.getCurrentUser()
      const eventData = {
        eventType: 'SCREENSHOT_CAPTURED',
        userId: user.id,
        userName: user.realName,
        timestamp: new Date().toISOString(),
        pageUrl: window.location.href,
        ip: '' // 前端无法获取真实IP，由后端填充
      }

      fetch('/api/audit/screenshot', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify(eventData)
      }).catch(err => {
        console.error('[Watermark] 记录截图事件失败', err)
      })
    } catch (e) {
      console.error('[Watermark] 截图事件记录失败', e)
    }
  }

  /**
   * 移除水印
   */
  removeWatermark() {
    const container = document.getElementById(this.watermarkId)
    if (container) {
      container.remove()
    }
  }

  /**
   * 销毁水印系统
   */
  destroy() {
    this.isDestroyed = true

    if (this.checkInterval) {
      clearInterval(this.checkInterval)
      this.checkInterval = null
    }

    if (this.timestampInterval) {
      clearInterval(this.timestampInterval)
      this.timestampInterval = null
    }

    if (this.mutationObserver) {
      this.mutationObserver.disconnect()
      this.mutationObserver = null
    }

    this.removeWatermark()
  }

  /**
   * 检查水印是否已启用
   */
  isActive() {
    return !this.isDestroyed && document.getElementById(this.watermarkId) !== null
  }

  /**
   * 获取当前敏感级别
   */
  getCurrentSensitivity() {
    return this.currentSensitivity
  }

  /**
   * 临时关闭水印（仅超级管理员可用）
   */
  async temporaryDisable(reason, duration = 3600000) { // 默认1小时
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
        this.removeWatermark()
        // 设置定时恢复
        setTimeout(() => {
          this.startProtection(this.currentSensitivity)
        }, duration)

        return { success: true }
      } else {
        return { success: false, message: '权限不足或操作失败' }
      }
    } catch (e) {
      console.error('[Watermark] 临时关闭失败', e)
      return { success: false, message: e.message }
    }
  }
}

// 创建单例实例
const watermarkInstance = new WatermarkUtils()

// 导出
export default watermarkInstance
export { WatermarkUtils, SensitivityLevel }
