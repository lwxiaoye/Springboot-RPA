---
name: ui-ux-pro-max
description: 提供专业 UI/UX 设计智能，支持多平台和框架。当用户请求构建页面、设计界面、创建组件、优化 UI 或讨论设计系统时激活。
---

# UI/UX Pro Max

## 核心工作流

当用户请求 UI/UX 工作时：

1. **分析需求** - 确定产品类型、行业、技术栈
2. **生成设计系统** - 选择合适的风格、颜色、字体
3. **生成代码** - 使用 Vue + Element Plus 实现
4. **预交付检查** - 验证无障碍、响应式、最佳实践

## 设计系统生成器

对于任何 UI 请求，先确定设计系统：

```
┌─────────────────────────────────────────────────────────┐
│  行业类型 → UI 分类 → 风格匹配 → 颜色方案 → 字体搭配   │
└─────────────────────────────────────────────────────────┘
```

### 关键规则

**禁止**：
- ❌ 使用 emoji 作为图标（使用 SVG: Element Plus Icons / Heroicons）
- ❌ 硬编码颜色值（使用 CSS 变量）
- ❌ 固定宽度布局
- ❌ 缺少 hover 状态
- ❌ 忽略无障碍（对比度、焦点状态）

**必须**：
- ✅ cursor-pointer 在所有可点击元素
- ✅ 平滑过渡动画 (150-300ms)
- ✅ 响应式：375px, 768px, 1024px, 1440px
- ✅ prefers-reduced-motion 支持
- ✅ 键盘导航焦点状态
- ✅ 文本对比度 ≥ 4.5:1

## Vue 技术栈指南

### 组件结构

```vue
<script setup>
// 1. 导入
import { ref, computed } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'

// 2. 响应式数据
const loading = ref(false)
const formRef = ref(null)

// 3. 计算属性
const isValid = computed(() => formRef.value?.validate())

// 4. 方法
const handleSubmit = async () => {
  if (!await isValid.value) return
  loading.value = true
  // ...
}
</script>

<template>
  <!-- 使用 Element Plus 组件 -->
  <el-form ref="formRef" :model="form" :rules="rules">
    <el-form-item prop="name">
      <el-input v-model="form.name" placeholder="请输入名称">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </el-form-item>
  </el-form>
</template>

<style scoped>
/* 使用 CSS 变量 */
.form-container {
  --primary-color: var(--el-color-primary);
  padding: var(--spacing-md);
}
</style>
```

### Element Plus 最佳实践

- 使用 `el-config-provider` 配置全局主题
- 表单验证使用 async-validator
- 表格使用 `el-table-v2` 虚拟滚动优化大数据
- 弹窗使用 `Teleport to body`
- 图标使用 `@element-plus/icons-vue`

### 布局模式

| 场景 | 推荐布局 |
|------|----------|
| 后台管理 | Header + Sider + Main |
| 数据展示 | 筛选栏 + 表格/卡片 + 分页 |
| 表单页面 | 单列/双列 + 固定底部操作栏 |
| 仪表盘 | 网格布局 + 响应式断点 |

## 设计资源

详细的设计系统数据、风格指南、行业规则请参考：

- [UI 风格目录](STYLES.md) - 67 种风格及其适用场景
- [行业设计规则](INDUSTRY.md) - 161 个行业特定规则
- [Vue 技术栈详情](STACK.md) - Vue/Nuxt/Element Plus 详细指南

## 常用代码模板

### 卡片组件

```vue
<template>
  <el-card class="data-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <span class="title">{{ title }}</span>
        <el-button type="primary" link @click="handleEdit">
          编辑
        </el-button>
      </div>
    </template>
    <div class="card-content">
      <slot />
    </div>
  </el-card>
</template>

<style scoped>
.data-card {
  transition: box-shadow 0.3s ease;
  cursor: pointer;
}
.data-card:hover {
  box-shadow: var(--el-box-shadow-light);
}
</style>
```

### 搜索筛选栏

```vue
<template>
  <div class="filter-bar">
    <el-input
      v-model="searchQuery"
      placeholder="搜索..."
      clearable
      @keyup.enter="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
    <el-select v-model="filterStatus" placeholder="状态" clearable>
      <el-option label="启用" value="enabled" />
      <el-option label="禁用" value="disabled" />
    </el-select>
    <el-button type="primary" @click="handleSearch">
      <el-icon><Search /></el-icon> 搜索
    </el-button>
  </div>
</template>
```

## 响应式断点

```css
/* 移动优先 */
.container { padding: 16px; }

/* Tablet */
@media (min-width: 768px) {
  .container { padding: 24px; }
  .grid { grid-template-columns: repeat(2, 1fr); }
}

/* Desktop */
@media (min-width: 1024px) {
  .container { padding: 32px; }
  .grid { grid-template-columns: repeat(3, 1fr); }
}

/* Large */
@media (min-width: 1440px) {
  .grid { grid-template-columns: repeat(4, 1fr); }
}
```
