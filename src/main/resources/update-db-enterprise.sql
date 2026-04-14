-- ============================================
-- RPA企业级平台数据库扩展脚本 V3.0
-- 全模块功能关联方案
-- ============================================

USE rpa_system;

-- ============================================
-- 阶段一：核心业务闭环扩展
-- ============================================

-- ============================================
-- 1. 流程版本管理表
-- ============================================
CREATE TABLE IF NOT EXISTS process_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    process_id BIGINT NOT NULL COMMENT '流程ID',
    version VARCHAR(20) NOT NULL COMMENT '版本号，如1.0.0',
    version_type VARCHAR(20) DEFAULT 'release' COMMENT '版本类型：release-正式版，gray-灰度版，test-测试版',
    gray_robot_ids TEXT COMMENT '灰度机器人ID列表（JSON）',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态：draft-草稿，pending_approval-待审批，approved-已审批，published-已发布，deprecated-已废弃',
    design_config TEXT COMMENT '流程设计配置（JSON）',
    release_notes VARCHAR(1000) COMMENT '版本说明',
    approval_flow_id BIGINT COMMENT '审批流程ID',
    published_at DATETIME COMMENT '发布时间',
    published_by VARCHAR(100) COMMENT '发布人',
    rollback_from_version VARCHAR(20) COMMENT '回滚自版本',
   合规_check_result VARCHAR(20) DEFAULT 'pass' COMMENT '合规检查结果：pass-通过，warning-警告，failed-失败',
   合规_check_details TEXT COMMENT '合规检查详情',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_process_id (process_id),
    INDEX idx_version (version),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程版本管理表';

-- ============================================
-- 2. 审批流程表
-- ============================================
CREATE TABLE IF NOT EXISTS approval_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '审批流程名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '审批流程编码',
    flow_type VARCHAR(30) NOT NULL COMMENT '流程类型：process_publish-流程发布，robot_register-机器人注册，data_export-数据导出',
    approval_stages TEXT NOT NULL COMMENT '审批阶段配置（JSON数组）',
    current_stage INT DEFAULT 1 COMMENT '当前阶段',
    total_stages INT DEFAULT 1 COMMENT '总阶段数',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '审批状态：pending-待审批，approved-已通过，rejected-已拒绝，cancelled-已取消',
    target_type VARCHAR(50) COMMENT '审批对象类型',
    target_id BIGINT COMMENT '审批对象ID',
    target_name VARCHAR(200) COMMENT '审批对象名称',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    current_approver_name VARCHAR(100) COMMENT '当前审批人名称',
    apply_user_id BIGINT COMMENT '申请人ID',
    apply_user_name VARCHAR(100) COMMENT '申请人名称',
    apply_reason VARCHAR(500) COMMENT '申请理由',
    approval_comments TEXT COMMENT '审批意见（JSON数组）',
    result_comments TEXT COMMENT '最终审批意见',
    finished_at DATETIME COMMENT '完成时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (code),
    INDEX idx_flow_type (flow_type),
    INDEX idx_status (status),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程表';

-- ============================================
-- 3. 任务依赖编排表（DAG）
-- ============================================
CREATE TABLE IF NOT EXISTS task_dependency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '依赖编排名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '依赖编排编码',
    description VARCHAR(500) COMMENT '依赖编排描述',
    dag_config TEXT NOT NULL COMMENT 'DAG配置（JSON，包含节点和边）',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-启用，paused-暂停，completed-已完成，failed-失败',
    root_task_id BIGINT COMMENT '根任务ID',
    parent_task_id BIGINT COMMENT '父任务ID',
    current_execution_id BIGINT COMMENT '当前执行ID',
    execution_status VARCHAR(20) COMMENT '执行状态',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    total_duration VARCHAR(50) COMMENT '总耗时',
    success_nodes INT DEFAULT 0 COMMENT '成功节点数',
    failed_nodes INT DEFAULT 0 COMMENT '失败节点数',
    total_nodes INT DEFAULT 0 COMMENT '总节点数',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_parent_task (parent_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖编排表（DAG）';

-- ============================================
-- 4. 死信队列表
-- ============================================
CREATE TABLE IF NOT EXISTS dead_letter_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    original_queue_id BIGINT COMMENT '原队列ID',
    original_queue_name VARCHAR(200) COMMENT '原队列名称',
    task_id BIGINT COMMENT '原任务ID',
    task_name VARCHAR(200) COMMENT '原任务名称',
    process_id BIGINT COMMENT '原流程ID',
    process_name VARCHAR(200) COMMENT '原流程名称',
    robot_id BIGINT COMMENT '执行机器人ID',
    robot_name VARCHAR(100) COMMENT '执行机器人名称',
    error_code VARCHAR(50) COMMENT '错误码',
    error_message TEXT COMMENT '错误信息',
    error_stack TEXT COMMENT '错误堆栈',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retry INT DEFAULT 3 COMMENT '最大重试次数',
    last_retry_time DATETIME COMMENT '最后重试时间',
    original_params TEXT COMMENT '原始参数（JSON）',
    trace_id VARCHAR(64) COMMENT '全链路追踪ID',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待处理，analysing-分析中，resolved-已解决，manually_closed-人工关闭',
    resolution_type VARCHAR(30) COMMENT '解决方式：retry-重试，skip-跳过，manual_fix-人工修复',
    resolution_comment TEXT COMMENT '解决说明',
    resolved_by VARCHAR(100) COMMENT '处理人',
    resolved_at DATETIME COMMENT '处理时间',
    alert_id BIGINT COMMENT '关联告警ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_error_code (error_code),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信队列表';

-- ============================================
-- 5. 机器人健康检查表
-- ============================================
CREATE TABLE IF NOT EXISTS robot_health (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    robot_id BIGINT NOT NULL COMMENT '机器人ID',
    robot_name VARCHAR(100) COMMENT '机器人名称',
    check_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '检查时间',
    cpu_usage INT DEFAULT 0 COMMENT 'CPU使用率(%)',
    memory_usage INT DEFAULT 0 COMMENT '内存使用率(%)',
    disk_usage INT DEFAULT 0 COMMENT '磁盘使用率(%)',
    network_status VARCHAR(20) DEFAULT 'normal' COMMENT '网络状态：normal-正常，slow-缓慢，disconnected-断开',
    disk_io_wait INT DEFAULT 0 COMMENT '磁盘IO等待(%)',
    process_count INT DEFAULT 0 COMMENT '进程数',
    open_file_count INT DEFAULT 0 COMMENT '打开文件数',
    network_latency INT DEFAULT 0 COMMENT '网络延迟(ms)',
    health_score INT DEFAULT 100 COMMENT '健康评分(0-100)',
    health_status VARCHAR(20) DEFAULT 'healthy' COMMENT '健康状态：healthy-健康，warning-警告，critical-危险，offline-离线',
    warning_items TEXT COMMENT '警告项（JSON数组）',
    critical_items TEXT COMMENT '危险项（JSON数组）',
    heartbeat_timeout TINYINT(1) DEFAULT 0 COMMENT '心跳是否超时',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    recommendation TEXT COMMENT '建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_robot_id (robot_id),
    INDEX idx_check_time (check_time),
    INDEX idx_health_status (health_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人健康检查表';

-- ============================================
-- 6. 机器人主备关系表
-- ============================================
CREATE TABLE IF NOT EXISTS robot_backup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    primary_robot_id BIGINT NOT NULL COMMENT '主机器人ID',
    primary_robot_name VARCHAR(100) COMMENT '主机器人名称',
    backup_robot_id BIGINT NOT NULL COMMENT '备机器人ID',
    backup_robot_name VARCHAR(100) COMMENT '备机器人名称',
    relation_type VARCHAR(20) DEFAULT 'hot' COMMENT '关系类型：hot-热备（同时运行），cold-冷备（主故障时启动）',
    bound_process_id BIGINT COMMENT '绑定的流程ID',
    bound_process_name VARCHAR(200) COMMENT '绑定的流程名称',
    auto_switch TINYINT(1) DEFAULT 1 COMMENT '是否自动切换',
    switch_condition VARCHAR(100) DEFAULT 'heartbeat_timeout' COMMENT '切换条件：heartbeat_timeout-心跳超时，health_score-健康评分',
    switch_threshold INT DEFAULT 60 COMMENT '切换阈值（秒或评分）',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-生效，inactive-未生效，switched-已切换',
    last_switch_time DATETIME COMMENT '最后切换时间',
    switch_reason VARCHAR(200) COMMENT '切换原因',
    switch_count INT DEFAULT 0 COMMENT '累计切换次数',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_primary_robot (primary_robot_id),
    INDEX idx_backup_robot (backup_robot_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人主备关系表';

-- ============================================
-- 7. 队列优先级配置表
-- ============================================
ALTER TABLE task_queue ADD COLUMN priority_weight INT DEFAULT 10 COMMENT '优先级权重（数值越大优先级越高）';
ALTER TABLE task_queue ADD COLUMN exclusivity_mode VARCHAR(20) DEFAULT 'none' COMMENT '独占模式：none-不独占，robot-机器人独占，resource-资源独占';
ALTER TABLE task_queue ADD COLUMN exclusive_robot_ids TEXT COMMENT '独占机器人ID列表（JSON）';
ALTER TABLE task_queue ADD COLUMN max_retry_per_task INT DEFAULT 3 COMMENT '单个任务最大重试次数';
ALTER TABLE task_queue ADD COLUMN retry_interval INT DEFAULT 60 COMMENT '重试间隔（秒）';
ALTER TABLE task_queue ADD COLUMN timeout_per_task INT DEFAULT 3600 COMMENT '单个任务超时时间（秒）';
ALTER TABLE task_queue ADD COLUMN dead_letter_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用死信队列';

-- ============================================
-- 阶段二：数据合规扩展
-- ============================================

-- ============================================
-- 8. 数据血缘表
-- ============================================
CREATE TABLE IF NOT EXISTS data_lineage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    lineage_type VARCHAR(30) NOT NULL COMMENT '血缘类型：source-数据源，process-流程处理，storage-数据存储，output-数据输出',
    source_type VARCHAR(50) COMMENT '来源类型：execution_log-执行日志，invoice_data-发票数据，custom-自定义',
    source_id BIGINT COMMENT '来源ID',
    source_name VARCHAR(200) COMMENT '来源名称',
    process_id BIGINT COMMENT '流程ID',
    process_name VARCHAR(200) COMMENT '流程名称',
    process_version VARCHAR(20) COMMENT '流程版本',
    robot_id BIGINT COMMENT '机器人ID',
    robot_name VARCHAR(100) COMMENT '机器人名称',
    task_id BIGINT COMMENT '任务ID',
    trace_id VARCHAR(64) COMMENT '全链路追踪ID',
    data_table VARCHAR(100) COMMENT '数据表名',
    data_field VARCHAR(100) COMMENT '数据字段',
    data_record_id VARCHAR(100) COMMENT '数据记录ID',
    transformation_rule VARCHAR(500) COMMENT '数据转换规则',
    parent_lineage_id BIGINT COMMENT '父血缘ID',
    child_lineage_ids TEXT COMMENT '子血缘ID列表（JSON）',
    business_key VARCHAR(200) COMMENT '业务主键（如发票号码）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_trace_id (trace_id),
    INDEX idx_source (source_type, source_id),
    INDEX idx_process (process_id),
    INDEX idx_business_key (business_key),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据血缘表';

-- ============================================
-- 9. 执行录屏表
-- ============================================
CREATE TABLE IF NOT EXISTS execution_recording (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    execution_id BIGINT NOT NULL COMMENT '执行记录ID',
    trace_id VARCHAR(64) COMMENT '全链路追踪ID',
    task_id BIGINT COMMENT '任务ID',
    task_name VARCHAR(200) COMMENT '任务名称',
    process_id BIGINT COMMENT '流程ID',
    process_name VARCHAR(200) COMMENT '流程名称',
    process_version VARCHAR(20) COMMENT '流程版本',
    robot_id BIGINT COMMENT '机器人ID',
    robot_name VARCHAR(100) COMMENT '机器人名称',
    recording_type VARCHAR(20) DEFAULT 'video' COMMENT '录屏类型：video-视频，screenshot-截图序列',
    file_path VARCHAR(500) COMMENT '录屏文件路径',
    file_size BIGINT COMMENT '文件大小（字节）',
    duration INT COMMENT '录屏时长（秒）',
    start_offset INT DEFAULT 0 COMMENT '开始偏移（秒）',
    end_offset INT COMMENT '结束偏移（秒）',
    compression_ratio INT DEFAULT 0 COMMENT '压缩率',
    storage_location VARCHAR(100) DEFAULT 'local' COMMENT '存储位置：local-本地，oss-对象存储，s3-S3',
    oss_bucket VARCHAR(100) COMMENT 'OSS bucket',
    oss_key VARCHAR(200) COMMENT 'OSS key',
    status VARCHAR(20) DEFAULT 'recording' COMMENT '状态：recording-录制中，completed-已完成，uploading-上传中，failed-失败',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_execution_id (execution_id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_task_id (task_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行录屏表';

-- ============================================
-- 10. 审计日志哈希存证表
-- ============================================
CREATE TABLE IF NOT EXISTS audit_log_hash (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    audit_log_id BIGINT NOT NULL COMMENT '审计日志ID',
    previous_hash VARCHAR(64) COMMENT '前一条记录的哈希值',
    current_hash VARCHAR(64) NOT NULL COMMENT '当前记录的哈希值',
    hash_algorithm VARCHAR(20) DEFAULT 'SHA-256' COMMENT '哈希算法',
    signature VARCHAR(500) COMMENT '数字签名',
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    block_number BIGINT COMMENT '区块编号（用于区块链存证）',
    chain_id VARCHAR(50) COMMENT '链ID（用于区块链存证）',
    verify_status VARCHAR(20) DEFAULT 'verified' COMMENT '验证状态：verified-已验证，tampered-已篡改',
    verify_time DATETIME COMMENT '验证时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_audit_log_id (audit_log_id),
    INDEX idx_current_hash (current_hash),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志哈希存证表';

-- ============================================
-- 11. 数据脱敏规则表
-- ============================================
CREATE TABLE IF NOT EXISTS data_masking_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    data_type VARCHAR(30) NOT NULL COMMENT '数据类型：credit_code-统一社会信用代码，id_card-身份证，phone-手机号，bank_card-银行卡号，email-邮箱，amount-金额',
    masking_type VARCHAR(20) NOT NULL COMMENT '脱敏方式：partial-部分掩码，hash-哈希，encrypt-加密，replace-替换',
    pattern VARCHAR(200) COMMENT '匹配正则表达式',
    replacement VARCHAR(200) COMMENT '替换模板',
    show_start INT DEFAULT 0 COMMENT '显示前N位',
    show_end INT DEFAULT 0 COMMENT '显示后N位',
    mask_char VARCHAR(10) DEFAULT '*' COMMENT '掩码字符',
    scope VARCHAR(50) DEFAULT 'query' COMMENT '应用范围：query-查询，export-导出，display-展示',
    priority INT DEFAULT 100 COMMENT '优先级',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(500) COMMENT '规则说明',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (code),
    INDEX idx_data_type (data_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据脱敏规则表';

-- 插入默认脱敏规则
INSERT IGNORE INTO data_masking_rule (name, code, data_type, masking_type, show_start, show_end, mask_char, scope, creator) VALUES
('统一社会信用代码脱敏', 'CREDIT_CODE_MASK', 'credit_code', 'partial', 6, 2, '*', 'all', 'system'),
('身份证号脱敏', 'ID_CARD_MASK', 'id_card', 'partial', 6, 4, '*', 'all', 'system'),
('手机号脱敏', 'PHONE_MASK', 'phone', 'partial', 3, 4, '*', 'all', 'system'),
('银行卡号脱敏', 'BANK_CARD_MASK', 'bank_card', 'partial', 4, 4, '*', 'all', 'system'),
('邮箱脱敏', 'EMAIL_MASK', 'email', 'partial', 2, 0, '*', 'all', 'system');

-- ============================================
-- 阶段三：智能告警扩展
-- ============================================

-- ============================================
-- 12. 告警规则表
-- ============================================
CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '告警规则名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '告警规则编码',
    alert_type VARCHAR(30) NOT NULL COMMENT '告警类型：execution_failed-任务执行失败，robot_offline-机器人离线，queue_overflow-队列积压超时，credential_expiring-凭证即将过期，system_error-系统异常',
    severity VARCHAR(20) NOT NULL COMMENT '严重程度：P0-紧急，P1-重要，P2-一般，P3-提示',
    condition_config TEXT NOT NULL COMMENT '触发条件配置（JSON）',
    condition_expr TEXT COMMENT '触发条件表达式',
    threshold_value VARCHAR(100) COMMENT '阈值',
    comparison_type VARCHAR(20) DEFAULT 'gt' COMMENT '比较类型：gt-大于，lt-小于，eq-等于，gte-大于等于，lte-小于等于',
    check_interval INT DEFAULT 60 COMMENT '检查间隔（秒）',
    consecutive_count INT DEFAULT 1 COMMENT '连续触发次数',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    auto_create_incident TINYINT(1) DEFAULT 0 COMMENT '是否自动创建工单',
    notification_channels TEXT COMMENT '通知渠道（JSON数组）：wechat-企业微信，dingtalk-钉钉，feishu-飞书，email-邮件，sms-短信，webhook',
    notification_template TEXT COMMENT '通知模板',
    cooldown_period INT DEFAULT 600 COMMENT '告警冷却期（秒）',
    deduplication_key VARCHAR(100) COMMENT '去重键',
    department VARCHAR(100) COMMENT '所属部门',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (code),
    INDEX idx_alert_type (alert_type),
    INDEX idx_severity (severity),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- ============================================
-- 13. 告警记录表
-- ============================================
CREATE TABLE IF NOT EXISTS alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    alert_rule_id BIGINT COMMENT '告警规则ID',
    alert_rule_code VARCHAR(50) COMMENT '告警规则编码',
    alert_rule_name VARCHAR(100) COMMENT '告警规则名称',
    severity VARCHAR(20) NOT NULL COMMENT '严重程度：P0-紧急，P1-重要，P2-一般，P3-提示',
    alert_type VARCHAR(30) NOT NULL COMMENT '告警类型',
    title VARCHAR(200) NOT NULL COMMENT '告警标题',
    content TEXT COMMENT '告警内容',
    detail_url VARCHAR(500) COMMENT '详情链接',
    trigger_value VARCHAR(200) COMMENT '触发值',
    threshold_value VARCHAR(100) COMMENT '阈值',
    target_type VARCHAR(50) COMMENT '告警对象类型',
    target_id BIGINT COMMENT '告警对象ID',
    target_name VARCHAR(200) COMMENT '告警对象名称',
    trace_id VARCHAR(64) COMMENT '关联追踪ID',
    incident_id BIGINT COMMENT '关联工单ID',
    status VARCHAR(20) DEFAULT 'firing' COMMENT '状态：firing-触发中，resolved-已解决，suppressed-已抑制',
    suppressed_reason VARCHAR(200) COMMENT '抑制原因',
    first_fired_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次触发时间',
    last_fired_at DATETIME COMMENT '最后触发时间',
    fired_count INT DEFAULT 1 COMMENT '累计触发次数',
    resolved_at DATETIME COMMENT '解决时间',
    resolved_by VARCHAR(100) COMMENT '解决人',
    resolution VARCHAR(500) COMMENT '解决方案',
    notification_status VARCHAR(20) DEFAULT 'pending' COMMENT '通知状态：pending-待发送，sent-已发送，failed-发送失败',
    notification_channels TEXT COMMENT '已发送的通知渠道',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_rule_id (alert_rule_id),
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_target (target_type, target_id),
    INDEX idx_fired_at (first_fired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- ============================================
-- 14. 通知渠道配置表
-- ============================================
CREATE TABLE IF NOT EXISTS notification_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    channel_type VARCHAR(30) NOT NULL COMMENT '渠道类型：wechat-企业微信，dingtalk-钉钉，feishu-飞书，email-邮件，sms-短信，webhook',
    name VARCHAR(100) NOT NULL COMMENT '渠道名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '渠道编码',
    config TEXT NOT NULL COMMENT '渠道配置（JSON）',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认渠道',
    priority INT DEFAULT 100 COMMENT '优先级',
    rate_limit INT DEFAULT 100 COMMENT '发送频率限制（条/分钟）',
    success_rate DECIMAL(5,2) DEFAULT 100.00 COMMENT '发送成功率(%)',
    last_test_at DATETIME COMMENT '最后测试时间',
    last_test_result VARCHAR(20) COMMENT '最后测试结果：success-成功，failed-失败',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (code),
    INDEX idx_channel_type (channel_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知渠道配置表';

-- ============================================
-- 15. 告警排班表
-- ============================================
CREATE TABLE IF NOT EXISTS alert_roster (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '排班名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '排班编码',
    alert_rule_ids TEXT COMMENT '关联告警规则ID列表（JSON）',
    severity_levels TEXT COMMENT '适用严重级别（JSON数组）',
    schedule_config TEXT NOT NULL COMMENT '排班配置（JSON）',
    roster_members TEXT NOT NULL COMMENT '值班人员（JSON数组）',
    escalation_config TEXT COMMENT '升级配置（JSON）',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    effective_from DATETIME COMMENT '生效时间',
    effective_to DATETIME COMMENT '失效时间',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (code),
    INDEX idx_enabled (enabled),
    INDEX idx_effective (effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警排班表';

-- ============================================
-- 阶段四：高级特性扩展
-- ============================================

-- ============================================
-- 16. 异步导出任务表
-- ============================================
CREATE TABLE IF NOT EXISTS async_export_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_no VARCHAR(50) NOT NULL UNIQUE COMMENT '任务编号',
    export_type VARCHAR(30) NOT NULL COMMENT '导出类型：data_query-数据查询，report-报表，audit_log-审计日志',
    export_name VARCHAR(200) NOT NULL COMMENT '导出名称',
    query_params TEXT COMMENT '查询参数（JSON）',
    export_fields TEXT COMMENT '导出字段（JSON数组）',
    file_format VARCHAR(10) DEFAULT 'xlsx' COMMENT '文件格式：xlsx，csv，pdf',
    total_records INT DEFAULT 0 COMMENT '总记录数',
    exported_records INT DEFAULT 0 COMMENT '已导出记录数',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待处理，processing-处理中，completed-已完成，failed-失败',
    progress INT DEFAULT 0 COMMENT '进度(%)',
    file_path VARCHAR(500) COMMENT '导出文件路径',
    file_size BIGINT COMMENT '文件大小（字节）',
    file_md5 VARCHAR(64) COMMENT '文件MD5',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    download_expire_at DATETIME COMMENT '下载链接过期时间',
    error_message TEXT COMMENT '错误信息',
    user_id BIGINT COMMENT '导出用户ID',
    user_name VARCHAR(100) COMMENT '导出用户名称',
    department VARCHAR(100) COMMENT '用户部门',
    ip VARCHAR(50) COMMENT '用户IP',
    start_at DATETIME COMMENT '开始时间',
    completed_at DATETIME COMMENT '完成时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_no (task_no),
    INDEX idx_status (status),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出任务表';

-- ============================================
-- 17. 报表订阅表
-- ============================================
CREATE TABLE IF NOT EXISTS report_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '订阅名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '订阅编码',
    report_type VARCHAR(30) NOT NULL COMMENT '报表类型：daily-日报，weekly-周报，monthly-月报，quarterly-季报，yearly-年报，custom-自定义',
    report_config TEXT NOT NULL COMMENT '报表配置（JSON）',
    schedule_config TEXT COMMENT '调度配置（JSON）',
    notification_channels TEXT COMMENT '通知渠道（JSON数组）',
    notification_template TEXT COMMENT '通知模板',
    recipients TEXT NOT NULL COMMENT '接收人列表（JSON数组）',
    cc_recipients TEXT COMMENT '抄送人列表（JSON数组）',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    last_run_at DATETIME COMMENT '最后运行时间',
    last_status VARCHAR(20) COMMENT '最后运行状态：success-成功，failed-失败',
    last_error TEXT COMMENT '最后错误信息',
    next_run_at DATETIME COMMENT '下次运行时间',
    total_runs INT DEFAULT 0 COMMENT '累计运行次数',
    success_runs INT DEFAULT 0 COMMENT '成功次数',
    creator VARCHAR(100) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (code),
    INDEX idx_report_type (report_type),
    INDEX idx_enabled (enabled),
    INDEX idx_next_run (next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅表';

-- ============================================
-- 18. 报表订阅发送记录表
-- ============================================
CREATE TABLE IF NOT EXISTS report_subscription_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    subscription_id BIGINT NOT NULL COMMENT '订阅ID',
    subscription_name VARCHAR(100) COMMENT '订阅名称',
    report_period_start DATETIME COMMENT '报表周期开始时间',
    report_period_end DATETIME COMMENT '报表周期结束时间',
    report_file_path VARCHAR(500) COMMENT '报表文件路径',
    report_file_size BIGINT COMMENT '报表文件大小',
    report_data_preview TEXT COMMENT '报表数据预览',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待发送，sent-已发送，failed-发送失败',
    sent_channels TEXT COMMENT '已发送的渠道',
    recipient_count INT DEFAULT 0 COMMENT '接收人数',
    open_count INT DEFAULT 0 COMMENT '打开次数',
    error_message TEXT COMMENT '错误信息',
    sent_at DATETIME COMMENT '发送时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_subscription_id (subscription_id),
    INDEX idx_status (status),
    INDEX idx_sent_at (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅发送记录表';

-- ============================================
-- 19. 全链路追踪表
-- ============================================
CREATE TABLE IF NOT EXISTS trace_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL COMMENT '追踪ID',
    span_id VARCHAR(32) COMMENT '跨度ID',
    parent_span_id VARCHAR(32) COMMENT '父跨度ID',
    operation_name VARCHAR(200) NOT NULL COMMENT '操作名称',
    service_name VARCHAR(100) COMMENT '服务名称',
    module VARCHAR(50) COMMENT '模块：trigger-触发器，scheduler-调度器，robot-机器人，process-流程',
    status VARCHAR(20) DEFAULT 'success' COMMENT '状态：success-成功，failed-失败，running-进行中',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms BIGINT COMMENT '耗时（毫秒）',
    tags TEXT COMMENT '标签（JSON）',
    logs TEXT COMMENT '日志（JSON数组）',
    error_type VARCHAR(100) COMMENT '错误类型',
    error_message TEXT COMMENT '错误信息',
    request_data TEXT COMMENT '请求数据',
    response_data TEXT COMMENT '响应数据',
    metadata TEXT COMMENT '元数据（JSON）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_trace_id (trace_id),
    INDEX idx_start_time (start_time),
    INDEX idx_operation (operation_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全链路追踪表';

-- ============================================
-- 20. 凭证使用记录表（扩展）
-- ============================================
CREATE TABLE IF NOT EXISTS credential_usage_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    credential_id BIGINT NOT NULL COMMENT '凭证ID',
    credential_name VARCHAR(200) COMMENT '凭证名称',
    credential_type VARCHAR(30) COMMENT '凭证类型',
    process_id BIGINT COMMENT '使用流程ID',
    process_name VARCHAR(200) COMMENT '使用流程名称',
    process_version VARCHAR(20) COMMENT '流程版本',
    robot_id BIGINT COMMENT '使用机器人ID',
    robot_name VARCHAR(100) COMMENT '使用机器人名称',
    task_id BIGINT COMMENT '使用任务ID',
    trace_id VARCHAR(64) COMMENT '追踪ID',
    usage_type VARCHAR(30) DEFAULT 'execute' COMMENT '使用类型：create-创建，authorize-授权，use-使用，update-更新，delete-删除',
    usage_result VARCHAR(20) DEFAULT 'success' COMMENT '使用结果：success-成功，failed-失败',
    error_message TEXT COMMENT '错误信息',
    ip VARCHAR(50) COMMENT '使用IP',
    user_id BIGINT COMMENT '操作用户ID',
    user_name VARCHAR(100) COMMENT '操作用户名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_credential_id (credential_id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_task_id (task_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭证使用记录表';

-- ============================================
-- 为现有表添加新字段
-- ============================================

-- 为execution_log添加追踪相关字段
ALTER TABLE execution_log ADD COLUMN trace_id VARCHAR(64) COMMENT '全链路追踪ID';
ALTER TABLE execution_log ADD COLUMN recording_id BIGINT COMMENT '关联录屏ID';
ALTER TABLE execution_log ADD COLUMN data_lineage_ids TEXT COMMENT '数据血缘ID列表（JSON）';
ALTER TABLE execution_log ADD COLUMN idempotency_key VARCHAR(100) COMMENT '幂等性Key';

-- 为task添加依赖相关字段
ALTER TABLE task ADD COLUMN dependency_config TEXT COMMENT '任务依赖配置（JSON）';
ALTER TABLE task ADD COLUMN parent_task_id BIGINT COMMENT '父任务ID';
ALTER TABLE task ADD COLUMN dag_execution_id BIGINT COMMENT 'DAG执行ID';
ALTER TABLE task ADD COLUMN dag_node_id VARCHAR(50) COMMENT 'DAG节点ID';
ALTER TABLE task ADD COLUMN execution_order INT DEFAULT 0 COMMENT '执行顺序';

-- 为notification添加告警相关字段
ALTER TABLE notification ADD COLUMN alert_record_id BIGINT COMMENT '关联告警记录ID';
ALTER TABLE notification ADD COLUMN severity VARCHAR(20) COMMENT '严重程度：P0-紧急，P1-重要，P2-一般，P3-提示';
ALTER TABLE notification ADD COLUMN is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读';
ALTER TABLE notification ADD COLUMN read_at DATETIME COMMENT '阅读时间';
ALTER TABLE notification ADD COLUMN read_by VARCHAR(100) COMMENT '阅读人';

-- ============================================
-- 创建索引优化
-- ============================================
CREATE INDEX idx_execution_trace_id ON execution_log(trace_id);
CREATE INDEX idx_task_dependency ON task(parent_task_id);
CREATE INDEX idx_notification_read ON notification(is_read, create_time);
CREATE INDEX idx_audit_log_hash_chain ON audit_log_hash(audit_log_id, timestamp);

SELECT '企业级RPA平台数据库扩展完成！' AS Result;
