-- 创建数据库
CREATE DATABASE IF NOT EXISTS rpa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rpa_system;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    role INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- RPA 流程表
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

-- 机器人表
CREATE TABLE IF NOT EXISTS robot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'idle',
    capabilities TEXT,
    last_heartbeat DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务表
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

-- 执行日志表
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

-- 资源表
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

-- 插入测试数据
INSERT INTO users (username, password, real_name, role) VALUES ('admin', 'admin123', '系统管理员', 1);
INSERT INTO users (username, password, real_name, role) VALUES ('user1', 'user123', '测试用户 1', 0);

-- 插入资源数据
INSERT INTO resource (name, code, type, url, icon, sort, status) VALUES 
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
