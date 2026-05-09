-- ================================================
-- 公告阅读记录表
-- 适用于 MySQL 数据库
-- ================================================

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
