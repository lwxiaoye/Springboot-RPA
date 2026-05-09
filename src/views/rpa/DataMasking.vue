<template>
  <div class="masking-page">
    <div class="page-header">
      <h2>{{ t('masking.title') }}</h2>
      <p class="page-desc">{{ t('masking.pageDesc') }}</p>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：脱敏工具 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>{{ t('masking.tool') }}</span>
          </template>
          
          <el-form :model="maskForm" label-width="100px">
            <el-form-item :label="t('masking.dataType')">
              <el-select v-model="maskForm.type" :placeholder="t('masking.dataTypePlaceholder')" style="width: 100%">
                <el-option :label="t('masking.typeIdCard')" value="ID_CARD" />
                <el-option :label="t('masking.typePhone')" value="PHONE" />
                <el-option :label="t('masking.typeBankCard')" value="BANK_CARD" />
                <el-option :label="t('masking.typeEmail')" value="EMAIL" />
                <el-option :label="t('masking.typeName')" value="NAME" />
                <el-option :label="t('masking.typeAddress')" value="ADDRESS" />
                <el-option :label="t('masking.typePassword')" value="PASSWORD" />
                <el-option :label="t('masking.typeAmount')" value="AMOUNT" />
              </el-select>
            </el-form-item>
            
            <el-form-item :label="t('masking.originalInput')">
              <el-input
                v-model="maskForm.data"
                type="textarea"
                :rows="4"
                :placeholder="getPlaceholder()"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="maskData">
                <el-icon><Key /></el-icon> {{ t('masking.mask') }}
              </el-button>
              <el-button @click="maskForm.data = ''">{{ t('masking.clear') }}</el-button>
            </el-form-item>
          </el-form>

          <el-divider>{{ t('masking.result') }}</el-divider>

          <div v-if="maskResult" class="mask-result">
            <div class="result-row">
              <span class="label">{{ t('masking.originalData') }}:</span>
              <span class="value original">{{ maskResult.original }}</span>
            </div>
            <div class="result-row">
              <span class="label">{{ t('masking.maskedData') }}:</span>
              <span class="value masked">{{ maskResult.masked }}</span>
              <el-button type="primary" size="small" @click="copyResult(maskResult.masked)">{{ t('masking.copyResult') }}</el-button>
            </div>
          </div>
          <el-empty v-else :description="t('masking.pleaseInputData')" />
        </el-card>

        <!-- 批量脱敏 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <span>{{ t('masking.batchMask') }}</span>
          </template>
          
          <el-input
            v-model="batchData"
            type="textarea"
            :rows="6"
            :placeholder="t('masking.batchPlaceholder')"
          />
          
          <div class="batch-types">
            <span>{{ t('masking.fieldMapping') }}:</span>
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
            <el-button size="small" @click="showAddBatchField = true">{{ t('masking.addMapping') }}</el-button>
          </div>
          
          <el-button type="primary" @click="batchMask" :loading="batchLoading" style="margin-top: 15px">
            {{ t('masking.batchMask') }}
          </el-button>
        </el-card>
      </el-col>

      <!-- 右侧：规则说明 -->
      <el-col :span="12">
        <!-- 脱敏规则 -->
        <el-card shadow="hover">
          <template #header>
            <span>{{ t('masking.rules') }}</span>
          </template>
          
          <el-table :data="maskingRules" border stripe>
            <el-table-column prop="type" :label="t('masking.ruleType')" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.typeName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="rule" :label="t('masking.ruleContent')" />
            <el-table-column prop="example" :label="t('masking.example')">
              <template #default="{ row }">
                <span class="example-text">{{ row.original }} → {{ row.masked }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- API调用 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <span>{{ t('masking.apiUsage') }}</span>
          </template>
          
          <el-tabs>
            <el-tab-pane :label="t('masking.singleMask')">
              <pre class="api-code">POST /api/enterprise/mask
Content-Type: application/json

{
  "data": "110101199001011234",
  "type": "ID_CARD"
}</pre>
            </el-tab-pane>
            <el-tab-pane :label="t('masking.batchMaskApi')">
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
            <span>{{ t('masking.scenarios') }}</span>
          </template>
          
          <div class="scenario-list">
            <div class="scenario-item">
              <div class="scenario-icon">📋</div>
              <div class="scenario-content">
                <div class="scenario-title">{{ t('masking.scenarioLog') }}</div>
                <div class="scenario-desc">{{ t('masking.scenarioLogDesc') }}</div>
              </div>
            </div>
            <div class="scenario-item">
              <div class="scenario-icon">📊</div>
              <div class="scenario-content">
                <div class="scenario-title">{{ t('masking.scenarioReport') }}</div>
                <div class="scenario-desc">{{ t('masking.scenarioReportDesc') }}</div>
              </div>
            </div>
            <div class="scenario-item">
              <div class="scenario-icon">📤</div>
              <div class="scenario-content">
                <div class="scenario-title">{{ t('masking.scenarioExport') }}</div>
                <div class="scenario-desc">{{ t('masking.scenarioExportDesc') }}</div>
              </div>
            </div>
            <div class="scenario-item">
              <div class="scenario-icon">🔍</div>
              <div class="scenario-content">
                <div class="scenario-title">{{ t('masking.scenarioAudit') }}</div>
                <div class="scenario-desc">{{ t('masking.scenarioAuditDesc') }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加批量字段对话框 -->
    <el-dialog v-model="showAddBatchField" :title="t('masking.addFieldMapping')" width="400px">
      <el-form :model="newBatchField" label-width="80px">
        <el-form-item :label="t('masking.fieldName')">
          <el-input v-model="newBatchField.key" :placeholder="t('masking.fieldNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('masking.maskingType')">
          <el-select v-model="newBatchField.type" :placeholder="t('masking.maskingTypePlaceholder')" style="width: 100%">
            <el-option :label="t('masking.typeIdCard')" value="ID_CARD" />
            <el-option :label="t('masking.typePhone')" value="PHONE" />
            <el-option :label="t('masking.typeBankCard')" value="BANK_CARD" />
            <el-option :label="t('masking.typeEmail')" value="EMAIL" />
            <el-option :label="t('masking.typeName')" value="NAME" />
            <el-option :label="t('masking.typeAddress')" value="ADDRESS" />
            <el-option :label="t('masking.typePassword')" value="PASSWORD" />
            <el-option :label="t('masking.typeAmount')" value="AMOUNT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddBatchField = false">{{ t('masking.cancel') }}</el-button>
        <el-button type="primary" @click="addBatchField">{{ t('masking.add') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Key } from '@element-plus/icons-vue'

const { t } = useI18n()

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
  { typeName: t('masking.typeIdCard'), type: 'ID_CARD', rule: t('masking.ruleIdCard'), original: '110101199001011234', masked: '110****1234' },
  { typeName: t('masking.typePhone'), type: 'PHONE', rule: t('masking.rulePhone'), original: '13812345678', masked: '138****5678' },
  { typeName: t('masking.typeBankCard'), type: 'BANK_CARD', rule: t('masking.ruleBankCard'), original: '6222021234567890123', masked: '***********90123' },
  { typeName: t('masking.typeEmail'), type: 'EMAIL', rule: t('masking.ruleEmail'), original: 'zhangsan@example.com', masked: 'z***san@example.com' },
  { typeName: t('masking.typeName'), type: 'NAME', rule: t('masking.ruleName'), original: '张三', masked: '张*' },
  { typeName: t('masking.typeAddress'), type: 'ADDRESS', rule: t('masking.ruleAddress'), original: '北京市朝阳区XX街道XX号', masked: '北京市朝阳区***' },
  { typeName: t('masking.typePassword'), type: 'PASSWORD', rule: t('masking.rulePassword'), original: 'MyP@ssw0rd!', masked: '**********' },
  { typeName: t('masking.typeAmount'), type: 'AMOUNT', rule: t('masking.ruleAmount'), original: '12345.67', masked: '1*****.67' },
]

const getPlaceholder = () => {
  const placeholders = {
    ID_CARD: t('masking.placeholderIdCard'),
    PHONE: t('masking.placeholderPhone'),
    BANK_CARD: t('masking.placeholderBankCard'),
    EMAIL: t('masking.placeholderEmail'),
    NAME: t('masking.placeholderName'),
    ADDRESS: t('masking.placeholderAddress'),
    PASSWORD: t('masking.placeholderPassword'),
    AMOUNT: t('masking.placeholderAmount'),
  }
  return placeholders[maskForm.type] || t('masking.placeholderInput')
}

const maskData = async () => {
  if (!maskForm.data.trim()) {
    ElMessage.warning(t('masking.inputRequired'))
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
      ElMessage.success(t('masking.maskSuccess'))
    } else {
      ElMessage.error(data.message || t('masking.maskFailed'))
    }
  } catch (e) {
    ElMessage.error(t('masking.maskFailed'))
  }
}

const copyResult = (text) => {
  navigator.clipboard.writeText(text)
  ElMessage.success(t('masking.copied'))
}

const addBatchField = () => {
  if (!newBatchField.key || !newBatchField.type) {
    ElMessage.warning(t('masking.fillComplete'))
    return
  }
  if (batchFields.value.find(f => f.key === newBatchField.key)) {
    ElMessage.warning(t('masking.fieldExists'))
    return
  }
  batchFields.value.push({ ...newBatchField })
  newBatchField.key = ''
  newBatchField.type = 'ID_CARD'
  showAddBatchField.value = false
  ElMessage.success(t('masking.added'))
}

const removeBatchField = (key) => {
  batchFields.value = batchFields.value.filter(f => f.key !== key)
}

const batchMask = async () => {
  if (!batchData.value.trim()) {
    ElMessage.warning(t('masking.inputBatchData'))
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
      ElMessage.success(t('masking.batchMaskSuccess'))
    }
  } catch (e) {
    ElMessage.error(t('masking.batchMaskFailed'))
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
