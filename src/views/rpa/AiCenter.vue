<template>
  <div class="ai-page">
    <div class="page-header">
      <h2>AI 能力中心</h2>
      <p class="page-desc">内置OCR识别、NLP处理等AI能力，提升RPA自动化效率</p>
    </div>

    <!-- AI能力卡片 -->
    <div class="capabilities-grid">
      <div class="capability-card" @click="activeTab = 'ocr'">
        <div class="cap-icon ocr">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-4 6H9V7h6v2zm-4 4H9v-2h6v2zm4 4H9v-2h6v2z"/>
          </svg>
        </div>
        <h3>文字识别 (OCR)</h3>
        <p>通用文字识别，支持多种语言和字体</p>
        <div class="cap-tags">
          <el-tag size="small">身份证</el-tag>
          <el-tag size="small">发票</el-tag>
          <el-tag size="small">银行卡</el-tag>
        </div>
      </div>

      <div class="capability-card" @click="activeTab = 'captcha'">
        <div class="cap-icon captcha">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
          </svg>
        </div>
        <h3>验证码识别</h3>
        <p>图片验证码、滑块验证、点选验证</p>
        <div class="cap-tags">
          <el-tag size="small">数字</el-tag>
          <el-tag size="small">字母</el-tag>
          <el-tag size="small">滑块</el-tag>
        </div>
      </div>

      <div class="capability-card" @click="activeTab = 'nlp'">
        <div class="cap-icon nlp">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
            <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
          </svg>
        </div>
        <h3>NLP 文本处理</h3>
        <p>情感分析、实体识别、关键词提取</p>
        <div class="cap-tags">
          <el-tag size="small">情感</el-tag>
          <el-tag size="small">翻译</el-tag>
          <el-tag size="small">分类</el-tag>
        </div>
      </div>

      <div class="capability-card" @click="activeTab = 'table'">
        <div class="cap-icon table">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
            <path d="M3 3v18h18V3H3zm8 16H5v-6h6v6zm0-8H5V5h6v6zm8 8h-6v-6h6v6zm0-8h-6V5h6v6z"/>
          </svg>
        </div>
        <h3>表格识别</h3>
        <p>从图片中识别表格结构化数据</p>
        <div class="cap-tags">
          <el-tag size="small">结构化</el-tag>
          <el-tag size="small">导出</el-tag>
        </div>
      </div>
    </div>

    <!-- 功能区域 -->
    <el-tabs v-model="activeTab" class="feature-tabs">
      <!-- OCR识别 -->
      <el-tab-pane label="文字识别" name="ocr">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>
                  <span>上传图片</span>
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
                  <div class="el-upload__text">拖拽图片到此处或<em>点击上传</em></div>
                  <template #tip>
                    <div class="el-upload__tip">支持 JPG、PNG 格式，建议图片清晰无遮挡</div>
                  </template>
                </el-upload>

                <el-form :model="ocrForm" label-width="100px" style="margin-top: 20px">
                  <el-form-item label="识别语言">
                    <el-select v-model="ocrForm.language" style="width: 100%">
                      <el-option label="简体中文+英文" value="chi_sim+eng" />
                      <el-option label="繁体中文+英文" value="chi_tra+eng" />
                      <el-option label="纯英文" value="eng" />
                      <el-option label="日文" value="jpn" />
                      <el-option label="韩文" value="kor" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>
                  <span>识别结果</span>
                </template>
                <div v-if="ocrResult" class="result-content">
                  <el-input
                    v-model="ocrResult.text"
                    type="textarea"
                    :rows="10"
                    placeholder="识别结果将显示在这里..."
                  />
                  <div class="result-meta">
                    <el-tag type="success">置信度: {{ (ocrResult.confidence * 100).toFixed(1) }}%</el-tag>
                    <el-tag>耗时: {{ ocrResult.duration }}ms</el-tag>
                  </div>
                  <el-button type="primary" @click="copyResult(ocrResult.text)">复制结果</el-button>
                </div>
                <div v-else class="empty-result">
                  <el-empty description="暂无识别结果" />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 身份证识别 -->
      <el-tab-pane label="身份证识别" name="idcard">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card shadow="hover">
                <template #header>上传身份证正面</template>
                <el-upload
                  class="upload-demo"
                  :action="apiBase + '/ai/idcard'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="(res) => handleIdCardSuccess(res, 'front')"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">点击上传身份证正面</div>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover">
                <template #header>上传身份证背面</template>
                <el-upload
                  class="upload-demo"
                  :action="apiBase + '/ai/idcard'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="(res) => handleIdCardSuccess(res, 'back')"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">点击上传身份证背面</div>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover">
                <template #header>识别结果</template>
                <div v-if="idCardResult" class="idcard-result">
                  <div class="result-item">
                    <span class="label">姓名:</span>
                    <span class="value">{{ idCardResult.name }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">性别:</span>
                    <span class="value">{{ idCardResult.gender }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">民族:</span>
                    <span class="value">{{ idCardResult.ethnicity }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">出生日期:</span>
                    <span class="value">{{ idCardResult.birthDate }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">身份证号:</span>
                    <span class="value masked">{{ idCardResult.idNumber }}</span>
                  </div>
                  <div class="result-item">
                    <span class="label">地址:</span>
                    <span class="value">{{ idCardResult.address }}</span>
                  </div>
                  <div v-if="idCardResult.issueAuthority" class="result-item">
                    <span class="label">签发机关:</span>
                    <span class="value">{{ idCardResult.issueAuthority }}</span>
                  </div>
                  <div v-if="idCardResult.validDate" class="result-item">
                    <span class="label">有效期:</span>
                    <span class="value">{{ idCardResult.validDate }}</span>
                  </div>
                </div>
                <el-empty v-else description="暂无识别结果" />
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 发票识别 -->
      <el-tab-pane label="发票识别" name="invoice">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>上传发票</template>
                <el-upload
                  class="upload-demo"
                  :action="apiBase + '/ai/invoice'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="handleInvoiceSuccess"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">点击上传发票图片</div>
                  <template #tip>
                    <div class="el-upload__tip">支持增值税发票、电子发票等</div>
                  </template>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>发票信息</template>
                <div v-if="invoiceResult" class="invoice-result">
                  <el-descriptions :column="2" border>
                    <el-descriptions-item label="发票代码">{{ invoiceResult.invoiceCode }}</el-descriptions-item>
                    <el-descriptions-item label="发票号码">{{ invoiceResult.invoiceNumber }}</el-descriptions-item>
                    <el-descriptions-item label="开票日期">{{ invoiceResult.invoiceDate }}</el-descriptions-item>
                    <el-descriptions-item label="发票类型">
                      <el-tag size="small">{{ invoiceResult.invoiceType === 'vat_special' ? '增值税专用发票' : '普通发票' }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="购买方">{{ invoiceResult.buyerName }}</el-descriptions-item>
                    <el-descriptions-item label="销售方">{{ invoiceResult.sellerName }}</el-descriptions-item>
                    <el-descriptions-item label="金额">
                      <span class="amount">{{ invoiceResult.invoiceAmount }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="税额">{{ invoiceResult.taxAmount }}</el-descriptions-item>
                  </el-descriptions>
                </div>
                <el-empty v-else description="暂无识别结果" />
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 验证码识别 -->
      <el-tab-pane label="验证码识别" name="captcha">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>上传验证码</template>
                <el-upload
                  class="upload-demo"
                  :action="apiBase + '/ai/captcha'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="handleCaptchaSuccess"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">点击上传验证码图片</div>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>识别结果</template>
                <div v-if="captchaResult" class="captcha-result">
                  <div class="captcha-value">
                    <span class="label">识别结果:</span>
                    <span class="value">{{ captchaResult.text }}</span>
                    <el-button type="primary" size="small" @click="copyResult(captchaResult.text)">复制</el-button>
                  </div>
                  <div class="captcha-confidence">
                    <span>置信度: </span>
                    <el-progress :percentage="(captchaResult.confidence * 100)" :color="getConfidenceColor(captchaResult.confidence)" />
                  </div>
                </div>
                <el-empty v-else description="暂无识别结果" />
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- NLP处理 -->
      <el-tab-pane label="NLP处理" name="nlp">
        <div class="feature-panel">
          <el-card shadow="hover">
            <template #header>
              <el-radio-group v-model="nlpType" size="small">
                <el-radio-button label="sentiment">情感分析</el-radio-button>
                <el-radio-button label="translate">文本翻译</el-radio-button>
                <el-radio-button label="entity">实体提取</el-radio-button>
                <el-radio-button label="keyword">关键词提取</el-radio-button>
                <el-radio-button label="classify">文本分类</el-radio-button>
              </el-radio-group>
            </template>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-input
                  v-model="nlpInput"
                  type="textarea"
                  :rows="8"
                  :placeholder="getNlpPlaceholder()"
                />
                <el-button type="primary" @click="processNlp" :loading="nlpLoading" style="margin-top: 10px">
                  开始处理
                </el-button>
              </el-col>
              <el-col :span="12">
                <div v-if="nlpResult" class="nlp-result">
                  <!-- 情感分析结果 -->
                  <div v-if="nlpType === 'sentiment'" class="sentiment-result">
                    <el-tag :type="getSentimentType(nlpResult.data?.sentiment)" size="large">
                      {{ getSentimentText(nlpResult.data?.sentiment) }}
                    </el-tag>
                    <div class="sentiment-score">
                      情感得分: {{ nlpResult.data?.score?.toFixed(2) }}
                    </div>
                  </div>

                  <!-- 翻译结果 -->
                  <div v-else-if="nlpType === 'translate'" class="translate-result">
                    <div class="translated-text">{{ nlpResult.text }}</div>
                    <el-button type="primary" size="small" @click="copyResult(nlpResult.text)">复制译文</el-button>
                  </div>

                  <!-- 实体提取结果 -->
                  <div v-else-if="nlpType === 'entity'" class="entity-result">
                    <el-tag v-for="e in nlpResult.data?.entities" :key="e.value" size="small" style="margin: 5px">
                      {{ e.type }}: {{ e.value }}
                    </el-tag>
                  </div>

                  <!-- 关键词提取结果 -->
                  <div v-else-if="nlpType === 'keyword'" class="keyword-result">
                    <el-tag v-for="kw in nlpResult.data?.keywords" :key="kw.word" size="small" style="margin: 5px">
                      {{ kw.word }} ({{ kw.freq }})
                    </el-tag>
                  </div>

                  <!-- 文本分类结果 -->
                  <div v-else-if="nlpType === 'classify'" class="classify-result">
                    <div class="category">类别: {{ nlpResult.data?.category }}</div>
                    <div class="confidence">置信度: {{ (nlpResult.data?.confidence * 100).toFixed(1) }}%</div>
                  </div>
                </div>
                <el-empty v-else description="暂无处理结果" />
              </el-col>
            </el-row>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 表格识别 -->
      <el-tab-pane label="表格识别" name="table">
        <div class="feature-panel">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>上传表格图片</template>
                <el-upload
                  class="upload-demo"
                  :action="apiBase + '/ai/table'"
                  :headers="{ Authorization: 'Bearer ' + token }"
                  :on-success="handleTableSuccess"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">点击上传表格图片</div>
                </el-upload>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>表格数据</template>
                <div v-if="tableResult && tableResult.data?.data" class="table-result">
                  <el-table :data="tableResult.data.data" border stripe max-height="400">
                    <el-table-column v-for="(val, key) in tableResult.data.data[0]" :key="key" :prop="key" :label="key" />
                  </el-table>
                  <div class="table-meta">共 {{ tableResult.data.data.length }} 行数据</div>
                </div>
                <el-empty v-else description="暂无识别结果" />
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
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const apiBase = '/api'
const token = localStorage.getItem('token') || ''

const activeTab = ref('ocr')

// OCR
const ocrForm = reactive({ language: 'chi_sim+eng' })
const ocrResult = ref(null)

const handleOcrSuccess = (res) => {
  if (res.code === 0) {
    ocrResult.value = res.data
    ElMessage.success('识别成功')
  } else {
    ElMessage.error(res.message || '识别失败')
  }
}

const handleOcrError = () => {
  ElMessage.error('上传失败，请重试')
}

// 身份证
const idCardResult = ref(null)
const handleIdCardSuccess = (res, side) => {
  if (res.code === 0) {
    if (side === 'front') {
      idCardResult.value = { ...res.data, side: 'front' }
    } else {
      idCardResult.value = { ...idCardResult.value, ...res.data, side: 'back' }
    }
    ElMessage.success('识别成功')
  }
}

// 发票
const invoiceResult = ref(null)
const handleInvoiceSuccess = (res) => {
  if (res.code === 0) {
    invoiceResult.value = res.data
    ElMessage.success('识别成功')
  }
}

// 验证码
const captchaResult = ref(null)
const handleCaptchaSuccess = (res) => {
  if (res.code === 0) {
    captchaResult.value = res.data
    ElMessage.success('识别成功')
  }
}

// 表格
const tableResult = ref(null)
const handleTableSuccess = (res) => {
  if (res.code === 0) {
    tableResult.value = res.data
    ElMessage.success('识别成功')
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
  if (sentiment === 'positive') return '正面情感'
  if (sentiment === 'negative') return '负面情感'
  return '中性'
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
.cap-icon.captcha { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: #fff; }
.cap-icon.nlp { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: #fff; }
.cap-icon.table { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); color: #fff; }

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