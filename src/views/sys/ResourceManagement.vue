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
    <el-table :data="filteredResources" style="width: 100%" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="80">
        <template #default="{ $index }">
          {{ (pagination.current - 1) * pagination.size + $index + 1 }}
        </template>
      </el-table-column>
      <el-table-column prop="resourceName" label="资源名称" min-width="150" />
      <el-table-column prop="resourceCode" label="资源编码" min-width="150" />
      <el-table-column prop="resourceType" label="资源类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getResourceTypeTag(row.resourceType)" size="small">
            {{ getResourceTypeText(row.resourceType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径/URL" min-width="180">
        <template #default="{ row }">{{ row.path || '-' }}</template>
      </el-table-column>
      <el-table-column prop="icon" label="图标" width="100">
        <template #default="{ row }">
          <span v-if="row.icon">
            <el-icon><component :is="row.icon" /></el-icon>
            {{ row.icon }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="editResource(row)">编辑</el-button>
          <el-popconfirm title="确认删除该资源吗？" @confirm="deleteResource(row)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
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
        <el-form-item label="资源名称" prop="resourceName">
          <el-input v-model="resourceForm.resourceName" placeholder="请输入资源名称" />
        </el-form-item>
        <el-form-item label="资源编码" prop="resourceCode">
          <el-input v-model="resourceForm.resourceCode" placeholder="请输入资源编码，如：SYS_RESOURCE" />
        </el-form-item>
        <el-form-item label="资源类型" prop="resourceType">
          <el-select v-model="resourceForm.resourceType" placeholder="请选择资源类型" style="width: 100%">
            <el-option label="菜单" value="menu" />
            <el-option label="按钮" value="button" />
            <el-option label="页面" value="page" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级资源" prop="parentId">
          <el-tree-select
            v-model="resourceForm.parentId"
            :data="resourceTree"
            :props="{ label: 'resourceName', value: 'id', children: 'children' }"
            placeholder="请选择父级资源"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="路径/URL" prop="path">
          <el-input v-model="resourceForm.path" placeholder="请输入路径或URL，如：/system/resource" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="resourceForm.icon" placeholder="请输入图标名称，如：Setting" />
          <div class="icon-tip">常用图标：Setting、User、Menu、Document、FolderOpened等</div>
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
        <el-form-item label="备注" prop="remark">
          <el-input v-model="resourceForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
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

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

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
  resourceName: '',
  resourceCode: '',
  resourceType: 'menu',
  parentId: null,
  path: '',
  icon: '',
  sort: 0,
  status: 1,
  remark: ''
})

// 表单验证规则
const formRules = {
  resourceName: [
    { required: true, message: '请输入资源名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  resourceCode: [
    { required: true, message: '请输入资源编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '资源编码应以大写字母开头，只能包含大写字母、数字和下划线', trigger: 'blur' }
  ],
  resourceType: [
    { required: true, message: '请选择资源类型', trigger: 'change' }
  ],
  sort: [
    { type: 'number', message: '排序必须为数字', trigger: 'blur' }
  ]
}

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑资源' : '新增资源')

// 资源树（用于选择父级）
const resourceTree = computed(() => {
  const buildTree = (items, parentId = null) => {
    return items
      .filter(item => item.parentId === parentId)
      .map(item => ({
        ...item,
        children: buildTree(items, item.id)
      }))
  }
  return buildTree(resources.value)
})

// 筛选后的资源列表
const filteredResources = computed(() => {
  let list = resources.value
  if (searchForm.resourceName) {
    list = list.filter(r => r.resourceName.includes(searchForm.resourceName))
  }
  if (searchForm.resourceType) {
    list = list.filter(r => r.resourceType === searchForm.resourceType)
  }
  return list
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

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
}

// 加载资源列表
const loadResources = async () => {
  loading.value = true
  setTimeout(() => {
    resources.value = [
      { id: 1, resourceName: '系统管理', resourceCode: 'SYSTEM_MANAGE', resourceType: 'menu', parentId: null, path: '/system', icon: 'Setting', sort: 1, status: 1, createTime: '2026-03-20T10:00:00', remark: '系统管理模块' },
      { id: 2, resourceName: '个人信息', resourceCode: 'USER_PROFILE', resourceType: 'menu', parentId: 1, path: '/system/profile', icon: 'User', sort: 1, status: 1, createTime: '2026-03-20T10:00:00', remark: '' },
      { id: 3, resourceName: '用户管理', resourceCode: 'USER_MANAGE', resourceType: 'menu', parentId: 1, path: '/system/users', icon: 'UserFilled', sort: 2, status: 1, createTime: '2026-03-20T10:00:00', remark: '' },
      { id: 4, resourceName: '角色管理', resourceCode: 'ROLE_MANAGE', resourceType: 'menu', parentId: 1, path: '/system/roles', icon: 'Management', sort: 3, status: 1, createTime: '2026-03-20T10:00:00', remark: '' },
      { id: 5, resourceName: '资源管理', resourceCode: 'RESOURCE_MANAGE', resourceType: 'menu', parentId: 1, path: '/system/resources', icon: 'FolderOpened', sort: 4, status: 1, createTime: '2026-03-20T10:00:00', remark: '' },
      { id: 6, resourceName: '新增用户', resourceCode: 'USER_ADD', resourceType: 'button', parentId: 3, path: '', icon: 'Plus', sort: 1, status: 1, createTime: '2026-03-21T09:30:00', remark: '' },
      { id: 7, resourceName: '编辑用户', resourceCode: 'USER_EDIT', resourceType: 'button', parentId: 3, path: '', icon: 'Edit', sort: 2, status: 1, createTime: '2026-03-21T09:30:00', remark: '' },
      { id: 8, resourceName: '删除用户', resourceCode: 'USER_DELETE', resourceType: 'button', parentId: 3, path: '', icon: 'Delete', sort: 3, status: 1, createTime: '2026-03-21T09:30:00', remark: '' },
      { id: 9, resourceName: 'GGBond', resourceCode: 'SYS_GGBond', resourceType: 'menu', parentId: null, path: '/ggbondmanagent', icon: 'bond', sort: 0, status: 1, createTime: '2026-03-22T14:20:00', remark: '' },
      { id: 10, resourceName: 'wada', resourceCode: 'wada', resourceType: 'menu', parentId: null, path: '', icon: '', sort: 0, status: 1, createTime: '2026-03-23T11:15:00', remark: '' }
    ]
    pagination.total = resources.value.length
    loading.value = false
  }, 300)
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
    resourceName: '',
    resourceCode: '',
    resourceType: 'menu',
    parentId: null,
    path: '',
    icon: '',
    sort: 0,
    status: 1,
    remark: ''
  })
  dialogVisible.value = true
}

// 编辑资源
const editResource = (resource) => {
  isEdit.value = true
  currentResourceId.value = resource.id
  Object.assign(resourceForm, {
    resourceName: resource.resourceName,
    resourceCode: resource.resourceCode,
    resourceType: resource.resourceType,
    parentId: resource.parentId,
    path: resource.path || '',
    icon: resource.icon || '',
    sort: resource.sort || 0,
    status: resource.status,
    remark: resource.remark || ''
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
    setTimeout(() => {
      if (isEdit.value) {
        const index = resources.value.findIndex(r => r.id === currentResourceId.value)
        if (index !== -1) {
          resources.value[index] = { 
            ...resources.value[index], 
            ...resourceForm,
            updateTime: new Date().toISOString()
          }
        }
        ElMessage.success('更新成功')
      } else {
        resources.value.push({
          id: Date.now(),
          ...resourceForm,
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString()
        })
        pagination.total++
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      submitLoading.value = false
    }, 500)
  })
}

// 删除资源
const deleteResource = (resource) => {
  // 检查是否有子资源
  const hasChildren = resources.value.some(r => r.parentId === resource.id)
  if (hasChildren) {
    ElMessage.warning('该资源下存在子资源，请先删除子资源')
    return
  }
  
  const index = resources.value.findIndex(r => r.id === resource.id)
  if (index !== -1) {
    resources.value.splice(index, 1)
    pagination.total--
  }
  ElMessage.success('删除成功')
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

.content-header {
  margin-bottom: 20px;
  background: white;
  padding: 16px 24px;
  border-radius: 12px;
}

.content-header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 500;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem 1.5rem;
  background: white;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.search-form {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 16px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  background: white;
  padding: 12px 20px;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-table th) {
  background-color: #f8fafc;
  color: #1e293b;
  font-weight: 600;
}

:deep(.el-table .el-table__row:hover) {
  background-color: #f8fafc;
}

.icon-tip {
  font-size: 12px;
  color: #8aa5b8;
  margin-top: 4px;
}
</style>