<template>
  <div class="app-layout">
    <!-- 顶部导航栏 -->
    <header class="top-navbar">
      <div class="logo-area">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
          </svg>
        </div>
        <span class="system-name">RPA智能运营平台</span>
      </div>

      <nav class="main-nav">
        <div
          v-for="menu in accessibleNavItems"
          :key="menu.key"
          class="nav-item"
          :class="{ active: activeNav === menu.key }"
          @click="handleNavClick(menu)"
        >
          <span class="nav-icon" v-html="menu.icon"></span>
          <span>{{ menu.label }}</span>
        </div>
      </nav>

      <div class="user-area">
        <div class="user-dropdown" @click.stop="toggleDropdown">
          <div class="avatar-circle">{{ userInitials }}</div>
          <div class="user-info">
            <span class="user-name">{{ currentUser.realName || currentUser.username }}</span>
            <span class="user-role">{{ getRoleText(currentUser.role) }}</span>
          </div>
          <span class="dropdown-arrow">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M7 10l5 5 5-5z"/>
            </svg>
          </span>
          <Transition name="dropdown">
            <div class="dropdown-menu" v-if="dropdownVisible" @click.stop>
              <div class="dropdown-item" @click="goToProfile">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                <span>个人中心</span>
              </div>
              <div class="dropdown-divider"></div>
              <div class="dropdown-item danger" @click="handleLogout">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
                <span>退出登录</span>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </header>

    <!-- 主内容区域 -->
    <div class="main-container">
      <!-- 侧边栏 -->
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6 1.41-1.41z"/>
          </svg>
        </div>

        <div class="sidebar-header" v-if="!sidebarCollapsed">
          <span class="sidebar-title">{{ currentSidebarTitle }}</span>
        </div>

        <nav class="sidebar-menu">
          <template v-for="item in currentSidebarMenu" :key="item.key">
            <div v-if="!item.children" class="menu-item"
              :class="{ active: activeMenu === item.key }"
              @click="switchMenu(item.key)"
            >
              <span class="menu-icon" v-html="item.icon"></span>
              <span class="menu-text" v-if="!sidebarCollapsed">{{ item.label }}</span>
            </div>
            <div v-else class="menu-group">
              <div class="menu-item has-submenu"
                :class="{ active: activeMenu.startsWith(item.key) }"
                @click="toggleSubmenu(item)"
              >
                <span class="menu-icon" v-html="item.icon"></span>
                <span class="menu-text" v-if="!sidebarCollapsed">{{ item.label }}</span>
                <span class="submenu-arrow" v-if="!sidebarCollapsed">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                    <path d="M7 10l5 5 5-5z"/>
                  </svg>
                </span>
              </div>
              <Transition name="submenu">
                <div v-if="!sidebarCollapsed && expandedSubmenus.includes(item.key)" class="submenu">
                  <div v-for="sub in item.children" :key="sub.key"
                    class="menu-item submenu-item"
                    :class="{ active: activeMenu === sub.key }"
                    @click="switchMenu(sub.key)"
                  >
                    <span class="menu-text">{{ sub.label }}</span>
                  </div>
                </div>
              </Transition>
            </div>
          </template>
        </nav>
      </aside>

      <!-- 内容区 -->
      <main class="content-area">
        <!-- ========== 首页 ========== -->
        <div v-if="activeMenu === 'dashboard'" class="dashboard-content">
          <div class="content-header">
            <h2>数据概览</h2>
            <p class="header-tip">实时监控RPA系统运行状态</p>
          </div>

          <!-- 统计卡片 -->
          <div class="stats-grid">
            <div class="stat-card clickable" @click="switchMenu('tasks')">
              <div class="stat-icon-box blue">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.tasks }}</div>
                <div class="stat-label">任务总数</div>
                <div class="stat-change up">+{{ statsChange.tasks }}%</div>
              </div>
              <div class="stat-arrow">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/></svg>
              </div>
            </div>
            <div class="stat-card clickable" @click="switchMenu('robots')">
              <div class="stat-icon-box green">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7s2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z"/></svg>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.robots }}</div>
                <div class="stat-label">机器人总数</div>
                <div class="stat-change up">+{{ statsChange.robots }}%</div>
              </div>
              <div class="stat-arrow">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/></svg>
              </div>
            </div>
            <div class="stat-card clickable" @click="switchMenu('processes')">
              <div class="stat-icon-box purple">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.processes }}</div>
                <div class="stat-label">流程总数</div>
                <div class="stat-change up">+{{ statsChange.processes }}%</div>
              </div>
              <div class="stat-arrow">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/></svg>
              </div>
            </div>
            <div class="stat-card clickable" @click="switchMenu('dataCollect')">
              <div class="stat-icon-box orange">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-6 10H6v-2h10v2zm4-4H6v-2h12v2z"/></svg>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.dataCollects }}</div>
                <div class="stat-label">数据采集</div>
                <div class="stat-change up">点击查看</div>
              </div>
              <div class="stat-arrow">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/></svg>
              </div>
            </div>
          </div>

          <!-- 图表区域 -->
          <div class="charts-row">
            <div class="chart-card">
              <div class="chart-header">
                <h3>任务执行趋势</h3>
                <div class="period-selector">
                  <button v-for="p in periods" :key="p.value"
                    class="period-btn" :class="{ active: selectedPeriod === p.value }"
                    @click="changePeriod(p.value)">
                    {{ p.label }}
                  </button>
                </div>
              </div>
              <div class="chart-body">
                <v-chart :option="lineChartOption" autoresize style="height: 320px;"></v-chart>
              </div>
            </div>
            <div class="chart-card chart-small">
              <div class="chart-header">
                <h3>任务状态分布</h3>
              </div>
              <div class="chart-body">
                <v-chart :option="pieChartOption" autoresize style="height: 320px;"></v-chart>
              </div>
            </div>
          </div>

          <!-- 状态概览 -->
          <div class="status-row">
            <div class="status-card" v-for="s in taskStatusList" :key="s.key">
              <div class="status-indicator" :class="s.key"></div>
              <div class="status-info">
                <div class="status-value">{{ s.value }}</div>
                <div class="status-label">{{ s.label }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- ========== 用户管理 ========== -->
        <div v-if="activeMenu === 'users'" class="management-content">
          <div class="content-header">
            <h2>用户管理</h2>
            <p class="header-tip">管理系统用户账户</p>
          </div>

          <div class="toolbar">
            <div class="search-box">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
              <input v-model="userSearch" placeholder="搜索用户名/姓名/邮箱..." @input="filterUsers" />
            </div>
            <el-button type="primary" @click="showUserModal(null)">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
              新建用户
            </el-button>
          </div>

          <el-table :data="filteredUsers" v-loading="loading" stripe border class="data-table">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="realName" label="真实姓名" min-width="100">
              <template #default="{ row }">{{ row.realName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="160">
              <template #default="{ row }">{{ row.email || '-' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" min-width="120">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column prop="role" label="角色" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.role === 1 ? 'danger' : 'default'" size="small">
                  {{ row.role === 1 ? '管理员' : '普通用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="showUserModal(row)">编辑</el-button>
                <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleUserStatus(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button link type="danger" @click="deleteUser(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- ========== 任务管理 ========== -->
        <div v-if="activeMenu === 'tasks'" class="management-content">
          <div class="content-header">
            <h2>任务管理</h2>
            <p class="header-tip">创建和管理RPA自动化任务</p>
          </div>

          <el-tabs v-model="taskActiveTab" class="custom-tabs">
            <!-- 任务列表 -->
            <el-tab-pane label="任务列表" name="taskList">
              <div class="toolbar">
                <div class="search-box">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
                  <input v-model="taskSearch" placeholder="搜索任务名称/企业名称..." />
                </div>
                <el-select v-model="taskStatusFilter" placeholder="状态筛选" clearable style="width: 120px;">
                  <el-option label="待执行" :value="0" />
                  <el-option label="执行中" :value="1" />
                  <el-option label="成功" :value="2" />
                  <el-option label="失败" :value="3" />
                </el-select>
                <el-button type="primary" @click="showTaskModal(null)">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                  新建任务
                </el-button>
              </div>

              <el-table :data="paginatedTasks" v-loading="loading" stripe border class="data-table">
                <el-table-column type="index" label="序号" width="70" align="center" />
                <el-table-column prop="taskCode" label="任务编码" min-width="140" />
                <el-table-column prop="name" label="任务名称" min-width="160" show-overflow-tooltip />
                <el-table-column prop="taxId" label="纳税人识别号" min-width="160" />
                <el-table-column prop="companyName" label="企业名称" min-width="180" show-overflow-tooltip />
                <el-table-column prop="status" label="任务状态" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getTaskStatusType(row.status)" size="small">{{ getTaskStatusText(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" min-width="160" />
                <el-table-column label="操作" width="150" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="viewTaskDetail(row)">详情</el-button>
                    <el-button link type="primary" @click="showTaskModal(row)">编辑</el-button>
                    <el-button link type="danger" @click="deleteTask(row.id)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                v-model:current-page="taskPage"
                v-model:page-size="taskPageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="filteredTasks.length"
                layout="total, sizes, prev, pager, next, jumper"
                class="custom-pagination"
              />
            </el-tab-pane>

            <!-- 执行日志 -->
            <el-tab-pane label="执行日志" name="execLog">
              <div class="toolbar">
                <div class="search-box">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
                  <input v-model="logSearch" placeholder="搜索任务编码..." />
                </div>
                <el-select v-model="logStatusFilter" placeholder="状态筛选" clearable style="width: 100px;">
                  <el-option label="成功" value="success" />
                  <el-option label="失败" value="failed" />
                  <el-option label="进行中" value="running" />
                </el-select>
              </div>

              <el-table :data="paginatedLogs" v-loading="loading" stripe border class="data-table">
                <el-table-column type="index" label="执行序号" width="80" align="center" />
                <el-table-column prop="executionId" label="执行ID" min-width="140" />
                <el-table-column prop="taskCode" label="任务编码" min-width="140" />
                <el-table-column prop="processCode" label="流程编码" min-width="120" />
                <el-table-column prop="robotCode" label="机器人编码" min-width="120" />
                <el-table-column prop="status" label="执行状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getLogStatusType(row.status)" size="small">{{ getLogStatusText(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="startTime" label="开始时间" min-width="160" />
                <el-table-column prop="endTime" label="结束时间" min-width="160" />
                <el-table-column prop="duration" label="执行时长" width="100" align="center" />
                <el-table-column label="操作" width="80" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="viewLogDetail(row)">详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                v-model:current-page="logPage"
                v-model:page-size="logPageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="filteredLogs.length"
                layout="total, sizes, prev, pager, next, jumper"
                class="custom-pagination"
              />
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- ========== 机器人管理 ========== -->
        <div v-if="activeMenu === 'robots'" class="management-content">
          <div class="content-header">
            <h2>机器人管理</h2>
            <p class="header-tip">管理和监控RPA机器人</p>
          </div>

          <div class="toolbar">
            <div class="search-box">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
              <input v-model="robotSearch" placeholder="搜索机器人名称/IP地址..." />
            </div>
            <el-select v-model="robotStatusFilter" placeholder="状态筛选" clearable style="width: 120px;">
              <el-option label="活跃" value="active" />
              <el-option label="空闲" value="idle" />
              <el-option label="离线" value="offline" />
            </el-select>
            <el-button type="primary" @click="showRobotModal(null)">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
              注册机器人
            </el-button>
          </div>

          <el-table :data="filteredRobots" v-loading="loading" stripe border class="data-table">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="name" label="机器人名称" min-width="140" />
            <el-table-column prop="ip" label="IP地址" min-width="140">
              <template #default="{ row }">{{ row.ip || '-' }}</template>
            </el-table-column>
            <el-table-column prop="hostname" label="主机名" min-width="120">
              <template #default="{ row }">{{ row.hostname || '-' }}</template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="80" align="center">
              <template #default="{ row }">{{ row.type || '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="getRobotStatusType(row.status)" size="small">{{ getRobotStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="cpuUsage" label="CPU" width="100" align="center">
              <template #default="{ row }">
                <el-progress :percentage="row.cpuUsage || 0" :color="getCpuColor(row.cpuUsage)" size="small" :stroke-width="6" />
              </template>
            </el-table-column>
            <el-table-column prop="memoryUsage" label="内存" width="100" align="center">
              <template #default="{ row }">
                <el-progress :percentage="row.memoryUsage || 0" :color="getMemoryColor(row.memoryUsage)" size="small" :stroke-width="6" />
              </template>
            </el-table-column>
            <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="160">
              <template #default="{ row }">{{ formatDateTime(row.lastHeartbeat) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewRobotDetail(row)">详情</el-button>
                <el-button link type="primary" @click="showRobotModal(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteRobot(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- ========== 流程管理 ========== -->
        <div v-if="activeMenu === 'processes'" class="management-content">
          <div class="content-header">
            <h2>流程管理</h2>
            <p class="header-tip">管理RPA自动化流程</p>
          </div>

          <div class="toolbar">
            <div class="search-box">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
              <input v-model="processSearch" placeholder="搜索流程名称..." />
            </div>
            <el-button type="primary" @click="showProcessModal(null)">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
              新建流程
            </el-button>
          </div>

          <el-table :data="filteredProcesses" v-loading="loading" stripe border class="data-table">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="name" label="流程名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="code" label="流程编码" min-width="120" />
            <el-table-column prop="version" label="版本" width="80" align="center" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description || '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
                  {{ row.status === 'active' ? '已发布' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="creatorName" label="创建人" width="100" align="center" />
            <el-table-column prop="createTime" label="创建时间" min-width="160" />
            <el-table-column label="操作" width="150" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewProcessDetail(row)">详情</el-button>
                <el-button link type="primary" @click="showProcessModal(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteProcess(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- ========== 数据管理 ========== -->
        <div v-if="activeMenu.startsWith('data')" class="management-content">
          <!-- 数据采集 -->
          <div v-if="activeMenu === 'dataCollect'">
            <div class="content-header">
              <h2>数据采集</h2>
              <p class="header-tip">配置和管理数据采集任务</p>
            </div>

            <!-- 统计图表 -->
            <div class="stats-chart-row">
              <div class="chart-card">
                <div class="chart-header">
                  <h3>采集统计</h3>
                  <div class="chart-stats">
                    <div class="chart-stat-item"><span class="chart-stat-dot total"></span>总数 {{ dataCollectStats.total }}</div>
                    <div class="chart-stat-item"><span class="chart-stat-dot success"></span>成功 {{ dataCollectStats.success }}</div>
                    <div class="chart-stat-item"><span class="chart-stat-dot running"></span>采集中 {{ dataCollectStats.running }}</div>
                    <div class="chart-stat-item"><span class="chart-stat-dot failed"></span>失败 {{ dataCollectStats.failed }}</div>
                  </div>
                </div>
                <v-chart :option="dataCollectChartOption" autoresize style="height: 200px;"></v-chart>
              </div>
            </div>

            <div class="toolbar">
              <div class="search-box">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
                <input v-model="dataCollectSearch" placeholder="搜索采集名称..." />
              </div>
              <el-button type="primary" @click="showDataCollectModal(null)">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                新建采集
              </el-button>
            </div>
            <el-table :data="filteredDataCollects" v-loading="loading" stripe border class="data-table">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="name" label="采集名称" min-width="160" />
              <el-table-column prop="sourceUrl" label="数据来源" min-width="180" show-overflow-tooltip />
              <el-table-column prop="sourceType" label="类型" width="80" align="center" />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="dataCount" label="数据量" width="80" align="center" />
              <el-table-column prop="lastCollectTime" label="最后采集" min-width="160">
                <template #default="{ row }">{{ row.lastCollectTime ? new Date(row.lastCollectTime).toLocaleString() : '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" @click="viewCollectData(row)">查看数据</el-button>
                  <el-button link type="primary" @click="runDataCollect(row)">执行</el-button>
                  <el-button link type="primary" @click="showDataCollectModal(row)">编辑</el-button>
                  <el-button link type="danger" @click="deleteDataCollect(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="taskPage"
              v-model:page-size="taskPageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="filteredDataCollects.length"
              layout="total, sizes, prev, pager, next, jumper"
              class="custom-pagination"
            />
          </div>

          <!-- 数据解析 -->
          <div v-if="activeMenu === 'dataParse'">
            <div class="content-header">
              <h2>数据解析</h2>
              <p class="header-tip">配置和管理数据解析规则</p>
            </div>

            <!-- 统计图表 -->
            <div class="stats-chart-row">
              <div class="chart-card">
                <div class="chart-header">
                  <h3>解析统计</h3>
                  <div class="chart-stats">
                    <div class="chart-stat-item"><span class="chart-stat-dot total"></span>总数 {{ dataParseStats.total }}</div>
                    <div class="chart-stat-item"><span class="chart-stat-dot success"></span>成功 {{ dataParseStats.success }}</div>
                    <div class="chart-stat-item"><span class="chart-stat-dot running"></span>解析中 {{ dataParseStats.running }}</div>
                    <div class="chart-stat-item"><span class="chart-stat-dot failed"></span>失败 {{ dataParseStats.failed }}</div>
                  </div>
                </div>
                <v-chart :option="dataParseChartOption" autoresize style="height: 200px;"></v-chart>
              </div>
            </div>

            <div class="toolbar">
              <div class="search-box">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
                <input v-model="dataParseSearch" placeholder="搜索解析名称..." />
              </div>
              <el-button type="primary" @click="showDataParseModal(null)">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                新建解析
              </el-button>
            </div>
            <el-table :data="filteredDataParses" v-loading="loading" stripe border class="data-table">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="name" label="解析名称" min-width="160" />
              <el-table-column prop="parseType" label="解析类型" width="100" align="center" />
              <el-table-column prop="parseRules" label="解析规则" min-width="180" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="成功率" width="100" align="center">
                <template #default="{ row }">{{ row.successCount + row.failCount > 0 ? Math.round(row.successCount / (row.successCount + row.failCount) * 100) + '%' : '0%' }}</template>
              </el-table-column>
              <el-table-column prop="lastParseTime" label="解析时间" min-width="160">
                <template #default="{ row }">{{ row.lastParseTime ? new Date(row.lastParseTime).toLocaleString() : '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" @click="runDataParse(row)">执行</el-button>
                  <el-button link type="primary" @click="showDataParseModal(row)">编辑</el-button>
                  <el-button link type="danger" @click="deleteDataParse(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="taskPage"
              v-model:page-size="taskPageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="filteredDataParses.length"
              layout="total, sizes, prev, pager, next, jumper"
              class="custom-pagination"
            />
          </div>

          <!-- 数据加工 -->
          <div v-if="activeMenu === 'dataProcess'">
            <div class="content-header">
              <h2>数据加工</h2>
              <p class="header-tip">配置和管理数据加工任务</p>
            </div>

            <!-- 统计图表 -->
            <div class="stats-chart-row">
              <div class="chart-card">
                <div class="chart-header">
                  <h3>加工统计</h3>
                  <div class="chart-stats">
                    <div class="chart-stat-item"><span class="chart-stat-dot total"></span>总数 {{ dataProcessStats.total }}</div>
                    <div class="chart-stat-item"><span class="chart-stat-dot success"></span>成功 {{ dataProcessStats.success }}</div>
                    <div class="chart-stat-item"><span class="chart-stat-dot running"></span>加工中 {{ dataProcessStats.running }}</div>
                    <div class="chart-stat-item"><span class="chart-stat-dot failed"></span>失败 {{ dataProcessStats.failed }}</div>
                  </div>
                </div>
                <v-chart :option="dataProcessChartOption" autoresize style="height: 200px;"></v-chart>
              </div>
            </div>

            <div class="toolbar">
              <div class="search-box">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
                <input v-model="dataProcessSearch" placeholder="搜索加工名称..." />
              </div>
              <el-button type="primary" @click="showDataProcessModal(null)">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                新建加工
              </el-button>
            </div>
            <el-table :data="filteredDataProcesses" v-loading="loading" stripe border class="data-table">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="name" label="加工名称" min-width="160" />
              <el-table-column prop="processType" label="加工类型" width="100" align="center" />
              <el-table-column prop="processRules" label="加工规则" min-width="180" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="processedCount" label="处理数量" width="100" align="center" />
              <el-table-column prop="lastProcessTime" label="处理时间" min-width="160">
                <template #default="{ row }">{{ row.lastProcessTime ? new Date(row.lastProcessTime).toLocaleString() : '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" @click="runDataProcess(row)">执行</el-button>
                  <el-button link type="primary" @click="showDataProcessModal(row)">编辑</el-button>
                  <el-button link type="danger" @click="deleteDataProcess(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="taskPage"
              v-model:page-size="taskPageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="filteredDataProcesses.length"
              layout="total, sizes, prev, pager, next, jumper"
              class="custom-pagination"
            />
          </div>

          <!-- 数据查询 -->
          <div v-if="activeMenu === 'dataQuery'">
            <div class="content-header">
              <h2>数据查询</h2>
              <p class="header-tip">查询采集数据和加工后数据</p>
            </div>

            <!-- 查询表单 -->
            <div class="query-form-card">
              <el-form :model="queryFormData" inline class="query-form">
                <el-form-item label="关键字">
                  <el-input v-model="queryFormData.keyword" placeholder="数据名称/来源URL" style="width: 180px;" />
                </el-form-item>
                <el-form-item label="数据来源">
                  <el-select v-model="queryFormData.sourceType" placeholder="全部" clearable style="width: 120px;">
                    <el-option label="原始数据" value="collected" />
                    <el-option label="加工数据" value="processed" />
                  </el-select>
                </el-form-item>
                <el-form-item label="采集配置">
                  <el-select v-model="queryFormData.collectId" placeholder="全部" clearable filterable style="width: 180px;">
                    <el-option v-for="c in dataCollects" :key="c.id" :label="c.name" :value="c.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="数据状态">
                  <el-select v-model="queryFormData.dataStatus" placeholder="请选择" clearable style="width: 120px;">
                    <el-option label="未解析" :value="0" />
                    <el-option label="已解析" :value="1" />
                    <el-option label="解析失败" :value="2" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="doDataQuery">查询</el-button>
                  <el-button @click="resetQueryForm">重置</el-button>
                  <el-button type="success" @click="loadProcessedData">刷新加工数据</el-button>
                </el-form-item>
              </el-form>
            </div>

            <el-tabs v-model="dataQueryTab" class="custom-tabs">
              <el-tab-pane label="原始采集数据" name="collected">
                <el-table :data="paginatedCollectedData" v-loading="loading" stripe border class="data-table">
                  <el-table-column type="index" label="序号" width="70" align="center" />
                  <el-table-column prop="collectName" label="采集名称" min-width="150" show-overflow-tooltip />
                  <el-table-column prop="dataType" label="数据类型" width="100" />
                  <el-table-column prop="sourceUrl" label="来源URL" min-width="200" show-overflow-tooltip />
                  <el-table-column prop="rawData" label="原始数据" min-width="200" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span style="font-size:12px;">{{ formatRawData(row.rawData) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="parseStatus" label="解析状态" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag size="small" :type="row.parseStatus === 1 ? 'success' : row.parseStatus === 2 ? 'danger' : 'info'">
                        {{ ['', '已解析', '失败', '解析中'][row.parseStatus] || '未解析' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="collectTime" label="采集时间" min-width="160" />
                  <el-table-column label="操作" width="80" fixed="right" align="center">
                    <template #default="{ row }">
                      <el-button link type="primary" @click="viewQueryDataDetail(row)">详情</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-pagination
                  v-model:current-page="collectedDataPage"
                  v-model:page-size="collectedDataPageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="collectedDataTotal"
                  layout="total, sizes, prev, pager, next, jumper"
                  class="custom-pagination"
                />
              </el-tab-pane>

              <el-tab-pane label="加工后数据" name="processed">
                <el-table :data="paginatedProcessedData" v-loading="loading" stripe border class="data-table">
                  <el-table-column type="index" label="序号" width="70" align="center" />
                  <el-table-column prop="name" label="数据名称" min-width="150" show-overflow-tooltip />
                  <el-table-column prop="processName" label="加工流程" min-width="120" />
                  <el-table-column prop="collectName" label="原始采集" min-width="120" />
                  <el-table-column prop="dataCategory" label="数据分类" width="100" />
                  <el-table-column prop="sourceType" label="来源类型" width="100" />
                  <el-table-column prop="processedData" label="加工后数据" min-width="200" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span style="font-size:12px;">{{ formatProcessedData(row.processedData) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="processTime" label="加工时间" min-width="160" />
                  <el-table-column label="操作" width="80" fixed="right" align="center">
                    <template #default="{ row }">
                      <el-button link type="primary" @click="viewProcessedDataDetail(row)">详情</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-pagination
                  v-model:current-page="processedDataPage"
                  v-model:page-size="processedDataPageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="processedDataTotal"
                  layout="total, sizes, prev, pager, next, jumper"
                  class="custom-pagination"
                />
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>

        <!-- ========== 通知管理 ========== -->
        <div v-if="activeMenu === 'notification'" class="management-content">
          <div class="content-header">
            <h2>通知管理</h2>
            <p class="header-tip">查看和管理各类系统通知，支持按类型筛选</p>
          </div>

          <!-- 左右布局容器 -->
          <div class="notification-layout">
        <!-- 左侧：操作栏 -->
        <div class="notification-sidebar">
          <div class="notif-sidebar-header">
            <span class="sidebar-title">通知类型</span>
          </div>
          <div class="notif-type-list">
            <div class="notif-type-item" :class="{ active: notifTab === 'collect' }" @click="switchNotifTab('collect')">
              <div class="notif-type-icon collect-icon">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
              </div>
              <div class="notif-type-info">
                <div class="notif-type-name">采集通知</div>
                <div class="notif-type-count">{{ notifCollectCount }} 条</div>
              </div>
              <div class="notif-unread-badge" v-if="notifCollectUnread > 0">{{ notifCollectUnread }}</div>
            </div>
            <div class="notif-type-item" :class="{ active: notifTab === 'temp' }" @click="switchNotifTab('temp')">
              <div class="notif-type-icon temp-icon">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
              </div>
              <div class="notif-type-info">
                <div class="notif-type-name">临时通知</div>
                <div class="notif-type-count">{{ notifTempCount }} 条</div>
              </div>
              <div class="notif-unread-badge" v-if="notifTempUnread > 0">{{ notifTempUnread }}</div>
            </div>
            <div class="notif-type-item" :class="{ active: notifTab === 'user' }" @click="switchNotifTab('user')">
              <div class="notif-type-icon user-icon">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              </div>
              <div class="notif-type-info">
                <div class="notif-type-name">用户操作</div>
                <div class="notif-type-count">{{ notifUserCount }} 条</div>
              </div>
              <div class="notif-unread-badge" v-if="notifUserUnread > 0">{{ notifUserUnread }}</div>
            </div>
          </div>
          
          <div class="notif-filter-section">
            <div class="filter-label">状态筛选</div>
            <el-radio-group v-model="notifStatusFilter" size="default" @change="loadNotifications">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="unread">未读</el-radio-button>
              <el-radio-button label="read">已读</el-radio-button>
            </el-radio-group>
          </div>
          
          <div class="notif-actions">
            <el-button size="small" @click="markNotifAllRead">全部标为已读</el-button>
            <el-button size="small" type="danger" @click="clearReadNotifs">清空已读</el-button>
          </div>
        </div>

        <!-- 右侧：图表+列表 -->
        <div class="notification-main">
          <!-- 统计卡片+折线图 -->
          <div class="notification-overview">
            <div class="notif-chart-wrapper" v-if="notifChartData.length > 0">
              <div class="notif-chart-title">近7天通知趋势 - {{ notifTab === 'collect' ? '采集通知' : notifTab === 'temp' ? '临时通知' : notifTab === 'user' ? '用户操作' : '全部' }}</div>
              <div ref="notifChartRef" class="notif-echarts"></div>
            </div>
            <div class="notif-chart-empty" v-else>
              <p>暂无趋势数据</p>
            </div>
          </div>

          <!-- 通知列表 -->
          <div class="notif-list-header">
            <span>通知列表</span>
            <span class="notif-total">共 {{ notifTotal }} 条</span>
          </div>
          <el-table :data="paginatedNotifs" v-loading="notifLoading" stripe border class="data-table notif-table">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getNotifTypeTag(row.type)">{{ getNotifTypeText(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="通知标题" min-width="150" show-overflow-tooltip />
            <el-table-column prop="content" label="通知内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="creatorName" label="发送者" width="90" align="center" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'unread' ? 'danger' : 'info'">
                  {{ row.status === 'unread' ? '未读' : '已读' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160" />
            <el-table-column label="操作" width="110" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewNotifDetail(row)" v-if="row.status === 'unread'">标记已读</el-button>
                <el-button link type="primary" size="small" @click="viewNotifDetail(row)" v-else>查看</el-button>
                <el-button link type="danger" size="small" @click="deleteNotif(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="notifPage"
            v-model:page-size="notifPageSize"
            :page-sizes="[10, 20, 50]"
            :total="notifTotal"
            layout="total, sizes, prev, pager, next, jumper"
            class="custom-pagination"
          />
        </div>
      </div>

      <!-- 通知详情弹窗 -->
      <el-dialog v-model="notifDetailVisible" :title="notifDetailTitle" width="540px" class="custom-dialog">
        <div class="notif-detail-content" v-html="notifDetailContent"></div>
        <template #footer>
          <el-button @click="notifDetailVisible = false">关闭</el-button>
        </template>
      </el-dialog>

        </div>

      </main>
    </div>

    <!-- ========== 弹窗 ========== -->
    <!-- 用户编辑弹窗 -->
    <el-dialog v-model="userDialogVisible" :title="editingUser ? '编辑用户' : '新建用户'" width="520px" class="custom-dialog">
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username" v-if="!editingUser">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!editingUser">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="userForm.role">
            <el-radio :label="0">普通用户</el-radio>
            <el-radio :label="1">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" v-if="editingUser">
          <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="saveLoading">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 任务编辑弹窗 -->
    <el-dialog v-model="taskDialogVisible" :title="editingTask ? '编辑任务' : '新建任务'" width="520px" class="custom-dialog">
      <el-form :model="taskForm" :rules="taskRules" ref="taskFormRef" label-width="100px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="taskForm.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="所属流程" prop="processId">
          <el-select v-model="taskForm.processId" placeholder="请选择流程" style="width: 100%;">
            <el-option v-for="p in processes" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行机器人" prop="robotId">
          <el-select v-model="taskForm.robotId" placeholder="请选择机器人" style="width: 100%;">
            <el-option v-for="r in robots" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="taskForm.priority" placeholder="请选择优先级" style="width: 100%;">
            <el-option label="低" value="low" />
            <el-option label="普通" value="normal" />
            <el-option label="高" value="high" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="taskForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTask" :loading="saveLoading">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 机器人编辑弹窗 -->
    <el-dialog v-model="robotDialogVisible" :title="editingRobot ? '编辑机器人' : '注册机器人'" width="520px" class="custom-dialog">
      <el-form :model="robotForm" :rules="robotRules" ref="robotFormRef" label-width="100px">
        <el-form-item label="机器人名称" prop="name">
          <el-input v-model="robotForm.name" placeholder="请输入机器人名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="robotForm.type" placeholder="请选择类型" style="width: 100%;">
            <el-option label="桌面机器人" value="Desktop" />
            <el-option label="服务器机器人" value="Server" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP地址" prop="ip">
          <el-input v-model="robotForm.ip" placeholder="请输入IP地址，如：192.168.1.100" />
        </el-form-item>
        <el-form-item label="主机名" prop="hostname">
          <el-input v-model="robotForm.hostname" placeholder="请输入主机名，如：WORKSTATION-01" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="robotForm.port" :min="1" :max="65535" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="robotForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="robotDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRobot" :loading="saveLoading">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 流程编辑弹窗 -->
    <el-dialog v-model="processDialogVisible" :title="editingProcess ? '编辑流程' : '新建流程'" width="520px" class="custom-dialog">
      <el-form :model="processForm" :rules="processRules" ref="processFormRef" label-width="100px">
        <el-form-item label="流程名称" prop="name">
          <el-input v-model="processForm.name" placeholder="请输入流程名称" />
        </el-form-item>
        <el-form-item label="流程编码" prop="code">
          <el-input v-model="processForm.code" placeholder="请输入流程编码，如：ORDER_PROCESS" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="processForm.version" placeholder="请输入版本号，如：1.0.0" />
        </el-form-item>
        <el-form-item label="流程描述">
          <el-input v-model="processForm.description" type="textarea" :rows="3" placeholder="请输入流程描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="processForm.status">
            <el-radio label="draft">草稿</el-radio>
            <el-radio label="active">已发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProcess" :loading="saveLoading">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" :title="detailTitle" width="600px" class="custom-dialog">
      <div class="detail-content" v-html="detailContent"></div>
    </el-dialog>

    <!-- 数据采集编辑弹窗 -->
    <el-dialog v-model="dataCollectDialogVisible" :title="editingDataCollect ? '编辑采集' : '新建采集'" width="600px" class="custom-dialog">
      <el-form :model="dataCollectForm" label-width="100px">
        <el-form-item label="采集名称" required>
          <el-input v-model="dataCollectForm.name" placeholder="请输入采集名称" />
        </el-form-item>
        <el-form-item label="数据来源URL" required>
          <el-input v-model="dataCollectForm.sourceUrl" placeholder="请输入网址，如：https://news.163.com" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="dataCollectForm.sourceType" style="width: 100%;">
            <el-option label="网页" value="web" />
            <el-option label="API" value="api" />
            <el-option label="文件" value="file" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择器规则">
          <el-input v-model="dataCollectForm.selectorRules" type="textarea" :rows="3" placeholder='{"listSelector": "tr", "title": "td:nth-child(1)"}' />
        </el-form-item>
        <el-form-item label="定时表达式">
          <el-input v-model="dataCollectForm.cronExpression" placeholder="如：0 0 * * * ? (每天执行)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataCollectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDataCollect" :loading="saveLoading">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据解析编辑弹窗 -->
    <el-dialog v-model="dataParseDialogVisible" :title="editingDataParse ? '编辑解析' : '新建解析'" width="600px" class="custom-dialog">
      <el-form :model="dataParseForm" label-width="100px">
        <el-form-item label="解析名称" required>
          <el-input v-model="dataParseForm.name" placeholder="请输入解析名称" />
        </el-form-item>
        <el-form-item label="关联采集">
          <el-select v-model="dataParseForm.collectId" placeholder="选择关联的采集配置" clearable style="width: 100%;">
            <el-option v-for="c in dataCollects" :key="c.id" :label="c.name" :value="String(c.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="解析类型">
          <el-select v-model="dataParseForm.parseType" style="width: 100%;">
            <el-option label="JSON" value="json" />
            <el-option label="XML" value="xml" />
            <el-option label="HTML" value="html" />
            <el-option label="正则" value="regex" />
          </el-select>
        </el-form-item>
        <el-form-item label="解析规则">
          <el-input v-model="dataParseForm.parseRules" type="textarea" :rows="3" placeholder='{"title": "trim", "content": "regex:(.+)", "time": "trim"}' />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-select v-model="dataParseForm.outputFormat" style="width: 100%;">
            <el-option label="JSON" value="json" />
            <el-option label="XML" value="xml" />
            <el-option label="CSV" value="csv" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataParseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDataParse" :loading="saveLoading">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据加工编辑弹窗 -->
    <el-dialog v-model="dataProcessDialogVisible" :title="editingDataProcess ? '编辑加工' : '新建加工'" width="600px" class="custom-dialog">
      <el-form :model="dataProcessForm" label-width="100px">
        <el-form-item label="加工名称" required>
          <el-input v-model="dataProcessForm.name" placeholder="请输入加工名称" />
        </el-form-item>
        <el-form-item label="数据源ID">
          <el-input v-model="dataProcessForm.sourceIds" placeholder="留空表示全部数据，多个用逗号分隔" />
        </el-form-item>
        <el-form-item label="加工类型">
          <el-select v-model="dataProcessForm.processType" style="width: 100%;">
            <el-option label="数据转换" value="transform" />
            <el-option label="数据过滤" value="filter" />
            <el-option label="数据聚合" value="aggregate" />
          </el-select>
        </el-form-item>
        <el-form-item label="加工规则">
          <el-input v-model="dataProcessForm.processRules" type="textarea" :rows="3" placeholder='{"filter": {"status": "active"}, "transform": {"name": "[{value}]"}}' />
        </el-form-item>
        <el-form-item label="输出表名">
          <el-input v-model="dataProcessForm.outputTable" placeholder="输出目标表名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataProcessDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDataProcess" :loading="saveLoading">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据查询编辑弹窗 -->
    <el-dialog v-model="dataQueryDialogVisible" :title="editingDataQuery ? '编辑查询' : '新建查询'" width="600px" class="custom-dialog">
      <el-form :model="dataQueryForm" label-width="100px">
        <el-form-item label="查询名称" required>
          <el-input v-model="dataQueryForm.name" placeholder="请输入查询名称" />
        </el-form-item>
        <el-form-item label="数据源表">
          <el-select v-model="dataQueryForm.sourceTable" style="width: 100%;">
            <el-option label="采集数据表" value="collected_data" />
            <el-option label="解析后数据" value="parsed_data" />
          </el-select>
        </el-form-item>
        <el-form-item label="查询条件">
          <el-input v-model="dataQueryForm.queryCondition" type="textarea" :rows="2" placeholder='{"status": "active"}' />
        </el-form-item>
        <el-form-item label="查询列">
          <el-input v-model="dataQueryForm.queryColumns" placeholder="留空查询全部列，多个用逗号分隔，如：id,name,status" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataQueryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDataQuery" :loading="saveLoading">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 遮罩 -->
    <div class="overlay" v-if="dropdownVisible" @click="dropdownVisible = false"></div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 统一请求封装，自动携带 JWT token
const request = async (url, options = {}) => {
  const token = localStorage.getItem('token')
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (token) headers['Authorization'] = `Bearer ${token}`
  return fetch(url, { ...options, headers })
}

// ========== 状态 ==========
const dropdownVisible = ref(false)
const sidebarCollapsed = ref(false)
const activeNav = ref('rpa')
const activeMenu = ref('dashboard')
const expandedSubmenus = ref(['data'])
const loading = ref(false)
const saveLoading = ref(false)
const taskActiveTab = ref('taskList')

// ========== 用户信息 ==========
const currentUser = ref({ id: 1, username: 'admin', realName: '系统管理员', email: 'admin@rpa.com', role: 1 })

// ========== 统计数据 ==========
const stats = ref({ tasks: 0, robots: 0, processes: 0, logs: 0, dataCollects: 0, dataParses: 0, dataProcesses: 0, dataQueries: 0 })
const statsChange = ref({ tasks: 12, robots: 5, processes: 8, logs: 15 })
const taskStatus = ref({ running: 0, pending: 0, completed: 0, failed: 0 })
const selectedPeriod = ref('7d')
const periods = [
  { label: '近7天', value: '7d' },
  { label: '近30天', value: '30d' },
  { label: '近90天', value: '90d' }
]

// ========== 表格数据 ==========
const users = ref([])
const tasks = ref([])
const robots = ref([])
const processes = ref([])
const logs = ref([])
const dataCollects = ref([])
const dataParses = ref([])
const dataProcesses = ref([])
const dataQueries = ref([])

// ========== 搜索筛选 ==========
const userSearch = ref('')
const taskSearch = ref('')
const taskStatusFilter = ref('')
const taskPage = ref(1)
const taskPageSize = ref(10)
const logSearch = ref('')
const logStatusFilter = ref('')
const logPage = ref(1)
const logPageSize = ref(10)
const robotSearch = ref('')
const robotStatusFilter = ref('')
const processSearch = ref('')
const dataCollectSearch = ref('')
const dataParseSearch = ref('')
const dataProcessSearch = ref('')
const dataQuerySearch = ref('')
// ========== 数据查询 ==========
const dataQueryTab = ref('collected')
const collectedData = ref([])
const processedData = ref([])
const collectedDataPage = ref(1)
const collectedDataPageSize = ref(10)
const processedDataPage = ref(1)
const processedDataPageSize = ref(10)

// ========== 通知管理 ==========
const notifTab = ref('collect')
const notifStatusFilter = ref('')
const notifList = ref([])
const notifPage = ref(1)
const notifPageSize = ref(10)
const notifTotal = ref(0)
const notifLoading = ref(false)
const notifDetailVisible = ref(false)
const notifDetailTitle = ref('')
const notifDetailContent = ref('')
const notifChartRef = ref(null)
const notifChartData = ref([])
const notifChartInstance = ref(null)
// 统计
const notifCollectCount = ref(0)
const notifTempCount = ref(0)
const notifUserCount = ref(0)
const notifCollectUnread = ref(0)
const notifTempUnread = ref(0)
const notifUserUnread = ref(0)

const collectedDataTotal = computed(() => collectedData.value.length)
const processedDataTotal = computed(() => processedData.value.length)

const paginatedCollectedData = computed(() => {
  const start = (collectedDataPage.value - 1) * collectedDataPageSize.value
  return collectedData.value.slice(start, start + collectedDataPageSize.value)
})

const paginatedProcessedData = computed(() => {
  const start = (processedDataPage.value - 1) * processedDataPageSize.value
  return processedData.value.slice(start, start + processedDataPageSize.value)
})

const paginatedNotifs = computed(() => {
  const start = (notifPage.value - 1) * notifPageSize.value
  return notifList.value.slice(start, start + notifPageSize.value)
})

const queryFormData = reactive({
  keyword: '',
  collectId: null,
  sourceType: '',
  dataStatus: null
})

const loadCollectedData = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataQuery/collectedData`)
    const result = await res.json()
    if (result.code === 0) {
      collectedData.value = result.data || []
    }
  } catch {
    collectedData.value = []
  } finally {
    loading.value = false
  }
}

const loadProcessedData = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataQuery/processedData`)
    const result = await res.json()
    if (result.code === 0) {
      processedData.value = result.data || []
    }
  } catch {
    processedData.value = []
  } finally {
    loading.value = false
  }
}

const doDataQuery = async () => {
  loading.value = true
  try {
    if (dataQueryTab.value === 'collected') {
      await loadCollectedData()
    } else {
      await loadProcessedData()
    }
  } catch { ElMessage.error('查询失败') }
  finally { loading.value = false }
}

const resetQueryForm = () => {
  queryFormData.keyword = ''
  queryFormData.collectId = null
  queryFormData.sourceType = ''
  queryFormData.dataStatus = null
  collectedData.value = []
  processedData.value = []
}

const formatRawData = (rawData) => {
  if (!rawData) return '-'
  try {
    const obj = JSON.parse(rawData)
    return Object.keys(obj).slice(0, 3).map(k => `${k}:${obj[k]}`).join(', ') + '...'
  } catch {
    return rawData.substring(0, 50) + '...'
  }
}

const formatProcessedData = (data) => {
  if (!data) return '-'
  try {
    const obj = JSON.parse(data)
    return Object.keys(obj).slice(0, 3).map(k => `${k}:${obj[k]}`).join(', ') + '...'
  } catch {
    return data.substring(0, 50) + '...'
  }
}

const viewQueryDataDetail = (row) => {
  detailTitle.value = '原始数据详情'
  detailContent.value = `<pre style="max-height:400px;overflow:auto;font-size:12px;">${JSON.stringify(row, null, 2)}</pre>`
  detailDialogVisible.value = true
}

const viewProcessedDataDetail = (row) => {
  detailTitle.value = '加工数据详情'
  detailContent.value = `<pre style="max-height:400px;overflow:auto;font-size:12px;">${JSON.stringify(row, null, 2)}</pre>`
  detailDialogVisible.value = true
}

// ========== 通知管理 ==========
const getNotifTypeText = (type) => ({ collect: '采集通知', temp: '临时通知', user: '用户操作' }[type] || type)
const getNotifTypeTag = (type) => ({ collect: 'primary', temp: 'warning', user: 'success' }[type] || 'info')

const switchNotifTab = async (tab) => {
  notifTab.value = tab
  notifPage.value = 1
  notifStatusFilter.value = ''
  await loadNotifications()
  // 切换tab后重新渲染图表，只显示选中类型的趋势
  renderNotifChart()
}

const loadNotificationStats = async () => {
  try {
    const res = await request(`${API_BASE}/notification/stats?days=7`)
    const result = await res.json()
    if (result.code === 0) {
      const data = result.data
      notifCollectCount.value = data.totals?.collect || 0
      notifTempCount.value = data.totals?.temp || 0
      notifUserCount.value = data.totals?.user || 0
      notifCollectUnread.value = data.unreads?.collect || 0
      notifTempUnread.value = data.unreads?.temp || 0
      notifUserUnread.value = data.unreads?.user || 0
      notifChartData.value = data.chartData || []
      renderNotifChart()
    }
  } catch { /* silent */ }
}

const loadNotifications = async () => {
  notifLoading.value = true
  try {
    const params = new URLSearchParams({ type: notifTab.value, page: notifPage.value, size: notifPageSize.value })
    if (notifStatusFilter.value) params.set('status', notifStatusFilter.value)
    const res = await request(`${API_BASE}/notification?${params}`)
    const result = await res.json()
    if (result.code === 0) {
      notifList.value = result.data || []
      notifTotal.value = result.total || 0
    }
  } catch { notifList.value = [] } finally { notifLoading.value = false }
}

const viewNotifDetail = async (row) => {
  if (row.status === 'unread') {
    await request(`${API_BASE}/notification/${row.id}/read`, { method: 'PUT' })
    row.status = 'read'
    // 更新未读数
    if (notifTab.value === 'collect') notifCollectUnread.value = Math.max(0, notifCollectUnread.value - 1)
    else if (notifTab.value === 'temp') notifTempUnread.value = Math.max(0, notifTempUnread.value - 1)
    else notifUserUnread.value = Math.max(0, notifUserUnread.value - 1)
  }
  notifDetailTitle.value = row.title
  notifDetailContent.value = `<p><strong>类型：</strong>${getNotifTypeText(row.type)}</p><p><strong>发送者：</strong>${row.creatorName || '-'}</p><p><strong>时间：</strong>${row.createTime || '-'}</p><hr/><p>${row.content || '-'}</p>`
  notifDetailVisible.value = true
}

const markNotifAllRead = async () => {
  try {
    await request(`${API_BASE}/notification/readAll?type=${notifTab.value}`, { method: 'PUT' })
    ElMessage.success('已全部标记为已读')
    notifCollectUnread.value = 0
    notifTempUnread.value = 0
    notifUserUnread.value = 0
    notifList.value.forEach(n => { n.status = 'read' })
  } catch { ElMessage.error('操作失败') }
}

const clearReadNotifs = async () => {
  const reads = notifList.value.filter(n => n.status === 'read')
  if (reads.length === 0) { ElMessage.info('没有已读通知可清理'); return }
  await ElMessageBox.confirm(`确定删除 ${reads.length} 条已读通知吗？`, '提示', { type: 'warning' })
  try {
    for (const n of reads) {
      await request(`${API_BASE}/notification/${n.id}`, { method: 'DELETE' })
    }
    ElMessage.success('清理成功')
    loadNotifications()
    loadNotificationStats()
  } catch { ElMessage.error('清理失败') }
}

const deleteNotif = async (id) => {
  await ElMessageBox.confirm('确定删除该通知吗？', '提示', { type: 'warning' })
  try {
    await request(`${API_BASE}/notification/${id}`, { method: 'DELETE' })
    ElMessage.success('删除成功')
    loadNotifications()
    loadNotificationStats()
  } catch { ElMessage.error('删除失败') }
}

const renderNotifChart = () => {
  if (!notifChartRef.value || notifChartData.value.length === 0) return
  import('echarts').then(({ default: echarts }) => {
    if (notifChartInstance.value) notifChartInstance.value.dispose()
    const chart = echarts.init(notifChartRef.value)
    notifChartInstance.value = chart
    const dates = notifChartData.value.map(d => d.date)
    const collectData = notifChartData.value.map(d => d.collect || 0)
    const tempData = notifChartData.value.map(d => d.temp || 0)
    const userData = notifChartData.value.map(d => d.user || 0)
    
    // 根据当前选中的通知类型决定显示哪些数据
    const currentTab = notifTab.value
    const series = []
    const legendData = []
    
    if (currentTab === 'collect' || currentTab === '') {
      series.push({ name: '采集通知', type: 'line', data: collectData, smooth: true, color: '#1890ff', areaStyle: { opacity: 0.2 } })
      legendData.push('采集通知')
    }
    if (currentTab === 'temp' || currentTab === '') {
      series.push({ name: '临时通知', type: 'line', data: tempData, smooth: true, color: '#fa8c16', areaStyle: { opacity: 0.2 } })
      legendData.push('临时通知')
    }
    if (currentTab === 'user' || currentTab === '') {
      series.push({ name: '用户操作', type: 'line', data: userData, smooth: true, color: '#52c41a', areaStyle: { opacity: 0.2 } })
      legendData.push('用户操作')
    }
    
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: legendData, bottom: 0 },
      grid: { top: 10, right: 20, bottom: 40, left: 40 },
      xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', name: '条数', axisLabel: { fontSize: 11 }, minInterval: 1 },
      series: series
    })
  })
}

// 监听通知 tab 切换
watch(notifTab, () => { loadNotifications() })

// ========== 弹窗 ==========
const userDialogVisible = ref(false)
const taskDialogVisible = ref(false)
const robotDialogVisible = ref(false)
const processDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const detailTitle = ref('')
const detailContent = ref('')

// ========== 编辑状态 ==========
const editingUser = ref(null)
const editingTask = ref(null)
const editingRobot = ref(null)
const editingProcess = ref(null)

// ========== 表单 ==========
const userFormRef = ref(null)
const taskFormRef = ref(null)
const robotFormRef = ref(null)
const processFormRef = ref(null)

const userForm = reactive({ username: '', password: '', realName: '', email: '', phone: '', role: 0, status: 1 })
const taskForm = reactive({ name: '', processId: '', robotId: '', priority: 'normal', remark: '' })
const robotForm = reactive({ name: '', type: 'Desktop', ip: '', hostname: '', port: 8080, description: '' })
const processForm = reactive({ name: '', code: '', version: '1.0.0', description: '', status: 'draft' })

// ========== 验证规则 ==========
const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ min: 6, message: '密码至少 6 位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
}
const taskRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  processId: [{ required: true, message: '请选择流程', trigger: 'change' }],
  robotId: [{ required: true, message: '请选择机器人', trigger: 'change' }]
}
const robotRules = {
  name: [{ required: true, message: '请输入机器人名称', trigger: 'blur' }],
  ip: [{ required: true, message: '请输入IP地址', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}
const processRules = {
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入流程编码', trigger: 'blur' }]
}

// ========== 计算属性 ==========
const userInitials = computed(() => (currentUser.value.realName || currentUser.value.username).charAt(0).toUpperCase())

const taskStatusList = computed(() => [
  { key: 'running', label: '运行中', value: taskStatus.value.running },
  { key: 'pending', label: '待执行', value: taskStatus.value.pending },
  { key: 'completed', label: '已完成', value: taskStatus.value.completed },
  { key: 'failed', label: '失败', value: taskStatus.value.failed }
])

// ========== 导航菜单 ==========
const allNavItems = [
  { key: 'dashboard', label: '首页', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>' },
  { key: 'rpa', label: 'RPA运营', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7s2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z"/></svg>' },
  { key: 'system', label: '系统管理', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19.14 12.94c.04-.31.06-.63.06-.94 0-.31-.02-.63-.06-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>', adminOnly: true }
]

const accessibleNavItems = computed(() => currentUser.value.role === 1 ? allNavItems : allNavItems.filter(m => !m.adminOnly))

// ========== 侧边栏菜单 ==========
const allSidebarMenus = [
  { key: 'dashboard', label: '首页', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>' },
  { key: 'tasks', label: '任务管理', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10H7v-2h10v2z"/></svg>' },
  { key: 'robots', label: '机器人管理', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7s2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z"/></svg>' },
  { key: 'processes', label: '流程管理', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>' },
  {
    key: 'data', label: '数据管理', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-6 10H6v-2h10v2zm4-4H6v-2h12v2z"/></svg>',
    children: [
      { key: 'dataCollect', label: '数据采集' },
      { key: 'dataParse', label: '数据解析' },
      { key: 'dataProcess', label: '数据加工' },
      { key: 'dataQuery', label: '数据查询' }
    ]
  },
  { key: 'notification', label: '通知管理', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>' },
  { key: 'users', label: '用户管理', icon: '<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>', adminOnly: true }
]

const currentSidebarMenu = computed(() => {
  if (activeNav.value === 'dashboard') return [allSidebarMenus[0]]
  if (activeNav.value === 'rpa') return allSidebarMenus.filter(m => !m.adminOnly)
  if (activeNav.value === 'system') return allSidebarMenus.filter(m => m.adminOnly)
  return allSidebarMenus
})

const currentSidebarTitle = computed(() => {
  if (activeNav.value === 'dashboard') return '首页'
  if (activeNav.value === 'rpa') return 'RPA运营'
  if (activeNav.value === 'system') return '系统管理'
  return ''
})

// ========== 筛选后数据 ==========
const filteredUsers = computed(() => users.value.filter(u => !userSearch.value || u.username.includes(userSearch.value) || (u.realName && u.realName.includes(userSearch.value)) || (u.email && u.email.includes(userSearch.value))))
const filteredTasks = computed(() => tasks.value.filter(t => {
  const matchSearch = !taskSearch.value || (t.name && t.name.includes(taskSearch.value)) || (t.companyName && t.companyName.includes(taskSearch.value)) || (t.taskCode && t.taskCode.includes(taskSearch.value))
  const matchStatus = !taskStatusFilter.value || t.status === taskStatusFilter.value
  return matchSearch && matchStatus
}))
const paginatedTasks = computed(() => {
  const start = (taskPage.value - 1) * taskPageSize.value
  return filteredTasks.value.slice(start, start + taskPageSize.value)
})
const filteredLogs = computed(() => logs.value.filter(l => (!logSearch.value || (l.taskCode && l.taskCode.includes(logSearch.value))) && (!logStatusFilter.value || l.status === logStatusFilter.value)))
const paginatedLogs = computed(() => {
  const start = (logPage.value - 1) * logPageSize.value
  return filteredLogs.value.slice(start, start + logPageSize.value)
})
const filteredRobots = computed(() => robots.value.filter(r => (!robotSearch.value || r.name.includes(robotSearch.value) || (r.ip && r.ip.includes(robotSearch.value))) && (!robotStatusFilter.value || r.status === robotStatusFilter.value)))
const filteredProcesses = computed(() => processes.value.filter(p => !processSearch.value || p.name.includes(processSearch.value)))
const filteredDataCollects = computed(() => dataCollects.value.filter(d => !dataCollectSearch.value || d.name.includes(dataCollectSearch.value)))
const filteredDataParses = computed(() => dataParses.value.filter(d => !dataParseSearch.value || d.name.includes(dataParseSearch.value)))
const filteredDataProcesses = computed(() => dataProcesses.value.filter(d => !dataProcessSearch.value || d.name.includes(dataProcessSearch.value)))
const filteredDataQueries = computed(() => dataQueries.value.filter(d => !dataQuerySearch.value || d.name.includes(dataQuerySearch.value)))

// ========== 图表配置 ==========
const lineChartOption = computed(() => {
  const days = selectedPeriod.value === '7d' ? 7 : selectedPeriod.value === '30d' ? 30 : 90
  const labels = []
  const completedData = []
  const newData = []
  const now = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    labels.push(`${d.getMonth() + 1}/${d.getDate()}`)
    completedData.push(Math.floor(Math.random() * 20) + 10)
    newData.push(Math.floor(Math.random() * 15) + 5)
  }
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['完成任务', '新建任务'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '8%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels },
    yAxis: { type: 'value', min: 0 },
    series: [
      { name: '完成任务', type: 'line', smooth: true, data: completedData, itemStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64,158,255,0.1)' } },
      { name: '新建任务', type: 'line', smooth: true, data: newData, itemStyle: { color: '#67c23a' }, areaStyle: { color: 'rgba(103,194,58,0.1)' } }
    ]
  }
})

const pieChartOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['45%', '70%'],
    center: ['50%', '45%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { show: true, formatter: '{b}\n{d}%' },
    data: [
      { value: taskStatus.value.running, name: '运行中', itemStyle: { color: '#e6a23c' } },
      { value: taskStatus.value.pending, name: '待执行', itemStyle: { color: '#909399' } },
      { value: taskStatus.value.completed, name: '已完成', itemStyle: { color: '#67c23a' } },
      { value: taskStatus.value.failed, name: '失败', itemStyle: { color: '#f56c6c' } }
    ]
  }]
}))

// 数据采集统计图表
const dataCollectStats = computed(() => {
  const total = dataCollects.value.reduce((sum, d) => sum + (d.dataCount || 0), 0)
  const success = dataCollects.value.filter(d => d.status === 1).length
  const running = dataCollects.value.filter(d => d.status === 2).length
  const failed = dataCollects.value.filter(d => d.status === 3).length
  return { total, success, running, failed }
})
const dataCollectChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c} 条' },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
  xAxis: { type: 'category', data: ['总采集数', '成功', '采集中', '失败'], axisLabel: { fontSize: 12 } },
  yAxis: { type: 'value', name: '数量', axisLabel: { fontSize: 12 } },
  series: [
    {
      name: '采集数量',
      type: 'bar',
      barWidth: '50%',
      data: [
        { value: dataCollectStats.value.total, itemStyle: { color: '#409eff' } },
        { value: dataCollectStats.value.success, itemStyle: { color: '#67c23a' } },
        { value: dataCollectStats.value.running, itemStyle: { color: '#e6a23c' } },
        { value: dataCollectStats.value.failed, itemStyle: { color: '#f56c6c' } }
      ],
      label: { show: true, position: 'top', fontSize: 12, color: '#666' }
    }
  ]
}))

// 数据解析统计图表
const dataParseStats = computed(() => {
  const total = dataParses.value.reduce((sum, d) => sum + (d.successCount || 0) + (d.failCount || 0), 0)
  const success = dataParses.value.reduce((sum, d) => sum + (d.successCount || 0), 0)
  const running = dataParses.value.filter(d => d.status === 2).length
  const failed = dataParses.value.reduce((sum, d) => sum + (d.failCount || 0), 0)
  return { total, success, running, failed }
})
const dataParseChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c} 条' },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
  xAxis: { type: 'category', data: ['总解析数', '成功', '解析中', '失败'], axisLabel: { fontSize: 12 } },
  yAxis: { type: 'value', name: '数量', axisLabel: { fontSize: 12 } },
  series: [
    {
      name: '解析数量',
      type: 'bar',
      barWidth: '50%',
      data: [
        { value: dataParseStats.value.total, itemStyle: { color: '#67c23a' } },
        { value: dataParseStats.value.success, itemStyle: { color: '#95d475' } },
        { value: dataParseStats.value.running, itemStyle: { color: '#e6a23c' } },
        { value: dataParseStats.value.failed, itemStyle: { color: '#f56c6c' } }
      ],
      label: { show: true, position: 'top', fontSize: 12, color: '#666' }
    }
  ]
}))

// 数据加工统计图表
const dataProcessStats = computed(() => {
  const total = dataProcesses.value.reduce((sum, d) => sum + (d.processedCount || 0), 0)
  const success = dataProcesses.value.filter(d => d.status === 1).length
  const running = dataProcesses.value.filter(d => d.status === 2).length
  const failed = dataProcesses.value.filter(d => d.status === 3).length
  return { total, success, running, failed }
})
const dataProcessChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c} 条' },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
  xAxis: { type: 'category', data: ['总加工数', '成功', '加工中', '失败'], axisLabel: { fontSize: 12 } },
  yAxis: { type: 'value', name: '数量', axisLabel: { fontSize: 12 } },
  series: [
    {
      name: '加工数量',
      type: 'bar',
      barWidth: '50%',
      data: [
        { value: dataProcessStats.value.total, itemStyle: { color: '#e6a23c' } },
        { value: dataProcessStats.value.success, itemStyle: { color: '#f0b775' } },
        { value: dataProcessStats.value.running, itemStyle: { color: '#e6a23c' } },
        { value: dataProcessStats.value.failed, itemStyle: { color: '#f56c6c' } }
      ],
      label: { show: true, position: 'top', fontSize: 12, color: '#666' }
    }
  ]
}))

// ========== 方法 ==========
const getRoleText = (role) => role === 1 ? '管理员' : '普通用户'
const toggleDropdown = () => { dropdownVisible.value = !dropdownVisible.value }

const handleNavClick = (menu) => {
  activeNav.value = menu.key
  if (menu.key === 'dashboard') switchMenu('dashboard')
  else if (menu.key === 'rpa') switchMenu('tasks')
  else if (menu.key === 'system') switchMenu('users')
}

const switchMenu = (menu) => {
  activeMenu.value = menu
  if (menu === 'notification') {
    notifPage.value = 1
    notifStatusFilter.value = ''
    loadNotificationStats()
    loadNotifications()
  }
}

const toggleSubmenu = (item) => {
  const idx = expandedSubmenus.value.indexOf(item.key)
  if (idx >= 0) expandedSubmenus.value.splice(idx, 1)
  else expandedSubmenus.value.push(item.key)
  if (item.children && item.children.length > 0) activeMenu.value = item.children[0].key
}

const goToProfile = () => { dropdownVisible.value = false; router.push('/profile') }

const handleLogout = () => {
  dropdownVisible.value = false
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('userInfo'); localStorage.removeItem('token')
    ElMessage.success('已退出登录'); router.push('/login')
  })
}

const changePeriod = (period) => { selectedPeriod.value = period }

// ========== 状态方法 ==========
const getStatusText = (s) => ({ running: '运行中', pending: '待执行', completed: '已完成', failed: '失败', active: '活跃', idle: '空闲', offline: '离线' }[s] || s)
const getStatusType = (s) => ({ running: 'warning', pending: 'info', completed: 'success', failed: 'danger' }[s] || 'info')
const getTaskStatusText = (s) => ({ 0: '待执行', 1: '执行中', 2: '成功', 3: '失败' }[s] || '未知')
const getTaskStatusType = (s) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')
const getRobotStatusText = (s) => ({ active: '活跃', idle: '空闲', offline: '离线' }[s] || s)
const getRobotStatusType = (s) => ({ active: 'success', idle: 'info', offline: 'danger' }[s] || 'info')
const getLogStatusText = (s) => ({ success: '成功', failed: '失败', running: '进行中' }[s] || s)
const getLogStatusType = (s) => ({ success: 'success', failed: 'danger', running: 'warning' }[s] || 'info')
const getPriorityText = (p) => ({ high: '高', normal: '普通', low: '低' }[p] || p)
const getPriorityType = (p) => ({ high: 'danger', normal: 'warning', low: 'info' }[p] || 'info')
const getCpuColor = (v) => v >= 80 ? '#f56c6c' : v >= 60 ? '#e6a23c' : '#67c23a'
const getMemoryColor = (v) => v >= 85 ? '#f56c6c' : v >= 70 ? '#e6a23c' : '#409eff'
const formatDateTime = (dt) => { if (!dt) return ''; const d = new Date(typeof dt === 'string' ? dt.replace(' ', 'T') : dt); return d.toLocaleString('zh-CN', { hour12: false }) }

// ========== 用户管理 ==========
const showUserModal = (user) => {
  editingUser.value = user
  if (user) Object.assign(userForm, { username: user.username, password: '', realName: user.realName || '', email: user.email || '', phone: user.phone || '', role: user.role || 0, status: user.status || 1 })
  else Object.assign(userForm, { username: '', password: '', realName: '', email: '', phone: '', role: 0, status: 1 })
  userDialogVisible.value = true
}
const saveUser = async () => {
  if (!userFormRef.value) return
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      saveLoading.value = true
      try {
        const url = editingUser.value ? `${API_BASE}/user/${editingUser.value.id}` : `${API_BASE}/user/register`
        const method = editingUser.value ? 'PUT' : 'POST'
        // 新建用户时，若未填写密码，默认使用 123456
        const body = editingUser.value 
          ? { realName: userForm.realName, email: userForm.email, phone: userForm.phone, role: userForm.role, status: userForm.status }
          : { ...userForm, password: userForm.password || '123456' }
        const res = await request(url, { method, body: JSON.stringify(body) })
        const result = await res.json()
        if (result.code === 0) { ElMessage.success(editingUser.value ? '更新成功' : '创建成功'); userDialogVisible.value = false; loadUsers() }
        else ElMessage.error(result.message || '操作失败')
      } catch { ElMessage.error('网络错误') } finally { saveLoading.value = false }
    }
  })
}
const toggleUserStatus = async (user) => {
  try {
    const res = await request(`${API_BASE}/user/${user.id}/status`, { method: 'PUT', body: JSON.stringify({ status: user.status === 1 ? 0 : 1 }) })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success(user.status === 1 ? '已禁用' : '已启用'); loadUsers() }
    else ElMessage.error(result.message || '操作失败')
  } catch { ElMessage.error('网络错误') }
}
const deleteUser = async (id) => {
  await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
  try {
    const res = await request(`${API_BASE}/user/${id}`, { method: 'DELETE' })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success('删除成功'); loadUsers() }
    else ElMessage.error(result.message || '删除失败')
  } catch { ElMessage.error('网络错误') }
}

// ========== 任务管理 ==========
const showTaskModal = (task) => {
  editingTask.value = task
  if (task) Object.assign(taskForm, { name: task.name, processId: task.processId, robotId: task.robotId, priority: task.priority || 'normal', remark: task.remark || '' })
  else Object.assign(taskForm, { name: '', processId: '', robotId: '', priority: 'normal', remark: '' })
  taskDialogVisible.value = true
}
const saveTask = async () => {
  if (!taskFormRef.value) return
  await taskFormRef.value.validate(async (valid) => {
    if (valid) {
      saveLoading.value = true
      try {
        const process = processes.value.find(p => p.id === taskForm.processId)
        const robot = robots.value.find(r => r.id === taskForm.robotId)
        const url = editingTask.value ? `${API_BASE}/task/${editingTask.value.id}` : `${API_BASE}/task`
        const method = editingTask.value ? 'PUT' : 'POST'
        const body = { name: taskForm.name, category: 'RPA任务', priority: taskForm.priority, processId: taskForm.processId, processName: process?.name, robotId: taskForm.robotId, robotName: robot?.name, inputData: taskForm.remark }
        const res = await request(url, { method, body: JSON.stringify(body) })
        const result = await res.json()
        if (result.code === 0) { ElMessage.success(editingTask.value ? '更新成功' : '创建成功'); taskDialogVisible.value = false; loadTasks() }
        else ElMessage.error(result.message || '操作失败')
      } catch { ElMessage.error('网络错误') } finally { saveLoading.value = false }
    }
  })
}
const viewTaskDetail = (task) => {
  detailTitle.value = '任务详情'
  detailContent.value = `<div class="detail-grid"><div class="detail-item"><label>任务名称</label><span>${task.name}</span></div><div class="detail-item"><label>所属流程</label><span>${task.processName || '-'}</span></div><div class="detail-item"><label>执行机器人</label><span>${task.robotName || '-'}</span></div><div class="detail-item"><label>优先级</label><span>${getPriorityText(task.priority)}</span></div><div class="detail-item"><label>状态</label><span>${getStatusText(task.status)}</span></div><div class="detail-item"><label>创建时间</label><span>${formatDateTime(task.createTime) || '-'}</span></div></div>`
  detailDialogVisible.value = true
}
const deleteTask = async (id) => {
  await ElMessageBox.confirm('确定删除该任务吗？', '提示', { type: 'warning' })
  try {
    const res = await request(`${API_BASE}/task/${id}`, { method: 'DELETE' })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success('删除成功'); loadTasks() }
    else ElMessage.error(result.message || '删除失败')
  } catch { ElMessage.error('网络错误') }
}

// ========== 机器人管理 ==========
const showRobotModal = (robot) => {
  editingRobot.value = robot
  if (robot) Object.assign(robotForm, { name: robot.name, type: robot.type || 'Desktop', ip: robot.ip || '', hostname: robot.hostname || '', port: robot.port || 8080, description: robot.description || '' })
  else Object.assign(robotForm, { name: '', type: 'Desktop', ip: '', hostname: '', port: 8080, description: '' })
  robotDialogVisible.value = true
}
const saveRobot = async () => {
  if (!robotFormRef.value) return
  await robotFormRef.value.validate(async (valid) => {
    if (valid) {
      saveLoading.value = true
      try {
        const url = editingRobot.value ? `${API_BASE}/robot/${editingRobot.value.id}` : `${API_BASE}/robot`
        const method = editingRobot.value ? 'PUT' : 'POST'
        const res = await request(url, { method, body: JSON.stringify(robotForm) })
        const result = await res.json()
        if (result.code === 0) { ElMessage.success(editingRobot.value ? '更新成功' : '注册成功'); robotDialogVisible.value = false; loadRobots() }
        else ElMessage.error(result.message || '操作失败')
      } catch { ElMessage.error('网络错误') } finally { saveLoading.value = false }
    }
  })
}
const viewRobotDetail = (robot) => {
  detailTitle.value = '机器人详情'
  detailContent.value = `<div class="detail-grid"><div class="detail-item"><label>机器人名称</label><span>${robot.name}</span></div><div class="detail-item"><label>IP地址</label><span>${robot.ip || '-'}</span></div><div class="detail-item"><label>主机名</label><span>${robot.hostname || '-'}</span></div><div class="detail-item"><label>类型</label><span>${robot.type || '-'}</span></div><div class="detail-item"><label>端口</label><span>${robot.port || 8080}</span></div><div class="detail-item"><label>状态</label><span>${getRobotStatusText(robot.status)}</span></div><div class="detail-item"><label>CPU使用率</label><span>${robot.cpuUsage || 0}%</span></div><div class="detail-item"><label>内存使用率</label><span>${robot.memoryUsage || 0}%</span></div><div class="detail-item full"><label>描述</label><span>${robot.description || '-'}</span></div></div>`
  detailDialogVisible.value = true
}
const deleteRobot = async (id) => {
  await ElMessageBox.confirm('确定删除该机器人吗？', '提示', { type: 'warning' })
  try {
    const res = await request(`${API_BASE}/robot/${id}`, { method: 'DELETE' })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success('删除成功'); loadRobots() }
    else ElMessage.error(result.message || '删除失败')
  } catch { ElMessage.error('网络错误') }
}

// ========== 流程管理 ==========
const showProcessModal = (process) => {
  editingProcess.value = process
  if (process) Object.assign(processForm, { name: process.name, code: process.code || '', version: process.version || '1.0.0', description: process.description || '', status: process.status || 'draft' })
  else Object.assign(processForm, { name: '', code: '', version: '1.0.0', description: '', status: 'draft' })
  processDialogVisible.value = true
}
const saveProcess = async () => {
  if (!processFormRef.value) return
  await processFormRef.value.validate(async (valid) => {
    if (valid) {
      saveLoading.value = true
      try {
        if (editingProcess.value) {
          const res = await request(`${API_BASE}/process/${editingProcess.value.id}`, { method: 'PUT', body: JSON.stringify(processForm) })
          const result = await res.json()
          if (result.code === 0) { ElMessage.success('更新成功'); processDialogVisible.value = false; loadProcesses() }
          else ElMessage.error(result.message || '操作失败')
        } else {
          const res = await request(`${API_BASE}/process`, { method: 'POST', body: JSON.stringify({ ...processForm, creatorId: currentUser.value.id, creatorName: currentUser.value.realName || currentUser.value.username }) })
          const result = await res.json()
          if (result.code === 0) { ElMessage.success('创建成功'); processDialogVisible.value = false; loadProcesses() }
          else ElMessage.error(result.message || '操作失败')
        }
      } catch { ElMessage.error('网络错误') } finally { saveLoading.value = false }
    }
  })
}
const viewProcessDetail = (process) => {
  detailTitle.value = '流程详情'
  detailContent.value = `<div class="detail-grid"><div class="detail-item"><label>流程名称</label><span>${process.name}</span></div><div class="detail-item"><label>流程编码</label><span>${process.code || '-'}</span></div><div class="detail-item"><label>版本</label><span>${process.version || '-'}</span></div><div class="detail-item"><label>状态</label><span>${process.status === 'active' ? '已发布' : '草稿'}</span></div><div class="detail-item"><label>创建人</label><span>${process.creatorName || '-'}</span></div><div class="detail-item"><label>创建时间</label><span>${formatDateTime(process.createTime) || '-'}</span></div><div class="detail-item full"><label>描述</label><span>${process.description || '-'}</span></div></div>`
  detailDialogVisible.value = true
}
const deleteProcess = async (id) => {
  await ElMessageBox.confirm('确定删除该流程吗？', '提示', { type: 'warning' })
  try {
    const res = await request(`${API_BASE}/process/${id}`, { method: 'DELETE' })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success('删除成功'); loadProcesses() }
    else ElMessage.error(result.message || '删除失败')
  } catch { ElMessage.error('网络错误') }
}

// ========== 执行日志 ==========
const viewLogDetail = (log) => {
  detailTitle.value = '执行日志详情'
  detailContent.value = `<div class="detail-grid"><div class="detail-item"><label>任务名称</label><span>${log.taskName || '-'}</span></div><div class="detail-item"><label>机器人</label><span>${log.robotName || '-'}</span></div><div class="detail-item"><label>操作</label><span>${log.action || '-'}</span></div><div class="detail-item"><label>状态</label><span>${getLogStatusText(log.status)}</span></div><div class="detail-item"><label>开始时间</label><span>${formatDateTime(log.startTime) || '-'}</span></div><div class="detail-item"><label>结束时间</label><span>${formatDateTime(log.endTime) || '-'}</span></div><div class="detail-item"><label>耗时</label><span>${log.duration || '-'}</span></div><div class="detail-item full"><label>信息</label><span>${log.message || '-'}</span></div></div>`
  detailDialogVisible.value = true
}

// ========== 数据管理方法 ==========
const editingDataCollect = ref(null)
const editingDataParse = ref(null)
const editingDataProcess = ref(null)
const editingDataQuery = ref(null)
const dataCollectDialogVisible = ref(false)
const dataParseDialogVisible = ref(false)
const dataProcessDialogVisible = ref(false)
const dataQueryDialogVisible = ref(false)

const dataCollectForm = reactive({ name: '', sourceUrl: '', sourceType: 'web', selectorRules: '', cronExpression: '' })
const dataParseForm = reactive({ name: '', collectId: '', parseType: 'json', parseRules: '', outputFormat: 'json' })
const dataProcessForm = reactive({ name: '', sourceIds: '', processType: 'transform', processRules: '', outputTable: '' })
const dataQueryForm = reactive({ name: '', sourceTable: 'collected_data', queryCondition: '', queryColumns: '' })

const showDataCollectModal = (item) => {
  editingDataCollect.value = item
  if (item) Object.assign(dataCollectForm, { name: item.name, sourceUrl: item.sourceUrl || '', sourceType: item.sourceType || 'web', selectorRules: item.selectorRules || '', cronExpression: item.cronExpression || '' })
  else Object.assign(dataCollectForm, { name: '', sourceUrl: '', sourceType: 'web', selectorRules: '', cronExpression: '' })
  dataCollectDialogVisible.value = true
}
const saveDataCollect = async () => {
  if (!dataCollectForm.name) { ElMessage.warning('请输入采集名称'); return }
  saveLoading.value = true
  try {
    const url = editingDataCollect.value ? `${API_BASE}/dataCollect/${editingDataCollect.value.id}` : `${API_BASE}/dataCollect`
    const method = editingDataCollect.value ? 'PUT' : 'POST'
    const res = await request(url, { method, body: JSON.stringify(dataCollectForm) })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success(editingDataCollect.value ? '更新成功' : '创建成功'); dataCollectDialogVisible.value = false; loadDataCollects() }
    else ElMessage.error(result.message || '操作失败')
  } catch { ElMessage.error('网络错误') } finally { saveLoading.value = false }
}
const runDataCollect = async (item) => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataCollect/${item.id}/execute`, { method: 'POST' })
    const result = await res.json()
    if (result.success) { ElMessage.success(`采集完成，共获取 ${result.count || 0} 条数据`) }
    else ElMessage.error(result.message || '采集失败')
  } catch { ElMessage.error('网络错误') } finally { loading.value = false }
}
const deleteDataCollect = async (id) => {
  await ElMessageBox.confirm('确定删除该采集配置吗？', '提示', { type: 'warning' })
  try {
    const res = await request(`${API_BASE}/dataCollect/${id}`, { method: 'DELETE' })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success('删除成功'); loadDataCollects() }
    else ElMessage.error(result.message || '删除失败')
  } catch { ElMessage.error('网络错误') }
}
const viewCollectData = async (item) => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataCollect/${item.id}/data`)
    const result = await res.json()
    if (result.code === 0) {
      detailTitle.value = `采集数据 - ${item.name} (${result.count || 0}条)`
      if (result.data && result.data.length > 0) {
        detailContent.value = `<pre style="max-height:500px;overflow:auto;font-size:12px;">${JSON.stringify(result.data, null, 2)}</pre>`
      } else {
        detailContent.value = '<div style="padding:20px;color:#999;">暂无采集数据，请先执行采集任务</div>'
      }
      detailDialogVisible.value = true
    } else {
      ElMessage.error(result.message || '获取数据失败')
    }
  } catch { ElMessage.error('网络错误') } finally { loading.value = false }
}

const showDataParseModal = (item) => {
  editingDataParse.value = item
  if (item) Object.assign(dataParseForm, { name: item.name, collectId: item.collectId || '', parseType: item.parseType || 'json', parseRules: item.parseRules || '', outputFormat: item.outputFormat || 'json' })
  else Object.assign(dataParseForm, { name: '', collectId: '', parseType: 'json', parseRules: '', outputFormat: 'json' })
  dataParseDialogVisible.value = true
}
const saveDataParse = async () => {
  if (!dataParseForm.name) { ElMessage.warning('请输入解析名称'); return }
  saveLoading.value = true
  try {
    const url = editingDataParse.value ? `${API_BASE}/dataParse/${editingDataParse.value.id}` : `${API_BASE}/dataParse`
    const method = editingDataParse.value ? 'PUT' : 'POST'
    const res = await request(url, { method, body: JSON.stringify(dataParseForm) })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success(editingDataParse.value ? '更新成功' : '创建成功'); dataParseDialogVisible.value = false; loadDataParses() }
    else ElMessage.error(result.message || '操作失败')
  } catch { ElMessage.error('网络错误') } finally { saveLoading.value = false }
}
const runDataParse = async (item) => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataParse/${item.id}/execute`, { method: 'POST' })
    const result = await res.json()
    if (result.success) { ElMessage.success(`解析完成，成功 ${result.successCount || 0}} 条，失败 ${result.failCount || 0} 条`) }
    else ElMessage.error(result.message || '解析失败')
  } catch { ElMessage.error('网络错误') } finally { loading.value = false }
}
const deleteDataParse = async (id) => {
  await ElMessageBox.confirm('确定删除该解析配置吗？', '提示', { type: 'warning' })
  try {
    const res = await request(`${API_BASE}/dataParse/${id}`, { method: 'DELETE' })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success('删除成功'); loadDataParses() }
    else ElMessage.error(result.message || '删除失败')
  } catch { ElMessage.error('网络错误') }
}

const showDataProcessModal = (item) => {
  editingDataProcess.value = item
  if (item) Object.assign(dataProcessForm, { name: item.name, sourceIds: item.sourceIds || '', processType: item.processType || 'transform', processRules: item.processRules || '', outputTable: item.outputTable || '' })
  else Object.assign(dataProcessForm, { name: '', sourceIds: '', processType: 'transform', processRules: '', outputTable: '' })
  dataProcessDialogVisible.value = true
}
const saveDataProcess = async () => {
  if (!dataProcessForm.name) { ElMessage.warning('请输入加工名称'); return }
  saveLoading.value = true
  try {
    const url = editingDataProcess.value ? `${API_BASE}/dataProcess/${editingDataProcess.value.id}` : `${API_BASE}/dataProcess`
    const method = editingDataProcess.value ? 'PUT' : 'POST'
    const res = await request(url, { method, body: JSON.stringify(dataProcessForm) })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success(editingDataProcess.value ? '更新成功' : '创建成功'); dataProcessDialogVisible.value = false; loadDataProcesses() }
    else ElMessage.error(result.message || '操作失败')
  } catch { ElMessage.error('网络错误') } finally { saveLoading.value = false }
}
const runDataProcess = async (item) => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataProcess/${item.id}/execute`, { method: 'POST' })
    const result = await res.json()
    if (result.success) { ElMessage.success(`加工完成，处理 ${result.count || 0} 条数据`) }
    else ElMessage.error(result.message || '加工失败')
  } catch { ElMessage.error('网络错误') } finally { loading.value = false }
}
const deleteDataProcess = async (id) => {
  await ElMessageBox.confirm('确定删除该加工配置吗？', '提示', { type: 'warning' })
  try {
    const res = await request(`${API_BASE}/dataProcess/${id}`, { method: 'DELETE' })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success('删除成功'); loadDataProcesses() }
    else ElMessage.error(result.message || '删除失败')
  } catch { ElMessage.error('网络错误') }
}

const showDataQueryModal = (item) => {
  editingDataQuery.value = item
  if (item) Object.assign(dataQueryForm, { name: item.name, sourceTable: item.sourceTable || 'collected_data', queryCondition: item.queryCondition || '', queryColumns: item.queryColumns || '' })
  else Object.assign(dataQueryForm, { name: '', sourceTable: 'collected_data', queryCondition: '', queryColumns: '' })
  dataQueryDialogVisible.value = true
}
const saveDataQuery = async () => {
  if (!dataQueryForm.name) { ElMessage.warning('请输入查询名称'); return }
  saveLoading.value = true
  try {
    const url = editingDataQuery.value ? `${API_BASE}/dataQuery/${editingDataQuery.value.id}` : `${API_BASE}/dataQuery`
    const method = editingDataQuery.value ? 'PUT' : 'POST'
    const res = await request(url, { method, body: JSON.stringify(dataQueryForm) })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success(editingDataQuery.value ? '更新成功' : '创建成功'); dataQueryDialogVisible.value = false; loadDataQueries() }
    else ElMessage.error(result.message || '操作失败')
  } catch { ElMessage.error('网络错误') } finally { saveLoading.value = false }
}
const runDataQuery = async (item) => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataQuery/${item.id}/execute`, { method: 'POST' })
    const result = await res.json()
    if (result.success) { 
      if (result.data && result.data.length > 0) {
        detailTitle.value = '查询结果'
        detailContent.value = `<pre style="max-height:400px;overflow:auto;">${JSON.stringify(result.data, null, 2)}</pre>`
        detailDialogVisible.value = true
      } else {
        ElMessage.success(`查询完成，共 ${result.count || 0} 条结果`)
      }
    }
    else ElMessage.error(result.message || '查询失败')
  } catch { ElMessage.error('网络错误') } finally { loading.value = false }
}
const deleteDataQuery = async (id) => {
  await ElMessageBox.confirm('确定删除该查询配置吗？', '提示', { type: 'warning' })
  try {
    const res = await request(`${API_BASE}/dataQuery/${id}`, { method: 'DELETE' })
    const result = await res.json()
    if (result.code === 0) { ElMessage.success('删除成功'); loadDataQueries() }
    else ElMessage.error(result.message || '删除失败')
  } catch { ElMessage.error('网络错误') }
}

// ========== 数据加载 ==========
const loadUserFromStorage = () => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) try { const user = JSON.parse(userStr); currentUser.value = { ...currentUser.value, ...user } } catch {}
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/user`)
    const result = await res.json()
    if (result.code === 0) users.value = result.data || []
  } catch { users.value = [{ id: 1, username: 'admin', realName: '系统管理员', email: 'admin@rpa.com', phone: '13800138001', role: 1, status: 1 }] }
  finally { loading.value = false }
}

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/task`)
    const result = await res.json()
    if (result.code === 0) tasks.value = result.data || []
  } catch { tasks.value = [] }
  finally { loading.value = false }
  updateTaskStatus()
}

const loadRobots = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/robot`)
    const result = await res.json()
    if (result.code === 0) robots.value = result.data || []
  } catch { robots.value = [] }
  finally { loading.value = false }
}

const loadProcesses = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/process`)
    const result = await res.json()
    if (result.code === 0) processes.value = result.data || []
  } catch { processes.value = [] }
  finally { loading.value = false }
}

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/log`)
    const result = await res.json()
    if (result.code === 0) logs.value = result.data || []
  } catch { logs.value = [] }
  finally { loading.value = false }
}

const loadDataCollects = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataCollect`)
    const result = await res.json()
    if (result.code === 0) {
      dataCollects.value = result.data || []
    }
  } catch {
    // 后端未响应时使用模拟数据
    dataCollects.value = [
      { id: 1, name: '企业基本信息采集', sourceUrl: 'https://www.example.com/tax', sourceType: '网页', status: 1, dataCount: 1520, lastCollectTime: new Date().toISOString() },
      { id: 2, name: '税务申报数据采集', sourceUrl: 'https://www.example.com/report', sourceType: 'API', status: 1, dataCount: 856, lastCollectTime: new Date().toISOString() },
      { id: 3, name: '发票信息采集', sourceUrl: 'https://www.example.com/invoice', sourceType: '数据库', status: 2, dataCount: 2340, lastCollectTime: new Date().toISOString() },
      { id: 4, name: '财务报表采集', sourceUrl: 'https://www.example.com/finance', sourceType: '网页', status: 1, dataCount: 1890, lastCollectTime: new Date().toISOString() }
    ]
  } finally { loading.value = false }
}

const loadDataParses = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataParse`)
    const result = await res.json()
    if (result.code === 0) {
      dataParses.value = result.data || []
    }
  } catch {
    // 后端未响应时使用模拟数据
    dataParses.value = [
      { id: 1, name: 'JSON解析', parseType: '结构化', parseRules: 'JSON格式解析', status: 1, successCount: 4560, failCount: 120 },
      { id: 2, name: 'HTML解析', parseType: '非结构化', parseRules: '网页内容提取', status: 1, successCount: 3200, failCount: 85 },
      { id: 3, name: 'PDF解析', parseType: '文档', parseRules: 'PDF文本提取', status: 2, successCount: 1890, failCount: 230 },
      { id: 4, name: 'Excel解析', parseType: '表格', parseRules: 'Excel数据读取', status: 1, successCount: 2100, failCount: 45 }
    ]
  } finally { loading.value = false }
}

const loadDataProcesses = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataProcess`)
    const result = await res.json()
    if (result.code === 0) {
      dataProcesses.value = result.data || []
    }
  } catch {
    // 后端未响应时使用模拟数据
    dataProcesses.value = [
      { id: 1, name: '数据清洗', processType: '清洗', processRules: '去除重复数据', status: 1, processedCount: 12500 },
      { id: 2, name: '数据转换', processType: '转换', processRules: '格式标准化', status: 1, processedCount: 8900 },
      { id: 3, name: '数据补全', processType: '补全', processRules: '缺失值填充', status: 2, processedCount: 5600 },
      { id: 4, name: '数据校验', processType: '校验', processRules: '数据一致性检查', status: 1, processedCount: 9800 }
    ]
  } finally { loading.value = false }
}

const loadDataQueries = async () => {
  loading.value = true
  try {
    const res = await request(`${API_BASE}/dataQuery`)
    const result = await res.json()
    if (result.code === 0) dataQueries.value = result.data || []
  } catch { dataQueries.value = [] }
  finally { loading.value = false }
}

const updateTaskStatus = () => {
  taskStatus.value = {
    running: tasks.value.filter(t => t.status === 1).length,
    pending: tasks.value.filter(t => t.status === 0).length,
    completed: tasks.value.filter(t => t.status === 2).length,
    failed: tasks.value.filter(t => t.status === 3).length
  }
  stats.value = {
    tasks: tasks.value.length,
    robots: robots.value.length,
    processes: processes.value.length,
    logs: logs.value.length,
    dataCollects: dataCollects.value.length,
    dataParses: dataParses.value.length,
    dataProcesses: dataProcesses.value.length,
    dataQueries: dataQueries.value.length
  }
}

const loadAllData = async () => {
  await Promise.all([loadUsers(), loadTasks(), loadRobots(), loadProcesses(), loadLogs(), loadDataCollects(), loadDataParses(), loadDataProcesses(), loadDataQueries()])
  updateTaskStatus()
}

onMounted(() => { loadUserFromStorage(); loadAllData() })
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #f5f7fa; }

.app-layout { display: flex; flex-direction: column; min-height: 100vh; }

/* 顶部导航 */
.top-navbar { background: #fff; height: 60px; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; position: sticky; top: 0; z-index: 100; box-shadow: 0 1px 4px rgba(0,0,0,.08); border-bottom: 1px solid #e8e8e8; }
.logo-area { display: flex; align-items: center; gap: 12px; }
.logo-icon { width: 36px; height: 36px; background: linear-gradient(135deg, #1890ff, #0050b3); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; }
.system-name { font-size: 17px; font-weight: 600; color: #1a1a1a; letter-spacing: .5px; }

.main-nav { display: flex; gap: 4px; }
.nav-item { display: flex; align-items: center; gap: 6px; padding: 8px 18px; border-radius: 6px; color: #595961; cursor: pointer; transition: all .2s; font-size: 14px; font-weight: 500; }
.nav-item:hover { background: #f5f7fa; color: #1890ff; }
.nav-item.active { background: #e6f4ff; color: #1890ff; }
.nav-icon { display: flex; align-items: center; }

.user-area { position: relative; }
.user-dropdown { display: flex; align-items: center; gap: 10px; padding: 6px 12px; border-radius: 8px; cursor: pointer; transition: background .2s; }
.user-dropdown:hover { background: #f5f7fa; }
.avatar-circle { width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(135deg, #1890ff, #0050b3); display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; font-size: 14px; }
.user-info { display: flex; flex-direction: column; }
.user-name { font-size: 13px; font-weight: 500; color: #262626; }
.user-role { font-size: 11px; color: #8c8c8c; }
.dropdown-arrow { color: #8c8c8c; }

.dropdown-menu { position: absolute; top: calc(100% + 8px); right: 0; background: #fff; border-radius: 12px; box-shadow: 0 6px 30px rgba(0,0,0,.12); min-width: 180px; overflow: hidden; z-index: 1000; border: 1px solid #f0f0f0; }
.dropdown-item { display: flex; align-items: center; gap: 10px; padding: 12px 16px; color: #262626; cursor: pointer; transition: background .2s; font-size: 14px; }
.dropdown-item:hover { background: #f5f7fa; }
.dropdown-item.danger { color: #ff4d4f; }
.dropdown-divider { height: 1px; background: #f0f0f0; }

/* 主容器 */
.main-container { display: flex; flex: 1; }

/* 侧边栏 */
.sidebar { width: 220px; background: #fff; border-right: 1px solid #e8e8e8; position: relative; transition: width .3s; overflow: hidden; }
.sidebar.collapsed { width: 64px; }
.sidebar-toggle { position: absolute; right: -12px; top: 20px; width: 24px; height: 24px; background: #fff; border: 1px solid #e8e8e8; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; z-index: 10; color: #8c8c8c; transition: all .2s; }
.sidebar-toggle:hover { background: #f5f7fa; color: #1890ff; }
.sidebar-header { padding: 16px 20px 8px; border-bottom: 1px solid #f0f0f0; }
.sidebar-title { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.sidebar-menu { padding: 12px 8px; }
.menu-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-radius: 8px; color: #595961; cursor: pointer; transition: all .2s; font-size: 14px; white-space: nowrap; overflow: hidden; }
.menu-item:hover { background: #f5f7fa; color: #1890ff; }
.menu-item.active { background: #e6f4ff; color: #1890ff; font-weight: 500; }
.menu-icon { display: flex; align-items: center; flex-shrink: 0; }
.submenu-arrow { margin-left: auto; }
.submenu { padding-left: 36px; }
.submenu-item { font-size: 13px; padding: 8px 14px; }

/* 内容区 */
.content-area { flex: 1; padding: 24px; overflow-y: auto; }

/* 首页 */
.dashboard-content { max-width: 1400px; margin: 0 auto; }
.content-header { margin-bottom: 24px; }
.content-header h2 { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.header-tip { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.04); border: 1px solid #f0f0f0; transition: all .2s; cursor: pointer; }
.stat-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.08); transform: translateY(-2px); }
.stat-card.clickable { cursor: pointer; }
.stat-arrow { opacity: 0; transition: opacity .2s; color: #8c8c8c; }
.stat-card:hover .stat-arrow { opacity: 1; }
.stat-icon-box { width: 52px; height: 52px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-icon-box.blue { background: linear-gradient(135deg, #e6f4ff, #bae0ff); color: #1890ff; }
.stat-icon-box.green { background: linear-gradient(135deg, #f6ffed, #d9f7be); color: #52c41a; }
.stat-icon-box.purple { background: linear-gradient(135deg, #f9f0ff, #d3adf7); color: #722ed1; }
.stat-icon-box.orange { background: linear-gradient(135deg, #fff7e6, #ffd591); color: #fa8c16; }
.stat-info { flex: 1; }
.stat-value { font-size: 26px; font-weight: 700; color: #1a1a1a; line-height: 1.2; }
.stat-label { font-size: 13px; color: #8c8c8c; margin-top: 2px; }
.stat-change { font-size: 12px; color: #52c41a; margin-top: 4px; }

.charts-row { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 24px; }
.chart-card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,.04); border: 1px solid #f0f0f0; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.chart-header h3 { font-size: 15px; font-weight: 600; color: #1a1a1a; }
.period-selector { display: flex; gap: 4px; }
.period-btn { padding: 4px 12px; border-radius: 6px; font-size: 12px; color: #8c8c8c; cursor: pointer; transition: all .2s; border: none; background: transparent; }
.period-btn:hover { color: #1890ff; background: #f5f7fa; }
.period-btn.active { color: #fff; background: #1890ff; }
.chart-body { width: 100%; }

.status-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.status-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.04); border: 1px solid #f0f0f0; }
.status-indicator { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.status-indicator.running { background: #faad14; box-shadow: 0 0 8px rgba(250,173,20,.4); }
.status-indicator.pending { background: #8c8c8c; }
.status-indicator.completed { background: #52c41a; box-shadow: 0 0 8px rgba(82,196,26,.4); }
.status-indicator.failed { background: #ff4d4f; box-shadow: 0 0 8px rgba(255,77,79,.4); }
.status-info { flex: 1; }
.status-value { font-size: 28px; font-weight: 700; color: #1a1a1a; line-height: 1.2; }
.status-label { font-size: 13px; color: #8c8c8c; }

/* 管理内容 */
.management-content { max-width: 1400px; margin: 0 auto; }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; flex: 1; max-width: 320px; }
.search-box input { border: none; outline: none; font-size: 14px; flex: 1; background: transparent; color: #262626; }
.search-box input::placeholder { color: #bfbfbf; }
.toolbar .el-button { display: flex; align-items: center; gap: 6px; }
.data-table { border-radius: 8px; overflow: hidden; }
.data-table :deep(.el-table__header th) { background: #fafafa !important; }

/* 分页 */
.custom-pagination { display: flex; justify-content: flex-end; margin-top: 16px; padding: 12px 0; }

/* 统计图表 */
.stats-chart-row { margin-bottom: 20px; display: flex; gap: 20px; }
.stats-chart-row .chart-card { flex: 1; background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.04); border: 1px solid #f0f0f0; }
.stats-chart-row .chart-header { margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center; }
.stats-chart-row .chart-header h3 { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.chart-stats { display: flex; gap: 16px; }
.chart-stat-item { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.chart-stat-dot { width: 8px; height: 8px; border-radius: 50%; }
.chart-stat-dot.total { background: #409eff; }
.chart-stat-dot.success { background: #67c23a; }
.chart-stat-dot.running { background: #e6a23c; }
.chart-stat-dot.failed { background: #f56c6c; }

/* 查询表单 */
.query-form-card { background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.04); border: 1px solid #f0f0f0; }
.query-form { display: flex; flex-wrap: wrap; gap: 16px; }

/* Tabs */
.custom-tabs :deep(.el-tabs__header) { margin-bottom: 20px; }
.custom-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; }
.custom-tabs :deep(.el-tabs__item) { font-size: 14px; color: #8c8c8c; }
.custom-tabs :deep(.el-tabs__item.is-active) { color: #1890ff; font-weight: 600; }
.custom-tabs :deep(.el-tabs__active-bar) { background-color: #1890ff; }

/* 详情弹窗 */
.detail-content { padding: 8px 0; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-item.full { grid-column: 1 / -1; }
.detail-item label { font-size: 12px; color: #8c8c8c; }
.detail-item span { font-size: 14px; color: #262626; font-weight: 500; }

/* 弹窗 */
.custom-dialog :deep(.el-dialog) { border-radius: 12px; overflow: hidden; }
.custom-dialog :deep(.el-dialog__header) { background: #fff; border-bottom: 1px solid #f0f0f0; padding: 16px 24px; margin: 0; }
.custom-dialog :deep(.el-dialog__title) { font-size: 16px; font-weight: 600; color: #1a1a1a; }
.custom-dialog :deep(.el-dialog__body) { padding: 24px; }
.custom-dialog :deep(.el-dialog__footer) { border-top: 1px solid #f0f0f0; padding: 12px 24px; }

/* 动画 */
.dropdown-enter-active, .dropdown-leave-active { transition: all .2s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-8px); }
.submenu-enter-active, .submenu-leave-active { transition: all .2s ease; }
.submenu-enter-from, .submenu-leave-to { opacity: 0; transform: translateY(-8px); }

.overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 99; }

/* 通知管理 - 左右布局 */
.notification-layout { display: flex; gap: 20px; max-width: 1400px; }
.notification-sidebar { width: 240px; flex-shrink: 0; background: #fff; border-radius: 12px; padding: 16px; border: 1px solid #f0f0f0; }
.notif-sidebar-header { padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; margin-bottom: 12px; }
.sidebar-title { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.notif-type-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.notif-type-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px; border: 2px solid transparent; cursor: pointer; transition: all .25s; background: #fafafa; }
.notif-type-item:hover { border-color: #e6e6e6; }
.notif-type-item.active { border-color: #1890ff; background: #f0f7ff; }
.notif-type-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.collect-icon { background: linear-gradient(135deg, #e6f4ff, #bae0ff); color: #1890ff; }
.temp-icon { background: linear-gradient(135deg, #fff7e6, #ffd591); color: #fa8c16; }
.user-icon { background: linear-gradient(135deg, #f6ffed, #d9f7be); color: #52c41a; }
.notif-type-info { flex: 1; }
.notif-type-name { font-size: 13px; color: #262626; font-weight: 500; }
.notif-type-count { font-size: 12px; color: #8c8c8c; margin-top: 2px; }
.notif-unread-badge { background: #ff4d4f; color: #fff; font-size: 11px; font-weight: 600; padding: 2px 6px; border-radius: 10px; min-width: 18px; text-align: center; }
.notif-filter-section { margin-bottom: 16px; }
.filter-label { font-size: 13px; color: #8c8c8c; margin-bottom: 8px; }
.notif-actions { display: flex; flex-direction: column; gap: 8px; }
.notif-actions .el-button { width: 100%; }

.notification-main { flex: 1; min-width: 0; }
.notification-overview { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 16px; border: 1px solid #f0f0f0; }
.notif-chart-wrapper { }
.notif-chart-title { font-size: 14px; font-weight: 600; color: #1a1a1a; margin-bottom: 8px; }
.notif-echarts { width: 100%; height: 200px; }
.notif-chart-empty { height: 200px; display: flex; align-items: center; justify-content: center; color: #8c8c8c; font-size: 14px; }
.notif-list-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding: 0 4px; }
.notif-list-header span:first-child { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.notif-total { font-size: 13px; color: #8c8c8c; font-weight: 400; }
.notif-table { margin-bottom: 12px; }
.notif-detail-content { font-size: 14px; color: #262626; line-height: 1.8; }
.notif-detail-content p { margin: 8px 0; }

/* 响应式 */
@media (max-width: 1200px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } .charts-row { grid-template-columns: 1fr; } .status-row { grid-template-columns: repeat(2, 1fr); } .notification-layout { flex-direction: column; } .notification-sidebar { width: 100%; } }
@media (max-width: 768px) { .main-nav { display: none; } .stats-grid { grid-template-columns: 1fr; } .status-row { grid-template-columns: repeat(2, 1fr); } }
</style>
