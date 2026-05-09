import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN.js'
import enUS from './en-US.js'

// 从 localStorage 读取保存的语言设置
const savedLocale = localStorage.getItem('locale') || 'zh-CN'

// 创建 i18n 实例
const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: savedLocale,
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

// 切换语言的辅助函数
export function setLocale(locale) {
  if (i18n.global.locale) {
    i18n.global.locale.value = locale
  } else {
    i18n.global.locale = locale
  }
  localStorage.setItem('locale', locale)
  document.documentElement.setAttribute('lang', locale)
}

// 获取当前语言
export function getLocale() {
  return i18n.global.locale?.value || i18n.global.locale || 'zh-CN'
}

export default i18n
