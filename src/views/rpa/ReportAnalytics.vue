<template>
  <div class="report-page">
    <div class="page-header">
      <h2>报表与分析中心</h2>
      <p class="page-desc">评估RPA投资回报率和运营效率</p>
    </div>

    <!-- 统计概览卡片 -->
    <div class="stats-row">
      <div class="stat-card" @click="switchToTab('daily')">
        <div class="stat-icon primary"><el-icon><DataLine /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ formatNumber(stats.totalTasks) }}</span>
          <span class="stat-label">总执行任务</span>
        </div>
        <div class="stat-trend up">
          <el-icon><Top /></el-icon>
          <span>12.5%</span>
        </div>
      </div>
      <div class="stat-card" @click="switchToTab('daily')">
        <div class="stat-icon success"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.successRate }}%</span>
          <span class="stat-label">任务成功率</span>
        </div>
        <div class="stat-trend up">
          <el-icon><Top /></el-icon>
          <span>2.3%</span>
        </div>
      </div>
      <div class="stat-card" @click="switchToTab('process')">
        <div class="stat-icon warning"><el-icon><Timer /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.avgDuration }}h</span>
          <span class="stat-label">平均运行时长</span>
        </div>
        <div class="stat-trend down">
          <el-icon><Bottom /></el-icon>
          <span>8.2%</span>
        </div>
      </div>
      <div class="stat-card" @click="switchToTab('roi')">
        <div class="stat-icon danger"><el-icon><Money /></el-icon></div>
        <div class="stat-content">
          <span class="stat-value">{{ formatNumber(stats.savedHours) }}</span>
          <span class="stat-label">节省工时(小时)</span>
        </div>
        <div class="stat-trend up">
          <el-icon><Top /></el-icon>
          <span>15.8%</span>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-entry">
      <div class="quick-item" @click="switchToTab('daily')">
        <el-icon><Document /></el-icon>
        <span>任务执行日报</span>
      </div>
      <div class="quick-item" @click="switchToTab('monthly')">
        <el-icon><Calendar /></el-icon>
        <span>任务执行月报</span>
      </div>
      <div class="quick-item" @click="switchToTab('robot')">
        <el-icon><Cpu /></el-icon>
        <span>机器人利用率</span>
      </div>
      <div class="quick-item" @click="switchToTab('process')">
        <el-icon><Stopwatch /></el-icon>
        <span>流程耗时排行</span>
      </div>
      <div class="quick-item" @click="switchToTab('custom')">
        <el-icon><Edit /></el-icon>
        <span>自定义报表</span>
      </div>
      <div class="quick-item" @click="switchToTab('roi')">
        <el-icon><Coin /></el-icon>
        <span>成本节省</span>
      </div>
    </div>

    <el-tabs v-model="currentTab" class="report-tabs" @tab-change="onTabChange">
      <!-- 任务执行日报 -->
      <el-tab-pane label="任务执行日报" name="daily">
        <div class="report-section">
          <div class="section-header">
            <h3>今日任务执行概览</h3>
            <div class="header-actions">
              <el-date-picker v-model="dailyDate" type="date" placeholder="选择日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
              <el-button type="primary" @click="exportReport('daily')">
                <el-icon><Download /></el-icon> 导出
              </el-button>
            </div>
          </div>

          <div class="stats-row-mini">
            <div class="stat-mini success">
              <span class="value">{{ dailyStats.success }}</span>
              <span class="label">成功</span>
            </div>
            <div class="stat-mini danger">
              <span class="value">{{ dailyStats.failed }}</span>
              <span class="label">失败</span>
            </div>
            <div class="stat-mini warning">
              <span class="value">{{ dailyStats.running }}</span>
              <span class="label">进行中</span>
            </div>
            <div class="stat-mini info">
              <span class="value">{{ dailyStats.total }}</span>
              <span class="label">总计</span>
            </div>
          </div>

          <div class="chart-row">
            <div class="chart-container">
              <h4>每小时执行趋势</h4>
              <v-chart :option="hourlyTrendOption" autoresize style="height: 280px;"></v-chart>
            </div>
            <div class="chart-container small">
              <h4>执行状态分布</h4>
              <v-chart :option="dailyPieOption" autoresize style="height: 280px;"></v-chart>
            </div>
          </div>

          <div class="detail-table">
            <h4>今日执行明细</h4>
            <el-table :data="dailyLogs" border stripe size="small" max-height="300">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
              <el-table-column prop="processName" label="流程" min-width="120" />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="dataCount" label="采集数据" width="90" align="center" />
              <el-table-column prop="startTime" label="开始时间" min-width="150" />
              <el-table-column prop="duration" label="耗时" width="80" align="center" />
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- 任务执行月报 -->
      <el-tab-pane label="任务执行月报" name="monthly">
        <div class="report-section">
          <div class="section-header">
            <h3>{{ currentMonth }} 月度执行汇总</h3>
            <div class="header-actions">
              <el-date-picker v-model="monthlyDate" type="month" placeholder="选择月份" format="YYYY-MM" value-format="YYYY-MM" />
              <el-button type="primary" @click="exportReport('monthly')">
                <el-icon><Download /></el-icon> 导出
              </el-button>
            </div>
          </div>

          <div class="monthly-summary">
            <div class="summary-card">
              <div class="summary-title">本月执行统计</div>
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="label">总执行次数</span>
                  <span class="value primary">{{ formatNumber(monthlyStats.totalExecutions) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">成功次数</span>
                  <span class="value success">{{ formatNumber(monthlyStats.successCount) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">失败次数</span>
                  <span class="value danger">{{ formatNumber(monthlyStats.failedCount) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">成功率</span>
                  <span class="value">{{ monthlyStats.successRate }}%</span>
                </div>
              </div>
            </div>
            <div class="summary-card">
              <div class="summary-title">数据采集统计</div>
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="label">采集数据总量</span>
                  <span class="value primary">{{ formatNumber(monthlyStats.totalData) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">日均采集</span>
                  <span class="value">{{ formatNumber(monthlyStats.dailyAvg) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">峰值日采集</span>
                  <span class="value">{{ formatNumber(monthlyStats.peakData) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">采集成功率</span>
                  <span class="value success">{{ monthlyStats.dataSuccessRate }}%</span>
                </div>
              </div>
            </div>
          </div>

          <div class="chart-row full">
            <div class="chart-container">
              <h4>月度执行趋势</h4>
              <v-chart :option="monthlyTrendOption" autoresize style="height: 300px;"></v-chart>
            </div>
          </div>

          <div class="chart-row">
            <div class="chart-container">
              <h4>各流程执行占比</h4>
              <v-chart :option="processPieOption" autoresize style="height: 280px;"></v-chart>
            </div>
            <div class="chart-container">
              <h4>每日采集数据量</h4>
              <v-chart :option="dataCollectTrendOption" autoresize style="height: 280px;"></v-chart>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 机器人利用率 -->
      <el-tab-pane label="机器人利用率" name="robot">
        <div class="report-section">
          <div class="section-header">
            <h3>机器人工作负载分析</h3>
            <div class="header-actions">
              <el-date-picker v-model="robotDateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
              <el-button type="primary" @click="exportReport('robot')">
                <el-icon><Download /></el-icon> 导出
              </el-button>
            </div>
          </div>

          <div class="robot-overview">
            <div class="robot-stat-card">
              <div class="robot-stat-icon busy"><el-icon><Cpu /></el-icon></div>
              <div class="robot-stat-info">
                <span class="value">{{ robotStats.busyRate }}%</span>
                <span class="label">忙碌率</span>
              </div>
            </div>
            <div class="robot-stat-card">
              <div class="robot-stat-icon idle"><el-icon><Coffee /></el-icon></div>
              <div class="robot-stat-info">
                <span class="value">{{ robotStats.idleRate }}%</span>
                <span class="label">空闲率</span>
              </div>
            </div>
            <div class="robot-stat-card">
              <div class="robot-stat-icon offline"><el-icon><CloseBold /></el-icon></div>
              <div class="robot-stat-info">
                <span class="value">{{ robotStats.offlineRate }}%</span>
                <span class="label">离线率</span>
              </div>
            </div>
            <div class="robot-stat-card">
              <div class="robot-stat-icon runtime"><el-icon><Timer /></el-icon></div>
              <div class="robot-stat-info">
                <span class="value">{{ robotStats.totalRuntime }}</span>
                <span class="label">总运行时长</span>
              </div>
            </div>
          </div>

          <div class="chart-row">
            <div class="chart-container">
              <h4>机器人工作状态分布</h4>
              <v-chart :option="robotStatusOption" autoresize style="height: 280px;"></v-chart>
            </div>
            <div class="chart-container">
              <h4>各机器人执行量排行</h4>
              <v-chart :option="robotRankingOption" autoresize style="height: 280px;"></v-chart>
            </div>
          </div>

          <div class="robot-table">
            <h4>机器人详细数据</h4>
            <el-table :data="robotList" border stripe size="small">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="name" label="机器人名称" min-width="150" />
              <el-table-column prop="status" label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="getRobotStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="execCount" label="执行次数" width="100" align="center" />
              <el-table-column prop="successRate" label="成功率" width="100" align="center">
                <template #default="{ row }">
                  <span :class="row.successRate >= 90 ? 'text-success' : row.successRate >= 70 ? 'text-warning' : 'text-danger'">
                    {{ row.successRate }}%
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="runtime" label="运行时长" width="100" align="center" />
              <el-table-column prop="dataCount" label="采集数据" width="100" align="center" />
              <el-table-column prop="lastRun" label="最后执行" min-width="150" />
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- 流程耗时排行 -->
      <el-tab-pane label="流程耗时排行" name="process">
        <div class="report-section">
          <div class="section-header">
            <h3>流程执行效率分析</h3>
            <div class="header-actions">
              <el-select v-model="processTopN" style="width: 120px;">
                <el-option label="TOP 5" :value="5" />
                <el-option label="TOP 10" :value="10" />
                <el-option label="TOP 20" :value="20" />
              </el-select>
              <el-date-picker v-model="processDateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
              <el-button type="primary" @click="exportReport('process')">
                <el-icon><Download /></el-icon> 导出
              </el-button>
            </div>
          </div>

          <div class="chart-row full">
            <div class="chart-container">
              <h4>执行耗时最长的流程 TOP {{ processTopN }}</h4>
              <v-chart :option="processDurationOption" autoresize style="height: 350px;"></v-chart>
            </div>
          </div>

          <div class="process-table">
            <h4>流程耗时明细</h4>
            <el-table :data="processList" border stripe size="small">
              <el-table-column type="index" label="排名" width="60" align="center" />
              <el-table-column prop="name" label="流程名称" min-width="180" />
              <el-table-column prop="code" label="流程编码" width="150" />
              <el-table-column prop="execCount" label="执行次数" width="100" align="center" />
              <el-table-column prop="avgDuration" label="平均耗时" width="100" align="center">
                <template #default="{ row }">
                  <span class="text-warning">{{ row.avgDuration }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="maxDuration" label="最长耗时" width="100" align="center" />
              <el-table-column prop="minDuration" label="最短耗时" width="100" align="center" />
              <el-table-column prop="totalDuration" label="总耗时" width="100" align="center" />
              <el-table-column prop="successRate" label="成功率" width="90" align="center">
                <template #default="{ row }">
                  <span :class="row.successRate >= 90 ? 'text-success' : 'text-danger'">{{ row.successRate }}%</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- 自定义报表 -->
      <el-tab-pane label="自定义报表" name="custom">
        <div class="report-section">
          <div class="toolbar">
            <el-button type="primary" @click="createCustomReport">
              <el-icon><Plus /></el-icon> 创建报表
            </el-button>
            <el-button @click="exportReport('custom')">
              <el-icon><Download /></el-icon> 导出
            </el-button>
          </div>

          <el-table :data="customReports" v-loading="loading" border stripe>
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="name" label="报表名称" min-width="180" />
            <el-table-column prop="type" label="报表类型" width="130" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getReportTypeTag(row.type)">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dimensions" label="统计维度" min-width="150" />
            <el-table-column prop="createUser" label="创建人" width="100" />
            <el-table-column prop="createTime" label="创建时间" min-width="160" />
            <el-table-column prop="lastRun" label="最后运行" min-width="160" />
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="runReport(row)">运行</el-button>
                <el-button link type="primary" size="small" @click="editCustomReport(row)">编辑</el-button>
                <el-popconfirm title="确认删除该报表吗？" @confirm="deleteCustomReport(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 成本节省 -->
      <el-tab-pane label="成本节省" name="roi">
        <div class="report-section">
          <div class="section-header">
            <h3>RPA投资回报率分析</h3>
          </div>

          <div class="roi-cards">
            <div class="roi-card primary">
              <div class="roi-icon"><el-icon><Money /></el-icon></div>
              <div class="roi-info">
                <span class="roi-value">¥ {{ formatNumber(roiStats.annualSavings) }}</span>
                <span class="roi-label">年度节省成本</span>
              </div>
            </div>
            <div class="roi-card success">
              <div class="roi-icon"><el-icon><TrendCharts /></el-icon></div>
              <div class="roi-info">
                <span class="roi-value">{{ roiStats.roi }}%</span>
                <span class="roi-label">投资回报率</span>
              </div>
            </div>
            <div class="roi-card warning">
              <div class="roi-icon"><el-icon><Clock /></el-icon></div>
              <div class="roi-info">
                <span class="roi-value">{{ roiStats.paybackPeriod }} 月</span>
                <span class="roi-label">投资回收期</span>
              </div>
            </div>
            <div class="roi-card info">
              <div class="roi-icon"><el-icon><User /></el-icon></div>
              <div class="roi-info">
                <span class="roi-value">{{ roiStats.laborReduction }}</span>
                <span class="roi-label">减少人力(人)</span>
              </div>
            </div>
          </div>

          <div class="cost-section">
            <el-card class="cost-calculator">
              <template #header>
                <div class="card-header">
                  <span>成本节省计算器</span>
                  <el-button link type="primary" @click="resetCalculator">重置</el-button>
                </div>
              </template>
              <el-form :model="costForm" label-width="120px">
                <el-form-item label="RPA部署数量">
                  <el-input-number v-model="costForm.rpaCount" :min="1" :max="100" />
                  <span class="unit">个</span>
                </el-form-item>
                <el-form-item label="RPA年费/个">
                  <el-input-number v-model="costForm.rpaCostPerUnit" :min="10000" :max="100000" :step="10000" />
                  <span class="unit">元</span>
                </el-form-item>
                <el-form-item label="人工执行耗时">
                  <el-input-number v-model="costForm.manualTime" :min="1" :max="1000" />
                  <span class="unit">分钟/次</span>
                </el-form-item>
                <el-form-item label="执行频率">
                  <el-input-number v-model="costForm.frequency" :min="1" :max="10000" />
                  <span class="unit">次/月</span>
                </el-form-item>
                <el-form-item label="人工时薪">
                  <el-input-number v-model="costForm.hourlyRate" :min="20" :max="1000" />
                  <span class="unit">元/小时</span>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="calculateSavings">计算节省</el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <el-card class="cost-result" v-if="showResult">
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
                  <span class="savings-value success">¥ {{ formatNumber(savings.monthlyCost) }}</span>
                </div>
                <div class="savings-item">
                  <span class="savings-label">每年节省工时:</span>
                  <span class="savings-value primary">{{ formatNumber(savings.yearlyHours) }} 小时</span>
                </div>
                <div class="savings-item">
                  <span class="savings-label">每年节省成本:</span>
                  <span class="savings-value success">¥ {{ formatNumber(savings.yearlyCost) }}</span>
                </div>
                <div class="savings-item">
                  <span class="savings-label">RPA年成本:</span>
                  <span class="savings-value">¥ {{ formatNumber(costForm.rpaCount * costForm.rpaCostPerUnit) }}</span>
                </div>
                <div class="savings-item highlight">
                  <span class="savings-label">ROI:</span>
                  <span class="savings-value">{{ savings.roi }}%</span>
                </div>
                <div class="savings-item highlight">
                  <span class="savings-label">回收期:</span>
                  <span class="savings-value">{{ savings.paybackPeriod }} 个月</span>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <!-- 趋势预测 -->
      <el-tab-pane label="趋势预测" name="forecast">
        <div class="report-section">
          <div class="section-header">
            <h3>智能任务量预测</h3>
            <div class="header-actions">
              <el-select v-model="forecastPeriod" style="width: 150px;">
                <el-option label="未来7天" value="7" />
                <el-option label="未来30天" value="30" />
                <el-option label="未来90天" value="90" />
              </el-select>
            </div>
          </div>

          <div class="forecast-cards">
            <div class="forecast-card">
              <div class="forecast-icon"><el-icon><TrendCharts /></el-icon></div>
              <div class="forecast-info">
                <span class="forecast-value">预计增长 {{ forecastStats.growthRate }}%</span>
                <span class="forecast-label">下期任务量预测</span>
              </div>
            </div>
            <div class="forecast-card">
              <div class="forecast-icon warning"><el-icon><Warning /></el-icon></div>
              <div class="forecast-info">
                <span class="forecast-value">建议扩容 {{ forecastStats.suggestRobotCount }} 台</span>
                <span class="forecast-label">机器人扩容建议</span>
              </div>
            </div>
            <div class="forecast-card">
              <div class="forecast-icon success"><el-icon><Check /></el-icon></div>
              <div class="forecast-info">
                <span class="forecast-value">{{ forecastStats.capacityRate }}%</span>
                <span class="forecast-label">当前容量利用率</span>
              </div>
            </div>
          </div>

          <div class="forecast-chart">
            <h4>未来任务量趋势预测</h4>
            <v-chart :option="forecastChartOption" autoresize style="height: 350px;"></v-chart>
          </div>

          <div class="forecast-tips">
            <el-alert type="info" :closable="false">
              <template #title>
                <strong>智能建议:</strong> {{ forecastStats.suggestion }}
              </template>
            </el-alert>
          </div>
        </div>
      </el-tab-pane>

      <!-- 报表订阅 -->
      <el-tab-pane label="报表订阅" name="subscription">
        <div class="report-section">
          <div class="toolbar">
            <el-button type="primary" @click="addSubscription">
              <el-icon><Plus /></el-icon> 添加订阅
            </el-button>
          </div>

          <el-table :data="subscriptions" v-loading="subLoading" border stripe>
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="name" label="订阅名称" min-width="180" />
            <el-table-column prop="report" label="报表类型" min-width="130" />
            <el-table-column prop="frequency" label="发送频率" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.frequency }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="recipients" label="接收人" min-width="150" show-overflow-tooltip />
            <el-table-column prop="channel" label="推送方式" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.channel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.status" @change="toggleSubscription(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="lastRun" label="最后发送" min-width="160" />
            <el-table-column label="操作" width="150" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="editSubscription(row)">编辑</el-button>
                <el-popconfirm title="确认删除吗？" @confirm="deleteSubscription(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建自定义报表弹窗 -->
    <el-dialog v-model="reportDialogVisible" title="创建自定义报表" width="650px">
      <el-form :model="reportForm" label-width="100px">
        <el-form-item label="报表名称">
          <el-input v-model="reportForm.name" placeholder="请输入报表名称" />
        </el-form-item>
        <el-form-item label="报表类型">
          <el-select v-model="reportForm.type" style="width: 100%;">
            <el-option label="任务执行统计" value="任务执行" />
            <el-option label="数据采集统计" value="数据采集" />
            <el-option label="机器人效能" value="机器人效能" />
            <el-option label="流程效率分析" value="流程效率" />
            <el-option label="成本效益分析" value="成本效益" />
            <el-option label="自定义" value="自定义" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计维度">
          <el-select v-model="reportForm.dimensions" multiple placeholder="选择统计维度" style="width: 100%;">
            <el-option label="执行次数" value="execCount" />
            <el-option label="成功率" value="successRate" />
            <el-option label="采集数据量" value="dataCount" />
            <el-option label="执行耗时" value="duration" />
            <el-option label="流程分布" value="process" />
            <el-option label="时间趋势" value="trend" />
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
          <el-input v-model="reportForm.description" type="textarea" :rows="2" placeholder="请输入报表描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCustomReport">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加订阅弹窗 -->
    <el-dialog v-model="subDialogVisible" title="添加报表订阅" width="550px">
      <el-form :model="subForm" label-width="100px">
        <el-form-item label="订阅名称">
          <el-input v-model="subForm.name" placeholder="请输入订阅名称" />
        </el-form-item>
        <el-form-item label="报表类型">
          <el-select v-model="subForm.report" style="width: 100%;">
            <el-option label="任务执行日报" value="daily" />
            <el-option label="任务执行周报" value="weekly" />
            <el-option label="任务执行月报" value="monthly" />
            <el-option label="机器人利用率" value="robot" />
            <el-option label="成本节省报告" value="roi" />
          </el-select>
        </el-form-item>
        <el-form-item label="发送频率">
          <el-select v-model="subForm.frequency" style="width: 100%;">
            <el-option label="每日" value="daily" />
            <el-option label="每周一" value="weekly" />
            <el-option label="每月1日" value="monthly" />
          </el-select>
        </el-form-item>
        <el-form-item label="推送方式">
          <el-select v-model="subForm.channel" style="width: 100%;">
            <el-option label="邮件" value="email" />
            <el-option label="钉钉" value="dingtalk" />
            <el-option label="企业微信" value="wecom" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人">
          <el-input v-model="subForm.recipients" placeholder="请输入接收人邮箱或账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSubscription">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DataLine, CircleCheck, Timer, Money, Plus, Download, TrendCharts, PieChart,
  Document, Calendar, Cpu, Stopwatch, Edit, Coin, Clock, User, Top, Bottom,
  Coffee, CloseBold, Warning, Check
} from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart as EChartsBarChart, PieChart as PieChartType } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { apiGet } from '../../utils/api.js'

use([CanvasRenderer, LineChart, EChartsBarChart, PieChartType, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const currentTab = ref('daily')
const loading = ref(false)
const subLoading = ref(false)
const reportDialogVisible = ref(false)
const subDialogVisible = ref(false)
const showResult = ref(false)

// 日期选择
const dailyDate = ref(new Date().toISOString().split('T')[0])
const monthlyDate = ref(new Date().toISOString().slice(0, 7))
const robotDateRange = ref([])
const processDateRange = ref([])
const processTopN = ref(10)
const forecastPeriod = ref('30')

const currentMonth = computed(() => {
  const [year, month] = monthlyDate.value.split('-')
  return `${year}年${parseInt(month)}月`
})

// 统计概览
const stats = reactive({
  totalTasks: 12580,
  successRate: 94.5,
  avgDuration: 2.3,
  savedHours: 2560
})

// 日报数据
const dailyStats = reactive({
  success: 156,
  failed: 8,
  running: 12,
  total: 176
})

const dailyLogs = ref([
  { taskName: '客户信息采集', processName: '数据采集流程', status: 'success', dataCount: 320, startTime: '2026-04-03 09:00:00', duration: '5m30s' },
  { taskName: '订单同步', processName: '订单处理流程', status: 'success', dataCount: 128, startTime: '2026-04-03 09:30:00', duration: '3m45s' },
  { taskName: '发票审核', processName: '发票审核流程', status: 'failed', dataCount: 0, startTime: '2026-04-03 10:00:00', duration: '1m20s' },
  { taskName: '报表生成', processName: '报表生成流程', status: 'running', dataCount: 0, startTime: '2026-04-03 10:15:00', duration: '-' },
])

// 月报数据
const monthlyStats = reactive({
  totalExecutions: 4250,
  successCount: 4016,
  failedCount: 234,
  successRate: 94.5,
  totalData: 89560,
  dailyAvg: 2985,
  peakData: 5200,
  dataSuccessRate: 96.2
})

// 机器人数据
const robotStats = reactive({
  busyRate: 65,
  idleRate: 28,
  offlineRate: 7,
  totalRuntime: '1,520h'
})

const robotList = ref([
  { name: 'Robot-Collector-01', status: 'busy', statusText: '忙碌', execCount: 1256, successRate: 98.5, runtime: '320h', dataCount: 45600, lastRun: '2026-04-03 10:30:00' },
  { name: 'Robot-Parser-01', status: 'busy', statusText: '忙碌', execCount: 892, successRate: 95.2, runtime: '280h', dataCount: 38900, lastRun: '2026-04-03 10:28:00' },
  { name: 'Robot-Processor-01', status: 'idle', statusText: '空闲', execCount: 456, successRate: 92.1, runtime: '180h', dataCount: 12500, lastRun: '2026-04-03 09:45:00' },
  { name: 'Robot-General-01', status: 'offline', statusText: '离线', execCount: 234, successRate: 88.9, runtime: '120h', dataCount: 8600, lastRun: '2026-04-02 18:00:00' },
])

// 流程数据
const processList = ref([
  { name: '客户信息采集流程', code: 'CUSTOMER_COLLECT', execCount: 1256, avgDuration: '5m30s', maxDuration: '12m', minDuration: '3m', totalDuration: '115h', successRate: 98.5 },
  { name: '订单同步流程', code: 'ORDER_SYNC', execCount: 892, avgDuration: '3m45s', maxDuration: '8m', minDuration: '2m', totalDuration: '56h', successRate: 96.8 },
  { name: '发票审核流程', code: 'INVOICE_CHECK', execCount: 567, avgDuration: '4m20s', maxDuration: '15m', minDuration: '2m30s', totalDuration: '41h', successRate: 94.2 },
  { name: '报表生成流程', code: 'REPORT_GEN', execCount: 320, avgDuration: '8m15s', maxDuration: '25m', minDuration: '5m', totalDuration: '44h', successRate: 91.5 },
])

// ROI数据
const roiStats = reactive({
  annualSavings: 358000,
  roi: 358,
  paybackPeriod: 3,
  laborReduction: 12
})

// 预测数据
const forecastStats = reactive({
  growthRate: 15,
  suggestRobotCount: 2,
  capacityRate: 78,
  suggestion: '基于历史数据分析，预计下月任务量将增长15%。建议扩容2台机器人以确保服务质量。同时建议优化发票审核流程，预计可提升20%效率。'
})

// 自定义报表
const customReports = ref([
  { id: 1, name: '财务流程周报', type: '任务执行', dimensions: '执行次数,成功率,采集数据量', createUser: 'admin', createTime: '2026-03-15 10:00:00', lastRun: '2026-03-30 09:00:00' },
  { id: 2, name: 'HR流程报表', type: '数据采集', dimensions: '采集数据量,时间趋势', createUser: 'admin', createTime: '2026-03-10 14:30:00', lastRun: '2026-03-29 18:00:00' }
])

// 订阅列表
const subscriptions = ref([
  { id: 1, name: '每日任务日报', report: '任务执行日报', frequency: '每日', recipients: 'admin@company.com', channel: '邮件', status: true, lastRun: '2026-04-03 09:00:00' },
  { id: 2, name: '每周汇总', report: '任务执行周报', frequency: '每周', recipients: 'leader@company.com', channel: '钉钉', status: true, lastRun: '2026-03-31 09:00:00' },
  { id: 3, name: '月度分析', report: '任务执行月报', frequency: '每月', recipients: 'manager@company.com', channel: '邮件', status: false, lastRun: '2026-03-01 09:00:00' }
])

// 成本计算
const costForm = reactive({
  rpaCount: 5,
  rpaCostPerUnit: 50000,
  manualTime: 30,
  frequency: 100,
  hourlyRate: 100
})

const savings = reactive({
  monthlyHours: 0,
  monthlyCost: 0,
  yearlyHours: 0,
  yearlyCost: 0,
  roi: 0,
  paybackPeriod: 0
})

// 报表表单
const reportForm = reactive({
  name: '',
  type: '任务执行',
  dimensions: [],
  dateRange: 'month',
  chartType: 'line',
  description: ''
})

// 订阅表单
const subForm = reactive({
  name: '',
  report: 'daily',
  frequency: 'daily',
  channel: 'email',
  recipients: ''
})

// 格式化数字
const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

// 切换Tab
const switchToTab = (tab) => {
  currentTab.value = tab
}

const onTabChange = (tab) => {
  console.log('切换到:', tab)
}

// 状态标签类型
const getStatusTagType = (status) => {
  const map = { success: 'success', failed: 'danger', running: 'warning', abnormal: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { success: '成功', failed: '失败', running: '进行中', abnormal: '异常' }
  return map[status] || status || '-'
}

// 机器人状态
const getRobotStatusType = (status) => {
  const map = { busy: 'warning', idle: 'success', offline: 'info' }
  return map[status] || 'info'
}

// 报表类型标签
const getReportTypeTag = (type) => {
  const map = { '任务执行': 'primary', '数据采集': 'success', '机器人效能': 'warning', '流程效率': 'info', '成本效益': 'danger' }
  return map[type] || 'info'
}

// 图表配置
const hourlyTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['00:00', '02:00', '04:00', '06:00', '08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']
  },
  yAxis: { type: 'value', name: '任务数' },
  series: [
    {
      name: '成功',
      type: 'bar',
      stack: 'total',
      data: [2, 0, 0, 5, 25, 35, 30, 28, 22, 15, 8, 3],
      itemStyle: { color: '#67c23a' }
    },
    {
      name: '失败',
      type: 'bar',
      stack: 'total',
      data: [0, 0, 0, 1, 2, 1, 2, 1, 0, 0, 1, 0],
      itemStyle: { color: '#f56c6c' }
    }
  ]
}))

const dailyPieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
    label: { show: true, formatter: '{b}: {d}%' },
    data: [
      { value: 156, name: '成功', itemStyle: { color: '#67c23a' } },
      { value: 8, name: '失败', itemStyle: { color: '#f56c6c' } },
      { value: 12, name: '进行中', itemStyle: { color: '#e6a23c' } }
    ]
  }]
}))

const monthlyTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['执行次数', '成功次数'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['1日', '5日', '10日', '15日', '20日', '25日', '30日']
  },
  yAxis: { type: 'value', name: '次数' },
  series: [
    { name: '执行次数', type: 'line', data: [180, 165, 190, 175, 200, 185, 195], smooth: true, itemStyle: { color: '#409eff' } },
    { name: '成功次数', type: 'line', data: [170, 158, 180, 166, 190, 175, 186], smooth: true, itemStyle: { color: '#67c23a' } }
  ]
}))

const processPieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: '60%',
    label: { show: true, formatter: '{b}: {d}%' },
    data: [
      { value: 35, name: '数据采集', itemStyle: { color: '#409eff' } },
      { value: 25, name: '订单处理', itemStyle: { color: '#67c23a' } },
      { value: 18, name: '发票审核', itemStyle: { color: '#e6a23c' } },
      { value: 12, name: '报表生成', itemStyle: { color: '#f56c6c' } },
      { value: 10, name: '其他', itemStyle: { color: '#909399' } }
    ]
  }]
}))

const dataCollectTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
  xAxis: { type: 'category', data: ['1日', '5日', '10日', '15日', '20日', '25日', '30日'] },
  yAxis: { type: 'value', name: '数据量' },
  series: [{
    name: '采集数据',
    type: 'bar',
    data: [2800, 3200, 2900, 3500, 3800, 3600, 4200],
    itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] }
  }]
}))

const robotStatusOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c}% ({d}%)' },
  series: [{
    type: 'pie',
    radius: ['45%', '70%'],
    innerRadius: '35%',
    data: [
      { value: 65, name: '忙碌', itemStyle: { color: '#e6a23c' } },
      { value: 28, name: '空闲', itemStyle: { color: '#67c23a' } },
      { value: 7, name: '离线', itemStyle: { color: '#909399' } }
    ]
  }]
}))

const robotRankingOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
  xAxis: { type: 'value', name: '执行次数' },
  yAxis: { type: 'category', data: ['Robot-Collector-01', 'Robot-Parser-01', 'Robot-Processor-01', 'Robot-General-01'] },
  series: [{
    type: 'bar',
    data: [1256, 892, 456, 234],
    itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] }
  }]
}))

const processDurationOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: { type: 'value', name: '平均耗时(分钟)' },
  yAxis: {
    type: 'category',
    data: processList.value.slice(0, processTopN.value).map(p => p.name).reverse()
  },
  series: [{
    type: 'bar',
    data: processList.value.slice(0, processTopN.value).map(p => parseFloat(p.avgDuration)).reverse(),
    itemStyle: { color: '#e6a23c', borderRadius: [0, 4, 4, 0] },
    label: { show: true, position: 'right', formatter: '{c}m' }
  }]
}))

const forecastChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['历史数据', '预测值'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: { type: 'value', name: '任务数' },
  series: [
    {
      name: '历史数据',
      type: 'line',
      data: [120, 132, 101, 134, 190, 230, 210, null, null, null, null, null, null, null],
      itemStyle: { color: '#409eff' },
      areaStyle: { color: 'rgba(64, 158, 255, 0.1)' }
    },
    {
      name: '预测值',
      type: 'line',
      data: [null, null, null, null, null, null, null, 230, 250, 280, 260, 290, 310, 340],
      itemStyle: { color: '#67c23a' },
      lineStyle: { type: 'dashed' },
      areaStyle: { color: 'rgba(103, 194, 58, 0.1)' }
    }
  ]
}))

// 导出报表
const exportReport = (type) => {
  let data = []
  let filename = ''
  let headers = []

  switch (type) {
    case 'daily':
      filename = '任务执行日报'
      headers = ['任务名称', '流程', '状态', '采集数据', '开始时间', '耗时']
      data = dailyLogs.value.map(item => ({
        '任务名称': item.taskName,
        '流程': item.processName,
        '状态': getStatusText(item.status),
        '采集数据': item.dataCount,
        '开始时间': item.startTime,
        '耗时': item.duration
      }))
      break
    case 'monthly':
      filename = '任务执行月报'
      headers = ['统计项', '数值']
      data = [
        {'统计项': '总执行次数', '数值': monthlyStats.totalExecutions},
        {'统计项': '成功次数', '数值': monthlyStats.successCount},
        {'统计项': '失败次数', '数值': monthlyStats.failedCount},
        {'统计项': '成功率', '数值': monthlyStats.successRate + '%'},
        {'统计项': '采集数据总量', '数值': monthlyStats.totalData},
        {'统计项': '日均采集', '数值': monthlyStats.dailyAvg},
        {'统计项': '峰值日采集', '数值': monthlyStats.peakData},
        {'统计项': '采集成功率', '数值': monthlyStats.dataSuccessRate + '%'}
      ]
      break
    case 'robot':
      filename = '机器人利用率'
      headers = ['机器人名称', '状态', '执行次数', '成功率', '运行时长', '采集数据', '最后执行']
      data = robotList.value.map(item => ({
        '机器人名称': item.name,
        '状态': item.statusText,
        '执行次数': item.execCount,
        '成功率': item.successRate + '%',
        '运行时长': item.runtime,
        '采集数据': item.dataCount,
        '最后执行': item.lastRun
      }))
      break
    case 'process':
      filename = '流程耗时排行'
      headers = ['流程名称', '流程编码', '执行次数', '平均耗时', '最长耗时', '最短耗时', '总耗时', '成功率']
      data = processList.value.map(item => ({
        '流程名称': item.name,
        '流程编码': item.code,
        '执行次数': item.execCount,
        '平均耗时': item.avgDuration,
        '最长耗时': item.maxDuration,
        '最短耗时': item.minDuration,
        '总耗时': item.totalDuration,
        '成功率': item.successRate + '%'
      }))
      break
    case 'custom':
      filename = '自定义报表'
      headers = ['报表名称', '报表类型', '统计维度', '创建人', '创建时间', '最后运行']
      data = customReports.value.map(item => ({
        '报表名称': item.name,
        '报表类型': item.type,
        '统计维度': item.dimensions,
        '创建人': item.createUser,
        '创建时间': item.createTime,
        '最后运行': item.lastRun
      }))
      break
    default:
      ElMessage.warning('暂不支持导出该报表类型')
      return
  }

  // 生成CSV内容
  if (data.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  const csvHeaders = Object.keys(data[0])
  const csvContent = [
    csvHeaders.join(','),
    ...data.map(row => csvHeaders.map(h => `"${(row[h] || '').toString().replace(/"/g, '""')}"`).join(','))
  ].join('\n')

  // 添加BOM以支持Excel正确显示中文
  const bom = '\uFEFF'
  const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  URL.revokeObjectURL(url)
  ElMessage.success(`${filename}导出成功，共 ${data.length} 条记录`)
}

const createCustomReport = () => {
  Object.assign(reportForm, { name: '', type: '任务执行', dimensions: [], dateRange: 'month', chartType: 'line', description: '' })
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
  const rpaCost = costForm.rpaCount * costForm.rpaCostPerUnit

  savings.monthlyHours = Math.round(monthlyManualHours * 100) / 100
  savings.monthlyCost = Math.round(monthlyCost * 100) / 100
  savings.yearlyHours = Math.round(savings.monthlyHours * 12 * 100) / 100
  savings.yearlyCost = Math.round(savings.monthlyCost * 12 * 100) / 100
  savings.roi = Math.round((savings.yearlyCost / rpaCost) * 100)
  savings.paybackPeriod = Math.round((rpaCost / savings.monthlyCost) * 10) / 10
  showResult.value = true
}

const resetCalculator = () => {
  Object.assign(costForm, { rpaCount: 5, rpaCostPerUnit: 50000, manualTime: 30, frequency: 100, hourlyRate: 100 })
  showResult.value = false
}

const addSubscription = () => {
  Object.assign(subForm, { name: '', report: 'daily', frequency: 'daily', channel: 'email', recipients: '' })
  subDialogVisible.value = true
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
  if (!reportForm.name) {
    ElMessage.warning('请输入报表名称')
    return
  }
  customReports.value.push({
    id: Date.now(),
    ...reportForm,
    dimensions: reportForm.dimensions.join(','),
    createUser: 'admin',
    createTime: new Date().toLocaleString(),
    lastRun: '从未运行'
  })
  reportDialogVisible.value = false
  ElMessage.success('报表创建成功')
}

const saveSubscription = () => {
  if (!subForm.name || !subForm.recipients) {
    ElMessage.warning('请填写完整信息')
    return
  }
  subscriptions.value.push({
    id: Date.now(),
    ...subForm,
    status: true,
    lastRun: '从未发送'
  })
  subDialogVisible.value = false
  ElMessage.success('订阅创建成功')
}

// 加载真实统计数据
const loadStats = async () => {
  try {
    // 从执行日志接口获取数据
    const logResult = await apiGet('/log')
    if (logResult.code === 0) {
      const logs = logResult.data || []

      // 计算统计数据
      const total = logs.length
      const success = logs.filter(l => l.status === 'success' || l.status === 'completed').length
      const failed = logs.filter(l => l.status === 'failed').length
      const running = logs.filter(l => l.status === 'running').length
      const totalDataCount = logs.reduce((sum, l) => sum + (l.dataCount || 0), 0)

      // 更新概览统计
      stats.totalTasks = total
      stats.successRate = total > 0 ? Math.round((success / total) * 100 * 10) / 10 : 0
      stats.savedHours = Math.round(total * 0.5 * 10) / 10

      // 更新日报数据
      dailyStats.total = total
      dailyStats.success = success
      dailyStats.failed = failed
      dailyStats.running = running

      // 更新数据
      dailyLogs.value = logs.slice(0, 50)
    }

    // 从机器人接口获取数据
    const robotResult = await apiGet('/robot')
    if (robotResult.code === 0) {
      const robots = robotResult.data || []
      const busy = robots.filter(r => r.status === 'busy').length
      const idle = robots.filter(r => r.status === 'idle').length
      const offline = robots.filter(r => r.status === 'offline').length

      robotStats.busyRate = robots.length > 0 ? Math.round((busy / robots.length) * 100) : 0
      robotStats.idleRate = robots.length > 0 ? Math.round((idle / robots.length) * 100) : 0
      robotStats.offlineRate = robots.length > 0 ? Math.round((offline / robots.length) * 100) : 0

      robotList.value = robots.slice(0, 10).map(r => ({
        name: r.name,
        status: r.status,
        statusText: r.status === 'idle' ? '空闲' : r.status === 'busy' ? '忙碌' : '离线',
        execCount: r.totalExecutions || 0,
        successRate: r.totalExecutions > 0 ? Math.round((r.successExecutions / r.totalExecutions) * 100) : 0,
        runtime: formatRuntime(r.totalRuntime || 0),
        dataCount: 0,
        lastRun: r.lastExecutionTime || '-'
      }))
    }

    // 从流程接口获取数据
    const processResult = await apiGet('/process')
    if (processResult.code === 0) {
      const processes = processResult.data || []
      processList.value = processes.slice(0, 10).map(p => ({
        name: p.name,
        code: p.code,
        execCount: p.taskCount || p.todayExecutions || 0,
        avgDuration: '5m00s',
        maxDuration: '10m00s',
        minDuration: '2m00s',
        totalDuration: '50h00m',
        successRate: 95
      }))

      // 更新月度统计
      monthlyStats.totalExecutions = processes.reduce((sum, p) => sum + (p.taskCount || 0), 0)
      monthlyStats.totalData = processes.reduce((sum, p) => sum + (p.totalDataCount || 0), 0)
    }

    // 从队列接口获取数据
    const queueResult = await apiGet('/queue')
    if (queueResult.code === 0) {
      const queues = queueResult.data || []
      const totalPending = queues.reduce((sum, q) => sum + (q.currentPendingCount || 0), 0)
      const totalRunning = queues.reduce((sum, q) => sum + (q.currentRunningCount || 0), 0)
      stats.pendingTasks = totalPending
      stats.runningTasks = totalRunning
    }

    // 从触发器接口获取数据
    const triggerResult = await apiGet('/trigger')
    if (triggerResult.code === 0) {
      const triggers = triggerResult.data || []
      const totalTriggers = triggers.reduce((sum, t) => sum + (t.totalTriggers || 0), 0)
      const successTriggers = triggers.reduce((sum, t) => sum + (t.successTriggers || 0), 0)
      stats.totalTriggers = totalTriggers
      stats.triggerSuccessRate = totalTriggers > 0 ? Math.round((successTriggers / totalTriggers) * 100) : 0
    }

  } catch (e) {
    console.error('加载报表数据失败:', e)
  }
}

// 格式化运行时长
const formatRuntime = (seconds) => {
  if (!seconds) return '0h'
  const hours = Math.floor(seconds / 3600)
  return hours + 'h'
}

onMounted(() => { loadStats() })
</script>

<style scoped>
.report-page { max-width: 1600px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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
  flex: 1;
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

.stat-trend {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.stat-trend.up { color: #67c23a; background: rgba(103, 194, 58, 0.1); }
.stat-trend.down { color: #f56c6c; background: rgba(245, 108, 108, 0.1); }

/* 快捷入口 */
.quick-entry {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  color: #606266;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.quick-item:hover {
  background: #409eff;
  color: white;
}

.quick-item .el-icon { font-size: 16px; }

/* 报表部分 */
.report-section {
  background: white;
  padding: 24px;
  border-radius: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e3a4a;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 迷你统计 */
.stats-row-mini {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-mini {
  padding: 16px;
  border-radius: 8px;
  text-align: center;
}

.stat-mini .value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 4px;
}

.stat-mini .label {
  font-size: 12px;
  color: #fff;
}

.stat-mini.success { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-mini.danger { background: linear-gradient(135deg, #f56c6c, #f78989); }
.stat-mini.warning { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.stat-mini.info { background: linear-gradient(135deg, #909399, #a6a9ad); }

/* 图表 */
.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-row.full {
  grid-template-columns: 1fr;
}

.chart-container {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
}

.chart-container h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e3a4a;
  margin-bottom: 12px;
}

.detail-table, .process-table, .robot-table {
  margin-top: 20px;
}

.detail-table h4, .process-table h4, .robot-table h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e3a4a;
  margin-bottom: 12px;
}

/* 月度汇总 */
.monthly-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.summary-card {
  background: #fafafa;
  padding: 20px;
  border-radius: 8px;
}

.summary-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e3a4a;
  margin-bottom: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  padding: 12px;
  background: white;
  border-radius: 6px;
}

.summary-item .label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.summary-item .value {
  font-size: 18px;
  font-weight: 600;
}

.summary-item .value.primary { color: #409eff; }
.summary-item .value.success { color: #67c23a; }
.summary-item .value.danger { color: #f56c6c; }

/* 机器人 */
.robot-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.robot-stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.robot-stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.robot-stat-icon.busy { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.robot-stat-icon.idle { background: linear-gradient(135deg, #67c23a, #85ce61); }
.robot-stat-icon.offline { background: linear-gradient(135deg, #909399, #a6a9ad); }
.robot-stat-icon.runtime { background: linear-gradient(135deg, #409eff, #66b1ff); }

.robot-stat-info {
  display: flex;
  flex-direction: column;
}

.robot-stat-info .value {
  font-size: 20px;
  font-weight: bold;
  color: #1e3a4a;
}

.robot-stat-info .label {
  font-size: 12px;
  color: #8c8c8c;
}

.text-success { color: #67c23a; font-weight: 600; }
.text-warning { color: #e6a23c; font-weight: 600; }
.text-danger { color: #f56c6c; font-weight: 600; }

/* ROI */
.roi-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.roi-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.roi-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.roi-card.primary .roi-icon { background: linear-gradient(135deg, #409eff, #66b1ff); }
.roi-card.success .roi-icon { background: linear-gradient(135deg, #67c23a, #85ce61); }
.roi-card.warning .roi-icon { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.roi-card.info .roi-icon { background: linear-gradient(135deg, #909399, #a6a9ad); }

.roi-info { display: flex; flex-direction: column; }
.roi-value { font-size: 22px; font-weight: bold; color: #1e3a4a; }
.roi-label { font-size: 12px; color: #8c8c8c; }

/* 成本计算 */
.cost-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.cost-calculator, .cost-result {
  border-radius: 12px;
}

.unit {
  margin-left: 8px;
  color: #8c8c8c;
}

.savings-result { display: grid; gap: 12px; }

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

.savings-label { color: #8c8c8c; }
.savings-item.highlight .savings-label { color: rgba(255,255,255,0.8); }

.savings-value {
  font-weight: 600;
  font-size: 16px;
}

.savings-value.primary { color: #409eff; }
.savings-value.success { color: #67c23a; }

/* 预测 */
.forecast-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.forecast-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.forecast-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.forecast-icon.warning { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.forecast-icon.success { background: linear-gradient(135deg, #67c23a, #85ce61); }

.forecast-info { display: flex; flex-direction: column; }
.forecast-value { font-size: 18px; font-weight: bold; color: #1e3a4a; }
.forecast-label { font-size: 12px; color: #8c8c8c; }

.forecast-chart {
  background: #fafafa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.forecast-chart h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e3a4a;
  margin-bottom: 12px;
}

.forecast-tips { margin-top: 16px; }

.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.report-tabs :deep(.el-tabs__content) { padding: 0; }

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .chart-row { grid-template-columns: 1fr; }
  .monthly-summary { grid-template-columns: 1fr; }
  .robot-overview { grid-template-columns: repeat(2, 1fr); }
  .roi-cards { grid-template-columns: repeat(2, 1fr); }
  .cost-section { grid-template-columns: 1fr; }
  .forecast-cards { grid-template-columns: 1fr; }
}
</style>
