-- ============================================
-- RPA系统数据库结构更新脚本
-- 请逐条执行以下SQL语句
-- ============================================

USE rpa_system;

-- 1. 为robot表添加新字段（逐条执行，如果已存在会报错，跳过即可）

-- 添加IP地址字段
ALTER TABLE robot ADD COLUMN ip VARCHAR(50) DEFAULT NULL COMMENT '机器人IP地址';

-- 添加主机名字段
ALTER TABLE robot ADD COLUMN hostname VARCHAR(100) DEFAULT NULL COMMENT '机器人主机名';

-- 添加端口字段
ALTER TABLE robot ADD COLUMN port INT DEFAULT 8080 COMMENT '机器人端口';

-- 添加CPU使用率字段
ALTER TABLE robot ADD COLUMN cpu_usage INT DEFAULT 0 COMMENT 'CPU使用率';

-- 添加内存使用率字段
ALTER TABLE robot ADD COLUMN memory_usage INT DEFAULT 0 COMMENT '内存使用率';

-- 添加描述字段
ALTER TABLE robot ADD COLUMN description TEXT COMMENT '机器人描述';

-- 2. 为rpa_process表添加新字段

-- 添加版本号字段
ALTER TABLE rpa_process ADD COLUMN version VARCHAR(20) DEFAULT '1.0.0' COMMENT '流程版本';

-- 添加关联任务数字段
ALTER TABLE rpa_process ADD COLUMN task_count INT DEFAULT 0 COMMENT '关联任务数';

-- 3. 为execution_log表添加新字段

-- 添加任务名字段
ALTER TABLE execution_log ADD COLUMN task_name VARCHAR(100) COMMENT '任务名称';

-- 添加机器人名字段
ALTER TABLE execution_log ADD COLUMN robot_name VARCHAR(100) COMMENT '机器人名称';

-- 添加开始时间字段
ALTER TABLE execution_log ADD COLUMN start_time DATETIME COMMENT '开始时间';

-- 添加结束时间字段
ALTER TABLE execution_log ADD COLUMN end_time DATETIME COMMENT '结束时间';

-- 添加耗时字段
ALTER TABLE execution_log ADD COLUMN duration VARCHAR(50) COMMENT '执行耗时';

-- 添加执行步骤JSON字段
ALTER TABLE execution_log ADD COLUMN steps TEXT COMMENT '执行步骤JSON';

-- ============================================
-- 插入示例数据（先检查是否已存在）
-- ============================================

-- 查看当前数据
SELECT '当前机器人数据:' AS '';
SELECT * FROM robot;

-- 插入示例机器人（如果不存在）
INSERT INTO robot (name, type, status, ip, hostname, port, cpu_usage, memory_usage, description)
SELECT * FROM (
    SELECT 'Robot-001' AS name, 'Desktop' AS type, 'active' AS status, '192.168.1.101' AS ip, 'WORKSTATION-01' AS hostname, 8080 AS port, 45 AS cpu_usage, 62 AS memory_usage, '桌面机器人，用于日常办公自动化' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM robot WHERE name = 'Robot-001');

INSERT INTO robot (name, type, status, ip, hostname, port, cpu_usage, memory_usage, description)
SELECT * FROM (
    SELECT 'Robot-002' AS name, 'Desktop' AS type, 'idle' AS status, '192.168.1.102' AS ip, 'WORKSTATION-02' AS hostname, 8080 AS port, 12 AS cpu_usage, 35 AS memory_usage, '桌面机器人，待机中' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM robot WHERE name = 'Robot-002');

INSERT INTO robot (name, type, status, ip, hostname, port, cpu_usage, memory_usage, description)
SELECT * FROM (
    SELECT 'Robot-003' AS name, 'Server' AS type, 'active' AS status, '192.168.1.103' AS ip, 'SERVER-01' AS hostname, 8080 AS port, 78 AS cpu_usage, 85 AS memory_usage, '服务器机器人，处理大批量任务' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM robot WHERE name = 'Robot-003');

INSERT INTO robot (name, type, status, ip, hostname, port, cpu_usage, memory_usage, description)
SELECT * FROM (
    SELECT 'Robot-004' AS name, 'Desktop' AS type, 'offline' AS status, '192.168.1.104' AS ip, 'WORKSTATION-03' AS hostname, 8080 AS port, 0 AS cpu_usage, 0 AS memory_usage, '桌面机器人，已离线' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM robot WHERE name = 'Robot-004');

-- 插入示例流程
INSERT INTO rpa_process (name, code, description, status, version, creator_id, creator_name, task_count)
SELECT * FROM (
    SELECT '客户信息录入流程' AS name, 'CUSTOMER_IMPORT' AS code, '从Excel导入客户信息到CRM系统' AS description, 'active' AS status, '1.0' AS version, 1 AS creator_id, '系统管理员' AS creator_name, 5 AS task_count
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM rpa_process WHERE code = 'CUSTOMER_IMPORT');

INSERT INTO rpa_process (name, code, description, status, version, creator_id, creator_name, task_count)
SELECT * FROM (
    SELECT '订单处理流程' AS name, 'ORDER_PROCESS' AS code, '自动处理订单，同步到ERP系统' AS description, 'active' AS status, '2.1' AS version, 1 AS creator_id, '系统管理员' AS creator_name, 8 AS task_count
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM rpa_process WHERE code = 'ORDER_PROCESS');

INSERT INTO rpa_process (name, code, description, status, version, creator_id, creator_name, task_count)
SELECT * FROM (
    SELECT '发票审核流程' AS name, 'INVOICE_APPROVAL' AS code, 'OCR识别发票信息，自动审核发票合规性' AS description, 'active' AS status, '1.2' AS version, 1 AS creator_id, '系统管理员' AS creator_name, 3 AS task_count
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM rpa_process WHERE code = 'INVOICE_APPROVAL');

INSERT INTO rpa_process (name, code, description, status, version, creator_id, creator_name, task_count)
SELECT * FROM (
    SELECT '数据采集流程' AS name, 'DATA_COLLECTION' AS code, '从多个数据源自动采集数据' AS description, 'active' AS status, '1.0' AS version, 1 AS creator_id, '系统管理员' AS creator_name, 4 AS task_count
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM rpa_process WHERE code = 'DATA_COLLECTION');

INSERT INTO rpa_process (name, code, description, status, version, creator_id, creator_name, task_count)
SELECT * FROM (
    SELECT '报表生成流程' AS name, 'REPORT_GENERATE' AS code, '自动生成日报、周报、月报' AS description, 'draft' AS status, '1.0' AS version, 1 AS creator_id, '系统管理员' AS creator_name, 2 AS task_count
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM rpa_process WHERE code = 'REPORT_GENERATE');

-- 插入示例任务
INSERT INTO task (name, category, priority, process_id, process_name, robot_id, robot_name, status, create_time)
SELECT * FROM (
    SELECT '数据同步任务' AS name, '数据处理' AS category, 'high' AS priority, 1 AS process_id, '客户信息录入流程' AS process_name, 1 AS robot_id, 'Robot-001' AS robot_name, 'running' AS status, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '数据同步任务');

INSERT INTO task (name, category, priority, process_id, process_name, robot_id, robot_name, status, create_time)
SELECT * FROM (
    SELECT '报表生成任务' AS name, '报表' AS category, 'normal' AS priority, 5 AS process_id, '报表生成流程' AS process_name, 2 AS robot_id, 'Robot-002' AS robot_name, 'pending' AS status, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '报表生成任务');

INSERT INTO task (name, category, priority, process_id, process_name, robot_id, robot_name, status, create_time)
SELECT * FROM (
    SELECT '订单处理任务' AS name, '订单' AS category, 'high' AS priority, 2 AS process_id, '订单处理流程' AS process_name, 1 AS robot_id, 'Robot-001' AS robot_name, 'completed' AS status, DATE_SUB(NOW(), INTERVAL 1 HOUR) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '订单处理任务');

INSERT INTO task (name, category, priority, process_id, process_name, robot_id, robot_name, status, create_time)
SELECT * FROM (
    SELECT '发票审核任务' AS name, '审核' AS category, 'normal' AS priority, 3 AS process_id, '发票审核流程' AS process_name, 3 AS robot_id, 'Robot-003' AS robot_name, 'failed' AS status, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '发票审核任务');

INSERT INTO task (name, category, priority, process_id, process_name, robot_id, robot_name, status, create_time)
SELECT * FROM (
    SELECT '数据采集任务' AS name, '采集' AS category, 'low' AS priority, 4 AS process_id, '数据采集流程' AS process_name, 2 AS robot_id, 'Robot-002' AS robot_name, 'pending' AS status, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '数据采集任务');

-- 插入示例执行日志
INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, end_time, duration, create_time)
SELECT * FROM (
    SELECT 1 AS task_id, 1 AS process_id, 1 AS robot_id, '数据同步任务' AS task_name, 'Robot-001' AS robot_name, '任务执行' AS action, 'success' AS status, '任务执行成功，处理了120条数据' AS message, NOW() AS start_time, DATE_ADD(NOW(), INTERVAL 15 MINUTE) AS end_time, '15分钟' AS duration, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '数据同步任务');

INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, duration, create_time)
SELECT * FROM (
    SELECT 2 AS task_id, 5 AS process_id, 2 AS robot_id, '报表生成任务' AS task_name, 'Robot-002' AS robot_name, '任务执行' AS action, 'running' AS status, '正在生成报表...' AS message, NOW() AS start_time, NULL AS duration, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '报表生成任务');

INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, end_time, duration, create_time)
SELECT * FROM (
    SELECT 3 AS task_id, 2 AS process_id, 1 AS robot_id, '订单处理任务' AS task_name, 'Robot-001' AS robot_name, '任务执行' AS action, 'success' AS status, '成功处理了35个订单' AS message, DATE_SUB(NOW(), INTERVAL 1 HOUR) AS start_time, DATE_SUB(NOW(), INTERVAL 45 MINUTE) AS end_time, '15分钟' AS duration, DATE_SUB(NOW(), INTERVAL 1 HOUR) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '订单处理任务');

INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, end_time, duration, create_time)
SELECT * FROM (
    SELECT 4 AS task_id, 3 AS process_id, 3 AS robot_id, '发票审核任务' AS task_name, 'Robot-003' AS robot_name, '任务执行' AS action, 'failed' AS status, '网络连接超时，请检查网络' AS message, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS start_time, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS end_time, '3分钟' AS duration, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '发票审核任务');

SELECT '数据库更新完成！' AS Result;
