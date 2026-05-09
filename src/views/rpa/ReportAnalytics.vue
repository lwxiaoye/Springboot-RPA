<template>
  <div class="report-page">
    <div class="page-header">
      <h2>{{ t('report.title') }}</h2>
      <p class="page-desc">{{ t('report.subtitle') }}</p>
    </div>

    <!-- 统计概览卡片 -->
    <div class="stats-row">
      <div class="stat-card" :class="{ 'stat-card-active': currentTab === 'daily' }" @click="switchToTab('daily')">
        <div class="stat-card-bg"></div>
        <div class="stat-icon-wrap primary">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ formatNumber(stats.totalTasks) }}</span>
          <span class="stat-label">{{ t('report.totalTasks') }}</span>
        </div>
        <div class="stat-meta">
          <div class="stat-trend up">
            <el-icon><Top /></el-icon>
            <span>12.5%</span>
          </div>
          <span class="stat-period">{{ t('report.comparedToLastWeek') }}</span>
        </div>
      </div>

      <div class="stat-card" :class="{ 'stat-card-active': currentTab === 'daily' }" @click="switchToTab('daily')">
        <div class="stat-card-bg"></div>
        <div class="stat-icon-wrap success">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.successRate }}<span class="stat-unit">%</span></span>
          <span class="stat-label">{{ t('report.taskSuccessRate') }}</span>
        </div>
        <div class="stat-meta">
          <div class="stat-trend up">
            <el-icon><Top /></el-icon>
            <span>2.3%</span>
          </div>
          <span class="stat-period">{{ t('report.comparedToLastWeek') }}</span>
        </div>
      </div>

      <div class="stat-card" :class="{ 'stat-card-active': currentTab === 'process' }" @click="switchToTab('process')">
        <div class="stat-card-bg"></div>
        <div class="stat-icon-wrap warning">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.avgDuration }}<span class="stat-unit">h</span></span>
          <span class="stat-label">{{ t('report.avgDuration') }}</span>
        </div>
        <div class="stat-meta">
          <div class="stat-trend down">
            <el-icon><Bottom /></el-icon>
            <span>8.2%</span>
          </div>
          <span class="stat-period">{{ t('report.comparedToLastWeek') }}</span>
        </div>
      </div>

      <div class="stat-card" :class="{ 'stat-card-active': currentTab === 'daily' }" @click="switchToTab('daily')">
        <div class="stat-card-bg"></div>
        <div class="stat-icon-wrap danger">
          <el-icon><Money /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ formatNumber(stats.savedHours) }}</span>
          <span class="stat-label">{{ t('report.savedHours') }}</span>
        </div>
        <div class="stat-meta">
          <div class="stat-trend up">
            <el-icon><Top /></el-icon>
            <span>15.8%</span>
          </div>
          <span class="stat-period">较上周</span>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-entry">
      <div class="quick-item" @click="switchToTab('daily')">
        <el-icon><Document /></el-icon>
        <span>{{ t('report.dailyReport') }}</span>
      </div>
      <div class="quick-item" @click="switchToTab('monthly')">
        <el-icon><Calendar /></el-icon>
        <span>{{ t('report.monthlyReport') }}</span>
      </div>
      <div class="quick-item" @click="switchToTab('robot')">
        <el-icon><Cpu /></el-icon>
        <span>{{ t('report.robotUtilization') }}</span>
      </div>
      <div class="quick-item" @click="switchToTab('process')">
        <el-icon><Stopwatch /></el-icon>
        <span>{{ t('report.processDuration') }}</span>
      </div>
      <div class="quick-item" @click="switchToTab('custom')">
        <el-icon><Edit /></el-icon>
        <span>{{ t('report.customReport') }}</span>
      </div>
    </div>

    <el-tabs v-model="currentTab" class="report-tabs" @tab-change="onTabChange">
      <!-- 任务执行日报 -->
      <el-tab-pane :label="t('report.dailyReport')" name="daily">
        <div class="report-section">
          <div class="section-header">
            <h3>{{ t('report.todayOverview') }}</h3>
            <div class="header-actions">
              <el-date-picker v-model="dailyDate" type="date" :placeholder="t('report.selectDate')" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
              <el-button type="primary" @click="exportReport('daily')">
                <el-icon><Download /></el-icon> {{ t('report.export') }}
              </el-button>
            </div>
          </div>

          <div class="stats-row-mini">
            <div class="stat-mini success">
              <span class="value">{{ dailyStats.success }}</span>
              <span class="label">{{ t('report.success') }}</span>
            </div>
            <div class="stat-mini danger">
              <span class="value">{{ dailyStats.failed }}</span>
              <span class="label">{{ t('report.failed') }}</span>
            </div>
            <div class="stat-mini warning">
              <span class="value">{{ dailyStats.running }}</span>
              <span class="label">{{ t('report.running') }}</span>
            </div>
            <div class="stat-mini info">
              <span class="value">{{ dailyStats.total }}</span>
              <span class="label">{{ t('report.total') }}</span>
            </div>
          </div>

          <div class="chart-row">
            <div class="chart-container">
              <h4>{{ t('report.hourlyTrend') }}</h4>
              <v-chart :option="hourlyTrendOption" autoresize style="height: 280px;"></v-chart>
            </div>
            <div class="chart-container small">
              <h4>{{ t('report.statusDistribution') }}</h4>
              <v-chart :option="dailyPieOption" autoresize style="height: 280px;"></v-chart>
            </div>
          </div>

          <div class="detail-table">
            <h4>{{ t('report.todayDetail') }}</h4>
            <el-table :data="dailyLogs" border stripe size="small" max-height="300" default-sort="{ prop: 'startTime', order: 'descending' }">
              <el-table-column type="index" :label="t('report.seq')" width="60" align="center" />
              <el-table-column prop="taskName" :label="t('report.taskName')" min-width="150" show-overflow-tooltip />
              <el-table-column prop="processName" :label="t('report.process')" min-width="120" />
              <el-table-column prop="status" :label="t('report.status')" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="dataCount" :label="t('report.dataCollected')" width="90" align="center" />
              <el-table-column prop="startTime" :label="t('report.startTime')" min-width="150" sortable :sort-method="(a, b) => new Date(b.startTime) - new Date(a.startTime)" />
              <el-table-column prop="duration" :label="t('report.duration')" width="80" align="center" />
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- 任务执行月报 -->
      <el-tab-pane :label="t('report.monthlyReport')" name="monthly">
        <div class="report-section">
          <div class="section-header">
            <h3>{{ currentMonth }} {{ t('report.monthlySummary') }}</h3>
            <div class="header-actions">
              <el-date-picker v-model="monthlyDate" type="month" :placeholder="t('report.selectMonth')" format="YYYY-MM" value-format="YYYY-MM" />
              <el-button type="primary" @click="exportReport('monthly')">
                <el-icon><Download /></el-icon> {{ t('report.export') }}
              </el-button>
            </div>
          </div>

          <div class="monthly-summary">
            <div class="summary-card">
              <div class="summary-title">{{ t('report.monthExecStats') }}</div>
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="label">{{ t('report.totalExecutions') }}</span>
                  <span class="value primary">{{ formatNumber(monthlyStats.totalExecutions) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">{{ t('report.successCount') }}</span>
                  <span class="value success">{{ formatNumber(monthlyStats.successCount) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">{{ t('report.failedCount') }}</span>
                  <span class="value danger">{{ formatNumber(monthlyStats.failedCount) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">{{ t('report.successRate') }}</span>
                  <span class="value">{{ monthlyStats.successRate }}%</span>
                </div>
              </div>
            </div>
            <div class="summary-card">
              <div class="summary-title">{{ t('report.dataCollectionStats') }}</div>
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="label">{{ t('report.totalDataCollected') }}</span>
                  <span class="value primary">{{ formatNumber(monthlyStats.totalData) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">{{ t('report.dailyAvg') }}</span>
                  <span class="value">{{ formatNumber(monthlyStats.dailyAvg) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">{{ t('report.peakData') }}</span>
                  <span class="value">{{ formatNumber(monthlyStats.peakData) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">{{ t('report.dataSuccessRate') }}</span>
                  <span class="value success">{{ monthlyStats.dataSuccessRate }}%</span>
                </div>
              </div>
            </div>
          </div>

          <div class="chart-row full">
            <div class="chart-container">
              <h4>{{ t('report.monthlyTrend') }}</h4>
              <v-chart :option="monthlyTrendOption" autoresize style="height: 300px;"></v-chart>
            </div>
          </div>

          <div class="chart-row">
            <div class="chart-container">
              <h4>{{ t('report.processDistribution') }}</h4>
              <v-chart :option="processPieOption" autoresize style="height: 280px;"></v-chart>
            </div>
            <div class="chart-container">
              <h4>{{ t('report.dailyDataTrend') }}</h4>
              <v-chart :option="dataCollectTrendOption" autoresize style="height: 280px;"></v-chart>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 机器人利用率 -->
      <el-tab-pane :label="t('report.robotUtilization')" name="robot">
        <div class="report-section">
          <div class="section-header">
            <h3>{{ t('report.robotWorkloadAnalysis') }}</h3>
            <div class="header-actions">
              <el-date-picker v-model="robotDateRange" type="daterange" :range-separator="t('common.to')" :start-placeholder="t('common.startDate')" :end-placeholder="t('common.endDate')" />
              <el-button type="primary" @click="exportReport('robot')">
                <el-icon><Download /></el-icon> {{ t('report.export') }}
              </el-button>
            </div>
          </div>

          <div class="robot-overview">
            <div class="robot-stat-card">
              <div class="robot-stat-icon busy"><el-icon><Cpu /></el-icon></div>
              <div class="robot-stat-info">
                <span class="value">{{ robotStats.busyRate }}%</span>
                <span class="label">{{ t('report.busyRate') }}</span>
              </div>
            </div>
            <div class="robot-stat-card">
              <div class="robot-stat-icon idle"><el-icon><Coffee /></el-icon></div>
              <div class="robot-stat-info">
                <span class="value">{{ robotStats.idleRate }}%</span>
                <span class="label">{{ t('report.idleRate') }}</span>
              </div>
            </div>
            <div class="robot-stat-card">
              <div class="robot-stat-icon offline"><el-icon><CloseBold /></el-icon></div>
              <div class="robot-stat-info">
                <span class="value">{{ robotStats.offlineRate }}%</span>
                <span class="label">{{ t('report.offlineRate') }}</span>
              </div>
            </div>
            <div class="robot-stat-card">
              <div class="robot-stat-icon runtime"><el-icon><Timer /></el-icon></div>
              <div class="robot-stat-info">
                <span class="value">{{ robotStats.totalRuntime }}</span>
                <span class="label">{{ t('report.totalRuntime') }}</span>
              </div>
            </div>
          </div>

          <div class="chart-row">
            <div class="chart-container">
              <h4>{{ t('report.robotStatusDistribution') }}</h4>
              <v-chart :option="robotStatusOption" autoresize style="height: 280px;"></v-chart>
            </div>
            <div class="chart-container">
              <h4>{{ t('report.robotRanking') }}</h4>
              <v-chart :option="robotRankingOption" autoresize style="height: 280px;"></v-chart>
            </div>
          </div>

          <div class="robot-table">
            <h4>{{ t('report.robotDetailData') }}</h4>
            <el-table :data="robotList" border stripe size="small" default-sort="{ prop: 'lastRun', order: 'descending' }">
              <el-table-column type="index" :label="t('report.seq')" width="60" align="center" />
              <el-table-column prop="name" :label="t('report.robotName')" min-width="150" />
              <el-table-column prop="status" :label="t('report.status')" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="getRobotStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="execCount" :label="t('report.execCount')" width="100" align="center" sortable />
              <el-table-column prop="successRate" :label="t('report.successRate')" width="100" align="center" sortable>
                <template #default="{ row }">
                  <span :class="row.successRate >= 90 ? 'text-success' : row.successRate >= 70 ? 'text-warning' : 'text-danger'">
                    {{ row.successRate }}%
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="runtime" :label="t('report.runtime')" width="100" align="center" />
              <el-table-column prop="dataCount" :label="t('report.dataCollected')" width="100" align="center" />
              <el-table-column prop="lastRun" :label="t('report.lastRun')" min-width="150" sortable :sort-method="(a, b) => new Date(b.lastRun) - new Date(a.lastRun)" />
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- 流程耗时排行 -->
      <el-tab-pane :label="t('report.processDuration')" name="process">
        <div class="report-section">
          <div class="section-header">
            <h3>{{ t('report.processEfficiencyAnalysis') }}</h3>
            <div class="header-actions">
              <el-select v-model="processTopN" style="width: 120px;">
                <el-option :label="t('report.topN', { n: 5 })" :value="5" />
                <el-option :label="t('report.topN', { n: 10 })" :value="10" />
                <el-option :label="t('report.topN', { n: 20 })" :value="20" />
              </el-select>
              <el-date-picker v-model="processDateRange" type="daterange" :range-separator="t('common.to')" :start-placeholder="t('common.startDate')" :end-placeholder="t('common.endDate')" />
              <el-button type="primary" @click="exportReport('process')">
                <el-icon><Download /></el-icon> {{ t('report.export') }}
              </el-button>
            </div>
          </div>

          <div class="chart-row full">
            <div class="chart-container">
              <h4>{{ t('report.longestProcessTop', { n: processTopN }) }}</h4>
              <v-chart :option="processDurationOption" autoresize style="height: 350px;"></v-chart>
            </div>
          </div>

          <div class="process-table">
            <h4>{{ t('report.processDurationDetail') }}</h4>
            <el-table :data="processList" border stripe size="small" default-sort="{ prop: 'avgDuration', order: 'descending' }">
              <el-table-column type="index" :label="t('report.rank')" width="60" align="center" />
              <el-table-column prop="name" :label="t('report.processName')" min-width="180" />
              <el-table-column prop="code" :label="t('report.processCode')" width="150" />
              <el-table-column prop="execCount" :label="t('report.execCount')" width="100" align="center" sortable />
              <el-table-column prop="avgDuration" :label="t('report.avgDuration')" width="100" align="center" sortable sort-by="avgDuration">
                <template #default="{ row }">
                  <span class="text-warning">{{ row.avgDuration }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="maxDuration" :label="t('report.maxDuration')" width="100" align="center" sortable />
              <el-table-column prop="minDuration" :label="t('report.minDuration')" width="100" align="center" />
              <el-table-column prop="totalDuration" :label="t('report.totalDuration')" width="100" align="center" />
              <el-table-column prop="successRate" :label="t('report.successRate')" width="90" align="center" sortable>
                <template #default="{ row }">
                  <span :class="row.successRate >= 90 ? 'text-success' : 'text-danger'">{{ row.successRate }}%</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- 自定义报表 -->
      <el-tab-pane :label="t('report.customReport')" name="custom">
        <div class="report-section">
          <div class="toolbar">
            <el-button type="primary" @click="createCustomReport">
              <el-icon><Plus /></el-icon> {{ t('report.createReport') }}
            </el-button>
            <el-button @click="exportReport('custom')">
              <el-icon><Download /></el-icon> {{ t('report.export') }}
            </el-button>
          </div>

          <el-table :data="customReports" v-loading="loading" border stripe>
            <el-table-column type="index" :label="t('report.seq')" width="60" align="center" />
            <el-table-column prop="name" :label="t('report.reportName')" min-width="180" />
            <el-table-column prop="type" :label="t('report.reportType')" width="130" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getReportTypeTag(row.type)">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dimensions" :label="t('report.dimensions')" min-width="150" />
            <el-table-column prop="createUser" :label="t('report.creator')" width="100" />
            <el-table-column prop="createTime" :label="t('report.createTime')" min-width="160" />
            <el-table-column prop="lastRun" :label="t('report.lastRun')" min-width="160" />
            <el-table-column :label="t('common.actions')" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="runReport(row)">{{ t('common.run') }}</el-button>
                <el-button link type="primary" size="small" @click="editCustomReport(row)">{{ t('common.edit') }}</el-button>
                <el-popconfirm :title="t('report.confirmDeleteReport')" @confirm="deleteCustomReport(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">{{ t('common.delete') }}</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 趋势预测 -->
      <el-tab-pane :label="t('report.forecast')" name="forecast">
        <div class="report-section">
          <div class="section-header">
            <h3>{{ t('report.intelligentTaskForecast') }}</h3>
            <div class="header-actions">
              <el-select v-model="forecastPeriod" style="width: 150px;">
                <el-option :label="t('report.nextDays', { days: 7 })" value="7" />
                <el-option :label="t('report.nextDays', { days: 30 })" value="30" />
                <el-option :label="t('report.nextDays', { days: 90 })" value="90" />
              </el-select>
            </div>
          </div>

          <div class="forecast-cards">
            <div class="forecast-card">
              <div class="forecast-icon"><el-icon><TrendCharts /></el-icon></div>
              <div class="forecast-info">
                <span class="forecast-value">{{ t('report.expectedGrowth', { rate: forecastStats.growthRate }) }}</span>
                <span class="forecast-label">{{ t('report.nextPeriodForecast') }}</span>
              </div>
            </div>
            <div class="forecast-card">
              <div class="forecast-icon warning"><el-icon><Warning /></el-icon></div>
              <div class="forecast-info">
                <span class="forecast-value">{{ t('report.suggestExpand', { count: forecastStats.suggestRobotCount }) }}</span>
                <span class="forecast-label">{{ t('report.robotExpandSuggestion') }}</span>
              </div>
            </div>
            <div class="forecast-card">
              <div class="forecast-icon success"><el-icon><Check /></el-icon></div>
              <div class="forecast-info">
                <span class="forecast-value">{{ forecastStats.capacityRate }}%</span>
                <span class="forecast-label">{{ t('report.currentCapacityUtilization') }}</span>
              </div>
            </div>
          </div>

          <div class="forecast-chart">
            <h4>{{ t('report.futureTaskTrendForecast') }}</h4>
            <v-chart :option="forecastChartOption" autoresize style="height: 350px;"></v-chart>
          </div>

          <div class="forecast-tips">
            <el-alert type="info" :closable="false">
              <template #title>
                <strong>{{ t('report.intelligentSuggestion') }}:</strong> {{ forecastStats.suggestion }}
              </template>
            </el-alert>
          </div>
        </div>
      </el-tab-pane>

      <!-- 报表订阅 -->
      <el-tab-pane :label="t('report.subscription')" name="subscription">
        <div class="report-section">
          <div class="toolbar">
            <el-button type="primary" @click="addSubscription">
              <el-icon><Plus /></el-icon> {{ t('report.addSubscription') }}
            </el-button>
            <el-button type="success" @click="triggerSubscription" :loading="triggerLoading">
              <el-icon><Promotion /></el-icon> {{ t('report.sendNow') }}
            </el-button>
          </div>

          <el-table :data="subscriptions" v-loading="subLoading" border stripe>
            <el-table-column type="index" :label="t('report.seq')" width="60" align="center" />
            <el-table-column prop="name" :label="t('report.subscriptionName')" min-width="180" />
            <el-table-column prop="report" :label="t('report.reportType')" min-width="130">
              <template #default="{ row }">
                <el-tag size="small" :type="getReportTypeTag(row.reportType)">{{ getReportTypeName(row.reportType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="frequency" :label="t('report.frequency')" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ getFrequencyName(row.frequency) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="recipients" :label="t('report.recipients')" min-width="150" show-overflow-tooltip />
            <el-table-column prop="channel" :label="t('report.pushChannel')" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ getChannelName(row.channel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="t('report.status')" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" @change="toggleSubscription(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="lastSendTime" :label="t('report.lastSend')" min-width="160">
              <template #default="{ row }">
                {{ row.lastSendTime || t('report.neverSent') }}
              </template>
            </el-table-column>
            <el-table-column :label="t('common.actions')" width="150" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="triggerOneSubscription(row)">
                  <el-icon><VideoPlay /></el-icon> {{ t('report.send') }}
                </el-button>
                <el-button link type="primary" size="small" @click="editSubscription(row)">{{ t('common.edit') }}</el-button>
                <el-popconfirm :title="t('report.confirmDelete')" @confirm="deleteSubscription(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">{{ t('common.delete') }}</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <div class="subscription-tips">
            <el-alert type="info" :closable="false">
              <template #title>
                <strong>{{ t('report.usageInstructions') }}：</strong>
                <span style="margin-left: 8px;">{{ t('report.subscriptionDescription') }}</span>
                <span style="margin-left: 8px; color: #67c23a;">{{ t('report.manualTriggerHint') }}</span>
              </template>
            </el-alert>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建自定义报表弹窗 -->
    <el-dialog v-model="reportDialogVisible" :title="t('report.createCustomReport')" width="650px">
      <el-form :model="reportForm" label-width="100px">
        <el-form-item :label="t('report.reportName')">
          <el-input v-model="reportForm.name" :placeholder="t('report.inputReportName')" />
        </el-form-item>
        <el-form-item :label="t('report.reportType')">
          <el-select v-model="reportForm.type" style="width: 100%;">
            <el-option :label="t('report.taskExecutionStats')" value="任务执行" />
            <el-option :label="t('report.dataCollectionStats')" value="数据采集" />
            <el-option :label="t('report.robotEfficiency')" value="机器人效能" />
            <el-option :label="t('report.processEfficiency')" value="流程效率" />
            <el-option :label="t('report.costBenefit')" value="成本效益" />
            <el-option :label="t('report.custom')" value="自定义" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('report.dimensions')">
          <el-select v-model="reportForm.dimensions" multiple :placeholder="t('report.selectDimensions')" style="width: 100%;">
            <el-option :label="t('report.execCount')" value="execCount" />
            <el-option :label="t('report.successRate')" value="successRate" />
            <el-option :label="t('report.dataCount')" value="dataCount" />
            <el-option :label="t('report.duration')" value="duration" />
            <el-option :label="t('report.processDistribution')" value="process" />
            <el-option :label="t('report.timeTrend')" value="trend" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('report.timeRange')">
          <el-select v-model="reportForm.dateRange" style="width: 100%;">
            <el-option :label="t('report.today')" value="today" />
            <el-option :label="t('report.thisWeek')" value="week" />
            <el-option :label="t('report.thisMonth')" value="month" />
            <el-option :label="t('report.thisQuarter')" value="quarter" />
            <el-option :label="t('report.thisYear')" value="year" />
            <el-option :label="t('report.custom')" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('report.chartType')">
          <el-select v-model="reportForm.chartType" style="width: 100%;">
            <el-option :label="t('report.lineChart')" value="line" />
            <el-option :label="t('report.barChart')" value="bar" />
            <el-option :label="t('report.pieChart')" value="pie" />
            <el-option :label="t('report.table')" value="table" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('report.description')">
          <el-input v-model="reportForm.description" type="textarea" :rows="2" :placeholder="t('report.inputDescription')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveCustomReport">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 添加订阅弹窗 -->
    <el-dialog v-model="subDialogVisible" :title="isEditSubscription ? t('report.editSubscription') : t('report.addSubscription')" width="600px">
      <el-form :model="subForm" :rules="subFormRules" ref="subFormRef" label-width="110px">
        <el-form-item :label="t('report.subscriptionName')" prop="name">
          <el-input v-model="subForm.name" :placeholder="t('report.inputSubscriptionName')" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('report.reportType')" prop="reportType">
          <el-select v-model="subForm.reportType" style="width: 100%;">
            <el-option :label="t('report.dailyReport')" value="daily" />
            <el-option :label="t('report.weeklyReport')" value="weekly" />
            <el-option :label="t('report.monthlyReport')" value="monthly" />
            <el-option :label="t('report.robotUtilization')" value="robot" />
            <el-option :label="t('report.roiReport')" value="roi" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('report.frequency')" prop="frequency">
          <el-select v-model="subForm.frequency" style="width: 100%;">
            <el-option :label="t('report.daily')" value="daily" />
            <el-option :label="t('report.weeklyMonday')" value="weekly" />
            <el-option :label="t('report.monthlyFirstDay')" value="monthly" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('report.pushChannel')" prop="channel">
          <el-select v-model="subForm.channel" style="width: 100%;" @change="onChannelChange">
            <el-option :label="t('report.email')" value="email">
              <div class="channel-option">
                <el-icon><Message /></el-icon>
                <span>{{ t('report.email') }}</span>
                <el-tag size="small" type="success">{{ t('report.recommended') }}</el-tag>
              </div>
            </el-option>
            <el-option :label="t('report.dingtalk')" value="dingtalk">
              <div class="channel-option">
                <el-icon><ChatDotRound /></el-icon>
                <span>{{ t('report.dingtalk') }}</span>
              </div>
            </el-option>
            <el-option :label="t('report.wecom')" value="wecom">
              <div class="channel-option">
                <el-icon><Comment /></el-icon>
                <span>{{ t('report.wecom') }}</span>
              </div>
            </el-option>
            <el-option :label="t('report.feishu')" value="feishu">
              <div class="channel-option">
                <el-icon><Promotion /></el-icon>
                <span>{{ t('report.feishu') }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('report.recipients')" prop="recipients">
          <el-input
            v-model="subForm.recipients"
            :placeholder="getChannelTip(subForm.channel)"
            type="textarea"
            :rows="2"
            @blur="validateRecipients"
          />
          <div class="form-tip" v-if="subForm.channel === 'email'">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ t('report.multipleRecipientsTip') }}</span>
          </div>
          <div class="form-error" v-if="recipientError">
            <el-icon><CircleClose /></el-icon>
            <span>{{ recipientError }}</span>
          </div>
        </el-form-item>
        <el-form-item :label="t('report.pushTime')" prop="fixedTime">
          <el-time-picker
            v-model="subForm.fixedTime"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%;"
            :placeholder="t('report.selectPushTime')"
          />
        </el-form-item>
        <el-form-item :label="t('report.status')">
          <el-switch v-model="subForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="subDialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" @click="saveSubscription" :loading="saveLoading">
            <el-icon v-if="!saveLoading"><Check /></el-icon>
            {{ isEditSubscription ? t('report.saveChanges') : t('report.createSubscription') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  DataLine, CircleCheck, Timer, Money, Plus, Download, TrendCharts, PieChart,
  Document, Calendar, Cpu, Stopwatch, Edit, Coin, Clock, User, Top, Bottom,
  Coffee, CloseBold, Warning, Check, Lock, Promotion, VideoPlay,
  Message, ChatDotRound, Comment, InfoFilled, CircleClose
} from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart as EChartsBarChart, PieChart as PieChartType } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { apiGet, apiPost, apiPut, apiDelete } from '../../utils/api.js'

use([CanvasRenderer, LineChart, EChartsBarChart, PieChartType, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const { t } = useI18n()
const currentTab = ref('daily')
const loading = ref(false)
const subLoading = ref(false)
const triggerLoading = ref(false)
const reportDialogVisible = ref(false)
const subDialogVisible = ref(false)
const showResult = ref(false)

// 权限控制
const permissions = reactive({
  canView: true,
  canExport: false,
  canSubscription: false,
  isAdmin: false,
  allPermissions: [],
  loaded: false
})

// 日期选择
const dailyDate = ref(new Date().toISOString().split('T')[0])
const monthlyDate = ref(new Date().toISOString().slice(0, 7))
const robotDateRange = ref([])
const processDateRange = ref([])
const processTopN = ref(10)
const forecastPeriod = ref('30')

// 监听日报日期变化，自动重新加载
watch(dailyDate, (newDate) => {
  if (currentTab.value === 'daily') {
    loadDailyReport()
  }
})

// 监听月报月份变化，自动重新加载
watch(monthlyDate, (newMonth) => {
  if (currentTab.value === 'monthly') {
    loadMonthlyReport()
  }
})

// 监听预测周期变化
watch(forecastPeriod, () => {
  if (currentTab.value === 'forecast') {
    loadForecast()
  }
})

const currentMonth = computed(() => {
  const [year, month] = monthlyDate.value.split('-')
  return `${year}年${parseInt(month)}月`
})

// 统计概览 - 初始为空，从 API 加载
const stats = reactive({
  totalTasks: 0,
  successRate: 0,
  avgDuration: 0,
  savedHours: 0
})

// 日报数据 - 初始为空
const dailyStats = reactive({
  success: 0,
  failed: 0,
  running: 0,
  total: 0
})

const dailyLogs = ref([])

// 月报数据 - 初始为空
const monthlyStats = reactive({
  totalExecutions: 0,
  successCount: 0,
  failedCount: 0,
  successRate: 0,
  totalData: 0,
  dailyAvg: 0,
  peakData: 0,
  dataSuccessRate: 0
})

// 机器人数据 - 初始为空
const robotStats = reactive({
  busyRate: 0,
  idleRate: 0,
  offlineRate: 0,
  totalRuntime: '0h'
})

const robotList = ref([])

// 流程数据 - 初始为空
const processList = ref([])

// ROI数据 - 初始为空
const roiStats = reactive({
  annualSavings: 0,
  roi: 0,
  paybackPeriod: 0,
  laborReduction: 0
})

// 预测数据 - 初始为空
const forecastStats = reactive({
  growthRate: 0,
  suggestRobotCount: 0,
  capacityRate: 0,
  suggestion: t('report.noForecastData')
})

// 自定义报表 - 从 API 加载
const customReports = ref([])

// 订阅列表 - 从 API 加载
const subscriptions = ref([])

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
  reportType: 'daily',
  frequency: 'daily',
  channel: 'email',
  recipients: '',
  fixedTime: '09:00',
  enabled: 1
})

// 是否为编辑模式
const isEditSubscription = ref(false)
const editingSubscriptionId = ref(null)
const saveLoading = ref(false)
const subFormRef = ref(null)
const recipientError = ref('')

// 订阅表单验证规则
const subFormRules = {
  name: [
    { required: true, message: t('report.inputSubscriptionName'), trigger: 'blur' },
    { min: 2, max: 100, message: t('report.subscriptionNameLength'), trigger: 'blur' }
  ],
  reportType: [
    { required: true, message: t('report.selectReportType'), trigger: 'change' }
  ],
  frequency: [
    { required: true, message: t('report.selectFrequency'), trigger: 'change' }
  ],
  channel: [
    { required: true, message: t('report.selectPushChannel'), trigger: 'change' }
  ],
  recipients: [
    { required: true, message: t('report.inputRecipients'), trigger: 'blur' }
  ]
}

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
  // 根据不同 Tab 加载对应数据
  switch (tab) {
    case 'forecast':
      loadForecast()
      break
    case 'monthly':
      loadMonthlyReport()
      break
    case 'roi':
      // ROI 页面不需要预加载，用户点击计算时才调用
      break
  }
}

// 状态标签类型
const getStatusTagType = (status) => {
  const map = { success: 'success', failed: 'danger', running: 'warning', abnormal: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { success: t('report.success'), failed: t('report.failed'), running: t('report.running'), abnormal: t('report.abnormal') }
  return map[status] || status || '-'
}

// 机器人状态
const getRobotStatusType = (status) => {
  const map = { busy: 'warning', idle: 'success', offline: 'info' }
  return map[status] || 'info'
}

// 报表类型标签
const getReportTypeTag = (type) => {
  const map = { '任务执行': 'primary', '数据采集': 'success', '机器人效能': 'warning', '流程效率': 'info', '成本效益': 'danger', 'daily': 'primary', 'weekly': 'success', 'monthly': 'warning', 'robot': 'info' }
  return map[type] || 'info'
}

// 获取报表类型名称
const getReportTypeName = (type) => {
  const map = { 
    'daily': t('report.daily'), 
    'weekly': t('report.weekly'), 
    'monthly': t('report.monthly'), 
    'robot': t('report.robotReport') 
  }
  return map[type] || type || '-'
}

// 获取发送频率名称
const getFrequencyName = (freq) => {
  const map = { 'daily': t('report.daily'), 'weekly': t('report.weekly'), 'monthly': t('report.monthly') }
  return map[freq] || freq || '-'
}

// 获取推送方式名称
const getChannelName = (channel) => {
  const map = { 
    'email': t('report.email'), 
    'dingtalk': t('report.dingtalk'), 
    'wecom': t('report.wecom'), 
    'feishu': t('report.feishu') 
  }
  return map[channel] || channel || '-'
}

// 邮箱格式验证
const validateEmail = (email) => {
  const emailRegex = /^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/
  return emailRegex.test(email)
}

// 获取推送方式提示
const getChannelTip = (channel) => {
  const tips = {
    'email': t('report.emailTip'),
    'dingtalk': t('report.dingtalkTip'),
    'wecom': t('report.wecomTip'),
    'feishu': t('report.feishuTip')
  }
  return tips[channel] || ''
}

// 获取推送方式图标
const getChannelIcon = (channel) => {
  const icons = {
    'email': 'Message',
    'dingtalk': 'ChatDotRound',
    'wecom': 'Comment',
    'feishu': 'Promotion'
  }
  return icons[channel] || 'Message'
}

// 触发所有订阅
const triggerSubscription = async () => {
  try {
    triggerLoading.value = true
    const result = await apiPost('/report/subscription/trigger', {})
    if (result.code === 0) {
      ElMessage.success(t('report.subscriptionSending'))
    } else {
      ElMessage.error(result.message || t('report.sendFailed'))
    }
  } catch (e) {
    console.error(t('report.triggerSubscriptionFailed'), e)
    ElMessage.error(t('report.sendFailedWithMsg', { msg: e.message }))
  } finally {
    triggerLoading.value = false
  }
}

// 触发单个订阅
const triggerOneSubscription = async (sub) => {
  try {
    const result = await apiPost(`/report/subscription/${sub.id}/trigger`, {})
    if (result.code === 0) {
      ElMessage.success(t('report.reportSending', { name: sub.name }))
      await loadSubscriptions()
    } else {
      ElMessage.error(result.message || t('report.sendFailed'))
    }
  } catch (e) {
    console.error(t('report.triggerSubscriptionFailed'), e)
    ElMessage.error(t('report.sendFailedWithMsg', { msg: e.message }))
  }
}

// 图表配置 - 使用真实数据
const hourlyTrendOption = computed(() => {
  // 从 dailyLogs 中计算每小时趋势
  const hourlyData = Array(24).fill(0).map(() => ({ success: 0, failed: 0 }))
  
  dailyLogs.value.forEach(log => {
    if (log.startTime) {
      const hour = new Date(log.startTime).getHours()
      if (hour >= 0 && hour < 24) {
        if (log.status === 'success' || log.status === 'completed') {
          hourlyData[hour].success++
        } else if (log.status === 'failed' || log.status === 'abnormal') {
          hourlyData[hour].failed++
        }
      }
    }
  })
  
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)
    },
    yAxis: { type: 'value', name: t('report.taskCount') },
    series: [
      {
        name: t('report.success'),
        type: 'bar',
        stack: 'total',
        data: hourlyData.map(h => h.success),
        itemStyle: { color: '#67c23a' }
      },
      {
        name: t('report.failed'),
        type: 'bar',
        stack: 'total',
        data: hourlyData.map(h => h.failed),
        itemStyle: { color: '#f56c6c' }
      }
    ]
  }
})

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
      { value: dailyStats.success, name: t('report.success'), itemStyle: { color: '#67c23a' } },
      { value: dailyStats.failed, name: t('report.failed'), itemStyle: { color: '#f56c6c' } },
      { value: dailyStats.running, name: t('report.running'), itemStyle: { color: '#e6a23c' } }
    ]
  }]
}))

const monthlyTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: [t('report.execCount'), t('report.successCount')], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [t('report.day1'), t('report.day5'), t('report.day10'), t('report.day15'), t('report.day20'), t('report.day25'), t('report.day30')]
  },
  yAxis: { type: 'value', name: t('report.count') },
  series: [
    { name: t('report.execCount'), type: 'line', data: [180, 165, 190, 175, 200, 185, 195], smooth: true, itemStyle: { color: '#409eff' } },
    { name: t('report.successCount'), type: 'line', data: [170, 158, 180, 166, 190, 175, 186], smooth: true, itemStyle: { color: '#67c23a' } }
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
      { value: 35, name: t('report.dataCollection'), itemStyle: { color: '#409eff' } },
      { value: 25, name: t('report.orderProcessing'), itemStyle: { color: '#67c23a' } },
      { value: 18, name: t('report.invoiceReview'), itemStyle: { color: '#e6a23c' } },
      { value: 12, name: t('report.reportGeneration'), itemStyle: { color: '#f56c6c' } },
      { value: 10, name: t('report.other'), itemStyle: { color: '#909399' } }
    ]
  }]
}))

const dataCollectTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
  xAxis: { type: 'category', data: [t('report.day1'), t('report.day5'), t('report.day10'), t('report.day15'), t('report.day20'), t('report.day25'), t('report.day30')] },
  yAxis: { type: 'value', name: t('report.dataVolume') },
  series: [{
    name: t('report.collectedData'),
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
      { value: robotStats.busyRate, name: t('report.busy'), itemStyle: { color: '#e6a23c' } },
      { value: robotStats.idleRate, name: t('report.idle'), itemStyle: { color: '#67c23a' } },
      { value: robotStats.offlineRate, name: t('report.offline'), itemStyle: { color: '#909399' } }
    ]
  }]
}))

const robotRankingOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
  xAxis: { type: 'value', name: t('report.execCount') },
  yAxis: { 
    type: 'category', 
    data: robotList.value.slice(0, 10).map(r => r.name).reverse() 
  },
  series: [{
    type: 'bar',
    data: robotList.value.slice(0, 10).map(r => r.execCount).reverse(),
    itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] }
  }]
}))

const processDurationOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: { type: 'value', name: t('report.avgDurationMinutes') },
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
  legend: { data: [t('report.historicalData'), t('report.forecastValue')], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [t('report.monday'), t('report.tuesday'), t('report.wednesday'), t('report.thursday'), t('report.friday'), t('report.saturday'), t('report.sunday'), t('report.monday'), t('report.tuesday'), t('report.wednesday'), t('report.thursday'), t('report.friday'), t('report.saturday'), t('report.sunday')]
  },
  yAxis: { type: 'value', name: t('report.taskCount') },
  series: [
    {
      name: t('report.historicalData'),
      type: 'line',
      data: [120, 132, 101, 134, 190, 230, 210, null, null, null, null, null, null, null],
      itemStyle: { color: '#409eff' },
      areaStyle: { color: 'rgba(64, 158, 255, 0.1)' }
    },
    {
      name: t('report.forecastValue'),
      type: 'line',
      data: [null, null, null, null, null, null, null, 230, 250, 280, 260, 290, 310, 340],
      itemStyle: { color: '#67c23a' },
      lineStyle: { type: 'dashed' },
      areaStyle: { color: 'rgba(103, 194, 58, 0.1)' }
    }
  ]
}))

const createCustomReport = () => {
  Object.assign(reportForm, { name: '', type: '任务执行', dimensions: [], dateRange: 'month', chartType: 'line', description: '' })
  reportDialogVisible.value = true
}

const editCustomReport = (report) => {
  ElMessage.info(t('report.editReport') + report.name)
}

const deleteCustomReport = async (report) => {
  try {
    const result = await apiDelete(`/report/custom/${report.id}`)
    if (result.code === 0) {
      await loadCustomReports()
      ElMessage.success(t('report.reportDeleted'))
    } else {
      ElMessage.error(result.message || t('report.deleteFailed'))
    }
  } catch (e) {
    console.error(t('report.deleteReportFailed'), e)
    ElMessage.error(t('report.deleteReportFailed'))
  }
}

const runReport = async (report) => {
  try {
    const result = await apiPost(`/report/custom/${report.id}/run`, {})
    if (result.code === 0) {
      ElMessage.success(t('report.reportRunSuccess', { name: report.name }))
      await loadCustomReports()
    } else {
      ElMessage.error(result.message || t('report.runFailed'))
    }
  } catch (e) {
    console.error(t('report.runReportFailed'), e)
    ElMessage.error(t('report.runReportFailed'))
  }
}

const calculateSavings = async () => {
  try {
    const payload = {
      rpaCount: costForm.rpaCount,
      rpaCostPerUnit: costForm.rpaCostPerUnit,
      manualTime: costForm.manualTime,
      frequency: costForm.frequency,
      hourlyRate: costForm.hourlyRate
    }
    
    const result = await apiPost('/report/roi/calculate', payload)
    if (result.code === 0 && result.data) {
      const data = result.data
      savings.monthlyHours = data.monthlyHours || 0
      savings.monthlyCost = data.monthlyCost || 0
      savings.yearlyHours = data.yearlyHours || 0
      savings.yearlyCost = data.yearlyCost || 0
      savings.roi = data.roi || 0
      savings.paybackPeriod = data.paybackPeriod || 0
      showResult.value = true
    } else {
      ElMessage.error(result.message || '计算失败')
    }
  } catch (e) {
    console.error('计算ROI失败:', e)
    ElMessage.error('计算失败')
  }
}

const resetCalculator = () => {
  Object.assign(costForm, { rpaCount: 5, rpaCostPerUnit: 50000, manualTime: 30, frequency: 100, hourlyRate: 100 })
  showResult.value = false
}

// 推送方式改变时清空接收人并重置错误
const onChannelChange = () => {
  recipientError.value = ''
}

// 验证接收人格式
const validateRecipients = () => {
  if (!subForm.recipients || !subForm.recipients.trim()) {
    recipientError.value = t('report.recipientsRequired')
    return false
  }

  const recipients = subForm.recipients.split(/[,，;；]/).filter(r => r.trim())

  if (recipients.length === 0) {
    recipientError.value = t('report.recipientsRequired')
    return false
  }

  if (subForm.channel === 'email') {
    for (const email of recipients) {
      if (!validateEmail(email.trim())) {
        recipientError.value = t('report.emailFormatError', { email: email.trim() })
        return false
      }
    }
  }

  recipientError.value = ''
  return true
}

const addSubscription = () => {
  isEditSubscription.value = false
  editingSubscriptionId.value = null
  Object.assign(subForm, {
    name: '',
    reportType: 'daily',
    frequency: 'daily',
    channel: 'email',
    recipients: '',
    fixedTime: '09:00',
    enabled: 1
  })
  recipientError.value = ''
  subDialogVisible.value = true
}

const editSubscription = (sub) => {
  isEditSubscription.value = true
  editingSubscriptionId.value = sub.id
  Object.assign(subForm, {
    name: sub.name,
    reportType: sub.reportType,
    frequency: sub.frequency,
    channel: sub.channel,
    recipients: sub.recipients,
    fixedTime: sub.fixedTime || '09:00',
    enabled: sub.enabled
  })
  recipientError.value = ''
  subDialogVisible.value = true
}

const deleteSubscription = async (sub) => {
  try {
    const result = await apiDelete(`/report/subscription/${sub.id}`)
    if (result.code === 0) {
      await loadSubscriptions()
      ElMessage.success(t('report.subscriptionDeleted'))
    } else {
      ElMessage.error(result.message || t('report.deleteFailed'))
    }
  } catch (e) {
    console.error(t('report.deleteSubscriptionFailed'), e)
    ElMessage.error(t('report.deleteSubscriptionFailed'))
  }
}

const toggleSubscription = async (sub) => {
  try {
    const result = await apiPut(`/report/subscription/${sub.id}/toggle`, {})
    if (result.code === 0) {
      await loadSubscriptions()
      ElMessage.success(result.message || t('common.operationSuccess'))
    } else {
      await loadSubscriptions() // 恢复原状态
      ElMessage.error(result.message || t('common.operationFailed'))
    }
  } catch (e) {
    console.error(t('report.toggleSubscriptionFailed'), e)
    await loadSubscriptions() // 恢复原状态
    ElMessage.error(t('common.operationFailed'))
  }
}

const saveCustomReport = async () => {
  if (!reportForm.name) {
    ElMessage.warning(t('report.inputReportName'))
    return
  }
  try {
    // 从 localStorage 获取当前用户信息
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const userId = userInfo.id || 1
    const userName = userInfo.username || userInfo.name || 'admin'
    
    const payload = {
      name: reportForm.name,
      type: reportForm.type,
      dimensions: JSON.stringify(reportForm.dimensions),
      dateRange: reportForm.dateRange,
      chartType: reportForm.chartType,
      description: reportForm.description,
      createUser: Number(userId), // 确保是数字类型
      createUserName: userName
    }
    
    console.log('创建报表请求:', payload) // 调试日志
    
    const result = await apiPost('/report/custom', payload)
    console.log('创建报表响应:', result) // 调试日志
    
    if (result.code === 0) {
      reportDialogVisible.value = false
      await loadCustomReports()
      ElMessage.success(t('report.reportCreated'))
    } else {
      ElMessage.error(result.message || t('report.createFailed'))
    }
  } catch (e) {
    console.error(t('report.createReportFailed'), e)
    ElMessage.error(t('report.createReportFailed') + ': ' + e.message)
  }
}

const saveSubscription = async () => {
  // 1. 表单验证
  if (!subForm.name || !subForm.name.trim()) {
    ElMessage.warning(t('report.inputSubscriptionName'))
    return
  }
  if (!subForm.reportType) {
    ElMessage.warning(t('report.selectReportType'))
    return
  }
  if (!subForm.frequency) {
    ElMessage.warning(t('report.selectFrequency'))
    return
  }
  if (!subForm.channel) {
    ElMessage.warning(t('report.selectPushChannel'))
    return
  }

  // 2. 接收人验证
  if (!validateRecipients()) {
    ElMessage.warning(t('report.checkRecipientsFormat'))
    return
  }

  try {
    saveLoading.value = true

    // 3. 从 localStorage 获取当前用户信息
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const userId = userInfo.id || 1
    const userName = userInfo.username || userInfo.name || 'admin'

    // 4. 构建请求数据
    const payload = {
      name: subForm.name,
      reportType: subForm.reportType,
      frequency: subForm.frequency,
      channel: subForm.channel,
      recipients: subForm.recipients,
      fixedTime: subForm.fixedTime || '09:00',
      enabled: subForm.enabled,
      createUser: Number(userId),
      createUserName: userName
    }

    let result

    if (isEditSubscription.value && editingSubscriptionId.value) {
      // 编辑模式
      result = await apiPut(`/report/subscription/${editingSubscriptionId.value}`, payload)
    } else {
      // 新建模式
      result = await apiPost('/report/subscription', payload)
    }

    if (result.code === 0) {
      subDialogVisible.value = false
      await loadSubscriptions()
      ElMessage.success(isEditSubscription.value ? t('report.subscriptionUpdated') : t('report.subscriptionCreated'))
    } else {
      ElMessage.error(result.message || t('report.saveFailed'))
    }
  } catch (e) {
    console.error(t('report.saveSubscriptionFailed'), e)
    ElMessage.error(t('report.saveFailedWithMsg', { msg: e.message || t('report.checkNetworkConnection') }))
  } finally {
    saveLoading.value = false
  }
}

// 加载真实统计数据
const loadStats = async () => {
  try {
    // 从报表分析接口获取概览数据
    const overviewResult = await apiGet('/report/overview')
    if (overviewResult.code === 0 && overviewResult.data) {
      const data = overviewResult.data
      stats.totalTasks = data.totalTasks || 0
      stats.successRate = data.successRate || 0
      stats.avgDuration = data.avgDuration || 0
      stats.savedHours = data.savedHours || 0
    }

    // 加载日报数据
    await loadDailyReport()

    // 加载机器人利用率
    await loadRobotUtilization()

    // 加载流程效率
    await loadProcessEfficiency()

    // 加载自定义报表列表
    await loadCustomReports()

    // 加载订阅列表
    await loadSubscriptions()

  } catch (e) {
    console.error('加载报表数据失败:', e)
  }
}

// 加载日报数据
const loadDailyReport = async () => {
  try {
    console.log('加载日报数据，日期:', dailyDate.value)
    const result = await apiGet('/report/daily', { date: dailyDate.value })
    console.log('日报API响应:', result)
    if (result.code === 0 && result.data) {
      const data = result.data
      dailyStats.total = data.total || 0
      dailyStats.success = data.success || 0
      dailyStats.failed = data.failed || 0
      dailyStats.running = data.running || 0
      dailyLogs.value = data.logs || []
      console.log('日报数据已更新:', { dailyStats, logsCount: dailyLogs.value.length })
    } else {
      console.warn('日报API返回异常:', result)
    }
  } catch (e) {
    console.error('加载日报数据失败:', e)
  }
}

// 加载机器人利用率
const loadRobotUtilization = async () => {
  try {
    const result = await apiGet('/report/robot-utilization')
    if (result.code === 0 && result.data) {
      const data = result.data
      robotStats.busyRate = data.busyRate || 0
      robotStats.idleRate = data.idleRate || 0
      robotStats.offlineRate = data.offlineRate || 0
      robotStats.totalRuntime = data.totalRuntime || '0h'
      robotList.value = data.robots || []
    }
  } catch (e) {
    console.error('加载机器人数据失败:', e)
  }
}

// 加载流程效率
const loadProcessEfficiency = async () => {
  try {
    const result = await apiGet('/report/process-efficiency', { topN: processTopN.value })
    if (result.code === 0 && result.data) {
      processList.value = result.data.processes || []
    }
  } catch (e) {
    console.error('加载流程数据失败:', e)
  }
}

// 加载自定义报表列表
const loadCustomReports = async () => {
  try {
    loading.value = true
    const result = await apiGet('/report/custom')
    if (result.code === 0) {
      customReports.value = result.data || []
    }
  } catch (e) {
    console.error('加载自定义报表失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载订阅列表
const loadSubscriptions = async () => {
  try {
    subLoading.value = true
    const result = await apiGet('/report/subscription')
    if (result.code === 0) {
      subscriptions.value = result.data || []
    }
  } catch (e) {
    console.error('加载订阅列表失败:', e)
  } finally {
    subLoading.value = false
  }
}

// 加载趋势预测
const loadForecast = async () => {
  try {
    const result = await apiGet('/report/forecast', { days: parseInt(forecastPeriod.value) })
    if (result.code === 0 && result.data) {
      const data = result.data
      forecastStats.growthRate = data.growthRate || 0
      forecastStats.suggestRobotCount = data.suggestRobotCount || 0
      forecastStats.capacityRate = data.capacityRate || 0
      forecastStats.suggestion = data.suggestion || t('report.noForecastData')
    }
  } catch (e) {
    console.error('加载趋势预测失败:', e)
  }
}

// 加载月报数据
const loadMonthlyReport = async () => {
  try {
    const result = await apiGet('/report/monthly', { yearMonth: monthlyDate.value })
    if (result.code === 0 && result.data) {
      const data = result.data
      monthlyStats.totalExecutions = data.totalExecutions || 0
      monthlyStats.successCount = data.successCount || 0
      monthlyStats.failedCount = data.failedCount || 0
      monthlyStats.successRate = data.successRate || 0
      monthlyStats.totalData = data.totalData || 0
      monthlyStats.dailyAvg = data.dailyAvg || 0
      monthlyStats.peakData = data.peakData || 0
      monthlyStats.dataSuccessRate = data.dataSuccessRate || 0
    }
  } catch (e) {
    console.error('加载月报数据失败:', e)
  }
}

// 格式化运行时长
const formatRuntime = (seconds) => {
  if (!seconds) return '0h'
  const hours = Math.floor(seconds / 3600)
  return hours + 'h'
}

onMounted(() => {
  loadStats()
  loadPermissions()
  // 使用 nextTick 确保 DOM 渲染完成后再初始化图表
  nextTick(() => {
    // 图表会在 v-chart 的 autoresize 属性下自动调整
  })
})

// 加载用户权限
const loadPermissions = async () => {
  try {
    const result = await apiGet('/report/permissions')
    if (result.code === 0 && result.data) {
      permissions.canView = result.data.canView
      permissions.canExport = result.data.canExport
      permissions.canSubscription = result.data.canSubscription
      permissions.isAdmin = result.data.isAdmin
      permissions.allPermissions = result.data.permissions || []
      permissions.loaded = true
    }
  } catch (e) {
    console.error('加载权限失败:', e)
    // 默认允许查看，拒绝导出
    permissions.canView = true
    permissions.canExport = false
    permissions.loaded = true
  }
}

// 检查是否有导出权限
const checkExportPermission = () => {
  if (!permissions.loaded) {
    ElMessage.warning(t('report.permissionLoading'))
    return false
  }
  if (!permissions.canExport && !permissions.isAdmin) {
    ElMessage({
      type: 'warning',
      message: t('report.noExportPermission'),
      duration: 5000
    })
    return false
  }
  return true
}

// 导出报表（带权限检查）
const exportReport = (type) => {
  // 检查权限
  if (!checkExportPermission()) {
    return
  }

  let data = []
  let filename = ''
  let headers = []

  switch (type) {
    case 'daily':
      filename = t('report.dailyReport')
      headers = [t('report.taskName'), t('report.process'), t('report.status'), t('report.dataCollected'), t('report.startTime'), t('report.duration')]
      data = dailyLogs.value.map(item => ({
        [t('report.taskName')]: item.taskName,
        [t('report.process')]: item.processName,
        [t('report.status')]: getStatusText(item.status),
        [t('report.dataCollected')]: item.dataCount,
        [t('report.startTime')]: item.startTime,
        [t('report.duration')]: item.duration
      }))
      break
    case 'monthly':
      filename = t('report.monthlyReport')
      headers = [t('report.statItem'), t('report.value')]
      data = [
        {[t('report.statItem')]: t('report.totalExecutions'), [t('report.value')]: monthlyStats.totalExecutions},
        {[t('report.statItem')]: t('report.successCount'), [t('report.value')]: monthlyStats.successCount},
        {[t('report.statItem')]: t('report.failedCount'), [t('report.value')]: monthlyStats.failedCount},
        {[t('report.statItem')]: t('report.successRate'), [t('report.value')]: monthlyStats.successRate + '%'},
        {[t('report.statItem')]: t('report.totalDataCollected'), [t('report.value')]: monthlyStats.totalData},
        {[t('report.statItem')]: t('report.dailyAvg'), [t('report.value')]: monthlyStats.dailyAvg},
        {[t('report.statItem')]: t('report.peakData'), [t('report.value')]: monthlyStats.peakData},
        {[t('report.statItem')]: t('report.dataSuccessRate'), [t('report.value')]: monthlyStats.dataSuccessRate + '%'}
      ]
      break
    case 'robot':
      filename = t('report.robotUtilization')
      headers = [t('report.robotName'), t('report.status'), t('report.execCount'), t('report.successRate'), t('report.runtime'), t('report.dataCollected'), t('report.lastRun')]
      data = robotList.value.map(item => ({
        [t('report.robotName')]: item.name,
        [t('report.status')]: item.statusText,
        [t('report.execCount')]: item.execCount,
        [t('report.successRate')]: item.successRate + '%',
        [t('report.runtime')]: item.runtime,
        [t('report.dataCollected')]: item.dataCount,
        [t('report.lastRun')]: item.lastRun
      }))
      break
    case 'process':
      filename = t('report.processDuration')
      headers = [t('report.processName'), t('report.processCode'), t('report.execCount'), t('report.avgDuration'), t('report.maxDuration'), t('report.minDuration'), t('report.totalDuration'), t('report.successRate')]
      data = processList.value.map(item => ({
        [t('report.processName')]: item.name,
        [t('report.processCode')]: item.code,
        [t('report.execCount')]: item.execCount,
        [t('report.avgDuration')]: item.avgDuration,
        [t('report.maxDuration')]: item.maxDuration,
        [t('report.minDuration')]: item.minDuration,
        [t('report.totalDuration')]: item.totalDuration,
        [t('report.successRate')]: item.successRate + '%'
      }))
      break
    case 'custom':
      filename = t('report.customReport')
      headers = [t('report.reportName'), t('report.reportType'), t('report.dimensions'), t('report.creator'), t('report.createTime'), t('report.lastRun')]
      data = customReports.value.map(item => ({
        [t('report.reportName')]: item.name,
        [t('report.reportType')]: item.type,
        [t('report.dimensions')]: item.dimensions,
        [t('report.creator')]: item.createUser,
        [t('report.createTime')]: item.createTime,
        [t('report.lastRun')]: item.lastRun
      }))
      break
    default:
      ElMessage.warning(t('report.exportNotSupported'))
      return
  }

  // 生成CSV内容
  if (data.length === 0) {
    ElMessage.warning(t('report.noDataToExport'))
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
  ElMessage.success(t('report.exportSuccess', { count: data.length, filename: filename }))
}
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
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.stat-card-bg {
  position: absolute;
  top: -50%;
  right: -30%;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  opacity: 0.08;
  transition: all 0.4s ease;
}

.stat-card:nth-child(1) .stat-card-bg { background: #409eff; }
.stat-card:nth-child(2) .stat-card-bg { background: #67c23a; }
.stat-card:nth-child(3) .stat-card-bg { background: #e6a23c; }
.stat-card:nth-child(4) .stat-card-bg { background: #f56c6c; }

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-card:hover .stat-card-bg {
  transform: scale(1.5);
  opacity: 0.12;
}

.stat-card-active {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
}

.stat-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.stat-icon-wrap::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 14px;
  background: inherit;
  filter: blur(12px);
  opacity: 0.25;
  z-index: -1;
}

.stat-icon-wrap.primary { background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%); }
.stat-icon-wrap.success { background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%); }
.stat-icon-wrap.warning { background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%); }
.stat-icon-wrap.danger { background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%); }

.stat-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #1a1a1a;
  line-height: 1.2;
  letter-spacing: -0.5px;
}

.stat-unit {
  font-size: 16px;
  font-weight: 600;
  color: #606266;
  margin-left: 2px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
  font-weight: 500;
}

.stat-meta {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 12px;
}

.stat-trend.up { color: #67c23a; background: rgba(103, 194, 58, 0.12); }
.stat-trend.down { color: #f56c6c; background: rgba(245, 108, 108, 0.12); }

.stat-period {
  font-size: 10px;
  color: #c0c4cc;
}

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

.subscription-tips { margin-top: 20px; }

.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.report-tabs :deep(.el-tabs__content) { padding: 0; }

/* 推送方式选项样式 */
.channel-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.channel-option .el-icon {
  font-size: 16px;
}

/* 表单提示样式 */
.form-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.form-tip .el-icon {
  font-size: 12px;
}

/* 表单错误样式 */
.form-error {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-size: 12px;
  color: #f56c6c;
}

.form-error .el-icon {
  font-size: 12px;
}

/* 对话框底部按钮 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

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
