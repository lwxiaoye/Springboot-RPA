-- ===============================================
-- RPA通知系统数据库初始化脚本
-- 创建日期: 2024-01-01
-- ===============================================

-- 1. 通知模板表
CREATE TABLE IF NOT EXISTS `notification_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `type` VARCHAR(30) NOT NULL COMMENT '模板类型',
    `content` TEXT NOT NULL COMMENT '模板内容',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '模板描述',
    `enabled` INT NOT NULL DEFAULT 1 COMMENT '是否启用(0-禁用,1-启用)',
    `is_default` INT NOT NULL DEFAULT 0 COMMENT '是否默认模板(0-否,1-是)',
    `channels` VARCHAR(100) DEFAULT 'email' COMMENT '支持渠道',
    `subject_template` VARCHAR(200) DEFAULT NULL COMMENT '主题模板',
    `creator_id` BIGINT DEFAULT NULL COMMENT '创建者ID',
    `creator_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `use_count` INT DEFAULT 0 COMMENT '使用次数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';

-- 2. 订阅审批记录表
CREATE TABLE IF NOT EXISTS `subscription_approval` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `subscription_id` BIGINT NOT NULL COMMENT '订阅ID',
    `subscription_name` VARCHAR(200) DEFAULT NULL COMMENT '订阅名称',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(100) DEFAULT NULL COMMENT '申请人姓名',
    `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(100) DEFAULT NULL COMMENT '审批人姓名',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '审批状态',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '审批备注',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '申请原因',
    `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `subscription_summary` VARCHAR(500) DEFAULT NULL COMMENT '订阅内容摘要',
    PRIMARY KEY (`id`),
    KEY `idx_subscription_id` (`subscription_id`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅审批记录表';

-- ===============================================
-- 修改报表订阅表（添加新字段）- MySQL逐字段添加
-- ===============================================

-- 添加 schedule_type 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'schedule_type');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `schedule_type` VARCHAR(20) DEFAULT ''fixed'' COMMENT ''推送时间类型''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 fixed_time 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'fixed_time');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `fixed_time` VARCHAR(10) DEFAULT ''09:00'' COMMENT ''固定推送时间''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 cron_expression 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'cron_expression');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `cron_expression` VARCHAR(100) DEFAULT NULL COMMENT ''自定义Cron表达式''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 weekdays 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'weekdays');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `weekdays` VARCHAR(20) DEFAULT NULL COMMENT ''推送星期''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 months 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'months');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `months` VARCHAR(50) DEFAULT NULL COMMENT ''推送月份''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 month_days 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'month_days');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `month_days` VARCHAR(100) DEFAULT NULL COMMENT ''推送日期''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 timezone 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'timezone');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `timezone` VARCHAR(50) DEFAULT ''Asia/Shanghai'' COMMENT ''时区''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 require_approval 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'require_approval');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `require_approval` INT DEFAULT 0 COMMENT ''是否需要审批(0-否,1-是)''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 approval_status 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'approval_status');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `approval_status` VARCHAR(20) DEFAULT ''approved'' COMMENT ''审批状态''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 approver_id 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'approver_id');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `approver_id` BIGINT DEFAULT NULL COMMENT ''审批人ID''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 approver_name 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'approver_name');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `approver_name` VARCHAR(100) DEFAULT NULL COMMENT ''审批人姓名''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 approval_time 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'approval_time');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `approval_time` DATETIME DEFAULT NULL COMMENT ''审批时间''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 approval_remark 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'approval_remark');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `approval_remark` VARCHAR(500) DEFAULT NULL COMMENT ''审批备注''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 source 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'source');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `source` VARCHAR(20) DEFAULT ''user'' COMMENT ''订阅来源''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 sensitivity_level 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'sensitivity_level');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `sensitivity_level` VARCHAR(20) DEFAULT ''normal'' COMMENT ''敏感级别''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 template_id 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'template_id');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `template_id` BIGINT DEFAULT NULL COMMENT ''通知模板ID''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 include_attachment 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'include_attachment');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `include_attachment` INT DEFAULT 1 COMMENT ''是否包含附件(0-否,1-是)''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 attachment_type 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'attachment_type');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `attachment_type` VARCHAR(20) DEFAULT ''log'' COMMENT ''附件类型''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 success_count 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'success_count');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `success_count` INT DEFAULT 0 COMMENT ''发送成功次数''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 failed_count 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_subscription' AND COLUMN_NAME = 'failed_count');
SET @sqlstmt := IF(@exist = 0, 'ALTER TABLE `report_subscription` ADD COLUMN `failed_count` INT DEFAULT 0 COMMENT ''发送失败次数''', 'SELECT ''Column already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===============================================
-- 初始化默认通知模板
-- ===============================================

INSERT IGNORE INTO `notification_template` (`name`, `code`, `type`, `content`, `description`, `enabled`, `is_default`, `channels`, `subject_template`, `creator_name`) VALUES
('任务完成通知', 'task_complete', 'task_complete', 
'<!DOCTYPE html><html><head><meta charset="UTF-8"><style>body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;margin:0;padding:20px;background:#f5f7fa}.container{max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.08)}.header{background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:24px;color:white;text-align:center}.header h2{margin:0;font-size:20px;font-weight:600}.content{padding:24px}.info-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px}.info-item{background:#f8f9fa;padding:16px;border-radius:8px}.info-item label{display:block;color:#666;font-size:12px;margin-bottom:4px}.info-item .value{font-size:16px;font-weight:600;color:#333}.btn{display:inline-block;padding:12px 32px;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white;text-decoration:none;border-radius:8px;font-weight:600}.footer{padding:16px 24px;background:#f8f9fa;text-align:center;color:#999;font-size:12px}</style></head><body><div class="container"><div class="header"><h2>✅ 任务执行完成</h2></div><div class="content"><p>尊敬的用户，您的RPA任务已成功完成。</p><div class="info-grid"><div class="info-item"><label>任务名称</label><div class="value">${taskName}</div></div><div class="info-item"><label>任务ID</label><div class="value">${taskId}</div></div><div class="info-item"><label>开始时间</label><div class="value">${startTime}</div></div><div class="info-item"><label>完成时间</label><div class="value">${endTime}</div></div></div><p style="text-align:center;margin-top:24px;"><a href="${reportUrl}" class="btn">查看执行详情 →</a></p></div><div class="footer"><p>此邮件由RPA系统自动发送，请勿回复</p></div></div></body></html>', 
'任务成功完成时发送的通知模板', 1, 1, 'email,dingtalk,wecom', '【RPA通知】任务已完成：${taskName}', 'system'),

('任务失败通知', 'task_failed', 'task_failed', 
'<!DOCTYPE html><html><head><meta charset="UTF-8"><style>body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;margin:0;padding:20px;background:#f5f7fa}.container{max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.08)}.header{background:linear-gradient(135deg,#f56c6c 0%,#e64a19 100%);padding:24px;color:white;text-align:center}.header h2{margin:0;font-size:20px;font-weight:600}.content{padding:24px}.error-box{background:#ffebee;padding:16px;border-radius:8px;border-left:4px solid #dc3545;margin:16px 0}.error-box h4{margin:0 0 8px 0;color:#dc3545;font-size:14px}.error-box p{margin:0;color:#666;font-size:13px;line-height:1.6}.btn{display:inline-block;padding:12px 32px;background:linear-gradient(135deg,#f56c6c 0%,#e64a19 100%);color:white;text-decoration:none;border-radius:8px;font-weight:600}.footer{padding:16px 24px;background:#f8f9fa;text-align:center;color:#999;font-size:12px}</style></head><body><div class="container"><div class="header"><h2>⚠️ 任务执行失败</h2></div><div class="content"><p>尊敬的用户，您的RPA任务执行失败，请及时处理。</p><div class="error-box"><h4>错误信息</h4><p>${errorMessage}</p></div><p style="text-align:center;margin-top:24px;"><a href="${reportUrl}" class="btn">查看任务详情 →</a></p></div><div class="footer"><p>此邮件由RPA系统自动发送，请勿回复</p></div></div></body></html>', 
'任务执行失败时发送的通知模板', 1, 1, 'email,dingtalk', '【RPA告警】任务执行失败：${taskName}', 'system'),

('报表订阅通知', 'subscription', 'subscription', 
'<!DOCTYPE html><html><head><meta charset="UTF-8"><style>body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;margin:0;padding:20px;background:#f5f7fa}.container{max-width:700px;margin:0 auto;background:white;border-radius:12px;overflow:hidden}.header{background:linear-gradient(135deg,#409eff 0%,#66b1ff 100%);padding:24px;color:white}.header h2{margin:0;font-size:20px;font-weight:600}.content{padding:24px}.stats-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:24px}.stat-card{background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:20px;border-radius:10px;text-align:center;color:white}.stat-card.green{background:linear-gradient(135deg,#67c23a 0%,#95d475 100%)}.stat-card.orange{background:linear-gradient(135deg,#e6a23c 0%,#f3d19e 100%);color:#333}.stat-card.red{background:linear-gradient(135deg,#f56c6c 0%,#f78989 100%)}.stat-value{font-size:28px;font-weight:700}.stat-label{font-size:12px;opacity:.9;margin-top:4px}.btn{display:inline-block;padding:12px 32px;background:linear-gradient(135deg,#409eff 0%,#66b1ff 100%);color:white;text-decoration:none;border-radius:8px;font-weight:600}.footer{padding:16px 24px;background:#f8f9fa;text-align:center;color:#999;font-size:12px}</style></head><body><div class="container"><div class="header"><h2>📊 RPA报表订阅 - ${period}</h2></div><div class="content"><p><strong>报表类型：</strong>${reportType} | <strong>生成时间：</strong>${generateTime}</p><div class="stats-grid"><div class="stat-card"><div class="stat-value">${totalExecutions}</div><div class="stat-label">总执行次数</div></div><div class="stat-card green"><div class="stat-value">${successRate}%</div><div class="stat-label">成功率</div></div><div class="stat-card orange"><div class="stat-value">${totalData}</div><div class="stat-label">数据采集量</div></div><div class="stat-card red"><div class="stat-value">${failedCount}</div><div class="stat-label">失败次数</div></div></div><p style="text-align:center;margin-top:24px;"><a href="${reportUrl}" class="btn">查看完整报表 →</a></p></div><div class="footer"><p>此报表由RPA系统自动生成</p></div></div></body></html>', 
'报表订阅定时发送的通知模板', 1, 1, 'email', '【RPA报表】${period} - ${subscriptionName}', 'system'),

('系统告警通知', 'alert', 'alert', 
'<!DOCTYPE html><html><head><meta charset="UTF-8"><style>body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;margin:0;padding:20px;background:#f5f7fa}.container{max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden}.header{background:linear-gradient(135deg,#ffc107 0%,#ff9800 100%);padding:24px;color:white;text-align:center}.header.critical{background:linear-gradient(135deg,#dc3545 0%,#c82333 100%)}.header h2{margin:0;font-size:20px;font-weight:600}.content{padding:24px}.level-badge{display:inline-block;padding:6px 16px;border-radius:20px;font-size:12px;font-weight:600;margin-bottom:16px}.level-badge.info{background:#17a2b8;color:white}.level-badge.warning{background:#ffc107;color:#333}.level-badge.error{background:#dc3545;color:white}.alert-content{background:#fff3cd;padding:16px;border-radius:8px;border-left:4px solid #ffc107}.footer{padding:16px 24px;background:#f8f9fa;text-align:center;color:#999;font-size:12px}</style></head><body><div class="container"><div class="header"><h2>🚨 系统告警</h2></div><div class="content"><span class="level-badge warning">${level}</span><h3 style="margin:16px 0;">${alertTitle}</h3><div class="alert-content"><p>${alertContent}</p></div><p style="color:#666;font-size:12px;margin-top:16px;">发送时间：${alertTime}</p></div><div class="footer"><p>此告警由RPA系统自动发送，请及时处理</p></div></div></body></html>', 
'系统告警时发送的通知模板', 1, 1, 'email,dingtalk', '【RPA告警-${level}】${alertTitle}', 'system');
