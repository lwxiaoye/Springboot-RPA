---
# 配色方案指南
---

## 通用配色原则

### 70-20-10 法则
- **70%** - 主色（背景、大面积）
- **20%** - 辅助色（次要元素）
- **10%** - 强调色（CTA、关键操作）

### 无障碍对比度
- 普通文本: ≥ 4.5:1
- 大文本 (≥18pt): ≥ 3:1
- UI 组件: ≥ 3:1
- 工具: https://webaim.org/resources/contrastchecker/

## Element Plus 默认色板

```javascript
const colors = {
  primary: '#409EFF',    // 主色 - 蓝
  success: '#67C23A',    // 成功 - 绿
  warning: '#E6A23C',    // 警告 - 黄
  danger: '#F56C6C',     // 危险 - 红
  info: '#909399'        // 信息 - 灰
}
```

## 行业配色方案

### 1. 企业级 SaaS
```css
:root {
  --primary: #2563EB;      /* 专业蓝 */
  --secondary: #64748B;    /* 石板灰 */
  --accent: #10B981;       /* 翡翠绿 */
  --background: #F8FAFC;   /* 浅灰白 */
  --surface: #FFFFFF;      /* 纯白 */
  --text-primary: #1E293B;  /* 深灰 */
  --text-secondary: #64748B;
}
```

### 2. 金融/银行
```css
:root {
  --primary: #1E40AF;      /* 深海蓝 */
  --secondary: #475569;    /* 炭灰 */
  --accent: #D97706;       /* 古铜金 */
  --background: #F1F5F9;   /* 浅蓝灰 */
  --surface: #FFFFFF;
  --success: #059669;      /* 金融绿 */
  --danger: #DC2626;       /* 警示红 */
}
```

### 3. 电商/零售
```css
:root {
  --primary: #7C3AED;      /* 紫罗兰 */
  --secondary: #EC4899;    /* 粉红 */
  --accent: #F59E0B;       /* 琥珀 */
  --background: #FAFAFA;
  --surface: #FFFFFF;
  --cta: #EF4444;         /* 行动红 */
}
```

### 4. 健康/医疗
```css
:root {
  --primary: #0891B2;      /* 青蓝 */
  --secondary: #06B6D4;    /* 青色 */
  --accent: #10B981;       /* 薄荷绿 */
  --background: #F0FDFA;   /* 淡青白 */
  --surface: #FFFFFF;
  --text-primary: #134E4A;
}
```

### 5. 创意/设计
```css
:root {
  --primary: #6366F1;      /* 靛蓝 */
  --secondary: #8B5CF6;   /* 紫罗兰 */
  --accent: #EC4899;       /* 粉红 */
  --background: #0F172A;   /* 深空蓝 */
  --surface: #1E293B;
  --text: #F8FAFC;
}
```

## Vue 中的实现

```vue
<!-- ColorScheme.vue -->
<script setup>
// 动态主题切换
const theme = ref('light')

const setTheme = (newTheme) => {
  theme.value = newTheme
  document.documentElement.setAttribute('data-theme', newTheme)
}
</script>

<template>
  <el-config-provider :theme="theme">
    <router-view />
  </el-config-provider>
</template>

<style>
:root {
  /* 亮色主题 */
  --bg-primary: #ffffff;
  --bg-secondary: #f5f7fa;
  --text-primary: #303133;
  --text-secondary: #606266;
}

[data-theme="dark"] {
  --bg-primary: #1a1a1a;
  --bg-secondary: #2d2d2d;
  --text-primary: #e5eaf3;
  --text-secondary: #909399;
}
</style>
```

## 渐变配色

### 现代 SaaS 渐变
```css
.gradient-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.gradient-hero {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.gradient-glass {
  background: linear-gradient(135deg, 
    rgba(255, 255, 255, 0.1),
    rgba(255, 255, 255, 0.05)
  );
}
```

## 语义化颜色变量

```scss
// semantic-colors.scss
:root {
  // 交互状态
  --color-interactive: var(--primary);
  --color-interactive-hover: var(--primary-light-3);
  --color-interactive-active: var(--primary-dark-2);
  --color-interactive-disabled: var(--primary-light-5);
  
  // 反馈状态
  --color-success-bg: #f0f9eb;
  --color-warning-bg: #fdf6ec;
  --color-danger-bg: #fef0f0;
  --color-info-bg: #f4f4f5;
  
  // 边框
  --border-color: #dcdfe6;
  --border-color-light: #e4e7ed;
  --border-color-lighter: #ebeef5;
}
```
