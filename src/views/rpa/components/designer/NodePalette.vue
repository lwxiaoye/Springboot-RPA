/**
 * RPA可视化流程设计器 - 节点面板组件
 * 
 * 功能说明：
 * - 展示分类的活动节点列表
 * - 支持拖拽节点到画布
 * - 节点分类：触发、浏览器、数据处理、文件操作、Excel、数据库、AI能力、逻辑控制
 * 
 * 节点图标说明：
 * - 🖱️ 鼠标操作 - click
 * - ⌨️ 键盘操作 - keyboard  
 * - 🌐 浏览器操作 - browser
 * - 📊 数据处理 - data
 * - 📁 文件操作 - file
 * - 📋 Excel操作 - excel
 * - 🗄️ 数据库 - database
 * - 🤖 AI能力 - ai
 * - 🔄 循环 - loop
 * - ⚡ 条件判断 - condition
 * - 🐍 Python脚本 - python
 * - 📧 邮件 - email
 * - 🔔 通知 - notification
 */

<template>
  <div class="node-palette">
    <div class="palette-header">
      <h3>活动组件</h3>
      <el-input v-model="searchText" placeholder="搜索组件..." size="small" clearable>
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>
    
    <div class="palette-content">
      <el-collapse v-model="activeCategories" accordion>
        <!-- 触发器分类 -->
        <el-collapse-item title="触发器" name="trigger">
          <template #title>
            <div class="category-title">
              <span class="category-icon">⏰</span>
              <span>触发器</span>
              <el-tag size="small" type="info">{{ getFilteredNodes('trigger').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('trigger')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 浏览器操作分类 -->
        <el-collapse-item title="浏览器自动化" name="browser">
          <template #title>
            <div class="category-title">
              <span class="category-icon">🌐</span>
              <span>浏览器自动化</span>
              <el-tag size="small" type="primary">{{ getFilteredNodes('browser').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('browser')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 桌面应用操作分类 -->
        <el-collapse-item title="桌面应用" name="desktop">
          <template #title>
            <div class="category-title">
              <span class="category-icon">🖥️</span>
              <span>桌面应用</span>
              <el-tag size="small" type="primary">{{ getFilteredNodes('desktop').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('desktop')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- Excel操作分类 -->
        <el-collapse-item title="Excel操作" name="excel">
          <template #title>
            <div class="category-title">
              <span class="category-icon">📊</span>
              <span>Excel操作</span>
              <el-tag size="small" type="success">{{ getFilteredNodes('excel').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('excel')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 文件操作分类 -->
        <el-collapse-item title="文件操作" name="file">
          <template #title>
            <div class="category-title">
              <span class="category-icon">📁</span>
              <span>文件操作</span>
              <el-tag size="small" type="success">{{ getFilteredNodes('file').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('file')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 数据库操作分类 -->
        <el-collapse-item title="数据库" name="database">
          <template #title>
            <div class="category-title">
              <span class="category-icon">🗄️</span>
              <span>数据库</span>
              <el-tag size="small" type="warning">{{ getFilteredNodes('database').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('database')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- AI能力分类 -->
        <el-collapse-item title="AI能力" name="ai">
          <template #title>
            <div class="category-title">
              <span class="category-icon">🤖</span>
              <span>AI能力</span>
              <el-tag size="small" type="danger">AI</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('ai')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 逻辑控制分类 -->
        <el-collapse-item title="逻辑控制" name="logic">
          <template #title>
            <div class="category-title">
              <span class="category-icon">⚡</span>
              <span>逻辑控制</span>
              <el-tag size="small" type="info">{{ getFilteredNodes('logic').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('logic')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 脚本代码分类 -->
        <el-collapse-item title="脚本代码" name="script">
          <template #title>
            <div class="category-title">
              <span class="category-icon">🐍</span>
              <span>脚本代码</span>
              <el-tag size="small" type="info">{{ getFilteredNodes('script').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('script')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 通信工具分类 -->
        <el-collapse-item title="通信工具" name="communication">
          <template #title>
            <div class="category-title">
              <span class="category-icon">📧</span>
              <span>通信工具</span>
              <el-tag size="small" type="info">{{ getFilteredNodes('communication').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('communication')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 图像识别分类 -->
        <el-collapse-item title="图像识别" name="vision">
          <template #title>
            <div class="category-title">
              <span class="category-icon">👁️</span>
              <span>图像识别</span>
              <el-tag size="small" type="warning">{{ getFilteredNodes('vision').length }}</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('vision')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 智能录制分类 -->
        <el-collapse-item title="智能录制" name="recorder">
          <template #title>
            <div class="category-title">
              <span class="category-icon">🎬</span>
              <span>智能录制</span>
              <el-tag size="small" type="success">NEW</el-tag>
            </div>
          </template>
          <div class="node-list">
            <div
              v-for="node in getFilteredNodes('recorder')"
              :key="node.type"
              class="palette-node"
              draggable="true"
              @dragstart="onDragStart($event, node)"
            >
              <span class="node-icon">{{ node.icon }}</span>
              <span class="node-name">{{ node.name }}</span>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div class="palette-footer">
      <div class="node-stats">
        <span>共 {{ totalNodes }} 个组件</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

const emit = defineEmits(['drag-start'])

const searchText = ref('')
const activeCategories = ref('trigger')

// 节点分类定义
const nodeCategories = {
  trigger: '触发器',
  browser: '浏览器自动化',
  desktop: '桌面应用',
  excel: 'Excel操作',
  file: '文件操作',
  database: '数据库',
  ai: 'AI能力',
  logic: '逻辑控制',
  script: '脚本代码',
  communication: '通信工具',
  vision: '图像识别',
  recorder: '智能录制'
}

// 活动节点定义
const allNodes = [
  // 触发器
  { type: 'trigger_schedule', name: '定时触发', icon: '⏰', category: 'trigger', description: '按照设定的时间规则触发流程' },
  { type: 'trigger_api', name: 'API触发', icon: '🔗', category: 'trigger', description: '通过API接口调用触发流程' },
  { type: 'trigger_queue', name: '队列触发', icon: '📬', category: 'trigger', description: '当队列有新任务时触发' },
  { type: 'trigger_file', name: '文件触发', icon: '📄', category: 'trigger', description: '当指定文件变化时触发' },
  { type: 'trigger_webhook', name: 'Webhook', icon: '🪝', category: 'trigger', description: '接收Webhook回调触发' },
  { type: 'trigger_mq', name: '消息队列', icon: '📨', category: 'trigger', description: '监听消息队列触发' },

  // 浏览器自动化
  { type: 'browser_open', name: '打开浏览器', icon: '🌐', category: 'browser', description: '启动Chrome/Edge/Firefox浏览器' },
  { type: 'browser_navigate', name: '跳转网页', icon: '🔀', category: 'browser', description: '导航到指定URL' },
  { type: 'browser_click', name: '点击元素', icon: '🖱️', category: 'browser', description: '点击页面上的元素' },
  { type: 'browser_input', name: '输入文本', icon: '⌨️', category: 'browser', description: '在输入框中输入文本' },
  { type: 'browser_extract', name: '提取数据', icon: '📥', category: 'browser', description: '从页面提取结构化数据' },
  { type: 'browser_wait', name: '等待元素', icon: '⏳', category: 'browser', description: '等待页面元素出现' },
  { type: 'browser_screenshot', name: '截图', icon: '📸', category: 'browser', description: '对当前页面截图' },
  { type: 'browser_close', name: '关闭浏览器', icon: '❌', category: 'browser', description: '关闭当前浏览器' },
  { type: 'browser_iframe', name: '切换Frame', icon: '📦', category: 'browser', description: '切换到iframe框架' },
  { type: 'browser_download', name: '下载文件', icon: '⬇️', category: 'browser', description: '下载页面中的文件' },

  // 桌面应用
  { type: 'desktop_open', name: '启动应用', icon: '🚀', category: 'desktop', description: '启动指定的桌面应用程序' },
  { type: 'desktop_click', name: '点击元素', icon: '🖱️', category: 'desktop', description: '点击桌面应用中的元素' },
  { type: 'desktop_input', name: '输入文本', icon: '⌨️', category: 'desktop', description: '向桌面应用输入文本' },
  { type: 'desktop_hotkey', name: '热键操作', icon: '🎹', category: 'desktop', description: '模拟键盘快捷键' },
  { type: 'desktop_window', name: '窗口操作', icon: '🪟', category: 'desktop', description: '最大化/最小化/关闭窗口' },

  // Excel操作
  { type: 'excel_open', name: '打开Excel', icon: '📂', category: 'excel', description: '打开指定的Excel文件' },
  { type: 'excel_read', name: '读取单元格', icon: '📖', category: 'excel', description: '读取Excel单元格数据' },
  { type: 'excel_write', name: '写入单元格', icon: '✍️', category: 'excel', description: '向单元格写入数据' },
  { type: 'excel_read_range', name: '读取区域', icon: '📜', category: 'excel', description: '读取指定范围的单元格' },
  { type: 'excel_write_range', name: '写入区域', icon: '📝', category: 'excel', description: '向指定范围写入数据' },
  { type: 'excel_append', name: '追加行', icon: '➕', category: 'excel', description: '在表格末尾追加新行' },
  { type: 'excel_formula', name: '公式计算', icon: '🔣', category: 'excel', description: '执行Excel公式计算' },
  { type: 'excel_filter', name: '筛选数据', icon: '🔍', category: 'excel', description: '根据条件筛选数据' },
  { type: 'excel_save', name: '保存Excel', icon: '💾', category: 'excel', description: '保存Excel文件' },
  { type: 'excel_close', name: '关闭Excel', icon: '❌', category: 'excel', description: '关闭Excel应用' },
  // Excel大文件支持
  { type: 'excel_stream_read', name: '流式读取', icon: '📑', category: 'excel', description: '流式读取大文件（百万行支持）' },
  { type: 'excel_stream_write', name: '流式写入', icon: '📤', category: 'excel', description: '流式写入大文件避免内存溢出' },
  { type: 'excel_template', name: '模板生成', icon: '📋', category: 'excel', description: '使用模板生成Excel文件' },
  { type: 'excel_search', name: '搜索数据', icon: '🔎', category: 'excel', description: '在Excel中搜索数据' },
  { type: 'excel_sheet', name: 'Sheet操作', icon: '📃', category: 'excel', description: '切换、创建、删除Sheet' },

  // 文件操作
  { type: 'file_read', name: '读取文件', icon: '📖', category: 'file', description: '读取文本文件内容' },
  { type: 'file_write', name: '写入文件', icon: '✍️', category: 'file', description: '向文件写入内容' },
  { type: 'file_copy', name: '复制文件', icon: '📋', category: 'file', description: '复制文件到指定位置' },
  { type: 'file_move', name: '移动文件', icon: '📦', category: 'file', description: '移动文件到指定位置' },
  { type: 'file_delete', name: '删除文件', icon: '🗑️', category: 'file', description: '删除指定的文件' },
  { type: 'file_rename', name: '重命名', icon: '✏️', category: 'file', description: '重命名文件' },
  { type: 'file_exists', name: '判断存在', icon: '❓', category: 'file', description: '检查文件是否存在' },
  { type: 'file_watch', name: '监控文件', icon: '👁️', category: 'file', description: '监控文件变化' },
  { type: 'file_compress', name: '压缩文件', icon: '📦', category: 'file', description: '压缩文件或文件夹' },
  { type: 'file_zip', name: '解压文件', icon: '📂', category: 'file', description: '解压ZIP压缩包' },

  // 数据库
  { type: 'db_connect', name: '连接数据库', icon: '🔌', category: 'database', description: '建立数据库连接' },
  { type: 'db_query', name: '查询数据', icon: '🔍', category: 'database', description: '执行SQL查询' },
  { type: 'db_execute', name: '执行SQL', icon: '⚡', category: 'database', description: '执行INSERT/UPDATE/DELETE' },
  { type: 'db_stored_proc', name: '存储过程', icon: '📦', category: 'database', description: '调用数据库存储过程' },
  { type: 'db_transaction', name: '事务处理', icon: '🔄', category: 'database', description: '开始/提交/回滚事务' },
  { type: 'db_disconnect', name: '断开连接', icon: '🔌', category: 'database', description: '关闭数据库连接' },

  // AI能力
  { type: 'ai_ocr', name: 'OCR识别', icon: '👁️', category: 'ai', description: '从图片中识别文字' },
  { type: 'ai_idcard', name: '身份证识别', icon: '🪪', category: 'ai', description: '识别身份证信息' },
  { type: 'ai_invoice', name: '发票识别', icon: '🧾', category: 'ai', description: '识别发票内容' },
  { type: 'ai_bankcard', name: '银行卡识别', icon: '💳', category: 'ai', description: '识别银行卡号' },
  { type: 'ai_captcha', name: '验证码识别', icon: '🔐', category: 'ai', description: '识别图片验证码' },
  { type: 'ai_table', name: '表格识别', icon: '📊', category: 'ai', description: '从图片中提取表格结构' },
  { type: 'ai_nlp', name: '文本NLP', icon: '📝', category: 'ai', description: '文本分析、情感分析' },
  { type: 'ai_translate', name: '翻译', icon: '🌍', category: 'ai', description: '文本翻译' },

  // 逻辑控制
  { type: 'logic_if', name: '条件分支', icon: '🔀', category: 'logic', description: '根据条件执行不同分支' },
  { type: 'logic_loop', name: '循环', icon: '🔁', category: 'logic', description: '循环执行一组操作' },
  { type: 'logic_foreach', name: '遍历', icon: '📋', category: 'logic', description: '遍历数组或集合' },
  { type: 'logic_break', name: '跳出循环', icon: '⏹️', category: 'logic', description: '提前结束循环' },
  { type: 'logic_continue', name: '继续下一个', icon: '▶️', category: 'logic', description: '跳过本次循环继续下一次' },
  { type: 'logic_try', name: '异常捕获', icon: '🛡️', category: 'logic', description: '捕获并处理异常' },
  { type: 'logic_switch', name: '多条件分支', icon: '🔢', category: 'logic', description: '多条件判断分支' },
  { type: 'logic_wait', name: '延时等待', icon: '⏰', category: 'logic', description: '等待指定时间' },

  // 脚本代码
  { type: 'script_python', name: 'Python脚本', icon: '🐍', category: 'script', description: '执行Python代码' },
  { type: 'script_js', name: 'JavaScript', icon: '📜', category: 'script', description: '执行JavaScript代码' },
  { type: 'script_http', name: 'HTTP请求', icon: '🌐', category: 'script', description: '发送HTTP请求' },
  { type: 'script_json', name: 'JSON解析', icon: '{ }', category: 'script', description: '解析和构建JSON' },
  { type: 'script_xml', name: 'XML解析', icon: '📄', category: 'script', description: '解析和构建XML' },
  { type: 'script_regex', name: '正则匹配', icon: '🔍', category: 'script', description: '使用正则表达式处理文本' },

  // 通信工具
  { type: 'email_send', name: '发送邮件', icon: '📧', category: 'communication', description: '发送电子邮件' },
  { type: 'email_read', name: '读取邮件', icon: '📩', category: 'communication', description: '读取邮箱中的邮件' },
  { type: 'email_attachment', name: '邮件附件', icon: '📎', category: 'communication', description: '处理邮件附件' },
  { type: 'dingtalk', name: '钉钉消息', icon: '💬', category: 'communication', description: '发送钉钉消息' },
  { type: 'wechat', name: '企业微信', icon: '💼', category: 'communication', description: '发送企业微信消息' },
  { type: 'ftp', name: 'FTP传输', icon: '📤', category: 'communication', description: 'FTP文件上传下载' },

  // 新增通用操作
  { type: 'general_log', name: '写入日志', icon: '📝', category: 'logic', description: '输出日志信息' },
  { type: 'general_variable', name: '变量赋值', icon: '📌', category: 'logic', description: '设置变量值' },
  { type: 'general_clipboard', name: '剪贴板', icon: '📋', category: 'logic', description: '读取或写入剪贴板' },
  { type: 'general_notify', name: '发送通知', icon: '🔔', category: 'communication', description: '发送系统通知' },

  // 图像识别
  { type: 'vision_find', name: '查找图像', icon: '🔍', category: 'vision', description: '在屏幕上查找模板图像' },
  { type: 'vision_click', name: '图像点击', icon: '🖱️', category: 'vision', description: '查找图像并点击' },
  { type: 'vision_wait', name: '等待图像', icon: '⏳', category: 'vision', description: '等待图像出现在屏幕上' },
  { type: 'vision_screenshot', name: '屏幕截图', icon: '📸', category: 'vision', description: '截取屏幕或区域图像' },
  { type: 'vision_capture', name: '截取模板', icon: '🎯', category: 'vision', description: '截取图像区域作为模板' },
  { type: 'vision_exists', name: '图像存在', icon: '❓', category: 'vision', description: '检查图像是否存在于屏幕' },

  // 智能录制
  { type: 'recorder_start', name: '开始录制', icon: '🎬', category: 'recorder', description: '开始录制用户操作' },
  { type: 'recorder_stop', name: '停止录制', icon: '⏹️', category: 'recorder', description: '停止录制并生成脚本' },
  { type: 'recorder_pause', name: '暂停录制', icon: '⏸️', category: 'recorder', description: '暂停录制操作' },
  { type: 'recorder_resume', name: '恢复录制', icon: '▶️', category: 'recorder', description: '恢复录制操作' },
  { type: 'recorder_anchor', name: '添加锚点', icon: '📍', category: 'recorder', description: '在录制中添加位置标注' },
  { type: 'recorder_import', name: '导入脚本', icon: '📥', category: 'recorder', description: '导入录制生成的脚本' }
]

// 计算属性
const totalNodes = computed(() => allNodes.length)

// 获取过滤后的节点
const getFilteredNodes = (category) => {
  let nodes = allNodes.filter(n => n.category === category)
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    nodes = nodes.filter(n => 
      n.name.toLowerCase().includes(keyword) ||
      n.description.toLowerCase().includes(keyword)
    )
  }
  return nodes
}

// 拖拽开始事件
const onDragStart = (event, node) => {
  event.dataTransfer.setData('application/json', JSON.stringify(node))
  event.dataTransfer.effectAllowed = 'copy'
  emit('drag-start', node)
}
</script>

<style scoped>
.node-palette {
  width: 260px;
  height: 100%;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.palette-header {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.palette-header h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.palette-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.category-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.category-icon {
  font-size: 16px;
}

.node-list {
  padding: 4px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.palette-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: grab;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.palette-node:hover {
  background: #ecf5ff;
  border-color: #409eff;
  transform: translateX(4px);
}

.palette-node:active {
  cursor: grabbing;
  opacity: 0.8;
}

.node-icon {
  font-size: 16px;
  width: 24px;
  text-align: center;
}

.node-name {
  font-size: 13px;
  color: #606266;
}

.palette-footer {
  padding: 12px 16px;
  border-top: 1px solid #e8e8e8;
  background: #fafafa;
}

.node-stats {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

/* 折叠面板样式覆盖 */
:deep(.el-collapse-item__header) {
  padding: 0 12px;
  font-size: 13px;
}

:deep(.el-collapse-item__content) {
  padding-bottom: 8px;
}

:deep(.el-collapse-item__wrap) {
  border-bottom: none;
}
</style>
