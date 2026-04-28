-- ===============================================
-- RPA协作中枢 - 聊天系统数据库表结构
-- ===============================================

-- 1. 会话表
CREATE TABLE IF NOT EXISTS `chat_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `type` VARCHAR(20) NOT NULL DEFAULT 'private' COMMENT '会话类型: private-单聊, group-群聊, temporary-临时群',
    `name` VARCHAR(200) DEFAULT NULL COMMENT '群名称(群聊时)',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '群头像',
    `owner_id` BIGINT DEFAULT NULL COMMENT '群主ID',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '群描述',
    `is_system` TINYINT(1) DEFAULT 0 COMMENT '是否系统会话',
    `is_archived` TINYINT(1) DEFAULT 0 COMMENT '是否已归档(临时群)',
    `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间',
    `member_count` INT DEFAULT 0 COMMENT '成员数量',
    `message_count` INT DEFAULT 0 COMMENT '消息数量',
    `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
    `last_message_content` VARCHAR(500) DEFAULT NULL COMMENT '最后消息摘要',
    `related_type` VARCHAR(50) DEFAULT NULL COMMENT '关联类型: task/robot/flow',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联ID',
    `created_by` BIGINT NOT NULL COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_archived` (`is_archived`),
    KEY `idx_last_message` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 2. 消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送人ID',
    `sender_name` VARCHAR(100) DEFAULT NULL COMMENT '发送人姓名',
    `sender_avatar` VARCHAR(500) DEFAULT NULL COMMENT '发送人头像',
    `type` VARCHAR(30) NOT NULL DEFAULT 'text' COMMENT '消息类型: text/image/file/link/rpa_card/notice',
    `content` TEXT COMMENT '消息内容',
    `card_type` VARCHAR(50) DEFAULT NULL COMMENT '卡片类型: TASK/ROBOT/FLOW/LOG/APPROVAL/ALERT',
    `card_data` JSON DEFAULT NULL COMMENT '卡片数据(JSON)',
    `is_recalled` TINYINT(1) DEFAULT 0 COMMENT '是否已撤回',
    `recalled_at` DATETIME DEFAULT NULL COMMENT '撤回时间',
    `recalled_by` BIGINT DEFAULT NULL COMMENT '撤回人',
    `is_pinned` TINYINT(1) DEFAULT 0 COMMENT '是否置顶(群公告)',
    `reply_to_id` BIGINT DEFAULT NULL COMMENT '回复的消息ID',
    `mentioned_users` VARCHAR(500) DEFAULT NULL COMMENT '@提及的用户ID列表',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation` (`conversation_id`),
    KEY `idx_sender` (`sender_id`),
    KEY `idx_type` (`type`),
    KEY `idx_created` (`created_at`),
    KEY `idx_card_type` (`card_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 3. 消息附件表
CREATE TABLE IF NOT EXISTS `chat_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '附件ID',
    `message_id` BIGINT NOT NULL COMMENT '消息ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    `is_rpa_related` TINYINT(1) DEFAULT 0 COMMENT '是否RPA相关文件(log/录屏)',
    `rpa_resource_type` VARCHAR(50) DEFAULT NULL COMMENT 'RPA资源类型: log/screenshot/recording/flow',
    `rpa_resource_id` BIGINT DEFAULT NULL COMMENT 'RPA资源ID',
    `upload_by` BIGINT NOT NULL COMMENT '上传人',
    `upload_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_message` (`message_id`),
    KEY `idx_rpa_resource` (`rpa_resource_type`, `rpa_resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息附件表';

-- 4. 会话参与者表
CREATE TABLE IF NOT EXISTS `chat_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '群昵称',
    `role` VARCHAR(20) DEFAULT 'member' COMMENT '角色: owner-群主, admin-管理员, member-成员',
    `is_muted` TINYINT(1) DEFAULT 0 COMMENT '是否被禁言',
    `is_pinned` TINYINT(1) DEFAULT 0 COMMENT '会话是否置顶',
    `is_archived` TINYINT(1) DEFAULT 0 COMMENT '会话是否归档',
    `last_read_time` DATETIME DEFAULT NULL COMMENT '最后已读时间',
    `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `left_at` DATETIME DEFAULT NULL COMMENT '离开时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conv_user` (`conversation_id`, `user_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话参与者表';

-- 5. 已读状态表
CREATE TABLE IF NOT EXISTS `chat_read_status` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `message_id` BIGINT NOT NULL COMMENT '消息ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `read_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_user` (`message_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息已读状态表';

-- 6. 聊天审计日志表(金融合规必需)
CREATE TABLE IF NOT EXISTS `chat_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `conversation_id` BIGINT DEFAULT NULL COMMENT '会话ID',
    `message_id` BIGINT DEFAULT NULL COMMENT '消息ID',
    `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
    `user_name` VARCHAR(100) DEFAULT NULL COMMENT '操作用户姓名',
    `user_ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `user_device` VARCHAR(200) DEFAULT NULL COMMENT '设备信息',
    `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `action_detail` VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
    `old_value` TEXT DEFAULT NULL COMMENT '旧值(修改前)',
    `new_value` TEXT DEFAULT NULL COMMENT '新值(修改后)',
    `related_type` VARCHAR(50) DEFAULT NULL COMMENT '关联类型: task/robot/flow',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联ID',
    `result` VARCHAR(20) DEFAULT 'success' COMMENT '结果: success/failed',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_conversation` (`conversation_id`),
    KEY `idx_action` (`action`),
    KEY `idx_related` (`related_type`, `related_id`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天审计日志表';

-- 7. 敏感词过滤表
CREATE TABLE IF NOT EXISTS `chat_sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `word` VARCHAR(100) NOT NULL COMMENT '敏感词',
    `word_pattern` VARCHAR(200) DEFAULT NULL COMMENT '正则表达式',
    `level` VARCHAR(20) DEFAULT 'warn' COMMENT '级别: warn-警告, block-拦截',
    `replacement` VARCHAR(100) DEFAULT '***' COMMENT '替换字符',
    `category` VARCHAR(50) DEFAULT 'general' COMMENT '分类',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_word` (`word`),
    KEY `idx_level` (`level`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- 8. 用户收藏联系人表
CREATE TABLE IF NOT EXISTS `chat_contact_favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `favorite_user_id` BIGINT NOT NULL COMMENT '收藏的用户ID',
    `group_name` VARCHAR(100) DEFAULT NULL COMMENT '分组名称',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_favorite` (`user_id`, `favorite_user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏联系人表';

-- 9. AI聊天记录表
CREATE TABLE IF NOT EXISTS `chat_ai_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_message` TEXT NOT NULL COMMENT '用户消息',
    `ai_response` TEXT COMMENT 'AI回复',
    `intent` VARCHAR(100) DEFAULT NULL COMMENT '识别的意图',
    `entities` JSON DEFAULT NULL COMMENT '识别的实体',
    `suggestions` JSON DEFAULT NULL COMMENT '建议的操作',
    `session_id` VARCHAR(100) DEFAULT NULL COMMENT '会话ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_session` (`session_id`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天记录表';

-- ===============================================
-- 初始化数据
-- ===============================================

-- 插入默认敏感词
INSERT IGNORE INTO `chat_sensitive_word` (`word`, `level`, `category`, `created_by`) VALUES
('银行卡', 'warn', 'financial', 1),
('身份证', 'warn', 'personal', 1),
('密码', 'block', 'security', 1),
('secret', 'block', 'security', 1),
('账号', 'warn', 'security', 1);

SELECT 'RPA协作中枢数据库表创建完成！' AS Result;
