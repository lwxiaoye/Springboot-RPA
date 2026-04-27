<template>
  <div class="data-process-page">
    <div class="page-header">
      <h2>数据加工</h2>
      <p class="page-desc">配置和管理数据加工任务</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索加工名称..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="成功" value="success" />
        <el-option label="运行中" value="running" />
        <el-option label="待执行" value="pending" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 新建加工
      </el-button>
    </div>

    <el-table :data="paginatedData" v-loading="loading" border stripe class="unified-table" :default-sort="{ prop: 'createTime', order: 'descending' }">
      <el-table-column type="index" label="序号" width="60" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="加工名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="processType" label="加工类型" width="100" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="processedCount" label="处理数量" width="100" align="center" />
      <el-table-column prop="lastProcessTime" label="处理时间" min-width="160">
        <template #default="{ row }">{{ row.lastProcessTime ? new Date(row.lastProcessTime).toLocaleString() : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="success" @click="runTask(row)" class="action-btn">
              <span>执行</span>
            </el-button>
            <el-button link type="primary" @click="editTask(row)" class="action-btn">
              <span>编辑</span>
            </el-button>
            <el-popconfirm
              :title="'确定要删除加工任务「' + row.name + '」吗？'"
              confirmButtonText="确认删除"
              cancelButtonText="取消"
              icon="Delete"
              iconColor="#f56c6c"
              @confirm="deleteTask(row)"
            >
              <template #reference>
                <el-button link type="danger" class="action-btn">
                  <span>删除</span>
                </el-button>
              </template>
            </el-popconfirm>
          </div>
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

    <!-- 新建/编辑加工弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="dataForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="加工名称" prop="name">
          <el-input v-model="dataForm.name" placeholder="请输入加工名称" />
        </el-form-item>
        <el-form-item label="加工类型">
          <el-select v-model="dataForm.processType" placeholder="请选择加工类型" style="width: 100%">
            <el-option label="数据清洗" value="清洗" />
            <el-option label="数据转换" value="转换" />
            <el-option label="数据聚合" value="聚合" />
            <el-option label="数据脱敏" value="脱敏" />
          </el-select>
        </el-form-item>
        <el-form-item label="加工规则">
          <el-input v-model="dataForm.processRules" type="textarea" :rows="4" placeholder="请输入加工规则（JSON格式），如：{&quot;rules&quot;: [&quot;去重&quot;, &quot;格式化&quot;]}" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])
const searchKeyword = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentEditId = ref(null)
const formRef = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const dataForm = reactive({
  name: '',
  sourceIds: '',
  processType: '清洗',
  processRules: '',
  outputTable: ''
})

const formRules = {
  name: [{ required: true, message: '请输入加工名称', trigger: 'blur' }]
}

const getStatusText = (s) => {
  const map = { success: '成功', running: '运行中', pending: '待执行', failed: '失败' }
  return map[s] || s
}

const getStatusType = (s) => {
  const map = { success: 'success', running: 'warning', pending: 'info', failed: 'danger' }
  return map[s] || 'info'
}

const filteredData = computed(() => {
  let list = dataList.value
  if (searchKeyword.value) {
    list = list.filter(d => d.name.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(d => d.status === statusFilter.value)
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedData = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredData.value.slice(start, end)
})

const loadData = async () => {
  loading.value = true
  try {
    const result = await apiGet('/dataProcess')
    if (result.code === 0) {
      dataList.value = result.data || []
      // 不需要在这里设置 pagination.total，filteredData 会计算
    }
  } catch {
    dataList.value = []
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isEdit.value = false
  currentEditId.value = null
  Object.assign(dataForm, { name: '', sourceIds: '', processType: '清洗', processRules: '', outputTable: '' })
  dialogVisible.value = true
}

const editTask = (item) => {
  isEdit.value = true
  currentEditId.value = item.id
  Object.assign(dataForm, {
    name: item.name,
    sourceIds: item.sourceIds,
    processType: item.processType,
    processRules: item.processRules,
    outputTable: item.outputTable
  })
  dialogVisible.value = true
}

const submitTask = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          const result = await apiPut(`/dataProcess/${currentEditId.value}`, {
            name: dataForm.name,
            sourceIds: dataForm.sourceIds,
            processType: dataForm.processType,
            processRules: dataForm.processRules,
            outputTable: dataForm.outputTable
          })
          if (result.code === 0) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            await loadData()
          } else {
            ElMessage.error(result.message || '更新失败')
          }
        } else {
          const result = await apiPost('/dataProcess', {
            name: dataForm.name,
            sourceIds: dataForm.sourceIds,
            processType: dataForm.processType,
            processRules: dataForm.processRules,
            outputTable: dataForm.outputTable
          })
          if (result.code === 0) {
            ElMessage.success('创建成功')
            dialogVisible.value = false
            await loadData()
          } else {
            ElMessage.error(result.message || '创建失败')
          }
        }
      } catch {
        ElMessage.error('请求失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const runTask = async (item) => {
  try {
    const result = await apiPost(`/dataProcess/${item.id}/execute`)
    if (result.code === 0 || result.success) {
      ElMessage.success(`加工任务已启动: ${item.name}`)
      await loadData()
    } else {
      ElMessage.error(result.message || '执行失败')
    }
  } catch {
    ElMessage.error('请求失败')
  }
}

const deleteTask = async (item) => {
  try {
    const result = await apiDelete(`/dataProcess/${item.id}`)
    if (result.code === 0) {
      const index = dataList.value.findIndex(d => d.id === item.id)
      if (index !== -1) {
        dataList.value.splice(index, 1)
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

onMounted(() => { loadData() })
</script>

<style scoped>
.data-process-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid var(--border-color, #e5e7eb); }
.page-header h2 { font-size: 28px; font-weight: 700; color: var(--text-primary, #1f2937); margin: 0 0 8px 0; letter-spacing: -0.5px; }
.page-desc { font-size: 14px; color: var(--text-secondary, #6b7280); margin: 0; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; flex-wrap: wrap; padding: 16px 20px; background: var(--bg-secondary, #ffffff); border-radius: 12px; border: 1px solid var(--border-color, #e5e7eb); }
.search-box { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: var(--bg-tertiary, #f9fafb); border: 1px solid var(--border-color, #e5e7eb); border-radius: 10px; flex: 1; max-width: 320px; transition: all 0.2s; }
.search-box:focus-within { border-color: var(--primary, #409eff); box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1); }
.search-box input { border: none; outline: none; flex: 1; background: transparent; font-size: 14px; }
.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; background: var(--bg-secondary, #ffffff); padding: 12px 16px; border-radius: 10px; border: 1px solid var(--border-color, #e5e7eb); }

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
  gap: 2px;
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
</style>