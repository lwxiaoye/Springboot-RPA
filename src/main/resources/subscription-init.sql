-- ===============================================
-- RPA订阅系统数据库初始化脚本
-- 修复数据库表结构问题
-- 注意：此脚本仅在需要时手动执行，不要在应用启动时自动执行
-- ===============================================

-- 0. 确保使用正确的数据库
USE rpa;

-- 1. 检查 report_subscription 表是否存在 report_config 字段，如果没有则添加
SET @column_exists = 0;
SELECT COUNT(*) INTO @column_exists FROM INFORMATION_SCHEMA.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'report_subscription'
                      AND COLUMN_NAME = 'report_config';
IF @column_exists = 0 THEN
    -- 检查表是否存在
    SET @table_exists = 0;
    SELECT COUNT(*) INTO @table_exists FROM INFORMATION_SCHEMA.TABLES
                          WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'report_subscription';
    IF @table_exists > 0 THEN
        ALTER TABLE `report_subscription` ADD COLUMN `report_config` TEXT COMMENT '报表配置JSON' AFTER `report_type`;
    END IF;
END IF;

-- 2. 创建订阅发送日志表
CREATE TABLE IF NOT EXISTS `report_subscription_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `subscription_id` BIGINT NOT NULL COMMENT '订阅ID',
    `subscription_name` VARCHAR(200) COMMENT '订阅名称',
    `channel` VARCHAR(50) NOT NULL COMMENT '推送渠道',
    `recipient` VARCHAR(500) NOT NULL COMMENT '接收人',
    `status` VARCHAR(20) NOT NULL COMMENT '发送状态(success/failed)',
    `error_message` TEXT COMMENT '错误信息',
    `send_time` DATETIME NOT NULL COMMENT '发送时间',
    `report_content` TEXT COMMENT '报表内容',
    `attachment_path` VARCHAR(500) COMMENT '附件路径',
    PRIMARY KEY (`id`),
    KEY `idx_subscription_id` (`subscription_id`),
    KEY `idx_send_time` (`send_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅发送记录表';

-- 3. 创建通知模板表（如果不存在）
CREATE TABLE IF NOT EXISTS `notification_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `type` VARCHAR(30) NOT NULL COMMENT '模板类型',
    `content` TEXT NOT NULL COMMENT '模板内容(HTML)',
    `description` VARCHAR(500) COMMENT '模板描述',
    `enabled` INT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `is_default` INT NOT NULL DEFAULT 0 COMMENT '是否默认模板',
    `channels` VARCHAR(100) DEFAULT 'email' COMMENT '支持的渠道',
    `subject_template` VARCHAR(200) COMMENT '邮件主题模板',
    `creator_id` BIGINT COMMENT '创建人ID',
    `creator_name` VARCHAR(50) COMMENT '创建人姓名',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间',
    `use_count` INT DEFAULT 0 COMMENT '使用次数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板表';

-- 4. 初始化默认通知模板
INSERT IGNORE INTO `notification_template`
(`name`, `code`, `type`, `content`, `description`, `enabled`, `is_default`, `channels`, `subject_template`, `creator_name`)
VALUES
('报表订阅通知', 'subscription', 'subscription',
'<div style="font-family:Arial,sans-serif;max-width:700px;margin:0 auto;padding:20px;"><div style="background:linear-gradient(135deg,#409eff 0%,#66b1ff 100%);padding:20px;border-radius:10px 10px 0 0;color:white;"><h2 style="margin:0;">RPA报表订阅 - ${period}</h2></div><div style="background:#f8f9fa;padding:20px;border-radius:0 0 10px 10px;"><div style="display:grid;grid-template-columns:repeat(4,1fr);gap:15px;margin-bottom:20px;"><div style="background:white;padding:15px;border-radius:8px;text-align:center;"><div style="font-size:24px;font-weight:bold;color:#409eff;">${totalExecutions}</div><div>总执行次数</div></div><div style="background:white;padding:15px;border-radius:8px;text-align:center;"><div style="font-size:24px;font-weight:bold;color:#67c23a;">${successRate}%</div><div>成功率</div></div><div style="background:white;padding:15px;border-radius:8px;text-align:center;"><div style="font-size:24px;font-weight:bold;color:#e6a23c;">${totalData}</div><div>数据采集量</div></div><div style="background:white;padding:15px;border-radius:8px;text-align:center;"><div style="font-size:24px;font-weight:bold;color:#f56c6c;">${failedCount}</div><div>失败次数</div></div></div></div></div>',
'报表订阅定时发送的通知模板', 1, 1, 'email', '【RPA报表】${period} - ${subscriptionName}', 'system');

SELECT '订阅系统数据库初始化完成！' AS Result;
