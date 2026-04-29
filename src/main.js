import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 全局表格Hover样式
import './styles/table-hover.css'

// 全局 ECharts 注册（必须在 vue-echarts 之前）
import './utils/echarts.ts'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus)

// 全局水印管理器（仅在非登录页面初始化）
import globalWatermarkManager from './utils/watermark/GlobalWatermarkManager.js'

app.mount('#app')

// 应用挂载后初始化水印系统
router.isReady().then(() => {
  // 检查是否登录页面
  const isLoginPage = router.currentRoute.value.path === '/login'

  if (!isLoginPage) {
    // 延迟初始化，确保所有组件已加载
    setTimeout(() => {
      globalWatermarkManager.init()
    }, 100)
  }
})