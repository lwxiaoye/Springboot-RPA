---
# 常用组件模板
---

## 数据展示组件

### 1. 数据卡片 (Stat Card)

```vue
<template>
  <el-card class="stat-card" shadow="hover">
    <div class="stat-content">
      <div class="stat-icon" :style="{ backgroundColor: iconBg }">
        <el-icon :size="24" :color="iconColor">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="stat-info">
        <div class="stat-label">{{ label }}</div>
        <div class="stat-value">{{ displayValue }}</div>
        <div class="stat-trend" v-if="trend">
          <span :class="trend > 0 ? 'trend-up' : 'trend-down'">
            {{ trend > 0 ? '↑' : '↓' }} {{ Math.abs(trend) }}%
          </span>
          <span class="trend-period">{{ period }}</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: String,
  value: [Number, String],
  icon: Object,
  iconBg: { type: String, default: '#ecf5ff' },
  iconColor: { type: String, default: '#409eff' },
  trend: Number,
  period: { type: String, default: '较上期' },
  prefix: { type: String, default: '' },
  suffix: { type: String, default: '' }
})

const displayValue = computed(() => {
  return `${props.prefix}${props.value}${props.suffix}`
})
</script>

<style scoped>
.stat-card {
  transition: all 0.3s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
}
.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.stat-trend {
  margin-top: 4px;
  font-size: 12px;
}
.trend-up { color: #67c23a; }
.trend-down { color: #f56c6c; }
.trend-period {
  color: #909399;
  margin-left: 4px;
}
</style>
```

### 2. 表格工具栏 (Table Toolbar)

```vue
<template>
  <div class="table-toolbar">
    <div class="toolbar-left">
      <slot name="left">
        <el-input
          v-model="searchQuery"
          placeholder="搜索..."
          clearable
          @keyup.enter="handleSearch"
          style="width: 240px;"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px;">
          <el-option label="启用" value="enabled" />
          <el-option label="禁用" value="disabled" />
        </el-select>
        
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </slot>
    </div>
    
    <div class="toolbar-right">
      <slot name="right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const searchQuery = ref('')
const filterStatus = ref('')
const dateRange = ref([])

const emit = defineEmits(['search', 'add', 'export'])

const handleSearch = () => emit('search', { 
  query: searchQuery.value, 
  status: filterStatus.value,
  dateRange: dateRange.value 
})
const handleAdd = () => emit('add')
const handleExport = () => emit('export')
</script>

<style scoped>
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-right {
  display: flex;
  gap: 12px;
}
</style>
```

### 3. 弹窗表单 (Dialog Form)

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑' : '新增'"
    width="600px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入名称" />
      </el-form-item>
      
      <el-form-item label="类型" prop="type">
        <el-select v-model="formData.type" placeholder="请选择类型">
          <el-option label="类型一" value="type1" />
          <el-option label="类型二" value="type2" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入描述"
        />
      </el-form-item>
      
      <el-form-item label="状态" prop="status">
        <el-switch v-model="formData.status" />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确认
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  data: Object
})
const emit = defineEmits(['update:modelValue', 'submit'])

const formRef = ref()
const submitting = ref(false)
const formData = reactive({
  id: '',
  name: '',
  type: '',
  description: '',
  status: true
})

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isEdit = computed(() => !!props.data?.id)

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  emit('submit', { ...formData })
  
  setTimeout(() => {
    submitting.value = false
    visible.value = false
  }, 500)
}
</script>
```

## 导航组件

### 4. 面包屑导航

```vue
<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
    <el-breadcrumb-item v-for="item in items" :key="item.path">
      <router-link v-if="item.path" :to="item.path">{{ item.title }}</router-link>
      <span v-else>{{ item.title }}</span>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  }
})
</script>
```

### 5. 页面标题栏

```vue
<template>
  <div class="page-header">
    <div class="header-left">
      <h1 class="page-title">{{ title }}</h1>
      <p class="page-description" v-if="description">{{ description }}</p>
    </div>
    <div class="header-right">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: String,
  description: String
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
}
.page-description {
  margin-top: 8px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
</style>
```

## 状态组件

### 6. 空状态

```vue
<template>
  <div class="empty-state">
    <el-empty :image-size="imageSize" :description="description">
      <template #image>
        <slot name="image">
          <el-icon :size="64" color="#c0c4cc">
            <component :is="icon" />
          </el-icon>
        </slot>
      </template>
      <el-button type="primary" @click="handleAction" v-if="actionText">
        {{ actionText }}
      </el-button>
    </el-empty>
  </div>
</template>

<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
  imageSize: { type: Number, default: 200 },
  icon: { type: Object, default: () => 'FolderOpened' },
  actionText: String
})

const emit = defineEmits(['action'])
const handleAction = () => emit('action')
</script>
```

### 7. 加载状态

```vue
<template>
  <div class="loading-state" v-if="loading">
    <el-skeleton :rows="rows" animated />
  </div>
</template>

<script setup>
defineProps({
  loading: Boolean,
  rows: { type: Number, default: 5 }
})
</script>
```

## 反馈组件

### 8. 操作确认

```vue
<script setup>
import { ElMessageBox } from 'element-plus'

const confirmDelete = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${item.name}」吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    // 执行删除
    await deleteItem(item.id)
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}
</script>
```

## 布局组件

### 9. 响应式网格

```vue
<template>
  <div class="responsive-grid" :style="gridStyle">
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  columns: { type: Number, default: 4 },
  gap: { type: String, default: '24px' }
})

const gridStyle = computed(() => ({
  display: 'grid',
  gridTemplateColumns: `repeat(${props.columns}, 1fr)`,
  gap: props.gap
}))
</script>

<style scoped>
@media (max-width: 1200px) {
  .responsive-grid { grid-template-columns: repeat(3, 1fr) !important; }
}
@media (max-width: 768px) {
  .responsive-grid { grid-template-columns: repeat(2, 1fr) !important; }
}
@media (max-width: 480px) {
  .responsive-grid { grid-template-columns: 1fr !important; }
}
</style>
```

### 10. 标签页内容区

```vue
<template>
  <div class="tab-content">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all">
        <slot name="all" />
      </el-tab-pane>
      <el-tab-pane label="进行中" name="pending">
        <slot name="pending" />
      </el-tab-pane>
      <el-tab-pane label="已完成" name="completed">
        <slot name="completed" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const activeTab = ref('all')
const emit = defineEmits(['tab-change'])

const handleTabChange = (tab) => {
  emit('tab-change', tab)
}
</script>

<style scoped>
.tab-content {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}
</style>
```
