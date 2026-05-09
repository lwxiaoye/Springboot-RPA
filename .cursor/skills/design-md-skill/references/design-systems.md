# 设计系统参考指南

本目录包含热门网站和品牌的设计系统规范，可用于 AI 编码时生成一致的 UI。

## 使用方法

当用户请求类似某个网站的设计时，在对应的文件中查找规范并应用。

---

## Linear 风格

**特点**：超深黑 (#010102)、紫色强调 (#5e6ad2)、极简精致

**适用场景**：项目管理工具、SaaS 产品页、开发者工具

### 核心颜色
| Token | 值 | 用途 |
|-------|-----|------|
| canvas | #010102 | 深黑背景 |
| surface-1 | #0f1011 | 卡片背景 |
| primary | #5e6ad2 | 主强调色 |
| ink | #f7f8f8 | 主文本 |

### 字体
- Display: SF Pro Display, weight 500-700
- Text: SF Pro Text, weight 400
- 展示字体使用负字间距 (-3.0px at 80px)

### 圆角
- 按钮: 8px
- 卡片: 12px
- 大卡片: 16px

### Do's
- 使用 canvas (#010102) 作为基础
- 紫色仅用于：品牌标识、主 CTA、焦点环
- 产品截图主导页面

### Don'ts
- 不要使用亮色背景
- 不要引入第二强调色
- 不要使用圆角过大的按钮

---

## Vercel 风格

**特点**：黑白精准、Geist 字体、极简主义

**适用场景**：部署平台、着陆页、开发者文档

### 核心颜色
| Token | 值 | 用途 |
|-------|-----|------|
| black | #000000 | 主背景 |
| white | #ffffff | 主文本 |
| gray | #666666 | 次要文本 |
| gray-light | #eaeaea | 边框 |

### 字体
- Geist Sans
- Geist Mono (代码)
- 极致负字间距

### 特点
- 无圆角或极小圆角
- 纯色块设计
- 网格布局

---

## Stripe 风格

**特点**：紫色渐变 (#635bff)、300 字重、精致优雅

**适用场景**：支付平台、金融 SaaS、企业产品

### 核心颜色
| Token | 值 | 用途 |
|-------|-----|------|
| primary | #635bff | 主品牌色 |
| primary-light | #f7faff | 浅色背景 |
| ink | #1a1a1a | 主文本 |
| gray | #6b7280 | 次要文本 |

### 字体
- Graphik (或 Inter)
- 字重 300-500
- 优雅的负字间距

### 特点
- 紫色渐变用于 CTA
- 卡片带微圆角 (8-12px)
- 充足的留白

---

## Supabase 风格

**特点**：深绿主题 (#3ecf8e)、代码优先、开发者友好

**适用场景**：后端即服务、开发者工具、数据库管理

### 核心颜色
| Token | 值 | 用途 |
|-------|-----|------|
| canvas | #111111 | 深黑背景 |
| surface | #1a1a1a | 卡片背景 |
| primary | #3ecf8e | Supabase 绿 |
| green-light | #e8fcf0 | 浅绿背景 |

### 字体
- SF Pro (macOS)
- JetBrains Mono (代码)
- 干净的无衬线

### 特点
- 绿色用于成功状态和强调
- 大量使用代码块
- 开发者友好的深色主题

---

## Notion 风格

**特点**：温暖极简、衬线标题、柔和表面

**适用场景**：协作工具、知识管理、文档

### 核心颜色
| Token | 值 | 用途 |
|-------|-----|------|
| canvas | #ffffff | 白色背景 |
| surface | #f7f6f3 | 柔和卡片 |
| ink | #37352f | 主文本 |
| gray | #9b9a97 | 次要文本 |

### 字体
- 衬线标题 + 无衬线正文
- 或纯 Inter
- 字重 400-600

### 特点
- 温暖米色表面
- 衬线标题增加个性
- 柔和的圆角

---

## Claude (Anthropic) 风格

**特点**：暖陶土色 (#c56a46)、编辑布局、清晰排版

**适用场景**：AI 助手界面、对话界面、内容展示

### 核心颜色
| Token | 值 | 用途 |
|-------|-----|------|
| canvas | #fcfbf9 | 暖白背景 |
| surface | #f7f5f2 | 卡片背景 |
| primary | #c56a46 | 陶土强调 |
| ink | #1f1f1f | 主文本 |

### 字体
- 清晰的无衬线
- 编辑级排版
- 充足的行高

### 特点
- 暖色调增加温度
- 编辑布局为主
- 清晰的视觉层级

---

## Cursor 风格

**特点**：AI 优先编辑器、渐变强调、深色主题

**适用场景**：IDE、代码编辑器、AI 工具界面

### 核心颜色
| Token | 值 | 用途 |
|-------|-----|------|
| canvas | #0a0a0a | 纯黑背景 |
| surface | #111111 | 卡片 |
| primary | #7c3aed | 紫色渐变 |
| accent | #22d3ee | 青色点缀 |

### 字体
- JetBrains Mono (代码)
- Inter (UI)
- 开发者友好

### 特点
- 紫色/青色渐变
- 深色主题为主
- 代码优先设计

---

## Shopify 风格

**特点**：深色电影感、霓虹绿 (#00d624)、超轻展示字

**适用场景**：电商平台、在线商店、商业 SaaS

### 核心颜色
| Token | 值 | 用途 |
|-------|-----|------|
| canvas | #0f0f0f | 深黑背景 |
| surface | #1a1a1a | 卡片 |
| primary | #00d624 | 霓虹绿 |
| white | #fffefe | 主文本 |

### 字体
- 展示字重极轻 (100-300)
- 无衬线
- 大号标题

### 特点
- 霓虹绿作为主要 CTA
- 电影级全出血图像
- 超轻展示字体

---

## 选择指南

| 用户需求 | 推荐设计系统 |
|----------|--------------|
| 开发工具/IDE | Cursor, Linear, Vercel |
| SaaS 产品页 | Linear, Stripe, Notion |
| 电商 | Shopify, Airbnb |
| 金融/支付 | Stripe, Revolut, Kraken |
| AI 工具 | Claude, VoltAgent, Together AI |
| 数据库/DevOps | Supabase, ClickHouse, Sentry |
| 项目管理 | Linear, Notion |
| 创意工具 | Figma, Framer |

---

## 获取完整设计系统

完整的 DESIGN.md 文件可从以下来源获取：

1. **GitHub**: [VoltAgent/awesome-design-md](https://github.com/VoltAgent/awesome-design-md)
2. **网站**: [getdesign.md](https://getdesign.md)

使用命令安装设计系统：
```bash
npx getdesign@latest add <brand-name>
```
