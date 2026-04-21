/**
 * RPA可视化流程设计器 - 属性配置面板组件
 * 
 * 功能说明：
 * - 根据选中节点类型动态渲染配置表单
 * - 支持多种输入类型：文本、数字、选择、开关、代码编辑器
 * - 支持节点重命名
 * - 支持节点删除
 */

<template>
  <div class="property-panel">
    <div class="panel-header">
      <h3>属性配置</h3>
      <el-tag :type="getCategoryType(selectedNode?.data?.category)" size="small">
        {{ getCategoryText(selectedNode?.data?.category) }}
      </el-tag>
    </div>

    <div class="panel-content" v-if="selectedNode">
      <!-- 基础信息 -->
      <div class="property-section">
        <div class="section-title">基础信息</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="节点名称">
            <el-input v-model="nodeForm.name" placeholder="请输入节点名称" @change="updateNodeName" />
          </el-form-item>
          <el-form-item label="节点描述">
            <el-input v-model="nodeForm.description" type="textarea" :rows="2" placeholder="请输入节点描述" />
          </el-form-item>
          <el-form-item label="节点ID">
            <el-input v-model="selectedNode.id" disabled />
          </el-form-item>
        </el-form>
      </div>

      <!-- 浏览器配置 -->
      <div class="property-section" v-if="isBrowserNode">
        <div class="section-title">浏览器配置</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="浏览器类型" v-if="isType('browser_open')">
            <el-select v-model="nodeForm.config.browser" placeholder="选择浏览器">
              <el-option label="Chrome" value="chrome" />
              <el-option label="Edge" value="edge" />
              <el-option label="Firefox" value="firefox" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标URL" v-if="isType('browser_navigate')">
            <el-input v-model="nodeForm.config.url" placeholder="https://example.com">
              <template #append>
                <el-button @click="testUrl">测试</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="选择器" v-if="isClickOrInput()">
            <el-input v-model="nodeForm.config.selector" placeholder="CSS选择器或XPath">
              <template #append>
                <el-tooltip content="点击后在浏览器中拾取元素">
                  <el-button @click="pickElement">拾取</el-button>
                </el-tooltip>
              </template>
            </el-input>
            <div class="selector-hint">
              <el-radio-group v-model="nodeForm.config.selectorType" size="small">
                <el-radio-button label="css">CSS</el-radio-button>
                <el-radio-button label="xpath">XPath</el-radio-button>
              </el-radio-group>
            </div>
          </el-form-item>
          <el-form-item label="输入内容" v-if="isType('browser_input')">
            <el-input v-model="nodeForm.config.value" type="textarea" :rows="2" placeholder="要输入的文本" />
          </el-form-item>
          <el-form-item label="清空后输入" v-if="isType('browser_input')">
            <el-switch v-model="nodeForm.config.clear" />
          </el-form-item>
          <el-form-item label="等待加载" v-if="isType('browser_navigate')">
            <el-switch v-model="nodeForm.config.waitLoad" />
          </el-form-item>
          <el-form-item label="超时时间(秒)" v-if="hasTimeout()">
            <el-input-number v-model="nodeForm.config.timeout" :min="1" :max="300" />
          </el-form-item>
        </el-form>
      </div>

      <!-- Excel配置 -->
      <div class="property-section" v-if="isExcelNode">
        <div class="section-title">Excel配置</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="文件路径" v-if="isExcelFileNode()">
            <el-input v-model="nodeForm.config.filePath" placeholder="C:\Data\sales.xlsx">
              <template #append>
                <el-upload
                  :action="'/api/file/select'"
                  :show-file-list="false"
                  :on-success="onFileSelected"
                >
                  <el-button>选择</el-button>
                </el-upload>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="工作表名称" v-if="hasSheet()">
            <el-input v-model="nodeForm.config.sheetName" placeholder="Sheet1" />
          </el-form-item>
          <el-form-item label="单元格" v-if="isType('excel_read') || isType('excel_write')">
            <el-input v-model="nodeForm.config.cell" placeholder="A1" />
          </el-form-item>
          <el-form-item label="起始单元格" v-if="isType('excel_read_range') || isType('excel_write_range')">
            <el-input v-model="nodeForm.config.startCell" placeholder="A1" />
          </el-form-item>
          <el-form-item label="结束单元格" v-if="isType('excel_read_range') || isType('excel_write_range')">
            <el-input v-model="nodeForm.config.endCell" placeholder="Z100" />
          </el-form-item>
          <el-form-item label="写入值" v-if="isType('excel_write')">
            <el-input v-model="nodeForm.config.value" placeholder="要写入的值" />
          </el-form-item>
          <el-form-item label="写入数据" v-if="isType('excel_write_range')">
            <el-input v-model="nodeForm.config.data" type="textarea" :rows="4" placeholder="JSON数组格式: [[1,2,3],[4,5,6]]" />
          </el-form-item>
          <el-form-item label="可见模式" v-if="isType('excel_open')">
            <el-switch v-model="nodeForm.config.visible" />
          </el-form-item>
          <el-form-item label="只读模式" v-if="isType('excel_open')">
            <el-switch v-model="nodeForm.config.readOnly" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 文件操作配置 -->
      <div class="property-section" v-if="isFileNode">
        <div class="section-title">文件配置</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="文件路径">
            <el-input v-model="nodeForm.config.filePath" placeholder="C:\Data\file.txt">
              <template #append>
                <el-upload
                  :action="'/api/file/select'"
                  :show-file-list="false"
                  :on-success="(res) => nodeForm.config.filePath = res.data"
                >
                  <el-button>选择</el-button>
                </el-upload>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="目标路径" v-if="isType('file_copy') || isType('file_move')">
            <el-input v-model="nodeForm.config.target" placeholder="D:\Backup\file.txt" />
          </el-form-item>
          <el-form-item label="文件内容" v-if="isType('file_write')">
            <el-input v-model="nodeForm.config.content" type="textarea" :rows="4" placeholder="文件内容" />
          </el-form-item>
          <el-form-item label="追加模式" v-if="isType('file_write')">
            <el-switch v-model="nodeForm.config.append" />
          </el-form-item>
          <el-form-item label="编码格式">
            <el-select v-model="nodeForm.config.encoding" placeholder="选择编码">
              <el-option label="UTF-8" value="UTF-8" />
              <el-option label="GBK" value="GBK" />
              <el-option label="ASCII" value="ASCII" />
            </el-select>
          </el-form-item>
          <el-form-item label="覆盖文件" v-if="isType('file_copy') || isType('file_move')">
            <el-switch v-model="nodeForm.config.overwrite" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据库配置 -->
      <div class="property-section" v-if="isDatabaseNode">
        <div class="section-title">数据库配置</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="数据库类型" v-if="isType('db_connect')">
            <el-select v-model="nodeForm.config.provider" placeholder="选择数据库">
              <el-option label="MySQL" value="mysql" />
              <el-option label="PostgreSQL" value="postgresql" />
              <el-option label="SQL Server" value="sqlserver" />
              <el-option label="Oracle" value="oracle" />
            </el-select>
          </el-form-item>
          <el-form-item label="连接字符串" v-if="isType('db_connect')">
            <el-input v-model="nodeForm.config.connectionString" type="textarea" :rows="2" placeholder="host:port/database" />
          </el-form-item>
          <el-form-item label="SQL语句" v-if="isType('db_query') || isType('db_execute')">
            <el-input v-model="nodeForm.config.sql" type="textarea" :rows="4" placeholder="SELECT * FROM table_name" />
          </el-form-item>
          <el-form-item label="超时时间" v-if="hasTimeout()">
            <el-input-number v-model="nodeForm.config.timeout" :min="1" :max="600" />
          </el-form-item>
        </el-form>
      </div>

      <!-- AI能力配置 -->
      <div class="property-section" v-if="isAiNode">
        <div class="section-title">AI配置</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="图片路径">
            <el-input v-model="nodeForm.config.imagePath" placeholder="C:\Images\invoice.png">
              <template #append>
                <el-upload
                  :action="'/api/file/select'"
                  :show-file-list="false"
                  :on-success="(res) => nodeForm.config.imagePath = res.data"
                >
                  <el-button>选择</el-button>
                </el-upload>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="识别语言" v-if="isType('ai_ocr')">
            <el-select v-model="nodeForm.config.language" placeholder="选择语言">
              <el-option label="简体中文+英文" value="chi_sim+eng" />
              <el-option label="繁体中文" value="chi_tra+eng" />
              <el-option label="英文" value="eng" />
              <el-option label="日语" value="jpn" />
              <el-option label="韩语" value="kor" />
            </el-select>
          </el-form-item>
          <el-form-item label="置信度阈值" v-if="isType('ai_ocr')">
            <el-slider v-model="nodeForm.config.confidence" :min="0" :max="1" :step="0.1" show-stops />
          </el-form-item>
          <el-form-item label="身份证面" v-if="isType('ai_idcard')">
            <el-radio-group v-model="nodeForm.config.side">
              <el-radio-button label="front">正面</el-radio-button>
              <el-radio-button label="back">背面</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="发票类型" v-if="isType('ai_invoice')">
            <el-select v-model="nodeForm.config.invoiceType" placeholder="选择发票类型">
              <el-option label="全部" value="all" />
              <el-option label="增值税发票" value="vat" />
              <el-option label="火车票" value="train" />
              <el-option label="出租车票" value="taxi" />
              <el-option label="机票" value="flight" />
            </el-select>
          </el-form-item>
          <el-form-item label="结果变量" v-if="isAiResult()">
            <el-input v-model="nodeForm.config.outputVar" placeholder="ocrResult" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 逻辑控制配置 -->
      <div class="property-section" v-if="isLogicNode">
        <div class="section-title">逻辑配置</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="条件表达式" v-if="isType('logic_if')">
            <el-input v-model="nodeForm.config.condition" placeholder="如: var1 > 10 AND var2 == 'ok'" />
            <div class="condition-hint">
              <span>支持变量: <code>${变量名}</code></span>
            </div>
          </el-form-item>
          <el-form-item label="循环次数" v-if="isType('logic_loop')">
            <el-input-number v-model="nodeForm.config.times" :min="1" :max="10000" />
          </el-form-item>
          <el-form-item label="遍历数据" v-if="isType('logic_foreach')">
            <el-input v-model="nodeForm.config.items" placeholder="如: ${tableData}" />
          </el-form-item>
          <el-form-item label="循环变量名" v-if="isType('logic_foreach')">
            <el-input v-model="nodeForm.config.itemName" placeholder="如: row" />
          </el-form-item>
          <el-form-item label="等待秒数" v-if="isType('logic_wait')">
            <el-input-number v-model="nodeForm.config.seconds" :min="0" :max="3600" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 脚本配置 -->
      <div class="property-section" v-if="isScriptNode">
        <div class="section-title">脚本配置</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="代码">
            <el-input 
              v-model="nodeForm.config.code" 
              type="textarea" 
              :rows="10" 
              placeholder="输入代码..."
              class="code-editor"
            />
          </el-form-item>
          <el-form-item label="超时时间(秒)">
            <el-input-number v-model="nodeForm.config.timeout" :min="1" :max="3600" />
          </el-form-item>
          <el-form-item label="输出变量" v-if="!isType('script_http')">
            <el-input v-model="nodeForm.config.outputVar" placeholder="result" />
          </el-form-item>
        </el-form>
        <div class="script-help">
          <el-collapse>
            <el-collapse-item title="Python示例">
              <pre class="code-example">
import json

# 获取变量
data = ${inputData}

# 数据处理
result = [x for x in data if x['status'] == 'active']

# 返回结果
print(json.dumps(result))
              </pre>
            </el-collapse-item>
            <el-collapse-item title="JavaScript示例">
              <pre class="code-example">
// 获取变量
const data = JSON.parse('${inputData}');

// 数据处理
const result = data.filter(x => x.status === 'active');

// 输出结果
JSON.stringify(result);
              </pre>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>

      <!-- HTTP请求配置 -->
      <div class="property-section" v-if="isType('script_http')">
        <div class="section-title">HTTP请求配置</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="请求URL">
            <el-input v-model="nodeForm.config.url" placeholder="https://api.example.com/data" />
          </el-form-item>
          <el-form-item label="请求方法">
            <el-select v-model="nodeForm.config.method">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
              <el-option label="PUT" value="PUT" />
              <el-option label="DELETE" value="DELETE" />
            </el-select>
          </el-form-item>
          <el-form-item label="请求头">
            <el-input v-model="nodeForm.config.headers" type="textarea" :rows="3" placeholder='{"Content-Type": "application/json"}' />
          </el-form-item>
          <el-form-item label="请求体">
            <el-input v-model="nodeForm.config.body" type="textarea" :rows="3" placeholder="请求body" />
          </el-form-item>
          <el-form-item label="响应变量">
            <el-input v-model="nodeForm.config.outputVar" placeholder="httpResponse" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 通用的高级配置 -->
      <div class="property-section">
        <div class="section-title">高级配置</div>
        <el-form :model="nodeForm" label-position="top" size="small">
          <el-form-item label="错误处理">
            <el-select v-model="nodeForm.config.errorHandling" placeholder="选择错误处理方式">
              <el-option label="默认（记录错误）" value="default" />
              <el-option label="跳过继续执行" value="skip" />
              <el-option label="停止流程" value="stop" />
              <el-option label="重试后继续" value="retry" />
            </el-select>
          </el-form-item>
          <el-form-item label="重试次数" v-if="nodeForm.config.errorHandling === 'retry'">
            <el-input-number v-model="nodeForm.config.retryCount" :min="0" :max="10" />
          </el-form-item>
          <el-form-item label="执行前等待">
            <el-input-number v-model="nodeForm.config.delayBefore" :min="0" :max="300" placeholder="秒" />
          </el-form-item>
          <el-form-item label="执行后等待">
            <el-input-number v-model="nodeForm.config.delayAfter" :min="0" :max="300" placeholder="秒" />
          </el-form-item>
          <el-form-item label="启用节点">
            <el-switch v-model="nodeForm.config.enabled" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作按钮 -->
      <div class="property-actions">
        <el-button type="primary" @click="applyChanges" :loading="saving">应用更改</el-button>
        <el-button @click="resetChanges">重置</el-button>
        <el-divider direction="vertical" />
        <el-button type="danger" @click="deleteNode">删除节点</el-button>
      </div>
    </div>

    <div class="panel-empty" v-else>
      <el-empty description="请在画布中选择一个节点">
        <template #image>
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="#c0c4cc" stroke-width="1">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <path d="M9 9h6v6H9z" />
          </svg>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  selectedNode: Object
})

const emit = defineEmits(['update', 'delete'])

// 表单数据
const nodeForm = ref({
  name: '',
  description: '',
  config: {}
})

const saving = ref(false)

// 监听选中节点变化
watch(() => props.selectedNode, (node) => {
  if (node && node.data) {
    nodeForm.value = {
      name: node.data.name || '',
      description: node.data.description || '',
      config: JSON.parse(JSON.stringify(node.data.config || {}))
    }
  }
}, { immediate: true, deep: true })

// 节点类型判断
const isType = (type) => props.selectedNode?.data?.type === type
const isBrowserNode = computed(() => ['browser'].includes(props.selectedNode?.type))
const isExcelNode = computed(() => ['excel'].includes(props.selectedNode?.type))
const isFileNode = computed(() => ['file'].includes(props.selectedNode?.type))
const isDatabaseNode = computed(() => ['database'].includes(props.selectedNode?.type))
const isAiNode = computed(() => ['ai'].includes(props.selectedNode?.type))
const isLogicNode = computed(() => ['logic'].includes(props.selectedNode?.type))
const isScriptNode = computed(() => ['script'].includes(props.selectedNode?.type))

// 详细类型判断
const isClickOrInput = () => ['browser_click', 'browser_input'].includes(props.selectedNode?.data?.type)
const hasTimeout = () => ['browser_open', 'browser_navigate', 'browser_click', 'browser_wait',
  'db_connect', 'db_query', 'db_execute', 'script_python', 'script_js'].includes(props.selectedNode?.data?.type)
const isExcelFileNode = () => ['excel_open', 'excel_read', 'excel_write', 'excel_read_range', 'excel_write_range', 'excel_save'].includes(props.selectedNode?.data?.type)
const hasSheet = () => ['excel_read', 'excel_write', 'excel_read_range', 'excel_write_range', 'excel_append'].includes(props.selectedNode?.data?.type)
const isAiResult = () => ['ai_ocr', 'ai_idcard', 'ai_invoice', 'ai_bankcard', 'ai_captcha', 'ai_table', 'ai_nlp', 'ai_translate'].includes(props.selectedNode?.data?.type)

// 获取分类文本
const getCategoryText = (category) => {
  const map = {
    trigger: '触发器',
    browser: '浏览器',
    desktop: '桌面应用',
    excel: 'Excel',
    file: '文件',
    database: '数据库',
    ai: 'AI能力',
    logic: '逻辑控制',
    script: '脚本',
    communication: '通信'
  }
  return map[category] || '其他'
}

// 获取分类样式
const getCategoryType = (category) => {
  const map = {
    trigger: 'success',
    browser: 'primary',
    desktop: 'primary',
    excel: 'success',
    file: 'success',
    database: 'warning',
    ai: 'danger',
    logic: 'warning',
    script: 'warning',
    communication: 'info'
  }
  return map[category] || 'info'
}

// 更新节点名称
const updateNodeName = () => {
  if (props.selectedNode) {
    props.selectedNode.data.name = nodeForm.value.name
  }
}

// 应用更改
const applyChanges = () => {
  saving.value = true
  try {
    if (props.selectedNode) {
      props.selectedNode.data.name = nodeForm.value.name
      props.selectedNode.data.description = nodeForm.value.description
      props.selectedNode.data.config = { ...props.selectedNode.data.config, ...nodeForm.value.config }
      emit('update', props.selectedNode)
      ElMessage.success('配置已更新')
    }
  } finally {
    saving.value = false
  }
}

// 重置更改
const resetChanges = () => {
  if (props.selectedNode && props.selectedNode.data) {
    nodeForm.value = {
      name: props.selectedNode.data.name || '',
      description: props.selectedNode.data.description || '',
      config: JSON.parse(JSON.stringify(props.selectedNode.data.config || {}))
    }
  }
}

// 删除节点
const deleteNode = () => {
  if (props.selectedNode) {
    emit('delete', props.selectedNode)
  }
}

// 测试URL
const testUrl = () => {
  ElMessage.info(`测试URL: ${nodeForm.value.config.url}`)
}

// 拾取元素
const pickElement = () => {
  ElMessage.info('请在浏览器中点击要选择的元素')
}

// 文件选择回调
const onFileSelected = (res) => {
  if (res.code === 0) {
    nodeForm.value.config.filePath = res.data
    ElMessage.success('文件已选择')
  }
}
</script>

<style scoped>
.property-panel {
  width: 320px;
  height: 100%;
  background: #fff;
  border-left: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.panel-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.property-section {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px dashed #ebeef5;
}

.property-section:last-of-type {
  border-bottom: none;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.selector-hint {
  margin-top: 8px;
}

.condition-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.condition-hint code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.script-help {
  margin-top: 12px;
}

.code-example {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  font-size: 12px;
  font-family: 'Monaco', 'Menlo', monospace;
  overflow-x: auto;
  margin: 0;
}

.property-actions {
  padding: 16px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  position: sticky;
  bottom: 0;
  background: #fff;
  border-top: 1px solid #e8e8e8;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}

:deep(.el-form-item__label) {
  font-size: 12px;
  color: #606266;
}

:deep(.code-editor textarea) {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
}
</style>
