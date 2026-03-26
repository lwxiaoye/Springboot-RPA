-- ============================================
-- Spring Security 权限管理模块数据库表
-- ============================================

USE rpa_system;

-- 1. 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    type VARCHAR(20) DEFAULT 'menu' COMMENT '类型：menu, button, api',
    url VARCHAR(255) COMMENT 'URL路径',
    method VARCHAR(20) COMMENT 'HTTP方法',
    icon VARCHAR(100) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    status INT DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '角色名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(255) COMMENT '描述',
    status INT DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 初始化权限数据（使用 IGNORE 避免重复插入）
-- ============================================

-- 插入系统管理权限
INSERT IGNORE INTO sys_permission (id, name, code, type, url, sort, parent_id, status) VALUES
(1, '系统管理', 'SYSTEM', 'menu', '/system', 1, 0, 1),
(2, '个人信息', 'SYSTEM_PROFILE', 'menu', '/profile', 1, 1, 1),
(3, '用户管理', 'USER_MANAGE', 'menu', '/user', 2, 1, 1),
(4, '用户新增', 'USER_ADD', 'button', NULL, 3, 3, 1),
(5, '用户编辑', 'USER_EDIT', 'button', NULL, 4, 3, 1),
(6, '用户删除', 'USER_DELETE', 'button', NULL, 5, 3, 1),
(7, '角色管理', 'ROLE_MANAGE', 'menu', '/role', 6, 1, 1),
(8, '角色新增', 'ROLE_ADD', 'button', NULL, 7, 7, 1),
(9, '角色编辑', 'ROLE_EDIT', 'button', NULL, 8, 7, 1),
(10, '角色删除', 'ROLE_DELETE', 'button', NULL, 9, 7, 1),
(11, '资源管理', 'RESOURCE_MANAGE', 'menu', '/resource', 10, 1, 1);

-- 插入RPA运营权限
INSERT IGNORE INTO sys_permission (id, name, code, type, url, sort, parent_id, status) VALUES
(12, 'RPA运营', 'RPA', 'menu', '/rpa', 11, 0, 1),
(13, '任务管理', 'TASK_MANAGE', 'menu', '/tasks', 12, 12, 1),
(14, '任务新增', 'TASK_ADD', 'button', NULL, 13, 12, 1),
(15, '任务编辑', 'TASK_EDIT', 'button', NULL, 14, 12, 1),
(16, '任务删除', 'TASK_DELETE', 'button', NULL, 15, 12, 1),
(17, '任务执行', 'TASK_EXECUTE', 'button', NULL, 16, 12, 1),
(18, '机器人管理', 'ROBOT_MANAGE', 'menu', '/robots', 17, 12, 1),
(19, '流程管理', 'PROCESS_MANAGE', 'menu', '/processes', 18, 12, 1),
(20, '执行日志', 'LOG_MANAGE', 'menu', '/logs', 19, 12, 1);

-- 插入数据管理权限
INSERT IGNORE INTO sys_permission (id, name, code, type, url, sort, parent_id, status) VALUES
(21, '数据管理', 'DATA', 'menu', '/data', 20, 0, 1),
(22, '数据采集', 'DATA_COLLECT', 'menu', '/dataCollect', 21, 21, 1),
(23, '数据解析', 'DATA_PARSE', 'menu', '/dataParse', 22, 21, 1),
(24, '数据加工', 'DATA_PROCESS', 'menu', '/dataProcess', 23, 21, 1),
(25, '数据查询', 'DATA_QUERY', 'menu', '/dataQuery', 24, 21, 1);

-- ============================================
-- 初始化角色（使用 IGNORE 避免重复插入）
-- ============================================

INSERT IGNORE INTO sys_role (id, name, code, description, status) VALUES
(1, '系统管理员', 'ROLE_ADMIN', '拥有系统所有权限', 1),
(2, '运营人员', 'ROLE_OPERATOR', '负责日常运营操作', 1),
(3, '普通用户', 'ROLE_USER', '基础功能使用权限', 1);

-- ============================================
-- 角色权限关联（使用 IGNORE 避免重复插入）
-- ============================================

-- 先清空关联表再重新插入
DELETE FROM sys_role_permission;

-- 系统管理员拥有所有权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 运营人员权限（不含用户和角色管理）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE code NOT LIKE 'USER_%' AND code NOT LIKE 'ROLE_%' AND code NOT LIKE 'RESOURCE_%';

-- 普通用户权限（仅菜单权限）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE type = 'menu' AND code NOT LIKE 'USER_%' AND code NOT LIKE 'ROLE_%' AND code NOT LIKE 'RESOURCE_%';

SELECT '权限管理模块初始化完成！' AS Result;
