<template>
  <div class="ai-code-generator">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2 class="page-title">
          <el-icon><MagicStick /></el-icon>
          AI智能代码生成
        </h2>
        <p class="page-subtitle">描述您的需求，AI为您生成专业的RPA机器人代码</p>
      </div>
      <div class="header-actions">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回机器人管理
        </el-button>
      </div>
    </div>

    <div class="main-container">
      <!-- 左侧：配置面板 -->
      <div class="config-panel">
        <!-- 机器人分类选择 -->
        <div class="panel-card category-card">
          <div class="card-header">
            <h3>
              <el-icon><Monitor /></el-icon>
              机器人分类
            </h3>
          </div>
          <div class="category-options">
            <div
              v-for="cat in categoryOptions"
              :key="cat.code"
              :class="['category-item', { active: configForm.robotCategory === cat.code }]"
              @click="configForm.robotCategory = cat.code"
            >
              <el-icon><component :is="cat.icon" /></el-icon>
              <span class="cat-name">{{ cat.name }}</span>
              <span class="cat-desc">{{ cat.description }}</span>
            </div>
          </div>
        </div>

        <!-- 场景配置 -->
        <div class="panel-card">
          <div class="card-header">
            <h3>
              <el-icon><Setting /></el-icon>
              {{ getCategoryName(configForm.robotCategory) }}配置
            </h3>
          </div>
          <el-form :model="configForm" label-width="90px" class="config-form">
            <!-- 采集场景 -->
            <template v-if="configForm.robotCategory === 'DATA_COLLECT'">
              <el-form-item label="采集场景" required>
                <el-select v-model="configForm.scene" placeholder="选择采集场景" style="width: 100%" @change="onSceneChange">
                  <el-option label="发票数据采集" value="invoice" />
                  <el-option label="订单数据采集" value="order" />
                  <el-option label="企业信息采集" value="company" />
                  <el-option label="商品信息采集" value="product" />
                  <el-option label="通用数据采集" value="general" />
                </el-select>
              </el-form-item>
              <el-form-item label="目标URL" required>
                <el-input v-model="configForm.url" placeholder="请输入目标网页URL" clearable />
              </el-form-item>
              <el-form-item label="表格选择器">
                <el-input v-model="configForm.tableSelector" placeholder="CSS选择器，如: #invoice-table tbody tr" clearable />
              </el-form-item>
            </template>

            <!-- 解析场景 -->
            <template v-else-if="configForm.robotCategory === 'DATA_PARSE'">
              <el-form-item label="解析场景" required>
                <el-select v-model="configForm.scene" placeholder="选择解析场景" style="width: 100%">
                  <el-option label="HTML表格解析" value="parse-html" />
                  <el-option label="JSON数据解析" value="parse-json" />
                  <el-option label="XML数据解析" value="parse-xml" />
                  <el-option label="CSV数据解析" value="parse-csv" />
                </el-select>
              </el-form-item>
              <el-form-item label="数据来源">
                <el-input v-model="configForm.url" placeholder="输入URL或文件路径" clearable />
              </el-form-item>
              <el-form-item label="解析规则">
                <el-input v-model="configForm.columns" type="textarea" :rows="3" placeholder="输入解析规则，如: $.data.items[*]" />
              </el-form-item>
            </template>

            <!-- 加工场景 -->
            <template v-else-if="configForm.robotCategory === 'DATA_PROCESS'">
              <el-form-item label="加工场景" required>
                <el-select v-model="configForm.scene" placeholder="选择加工场景" style="width: 100%">
                  <el-option label="数据清洗" value="clean" />
                  <el-option label="格式转换" value="transform" />
                  <el-option label="数据校验" value="validate" />
                  <el-option label="数据去重" value="dedup" />
                </el-select>
              </el-form-item>
              <el-form-item label="处理选项">
                <el-checkbox-group v-model="configForm.processSteps">
                  <el-checkbox label="clean">数据清洗</el-checkbox>
                  <el-checkbox label="dedup">去重处理</el-checkbox>
                  <el-checkbox label="validate">数据校验</el-checkbox>
                  <el-checkbox label="transform">格式转换</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item v-if="configForm.processSteps.includes('validate')" label="校验规则">
                <el-input v-model="configForm.validateRules" placeholder="如: mobile=手机号,email=邮箱" />
              </el-form-item>
              <el-form-item v-if="configForm.processSteps.includes('transform')" label="转换规则">
                <el-input v-model="configForm.transformRules" placeholder="如: date=yyyy-MM-dd,amount=2位小数" />
              </el-form-item>
            </template>

            <!-- 通用场景 -->
            <template v-else>
              <el-form-item label="执行场景" required>
                <el-select v-model="configForm.scene" placeholder="选择执行场景" style="width: 100%">
                  <el-option label="通用数据采集" value="general" />
                </el-select>
              </el-form-item>
              <el-form-item label="目标URL">
                <el-input v-model="configForm.url" placeholder="请输入目标网页URL" clearable />
              </el-form-item>
            </template>

            <!-- 落库配置（仅采集场景） -->
            <template v-if="configForm.robotCategory === 'DATA_COLLECT'">
              <el-divider content-position="left">
                <el-icon><FolderOpened /></el-icon>
                落库配置
              </el-divider>
              <el-form-item label="目标数据表" required>
                <el-select v-model="configForm.targetTable" placeholder="选择落库目标表" style="width: 100%">
                  <el-option label="发票数据表 (invoice_data)" value="invoice_data" />
                  <el-option label="订单数据表 (order_data)" value="order_data" />
                  <el-option label="客户数据表 (customer_data)" value="customer_data" />
                  <el-option label="商品数据表 (product_data)" value="product_data" />
                  <el-option label="财务报表表 (report_data)" value="report_data" />
                  <el-option label="自定义表" value="custom" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="configForm.targetTable === 'custom'" label="自定义表名">
                <el-input v-model="configForm.customTable" placeholder="请输入自定义表名" />
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="configForm.autoDetectColumns">自动检测表格列名</el-checkbox>
              </el-form-item>
            </template>
          </el-form>
        </div>

        <!-- 自然语言描述 -->
        <div class="panel-card prompt-card">
          <div class="card-header">
            <h3>
              <el-icon><ChatLineRound /></el-icon>
              需求描述
            </h3>
          </div>
          <div class="prompt-section">
            <el-input
              v-model="userPrompt"
              type="textarea"
              :rows="4"
              placeholder="请描述您的采集需求，例如：采集某网站发票信息，包含发票号码、金额、日期等字段..."
              class="prompt-input"
            />
            <div class="prompt-actions">
              <el-button
                @click="optimizePrompt"
                :loading="optimizingPrompt"
                :disabled="!userPrompt.trim()"
                size="default"
              >
                <el-icon><MagicStick /></el-icon>
                AI优化提示词
              </el-button>
            </div>

            <!-- AI优化后的提示词显示 -->
            <div v-if="optimizedPrompt" class="optimized-prompt-section">
              <div class="optimized-header">
                <el-icon><Clock /></el-icon>
                <span>AI优化后的需求描述</span>
                <el-button text size="small" @click="useOptimizedPrompt">
                  <el-icon><Edit /></el-icon>
                  使用此描述
                </el-button>
              </div>
              <div class="optimized-content">
                {{ optimizedPrompt }}
              </div>
            </div>
          </div>

          <!-- 生成代码按钮 -->
          <div class="generate-section">
            <el-button
              type="primary"
              size="large"
              :loading="generatingCode"
              :disabled="!canGenerate"
              @click="generateCode"
              class="generate-btn"
            >
              <el-icon><MagicStick /></el-icon>
              根据需求描述生成代码
            </el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：预览与生成 -->
      <div class="preview-panel">
        <div class="panel-card code-card">
          <div class="card-header">
            <h3>
              <el-icon><Document /></el-icon>
              生成的代码
            </h3>
            <div class="card-actions">
              <el-button size="small" @click="copyCode" :disabled="!generatedCode">
                <el-icon><DocumentCopy /></el-icon>
                复制
              </el-button>
              <el-button size="small" @click="previewCode" :disabled="!generatedCode">
                <el-icon><View /></el-icon>
                预览
              </el-button>
            </div>
          </div>

          <div class="code-content">
            <div v-if="!generatedCode" class="code-placeholder">
              <el-icon><Document /></el-icon>
              <p>配置完成后点击"根据需求描述生成代码"</p>
              <p class="hint">生成的代码将显示在这里</p>
            </div>
            <pre v-else class="code-block"><code>{{ generatedCode }}</code></pre>
          </div>
        </div>

        <!-- 代码模板快捷选项 -->
        <div class="panel-card help-card">
          <div class="card-header">
            <h3>
              <el-icon><InfoFilled /></el-icon>
              命令说明
            </h3>
          </div>
          <div class="command-list">
            <div class="command-item">
              <code>@collect URL</code>
              <span>采集网页内容</span>
            </div>
            <div class="command-item">
              <code>@parse</code>
              <span>解析HTML/JSON/XML</span>
            </div>
            <div class="command-item">
              <code>@process clean,dedup</code>
              <span>数据处理步骤</span>
            </div>
            <div class="command-item">
              <code>@store table_name</code>
              <span>存储到数据库</span>
            </div>
            <div class="command-item">
              <code>@log message</code>
              <span>日志输出</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 代码预览对话框 -->
    <el-dialog v-model="previewVisible" title="代码预览" width="800px">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="完整代码" name="code">
          <pre class="preview-code">{{ generatedCode }}</pre>
        </el-tab-pane>
        <el-tab-pane label="命令说明" name="help">
          <div class="command-help">
            <h4>支持的命令：</h4>
            <ul>
              <li><code>@collect URL</code> - ���集网页内容</li>
              <li><code>@parse</code> - 解析HTML/JSON/XML</li>
              <li><code>@process clean,dedup</code> - 数据处理步骤</li>
              <li><code>@store table_name</code> - 存储到数据库</li>
              <li><code>@log message</code> - 日志输出</li>
            </ul>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" @click="copyCode">复制代码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick, Setting, Document, ChatLineRound,
  Clock, Edit, DocumentCopy, View, Monitor,
  Download, Cpu, Connection, FolderOpened, ArrowLeft,
  InfoFilled, TrendCharts
} from '@element-plus/icons-vue'
import { apiPost } from '../../utils/api.js'

const router = useRouter()
const route = useRoute()

// 机器人分类选项
const categoryOptions = [
  { code: 'DATA_COLLECT', name: '数据采集', icon: 'Download', description: '采集网页数据' },
  { code: 'DATA_PARSE', name: '数据解析', icon: 'Document', description: '解析HTML/JSON/XML' },
  { code: 'DATA_PROCESS', name: '数据加工', icon: 'Cpu', description: '数据清洗转换' },
  { code: 'GENERAL', name: '通用执行', icon: 'Connection', description: '通用任务执行' }
]

// 分类到场景的映射
const categoryToScenes = {
  DATA_COLLECT: ['invoice', 'order', 'company', 'product', 'general'],
  DATA_PARSE: ['parse-html', 'parse-json', 'parse-xml', 'parse-csv'],
  DATA_PROCESS: ['clean', 'transform', 'validate', 'dedup'],
  GENERAL: ['general']
}

// 配置表单
const configForm = reactive({
  url: '',
  scene: '',
  tableSelector: '',
  columns: '',
  targetTable: 'invoice_data',
  customTable: '',
  autoDetectColumns: true,
  processSteps: [],
  validateRules: '',
  transformRules: '',
  robotCategory: 'DATA_COLLECT',
  robotName: '',
  description: ''
})

// 自然语言提示词
const userPrompt = ref('')
const optimizedPrompt = ref('')
const hasOptimizedPrompt = ref(false)

// 状态
const optimizingPrompt = ref(false)
const generating = ref(false)
const generatingCode = ref(false)
const previewVisible = ref(false)
const previewDialogVisible = ref(false)
const activeTab = ref('code')
const selectedTemplate = ref('')

// 生成的代码
const generatedCode = ref('')

// 计算属性：是否可以生成
const canGenerate = computed(() => {
  return configForm.scene
})

// 获取分类名称
const getCategoryName = (code) => {
  const cat = categoryOptions.find(c => c.code === code)
  return cat ? cat.name : code
}

// 分类切换
const onCategoryChange = (category) => {
  const defaultScenes = {
    DATA_COLLECT: 'invoice',
    DATA_PARSE: 'parse-html',
    DATA_PROCESS: 'clean',
    GENERAL: 'general'
  }
  configForm.scene = defaultScenes[category] || ''
  userPrompt.value = ''
  optimizedPrompt.value = ''
  generatedCode.value = ''
}

// 场景切换
const onSceneChange = (scene) => {
  const defaults = {
    invoice: {
      tableSelector: '#invoice-table tbody tr',
      columns: '#,号码,类型,状态,日期,不含税,税额,价税合计',
      targetTable: 'invoice_data'
    },
    order: {
      tableSelector: '.order-list tbody tr',
      columns: '订单号,商品名称,数量,单价,总价,下单时间',
      targetTable: 'order_data'
    },
    company: {
      tableSelector: '',
      columns: '',
      targetTable: 'customer_data'
    },
    product: {
      tableSelector: '.product-list tbody tr',
      columns: '商品编码,商品名称,规格,单价,库存',
      targetTable: 'product_data'
    },
    general: {
      tableSelector: 'table tbody tr',
      columns: '',
      targetTable: 'report_data'
    }
  }

  const def = defaults[scene]
  if (def) {
    if (!configForm.tableSelector) configForm.tableSelector = def.tableSelector
    if (!configForm.columns) configForm.columns = def.columns
    configForm.targetTable = def.targetTable
  }
}

// AI优化提示词
const optimizePrompt = async () => {
  if (!userPrompt.value.trim()) {
    ElMessage.warning('请输入需求描述')
    return
  }

  optimizingPrompt.value = true

  try {
    const response = await apiPost('/ai/optimize-prompt', {
      prompt: userPrompt.value,
      category: configForm.robotCategory
    })

    optimizedPrompt.value = response.data.optimizedPrompt
    hasOptimizedPrompt.value = true
    ElMessage.success('提示词优化完成')
  } catch (error) {
    ElMessage.error('AI优化失败：' + (error.message || '未知错误'))
  } finally {
    optimizingPrompt.value = false
  }
}

// 采用优化后的提示词
const acceptPrompt = () => {
  userPrompt.value = optimizedPrompt.value
  hasOptimizedPrompt.value = false
  ElMessage.success('已采用优化提示词')
}

// 生成代码
const generateCode = async () => {
  if (!canGenerate.value) {
    ElMessage.warning('请选择场景')
    return
  }

  generatingCode.value = true

  try {
    const table = configForm.targetTable === 'custom' ? configForm.customTable : configForm.targetTable
    const promptText = optimizedPrompt.value || userPrompt.value

    const response = await apiPost('/ai/generate-robot-code', {
      url: configForm.url,
      scene: configForm.scene,
      tableSelector: configForm.tableSelector,
      columns: configForm.columns,
      targetTable: table,
      processSteps: configForm.processSteps,
      validateRules: configForm.validateRules,
      transformRules: configForm.transformRules,
      prompt: promptText,
      robotCategory: configForm.robotCategory,
      robotName: configForm.robotName,
      description: configForm.description
    })

    if (response.code === 0 && response.data) {
      generatedCode.value = response.data.code || response.data
      ElMessage.success('代码生成成功！')
    } else {
      throw new Error(response.message || '生成失败')
    }
  } catch (error) {
    generatedCode.value = generateCodeLocally()
    ElMessage.warning('使用本地模板生成')
  } finally {
    generatingCode.value = false
  }
}

// 本地代码生成（备用方案）
const generateCodeLocally = () => {
  const { robotCategory, scene, url, tableSelector, columns, targetTable, processSteps, validateRules, transformRules, robotName } = configForm

  let code = `// ${robotName || 'AI生成的RPA机器人'}
// 分类: ${getCategoryName(robotCategory)}
// 场景: ${scene}

`

  switch (robotCategory) {
    case 'DATA_COLLECT':
      if (url) code += `@collect ${url}\n`
      if (tableSelector) code += `@table_selector ${tableSelector}\n`
      if (columns) code += `@columns ${columns}\n`
      break
    case 'DATA_PARSE':
      code += `@parse\n`
      if (columns) code += `@parse_rule ${columns}\n`
      break
    case 'DATA_PROCESS':
      if (processSteps.length > 0) {
        code += `@process ${processSteps.join(',')}\n`
      }
      if (validateRules) code += `@validate ${validateRules}\n`
      if (transformRules) code += `@transform ${transformRules}\n`
      break
    case 'GENERAL':
      if (url) code += `@collect ${url}\n`
      break
  }

  if (targetTable && targetTable !== 'custom' && robotCategory === 'DATA_COLLECT') {
    code += `@store ${targetTable}\n`
  }

  code += `\n@log ${getCategoryName(robotCategory)}任务执行完成`

  return code
}

// 复制代码
const copyCode = () => {
  if (!generatedCode.value) {
    ElMessage.warning('暂无代码可复制')
    return
  }
  navigator.clipboard.writeText(generatedCode.value)
    .then(() => ElMessage.success('已复制到剪贴板'))
    .catch(() => ElMessage.error('复制失败'))
}

// 预览代码
const previewCode = () => {
  previewDialogVisible.value = true
}

// 预览代码（兼容旧代码）
const previewCodeCompat = () => {
  previewVisible.value = true
}

// 应用代码到机器人
const applyCode = () => {
  if (!generatedCode.value) {
    ElMessage.warning('暂无代码可应用')
    return
  }
  localStorage.setItem('aiGeneratedCode', generatedCode.value)
  localStorage.setItem('aiRobotCategory', configForm.robotCategory)
  router.push('/rpa/robots?action=create')
}

// 使用优化后的提示词
const useOptimizedPrompt = () => {
  userPrompt.value = optimizedPrompt.value
  ElMessage.success('已使用优化后的描述')
}

// 跳转到注册机器人
const goToCreate = () => {
  // 保存生成的代码到localStorage或通过路由参数传递
  localStorage.setItem('aiGeneratedCode', generatedCode.value)
  localStorage.setItem('aiRobotCategory', configForm.robotCategory)
  localStorage.setItem('aiRobotName', configForm.robotName)
  localStorage.setItem('aiDescription', configForm.description)
  router.push('/rpa/robots?action=create')
}

// 返回机器人管理
const goBack = () => {
  router.back()
}

// 模板选项
const templateOptions = [
  { code: 'collect', name: '数据采集', description: '网页数据采集模板' },
  { code: 'parse', name: '数据解析', description: 'HTML/JSON解析模板' },
  { code: 'process', name: '数据加工', description: '数据清洗转换模板' },
  { code: 'store', name: '数据落库', description: '数据库存储模板' }
]

onMounted(() => {
  // 检查是否有从机器人注册页面跳转过来的参数
  const params = route.query
  if (params.category) {
    configForm.robotCategory = params.category
    onCategoryChange(params.category)
  }
  if (params.url) {
    configForm.url = params.url
  }
  if (params.table) {
    configForm.targetTable = params.table
  }

  // 初始化默认
  if (!configForm.scene) {
    onCategoryChange('DATA_COLLECT')
  }
})
</script>

<style scoped>
.ai-code-generator {
  padding: 24px;
  background: var(--bg-primary, #f5f7fa);
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
}

.header-content {
  color: white;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px;
}

.page-subtitle {
  font-size: 15px;
  opacity: 0.9;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.main-container {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 24px;
}

.config-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.category-card {
  padding: 20px;
}

.category-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 16px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 12px;
  border: 2px solid var(--border-color, #eee);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.category-item:hover {
  border-color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.category-item.active {
  border-color: #667eea;
  background: linear-gradient(135deg, #f0f4ff 0%, #f5f0ff 100%);
}

.category-item .el-icon {
  font-size: 32px;
  color: #667eea;
  margin-bottom: 8px;
}

.cat-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
}

.cat-desc {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color, #eee);
}

.card-header h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #333);
  margin: 0;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.config-form {
  margin-top: 16px;
}

.prompt-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.prompt-input {
  font-size: 14px;
}

.prompt-actions {
  display: flex;
  justify-content: flex-end;
}

.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.code-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.code-editor {
  flex: 1;
  background: #1e1e1e;
  border-radius: 8px;
  padding: 16px;
  margin: 16px 0;
  min-height: 400px;
  overflow: auto;
}

.code-content {
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.code-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--border-color, #eee);
  flex-wrap: wrap;
  gap: 12px;
}

.code-info {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.prompt-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf3 100%);
}

/* 优化提示词区域 */
.optimized-prompt-section {
  margin-top: 16px;
  border-radius: 8px;
  overflow: hidden;
}

.optimized-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 14px;
  font-weight: 500;
}

.optimized-header .el-button {
  margin-left: auto;
  color: white;
}

.optimized-content {
  padding: 14px 16px;
  background: white;
  border-radius: 0 0 8px 8px;
  border: 1px solid #dcdfe6;
  border-top: none;
  line-height: 1.8;
  color: var(--text-primary, #333);
  font-size: 14px;
}

/* 生成代码按钮区域 */
.generate-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px dashed #dcdfe6;
}

.generate-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.generate-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.generate-btn:disabled {
  background: #dcdfe6;
  box-shadow: none;
}

/* 代码占位符 */
.code-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
  text-align: center;
}

.code-placeholder .el-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.code-placeholder p {
  margin: 0 0 8px;
  font-size: 15px;
}

.code-placeholder .hint {
  font-size: 13px;
  color: #c0c4cc;
}

/* 代码模板选项 */
.template-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.template-item {
  padding: 14px;
  background: var(--bg-primary, #f5f7fa);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.template-item:hover {
  border-color: #667eea;
  background: #f0f0ff;
}

.template-item.active {
  border-color: #667eea;
  background: linear-gradient(135deg, #f0f0ff 0%, #e8e8ff 100%);
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.template-item .tpl-name {
  display: block;
  font-weight: 600;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
}

.template-item .tpl-desc {
  display: block;
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.optimized-prompt {
  padding: 16px;
  background: white;
  border-radius: 8px;
  border-left: 4px solid #667eea;
  line-height: 1.8;
  color: var(--text-primary, #333);
  font-size: 14px;
}

.help-card {
  background: #f8f9fa;
}

.command-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  padding: 8px 0;
}

.command-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.command-item code {
  background: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #e83e8c;
  border: 1px solid var(--border-color, #eee);
  min-width: 120px;
  text-align: center;
}

.command-item span {
  font-size: 13px;
  color: var(--text-secondary, #666);
}

.preview-code {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  max-height: 500px;
  overflow: auto;
  white-space: pre-wrap;
}

.command-help h4 {
  margin: 0 0 12px;
  color: var(--text-primary, #333);
}

.command-help ul {
  margin: 0;
  padding-left: 20px;
}

.command-help li {
  margin-bottom: 8px;
  color: var(--text-secondary, #666);
}

.command-help code {
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: monospace;
  color: #e83e8c;
}

@media (max-width: 1200px) {
  .main-container {
    grid-template-columns: 1fr;
  }

  .category-options {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
