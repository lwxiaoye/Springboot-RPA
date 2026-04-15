---
# UI 风格指南 (67 Styles)
---

## 通用风格 (49)

### 1. Minimalism & Swiss Style
- **适用场景**: 企业应用、仪表盘、文档系统
- **特征**: 清晰排版、网格系统、大量留白、高对比度
- **颜色**: 单色为主 + 一个强调色
- **字体**: Inter + Playfair Display

### 2. Neumorphism (新拟态)
- **适用场景**: 健康/冥想应用、医疗保健平台
- **特征**: 柔和阴影、凸起/凹陷效果
- **避免**: 深色背景、高对比度边框

### 3. Glassmorphism (玻璃态)
- **适用场景**: 现代 SaaS、金融仪表盘
- **特征**: 背景模糊、半透明、边框高光
- **性能**: 谨慎使用，影响渲染性能

### 4. Brutalism (野兽派)
- **适用场景**: 设计作品集、艺术项目
- **特征**: 粗边框、原始排版、高饱和度
- **警告**: 不适合企业应用

### 5. 3D & Hyperrealism
- **适用场景**: 游戏、产品展示、沉浸式体验
- **技术**: Three.js, Spline, 高质量渲染

### 6. Bento Grid (便当盒网格)
- **适用场景**: 仪表盘、功能展示、产品页面
- **灵感**: Apple 设计语言
- **特点**: 圆角卡片、自适应网格

### 7. Dark Mode (OLED)
- **适用场景**: 开发者工具、夜间应用
- **原则**: 纯黑背景 (#000) + 低饱和度强调色

### 8. AI-Native UI
- **适用场景**: AI 产品、聊天机器人、Copilot
- **特征**: 流式输出、对话式界面、渐进式渲染

## Vue 框架适配

### 样式实现方式

```vue
<!-- 使用 CSS 变量 -->
<template>
  <div class="glass-card">
    <slot />
  </div>
</template>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
}
</style>
```

### Element Plus 主题定制

```javascript
// theme-changes.js
import { ElConfigProvider } from 'element-plus'

const customTheme = {
  token: {
    colorPrimary: '#409EFF',
    borderRadius: '8px',
  }
}
```

## 风格选择流程图

```
产品类型 → 目标受众 → 使用场景 → 风格推荐
  ↓
企业级应用? → Yes → Swiss Style / Soft UI
  ↓
面向消费者? → Yes → Vibrant / Bento Grid
  ↓
创意作品集? → Yes → Brutalism / 3D
```
