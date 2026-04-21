<template>
  <div class="processes-page" v-show="!designerVisible">
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
      <el-table-column label="操作" width="260" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="success" @click="handleExecute(row)">执行</el-button>
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

    <!-- 新建/编辑流程弹窗 - 分步向导 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" class="process-wizard-dialog" :close-on-click-modal="false">
      <!-- 步骤指示器 -->
      <div class="wizard-steps">
        <div class="wizard-step" :class="{ active: wizardStep === 1, completed: wizardStep > 1 }">
          <div class="step-circle">{{ wizardStep > 1 ? '✓' : '1' }}</div>
          <span class="step-text">基本信息</span>
        </div>
        <div class="step-line" :class="{ active: wizardStep > 1 }"></div>
        <div class="wizard-step" :class="{ active: wizardStep === 2 }">
          <div class="step-circle">2</div>
          <span class="step-text">设计步骤</span>
        </div>
      </div>

      <!-- 步骤1：基本信息 -->
      <div v-show="wizardStep === 1" class="wizard-content">
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
          <el-form-item label="执行凭据">
            <el-select v-model="processForm.credentialId" placeholder="选择执行时使用的凭据（可选）" clearable filterable style="width: 100%">
              <el-option v-for="cred in credentials" :key="cred.id" :label="cred.name" :value="cred.id">
                <div class="credential-option">
                  <span>{{ cred.name }}</span>
                  <el-tag size="small" type="info">{{ getCredentialTypeText(cred.type) }}</el-tag>
                </div>
              </el-option>
            </el-select>
            <div class="form-tip">选择执行此流程时需要使用的凭据（如数据库密码、API密钥等）</div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤2：设计流程步骤 -->
      <div v-show="wizardStep === 2" class="wizard-content designer-content">
        <div class="designer-header-inline">
          <div class="header-info">
            <span class="process-name">{{ processForm.name }}</span>
            <span class="process-tip">为流程添加执行步骤并绑定机器人</span>
          </div>
          <el-button @click="addWizardStep" type="success" size="small">
            <el-icon><Plus /></el-icon> 添加步骤
          </el-button>
        </div>

        <div class="steps-list-container">
          <draggable v-model="wizardSteps" item-key="id" class="wizard-steps-list">
            <template #item="{ element, index }">
              <div class="wizard-step-card">
                <div class="step-drag">
                  <el-icon><Rank /></el-icon>
                </div>
                <div class="step-badge">{{ index + 1 }}</div>
                <div class="step-form">
                  <el-input v-model="element.name" placeholder="步骤名称" size="default" class="step-name-input">
                    <template #prefix><el-icon><Edit /></el-icon></template>
                  </el-input>
                  <el-select v-model="element.type" placeholder="选择步骤类型" size="default" clearable class="step-type-select" @change="onStepTypeChange(element)">
                    <template #prefix><el-icon><Operation /></el-icon></template>
                    <el-option value="collect" label="数据采集" />
                    <el-option value="parse" label="数据解析" />
                    <el-option value="process" label="数据加工" />
                    <el-option value="query" label="数据查询" />
                    <el-option value="transform" label="数据转换" />
                    <el-option value="output" label="数据输出" />
                    <el-option value="validate" label="数据校验" />
                  </el-select>
                  <div class="robot-select-row">
                    <el-select v-model="element.category" placeholder="选择机器人分类" size="default" clearable class="step-category-select" @change="onCategoryChange(element)">
                      <template #prefix><el-icon><Folder /></el-icon></template>
                      <el-option v-for="cat in robotCategories" :key="cat.code" :value="cat.code" :label="cat.name" />
                    </el-select>
                    <el-select v-model="element.robotId" placeholder="选择执行机器人" size="default" filterable clearable class="step-robot-select" :disabled="!element.category">
                      <template #prefix><el-icon><Monitor /></el-icon></template>
                      <el-option v-for="robot in getFilteredRobots(element.category)" :key="robot.id" :value="robot.id" :label="robot.name">
                        <div class="robot-option">
                          <span>{{ robot.name }}</span>
                          <el-tag size="small" :type="robot.status === 'idle' ? 'success' : robot.status === 'busy' ? 'warning' : 'info'">
                            {{ robot.status === 'idle' ? '空闲' : robot.status === 'busy' ? '忙碌' : '离线' }}
                          </el-tag>
                        </div>
                      </el-option>
                    </el-select>
                  </div>
                </div>
                <el-button link type="danger" @click="removeWizardStep(index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </template>
          </draggable>

          <div v-if="wizardSteps.length === 0" class="empty-wizard-steps">
            <el-empty description="暂无步骤" :image-size="60">
              <el-button type="primary" @click="addWizardStep">添加第一个步骤</el-button>
            </el-empty>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="wizard-footer">
          <div class="footer-left">
            <el-button v-if="wizardStep === 2" @click="skipDesign">跳过设计</el-button>
            <el-button v-if="isEdit" type="danger" @click="handleDeleteProcess">删除流程</el-button>
          </div>
          <div class="footer-right">
            <el-button v-if="wizardStep === 2" @click="wizardStep = 1">上一步</el-button>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button v-if="wizardStep === 1" type="primary" @click="nextStep" :disabled="!canNextStep">下一步</el-button>
            <el-button v-if="wizardStep === 2" type="primary" @click="submitProcessWithDesign" :loading="submitLoading">
              {{ wizardSteps.length > 0 ? '完成并保存' : '完成' }}
            </el-button>
          </div>
        </div>
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
                  <el-tag v-if="step.type" size="small" effect="light" :type="getDetailStepTypeTag(step.type)">
                    {{ getDetailStepTypeLabel(step.type) }}
                  </el-tag>
                </div>
                <div v-if="step.description" class="detail-step-desc">{{ step.description }}</div>
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
  </div>

  <!-- 流程设计器 - 画布模式 -->
  <div v-if="designerVisible" class="designer-page-container">
    <div class="designer-nav">
      <div class="nav-left">
        <el-button @click="closeDesigner" text class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <el-divider direction="vertical" />
        <span class="nav-title">流程设计 - {{ currentProcess.name }}</span>
      </div>
      <div class="nav-actions">
        <el-button @click="saveDesign" type="primary" :loading="savingDesign">
          <el-icon><Check /></el-icon> 保存设计
        </el-button>
      </div>
    </div>

    <div class="designer-main-content">
      <CanvasDesigner 
        ref="canvasDesignerRef"
        v-model="canvasData"
        :robots="robots"
        :robot-categories="robotCategories"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Search, Plus, Check, Delete, Edit, Rank, Monitor, List, Document, Warning, Folder, Operation, ArrowLeft } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import CanvasDesigner from './CanvasDesigner.vue'

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const router = useRouter()

const robots = ref([])
const robotCategories = ref([])
const credentials = ref([])

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
const canvasData = ref({ nodes: [], edges: [] })
const canvasDesignerRef = ref(null)
const detailSteps = ref([])
const wizardStep = ref(1)
const wizardSteps = ref([])
const tempProcessId = ref(null)

const canNextStep = computed(() => {
  return processForm.name && processForm.code && processForm.version
})

const pagination = reactive({ page: 1, size: 10, total: 0 })

const processForm = reactive({
  name: '',
  code: '',
  version: '1.0.0',
  description: '',
  status: 'draft',
  credentialId: null
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
  wizardStep.value = 1
  wizardSteps.value = []
  tempProcessId.value = null
  Object.assign(processForm, { name: '', code: '', version: '1.0.0', description: '', status: 'draft' })
  // 预加载机器人和分类
  loadRobots()
  loadRobotCategories()
  dialogVisible.value = true
}

// 下一步
const nextStep = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value && tempProcessId.value) {
          // 编辑模式：更新流程基本信息
          await apiPut(`/process/${tempProcessId.value}`, {
            name: processForm.name,
            code: processForm.code,
            version: processForm.version,
            description: processForm.description,
            status: processForm.status
          })
        } else {
          // 新建模式：创建流程
          const result = await apiPost('/process', {
            name: processForm.name,
            code: processForm.code,
            version: processForm.version,
            description: processForm.description,
            status: processForm.status
          })
          if (result.code === 0) {
            tempProcessId.value = result.data.id
            isEdit.value = true
            currentEditId.value = result.data.id
          } else {
            ElMessage.error(result.message || '创建失败')
            submitLoading.value = false
            return
          }
        }
        // 加载机器人列表和分类并跳转到步骤2
        await loadRobots()
        await loadRobotCategories()
        wizardStep.value = 2
      } catch {
        ElMessage.error('请求失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 跳过设计
const skipDesign = () => {
  dialogVisible.value = false
  wizardStep.value = 1
  wizardSteps.value = []
  ElMessage.success('流程创建成功')
  loadProcesses()
}

// 添加向导步骤
const addWizardStep = () => {
  wizardSteps.value.push({ 
    id: Date.now(), 
    name: '新步骤', 
    type: '',
    description: '',
    category: '',
    robotId: null,
    config: {}
  })
}

// 删除向导步骤
const removeWizardStep = (index) => {
  wizardSteps.value.splice(index, 1)
}

// 提交带设计的流程
const submitProcessWithDesign = async () => {
  submitLoading.value = true
  try {
    // 保存设计
    if (wizardSteps.value.length > 0 && tempProcessId.value) {
      const stepsData = JSON.stringify(wizardSteps.value)
      await apiPost(`/process/${tempProcessId.value}/design`, { steps: stepsData })
    }
    dialogVisible.value = false
    wizardStep.value = 1
    wizardSteps.value = []
    ElMessage.success(isEdit.value ? '流程更新成功' : '流程创建成功')
    await loadProcesses()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

const editProcess = async (process) => {
  isEdit.value = true
  currentEditId.value = process.id
  Object.assign(processForm, {
    name: process.name,
    code: process.code,
    version: process.version,
    description: process.description,
    status: process.status
  })
  
  // 加载该流程的已有设计步骤
  wizardStep.value = 1
  wizardSteps.value = []
  tempProcessId.value = process.id
  
  try {
    const result = await apiGet(`/process/${process.id}/design`)
    if (result.code === 0 && result.data) {
      try {
        wizardSteps.value = JSON.parse(result.data)
      } catch {
        wizardSteps.value = []
      }
    }
  } catch {
    wizardSteps.value = []
  }
  
  dialogVisible.value = true
}

const viewDetail = (process) => {
  router.push(`/rpa/process/${process.id}`)
}

const handleExecute = async (process) => {
  try {
    const result = await apiPost(`/process/${process.id}/execute`)
    if (result.code === 0) {
      ElMessage.success('流程执行已启动')
    } else {
      ElMessage.error(result.message || '执行失败')
    }
  } catch {
    ElMessage.error('执行失败')
  }
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

// 在编辑弹窗中删除流程
const handleDeleteProcess = async () => {
  if (!currentEditId.value) return
  
  try {
    await ElMessageBox.confirm('确认删除该流程吗？删除后无法恢复！', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const result = await apiDelete(`/process/${currentEditId.value}`)
    if (result.code === 0) {
      ElMessage.success('删除成功')
      dialogVisible.value = false
      await loadProcesses()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('请求失败')
    }
  }
}

// 打开流程设计器
const openDesigner = async (process) => {
  currentDesignId.value = process.id
  currentProcess.value = process
  
  // 加载机器人列表和分类
  await loadRobots()
  await loadRobotCategories()
  
  try {
    const result = await apiGet(`/process/${process.id}/design`)
    if (result.code === 0) {
      if (result.data) {
        try {
          const parsed = JSON.parse(result.data)
          // 判断是新的画布格式还是旧的步骤格式
          if (parsed.nodes && parsed.edges) {
            // 新格式
            canvasData.value = parsed
          } else {
            // 旧格式，转换为新格式
            canvasData.value = convertOldStepsToCanvas(parsed)
          }
        } catch {
          canvasData.value = { nodes: [], edges: [] }
        }
      } else {
        canvasData.value = { nodes: [], edges: [] }
      }
    } else {
      canvasData.value = { nodes: [], edges: [] }
    }
  } catch {
    canvasData.value = { nodes: [], edges: [] }
  }
  
  designerVisible.value = true
}

// 将旧格式的步骤转换为画布格式
const convertOldStepsToCanvas = (oldSteps) => {
  const nodes = oldSteps.map((step, index) => ({
    id: step.id || `node_${index}`,
    type: 'process',
    name: step.name || '未命名步骤',
    stepType: step.type || '',
    category: step.category || '',
    robotId: step.robotId || null,
    robotName: step.robotName || '',
    x: 200,
    y: 100 + index * 150
  }))
  
  // 创建连接线
  const edges = []
  for (let i = 0; i < nodes.length - 1; i++) {
    edges.push({
      id: `edge_${i}`,
      source: nodes[i].id,
      sourcePort: 'bottom',
      target: nodes[i + 1].id,
      targetPort: 'top'
    })
  }
  
  return { nodes, edges }
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

// 加载机器人分类列表
const loadRobotCategories = async () => {
  try {
    const result = await apiGet('/robot/category/list')
    if (result.code === 0) {
      robotCategories.value = result.data || []
    }
  } catch {
    robotCategories.value = []
  }
}

// 加载凭据列表
const loadCredentials = async () => {
  try {
    const result = await apiGet('/credential')
    if (result.code === 0) {
      credentials.value = result.data || []
    }
  } catch {
    credentials.value = []
  }
}

// 获取凭据类型文本
const getCredentialTypeText = (type) => {
  const typeMap = {
    'DATABASE': '数据库',
    'API_KEY': 'API密钥',
    'SYSTEM_ACCOUNT': '系统账号',
    'SSH_KEY': 'SSH密钥',
    'CERTIFICATE': '证书',
    'RPA_CREDENTIAL': 'RPA凭据',
    'OTHER': '其他'
  }
  return typeMap[type] || type || '其他'
}

// 根据分类过滤机器人
const getFilteredRobots = (category) => {
  if (!category) return []
  return robots.value.filter(r => r.robotCategory === category)
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

// 步骤类型标签映射
const stepTypeMap = {
  collect: { label: '数据采集', tag: 'primary' },
  parse: { label: '数据解析', tag: 'success' },
  process: { label: '数据加工', tag: 'warning' },
  query: { label: '数据查询', tag: 'info' },
  transform: { label: '数据转换', tag: 'warning' },
  output: { label: '数据输出', tag: 'danger' },
  validate: { label: '数据校验', tag: 'success' },
  default: { label: '通用步骤', tag: 'info' }
}

// 获取步骤类型标签
const getDetailStepTypeLabel = (type) => {
  return stepTypeMap[type]?.label || stepTypeMap.default.label
}

// 获取步骤类型标签颜色
const getDetailStepTypeTag = (type) => {
  return stepTypeMap[type]?.tag || 'info'
}

// 保存设计
const saveDesign = async () => {
  savingDesign.value = true
  try {
    // 从画布组件获取数据
    const data = canvasDesignerRef.value?.saveData() || canvasData.value
    const stepsData = JSON.stringify(data)
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

// 关闭设计器
const closeDesigner = () => {
  designerVisible.value = false
  currentDesignId.value = null
  currentProcess.value = {}
  canvasData.value = { nodes: [], edges: [] }
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => { loadProcesses(); loadCredentials() })
</script>

<style scoped>
.processes-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
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

/* 工具栏 */
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

/* 表格 - 使用全局统一样式 */
:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color, #e4e7ed);
  width: 100%;
}

:deep(.el-table th.el-table__cell) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: #374151;
  font-size: 13px;
  padding: 12px 16px !important;
  border-bottom: 2px solid #e4e7ed !important;
}

:deep(.el-table td.el-table__cell) {
  padding: 12px 16px !important;
  border-bottom: 1px solid #f1f5f9 !important;
  vertical-align: middle;
}

/* 表格行 Hover 动效 - 不使用position:relative避免影响布局 */
:deep(.el-table__body tr) {
  transition: all 0.2s ease;
}

/* 左侧边框指示器 - 使用td的::before */
:deep(.el-table__body tr:hover > td:first-child) {
  box-shadow: inset 4px 0 0 #409eff;
}

:deep(.el-table__body tr:hover > td) {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.03) 0%, rgba(64, 158, 255, 0.08) 100%) !important;
}

/* 操作按钮容器 */
:deep(.el-table .cell) {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: nowrap;
}

:deep(.el-table .cell .el-button) {
  padding: 4px 6px;
  font-size: 12px;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  background: var(--bg-secondary, #ffffff);
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid var(--border-color, #e5e7eb);
}
.detail-content { padding: 8px 0; }
.detail-item { display: flex; margin-bottom: 12px; }
.detail-item label { width: 80px; color: #8c8c8c; }
.detail-item span { color: #262626; font-weight: 500; }
.detail-item.full { flex-direction: column; }
.detail-item.full label { margin-bottom: 4px; }

/* 流程详情弹窗样式 */
.process-detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 24px;
  background: #f5f7fa;
}

/* 基本信息区域 */
.info-section {
  background: white;
  border-radius: 10px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(44, 62, 80, 0.05);
  border: 1px solid #eaeff3;
  transition: all 0.3s ease;
}

.section-title-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #ecf0f3;
}

.section-icon {
  font-size: 20px;
  color: #5a6c7d;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
}

.steps-count-tag {
  margin-left: auto;
  font-size: 11px;
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

/* 流程步骤区域 */
.steps-section {
  background: white;
  border-radius: 10px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(44, 62, 80, 0.05);
  border: 1px solid #eaeff3;
  transition: all 0.3s ease;
}

.detail-steps-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-step-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #ecf0f3;
  transition: all 0.25s ease;
  cursor: default;
}

.detail-step-item:hover {
  background: #f0f7fc;
  border-color: #b8d4e8;
  transform: translateX(4px);
  box-shadow: 0 2px 6px rgba(52, 152, 219, 0.08);
}

.detail-step-number {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #bdc3c7 0%, #95a5a6 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 12px;
}

.detail-step-content {
  flex: 1;
  min-width: 0;
}

.detail-step-header {
  margin-bottom: 8px;
}

.detail-step-name {
  font-size: 13px;
  color: #2c3e50;
  font-weight: 600;
}

.detail-step-desc {
  font-size: 12px;
  color: #7f8c8d;
  margin: 4px 0 8px;
}

.detail-step-robot-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-step-robot {
  display: flex;
  align-items: center;
  gap: 6px;
}

.robot-tag {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.robot-status-text {
  color: #bdc3c7;
  font-size: 11px;
}

.detail-step-robot-empty {
  display: inline-block;
}

.unbound-tag {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
  color: #bdc3c7;
}

.empty-detail-steps {
  padding: 40px 20px;
  background: #fafbfc;
  border-radius: 8px;
  border: 2px dashed #e8ecef;
  transition: all 0.3s ease;
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
  padding: 16px 24px;
  background: linear-gradient(135deg, #ecf0f3 0%, #dfe6ed 100%);
  border-bottom: 1px solid #d0d8e0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 24px;
  color: #5a6c7d;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.header-subtitle {
  font-size: 12px;
  color: #7f8c8d;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.header-actions .el-button {
  padding: 8px 14px;
  font-size: 13px;
  border-radius: 6px;
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

.credential-option {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
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

/* ================================================
   沉稳大气风格 - 深蓝灰色系 + 流畅动效
   主色调：#1a1f36 (深蓝灰) 
   强调色：#409eff (科技蓝), #67c23a (成功绿), #e6a23c (警示橙)
   背景：#f0f2f5 (浅灰)
   ================================================ */

/* 流程详情弹窗 */
.process-detail-dialog :deep(.el-dialog__header) {
  padding: 0;
  border-bottom: none;
}

.process-detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px 28px;
  background: #f0f2f5;
}

/* 基本信息区域 */
.info-section {
  background: white;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(26, 31, 54, 0.06);
  border: 1px solid rgba(26, 31, 54, 0.08);
  transition: all 0.3s ease;
}

.info-section:hover {
  box-shadow: 0 4px 20px rgba(26, 31, 54, 0.1);
  transform: translateY(-1px);
}

.section-title-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #e4e7ed;
}

.section-icon {
  font-size: 22px;
  color: #1a1f36;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1f36;
  letter-spacing: 0.5px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 32px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.info-row.full-row {
  margin-top: 8px;
  flex-direction: column;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;
}

.info-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  min-width: 75px;
  text-align: right;
  line-height: 28px;
}

.info-value {
  font-size: 14px;
  color: #303133;
  line-height: 28px;
  flex: 1;
}

.info-value.value-primary {
  font-size: 15px;
  font-weight: 600;
  color: #1a1f36;
}

.info-value.value-code {
  font-family: 'JetBrains Mono', 'Fira Code', 'Monaco', monospace;
  font-size: 13px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.08);
  padding: 4px 10px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}

.info-value.value-desc {
  color: #606266;
  line-height: 1.8;
}

/* 流程步骤区域 */
.steps-section {
  background: white;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(26, 31, 54, 0.06);
  border: 1px solid rgba(26, 31, 54, 0.08);
  transition: all 0.3s ease;
}

.steps-section:hover {
  box-shadow: 0 4px 20px rgba(26, 31, 54, 0.1);
}

.steps-count-tag {
  margin-left: auto;
  font-size: 12px;
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
  background: linear-gradient(135deg, #fafbfc 0%, #f5f7fa 100%);
  border-radius: 10px;
  border: 1px solid #e4e7ed;
  transition: all 0.25s ease;
  cursor: default;
}

.detail-step-item:hover {
  background: linear-gradient(135deg, #f0f5ff 0%, #e8f4ff 100%);
  border-color: #409eff;
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.detail-step-number {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a1f36 0%, #2d3748 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  box-shadow: 0 2px 6px rgba(26, 31, 54, 0.25);
}

.detail-step-content {
  flex: 1;
  min-width: 0;
}

.detail-step-header {
  margin-bottom: 10px;
}

.detail-step-name {
  font-size: 14px;
  color: #1a1f36;
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
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 500;
}

.robot-status-text {
  color: #909399;
  font-size: 12px;
}

.detail-step-robot-empty {
  display: inline-block;
}

.unbound-tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  color: #909399;
}

.empty-detail-steps {
  padding: 48px 20px;
  background: #fafbfc;
  border-radius: 10px;
  border: 2px dashed #dcdfe6;
  transition: all 0.3s ease;
}

.empty-detail-steps:hover {
  border-color: #409eff;
  background: #f0f5ff;
}

/* ================================================
   流程设计器弹窗
   ================================================ */
.process-designer-dialog :deep(.el-dialog__header) {
  padding: 0;
  border-bottom: none;
}

.process-designer-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 70vh;
  overflow: hidden;
}

.designer-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
}

.designer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: linear-gradient(135deg, #1a1f36 0%, #2d3748 100%);
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 28px;
  color: rgba(255, 255, 255, 0.9);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: white;
  letter-spacing: 0.5px;
}

.header-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions :deep(.el-button) {
  padding: 10px 18px;
  font-size: 14px;
  border-radius: 6px;
  transition: all 0.25s ease;
}

.header-actions :deep(.el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.steps-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  background: #f0f2f5;
}

.steps-wrapper {
  min-height: 350px;
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.step-card {
  background: white;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(26, 31, 54, 0.06);
  border: 1px solid transparent;
  transition: all 0.25s ease;
}

.step-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.step-card.ghost {
  opacity: 0.5;
  background: #e8f4ff;
  border: 2px dashed #409eff;
}

.step-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.step-drag-handle {
  cursor: move;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #909399;
  transition: all 0.2s ease;
}

.step-card:hover .step-drag-handle {
  background: #e4e7ed;
  color: #409eff;
}

.step-number {
  flex-shrink: 0;
}

.number-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a1f36 0%, #2d3748 100%);
  color: white;
  font-weight: 600;
  font-size: 14px;
  box-shadow: 0 2px 6px rgba(26, 31, 54, 0.25);
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
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  min-width: 95px;
  text-align: right;
}

.field-input, .field-select {
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
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
}

.robot-status-text {
  color: #909399;
  font-size: 12px;
}

.field-input :deep(.el-input__wrapper),
.field-select :deep(.el-select__wrapper) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: all 0.2s ease;
}

.field-input:hover :deep(.el-input__wrapper),
.field-select:hover :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.field-input:focus-within :deep(.el-input__wrapper),
.field-select:focus-within :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) inset;
}

.credential-option {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.robot-name {
  flex: 1;
  font-weight: 500;
}

.step-actions {
  flex-shrink: 0;
}

.delete-btn {
  opacity: 0;
  transition: all 0.2s ease;
}

.step-card:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #fef0f0;
  border-radius: 4px;
}

.empty-steps {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  border: 2px dashed #dcdfe6;
  transition: all 0.3s ease;
}

.empty-steps:hover {
  border-color: #409eff;
  background: #f0f5ff;
}

.designer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 28px;
  background: white;
  border-top: 1px solid #e4e7ed;
}

.step-count {
  font-size: 13px;
  color: #606266;
}

.footer-buttons {
  display: flex;
  gap: 12px;
}

.footer-buttons :deep(.el-button) {
  min-width: 100px;
  border-radius: 6px;
  transition: all 0.25s ease;
}

.footer-buttons :deep(.el-button:hover) {
  transform: translateY(-1px);
}

/* ================================================
   分步向导样式 - 沉稳科技风
   ================================================ */
.process-wizard-dialog :deep(.el-dialog__header) {
  padding: 0;
  border-bottom: none;
}

.process-wizard-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.wizard-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 40px 24px;
  background: linear-gradient(135deg, #1a1f36 0%, #2d3748 100%);
}

.wizard-step {
  display: flex;
  align-items: center;
  gap: 12px;
}

.step-circle {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.35s ease;
  border: 2px solid rgba(255, 255, 255, 0.2);
}

.wizard-step.active .step-circle {
  background: #409eff;
  color: white;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
  border-color: transparent;
  transform: scale(1.1);
}

.wizard-step.completed .step-circle {
  background: #67c23a;
  color: white;
  border-color: transparent;
}

.step-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
  transition: all 0.3s ease;
}

.wizard-step.active .step-text {
  color: white;
}

.step-line {
  width: 80px;
  height: 2px;
  background: rgba(255, 255, 255, 0.2);
  margin: 0 20px;
  transition: all 0.5s ease;
}

.step-line.active {
  background: linear-gradient(90deg, #67c23a, #409eff);
}

.wizard-content {
  padding: 28px 40px;
  min-height: 300px;
  background: #f0f2f5;
}

.wizard-content :deep(.el-form) {
  max-width: 520px;
  margin: 0 auto;
  background: white;
  padding: 28px 32px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(26, 31, 54, 0.06);
}

.wizard-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 40px;
  border-top: 1px solid #e4e7ed;
  background: white;
}

.footer-left, .footer-right {
  display: flex;
  gap: 12px;
}

.wizard-footer :deep(.el-button) {
  border-radius: 6px;
  transition: all 0.25s ease;
  padding: 10px 20px;
}

.wizard-footer :deep(.el-button:hover) {
  transform: translateY(-1px);
}

.wizard-footer :deep(.el-button--primary) {
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
}

.wizard-footer :deep(.el-button--primary:hover) {
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.4);
}

/* 设计器内容样式 */
.designer-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.designer-header-inline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: linear-gradient(135deg, #1a1f36 0%, #2d3748 100%);
  border-radius: 8px;
  color: white;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.process-name {
  font-size: 15px;
  font-weight: 600;
}

.process-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.steps-list-container {
  flex: 1;
  max-height: 260px;
  overflow-y: auto;
  padding: 8px 0;
}

.wizard-steps-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wizard-step-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  transition: all 0.25s ease;
  box-shadow: 0 1px 4px rgba(26, 31, 54, 0.04);
}

.wizard-step-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.12);
  transform: translateX(3px);
}

.step-drag {
  cursor: move;
  color: #909399;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}

.wizard-step-card:hover .step-drag {
  color: #409eff;
}

.step-badge {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a1f36 0%, #2d3748 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-form {
  flex: 1;
  display: flex;
  gap: 12px;
  align-items: center;
}

.step-name-input, .step-robot-select {
  flex: 1;
}

.robot-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.empty-wizard-steps {
  padding: 40px 20px;
  background: white;
  border-radius: 8px;
  border: 2px dashed #dcdfe6;
  transition: all 0.3s ease;
}

.empty-wizard-steps:hover {
  border-color: #409eff;
  background: #f0f5ff;
}

</style>

<style>
/* 流程设计器 - 页面内嵌模式（保持在布局内，不覆盖全屏） */
.designer-page-container {
  background: #f0f2f5;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 120px);
}

.designer-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 32px;
  background: white;
  border-bottom: 1px solid #e8ecef;
  box-shadow: 0 1px 3px rgba(44, 62, 80, 0.04);
  flex-shrink: 0;
}

.designer-nav .nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.designer-nav .back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #7f8c8d;
  font-size: 14px;
  padding: 8px 14px;
  border-radius: 6px;
  transition: all 0.25s ease;
}

.designer-nav .back-btn:hover {
  color: #2c3e50;
  background: #f5f7fa;
}

.designer-nav .nav-title {
  font-size: 17px;
  font-weight: 600;
  color: #2c3e50;
}

.designer-nav .nav-actions {
  display: flex;
  gap: 12px;
}

.designer-nav .nav-actions .el-button {
  border-radius: 6px;
  padding: 9px 18px;
  font-size: 14px;
  transition: all 0.25s ease;
}

.designer-main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 画布设计器容器 */
.designer-main-content > div {
  flex: 1;
  overflow: hidden;
}

.designer-page-container .steps-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  background: #f0f2f5;
}

.designer-page-container .steps-wrapper {
  min-height: 350px;
}

.designer-page-container .steps-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 1200px;
  margin: 0 auto;
}

.designer-page-container .step-card {
  background: white;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(26, 31, 54, 0.06);
  border: 1px solid transparent;
  transition: all 0.25s ease;
}

.designer-page-container .step-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.designer-page-container .step-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.designer-page-container .step-drag-handle {
  cursor: move;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #909399;
  transition: all 0.2s ease;
}

.designer-page-container .step-card:hover .step-drag-handle {
  background: #e4e7ed;
  color: #409eff;
}

.designer-page-container .step-number {
  flex-shrink: 0;
}

.designer-page-container .number-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a1f36 0%, #2d3748 100%);
  color: white;
  font-weight: 600;
  font-size: 14px;
  box-shadow: 0 2px 6px rgba(26, 31, 54, 0.25);
}

.designer-page-container .step-content {
  flex: 1;
  min-width: 0;
}

.designer-page-container .step-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.designer-page-container .field-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.designer-page-container .field-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  min-width: 95px;
  text-align: right;
}

.designer-page-container .field-input,
.designer-page-container .field-select {
  flex: 1;
}

.designer-page-container .robot-display {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.designer-page-container .field-input .el-input__wrapper,
.designer-page-container .field-select .el-select__wrapper {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: all 0.2s ease;
}

.designer-page-container .field-input:hover .el-input__wrapper,
.designer-page-container .field-select:hover .el-select__wrapper {
  box-shadow: 0 0 0 1px #409eff inset;
}

.designer-page-container .field-input:focus-within .el-input__wrapper,
.designer-page-container .field-select:focus-within .el-select__wrapper {
  box-shadow: 0 0 0 1px #409eff inset;
}

.designer-page-container .step-actions {
  flex-shrink: 0;
}

.designer-page-container .delete-btn {
  padding: 6px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.designer-page-container .delete-btn:hover {
  background: #fef0f0;
}

.designer-page-container .empty-steps {
  padding: 60px 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(26, 31, 54, 0.06);
  max-width: 1200px;
  margin: 0 auto;
}

.designer-page-container .empty-steps:hover {
  box-shadow: 0 4px 16px rgba(26, 31, 54, 0.1);
}

.designer-footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: white;
  border-top: 1px solid #e8ecef;
  box-shadow: 0 -1px 3px rgba(44, 62, 80, 0.04);
  flex-shrink: 0;
}

.designer-footer-bar .step-count {
  font-size: 13px;
  color: #606266;
}

.designer-footer-bar .footer-buttons {
  display: flex;
  gap: 12px;
}

.designer-footer-bar .footer-buttons .el-button {
  min-width: 100px;
  border-radius: 6px;
  transition: all 0.25s ease;
}

.designer-footer-bar .footer-buttons .el-button:hover {
  transform: translateY(-1px);
}
</style>