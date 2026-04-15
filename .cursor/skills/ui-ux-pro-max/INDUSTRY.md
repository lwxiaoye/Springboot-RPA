---
# 行业设计规则 (161 Categories)
---

## 技术 & SaaS

### SaaS Dashboard
- **推荐模式**: 数据密集型仪表盘
- **样式优先级**: Swiss Style + Bento Grid
- **颜色**: 深蓝/灰色系 + 亮色强调
- **字体**: Inter + JetBrains Mono (代码)
- **关键效果**: 悬停卡片高亮、平滑数据更新
- **反模式**: 过度动画、鲜艳渐变背景

### AI/Chatbot Platform
- **推荐模式**: 对话式界面 + 侧边栏
- **样式优先级**: AI-Native UI + Minimalism
- **颜色**: 紫色系 + 中性背景
- **字体**: Inter + Source Sans Pro
- **关键效果**: 流式文本动画、打字机效果
- **反模式**: 模拟人类拟人化过度

## 金融科技

### Fintech / Crypto
- **推荐模式**: 信任优先 + 清晰数据展示
- **样式优先级**: Glassmorphism + Swiss Style
- **颜色**: 深蓝/黑 + 金色/绿色强调
- **字体**: Roboto + Inter
- **关键效果**: 微交互确认、数字滚动动画
- **反模式**: AI 粉紫渐变、过度装饰

### Banking
- **推荐模式**: 传统与现代平衡
- **样式优先级**: Minimalism + Soft UI
- **颜色**: 蓝色系 (信任) + 白色背景
- **字体**: SF Pro / Roboto (清晰易读)
- **必须**: WCAG AA 无障碍标准

## 电商

### E-commerce General
- **推荐模式**: Hero + 产品网格 + CTA
- **样式优先级**: Bento Grid + Vibrant
- **颜色**: 品牌色 + 高对比度 CTA 按钮
- **字体**: 无衬线标题 + 易读正文字体
- **关键效果**: 产品悬停放大、快速查看

### Luxury E-commerce
- **推荐模式**: Hero-Centric + 沉浸式体验
- **样式优先级**: 3D + Minimalism
- **颜色**: 黑白金 + 少量强调色
- **字体**: 优雅衬线体 (Playfair, Cormorant)

## 健康医疗

### Medical Clinic
- **推荐模式**: 清晰导航 + 信息分层
- **样式优先级**: Soft UI + Accessible
- **颜色**: 蓝绿色系 (医疗感)
- **字体**: 超大字号、高可读性
- **必须**: 无障碍合规 (WCAG AAA)

### Mental Health
- **推荐模式**: 平静、安抚式界面
- **样式优先级**: Organic Biophilic + Claymorphism
- **颜色**: 柔和的自然色调
- **字体**: 圆润友好字体

## 创意类

### Portfolio
- **推荐模式**: 作品展示优先
- **样式优先级**: Brutalism / Minimalism / 3D
- **特点**: 大胆排版、创意布局

### Photography
- **推荐模式**: 图片最大化
- **样式优先级**: Minimalism + Dark Mode
- **颜色**: 深色背景 (#0a0a0a)
- **必须**: 图片懒加载、画廊视图

## 仪表盘类型

### Executive Dashboard
- **核心指标**: 3-5 个 KPI 卡片
- **图表**: 折线图、柱状图、饼图
- **布局**: 顶部摘要 + 网格布局
- **交互**: 下钻详情、日期筛选

### Real-Time Monitoring
- **更新频率**: 秒级刷新
- **可视化**: 实时图表、状态指示灯
- **布局**: 全屏数据密度
- **技术**: WebSocket + 虚拟滚动

## 设计决策规则

```javascript
// 产品类型 → UI 分类规则
const designRules = {
  // 企业应用
  enterprise: {
    style: ['Swiss', 'Soft UI'],
    colors: ['blue-gray', 'monochrome'],
    avoid: ['neon', '3d-heavy']
  },
  // 消费产品
  consumer: {
    style: ['Bento', 'Vibrant'],
    colors: ['colorful', 'gradient'],
    avoid: ['brutalism']
  }
}
```

## 快速参考表

| 行业 | 产品类型 | 推荐风格 | 推荐配色 |
|------|---------|----------|----------|
| 金融 | 银行 | Swiss + Glass | 深蓝 + 金 |
| 电商 | 奢侈品 | Minimal + 3D | 黑白金 |
| 健康 | 冥想 | Soft UI + Organic | 柔和自然 |
| 创意 | 作品集 | Brutalism / 3D | 高对比 |
