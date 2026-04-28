<template>
  <div class="chat-app">
    <!-- 左侧会话列表 -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <h2>协作中枢</h2>
        <div class="header-actions">
          <el-button circle size="small" type="primary" @click="openNewChatDialog" title="发起会话">
            <el-icon><Plus /></el-icon>
          </el-button>
          <el-button circle size="small" type="primary" @click="showNewGroup = true" title="创建群聊">
            <el-icon><UserFilled /></el-icon>
          </el-button>
        </div>
      </div>

      <div class="search-box">
        <el-input v-model="searchKeyword" placeholder="搜索会话..." prefix-icon="Search" clearable size="large" />
      </div>

      <!-- 会话分类 -->
      <div class="conversation-tabs">
        <el-radio-group v-model="activeTab" size="small">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="private">单聊</el-radio-button>
          <el-radio-button value="group">群聊</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 会话列表 -->
      <div class="conversation-list">
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <div v-else-if="filteredConversations.length === 0" class="empty-state">
          <el-icon :size="48"><ChatDotRound /></el-icon>
          <p>暂无会话</p>
          <el-button type="primary" size="small" @click="openNewChatDialog">开始聊天</el-button>
        </div>

        <div
          v-else
          v-for="conv in filteredConversations"
          :key="conv.conversation?.id"
          class="conversation-item"
          :class="{ active: currentConvId === conv.conversation?.id }"
          @click="selectConversation(conv)"
        >
          <div class="conv-avatar">
            <el-avatar :size="44" :src="getOtherUserAvatar(conv)">
              {{ getOtherUserAvatar(conv) ? '' : (conv.otherUserName?.charAt(0) || conv.conversation?.name?.charAt(0) || '?') }}
            </el-avatar>
            <span v-if="conv.unreadCount > 0" class="unread-badge">
              {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
            </span>
          </div>
          <div class="conv-info">
            <div class="conv-name">
              {{ conv.otherUserName || conv.conversation?.name || '未命名会话' }}
              <el-tag v-if="conv.conversation?.type === 'group'" size="small" type="success">群</el-tag>
              <el-tag v-if="conv.conversation?.type === 'temporary'" size="small" type="warning">临时</el-tag>
            </div>
            <div class="conv-preview">{{ conv.conversation?.lastMessageContent || '暂无消息' }}</div>
          </div>
          <div class="conv-meta">
            <div class="conv-time">{{ formatTime(conv.conversation?.lastMessageTime) }}</div>
          </div>
        </div>
      </div>

    </aside>

    <!-- 右侧聊天区域 -->
    <main class="chat-main">
      <template v-if="currentConversation">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="chat-info">
            <el-avatar :size="40" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              {{ otherUserName?.charAt(0) || currentConversation.name?.charAt(0) || '?' }}
            </el-avatar>
            <div class="chat-title">
              <h3>{{ otherUserName || currentConversation.name }}</h3>
              <span class="subtitle">
                <template v-if="currentConversation.type === 'group'">
                  {{ currentConversation.memberCount || 0 }} 人
                </template>
                <template v-else>在线</template>
              </span>
            </div>
          </div>
          <div class="chat-status">
            <span class="ws-status" :class="{ connected: wsConnected }">
              <span class="status-dot"></span>
              {{ wsConnected ? '已连接' : '连接中...' }}
            </span>
          </div>
          <div class="chat-actions">
            <el-button circle @click="showConvInfo = true" title="会话详情">
              <el-icon><InfoFilled /></el-icon>
            </el-button>
            <el-button circle @click="generateSummary" title="生成摘要">
              <el-icon><Document /></el-icon>
            </el-button>
            <el-button circle @click="refreshMessages" title="刷新">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="message-area" ref="messageAreaRef">
          <div v-if="loadingMessages" class="loading-messages">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          </div>

          <div v-else-if="messages.length === 0" class="empty-messages">
            <div class="empty-icon"><el-icon :size="64"><ChatLineSquare /></el-icon></div>
            <p>开始聊天吧</p>
            <span class="empty-hint">发送消息开始沟通</span>
          </div>

          <div v-else class="message-list">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{
                'message-self': msg.senderId === currentUserId,
                'message-other': msg.senderId !== currentUserId,
                'message-notice': msg.type === 'notice'
              }"
            >
              <!-- 系统消息 -->
              <div v-if="msg.type === 'notice'" class="notice-message">
                {{ msg.content }}
              </div>

              <!-- 普通消息 -->
              <template v-else>
                <el-avatar
                  v-if="msg.senderId !== currentUserId"
                  :size="36"
                  class="msg-avatar"
                  style="background: linear-gradient(135deg, #409eff, #66b1ff)"
                >
                  {{ msg.senderName?.charAt(0) || '?' }}
                </el-avatar>

                <div class="message-content">
                  <div class="message-header">
                    <span class="sender-name">{{ msg.senderName }}</span>
                    <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
                  </div>

                  <!-- 文本消息 -->
                  <div v-if="msg.type === 'text'" class="message-bubble">
                    {{ msg.content }}
                  </div>

                  <!-- RPA卡片 -->
                  <div v-else-if="msg.type === 'rpa_card'" class="rpa-card-wrapper">
                    <div class="rpa-card" :class="'card-' + msg.cardType">
                      <div class="card-header">
                        <el-icon><Timer /></el-icon>
                        <span>{{ getCardTitle(msg.cardType) }}</span>
                      </div>
                      <div class="card-body">
                        <div class="card-content">{{ msg.cardData || 'RPA卡片内容' }}</div>
                      </div>
                    </div>
                  </div>

                  <!-- 文件消息 -->
                  <div v-else-if="msg.type === 'file'" class="file-message">
                    <el-icon><Document /></el-icon>
                    <span>{{ msg.content }}</span>
                  </div>
                </div>

                <!-- 操作按钮（自己的消息） -->
                <div v-if="msg.senderId === currentUserId" class="message-actions">
                  <el-dropdown trigger="click">
                    <el-icon><MoreFilled /></el-icon>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="recallMessage(msg)">撤回</el-dropdown-item>
                        <el-dropdown-item @click="copyMessage(msg)">复制</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-toolbar">
            <el-button size="small" text @click="triggerFileUpload">
              <el-icon><FolderOpened /></el-icon> 文件
            </el-button>
            <el-button size="small" text @click="showRPAPanel = true">
              <el-icon><Grid /></el-icon> RPA卡片
            </el-button>
          </div>

          <!-- 敏感词提示 -->
          <div v-if="detectedSensitiveWords.length > 0" class="sensitive-warning">
            <el-icon><WarningFilled /></el-icon>
            <span>检测到敏感词：{{ detectedSensitiveWords.join('、') }}</span>
            <el-button size="small" text type="danger" @click="clearSensitiveWords">清除</el-button>
          </div>

          <div class="input-wrapper">
            <el-input
              ref="messageInputRef"
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              :placeholder="inputPlaceholder"
              @keydown.enter.exact.prevent="sendMessage"
              @input="checkSensitiveWords"
              resize="none"
            />
          </div>

          <div class="input-footer">
            <span class="input-hint">Enter 发送 | Shift+Enter 换行</span>
            <el-button type="primary" :disabled="(!inputMessage.trim() && !selectedFile) || detectedSensitiveWords.length > 0" @click="sendMessage" :loading="sending">
              <el-icon v-if="!sending"><Promotion /></el-icon>
              发送
            </el-button>
          </div>

          <!-- 文件预览 -->
          <div v-if="selectedFile" class="file-preview">
            <el-icon><Document /></el-icon>
            <span>{{ selectedFile.name }}</span>
            <el-icon class="remove-file" @click="removeFile"><Close /></el-icon>
          </div>

          <input type="file" ref="fileInput" style="display:none" @change="handleFileSelect" />
        </div>
      </template>

      <!-- 未选中会话 -->
      <div v-else class="no-conversation">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="80"><ChatLineSquare /></el-icon>
          </div>
          <h2>欢迎使用 RPA 协作中枢</h2>
          <p>选择左侧会话开始聊天，或创建新会话</p>

          <div class="quick-start">
            <el-button type="primary" size="large" @click="openNewChatDialog">
              <el-icon><ChatDotRound /></el-icon>
              发起会话
            </el-button>
            <el-button size="large" @click="showNewGroup = true">
              <el-icon><UserFilled /></el-icon>
              创建群聊
            </el-button>
          </div>

          <div class="feature-list">
            <h4>核心功能</h4>
            <ul>
              <li><el-icon><Timer /></el-icon> 任务状态实时推送</li>
              <li><el-icon><Cpu /></el-icon> 机器人状态卡片</li>
              <li><el-icon><ChatLineSquare /></el-icon> 团队实时协作</li>
              <li><el-icon><Document /></el-icon> 日志一键分享</li>
            </ul>
          </div>
        </div>
      </div>
    </main>

    <!-- 新建会话对话框 - 优化版：直接显示用户列表，点击即可发起会话 -->
    <el-dialog v-model="showNewChat" title="发起会话" width="520px">
      <div class="quick-chat-panel">
        <!-- 搜索框 -->
        <div class="quick-search">
          <el-input
            v-model="userSearchKeyword"
            placeholder="搜索用户姓名..."
            prefix-icon="Search"
            clearable
            size="large"
            @input="filterUsers"
          />
        </div>

        <!-- 用户列表 - 直接展示，点击即可发起会话 -->
        <div class="quick-user-list">
          <div v-if="userLoading" class="loading-users">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>

          <div v-else-if="filteredQuickUsers.length === 0" class="no-users">
            <el-icon :size="40"><Search /></el-icon>
            <p v-if="userSearchKeyword">未找到用户 "{{ userSearchKeyword }}"</p>
            <p v-else>暂无用户</p>
          </div>

          <div
            v-else
            v-for="user in filteredQuickUsers"
            :key="user.id"
            class="quick-user-item"
            @click="startChatWithUser(user)"
          >
            <el-avatar :size="44" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              {{ user.realName?.charAt(0) || user.username?.charAt(0) || '?' }}
            </el-avatar>
            <div class="quick-user-info">
              <div class="quick-user-name">{{ user.realName || user.username }}</div>
              <div class="quick-user-meta">
                <el-tag v-if="user.role === 1" size="small" type="danger">管理员</el-tag>
                <el-tag v-else size="small" type="success">用户</el-tag>
                <span v-if="user.email" class="quick-user-email">{{ user.email }}</span>
              </div>
            </div>
            <el-icon class="quick-start-icon"><ChatDotRound /></el-icon>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 新建群聊对话框 -->
    <el-dialog v-model="showNewGroup" title="创建群聊" width="600px">
      <el-form label-width="80px">
        <el-form-item label="群名称" required>
          <el-input v-model="newGroupName" placeholder="请输入群名称" />
        </el-form-item>
        <el-form-item label="群描述">
          <el-input v-model="newGroupDesc" type="textarea" :rows="2" placeholder="请输入群描述（可选）" />
        </el-form-item>
        <el-form-item label="群成员" required>
          <div class="group-member-select">
            <!-- 成员搜索 -->
            <div class="member-search-mini">
              <el-input
                v-model="memberSearchKeyword"
                placeholder="搜索成员..."
                prefix-icon="Search"
                clearable
                size="small"
              />
            </div>

            <!-- 成员列表 -->
            <div class="group-member-list">
              <div
                v-for="user in filteredGroupMembers"
                :key="user.id"
                class="group-member-item"
                :class="{ selected: selectedMembers.includes(user.id) }"
                @click="toggleGroupMember(user.id)"
              >
                <el-avatar :size="32">
                  {{ user.realName?.charAt(0) || user.username?.charAt(0) }}
                </el-avatar>
                <span class="member-name">{{ user.realName || user.username }}</span>
                <el-icon v-if="selectedMembers.includes(user.id)" color="#409eff"><Check /></el-icon>
              </div>
            </div>

            <!-- 已选成员 -->
            <div v-if="selectedMembers.length > 0" class="selected-members">
              <span class="count">已选 {{ selectedMembers.length }} 人</span>
              <el-button size="small" text type="danger" @click="selectedMembers = []">清空</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNewGroup = false">取消</el-button>
        <el-button type="primary" @click="createGroup" :disabled="!newGroupName || selectedMembers.length === 0">创建</el-button>
      </template>
    </el-dialog>

    <!-- 会话详情对话框 -->
    <el-dialog v-model="showConvInfo" :title="currentConversation?.name || '会话详情'" width="400px">
      <div class="conv-info-content">
        <div class="info-item">
          <span class="info-label">类型</span>
          <el-tag :type="currentConversation?.type === 'group' ? 'success' : 'primary'">
            {{ currentConversation?.type === 'group' ? '群聊' : currentConversation?.type === 'temporary' ? '临时群' : '单聊' }}
          </el-tag>
        </div>
        <div class="info-item">
          <span class="info-label">成员数</span>
          <span>{{ currentConversation?.memberCount || 0 }} 人</span>
        </div>
        <div class="info-item">
          <span class="info-label">创建时间</span>
          <span>{{ formatTime(currentConversation?.createdAt) }}</span>
        </div>
        <div class="info-item" v-if="currentConversation?.description">
          <span class="info-label">描述</span>
          <span>{{ currentConversation.description }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- RPA卡片面板 -->
    <el-drawer v-model="showRPAPanel" title="发送RPA卡片" direction="rtl" size="400px">
      <div class="rpa-card-panel">
        <div class="rpa-card-item" @click="sendTaskCard">
          <div class="card-icon task"><el-icon><Timer /></el-icon></div>
          <div class="card-info">
            <h4>任务卡片</h4>
            <p>分享任务执行状态</p>
          </div>
        </div>

        <div class="rpa-card-item" @click="sendRobotCard">
          <div class="card-icon robot"><el-icon><Cpu /></el-icon></div>
          <div class="card-info">
            <h4>机器人卡片</h4>
            <p>分享机器人运行状态</p>
          </div>
        </div>

        <div class="rpa-card-item" @click="sendLogCard">
          <div class="card-icon log"><el-icon><Document /></el-icon></div>
          <div class="card-info">
            <h4>日志卡片</h4>
            <p>分享执行日志</p>
          </div>
        </div>

        <div class="rpa-card-item" @click="sendAlertCard">
          <div class="card-icon alert"><el-icon><Warning /></el-icon></div>
          <div class="card-info">
            <h4>告警卡片</h4>
            <p>发送系统告警通知</p>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, onActivated, onDeactivated, nextTick, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Plus, ChatLineSquare, ChatDotRound, Search, UserFilled, FolderOpened,
  Grid, Document, Timer, Cpu, Refresh, ArrowRight,
  Promotion, Warning, Avatar, Loading, Check, MoreFilled, InfoFilled, CaretRight, Close,
  WarningFilled
} from '@element-plus/icons-vue'
import { connect, disconnect, subscribeConversation, unsubscribe, isConnected } from '../../utils/stomp.js'

// API工具
const getAuthHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { 'Authorization': `Bearer ${token}` } : {}
}

const apiGet = async (url) => {
  const res = await fetch(`/api${url}`, {
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() }
  })
  return res.json()
}

const apiPost = async (url, data) => {
  const res = await fetch(`/api${url}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify(data)
  })
  return res.json()
}

// WebSocket状态
const wsConnected = ref(false)
const socket = ref(null)

// 从 localStorage 获取当前用户ID
const getCurrentUserId = () => {
  try {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      const user = JSON.parse(userInfo)
      return user.id || 1
    }
  } catch (e) {
    console.error('解析用户信息失败:', e)
  }
  return 1
}

const getCurrentUserName = () => {
  try {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      const user = JSON.parse(userInfo)
      return user.realName || user.username || '用户'
    }
  } catch (e) {
    console.error('解析用户信息失败:', e)
  }
  return '用户'
}

// 状态
const currentUserId = ref(getCurrentUserId())
const currentUserName = ref(getCurrentUserName())
const conversations = ref([])
const messages = ref([])
const currentConvId = ref(null)
const currentConversation = ref(null)
const otherUserName = ref('')
const inputMessage = ref('')
const searchKeyword = ref('')
const activeTab = ref('all')
const loading = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const showNewChat = ref(false)
const showNewGroup = ref(false)
const showRPAPanel = ref(false)
const showConvInfo = ref(false)
const userSearchKeyword = ref('')
const memberSearchKeyword = ref('')

// 用户相关状态
const allUsers = ref([])
const selectedUserId = ref(null)
const selectedUser = ref(null)
const newGroupName = ref('')
const newGroupDesc = ref('')
const selectedMembers = ref([])
const userLoading = ref(false)

// 快速发起会话的用户列表
const filteredQuickUsers = computed(() => {
  const users = allUsers.value.filter(u => u.id !== currentUserId.value)
  if (!userSearchKeyword.value) return users

  const keyword = userSearchKeyword.value.toLowerCase()
  return users.filter(u =>
    (u.realName && u.realName.toLowerCase().includes(keyword)) ||
    (u.username && u.username.toLowerCase().includes(keyword)) ||
    (u.email && u.email.toLowerCase().includes(keyword))
  )
})

// 过滤用户方法
const filterUsers = () => {
  // 使用计算属性自动过滤，这里不需要额外处理
}

// 引用
const messageAreaRef = ref(null)
const aiMessagesRef = ref(null)
const fileInput = ref(null)
const messageInputRef = ref(null)

// 待创建的群组（发送消息时才真正创建）
const pendingGroup = ref(null)

// 文件相关
const selectedFile = ref(null)

// 敏感词检测
const detectedSensitiveWords = ref([])
const sensitiveWordPatterns = [
  // 金融敏感词
  /银行|账户|密码|验证码|转账|汇款|支付|红包|余额|利率|利息|本金|贷款|信用卡|借记卡|社保|医保|公积金|投资|理财|基金|股票|证券|期货|保险|理赔|赔付/g,
  // 身份敏感词
  /身份证|护照|驾照|户口|社保卡|医保卡|工号|学号|姓名|性别|年龄|生日|籍贯|民族|学历|学位/g,
  // 联系方式敏感词
  /手机号|手机|电话|邮箱|地址|门牌|邮编|QQ号|QQ|微信号|微信/g,
  // 账号敏感词
  /账号|用户名|登录名|登录|注册|找回密码|安全问题/g,
  // 企业敏感词
  /公司|法人|营业执照|税务|发票|合同|协议|公章/g,
  // 金额敏感词
  /\d{4,}/g,  // 4位及以上纯数字
  /\d{16,19}/g,  // 16-19位数字（疑似卡号）
  // 其他敏感词
  /秘密|机密|隐私|泄露|盗用|诈骗|钓鱼|病毒|木马/g,
]

// 检测敏感词
const checkSensitiveWords = () => {
  const content = inputMessage.value
  if (!content || !content.trim()) {
    detectedSensitiveWords.value = []
    return
  }
  
  const found = []
  for (const pattern of sensitiveWordPatterns) {
    // 重置正则表达式的 lastIndex
    pattern.lastIndex = 0
    const matches = content.match(pattern)
    if (matches) {
      // 去重
      for (const match of matches) {
        const cleanMatch = match.trim()
        if (cleanMatch && !found.includes(cleanMatch)) {
          found.push(cleanMatch)
        }
      }
    }
  }
  
  detectedSensitiveWords.value = found.slice(0, 5)  // 最多显示5个
  
  // 调试日志
  if (found.length > 0) {
    console.log('检测到敏感词:', found)
  }
}

// 清除敏感词内容
const clearSensitiveWords = () => {
  inputMessage.value = ''
  detectedSensitiveWords.value = []
}

// 计算属性
const filteredConversations = computed(() => {
  let result = conversations.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(c =>
      c.conversation?.name?.toLowerCase().includes(keyword) ||
      c.otherUserName?.toLowerCase().includes(keyword)
    )
  }

  if (activeTab.value === 'private') {
    result = result.filter(c => c.conversation?.type === 'private')
  } else if (activeTab.value === 'group') {
    result = result.filter(c => c.conversation?.type === 'group' || c.conversation?.type === 'temporary')
  }

  return result
})

// 成员分组 - 使用username首字母分组
const memberGroups = computed(() => {
  const groups = {}
  const users = allUsers.value.filter(u => u.id !== currentUserId.value)

  users.forEach(user => {
    // 按角色分组
    const role = user.role === 1 ? '管理员' : '用户'
    if (!groups[role]) {
      groups[role] = { dept: role, members: [], expanded: true }
    }
    groups[role].members.push(user)
  })

  return Object.values(groups)
})

const filteredMemberGroups = computed(() => {
  if (!userSearchKeyword.value) return memberGroups.value

  const keyword = userSearchKeyword.value.toLowerCase()
  return memberGroups.value
    .map(group => ({
      ...group,
      members: group.members.filter(u =>
        (u.realName && u.realName.toLowerCase().includes(keyword)) ||
        (u.username && u.username.toLowerCase().includes(keyword)) ||
        (u.email && u.email.toLowerCase().includes(keyword))
      ),
      expanded: true
    }))
    .filter(group => group.members.length > 0)
})

const filteredGroupMembers = computed(() => {
  if (!memberSearchKeyword.value) return allUsers.value.filter(u => u.id !== currentUserId.value)

  const keyword = memberSearchKeyword.value.toLowerCase()
  return allUsers.value.filter(u =>
    u.id !== currentUserId.value &&
    ((u.realName && u.realName.toLowerCase().includes(keyword)) ||
     (u.username && u.username.toLowerCase().includes(keyword)))
  )
})

const inputPlaceholder = computed(() => {
  if (detectedSensitiveWords.value.length > 0) {
    return '请修改内容后再发送...'
  }
  if (inputMessage.value.startsWith('/')) return '输入 / 查看可用指令...'
  return '输入消息... (Ctrl+Enter 发送)'
})

// WebSocket/STOMP连接
let currentConvSubscription = null
let globalSubscription = null  // 全局消息订阅

const connectWebSocket = () => {
  if (isConnected()) {
    console.log('STOMP already connected')
    wsConnected.value = true
    return
  }

  // 使用 http/https 协议，SockJS 会自动处理 WebSocket 升级
  const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'

  connect({
    url: `${protocol}//${window.location.host}/ws/chat`,
    onConnect: () => {
      console.log('STOMP connected')
      wsConnected.value = true
      // 连接成功后，订阅全局消息通知
      subscribeGlobalNotifications()
    },
    onDisconnect: () => {
      console.log('STOMP disconnected')
      wsConnected.value = false
    },
    onError: (error) => {
      console.error('STOMP error:', error)
      wsConnected.value = false
    }
  })
}

// 订阅全局消息通知（用于更新会话列表）
const subscribeGlobalNotifications = () => {
  import('../../utils/stomp.js').then(({ subscribe }) => {
    // 订阅所有会话更新通知（用户级别）
    globalSubscription = subscribe('/topic/conversations/updates', (notification) => {
      console.log('收到会话更新通知:', notification)
      // 处理会话列表更新通知
      if (notification.type === 'conversation_update') {
        // 检查是否需要刷新会话列表
        // 只有当通知包含当前用户相关的信息时才刷新
        // 这里直接刷新会话列表以确保数据最新
        loadConversationsSilent()
      }
    })
  })
}

// 静默刷新会话列表（不显示loading状态）
const loadConversationsSilent = async () => {
  try {
    const res = await apiGet(`/chat/conversations?userId=${currentUserId.value}`)
    if (res.code === 0) {
      // 保留未读数状态，只更新会话数据
      const oldUnreadMap = new Map()
      conversations.value.forEach(c => {
        if (c.conversation?.id) {
          oldUnreadMap.set(c.conversation.id, c.unreadCount)
        }
      })
      
      conversations.value = res.data || []
      
      // 恢复未读数状态（只恢复非当前会话的未读数）
      conversations.value.forEach(c => {
        const convId = c.conversation?.id
        if (convId && convId !== currentConvId.value) {
          c.unreadCount = oldUnreadMap.get(convId) || 0
        }
      })
      
      // 更新全局未读数
      updateGlobalUnreadCount()
    }
  } catch (error) {
    console.error('静默刷新会话失败:', error)
  }
}

const subscribeToConversation = (conversationId) => {
  // 取消之前的订阅
  if (currentConvSubscription) {
    unsubscribe(currentConvSubscription)
    currentConvSubscription = null
  }
  
  // 订阅新会话
  currentConvSubscription = subscribeConversation(conversationId, (message) => {
    handleNewMessage(message)
  })
}

const handleNewMessage = (msg) => {
  // 统一处理：支持新旧两种消息格式
  // 新格式: { type, message, conversationId }
  // 旧格式: 直接是 ChatMessage 对象
  let message
  let conversationId
  
  if (msg.message) {
    // 新格式
    message = msg.message
    conversationId = msg.conversationId || msg.message.conversationId
  } else {
    // 旧格式，直接是消息对象
    message = msg
    conversationId = msg.conversationId
  }

  // 如果是当前会话的消息，添加到列表
  if (message.conversationId === currentConvId.value) {
    // 检查是否已经存在（避免重复）
    const exists = messages.value.some(m => m.id === message.id)
    if (!exists) {
      // 检查是否是本地发送的消息（发送者是自己）
      // 如果是发送者自己发的消息，且本地已经有这条消息，则不重复添加
      const localExists = messages.value.some(m =>
        m.content === message.content &&
        m.senderId === message.senderId &&
        (m.id + '').startsWith('temp_')
      )
      if (!localExists) {
        messages.value.push(message)
      }
      // 按时间正序排列
      messages.value.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
      scrollToBottom()
    }
    // 注意：不在这里自动标记已读，已读应该在用户选择会话时或点击已读按钮时处理
  } else {
    // 非当前会话的消息才增加未读数（只有他人发消息才显示红点）
    const conv = conversations.value.find(c => c.conversation?.id === message.conversationId)
    if (conv) {
      conv.unreadCount = (conv.unreadCount || 0) + 1
      // 更新最后消息
      conv.conversation.lastMessageContent = message.content
      conv.conversation.lastMessageTime = message.createdAt
      // 如果会话不在列表顶部，移动到顶部
      moveConversationToTop(message.conversationId)
    }
    // 更新全局未读数
    dispatchChatUnreadUpdate()
  }
}

// 将会话移动到列表顶部
const moveConversationToTop = (conversationId) => {
  const convIndex = conversations.value.findIndex(c => c.conversation?.id === conversationId)
  if (convIndex > 0) {
    const conv = conversations.value.splice(convIndex, 1)[0]
    conversations.value.unshift(conv)
  }
}

const updateUnreadCount = (conversationId, count) => {
  const conv = conversations.value.find(c => c.conversation?.id === conversationId)
  if (conv) {
    conv.unreadCount = count
  }
}

// 派发全局未读数更新事件
const dispatchChatUnreadUpdate = () => {
  // 计算当前所有会话的未读数总和
  const totalUnread = conversations.value.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
  // 发送到全局
  window.dispatchEvent(new CustomEvent('chatUnreadUpdated', {
    detail: { unread: totalUnread }
  }))
  // 同时发送到 SystemLayout
  window.dispatchEvent(new CustomEvent('syncChatUnread', {
    detail: { unread: totalUnread }
  }))
}

// 更新全局未读数显示
const updateGlobalUnreadCount = () => {
  dispatchChatUnreadUpdate()
}

// 方法
const loadAllUsers = async () => {
  userLoading.value = true
  try {
    const res = await apiGet('/user')
    if (res.code === 0) {
      allUsers.value = res.data || []
      console.log('Loaded users:', allUsers.value.length)
    } else {
      console.error('Failed to load users:', res.message)
      allUsers.value = []
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    allUsers.value = []
  }
  userLoading.value = false
}

const loadConversations = async () => {
  loading.value = true
  try {
    const res = await apiGet(`/chat/conversations?userId=${currentUserId.value}`)
    if (res.code === 0) {
      conversations.value = res.data || []
      // 同步全局未读数
      updateGlobalUnreadCount()
    } else {
      conversations.value = []
    }
  } catch (error) {
    console.error('加载会话失败:', error)
    conversations.value = []
  }
  loading.value = false
}

const loadMessages = async () => {
  if (!currentConvId.value) return

  loadingMessages.value = true
  try {
    const res = await apiGet(`/chat/${currentConvId.value}/messages?page=0&size=50`)
    if (res.code === 0) {
      messages.value = res.data || []
      // 按时间正序排列（最新消息在下面）
      messages.value.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
      // 滚动到底部
      nextTick(() => scrollToBottom())
    } else {
      messages.value = []
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    messages.value = []
  }
  loadingMessages.value = false
}

const selectConversation = (conv) => {
  // 发送离开消息
  if (currentConvId.value && socket.value) {
    socket.value.send(JSON.stringify({
      type: 'leave',
      conversationId: currentConvId.value,
      userId: currentUserId.value
    }))
  }

  const previousConvId = currentConvId.value
  currentConvId.value = conv.conversation?.id
  currentConversation.value = conv.conversation
  otherUserName.value = conv.otherUserName || ''

  // 订阅新会话
  subscribeToConversation(conv.conversation?.id)

  // 清零当前会话未读数（立即更新UI）
  if (conv.unreadCount > 0) {
    conv.unreadCount = 0
    // 异步调用后端标记已读
    markAsRead(conv.conversation?.id)
    // 同步更新全局未读数
    updateGlobalUnreadCount()
  }

  loadMessages()
}

const selectUser = (user) => {
  selectedUserId.value = user.id
  selectedUser.value = user
}

const clearSelection = () => {
  selectedUserId.value = null
  selectedUser.value = null
}

const toggleGroupMember = (userId) => {
  const index = selectedMembers.value.indexOf(userId)
  if (index > -1) {
    selectedMembers.value.splice(index, 1)
  } else {
    selectedMembers.value.push(userId)
  }
}

// 打开新建会话对话框
const openNewChatDialog = async () => {
  userSearchKeyword.value = ''
  showNewChat.value = true
  // 刷新用户列表
  if (allUsers.value.length === 0) {
    await loadAllUsers()
  }
}

const markAsRead = async (conversationId) => {
  if (!conversationId) return
  try {
    const res = await apiPost('/chat/mark-read', { conversationId, userId: currentUserId.value })
    if (res.code === 0) {
      // 无论后端返回什么值，都确保未读数为0（因为用户已经看到这条消息了）
      const conv = conversations.value.find(c => c.conversation?.id === conversationId)
      if (conv) {
        conv.unreadCount = 0
      }
      // 同步全局未读数
      updateGlobalUnreadCount()
    }
  } catch (e) {
    console.error('标记已读失败:', e)
  }
}

// 发送消息 - 本地添加后立即显示
const sendMessage = async () => {
  // 清理输入内容（移除首尾空格，但保留中间的内容）
  const cleanContent = inputMessage.value.trim()
  
  if (!cleanContent && !selectedFile.value) return

  if (cleanContent.startsWith('/')) {
    await executeCommand(cleanContent)
    inputMessage.value = ''
    detectedSensitiveWords.value = []  // 清除敏感词提示
    return
  }

  // 如果有敏感词，不允许发送
  if (detectedSensitiveWords.value.length > 0) {
    ElMessage.warning('请先修改敏感词内容后再发送')
    return
  }

  // 检查是否有待创建的群组
  if (pendingGroup.value && !currentConvId.value) {
    try {
      const res = await apiPost('/chat/conversation/group', {
        ownerId: pendingGroup.value.ownerId,
        name: pendingGroup.value.name,
        description: pendingGroup.value.description,
        memberIds: pendingGroup.value.memberIds
      })

      if (res.code === 0) {
        // 创建成功，更新会话ID
        currentConvId.value = res.data.id
        currentConversation.value = res.data

        // 添加到会话列表
        const newConv = {
          conversation: res.data,
          otherUserName: pendingGroup.value.name,
          unreadCount: 0
        }
        conversations.value.unshift(newConv)

        // 订阅新会话
        subscribeToConversation(res.data.id)

        // 清空待创建状态
        pendingGroup.value = null

        ElMessage.success('群聊已创建')
      }
    } catch (error) {
      ElMessage.error('创建群聊失败')
      return
    }
  }

  if (!currentConvId.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }

  // 先本地添加消息，立即显示（使用清理后的内容）
  const tempMessage = {
    id: 'temp_' + Date.now(),  // 使用临时ID避免与后端返回的ID冲突
    conversationId: currentConvId.value,
    senderId: currentUserId.value,
    senderName: currentUserName.value,
    content: cleanContent,  // 使用清理后的内容
    type: 'text',
    createdAt: new Date().toISOString(),
    status: 'sending'
  }
  messages.value.push(tempMessage)
  // 按时间正序排列（最新消息在下面）
  messages.value.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
  const inputText = cleanContent  // 保存清理后的内容
  inputMessage.value = ''
  scrollToBottom()

  sending.value = true
  try {
    let res
    if (selectedFile.value) {
      // 文件发送
      const formData = new FormData()
      formData.append('file', selectedFile.value)
      formData.append('conversationId', currentConvId.value)
      formData.append('senderId', currentUserId.value)
      formData.append('senderName', currentUserName.value)
      formData.append('content', inputText || selectedFile.value.name)

      const token = localStorage.getItem('token')
      const response = await fetch('/api/chat/send/file', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData
      })
      res = await response.json()
      selectedFile.value = null
    } else {
      // 文本消息
      res = await apiPost('/chat/send', {
        conversationId: currentConvId.value,
        senderId: currentUserId.value,
        senderName: currentUserName.value,
        content: inputText,
        type: 'text'
      })
    }

    if (res.code === 0) {
      // 替换临时消息为真实消息
      const index = messages.value.findIndex(m => m.id === tempMessage.id)
      if (index > -1) {
        // 确保消息唯一：如果后端返回的消息已存在，则删除临时消息
        const existsIndex = messages.value.findIndex(m => m.id === res.data.id && m.id !== tempMessage.id)
        if (existsIndex > -1) {
          messages.value.splice(index, 1)  // 删除临时消息
        } else {
          messages.value.splice(index, 1, res.data)  // 替换为真实消息
        }
      }
      // 按时间正序排列
      messages.value.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
      // 更新会话列表最后消息
      const conv = conversations.value.find(c => c.conversation?.id === currentConvId.value)
      if (conv) {
        conv.conversation.lastMessageContent = inputText
        conv.conversation.lastMessageTime = new Date().toISOString()
      }
    } else {
      ElMessage.error(res.message || '发送失败')
      // 标记发送失败
      const index = messages.value.findIndex(m => m.id === tempMessage.id)
      if (index > -1) {
        messages.value[index].status = 'failed'
      }
    }
  } catch (error) {
    ElMessage.error('发送失败，请检查网络')
    const index = messages.value.findIndex(m => m.id === tempMessage.id)
    if (index > -1) {
      messages.value[index].status = 'failed'
    }
  }
  sending.value = false
}

const executeCommand = async (cmd) => {
  try {
    const res = await apiPost('/chat/command', { command: cmd, userId: currentUserId.value })
    if (res.code === 0) {
      ElMessage.success(res.data?.result || '命令已执行')
    }
  } catch (error) {
    ElMessage.error('命令执行失败')
  }
}

const recallMessage = async (msg) => {
  try {
    await apiPost('/chat/recall', { messageId: msg.id, userId: currentUserId.value })
    msg.content = '[此消息已被撤回]'
    ElMessage.success('已撤回')
  } catch (error) {
    ElMessage.error('撤回失败')
  }
}

const copyMessage = (msg) => {
  navigator.clipboard.writeText(msg.content)
  ElMessage.success('已复制')
}

const generateSummary = async () => {
  if (!currentConvId.value) return
  try {
    const res = await apiPost('/chat/summary', { conversationId: currentConvId.value })
    if (res.code === 0) {
      ElMessage.success('已生成摘要')
    }
  } catch (error) {
    ElMessage.error('生成摘要失败')
  }
}

const refreshMessages = () => loadMessages()

// 直接与用户开始会话（点击用户即可发起）
const startChatWithUser = async (user) => {
  try {
    showNewChat.value = false

    // 先查找是否已存在与该用户的会话
    const existingConv = conversations.value.find(c =>
      c.conversation?.type === 'private' &&
      c.otherUserId === user.id
    )

    if (existingConv) {
      // 已存在会话，直接选中
      selectConversation(existingConv)
      ElMessage.success(`已与 ${user.realName || user.username} 开始会话`)
    } else {
      // 不存在，创建新会话
      const res = await apiPost('/chat/conversation/private', {
        userId1: currentUserId.value,
        userId2: user.id
      })

      if (res.code === 0) {
        await loadConversations()

        // 查找刚创建的会话并选中
        let newConv = conversations.value.find(c => c.conversation?.id === res.data.id)
        if (!newConv) {
          // 如果在后端返回的会话列表中没找到，手动构造一个
          newConv = {
            conversation: res.data,
            otherUserId: user.id,
            otherUserName: user.realName || user.username,
            unreadCount: 0
          }
          conversations.value.unshift(newConv)
        } else {
          // 确保 otherUserName 正确设置
          newConv.otherUserName = user.realName || user.username
          newConv.otherUserId = user.id
        }
        selectConversation(newConv)
        ElMessage.success(`已与 ${user.realName || user.username} 开始会话`)
      } else {
        ElMessage.error(res.message || '创建会话失败')
      }
    }
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

// 创建会话（保留原方法以兼容）
const createPrivateChat = async () => {
  if (!selectedUserId.value) return

  try {
    const res = await apiPost('/chat/conversation/private', {
      userId1: currentUserId.value,
      userId2: selectedUserId.value
    })

    if (res.code === 0) {
      showNewChat.value = false
      selectedUserId.value = null
      selectedUser.value = null
      userSearchKeyword.value = ''
      await loadConversations()

      const newConv = conversations.value.find(c => c.conversation?.id === res.data.id)
      if (newConv) selectConversation(newConv)
      ElMessage.success('会话已创建')
    }
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

const createGroup = async () => {
  if (!newGroupName.value || selectedMembers.value.length === 0) return

  try {
    showNewGroup.value = false

    // 暂存群信息（不立即创建会话）
    const tempGroup = {
      id: 'temp_' + Date.now(),
      name: newGroupName.value,
      description: newGroupDesc.value,
      memberIds: [...selectedMembers.value],
      ownerId: currentUserId.value,
      isPending: true  // 标记为待创建
    }

    // 清空表单
    newGroupName.value = ''
    newGroupDesc.value = ''
    selectedMembers.value = []
    memberSearchKeyword.value = ''

    // 保存到临时群组（用于发送消息时创建）
    pendingGroup.value = tempGroup

    // 提示用户可以开始发送消息了
    ElMessage.info('请在下方输入消息，发送后将自动创建群聊')

    // 聚焦到输入框
    nextTick(() => {
      messageInputRef.value?.focus()
    })
  } catch (error) {
    ElMessage.error('创建群聊失败')
  }
}

// RPA卡片
const sendTaskCard = () => {
  showRPAPanel.value = false
  ElMessage.info('请在任务管理中选择任务发送卡片')
}

const sendRobotCard = () => {
  showRPAPanel.value = false
  ElMessage.info('请在机器人管理中选择机器人发送卡片')
}

const sendLogCard = () => {
  showRPAPanel.value = false
  ElMessage.info('请在日志管理中选择日志发送卡片')
}

const sendAlertCard = () => {
  showRPAPanel.value = false
  ElMessage.info('请输入告警内容')
}

// 文件处理
const triggerFileUpload = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    selectedFile.value = file
  }
}

const removeFile = () => {
  selectedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 工具方法
const scrollToBottom = () => {
  nextTick(() => {
    if (messageAreaRef.value) {
      messageAreaRef.value.scrollTop = messageAreaRef.value.scrollHeight
    }
  })
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const getCardTitle = (cardType) => {
  const titles = { TASK: '任务卡片', ROBOT: '机器人卡片', FLOW: '流程卡片', LOG: '日志卡片', ALERT: '告警卡片' }
  return titles[cardType] || 'RPA卡片'
}

// 获取对方用户头像
const getOtherUserAvatar = (conv) => {
  if (!conv.otherUserAvatar) return null
  if (conv.otherUserAvatar.startsWith('http') || conv.otherUserAvatar.startsWith('/')) {
    return conv.otherUserAvatar
  }
  return '/' + conv.otherUserAvatar
}

// 生命周期
onMounted(async () => {
  await loadAllUsers()
  await loadConversations()
  connectWebSocket()
})

// 每次进入聊天中枢时刷新会话列表（获取最新未读状态）
onActivated(async () => {
  await loadConversations()
  // 同步全局未读数
  updateGlobalUnreadCount()
})

onUnmounted(() => {
  // 取消订阅
  if (currentConvSubscription) {
    unsubscribe(currentConvSubscription)
  }
  if (globalSubscription) {
    unsubscribe(globalSubscription)
  }
  // 断开STOMP连接
  disconnect()
})
</script>

<style scoped>
.chat-app {
  display: flex;
  height: calc(100vh - 60px);
  background: #f5f7fa;
  font-family: 'PingFang SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* 左侧边栏 */
.chat-sidebar {
  width: 320px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0,0,0,0.05);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.search-box {
  padding: 12px 16px;
}

.conversation-tabs {
  padding: 0 16px 12px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
  gap: 12px;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.conversation-item:hover {
  background: #f5f7fa;
}

.conversation-item.active {
  background: linear-gradient(90deg, #ecf5ff 0%, #f0f7ff 100%);
  border-left: 3px solid #409eff;
}

.conv-avatar {
  position: relative;
  margin-right: 12px;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: linear-gradient(135deg, #f56c6c, #e64a4a);
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 11px;
  min-width: 18px;
  text-align: center;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-name {
  font-weight: 500;
  color: #1a1a2e;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.conv-preview {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-meta {
  text-align: right;
  margin-left: 8px;
}

.conv-time {
  font-size: 11px;
  color: #ccc;
}

/* 主聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: white;
}

.chat-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.subtitle {
  font-size: 12px;
  color: #999;
}

.chat-status {
  flex: 1;
  display: flex;
  justify-content: center;
}

.ws-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #999;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
}

.ws-status.connected .status-dot {
  background: #67c23a;
}

.ws-status.connected {
  color: #67c23a;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

/* 消息区域 */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f8f9fa;
}

.loading-messages, .empty-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #ccc;
  gap: 16px;
}

.empty-icon {
  color: #dcdfe6;
}

.empty-hint {
  font-size: 13px;
  color: #c0c4cc;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 75%;
  position: relative;
}

.message-self {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-other {
  margin-right: auto;
}

.message-actions {
  position: absolute;
  top: 0;
  right: -30px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-item:hover .message-actions {
  opacity: 1;
}

.notice-message {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 8px 16px;
  background: rgba(0,0,0,0.03);
  border-radius: 16px;
  margin: 0 auto;
  width: 100%;
  max-width: 400px;
}

.msg-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.message-self .message-header {
  flex-direction: row-reverse;
}

.sender-name {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.message-time {
  font-size: 11px;
  color: #ccc;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  background: #ffffff;
  line-height: 1.6;
  word-break: break-word;
  position: relative;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.message-self .message-bubble {
  background: #95EC91;
  color: #1a1a1a;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.message-other .message-bubble {
  background: #ffffff;
}

.message-self .message-bubble::after {
  content: '';
  position: absolute;
  right: -10px;
  top: 12px;
  border: 6px solid transparent;
  border-left-color: #95EC91;
}

.message-other .message-bubble::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 12px;
  border: 6px solid transparent;
  border-right-color: #ffffff;
}

.rpa-card-wrapper {
  max-width: 320px;
}

.file-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

/* 输入区域 */
.input-area {
  border-top: 1px solid #f0f0f0;
  padding: 16px 24px;
  background: white;
}

.input-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.input-wrapper {
  margin-bottom: 12px;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-hint {
  font-size: 12px;
  color: #999;
}

/* 敏感词提示 */
.sensitive-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  margin-bottom: 12px;
  color: #f56c6c;
  font-size: 13px;
  animation: slideDown 0.3s ease-out;
}

.sensitive-warning .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.sensitive-warning span {
  flex: 1;
}

.sensitive-warning .el-button {
  padding: 2px 8px;
  font-size: 12px;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
}

.file-preview .remove-file {
  cursor: pointer;
  color: #909399;
}

.file-preview .remove-file:hover {
  color: #f56c6c;
}

/* 无会话状态 */
.no-conversation {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.welcome-card {
  text-align: center;
  max-width: 500px;
}

.welcome-icon {
  color: #dcdfe6;
  margin-bottom: 24px;
}

.welcome-card h2 {
  margin: 0 0 12px 0;
  color: #1a1a2e;
  font-size: 28px;
}

.welcome-card p {
  color: #999;
  margin-bottom: 32px;
  font-size: 16px;
}

.quick-start {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 40px;
}

.feature-list {
  text-align: left;
  background: white;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
}

.feature-list h4 {
  margin: 0 0 16px 0;
  color: #666;
  font-size: 15px;
}

.feature-list ul {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: #606266;
  font-size: 14px;
}

.feature-list li .el-icon {
  color: #409eff;
  font-size: 18px;
}

/* 快速发起会话面板 */
.quick-chat-panel {
  max-height: 450px;
  display: flex;
  flex-direction: column;
}

.quick-search {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.quick-user-list {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
  max-height: 350px;
}

.loading-users, .no-users {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #999;
  gap: 12px;
}

.quick-user-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.quick-user-item:hover {
  background: linear-gradient(90deg, #ecf5ff 0%, #f5f7ff 100%);
  transform: translateX(4px);
}

.quick-user-item:hover .quick-start-icon {
  opacity: 1;
  color: #409eff;
}

.quick-user-info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
}

.quick-user-name {
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
  font-size: 15px;
}

.quick-user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-user-email {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-start-icon {
  opacity: 0;
  color: #c0c4cc;
  transition: all 0.2s;
  font-size: 20px;
}

/* 成员选择面板 */
.member-select-panel {
  max-height: 500px;
}

.member-search {
  margin-bottom: 16px;
}

.member-groups {
  max-height: 350px;
  overflow-y: auto;
}

.member-group {
  margin-bottom: 8px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.group-header:hover {
  background: #e8e8e8;
}

.group-header .el-icon {
  transition: transform 0.3s;
}

.group-header .el-icon.expanded {
  transform: rotate(90deg);
}

.group-name {
  flex: 1;
  font-weight: 500;
  color: #303133;
}

.group-members {
  padding: 8px 0;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
}

.member-item:hover {
  background: #f5f7fa;
}

.member-item.selected {
  background: #ecf5ff;
}

.member-info {
  flex: 1;
  margin-left: 12px;
}

.member-name {
  font-weight: 500;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.member-role {
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-email {
  font-size: 12px;
  color: #999;
}

.member-check {
  width: 24px;
}

.no-results {
  text-align: center;
  padding: 40px;
  color: #999;
}

.no-results p {
  margin-top: 12px;
}

.selected-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #ecf5ff;
  border-radius: 8px;
  margin-top: 16px;
}

.preview-label {
  color: #409eff;
  font-size: 13px;
}

/* 群成员选择 */
.group-member-select {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.member-search-mini {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.group-member-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px;
}

.group-member-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.group-member-item:hover {
  background: #f5f7fa;
}

.group-member-item.selected {
  background: #ecf5ff;
}

.group-member-item .member-name {
  flex: 1;
  margin-left: 8px;
  font-size: 14px;
}

.selected-members {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-top: 1px solid #e8e8e8;
}

.selected-members .count {
  font-size: 13px;
  color: #606266;
}

/* 会话详情 */
.conv-info-content {
  padding: 8px 0;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-label {
  color: #999;
}

.info-item:last-child {
  border-bottom: none;
}

/* RPA卡片面板 */
.rpa-card-panel {
  padding: 0 16px;
}

.rpa-card-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.rpa-card-item:hover {
  background: #f0f2f5;
  transform: translateX(4px);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  margin-right: 16px;
}

.card-icon.task { background: linear-gradient(135deg, #409eff, #66b1ff); }
.card-icon.robot { background: linear-gradient(135deg, #67c23a, #95d475); }
.card-icon.log { background: linear-gradient(135deg, #e6a23c, #f3d19e); }
.card-icon.alert { background: linear-gradient(135deg, #f56c6c, #f78989); }

.card-info h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #1a1a2e;
}

.card-info p {
  margin: 0;
  font-size: 13px;
  color: #999;
}

/* RPA卡片样式 */
.rpa-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  color: white;
  font-weight: 600;
}

.card-TASK .card-header { background: linear-gradient(135deg, #409eff, #66b1ff); }
.card-ROBOT .card-header { background: linear-gradient(135deg, #67c23a, #95d475); }
.card-ALERT .card-header { background: linear-gradient(135deg, #f56c6c, #f78989); }

.card-body {
  padding: 16px;
}
</style>
