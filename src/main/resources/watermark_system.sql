-- ================================================
-- 水印系统数据库初始化脚本
-- 适用于 MySQL 数据库
-- ================================================

-- 创建水印配置表
CREATE TABLE IF NOT EXISTS `watermark_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型',
    `sensitivity_level` VARCHAR(20) NOT NULL COMMENT '敏感级别：extreme, high, medium, low',
    `sensitivity_name` VARCHAR(50) COMMENT '敏感级别名称',
    `opacity` DECIMAL(5,2) DEFAULT 0.10 COMMENT '不透明度',
    `font_size` INT DEFAULT 14 COMMENT '字体大小',
    `font_family` VARCHAR(200) DEFAULT 'KaiTi, 楷体, Microsoft YaHei' COMMENT '字体',
    `color` VARCHAR(20) DEFAULT '#000000' COMMENT '水印颜色',
    `tile_width` INT DEFAULT 200 COMMENT '平铺宽度',
    `tile_height` INT DEFAULT 200 COMMENT '平铺高度',
    `rotation` INT DEFAULT -45 COMMENT '旋转角度',
    `random_offset` TINYINT(1) DEFAULT 1 COMMENT '随机偏移：0-否，1-是',
    `show_timestamp` TINYINT(1) DEFAULT 1 COMMENT '显示时间戳：0-否，1-是',
    `timestamp_format` VARCHAR(50) DEFAULT 'YYYY-MM-DD HH:mm' COMMENT '时间戳格式',
    `description` TEXT COMMENT '描述',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `create_by` BIGINT COMMENT '创建人ID',
    `create_by_name` VARCHAR(100) COMMENT '创建人名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT COMMENT '更新人ID',
    `update_by_name` VARCHAR(100) COMMENT '更新人名称',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_sensitivity_level` (`sensitivity_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水印配置表';

-- 创建水印临时关闭记录表
CREATE TABLE IF NOT EXISTS `watermark_temporary_disable` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_name` VARCHAR(100) NOT NULL COMMENT '用户名称',
    `reason` VARCHAR(500) NOT NULL COMMENT '关闭原因',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `scheduled_restore_time` DATETIME NOT NULL COMMENT '计划恢复时间',
    `actual_restore_time` DATETIME COMMENT '实际恢复时间',
    `duration_minutes` INT NOT NULL COMMENT '关闭时长（分钟）',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT '浏览器User-Agent',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-生效中，RESTORED-已恢复',
    `manual_restore` TINYINT(1) DEFAULT 0 COMMENT '是否手动恢复：0-否，1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_scheduled_restore_time` (`scheduled_restore_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水印临时关闭记录表';

-- 创建水印审计日志表
CREATE TABLE IF NOT EXISTS `watermark_audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型：WATERMARK_TAMPERING, WATERMARK_DISABLED, WATERMARK_ENABLED, SCREENSHOT_CAPTURED, PRINT_ACTION, EXPORT_WITH_WATERMARK',
    `user_id` BIGINT COMMENT '用户ID',
    `user_name` VARCHAR(100) COMMENT '用户名称',
    `page_url` VARCHAR(500) COMMENT '页面URL',
    `description` VARCHAR(500) COMMENT '描述',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT '浏览器User-Agent',
    `risk_level` VARCHAR(20) DEFAULT 'LOW' COMMENT '风险等级：LOW, MEDIUM, HIGH, CRITICAL',
    `status` VARCHAR(20) DEFAULT 'DETECTED' COMMENT '状态：DETECTED-已检测, BLOCKED-已阻止',
    `screenshot_method` VARCHAR(50) COMMENT '截图方式',
    `print_content_type` VARCHAR(50) COMMENT '打印内容类型',
    `export_file_type` VARCHAR(50) COMMENT '导出文件类型',
    `export_file_name` VARCHAR(200) COMMENT '导出文件名',
    `tamper_reason` VARCHAR(100) COMMENT '篡改原因',
    `tamper_details` TEXT COMMENT '篡改详情',
    `additional_data` TEXT COMMENT '附加数据（JSON格式）',
    `event_time` DATETIME COMMENT '事件时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_event_type` (`event_type`),
    INDEX `idx_risk_level` (`risk_level`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水印审计日志表';

-- 创建公告阅读记录表
CREATE TABLE IF NOT EXISTS `announcement_read_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `announcement_id` BIGINT NOT NULL COMMENT '公告ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_name` VARCHAR(100) NOT NULL COMMENT '用户名',
    `department` VARCHAR(100) COMMENT '用户部门',
    `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `read_time` DATETIME COMMENT '阅读时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_announcement_user` (`announcement_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_announcement_id` (`announcement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告阅读记录表';

-- ================================================
-- 插入默认水印配置
-- ================================================

INSERT INTO `watermark_config` (`config_type`, `sensitivity_level`, `sensitivity_name`, `opacity`, `font_size`, `font_family`, `color`, `tile_width`, `tile_height`, `rotation`, `random_offset`, `show_timestamp`, `timestamp_format`, `description`, `enabled`) VALUES
('LEVEL_EXTREME', 'extreme', '极高敏感', 0.12, 14, 'KaiTi, 楷体, Microsoft YaHei', '#000000', 150, 150, -45, 1, 1, 'YYYY-MM-DD HH:mm', '极高敏感页面：执行日志、审计日志、凭证中心、数据查询', 1),
('LEVEL_HIGH', 'high', '高敏感', 0.10, 14, 'KaiTi, 楷体, Microsoft YaHei', '#000000', 200, 200, -45, 1, 1, 'YYYY-MM-DD HH:mm', '高敏感页面：任务调度中心、机器人管理、队列管理、触发器管理、数据脱敏', 1),
('LEVEL_MEDIUM', 'medium', '中敏感', 0.07, 12, 'KaiTi, 楷体, Microsoft YaHei', '#000000', 250, 250, -45, 1, 1, 'YYYY-MM-DD HH:mm', '中敏感页面：流程仓库、脚本执行、分布式锁、报表分析', 1),
('LEVEL_LOW', 'low', '低敏感', 0.05, 12, 'KaiTi, 楷体, Microsoft YaHei', '#000000', 300, 300, -45, 1, 1, 'YYYY-MM-DD HH:mm', '低敏感页面：系统设置、通知管理', 1);

-- ================================================
-- 查询验证
-- ================================================
SELECT '水印配置总数' as 项目, COUNT(*) as 数量 FROM watermark_config
UNION ALL
SELECT '启用配置数', COUNT(*) FROM watermark_config WHERE enabled = 1
UNION ALL
SELECT '公告阅读记录数', COUNT(*) FROM announcement_read_record;
