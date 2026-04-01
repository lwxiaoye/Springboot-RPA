<template>
  <div class="processes-page">
    <div class="page-header">
      <h2>流程管理</h2>
      <p class="page-desc">管理RPA自动化流程</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索流程名称/编码..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="已发布" value="active" />
        <el-option label="草稿" value="draft" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 新建流程
      </el-button>
    </div>

    <el-table :data="paginatedProcesses" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="流程名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="code" label="流程编码" min-width="140" />
      <el-table-column prop="version" label="版本" width="80" align="center" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '-' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
            {{ row.status === 'active' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="creatorName" label="创建人" width="100" align="center" />
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="240" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="primary" @click="editProcess(row)">编辑</el-button>
          <el-button link type="primary" @click="openDesigner(row)">设计</el-button>
          <el-popconfirm title="确认删除该流程吗？" @confirm="deleteProcess(row)">
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

    <!-- 新建/编辑流程弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="processForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="流程名称" prop="name">
          <el-input v-model="processForm.name" placeholder="请输入流程名称" />
        </el-form-item>
        <el-form-item label="流程编码" prop="code">
          <el-input v-model="processForm.code" placeholder="请输入流程编码，如：ORDER_PROCESS" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="processForm.version" placeholder="请输入版本号，如：1.0.0" />
        </el-form-item>
        <el-form-item label="流程描述">
          <el-input v-model="processForm.description" type="textarea" :rows="3" placeholder="请输入流程描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="processForm.status">
            <el-radio label="draft">草稿</el-radio>
            <el-radio label="active">已发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcess" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="流程详情" width="700px" class="process-detail-dialog">
      <div class="detail-content">
        <!-- 基本信息卡片 -->
        <div class="info-section">
          <div class="section-title-bar">
            <el-icon class="section-icon"><Document /></el-icon>
            <span class="section-title">基本信息</span>
          </div>
          
          <div class="info-grid">
            <div class="info-row">
              <label class="info-label">流程名称：</label>
              <span class="info-value value-primary">{{ currentProcess.name }}</span>
            </div>
            <div class="info-row">
              <label class="info-label">流程编码：</label>
              <span class="info-value value-code">{{ currentProcess.code }}</span>
            </div>
            <div class="info-row">
              <label class="info-label">版本号：</label>
              <el-tag size="small" effect="plain">{{ currentProcess.version }}</el-tag>
            </div>
            <div class="info-row">
              <label class="info-label">状态：</label>
              <el-tag :type="currentProcess.status === 'active' ? 'success' : 'info'" size="small" effect="light">
                {{ currentProcess.status === 'active' ? '已发布' : '草稿' }}
              </el-tag>
            </div>
            <div class="info-row">
              <label class="info-label">创建人：</label>
              <span class="info-value">{{ currentProcess.creatorName }}</span>
            </div>
            <div class="info-row">
              <label class="info-label">创建时间：</label>
              <span class="info-value">{{ currentProcess.createTime }}</span>
            </div>
          </div>
          
          <div class="info-row full-row">
            <label class="info-label">描述：</label>
            <span class="info-value value-desc">{{ currentProcess.description || '-' }}</span>
          </div>
        </div>
        
        <!-- 流程步骤卡片 -->
        <div class="steps-section">
          <div class="section-title-bar">
            <el-icon class="section-icon"><List /></el-icon>
            <span class="section-title">流程步骤（{{ detailSteps.length }}个）</span>
            <el-tag type="info" size="small" effect="plain" class="steps-count-tag">
              {{ detailSteps.filter(s => s.robotId).length }} 个已绑定机器人
            </el-tag>
          </div>
          
          <div v-if="detailSteps.length > 0" class="detail-steps-list">
            <div v-for="(step, index) in detailSteps" :key="step.id" class="detail-step-item">
              <div class="detail-step-number">
                <span>{{ index + 1 }}</span>
              </div>
              <div class="detail-step-content">
                <div class="detail-step-header">
                  <span class="detail-step-name">{{ step.name || '未命名步骤' }}</span>
                </div>
                <div class="detail-step-robot-info">
                  <div v-if="step.robotId" class="detail-step-robot">
                    <el-tag :type="getRobotStatus(step.robotId).type" size="small" effect="light" class="robot-tag">
                      <el-icon style="margin-right: 4px;"><Monitor /></el-icon>
                      {{ getRobotStatus(step.robotId).name }}
                    </el-tag>
                    <span class="robot-status-text"> · {{ getRobotStatus(step.robotId).statusText }}</span>
                  </div>
                  <div v-else class="detail-step-robot-empty">
                    <el-tag type="info" size="small" effect="plain" class="unbound-tag">
                      <el-icon style="margin-right: 4px;"><Warning /></el-icon>
                      未绑定机器人
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="empty-detail-steps">
            <el-empty description="暂无步骤" :image-size="80">
              <el-button type="primary" size="small" @click="openDesigner(currentProcess); detailVisible = false">去设计</el-button>
            </el-empty>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 流程设计器弹窗 -->
    <el-dialog v-model="designerVisible" :title="`流程设计 - ${currentProcess.name || ''}`" width="1000px" class="process-designer-dialog" :close-on-click-modal="false">
      <div class="designer-container">
        <div class="designer-header">
          <div class="header-left">
            <el-icon class="header-icon"><Setting /></el-icon>
            <div class="header-text">
              <div class="header-title">设计流程步骤</div>
              <div class="header-subtitle">为每个步骤分配合适的机器人执行</div>
            </div>
          </div>
          <div class="header-actions">
            <el-button @click="addStep" type="success" size="default">
              <el-icon><Plus /></el-icon> 添加步骤
            </el-button>
            <el-button @click="saveDesign" type="primary" :loading="savingDesign" size="default">
              <el-icon><Check /></el-icon> 保存设计
            </el-button>
          </div>
        </div>

        <el-divider style="margin: 0 0 20px 0;" />

        <div class="steps-container">
          <div class="steps-wrapper">
            <draggable v-model="steps" item-key="id" class="steps-list" @end="onDragEnd">
              <template #item="{ element, index }">
                <div class="step-card">
                  <div class="step-card-header">
                    <div class="step-drag-handle">
                      <el-icon><Rank /></el-icon>
                    </div>
                    <div class="step-number">
                      <span class="number-badge">{{ index + 1 }}</span>
                    </div>
                    <div class="step-content">
                      <div class="step-fields">
                        <div class="field-row">
                          <label class="field-label">步骤名称：</label>
                          <el-input 
                            v-model="element.name" 
                            placeholder="请输入步骤名称" 
                            size="default"
                            class="field-input"
                          >
                            <template #prefix>
                              <el-icon><Edit /></el-icon>
                            </template>
                          </el-input>
                        </div>
                        <div class="field-row">
                          <label class="field-label">执行机器人：</label>
                          <div class="robot-display">
                            <el-select 
                              v-model="element.robotId" 
                              placeholder="请选择执行机器人" 
                              size="default"
                              class="field-select"
                              filterable
                              clearable
                            >
                              <template #prefix>
                                <el-icon><Monitor /></el-icon>
                              </template>
                              <el-option
                                v-for="robot in robots"
                                :key="robot.id"
                                :value="robot.id"
                              >
                                <div class="robot-option">
                                  <span class="robot-name">{{ robot.name }}</span>
                                  <el-tag 
                                    size="small" 
                                    :type="robot.status === 'idle' ? 'success' : robot.status === 'busy' ? 'warning' : 'info'"
                                    effect="plain"
                                  >
                                    {{ robot.status === 'idle' ? '空闲' : robot.status === 'busy' ? '忙碌' : '离线' }}
                                  </el-tag>
                                </div>
                              </el-option>
                            </el-select>
                            <div v-if="element.robotId" class="robot-selected-tag">
                              <el-tag :type="getRobotStatus(element.robotId).type" effect="light">
                                <el-icon style="margin-right: 4px;"><Monitor /></el-icon>
                                {{ getRobotStatus(element.robotId).name }}
                                <span class="robot-status-text"> · {{ getRobotStatus(element.robotId).statusText }}</span>
                              </el-tag>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="step-actions">
                      <el-popconfirm title="确定删除该步骤？" @confirm="removeStep(index)">
                        <template #reference>
                          <el-button link type="danger" size="small" class="delete-btn">
                            <el-icon><Delete /></el-icon>
                          </el-button>
                        </template>
                      </el-popconfirm>
                    </div>
                  </div>
                </div>
              </template>
            </draggable>
          </div>

          <div v-if="steps.length === 0" class="empty-steps">
            <el-empty description="暂无步骤，请添加步骤">
              <el-button type="primary" @click="addStep">添加第一个步骤</el-button>
            </el-empty>
          </div>
        </div>

        <div class="designer-footer">
          <div class="step-count">
            <el-tag type="info" effect="plain">共 {{ steps.length }} 个步骤</el-tag>
          </div>
          <div class="footer-buttons">
            <el-button @click="designerVisible = false">关闭</el-button>
            <el-button type="primary" @click="saveDesign" :loading="savingDesign">保存设计</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Check, Delete, Setting, Edit, Rank, Monitor, List, Document, Warning } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const robots = ref([])

const loading = ref(false)
const submitLoading = ref(false)
const processes = ref([])
const searchKeyword = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const designerVisible = ref(false)
const isEdit = ref(false)
const currentProcess = ref({})
const currentEditId = ref(null)
const currentDesignId = ref(null)
const formRef = ref(null)
const savingDesign = ref(false)
const steps = ref([])
const detailSteps = ref([])

const pagination = reactive({ page: 1, size: 10, total: 0 })

const processForm = reactive({
  name: '',
  code: '',
  version: '1.0.0',
  description: '',
  status: 'draft'
})

const formRules = {
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入流程编码', trigger: 'blur' }],
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑流程' : '新建流程')

const filteredProcesses = computed(() => {
  let list = processes.value
  if (searchKeyword.value) {
    list = list.filter(p => p.name.includes(searchKeyword.value) || p.code?.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(p => p.status === statusFilter.value)
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedProcesses = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredProcesses.value.slice(start, end)
})

const loadProcesses = async () => {
  loading.value = true
  try {
    const result = await apiGet('/process')
    if (result.code === 0) {
      processes.value = result.data || []
      // 不需要在这里设置 pagination.total，filteredProcesses 会计算
    }
  } catch {
    processes.value = []
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isEdit.value = false
  currentEditId.value = null
  Object.assign(processForm, { name: '', code: '', version: '1.0.0', description: '', status: 'draft' })
  dialogVisible.value = true
}

const editProcess = (process) => {
  isEdit.value = true
  currentEditId.value = process.id
  Object.assign(processForm, {
    name: process.name,
    code: process.code,
    version: process.version,
    description: process.description,
    status: process.status
  })
  dialogVisible.value = true
}

const viewDetail = async (process) => {
  currentProcess.value = process
  
  // 加载步骤数据
  try {
    const result = await apiGet(`/process/${process.id}/design`)
    if (result.code === 0 && result.data) {
      try {
        detailSteps.value = JSON.parse(result.data)
      } catch {
        detailSteps.value = []
      }
    } else {
      detailSteps.value = []
    }
  } catch {
    detailSteps.value = []
  }
  
  detailVisible.value = true
}

const submitProcess = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          const result = await apiPut(`/process/${currentEditId.value}`, {
            name: processForm.name,
            code: processForm.code,
            description: processForm.description,
            version: processForm.version,
            status: processForm.status
          })
          if (result.code === 0) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            await loadProcesses()
          } else {
            ElMessage.error(result.message || '更新失败')
          }
        } else {
          const result = await apiPost('/process', {
            name: processForm.name,
            code: processForm.code,
            description: processForm.description,
            version: processForm.version,
            status: processForm.status
          })
          if (result.code === 0) {
            ElMessage.success('创建成功')
            dialogVisible.value = false
            await loadProcesses()
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

const deleteProcess = async (process) => {
  try {
    const result = await apiDelete(`/process/${process.id}`)
    if (result.code === 0) {
      const index = processes.value.findIndex(p => p.id === process.id)
      if (index !== -1) {
        processes.value.splice(index, 1)
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

// 打开流程设计器
const openDesigner = async (process) => {
  currentDesignId.value = process.id
  currentProcess.value = process
  
  // 加载机器人列表
  await loadRobots()
  
  try {
    const result = await apiGet(`/process/${process.id}/design`)
    if (result.code === 0) {
      if (result.data) {
        try {
          steps.value = JSON.parse(result.data)
        } catch {
          steps.value = []
        }
      } else {
        steps.value = []
      }
    } else {
      steps.value = []
    }
  } catch {
    steps.value = []
  }
  
  designerVisible.value = true
}

// 加载机器人列表
const loadRobots = async () => {
  try {
    const result = await apiGet('/robot')
    if (result.code === 0) {
      robots.value = result.data || []
    }
  } catch {
    robots.value = []
  }
}

// 获取机器人状态信息
const getRobotStatus = (robotId) => {
  const robot = robots.value.find(r => r.id === robotId)
  if (!robot) {
    return { name: '', statusText: '', type: 'info' }
  }
  const statusMap = {
    idle: { text: '空闲', type: 'success' },
    busy: { text: '忙碌', type: 'warning' },
    offline: { text: '离线', type: 'info' }
  }
  const status = statusMap[robot.status] || { text: robot.status, type: 'info' }
  return {
    name: robot.name,
    statusText: status.text,
    type: status.type
  }
}

// 添加步骤
const addStep = () => {
  steps.value.push({
    id: Date.now(),
    name: '新步骤',
    robotId: null,
    robotName: ''
  })
}

// 删除步骤
const removeStep = (index) => {
  steps.value.splice(index, 1)
}

// 拖拽结束
const onDragEnd = () => {
  // 拖拽后不需要额外操作，vuedraggable 已经更新了顺序
}

// 保存设计
const saveDesign = async () => {
  savingDesign.value = true
  try {
    const stepsData = JSON.stringify(steps.value)
    const result = await apiPost(`/process/${currentDesignId.value}/design`, {
      steps: stepsData
    })
    if (result.code === 0) {
      ElMessage.success('保存成功')
      // 不关闭设计器，只刷新流程列表
      await loadProcesses()
    } else {
      ElMessage.error(result.message || '保存失败')
    }
  } catch {
    ElMessage.error('请求失败')
  } finally {
    savingDesign.value = false
  }
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => { loadProcesses() })
</script>

<style scoped>
.processes-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }
.detail-content { padding: 8px 0; }
.detail-item { display: flex; margin-bottom: 12px; }
.detail-item label { width: 80px; color: #8c8c8c; }
.detail-item span { color: #262626; font-weight: 500; }
.detail-item.full { flex-direction: column; }
.detail-item.full label { margin-bottom: 4px; }

/* 流程详情弹窗样式 */
.process-detail-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 基本信息区域 */
.info-section {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e9f2 100%);
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #e5e7eb;
}

.section-title-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #667eea;
}

.section-icon {
  font-size: 20px;
  color: #667eea;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.steps-count-tag {
  margin-left: auto;
  font-size: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 24px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.info-row.full-row {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #dcdfe6;
}

.info-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  min-width: 90px;
  text-align: right;
  line-height: 1.5;
}

.info-value {
  font-size: 14px;
  color: #262626;
  line-height: 1.5;
  flex: 1;
}

.value-primary {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 15px;
}

.value-code {
  font-family: 'Courier New', monospace;
  color: #667eea;
  font-weight: 500;
}

.value-desc {
  color: #606266;
  line-height: 1.8;
}

/* 步骤区域 */
.steps-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.detail-steps-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-step-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.detail-step-item:hover {
  background: linear-gradient(135deg, #f0f2f5 0%, #e8e8e8 100%);
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.detail-step-number {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 15px;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
}

.detail-step-content {
  flex: 1;
  min-width: 0;
}

.detail-step-header {
  margin-bottom: 10px;
}

.detail-step-name {
  font-size: 15px;
  color: #1a1a1a;
  font-weight: 600;
}

.detail-step-robot-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-step-robot {
  display: flex;
  align-items: center;
  gap: 8px;
}

.robot-tag {
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 500;
}

.robot-status-text {
  color: #909399;
  font-size: 13px;
}

.detail-step-robot-empty {
  display: inline-block;
}

.unbound-tag {
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 6px;
  color: #909399;
}

.empty-detail-steps {
  padding: 60px 20px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 10px;
  border: 2px dashed #e5e7eb;
}

/* 流程设计器样式 */
.process-designer-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 75vh;
  overflow: hidden;
}

.process-designer-dialog :deep(.el-dialog__header) {
  padding: 0;
  border-bottom: 1px solid #e5e7eb;
}

.designer-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.designer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 32px;
  color: rgba(255, 255, 255, 0.9);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: white;
}

.header-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions .el-button {
  padding: 10px 20px;
  font-size: 14px;
}

.steps-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f5f7fa;
}

.steps-wrapper {
  min-height: 400px;
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 2px solid transparent;
}

.step-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-color: #667eea;
}

.step-card-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.step-drag-handle {
  cursor: move;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #f0f2f5;
  color: #909399;
}

.step-card:hover .step-drag-handle {
  background: #e5e7eb;
  color: #667eea;
}

.step-number {
  flex-shrink: 0;
}

.number-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  font-size: 16px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.field-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  min-width: 100px;
  text-align: right;
}

.field-input,
.field-select {
  flex: 1;
}

.robot-display {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.robot-selected-tag {
  flex-shrink: 0;
}

.robot-selected-tag :deep(.el-tag) {
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 6px;
}

.robot-status-text {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}

.field-input :deep(.el-input__wrapper),
.field-select :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.field-input:hover :deep(.el-input__wrapper),
.field-select:hover :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

.field-input:focus-within :deep(.el-input__wrapper),
.field-select:focus-within :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 2px #667eea inset;
}

.robot-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.robot-name {
  flex: 1;
  font-weight: 500;
}

.step-actions {
  flex-shrink: 0;
  padding-left: 8px;
}

.delete-btn {
  opacity: 0;
}

.step-card:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #fef0f0;
  border-radius: 6px;
}

.empty-steps {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  border: 2px dashed #e5e7eb;
}

.designer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-top: 1px solid #e5e7eb;
}

.step-count {
  font-size: 14px;
  color: #606266;
}

.footer-buttons {
  display: flex;
  gap: 12px;
}

.footer-buttons .el-button {
  min-width: 100px;
}
</style>