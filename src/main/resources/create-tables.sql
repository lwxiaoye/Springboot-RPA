-- ============================================
-- RPA系统补充数据库脚本
-- 用于创建缺失的表：credential, audit_log
-- ============================================

USE rpa_system;

-- ============================================
-- 1. 凭据表（如果不存在）
-- ============================================
CREATE TABLE IF NOT EXISTS credential (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '凭据名称（唯一标识）',
    type VARCHAR(50) COMMENT '凭据类型',
    username VARCHAR(100) COMMENT '用户名/账号',
    secret_value TEXT COMMENT '加密后的凭据值',
    secret_key TEXT COMMENT '额外密钥（加密）',
    url VARCHAR(500) COMMENT '关联URL',
    description VARCHAR(500) COMMENT '描述',
    tags VARCHAR(500) COMMENT '标签（JSON格式）',
    create_by BIGINT COMMENT '创建人ID',
    create_by_name VARCHAR(100) COMMENT '创建人名称',
    last_used_time DATETIME COMMENT '最后使用时间',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态（active-激活，inactive-停用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_type (type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭据表';

-- ============================================
-- 2. 审计日志表（如果不存在）
-- ============================================
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    module VARCHAR(50) NOT NULL COMMENT '操作模块',
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) COMMENT '操作对象类型',
    target_id BIGINT COMMENT '操作对象ID',
    target_name VARCHAR(200) COMMENT '操作对象名称',
    description VARCHAR(500) COMMENT '操作描述',
    risk_level VARCHAR(20) DEFAULT 'low' COMMENT '风险等级（low-低，medium-中，high-高）',
    user_id BIGINT COMMENT '操作人ID',
    user_name VARCHAR(100) COMMENT '操作人名称',
    ip VARCHAR(50) COMMENT '操作IP地址',
    request_params TEXT COMMENT '请求参数',
    response_result TEXT COMMENT '响应结果',
    status VARCHAR(20) DEFAULT 'success' COMMENT '执行状态（success-成功，failed-失败）',
    hash VARCHAR(64) COMMENT '哈希值（用于完整性验证）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_module (module),
    INDEX idx_user_id (user_id),
    INDEX idx_risk_level (risk_level),
    INDEX idx_create_time (create_time),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- ============================================
-- 3. 为 robot 表添加缺失字段（如果不存在）
-- ============================================
-- 注意：MySQL 的 ALTER TABLE 如果列已存在会报错，使用 IF NOT EXISTS 需要 MySQL 8.0+
-- 如果报错说明字段已存在，可以忽略

-- 添加IP地址字段
-- ALTER TABLE robot ADD COLUMN ip VARCHAR(50) DEFAULT NULL COMMENT '机器人IP地址';

-- 添加主机名字段
-- ALTER TABLE robot ADD COLUMN hostname VARCHAR(100) DEFAULT NULL COMMENT '机器人主机名';

-- 添加端口字段
-- ALTER TABLE robot ADD COLUMN port INT DEFAULT 8080 COMMENT '机器人端口';

-- 添加CPU使用率字段
-- ALTER TABLE robot ADD COLUMN cpu_usage INT DEFAULT 0 COMMENT 'CPU使用率';

-- 添加内存使用率字段
-- ALTER TABLE robot ADD COLUMN memory_usage INT DEFAULT 0 COMMENT '内存使用率';

-- 添加描述字段
-- ALTER TABLE robot ADD COLUMN description TEXT COMMENT '机器人描述';

-- 添加机器人分类字段
-- ALTER TABLE robot ADD COLUMN robot_category VARCHAR(50) DEFAULT 'GENERAL' COMMENT '机器人分类';

-- 添加机器人执行代码字段
-- ALTER TABLE robot ADD COLUMN robot_code TEXT COMMENT '机器人执行代码';

SELECT '补充数据库表创建完成！请检查上方的错误信息' AS Result;
