<template>
  <div class="role-management-content">

    <div class="section-header">
      <h2 class="section-title">{{ t('role.title') }}</h2>
      <el-button type="primary" @click="openAddDialog" :loading="loading">
        <el-icon><Plus /></el-icon> {{ t('role.addRole') }}
      </el-button>
    </div>

    <!-- 查询表单 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item :label="t('role.roleName') + '：'">
          <el-input v-model="searchForm.roleName" :placeholder="t('role.input')" clearable />
        </el-form-item>
        <el-form-item :label="t('role.roleCode') + '：'">
          <el-input v-model="searchForm.roleCode" :placeholder="t('role.input')" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">{{ t('role.query') }}</el-button>
          <el-button @click="resetSearch">{{ t('role.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 角色表格 -->
    <el-table :data="paginatedTableData" border stripe v-loading="loading" style="width: 100%" class="unified-table">
      <el-table-column type="index" :label="t('role.seq')" width="60" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="code" :label="t('role.roleCode')" min-width="120" />
      <el-table-column prop="name" :label="t('role.roleName')" min-width="120" />
      <el-table-column prop="description" :label="t('role.description')" min-width="180" show-overflow-tooltip />
      <el-table-column :label="t('role.permissions')" width="100" align="center">
        <template #default="{ row }">
          <span>{{ row.permissionCount || 0 }} {{ t('role.permissionCount') }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="userCount" :label="t('role.userCount')" width="80" align="center" />
      <el-table-column :label="t('common.status')" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? t('role.enable') : t('role.disable') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="t('common.createTime')" min-width="170" />
      <el-table-column :label="t('common.actions')" fixed="right" width="220" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-tooltip :content="t('role.edit')" placement="top" :show-after="300">
              <el-button link type="primary" size="small" @click="editRole(row)" class="action-btn">
                <el-icon class="action-icon"><Edit /></el-icon>
                <span>{{ t('role.edit') }}</span>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="t('role.assignPermissions')" placement="top" :show-after="300">
              <el-button link type="success" size="small" @click="assignPermissions(row)" class="action-btn">
                <el-icon class="action-icon"><Key /></el-icon>
                <span>{{ t('role.permissions') }}</span>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="t('role.deleteRole')" placement="top" :show-after="300">
              <el-popconfirm
                :title="t('role.confirmDelete') + row.name + '？'"
                :confirmButtonText="t('role.confirmDeleteBtn')"
                :cancelButtonText="t('common.cancel')"
                icon="Delete"
                iconColor="#f56c6c"
                @confirm="deleteRole(row)"
              >
                <template #reference>
                  <el-button link type="danger" size="small" class="action-btn danger-btn">
                    <el-icon class="action-icon"><Delete /></el-icon>
                    <span>{{ t('common.delete') }}</span>
                  </el-button>
                </template>
              </el-popconfirm>
            </el-tooltip>
          </div>
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
        <el-form-item :label="t('role.roleCode')" prop="roleCode">
          <el-input v-model="roleForm.roleCode" :placeholder="t('role.inputRoleCode')" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('role.roleName')" prop="roleName">
          <el-input v-model="roleForm.roleName" :placeholder="t('role.inputRoleName')" />
        </el-form-item>
        <el-form-item :label="t('role.description')" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" :placeholder="t('role.input')" />
        </el-form-item>
        <el-form-item :label="t('common.status')" prop="status">
          <el-radio-group v-model="roleForm.status">
            <el-radio :label="1">{{ t('role.enable') }}</el-radio>
            <el-radio :label="0">{{ t('role.disable') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" @click="submitRole" :loading="submitLoading">{{ t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog
      v-model="permDialogVisible"
      :title="t('role.assignPermissions')"
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
        <el-button @click="permDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="savePermissions" :loading="permLoading">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus, Edit, Delete, Key } from '@element-plus/icons-vue'

import { apiGet, apiPost, apiPut } from '../../utils/api.js'

const { t } = useI18n()

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
  roleCode: [{ required: true, message: t('role.inputRoleCode'), trigger: 'blur' }],
  roleName: [{ required: true, message: t('role.inputRoleName'), trigger: 'blur' }]
}

const fetchRoleList = async () => {
  loading.value = true
  try {
    const result = await apiGet('/role')
    if (result.code === 0) {
      tableData.value = result.data || []
      // 不需要在这里设置 pagination.total，filteredTableData 会计算
    }
  } catch {
    tableData.value = []
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
      ElMessage.success(t('role.createSuccess'))
      await fetchRoleList()
      return true
    }
    ElMessage.error(result.message || t('role.createFailed'))
    return false
  } catch {
    ElMessage.error(t('role.requestFailed'))
    return false
  }
}

const updateRole = async (id, data) => {
  try {
    const result = await apiPut(`/role/${id}`, data)
    if (result.code === 0) {
      ElMessage.success(t('role.updateSuccess'))
      await fetchRoleList()
      return true
    }
    ElMessage.error(result.message || t('role.requestFailed'))
    return false
  } catch {
    ElMessage.error(t('role.requestFailed'))
    return false
  }
}

const deleteRoleApi = async (id) => {
  try {
    const result = await apiDelete(`/role/${id}`)
    if (result.code === 0) {
      ElMessage.success(t('role.deleteSuccess'))
      await fetchRoleList()
      return true
    }
    ElMessage.error(result.message || t('role.deleteFailed'))
    return false
  } catch {
    ElMessage.error(t('role.deleteFailed'))
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
      ElMessage.success(t('role.updateSuccess'))
      return true
    }
    ElMessage.error(result.message || t('role.requestFailed'))
    return false
  } catch {
    ElMessage.error(t('role.requestFailed'))
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

const filteredTableData = computed(() => {
  let list = tableData.value
  if (searchForm.roleName) {
    list = list.filter(r => r.name.includes(searchForm.roleName))
  }
  if (searchForm.roleCode) {
    list = list.filter(r => r.code.includes(searchForm.roleCode))
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedTableData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredTableData.value.slice(start, end)
})
const dialogTitle = computed(() => isEdit.value ? t('role.editRole') : t('role.addRole'))

onMounted(() => { fetchRoleList(); fetchPermissionTree() })
</script>

<style scoped>
.role-management-content {
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
  border-radius: 12px;
  overflow: hidden;
  border: none !important;
}

:deep(.el-table::before) {
  display: none;
}

:deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  border-bottom: 2px solid #e5e7eb !important;
}

:deep(.el-table__body-wrapper td) {
  background: #fff;
  border-bottom: 1px solid #f0f0f0 !important;
  transition: all 0.2s ease;
}

:deep(.el-table__row) {
  transition: all 0.2s ease;
}

:deep(.el-table__row:hover > td) {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

:deep(.el-table__row:hover > td) {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%) !important;
}

/* 统一表格样式 */
.unified-table :deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  padding: 14px 0;
  border-bottom: 2px solid #e5e7eb !important;
}

.unified-table :deep(.el-table__body-wrapper td) {
  padding: 12px 0;
  vertical-align: middle;
  border-bottom: 1px solid #f0f0f0 !important;
}

.unified-table :deep(.el-table__header-wrapper th .cell),
.unified-table :deep(.el-table__body-wrapper td .cell) {
  padding: 0 16px;
}

.unified-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.unified-table :deep(.el-table__row:hover > td) {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%) !important;
}

.unified-table :deep(.el-table) {
  border: none !important;
}

.unified-table :deep(.el-table::before) {
  display: none;
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
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
  font-size: 13px;
  font-weight: 500;
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(255,255,255,0.5) 50%, transparent 100%);
  transform: translateX(-100%);
  transition: transform 0.4s ease;
}

.action-btn:hover::before {
  transform: translateX(100%);
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-icon {
  font-size: 14px;
  transition: transform 0.2s ease;
}

.action-btn:hover .action-icon {
  transform: scale(1.15);
}

/* 编辑按钮 */
.action-btn[type="primary"]:hover {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.15) 0%, rgba(64, 158, 255, 0.05) 100%);
  color: #409eff;
}

/* 权限按钮 */
.action-btn[type="success"]:hover {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.15) 0%, rgba(103, 194, 58, 0.05) 100%);
  color: #67c23a;
}

/* 删除按钮 */
.danger-btn {
  background: rgba(245, 108, 108, 0.08);
  border: none;
}

.danger-btn:hover {
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.15) 0%, rgba(245, 108, 108, 0.05) 100%);
  color: #f56c6c;
}

/* 分页 */
.pagination-container {
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

/* 输入框 */
:deep(.el-input__wrapper) {
  border-radius: 10px;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary, #409eff) inset;
}
</style>