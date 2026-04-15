---
# 使用示例
---

## 示例 1: 构建数据仪表盘

**用户请求**:
```
创建一个销售数据仪表盘，包含 KPI 卡片、销售趋势图和最近订单表格
```

**AI 分析**:
- 产品类型: Dashboard / Analytics
- 行业: E-commerce / Retail
- 风格: Data-Dense Dashboard + Bento Grid
- 配色: 深蓝系 + 绿色/红色趋势色

**输出**:
```vue
<template>
  <div class="dashboard-container">
    <!-- KPI 卡片区 -->
    <div class="kpi-section">
      <StatCard
        v-for="kpi in kpis"
        :key="kpi.id"
        :label="kpi.label"
        :value="kpi.value"
        :trend="kpi.trend"
        :icon="kpi.icon"
      />
    </div>
    
    <!-- 图表区 -->
    <div class="chart-section">
      <el-row :gutter="24">
        <el-col :xs="24" :lg="16">
          <el-card>
            <TrendChart :data="salesTrend" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card>
            <CategoryPie :data="categoryData" />
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <!-- 表格区 -->
    <div class="table-section">
      <TableToolbar @search="handleSearch" @export="handleExport" />
      <el-table :data="recentOrders" stripe>
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="customer" label="客户" />
        <el-table-column prop="amount" label="金额" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
```

---

## 示例 2: 表单页面

**用户请求**:
```
创建一个用户注册表单，需要验证邮箱、密码、确认密码
```

**AI 输出**:
```vue
<template>
  <div class="register-container">
    <el-card class="register-card" shadow="hover">
      <template #header>
        <h2>创建账户</h2>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            type="email"
            placeholder="请输入邮箱"
            :prefix-icon="Message"
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>
        
        <el-form-item>
          <el-checkbox v-model="form.agreedToTerms">
            我已阅读并同意 <el-link type="primary">服务条款</el-link>
          </el-checkbox>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            style="width: 100%;"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Message, Lock } from '@element-plus/icons-vue'

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreedToTerms: false
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || !form.agreedToTerms) return
  
  loading.value = true
  try {
    // 注册逻辑
    ElMessage.success('注册成功！')
    // 跳转...
  } catch (error) {
    ElMessage.error('注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}
.register-card {
  width: 100%;
  max-width: 480px;
  border-radius: 16px;
}
</style>
```

---

## 示例 3: 响应式卡片网格

**用户请求**:
```
创建一个作品展示页面，展示项目卡片网格
```

**AI 输出**:
```vue
<template>
  <div class="portfolio-page">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-radio-group v-model="activeFilter" @change="handleFilter">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="web">Web 应用</el-radio-button>
        <el-radio-button label="mobile">移动端</el-radio-button>
        <el-radio-button label="design">UI 设计</el-radio-button>
      </el-radio-group>
    </div>
    
    <!-- 项目网格 -->
    <div class="project-grid">
      <el-card
        v-for="project in filteredProjects"
        :key="project.id"
        class="project-card"
        shadow="hover"
        :body-style="{ padding: '0' }"
      >
        <div class="project-cover">
          <img :src="project.cover" :alt="project.title" loading="lazy" />
        </div>
        <div class="project-info">
          <h3 class="project-title">{{ project.title }}</h3>
          <p class="project-desc">{{ project.description }}</p>
          <div class="project-tags">
            <el-tag v-for="tag in project.tags" :key="tag" size="small">
              {{ tag }}
            </el-tag>
          </div>
          <div class="project-actions">
            <el-button type="primary" link @click="viewProject(project)">
              查看详情
            </el-button>
            <el-button type="primary" link @click="previewProject(project)">
              预览
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 空状态 -->
    <el-empty v-if="filteredProjects.length === 0" description="暂无项目" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const activeFilter = ref('all')
const projects = ref([
  {
    id: 1,
    title: '电商平台重构',
    description: '基于 Vue 3 + Element Plus 的现代化电商后台',
    cover: '/images/project1.jpg',
    tags: ['Vue3', 'Element Plus', 'Vite'],
    category: 'web'
  },
  // ...
])

const filteredProjects = computed(() => {
  if (activeFilter.value === 'all') return projects.value
  return projects.value.filter(p => p.category === activeFilter.value)
})

const handleFilter = () => {
  // 筛选逻辑
}
</script>

<style scoped>
.filter-bar {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.project-card {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.project-cover {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.project-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.project-card:hover .project-cover img {
  transform: scale(1.05);
}

.project-info {
  padding: 16px;
}

.project-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}

.project-desc {
  margin: 0 0 12px;
  color: #909399;
  font-size: 14px;
  line-height: 1.5;
}

.project-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.project-actions {
  display: flex;
  gap: 16px;
}
</style>
```

---

## 示例 4: 复杂表单验证

**用户请求**:
```
创建订单表单，包含商品选择、数量、收货地址
```

```vue
<template>
  <el-form :model="orderForm" :rules="rules" ref="formRef" label-width="120px">
    <!-- 商品选择 -->
    <el-form-item label="商品" prop="productId">
      <el-select 
        v-model="orderForm.productId" 
        placeholder="请选择商品"
        @change="handleProductChange"
        filterable
      >
        <el-option
          v-for="product in products"
          :key="product.id"
          :label="product.name"
          :value="product.id"
        >
          <span>{{ product.name }}</span>
          <span style="float: right; color: #8492a6;">¥{{ product.price }}</span>
        </el-option>
      </el-select>
    </el-form-item>
    
    <!-- 数量 -->
    <el-form-item label="数量" prop="quantity">
      <el-input-number
        v-model="orderForm.quantity"
        :min="1"
        :max="product.stock"
        @change="calculateTotal"
      />
      <span class="stock-info">库存: {{ product.stock }}</span>
    </el-form-item>
    
    <!-- 总价 -->
    <el-form-item label="总价">
      <span class="total-price">¥{{ totalPrice }}</span>
    </el-form-item>
    
    <!-- 收货地址 -->
    <el-form-item label="收货地址" prop="address">
      <el-cascader
        v-model="orderForm.addressRegion"
        :options="regionOptions"
        placeholder="选择省/市/区"
        style="width: 100%;"
      />
    </el-form-item>
    
    <el-form-item label="详细地址" prop="detailAddress">
      <el-input
        v-model="orderForm.detailAddress"
        type="textarea"
        :rows="3"
        placeholder="请输入街道、门牌号等详细信息"
      />
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'

const formRef = ref()

const orderForm = reactive({
  productId: '',
  quantity: 1,
  addressRegion: [],
  detailAddress: ''
})

const products = ref([
  { id: 1, name: '商品 A', price: 100, stock: 50 },
  { id: 2, name: '商品 B', price: 200, stock: 30 }
])

const product = computed(() => 
  products.value.find(p => p.id === orderForm.productId) || {}
)

const totalPrice = computed(() => 
  product.value.price * orderForm.quantity
)

const calculateTotal = () => {
  // 自动计算
}

const handleProductChange = () => {
  orderForm.quantity = 1
}
</script>
```

---

## 示例 5: 移动端适配

```vue
<template>
  <div class="mobile-responsive">
    <!-- 桌面端显示 -->
    <div class="desktop-only">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="12" :lg="8">
          <CardComponent />
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
/* 移动优先的媒体查询 */
.mobile-responsive {
  padding: 16px;
}

@media (min-width: 768px) {
  .mobile-responsive {
    padding: 24px;
  }
}

@media (min-width: 1024px) {
  .mobile-responsive {
    padding: 32px;
    max-width: 1200px;
    margin: 0 auto;
  }
}

/* 工具类 */
.desktop-only {
  display: block;
}

@media (max-width: 767px) {
  .desktop-only {
    display: none;
  }
}
</style>
```
