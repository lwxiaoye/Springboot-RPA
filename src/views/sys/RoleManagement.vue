<template>
  <div class="role-management-content">
  
    <div class="section-header">
      <h2 class="section-title">角色管理</h2>
      <el-button type="primary" @click="openAddDialog" :loading="loading">
        <el-icon><Plus /></el-icon> 新增角色
      </el-button>
    </div>

    <!-- 查询表单 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="角色名称：">
          <el-input v-model="searchForm.roleName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="角色编码：">
          <el-input v-model="searchForm.roleCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 角色表格 -->
    <el-table :data="filteredTableData" border stripe v-loading="loading" style="width: 100%">
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="code" label="角色编码" min-width="120" />
      <el-table-column prop="name" label="角色名称" min-width="120" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="权限" width="100" align="center">
        <template #default="{ row }">
          <span>{{ row.permissionCount || 0 }} 个</span>
        </template>
      </el-table-column>
      <el-table-column prop="userCount" label="用户数" width="80" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" fixed="right" width="240" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="editRole(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="assignPermissions(row)">分配权限</el-button>
          <el-popconfirm title="确认删除该角色吗？" @confirm="deleteRole(row)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form :model="roleForm" :rules="formRules" ref="roleFormRef" label-width="100px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="roleForm.roleCode" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roleForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRole" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog
      v-model="permDialogVisible"
      title="分配权限"
      width="500px"
    >
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
        :default-checked-keys="defaultCheckedKeys"
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions" :loading="permLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import { apiGet, apiPost, apiPut } from '../../utils/api.js'

const tableData = ref([])
const searchForm = reactive({ roleName: '', roleCode: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const loading = ref(false)
const submitLoading = ref(false)
const permLoading = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const roleFormRef = ref(null)
const roleForm = reactive({ roleCode: '', roleName: '', description: '', status: 1 })
const currentEditId = ref(null)

const permDialogVisible = ref(false)
const currentPermRole = ref(null)
const treeRef = ref(null)
const defaultCheckedKeys = ref([])
const permissionTree = ref([])

const formRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const fetchRoleList = async () => {
  loading.value = true
  try {
    const result = await apiGet('/role')
    if (result.code === 0) {
      tableData.value = result.data || []
      pagination.total = tableData.value.length
    }
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const fetchPermissionTree = async () => {
  try {
    const result = await apiGet('/permission/tree')
    if (result.code === 0) {
      permissionTree.value = result.data || []
    }
  } catch {
    permissionTree.value = []
  }
}

const createRole = async (data) => {
  try {
    const result = await apiPost('/role', data)
    if (result.code === 0) {
      ElMessage.success('创建成功')
      await fetchRoleList()
      return true
    }
    ElMessage.error(result.message || '创建失败')
    return false
  } catch {
    ElMessage.error('创建角色失败')
    return false
  }
}

const updateRole = async (id, data) => {
  try {
    const result = await apiPut(`/role/${id}`, data)
    if (result.code === 0) {
      ElMessage.success('更新成功')
      await fetchRoleList()
      return true
    }
    ElMessage.error(result.message || '更新失败')
    return false
  } catch {
    ElMessage.error('更新角色失败')
    return false
  }
}

const deleteRoleApi = async (id) => {
  try {
    const result = await apiDelete(`/role/${id}`)
    if (result.code === 0) {
      ElMessage.success('删除成功')
      await fetchRoleList()
      return true
    }
    ElMessage.error(result.message || '删除失败')
    return false
  } catch {
    ElMessage.error('删除角色失败')
    return false
  }
}

const getRolePermissions = async (roleId) => {
  try {
    const result = await apiGet(`/role/${roleId}`)
    if (result.code === 0) {
      return result.data?.permissionIds || []
    }
    return []
  } catch {
    return []
  }
}

const assignRolePermissions = async (roleId, permissionIds) => {
  try {
    const result = await apiPut(`/role/${roleId}/permissions`, { permissionIds })
    if (result.code === 0) {
      ElMessage.success('权限分配成功')
      return true
    }
    ElMessage.error(result.message || '分配失败')
    return false
  } catch {
    ElMessage.error('分配权限失败')
    return false
  }
}

const handleSearch = () => { pagination.page = 1; fetchRoleList() }
const resetSearch = () => { searchForm.roleName = ''; searchForm.roleCode = ''; pagination.page = 1; fetchRoleList() }
const handleSizeChange = (val) => { pagination.size = val; pagination.page = 1; fetchRoleList() }
const handleCurrentChange = (val) => { pagination.page = val; fetchRoleList() }

const openAddDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
  roleForm.roleCode = ''
  roleForm.roleName = ''
  roleForm.description = ''
  roleForm.status = 1
  currentEditId.value = null
  roleFormRef.value?.resetFields()
}

const editRole = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  currentEditId.value = row.id
  roleForm.roleCode = row.code
  roleForm.roleName = row.name
  roleForm.description = row.description
  roleForm.status = row.status
  roleFormRef.value?.clearValidate()
}

const submitRole = async () => {
  if (!roleFormRef.value) return
  await roleFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const data = {
          name: roleForm.roleName,
          code: roleForm.roleCode,
          description: roleForm.description,
          status: roleForm.status
        }
        let success = false
        if (isEdit.value) {
          success = await updateRole(currentEditId.value, data)
        } else {
          success = await createRole(data)
        }
        if (success) dialogVisible.value = false
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const deleteRole = async (row) => { await deleteRoleApi(row.id) }

const assignPermissions = async (row) => {
  currentPermRole.value = row
  permDialogVisible.value = true
  permLoading.value = true
  try {
    if (permissionTree.value.length === 0) {
      await fetchPermissionTree()
    }
    const checkedIds = await getRolePermissions(row.id)
    defaultCheckedKeys.value = checkedIds
    setTimeout(() => {
      if (treeRef.value) treeRef.value.setCheckedKeys(checkedIds)
    }, 100)
  } finally {
    permLoading.value = false
  }
}

const savePermissions = async () => {
  if (!treeRef.value || !currentPermRole.value) return
  permLoading.value = true
  try {
    const checkedKeys = treeRef.value.getCheckedKeys()
    const halfCheckedKeys = treeRef.value.getHalfCheckedKeys()
    const allCheckedKeys = [...checkedKeys, ...halfCheckedKeys]
    const success = await assignRolePermissions(currentPermRole.value.id, allCheckedKeys)
    if (success) permDialogVisible.value = false
  } finally {
    permLoading.value = false
  }
}

const filteredTableData = computed(() => tableData.value)
const dialogTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')

onMounted(() => { fetchRoleList(); fetchPermissionTree() })
</script>

<style scoped>
.role-management-content {
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
  margin-bottom: 20px;
  background: white;
  padding: 16px 24px;
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
  padding: 20px 24px;
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
}

.pagination-container {
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
  background-color: #fafafa;
}
</style>