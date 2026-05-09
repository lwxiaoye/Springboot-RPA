<template>
  <div class="robots-page">
    <!-- 主列表视图 -->
    <div v-if="!showCreateView" class="main-content">
      <div class="page-header">
        <h2>{{ t('robot.title') }}</h2>
        <p class="page-desc">{{ t('robot.title') }}</p>
      </div>

      <!-- 统计概览 -->
      <div class="stats-row">
        <div class="stat-card" @click="goTo('/rpa/tasks')">
          <div class="stat-icon-wrapper" style="background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);">
            <el-icon style="color: #1976d2;"><Monitor /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">{{ t('robot.totalRobots') }}</div>
          </div>
          <div class="stat-arrow"><el-icon><ArrowRight /></el-icon></div>
        </div>

        <div class="stat-card" @click="goTo('/rpa/robots?status=online')">
          <div class="stat-icon-wrapper" style="background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);">
            <el-icon style="color: #388e3c;"><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value success">{{ stats.online }}</div>
            <div class="stat-label">{{ t('robot.online') }}</div>
          </div>
          <div class="stat-arrow"><el-icon><ArrowRight /></el-icon></div>
        </div>

        <div class="stat-card" @click="goTo('/rpa/robots?status=busy')">
          <div class="stat-icon-wrapper" style="background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);">
            <el-icon style="color: #f57c00;"><Timer /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value warning">{{ stats.busy }}</div>
            <div class="stat-label">{{ t('robot.busy') }}</div>
          </div>
          <div class="stat-arrow"><el-icon><ArrowRight /></el-icon></div>
        </div>

        <div class="stat-card" @click="goTo('/rpa/logs')">
          <div class="stat-icon-wrapper" style="background: linear-gradient(135deg, #fce4ec 0%, #f8bbd0 100%);">
            <el-icon style="color: #c2185b;"><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.todayExecutions }}</div>
            <div class="stat-label">{{ t('robot.todayExecutions') }}</div>
          </div>
          <div class="stat-arrow"><el-icon><ArrowRight /></el-icon></div>
        </div>
      </div>

      <div class="toolbar">
        <div class="search-box">
          <el-icon><Search /></el-icon>
          <input v-model="searchKeyword" :placeholder="t('robot.searchPlaceholder')" />
        </div>
        <el-select v-model="categoryFilter" :placeholder="t('robot.categoryFilter')" clearable style="width: 140px;">
          <el-option v-for="cat in categoryList" :key="cat.code" :label="cat.name" :value="cat.code" />
        </el-select>
        <el-select v-model="statusFilter" :placeholder="t('robot.statusFilter')" clearable style="width: 120px;">
          <el-option :label="t('robot.idle')" value="idle" />
          <el-option :label="t('robot.busy')" value="busy" />
          <el-option :label="t('robot.offline')" value="offline" />
        </el-select>
        <el-button @click="showCategoryDialog">
          <el-icon><FolderOpened /></el-icon> {{ t('robot.categoryManage') }}
        </el-button>
        <el-button type="primary" @click="showCreateView = true">
          <el-icon><Plus /></el-icon> {{ t('robot.register') }}
        </el-button>
      </div>

      <el-table :data="paginatedRobots" v-loading="loading" border stripe highlight-current-row class="robots-table" :default-sort="{ prop: 'createTime', order: 'descending' }">
        <el-table-column type="index" :label="t('common.index')" width="60" align="center">
          <template #default="{ $index }">
            <div class="index-cell">
              <div class="index-line"></div>
              <span class="index-number">{{ (pagination.page - 1) * pagination.size + $index + 1 }}</span>
              <div class="index-line"></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="t('robot.name')" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column prop="ip" :label="t('robot.ip')" width="110" align="center" />
        <el-table-column prop="robotCategory" :label="t('common.category')" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getCategoryType(row.robotCategory)" size="small" effect="light">
              {{ getCategoryText(row.robotCategory) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="queueName" :label="t('robot.queue')" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.queueName" class="queue-tag">{{ row.queueName }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('common.status')" width="75" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <span class="status-dot" :class="row.status"></span>
              <el-tag :type="getStatusType(row.status)" size="small" effect="light">
                {{ getStatusText(row.status) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalExecutions" :label="t('robot.executions')" width="75" align="center" />
        <el-table-column prop="successRate" :label="t('robot.successRate')" width="70" align="center">
          <template #default="{ row }">
            <div class="rate-cell" :class="getSuccessRateClass(row)">
              <el-icon v-if="getSuccessRateClass(row) === 'rate-high'"><CircleCheck /></el-icon>
              <span>{{ row.successRate || '0' }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" :label="t('robot.lastHeartbeat')" min-width="140" align="center">
          <template #default="{ row }">
            <span v-if="row.lastHeartbeat" class="heartbeat-time">{{ formatHeartbeat(row.lastHeartbeat) }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" @click="editRobot(row)" class="action-btn">
                <span>{{ t('common.edit') }}</span>
              </el-button>
              <el-button link type="primary" @click="viewDetail(row)" class="action-btn">
                <span>{{ t('common.view') }}</span>
              </el-button>
              <el-popconfirm
                :title="t('robot.deleteConfirm', { name: row.name })"
                :confirmButtonText="t('robot.confirmDelete')"
                :cancelButtonText="t('common.cancel')"
                icon="Delete"
                iconColor="#f56c6c"
                @confirm="deleteRobot(row)"
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
                    v-model="editForm.aiPrompt"
                    type="textarea"
                    :rows="3"
                    placeholder="例如：采集某网站发票信息，包含发票号码、金额、日期等字段..."
                  />
                  <div class="ai-buttons">
                    <el-button type="primary" @click="generateCodeWithAIForEdit" :loading="generatingCode">
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
                  快速模板
                </span>
              </div>
              <div class="template-grid">
                <div
                  v-for="tpl in codeTemplates"
                  :key="tpl.code"
                  class="template-card"
                  :class="{ active: editForm.selectedTemplate === tpl.code }"
                  @click="selectTemplateForEdit(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><component :is="tpl.icon" /></el-icon>
                  </div>
                  <div class="template-info">
                    <div class="template-name">{{ tpl.name }}</div>
                    <div class="template-desc">{{ tpl.description }}</div>
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
                <el-button
                  type="primary"
                  plain
                  size="small"
                  @click="goToAiGeneratorForEdit"
                >
                  <el-icon><MagicStick /></el-icon>
                  AI代码助手
                </el-button>
                <el-button type="default" plain size="small" @click="copyCodeForEdit">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </div>
            </div>
            <el-input
              v-model="editForm.robotCode"
              type="textarea"
              :rows="10"
              placeholder="在此编辑机器人代码..."
              class="code-textarea"
            />
          </div>
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
            <el-form-item label="机器人状态">
              <el-select v-model="editForm.status" placeholder="选择状态" size="large" style="width: 100%">
                <el-option label="空闲" value="idle" />
                <el-option label="忙碌" value="busy" />
                <el-option label="离线" value="offline" />
              </el-select>
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
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Plus, FolderOpened, Cpu, CircleCheck, Timer, TrendCharts, InfoFilled, MagicStick, Document, Edit, Monitor, Connection, More, ArrowLeft, ArrowRight, View, Delete } from '@element-plus/icons-vue'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const { t } = useI18n()
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
  enabled: true,
  status: 'idle'
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

// 使用智谱AI API生成代码
const generatingCode = ref(false)
const generateCodeWithAI = async () => {
  if (!createForm.aiPrompt.trim()) {
    ElMessage.warning('请输入需求描述')
    return
  }

  generatingCode.value = true

  try {
    const response = await apiPost('/ai/generate-robot-code', {
      prompt: createForm.aiPrompt,
      category: createForm.robotCategory
    })

    if (response.code === 0 && response.data?.code) {
      createForm.robotCode = response.data.code
      ElMessage.success('代码生成成功')
    } else {
      // API返回错误，使用备用代码
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
      ElMessage.warning(response.message || '使用本地模板生成，请确保已在集成中心配置智谱AI')
    }
  } catch (error) {
    // 网络错误等
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
    ElMessage.warning('AI服务不可用，使用本地模板生成')
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

// AI生成代码（编辑模式）
const generateCodeWithAIForEdit = async () => {
  if (!editForm.aiPrompt?.trim()) {
    ElMessage.warning('请输入需求描述')
    return
  }

  generatingCode.value = true

  try {
    const response = await apiPost('/ai/generate-robot-code', {
      prompt: editForm.aiPrompt,
      category: editForm.robotCategory
    })

    if (response.code === 0 && response.data?.code) {
      editForm.robotCode = response.data.code
      ElMessage.success('代码生成成功')
    } else {
      // API返回错误，使用备用代码
      const categoryNames = {
        'DATA_COLLECT': '数据采集',
        'DATA_PARSE': '数据解析',
        'DATA_PROCESS': '数据加工',
        'GENERAL': '通用执行'
      }

      const mockCode = `// AI生成的RPA机器人\n// 分类: ${categoryNames[editForm.robotCategory] || '通用'}\n// 描述: ${editForm.aiPrompt}\n\n@collect http://example.com/data\n@table_selector table.data-list tbody tr\n@columns 列1,列2,列3\n@process clean,transform\n@store report_data\n@log ${categoryNames[editForm.robotCategory] || '任务'}执行完成`

      editForm.robotCode = mockCode
      ElMessage.warning(response.message || '使用本地模板生成，请确保已在集成中心配置智谱AI')
    }
  } catch (error) {
    // 网络错误等
    const categoryNames = {
      'DATA_COLLECT': '数据采集',
      'DATA_PARSE': '数据解析',
      'DATA_PROCESS': '数据加工',
      'GENERAL': '通用执行'
    }

    const mockCode = `// AI生成的RPA机器人\n// 分类: ${categoryNames[editForm.robotCategory] || '通用'}\n// 描述: ${editForm.aiPrompt}\n\n@collect http://example.com/data\n@table_selector table.data-list tbody tr\n@columns 列1,列2,列3\n@process clean,transform\n@store report_data\n@log ${categoryNames[editForm.robotCategory] || '任务'}执行完成`

    editForm.robotCode = mockCode
    ElMessage.warning('AI服务不可用，使用本地模板生成')
  } finally {
    generatingCode.value = false
  }
}

// 复制代码（编辑模式）
const copyCodeForEdit = () => {
  if (!editForm.robotCode) {
    ElMessage.warning('暂无代码可复制')
    return
  }
  navigator.clipboard.writeText(editForm.robotCode)
    .then(() => ElMessage.success('已复制到剪贴板'))
    .catch(() => ElMessage.error('复制失败'))
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

// 格式化心跳时间
const formatHeartbeat = (time) => {
  if (!time) return '-'
  const date = new Date(time)

  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
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
    enabled: robot.enabled !== false,
    status: robot.status || 'idle'
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
          enabled: editForm.enabled,
          status: editForm.status
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
  padding: 20px 24px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--border-color, #e5e7eb);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary) 0%, var(--primary-light) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md, 0 8px 24px rgba(0, 0, 0, 0.12));
  border-color: var(--primary, #409eff);
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.stat-card:hover .stat-icon-wrapper {
  transform: scale(1.1);
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  line-height: 1;
  letter-spacing: -0.5px;
}

.stat-value.success {
  color: #388e3c;
}

.stat-value.warning {
  color: #f57c00;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary, #6b7280);
}

.stat-arrow {
  opacity: 0;
  transform: translateX(-8px);
  transition: all 0.3s ease;
  color: var(--primary, #409eff);
}

.stat-card:hover .stat-arrow {
  opacity: 1;
  transform: translateX(0);
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
.robots-table {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color, #e5e7eb);
}

:deep(.robots-table) .el-table__header-wrapper th {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
  padding: 12px 0;
  border-bottom: 2px solid #e5e7eb;
}

:deep(.robots-table) .el-table__body-wrapper td {
  padding: 10px 0;
  vertical-align: middle;
}

:deep(.robots-table) .el-table__header-wrapper th .cell,
:deep(.robots-table) .el-table__body-wrapper td .cell {
  padding: 0 6px;
}

:deep(.robots-table .el-table__row) {
  transition: all 0.2s ease;
}

:deep(.robots-table .el-table__row:hover > td) {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

:deep(.robots-table .el-table__row.current-row > td) {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%) !important;
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

:deep(.robots-table .el-table__row:hover .index-line) {
  height: 12px;
  opacity: 1;
}

:deep(.robots-table .el-table__row:hover .index-number) {
  transform: scale(1.1);
  transition: transform 0.3s ease;
}

/* 状态单元格 */
.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  animation: pulse 2s infinite;
}

.status-dot.idle {
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.5);
}

.status-dot.busy {
  background: #e6a23c;
  box-shadow: 0 0 6px rgba(230, 162, 60, 0.5);
}

.status-dot.offline {
  background: #909399;
  box-shadow: 0 0 6px rgba(144, 147, 153, 0.5);
  animation: none;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(103, 194, 58, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
  }
}

/* 成功率单元格 */
.rate-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.rate-high {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.1);
}

.rate-mid {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.1);
}

.rate-low {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
}

:deep(.robots-table .el-table__row:hover .rate-cell) {
  transform: scale(1.05);
}

/* 心跳时间 */
.heartbeat-time {
  font-size: 12px;
  color: #606266;
  font-family: 'Consolas', 'Monaco', monospace;
  background: rgba(64, 158, 255, 0.08);
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px dashed rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
}

:deep(.robots-table .el-table__row:hover .heartbeat-time) {
  background: rgba(64, 158, 255, 0.15);
  border-color: rgba(64, 158, 255, 0.5);
  transform: translateX(2px);
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

/* 分页 */
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  background: var(--bg-secondary, #ffffff);
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-color, #e5e7eb);
}

.text-muted {
  color: var(--text-secondary, #6b7280);
  font-size: 13px;
}

.queue-tag {
  color: var(--success, #67c23a);
  font-size: 12px;
  font-weight: 500;
  background: rgba(103, 194, 58, 0.08);
  padding: 2px 8px;
  border-radius: 4px;
}

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

/* 确认删除弹窗样式 */
:deep(.el-popover.el-popper) {
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

:deep(.el-popconfirm__main) {
  padding: 16px;
}

:deep(.el-popconfirm__main .el-icon) {
  color: #f56c6c;
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