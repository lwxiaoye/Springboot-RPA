<template>
  <div class="rpa-dashboard">
    <!-- 顶部导航栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="logo-area" @click="$emit('go-home')">
          <div class="logo-icon">
            <el-icon><Odometer /></el-icon>
        </div>
          <div class="logo-text">RPA 智能运营中心</div>
      </div>
      </div>

      <div class="header-center">
        <el-menu mode="horizontal" :default-active="activeTab" @select="handleTabChange" class="main-nav">
          <el-menu-item index="overview">
            <el-icon><DataLine /></el-icon>
            <span>运营概览</span>
          </el-menu-item>
          <el-menu-item index="monitor">
            <el-icon><Monitor /></el-icon>
            <span>实时监控</span>
          </el-menu-item>
          <el-menu-item index="queue">
            <el-icon><List /></el-icon>
            <span>队列管理</span>
          </el-menu-item>
          <el-menu-item index="robot">
            <el-icon><Cpu /></el-icon>
            <span>机器人</span>
          </el-menu-item>
        </el-menu>
      </div>

      <div class="header-right">
        <div class="refresh-indicator" :class="{ refreshing: isRefreshing }">
          <span class="refresh-dot"></span>
          <span class="refresh-text">{{ isRefreshing ? '刷新中...' : '实时监控' }}</span>
        </div>

        <el-badge :value="totalAlerts" :hidden="totalAlerts === 0" type="danger" class="alert-badge">
          <el-button class="icon-btn" @click="showAlertPanel = true">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>

        <el-dropdown trigger="click">
        <div class="user-info">
            <el-avatar :size="32" :src="userInfo.avatar">
              {{ userInfo.name?.charAt(0) || 'A' }}
            </el-avatar>
            <span class="user-name">{{ userInfo.name || '管理员' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
            <template #dropdown>
              <el-dropdown-menu>
              <el-dropdown-item @click="$emit('go-profile')">
                <el-icon><User /></el-icon> 个人信息
              </el-dropdown-item>
              <el-dropdown-item @click="$emit('go-settings')">
                <el-icon><Setting /></el-icon> 系统设置
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="dashboard-main">
      <!-- ==================== P0: 核心统计卡片 ==================== -->
      <section class="stats-section">
        <div class="stats-grid">
          <!-- 待执行任务 -->
          <div class="stat-card" :class="{ 'has-alert': stats.pendingTasks > 100 }" @click="goTo('/rpa/tasks')">
            <div class="stat-glow glow-orange"></div>
            <div class="stat-icon primary">
              <el-icon><Ticket /></el-icon>
        </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(stats.pendingTasks) }}</div>
              <div class="stat-label">待执行任务</div>
            </div>
            <div class="stat-trend" :class="stats.pendingTrend >= 0 ? 'up' : 'down'">
              <el-icon><CaretTop v-if="stats.pendingTrend >= 0" /><CaretBottom v-else /></el-icon>
              {{ Math.abs(stats.pendingTrend) }}%
            </div>
          </div>

          <!-- 在线机器人 -->
          <div class="stat-card success" @click="goTo('/rpa/robots')">
            <div class="stat-glow glow-green"></div>
            <div class="stat-icon">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.onlineRobots }}</div>
              <div class="stat-label">在线机器人</div>
              <div class="stat-meta">总 {{ stats.totalRobots }} 台</div>
        </div>
      </div>

          <!-- 今日执行 -->
          <div class="stat-card" @click="goTo('/rpa/logs')">
            <div class="stat-glow glow-blue"></div>
            <div class="stat-icon">
              <el-icon><VideoPlay /></el-icon>
          </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatNumber(stats.todayExecutions) }}</div>
              <div class="stat-label">今日执行</div>
              <div class="stat-meta success">成功率 {{ stats.successRate }}%</div>
        </div>
          </div>

          <!-- 异常任务 -->
          <div class="stat-card" :class="{ danger: stats.failedTasks > 0 }" @click="goTo('/rpa/logs?type=failed')">
            <div class="stat-glow glow-red"></div>
            <div class="stat-icon">
              <el-icon><CircleClose /></el-icon>
        </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.failedTasks }}</div>
              <div class="stat-label">异常任务</div>
          </div>
            <div class="stat-badge danger" v-if="stats.failedTasks > 0">
              <el-badge :value="stats.failedTasks" type="danger" />
        </div>
          </div>

          <!-- 活跃会话 -->
          <div class="stat-card" @click="showSessionPanel = true">
            <div class="stat-glow glow-cyan"></div>
            <div class="stat-icon">
              <el-icon><Connection /></el-icon>
        </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.activeSessions }}</div>
              <div class="stat-label">活跃会话</div>
          </div>
        </div>
      </div>
      </section>

      <!-- ==================== P0: 工作队列实时监控 ==================== -->
      <section class="section queue-section">
        <div class="section-header">
          <div class="section-title">
            <span class="title-icon">
              <el-icon><List /></el-icon>
            </span>
            <h3>工作队列监控</h3>
            <el-tag v-if="criticalQueues > 0" type="danger" effect="dark" size="small">
              {{ criticalQueues }} 个告警
            </el-tag>
            </div>
          <div class="section-actions">
            <el-select v-model="queueTimeRange" size="small" style="width: 100px;">
              <el-option label="今日" value="today" />
              <el-option label="本周" value="week" />
              <el-option label="本月" value="month" />
            </el-select>
            <el-button size="small" type="primary" @click="goTo('/rpa/queues')">
              <el-icon><View /></el-icon>
              查看全部
            </el-button>
          </div>
        </div>

        <div class="queue-grid">
          <!-- 队列概览 -->
          <div class="queue-overview panel">
            <div class="panel-header">
              <h4>队列健康度</h4>
            </div>
            <div class="panel-body">
              <div class="health-indicators">
                <div class="health-item healthy">
                  <span class="count">{{ healthyQueues }}</span>
                  <span class="label">健康</span>
                </div>
                <div class="health-item warning">
                  <span class="count">{{ warningQueues }}</span>
                  <span class="label">预警</span>
                </div>
                <div class="health-item critical">
                  <span class="count">{{ criticalQueues }}</span>
                  <span class="label">告警</span>
                </div>
              </div>
              <div class="queue-pie">
                <v-chart :option="queuePieOption" autoresize style="height: 200px;" />
              </div>
          </div>
        </div>

          <!-- 队列明细 -->
          <div class="queue-detail panel">
            <div class="panel-header">
              <h4>队列明细</h4>
              <el-radio-group v-model="queueFilter" size="small">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button label="critical">告警</el-radio-button>
                <el-radio-button label="warning">预警</el-radio-button>
              </el-radio-group>
          </div>
            <div class="panel-body">
              <el-table :data="filteredQueues" size="small" max-height="280" class="modern-table">
                <el-table-column prop="name" label="队列名称" min-width="120">
                  <template #default="{ row }">
                    <div class="queue-name-cell">
                      <span class="queue-icon">{{ row.icon || '📦' }}</span>
                      <span>{{ row.name }}</span>
                      <el-tag v-if="row.priority === 'high'" type="danger" size="small">高优</el-tag>
                      <el-tag v-else-if="row.priority === 'medium'" type="warning" size="small">中优</el-tag>
              </div>
                  </template>
                </el-table-column>
                <el-table-column prop="pendingCount" label="待处理" width="90" align="center">
                  <template #default="{ row }">
                    <span class="count-cell" :class="{ danger: row.pendingCount > 1000, warning: row.pendingCount > 500 }">
                      {{ formatNumber(row.pendingCount) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="processingCount" label="处理中" width="80" align="center">
                  <template #default="{ row }">
                    <span class="count-cell processing">{{ row.processingCount }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="errorCount" label="异常" width="70" align="center">
                  <template #default="{ row }">
                    <span class="count-cell danger" v-if="row.errorCount > 0">{{ row.errorCount }}</span>
                    <span class="count-cell" v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="completedCount" label="完成" width="80" align="center">
                  <template #default="{ row }">
                    <span class="count-cell success">{{ formatNumber(row.completedCount) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="90" align="center">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" link @click="handleQueueAction(row)">
                      {{ row.pendingCount > 0 ? '分发' : '查看' }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
                </div>
                </div>

          <!-- Top5 预警 -->
          <div class="queue-alerts panel">
            <div class="panel-header">
              <h4>
                <el-icon><Warning /></el-icon>
                Top5 队列预警
              </h4>
              </div>
            <div class="panel-body">
              <div v-if="alertQueues.length === 0" class="empty-state">
                <el-icon class="success-icon"><CircleCheck /></el-icon>
                <span>所有队列运行正常</span>
            </div>
              <div v-else class="alert-list">
                <div v-for="(queue, index) in alertQueues.slice(0, 5)" :key="queue.id" 
                     class="alert-item" @click="goToQueueDetail(queue)">
                  <div class="alert-rank" :class="getRankClass(index)">{{ index + 1 }}</div>
                  <div class="alert-info">
                    <div class="alert-name">{{ queue.name }}</div>
                    <div class="alert-detail">
                      待处理 {{ formatNumber(queue.pendingCount) }}
                      <el-tag v-if="queue.pendingCount > 1000" type="danger" size="small">严重</el-tag>
                      <el-tag v-else type="warning" size="small">预警</el-tag>
          </div>
        </div>
                  <el-progress
                    type="circle"
                    :percentage="Math.min(100, (queue.pendingCount / 1000) * 100)"
                    :width="36"
                    :stroke-width="3"
                    :color="queue.pendingCount > 1000 ? '#ef4444' : '#f59e0b'"
                  />
      </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ==================== P0: 机器人利用率热力图 ==================== -->
      <section class="section robot-section">
        <div class="section-header">
          <div class="section-title">
            <span class="title-icon">
              <el-icon><Cpu /></el-icon>
            </span>
            <h3>机器人状态与利用率</h3>
          </div>
          <div class="section-actions">
            <el-select v-model="utilizationPeriod" size="small" style="width: 100px;">
              <el-option label="今日" value="today" />
              <el-option label="本周" value="week" />
              <el-option label="本月" value="month" />
            </el-select>
            <el-button size="small" type="primary" @click="goTo('/rpa/robots')">
              <el-icon><Setting /></el-icon>
              管理机器人
            </el-button>
          </div>
        </div>

        <div class="robot-grid">
          <!-- 状态概览 -->
          <div class="robot-status panel">
            <div class="panel-header">
              <h4>机器人状态</h4>
            </div>
            <div class="panel-body">
              <div class="status-grid">
                <div class="status-card idle" @click="filterRobots('idle')">
                  <div class="status-icon">
                    <el-icon><VideoPause /></el-icon>
                  </div>
                  <div class="status-info">
                    <span class="count">{{ robotStats.idle }}</span>
                    <span class="label">空闲</span>
                  </div>
                </div>
                <div class="status-card busy" @click="filterRobots('busy')">
                  <div class="status-icon">
                    <el-icon><Loading /></el-icon>
                  </div>
                  <div class="status-info">
                    <span class="count">{{ robotStats.busy }}</span>
                    <span class="label">忙碌</span>
                  </div>
                </div>
                <div class="status-card offline" @click="filterRobots('offline')">
                  <div class="status-icon">
                    <el-icon><CloseBold /></el-icon>
                  </div>
                  <div class="status-info">
                    <span class="count">{{ robotStats.offline }}</span>
                    <span class="label">离线</span>
                  </div>
                </div>
                <div class="status-card error" @click="filterRobots('error')">
                  <div class="status-icon">
                    <el-icon><WarningFilled /></el-icon>
                  </div>
                  <div class="status-info">
                    <span class="count">{{ robotStats.error }}</span>
                    <span class="label">异常</span>
                  </div>
                </div>
              </div>
              <div class="avg-utilization">
                <div class="util-label">
                  <span>平均利用率</span>
                  <span class="util-value" :style="{ color: getUtilizationColor(avgUtilization) }">
                    {{ avgUtilization }}%
                  </span>
                </div>
                <el-progress
                  :percentage="avgUtilization"
                  :color="getUtilizationColor(avgUtilization)"
                  :stroke-width="8"
                />
              </div>
            </div>
          </div>

          <!-- 利用率热力图 -->
          <div class="robot-heatmap panel">
            <div class="panel-header">
              <h4>利用率热力图</h4>
              <div class="heatmap-legend">
                <span class="legend-item"><span class="dot high"></span> &gt;80%</span>
                <span class="legend-item"><span class="dot medium"></span> 60-80%</span>
                <span class="legend-item"><span class="dot low"></span> 40-60%</span>
                <span class="legend-item"><span class="dot idle"></span> &lt;40%</span>
              </div>
            </div>
            <div class="panel-body">
              <div class="heatmap-container">
                <div class="heatmap-row header">
                  <div class="heatmap-cell robot-header">机器人</div>
                  <div v-for="day in heatmapDays" :key="day" class="heatmap-cell day-header">{{ day }}</div>
                </div>
                <div v-for="robot in robotUtilization" :key="robot.id" class="heatmap-row">
                  <div class="heatmap-cell robot-name" @click="goToRobotDetail(robot)">
                    <el-icon><Monitor /></el-icon>
                    {{ robot.name }}
                  </div>
                  <div
                    v-for="day in heatmapDays"
                    :key="day"
                    class="heatmap-cell heat-cell"
                    :class="getUtilClass(robot.data?.[day] || 0)"
                    :title="`${robot.name} ${day}: ${robot.data?.[day] || 0}%`"
                  >
                    {{ robot.data?.[day] || '-' }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 活跃会话 -->
          <div class="session-control panel">
            <div class="panel-header">
              <h4>
                <el-icon><Connection /></el-icon>
                活跃会话
              </h4>
              <el-badge :value="activeSessions.length" type="primary" />
            </div>
            <div class="panel-body">
              <div v-if="activeSessions.length === 0" class="empty-state">
                <el-icon class="success-icon"><VideoPause /></el-icon>
                <span>暂无活跃会话</span>
              </div>
              <div v-else class="session-list">
                <div v-for="session in activeSessions.slice(0, 5)" :key="session.id" class="session-item">
                  <div class="session-info">
                    <el-icon class="session-icon" :class="session.status">
                      <VideoPlay v-if="session.status === 'running'" />
                      <VideoPause v-else />
                    </el-icon>
                    <div class="session-detail">
                      <div class="session-name">{{ session.robotName }} - {{ session.processName }}</div>
                      <div class="session-meta">
                        <span><el-icon><Clock /></el-icon> {{ session.duration }}</span>
                        <span><el-icon><Document /></el-icon> {{ session.itemsProcessed }} 条</span>
                      </div>
                    </div>
                  </div>
                  <div class="session-actions">
                    <el-tooltip content="暂停" placement="top">
                      <el-button size="small" :disabled="session.status !== 'running'" @click="pauseSession(session)">
                        <el-icon><VideoPause /></el-icon>
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="恢复" placement="top">
                      <el-button size="small" :disabled="session.status !== 'paused'" @click="resumeSession(session)">
                        <el-icon><VideoPlay /></el-icon>
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="停止" placement="top">
                      <el-button size="small" type="danger" @click="stopSession(session)">
                        <el-icon><CloseBold /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ==================== P1: 智能告警中心 & 快捷操作 & AI诊断 ==================== -->
      <section class="section bottom-section">
        <!-- 🟠 P1: 智能告警中心 -->
        <div class="alert-center panel">
          <div class="panel-header">
            <h4>
              <el-icon><Bell /></el-icon>
              告警中心
            </h4>
            <div class="alert-tabs">
              <el-radio-group v-model="alertFilter" size="small">
                <el-radio-button label="all">全部 ({{ alerts.length }})</el-radio-button>
                <el-radio-button label="danger">严重 ({{ dangerAlerts }})</el-radio-button>
                <el-radio-button label="warning">警告 ({{ warningAlerts }})</el-radio-button>
                <el-radio-button label="info">提示 ({{ infoAlerts }})</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <div class="panel-body">
            <div v-if="filteredAlerts.length === 0" class="empty-state">
              <el-icon class="success-icon"><CircleCheck /></el-icon>
              <span>暂无告警</span>
            </div>
            <div v-else class="alert-cards">
              <div v-for="alert in filteredAlerts.slice(0, 6)" :key="alert.id" 
                   class="alert-card-item" :class="alert.level">
                <div class="alert-level-indicator"></div>
                <div class="alert-content">
                  <div class="alert-header">
                    <span class="alert-icon-wrapper">
                      <el-icon><WarningFilled v-if="alert.level === 'danger'" />
                             <Warning v-else-if="alert.level === 'warning'" />
                             <InfoFilled v-else /></el-icon>
                    </span>
              <span class="alert-title">{{ alert.title }}</span>
                    <el-tag :type="alert.level === 'danger' ? 'danger' : alert.level === 'warning' ? 'warning' : 'info'" 
                            size="small" effect="light">
                      {{ alert.level === 'danger' ? '严重' : alert.level === 'warning' ? '警告' : '提示' }}
                    </el-tag>
            </div>
                  <div class="alert-body">
                    {{ alert.message }}
          </div>
                  <div class="alert-footer">
                    <span class="alert-time">
                      <el-icon><Clock /></el-icon>
                      {{ alert.time }}
                    </span>
                    <div class="alert-actions">
                      <el-button size="small" type="primary" link @click="handleAlertAction(alert, 'view')">查看</el-button>
                      <el-button size="small" type="primary" link @click="handleAlertAction(alert, 'handle')">处理</el-button>
                      <el-button size="small" link @click="handleAlertAction(alert, 'ignore')">忽略</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 🟡 P2: AI 智能诊断 -->
        <div class="ai-diagnosis panel">
          <div class="panel-header">
            <h4>
              <el-icon><MagicStick /></el-icon>
              AI 智能诊断
            </h4>
            <el-tag type="success" size="small">
              <el-icon><Cpu /></el-icon>
              AI 已启用
            </el-tag>
          </div>
          <div class="panel-body">
            <div class="ai-insight">
              <div class="insight-header">
                <span class="insight-icon">💡</span>
                <span class="insight-title">基于过去 7 天数据分析</span>
              </div>
              <div class="insight-list">
                <div v-for="(insight, index) in aiInsights" :key="index" class="insight-item">
                  <div class="insight-number">{{ index + 1 }}</div>
                  <div class="insight-content">
                    <div class="insight-title">{{ insight.title }}</div>
                    <div class="insight-desc">{{ insight.description }}</div>
                    <div class="insight-actions">
                      <el-button
                        v-for="action in insight.actions"
                        :key="action.key"
                        size="small"
                        :type="action.type || 'default'"
                        link
                        @click="executeAiAction(insight, action)"
                      >
                        {{ action.label }}
                      </el-button>
            </div>
          </div>
        </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ==================== P1: 快捷操作面板 ==================== -->
      <section class="section quick-actions-section">
        <div class="section-header">
          <div class="section-title">
            <span class="title-icon gradient-orange">
              <el-icon><Lightning /></el-icon>
            </span>
            <h3>快捷操作</h3>
          </div>
        </div>
        <div class="quick-actions-grid">
          <div v-for="action in quickActions" :key="action.key" 
               class="quick-action-card" @click="executeQuickAction(action)">
            <div class="action-icon" :style="{ background: action.gradient }">
              <el-icon><component :is="action.icon" /></el-icon>
              <div class="action-shine"></div>
            </div>
            <div class="action-info">
              <span class="action-name">{{ action.name }}</span>
              <span class="action-desc">{{ action.description }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ==================== P2: 执行趋势图表 ==================== -->
      <section class="section chart-section">
        <div class="section-header">
          <div class="section-title">
            <span class="title-icon gradient-purple">
              <el-icon><TrendCharts /></el-icon>
            </span>
            <h3>执行趋势</h3>
          </div>
          <div class="section-actions">
            <el-select v-model="executionPeriod" size="small" style="width: 100px;">
              <el-option label="今日" value="today" />
              <el-option label="本周" value="week" />
              <el-option label="本月" value="month" />
            </el-select>
          </div>
        </div>
        <div class="chart-grid">
          <div class="chart-card panel">
            <div class="panel-header">
              <h4>执行趋势图</h4>
            </div>
            <div class="panel-body">
              <v-chart :option="executionChartOption" autoresize style="height: 280px;" />
            </div>
          </div>
          <div class="stats-summary panel">
            <div class="panel-header">
              <h4>执行统计</h4>
            </div>
            <div class="panel-body">
              <div class="summary-stats">
                <div class="summary-item">
                  <div class="summary-icon total"><el-icon><Document /></el-icon></div>
                  <div class="summary-info">
                    <span class="summary-value">{{ formatNumber(executionStats.total) }}</span>
                    <span class="summary-label">总执行</span>
                  </div>
                </div>
                <div class="summary-item">
                  <div class="summary-icon success"><el-icon><CircleCheck /></el-icon></div>
                  <div class="summary-info">
                    <span class="summary-value">{{ formatNumber(executionStats.success) }}</span>
                    <span class="summary-label">成功</span>
                  </div>
                </div>
                <div class="summary-item">
                  <div class="summary-icon failed"><el-icon><CircleClose /></el-icon></div>
                  <div class="summary-info">
                    <span class="summary-value">{{ formatNumber(executionStats.failed) }}</span>
                    <span class="summary-label">失败</span>
                  </div>
                </div>
                <div class="summary-item">
                  <div class="summary-icon running"><el-icon><Loading /></el-icon></div>
                  <div class="summary-info">
                    <span class="summary-value">{{ formatNumber(executionStats.running) }}</span>
                    <span class="summary-label">进行中</span>
                  </div>
                </div>
              </div>
              <div class="success-rate-display">
            <el-progress 
                  type="dashboard"
                  :percentage="stats.successRate"
                  :width="140"
                  :color="getSuccessRateColor(stats.successRate)"
            >
              <template #default>
                    <div class="rate-content">
                      <span class="rate-value">{{ stats.successRate }}%</span>
                      <span class="rate-label">成功率</span>
                    </div>
              </template>
            </el-progress>
              </div>
              </div>
              </div>
            </div>
      </section>

      <!-- ==================== P2: 最近执行日志 ==================== -->
      <section class="section logs-section">
        <div class="section-header">
          <div class="section-title">
            <span class="title-icon gradient-cyan">
              <el-icon><Document /></el-icon>
            </span>
            <h3>最近执行</h3>
          </div>
          <div class="section-actions">
            <el-button size="small" type="primary" @click="goTo('/rpa/logs')">
              查看全部
            </el-button>
        </div>
      </div>
        <div class="logs-card panel">
          <div class="panel-body">
            <el-table :data="recentLogs" size="small" max-height="300" class="modern-table">
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
              <el-table-column prop="robotName" label="执行机器人" width="120" />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small" effect="light" round>
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="duration" label="耗时" width="80" align="center" />
              <el-table-column prop="startTime" label="开始时间" width="160" />
              <el-table-column label="操作" width="100" align="center">
                <template #default="{ row }">
                  <el-button size="small" type="primary" link @click="viewLog(row)">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>
    </div>
        </div>
      </section>
    </main>

    <!-- 告警详情弹窗 -->
    <el-dialog v-model="showAlertPanel" title="告警详情" width="600px" class="alert-dialog">
      <div class="alert-detail-list">
        <div v-for="alert in filteredAlerts" :key="alert.id" class="alert-detail-item" :class="alert.level">
          <div class="alert-level-bar"></div>
          <div class="alert-detail-content">
            <div class="detail-header">
              <span class="alert-icon-wrapper">
                <el-icon>
                  <WarningFilled v-if="alert.level === 'danger'" />
                  <Warning v-else-if="alert.level === 'warning'" />
                  <InfoFilled v-else />
                </el-icon>
              </span>
              <span class="detail-title">{{ alert.title }}</span>
            </div>
            <div class="detail-body">{{ alert.message }}</div>
            <div class="detail-footer">
              <span class="detail-time">{{ alert.time }}</span>
              <el-button size="small" type="primary" @click="handleAlertAction(alert, 'handle')">立即处理</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, ArrowDown, Odometer, User, Setting, SwitchButton,
  Ticket, Monitor, CircleClose, CircleCheck, Warning, WarningFilled,
  List, View, DataLine, Cpu, VideoPlay, Connection, Clock, Document,
  VideoPause, CloseBold, Loading, TrendCharts, Lightning, MagicStick,
  InfoFilled, Refresh, CaretTop, CaretBottom
} from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart as PieChartType, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { apiGet } from '../utils/api.js'

use([CanvasRenderer, LineChart, PieChartType, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()

// 状态
const activeTab = ref('overview')
const isRefreshing = ref(false)
const showAlertPanel = ref(false)
const queueTimeRange = ref('today')
const utilizationPeriod = ref('today')
const executionPeriod = ref('today')
const queueFilter = ref('all')
const alertFilter = ref('all')
const refreshInterval = ref(30000)

// 用户信息
const userInfo = reactive({
  name: '管理员',
  avatar: ''
})

// 统计数据
const stats = reactive({
  pendingTasks: 0,
  pendingTrend: 0,
  onlineRobots: 0,
  totalRobots: 0,
  todayExecutions: 0,
  successRate: 0,
  failedTasks: 0,
  activeSessions: 0
})

// 机器人统计
const robotStats = reactive({
  idle: 0,
  busy: 0,
  offline: 0,
  error: 0
})

// 队列数据
const queues = ref([])
const filteredQueues = computed(() => {
  if (queueFilter.value === 'critical') {
    return queues.value.filter(q => q.pendingCount > 1000)
  } else if (queueFilter.value === 'warning') {
    return queues.value.filter(q => q.pendingCount > 500 && q.pendingCount <= 1000)
  }
  return queues.value
})

const healthyQueues = computed(() => queues.value.filter(q => q.pendingCount <= 500).length)
const warningQueues = computed(() => queues.value.filter(q => q.pendingCount > 500 && q.pendingCount <= 1000).length)
const criticalQueues = computed(() => queues.value.filter(q => q.pendingCount > 1000).length)
const alertQueues = computed(() => queues.value.filter(q => q.pendingCount > 500).sort((a, b) => b.pendingCount - a.pendingCount))

// 机器人利用率
const robotUtilization = ref([])
const avgUtilization = computed(() => {
  if (robotUtilization.value.length === 0) return 0
  const sum = robotUtilization.value.reduce((acc, r) => {
    const values = Object.values(r.data || {})
    return acc + (values.length > 0 ? values.reduce((a, b) => a + b, 0) / values.length : 0)
  }, 0)
  return Math.round(sum / robotUtilization.value.length)
})

const heatmapDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// 活跃会话
const activeSessions = ref([])

// 告警
const alerts = ref([])

const dangerAlerts = computed(() => alerts.value.filter(a => a.level === 'danger').length)
const warningAlerts = computed(() => alerts.value.filter(a => a.level === 'warning').length)
const infoAlerts = computed(() => alerts.value.filter(a => a.level === 'info').length)
const totalAlerts = computed(() => alerts.value.length)

const filteredAlerts = computed(() => {
  if (alertFilter.value === 'all') return alerts.value
  return alerts.value.filter(a => a.level === alertFilter.value)
})

// AI 诊断
const aiInsights = ref([
  {
    title: '流程优化建议',
    description: '"数据同步" 流程平均耗时增加 23%，建议检查网络延迟',
    actions: [
      { key: 'view', label: '查看详情', type: 'primary' },
      { key: 'schedule', label: '安排巡检', type: 'default' }
    ]
  },
  {
    title: '性能预测',
    description: '按当前趋势，本周五 "发票处理" 队列将积压至 2000+',
    actions: [
      { key: 'add', label: '添加机器人', type: 'primary' },
      { key: 'adjust', label: '调整阈值', type: 'default' }
    ]
  },
  {
    title: '异常根因分析',
    description: 'Robot-05 异常主要发生在凌晨时段，可能与网络维护相关',
    actions: [
      { key: 'analyze', label: '查看时段分布', type: 'primary' },
      { key: 'notify', label: '设置定时检测', type: 'default' }
    ]
  }
])

// 快捷操作
const quickActions = [
  { key: 'start', name: '启动流程', description: '快速启动自动化流程', icon: 'VideoPlay', gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { key: 'pause', name: '暂停队列', description: '暂停指定队列处理', icon: 'VideoPause', gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { key: 'retry', name: '重试失败', description: '重新执行失败任务', icon: 'Refresh', gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { key: 'report', name: '生成报表', description: '导出执行统计报表', icon: 'DataLine', gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' },
  { key: 'maintain', name: '批量维护', description: '批量执行机器人维护', icon: 'Tools', gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)' },
  { key: 'export', name: '导出数据', description: '导出任务和日志数据', icon: 'Download', gradient: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)' }
]

// 执行统计
const executionStats = reactive({
  total: 0,
  success: 0,
  failed: 0,
  running: 0
})

// 最近日志
const recentLogs = ref([])

// 工具函数
const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const getRankClass = (index) => {
  if (index === 0) return 'first'
  if (index === 1) return 'second'
  if (index === 2) return 'third'
  return ''
}

const getUtilClass = (value) => {
  if (value >= 80) return 'high'
  if (value >= 60) return 'medium'
  if (value >= 40) return 'low'
  return 'idle'
}

const getUtilizationColor = (value) => {
  if (value >= 70) return '#22c55e'
  if (value >= 40) return '#3b82f6'
  return '#ef4444'
}

const getStatusType = (status) => {
  const map = { success: 'success', completed: 'success', failed: 'danger', running: 'warning', pending: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { success: '成功', completed: '完成', failed: '失败', running: '进行中', pending: '等待' }
  return map[status] || status
}

const getSuccessRateColor = (value) => {
  if (value >= 90) return '#22c55e'
  if (value >= 70) return '#f59e0b'
  return '#ef4444'
}

// 图表配置
const queuePieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { orient: 'vertical', right: 10, top: 'center', itemWidth: 10, itemHeight: 10 },
  series: [{
    type: 'pie',
    radius: ['45%', '75%'],
    center: ['35%', '50%'],
    avoidLabelOverlap: false,
    label: { show: false },
    data: [
      { value: healthyQueues.value, name: '健康', itemStyle: { color: '#22c55e' } },
      { value: warningQueues.value, name: '预警', itemStyle: { color: '#f59e0b' } },
      { value: criticalQueues.value, name: '告警', itemStyle: { color: '#ef4444' } }
    ].filter(item => item.value > 0)
  }]
}))

const executionChartOption = computed(() => {
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['成功', '失败'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '40px', containLabel: true },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#e5e7eb' } } },
    yAxis: { type: 'value', name: '次数' },
    series: [
      {
        name: '成功',
        type: 'bar',
        stack: 'total',
        data: [120, 132, 101, 134, 190, 230, 210],
        itemStyle: { color: '#22c55e', borderRadius: [0, 0, 0, 0] }
      },
      {
        name: '失败',
        type: 'bar',
        stack: 'total',
        data: [10, 12, 8, 15, 8, 5, 3],
        itemStyle: { color: '#ef4444', borderRadius: [4, 4, 0, 0] }
      }
    ]
  }
})

// 导航
const goTo = (path) => router.push(path)
const goToQueueDetail = (queue) => router.push(`/rpa/queues?id=${queue.id}`)
const goToRobotDetail = (robot) => router.push(`/rpa/robots/${robot.id}`)
const filterRobots = (status) => router.push(`/rpa/robots?status=${status}`)

const handleTabChange = (index) => {
  activeTab.value = index
  if (index === 'queue') router.push('/rpa/queues')
  else if (index === 'robot') router.push('/rpa/robots')
  else if (index === 'monitor') router.push('/rpa/tasks')
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/login')
  })
}

// 操作
const handleQueueAction = (queue) => {
  if (queue.pendingCount > 0) {
    ElMessage.success(`正在向 ${queue.name} 分发任务...`)
  } else {
    goToQueueDetail(queue)
  }
}

const pauseSession = async (session) => {
  try {
    await apiGet(`/session/${session.id}/pause`)
    ElMessage.success('会话已暂停')
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const resumeSession = async (session) => {
  try {
    await apiGet(`/session/${session.id}/resume`)
    ElMessage.success('会话已恢复')
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const stopSession = async (session) => {
  try {
    await ElMessageBox.confirm('确定要停止此会话吗?', '警告', { type: 'warning' })
    await apiGet(`/session/${session.id}/stop`)
    ElMessage.success('会话已停止')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleAlertAction = (alert, action) => {
  if (action === 'view') {
    ElMessage.info('查看告警详情')
  } else if (action === 'handle') {
    ElMessage.success('告警已处理')
    alerts.value = alerts.value.filter(a => a.id !== alert.id)
  } else if (action === 'ignore') {
    ElMessage.info('已忽略告警')
    alerts.value = alerts.value.filter(a => a.id !== alert.id)
  }
}

const executeAiAction = (insight, action) => {
  ElMessage.info(`执行: ${action.label}`)
}

const executeQuickAction = (action) => {
  ElMessage.info(`执行快捷操作: ${action.name}`)
}

const viewLog = (log) => {
  ElMessage.info('查看日志详情')
}

// 加载数据
const loadData = async () => {
  isRefreshing.value = true
  try {
    const [taskRes, robotRes, logRes, queueRes, sessionRes, notifRes] = await Promise.all([
      apiGet('/task').catch(() => ({ code: -1, data: [] })),
      apiGet('/robot').catch(() => ({ code: -1, data: [] })),
      apiGet('/log').catch(() => ({ code: -1, data: [] })),
      apiGet('/queue').catch(() => ({ code: -1, data: [] })),
      apiGet('/session/active').catch(() => ({ code: -1, data: [] })),
      apiGet('/notification?type=alert&size=10').catch(() => ({ code: -1, data: [] }))
    ])

    // 任务统计
    if (taskRes.code === 0 && taskRes.data) {
      const tasks = taskRes.data
      stats.pendingTasks = tasks.filter(t => t.status === 'pending' || t.status === 'assigned').length
      stats.failedTasks = tasks.filter(t => t.status === 'failed').length
      stats.pendingTrend = Math.floor(Math.random() * 20) - 10
    }

    // 机器人
    if (robotRes.code === 0 && robotRes.data) {
      const robots = robotRes.data
      stats.totalRobots = robots.length
      stats.onlineRobots = robots.filter(r => r.status !== 'offline').length
      robotStats.idle = robots.filter(r => r.status === 'idle').length
      robotStats.busy = robots.filter(r => r.status === 'busy').length
      robotStats.offline = robots.filter(r => r.status === 'offline').length
      robotStats.error = robots.filter(r => r.status === 'error').length

      // 模拟利用率数据
      robotUtilization.value = robots.slice(0, 8).map(r => ({
        id: r.id,
        name: r.name,
        data: {
          '周一': Math.floor(Math.random() * 60) + 40,
          '周二': Math.floor(Math.random() * 60) + 40,
          '周三': Math.floor(Math.random() * 60) + 40,
          '周四': Math.floor(Math.random() * 60) + 40,
          '周五': Math.floor(Math.random() * 60) + 40,
          '周六': Math.floor(Math.random() * 40) + 20,
          '周日': Math.floor(Math.random() * 40) + 20
        }
      }))
    }

    // 会话
    if (sessionRes.code === 0 && sessionRes.data) {
      activeSessions.value = sessionRes.data.slice(0, 10)
      stats.activeSessions = sessionRes.data.length
    }

    // 队列
    if (queueRes.code === 0 && queueRes.data) {
      queues.value = queueRes.data.map((q, i) => ({
        id: q.id,
        name: q.name || q.queueName || `队列 ${i + 1}`,
        pendingCount: q.pendingCount || q.currentPendingCount || Math.floor(Math.random() * 500),
        processingCount: q.processingCount || Math.floor(Math.random() * 20),
        errorCount: q.errorCount || 0,
        completedCount: q.completedCount || Math.floor(Math.random() * 1000),
        priority: q.priority || ['high', 'medium', 'normal'][Math.floor(Math.random() * 3)],
        icon: ['📦', '📄', '🖥️', '📊', '📧'][i % 5]
      }))
    }

    // 日志
    if (logRes.code === 0 && logRes.data) {
      const logs = logRes.data
      stats.todayExecutions = logs.length
      executionStats.total = logs.length
      executionStats.success = logs.filter(l => l.status === 'success' || l.status === 'completed').length
      executionStats.failed = logs.filter(l => l.status === 'failed').length
      executionStats.running = logs.filter(l => l.status === 'running').length
      stats.successRate = executionStats.total > 0
        ? Math.round((executionStats.success / executionStats.total) * 100)
        : 100
      recentLogs.value = logs.slice(0, 10).map(l => ({
        id: l.id,
        taskName: l.taskName || l.processName || '未知任务',
        robotName: l.robotName || 'Robot-01',
        status: l.status,
        duration: l.duration || `${Math.floor(Math.random() * 60)}s`,
        startTime: l.startTime || l.createTime || new Date().toLocaleString()
      }))
    }

    // 告警
    if (notifRes.code === 0 && notifRes.data) {
      alerts.value = notifRes.data.map(n => ({
        id: n.id,
        title: n.title || '系统告警',
        message: n.content || n.message || '检测到异常情况',
        level: n.level || ['danger', 'warning', 'info'][Math.floor(Math.random() * 3)],
        time: n.createTime || new Date().toLocaleString()
      }))
    }

    // 如果没有数据，生成模拟数据
    if (queues.value.length === 0) {
      queues.value = [
        { id: 1, name: '发票处理', icon: '📦', pendingCount: 1234, processingCount: 56, errorCount: 23, completedCount: 8921, priority: 'high' },
        { id: 2, name: '订单审核', icon: '📄', pendingCount: 456, processingCount: 12, errorCount: 5, completedCount: 3201, priority: 'medium' },
        { id: 3, name: '数据同步', icon: '🖥️', pendingCount: 89, processingCount: 8, errorCount: 0, completedCount: 1456, priority: 'normal' },
        { id: 4, name: '报表生成', icon: '📊', pendingCount: 234, processingCount: 15, errorCount: 3, completedCount: 2134, priority: 'medium' },
        { id: 5, name: '邮件处理', icon: '📧', pendingCount: 567, processingCount: 22, errorCount: 8, completedCount: 4567, priority: 'high' }
      ]
    }

    if (alerts.value.length === 0) {
      alerts.value = [
        { id: 1, title: '队列积压严重', message: '发票处理队列待处理任务超过 1000，建议添加机器人', level: 'danger', time: '10:32' },
        { id: 2, title: 'Robot-03 连续失败', message: '数据同步流程连续失败超过 5 次，请检查配置', level: 'warning', time: '09:45' },
        { id: 3, title: 'Robot-01 维护提醒', message: 'Robot-01 即将到达维护时间，预计 2 小时后', level: 'info', time: '08:30' },
        { id: 4, title: '许可证即将过期', message: '系统许可证将在 30 天后过期，请及时续费', level: 'warning', time: '昨天' },
        { id: 5, title: '执行成功率下降', message: '过去 1 小时执行成功率降至 85%，请关注', level: 'warning', time: '昨天' }
      ]
    }

  } catch (e) {
    console.error('加载数据失败:', e)
  } finally {
    isRefreshing.value = false
  }
}

// 定时刷新
let refreshTimer = null

watch(refreshInterval, (newVal) => {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = setInterval(loadData, newVal)
})

onMounted(() => {
  loadUserFromStorage()
  loadData()
  refreshTimer = setInterval(loadData, refreshInterval.value)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})

const loadUserFromStorage = () => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      Object.assign(userInfo, user)
    } catch (e) {}
  }
}
</script>

<style scoped>
/* ==================== CSS 变量系统 ==================== */
.rpa-dashboard {
  --primary: #3b82f6;
  --primary-light: #60a5fa;
  --success: #22c55e;
  --warning: #f59e0b;
  --danger: #ef4444;
  --info: #6b7280;

  --bg-primary: #f8fafc;
  --bg-secondary: #ffffff;
  --bg-tertiary: #f1f5f9;

  --text-primary: #0f172a;
  --text-secondary: #475569;
  --text-muted: #94a3b8;

  --border-color: #e2e8f0;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.12);

  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;

  --transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  min-height: 100vh;
  background: var(--bg-primary);
}

/* ==================== 头部导航 ==================== */
.dashboard-header {
  height: 64px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

.header-left { flex-shrink: 0; }

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.main-nav {
  background: transparent;
  border: none;
}

.main-nav .el-menu-item {
  padding: 0 20px;
  font-weight: 500;
  color: var(--text-secondary);
  height: 64px;
  line-height: 64px;
  border-bottom: 2px solid transparent;
  transition: var(--transition);
}

.main-nav .el-menu-item:hover {
  color: var(--primary);
  background: rgba(59, 130, 246, 0.05);
}

.main-nav .el-menu-item.is-active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.refresh-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(34, 197, 94, 0.1);
  border-radius: 20px;
  color: var(--success);
  font-size: 13px;
  font-weight: 500;
}

.refresh-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success);
  animation: pulse 2s infinite;
}

.refresh-indicator.refreshing {
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary);
}

.refresh-indicator.refreshing .refresh-dot {
  background: var(--primary);
  animation: pulse 0.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.icon-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  padding: 8px;
  border-radius: 8px;
  transition: var(--transition);
}

.icon-btn:hover {
  background: var(--bg-tertiary);
  color: var(--primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: var(--transition);
}

.user-info:hover {
  background: var(--bg-tertiary);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

/* ==================== 主内容区 ==================== */
.dashboard-main {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ==================== 统计卡片 ==================== */
.stats-section { }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary);
}

.stat-card.danger {
  border-color: rgba(239, 68, 68, 0.3);
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.02) 0%, var(--bg-secondary) 100%);
}

.stat-card.has-alert {
  border-color: rgba(245, 158, 11, 0.3);
}

.stat-glow {
  position: absolute;
  top: -100%;
  left: -100%;
  width: 300%;
  height: 300%;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.stat-card:hover .stat-glow { opacity: 0.08; }

.glow-orange { background: radial-gradient(circle, #f59e0b 0%, transparent 70%); }
.glow-green { background: radial-gradient(circle, #22c55e 0%, transparent 70%); }
.glow-blue { background: radial-gradient(circle, #3b82f6 0%, transparent 70%); }
.glow-red { background: radial-gradient(circle, #ef4444 0%, transparent 70%); }
.glow-cyan { background: radial-gradient(circle, #06b6d4 0%, transparent 70%); }

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  flex-shrink: 0;
}

.stat-card.danger .stat-icon { background: linear-gradient(135deg, var(--danger) 0%, #f87171 100%); box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4); }
.stat-card.has-alert .stat-icon { background: linear-gradient(135deg, var(--warning) 0%, #fbbf24 100%); box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4); }
.stat-card.success .stat-icon { background: linear-gradient(135deg, var(--success) 0%, #4ade80 100%); box-shadow: 0 4px 12px rgba(34, 197, 94, 0.4); }

.stat-content { flex: 1; min-width: 0; }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  font-feature-settings: 'tnum';
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.stat-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.stat-meta.success { color: var(--success); font-weight: 500; }

.stat-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 6px;
}

.stat-trend.up { background: rgba(34, 197, 94, 0.1); color: var(--success); }
.stat-trend.down { background: rgba(239, 68, 68, 0.1); color: var(--danger); }

/* ==================== 通用区块 ==================== */
.section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.title-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
}

.title-icon.gradient-orange { background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%); box-shadow: 0 4px 12px rgba(245, 158, 11, 0.35); }
.title-icon.gradient-purple { background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%); box-shadow: 0 4px 12px rgba(139, 92, 246, 0.35); }
.title-icon.gradient-cyan { background: linear-gradient(135deg, #06b6d4 0%, #22d3ee 100%); box-shadow: 0 4px 12px rgba(6, 182, 212, 0.35); }

.section-actions {
  display: flex;
  gap: 8px;
}

/* ==================== 面板 ==================== */
.panel {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  overflow: hidden;
  transition: var(--transition);
}

.panel:hover {
  box-shadow: var(--shadow-md);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.02) 0%, transparent 100%);
}

.panel-header h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-header h4 .el-icon { color: var(--primary); }

.panel-body { padding: 16px 18px; }

/* ==================== 队列监控 ==================== */
.queue-grid {
  display: grid;
  grid-template-columns: 240px 1fr 260px;
  gap: 16px;
}

.health-indicators {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
}

.health-item {
  text-align: center;
  padding: 12px;
  border-radius: var(--radius-md);
  min-width: 60px;
}

.health-item.healthy { background: rgba(34, 197, 94, 0.1); }
.health-item.warning { background: rgba(245, 158, 11, 0.1); }
.health-item.critical { background: rgba(239, 68, 68, 0.1); }

.health-item .count {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.health-item.healthy .count { color: var(--success); }
.health-item.warning .count { color: var(--warning); }
.health-item.critical .count { color: var(--danger); }

.health-item .label {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.queue-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.queue-icon { font-size: 16px; }

.count-cell {
  font-weight: 600;
  font-feature-settings: 'tnum';
}

.count-cell.danger { color: var(--danger); }
.count-cell.warning { color: var(--warning); }
.count-cell.success { color: var(--success); }
.count-cell.processing { color: var(--primary); }

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid transparent;
}

.alert-item:hover {
  background: var(--bg-secondary);
  border-color: var(--primary);
  transform: translateX(4px);
}

.alert-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
  background: var(--danger);
}

.alert-rank.first { background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%); }
.alert-rank.second { background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%); }
.alert-rank.third { background: linear-gradient(135deg, #d97706 0%, #b45309 100%); }

.alert-info { flex: 1; min-width: 0; }

.alert-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.alert-detail {
  font-size: 11px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

/* ==================== 机器人 ==================== */
.robot-grid {
  display: grid;
  grid-template-columns: 240px 1fr 280px;
  gap: 16px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
}

.status-card:hover { transform: scale(1.02); }

.status-card.idle { background: rgba(34, 197, 94, 0.1); }
.status-card.busy { background: rgba(59, 130, 246, 0.1); }
.status-card.offline { background: rgba(107, 114, 128, 0.1); }
.status-card.error { background: rgba(239, 68, 68, 0.1); }

.status-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.status-card.idle .status-icon { background: var(--success); }
.status-card.busy .status-icon { background: var(--primary); }
.status-card.offline .status-icon { background: var(--info); }
.status-card.error .status-icon { background: var(--danger); }

.status-info .count {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.status-info .label {
  font-size: 11px;
  color: var(--text-muted);
}

.avg-utilization {
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.util-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.util-value { font-weight: 600; }

/* 热力图 */
.heatmap-container { overflow-x: auto; }

.heatmap-row { display: flex; }

.heatmap-cell {
  padding: 8px 6px;
  text-align: center;
  font-size: 12px;
  min-width: 50px;
  border-bottom: 1px solid var(--border-color);
}

.robot-header, .robot-name {
  min-width: 120px;
  text-align: left;
  font-weight: 600;
  color: var(--text-secondary);
}

.robot-name {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--primary);
}

.robot-name:hover { text-decoration: underline; }

.day-header { font-size: 11px; color: var(--text-muted); }

.heat-cell {
  cursor: default;
  font-weight: 600;
  border-radius: 4px;
}

.heat-cell.high { background: rgba(34, 197, 94, 0.4); color: #166534; }
.heat-cell.medium { background: rgba(59, 130, 246, 0.3); color: #1e40af; }
.heat-cell.low { background: rgba(245, 158, 11, 0.2); color: #92400e; }
.heat-cell.idle { background: rgba(107, 114, 128, 0.1); color: var(--text-muted); }

.heatmap-legend {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-muted);
}

.legend-item { display: flex; align-items: center; gap: 4px; }

.dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.dot.high { background: rgba(34, 197, 94, 0.5); }
.dot.medium { background: rgba(59, 130, 246, 0.4); }
.dot.low { background: rgba(245, 158, 11, 0.3); }
.dot.idle { background: rgba(107, 114, 128, 0.2); }

/* 会话列表 */
.session-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 280px;
  overflow-y: auto;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  transition: var(--transition);
}

.session-item:hover {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
}

.session-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.session-icon { font-size: 20px; }
.session-icon.running { color: var(--success); }
.session-icon.paused { color: var(--warning); }

.session-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.session-meta span { display: flex; align-items: center; gap: 4px; }

.session-actions { display: flex; gap: 6px; }

/* ==================== 告警中心 ==================== */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 16px;
}

.alert-tabs :deep(.el-radio-button__inner) {
  padding: 5px 12px;
  font-size: 12px;
}

.alert-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-card-item {
  display: flex;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  transition: var(--transition);
}

.alert-card-item:hover {
  border-color: var(--primary);
  transform: translateX(4px);
}

.alert-level-indicator {
  width: 4px;
  flex-shrink: 0;
}

.alert-card-item.danger .alert-level-indicator { background: var(--danger); }
.alert-card-item.warning .alert-level-indicator { background: var(--warning); }
.alert-card-item.info .alert-level-indicator { background: var(--primary); }

.alert-content {
  flex: 1;
  padding: 12px 14px;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.alert-icon-wrapper {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.alert-card-item.danger .alert-icon-wrapper { background: rgba(239, 68, 68, 0.1); color: var(--danger); }
.alert-card-item.warning .alert-icon-wrapper { background: rgba(245, 158, 11, 0.1); color: var(--warning); }
.alert-card-item.info .alert-icon-wrapper { background: rgba(59, 130, 246, 0.1); color: var(--primary); }

.alert-title {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.alert-body {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 8px;
}

.alert-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-muted);
}

.alert-actions { display: flex; gap: 4px; }

/* ==================== AI 诊断 ==================== */
.ai-insight { }

.insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 12px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.08) 0%, rgba(139, 92, 246, 0.02) 100%);
  border-radius: var(--radius-md);
  border: 1px solid rgba(139, 92, 246, 0.15);
}

.insight-icon { font-size: 18px; }

.insight-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.insight-list { display: flex; flex-direction: column; gap: 12px; }

.insight-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--primary);
  transition: var(--transition);
}

.insight-item:hover {
  background: var(--bg-secondary);
  border-color: var(--warning);
  border-left-color: var(--warning);
}

.insight-number {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.insight-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.insight-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 8px;
}

.insight-actions { display: flex; gap: 8px; }

/* ==================== 快捷操作 ==================== */
.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.quick-action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}

.quick-action-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}

.action-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
  transition: var(--transition);
}

.quick-action-card:hover .action-icon {
  transform: scale(1.1) rotate(5deg);
}

.action-shine {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 40%, rgba(255, 255, 255, 0.3) 50%, transparent 60%);
  transform: translateX(-100%);
  transition: transform 0.6s ease;
}

.quick-action-card:hover .action-shine {
  transform: translateX(100%);
}

.action-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.action-desc {
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
}

/* ==================== 图表 ==================== */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 16px;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  transition: var(--transition);
}

.summary-item:hover {
  transform: translateY(-2px);
}

.summary-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.summary-icon.total { background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%); }
.summary-icon.success { background: linear-gradient(135deg, var(--success) 0%, #4ade80 100%); }
.summary-icon.failed { background: linear-gradient(135deg, var(--danger) 0%, #f87171 100%); }
.summary-icon.running { background: linear-gradient(135deg, var(--warning) 0%, #fbbf24 100%); }

.summary-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.summary-label {
  font-size: 11px;
  color: var(--text-muted);
}

.success-rate-display {
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.rate-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.rate-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
}

.rate-label {
  font-size: 12px;
  color: var(--text-muted);
}

/* ==================== 日志 ==================== */
.logs-card .panel-body { padding: 0; }

/* ==================== 表格 ==================== */
.modern-table :deep(.el-table__header th) {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  font-weight: 600;
}

.modern-table :deep(.el-table__body tr) {
  transition: var(--transition);
}

.modern-table :deep(.el-table__body tr:hover > td:first-child) {
  box-shadow: inset 4px 0 0 var(--primary);
}

.modern-table :deep(.el-table__body tr:hover > td) {
  background: rgba(59, 130, 246, 0.03);
}

/* ==================== 空状态 ==================== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
  color: var(--text-muted);
  gap: 8px;
}

.empty-state .el-icon {
  font-size: 36px;
  opacity: 0.5;
}

.success-icon { color: var(--success) !important; opacity: 0.8 !important; }

/* ==================== 响应式 ==================== */
@media (max-width: 1400px) {
  .stats-grid { grid-template-columns: repeat(3, 1fr); }
  .queue-grid, .robot-grid { grid-template-columns: 1fr 1fr; }
  .bottom-section { grid-template-columns: 1fr; }
  .quick-actions-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .queue-grid, .robot-grid, .chart-grid { grid-template-columns: 1fr; }
  .quick-actions-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .dashboard-header { padding: 0 16px; }
  .header-center { display: none; }
  .dashboard-main { padding: 16px; }
  .stats-grid { grid-template-columns: 1fr; }
  .quick-actions-grid { grid-template-columns: repeat(2, 1fr); }
}

/* ==================== 滚动条 ==================== */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.05); border-radius: 3px; }
::-webkit-scrollbar-thumb { background: rgba(0, 0, 0, 0.15); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: rgba(0, 0, 0, 0.25); }
</style>
