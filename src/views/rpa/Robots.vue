<template>
  <div class="robots-page">
    <div class="page-header">
      <h2>机器人管理</h2>
      <p class="page-desc">管理和监控RPA机器人，按功能分类执行流程任务</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索机器人名称/IP地址..." />
      </div>
      <el-select v-model="categoryFilter" placeholder="分类筛选" clearable style="width: 140px;">
        <el-option v-for="cat in categoryList" :key="cat.code" :label="cat.name" :value="cat.code" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="空闲" value="idle" />
        <el-option label="忙碌" value="busy" />
        <el-option label="离线" value="offline" />
      </el-select>
      <el-button @click="showCategoryDialog">
        <el-icon><FolderOpened /></el-icon> 分类管理
      </el-button>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 注册机器人
      </el-button>
    </div>

    <el-table :data="paginatedRobots" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="机器人名称" min-width="140" />
      <el-table-column prop="ip" label="IP地址" min-width="140" />
      <el-table-column prop="hostname" label="主机名" min-width="120" />
      <el-table-column prop="boundProcessName" label="绑定流程" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="row.boundProcessName" type="info" size="small">{{ row.boundProcessName }}</el-tag>
          <span v-else class="text-muted">未绑定</span>
        </template>
      </el-table-column>
      <el-table-column prop="robotCategory" label="分类" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="getCategoryType(row.robotCategory)" size="small">
            {{ getCategoryText(row.robotCategory) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-select
            v-model="row.status"
            @change="handleStatusChange(row, $event)"
            size="small"
            style="width: 90px;"
            :disabled="statusChanging === row.id"
          >
            <el-option label="空闲" value="idle" />
            <el-option label="忙碌" value="busy" />
            <el-option label="离线" value="offline" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="160" />
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="primary" @click="editRobot(row)">编辑</el-button>
          <el-popconfirm title="确认删除该机器人吗？" @confirm="deleteRobot(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
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

    <!-- 分类管理弹窗 -->
    <el-dialog v-model="categoryDialogVisible" title="分类管理" width="500px">
      <div class="category-list">
        <el-table :data="categoryList" border size="small">
          <el-table-column prop="name" label="分类名称" />
          <el-table-column prop="code" label="分类编码" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="editCategory(row)">编辑</el-button>
              <el-popconfirm title="确认删除该分类吗？" @confirm="deleteCategory(row)">
                <template #reference>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-divider />
      <div class="category-form">
        <h4>{{ editingCategory.id ? '编辑分类' : '新增分类' }}</h4>
        <el-form :model="editingCategory" label-width="80px" size="small">
          <el-form-item label="分类名称">
            <el-input v-model="editingCategory.name" placeholder="请输入分类名称" />
          </el-form-item>
          <el-form-item label="分类编码">
            <el-input v-model="editingCategory.code" placeholder="请输入分类编码，如：CUSTOM" :disabled="editingCategory.id !== null" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveCategory" :loading="categoryLoading">保存</el-button>
            <el-button @click="resetCategoryForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>

    <!-- 注册机器人弹窗 -->
    <el-dialog v-model="createDialogVisible" title="注册机器人" width="640px">
      <el-form :model="createForm" :rules="formRules" ref="createFormRef" label-width="100px">
        <el-form-item label="机器人名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入机器人名称" />
        </el-form-item>
        <el-form-item label="机器人分类" prop="robotCategory">
          <el-select v-model="createForm.robotCategory" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categoryList" :key="cat.code" :label="cat.name" :value="cat.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="能力描述">
          <el-input v-model="createForm.capabilities" type="textarea" :rows="2" placeholder="请输入机器人能力描述" />
        </el-form-item>
        <el-form-item label="IP地址" prop="ip">
          <el-input v-model="createForm.ip" placeholder="请输入IP地址，如：192.168.1.100" />
        </el-form-item>
        <el-form-item label="主机名">
          <el-input v-model="createForm.hostname" placeholder="请输入主机名" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="createForm.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="机器人代码">
          <el-input v-model="createForm.robotCode" type="textarea" :rows="6" placeholder="请输入机器人执行代码..." class="code-textarea" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑机器人弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑机器人" width="640px">
      <el-form :model="editForm" :rules="formRules" ref="editFormRef" label-width="100px">
        <el-form-item label="机器人名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入机器人名称" />
        </el-form-item>
        <el-form-item label="机器人分类" prop="robotCategory">
          <el-select v-model="editForm.robotCategory" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categoryList" :key="cat.code" :label="cat.name" :value="cat.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="机器人状态" prop="status">
          <el-select v-model="editForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="空闲" value="idle" />
            <el-option label="忙碌" value="busy" />
            <el-option label="离线" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定流程">
          <el-input :value="editForm.boundProcessName || '未绑定'" disabled>
            <template #append>
              <el-tag type="info" size="small">由流程仓库绑定</el-tag>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="能力描述">
          <el-input v-model="editForm.capabilities" type="textarea" :rows="2" placeholder="请输入机器人能力描述" />
        </el-form-item>
        <el-form-item label="IP地址" prop="ip">
          <el-input v-model="editForm.ip" placeholder="请输入IP地址，如：192.168.1.100" />
        </el-form-item>
        <el-form-item label="主机名">
          <el-input v-model="editForm.hostname" placeholder="请输入主机名" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="editForm.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="机器人代码">
          <el-input v-model="editForm.robotCode" type="textarea" :rows="8" placeholder="请输入机器人执行代码..." class="code-textarea" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, FolderOpened } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitLoading = ref(false)
const statusChanging = ref(null)
const categoryLoading = ref(false)
const robots = ref([])
const categoryList = ref([])
const searchKeyword = ref('')
const categoryFilter = ref('')
const statusFilter = ref('')

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const categoryDialogVisible = ref(false)
const createFormRef = ref(null)
const editFormRef = ref(null)
const currentEditId = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const createForm = reactive({
  name: '',
  robotCategory: 'GENERAL',
  capabilities: '',
  ip: '',
  hostname: '',
  port: 8080,
  robotCode: '',
  description: ''
})

const editForm = reactive({
  name: '',
  robotCategory: 'GENERAL',
  status: 'idle',
  boundProcessId: null,
  boundProcessName: '',
  capabilities: '',
  ip: '',
  hostname: '',
  port: 8080,
  robotCode: '',
  description: ''
})

const editingCategory = reactive({
  id: null,
  name: '',
  code: ''
})

const formRules = {
  name: [{ required: true, message: '请输入机器人名称', trigger: 'blur' }],
  ip: [{ required: true, message: '请输入IP地址', trigger: 'blur' }]
}

const getCategoryText = (category) => {
  const cat = categoryList.value.find(c => c.code === category)
  if (cat) return cat.name
  const map = {
    'DATA_COLLECT': '数据采集',
    'DATA_PARSE': '数据解析',
    'DATA_PROCESS': '数据加工',
    'GENERAL': '通用执行'
  }
  return map[category] || category || '通用执行'
}

const getCategoryType = (category) => {
  const map = {
    'DATA_COLLECT': 'primary',
    'DATA_PARSE': 'success',
    'DATA_PROCESS': 'warning',
    'GENERAL': 'info'
  }
  return map[category] || 'info'
}

const getStatusText = (s) => {
  const map = { idle: '空闲', busy: '忙碌', offline: '离线' }
  return map[s] || s
}

const getStatusType = (s) => {
  const map = { idle: 'success', busy: 'warning', offline: 'danger' }
  return map[s] || 'info'
}

const filteredRobots = computed(() => {
  let list = robots.value
  if (searchKeyword.value) {
    list = list.filter(r => r.name.includes(searchKeyword.value) || r.ip?.includes(searchKeyword.value))
  }
  if (categoryFilter.value) {
    list = list.filter(r => r.robotCategory === categoryFilter.value)
  }
  if (statusFilter.value) {
    list = list.filter(r => r.status === statusFilter.value)
  }
  pagination.total = list.length
  return list
})

const paginatedRobots = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredRobots.value.slice(start, end)
})

const loadRobots = async () => {
  loading.value = true
  try {
    const result = await apiGet('/robot')
    if (result.code === 0) {
      robots.value = result.data || []
    }
  } catch {
    robots.value = []
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const result = await apiGet('/robot/category/list')
    if (result.code === 0) {
      categoryList.value = result.data || []
    }
  } catch {
    // 使用默认分类
    categoryList.value = [
      { code: 'DATA_COLLECT', name: '数据采集' },
      { code: 'DATA_PARSE', name: '数据解析' },
      { code: 'DATA_PROCESS', name: '数据加工' },
      { code: 'GENERAL', name: '通用执行' }
    ]
  }
}

const showCategoryDialog = () => {
  categoryDialogVisible.value = true
}

const resetCategoryForm = () => {
  editingCategory.id = null
  editingCategory.name = ''
  editingCategory.code = ''
}

const editCategory = (cat) => {
  editingCategory.id = cat.code
  editingCategory.name = cat.name
  editingCategory.code = cat.code
}

const saveCategory = async () => {
  if (!editingCategory.name || !editingCategory.code) {
    ElMessage.warning('请填写完整的分类信息')
    return
  }

  categoryLoading.value = true
  try {
    if (editingCategory.id) {
      // 更新
      const result = await apiPut(`/robot/category/${editingCategory.id}`, {
        name: editingCategory.name,
        code: editingCategory.code
      })
      if (result.code === 0) {
        ElMessage.success('分类更新成功')
        await loadCategories()
        resetCategoryForm()
      } else {
        ElMessage.error(result.message || '更新失败')
      }
    } else {
      // 新增
      const result = await apiPost('/robot/category', {
        name: editingCategory.name,
        code: editingCategory.code
      })
      if (result.code === 0) {
        ElMessage.success('分类添加成功')
        await loadCategories()
        resetCategoryForm()
      } else {
        ElMessage.error(result.message || '添加失败')
      }
    }
  } catch {
    ElMessage.error('请求失败')
  } finally {
    categoryLoading.value = false
  }
}

const deleteCategory = async (cat) => {
  // 不能删除默认分类
  if (['DATA_COLLECT', 'DATA_PARSE', 'DATA_PROCESS', 'GENERAL'].includes(cat.code)) {
    ElMessage.warning('默认分类不能删除')
    return
  }
  try {
    const result = await apiDelete(`/robot/category/${cat.code}`)
    if (result.code === 0) {
      ElMessage.success('删除成功')
      await loadCategories()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

const showCreateModal = () => {
  Object.assign(createForm, {
    name: '',
    robotCategory: categoryList.value[0]?.code || 'GENERAL',
    capabilities: '',
    ip: '',
    hostname: '',
    port: 8080,
    robotCode: '',
    description: ''
  })
  createDialogVisible.value = true
}

const editRobot = (robot) => {
  currentEditId.value = robot.id
  Object.assign(editForm, {
    name: robot.name || '',
    robotCategory: robot.robotCategory || categoryList.value[0]?.code || 'GENERAL',
    status: robot.status || 'idle',
    boundProcessId: robot.boundProcessId || null,
    boundProcessName: robot.boundProcessName || '',
    capabilities: robot.capabilities || '',
    ip: robot.ip || '',
    hostname: robot.hostname || '',
    port: robot.port || 8080,
    robotCode: robot.robotCode || '',
    description: robot.description || ''
  })
  editDialogVisible.value = true
}

const viewDetail = (robot) => {
  router.push(`/rpa/robot/${robot.id}`)
}

const handleStatusChange = async (robot, newStatus) => {
  if (statusChanging.value === robot.id) return
  statusChanging.value = robot.id
  try {
    const result = await apiPut(`/robot/${robot.id}`, {
      status: newStatus
    })
    if (result.code === 0) {
      robot.status = newStatus
      ElMessage.success('状态更新成功')
    } else {
      ElMessage.error(result.message || '状态更新失败')
      await loadRobots()
    }
  } catch {
    ElMessage.error('状态更新失败')
    await loadRobots()
  } finally {
    statusChanging.value = null
  }
}

const submitCreate = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const result = await apiPost('/robot', {
          name: createForm.name,
          robotCategory: createForm.robotCategory,
          capabilities: createForm.capabilities,
          ip: createForm.ip,
          hostname: createForm.hostname,
          port: createForm.port,
          robotCode: createForm.robotCode,
          description: createForm.description
        })
        if (result.code === 0) {
          ElMessage.success('注册成功')
          createDialogVisible.value = false
          await loadRobots()
        } else {
          ElMessage.error(result.message || '注册失败')
        }
      } catch {
        ElMessage.error('请求失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const submitEdit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const result = await apiPut(`/robot/${currentEditId.value}`, {
          name: editForm.name,
          robotCategory: editForm.robotCategory,
          status: editForm.status,
          capabilities: editForm.capabilities,
          ip: editForm.ip,
          hostname: editForm.hostname,
          port: editForm.port,
          robotCode: editForm.robotCode,
          description: editForm.description
        })
        if (result.code === 0) {
          ElMessage.success('更新成功')
          editDialogVisible.value = false
          await loadRobots()

          const detailRobotId = route.params.id
          if (detailRobotId && Number(detailRobotId) === currentEditId.value) {
            router.go(0)
          }
        } else {
          ElMessage.error(result.message || '更新失败')
        }
      } catch {
        ElMessage.error('请求失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const deleteRobot = async (robot) => {
  try {
    const result = await apiDelete(`/robot/${robot.id}`)
    if (result.code === 0) {
      const index = robots.value.findIndex(r => r.id === robot.id)
      if (index !== -1) {
        robots.value.splice(index, 1)
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

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => {
  loadRobots()
  loadCategories()
})
</script>

<style scoped>
.robots-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }
.text-muted { color: #909399; font-size: 13px; }
.code-textarea :deep(textarea) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  background: #1e1e1e;
  color: #d4d4d4;
}
.category-list { margin-bottom: 20px; }
.category-form { margin-top: 16px; }
.category-form h4 { margin-bottom: 12px; color: #303133; }
</style>
