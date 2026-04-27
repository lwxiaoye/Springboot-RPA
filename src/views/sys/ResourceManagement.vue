<template>
  <div class="resource-management-content">

    <div class="section-header">
      <h2 class="section-title">资源管理</h2>
      <el-button type="primary" @click="showResourceModal">
        <span style="margin-right: 4px;">+</span> 新增资源
      </el-button>
    </div>

    <!-- 查询表单 -->
    <div class="search-form">
      <el-form :model="searchForm" inline>
        <el-form-item label="资源名称：">
          <el-input 
            v-model="searchForm.resourceName" 
            placeholder="请输入" 
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="资源类型：">
          <el-select 
            v-model="searchForm.resourceType" 
            placeholder="请选择" 
            clearable
            style="width: 140px"
          >
            <el-option label="菜单" value="menu" />
            <el-option label="按钮" value="button" />
            <el-option label="页面" value="page" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 资源表格 -->
    <el-table :data="paginatedResources" style="width: 100%" v-loading="loading" border stripe class="unified-table">
      <el-table-column type="index" label="序号" width="80" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.current - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="资源名称" min-width="150" />
      <el-table-column prop="code" label="资源编码" min-width="150" />
      <el-table-column prop="type" label="资源类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getResourceTypeTag(row.type)" size="small">
            {{ getResourceTypeText(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="路径/URL" min-width="180">
        <template #default="{ row }">{{ row.url || '-' }}</template>
      </el-table-column>
      <el-table-column prop="icon" label="图标" width="100">
        <template #default="{ row }">
          <span v-if="row.icon">{{ row.icon }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="primary" size="small" @click="editResource(row)" class="action-btn">编辑</el-button>
            <el-popconfirm
              :title="'确定要删除资源「' + row.name + '」吗？'"
              confirmButtonText="确认删除"
              cancelButtonText="取消"
              icon="Delete"
              iconColor="#f56c6c"
              @confirm="deleteResource(row)"
            >
              <template #reference>
                <el-button link type="danger" size="small" class="action-btn">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑资源弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="550px"
      @close="closeDialog"
    >
      <el-form :model="resourceForm" :rules="formRules" ref="resourceFormRef" label-width="100px">
        <el-form-item label="资源名称" prop="name">
          <el-input v-model="resourceForm.name" placeholder="请输入资源名称" />
        </el-form-item>
        <el-form-item label="资源编码" prop="code">
          <el-input v-model="resourceForm.code" placeholder="请输入资源编码，如：SYS_RESOURCE" />
        </el-form-item>
        <el-form-item label="资源类型" prop="type">
          <el-select v-model="resourceForm.type" placeholder="请选择资源类型" style="width: 100%">
            <el-option label="菜单" value="menu" />
            <el-option label="按钮" value="button" />
            <el-option label="页面" value="page" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径/URL" prop="url">
          <el-input v-model="resourceForm.url" placeholder="请输入路径或URL，如：/system/resource" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="resourceForm.icon" placeholder="请输入图标名称，如：Setting" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="resourceForm.sort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="resourceForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveResource" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

// 数据状态
const loading = ref(false)
const submitLoading = ref(false)
const resources = ref([])

// 搜索表单
const searchForm = reactive({
  resourceName: '',
  resourceType: null
})

// 分页参数
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 弹窗控制
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentResourceId = ref(null)

// 表单引用
const resourceFormRef = ref(null)

// 资源表单
const resourceForm = reactive({
  name: '',
  code: '',
  type: 'menu',
  url: '',
  icon: '',
  sort: 0,
  status: 1
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入资源名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入资源编码', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择资源类型', trigger: 'change' }
  ],
  sort: [
    { type: 'number', message: '排序必须为数字', trigger: 'blur' }
  ]
}

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑资源' : '新增资源')

// 筛选后的资源列表
const filteredResources = computed(() => {
  let list = resources.value
  if (searchForm.resourceName) {
    list = list.filter(r => r.name.includes(searchForm.resourceName))
  }
  if (searchForm.resourceType) {
    list = list.filter(r => r.type === searchForm.resourceType)
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedResources = computed(() => {
  const start = (pagination.current - 1) * pagination.size
  const end = start + pagination.size
  return filteredResources.value.slice(start, end)
})

// 获取资源类型标签样式
const getResourceTypeTag = (type) => {
  const map = {
    menu: 'primary',
    button: 'success',
    page: 'warning'
  }
  return map[type] || 'info'
}

// 获取资源类型文本
const getResourceTypeText = (type) => {
  const map = {
    menu: '菜单',
    button: '按钮',
    page: '页面'
  }
  return map[type] || type
}

// 加载资源列表
const loadResources = async () => {
  loading.value = true
  try {
    const result = await apiGet('/resource')
    if (result.code === 0) {
      resources.value = result.data || []
      // 不需要在这里设置 pagination.total，filteredResources 会计算
    }
  } catch {
    resources.value = []
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  // 由于使用了 computed 筛选，不需要重新加载
}

// 重置搜索
const resetSearch = () => {
  searchForm.resourceName = ''
  searchForm.resourceType = null
  pagination.current = 1
}

// 分页变化（由于使用 computed 筛选，这里只需要处理分页显示）
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
}

const handleCurrentChange = (page) => {
  pagination.current = page
}

// 显示新增资源弹窗
const showResourceModal = () => {
  isEdit.value = false
  currentResourceId.value = null
  Object.assign(resourceForm, {
    name: '',
    code: '',
    type: 'menu',
    url: '',
    icon: '',
    sort: 0,
    status: 1
  })
  dialogVisible.value = true
}

// 编辑资源
const editResource = (resource) => {
  isEdit.value = true
  currentResourceId.value = resource.id
  Object.assign(resourceForm, {
    name: resource.name,
    code: resource.code,
    type: resource.type,
    url: resource.url || '',
    icon: resource.icon || '',
    sort: resource.sort || 0,
    status: resource.status
  })
  dialogVisible.value = true
}

// 关闭弹窗
const closeDialog = () => {
  resourceFormRef.value?.resetFields()
}

// 保存资源
const saveResource = async () => {
  if (!resourceFormRef.value) return
  
  await resourceFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      if (isEdit.value) {
        const result = await apiPut(`/resource/${currentResourceId.value}`, {
          name: resourceForm.name,
          code: resourceForm.code,
          type: resourceForm.type,
          url: resourceForm.url,
          icon: resourceForm.icon,
          sort: resourceForm.sort,
          status: resourceForm.status
        })
        if (result.code === 0) {
          ElMessage.success('更新成功')
          await loadResources()
        } else {
          ElMessage.error(result.message || '更新失败')
        }
      } else {
        const result = await apiPost('/resource', {
          name: resourceForm.name,
          code: resourceForm.code,
          type: resourceForm.type,
          url: resourceForm.url,
          icon: resourceForm.icon,
          sort: resourceForm.sort
        })
        if (result.code === 0) {
          ElMessage.success('创建成功')
          await loadResources()
        } else {
          ElMessage.error(result.message || '创建失败')
        }
      }
      dialogVisible.value = false
    } catch {
      ElMessage.error('请求失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 删除资源
const deleteResource = async (resource) => {
  try {
    const result = await apiDelete(`/resource/${resource.id}`)
    if (result.code === 0) {
      const index = resources.value.findIndex(r => r.id === resource.id)
      if (index !== -1) {
        resources.value.splice(index, 1)
        pagination.total--
      }
      ElMessage.success('删除成功')
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch {
    ElMessage.error('请求失败')
  }
}

// 初始化
onMounted(() => {
  loadResources()
})
</script>

<style scoped>
.resource-management-content {
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: var(--bg-secondary, #ffffff);
  border-radius: var(--radius-lg, 16px);
  border: 1px solid var(--border-color, #e5e7eb);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  margin: 0;
  letter-spacing: -0.5px;
}

/* 搜索表单 */
.search-form {
  background: var(--bg-secondary, #ffffff);
  padding: 20px 24px;
  border-radius: var(--radius-lg, 16px);
  margin-bottom: 24px;
  border: 1px solid var(--border-color, #e5e7eb);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 16px;
}

.search-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-md, 12px);
  box-shadow: 0 0 0 1px var(--border-color, #e5e7eb) inset;
  transition: all 0.2s;
}

.search-form :deep(.el-input__wrapper:hover) {
  border-color: var(--primary, #409eff);
}

.search-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) inset;
}

/* 表格 */
:deep(.el-table) {
  border-radius: var(--radius-lg, 16px);
  overflow: hidden;
  border: 1px solid var(--border-color, #e5e7eb);
}

:deep(.el-table th) {
  background: var(--bg-tertiary, #f9fafb) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
}

:deep(.el-table td) {
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

:deep(.el-table__row:hover > td) {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 统一表格样式 */
.unified-table :deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  padding: 12px 0;
  border-bottom: 2px solid #e5e7eb;
}

.unified-table :deep(.el-table__body-wrapper td) {
  padding: 10px 0;
  vertical-align: middle;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.unified-table :deep(.el-table__header-wrapper th .cell),
.unified-table :deep(.el-table__body-wrapper td .cell) {
  padding: 0 6px;
}

.unified-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.unified-table :deep(.el-table__row:hover > td) {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 序号单元格动画 */
.index-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  height: 100%;
  padding: 4px 0;
}

.index-number {
  font-weight: 700;
  font-size: 14px;
  color: var(--text-primary, #1f2937);
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.index-line {
  width: 2px;
  height: 0;
  background: linear-gradient(180deg, #00d4ff, #0077ff);
  border-radius: 1px;
  transition: height 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.6;
}

.unified-table :deep(.el-table__row:hover .index-line) {
  height: 12px;
  opacity: 1;
}

.unified-table :deep(.el-table__row:hover .index-number) {
  transform: scale(1.1);
  transition: transform 0.3s ease;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.2s ease;
  font-size: 13px;
}

.action-btn:hover {
  background: rgba(64, 158, 255, 0.1);
  transform: translateY(-1px);
}

/* 分页 */
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  background: var(--bg-secondary, #ffffff);
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-color, #e5e7eb);
}

/* 弹窗 */
:deep(.el-dialog) {
  border-radius: var(--radius-lg, 16px);
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 20px 24px;
  background: var(--bg-tertiary, #f9fafb);
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
}

:deep(.el-dialog__body) {
  padding: 24px;
}

/* 输入框样式 */
:deep(.el-input__wrapper) {
  border-radius: 10px;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary, #409eff) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) inset;
}

/* 文本域样式 */
:deep(.el-textarea__inner) {
  border-radius: 10px;
}

:deep(.el-textarea__inner:hover) {
  border-color: var(--primary, #409eff);
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--primary, #409eff);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) inset;
}
</style>