<template>
  <div class="ai-assistant-container">
    <el-card class="assistant-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><MagicStick /></el-icon> AI 流程助手</span>
          <el-tag type="success">DeepSeek</el-tag>
        </div>
      </template>

      <!-- 功能切换 -->
      <el-tabs v-model="activeTab" class="function-tabs">
        <el-tab-pane label="智能生成" name="generate">
          <div class="generate-panel">
            <!-- 模板选择 -->
            <div class="template-section">
              <div class="section-title">快速模板</div>
              <el-row :gutter="10" class="template-grid">
                <el-col :span="6" v-for="tpl in templates" :key="tpl.id">
                  <div class="template-card" @click="selectTemplate(tpl)">
                    <el-icon size="24"><component :is="getIconComponent(tpl.icon)" /></el-icon>
                    <div class="template-name">{{ tpl.name }}</div>
                  </div>
                </el-col>
              </el-row>
            </div>

            <!-- 输入区域 -->
            <div class="input-section">
              <el-input
                v-model="description"
                type="textarea"
                :rows="4"
                placeholder="描述你要自动化的业务流程...&#10;例如：帮我自动抓取淘宝店铺的月销售额，保存到Excel"
                resize="none"
              />
              <div class="input-actions">
                <el-button type="primary" @click="generateFlow" :loading="generating">
                  <el-icon><Lightning /></el-icon>
                  一键生成
                </el-button>
                <el-button @click="clearInput">
                  <el-icon><Delete /></el-icon>
                  清空
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="流程优化" name="optimize">
          <div class="optimize-panel">
            <el-input
              v-model="optimizeGoal"
              type="textarea"
              :rows="3"
              placeholder="描述优化目标...&#10;例如：提高执行效率、添加错误处理、优化资源使用"
            />
            <div class="optimize-hint">
              <el-icon><InfoFilled /></el-icon>
              优化当前流程，请先在设计器中打开要优化的流程
            </div>
            <el-button type="warning" @click="optimizeFlow" :loading="optimizing">
              <el-icon><Setting /></el-icon>
              开始优化
            </el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="智能建议" name="suggest">
          <div class="suggest-panel">
            <el-alert
              title="智能分析"
              type="info"
              description="分析当前流程配置，提供改进建议和最佳实践"
              :closable="false"
              show-icon
            />
            <el-button type="success" @click="getSuggestions" :loading="analyzing" style="margin-top: 15px;">
              <el-icon><ChatDotRound /></el-icon>
              分析流程
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 生成结果 -->
      <div class="result-section" v-if="generatedFlow">
        <el-divider content-position="left">
          <el-icon><Document /></el-icon>
          生成结果
        </el-divider>

        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="result-header">
              <span>{{ generatedFlow.name }}</span>
              <el-tag type="success">已生成</el-tag>
            </div>
          </template>

          <!-- 流程预览 -->
          <div class="flow-preview">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="意图">{{ generatedFlow.intent }}</el-descriptions-item>
              <el-descriptions-item label="步骤数">{{ generatedFlow.nodes?.length || 0 }}</el-descriptions-item>
            </el-descriptions>

            <!-- 步骤列表 -->
            <div class="steps-list">
              <el-timeline>
                <el-timeline-item
                  v-for="(node, index) in generatedFlow.nodes"
                  :key="index"
                  :icon="getNodeIcon(node.type)"
                  :color="getNodeColor(node.type)"
                >
                  <div class="step-item">
                    <div class="step-name">{{ index + 1 }}. {{ node.name }}</div>
                    <div class="step-info">
                      <el-tag size="small">{{ node.type }}</el-tag>
                      <span>{{ node.action }}</span>
                    </div>
                    <div class="step-desc" v-if="node.description">{{ node.description }}</div>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="result-actions">
            <el-button type="primary" @click="openInDesigner">
              <el-icon><Edit /></el-icon>
              在设计器中打开
            </el-button>
            <el-button @click="saveToLibrary">
              <el-icon><FolderAdd /></el-icon>
              保存到流程库
            </el-button>
            <el-button @click="exportFlow">
              <el-icon><Download /></el-icon>
              导出JSON
            </el-button>
          </div>
        </el-card>
      </div>

      <!-- 建议列表 -->
      <div class="suggestions-section" v-if="suggestions.length > 0">
        <el-divider content-position="left">
          <el-icon><ChatLineSquare /></el-icon>
          优化建议
        </el-divider>

        <el-space direction="vertical" fill class="suggestions-list">
          <el-alert
            v-for="(s, i) in suggestions"
            :key="i"
            :title="s"
            type="warning"
            show-icon
            :closable="false"
          />
        </el-space>
      </div>

      <!-- 错误提示 -->
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        closable
        @close="error = null"
        style="margin-top: 15px;"
      />
    </el-card>

    <!-- 导入确认对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入到流程设计器"
      width="500px"
    >
      <p>确定要将生成的流程导入到设计器吗？</p>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  MagicStick,
  Lightning,
  Delete,
  Setting,
  InfoFilled,
  ChatDotRound,
  Document,
  Edit,
  FolderAdd,
  Download,
  ChatLineSquare,
  Monitor,
  DocumentCopy,
  ChatRound,
  Connection,
  DataAnalysis,
  EditPen,
  Box,
  Folder,
  Link
} from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()

const api = axios.create({
  baseURL: '/api/ai/flow',
  timeout: 60000
})

// 状态
const activeTab = ref('generate')
const description = ref('')
const optimizeGoal = ref('')
const templates = ref([])
const generating = ref(false)
const optimizing = ref(false)
const analyzing = ref(false)
const generatedFlow = ref(null)
const suggestions = ref([])
const error = ref(null)
const importDialogVisible = ref(false)

// 加载模板
async function loadTemplates() {
  try {
    const response = await api.get('/templates')
    if (response.data.success) {
      templates.value = response.data.data.templates
    }
  } catch (e) {
    console.error('加载模板失败', e)
  }
}

// 获取图标组件 - 使用已导入的有效图标
function getIconComponent(iconName) {
  const iconMap = {
    'Monitor': Monitor,
    'Document': Document,
    'ChatRound': ChatRound,
    'Connection': Connection,
    'DataAnalysis': DataAnalysis,
    'Edit': EditPen,
    'Box': Box,
    'Folder': Folder,
    'Link': Link
  }
  return iconMap[iconName] || Document
}

// 选择模板
function selectTemplate(tpl) {
  description.value = tpl.description
  activeTab.value = 'generate'
}

// 清空输入
function clearInput() {
  description.value = ''
  error.value = null
}

// 生成流程
async function generateFlow() {
  if (!description.value.trim()) {
    ElMessage.warning('请输入流程描述')
    return
  }

  generating.value = true
  error.value = null

  try {
    const response = await api.post('/generate', {
      description: description.value,
      context: {}
    })

    if (response.data.success) {
      generatedFlow.value = response.data.data.config
      ElMessage.success('流程生成成功！共 ' + (response.data.data.stepCount || 0) + ' 个步骤')
    } else {
      error.value = response.data.message || '生成失败'
    }
  } catch (e) {
    error.value = '生成失败: ' + e.message
    ElMessage.error(error.value)
  } finally {
    generating.value = false
  }
}

// 优化流程
async function optimizeFlow() {
  if (!optimizeGoal.value.trim()) {
    ElMessage.warning('请输入优化目标')
    return
  }

  optimizing.value = true
  error.value = null

  try {
    const response = await api.post('/optimize', {
      flow: { name: 'current', nodes: [] },
      goal: optimizeGoal.value
    })

    if (response.data.success) {
      generatedFlow.value = response.data.data.config
      ElMessage.success('流程优化完成')
    } else {
      error.value = response.data.message || '优化失败'
    }
  } catch (e) {
    error.value = '优化失败: ' + e.message
  } finally {
    optimizing.value = false
  }
}

// 获取建议
async function getSuggestions() {
  analyzing.value = true
  suggestions.value = []

  try {
    const response = await api.post('/suggest', {
      flow: { name: 'current', description: '', nodes: [] }
    })

    if (response.data.success) {
      suggestions.value = response.data.data.suggestions || []
      if (suggestions.value.length === 0) {
        ElMessage.info('未找到改进建议')
      }
    }
  } catch (e) {
    ElMessage.error('分析失败: ' + e.message)
  } finally {
    analyzing.value = false
  }
}

// 在设计器中打开
function openInDesigner() {
  if (generatedFlow.value) {
    sessionStorage.setItem('ai-generated-flow', JSON.stringify(generatedFlow.value))
    router.push('/rpa/process-designer?mode=ai')
  }
}

// 保存到流程库
async function saveToLibrary() {
  if (!generatedFlow.value) return

  try {
    const response = await axios.post('/api/process', {
      name: generatedFlow.value.name,
      description: generatedFlow.value.description,
      config: JSON.stringify(generatedFlow.value)
    })

    if (response.data.success) {
      ElMessage.success('已保存到流程库')
    } else {
      ElMessage.error('保存失败: ' + response.data.message)
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

// 导出JSON
function exportFlow() {
  if (!generatedFlow.value) return

  const blob = new Blob([JSON.stringify(generatedFlow.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `flow_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success('已导出JSON文件')
}

// 确认导入
function confirmImport() {
  importDialogVisible.value = false
  openInDesigner()
}

// 获取节点图标
function getNodeIcon(type) {
  return EditPen
}

// 获取节点颜色
function getNodeColor(type) {
  const colorMap = {
    'browser': '#409eff',
    'database': '#67c23a',
    'file': '#e6a23c',
    'excel': '#909399',
    'email': '#f56c6c',
    'condition': '#9c27b0',
    'loop': '#00bcd4',
    'script': '#795548'
  }
  return colorMap[type] || '#409eff'
}

// 初始化
onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.ai-assistant-container {
  padding: 20px;
}

.assistant-card {
  max-width: 900px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
}

.function-tabs {
  margin-bottom: 20px;
}

/* 生成面板 */
.generate-panel {
  padding: 10px 0;
}

.template-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 10px;
}

.template-grid {
  margin-bottom: 10px;
}

.template-card {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 15px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.template-card:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.template-card .el-icon {
  color: #409eff;
  margin-bottom: 8px;
}

.template-name {
  font-size: 12px;
  color: #606266;
}

.input-section {
  margin-top: 15px;
}

.input-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

/* 优化面板 */
.optimize-panel {
  padding: 15px 0;
}

.optimize-hint {
  margin: 10px 0;
  padding: 10px;
  background: #fdf6ec;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 12px;
}

/* 建议面板 */
.suggest-panel {
  padding: 15px 0;
}

/* 结果区域 */
.result-section {
  margin-top: 20px;
}

.result-card {
  margin-bottom: 15px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.flow-preview {
  margin-bottom: 15px;
}

.steps-list {
  margin-top: 15px;
  max-height: 400px;
  overflow-y: auto;
}

.step-item {
  padding: 5px 0;
}

.step-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.step-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.step-desc {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 3px;
}

.result-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

/* 建议列表 */
.suggestions-section {
  margin-top: 20px;
}

.suggestions-list {
  width: 100%;
}

.suggestions-list .el-alert {
  margin-bottom: 10px;
}
</style>