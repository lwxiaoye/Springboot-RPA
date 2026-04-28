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
    expire_time DATETIME COMMENT '过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭据表';

-- 添加过期时间字段（如果表已存在但没有该字段）
ALTER TABLE credential ADD COLUMN IF NOT EXISTS expire_time DATETIME COMMENT '过期时间' AFTER status;

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
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_module (module),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
