-- ============================================
-- 数据管理模块数据库表
-- ============================================

USE rpa_system;

-- 1. 数据采集配置表
CREATE TABLE IF NOT EXISTS data_collect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '采集名称',
    source_url VARCHAR(500) NOT NULL COMMENT '数据来源URL',
    source_type VARCHAR(50) DEFAULT 'web' COMMENT '数据来源类型：web, api, file',
    selector_rules TEXT COMMENT 'CSS选择器规则JSON',
    headers TEXT COMMENT '自定义请求头JSON',
    cookies TEXT COMMENT 'Cookie信息',
    cron_expression VARCHAR(100) COMMENT '定时表达式',
    status INT DEFAULT 0 COMMENT '状态：0-禁用，1-启用，2-采集中',
    last_collect_time BIGINT COMMENT '最后采集时间戳',
    data_count INT DEFAULT 0 COMMENT '累计采集数据量',
    creator_id BIGINT,
    creator_name VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 数据解析配置表
CREATE TABLE IF NOT EXISTS data_parse (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '解析名称',
    collect_id VARCHAR(100) COMMENT '关联的采集ID',
    parse_type VARCHAR(50) DEFAULT 'json' COMMENT '解析类型：json, xml, html, regex',
    parse_rules TEXT COMMENT '解析规则JSON',
    output_format VARCHAR(50) DEFAULT 'json' COMMENT '输出格式',
    status INT DEFAULT 0 COMMENT '状态：0-禁用，1-启用，2-解析中',
    success_count INT DEFAULT 0 COMMENT '成功解析数',
    fail_count INT DEFAULT 0 COMMENT '失败数',
    last_parse_time BIGINT COMMENT '最后解析时间戳',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 数据加工配置表
CREATE TABLE IF NOT EXISTS data_process (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '加工名称',
    source_ids VARCHAR(500) COMMENT '数据源ID列表，逗号分隔',
    process_type VARCHAR(50) DEFAULT 'transform' COMMENT '加工类型：transform, filter, aggregate',
    process_rules TEXT COMMENT '加工规则JSON',
    output_table VARCHAR(100) COMMENT '输出表名',
    status INT DEFAULT 0 COMMENT '状态：0-禁用，1-启用，2-加工中',
    processed_count INT DEFAULT 0 COMMENT '累计处理数量',
    last_process_time BIGINT COMMENT '最后处理时间戳',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 数据查询配置表
CREATE TABLE IF NOT EXISTS data_query (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '查询名称',
    source_table VARCHAR(100) DEFAULT 'collected_data' COMMENT '数据源表',
    query_condition TEXT COMMENT '查询条件JSON',
    query_columns TEXT COMMENT '查询列，逗号分隔',
    result_data TEXT COMMENT '最新查询结果',
    result_count INT DEFAULT 0 COMMENT '结果数量',
    status INT DEFAULT 0 COMMENT '状态：0-禁用，1-启用',
    last_query_time BIGINT COMMENT '最后查询时间戳',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 采集数据存储表
CREATE TABLE IF NOT EXISTS collected_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collect_id BIGINT COMMENT '采集配置ID',
    collect_name VARCHAR(100) COMMENT '采集名称',
    raw_data TEXT COMMENT '原始数据',
    parsed_data TEXT COMMENT '解析后数据',
    data_type VARCHAR(50) COMMENT '数据类型',
    source_url VARCHAR(500) COMMENT '来源URL',
    parse_status INT DEFAULT 0 COMMENT '解析状态：0-未解析，1-已解析，2-解析失败',
    collect_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
    parse_time DATETIME COMMENT '解析时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 插入示例数据
-- ============================================

-- 插入采集配置示例
INSERT INTO data_collect (name, source_url, source_type, selector_rules, status, creator_name) VALUES
('新闻头条采集', 'https://news.163.com', 'web', '{"listSelector": "div.news_title", "title": "h3 a", "time": ".time"}', 0, '系统管理员'),
('天气数据采集', 'https://tianqi.qq.com', 'web', '{"listSelector": "div.weather", "temp": ".temp", "city": ".city"}', 0, '系统管理员'),
('股票数据采集', 'https://finance.sina.com.cn/stock', 'web', '{"listSelector": "table tr", "code": "td:nth-child(1)", "name": "td:nth-child(2)", "price": "td:nth-child(3)"}', 0, '系统管理员');

-- 插入解析配置示例
INSERT INTO data_parse (name, collect_id, parse_type, parse_rules, status) VALUES
('JSON数据解析', '1', 'json', '{"title": "trim", "content": "trim"}', 0),
('正则表达式解析', '2', 'regex', '{"temp": "regex:(\\\\d+)℃"}', 0),
('文本清洗解析', '3', 'html', '{"code": "trim", "name": "trim", "price": "trim"}', 0);

-- 插入加工配置示例
INSERT INTO data_process (name, source_ids, process_type, process_rules, status) VALUES
('数据去重加工', '', 'transform', '{"filter": {}, "transform": {}}', 0),
('数据过滤加工', '', 'filter', '{"filter": {"status": "active"}}', 0),
('数据聚合加工', '', 'aggregate', '{"groupBy": "category", "aggregate": "count"}', 0);

-- 插入查询配置示例
INSERT INTO data_query (name, source_table, query_condition, query_columns, status) VALUES
('全部数据查询', 'collected_data', '', '', 0),
('时间范围查询', 'collected_data', '{"collectTime": "2024-01-01"}', 'id,collectName,dataType,collectTime', 0),
('关键词查询', 'collected_data', '{}', 'id,rawData,parsedData', 0);

-- 插入采集数据示例
INSERT INTO collected_data (collect_id, collect_name, raw_data, parsed_data, data_type, source_url, parse_status) VALUES
(1, '新闻头条采集', '{"title": "科技新闻标题", "content": "这是一条科技新闻的详细内容...", "time": "2024-01-15 10:30"}', '{"title": "科技新闻标题", "content": "这是一条科技新闻的详细内容...", "time": "2024-01-15 10:30"}', 'news', 'https://news.163.com', 1),
(1, '新闻头条采集', '{"title": "财经新闻标题", "content": "这是一条财经新闻的详细内容...", "time": "2024-01-15 11:00"}', '{"title": "财经新闻标题", "content": "这是一条财经新闻的详细内容...", "time": "2024-01-15 11:00"}', 'news', 'https://news.163.com', 1),
(2, '天气数据采集', '{"city": "北京", "temp": "25℃", "weather": "晴"}', '{"city": "北京", "temp": "25℃", "weather": "晴"}', 'weather', 'https://tianqi.qq.com', 1),
(3, '股票数据采集', '{"code": "600000", "name": "浦发银行", "price": "8.50", "change": "+0.05"}', '{"code": "600000", "name": "浦发银行", "price": "8.50", "change": "+0.05"}', 'stock', 'https://finance.sina.com.cn/stock', 1);

SELECT '数据管理模块表创建完成！' AS Result;
