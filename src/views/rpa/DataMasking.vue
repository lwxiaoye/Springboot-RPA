<template>
  <div class="masking-page">
    <div class="page-header">
      <h2>数据脱敏中心</h2>
      <p class="page-desc">对敏感数据进行脱敏处理，符合金融行业数据安全规范</p>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：脱敏工具 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>数据脱敏工具</span>
          </template>
          
          <el-form :model="maskForm" label-width="100px">
            <el-form-item label="数据类型">
              <el-select v-model="maskForm.type" placeholder="选择数据类型" style="width: 100%">
                <el-option label="身份证" value="ID_CARD" />
                <el-option label="手机号" value="PHONE" />
                <el-option label="银行卡" value="BANK_CARD" />
                <el-option label="邮箱" value="EMAIL" />
                <el-option label="姓名" value="NAME" />
                <el-option label="地址" value="ADDRESS" />
                <el-option label="密码" value="PASSWORD" />
                <el-option label="金额" value="AMOUNT" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="原始数据">
              <el-input
                v-model="maskForm.data"
                type="textarea"
                :rows="4"
                :placeholder="getPlaceholder()"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="maskData">
                <el-icon><Key /></el-icon> 脱敏
              </el-button>
              <el-button @click="maskForm.data = ''">清空</el-button>
            </el-form-item>
          </el-form>

          <el-divider>脱敏结果</el-divider>

          <div v-if="maskResult" class="mask-result">
            <div class="result-row">
              <span class="label">原始数据:</span>
              <span class="value original">{{ maskResult.original }}</span>
            </div>
            <div class="result-row">
              <span class="label">脱敏后:</span>
              <span class="value masked">{{ maskResult.masked }}</span>
              <el-button type="primary" size="small" @click="copyResult(maskResult.masked)">复制</el-button>
            </div>
          </div>
          <el-empty v-else description="请输入数据进行脱敏" />
        </el-card>

        <!-- 批量脱敏 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <span>批量脱敏</span>
          </template>
          
          <el-input
            v-model="batchData"
            type="textarea"
            :rows="6"
            placeholder="输入JSON格式数据，每行一个字段&#10;如: {&quot;idCard&quot;: &quot;110101199001011234&quot;, &quot;phone&quot;: &quot;13812345678&quot;}"
          />
          
          <div class="batch-types">
            <span>字段类型映射:</span>
            <el-tag
              v-for="field in batchFields"
              :key="field.key"
              closable
              size="small"
              style="margin: 5px"
              @close="removeBatchField(field.key)"
            >
              {{ field.key }}: {{ field.type }}
            </el-tag>
            <el-button size="small" @click="showAddBatchField = true">+ 添加映射</el-button>
          </div>
          
          <el-button type="primary" @click="batchMask" :loading="batchLoading" style="margin-top: 15px">
            批量脱敏
          </el-button>
        </el-card>
      </el-col>

      <!-- 右侧：规则说明 -->
      <el-col :span="12">
        <!-- 脱敏规则 -->
        <el-card shadow="hover">
          <template #header>
            <span>脱敏规则</span>
          </template>
          
          <el-table :data="maskingRules" border stripe>
            <el-table-column prop="type" label="数据类型" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.typeName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="rule" label="脱敏规则" />
            <el-table-column prop="example" label="示例">
              <template #default="{ row }">
                <span class="example-text">{{ row.original }} → {{ row.masked }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- API调用 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <span>API调用</span>
          </template>
          
          <el-tabs>
            <el-tab-pane label="单个脱敏">
              <pre class="api-code">POST /api/enterprise/mask
Content-Type: application/json

{
  "data": "110101199001011234",
  "type": "ID_CARD"
}</pre>
            </el-tab-pane>
            <el-tab-pane label="批量脱敏">
              <pre class="api-code">POST /api/enterprise/mask/batch
Content-Type: application/json

{
  "data": {
    "idCard": "110101199001011234",
    "phone": "13812345678"
  },
  "fields": ["idCard", "phone"],
  "typeMap": {
    "idCard": "ID_CARD",
    "phone": "PHONE"
  }
}</pre>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 使用场景 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <span>使用场景</span>
          </template>
          
          <div class="scenario-list">
            <div class="scenario-item">
              <div class="scenario-icon">📋</div>
              <div class="scenario-content">
                <div class="scenario-title">日志记录</div>
                <div class="scenario-desc">执行日志中自动脱敏敏感信息</div>
              </div>
            </div>
            <div class="scenario-item">
              <div class="scenario-icon">📊</div>
              <div class="scenario-content">
                <div class="scenario-title">报表展示</div>
                <div class="scenario-desc">数据报表中隐藏部分敏感字段</div>
              </div>
            </div>
            <div class="scenario-item">
              <div class="scenario-icon">📤</div>
              <div class="scenario-content">
                <div class="scenario-title">数据导出</div>
                <div class="scenario-desc">导出数据时自动应用脱敏规则</div>
              </div>
            </div>
            <div class="scenario-item">
              <div class="scenario-icon">🔍</div>
              <div class="scenario-content">
                <div class="scenario-title">日志审计</div>
                <div class="scenario-desc">满足金融行业合规审计要求</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加批量字段对话框 -->
    <el-dialog v-model="showAddBatchField" title="添加字段映射" width="400px">
      <el-form :model="newBatchField" label-width="80px">
        <el-form-item label="字段名">
          <el-input v-model="newBatchField.key" placeholder="如: idCard" />
        </el-form-item>
        <el-form-item label="脱敏类型">
          <el-select v-model="newBatchField.type" placeholder="选择类型" style="width: 100%">
            <el-option label="身份证" value="ID_CARD" />
            <el-option label="手机号" value="PHONE" />
            <el-option label="银行卡" value="BANK_CARD" />
            <el-option label="邮箱" value="EMAIL" />
            <el-option label="姓名" value="NAME" />
            <el-option label="地址" value="ADDRESS" />
            <el-option label="密码" value="PASSWORD" />
            <el-option label="金额" value="AMOUNT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddBatchField = false">取消</el-button>
        <el-button type="primary" @click="addBatchField">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Key } from '@element-plus/icons-vue'

const apiBase = '/api'
const token = localStorage.getItem('token') || ''

const maskForm = reactive({
  type: 'ID_CARD',
  data: ''
})

const maskResult = ref(null)
const batchData = ref('')
const batchLoading = ref(false)
const batchFields = ref([
  { key: 'idCard', type: 'ID_CARD' },
  { key: 'phone', type: 'PHONE' }
])
const showAddBatchField = ref(false)
const newBatchField = reactive({ key: '', type: 'ID_CARD' })

const maskingRules = [
  { typeName: '身份证', type: 'ID_CARD', rule: '前3后4，中间脱敏', original: '110101199001011234', masked: '110****1234' },
  { typeName: '手机号', type: 'PHONE', rule: '前3后4，中间脱敏', original: '13812345678', masked: '138****5678' },
  { typeName: '银行卡', type: 'BANK_CARD', rule: '只显示后4位', original: '6222021234567890123', masked: '***********90123' },
  { typeName: '邮箱', type: 'EMAIL', rule: '前缀保留1-3位+域名', original: 'zhangsan@example.com', masked: 'z***san@example.com' },
  { typeName: '姓名', type: 'NAME', rule: '只显示姓，名字脱敏', original: '张三', masked: '张*' },
  { typeName: '地址', type: 'ADDRESS', rule: '显示省市区，详细脱敏', original: '北京市朝阳区XX街道XX号', masked: '北京市朝阳区***' },
  { typeName: '密码', type: 'PASSWORD', rule: '全部脱敏', original: 'MyP@ssw0rd!', masked: '**********' },
  { typeName: '金额', type: 'AMOUNT', rule: '显示首位，保留小数', original: '12345.67', masked: '1*****.67' },
]

const getPlaceholder = () => {
  const placeholders = {
    ID_CARD: '请输入身份证号，如：110101199001011234',
    PHONE: '请输入手机号，如：13812345678',
    BANK_CARD: '请输入银行卡号，如：6222021234567890123',
    EMAIL: '请输入邮箱地址，如：zhangsan@example.com',
    NAME: '请输入姓名，如：张三',
    ADDRESS: '请输入地址，如：北京市朝阳区XX街道XX号',
    PASSWORD: '请输入密码',
    AMOUNT: '请输入金额，如：12345.67',
  }
  return placeholders[maskForm.type] || '请输入数据'
}

const maskData = async () => {
  if (!maskForm.data.trim()) {
    ElMessage.warning('请输入原始数据')
    return
  }
  
  try {
    const res = await fetch(apiBase + '/enterprise/mask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({
        data: maskForm.data,
        type: maskForm.type
      })
    })
    const data = await res.json()
    if (data.code === 0) {
      maskResult.value = data.data
      ElMessage.success('脱敏成功')
    } else {
      ElMessage.error(data.message || '脱敏失败')
    }
  } catch (e) {
    ElMessage.error('请求失败')
  }
}

const copyResult = (text) => {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

const addBatchField = () => {
  if (!newBatchField.key || !newBatchField.type) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (batchFields.value.find(f => f.key === newBatchField.key)) {
    ElMessage.warning('字段已存在')
    return
  }
  batchFields.value.push({ ...newBatchField })
  newBatchField.key = ''
  newBatchField.type = 'ID_CARD'
  showAddBatchField.value = false
  ElMessage.success('已添加')
}

const removeBatchField = (key) => {
  batchFields.value = batchFields.value.filter(f => f.key !== key)
}

const batchMask = async () => {
  if (!batchData.value.trim()) {
    ElMessage.warning('请输入批量数据')
    return
  }
  
  batchLoading.value = true
  try {
    // 模拟批量脱敏
    const data = JSON.parse(batchData.value)
    const fields = batchFields.value.map(f => f.key)
    const typeMap = {}
    batchFields.value.forEach(f => typeMap[f.key] = f.type)
    
    const res = await fetch(apiBase + '/enterprise/mask/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ data, fields, typeMap })
    })
    const result = await res.json()
    if (result.code === 0) {
      batchData.value = JSON.stringify(result.data, null, 2)
      ElMessage.success('批量脱敏成功')
    }
  } catch (e) {
    ElMessage.error('批量脱敏失败，请检查数据格式')
  } finally {
    batchLoading.value = false
  }
}
</script>

<style scoped>
.masking-page {
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

.mask-result {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
}

.result-row {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 10px;
}

.result-row:last-child {
  margin-bottom: 0;
}

.result-row .label {
  width: 80px;
  color: #666;
}

.result-row .value {
  flex: 1;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: monospace;
}

.result-row .original {
  background: #fee;
  color: #c00;
}

.result-row .masked {
  background: #efe;
  color: #060;
}

.batch-types {
  margin-top: 15px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
}

.batch-types > span {
  color: #666;
  margin-right: 10px;
}

.example-text {
  font-family: monospace;
  font-size: 12px;
}

.api-code {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 15px;
  border-radius: 4px;
  font-size: 12px;
  overflow: auto;
  margin: 0;
}

.scenario-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.scenario-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.scenario-icon {
  font-size: 32px;
}

.scenario-content {
  flex: 1;
}

.scenario-title {
  font-weight: 500;
  margin-bottom: 5px;
}

.scenario-desc {
  font-size: 13px;
  color: #666;
}
</style>