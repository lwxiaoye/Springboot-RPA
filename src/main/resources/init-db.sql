-- RPA 系统数据库初始化脚本
-- 运行此脚本创建所有必要的表和初始数据

-- 创建数据库
CREATE DATABASE IF NOT EXISTS rpa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rpa_system;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    role INT NOT NULL DEFAULT 0,
    status INT DEFAULT 1,
    avatar VARCHAR(255),
    password_change_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 2. 权限表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) DEFAULT 'menu',
    url VARCHAR(255),
    method VARCHAR(20),
    icon VARCHAR(100),
    sort INT DEFAULT 0,
    parent_id BIGINT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 3. 角色表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 4. 角色权限关联表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 5. 数据采集配置表
-- ============================================
CREATE TABLE IF NOT EXISTS data_collect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    source_url VARCHAR(500) NOT NULL,
    source_type VARCHAR(50) DEFAULT 'web',
    selector_rules TEXT,
    headers TEXT,
    cookies TEXT,
    cron_expression VARCHAR(100),
    status INT DEFAULT 0,
    last_collect_time BIGINT,
    data_count INT DEFAULT 0,
    creator_id BIGINT,
    creator_name VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 6. 采集数据存储表
-- ============================================
CREATE TABLE IF NOT EXISTS collected_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collect_id BIGINT,
    collect_name VARCHAR(100),
    raw_data TEXT,
    parsed_data TEXT,
    data_type VARCHAR(50),
    source_url VARCHAR(500),
    parse_status INT DEFAULT 0,
    collect_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    parse_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 7. RPA 流程表
-- ============================================
CREATE TABLE IF NOT EXISTS rpa_process (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    status VARCHAR(20) DEFAULT 'active',
    steps TEXT,
    creator_id BIGINT,
    creator_name VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 8. 机器人表
-- ============================================
CREATE TABLE IF NOT EXISTS robot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'idle',
    capabilities TEXT,
    last_heartbeat DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 9. 任务表
-- ============================================
CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    priority VARCHAR(20) DEFAULT 'normal',
    process_id BIGINT,
    process_name VARCHAR(100),
    robot_id BIGINT,
    robot_name VARCHAR(100),
    assignee_id BIGINT,
    assignee_name VARCHAR(50),
    status VARCHAR(20) DEFAULT 'pending',
    input_data TEXT,
    result_data TEXT,
    error_message TEXT,
    start_time DATETIME,
    end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 10. 执行日志表
-- ============================================
CREATE TABLE IF NOT EXISTS execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT,
    process_id BIGINT,
    robot_id BIGINT,
    action VARCHAR(100),
    status VARCHAR(20),
    message TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 11. 资源表
-- ============================================
CREATE TABLE IF NOT EXISTS resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'menu',
    url VARCHAR(200),
    icon VARCHAR(50),
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 12. 数据解析配置表
-- ============================================
CREATE TABLE IF NOT EXISTS data_parse (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    collect_id VARCHAR(100),
    parse_type VARCHAR(50) DEFAULT 'json',
    parse_rules TEXT,
    output_format VARCHAR(50) DEFAULT 'json',
    status INT DEFAULT 0,
    success_count INT DEFAULT 0,
    fail_count INT DEFAULT 0,
    last_parse_time BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 13. 数据加工配置表
-- ============================================
CREATE TABLE IF NOT EXISTS data_process (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    source_ids VARCHAR(500),
    process_type VARCHAR(50) DEFAULT 'transform',
    process_rules TEXT,
    output_table VARCHAR(100),
    status INT DEFAULT 0,
    processed_count INT DEFAULT 0,
    last_process_time BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 14. 数据查询配置表
-- ============================================
CREATE TABLE IF NOT EXISTS data_query (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    source_table VARCHAR(100) DEFAULT 'collected_data',
    query_condition TEXT,
    query_columns TEXT,
    result_data TEXT,
    result_count INT DEFAULT 0,
    status INT DEFAULT 0,
    last_query_time BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 初始化测试数据
-- ============================================

-- 注意：密码应该使用 BCrypt 加密，这里使用明文是为了测试
-- 实际应用中请使用加密后的密码或通过应用初始化

-- 插入测试用户（密码是加密后的123456）
INSERT IGNORE INTO users (username, password, real_name, role, email, phone) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 1, 'admin@rpa.com', '13800138000');

-- 插入资源数据
INSERT IGNORE INTO resource (name, code, type, url, icon, sort, status) VALUES 
('RPA 运营管理', 'RPA_MANAGE', 'menu', '/rpa', 'Setting', 2, 1),
('流程列表', 'PROCESS_LIST', 'menu', '/process-definition', 'Document', 4, 1),
('机器人列表', 'ROBOT_LIST', 'menu', '/robot-list', 'Service', 5, 1),
('执行记录', 'PROCESS_EXECUTION', 'menu', '/process-execution', 'Clock', 6, 1),
('数据采集', 'DATA_COLLECTION', 'menu', '/data-collection', 'Download', 7, 1),
('数据解析', 'DATA_PARSING', 'menu', '/data-parsing', 'Document', 8, 1),
('系统管理', 'SYSTEM_MANAGE', 'menu', '/system', 'User', 3, 1),
('个人信息', 'USER_PROFILE', 'menu', '/user-profile', 'User', 1, 1),
('用户管理', 'USER_MANAGE', 'menu', '/user-management', 'User', 2, 1),
('角色管理', 'ROLE_MANAGE', 'menu', '/role-management', 'UserFilled', 3, 1),
('资源管理', 'RESOURCE_MANAGE', 'menu', '/resource-management', 'Menu', 4, 1);

-- 插入采集配置示例
INSERT IGNORE INTO data_collect (name, source_url, source_type, selector_rules, status, creator_name) VALUES
('新闻头条采集', 'https://news.163.com', 'web', '{"listSelector": "div.news_title"}', 0, '系统管理员'),
('天气数据采集', 'https://tianqi.qq.com', 'web', '{"listSelector": "div.weather"}', 0, '系统管理员');

SELECT '数据库初始化完成！' AS Result;
