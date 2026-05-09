---
name: design-md
description: >
  使用 DESIGN.md 设计系统文件为 AI 编码代理生成一致的 UI 界面。适用于以下场景：
  (1) 用户请求构建页面、设计界面、创建组件时
  (2) 用户提到 "像 xxx 一样" 或参考某网站的设计风格
  (3) 用户需要统一的设计规范或设计系统
  (4) AI 代码生成需要设计参考时
  (5) 创建 landing page、Dashboard、产品页面等
---

# DESIGN.md 设计系统技能

## 概述

[DESIGN.md](https://stitch.withgoogle.com/docs/design-md/overview/) 是 Google Stitch 引入的概念 —— 一个纯文本设计系统文档，AI 代理可以读取它来生成一致的 UI。

它是纯 Markdown 文件，无需 Figma 导出、JSON Schema 或特殊工具。将其放入项目根目录，任何 AI 编码代理都能立即理解 UI 应该如何呈现。

| 文件 | 阅读者 | 定义内容 |
|------|--------|----------|
| `AGENTS.md` | 编码代理 | 如何构建项目 |
| `DESIGN.md` | 设计代理 | UI 应该如何看起来和感觉 |

## 快速使用

### 1. 选择合适的设计系统

DESIGN.md 文件存储在 `references/` 目录中，每个文件代表一个品牌/网站的设计系统。

选择时考虑：
- **产品类型**：SaaS、Dashboard、电商、文档等
- **视觉风格**：深色/浅色、简约/丰富、卡通/专业
- **行业领域**：科技、金融、创意等

### 2. 应用到项目

当用户请求类似某个网站的设计时：

1. 在 `references/` 目录中找到对应的 DESIGN.md
2. 读取并理解该设计系统的规范
3. 在当前项目中应用这些设计规范

### 3. 关键设计要素

每个 DESIGN.md 包含以下核心部分：

```yaml
---
version: alpha
name: 品牌名称
description: 设计系统描述
---

colors:      # 颜色调色板
typography:  # 字体层级
rounded:     # 圆角规范
spacing:     # 间距系统
components: # 组件样式
```

## 设计系统集合

| 类别 | 设计系统 | 特点 |
|------|----------|------|
| **AI & LLM** | Claude, Cohere, ElevenLabs, Mistral, Ollama | 深色主题、技术感 |
| **开发工具** | Cursor, Vercel, Expo, Raycast | 简洁、专业 |
| **数据库** | Supabase, MongoDB, ClickHouse | 深色、数据密集 |
| **SaaS** | Linear, Notion, Stripe | 简约、精致 |
| **电商** | Shopify, Nike, Airbnb | 视觉丰富 |
| **消费电子** | Apple, Tesla, NVIDIA | 极简、电影感 |

## 组件应用示例

### 按钮样式

```yaml
button-primary:
  backgroundColor: "{colors.primary}"
  textColor: "{colors.on-primary}"
  typography: "{typography.button}"
  rounded: "{rounded.md}"
  padding: 8px 14px
```

### 卡片样式

```yaml
pricing-card:
  backgroundColor: "{colors.surface-1}"
  textColor: "{colors.ink}"
  typography: "{typography.body}"
  rounded: "{rounded.lg}"
  padding: 24px
```

## 最佳实践

### 应该做

- 在 `references/` 中选择最接近用户需求的设计系统
- 严格遵循颜色、字体、间距规范
- 使用设计系统中的组件 token 名称
- 保持视觉一致性

### 不应该做

- 不要混合多个设计系统的风格
- 不要跳过设计系统定义的层级
- 不要忽略 Do's and Don'ts 部分
- 不要在没有参考的情况下自由发挥

## 迭代指南

1. 每次专注于一个组件，引用其 `components:` token 名称
2. 引入新区域时，先决定它使用哪个 surface 层级
3. 默认使用 `{typography.body}` weight 400
4. 完成后验证是否符合设计规范

## 参考资源

- [Google Stitch DESIGN.md 官方文档](https://stitch.withgoogle.com/docs/design-md/overview/)
- [awesome-design-md GitHub 仓库](https://github.com/VoltAgent/awesome-design-md)
- 详细的设计系统规范请查看 `references/` 目录
