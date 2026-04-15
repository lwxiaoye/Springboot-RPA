---
# 字体搭配指南
---

## 字体选择原则

### 可读性优先
- 正文字号: 14-16px
- 行高: 1.5-1.75
- 标题字重: 600-700
- 避免纯黑色 (#000) 用于正文

### 层次分明
- H1: 32-48px, font-weight: 700
- H2: 24-32px, font-weight: 600
- H3: 18-24px, font-weight: 600
- Body: 14-16px, font-weight: 400
- Caption: 12-14px, font-weight: 400

## Google Fonts 搭配

### 1. 现代企业风格
```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
```
```css
font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
```
**适用**: SaaS、仪表盘、企业应用

### 2. 优雅专业风格
```html
<link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600;700&family=Source+Sans+3:wght@400;500;600&display=swap" rel="stylesheet">
```
```css
h1, h2, h3 { font-family: 'Playfair Display', serif; }
body { font-family: 'Source Sans 3', sans-serif; }
```
**适用**: 高端品牌、奢华电商、媒体

### 3. 科技开发者风格
```html
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
```
```css
code, pre { font-family: 'JetBrains Mono', monospace; }
body { font-family: 'Inter', sans-serif; }
```
**适用**: 开发者工具、代码平台、文档

### 4. 简洁亚洲风格
```html
<link href="https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&family=Noto+Serif+SC:wght@600;700&display=swap" rel="stylesheet">
```
```css
h1, h2 { font-family: 'Noto Serif SC', serif; }
body { font-family: 'Noto Sans SC', sans-serif; }
```
**适用**: 中文企业、中高端网站

### 5. 活力创意风格
```html
<link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&family=Open+Sans:wght@400;500;600&display=swap" rel="stylesheet">
```
```css
h1, h2, h3 { font-family: 'Poppins', sans-serif; }
body { font-family: 'Open Sans', sans-serif; }
```
**适用**: 创意机构、初创公司

## 中文项目推荐

### 企业级中文
```html
<link href="https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap">
```
```css
body {
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
```

### 正式文档
```html
<link href="https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@600;700&display=swap">
```
```css
h1, h2 {
  font-family: 'Noto Serif SC', 'Songti SC', serif;
}
```

## Vue 中的字体配置

### main.js / main.ts
```javascript
import { createApp } from 'vue'
import App from './App.vue'

// 全局字体样式
const style = document.createElement('style')
style.textContent = `
  :root {
    --font-sans: 'Inter', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
    --font-serif: 'Playfair Display', 'Noto Serif SC', serif;
    --font-mono: 'JetBrains Mono', 'Fira Code', monospace;
  }
  
  * {
    font-family: var(--font-sans);
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }
`
document.head.appendChild(style)

createApp(App).mount('#app')
```

### CSS 变量应用
```scss
// styles/typography.scss
:root {
  --font-size-xs: 12px;
  --font-size-sm: 14px;
  --font-size-base: 16px;
  --font-size-lg: 18px;
  --font-size-xl: 20px;
  --font-size-2xl: 24px;
  --font-size-3xl: 30px;
  --font-size-4xl: 36px;
  
  --line-height-tight: 1.25;
  --line-height-normal: 1.5;
  --line-height-relaxed: 1.75;
}

body {
  font-size: var(--font-size-base);
  line-height: var(--line-height-normal);
}

h1 { font-size: var(--font-size-4xl); line-height: var(--line-height-tight); }
h2 { font-size: var(--font-size-3xl); line-height: var(--line-height-tight); }
h3 { font-size: var(--font-size-2xl); }
h4 { font-size: var(--font-size-xl); }
```

## 响应式字体

```scss
// 响应式字号
html {
  font-size: 16px;
}

@media (max-width: 768px) {
  html { font-size: 14px; }
}

@media (max-width: 375px) {
  html { font-size: 12px; }
}

// 使用 rem 单位
.card-title {
  font-size: 1.5rem;     /* 24px */
}

.card-body {
  font-size: 1rem;       /* 16px */
}

.card-footer {
  font-size: 0.875rem;   /* 14px */
}
```

## 字体加载优化

### 字体子集化
```html
<!-- 只加载需要的字符 -->
<link href="https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500&display=swap&subset=latin,chinese-simplified">
```

### 预加载关键字体
```html
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="preload" href="/fonts/inter-var.woff2" as="font" type="font/woff2" crossorigin>
```

## 设计系统中的字体

```javascript
// design-tokens.js
export const typography = {
  fontFamily: {
    sans: "'Inter', 'Noto Sans SC', sans-serif",
    serif: "'Playfair Display', serif",
    mono: "'JetBrains Mono', monospace"
  },
  fontSize: {
    xs: '0.75rem',    // 12px
    sm: '0.875rem',   // 14px
    base: '1rem',     // 16px
    lg: '1.125rem',   // 18px
    xl: '1.25rem',    // 20px
    '2xl': '1.5rem',   // 24px
    '3xl': '1.875rem', // 30px
    '4xl': '2.25rem',  // 36px
  },
  fontWeight: {
    normal: 400,
    medium: 500,
    semibold: 600,
    bold: 700
  },
  lineHeight: {
    tight: 1.25,
    normal: 1.5,
    relaxed: 1.75
  }
}
```
