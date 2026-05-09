# Linear 风格 DESIGN.md 模板

这是 Linear 设计系统的完整 YAML 前端 + Markdown 描述格式。可直接复制使用。

```yaml
---
version: alpha
name: YourBrand
description: "设计系统描述：深色主题、强调色、功能特点"
# 示例：深色产品营销画布，围绕深黑背景、灰色文本和单色强调色构建。
# 简洁、技术感、精致。展示字体使用负字间距。

colors:
  # 品牌色
  primary: "#5e6ad2"           # 主强调色
  on-primary: "#ffffff"        # 主色上的文本
  primary-hover: "#828fff"     # 主色悬停
  primary-focus: "#5e69d1"     # 焦点环

  # 表面层级
  canvas: "#010102"             # 默认背景（最深的黑）
  surface-1: "#0f1011"         # 卡片、面板
  surface-2: "#141516"         # 悬停卡片
  surface-3: "#18191a"         # 下拉菜单
  hairline: "#23252a"          # 1px 边框

  # 文本
  ink: "#f7f8f8"               # 主文本
  ink-muted: "#d0d6e0"         # 次要文本
  ink-subtle: "#8a8f98"        # 第三文本
  ink-tertiary: "#62666d"      # 禁用文本

  # 语义色
  semantic-success: "#27a644"   # 成功状态

typography:
  display-xl:
    fontFamily: "SF Pro Display, system-ui, sans-serif"
    fontSize: 80px
    fontWeight: 600
    lineHeight: 1.05
    letterSpacing: -3.0px
  display-lg:
    fontFamily: "SF Pro Display, system-ui, sans-serif"
    fontSize: 56px
    fontWeight: 600
    lineHeight: 1.10
    letterSpacing: -1.8px
  display-md:
    fontFamily: "SF Pro Display, system-ui, sans-serif"
    fontSize: 40px
    fontWeight: 600
    lineHeight: 1.15
    letterSpacing: -1.0px
  headline:
    fontFamily: "SF Pro Display, system-ui, sans-serif"
    fontSize: 28px
    fontWeight: 600
    lineHeight: 1.20
    letterSpacing: -0.6px
  body:
    fontFamily: "SF Pro Text, system-ui, sans-serif"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.50
    letterSpacing: -0.05px
  button:
    fontFamily: "SF Pro Text, system-ui, sans-serif"
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.20
  caption:
    fontFamily: "SF Pro Text, system-ui, sans-serif"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.40

rounded:
  xs: 4px      # 小芯片、状态徽章
  sm: 6px      # 内联标签
  md: 8px      # 按钮、输入框
  lg: 12px     # 卡片
  xl: 16px     # 大卡片、面板
  pill: 9999px # 切换按钮
  full: 9999px # 头像圆

spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  section: 96px

components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: 8px 14px
  button-secondary:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: 8px 14px
    border: "1px solid {colors.hairline}"
  card:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: 24px
  input:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: 8px 12px
---
```

## Markdown 描述部分

在 YAML 前端之后，添加以下 Markdown 描述：

```markdown
## 概述

[品牌名称] 的设计系统围绕 [核心设计理念] 构建。

**关键特性：**
- [特性 1]
- [特性 2]
- [特性 3]

## 颜色

### 品牌色
- **Primary** ({colors.primary}): [用途]
- **Primary Hover** ({colors.primary-hover}): [用途]

### 表面
- **Canvas** ({colors.canvas}): [用途]
- **Surface 1** ({colors.surface-1}): [用途]

### 文本
- **Ink** ({colors.ink}): [用途]
- **Ink Muted** ({colors.ink-muted}): [用途]

## 排版

### 层级表

| Token | 尺寸 | 字重 | 行高 | 用途 |
|-------|------|------|------|------|
| display-xl | 80px | 600 | 1.05 | 主标题 |
| display-lg | 56px | 600 | 1.10 | 区块标题 |
| body | 16px | 400 | 1.50 | 正文 |

## 布局

### 间距系统
- 基础单位：4px
- 卡片内边距：24px
- 区块间距：96px

### 网格
- 最大内容宽度：1280px
- 卡片网格：桌面 3 列 → 平板 2 列 → 移动 1 列

## 组件

### 按钮
- **Primary**: 主 CTA，使用 {colors.primary}
- **Secondary**: 次要 CTA，使用 {colors.surface-1}

### 卡片
- 背景：{colors.surface-1}
- 圆角：{rounded.lg} (12px)
- 内边距：24px
- 边框：1px {colors.hairline}

## Do's and Don'ts

### Do
- 使用 canvas 作为基础表面
- 保持简洁的设计
- 遵循间距系统

### Don't
- 不要混合多个设计系统
- 不要跳过层级
- 不要忽略设计规范
```

## 快速自定义步骤

1. **复制模板**到新文件 `DESIGN.md`
2. **修改描述**：更新品牌名称和设计理念
3. **调整颜色**：替换为你品牌的调色板
4. **更新字体**：选择合适的字体家族
5. **添加组件**：根据需要添加更多组件
6. **编写 Do's and Don'ts**：定义设计边界
