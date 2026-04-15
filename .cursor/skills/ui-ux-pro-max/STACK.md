---
# Vue 技术栈详细指南
---

## 技术栈配置

### 核心依赖
```json
{
  "vue": "^3.5+",
  "vue-router": "^5.0+",
  "element-plus": "^2.13+",
  "@element-plus/icons-vue": "^2.3+",
  "vite": "^5.0+"
}
```

## Element Plus 最佳实践

### 组件使用规范

#### 1. 表单组件

```vue
<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const formRef = ref()
const form = reactive({
  name: '',
  email: '',
  role: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
  ]
}

const submitForm = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return
  
  // 提交逻辑
  ElMessage.success('提交成功')
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
    <el-form-item label="名称" prop="name">
      <el-input v-model="form.name" placeholder="请输入名称">
        <template #prefix>
          <el-icon><User /></el-icon>
        </template>
      </el-input>
    </el-form-item>
    
    <el-form-item label="邮箱" prop="email">
      <el-input v-model="form.email" type="email" placeholder="请输入邮箱" />
    </el-form-item>
    
    <el-form-item label="角色" prop="role">
      <el-select v-model="form.role" placeholder="请选择角色" clearable>
        <el-option label="管理员" value="admin" />
        <el-option label="用户" value="user" />
      </el-select>
    </el-form-item>
    
    <el-form-item>
      <el-button type="primary" @click="submitForm">提交</el-button>
      <el-button @click="handleReset">重置</el-button>
    </el-form-item>
  </el-form>
</template>
```

#### 2. 表格组件

```vue
<template>
  <el-table :data="tableData" v-loading="loading" stripe>
    <el-table-column prop="id" label="ID" width="80" />
    <el-table-column prop="name" label="名称" min-width="120">
      <template #default="{ row }">
        <el-tag type="success">{{ row.name }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="status" label="状态" width="100">
      <template #default="{ row }">
        <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
          {{ row.status === 'active' ? '启用' : '禁用' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="200" fixed="right">
      <template #default="{ row }">
        <el-button type="primary" link @click="handleEdit(row)">
          编辑
        </el-button>
        <el-button type="danger" link @click="handleDelete(row)">
          删除
        </el-button>
      </template>
    </el-table-column>
  </el-table>
  
  <!-- 分页 -->
  <div class="pagination-wrapper">
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />
  </div>
</template>
```

#### 3. 弹窗组件

```vue
<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="600px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form :model="form" label-width="100px">
      <!-- 表单内容 -->
    </el-form>
    
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确认</el-button>
    </template>
  </el-dialog>
</template>
```

### 主题定制

#### 全局样式变量

```scss
// styles/variables.scss
$primary-color: #409EFF;
$success-color: #67C23A;
$warning-color: #E6A23C;
$danger-color: #F56C6C;
$info-color: #909399;

// 自定义主题
$--colors: (
  "primary": $primary-color,
  "success": $success-color,
  "warning": $warning-color,
  "danger": $danger-color,
);

// 导入并覆盖
@use "element-plus/theme-chalk/src/common/var.scss" as *;
```

### 布局组件

```vue
<template>
  <el-container class="layout-container">
    <el-aside width="240px">
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header>
        <div class="header-content">
          <Breadcrumb />
          <UserDropdown />
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
```

## Nuxt.js 集成

### 自动导入配置

```javascript
// nuxt.config.ts
export default defineNuxtConfig({
  modules: ['@nuxtjs/element-plus'],
  elementPlus: {
    importStyle: 'scss', // 或 'css'
    theme: 'dark' // 或 'light'
  }
})
```

### 插件注册

```javascript
// plugins/element-plus.ts
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

export default defineNuxtPlugin((nuxtApp) => {
  const app = nuxtApp.vueApp
  
  // 注册所有图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }
})
```

## Vite 配置优化

```javascript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@import "@/styles/variables.scss";`
      }
    }
  }
})
```

## 组件设计模式

### 复合组件模式

```vue
<!-- UserCard.vue -->
<template>
  <el-card>
    <UserAvatar :user="user" />
    <UserName :user="user" />
    <UserActions :user="user" />
  </el-card>
</template>
```

### 组合式函数

```javascript
// composables/useTable.ts
export function useTable(options) {
  const data = ref([])
  const loading = ref(false)
  const pagination = reactive({ page: 1, size: 20, total: 0 })
  
  const fetchData = async () => {
    loading.value = true
    try {
      const res = await api.list(options)
      data.value = res.list
      pagination.total = res.total
    } finally {
      loading.value = false
    }
  }
  
  return { data, loading, pagination, fetchData }
}
```

## 性能优化

### 虚拟滚动

```vue
<template>
  <el-table-v2
    :columns="columns"
    :data="data"
    :width="800"
    :height="400"
  />
</template>
```

### 懒加载图片

```vue
<template>
  <el-image
    :src="imageUrl"
    :lazy="true"
    :preview-src-list="[imageUrl]"
  />
</template>
```

## 无障碍访问

```vue
<template>
  <el-button
    type="primary"
    :aria-label="'提交表单'"
    :disabled="loading"
  >
    提交
  </el-button>
</template>

<script setup>
// 键盘导航支持
const handleKeydown = (e) => {
  if (e.key === 'Enter' || e.key === ' ') {
    // 执行操作
  }
}
</script>
```

## 国际化

```vue
<template>
  <el-config-provider :locale="locale">
    <el-button type="primary">{{ $t('common.submit') }}</el-button>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const { locale } = useI18n()
</script>
```

## 设计资源链接

- Element Plus 官方文档: https://element-plus.org/
- Vue 3 组合式 API: https://vuejs.org/guide/introduction.html
- Vite 构建工具: https://vitejs.dev/
