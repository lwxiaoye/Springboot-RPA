<template>
  <div class="rpa-role-management">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <div class="header-left">
        <div class="logo-area">
          <div class="logo-icon">RPA</div>
          <div class="logo-text">RPA运营管理系统</div>
        </div>
      </div>
      <div class="header-center">
        <el-menu
          :default-active="activeTopMenu"
          mode="horizontal"
          class="top-menu"
          :ellipsis="false"
          @select="handleTopMenuSelect"
        >
          <el-menu-item index="dashboard">
            <el-icon><DataLine /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="rpa">
            <el-icon><Monitor /></el-icon>
            <span>RPA运营管理</span>
          </el-menu-item>
          <el-menu-item index="system">
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <div class="user-info">
          <el-dropdown>
            <span class="user-dropdown">
              <el-icon><User /></el-icon>
              {{ currentUser.username }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">个人信息</el-dropdown-item>
                <el-dropdown-item divided>设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 主体内容区域：左侧菜单 + 右侧内容 -->
    <div class="main-layout">
      <!-- 左侧菜单 - 仅当选中系统管理时显示 -->
      <aside class="sidebar" :class="{ 'sidebar-hidden': !showSidebar }">
        <el-menu
          :default-active="activeLeftMenu"
          class="sidebar-menu"
          background-color="#f5f7fa"
          text-color="#303133"
          active-text-color="#1677ff"
          @select="handleLeftMenuSelect"
        >
          <el-menu-item index="userInfo">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="user">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="role">
            <el-icon><Management /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item index="resource">
            <el-icon><FolderOpened /></el-icon>
            <span>资源管理</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 右侧内容区 -->
      <main class="main-content" :class="{ 'content-full': !showSidebar }">
        <!-- 动态内容：根据选中的菜单显示不同内容 -->
        <div v-if="currentView === 'role'">
          <!-- 角色管理内容 -->
          <div class="content-header">
            <div class="header-left">
              <h2>角色管理</h2>
              <el-breadcrumb separator="/">
                <el-breadcrumb-item>系统管理</el-breadcrumb-item>
                <el-breadcrumb-item>角色管理</el-breadcrumb-item>
              </el-breadcrumb>
            </div>
            <div class="header-right">
              <el-button type="primary" @click="openAddDialog" :loading="loading">
                <el-icon><Plus /></el-icon> 新增角色
              </el-button>
            </div>
          </div>

          <!-- 查询表单 -->
          <div class="search-form">
            <el-form :inline="true" :model="searchForm" class="demo-form-inline">
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
            <el-table-column prop="roleCode" label="角色编码" min-width="120" />
            <el-table-column prop="roleName" label="角色名称" min-width="120" />
            <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column label="权限" width="100" align="center">
              <template #default="{ row }">
                <span>{{ row.permissionDesc || '无权限' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="userCount" label="用户数" width="80" align="center" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">
                  {{ row.status }}
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
        </div>
      </main>
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
            <el-radio label="启用">启用</el-radio>
            <el-radio label="禁用">禁用</el-radio>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataLine,
  Monitor,
  Setting,
  User,
  ArrowDown,
  Plus,
  Management,
  FolderOpened,
  UserFilled
} from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()

// ==================== API 配置 ====================
const API_BASE_URL = 'http://localhost:8080/api'

// 创建 axios 实例
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加 token
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          ElMessage.error('未授权，请重新登录')
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error('请求配置错误')
    }
    return Promise.reject(error)
  }
)

// ==================== 数据 ====================
const tableData = ref([])
const searchForm = reactive({
  roleName: '',
  roleCode: ''
})
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 加载状态
const loading = ref(false)
const submitLoading = ref(false)
const permLoading = ref(false)

// 菜单状态
const activeTopMenu = ref('system')
const activeLeftMenu = ref('role')
const showSidebar = ref(true)
const currentView = ref('role')

// 当前用户
const currentUser = ref({
  username: '系统管理员'
})

// 角色相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const roleFormRef = ref(null)
const roleForm = reactive({
  roleCode: '',
  roleName: '',
  description: '',
  status: '启用'
})
const currentEditId = ref(null)

// 权限分配
const permDialogVisible = ref(false)
const currentPermRole = ref(null)
const treeRef = ref(null)
const defaultCheckedKeys = ref([])
const permissionTree = ref([
  { id: 1, name: 'RPA运营管理', children: [{ id: 11, name: '任务管理' }, { id: 12, name: '执行记录' }] },
  { id: 2, name: '系统管理', children: [{ id: 21, name: '用户管理' }, { id: 22, name: '角色管理' }, { id: 23, name: '资源管理' }] },
  { id: 3, name: '仪表盘' }
])

// 表单校验规则
const formRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

// ==================== API 方法 ====================

// 获取角色列表
const fetchRoleList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.page,
      pageSize: pagination.size,
      roleName: searchForm.roleName,
      roleCode: searchForm.roleCode
    }
    
    const response = await api.get('/roles', { params })
    
    if (response.code === 200 || response.success) {
      const data = response.data || response
      tableData.value = data.records || data.list || []
      pagination.total = data.total || 0
    } else {
      if (import.meta.env.DEV) {
        useMockData()
      } else {
        ElMessage.error(response.message || '获取角色列表失败')
      }
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
    if (import.meta.env.DEV) {
      useMockData()
    }
  } finally {
    loading.value = false
  }
}

// 模拟数据
const useMockData = () => {
  tableData.value = [
    { id: 1, roleCode: 'Greatbd', roleName: 'GG', description: '有所有权限', permissionDesc: '所有权限', userCount: 0, status: '启用', createTime: '2026-03-23T17:10:30' },
    { id: 2, roleCode: 'MANAGER', roleName: '经理', description: '项目经理', permissionDesc: '无权限', userCount: 0, status: '启用', createTime: '2026-03-23T16:54:49' },
    { id: 3, roleCode: 'ADMIN', roleName: '系统管理员', description: '拥有所有权限', permissionDesc: '无权限', userCount: 0, status: '启用', createTime: '2026-03-16T21:09:34' },
    { id: 4, roleCode: 'OPERATOR', roleName: '操作员', description: '可以创建和管理任务', permissionDesc: '无权限', userCount: 0, status: '启用', createTime: '2026-03-16T21:09:34' },
    { id: 5, roleCode: 'VIEWER', roleName: '查看者', description: '只能查看数据', permissionDesc: '无权限', userCount: 0, status: '启用', createTime: '2026-03-16T21:09:34' }
  ]
  pagination.total = 5
}

// 创建角色
const createRole = async (data) => {
  try {
    const response = await api.post('/roles', data)
    if (response.code === 200 || response.success) {
      ElMessage.success('创建成功')
      await fetchRoleList()
      return true
    } else {
      ElMessage.error(response.message || '创建失败')
      return false
    }
  } catch (error) {
    console.error('创建角色失败:', error)
    ElMessage.error('创建角色失败')
    return false
  }
}

// 更新角色
const updateRole = async (id, data) => {
  try {
    const response = await api.put(`/roles/${id}`, data)
    if (response.code === 200 || response.success) {
      ElMessage.success('更新成功')
      await fetchRoleList()
      return true
    } else {
      ElMessage.error(response.message || '更新失败')
      return false
    }
  } catch (error) {
    console.error('更新角色失败:', error)
    ElMessage.error('更新角色失败')
    return false
  }
}

// 删除角色
const deleteRoleApi = async (id) => {
  try {
    const response = await api.delete(`/roles/${id}`)
    if (response.code === 200 || response.success) {
      ElMessage.success('删除成功')
      await fetchRoleList()
      return true
    } else {
      ElMessage.error(response.message || '删除失败')
      return false
    }
  } catch (error) {
    console.error('删除角色失败:', error)
    ElMessage.error('删除角色失败')
    return false
  }
}

// 获取角色权限
const getRolePermissions = async (roleId) => {
  try {
    const response = await api.get(`/roles/${roleId}/permissions`)
    if (response.code === 200 || response.success) {
      const permissions = response.data || response
      return permissions.map(p => p.id || p)
    }
    return []
  } catch (error) {
    console.error('获取角色权限失败:', error)
    return []
  }
}

// 分配角色权限
const assignRolePermissions = async (roleId, permissionIds) => {
  try {
    const response = await api.post(`/roles/${roleId}/permissions`, { permissionIds })
    if (response.code === 200 || response.success) {
      ElMessage.success('权限分配成功')
      return true
    } else {
      ElMessage.error(response.message || '权限分配失败')
      return false
    }
  } catch (error) {
    console.error('分配权限失败:', error)
    ElMessage.error('分配权限失败')
    return false
  }
}

// ==================== 页面跳转 ====================

// 跳转到个人信息页面
const goToProfile = () => {
  router.push('/profile')
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}

// ==================== 事件处理 ====================

// 顶部菜单选择
const handleTopMenuSelect = (index) => {
  activeTopMenu.value = index
  if (index === 'system') {
    showSidebar.value = true
    activeLeftMenu.value = currentView.value
  } else {
    showSidebar.value = false
    if (index === 'dashboard') {
      ElMessage.info('首页功能开发中')
    } else if (index === 'rpa') {
      ElMessage.info('RPA运营管理功能开发中')
    }
  }
}

// 左侧菜单选择
const handleLeftMenuSelect = (index) => {
  activeLeftMenu.value = index
  currentView.value = index
  
  if (index === 'role') {
    fetchRoleList()
  } else if (index === 'userInfo') {
    // 跳转到个人信息页面
    router.push('/profile')
  } else if (index === 'user') {
    ElMessage.info('用户管理功能开发中')
  } else if (index === 'resource') {
    ElMessage.info('资源管理功能开发中')
  }
}

// 计算分页后的数据
const filteredTableData = computed(() => {
  return tableData.value
})

// 查询
const handleSearch = () => {
  pagination.page = 1
  fetchRoleList()
}

// 重置查询
const resetSearch = () => {
  searchForm.roleName = ''
  searchForm.roleCode = ''
  pagination.page = 1
  fetchRoleList()
}

// 分页变化
const handleSizeChange = (val) => {
  pagination.size = val
  pagination.page = 1
  fetchRoleList()
}

const handleCurrentChange = (val) => {
  pagination.page = val
  fetchRoleList()
}

// 打开新增弹窗
const openAddDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
  roleForm.roleCode = ''
  roleForm.roleName = ''
  roleForm.description = ''
  roleForm.status = '启用'
  currentEditId.value = null
  roleFormRef.value?.resetFields()
}

// 编辑角色
const editRole = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  currentEditId.value = row.id
  roleForm.roleCode = row.roleCode
  roleForm.roleName = row.roleName
  roleForm.description = row.description
  roleForm.status = row.status
  roleFormRef.value?.clearValidate()
}

// 提交角色
const submitRole = async () => {
  if (!roleFormRef.value) return
  
  await roleFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const data = {
          roleCode: roleForm.roleCode,
          roleName: roleForm.roleName,
          description: roleForm.description,
          status: roleForm.status === '启用' ? 1 : 0
        }
        
        let success = false
        if (isEdit.value) {
          success = await updateRole(currentEditId.value, data)
        } else {
          success = await createRole(data)
        }
        
        if (success) {
          dialogVisible.value = false
        }
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 删除角色
const deleteRole = async (row) => {
  await deleteRoleApi(row.id)
}

// 分配权限
const assignPermissions = async (row) => {
  currentPermRole.value = row
  permDialogVisible.value = true
  permLoading.value = true
  
  try {
    const checkedIds = await getRolePermissions(row.id)
    defaultCheckedKeys.value = checkedIds
    
    setTimeout(() => {
      if (treeRef.value) {
        treeRef.value.setCheckedKeys(checkedIds)
      }
    }, 100)
  } finally {
    permLoading.value = false
  }
}

// 保存权限
const savePermissions = async () => {
  if (!treeRef.value || !currentPermRole.value) return
  
  permLoading.value = true
  try {
    const checkedKeys = treeRef.value.getCheckedKeys()
    const halfCheckedKeys = treeRef.value.getHalfCheckedKeys()
    const allCheckedKeys = [...checkedKeys, ...halfCheckedKeys]
    
    const success = await assignRolePermissions(currentPermRole.value.id, allCheckedKeys)
    if (success) {
      permDialogVisible.value = false
    }
  } finally {
    permLoading.value = false
  }
}

// 动态标题
const dialogTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')

// 页面加载
onMounted(() => {
  fetchRoleList()
  // 从 localStorage 加载用户信息
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo)
      currentUser.value.username = user.realName || user.username || '系统管理员'
    } catch (e) {}
  }
})
</script>

<style scoped>
.rpa-role-management {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background-color: #f0f2f6;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

.top-header {
  height: 60px;
  background-color: #001529;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  flex-shrink: 0;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: #1677ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
}

.logo-text {
  color: white;
  font-weight: 500;
  font-size: 16px;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.top-menu {
  background-color: transparent;
  border-bottom: none;
}

.top-menu .el-menu-item {
  color: #ffffffa6;
  border-bottom: 2px solid transparent;
}

.top-menu .el-menu-item:hover {
  color: #fff;
  background-color: rgba(255,255,255,0.1);
}

.top-menu .el-menu-item.is-active {
  color: #fff;
  border-bottom-color: #1677ff;
  background-color: transparent;
}

.header-right {
  flex-shrink: 0;
}

.user-info {
  color: #ffffffa6;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: white;
}

.user-dropdown:hover {
  color: #1677ff;
}

.main-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.sidebar {
  width: 220px;
  background-color: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  transition: width 0.3s ease;
  overflow-y: auto;
}

.sidebar-hidden {
  width: 0;
  overflow: hidden;
  border-right: none;
}

.sidebar-menu {
  border-right: none;
  background-color: #f5f7fa;
}

.sidebar-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 8px;
  border-radius: 8px;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #e6f2ff;
  color: #1677ff;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #eef2f6;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  transition: all 0.3s ease;
}

.content-full {
  padding: 20px 24px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}

.header-left h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 500;
}

.search-form {
  background: white;
  padding: 20px 24px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  background: white;
  padding: 12px 20px;
  border-radius: 12px;
}

:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-table th) {
  background-color: #fafafa;
}
</style>