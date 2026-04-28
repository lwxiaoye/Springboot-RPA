-- ===============================================
-- RPA通知系统数据库更新脚本
-- 适用于已有数据库的增量更新
-- ===============================================

-- 1. 创建通知模板表（如果不存在）
CREATE TABLE IF NOT EXISTS `notification_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(50) NOT NULL,
    `type` VARCHAR(30) NOT NULL,
    `content` TEXT NOT NULL,
    `description` VARCHAR(500),
    `enabled` INT NOT NULL DEFAULT 1,
    `is_default` INT NOT NULL DEFAULT 0,
    `channels` VARCHAR(100) DEFAULT 'email',
    `subject_template` VARCHAR(200),
    `creator_id` BIGINT,
    `creator_name` VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME,
    `use_count` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 创建订阅审批表（如果不存在）
CREATE TABLE IF NOT EXISTS `subscription_approval` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `subscription_id` BIGINT NOT NULL,
    `subscription_name` VARCHAR(200),
    `applicant_id` BIGINT NOT NULL,
    `applicant_name` VARCHAR(100),
    `approver_id` BIGINT,
    `approver_name` VARCHAR(100),
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `remark` VARCHAR(500),
    `reason` VARCHAR(500),
    `approval_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `subscription_summary` VARCHAR(500),
    PRIMARY KEY (`id`),
    KEY `idx_subscription_id` (`subscription_id`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===============================================
-- 为 report_subscription 表添加缺失的字段
-- 使用存储过程确保字段不存在时才添加
-- ===============================================

-- 添加 schedule_type（如果不存在）
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'schedule_type');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `schedule_type` VARCHAR(20) DEFAULT ''fixed'' COMMENT ''推送时间类型''', 
              'SELECT ''schedule_type already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 fixed_time
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'fixed_time');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `fixed_time` VARCHAR(10) DEFAULT ''09:00'' COMMENT ''固定推送时间''', 
              'SELECT ''fixed_time already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 cron_expression
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'cron_expression');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `cron_expression` VARCHAR(100) COMMENT ''自定义Cron表达式''', 
              'SELECT ''cron_expression already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 weekdays
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'weekdays');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `weekdays` VARCHAR(20) COMMENT ''推送星期''', 
              'SELECT ''weekdays already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 months
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'months');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `months` VARCHAR(50) COMMENT ''推送月份''', 
              'SELECT ''months already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 month_days
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'month_days');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `month_days` VARCHAR(100) COMMENT ''推送日期''', 
              'SELECT ''month_days already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 timezone
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'timezone');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `timezone` VARCHAR(50) DEFAULT ''Asia/Shanghai'' COMMENT ''时区''', 
              'SELECT ''timezone already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 require_approval
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'require_approval');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `require_approval` INT DEFAULT 0 COMMENT ''是否需要审批''', 
              'SELECT ''require_approval already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 approval_status
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'approval_status');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `approval_status` VARCHAR(20) DEFAULT ''approved'' COMMENT ''审批状态''', 
              'SELECT ''approval_status already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 approver_id
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'approver_id');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `approver_id` BIGINT COMMENT ''审批人ID''', 
              'SELECT ''approver_id already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 approver_name
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'approver_name');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `approver_name` VARCHAR(100) COMMENT ''审批人姓名''', 
              'SELECT ''approver_name already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 approval_time
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'approval_time');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `approval_time` DATETIME COMMENT ''审批时间''', 
              'SELECT ''approval_time already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 approval_remark
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'approval_remark');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `approval_remark` VARCHAR(500) COMMENT ''审批备注''', 
              'SELECT ''approval_remark already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 source
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'source');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `source` VARCHAR(20) DEFAULT ''user'' COMMENT ''订阅来源''', 
              'SELECT ''source already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 sensitivity_level
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'sensitivity_level');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `sensitivity_level` VARCHAR(20) DEFAULT ''normal'' COMMENT ''敏感级别''', 
              'SELECT ''sensitivity_level already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 template_id
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'template_id');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `template_id` BIGINT COMMENT ''通知模板ID''', 
              'SELECT ''template_id already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 include_attachment
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'include_attachment');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `include_attachment` INT DEFAULT 1 COMMENT ''是否包含附件''', 
              'SELECT ''include_attachment already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 attachment_type
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'attachment_type');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `attachment_type` VARCHAR(20) DEFAULT ''log'' COMMENT ''附件类型''', 
              'SELECT ''attachment_type already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 success_count
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'success_count');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `success_count` INT DEFAULT 0 COMMENT ''发送成功次数''', 
              'SELECT ''success_count already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 failed_count
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'report_subscription' 
                      AND COLUMN_NAME = 'failed_count');
SET @sql = IF(@column_exists = 0, 
              'ALTER TABLE `report_subscription` ADD COLUMN `failed_count` INT DEFAULT 0 COMMENT ''发送失败次数''', 
              'SELECT ''failed_count already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===============================================
-- 初始化默认通知模板（忽略已存在的）
-- ===============================================

INSERT IGNORE INTO `notification_template` 
(`name`, `code`, `type`, `content`, `description`, `enabled`, `is_default`, `channels`, `subject_template`, `creator_name`) 
VALUES 
('任务完成通知', 'task_complete', 'task_complete', 
'<div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;"><div style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:20px;border-radius:10px 10px 0 0;color:white;text-align:center;"><h2 style="margin:0;">任务执行完成</h2></div><div style="background:#f8f9fa;padding:20px;border-radius:0 0 10px 10px;"><p>尊敬的用户，您的RPA任务已成功完成。</p><table style="width:100%;border-collapse:collapse;"><tr><td style="padding:10px;border:1px solid #ddd;"><strong>任务名称</strong></td><td style="padding:10px;border:1px solid #ddd;">${taskName}</td></tr><tr><td style="padding:10px;border:1px solid #ddd;"><strong>任务ID</strong></td><td style="padding:10px;border:1px solid #ddd;">${taskId}</td></tr><tr><td style="padding:10px;border:1px solid #ddd;"><strong>执行状态</strong></td><td style="padding:10px;border:1px solid #ddd;color:#67c23a;"><strong>成功</strong></td></tr></table></div></div>', 
'任务成功完成时发送的通知模板', 1, 1, 'email,dingtalk,wecom', '【RPA通知】任务已完成：${taskName}', 'system');

INSERT IGNORE INTO `notification_template` 
(`name`, `code`, `type`, `content`, `description`, `enabled`, `is_default`, `channels`, `subject_template`, `creator_name`) 
VALUES 
('任务失败通知', 'task_failed', 'task_failed', 
'<div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;"><div style="background:linear-gradient(135deg,#f56c6c 0%,#e64a19 100%);padding:20px;border-radius:10px 10px 0 0;color:white;text-align:center;"><h2 style="margin:0;">任务执行失败</h2></div><div style="background:#fff3e0;padding:20px;border-radius:0 0 10px 10px;"><p>尊敬的用户，您的RPA任务执行失败，请及时处理。</p><div style="background:#ffebee;padding:15px;border-radius:5px;border-left:4px solid #f56c6c;"><strong>错误信息：</strong><p>${errorMessage}</p></div></div></div>', 
'任务执行失败时发送的通知模板', 1, 1, 'email,dingtalk', '【RPA告警】任务执行失败：${taskName}', 'system');

INSERT IGNORE INTO `notification_template` 
(`name`, `code`, `type`, `content`, `description`, `enabled`, `is_default`, `channels`, `subject_template`, `creator_name`) 
VALUES 
('报表订阅通知', 'subscription', 'subscription', 
'<div style="font-family:Arial,sans-serif;max-width:700px;margin:0 auto;padding:20px;"><div style="background:linear-gradient(135deg,#409eff 0%,#66b1ff 100%);padding:20px;border-radius:10px 10px 0 0;color:white;"><h2 style="margin:0;">RPA报表订阅 - ${period}</h2></div><div style="background:#f8f9fa;padding:20px;border-radius:0 0 10px 10px;"><div style="display:grid;grid-template-columns:repeat(4,1fr);gap:15px;margin-bottom:20px;"><div style="background:white;padding:15px;border-radius:8px;text-align:center;"><div style="font-size:24px;font-weight:bold;color:#409eff;">${totalExecutions}</div><div>总执行次数</div></div><div style="background:white;padding:15px;border-radius:8px;text-align:center;"><div style="font-size:24px;font-weight:bold;color:#67c23a;">${successRate}%</div><div>成功率</div></div><div style="background:white;padding:15px;border-radius:8px;text-align:center;"><div style="font-size:24px;font-weight:bold;color:#e6a23c;">${totalData}</div><div>数据采集量</div></div><div style="background:white;padding:15px;border-radius:8px;text-align:center;"><div style="font-size:24px;font-weight:bold;color:#f56c6c;">${failedCount}</div><div>失败次数</div></div></div></div></div>', 
'报表订阅定时发送的通知模板', 1, 1, 'email', '【RPA报表】${period} - ${subscriptionName}', 'system');

INSERT IGNORE INTO `notification_template` 
(`name`, `code`, `type`, `content`, `description`, `enabled`, `is_default`, `channels`, `subject_template`, `creator_name`) 
VALUES 
('系统告警通知', 'alert', 'alert', 
'<div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;"><div style="background:linear-gradient(135deg,#ffc107 0%,#ff9800 100%);padding:20px;border-radius:10px 10px 0 0;color:white;text-align:center;"><h2 style="margin:0;">系统告警</h2></div><div style="background:#fff3cd;padding:20px;border-radius:0 0 10px 10px;"><p style="font-weight:bold;">级别：${level}</p><h3>${alertTitle}</h3><p>${alertContent}</p><p style="color:#666;font-size:12px;">发送时间：${alertTime}</p></div></div>', 
'系统告警时发送的通知模板', 1, 1, 'email,dingtalk', '【RPA告警-${level}】${alertTitle}', 'system');

SELECT '数据库更新完成！' AS Result;
