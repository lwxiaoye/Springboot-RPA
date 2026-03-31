<template>
  <div class="audit-page">
    <div class="page-header">
      <h2>审计日志</h2>
      <p class="page-desc">记录和追溯所有系统操作行为</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-box">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">总操作数</span>
      </div>
      <div class="stat-box warning">
        <span class="stat-num">{{ stats.today }}</span>
        <span class="stat-label">今日操作</span>
      </div>
      <div class="stat-box danger">
        <span class="stat-num">{{ stats.highRisk }}</span>
        <span class="stat-label">高危操作</span>
      </div>
      <div class="stat-box info">
        <span class="stat-num">{{ stats.logins }}</span>
        <span class="stat-label">登录次数</span>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索操作内容/用户..." />
      </div>
      <el-select v-model="typeFilter" placeholder="操作类型" clearable style="width: 140px;">
        <el-option label="登录" value="login" />
        <el-option label="创建" value="create" />
        <el-option label="修改" value="update" />
        <el-option label="删除" value="delete" />
        <el-option label="导出" value="export" />
      </el-select>
      <el-select v-model="riskFilter" placeholder="风险等级" clearable style="width: 120px;">
        <el-option label="高危" value="high" />
        <el-option label="中危" value="medium" />
        <el-option label="低危" value="low" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px;" />
      <el-button @click="exportLogs"><el-icon><Download /></el-icon> 导出报表</el-button>
    </div>

    <el-table :data="paginatedLogs" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="time" label="操作时间" min-width="160" />
      <el-table-column prop="user" label="操作用户" width="120" />
      <el-table-column prop="ip" label="IP地址" width="140" />
      <el-table-column prop="type" label="操作类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="module" label="模块" width="100" align="center">
        <template #default="{ row }">
          <span>{{ getModuleText(row.module) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="操作内容" min-width="250" show-overflow-tooltip />
      <el-table-column prop="risk" label="风险" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getRiskTag(row.risk)" size="small">{{ getRiskText(row.risk) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="操作详情" width="600px">
      <div class="detail-content" v-if="currentLog">
        <div class="detail-row">
          <label>操作时间:</label>
          <span>{{ currentLog.time }}</span>
        </div>
        <div class="detail-row">
          <label>操作用户:</label>
          <span>{{ currentLog.user }}</span>
        </div>
        <div class="detail-row">
          <label>用户角色:</label>
          <span>{{ currentLog.role }}</span>
        </div>
        <div class="detail-row">
          <label>IP地址:</label>
          <span>{{ currentLog.ip }}</span>
        </div>
        <div class="detail-row">
          <label>操作类型:</label>
          <el-tag :type="getTypeTag(currentLog.type)">{{ getTypeText(currentLog.type) }}</el-tag>
        </div>
        <div class="detail-row">
          <label>所属模块:</label>
          <span>{{ getModuleText(currentLog.module) }}</span>
        </div>
        <div class="detail-row">
          <label>风险等级:</label>
          <el-tag :type="getRiskTag(currentLog.risk)">{{ getRiskText(currentLog.risk) }}</el-tag>
        </div>
        <div class="detail-row full">
          <label>操作内容:</label>
          <pre>{{ currentLog.content }}</pre>
        </div>
        <div class="detail-row full" v-if="currentLog.oldValue || currentLog.newValue">
          <label>变更详情:</label>
          <div class="change-detail">
            <div v-if="currentLog.oldValue">
              <span class="change-label">修改前:</span>
              <pre>{{ currentLog.oldValue }}</pre>
            </div>
            <div v-if="currentLog.newValue">
              <span class="change-label">修改后:</span>
              <pre>{{ currentLog.newValue }}</pre>
            </div>
          </div>
        </div>
        <div class="detail-row full" v-if="currentLog.result">
          <label>执行结果:</label>
          <span :class="currentLog.result === 'success' ? 'text-success' : 'text-danger'">
            {{ currentLog.result === 'success' ? '成功' : '失败' }}
          </span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'

const loading = ref(false)
const searchKeyword = ref('')
const typeFilter = ref('')
const riskFilter = ref('')
const dateRange = ref([])
const detailVisible = ref(false)
const currentLog = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const stats = reactive({ total: 0, today: 0, highRisk: 0, logins: 0 })

const logs = ref([])

const filteredLogs = computed(() => {
  let list = logs.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(l => 
      l.content.toLowerCase().includes(kw) || 
      l.user.toLowerCase().includes(kw) ||
      l.ip.includes(kw)
    )
  }
  if (typeFilter.value) {
    list = list.filter(l => l.type === typeFilter.value)
  }
  if (riskFilter.value) {
    list = list.filter(l => l.risk === riskFilter.value)
  }
  // 更新总数
  pagination.total = list.length
  return list
})

// 分页后的数据
const paginatedLogs = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredLogs.value.slice(start, end)
})

const getTypeTag = (type) => {
  const map = { login: 'info', create: 'success', update: 'warning', delete: 'danger', export: 'primary' }
  return map[type] || 'info'
}

const getTypeText = (type) => {
  const map = { login: '登录', create: '创建', update: '修改', delete: '删除', export: '导出' }
  return map[type] || type
}

const getModuleText = (module) => {
  const map = { 
    user: '用户管理', 
    robot: '机器人', 
    process: '流程管理', 
    task: '任务调度',
    log: '日志管理',
    system: '系统设置'
  }
  return map[module] || module
}

const getRiskTag = (risk) => {
  const map = { high: 'danger', medium: 'warning', low: 'info' }
  return map[risk] || 'info'
}

const getRiskText = (risk) => {
  const map = { high: '高危', medium: '中危', low: '低危' }
  return map[risk] || risk
}

const loadLogs = async () => {
  loading.value = true
  try {
    // 模拟数据 - 生成更多测试数据
    logs.value = []
    for (let i = 1; i <= 25; i++) {
      logs.value.push({
        id: i,
        time: `2026-03-30 ${(14 + Math.floor(i / 4)).toString().padStart(2, '0')}:${(i * 5 % 60).toString().padStart(2, '0')}:00`,
        user: ['admin', 'zhangsan', 'lisi'][i % 3],
        role: ['超级管理员', '普通用户', '运维人员'][i % 3],
        ip: `192.168.1.${100 + i}`,
        type: ['login', 'create', 'update', 'delete', 'export'][i % 5],
        module: ['user', 'robot', 'process', 'task', 'log', 'system'][i % 6],
        content: `操作内容 ${i}`,
        risk: ['low', 'medium', 'high'][i % 3],
        result: 'success'
      })
    }
    // 不需要在这里设置 pagination.total，filteredLogs 会计算
    stats.total = 1256
    stats.today = logs.value.length
    stats.highRisk = logs.value.filter(l => l.risk === 'high').length
    stats.logins = logs.value.filter(l => l.type === 'login').length
  } catch (e) {
    logs.value = []
  } finally {
    loading.value = false
  }
}

const viewDetail = (log) => {
  currentLog.value = log
  detailVisible.value = true
}

const exportLogs = () => {
  ElMessage.success('审计日志报表导出成功')
}

const handleSizeChange = (size) => { 
  pagination.size = size
  pagination.page = 1 // 重置到第一页
}
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.audit-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-box {
  flex: 1;
  background: white;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
}

.stat-box .stat-num {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #1e3a4a;
}

.stat-box .stat-label {
  font-size: 13px;
  color: #8c8c8c;
}

.stat-box.warning { border-left: 4px solid #e6a23c; }
.stat-box.danger { border-left: 4px solid #f56c6c; }
.stat-box.info { border-left: 4px solid #409eff; }

.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; flex-wrap: wrap; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }

.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }

.detail-content { padding: 8px 0; }
.detail-row { display: flex; margin-bottom: 12px; align-items: center; }
.detail-row label { width: 90px; color: #8c8c8c; flex-shrink: 0; }
.detail-row span { color: #262626; font-weight: 500; }
.detail-row.full { flex-direction: column; align-items: flex-start; }
.detail-row.full label { margin-bottom: 8px; }
.detail-row pre { background: #f5f7fa; padding: 12px; border-radius: 4px; font-size: 13px; margin: 8px 0; width: 100%; overflow-x: auto; }
.change-detail { width: 100%; }
.change-label { font-weight: 500; color: #8c8c8c; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
</style>