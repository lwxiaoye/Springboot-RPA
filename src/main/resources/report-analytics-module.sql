-- 报表分析模块数据库表结构

-- 1. 自定义报表配置表
CREATE TABLE IF NOT EXISTS custom_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '报表名称',
    type VARCHAR(50) NOT NULL COMMENT '报表类型（任务执行/数据采集/机器人效能/流程效率/成本效益/自定义）',
    dimensions TEXT COMMENT '统计维度（JSON数组格式）',
    date_range VARCHAR(20) COMMENT '时间范围（today/week/month/quarter/year/custom）',
    chart_type VARCHAR(20) COMMENT '图表类型（line/bar/pie/table）',
    description TEXT COMMENT '报表描述',
    create_user BIGINT NOT NULL COMMENT '创建人ID',
    create_user_name VARCHAR(100) COMMENT '创建人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_run_time DATETIME COMMENT '最后运行时间',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
    update_user BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_create_user (create_user),
    INDEX idx_type (type),
    INDEX idx_enabled (enabled),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义报表配置表';

-- 2. 报表订阅配置表
CREATE TABLE IF NOT EXISTS report_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '订阅名称',
    report_type VARCHAR(50) NOT NULL COMMENT '报表类型（daily/weekly/monthly/robot/roi）',
    frequency VARCHAR(20) NOT NULL COMMENT '发送频率（daily/weekly/monthly）',
    channel VARCHAR(20) NOT NULL COMMENT '推送渠道（email/dingtalk/wecom）',
    recipients TEXT NOT NULL COMMENT '接收人列表（逗号分隔）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
    create_user BIGINT NOT NULL COMMENT '创建人ID',
    create_user_name VARCHAR(100) COMMENT '创建人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_send_time DATETIME COMMENT '最后发送时间',
    last_send_status VARCHAR(20) COMMENT '最后发送状态（success/failed）',
    update_user BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_create_user (create_user),
    INDEX idx_report_type (report_type),
    INDEX idx_frequency (frequency),
    INDEX idx_enabled (enabled),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表订阅配置表';

-- 插入示例数据（可选）
INSERT INTO custom_report (name, type, dimensions, date_range, chart_type, description, create_user, create_user_name, enabled) VALUES
('财务流程周报', '任务执行', '["execCount","successRate","dataCount"]', 'week', 'line', '每周财务相关流程的执行统计', 1, 'admin', 1),
('HR流程报表', '数据采集', '["dataCount","trend"]', 'month', 'bar', '人力资源流程的数据采集统计', 1, 'admin', 1);

INSERT INTO report_subscription (name, report_type, frequency, channel, recipients, create_user, create_user_name, enabled) VALUES
('每日任务日报', 'daily', 'daily', 'email', 'admin@company.com', 1, 'admin', 1),
('每周汇总', 'weekly', 'weekly', 'dingtalk', 'leader@company.com', 1, 'admin', 1),
('月度分析', 'monthly', 'monthly', 'email', 'manager@company.com', 1, 'admin', 0);
