/**
 * 打印水印增强模块
 *
 * 功能：
 * 1. 打印时自动添加全屏水印
 * 2. 追加打印人、打印时间、IP地址
 * 3. 与现有打印功能无缝集成
 *
 * @author RPA Security Team
 * @version 1.0.0
 */

import { PRINT_WATERMARK_CONFIG } from './watermarkConfig.js'

/**
 * 打印水印工具类
 */
class PrintWatermark {
  constructor() {
    this.printWatermarkId = 'rpa-print-watermark-overlay'
    this.isPrintMode = false
    this.originalBeforePrint = null
    this.originalAfterPrint = null
  }

  /**
   * 获取当前用户信息
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
   * 格式化手机号（脱敏）
   */
  maskPhone(phone) {
    if (!phone || phone.length < 11) return '****'
    return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
  }

  /**
   * 获取当前时间戳
   */
  getCurrentTimestamp() {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hour = String(now.getHours()).padStart(2, '0')
    const minute = String(now.getMinutes()).padStart(2, '0')
    const second = String(now.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}:${second}`
  }

  /**
   * 获取IP地址（需要后端支持）
   */
  async getClientIP() {
    try {
      const response = await fetch('/api/user/ip')
      if (response.ok) {
        const data = await response.json()
        return data.ip || '未知'
      }
    } catch (e) {
      console.error('获取IP失败', e)
    }
    return '未知'
  }

  /**
   * 构建打印水印文本
   */
  buildPrintWatermarkText() {
    const user = this.getCurrentUser()
    const timestamp = this.getCurrentTimestamp()
    const maskedPhone = this.maskPhone(user.phone || '')
    const userName = user.realName || '未知'

    // 替换模板中的占位符
    let text = PRINT_WATERMARK_CONFIG.watermarkText
      .replace('{userName}', userName)
      .replace('{phone}', maskedPhone)
      .replace('{timestamp}', timestamp)
      .replace('{printUser}', userName)
      .replace('{printTime}', timestamp)

    return text
  }

  /**
   * 创建Canvas打印水印
   */
  createPrintCanvas(text) {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')

    // A4纸张大小（300 DPI）
    const width = 2480
    const height = 3508

    canvas.width = width
    canvas.height = height

    // 清除画布
    ctx.clearRect(0, 0, width, height)

    // 设置字体
    ctx.font = `${PRINT_WATERMARK_CONFIG.fontSize * 3}px "Microsoft YaHei", "PingFang SC", sans-serif`
    ctx.fillStyle = `rgba(128, 128, 128, ${PRINT_WATERMARK_CONFIG.opacity})`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'

    // 平铺水印
    const tileWidth = 400
    const tileHeight = 200

    for (let y = 0; y < height; y += tileHeight) {
      for (let x = 0; x < width; x += tileWidth) {
        ctx.save()

        // 应用45度旋转
        ctx.translate(x + tileWidth / 2, y + tileHeight / 2)
        ctx.rotate(-45 * Math.PI / 180)

        // 绘制文字
        ctx.fillText(text, 0, 0)

        ctx.restore()
      }
    }

    return canvas
  }

  /**
   * 添加打印水印到页面
   */
  addPrintWatermark() {
    // 移除已存在的打印水印
    this.removePrintWatermark()

    // 构建水印文本
    const text = this.buildPrintWatermarkText()
    const canvas = this.createPrintCanvas(text)

    // 创建水印容器
    const container = document.createElement('div')
    container.id = this.printWatermarkId
    container.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      z-index: 999999;
      pointer-events: none;
      opacity: 1;
    `

    // 创建水印图片
    const img = document.createElement('img')
    img.src = canvas.toDataURL('image/png')
    img.style.cssText = `
      width: 100%;
      height: 100%;
      object-fit: cover;
    `

    container.appendChild(img)
    document.body.appendChild(container)

    this.isPrintMode = true
    console.log('[PrintWatermark] 打印水印已添加')
  }

  /**
   * 移除打印水印
   */
  removePrintWatermark() {
    const container = document.getElementById(this.printWatermarkId)
    if (container) {
      container.remove()
    }
    this.isPrintMode = false
    console.log('[PrintWatermark] 打印水印已移除')
  }

  /**
   * 记录打印事件到审计日志
   */
  async logPrintEvent(pageUrl) {
    try {
      const user = this.getCurrentUser()
      const ip = await this.getClientIP()

      const eventData = {
        eventType: 'PRINT_ACTION',
        userId: user.id,
        userName: user.realName,
        timestamp: new Date().toISOString(),
        pageUrl: pageUrl,
        ip: ip
      }

      await fetch('/api/audit/print', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify(eventData)
      })

      console.log('[PrintWatermark] 打印事件已记录')
    } catch (e) {
      console.error('[PrintWatermark] 记录打印事件失败', e)
    }
  }

  /**
   * 拦截打印操作
   */
  setupPrintInterception() {
    // 保存原始函数
    this.originalBeforePrint = window.onbeforeprint
    this.originalAfterPrint = window.onafterprint

    // 设置beforeprint回调
    window.onbeforeprint = async (event) => {
      console.log('[PrintWatermark] 检测到打印操作')

      // 添加打印水印
      this.addPrintWatermark()

      // 记录打印事件
      await this.logPrintEvent(window.location.href)

      // 调用原始回调
      if (this.originalBeforePrint) {
        this.originalBeforePrint.call(window, event)
      }
    }

    // 设置afterprint回调
    window.onafterprint = (event) => {
      // 移除打印水印
      setTimeout(() => {
        this.removePrintWatermark()
      }, 100)

      // 调用原始回调
      if (this.originalAfterPrint) {
        this.originalAfterPrint.call(window, event)
      }
    }

    // 监听@media print事件
    const mediaQueryList = window.matchMedia('print')
    mediaQueryList.addEventListener('change', (mql) => {
      if (mql.matches) {
        console.log('[PrintWatermark] 打印媒体查询触发')
        this.addPrintWatermark()
      } else {
        this.removePrintWatermark()
      }
    })

    console.log('[PrintWatermark] 打印拦截已设置')
  }

  /**
   * 销毁打印拦截
   */
  destroy() {
    window.onbeforeprint = this.originalBeforePrint
    window.onafterprint = this.originalAfterPrint
    this.removePrintWatermark()
    console.log('[PrintWatermark] 打印拦截已销毁')
  }
}

// 创建单例实例
const printWatermarkInstance = new PrintWatermark()

export default printWatermarkInstance
export { PrintWatermark }
