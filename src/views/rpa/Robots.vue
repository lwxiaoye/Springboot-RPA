<template>
  <div class="robots-page">
    <!-- 主列表视图 -->
    <div v-if="!showCreateView" class="main-content">
      <div class="page-header">
        <h2>机器人管理</h2>
        <p class="page-desc">管理和监控RPA机器人，按功能分类执行流程任务</p>
      </div>

      <!-- 统计概览 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon primary"><el-icon><Cpu /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.total }}</span>
            <span class="stat-label">机器人总数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon success"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.online }}</span>
            <span class="stat-label">在线机器人</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon warning"><el-icon><Timer /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.busy }}</span>
            <span class="stat-label">忙碌中</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon danger"><el-icon><TrendCharts /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.todayExecutions }}</span>
            <span class="stat-label">今日执行</span>
          </div>
        </div>
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
        <el-button type="primary" @click="showCreateView = true">
          <el-icon><Plus /></el-icon> 注册机器人
        </el-button>
      </div>

      <el-table :data="paginatedRobots" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="机器人名称" min-width="140" />
        <el-table-column prop="ip" label="IP地址" min-width="120" />
        <el-table-column prop="robotCategory" label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getCategoryType(row.robotCategory)" size="small">
              {{ getCategoryText(row.robotCategory) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="queueName" label="所属队列" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.queueName" class="queue-tag">{{ row.queueName }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalExecutions" label="执行次数" width="80" align="center" />
        <el-table-column prop="successRate" label="成功率" width="80" align="center">
          <template #default="{ row }">
            <span :class="getSuccessRateClass(row)">{{ row.successRate || '0' }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="150" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="editRobot(row)">编辑</el-button>
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
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
    </div>

    <!-- 注册机器人视图 -->
    <div v-else class="create-view">
      <div class="create-header">
        <div class="header-left">
          <el-button text @click="showCreateView = false">
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </el-button>
          <h3>注册机器人</h3>
        </div>
      </div>

      <div class="create-content">
        <div class="form-card">
          <div class="form-section">
            <div class="section-header">
              <el-icon><Document /></el-icon>
              <span>基本信息</span>
            </div>
            <el-form :model="createForm" :rules="formRules" ref="createFormRef" label-position="top">
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="机器人名称" prop="name">
                    <el-input v-model="createForm.name" placeholder="请输入机器人名称" size="large" clearable />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="机器人分类" prop="robotCategory">
                    <el-select v-model="createForm.robotCategory" placeholder="选择分类" size="large" style="width: 100%">
                      <el-option v-for="cat in categoryList" :key="cat.code" :label="cat.name" :value="cat.code" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="form-section">
            <div class="section-header">
              <el-icon><Connection /></el-icon>
              <span>网络配置</span>
            </div>
            <el-form label-position="top">
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="IP地址" prop="ip">
                    <el-input v-model="createForm.ip" placeholder="如：192.168.1.100" size="large" clearable />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="主机名">
                    <el-input v-model="createForm.hostname" placeholder="请输入主机名" size="large" clearable />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="端口号">
                    <el-input-number v-model="createForm.port" :min="1" :max="65535" size="large" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="所属队列">
                    <el-select v-model="createForm.queueId" placeholder="选择所属队列" size="large" style="width: 100%" clearable>
                      <el-option v-for="q in queues" :key="q.id" :label="q.name" :value="q.id" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="form-section code-section">
            <div class="section-header">
              <el-icon><MagicStick /></el-icon>
              <span>AI代码生成</span>
            </div>

            <!-- AI生成与模板整合面板 -->
            <div class="ai-template-panel">
              <div class="ai-generator-area">
                <div class="area-header">
                  <span class="area-title">
                    <el-icon><MagicStick /></el-icon>
                    AI智能生成
                  </span>
                </div>
                <div class="area-content">
                  <p class="area-desc">描述您的需求，AI自动生成机器人代码</p>
                  <div class="ai-input-group">
                    <el-input
                      v-model="createForm.aiPrompt"
                      type="textarea"
                      :rows="3"
                      placeholder="例如：采集某网站发票信息，包含发票号码、金额、日期等字段..."
                    />
                    <div class="ai-buttons">
                      <el-button type="primary" @click="generateCodeWithAI" :loading="generatingCode">
                        <el-icon><MagicStick /></el-icon>
                        AI生成代码
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="template-area">
                <div class="area-header">
                  <span class="area-title">
                    <el-icon><Document /></el-icon>
                    代码模板
                  </span>
                </div>
                <div class="template-grid">
                  <div
                    v-for="tpl in codeTemplates"
                    :key="tpl.code"
                    :class="['template-card', { active: createForm.selectedTemplate === tpl.code }]"
                    @click="selectTemplate(tpl)"
                  >
                    <div class="template-icon">
                      <el-icon><component :is="tpl.icon" /></el-icon>
                    </div>
                    <div class="template-info">
                      <span class="template-name">{{ tpl.name }}</span>
                      <span class="template-desc">{{ tpl.description }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 代码编辑器 -->
            <div class="code-editor-wrapper">
              <div class="code-toolbar">
                <div class="toolbar-left">
                  <el-icon><Edit /></el-icon>
                  <span class="toolbar-label">代码编辑器</span>
                </div>
                <div class="toolbar-actions">
                  <el-button size="small" @click="copyCode">
                    <el-icon><Document /></el-icon>
                    复制
                  </el-button>
                </div>
              </div>
              <el-input
                v-model="createForm.robotCode"
                type="textarea"
                :rows="10"
                placeholder="AI生成的代码将显示在这里，或手动输入机器人执行代码..."
                class="code-textarea"
              />
            </div>
            <div class="code-hint">
              <el-icon><InfoFilled /></el-icon>
              <span>支持命令: @collect URL | @parse | @process clean,transform | @store table_name | @log message</span>
            </div>
          </div>

          <div class="form-section">
            <div class="section-header">
              <el-icon><More /></el-icon>
              <span>附加信息</span>
            </div>
            <el-form label-position="top">
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="能力描述">
                    <el-input v-model="createForm.capabilities" type="textarea" :rows="2" placeholder="描述机器人的核心能力..." size="large" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="备注说明">
                    <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="补充说明信息..." size="large" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="form-actions">
            <el-button size="large" @click="showCreateView = false">取消</el-button>
            <el-button type="primary" size="large" @click="submitCreate" :loading="submitLoading">确认注册</el-button>
          </div>
        </div>
      </div>
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

    <!-- 编辑机器人弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑机器人"
      width="760px"
      class="robot-dialog"
      :close-on-click-modal="false"
    >
      <div class="dialog-content">
        <!-- 基本信息区块 -->
        <div class="form-section">
          <div class="section-header">
            <el-icon><Document /></el-icon>
            <span>基本信息</span>
          </div>
          <el-form :model="editForm" :rules="formRules" ref="editFormRef" label-position="top">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="机器人名称" prop="name">
                  <el-input
                    v-model="editForm.name"
                    placeholder="请输入机器人名称"
                    size="large"
                    clearable
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="机器人分类" prop="robotCategory">
                  <el-select
                    v-model="editForm.robotCategory"
                    placeholder="选择分类"
                    size="large"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="cat in categoryList"
                      :key="cat.code"
                      :label="cat.name"
                      :value="cat.code"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>

        <!-- 网络配置区块 -->
        <div class="form-section">
          <div class="section-header">
            <el-icon><Connection /></el-icon>
            <span>网络配置</span>
          </div>
          <el-form label-position="top">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="IP地址" prop="ip">
                  <el-input
                    v-model="editForm.ip"
                    placeholder="如：192.168.1.100"
                    size="large"
                    clearable
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="主机名">
                  <el-input
                    v-model="editForm.hostname"
                    placeholder="请输入主机名"
                    size="large"
                    clearable
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="端口号">
                  <el-input-number
                    v-model="editForm.port"
                    :min="1"
                    :max="65535"
                    size="large"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所属队列">
                  <el-select
                    v-model="editForm.queueId"
                    placeholder="选择所属队列"
                    size="large"
                    style="width: 100%"
                    clearable
                  >
                    <el-option
                      v-for="q in queues"
                      :key="q.id"
                      :label="q.name"
                      :value="q.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>

        <!-- 代码配置区块 -->
        <div class="form-section code-section">
          <div class="section-header">
            <el-icon><Monitor /></el-icon>
            <span>代码配置</span>
          </div>
          <el-form label-position="top">
            <el-form-item label="代码模板">
              <div class="template-selector">
                <el-select
                  v-model="editForm.selectedTemplate"
                  placeholder="选择代码模板快速生成"
                  size="default"
                  style="width: 100%"
                  @change="onEditTemplateChange"
                >
                  <el-option label="不使用模板" value="" />
                  <el-option label="【采集】网页采集模板" value="collect" />
                  <el-option label="【解析】HTML表格解析模板" value="parse" />
                  <el-option label="【加工】数据清洗转换模板" value="process" />
                  <el-option label="【落库】数据库存储模板" value="store" />
                </el-select>
              </div>
            </el-form-item>
            <el-form-item label="机器人代码">
              <div class="code-editor-wrapper">
                <div class="code-toolbar">
                  <div class="toolbar-left">
                    <el-icon><Edit /></el-icon>
                    <span class="toolbar-label">代码编辑器</span>
                  </div>
                  <div class="toolbar-actions">
                    <el-button
                      type="primary"
                      plain
                      size="small"
                      @click="goToAiGeneratorForEdit"
                    >
                      <el-icon><MagicStick /></el-icon>
                      AI生成
                    </el-button>
                  </div>
                </div>
                <el-input
                  v-model="editForm.robotCode"
                  type="textarea"
                  :rows="8"
                  placeholder="请输入机器人执行代码，或使用上方模板生成..."
                  class="code-textarea"
                />
              </div>
              <div class="code-hint">
                <el-icon><InfoFilled /></el-icon>
                <span>支持命令: @collect URL | @parse | @process clean,transform | @store table_name | @log message</span>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 附加信息区块 -->
        <div class="form-section">
          <div class="section-header">
            <el-icon><More /></el-icon>
            <span>附加信息</span>
          </div>
          <el-form label-position="top">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="能力描述">
                  <el-input
                    v-model="editForm.capabilities"
                    type="textarea"
                    :rows="2"
                    placeholder="描述机器人的核心能力..."
                    size="large"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="备注说明">
                  <el-input
                    v-model="editForm.description"
                    type="textarea"
                    :rows="2"
                    placeholder="补充说明信息..."
                    size="large"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="启用状态">
              <el-switch v-model="editForm.enabled" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-form>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" size="large" @click="submitEdit" :loading="submitLoading">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, FolderOpened, Cpu, CircleCheck, Timer, TrendCharts, InfoFilled, MagicStick, Document, Edit, Monitor, Connection, More, ArrowLeft } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
const categoryLoading = ref(false)

const robots = ref([])
const processes = ref([])
const queues = ref([])
const categoryList = ref([])
const searchKeyword = ref('')
const categoryFilter = ref('')
const statusFilter = ref('')

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const categoryDialogVisible = ref(false)
const showCreateView = ref(false)
const isEditMode = computed(() => editDialogVisible.value)

const createFormRef = ref(null)
const editFormRef = ref(null)
const currentEditId = ref(null)

const editForm = reactive({
  name: '',
  robotCategory: '',
  capabilities: '',
  ip: '',
  hostname: '',
  port: 8080,
  robotCode: '',
  selectedTemplate: '',
  queueId: null,
  description: '',
  enabled: true
})

const pagination = reactive({ page: 1, size: 10, total: 0 })

const stats = reactive({
  total: 0,
  online: 0,
  busy: 0,
  todayExecutions: 0
})

const createForm = reactive({
  name: '',
  robotCategory: 'DATA_COLLECT',
  capabilities: '',
  ip: '',
  hostname: '',
  port: 8080,
  robotCode: '',
  selectedTemplate: '',
  aiPrompt: '',
  queueId: null,
  description: ''
})

// 代码模板列表
const codeTemplates = [
  {
    code: 'collect',
    name: '数据采集',
    description: '网页数据采集模板',
    icon: 'Download',
    template: `// 数据采集机器人
// 功能：采集指定网页的HTML内容
@collect http://localhost:8081/spider_target
@table_selector #data-table tbody tr
@columns 列1,列2,列3
@log 采集任务完成`
  },
  {
    code: 'parse',
    name: '数据解析',
    description: 'HTML表格解析模板',
    icon: 'Document',
    template: `// 数据解析机器人
// 功能：从HTML中解析发票表格数据
@parse
@parse_rule $.data.items[*]
@log 解析完成`
  },
  {
    code: 'process',
    name: '数据加工',
    description: '数据清洗转换模板',
    icon: 'Cpu',
    template: `// 数据加工机器人
// 功能：数据清洗、转换、校验
@process clean,transform,validate
@validate mobile=手机号,email=邮箱
@log 加工完成`
  },
  {
    code: 'store',
    name: '数据落库',
    description: '数据库存储模板',
    icon: 'FolderOpened',
    template: `// 数据落库机器人
// 功能：将数据保存到数据库
@store invoice_data
@columns 发票号码,金额,日期
@log 落库完成`
  }
]

// 选择模板
const selectTemplate = (tpl) => {
  createForm.selectedTemplate = tpl.code
  createForm.robotCode = tpl.template
  ElMessage.success(`已选择【${tpl.name}】模板`)
}

// 使用DeepSeek API生成代码
const generatingCode = ref(false)
const generateCodeWithAI = async () => {
  if (!createForm.aiPrompt.trim()) {
    ElMessage.warning('请输入需求描述')
    return
  }

  generatingCode.value = true

  try {
    const response = await apiPost('/api/ai/generate-robot-code', {
      prompt: createForm.aiPrompt,
      category: createForm.robotCategory
    })

    if (response.code === 0 || response.data) {
      createForm.robotCode = response.data?.code || response.data
      ElMessage.success('代码生成成功')
    } else {
      throw new Error(response.message || '生成失败')
    }
  } catch (error) {
    // 本地模拟生成（当API不可用时）
    const categoryNames = {
      'DATA_COLLECT': '数据采集',
      'DATA_PARSE': '数据解析',
      'DATA_PROCESS': '数据加工',
      'GENERAL': '通用执行'
    }

    const mockCode = `// AI生成的RPA机器人
// 分类: ${categoryNames[createForm.robotCategory] || '通用'}
// 描述: ${createForm.aiPrompt}

@collect http://example.com/data
@table_selector table.data-list tbody tr
@columns 列1,列2,列3
@process clean,transform
@store report_data
@log ${categoryNames[createForm.robotCategory] || '任务'}执行完成`

    createForm.robotCode = mockCode
    ElMessage.warning('使用本地模板生成（请配置DeepSeek API）')
  } finally {
    generatingCode.value = false
  }
}

// 复制代码
const copyCode = () => {
  if (!createForm.robotCode) {
    ElMessage.warning('暂无代码可复制')
    return
  }
  navigator.clipboard.writeText(createForm.robotCode)
    .then(() => ElMessage.success('已复制到剪贴板'))
    .catch(() => ElMessage.error('复制失败'))
}

// 跳转到AI代码生成页面
const goToAiGenerator = () => {
  const category = createForm.robotCategory || 'DATA_COLLECT'
  router.push(`/rpa/ai-code-generator?category=${category}`)
}

// 跳转到AI代码生成页面（编辑模式）
const goToAiGeneratorForEdit = () => {
  const category = editForm.robotCategory || 'DATA_COLLECT'
  // 保存当前编辑状态
  localStorage.setItem('editRobotCode', editForm.robotCode)
  localStorage.setItem('editRobotCategory', category)
  router.push(`/rpa/ai-code-generator?category=${category}&editMode=true`)
}

// 显示AI代码生成弹窗
const showAiCodeDialog = () => {
  const category = createForm.robotCategory || 'DATA_COLLECT'
  router.push(`/rpa/ai-code-generator?category=${category}`)
}

// 插入列配置提示
const insertTemplateHint = () => {
  ElMessage.info('请在AI代码生成器中选择模板生成代码')
}

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
    'DATA_STORE': '数据落库',
    'GENERAL': '通用执行'
  }
  return map[category] || category || '通用执行'
}

const getCategoryType = (category) => {
  const map = {
    'DATA_COLLECT': 'primary',
    'DATA_PARSE': 'success',
    'DATA_PROCESS': 'warning',
    'DATA_STORE': 'danger',
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

const getSuccessRateClass = (row) => {
  const rate = row.successRate || 0
  if (rate >= 90) return 'rate-high'
  if (rate >= 70) return 'rate-mid'
  return 'rate-low'
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

const updateStats = () => {
  stats.total = robots.value.length
  stats.online = robots.value.filter(r => r.status !== 'offline').length
  stats.busy = robots.value.filter(r => r.status === 'busy').length
  stats.todayExecutions = robots.value.reduce((sum, r) => sum + (r.todayExecutions || 0), 0)
}

const loadRobots = async () => {
  loading.value = true
  try {
    const result = await apiGet('/robot')
    if (result.code === 0) {
      robots.value = result.data || []
      updateStats()
    }
  } catch {
    robots.value = []
  } finally {
    loading.value = false
  }
}

const loadProcesses = async () => {
  try {
    const result = await apiGet('/process')
    if (result.code === 0) {
      processes.value = result.data || []
    }
  } catch {
    processes.value = []
  }
}

const loadQueues = async () => {
  try {
    const result = await apiGet('/queue')
    if (result.code === 0) {
      queues.value = result.data || []
    }
  } catch {
    queues.value = []
  }
}

const loadCategories = async () => {
  try {
    const result = await apiGet('/robot/category/list')
    if (result.code === 0) {
      categoryList.value = result.data || []
    }
  } catch {
    categoryList.value = [
      { code: 'DATA_COLLECT', name: '数据采集' },
      { code: 'DATA_PARSE', name: '数据解析' },
      { code: 'DATA_PROCESS', name: '数据加工' },
      { code: 'DATA_STORE', name: '数据落库' },
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
  if (['DATA_COLLECT', 'DATA_PARSE', 'DATA_PROCESS', 'DATA_STORE', 'GENERAL'].includes(cat.code)) {
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
  // 检查是否有AI生成的代码
  const aiCode = localStorage.getItem('aiGeneratedCode')
  const aiCategory = localStorage.getItem('aiRobotCategory')
  const aiName = localStorage.getItem('aiRobotName')
  const aiDesc = localStorage.getItem('aiDescription')

  Object.assign(createForm, {
    name: aiName || '',
    robotCategory: aiCategory || 'DATA_COLLECT',
    capabilities: '',
    ip: '',
    hostname: '',
    port: 8080,
    robotCode: aiCode || '',
    selectedTemplate: '',
    queueId: null,
    description: aiDesc || ''
  })

  // 清除localStorage
  localStorage.removeItem('aiGeneratedCode')
  localStorage.removeItem('aiRobotCategory')
  localStorage.removeItem('aiRobotName')
  localStorage.removeItem('aiDescription')

  createDialogVisible.value = true
}

// 编辑机器人
const editRobot = (robot) => {
  currentEditId.value = robot.id
  Object.assign(editForm, {
    name: robot.name,
    robotCategory: robot.robotCategory,
    capabilities: robot.capabilities || '',
    ip: robot.ip || '',
    hostname: robot.hostname || '',
    port: robot.port || 8080,
    robotCode: robot.robotCode || '',
    selectedTemplate: '',
    queueId: robot.queueId || null,
    description: robot.description || '',
    enabled: robot.enabled !== false
  })

  // 检查是否有AI生成的代码（编辑模式）
  const aiCode = localStorage.getItem('editRobotCode')
  if (aiCode) {
    editForm.robotCode = aiCode
    localStorage.removeItem('editRobotCode')
    localStorage.removeItem('editRobotCategory')
  }

  editDialogVisible.value = true
}

const onEditTemplateChange = (template) => {
  if (template && codeTemplates[template]) {
    editForm.robotCode = codeTemplates[template]
  }
}

const submitEdit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const queue = queues.value.find(q => q.id === editForm.queueId)
        const result = await apiPut(`/robot/${currentEditId.value}`, {
          name: editForm.name,
          robotCategory: editForm.robotCategory,
          capabilities: editForm.capabilities,
          ip: editForm.ip,
          hostname: editForm.hostname,
          port: editForm.port,
          robotCode: editForm.robotCode,
          description: editForm.description,
          queueId: editForm.queueId || null,
          queueName: queue?.name || null,
          enabled: editForm.enabled
        })
        if (result.code === 0) {
          ElMessage.success('更新成功')
          editDialogVisible.value = false
          await loadRobots()
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

const viewDetail = (robot) => {
  router.push(`/rpa/robot/${robot.id}`)
}

const submitCreate = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const queue = queues.value.find(q => q.id === createForm.queueId)
        const result = await apiPost('/robot', {
          name: createForm.name,
          robotCategory: createForm.robotCategory,
          capabilities: createForm.capabilities,
          ip: createForm.ip,
          hostname: createForm.hostname,
          port: createForm.port,
          robotCode: createForm.robotCode,
          description: createForm.description,
          queueId: createForm.queueId || null,
          queueName: queue?.name || null
        })
        if (result.code === 0) {
          ElMessage.success('注册成功')
          showCreateView.value = false
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

const deleteRobot = async (robot) => {
  try {
    const result = await apiDelete(`/robot/${robot.id}`)
    if (result.code === 0) {
      const index = robots.value.findIndex(r => r.id === robot.id)
      if (index !== -1) {
        robots.value.splice(index, 1)
        pagination.total--
        updateStats()
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
  loadProcesses()
  loadQueues()
})
</script>

<style scoped>
.robots-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* 主内容区域 */
.main-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 注册视图 */
.create-view {
  background: var(--bg-tertiary, #f9fafb);
  min-height: calc(100vh - 120px);
  border-radius: 16px;
  padding: 24px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}

.create-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
}

.create-content {
  max-width: 900px;
  margin: 0 auto;
}

.form-card {
  background: var(--bg-secondary, #ffffff);
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* 表单操作按钮 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color, #e5e7eb);
}

.form-actions .el-button {
  min-width: 120px;
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
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

/* 统计概览 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-secondary, #ffffff);
  padding: 24px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--border-color, #e5e7eb);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md, 0 4px 12px rgba(0, 0, 0, 0.1));
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  flex-shrink: 0;
}

.stat-icon.primary { background: linear-gradient(135deg, #409eff, #66b1ff); box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3); }
.stat-icon.success { background: linear-gradient(135deg, #67c23a, #85ce61); box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3); }
.stat-icon.warning { background: linear-gradient(135deg, #e6a23c, #ebb563); box-shadow: 0 4px 12px rgba(230, 162, 60, 0.3); }
.stat-icon.danger { background: linear-gradient(135deg, #f56c6c, #f78989); box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3); }

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary, #6b7280);
}

/* 工具栏 */
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
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
}

.search-box input::placeholder {
  color: var(--text-tertiary, #9ca3af);
}

/* 表格容器 */
:deep(.el-table) {
  border-radius: 12px;
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
  background: var(--bg-primary, #f5f7fa) !important;
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

.text-muted {
  color: var(--text-secondary, #6b7280);
  font-size: 13px;
}

.process-tag {
  color: var(--primary, #409eff);
  font-weight: 600;
}

.queue-tag {
  color: var(--success, #67c23a);
  font-size: 13px;
  font-weight: 500;
}

.rate-high { color: var(--success, #67c23a); font-weight: 700; }
.rate-mid { color: var(--warning, #e6a23c); font-weight: 700; }
.rate-low { color: var(--danger, #f56c6c); font-weight: 700; }

/* 机器人注册对话框样式 */
:deep(.robot-dialog) {
  --el-dialog-border-radius: 16px;
}

:deep(.robot-dialog .el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
  margin-right: 0;
}

:deep(.robot-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
}

:deep(.robot-dialog .el-dialog__body) {
  padding: 0;
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.robot-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color, #e5e7eb);
}

/* 对话框内容区域 */
.dialog-content {
  padding: 20px 24px;
}

/* 表单区块 */
.form-section {
  margin-bottom: 24px;
  padding: 20px;
  background: var(--bg-tertiary, #f9fafb);
  border-radius: 12px;
  border: 1px solid var(--border-color, #e5e7eb);
  transition: all 0.3s ease;
}

.form-section:last-child {
  margin-bottom: 0;
}

.form-section:hover {
  border-color: var(--primary, #409eff);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.code-section {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

/* 区块标题 */
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  color: var(--text-primary, #374151);
  font-size: 15px;
  font-weight: 600;
}

.section-header .el-icon {
  font-size: 18px;
  color: var(--primary, #409eff);
}

/* 表单项样式优化 */
:deep(.robot-dialog .el-form-item) {
  margin-bottom: 16px;
}

:deep(.robot-dialog .el-form-item__label) {
  font-weight: 500;
  color: var(--text-primary, #374151);
  font-size: 14px;
  padding-bottom: 6px;
  line-height: 1.4;
}

:deep(.robot-dialog .el-input__wrapper),
:deep(.robot-dialog .el-select__wrapper) {
  border-radius: 8px;
  padding: 4px 12px;
  transition: all 0.2s ease;
}

:deep(.robot-dialog .el-input__wrapper:hover),
:deep(.robot-dialog .el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary, #409eff) inset;
}

:deep(.robot-dialog .el-input__wrapper.is-focus),
:deep(.robot-dialog .el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) inset;
}

:deep(.robot-dialog .el-input__inner) {
  height: 40px;
  font-size: 14px;
}

:deep(.robot-dialog .el-input-number) {
  width: 100%;
}

:deep(.robot-dialog .el-input-number .el-input__wrapper) {
  padding: 0 12px;
}

/* AI+模板整合面板 */
.ai-template-panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.ai-generator-area,
.template-area {
  background: var(--bg-tertiary, #f9fafb);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 12px;
  padding: 16px;
}

.area-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.area-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #374151);
}

.area-title .el-icon {
  color: var(--primary, #409eff);
}

.area-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--text-secondary, #6b7280);
}

.ai-input-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-buttons {
  display: flex;
  gap: 8px;
}

.ai-buttons .el-button {
  flex: 1;
}

/* 模板卡片网格 */
.template-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.template-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--bg-secondary, #ffffff);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.template-card:hover {
  border-color: var(--primary, #409eff);
  background: var(--bg-primary, #f0f9ff);
  transform: translateY(-2px);
}

.template-card.active {
  border-color: var(--primary, #409eff);
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.template-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border-radius: 8px;
  color: white;
  font-size: 18px;
}

.template-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.template-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #374151);
  white-space: nowrap;
}

.template-desc {
  font-size: 11px;
  color: var(--text-secondary, #6b7280);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 代码编辑器容器 */
.code-editor-wrapper {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 10px;
  overflow: hidden;
  background: #1e1e1e;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.code-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: linear-gradient(180deg, #2d2d2d 0%, #252525 100%);
  border-bottom: 1px solid #3d3d3d;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #a0a0a0;
}

.toolbar-label {
  font-size: 13px;
  font-weight: 500;
  color: #ccc;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.toolbar-actions .el-button {
  border-radius: 6px;
}

/* 代码文本域 */
.code-textarea :deep(textarea) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  background: #1e1e1e;
  color: #d4d4d4;
  border: none;
  padding: 16px;
  line-height: 1.6;
  resize: vertical;
}

.code-textarea :deep(textarea):focus {
  outline: none;
  box-shadow: none;
}

/* 代码提示框 */
.code-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary, #6b7280);
  margin-top: 10px;
  padding: 10px 14px;
  background: rgba(64, 158, 255, 0.08);
  border-radius: 8px;
  border-left: 3px solid var(--primary, #409eff);
}

.code-hint .el-icon {
  color: var(--primary, #409eff);
  flex-shrink: 0;
}

/* 模板选择器 */
.template-selector {
  margin-bottom: 4px;
}

:deep(.template-selector .el-select) {
  --el-select-input-focus-border-color: var(--primary, #409eff);
}

/* 对话框底部按钮 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-footer .el-button {
  min-width: 100px;
  border-radius: 8px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  :deep(.robot-dialog) {
    width: 90% !important;
    max-width: 600px;
  }

  .dialog-content {
    padding: 16px;
  }

  .form-section {
    padding: 16px;
    margin-bottom: 16px;
  }

  :deep(.robot-dialog .el-col) {
    width: 100% !important;
  }
}

/* 滚动条样式 */
:deep(.robot-dialog .el-dialog__body)::-webkit-scrollbar {
  width: 6px;
}

:deep(.robot-dialog .el-dialog__body)::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}

:deep(.robot-dialog .el-dialog__body)::-webkit-scrollbar-track {
  background: transparent;
}

/* AI代码生成弹窗样式 */
:deep(.ai-code-dialog .el-divider--horizontal) {
  margin: 16px 0 12px;
}

:deep(.ai-code-dialog .el-divider__text) {
  font-size: 13px;
  color: var(--text-secondary, #6b7280);
}

/* 编辑弹窗特殊样式 */
.edit-dialog :deep(.el-input__wrapper) {
  border-radius: 10px;
}
</style> 