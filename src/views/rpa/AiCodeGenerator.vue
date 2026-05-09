<template>
  <div class="ai-code-generator">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2 class="page-title">
          <el-icon><MagicStick /></el-icon>
          {{ t('aiCode.title') }}
        </h2>
        <p class="page-subtitle">{{ t('aiCode.subtitle') }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          {{ t('aiCode.backToRobotManage') }}
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
              {{ t('aiCode.robotCategory') }}
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
              <span class="cat-name">{{ t(cat.nameKey) }}</span>
              <span class="cat-desc">{{ t(cat.descKey) }}</span>
            </div>
          </div>
        </div>

        <!-- 场景配置 -->
        <div class="panel-card">
          <div class="card-header">
            <h3>
              <el-icon><Setting /></el-icon>
              {{ getCategoryName(configForm.robotCategory) }}{{ t('aiCode.config') }}
            </h3>
          </div>
          <el-form :model="configForm" label-width="90px" class="config-form">
            <!-- 采集场景 -->
            <template v-if="configForm.robotCategory === 'DATA_COLLECT'">
              <el-form-item :label="t('aiCode.collectScene')" required>
                <el-select v-model="configForm.scene" :placeholder="t('aiCode.selectCollectScene')" style="width: 100%" @change="onSceneChange">
                  <el-option :label="t('aiCode.invoiceCollect')" value="invoice" />
                  <el-option :label="t('aiCode.orderCollect')" value="order" />
                  <el-option :label="t('aiCode.companyCollect')" value="company" />
                  <el-option :label="t('aiCode.productCollect')" value="product" />
                  <el-option :label="t('aiCode.generalCollect')" value="general" />
                </el-select>
              </el-form-item>
              <el-form-item :label="t('aiCode.targetUrl')" required>
                <el-input v-model="configForm.url" :placeholder="t('aiCode.enterTargetUrl')" clearable />
              </el-form-item>
              <el-form-item :label="t('aiCode.tableSelector')">
                <el-input v-model="configForm.tableSelector" :placeholder="t('aiCode.tableSelectorPlaceholder')" clearable />
              </el-form-item>
            </template>

            <!-- 解析场景 -->
            <template v-else-if="configForm.robotCategory === 'DATA_PARSE'">
              <el-form-item :label="t('aiCode.parseScene')" required>
                <el-select v-model="configForm.scene" :placeholder="t('aiCode.selectParseScene')" style="width: 100%">
                  <el-option :label="t('aiCode.htmlParse')" value="parse-html" />
                  <el-option :label="t('aiCode.jsonParse')" value="parse-json" />
                  <el-option :label="t('aiCode.xmlParse')" value="parse-xml" />
                  <el-option :label="t('aiCode.csvParse')" value="parse-csv" />
                </el-select>
              </el-form-item>
              <el-form-item :label="t('aiCode.dataSource')">
                <el-input v-model="configForm.url" :placeholder="t('aiCode.inputUrlOrPath')" clearable />
              </el-form-item>
              <el-form-item :label="t('aiCode.parseRule')">
                <el-input v-model="configForm.columns" type="textarea" :rows="3" :placeholder="t('aiCode.parseRulePlaceholder')" />
              </el-form-item>
            </template>

            <!-- 加工场景 -->
            <template v-else-if="configForm.robotCategory === 'DATA_PROCESS'">
              <el-form-item :label="t('aiCode.processScene')" required>
                <el-select v-model="configForm.scene" :placeholder="t('aiCode.selectProcessScene')" style="width: 100%">
                  <el-option :label="t('aiCode.dataClean')" value="clean" />
                  <el-option :label="t('aiCode.formatTransform')" value="transform" />
                  <el-option :label="t('aiCode.dataValidate')" value="validate" />
                  <el-option :label="t('aiCode.dataDedup')" value="dedup" />
                </el-select>
              </el-form-item>
              <el-form-item :label="t('aiCode.processOptions')">
                <el-checkbox-group v-model="configForm.processSteps">
                  <el-checkbox label="clean">{{ t('aiCode.dataClean') }}</el-checkbox>
                  <el-checkbox label="dedup">{{ t('aiCode.dedupProcess') }}</el-checkbox>
                  <el-checkbox label="validate">{{ t('aiCode.dataValidate') }}</el-checkbox>
                  <el-checkbox label="transform">{{ t('aiCode.formatTransform') }}</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item v-if="configForm.processSteps.includes('validate')" :label="t('aiCode.validateRule')">
                <el-input v-model="configForm.validateRules" :placeholder="t('aiCode.validateRulePlaceholder')" />
              </el-form-item>
              <el-form-item v-if="configForm.processSteps.includes('transform')" :label="t('aiCode.transformRule')">
                <el-input v-model="configForm.transformRules" :placeholder="t('aiCode.transformRulePlaceholder')" />
              </el-form-item>
            </template>

            <!-- 通用场景 -->
            <template v-else>
              <el-form-item :label="t('aiCode.executeScene')" required>
                <el-select v-model="configForm.scene" :placeholder="t('aiCode.selectExecuteScene')" style="width: 100%">
                  <el-option :label="t('aiCode.generalCollect')" value="general" />
                </el-select>
              </el-form-item>
              <el-form-item :label="t('aiCode.targetUrl')">
                <el-input v-model="configForm.url" :placeholder="t('aiCode.enterTargetUrl')" clearable />
              </el-form-item>
            </template>

            <!-- 落库配置（仅采集场景） -->
            <template v-if="configForm.robotCategory === 'DATA_COLLECT'">
              <el-divider content-position="left">
                <el-icon><FolderOpened /></el-icon>
                {{ t('aiCode.storageConfig') }}
              </el-divider>
              <el-form-item :label="t('aiCode.targetTable')" required>
                <el-select v-model="configForm.targetTable" :placeholder="t('aiCode.selectTargetTable')" style="width: 100%">
                  <el-option :label="t('aiCode.invoiceTable')" value="invoice_data" />
                  <el-option :label="t('aiCode.orderTable')" value="order_data" />
                  <el-option :label="t('aiCode.customerTable')" value="customer_data" />
                  <el-option :label="t('aiCode.productTable')" value="product_data" />
                  <el-option :label="t('aiCode.reportTable')" value="report_data" />
                  <el-option :label="t('aiCode.customTable')" value="custom" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="configForm.targetTable === 'custom'" :label="t('aiCode.customTableName')">
                <el-input v-model="configForm.customTable" :placeholder="t('aiCode.enterCustomTableName')" />
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="configForm.autoDetectColumns">{{ t('aiCode.autoDetectColumns') }}</el-checkbox>
              </el-form-item>
            </template>
          </el-form>
        </div>

        <!-- 自然语言描述 -->
        <div class="panel-card prompt-card">
          <div class="card-header">
            <h3>
              <el-icon><ChatLineRound /></el-icon>
              {{ t('aiCode.requirementDesc') }}
            </h3>
          </div>
          <div class="prompt-section">
            <el-input
              v-model="userPrompt"
              type="textarea"
              :rows="4"
              :placeholder="t('aiCode.promptPlaceholder')"
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
                {{ t('aiCode.optimizePrompt') }}
              </el-button>
            </div>

            <!-- AI优化后的提示词显示 -->
            <div v-if="optimizedPrompt" class="optimized-prompt-section">
              <div class="optimized-header">
                <el-icon><Clock /></el-icon>
                <span>{{ t('aiCode.optimizedPrompt') }}</span>
                <el-button text size="small" @click="useOptimizedPrompt">
                  <el-icon><Edit /></el-icon>
                  {{ t('aiCode.useThisDesc') }}
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
              {{ t('aiCode.generateCodeFromDesc') }}
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
              {{ t('aiCode.generatedCode') }}
            </h3>
            <div class="card-actions">
              <el-button size="small" @click="copyCode" :disabled="!generatedCode">
                <el-icon><DocumentCopy /></el-icon>
                {{ t('common.copy') }}
              </el-button>
              <el-button size="small" @click="previewCode" :disabled="!generatedCode">
                <el-icon><View /></el-icon>
                {{ t('common.preview') }}
              </el-button>
            </div>
          </div>

          <div class="code-content">
            <div v-if="!generatedCode" class="code-placeholder">
              <el-icon><Document /></el-icon>
              <p>{{ t('aiCode.configThenGenerate') }}</p>
              <p class="hint">{{ t('aiCode.generatedCodeHere') }}</p>
            </div>
            <pre v-else class="code-block"><code>{{ generatedCode }}</code></pre>
          </div>
        </div>

        <!-- 代码模板快捷选项 -->
        <div class="panel-card help-card">
          <div class="card-header">
            <h3>
              <el-icon><InfoFilled /></el-icon>
              {{ t('aiCode.commandHelp') }}
            </h3>
          </div>
          <div class="command-list">
            <div class="command-item">
              <code>@collect URL</code>
              <span>{{ t('aiCode.cmdCollectDesc') }}</span>
            </div>
            <div class="command-item">
              <code>@parse</code>
              <span>{{ t('aiCode.cmdParseDesc') }}</span>
            </div>
            <div class="command-item">
              <code>@process clean,dedup</code>
              <span>{{ t('aiCode.cmdProcessDesc') }}</span>
            </div>
            <div class="command-item">
              <code>@store table_name</code>
              <span>{{ t('aiCode.cmdStoreDesc') }}</span>
            </div>
            <div class="command-item">
              <code>@log message</code>
              <span>{{ t('aiCode.cmdLogDesc') }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 代码预览对话框 -->
    <el-dialog v-model="previewVisible" :title="t('aiCode.codePreview')" width="800px">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('aiCode.fullCode')" name="code">
          <pre class="preview-code">{{ generatedCode }}</pre>
        </el-tab-pane>
        <el-tab-pane :label="t('aiCode.commandHelp')" name="help">
          <div class="command-help">
            <h4>{{ t('aiCode.supportedCommands') }}</h4>
            <ul>
              <li><code>@collect URL</code> - {{ t('aiCode.cmdCollectDesc') }}</li>
              <li><code>@parse</code> - {{ t('aiCode.cmdParseDesc') }}</li>
              <li><code>@process clean,dedup</code> - {{ t('aiCode.cmdProcessDesc') }}</li>
              <li><code>@store table_name</code> - {{ t('aiCode.cmdStoreDesc') }}</li>
              <li><code>@log message</code> - {{ t('aiCode.cmdLogDesc') }}</li>
            </ul>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="previewVisible = false">{{ t('common.close') }}</el-button>
        <el-button type="primary" @click="copyCode">{{ t('aiCode.copyCode') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick, Setting, Document, ChatLineRound,
  Clock, Edit, DocumentCopy, View, Monitor,
  Download, Cpu, Connection, FolderOpened, ArrowLeft,
  InfoFilled, TrendCharts
} from '@element-plus/icons-vue'
import { apiPost } from '../../utils/api.js'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()

// 机器人分类选项
const categoryOptions = [
  { code: 'DATA_COLLECT', nameKey: 'aiCode.categoryCollect', icon: 'Download', descKey: 'aiCode.categoryCollectDesc' },
  { code: 'DATA_PARSE', nameKey: 'aiCode.categoryParse', icon: 'Document', descKey: 'aiCode.categoryParseDesc' },
  { code: 'DATA_PROCESS', nameKey: 'aiCode.categoryProcess', icon: 'Cpu', descKey: 'aiCode.categoryProcessDesc' },
  { code: 'GENERAL', nameKey: 'aiCode.categoryGeneral', icon: 'Connection', descKey: 'aiCode.categoryGeneralDesc' }
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
  return cat ? t(cat.nameKey) : code
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
    ElMessage.warning(t('aiCode.enterPromptDesc'))
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
    ElMessage.success(t('aiCode.promptOptimized'))
  } catch (error) {
    ElMessage.error(t('aiCode.aiOptimizeFailed') + (error.message || t('aiCode.unknownError')))
  } finally {
    optimizingPrompt.value = false
  }
}

// 采用优化后的提示词
const acceptPrompt = () => {
  userPrompt.value = optimizedPrompt.value
  hasOptimizedPrompt.value = false
  ElMessage.success(t('aiCode.adoptedOptimizedPrompt'))
}

// 生成代码
const generateCode = async () => {
  if (!canGenerate.value) {
    ElMessage.warning(t('aiCode.selectScene'))
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
      ElMessage.success(t('aiCode.codeGenerated'))
    } else {
      throw new Error(response.message || t('aiCode.generateFailed'))
    }
  } catch (error) {
    generatedCode.value = generateCodeLocally()
    ElMessage.warning(t('aiCode.usingLocalTemplate'))
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
    ElMessage.warning(t('aiCode.noCodeToCopy'))
    return
  }
  navigator.clipboard.writeText(generatedCode.value)
    .then(() => ElMessage.success(t('aiCode.copiedToClipboard')))
    .catch(() => ElMessage.error(t('aiCode.copyFailed')))
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
    ElMessage.warning(t('aiCode.noCodeToApply'))
    return
  }
  localStorage.setItem('aiGeneratedCode', generatedCode.value)
  localStorage.setItem('aiRobotCategory', configForm.robotCategory)
  router.push('/rpa/robots?action=create')
}

// 使用优化后的提示词
const useOptimizedPrompt = () => {
  userPrompt.value = optimizedPrompt.value
  ElMessage.success(t('aiCode.usedOptimizedDesc'))
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
  { code: 'collect', name: t('aiCode.templateCollect'), description: t('aiCode.templateCollectDesc') },
  { code: 'parse', name: t('aiCode.templateParse'), description: t('aiCode.templateParseDesc') },
  { code: 'process', name: t('aiCode.templateProcess'), description: t('aiCode.templateProcessDesc') },
  { code: 'store', name: t('aiCode.templateStore'), description: t('aiCode.templateStoreDesc') }
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
