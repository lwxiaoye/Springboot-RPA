<template>
  <div class="robots-page">
    <div class="page-header">
      <h2>机器人管理</h2>
      <p class="page-desc">管理和监控RPA机器人</p>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-icon><Search /></el-icon>
        <input v-model="searchKeyword" placeholder="搜索机器人名称/IP地址..." />
      </div>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;">
        <el-option label="活跃" value="active" />
        <el-option label="空闲" value="idle" />
        <el-option label="离线" value="offline" />
      </el-select>
      <el-button type="primary" @click="showCreateModal">
        <el-icon><Plus /></el-icon> 注册机器人
      </el-button>
    </div>

    <el-table :data="filteredRobots" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="机器人名称" min-width="140" />
      <el-table-column prop="ip" label="IP地址" min-width="140" />
      <el-table-column prop="hostname" label="主机名" min-width="120" />
      <el-table-column prop="type" label="类型" width="100" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="cpuUsage" label="CPU" width="100" align="center">
        <template #default="{ row }">
          <el-progress :percentage="row.cpuUsage" :color="getProgressColor(row.cpuUsage)" :stroke-width="6" />
        </template>
      </el-table-column>
      <el-table-column prop="memoryUsage" label="内存" width="100" align="center">
        <template #default="{ row }">
          <el-progress :percentage="row.memoryUsage" :color="getProgressColor(row.memoryUsage)" :stroke-width="6" />
        </template>
      </el-table-column>
      <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="160" />
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="primary" @click="editRobot(row)">编辑</el-button>
          <el-popconfirm title="确认删除该机器人吗？" @confirm="deleteRobot(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
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

    <!-- 注册/编辑机器人弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="robotForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="机器人名称" prop="name">
          <el-input v-model="robotForm.name" placeholder="请输入机器人名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="robotForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="桌面机器人" value="Desktop" />
            <el-option label="服务器机器人" value="Server" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="robotForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="空闲" value="idle" />
            <el-option label="忙碌" value="busy" />
            <el-option label="离线" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item label="能力">
          <el-input v-model="robotForm.capabilities" type="textarea" :rows="2" placeholder="请输入机器人能力描述" />
        </el-form-item>
        <el-form-item label="IP地址" prop="ip">
          <el-input v-model="robotForm.ip" placeholder="请输入IP地址，如：192.168.1.100" />
        </el-form-item>
        <el-form-item label="主机名" prop="hostname">
          <el-input v-model="robotForm.hostname" placeholder="请输入主机名" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="robotForm.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="robotForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRobot" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="机器人详情" width="500px">
      <div class="detail-content">
        <div class="detail-item"><label>机器人名称：</label><span>{{ currentRobot.name }}</span></div>
        <div class="detail-item"><label>IP地址：</label><span>{{ currentRobot.ip }}</span></div>
        <div class="detail-item"><label>主机名：</label><span>{{ currentRobot.hostname }}</span></div>
        <div class="detail-item"><label>类型：</label><span>{{ currentRobot.type }}</span></div>
        <div class="detail-item"><label>端口：</label><span>{{ currentRobot.port }}</span></div>
        <div class="detail-item"><label>状态：</label><span>{{ getStatusText(currentRobot.status) }}</span></div>
        <div class="detail-item"><label>CPU使用率：</label><span>{{ currentRobot.cpuUsage }}%</span></div>
        <div class="detail-item"><label>内存使用率：</label><span>{{ currentRobot.memoryUsage }}%</span></div>
        <div class="detail-item full"><label>描述：</label><span>{{ currentRobot.description || '-' }}</span></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

const loading = ref(false)
const submitLoading = ref(false)
const robots = ref([])
const searchKeyword = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentRobot = ref({})
const currentEditId = ref(null)
const formRef = ref(null)

const pagination = reactive({ page: 1, size: 10, total: 0 })

const robotForm = reactive({
  name: '',
  type: 'Desktop',
  status: 'idle',
  capabilities: '',
  ip: '',
  hostname: '',
  port: 8080,
  description: ''
})

const formRules = {
  name: [{ required: true, message: '请输入机器人名称', trigger: 'blur' }],
  ip: [{ required: true, message: '请输入IP地址', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑机器人' : '注册机器人')

const getStatusText = (s) => {
  const map = { active: '活跃', idle: '空闲', offline: '离线' }
  return map[s] || s
}

const getStatusType = (s) => {
  const map = { active: 'success', idle: 'info', offline: 'danger' }
  return map[s] || 'info'
}

const getProgressColor = (v) => v >= 80 ? '#f56c6c' : v >= 60 ? '#e6a23c' : '#67c23a'

const filteredRobots = computed(() => {
  let list = robots.value
  if (searchKeyword.value) {
    list = list.filter(r => r.name.includes(searchKeyword.value) || r.ip?.includes(searchKeyword.value))
  }
  if (statusFilter.value) {
    list = list.filter(r => r.status === statusFilter.value)
  }
  return list
})

const loadRobots = async () => {
  loading.value = true
  try {
    const result = await apiGet('/robot')
    if (result.code === 0) {
      robots.value = result.data || []
      pagination.total = robots.value.length
    }
  } catch {
    robots.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isEdit.value = false
  currentEditId.value = null
  Object.assign(robotForm, { name: '', type: 'Desktop', status: 'idle', capabilities: '', ip: '', hostname: '', port: 8080, description: '' })
  dialogVisible.value = true
}

const editRobot = (robot) => {
  isEdit.value = true
  currentEditId.value = robot.id
  Object.assign(robotForm, {
    name: robot.name,
    type: robot.type,
    status: robot.status || 'idle',
    capabilities: robot.capabilities,
    ip: robot.ip,
    hostname: robot.hostname,
    port: robot.port,
    description: robot.description
  })
  dialogVisible.value = true
}

const viewDetail = (robot) => {
  currentRobot.value = robot
  detailVisible.value = true
}

const submitRobot = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          const result = await apiPut(`/robot/${currentEditId.value}`, {
            name: robotForm.name,
            type: robotForm.type,
            status: robotForm.status,
            capabilities: robotForm.capabilities,
            ip: robotForm.ip,
            hostname: robotForm.hostname,
            port: robotForm.port,
            description: robotForm.description
          })
          if (result.code === 0) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            await loadRobots()
          } else {
            ElMessage.error(result.message || '更新失败')
          }
        } else {
          const result = await apiPost('/robot', {
            name: robotForm.name,
            type: robotForm.type,
            status: robotForm.status,
            capabilities: robotForm.capabilities,
            ip: robotForm.ip,
            hostname: robotForm.hostname,
            port: robotForm.port,
            description: robotForm.description
          })
          if (result.code === 0) {
            ElMessage.success('注册成功')
            dialogVisible.value = false
            await loadRobots()
          } else {
            ElMessage.error(result.message || '注册失败')
          }
        }
      } catch {
        ElMessage.error('请求失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const deleteRobot = async (robot) => {
  try {
    const result = await apiDelete(`/robot/${robot.id}`)
    if (result.code === 0) {
      const index = robots.value.findIndex(r => r.id === robot.id)
      if (index !== -1) {
        robots.value.splice(index, 1)
        pagination.total--
      }
      ElMessage.success('删除成功')
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch {
    ElMessage.error('请求失败')
  }
}

const handleSizeChange = (size) => { pagination.size = size; pagination.page = 1 }
const handleCurrentChange = (page) => { pagination.page = page }

onMounted(() => { loadRobots() })
</script>

<style scoped>
.robots-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; flex: 1; background: transparent; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; background: #fff; padding: 12px 20px; border-radius: 12px; }
.detail-content { padding: 8px 0; }
.detail-item { display: flex; margin-bottom: 12px; }
.detail-item label { width: 90px; color: #8c8c8c; }
.detail-item span { color: #262626; font-weight: 500; }
.detail-item.full { flex-direction: column; }
.detail-item.full label { margin-bottom: 4px; }
</style>