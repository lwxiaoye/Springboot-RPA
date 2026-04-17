<template>
  <div class="script-page">
    <div class="page-header">
      <h2>脚本执行器</h2>
      <p class="page-desc">编写和执行Python、JavaScript脚本，扩展RPA自动化能力</p>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：脚本编辑器 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>脚本编辑器</span>
              <el-select v-model="scriptType" size="small" style="width: 120px;">
                <el-option label="Python" value="python" />
                <el-option label="JavaScript" value="javascript" />
                <el-option label="Shell" value="shell" />
              </el-select>
            </div>
          </template>

          <div class="editor-toolbar">
            <el-button-group>
              <el-button size="small" @click="loadTemplate">加载模板</el-button>
              <el-button size="small" @click="validateScript">验证脚本</el-button>
              <el-button size="small" @click="clearEditor">清空</el-button>
            </el-button-group>
            <el-button-group>
              <el-button size="small" type="success" @click="showAiDialog">
                <el-icon><MagicStick /></el-icon> AI生成
              </el-button>
              <el-button size="small" type="primary" @click="executeScript" :loading="executing">
                <el-icon><VideoPlay /></el-icon> 执行
              </el-button>
              <el-button size="small" type="danger" @click="stopScript" :disabled="!runningScriptId">
                <el-icon><VideoPause /></el-icon> 停止
              </el-button>
            </el-button-group>
          </div>

          <el-input
            v-model="scriptCode"
            type="textarea"
            :rows="20"
            class="code-editor"
            placeholder="在此编写脚本代码..."
            spellcheck="false"
          />
        </el-card>

        <!-- 执行结果 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>执行结果</span>
              <el-tag v-if="executionResult" :type="executionResult.success ? 'success' : 'danger'">
                {{ executionResult.success ? '执行成功' : '执行失败' }}
              </el-tag>
            </div>
          </template>
          
          <div v-if="executionResult" class="result-panel">
            <div class="result-meta">
              <el-tag>耗时: {{ executionResult.duration }}ms</el-tag>
              <el-tag v-if="executionResult.exitCode !== undefined">退出码: {{ executionResult.exitCode }}</el-tag>
              <el-tag v-if="executionResult.killed" type="warning">被终止</el-tag>
            </div>
            
            <el-tabs>
              <el-tab-pane label="输出" name="output">
                <pre class="output-content">{{ executionResult.output || '（无输出）' }}</pre>
              </el-tab-pane>
              <el-tab-pane label="错误" name="error" v-if="executionResult.errorMessage">
                <pre class="error-content">{{ executionResult.errorMessage }}</pre>
              </el-tab-pane>
            </el-tabs>
          </div>
          <el-empty v-else description="暂无执行结果" />
        </el-card>
      </el-col>

      <!-- 右侧：配置和模板 -->
      <el-col :span="8">
        <!-- 变量注入 -->
        <el-card shadow="hover">
          <template #header>
            <span>环境变量</span>
          </template>
          <div class="variables-list">
            <div v-for="(value, key) in variables" :key="key" class="variable-item">
              <el-input v-model="variables[key]" :placeholder="key" style="flex: 1;">
                <template #prepend>{{ key }}</template>
              </el-input>
              <el-button type="danger" size="small" @click="removeVariable(key)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <div class="add-variable">
              <el-input v-model="newVarKey" placeholder="变量名" style="width: 120px;">
                <template #append>
                  <el-button @click="addVariable">添加</el-button>
                </template>
              </el-input>
            </div>
          </div>
        </el-card>

        <!-- 执行配置 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <span>执行配置</span>
          </template>
          <el-form label-width="80px">
            <el-form-item label="超时时间">
              <el-input-number v-model="timeout" :min="1000" :max="300000" :step="1000" />
              <span class="unit">ms</span>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 脚本模板 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <span>常用模板</span>
          </template>
          <div class="templates-list">
            <div class="template-item" @click="loadTemplate('python_data_process')">
              <div class="template-icon">🐍</div>
              <div class="template-info">
                <div class="template-name">Python数据处理</div>
                <div class="template-desc">JSON数据处理示例</div>
              </div>
            </div>
            <div class="template-item" @click="loadTemplate('python_http')">
              <div class="template-icon">🌐</div>
              <div class="template-info">
                <div class="template-name">Python HTTP请求</div>
                <div class="template-desc">发送HTTP请求示例</div>
              </div>
            </div>
            <div class="template-item" @click="loadTemplate('js_data_process')">
              <div class="template-icon">📜</div>
              <div class="template-info">
                <div class="template-name">JS数据处理</div>
                <div class="template-desc">JavaScript数据处理</div>
              </div>
            </div>
            <div class="template-item" @click="loadTemplate('js_http')">
              <div class="template-icon">⚡</div>
              <div class="template-info">
                <div class="template-name">JS HTTP请求</div>
                <div class="template-desc">Node.js请求示例</div>
              </div>
            </div>
            <div class="template-item" @click="loadTemplate('python_excel')">
              <div class="template-icon">📊</div>
              <div class="template-info">
                <div class="template-name">Python Excel</div>
                <div class="template-desc">Excel数据处理</div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 安全说明 -->
        <el-card shadow="hover" style="margin-top: 20px" class="security-note">
          <template #header>
            <span>安全提示</span>
          </template>
          <ul class="security-list">
            <li>危险命令已禁用：rm -rf /, format c: 等</li>
            <li>网络访问受限：subprocess, socket 等已禁用</li>
            <li>脚本长度限制：最大100KB</li>
            <li>执行超时保护：防止脚本死循环</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模板选择对话框 -->
    <el-dialog v-model="templateDialogVisible" title="选择模板" width="600px">
      <div class="template-grid">
        <div v-for="tpl in templates" :key="tpl.key" class="template-card" @click="selectTemplate(tpl)">
          <div class="template-icon">{{ tpl.icon }}</div>
          <div class="template-name">{{ tpl.name }}</div>
          <div class="template-desc">{{ tpl.desc }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- AI生成对话框 -->
    <el-dialog v-model="aiDialogVisible" title="AI生成脚本" width="600px" :close-on-click-modal="false">
      <el-alert
        v-if="!aiConfigured"
        type="warning"
        :closable="false"
        style="margin-bottom: 15px"
      >
        AI未配置，请在系统设置中配置LLM服务
      </el-alert>

      <el-form :model="aiForm" label-width="100px">
        <el-form-item label="脚本类型">
          <el-select v-model="aiForm.scriptType" style="width: 100%">
            <el-option label="Python" value="python" />
            <el-option label="JavaScript" value="javascript" />
            <el-option label="Shell" value="shell" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求描述">
          <el-input
            v-model="aiForm.prompt"
            type="textarea"
            :rows="4"
            placeholder="用自然语言描述你想要的脚本功能，例如：读取当前目录下所有CSV文件，合并成一个文件，并统计每列的总和"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateByAi" :loading="aiGenerating" :disabled="!aiConfigured">
          <el-icon v-if="!aiGenerating"><MagicStick /></el-icon>
          AI生成代码
        </el-button>
      </template>
    </el-dialog>

    <!-- AI生成结果预览对话框 -->
    <el-dialog v-model="aiPreviewVisible" title="AI生成结果" width="800px">
      <div v-if="aiGeneratedCode" class="ai-preview">
        <el-alert type="success" :closable="false" style="margin-bottom: 15px">
          AI生成成功！生成的代码已填充到编辑器中。
        </el-alert>
        <pre class="generated-code-preview">{{ aiGeneratedCode }}</pre>
      </div>
      <div v-else class="ai-preview">
        <el-alert type="warning" :closable="false">
          未生成代码，请重试。
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="aiPreviewVisible = false">关闭</el-button>
        <el-button type="primary" @click="applyAiCode">应用代码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, VideoPause, Delete, MagicStick } from '@element-plus/icons-vue'

const apiBase = '/api'
const token = localStorage.getItem('token') || ''

const scriptType = ref('python')
const scriptCode = ref('')
const executing = ref(false)
const runningScriptId = ref(null)
const executionResult = ref(null)
const timeout = ref(60000)
const variables = reactive({})
const newVarKey = ref('')
const templateDialogVisible = ref(false)

// AI生成相关
const aiDialogVisible = ref(false)
const aiPreviewVisible = ref(false)
const aiGenerating = ref(false)
const aiGeneratedCode = ref('')
const aiConfigured = ref(false)

const aiForm = reactive({
  scriptType: 'python',
  prompt: ''
})

// 检查AI配置状态
const checkAiConfig = async () => {
  try {
    const res = await fetch(apiBase + '/script-ai/status', {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const data = await res.json()
    if (data.code === 0 && data.data) {
      aiConfigured.value = data.data.configured
    }
  } catch (e) {
    aiConfigured.value = false
  }
}

const showAiDialog = () => {
  checkAiConfig()
  aiDialogVisible.value = true
}

const generateByAi = async () => {
  if (!aiForm.prompt.trim()) {
    ElMessage.warning('请输入需求描述')
    return
  }

  aiGenerating.value = true
  try {
    const res = await fetch(apiBase + '/script-ai/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({
        prompt: aiForm.prompt,
        scriptType: aiForm.scriptType
      })
    })
    const data = await res.json()

    if (data.code === 0 && data.data) {
      aiGeneratedCode.value = data.data.code || ''
      aiDialogVisible.value = false

      if (aiGeneratedCode.value) {
        aiPreviewVisible.value = true
      } else {
        ElMessage.warning('AI未返回有效代码')
      }
    } else {
      ElMessage.error(data.message || 'AI生成失败')
    }
  } catch (e) {
    console.error('AI生成失败:', e)
    ElMessage.error('AI生成失败: ' + (e.message || '请检查网络连接'))
  } finally {
    aiGenerating.value = false
  }
}

const applyAiCode = () => {
  if (aiGeneratedCode.value) {
    scriptCode.value = aiGeneratedCode.value
    // 根据选择的脚本类型设置
    if (aiForm.scriptType === 'python') {
      scriptType.value = 'python'
    } else if (aiForm.scriptType === 'javascript') {
      scriptType.value = 'javascript'
    } else {
      scriptType.value = 'shell'
    }
    aiPreviewVisible.value = false
    ElMessage.success('代码已应用到编辑器')
  }
}

const templates = [
  { key: 'python_data_process', name: '数据处理', desc: 'JSON数据处理模板', icon: '🐍', type: 'python', code: '# 数据处理示例\nimport json\n\n# 获取输入\ndata = json.loads("${inputData}")\n\n# 处理数据\nresult = [item for item in data]\n\n# 输出结果\nprint(json.dumps(result, ensure_ascii=False))' },
  { key: 'python_http', name: 'HTTP请求', desc: '发送HTTP请求', icon: '🌐', type: 'python', code: '# HTTP请求示例\nimport json\nprint(json.dumps({"status": "success", "data": []}))' },
  { key: 'js_data_process', name: 'JS数据处理', desc: 'JavaScript数据处理', icon: '📜', type: 'javascript', code: '// 数据处理示例\nconst data = JSON.parse("${inputData}");\nconst result = data.map(item => ({ ...item, processed: true }));\nconsole.log(JSON.stringify(result));' },
  { key: 'js_http', name: 'JS HTTP请求', desc: 'Node.js HTTP请求', icon: '⚡', type: 'javascript', code: '// HTTP请求示例\nconst result = { status: "success", data: [] };\nconsole.log(JSON.stringify(result));' },
  { key: 'python_excel', name: 'Excel处理', desc: 'Excel数据处理', icon: '📊', type: 'python', code: '# Excel处理示例\nimport json\ndata = json.loads("${excelData}")\nresult = [row for row in data]\nprint(json.dumps(result, ensure_ascii=False))' },
]

const loadTemplate = (key) => {
  if (typeof key === 'string') {
    const tpl = templates.find(t => t.key === key)
    if (tpl) {
      scriptType.value = tpl.type
      scriptCode.value = tpl.code
      ElMessage.success('已加载模板: ' + tpl.name)
    }
  } else {
    templateDialogVisible.value = true
  }
}

const selectTemplate = (tpl) => {
  scriptType.value = tpl.type
  scriptCode.value = tpl.code
  templateDialogVisible.value = false
  ElMessage.success('已加载模板: ' + tpl.name)
}

const clearEditor = () => {
  scriptCode.value = ''
  executionResult.value = null
}

const validateScript = async () => {
  if (!scriptCode.value.trim()) {
    ElMessage.warning('请输入脚本内容')
    return
  }
  try {
    const res = await fetch(apiBase + '/script/validate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ scriptType: scriptType.value, code: scriptCode.value })
    })
    const data = await res.json()
    if (data.code === 0 && data.data) {
      if (data.data.valid) {
        ElMessage.success('验证通过: ' + data.data.reason)
      } else {
        ElMessage.error('验证失败: ' + data.data.reason)
      }
    }
  } catch (e) {
    ElMessage.error('验证请求失败')
  }
}

const executeScript = async () => {
  if (!scriptCode.value.trim()) {
    ElMessage.warning('请输入脚本内容')
    return
  }
  
  executing.value = true
  executionResult.value = null
  
  try {
    const res = await fetch(apiBase + '/script/execute', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({
        scriptType: scriptType.value,
        code: scriptCode.value,
        variables: Object.keys(variables).length > 0 ? variables : null,
        timeout: timeout.value
      })
    })
    const data = await res.json()
    if (data.code === 0) {
      executionResult.value = data.data
      if (data.data.success) {
        ElMessage.success('执行成功')
      } else {
        ElMessage.error('执行失败: ' + (data.data.errorMessage || '未知错误'))
      }
    } else {
      ElMessage.error(data.message || '执行失败')
    }
  } catch (e) {
    ElMessage.error('执行请求失败')
  } finally {
    executing.value = false
  }
}

const stopScript = async () => {
  if (!runningScriptId.value) {
    ElMessage.warning('没有正在运行的脚本')
    return
  }
  try {
    const res = await fetch(apiBase + '/script/stop/' + runningScriptId.value, {
      method: 'POST'
    })
    const data = await res.json()
    if (data.code === 0) {
      ElMessage.success('脚本已停止')
      runningScriptId.value = null
    }
  } catch (e) {
    ElMessage.error('停止失败')
  }
}

const addVariable = () => {
  if (!newVarKey.value.trim()) {
    ElMessage.warning('请输入变量名')
    return
  }
  if (variables[newVarKey.value]) {
    ElMessage.warning('变量已存在')
    return
  }
  variables[newVarKey.value] = ''
  newVarKey.value = ''
}

const removeVariable = (key) => {
  delete variables[key]
}
</script>

<style scoped>
.script-page {
  padding: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
}

.page-desc {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}

.code-editor textarea {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  line-height: 1.6;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 4px;
}

.result-panel {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.result-meta {
  display: flex;
  gap: 10px;
}

.output-content, .error-content {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 15px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  max-height: 300px;
  overflow: auto;
  white-space: pre-wrap;
  margin: 0;
}

.error-content {
  color: #f48771;
}

.variables-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.variable-item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.add-variable {
  margin-top: 10px;
}

.unit {
  margin-left: 10px;
  color: #666;
}

.templates-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.template-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.template-item:hover {
  background: #e4e7ed;
  transform: translateX(5px);
}

.template-icon {
  font-size: 24px;
}

.template-info {
  flex: 1;
}

.template-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.template-desc {
  font-size: 12px;
  color: #666;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.template-card {
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
}

.template-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.2);
}

.template-card .template-icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.template-card .template-name {
  font-weight: 500;
  margin-bottom: 5px;
}

.template-card .template-desc {
  font-size: 12px;
  color: #666;
}

.security-note {
  background: #fdf6ec;
  border-color: #f5dab1;
}

.security-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #666;
}

.security-list li {
  margin-bottom: 8px;
}

.ai-preview {
  padding: 10px 0;
}

.generated-code-preview {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 15px;
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  max-height: 400px;
  overflow: auto;
  white-space: pre-wrap;
  margin: 0;
}
</style>