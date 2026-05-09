/**
 * 水印配置文件 - 金融行业分级配置
 *
 * 按页面敏感级别自动切换水印参数，严格遵循银保监会数据安全要求
 *
 * @author RPA Security Team
 * @version 1.0.0
 */

import { SensitivityLevel } from './WatermarkUtils.js'
export { SensitivityLevel }

/**
 * 敏感级别详细配置
 */
export const WATERMARK_CONFIG = {
  [SensitivityLevel.EXTREME]: {
    level: SensitivityLevel.EXTREME,
    levelName: '极高敏感',
    description: '执行日志、审计日志、凭证中心、数据查询',
    opacity: 0.5,
    fontSize: 18,
    fontFamily: '"KaiTi", "楷体", "STKaiti", "Microsoft YaHei", sans-serif',
    color: '#F5F5F5',
    tileWidth: 400,
    tileHeight: 300,
    rotation: -45,
    randomOffset: true,
    showTimestamp: true,
    timestampFormat: 'YYYY-MM-DD HH:mm',
    pages: [
      '/rpa/logs',
      '/rpa/audit',
      '/rpa/credentials',
      '/rpa/data-query'
    ]
  },
  [SensitivityLevel.HIGH]: {
    level: SensitivityLevel.HIGH,
    levelName: '高敏感',
    description: '任务调度中心、机器人管理、队列管理、触发器管理、数据脱敏',
    opacity: 0.5,
    fontSize: 18,
    fontFamily: '"KaiTi", "楷体", "STKaiti", "Microsoft YaHei", sans-serif',
    color: '#F5F5F5',
    tileWidth: 400,
    tileHeight: 300,
    rotation: -45,
    randomOffset: true,
    showTimestamp: true,
    timestampFormat: 'YYYY-MM-DD HH:mm',
    pages: [
      '/rpa/tasks',
      '/rpa/robots',
      '/rpa/queues',
      '/rpa/triggers',
      '/rpa/masking'
    ]
  },
  [SensitivityLevel.MEDIUM]: {
    level: SensitivityLevel.MEDIUM,
    levelName: '中敏感',
    description: '流程仓库、脚本执行、分布式锁、报表分析',
    opacity: 0.5,
    fontSize: 18,
    fontFamily: '"KaiTi", "楷体", "STKaiti", "Microsoft YaHei", sans-serif',
    color: '#F5F5F5',
    tileWidth: 450,
    tileHeight: 320,
    rotation: -45,
    randomOffset: true,
    showTimestamp: true,
    timestampFormat: 'YYYY-MM-DD HH:mm',
    pages: [
      '/rpa/processes',
      '/rpa/script',
      '/rpa/locks',
      '/rpa/reports'
    ]
  },
  [SensitivityLevel.LOW]: {
    level: SensitivityLevel.LOW,
    levelName: '低敏感',
    description: '系统设置、通知管理',
    opacity: 0.5,
    fontSize: 18,
    fontFamily: '"KaiTi", "楷体", "STKaiti", "Microsoft YaHei", sans-serif',
    color: '#F5F5F5',
    tileWidth: 500,
    tileHeight: 350,
    rotation: -45,
    randomOffset: true,
    showTimestamp: true,
    timestampFormat: 'YYYY-MM-DD HH:mm',
    pages: [
      '/rpa/settings',
      '/rpa/notifications',
      '/rpa/collaboration',
      '/system/profile',
      '/system/users',
      '/system/roles',
      '/system/resources'
    ]
  }
}

/**
 * 默认配置（低敏感级别）
 */
export const DEFAULT_WATERMARK_CONFIG = WATERMARK_CONFIG[SensitivityLevel.LOW]

/**
 * 基础样式常量
 */
export const BASE_WATERMARK_STYLES = {
  zIndex: 1,
  pointerEvents: 'none',
  position: 'fixed',
  top: 0,
  left: 0,
  width: '100vw',
  height: '100vh'
}

/**
 * 防篡改配置
 */
export const ANTI_TAMPER_CONFIG = {
  checkInterval: 2000,           // 检测间隔（毫秒）
  maxRetryAttempts: 3,          // 最大重试次数
  timestampUpdateInterval: 60000, // 时间戳更新间隔（毫秒）
  randomOffsetRange: 5          // 随机偏移范围（像素）
}

/**
 * 截图增强配置
 */
export const SCREENSHOT_ENHANCE_CONFIG = {
  enabled: true,
  opacityBoost: 0.20,          // 截图时透明度提升至20%
  boostDuration: 5000,         // 增强持续时间（毫秒）
  recordScreenshot: true        // 是否记录截图行为
}

/**
 * 权限控制配置
 */
export const PERMISSION_CONFIG = {
  canDisableTemporarily: ['ROLE_ADMIN'], // 可临时关闭水印的角色
  temporaryDisableDuration: 3600000,       // 临时关闭时长（1小时）
  requireReason: true,                     // 是否需要填写原因
  requireApproval: false,                  // 是否需要审批
  autoRestore: true                        // 是否自动恢复
}

/**
 * 审计日志配置
 */
export const AUDIT_CONFIG = {
  enabled: true,
  logTypes: {
    WATERMARK_TAMPERING: 'watermark_tampering',
    WATERMARK_DISABLED: 'watermark_disabled',
    WATERMARK_ENABLED: 'watermark_enabled',
    SCREENSHOT_CAPTURED: 'screenshot_captured',
    PRINT_ACTION: 'print_action',
    EXPORT_WITH_WATERMARK: 'export_with_watermark'
  }
}

/**
 * 导出水印配置
 */
export const EXPORT_WATERMARK_CONFIG = {
  excel: {
    enabled: true,
    position: 'diagonal',    // diagonal: 对角线平铺
    opacity: 0.15,
    fontSize: 10,
    color: '#808080'
  },
  pdf: {
    enabled: true,
    position: 'diagonal',
    opacity: 0.12,
    fontSize: 10,
    color: '#808080'
  },
  csv: {
    enabled: true,
    headerWatermark: true,   // 在第一行添加水印信息
    format: '[水印: {userName} {phone} {timestamp}]'
  }
}

/**
 * 打印水印配置
 */
export const PRINT_WATERMARK_CONFIG = {
  enabled: true,
  opacity: 0.20,
  fontSize: 12,
  includePrintInfo: true,     // 包含打印人、打印时间、IP地址
  watermarkText: '{userName} {phone} {timestamp} | 打印人: {printUser} | 打印时间: {printTime} | IP: {ip}'
}

/**
 * 页面路径到敏感级别的映射
 */
export const PAGE_SENSITIVITY_MAP = {
  // 极高敏感页面
  '/rpa/logs': SensitivityLevel.EXTREME,
  '/rpa/audit': SensitivityLevel.EXTREME,
  '/rpa/credentials': SensitivityLevel.EXTREME,
  '/rpa/data-query': SensitivityLevel.EXTREME,
  '/rpa/credential-vault': SensitivityLevel.EXTREME,

  // 高敏感页面
  '/rpa/tasks': SensitivityLevel.HIGH,
  '/rpa/robots': SensitivityLevel.HIGH,
  '/rpa/queues': SensitivityLevel.HIGH,
  '/rpa/triggers': SensitivityLevel.HIGH,
  '/rpa/masking': SensitivityLevel.HIGH,
  '/rpa/data-masking': SensitivityLevel.HIGH,
  '/rpa/queue-trigger': SensitivityLevel.HIGH,

  // 中敏感页面
  '/rpa/processes': SensitivityLevel.MEDIUM,
  '/rpa/script': SensitivityLevel.MEDIUM,
  '/rpa/locks': SensitivityLevel.MEDIUM,
  '/rpa/reports': SensitivityLevel.MEDIUM,
  '/rpa/report-analytics': SensitivityLevel.MEDIUM,
  '/rpa/script-executor': SensitivityLevel.MEDIUM,
  '/rpa/distributed-lock': SensitivityLevel.MEDIUM,

  // 低敏感页面
  '/rpa/settings': SensitivityLevel.LOW,
  '/rpa/system-settings': SensitivityLevel.LOW,
  '/rpa/notifications': SensitivityLevel.LOW,
  '/rpa/notification-center': SensitivityLevel.LOW,
  '/rpa/collaboration': SensitivityLevel.LOW,
  '/rpa/ai': SensitivityLevel.LOW,
  '/rpa/ai-center': SensitivityLevel.LOW,
  '/rpa/workbench': SensitivityLevel.LOW,
  '/dashboard': SensitivityLevel.LOW,
  '/system/profile': SensitivityLevel.LOW,
  '/system/users': SensitivityLevel.LOW,
  '/system/roles': SensitivityLevel.LOW,
  '/system/resources': SensitivityLevel.LOW
}

/**
 * 不需要水印的页面（白名单）
 */
export const EXCLUDED_PAGES = [
  '/rpa/monitor',
  '/rpa/real-time-monitor',
  '/rpa/control-center',
  '/dashboard',
  '/rpa/workbench',
  '/login',
  '/404',
  '/500'
]

/**
 * 根据路径获取敏感级别
 * @param {string} path - 页面路径
 * @returns {string} 敏感级别
 */
export function getSensitivityByPath(path) {
  if (!path) return SensitivityLevel.LOW

  // 检查是否在排除列表中
  if (EXCLUDED_PAGES.some(page => path.startsWith(page))) {
    return null
  }

  // 精确匹配
  if (PAGE_SENSITIVITY_MAP[path]) {
    return PAGE_SENSITIVITY_MAP[path]
  }

  // 前缀匹配
  for (const [pattern, level] of Object.entries(PAGE_SENSITIVITY_MAP)) {
    if (path.startsWith(pattern)) {
      return level
    }
  }

  // 默认低敏感
  return SensitivityLevel.LOW
}

/**
 * 获取页面水印配置
 * @param {string} path - 页面路径
 * @returns {object} 水印配置
 */
export function getWatermarkConfigByPath(path) {
  const sensitivity = getSensitivityByPath(path)
  if (!sensitivity) return null
  return WATERMARK_CONFIG[sensitivity] || DEFAULT_WATERMARK_CONFIG
}

/**
 * 检查页面是否需要水印
 * @param {string} path - 页面路径
 * @returns {boolean} 是否需要水印
 */
export function shouldApplyWatermark(path) {
  return getSensitivityByPath(path) !== null
}

/**
 * 检查是否是实时监控页面
 * @param {string} path - 页面路径
 * @returns {boolean} 是否是实时监控页面
 */
export function isMonitorPage(path) {
  return path && (
    path.includes('/monitor') ||
    path.includes('/real-time')
  )
}
