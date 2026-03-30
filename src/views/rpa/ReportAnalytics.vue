<template>
  <div class="report-page">
    <div class="page-header">
      <h2>报表与分析中心</h2>
      <p class="page-desc">评估RPA投资回报率和运营效率</p>
    </div>

    <!-- 统计概览 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon primary"><el-icon><DataLine /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.totalTasks }}</span>
          <span class="stat-label">总执行任务</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon success"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.successRate }}%</span>
          <span class="stat-label">任务成功率</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon warning"><el-icon><Timer /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.avgDuration }}h</span>
          <span class="stat-label">平均运行时长</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon danger"><el-icon><Money /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.savedHours }}</span>
          <span class="stat-label">节省工时(小时)</span>
        </div>
      </div>
    </div>

    <el-tabs v-model="currentTab" class="report-tabs">
      <!-- 预置报表 -->
      <el-tab-pane label="预置报表" name="preset">
        <div class="report-grid">
          <div class="report-card" v-for="report in presetReports" :key="report.id" @click="viewReport(report)">
            <div class="report-icon" :style="{ background: report.color }">
              <el-icon><component :is="report.icon" /></el-icon>
            </div>
            <div class="report-info">
              <h4>{{ report.name }}</h4>
              <p>{{ report.description }}</p>
            </div>
            <el-button link type="primary">查看报表</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- 自定义报表 -->
      <el-tab-pane label="自定义报表" name="custom">
        <div class="toolbar">
          <el-button type="primary" @click="createCustomReport">
            <el-icon><Plus /></el-icon> 创建报表
          </el-button>
        </div>

        <el-table :data="customReports" v-loading="loading" border stripe>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="name" label="报表名称" min-width="150" />
          <el-table-column prop="type" label="报表类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createUser" label="创建人" width="100" />
          <el-table-column prop="createTime" label="创建时间" min-width="160" />
          <el-table-column prop="lastRun" label="最后运行" min-width="160" />
          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="runReport(row)">运行</el-button>
              <el-button link type="primary" @click="editCustomReport(row)">编辑</el-button>
              <el-popconfirm title="确认删除该报表吗？" @confirm="deleteCustomReport(row)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 成本节省计算 -->
      <el-tab-pane label="成本节省" name="cost">
        <div class="cost-section">
          <el-card class="cost-card">
            <template #header>
              <div class="card-header">
                <span>成本节省计算器</span>
              </div>
            </template>
            <el-form :model="costForm" label-width="120px">
              <el-form-item label="人工执行耗时">
                <el-input-number v-model="costForm.manualTime" :min="1" :max="1000" />
                <span class="unit">分钟/次</span>
              </el-form-item>
              <el-form-item label="执行频率">
                <el-input-number v-model="costForm.frequency" :min="1" :max="10000" />
                <span class="unit">次/月</span>
              </el-form-item>
              <el-form-item label="人工时薪">
                <el-input-number v-model="costForm.hourlyRate" :min="1" :max="1000" />
                <span class="unit">元/小时</span>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="calculateSavings">计算节省</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card class="result-card" v-if="showResult">
            <template #header>
              <div class="card-header">
                <span>节省估算结果</span>
              </div>
            </template>
            <div class="savings-result">
              <div class="savings-item">
                <span class="savings-label">每月节省工时:</span>
                <span class="savings-value primary">{{ savings.monthlyHours }} 小时</span>
              </div>
              <div class="savings-item">
                <span class="savings-label">每月节省成本:</span>
                <span class="savings-value success">¥ {{ savings.monthlyCost }}</span>
              </div>
              <div class="savings-item">
                <span class="savings-label">每年节省工时:</span>
                <span class="savings-value primary">{{ savings.yearlyHours }} 小时</span>
              </div>
              <div class="savings-item">
                <span class="savings-label">每年节省成本:</span>
                <span class="savings-value success">¥ {{ savings.yearlyCost }}</span>
              </div>
              <div class="savings-item highlight">
                <span class="savings-label">ROI:</span>
                <span class="savings-value">投资回报率 300%+</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 趋势预测 -->
      <el-tab-pane label="趋势预测" name="forecast">
        <div class="forecast-section">
          <div class="forecast-header">
            <h3>未来任务量预测</h3>
            <el-select v-model="forecastPeriod" style="width: 150px;">
              <el-option label="未来7天" value="7" />
              <el-option label="未来30天" value="30" />
              <el-option label="未来90天" value="90" />
            </el-select>
          </div>
          <div class="forecast-chart">
            <v-chart :option="forecastChartOption" autoresize style="height: 300px;"></v-chart>
          </div>
          <div class="forecast-tips">
            <el-alert type="info" :closable="false">
              <template #title>
                <strong>智能建议:</strong> 基于历史数据分析，预计下周任务量将增长15%。建议扩容2台机器人以确保服务质量。
              </template>
            </el-alert>
          </div>
        </div>
      </el-tab-pane>

      <!-- 报表订阅 -->
      <el-tab-pane label="报表订阅" name="subscription">
        <div class="toolbar">
          <el-button type="primary" @click="addSubscription">
            <el-icon><Plus /></el-icon> 添加订阅
          </el-button>
        </div>

        <el-table :data="subscriptions" v-loading="subLoading" border stripe>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="name" label="订阅名称" min-width="150" />
          <el-table-column prop="report" label="报表" min-width="120" />
          <el-table-column prop="frequency" label="发送频率" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.frequency }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="recipients" label="接收人" min-width="150" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.status" @change="toggleSubscription(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="lastRun" label="最后发送" min-width="160" />
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="editSubscription(row)">编辑</el-button>
              <el-popconfirm title="确认删除吗？" @confirm="deleteSubscription(row)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建自定义报表弹窗 -->
    <el-dialog v-model="reportDialogVisible" title="创建自定义报表" width="600px">
      <el-form :model="reportForm" label-width="100px">
        <el-form-item label="报表名称">
          <el-input v-model="reportForm.name" placeholder="请输入报表名称" />
        </el-form-item>
        <el-form-item label="报表类型">
          <el-select v-model="reportForm.type" style="width: 100%;">
            <el-option label="任务执行报表" value="任务执行" />
            <el-option label="机器人利用率" value="机器人利用率" />
            <el-option label="流程耗时排行" value="流程耗时" />
            <el-option label="自定义" value="自定义" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-select v-model="reportForm.dateRange" style="width: 100%;">
            <el-option label="今日" value="today" />
            <el-option label="本周" value="week" />
            <el-option label="本月" value="month" />
            <el-option label="本季度" value="quarter" />
            <el-option label="本年" value="year" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="图表类型">
          <el-select v-model="reportForm.chartType" style="width: 100%;">
            <el-option label="折线图" value="line" />
            <el-option label="柱状图" value="bar" />
            <el-option label="饼图" value="pie" />
            <el-option label="表格" value="table" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="reportForm.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCustomReport">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DataLine, CircleCheck, Timer, Money, Plus, TrendCharts, PieChart, List, Histogram } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart as EChartsBarChart, PieChart as PieChartType } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, EChartsBarChart, PieChartType, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const currentTab = ref('preset')
const loading = ref(false)
const subLoading = ref(false)
const reportDialogVisible = ref(false)
const showResult = ref(false)
const forecastPeriod = ref('7')

const stats = reactive({
  totalTasks: 12580,
  successRate: 94.5,
  avgDuration: 2.3,
  savedHours: 2560
})

const presetReports = [
  { id: 1, name: '任务执行日报', description: '每日任务成功/失败数量统计', icon: 'TrendCharts', color: '#409eff' },
  { id: 2, name: '机器人利用率', description: '机器人空闲/忙碌时间占比', icon: 'PieChart', color: '#67c23a' },
  { id: 3, name: '流程耗时排行', description: '执行耗时最长的流程TOP10', icon: 'Histogram', color: '#e6a23c' },
  { id: 4, name: '任务执行月报', description: '月度任务执行汇总分析', icon: 'List', color: '#f56c6c' }
]

const customReports = ref([
  { id: 1, name: '财务流程报表', type: '任务执行', createUser: 'admin', createTime: '2026-03-15 10:00:00', lastRun: '2026-03-30 09:00:00' },
  { id: 2, name: 'HR流程报表', type: '机器人利用率', createUser: 'admin', createTime: '2026-03-10 14:30:00', lastRun: '2026-03-29 18:00:00' }
])

const subscriptions = ref([
  { id: 1, name: '每日任务日报', report: '任务执行日报', frequency: '每日', recipients: 'admin@company.com', status: true, lastRun: '2026-03-30 09:00:00' },
  { id: 2, name: '每周汇总', report: '任务执行周报', frequency: '每周', recipients: 'leader@company.com', status: true, lastRun: '2026-03-24 09:00:00' }
])

const costForm = reactive({
  manualTime: 30,
  frequency: 100,
  hourlyRate: 100
})

const savings = reactive({
  monthlyHours: 0,
  monthlyCost: 0,
  yearlyHours: 0,
  yearlyCost: 0
})

const reportForm = reactive({
  name: '',
  type: '任务执行',
  dateRange: 'month',
  chartType: 'line',
  description: ''
})

const forecastChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['历史数据', '预测值'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日', '周一(预测)', '周二(预测)', '周三(预测)', '周四(预测)', '周五(预测)']
  },
  yAxis: { type: 'value', name: '任务数' },
  series: [
    {
      name: '历史数据',
      type: 'line',
      data: [120, 132, 101, 134, 190, 230, 210],
      itemStyle: { color: '#409eff' }
    },
    {
      name: '预测值',
      type: 'line',
      data: [210, 230, 250, 280, 260, 290, 310, null, null, null, null, null],
      itemStyle: { color: '#67c23a' },
      lineStyle: { type: 'dashed' }
    }
  ]
}))

const viewReport = (report) => {
  ElMessage.info(`正在加载报表: ${report.name}`)
}

const createCustomReport = () => {
  Object.assign(reportForm, { name: '', type: '任务执行', dateRange: 'month', chartType: 'line', description: '' })
  reportDialogVisible.value = true
}

const editCustomReport = (report) => {
  ElMessage.info(`编辑报表: ${report.name}`)
}

const deleteCustomReport = (report) => {
  const index = customReports.value.findIndex(r => r.id === report.id)
  if (index !== -1) {
    customReports.value.splice(index, 1)
    ElMessage.success('报表删除成功')
  }
}

const runReport = (report) => {
  ElMessage.success(`报表 "${report.name}" 运行成功`)
}

const calculateSavings = () => {
  const monthlyManualHours = (costForm.manualTime * costForm.frequency) / 60
  const monthlyCost = monthlyManualHours * costForm.hourlyRate
  
  savings.monthlyHours = Math.round(monthlyManualHours * 100) / 100
  savings.monthlyCost = Math.round(monthlyCost * 100) / 100
  savings.yearlyHours = Math.round(savings.monthlyHours * 12 * 100) / 100
  savings.yearlyCost = Math.round(savings.monthlyCost * 12 * 100) / 100
  showResult.value = true
}

const addSubscription = () => {
  ElMessage.info('添加订阅功能')
}

const editSubscription = (sub) => {
  ElMessage.info(`编辑订阅: ${sub.name}`)
}

const deleteSubscription = (sub) => {
  const index = subscriptions.value.findIndex(s => s.id === sub.id)
  if (index !== -1) {
    subscriptions.value.splice(index, 1)
    ElMessage.success('订阅删除成功')
  }
}

const toggleSubscription = (sub) => {
  ElMessage.success(`订阅${sub.status ? '启用' : '禁用'}成功`)
}

const saveCustomReport = () => {
  customReports.value.push({
    id: Date.now(),
    ...reportForm,
    createUser: 'admin',
    createTime: new Date().toLocaleString(),
    lastRun: '从未运行'
  })
  reportDialogVisible.value = false
  ElMessage.success('报表创建成功')
}

onMounted(() => {})
</script>

<style scoped>
.report-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}

.stat-icon.primary { background: linear-gradient(135deg, #409eff, #66b1ff); }
.stat-icon.success { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-icon.warning { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.stat-icon.danger { background: linear-gradient(135deg, #f56c6c, #f78989); }

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #1e3a4a;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
}

.report-tabs :deep(.el-tabs__header) { margin-bottom: 20px; }

.report-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.report-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.report-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.report-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.report-info {
  flex: 1;
}

.report-info h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1e3a4a;
  margin-bottom: 4px;
}

.report-info p {
  font-size: 13px;
  color: #8c8c8c;
}

.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }

.cost-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.cost-card, .result-card {
  background: white;
  border-radius: 12px;
}

.unit {
  margin-left: 8px;
  color: #8c8c8c;
}

.savings-result {
  display: grid;
  gap: 16px;
}

.savings-item {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.savings-item.highlight {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.savings-label {
  color: #8c8c8c;
}

.savings-value {
  font-weight: 600;
  font-size: 18px;
}

.savings-value.primary { color: #409eff; }
.savings-value.success { color: #67c23a; }

.forecast-section {
  background: white;
  padding: 24px;
  border-radius: 12px;
}

.forecast-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.forecast-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e3a4a;
}

.forecast-chart {
  margin-bottom: 20px;
}

.forecast-tips {
  margin-top: 20px;
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .report-grid { grid-template-columns: 1fr; }
  .cost-section { grid-template-columns: 1fr; }
}
</style>