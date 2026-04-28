-- ===============================================
-- RPA系统数据库修复脚本
-- 修复 report_subscription 表结构问题
-- ===============================================

-- 查看当前表结构
DESCRIBE report_subscription;

-- 修复 enabled 字段类型（如果是 BOOLEAN 改为 INT）
SET @column_type = (SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'report_subscription' 
                   AND COLUMN_NAME = 'enabled');
SELECT CONCAT('Current enabled type: ', @column_type) AS Status;

-- 如果 enabled 是 tinyint(1)，修改为 int
SET @sql = IF(@column_type LIKE '%tinyint%', 
              'ALTER TABLE `report_subscription` MODIFY COLUMN `enabled` INT NOT NULL DEFAULT 1',
              'SELECT ''enabled column type is OK''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 确保 create_user 字段允许 NULL 或有默认值
SET @column_type = (SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'report_subscription' 
                   AND COLUMN_NAME = 'create_user');
SELECT CONCAT('create_user nullable: ', @column_type) AS Status;

-- 查看缺少哪些必要字段
SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_DEFAULT, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'report_subscription'
ORDER BY ORDINAL_POSITION;

-- ===============================================
-- 添加缺失的字段（如果不存在）
-- ===============================================

-- 定义添加字段的存储过程
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_column_if_not_exists(
    IN table_name VARCHAR(64),
    IN column_name VARCHAR(64),
    IN column_def VARCHAR(255)
)
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO col_exists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = table_name
    AND COLUMN_NAME = column_name;
    
    IF col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', table_name, '` ADD COLUMN `', column_name, '` ', column_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Added column: ', column_name) AS Result;
    ELSE
        SELECT CONCAT('Column already exists: ', column_name) AS Result;
    END IF;
END //
DELIMITER ;

-- 执行添加缺失字段
CALL add_column_if_not_exists('report_subscription', 'schedule_type', "VARCHAR(20) DEFAULT 'fixed' COMMENT '推送时间类型'");
CALL add_column_if_not_exists('report_subscription', 'fixed_time', "VARCHAR(10) DEFAULT '09:00' COMMENT '固定推送时间'");
CALL add_column_if_not_exists('report_subscription', 'cron_expression', "VARCHAR(100) COMMENT '自定义Cron表达式'");
CALL add_column_if_not_exists('report_subscription', 'weekdays', "VARCHAR(20) COMMENT '推送星期'");
CALL add_column_if_not_exists('report_subscription', 'months', "VARCHAR(50) COMMENT '推送月份'");
CALL add_column_if_not_exists('report_subscription', 'month_days', "VARCHAR(100) COMMENT '推送日期'");
CALL add_column_if_not_exists('report_subscription', 'timezone', "VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '时区'");
CALL add_column_if_not_exists('report_subscription', 'require_approval', "INT DEFAULT 0 COMMENT '是否需要审批'");
CALL add_column_if_not_exists('report_subscription', 'approval_status', "VARCHAR(20) DEFAULT 'approved' COMMENT '审批状态'");
CALL add_column_if_not_exists('report_subscription', 'approver_id', "BIGINT COMMENT '审批人ID'");
CALL add_column_if_not_exists('report_subscription', 'approver_name', "VARCHAR(100) COMMENT '审批人姓名'");
CALL add_column_if_not_exists('report_subscription', 'approval_time', "DATETIME COMMENT '审批时间'");
CALL add_column_if_not_exists('report_subscription', 'approval_remark', "VARCHAR(500) COMMENT '审批备注'");
CALL add_column_if_not_exists('report_subscription', 'source', "VARCHAR(20) DEFAULT 'user' COMMENT '订阅来源'");
CALL add_column_if_not_exists('report_subscription', 'sensitivity_level', "VARCHAR(20) DEFAULT 'normal' COMMENT '敏感级别'");
CALL add_column_if_not_exists('report_subscription', 'template_id', "BIGINT COMMENT '通知模板ID'");
CALL add_column_if_not_exists('report_subscription', 'include_attachment', "INT DEFAULT 1 COMMENT '是否包含附件'");
CALL add_column_if_not_exists('report_subscription', 'attachment_type', "VARCHAR(20) DEFAULT 'log' COMMENT '附件类型'");
CALL add_column_if_not_exists('report_subscription', 'success_count', "INT DEFAULT 0 COMMENT '发送成功次数'");
CALL add_column_if_not_exists('report_subscription', 'failed_count', "INT DEFAULT 0 COMMENT '发送失败次数'");

-- 确保 create_user 和 create_time 可以接受 NULL 或有默认值
SET @sql = 'ALTER TABLE `report_subscription` MODIFY COLUMN `create_user` BIGINT NOT NULL DEFAULT 1';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE `report_subscription` MODIFY COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 最终表结构
SELECT '修复完成！当前表结构：' AS Status;
DESCRIBE report_subscription;
