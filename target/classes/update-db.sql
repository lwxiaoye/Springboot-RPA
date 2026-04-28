-- ============================================
-- RPA系统数据库结构更新脚本 V2
-- 新增：任务队列、触发器、审计日志、系统配置
-- ============================================

USE rpa_system;

-- ============================================
-- 0. 系统配置表
-- ============================================
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    category VARCHAR(50) NOT NULL COMMENT '配置分类（general/message/storage/ocr/llm/license）',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键名',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(200) COMMENT '配置描述',
    is_encrypted TINYINT(1) DEFAULT 0 COMMENT '是否加密存储',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    UNIQUE INDEX uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 1. 任务队列表
-- ============================================
CREATE TABLE IF NOT EXISTS task_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '队列名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '队列编码（唯一）',
    description VARCHAR(500) COMMENT '队列描述',
    status VARCHAR(20) DEFAULT 'active' COMMENT '队列状态（active-运行中，paused-暂停，stopped-已停止）',
    priority_level INT DEFAULT 2 COMMENT '优先级（1-低，2-普通，3-高，4-紧急）',
    max_concurrent_tasks INT DEFAULT 5 COMMENT '最大并发任务数',
    current_pending_count INT DEFAULT 0 COMMENT '当前排队任务数',
    current_running_count INT DEFAULT 0 COMMENT '当前执行任务数',
    completed_count BIGINT DEFAULT 0 COMMENT '已完成任务数',
    failed_count BIGINT DEFAULT 0 COMMENT '失败任务数',
    process_ids TEXT COMMENT '关联的流程ID（JSON格式数组）',
    process_names TEXT COMMENT '关联的流程名称（JSON格式数组）',
    required_categories VARCHAR(500) COMMENT '需要的机器人分类（JSON格式数组）',
    department VARCHAR(100) COMMENT '所属部门',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    remark TEXT COMMENT '备注',
    INDEX idx_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务队列表';

-- 插入默认队列
INSERT IGNORE INTO task_queue (name, code, description, status, priority_level, max_concurrent_tasks, required_categories, creator)
VALUES
    ('发票处理队列', 'INVOICE_QUEUE', '用于发票采集、审核、处理的自动化任务队列', 'active', 3, 10, '["DATA_COLLECT","DATA_PARSE","DATA_PROCESS"]', 'system'),
    ('数据采集队列', 'DATA_COLLECT_QUEUE', '用于各类数据采集任务的自动化队列', 'active', 2, 5, '["DATA_COLLECT"]', 'system'),
    ('通用执行队列', 'GENERAL_QUEUE', '通用自动化任务执行队列', 'active', 1, 8, '["GENERAL","DATA_COLLECT","DATA_PARSE","DATA_PROCESS"]', 'system');

-- ============================================
-- 2. 触发器规则表
-- ============================================
CREATE TABLE IF NOT EXISTS trigger_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '触发器名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '触发器编码（唯一）',
    description VARCHAR(500) COMMENT '触发器描述',
    trigger_type VARCHAR(20) NOT NULL COMMENT '触发器类型（schedule-定时，file-文件，api-API，webhook-Webhook）',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态（active-启用，paused-暂停，disabled-禁用）',
    process_id BIGINT COMMENT '关联的流程ID',
    process_name VARCHAR(200) COMMENT '关联的流程名称',
    process_code VARCHAR(100) COMMENT '关联的流程编码',
    queue_id BIGINT COMMENT '关联的队列ID',
    queue_name VARCHAR(200) COMMENT '关联的队列名称',
    cron VARCHAR(100) COMMENT 'Cron表达式（定时触发时使用）',
    schedule_type VARCHAR(20) COMMENT '触发周期类型（minute-分钟，hour-小时，day-天，week-周，month-月）',
    schedule_time VARCHAR(10) COMMENT '定时执行时间（HH:mm格式）',
    schedule_days VARCHAR(50) COMMENT '定时执行日期（周：1-7，月：1-31）',
    watch_path VARCHAR(500) COMMENT '监控目录（文件触发时使用）',
    file_pattern VARCHAR(200) COMMENT '文件匹配规则（glob模式）',
    watch_subdirs TINYINT(1) DEFAULT 0 COMMENT '是否监控子目录',
    api_key VARCHAR(200) COMMENT 'API密钥',
    webhook_url VARCHAR(500) COMMENT 'Webhook URL',
    http_method VARCHAR(10) DEFAULT 'POST' COMMENT '请求方法（POST/GET）',
    trigger_condition TEXT COMMENT '触发条件（JSON格式）',
    trigger_params TEXT COMMENT '触发参数（JSON格式）',
    auto_start TINYINT(1) DEFAULT 1 COMMENT '触发后是否自动启动',
    max_concurrent INT DEFAULT 1 COMMENT '最大并发触发数',
    total_triggers BIGINT DEFAULT 0 COMMENT '累计触发次数',
    success_triggers BIGINT DEFAULT 0 COMMENT '成功触发次数',
    failed_triggers BIGINT DEFAULT 0 COMMENT '失败触发次数',
    last_trigger_time DATETIME COMMENT '最后触发时间',
    last_success_time DATETIME COMMENT '最后成功时间',
    last_failed_time DATETIME COMMENT '最后失败时间',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    remark TEXT COMMENT '备注',
    INDEX idx_code (code),
    INDEX idx_trigger_type (trigger_type),
    INDEX idx_status (status),
    INDEX idx_process_id (process_id),
    INDEX idx_queue_id (queue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='触发器规则表';

-- ============================================
-- 3. 审计日志表
-- ============================================
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
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
    INDEX idx_risk_level (risk_level),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- ============================================
-- 4. 为robot表添加队列关联字段（如果不存在）
-- ============================================
-- 注意：以下字段已在 update-db.sql 中添加，此处仅作检查
-- ALTER TABLE robot ADD COLUMN queue_id BIGINT COMMENT '关联的队列ID';
-- ALTER TABLE robot ADD COLUMN queue_name VARCHAR(200) COMMENT '关联的队列名称';
-- ALTER TABLE robot ADD COLUMN allowed_categories VARCHAR(500) COMMENT '可执行的机器人分类（JSON格式数组）';
-- ALTER TABLE robot ADD COLUMN bound_process_ids TEXT COMMENT '绑定的多个流程ID（JSON格式数组）';

-- ============================================
-- 5. 为rpa_process表添加队列和触发器关联字段（如果不存在）
-- ============================================
-- ALTER TABLE rpa_process ADD COLUMN queue_id BIGINT COMMENT '关联的队列ID';
-- ALTER TABLE rpa_process ADD COLUMN queue_name VARCHAR(200) COMMENT '关联的队列名称';
-- ALTER TABLE rpa_process ADD COLUMN trigger_id BIGINT COMMENT '绑定的触发器ID';

-- ============================================
-- 6. 为execution_log表添加数据导出相关字段（如果不存在）
-- ============================================
-- ALTER TABLE execution_log ADD COLUMN data_amount DECIMAL(15,2) DEFAULT 0 COMMENT '采集数据金额';
-- ALTER TABLE execution_log ADD COLUMN invoice_count INT DEFAULT 0 COMMENT '发票数量';

-- ============================================
-- 插入示例触发器
-- ============================================
INSERT IGNORE INTO trigger_rule (name, code, description, trigger_type, status, process_id, process_name, cron, schedule_type, schedule_time, auto_start, creator)
SELECT * FROM (
    SELECT '每日发票采集' AS name, 'DAILY_INVOICE_COLLECT' AS code, '每天凌晨自动执行发票采集任务' AS description, 'schedule' AS trigger_type, 'active' AS status, 4 AS process_id, '数据采集流程' AS process_name, '0 0 2 * * ?' AS cron, 'day' AS schedule_type, '02:00' AS schedule_time, 1 AS auto_start, 'system' AS creator
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM trigger_rule WHERE code = 'DAILY_INVOICE_COLLECT');

INSERT IGNORE INTO trigger_rule (name, code, description, trigger_type, status, process_id, process_name, cron, schedule_type, schedule_time, auto_start, creator)
SELECT * FROM (
    SELECT '每周报表生成' AS name, 'WEEKLY_REPORT' AS code, '每周一自动生成报表' AS description, 'schedule' AS trigger_type, 'active' AS status, 5 AS process_id, '报表生成流程' AS process_name, '0 0 8 ? * MON' AS cron, 'week' AS schedule_type, '08:00' AS schedule_time, 1 AS auto_start, 'system' AS creator
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM trigger_rule WHERE code = 'WEEKLY_REPORT');

INSERT IGNORE INTO trigger_rule (name, code, description, trigger_type, status, process_id, process_name, api_key, auto_start, creator)
SELECT * FROM (
    SELECT 'API发票触发' AS name, 'API_INVOICE_TRIGGER' AS code, '通过API接口触发发票处理' AS description, 'api' AS trigger_type, 'active' AS status, 3 AS process_id, '发票审核流程' AS process_name, 'rpa_api_key_2024' AS api_key, 1 AS auto_start, 'system' AS creator
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM trigger_rule WHERE code = 'API_INVOICE_TRIGGER');

SELECT '数据库扩展表创建完成！' AS Result;

-- 0. 创建机器人分类表（如果不存在）
CREATE TABLE IF NOT EXISTS robot_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类编码',
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人分类表';

-- 初始化默认分类数据
INSERT IGNORE INTO robot_category (name, code) VALUES
    ('数据采集', 'DATA_COLLECT'),
    ('数据解析', 'DATA_PARSE'),
    ('数据加工', 'DATA_PROCESS'),
    ('通用执行', 'GENERAL');

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

-- 添加机器人分类字段
ALTER TABLE robot ADD COLUMN robot_category VARCHAR(50) DEFAULT 'GENERAL' COMMENT '机器人分类（DATA_COLLECT-数据采集，DATA_PARSE-数据解析，DATA_PROCESS-数据加工，GENERAL-通用执行）';

-- 添加绑定流程ID字段
ALTER TABLE robot ADD COLUMN bound_process_id BIGINT COMMENT '绑定的流程ID';

-- 添加绑定流程名字段
ALTER TABLE robot ADD COLUMN bound_process_name VARCHAR(200) COMMENT '绑定的流程名称';

-- 添加机器人执行代码字段
ALTER TABLE robot ADD COLUMN robot_code TEXT COMMENT '机器人执行代码';

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

-- 添加采集数据数量字段
ALTER TABLE execution_log ADD COLUMN data_count INT DEFAULT 0 COMMENT '采集数据数量';

-- 4. 为task表添加多流程字段
ALTER TABLE task ADD COLUMN process_ids TEXT COMMENT '多个流程ID（JSON数组）';
ALTER TABLE task ADD COLUMN process_names TEXT COMMENT '多个流程名称（JSON数组）';

-- ============================================
-- 插入示例数据（先检查是否已存在）
-- ============================================

-- 查看当前数据
SELECT '当前机器人数据：' AS '';
SELECT * FROM robot;

-- 插入示例机器人（如果不存在）
INSERT INTO robot (name, robot_category, status, ip, hostname, port, cpu_usage, memory_usage, description, bound_process_id, bound_process_name)
SELECT * FROM (
    SELECT 'Robot-Collector-01' AS name, 'DATA_COLLECT' AS robot_category, 'idle' AS status, '192.168.1.101' AS ip, 'WORKSTATION-01' AS hostname, 8080 AS port, 45 AS cpu_usage, 62 AS memory_usage, '数据采集机器人，用于采集网页数据' AS description, 4 AS bound_process_id, '数据采集流程' AS bound_process_name
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM robot WHERE name = 'Robot-Collector-01');

INSERT INTO robot (name, robot_category, status, ip, hostname, port, cpu_usage, memory_usage, description, bound_process_id, bound_process_name)
SELECT * FROM (
    SELECT 'Robot-Parser-01' AS name, 'DATA_PARSE' AS robot_category, 'idle' AS status, '192.168.1.102' AS ip, 'WORKSTATION-02' AS hostname, 8080 AS port, 12 AS cpu_usage, 35 AS memory_usage, '数据解析机器人，用于解析采集的数据' AS description, NULL AS bound_process_id, NULL AS bound_process_name
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM robot WHERE name = 'Robot-Parser-01');

INSERT INTO robot (name, robot_category, status, ip, hostname, port, cpu_usage, memory_usage, description, bound_process_id, bound_process_name)
SELECT * FROM (
    SELECT 'Robot-Processor-01' AS name, 'DATA_PROCESS' AS robot_category, 'idle' AS status, '192.168.1.103' AS ip, 'SERVER-01' AS hostname, 8080 AS port, 78 AS cpu_usage, 85 AS memory_usage, '数据加工机器人，用于处理大批量数据' AS description, NULL AS bound_process_id, NULL AS bound_process_name
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM robot WHERE name = 'Robot-Processor-01');

INSERT INTO robot (name, robot_category, status, ip, hostname, port, cpu_usage, memory_usage, description)
SELECT * FROM (
    SELECT 'Robot-General-01' AS name, 'GENERAL' AS robot_category, 'idle' AS status, '192.168.1.104' AS ip, 'WORKSTATION-03' AS hostname, 8080 AS port, 20 AS cpu_usage, 45 AS memory_usage, '通用执行机器人，可执行各种流程任务' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM robot WHERE name = 'Robot-General-01');

INSERT INTO robot (name, robot_category, status, ip, hostname, port, cpu_usage, memory_usage, description)
SELECT * FROM (
    SELECT 'Robot-General-02' AS name, 'GENERAL' AS robot_category, 'offline' AS status, '192.168.1.105' AS ip, 'WORKSTATION-04' AS hostname, 8080 AS port, 0 AS cpu_usage, 0 AS memory_usage, '通用执行机器人，已离线' AS description
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM robot WHERE name = 'Robot-General-02');

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
    SELECT '数据同步任务' AS name, '数据处理' AS category, 'high' AS priority, 1 AS process_id, '客户信息录入流程' AS process_name, 1 AS robot_id, 'Robot-Collector-01' AS robot_name, 'running' AS status, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '数据同步任务');

INSERT INTO task (name, category, priority, process_id, process_name, robot_id, robot_name, status, create_time)
SELECT * FROM (
    SELECT '报表生成任务' AS name, '报表' AS category, 'normal' AS priority, 5 AS process_id, '报表生成流程' AS process_name, 4 AS robot_id, 'Robot-General-01' AS robot_name, 'pending' AS status, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '报表生成任务');

INSERT INTO task (name, category, priority, process_id, process_name, robot_id, robot_name, status, create_time)
SELECT * FROM (
    SELECT '订单处理任务' AS name, '订单' AS category, 'high' AS priority, 2 AS process_id, '订单处理流程' AS process_name, 1 AS robot_id, 'Robot-Collector-01' AS robot_name, 'completed' AS status, DATE_SUB(NOW(), INTERVAL 1 HOUR) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '订单处理任务');

INSERT INTO task (name, category, priority, process_id, process_name, robot_id, robot_name, status, create_time)
SELECT * FROM (
    SELECT '发票审核任务' AS name, '审核' AS category, 'normal' AS priority, 3 AS process_id, '发票审核流程' AS process_name, 3 AS robot_id, 'Robot-Processor-01' AS robot_name, 'failed' AS status, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '发票审核任务');

INSERT INTO task (name, category, priority, process_id, process_name, robot_id, robot_name, status, create_time)
SELECT * FROM (
    SELECT '数据采集任务' AS name, '采集' AS category, 'low' AS priority, 4 AS process_id, '数据采集流程' AS process_name, 1 AS robot_id, 'Robot-Collector-01' AS robot_name, 'pending' AS status, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM task WHERE name = '数据采集任务');

-- 插入示例执行日志
INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, end_time, duration, create_time)
SELECT * FROM (
    SELECT 1 AS task_id, 1 AS process_id, 1 AS robot_id, '数据同步任务' AS task_name, 'Robot-Collector-01' AS robot_name, '任务执行' AS action, 'success' AS status, '任务执行成功，处理了120条数据' AS message, NOW() AS start_time, DATE_ADD(NOW(), INTERVAL 15 MINUTE) AS end_time, '15分钟' AS duration, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '数据同步任务');

INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, duration, create_time)
SELECT * FROM (
    SELECT 2 AS task_id, 5 AS process_id, 4 AS robot_id, '报表生成任务' AS task_name, 'Robot-General-01' AS robot_name, '任务执行' AS action, 'running' AS status, '正在生成报表...' AS message, NOW() AS start_time, NULL AS duration, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '报表生成任务');

INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, end_time, duration, create_time)
SELECT * FROM (
    SELECT 3 AS task_id, 2 AS process_id, 1 AS robot_id, '订单处理任务' AS task_name, 'Robot-Collector-01' AS robot_name, '任务执行' AS action, 'success' AS status, '成功处理了35个订单' AS message, DATE_SUB(NOW(), INTERVAL 1 HOUR) AS start_time, DATE_SUB(NOW(), INTERVAL 45 MINUTE) AS end_time, '15分钟' AS duration, DATE_SUB(NOW(), INTERVAL 1 HOUR) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '订单处理任务');

INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, end_time, duration, create_time)
SELECT * FROM (
    SELECT 4 AS task_id, 3 AS process_id, 3 AS robot_id, '发票审核任务' AS task_name, 'Robot-Processor-01' AS robot_name, '任务执行' AS action, 'failed' AS status, '网络连接超时，请检查网络' AS message, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS start_time, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS end_time, '3分钟' AS duration, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '发票审核任务');

INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, end_time, duration, create_time)
SELECT * FROM (
    SELECT 5 AS task_id, 4 AS process_id, 1 AS robot_id, '数据采集任务' AS task_name, 'Robot-Collector-01' AS robot_name, '数据采集' AS action, 'success' AS status, '成功采集了200条新闻数据' AS message, DATE_SUB(NOW(), INTERVAL 30 MINUTE) AS start_time, DATE_SUB(NOW(), INTERVAL 25 MINUTE) AS end_time, '5分钟' AS duration, DATE_SUB(NOW(), INTERVAL 30 MINUTE) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '数据采集任务');

INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, end_time, duration, create_time)
SELECT * FROM (
    SELECT 5 AS task_id, 4 AS process_id, 2 AS robot_id, '数据采集任务' AS task_name, 'Robot-Parser-01' AS robot_name, '数据解析' AS action, 'success' AS status, '成功解析了195条数据，失败5条' AS message, DATE_SUB(NOW(), INTERVAL 25 MINUTE) AS start_time, DATE_SUB(NOW(), INTERVAL 20 MINUTE) AS end_time, '5分钟' AS duration, DATE_SUB(NOW(), INTERVAL 25 MINUTE) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '数据解析');

INSERT INTO execution_log (task_id, process_id, robot_id, task_name, robot_name, action, status, message, start_time, end_time, duration, create_time)
SELECT * FROM (
    SELECT 5 AS task_id, 4 AS process_id, 3 AS robot_id, '数据采集任务' AS task_name, 'Robot-Processor-01' AS robot_name, '数据加工' AS action, 'success' AS status, '成功加工了190条数据' AS message, DATE_SUB(NOW(), INTERVAL 20 MINUTE) AS start_time, DATE_SUB(NOW(), INTERVAL 10 MINUTE) AS end_time, '10分钟' AS duration, DATE_SUB(NOW(), INTERVAL 20 MINUTE) AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM execution_log WHERE task_name = '数据加工');

SELECT '数据库更新完成！' AS Result;

-- ============================================
-- 修复触发器enabled字段数据一致性
-- ============================================
-- 将status为active或paused的触发器的enabled字段设置为1（true）
-- 将status为disabled的触发器的enabled字段设置为0（false）
UPDATE trigger_rule SET enabled = 1 WHERE status IN ('active', 'paused') AND (enabled IS NULL OR enabled = 0);
UPDATE trigger_rule SET enabled = 0 WHERE status = 'disabled' AND (enabled IS NULL OR enabled = 1);

-- ============================================
-- 修复触发器max_concurrent字段NULL值
-- ============================================
-- 将max_concurrent为NULL的记录设置为默认值1
UPDATE trigger_rule SET max_concurrent = 1 WHERE max_concurrent IS NULL;

SELECT '触发器数据一致性修复完成！' AS Result;

-- ============================================
-- 修复队列计数器与任务状态不一致问题
-- ============================================
-- 重新计算每个队列的pending和running计数，确保与task表实际状态一致
UPDATE task_queue tq
SET 
    tq.current_pending_count = (
        SELECT COALESCE(COUNT(*), 0)
        FROM task t
        WHERE t.queue_id = tq.id AND t.status = 'pending'
    ),
    tq.current_running_count = (
        SELECT COALESCE(COUNT(*), 0)
        FROM task t
        WHERE t.queue_id = tq.id AND t.status IN ('assigned', 'running')
    )
WHERE tq.id IN (SELECT DISTINCT queue_id FROM task WHERE queue_id IS NOT NULL);

SELECT '队列计数器数据一致性修复完成！' AS Result;
