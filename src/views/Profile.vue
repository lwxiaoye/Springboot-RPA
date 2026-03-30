<template>
  <div class="profile-page">
    <!-- 顶部导航 -->
    <header class="top-navbar">
      <div class="navbar-brand" @click="goToMain">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
          </svg>
        </div>
        <span class="brand-name">RPA智能运营平台</span>
      </div>

      <nav class="navbar-menu">
        <div class="menu-item" :class="{ active: currentNav === 'dashboard' }" @click="goToMain">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>
          <span>首页</span>
        </div>
        <div class="menu-item" :class="{ active: currentNav === 'rpa' }" @click="goToMain">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7s2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z"/></svg>
          <span>RPA运营</span>
        </div>
      </nav>

      <div class="navbar-user">
        <div class="user-info" @click="toggleUserMenu">
          <div class="user-avatar">{{ userInitials }}</div>
          <div class="user-detail">
            <span class="user-name">{{ currentUser.realName || currentUser.username }}</span>
            <span class="user-role">{{ getRoleText(currentUser.role) }}</span>
          </div>
          <svg class="arrow-icon" :class="{ rotated: userMenuVisible }" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M7 10l5 5 5-5z"/>
          </svg>
        </div>
        <Transition name="dropdown">
          <div class="user-dropdown" v-if="userMenuVisible" @click.stop>
            <div class="dropdown-item" @click="goToProfile">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              个人中心
            </div>
            <div class="dropdown-divider"></div>
            <div class="dropdown-item danger" @click="handleLogout">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
              退出登录
            </div>
          </div>
        </Transition>
      </div>
      <div class="dropdown-overlay" v-if="userMenuVisible" @click="userMenuVisible = false"></div>
    </header>

    <!-- 主内容 -->
    <div class="main-container">
      <!-- 左侧导航 -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <div class="sidebar-title">系统管理</div>
        </div>
        <nav class="sidebar-nav">
          <div class="nav-item" :class="{ active: activeMenu === 'profile' }" @click="activeMenu = 'profile'">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <span>个人信息</span>
          </div>
          <div class="nav-item" :class="{ active: activeMenu === 'user' }" @click="activeMenu = 'user'" v-if="currentUser.role === 1">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
            <span>用户管理</span>
          </div>
          <div class="nav-item" :class="{ active: activeMenu === 'role' }" @click="activeMenu = 'role'" v-if="currentUser.role === 1">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12.65 10A5.99 5.99 0 0 0 7 6c-3.31 0-6 2.69-6 6s2.69 6 6 6a5.99 5.99 0 0 0 5.65-4H17v4h4v-4h2v-4H12.65zM7 14c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>
            <span>角色管理</span>
          </div>
          <div class="nav-item" :class="{ active: activeMenu === 'resource' }" @click="activeMenu = 'resource'" v-if="currentUser.role === 1">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-6 10H6v-2h10v2zm4-4H6v-2h12v2z"/></svg>
            <span>资源管理</span>
          </div>
        </nav>
      </aside>

      <!-- 内容区 -->
      <main class="content-area">
        <!-- 个人信息 -->
        <div v-if="activeMenu === 'profile'" class="content-panel">
          <div class="panel-header">
            <div class="header-info">
              <h2>个人中心</h2>
              <p>管理您的个人账户信息和安全设置</p>
            </div>
            <div class="header-actions">
              <el-button type="danger" @click="handleLogout" class="logout-btn">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
                退出登录
              </el-button>
            </div>
          </div>

          <div class="profile-content">
            <!-- 左侧信息卡片 -->
            <div class="profile-left">
              <!-- 用户头像卡片 -->
              <div class="profile-card avatar-card">
                <div class="avatar-header">
                  <span>我的头像</span>
                </div>
                <div class="avatar-body">
                  <div class="avatar-display">
                    <div class="avatar-large" :class="{ 'has-avatar': userAvatarUrl }" :style="userAvatarUrl ? { backgroundImage: `url(${userAvatarUrl})` } : {}">
                      <span v-if="!userAvatarUrl">{{ userInitials }}</span>
                      <img v-else :src="userAvatarUrl" :alt="currentUser.realName || currentUser.username" @error="handleAvatarError" style="display: none;" />
                    </div>
                    <label class="upload-btn">
                      <input type="file" accept="image/*" @change="handleAvatarUpload" style="display: none;" />
                      <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z"/></svg>
                      <span>更换头像</span>
                    </label>
                  </div>
                </div>
              </div>

              <!-- 基本信息卡片 -->
              <div class="profile-card main-card">
                <div class="card-header">
                  <div class="header-title">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                    <span>基本信息</span>
                  </div>
                  <el-button type="primary" @click="editProfileVisible = true" class="edit-btn">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                    编辑
                  </el-button>
                </div>
                <div class="card-body">
                  <div class="info-list">
                    <div class="info-item-row">
                      <div class="info-label">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                        <span>用户名</span>
                      </div>
                      <div class="info-value">{{ currentUser.username }}</div>
                    </div>
                    <div class="info-item-row">
                      <div class="info-label">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                        <span>真实姓名</span>
                      </div>
                      <div class="info-value">{{ currentUser.realName || '-' }}</div>
                    </div>
                    <div class="info-item-row">
                      <div class="info-label">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                        <span>邮箱</span>
                      </div>
                      <div class="info-value">{{ currentUser.email || '-' }}</div>
                    </div>
                    <div class="info-item-row">
                      <div class="info-label">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M17.71 7.71L12 2h-1v7.59L6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 11 14.41V22h1l5.71-5.71-4.3-4.29 4.3-4.29zM13 5.83l1.88 1.88L13 9.59V5.83zm1.88 10.46L13 18.17v-3.76l1.88 1.88z"/></svg>
                        <span>手机号</span>
                      </div>
                      <div class="info-value">{{ currentUser.phone || '-' }}</div>
                    </div>
                    <div class="info-item-row">
                      <div class="info-label">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12.65 10A5.99 5.99 0 0 0 7 6c-3.31 0-6 2.69-6 6s2.69 6 6 6a5.99 5.99 0 0 0 5.65-4H17v4h4v-4h2v-4H12.65zM7 14c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>
                        <span>角色</span>
                      </div>
                      <div class="info-value">
                        <el-tag size="small" :type="currentUser.role === 1 ? 'danger' : 'default'" class="role-tag">
                          {{ getRoleText(currentUser.role) }}
                        </el-tag>
                      </div>
                    </div>
                    <div class="info-item-row">
                      <div class="info-label">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>
                        <span>注册时间</span>
                      </div>
                      <div class="info-value">{{ formatDate(currentUser.createTime) }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧安全卡片 -->
            <div class="profile-right">
              <div class="profile-card security-card">
                <div class="card-header">
                  <div class="header-title">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                    <span>账户安全</span>
                  </div>
                </div>
                <div class="card-body">
                  <div class="security-items">
                    <div class="security-item" @click="passwordVisible = true">
                      <div class="security-icon">
                        <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                      </div>
                      <div class="security-content">
                        <div class="security-title">登录密码</div>
                        <div class="security-desc">定期更换密码可以保护账户安全</div>
                      </div>
                      <div class="security-action">
                        <span>修改</span>
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/></svg>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 账户统计 -->
              <div class="profile-card stats-card">
                <div class="card-header">
                  <div class="header-title">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/></svg>
                    <span>账户统计</span>
                  </div>
                </div>
                <div class="card-body">
                  <div class="stats-grid">
                    <div class="stat-box">
                      <div class="stat-number">{{ accountAge }}</div>
                      <div class="stat-label">账户天数</div>
                    </div>
                    <div class="stat-box">
                      <div class="stat-number">{{ lastLoginDays }}</div>
                      <div class="stat-label">最后登录</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 用户管理 -->
        <div v-if="activeMenu === 'user' && currentUser.role === 1" class="content-panel">
          <div class="panel-header">
            <div class="header-info">
              <h2>用户管理</h2>
              <p>管理系统中的用户账户和权限分配</p>
            </div>
            <el-button type="primary" @click="openUserDialog(null)">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
              新建用户
            </el-button>
          </div>

          <div class="toolbar">
            <el-input v-model="userSearch" placeholder="搜索用户名/姓名..." clearable style="width: 280px;">
              <template #prefix>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
              </template>
            </el-input>
          </div>

          <el-table :data="filteredUsers" stripe v-loading="loading" class="data-table">
            <el-table-column type="index" label="序号" width="70" align="center" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="realName" label="姓名" min-width="100" />
            <el-table-column prop="email" label="邮箱" min-width="160" />
            <el-table-column prop="phone" label="手机号" min-width="120" />
            <el-table-column prop="role" label="角色" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.role === 1 ? 'danger' : 'default'">{{ getRoleText(row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center">
              <template #default="{ row }">
                <el-button 
                  v-if="row.status === 1" 
                  link 
                  type="warning" 
                  @click="toggleUserStatus(row)"
                >
                  禁用
                </el-button>
                <el-button 
                  v-else 
                  link 
                  type="success" 
                  @click="toggleUserStatus(row)"
                >
                  启用
                </el-button>
                <el-button link type="primary" @click="openUserDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteUser(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 角色管理 -->
        <div v-if="activeMenu === 'role' && currentUser.role === 1" class="content-panel">
          <div class="panel-header">
            <div class="header-info">
              <h2>角色管理</h2>
              <p>管理系统角色和权限配置</p>
            </div>
            <el-button type="primary" @click="openRoleDialog(null)">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
              新建角色
            </el-button>
          </div>

          <div class="role-grid">
            <div class="role-card" v-for="role in roles" :key="role.id">
              <div class="role-header">
                <div class="role-icon" :class="getRoleClassName(role.code)">
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M12.65 10A5.99 5.99 0 0 0 7 6c-3.31 0-6 2.69-6 6s2.69 6 6 6a5.99 5.99 0 0 0 5.65-4H17v4h4v-4h2v-4H12.65zM7 14c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>
                </div>
                <div class="role-info">
                  <h3>{{ role.name }}</h3>
                  <p>{{ role.description || '暂无描述' }}</p>
                </div>
              </div>
              <div class="role-stats">
                <div class="stat-item">
                  <span class="stat-value">{{ role.userCount || 0 }}</span>
                  <span class="stat-label">用户</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value">{{ role.permissionCount || 0 }}</span>
                  <span class="stat-label">权限</span>
                </div>
              </div>
              <div class="role-tags">
                <el-tag size="small" :type="role.status === 1 ? 'success' : 'danger'">
                  {{ role.status === 1 ? '启用' : '禁用' }}
                </el-tag>
                <el-tag size="small" type="info">{{ role.code }}</el-tag>
              </div>
              <div class="role-actions">
                <el-button size="small" @click="viewRoleUsers(role)">查看用户</el-button>
                <el-button size="small" @click="openRoleDialog(role)">编辑</el-button>
                <el-button size="small" type="primary" @click="openPermissionDialog(role)">权限配置</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 资源管理 -->
        <div v-if="activeMenu === 'resource' && currentUser.role === 1" class="content-panel">
          <div class="panel-header">
            <div class="header-info">
              <h2>资源管理</h2>
              <p>管理系统的菜单资源和功能权限</p>
            </div>
          </div>

          <el-card shadow="never" class="resource-card">
            <el-tree :data="resourceTree" :props="treeProps" default-expand-all highlight-current>
              <template #default="{ data }">
                <span class="tree-node">
                  <svg v-if="data.children" viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2z"/></svg>
                  <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>
                  <span class="node-label">{{ data.label }}</span>
                  <el-tag size="small" type="info" class="node-type">{{ data.type }}</el-tag>
                </span>
              </template>
            </el-tree>
          </el-card>
        </div>
      </main>
    </div>

    <!-- 编辑个人信息弹窗 -->
    <el-dialog v-model="editProfileVisible" title="编辑个人信息" width="500px" class="custom-dialog">
      <el-form :model="profileForm" label-width="80px">
        <el-form-item label="真实姓名">
          <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editProfileVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordVisible" title="修改密码" width="450px" class="custom-dialog">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请确认新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 用户编辑弹窗 -->
    <el-dialog v-model="userDialogVisible" :title="editingUser ? '编辑用户' : '新建用户'" width="500px" class="custom-dialog">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="用户名" v-if="!editingUser">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" v-if="!editingUser">
          <el-input v-model="userForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role" style="width: 100%;" placeholder="请选择角色">
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.name"
              :value="getRoleValue(role.code)"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">确认</el-button>
      </template>
    </el-dialog>

    <!-- 角色编辑弹窗 -->
    <el-dialog v-model="roleDialogVisible" :title="editingRole ? '编辑角色' : '新建角色'" width="500px" class="custom-dialog">
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" required>
          <el-input v-model="roleForm.code" placeholder="请输入角色编码，如 ROLE_ADMIN" :disabled="editingRole" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="roleForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">确认</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置弹窗 -->
    <el-dialog v-model="permissionDialogVisible" :title="`权限配置 - ${editingRole?.name}`" width="700px" class="custom-dialog permission-dialog">
      <div class="permission-config">
        <div class="permission-tip">勾选该角色拥有的权限</div>
        <el-tree
          ref="permissionTreeRef"
          :data="permissionTree"
          :props="permissionProps"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          check-strictly
          class="permission-tree"
        >
          <template #default="{ data }">
            <span class="tree-node-content">
              <span class="node-name">{{ data.name }}</span>
              <el-tag size="small" type="info" class="node-type">{{ data.type === 'menu' ? '菜单' : '按钮' }}</el-tag>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions">保存配置</el-button>
      </template>
    </el-dialog>

    <!-- 查看角色用户弹窗 -->
    <el-dialog v-model="roleUsersDialogVisible" :title="`${editingRole?.name} - 用户列表`" width="700px" class="custom-dialog">
      <div class="role-users-container">
        <div class="role-info-bar">
          <div class="role-info-item">
            <span class="label">角色名称：</span>
            <span class="value">{{ editingRole?.name }}</span>
          </div>
          <div class="role-info-item">
            <span class="label">角色编码：</span>
            <el-tag size="small" type="info">{{ editingRole?.code }}</el-tag>
          </div>
          <div class="role-info-item">
            <span class="label">用户数量：</span>
            <span class="value">{{ roleUsers.length }}</span>
          </div>
        </div>
        <el-divider />
        <div class="users-list" v-if="roleUsers.length > 0">
          <div class="user-item" v-for="user in roleUsers" :key="user.id">
            <div class="user-avatar">
              {{ (user.realName || user.username).charAt(0).toUpperCase() }}
            </div>
            <div class="user-info">
              <div class="user-name">{{ user.realName || user.username }}</div>
              <div class="user-detail">
                <span>用户名：{{ user.username }}</span>
                <span v-if="user.email">邮箱：{{ user.email }}</span>
                <span v-if="user.phone">手机：{{ user.phone }}</span>
              </div>
            </div>
            <div class="user-status">
              <el-tag size="small" :type="user.status === 1 ? 'success' : 'danger'">
                {{ user.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-empty v-else description="该角色下暂无用户" />
      </div>
      <template #footer>
        <el-button type="primary" @click="goToAddUser">新建该角色用户</el-button>
        <el-button @click="roleUsersDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 带token的请求方法
const authFetch = async (url, options = {}) => {
  const token = localStorage.getItem('token')
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return fetch(url, { ...options, headers })
}

// 状态
const activeMenu = ref('profile')
const currentNav = ref('system')
const userMenuVisible = ref(false)
const loading = ref(false)
const editProfileVisible = ref(false)
const passwordVisible = ref(false)
const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const editingUser = ref(null)
const editingRole = ref(null)
const permissionTreeRef = ref(null)
const roleUsersDialogVisible = ref(false)
const roleUsers = ref([])

// 用户信息
const currentUser = ref({
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  email: 'admin@rpa.com',
  phone: '13800138002',
  role: 1,
  createTime: '2026-03-16T21:09:34'
})

// 表单
const profileForm = reactive({ realName: '', email: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const userForm = reactive({ username: '', password: '', realName: '', email: '', phone: '', role: 0 })

// 头像相关
const userAvatarUrl = ref('')

// 用户列表
const users = ref([])
const userSearch = ref('')
const filteredUsers = computed(() =>
  users.value.filter(u =>
    !userSearch.value ||
    u.username.includes(userSearch.value) ||
    (u.realName && u.realName.includes(userSearch.value))
  )
)

// 角色列表
const roles = ref([])
const roleForm = reactive({ name: '', code: '', description: '', status: 1 })

// 权限树
const permissionProps = { children: 'children', label: 'name' }
const permissionTree = ref([])

// 资源树
const treeProps = { children: 'children', label: 'label' }
const resourceTree = ref([
  { label: '系统管理', children: [
    { label: '个人信息', type: '菜单' },
    { label: '用户管理', type: '菜单' },
    { label: '角色管理', type: '菜单' },
    { label: '资源管理', type: '菜单' }
  ]},
  { label: 'RPA运营', children: [
    { label: '任务管理', type: '菜单' },
    { label: '机器人管理', type: '菜单' },
    { label: '流程管理', type: '菜单' }
  ]},
  { label: '数据管理', children: [
    { label: '数据采集', type: '菜单' },
    { label: '数据解析', type: '菜单' },
    { label: '数据加工', type: '菜单' },
    { label: '数据查询', type: '菜单' }
  ]}
])

// 计算属性
const userInitials = computed(() =>
  (currentUser.value.realName || currentUser.value.username).charAt(0).toUpperCase()
)

// 方法
const getRoleText = (role) => role === 1 ? '管理员' : '普通用户'

const goToMain = () => {
  router.push('/layout')
}

const goToProfile = () => {
  userMenuVisible.value = false
  activeMenu.value = 'profile'
}

const toggleUserMenu = () => {
  userMenuVisible.value = !userMenuVisible.value
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.removeItem('userInfo')
    localStorage.removeItem('token')
    router.push('/login')
    ElMessage.success('已退出登录')
  })
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', { hour12: false })
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await authFetch(`${API_BASE}/user`)
    const result = await res.json()
    if (result.code === 0) users.value = result.data || []
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

const openUserDialog = (user) => {
  editingUser.value = user
  if (user) {
    Object.assign(userForm, { username: user.username, password: '', realName: user.realName, email: user.email, phone: user.phone, role: user.role })
  } else {
    Object.assign(userForm, { username: '', password: '', realName: '', email: '', phone: '', role: 0 })
  }
  userDialogVisible.value = true
}

const saveUser = async () => {
  // 基本验证
  if (!editingUser.value && !userForm.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  try {
    const url = editingUser.value ? `${API_BASE}/user/${editingUser.value.id}` : `${API_BASE}/user/register`
    const method = editingUser.value ? 'PUT' : 'POST'

    // 构建请求数据
    const requestData = { ...userForm }
    if (editingUser.value) {
      // 编辑时只传需要更新的字段
      delete requestData.username
      delete requestData.password
    } else {
      // 新建用户时，若未填写密码，默认使用 123456
      if (!requestData.password || requestData.password.trim() === '') {
        requestData.password = '123456'
      }
    }

    const res = await authFetch(url, {
      method,
      body: JSON.stringify(requestData)
    })
    const result = await res.json()
    if (result.code === 0) {
      ElMessage.success(editingUser.value ? '更新成功' : '创建成功')
      userDialogVisible.value = false
      loadUsers()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

const deleteUser = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
    const res = await authFetch(`${API_BASE}/user/${id}`, { method: 'DELETE' })
    const result = await res.json()
    if (result.code === 0) {
      ElMessage.success('删除成功')
      loadUsers()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch {}
}

// 角色管理
const loadRoles = async () => {
  loading.value = true
  try {
    const res = await authFetch(`${API_BASE}/role`)
    const result = await res.json()
    if (result.code === 0) {
      roles.value = result.data || []
    }
  } catch {
    roles.value = []
  } finally {
    loading.value = false
  }
}

const openRoleDialog = (role) => {
  editingRole.value = role
  if (role) {
    Object.assign(roleForm, { name: role.name, code: role.code, description: role.description || '', status: role.status })
  } else {
    Object.assign(roleForm, { name: '', code: '', description: '', status: 1 })
  }
  roleDialogVisible.value = true
}

const saveRole = async () => {
  if (!roleForm.name) {
    ElMessage.warning('请输入角色名称')
    return
  }
  if (!roleForm.code) {
    ElMessage.warning('请输入角色编码')
    return
  }
  try {
    const url = editingRole.value ? `${API_BASE}/role/${editingRole.value.id}` : `${API_BASE}/role`
    const method = editingRole.value ? 'PUT' : 'POST'
    const res = await authFetch(url, {
      method,
      body: JSON.stringify(roleForm)
    })
    const result = await res.json()
    if (result.code === 0) {
      ElMessage.success(editingRole.value ? '更新成功' : '创建成功')
      roleDialogVisible.value = false
      loadRoles()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

const loadPermissions = async () => {
  try {
    const res = await authFetch(`${API_BASE}/permission/tree`)
    const result = await res.json()
    if (result.code === 0) {
      permissionTree.value = result.data || []
    }
  } catch {
    permissionTree.value = []
  }
}

const openPermissionDialog = async (role) => {
  editingRole.value = role
  permissionDialogVisible.value = true
  await loadPermissions()

  // 获取角色当前权限
  try {
    const res = await authFetch(`${API_BASE}/role/${role.id}`)
    const result = await res.json()
    if (result.code === 0 && result.data.permissionIds) {
      nextTick(() => {
        if (permissionTreeRef.value) {
          permissionTreeRef.value.setCheckedKeys(Array.from(result.data.permissionIds))
        }
      })
    }
  } catch {}
}

const savePermissions = async () => {
  if (!permissionTreeRef.value) return
  const checkedKeys = permissionTreeRef.value.getCheckedKeys()
  try {
    const res = await authFetch(`${API_BASE}/role/${editingRole.value.id}/permissions`, {
      method: 'PUT',
      body: JSON.stringify({ permissionIds: checkedKeys })
    })
    const result = await res.json()
    if (result.code === 0) {
      ElMessage.success('权限配置成功')
      permissionDialogVisible.value = false
      loadRoles()
    } else {
      ElMessage.error(result.message || '配置失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

const getRoleClassName = (code) => {
  if (code === 'ROLE_ADMIN') return 'admin'
  if (code === 'ROLE_OPERATOR') return 'operator'
  return 'user'
}

// 根据角色编码获取角色值
const getRoleValue = (code) => {
  if (code === 'ROLE_ADMIN') return 1
  if (code === 'ROLE_OPERATOR') return 2
  return 0
}

// 查看角色用户
const viewRoleUsers = async (role) => {
  editingRole.value = role
  roleUsersDialogVisible.value = true
  roleUsers.value = []

  try {
    const res = await authFetch(`${API_BASE}/role/${role.id}/users`)
    const result = await res.json()
    if (result.code === 0) {
      roleUsers.value = result.data || []
    }
  } catch {
    roleUsers.value = []
  }
}

// 跳转到新建用户页面
const goToAddUser = () => {
  roleUsersDialogVisible.value = false
  activeMenu.value = 'user'
  // 设置新建用户的角色为当前查看的角色
  if (editingRole.value) {
    if (editingRole.value.code === 'ROLE_ADMIN') {
      userForm.role = 1
    } else if (editingRole.value.code === 'ROLE_OPERATOR') {
      userForm.role = 2
    } else {
      userForm.role = 0
    }
  }
  nextTick(() => {
    openUserDialog(null)
  })
}

const saveProfile = async () => {
  try {
    const res = await authFetch(`${API_BASE}/user/${currentUser.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        realName: profileForm.realName,
        email: profileForm.email,
        phone: profileForm.phone
      })
    })
    const result = await res.json()
    if (result.code === 0) {
      ElMessage.success('保存成功')
      currentUser.value.realName = profileForm.realName
      currentUser.value.email = profileForm.email
      currentUser.value.phone = profileForm.phone
      // 更新本地存储
      localStorage.setItem('userInfo', JSON.stringify(currentUser.value))
      editProfileVisible.value = false
    } else {
      ElMessage.error(result.message || '保存失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

const changePassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入密码不一致')
    return
  }
  try {
    const res = await authFetch(`${API_BASE}/user/password/${currentUser.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
    })
    const result = await res.json()
    if (result.code === 0) {
      ElMessage.success('密码修改成功')
      passwordVisible.value = false
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

// 账户统计
const accountAge = computed(() => {
  if (!currentUser.value.createTime) return '0'
  const days = Math.floor((new Date() - new Date(currentUser.value.createTime)) / (1000 * 60 * 60 * 24))
  return days > 0 ? days : '0'
})

const lastLoginDays = ref('今天')

// 处理头像上传失败
const handleAvatarError = async (event) => {
  const imgElement = event.target
  const failedSrc = imgElement.src
  console.error('❌ 头像图片加载失败:')
  console.error('  - 请求的 URL:', failedSrc)
  console.error('  - 错误事件:', event)
  
  // 检查是否是文件不存在的问题
  if (failedSrc.includes('/api/user/avatar/image/')) {
    console.warn('⚠️ 头像文件可能不存在，尝试清除数据库中的无效记录...')
    
    // 清除本地存储中的头像信息
    userAvatarUrl.value = ''
    currentUser.value.avatar = null
    localStorage.setItem('userInfo', JSON.stringify(currentUser.value))
    
    // 调用后端 API 清除数据库中的头像路径
    try {
      await fetch(`${API_BASE}/user/${currentUser.value.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ avatar: null })
      })
      console.log('✅ 已清除数据库中的无效头像路径')
    } catch (error) {
      console.error('清除数据库头像路径失败:', error)
    }
    
    ElMessage.warning('头像文件不存在，已恢复默认头像')
  }
}

// 处理头像上传
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  console.log('📤 准备上传头像:', file.name, '大小:', (file.size / 1024).toFixed(2), 'KB')
  
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 2MB')
    return
  }
  
  const formData = new FormData()
  formData.append('avatar', file)
  
  const token = localStorage.getItem('token')
  try {
    // 使用带 token 的请求
    const res = await fetch(`${API_BASE}/user/avatar/${currentUser.value.id}`, {
      method: 'POST',
      headers: token ? { 'Authorization': `Bearer ${token}` } : {},
      body: formData
    })
    const result = await res.json()
    console.log('📥 头像上传响应:', result)
    
    if (result.code === 0) {
      // 从返回的数据中获取 imageUrl (已经是完整路径 /api/user/avatar/image/xxx)
      const imageUrl = result.data?.imageUrl || result.data
      console.log('✅ 获取到的 imageUrl:', imageUrl)
      
      // 构建完整的头像 URL（需要加上后端服务器地址）
      // API_BASE 通常是 http://localhost:8080/api
      // imageUrl 是 /api/user/avatar/image/xxx
      // 最终需要：http://localhost:8080/api/user/avatar/image/xxx
      userAvatarUrl.value = `${API_BASE}${imageUrl.replace('/api', '')}`
      console.log('✅ 设置的头像 URL:', userAvatarUrl.value)
      
      currentUser.value.avatar = imageUrl
      localStorage.setItem('userInfo', JSON.stringify(currentUser.value))
      ElMessage.success('头像更新成功')
      
      // 延迟刷新页面，确保数据库已更新
      setTimeout(() => {
        window.location.reload()
      }, 1500)
    } else {
      console.error('❌ 上传失败:', result.message)
      ElMessage.error(result.message || '上传失败')
    }
  } catch (error) {
    console.error('❌ 头像上传错误:', error)
    ElMessage.error('上传失败，请检查网络连接')
    
    // 如果上传失败，使用本地预览
    const reader = new FileReader()
    reader.onload = (e) => {
      userAvatarUrl.value = e.target.result
      ElMessage.warning('头像上传失败，已使用本地预览')
    }
    reader.readAsDataURL(file)
  }
}

const loadUserFromStorage = () => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUser.value = { ...currentUser.value, ...user }
      if (user.avatar) {
        // avatar 存储的是 /api/user/avatar/image/xxx 格式
        // 需要构建完整的 URL：http://localhost:8080/api/user/avatar/image/xxx
        userAvatarUrl.value = `${API_BASE}${user.avatar.replace('/api', '')}`
        console.log('✅ 从本地存储加载头像 URL:', userAvatarUrl.value)
      }
    } catch (e) {
      console.error('❌ 解析本地存储失败:', e)
    }
  }
  profileForm.realName = currentUser.value.realName
  profileForm.email = currentUser.value.email
  profileForm.phone = currentUser.value.phone
}

// 切换用户状态
const toggleUserStatus = async (user) => {
  const newStatus = user.status === 1 ? 0 : 1
  
  try {
    const res = await fetch(`${API_BASE}/user/${user.id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    })
    
    const result = await res.json()
    
    if (result.code === 0) {
      ElMessage.success(`用户已${newStatus === 1 ? '启用' : '禁用'}`)
      // 更新本地数据
      user.status = newStatus
    } else {
      // 恢复原状态
      user.status = newStatus === 1 ? 0 : 1
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    console.error('切换用户状态错误:', error)
    // 恢复原状态
    user.status = newStatus === 1 ? 0 : 1
    ElMessage.error('网络错误，请重试')
  }
}

onMounted(() => {
  loadUserFromStorage()
  loadUsers()
  loadRoles()
  loadPermissions()
})

// 点击其他地方关闭菜单
const handleClickOutside = (e) => {
  if (userMenuVisible.value) {
    userMenuVisible.value = false
  }
}
document.addEventListener('click', handleClickOutside)
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
</style>

<style scoped>
.profile-page { display: flex; flex-direction: column; min-height: 100vh; background: #f5f7fa; }

/* 顶部导航 */
.top-navbar { height: 60px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; border-bottom: 1px solid #e8e8e8; position: sticky; top: 0; z-index: 100; }
.navbar-brand { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.brand-icon { width: 36px; height: 36px; background: linear-gradient(135deg, #1890ff, #0050b3); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; }
.brand-name { font-size: 17px; font-weight: 600; color: #1a1a1a; letter-spacing: .5px; }

.navbar-menu { display: flex; gap: 4px; }
.menu-item { display: flex; align-items: center; gap: 6px; padding: 8px 18px; border-radius: 6px; color: #595961; cursor: pointer; transition: all .2s; font-size: 14px; font-weight: 500; }
.menu-item:hover { background: #f5f7fa; color: #1890ff; }
.menu-item.active { background: #e6f4ff; color: #1890ff; }

.navbar-user { position: relative; }
.user-info { display: flex; align-items: center; gap: 10px; padding: 6px 12px; border-radius: 8px; cursor: pointer; transition: background .2s; }
.user-info:hover { background: #f5f7fa; }
.user-avatar { width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(135deg, #1890ff, #0050b3); display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; font-size: 14px; }
.user-detail { display: flex; flex-direction: column; }
.user-name { font-size: 13px; font-weight: 500; color: #262626; }
.user-role { font-size: 11px; color: #8c8c8c; }
.arrow-icon { color: #8c8c8c; transition: transform .2s; }
.arrow-icon.rotated { transform: rotate(180deg); }

.user-dropdown { position: absolute; top: calc(100% + 8px); right: 0; background: #fff; border-radius: 12px; box-shadow: 0 6px 30px rgba(0,0,0,.12); min-width: 180px; overflow: hidden; z-index: 1000; border: 1px solid #f0f0f0; }
.dropdown-item { display: flex; align-items: center; gap: 10px; padding: 12px 16px; color: #262626; cursor: pointer; transition: background .2s; font-size: 14px; }
.dropdown-item:hover { background: #f5f7fa; }
.dropdown-item.danger { color: #ff4d4f; }
.dropdown-divider { height: 1px; background: #f0f0f0; }
.dropdown-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 999; }

/* 主容器 */
.main-container { display: flex; flex: 1; }

/* 侧边栏 */
.sidebar { width: 220px; background: #fff; border-right: 1px solid #e8e8e8; }
.sidebar-header { padding: 20px 24px; border-bottom: 1px solid #f0f0f0; }
.sidebar-title { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.sidebar-nav { padding: 12px 8px; }
.nav-item { display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-radius: 8px; color: #595961; cursor: pointer; transition: all .2s; font-size: 14px; margin-bottom: 4px; }
.nav-item:hover { background: #f5f7fa; color: #1890ff; }
.nav-item.active { background: #e6f4ff; color: #1890ff; font-weight: 500; }

/* 内容区 */
.content-area { flex: 1; padding: 24px; overflow-y: auto; }
.content-panel { animation: fadeIn .3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

.panel-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.header-info h2 { 
  font-size: 24px; 
  font-weight: 600; 
  color: #1a1a1a; 
  margin-bottom: 4px;
}

.header-info p { 
  font-size: 14px; 
  color: #8c8c8c;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  height: auto;
  font-size: 14px;
  border-radius: 6px;
  background: linear-gradient(135deg, #ff4d4f, #e64545);
  border: none;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.2);
}

.logout-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

/* 个人信息卡片 */
.profile-content {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 20px;
}

.profile-left {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-right {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
  overflow: hidden;
  transition: all 0.3s ease;
}

.profile-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  background: #fafafa;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title svg {
  color: #1890ff;
}

.card-header .edit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  font-size: 13px;
  height: auto;
}

.card-body {
  padding: 20px;
}

/* 头像卡片 */
.avatar-card .avatar-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  background: linear-gradient(to right, #fafafa, #fff);
}

.avatar-card .avatar-body {
  padding: 40px 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(180deg, #fafafa 0%, #fff 100%);
}

.avatar-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar-large {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1890ff, #0050b3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 48px;
  font-weight: 700;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  border: 3px solid #e6f4ff;
}

.avatar-large.has-avatar {
  background-color: #f0f0f0;
  border-color: #1890ff;
}

.avatar-large:hover {
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.5), 0 0 0 4px rgba(24, 144, 255, 0.1);
  transform: scale(1.05) rotate(2deg);
  border-color: #40a9ff;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  background: linear-gradient(135deg, #f5f7fa, #fff);
  border: 2px dashed #d9d9d9;
  border-radius: 10px;
  color: #595961;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.upload-btn:hover {
  background: linear-gradient(135deg, #e6f4ff, #f0f4ff);
  border-color: #1890ff;
  color: #1890ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.upload-btn svg {
  transition: transform 0.3s ease;
}

.upload-btn:hover svg {
  transform: scale(1.1) rotate(-5deg);
}

/* 信息列表 */
.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item-row {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  background: #fafafa;
  border-radius: 8px;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.info-item-row:hover {
  background: linear-gradient(135deg, #f0f4ff, #fafafa);
  transform: translateX(2px);
}

.info-label {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 120px;
  font-size: 13px;
  color: #8c8c8c;
}

.info-label svg {
  color: #1890ff;
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.role-tag {
  font-weight: 500;
}

/* 安全设置 */
.security-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.security-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #f0f0f0;
}

.security-item:hover {
  background: linear-gradient(135deg, #f0f4ff, #fafafa);
  border-color: #bae7ff;
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.security-icon {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #1890ff, #0050b3);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
}

.security-content {
  flex: 1;
  min-width: 0;
}

.security-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
}

.security-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.security-action {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #1890ff;
  font-weight: 500;
}

/* 统计卡片 */
.stats-card .card-header {
  background: linear-gradient(to right, #fafafa, #fff);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa, #fff);
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.stat-box:hover {
  background: linear-gradient(135deg, #e6f4ff, #f0f4ff);
  border-color: #bae7ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #1890ff;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 12px;
  color: #8c8c8c;
}

/* 工具栏 */
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.toolbar .el-input { display: flex; align-items: center; }

/* 数据表格 */
.data-table { border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,.04); border: 1px solid #f0f0f0; }
.data-table :deep(.el-table__header th) { background: #fafafa !important; }

/* 角色卡片 */
.role-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.role-card { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,.04); border: 1px solid #f0f0f0; transition: all .3s; }
.role-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,.1); transform: translateY(-2px); }
.role-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.role-icon { width: 52px; height: 52px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.role-icon.admin { background: linear-gradient(135deg, #ff4d4f, #e64545); }
.role-icon.operator { background: linear-gradient(135deg, #1890ff, #0050b3); }
.role-icon.user { background: linear-gradient(135deg, #52c41a, #389e0d); }
.role-info { flex: 1; min-width: 0; }
.role-info h3 { font-size: 16px; font-weight: 600; color: #1a1a1a; margin-bottom: 4px; }
.role-info p { font-size: 13px; color: #8c8c8c; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.role-stats { display: flex; gap: 32px; margin-bottom: 12px; padding: 12px 0; border-top: 1px solid #f0f0f0; border-bottom: 1px solid #f0f0f0; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-value { font-size: 24px; font-weight: 700; color: #1890ff; }
.stat-label { font-size: 12px; color: #8c8c8c; margin-top: 2px; }
.role-tags { display: flex; gap: 8px; margin-bottom: 16px; }
.role-actions { display: flex; gap: 8px; }

/* 权限配置 */
.permission-config { padding: 8px 0; }
.permission-tip { font-size: 13px; color: #8c8c8c; margin-bottom: 16px; padding: 8px 12px; background: #f5f7fa; border-radius: 6px; }
.permission-tree { background: transparent; }
.permission-tree :deep(.el-tree-node__content) { height: 36px; }
.tree-node-content { display: flex; align-items: center; gap: 8px; flex: 1; }
.tree-node-content .node-name { flex: 1; }
.permission-dialog :deep(.el-dialog__body) { max-height: 500px; overflow-y: auto; }

/* 角色用户弹窗 */
.role-users-container { padding: 8px 0; }
.role-info-bar { display: flex; gap: 24px; padding: 0 8px; }
.role-info-item { display: flex; align-items: center; gap: 8px; }
.role-info-item .label { font-size: 13px; color: #8c8c8c; }
.role-info-item .value { font-size: 14px; color: #262626; font-weight: 500; }
.users-list { display: flex; flex-direction: column; gap: 12px; max-height: 400px; overflow-y: auto; }
.user-item { display: flex; align-items: center; gap: 16px; padding: 16px; background: #fafafa; border-radius: 10px; transition: background 0.2s; }
.user-item:hover { background: #f0f4ff; }
.user-item .user-avatar { width: 44px; height: 44px; border-radius: 50%; background: linear-gradient(135deg, #1890ff, #0050b3); display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; font-size: 16px; flex-shrink: 0; }
.user-item .user-info { flex: 1; min-width: 0; }
.user-item .user-name { font-size: 14px; font-weight: 500; color: #262626; margin-bottom: 4px; }
.user-item .user-detail { display: flex; gap: 16px; font-size: 12px; color: #8c8c8c; }
.user-item .user-status { flex-shrink: 0; }

/* 资源卡片 */
.resource-card { border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,.04); border: 1px solid #f0f0f0; }
.tree-node { display: flex; align-items: center; gap: 8px; }
.node-label { margin-right: 8px; }
.node-type { font-size: 10px; }

/* 弹窗 */
.custom-dialog :deep(.el-dialog) { border-radius: 12px; }
.custom-dialog :deep(.el-dialog__header) { padding: 16px 24px; border-bottom: 1px solid #f0f0f0; }
.custom-dialog :deep(.el-dialog__title) { font-size: 16px; font-weight: 600; }
.custom-dialog :deep(.el-dialog__body) { padding: 24px; }
.custom-dialog :deep(.el-dialog__footer) { padding: 12px 24px; border-top: 1px solid #f0f0f0; }

/* 下拉动画 */
.dropdown-enter-active, .dropdown-leave-active { transition: all .2s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-8px); }

/* 响应式 */
@media (max-width: 1200px) {
  .profile-content {
    grid-template-columns: 1fr;
  }
  .profile-right {
    order: -1;
  }
  .role-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .sidebar {
    width: 180px;
  }
  .role-grid {
    grid-template-columns: 1fr;
  }
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
