<template>
  <div class="ai-page">
    <div class="page-header">
      <h2>{{ t('ai.title') }}</h2>
      <p class="page-desc">{{ t('ai.subtitle') }}</p>
    </div>

    <!-- AI能力卡片 -->
    <div class="capabilities-grid">
      <div class="capability-card" @click="activeTab = 'ocr'">
        <div class="cap-icon ocr">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-4 6H9V7h6v2zm-4 4H9v-2h6v2zm4 4H9v-2h6v2z"/>
          </svg>
        </div>
        <h3>{{ t('ai.ocr') }}</h3>
        <p>{{ t('ai.ocrDesc') }}</p>
        <div class="cap-tags">
          <el-tag size="small">{{ t('ai.ocrTags').split(',')[0] }}</el-tag>
          <el-tag size="small">{{ t('ai.ocrTags').split(',')[1] }}</el-tag>
          <el-tag size="small">{{ t('ai.ocrTags').split(',')[2] }}</el-tag>
        </div>
      </div>

      <div class="capability-card" @click="activeTab = 'table'">
        <div class="cap-icon table">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
            <path d="M3 3v18h18V3H3zm8 16H5v-6h6v6zm0-8H5V5h6v6zm8 8h-6v-6h6v6zm0-8h-6V5h6v6z"/>
          </svg>
        </div>
        <h3>{{ t('ai.table') }}</h3>
        <p>{{ t('ai.tableDesc') }}</p>
        <div class="cap-tags">
          <el-tag size="small">{{ t('ai.tableTags').split(',')[0] }}</el-tag>
          <el-tag size="small">{{ t('ai.tableTags').split(',')[1] }}</el-tag>
        </div>
      </div>

      <div class="capability-card" @click="activeTab = 'document'">
        <div class="cap-icon document">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
            <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
          </svg>
        </div>
        <h3>{{ t('ai.document') }}</h3>
        <p>{{ t('ai.documentDesc') }}</p>
        <div class="cap-tags">
          <el-tag size="small">{{ t('ai.documentTags').split(',')[0] }}</el-tag>
          <el-tag size="small">{{ t('ai.documentTags').split(',')[1] }}</el-tag>
        </div>
      </div>

      <div class="capability-card" @click="activeTab = 'barcode'">
        <div class="cap-icon barcode">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
            <path d="M2 6h2v12H2V6zm3 0h1v12H5V6zm3 0h1v12H8V6zm3 0h2v12h-2V6zm3 0h1v12h-1V6zm3 0h1v12h-1V6zm3 0h2v12h-2V6z"/>
          </svg>
        </div>
        <h3>{{ t('ai.barcode') }}</h3>
        <p>{{ t('ai.barcodeDesc') }}</p>
        <div class="cap-tags">
          <el-tag size="small">{{ t('ai.barcodeTags').split(',')[0] }}</el-tag>
          <el-tag size="small">{{ t('ai.barcodeTags').split(',')[1] }}</el-tag>
        </div>
      </div>
    </div>

    <!-- 功能区域 -->
    <el-tabs v-model="activeTab" class="feature-tabs">
      <!-- OCR识别 -->
      <el-tab-pane :label="t('ai.textRecognition')" name="ocr">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>
                  <span>{{ t('ai.uploadImage') }}</span>
                </template>
                <el-upload
                  class="upload-demo"
                  drag
                  :action="apiBase + '/ai/ocr'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="handleOcrSuccess"
                  :on-error="handleOcrError"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">{{ t('ai.dragOrClick') }}</div>
                  <template #tip>
                    <div class="el-upload__tip">{{ t('ai.supportFormat') }}</div>
                  </template>
                </el-upload>

                <el-form :model="ocrForm" label-width="100px" style="margin-top: 20px">
                  <el-form-item :label="t('ai.recognitionLang')">
                    <el-select v-model="ocrForm.language" style="width: 100%">
                      <el-option :label="t('ai.simplifiedChinese')" value="chi_sim+eng" />
                      <el-option :label="t('ai.traditionalChinese')" value="chi_tra+eng" />
                      <el-option :label="t('ai.english')" value="eng" />
                      <el-option :label="t('ai.japanese')" value="jpn" />
                      <el-option :label="t('ai.korean')" value="kor" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>
                  <span>{{ t('ai.recognitionResult') }}</span>
                </template>
                <div v-if="ocrResult" class="result-content">
                  <el-input
                    v-model="ocrResult.text"
                    type="textarea"
                    :rows="10"
                    :placeholder="t('ai.resultPlaceholder')"
                  />
                  <div class="result-meta">
                    <el-tag type="success">{{ t('ai.confidence') }}: {{ (ocrResult.confidence * 100).toFixed(1) }}%</el-tag>
                    <el-tag>{{ t('ai.duration') }}: {{ ocrResult.duration }}ms</el-tag>
                  </div>
                  <el-button type="primary" @click="copyResult(ocrResult.text)">{{ t('ai.copyResult') }}</el-button>
                </div>
                <div v-else class="empty-result">
                  <el-empty :description="t('ai.noResult')" />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 身份证识别 -->
      <el-tab-pane :label="t('ai.idCard')" name="idcard">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card shadow="hover">
                <template #header>{{ t('ai.uploadFront') }}</template>
                <el-upload
                  class="upload-demo"
                  :http-request="uploadIdCard('front')"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">{{ t('ai.uploadFront') }}</div>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover">
                <template #header>{{ t('ai.uploadBack') }}</template>
                <el-upload
                  class="upload-demo"
                  :http-request="uploadIdCard('back')"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">{{ t('ai.uploadBack') }}</div>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover">
                <template #header>{{ t('ai.idCardResult') }}</template>
                <div v-if="idCardResult" class="idcard-result">
                  <div class="result-item">
                    <span class="label">{{ t('ai.name') }}:</span>
                    <span class="value">{{ idCardResult.name }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">{{ t('ai.gender') }}:</span>
                    <span class="value">{{ idCardResult.gender }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">{{ t('ai.ethnicity') }}:</span>
                    <span class="value">{{ idCardResult.ethnicity }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">{{ t('ai.birthDate') }}:</span>
                    <span class="value">{{ idCardResult.birthDate }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">{{ t('ai.idNumber') }}:</span>
                    <span class="value masked">{{ idCardResult.idNumber }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">{{ t('ai.address') }}:</span>
                    <span class="value">{{ idCardResult.address }}</span>
                  </div>
                  <div v-if="idCardResult.issueAuthority" class="result-item">
                    <span class="label">{{ t('ai.issueAuthority') }}:</span>
                    <span class="value">{{ idCardResult.issueAuthority }}</span>
                  </div>
                  <div v-if="idCardResult.validDate" class="result-item">
                    <span class="label">{{ t('ai.validDate') }}:</span>
                    <span class="value">{{ idCardResult.validDate }}</span>
                  </div>

                  <!-- 一键复制按钮 -->
                  <div class="copy-actions">
                    <el-button type="primary" plain size="small" @click="copyIdCardResult">
                      {{ t('ai.copyAll') }}
                    </el-button>
                    <span v-if="copySuccess" class="copy-tip">{{ t('ai.copied') }}</span>
                  </div>
                </div>
                <el-empty v-else :description="t('ai.noResult')" />
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 发票识别 -->
      <el-tab-pane :label="t('ai.invoiceRecognition')" name="invoice">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>{{ t('ai.uploadInvoice') }}</template>
                <el-upload
                  class="upload-demo"
                  :action="apiBase + '/ai/invoice'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="handleInvoiceSuccess"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">{{ t('ai.uploadInvoice') }}</div>
                  <template #tip>
                    <div class="el-upload__tip">{{ t('ai.uploadInvoiceTip') }}</div>
                  </template>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>{{ t('ai.invoiceInfo') }}</template>
                <div v-if="invoiceResult" class="invoice-result">
                  <el-descriptions :column="2" border>
                    <el-descriptions-item :label="t('ai.invoiceCode')">{{ invoiceResult.invoiceCode }}</el-descriptions-item>
                    <el-descriptions-item :label="t('ai.invoiceNumber')">{{ invoiceResult.invoiceNumber }}</el-descriptions-item>
                    <el-descriptions-item :label="t('ai.invoiceDate2')">{{ invoiceResult.invoiceDate }}</el-descriptions-item>
                    <el-descriptions-item :label="t('ai.invoiceType2')">
                      <el-tag size="small">{{ invoiceResult.invoiceType === 'vat_special' ? t('ai.vatinvoice') : t('ai.normalInvoice') }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item :label="t('ai.buyer')">{{ invoiceResult.buyerName }}</el-descriptions-item>
                    <el-descriptions-item :label="t('ai.seller')">{{ invoiceResult.sellerName }}</el-descriptions-item>
                    <el-descriptions-item :label="t('ai.amount')">
                      <span class="amount">{{ invoiceResult.invoiceAmount }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item :label="t('ai.taxAmount')">{{ invoiceResult.taxAmount }}</el-descriptions-item>
                  </el-descriptions>
                </div>
                <el-empty v-else :description="t('ai.noResult')" />
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 表格识别 -->
      <el-tab-pane :label="t('ai.tableRecognition')" name="table">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>{{ t('ai.uploadTable') }}</template>
                <el-upload
                  class="upload-demo"
                  :action="apiBase + '/ai/table'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="handleTableSuccess"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">{{ t('ai.uploadTable') }}</div>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>{{ t('ai.tableData') }}</template>
                <div v-if="tableResult && tableResult.data?.data" class="table-result">
                  <el-table :data="tableResult.data.data" border stripe max-height="400">
                    <el-table-column v-for="(val, key) in tableResult.data.data[0]" :key="key" :prop="key" :label="key" />
                  </el-table>
                  <div class="table-meta">{{ t('ai.rows') }}: {{ tableResult.data.data.length }}</div>
                </div>
                <el-empty v-else :description="t('ai.noResult')" />
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 文档处理 -->
      <el-tab-pane :label="t('ai.docProcessing')" name="document">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>{{ t('ai.uploadDoc') }}</template>
                <el-upload
                  class="upload-demo"
                  :action="apiBase + '/ai/document'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="handleDocumentSuccess"
                  :show-file-list="false"
                  accept=".pdf,.doc,.docx"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">{{ t('ai.uploadDoc') }}</div>
                  <template #tip>
                    <div class="el-upload__tip">{{ t('ai.docTip') }}</div>
                  </template>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>{{ t('ai.docContent') }}</template>
                <div v-if="documentResult" class="document-result">
                  <el-input
                    v-model="documentResult.text"
                    type="textarea"
                    :rows="10"
                    :placeholder="t('ai.resultPlaceholder')"
                  />
                  <div class="result-meta">
                    <el-tag type="success">{{ t('ai.pages') }}: {{ documentResult.pages || 1 }}</el-tag>
                    <el-tag>{{ t('ai.duration') }}: {{ documentResult.duration || 0 }}ms</el-tag>
                  </div>
                  <el-button type="primary" @click="copyResult(documentResult.text)">{{ t('ai.copyContent') }}</el-button>
                </div>
                <div v-else class="empty-result">
                  <el-empty :description="t('ai.noDocContent')" />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 条码识别 -->
      <el-tab-pane :label="t('ai.barcodeRecognition')" name="barcode">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>{{ t('ai.uploadBarcode') }}</template>
                <el-upload
                  class="upload-demo"
                  :action="apiBase + '/ai/barcode'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="handleBarcodeSuccess"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">{{ t('ai.uploadBarcode') }}</div>
                  <template #tip>
                    <div class="el-upload__tip">{{ t('ai.barcodeTip') }}</div>
                  </template>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>{{ t('ai.recognitionResult') }}</template>
                <div v-if="barcodeResult" class="barcode-result">
                  <div class="barcode-value">
                    <span class="label">{{ t('ai.result') }}:</span>
                    <span class="value">{{ barcodeResult.text }}</span>
                    <el-button type="primary" size="small" @click="copyResult(barcodeResult.text)">{{ t('ai.copyResult') }}</el-button>
                  </div>
                  <div class="barcode-type">
                    <el-tag>{{ t('ai.format') }}: {{ barcodeResult.format || t('ai.unknown') }}</el-tag>
                  </div>
                  <div class="barcode-confidence">
                    <span>{{ t('ai.confidence') }}: </span>
                    <el-progress :percentage="(barcodeResult.confidence * 100) || 100" :color="getConfidenceColor(barcodeResult.confidence || 1)" />
                  </div>
                </div>
                <el-empty v-else :description="t('ai.noResult')" />
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { CopyDocument } from '@element-plus/icons-vue'

const { t } = useI18n()

const apiBase = '/api'
const token = localStorage.getItem('token') || ''

const activeTab = ref('ocr')

// OCR
const ocrForm = reactive({ language: 'chi_sim+eng' })
const ocrResult = ref(null)

const handleOcrSuccess = (res) => {
  if (res.code === 0) {
    ocrResult.value = res.data
    ElMessage.success(t('ai.recognition'))
  } else {
    ElMessage.error(res.message || t('ai.uploadFailed'))
  }
}

const handleOcrError = () => {
  ElMessage.error(t('ai.uploadFailed'))
}

// 身份证
const idCardResult = ref(null)
const copySuccess = ref(false)
const idCardUploadRef = ref(null)
const idCardSide = ref('front')

// 身份证上传方法
const uploadIdCard = (side) => {
  return async (options) => {
    const { file, onSuccess, onError } = options
    idCardSide.value = side
    
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('side', side)
      
      const response = await fetch(apiBase + '/ai/idcard', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token },
        body: formData
      })
      
      const res = await response.json()
      console.log('身份证识别响应:', res)
      
      if (res.code === 0 || res.success) {
        // 从res中直接获取身份证字段（后端已平铺到根级别）
        if (side === 'front') {
          idCardResult.value = {
            name: res.name || '',
            gender: res.gender || '',
            ethnicity: res.ethnicity || '',
            birthDate: res.birthDate || '',
            idNumber: res.idNumber || '',
            address: res.address || '',
            side: 'front'
          }
          console.log('身份证正面识别结果:', idCardResult.value)
        } else {
          idCardResult.value = {
            ...idCardResult.value,
            issueAuthority: res.issueAuthority || '',
            validDate: res.validDate || '',
            side: 'back'
          }
          console.log('身份证背面识别结果:', idCardResult.value)
        }
        ElMessage.success(t('ai.recognition'))
        onSuccess(res)
      } else {
        ElMessage.error(res.message || res.errorMessage || t('ai.uploadFailed'))
        onError(new Error(res.message || t('ai.uploadFailed')))
      }
    } catch (e) {
      console.error('上传失败:', e)
      ElMessage.error(t('ai.uploadFailed') + ': ' + e.message)
      onError(e)
    }
  }
}

// 一键复制身份证结果
const copyIdCardResult = async () => {
  if (!idCardResult.value) return
  const info = idCardResult.value
  const text = [
    `姓名: ${info.name || '-'}`,
    `性别: ${info.gender || '-'}`,
    `民族: ${info.ethnicity || '-'}`,
    `出生日期: ${info.birthDate || '-'}`,
    `身份证号: ${info.idNumber || '-'}`,
    `地址: ${info.address || '-'}`,
    info.issueAuthority ? `签发机关: ${info.issueAuthority}` : null,
    info.validDate ? `有效期: ${info.validDate}` : null
  ].filter(Boolean).join('\n')

  try {
    await navigator.clipboard.writeText(text)
    copySuccess.value = true
    ElMessage.success(t('ai.copied'))
    setTimeout(() => { copySuccess.value = false }, 2000)
  } catch (e) {
    ElMessage.error(t('collaboration.copyFailed'))
  }
}

// 发票
const invoiceResult = ref(null)
const handleInvoiceSuccess = (res) => {
  if (res.code === 0) {
    invoiceResult.value = res.data
    ElMessage.success(t('ai.recognition'))
  }
}

// 验证码
const captchaResult = ref(null)
const handleCaptchaSuccess = (res) => {
  if (res.code === 0) {
    captchaResult.value = res.data
    ElMessage.success(t('ai.recognition'))
  }
}

// 表格
const tableResult = ref(null)
const handleTableSuccess = (res) => {
  if (res.code === 0) {
    tableResult.value = res.data
    ElMessage.success(t('ai.recognition'))
  }
}

// 文档处理
const documentResult = ref(null)
const handleDocumentSuccess = (res) => {
  if (res.code === 0) {
    documentResult.value = res.data
    ElMessage.success(t('ai.recognition'))
  }
}

// 条码识别
const barcodeResult = ref(null)
const handleBarcodeSuccess = (res) => {
  if (res.code === 0) {
    barcodeResult.value = res.data
    ElMessage.success(t('ai.recognition'))
  }
}

// NLP
const nlpType = ref('sentiment')
const nlpInput = ref('')
const nlpLoading = ref(false)
const nlpResult = ref(null)

const getNlpPlaceholder = () => {
  const placeholders = {
    sentiment: '请输入要分析的文本...',
    translate: '请输入要翻译的文本...',
    entity: '请输入要提取实体的文本...',
    keyword: '请输入要提取关键词的文本...',
    classify: '请输入要分类的文本...'
  }
  return placeholders[nlpType.value]
}

const processNlp = async () => {
  if (!nlpInput.value.trim()) {
    ElMessage.warning('请输入文本内容')
    return
  }
  nlpLoading.value = true
  try {
    const endpoint = `/api/ai/${nlpType.value === 'sentiment' ? 'sentiment' : nlpType.value}`
    let requestBody = {}
    
    if (nlpType.value === 'translate') {
      requestBody = { text: nlpInput.value, from: 'auto', to: 'zh' }
    } else if (nlpType.value === 'keyword') {
      requestBody = { text: nlpInput.value, topN: 10 }
    } else {
      requestBody = { text: nlpInput.value }
    }
    
    const res = await fetch(apiBase + endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(requestBody)
    })
    const data = await res.json()
    if (data.code === 0) {
      nlpResult.value = data.data
      ElMessage.success('处理成功')
    }
  } catch (e) {
    ElMessage.error('处理失败')
  } finally {
    nlpLoading.value = false
  }
}

// 工具方法
const copyResult = (text) => {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

const getConfidenceColor = (confidence) => {
  if (confidence > 0.8) return '#67c23a'
  if (confidence > 0.5) return '#e6a23c'
  return '#f56c6c'
}

const getSentimentType = (sentiment) => {
  if (sentiment === 'positive') return 'success'
  if (sentiment === 'negative') return 'danger'
  return 'info'
}

const getSentimentText = (sentiment) => {
  if (sentiment === 'positive') return t('ai.positive')
  if (sentiment === 'negative') return t('ai.negative')
  return t('ai.neutral')
}
</script>

<style scoped>
.ai-page {
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

.capabilities-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.capability-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #eee;
}

.capability-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 20px rgba(0,0,0,0.1);
  border-color: #409eff;
}

.cap-icon {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 15px;
}

.cap-icon.ocr { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; }
.cap-icon.table { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); color: #fff; }
.cap-icon.document { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); color: #fff; }
.cap-icon.barcode { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); color: #fff; }

.capability-card h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.capability-card p {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 13px;
}

.cap-tags {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.feature-tabs {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.feature-panel {
  padding: 10px 0;
}

.el-icon--upload {
  font-size: 67px;
  color: #409eff;
  margin-bottom: 10px;
}

.empty-result {
  padding: 40px 0;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.result-meta {
  display: flex;
  gap: 10px;
}

.idcard-result {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result-item {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.result-item .label {
  width: 100px;
  color: #666;
}

.result-item .value {
  flex: 1;
  font-weight: 500;
}

.result-item .value.masked {
  font-family: monospace;
  color: #f56c6c;
}

.copy-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.copy-tip {
  color: #67c23a;
  font-size: 12px;
}

.invoice-result .amount {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.captcha-result {
  text-align: center;
}

.captcha-value {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  margin-bottom: 20px;
}

.captcha-value .value {
  font-size: 32px;
  font-weight: bold;
  font-family: monospace;
  letter-spacing: 5px;
}

.nlp-result {
  padding: 10px;
}

.sentiment-result {
  text-align: center;
}

.sentiment-score {
  margin-top: 15px;
  font-size: 16px;
}

.translate-result {
  text-align: center;
}

.translated-text {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 15px;
  min-height: 100px;
}

.entity-result, .keyword-result {
  display: flex;
  flex-wrap: wrap;
}

.classify-result {
  text-align: center;
}

.classify-result .category {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

.table-result {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.table-meta {
  text-align: right;
  color: #666;
  font-size: 13px;
}
</style>