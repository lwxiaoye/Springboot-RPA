# UI/UX Pro Max - Cursor Skill

为 Vue 3 + Element Plus 项目提供专业 UI/UX 设计智能的 Cursor Skill。

## 特性

- **行业推理引擎** - 基于 161 个行业规则生成针对性设计系统
- **67 种 UI 风格** - 从 Minimalism 到 3D Hyperrealism
- **Vue 优化** - 针对 Vue 3 + Element Plus + Vite 的最佳实践
- **响应式设计** - 自动适配 375px / 768px / 1024px / 1440px
- **无障碍支持** - WCAG AA 标准检查

## 快速开始

### 自动激活

当你在 Cursor 中请求任何 UI/UX 相关任务时，Skill 会自动激活：

```
创建一个销售数据仪表盘
```

```
设计一个用户注册页面
```

```
为我的电商应用添加商品卡片
```

### 激活方式

**Skill Mode (自动)** - 只需在聊天中自然描述你的需求，AI 会自动应用此 Skill。

**强制激活**：如果未自动激活，可以这样引导：
```
使用 UI/UX Pro Max 技能帮我设计一个响应式仪表盘
```

## 设计系统生成

Skill 会自动分析你的需求并生成完整的设计系统：

```
┌──────────────────────────────────────────────────────┐
│  产品类型  →  推荐风格  →  配色方案  →  字体搭配      │
└──────────────────────────────────────────────────────┘
```

例如，当你说「创建一个金融科技仪表盘」时，AI 会：

1. **识别行业**: Fintech / Banking
2. **推荐风格**: Swiss Style + Glassmorphism
3. **配色方案**: 深海蓝 + 古铜金强调
4. **字体**: Inter + JetBrains Mono (数据)
5. **输出**: 完整的 Vue + Element Plus 组件

## 支持的技术栈

| 类别 | 技术栈 |
|------|--------|
| Web | Vue 3, Vue 2, Nuxt.js |
| UI 库 | Element Plus, Element UI |
| 构建工具 | Vite, Webpack |

## 文件结构

```
ui-ux-pro-max/
├── SKILL.md           # 核心技能指南（自动激活）
├── STYLES.md          # 67 种 UI 风格参考
├── INDUSTRY.md        # 161 行业设计规则
├── STACK.md           # Vue 技术栈详细文档
├── COLORS.md          # 配色方案指南
├── TYPOGRAPHY.md      # 字体搭配指南
├── COMPONENTS.md      # 常用组件模板
├── EXAMPLES.md        # 使用示例
└── CHECKS.md          # 预交付检查清单
```

## 核心能力

### 1. 智能风格推荐

根据产品类型自动推荐最佳 UI 风格：

- **SaaS Dashboard** → Swiss + Bento Grid
- **Fintech** → Glassmorphism + Deep Blue
- **E-commerce** → Vibrant + Gradient
- **Portfolio** → Brutalism / 3D
- **Healthcare** → Soft UI + Organic

### 2. Vue 组件生成

自动生成符合最佳实践的 Vue 组件：

- ✅ `<script setup>` 组合式 API
- ✅ Element Plus 组件库
- ✅ 响应式设计（移动优先）
- ✅ 表单验证规则
- ✅ 虚拟滚动优化
- ✅ 无障碍访问

### 3. 响应式布局

内置断点系统：

```css
Mobile:    ≤ 767px  (单列布局)
Tablet:    768-1023px (双列布局)
Desktop:   1024-1439px (三列布局)
Large:     ≥ 1440px (四列布局)
```

### 4. 无障碍检查

自动确保 WCAG 合规：

- 颜色对比度 ≥ 4.5:1
- 键盘导航支持
- ARIA 标签正确
- 焦点状态可见

## 使用提示

### 最佳实践

1. **明确需求** - 在请求中包含产品类型和��标��户
   ```
   ✅ "创建一个 B2B SaaS 的客户管理仪表盘"
   ❌ "做一个页面"
   ```

2. **指定技术栈** - 提及技术偏好
   ```
   "使用 Element Plus 和 Vue 3 创建登录页"
   ```

3. **参考设计风格** - 如有偏好可说明
   ```
   "我想用 Bento Grid 风格展示功能"
   ```

### 请求示例

```
创建一个医疗预约系统，需要医生列表、预约表单和日历视图
```

```
为我的 Fintech 应用设计深色模式的仪表盘
```

```
用 Bento Grid 布局实现产品特性展示卡片
```

## 资源链接

- [Element Plus 官方文档](https://element-plus.org/)
- [Vue 3 组合式 API 指南](https://vuejs.org/guide/introduction.html)
- [设计系统生成原理](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)

## 安装来源

本 Skill 基于 [ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) 创建，针对 Vue 技术栈进行了定制优化。

## 许可证

MIT License - 基于原项目许可证
