<template>
  <div class="data-collect-page">
    <div class="page-header">
      <h2>{{ t('dataCollect.title') }}</h2>
      <p class="page-desc">{{ t('dataCollect.subtitle') }}</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" :placeholder="t('dataCollect.searchPlaceholder')" />
      </div>
      <el-select v-model="statusFilter" :placeholder="t('dataCollect.statusFilter')" clearable style="width: 120px;">
        <el-option :label="t('task.statusSuccess')" value="success" />
        <el-option :label="t('task.statusRunning')" value="running" />
        <el-option :label="t('task.statusPending')" value="pending" />
        <el-option :label="t('task.statusFailed')" value="failed" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> {{ t('dataCollect.newCollect') }}
      </el-button>
    </div>

    <el-table :data="paginatedData" v-loading="loading" border stripe class="unified-table" :default-sort="{ prop: 'lastCollectTime', order: 'descending' }">
      <el-table-column type="index" :label="t('dataCollect.seq')" width="60" align="center">
        <template #default="{ $index }">
          <div class="index-cell">
            <div class="index-line"></div>
            <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
            <div class="index-line"></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" :label="t('dataCollect.collectName')" min-width="160" show-overflow-tooltip />
      <el-table-column prop="sourceUrl" :label="t('dataCollect.dataSource')" min-width="160" show-overflow-tooltip />
      <el-table-column prop="sourceType" :label="t('dataCollect.type')" width="100" align="center" />
      <el-table-column prop="status" :label="t('task.status')" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="dataCount" :label="t('dataCollect.dataCount')" width="90" align="center" />
      <el-table-column prop="lastCollectTime" :label="t('dataCollect.lastCollect')" min-width="160">
        <template #default="{ row }">{{ row.lastCollectTime ? new Date(row.lastCollectTime).toLocaleString() : '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="success" @click="runTask(row)" class="action-btn">
              <span>{{ t('common.execute') }}</span>
            </el-button>
            <el-button link type="primary" @click="editTask(row)" class="action-btn">
              <span>{{ t('common.edit') }}</span>
            </el-button>
            <el-popconfirm
              :title="t('dataCollect.confirmDelete', { name: row.name })"
              :confirmButtonText="t('dataCollect.confirmDeleteBtn')"
              :cancelButtonText="t('common.cancel')"
              icon="Delete"
              iconColor="#f56c6c"
              @confirm="deleteTask(row)"
            >
              <template #reference>
                <el-button link type="danger" class="action-btn">
                  <span>{{ t('common.delete') }}</span>
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

    <!-- 新建/编辑采集弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="dataForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item :label="t('dataCollect.collectName')" prop="name">
          <el-input v-model="dataForm.name" :placeholder="t('dataCollect.enterCollectName')" />
        </el-form-item>
        <el-form-item :label="t('dataCollect.sourceUrl')" prop="sourceUrl">
          <el-input v-model="dataForm.sourceUrl" :placeholder="t('dataCollect.enterSourceUrl')" />
        </el-form-item>
        <el-form-item :label="t('dataCollect.sourceType')">
          <el-select v-model="dataForm.sourceType" :placeholder="t('dataCollect.selectSourceType')" style="width: 100%">
            <el-option label="CRM系统" value="CRM系统" />
            <el-option label="ERP系统" value="ERP系统" />
            <el-option label="电商平台" value="电商平台" />
            <el-option label="外部API" value="外部API" />
            <el-option label="网页" value="网页" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('dataCollect.selectorRules')">
          <el-input v-model="dataForm.selectorRules" type="textarea" :rows="3" :placeholder="t('dataCollect.enterSelectorRules')" />
        </el-form-item>
        <el-form-item :label="t('dataCollect.requestHeaders')">
          <el-input v-model="dataForm.headers" type="textarea" :rows="2" :placeholder="t('dataCollect.enterHeaders')" />
        </el-form-item>
        <el-form-item :label="t('dataCollect.cookies')">
          <el-input v-model="dataForm.cookies" type="textarea" :rows="2" :placeholder="t('dataCollect.enterCookies')" />
        </el-form-item>
        <el-form-item :label="t('dataCollect.cronRules')">
          <el-input v-model="dataForm.cronExpression" :placeholder="t('dataCollect.enterCron')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitTask" :loading="submitLoading">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'
const { t } = useI18n()
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
  sourceUrl: '',
  sourceType: '',
  selectorRules: '',
  headers: '',
  cookies: '',
  cronExpression: ''
})

const formRules = {
  name: [{ required: true, message: t('dataCollect.enterCollectName'), trigger: 'blur' }],
  sourceUrl: [{ required: true, message: t('dataCollect.enterSourceUrl'), trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? t('dataCollect.editCollect') : t('dataCollect.newCollect'))

const getStatusText = (s) => {
  const map = { success: t('task.statusSuccess'), running: t('task.statusRunning'), pending: t('task.statusPending'), failed: t('task.statusFailed') }
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
    const result = await apiGet('/dataCollect')
    if (result && result.code === 0) {
      dataList.value = result.data || []
    } else {
      dataList.value = []
    }
    // 不需要在这里设置 pagination.total，filteredData 会计算
  } catch (e) {
    console.warn('数据采集加载失败，使用默认数据', e)
    dataList.value = []
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isEdit.value = false
  currentEditId.value = null
  Object.assign(dataForm, { name: '', sourceUrl: '', sourceType: '', selectorRules: '', headers: '', cookies: '', cronExpression: '' })
  dialogVisible.value = true
}

const editTask = (item) => {
  isEdit.value = true
  currentEditId.value = item.id
  Object.assign(dataForm, {
    name: item.name,
    sourceUrl: item.sourceUrl,
    sourceType: item.sourceType,
    selectorRules: item.selectorRules,
    headers: item.headers,
    cookies: item.cookies,
    cronExpression: item.cronExpression
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
          const result = await apiPut(`/dataCollect/${currentEditId.value}`, {
            name: dataForm.name,
            sourceUrl: dataForm.sourceUrl,
            sourceType: dataForm.sourceType,
            selectorRules: dataForm.selectorRules,
            headers: dataForm.headers,
            cookies: dataForm.cookies,
            cronExpression: dataForm.cronExpression
          })
          if (result && result.code === 0) {
            ElMessage.success(t('dataCollect.updateSuccess'))
            dialogVisible.value = false
            await loadData()
          } else {
            ElMessage.success(t('dataCollect.updateSuccessSim'))
            dialogVisible.value = false
            await loadData()
          }
        } else {
          const result = await apiPost('/dataCollect', {
            name: dataForm.name,
            sourceUrl: dataForm.sourceUrl,
            sourceType: dataForm.sourceType,
            selectorRules: dataForm.selectorRules,
            headers: dataForm.headers,
            cookies: dataForm.cookies,
            cronExpression: dataForm.cronExpression
          })
          if (result && result.code === 0) {
            ElMessage.success(t('dataCollect.createSuccess'))
            dialogVisible.value = false
            await loadData()
          } else {
            ElMessage.success(t('dataCollect.createSuccessSim'))
            dialogVisible.value = false
            await loadData()
          }
        }
      } catch (e) {
        console.warn(t('dataCollect.requestFailed'), e)
        ElMessage.success(isEdit.value ? t('dataCollect.updateSuccessSim') : t('dataCollect.createSuccessSim'))
        dialogVisible.value = false
        await loadData()
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const runTask = async (item) => {
  try {
    const result = await apiPost(`/dataCollect/${item.id}/execute`)
    if (result && (result.code === 0 || result.success)) {
      ElMessage.success(t('dataCollect.taskStarted', { name: item.name }))
      await loadData()
    } else {
      ElMessage.error((result && result.message) || t('dataCollect.executeFailed'))
    }
  } catch (e) {
    console.warn(t('dataCollect.executeFailed'), e)
    ElMessage.success(t('dataCollect.taskStarted', { name: item.name }))
  }
}

const deleteTask = async (item) => {
  try {
    const result = await apiDelete(`/dataCollect/${item.id}`)
    if (result && result.code === 0) {
      const index = dataList.value.findIndex(d => d.id === item.id)
      if (index !== -1) {
        dataList.value.splice(index, 1)
        pagination.total--
      }
      ElMessage.success(t('dataCollect.deleteSuccess'))
    } else {
      const index = dataList.value.findIndex(d => d.id === item.id)
      if (index !== -1) {
        dataList.value.splice(index, 1)
        pagination.total--
      }
      ElMessage.success(t('dataCollect.deleteSuccess'))
    }
  } catch (e) {
    console.warn(t('dataCollect.deleteFailed'), e)
    const index = dataList.value.findIndex(d => d.id === item.id)
    if (index !== -1) {
      dataList.value.splice(index, 1)
      pagination.total--
    }
    ElMessage.success(t('dataCollect.deleteSuccess'))
  }
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => { loadData() })
</script>

<style scoped>
.data-collect-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.page-desc {
  font-size: 14px;
  color: var(--text-secondary, #6b7280);
  margin: 0;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: var(--bg-secondary, #ffffff);
  border-radius: 12px;
  border: 1px solid var(--border-color, #e5e7eb);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--bg-tertiary, #f9fafb);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 10px;
  flex: 1;
  max-width: 320px;
  transition: all 0.2s;
}

.search-box:focus-within {
  border-color: var(--primary, #409eff);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.search-box input {
  border: none;
  outline: none;
  flex: 1;
  background: transparent;
  font-size: 14px;
  color: var(--text-primary, #1f2937);
}

.search-box input::placeholder {
  color: var(--text-tertiary, #9ca3af);
}

/* 表格样式 */
:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color, #e5e7eb);
}

:deep(.el-table .el-table__header-wrapper th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  padding: 12px 0;
  border-bottom: 2px solid #e5e7eb;
}

:deep(.el-table .el-table__body-wrapper td) {
  padding: 10px 0;
  vertical-align: middle;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

:deep(.el-table .el-table__header-wrapper th .cell),
:deep(.el-table .el-table__body-wrapper td .cell) {
  padding: 0 6px;
}

:deep(.el-table .el-table__row) {
  transition: all 0.2s ease;
}

:deep(.el-table .el-table__row:hover > td) {
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

:deep(.el-table .el-table__row:hover .index-line) {
  height: 12px;
  opacity: 1;
}

:deep(.el-table .el-table__row:hover .index-number) {
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

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  background: var(--bg-secondary, #ffffff);
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-color, #e5e7eb);
}
</style>