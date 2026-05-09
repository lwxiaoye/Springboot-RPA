-- 智能修复 report_subscription 表结构
-- 使用条件判断，只添加缺失的字段

-- 1. 检查并添加 report_config 字段（如果不存在）
-- 使用 MySQL 兼容的条件检查
SET @column_exists = 0;
SELECT COUNT(*) INTO @column_exists FROM INFORMATION_SCHEMA.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'report_subscription'
                      AND COLUMN_NAME = 'report_config';
IF @column_exists = 0 THEN
    ALTER TABLE `report_subscription` ADD COLUMN `report_config` TEXT COMMENT '报表配置JSON' AFTER `report_type`;
END IF;

-- 2. 确保 notification_template 表存在
CREATE TABLE IF NOT EXISTS `notification_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `type` VARCHAR(30) NOT NULL COMMENT '模板类型',
    `content` TEXT NOT NULL COMMENT '模板内容',
    `description` VARCHAR(500) COMMENT '模板描述',
    `enabled` INT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `is_default` INT NOT NULL DEFAULT 0 COMMENT '是否默认',
    `channels` VARCHAR(100) DEFAULT 'email' COMMENT '支持渠道',
    `subject_template` VARCHAR(200) COMMENT '邮件主题模板',
    `creator_id` BIGINT COMMENT '创建人ID',
    `creator_name` VARCHAR(50) COMMENT '创建人姓名',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间',
    `use_count` INT DEFAULT 0 COMMENT '使用次数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板表';

-- 3. 确保 subscription_approval 表存在
CREATE TABLE IF NOT EXISTS `subscription_approval` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `subscription_id` BIGINT NOT NULL COMMENT '订阅ID',
    `subscription_name` VARCHAR(200) COMMENT '订阅名称',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(100) COMMENT '申请人姓名',
    `approver_id` BIGINT COMMENT '审批人ID',
    `approver_name` VARCHAR(100) COMMENT '审批人姓名',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态',
    `remark` VARCHAR(500) COMMENT '审批备注',
    `reason` VARCHAR(500) COMMENT '申请原因',
    `approval_time` DATETIME COMMENT '审批时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `subscription_summary` VARCHAR(500) COMMENT '订阅摘要',
    PRIMARY KEY (`id`),
    KEY `idx_subscription_id` (`subscription_id`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅审批表';

SELECT '数据库修复完成！所有缺失的字段已添加。' AS Result;
