-- ================================================
-- 系统公告表初始化脚本
-- 适用于 MySQL 数据库
-- ================================================

-- 创建公告表
CREATE TABLE IF NOT EXISTS `announcement` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告内容',
    `priority` VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '优先级：normal-普通，important-重要，urgent-紧急',
    `status` VARCHAR(20) NOT NULL DEFAULT 'published' COMMENT '状态：draft-草稿，published-已发布，expired-已过期',
    `publisher_id` BIGINT COMMENT '发布者ID',
    `publisher_name` VARCHAR(100) COMMENT '发布者名称',
    `department` VARCHAR(100) COMMENT '发布者部门',
    `publish_time` DATETIME COMMENT '发布时间',
    `expire_time` DATETIME COMMENT '过期时间',
    `read_count` INT DEFAULT 0 COMMENT '阅读次数',
    `scope` VARCHAR(20) DEFAULT 'all' COMMENT '发布范围：all-全平台，department-本部门，specific-指定用户',
    `attachments` TEXT COMMENT '附件JSON',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- ================================================
-- 插入测试数据
-- ================================================

-- 清空表数据（可选，保留此行可清空）
-- TRUNCATE TABLE announcement;

-- 插入紧急公告
INSERT INTO `announcement` (`title`, `content`, `priority`, `status`, `publisher_id`, `publisher_name`, `department`, `publish_time`, `read_count`, `scope`) VALUES
('【紧急】系统升级维护通知',
'<p>尊敬的用户：</p>
<p>为提升系统性能和稳定性，信息技术部计划于本周六凌晨<strong>2:00-6:00</strong>进行系统升级维护。</p>
<p><strong>维护内容：</strong></p>
<ul>
<li>RPA流程引擎优化升级</li>
<li>数据库性能调优</li>
<li>安全漏洞修复</li>
</ul>
<p><strong>注意事项：</strong></p>
<ol>
<li>维护期间系统暂停服务</li>
<li>请提前保存正在执行的任务</li>
<li>如有紧急情况请联系：support@company.com</li>
</ol>
<p>给您带来的不便，敬请谅解！</p>',
'urgent', 'published', 1, '系统管理员', '信息技术部', NOW(), 45, 'all');

-- 插入重要公告
INSERT INTO `announcement` (`title`, `content`, `priority`, `status`, `publisher_id`, `publisher_name`, `department`, `publish_time`, `read_count`, `scope`) VALUES
('【重要】RPA流程优化专项培训通知',
'<p>各部门同事：</p>
<p>为提高全员RPA应用能力，信息中心将于下周一举办<strong>RPA流程优化专题培训</strong>。</p>
<p><strong>培训安排：</strong></p>
<table border="1" cellpadding="5" cellspacing="0">
<tr><td><strong>培训时间</strong></td><td>2024年4月15日 14:00-17:00</td></tr>
<tr><td><strong>培训地点</strong></td><td>总部大楼3层会议室A</td></tr>
<tr><td><strong>培训讲师</strong></td><td>张明 高级RPA工程师</td></tr>
<tr><td><strong>培训对象</strong></td><td>各部门业务骨干、流程负责人</td></tr>
</table>
<p><strong>培训内容：</strong></p>
<ul>
<li>RPA基础操作与流程设计</li>
<li>常见业务场景案例分享</li>
<li>实操演练与答疑</li>
</ul>
<p>请各部门于4月12日前将参训人员名单发送至：rpa-training@company.com</p>',
'important', 'published', 2, '张经理', '信息中心', DATE_SUB(NOW(), INTERVAL 1 DAY), 32, 'all');

-- 插入普通公告
INSERT INTO `announcement` (`title`, `content`, `priority`, `status`, `publisher_id`, `publisher_name`, `department`, `publish_time`, `read_count`, `scope`) VALUES
('清明节放假安排通知',
'<p>各位同事：</p>
<p>根据国家法定节假日安排，结合公司实际情况，现将2024年清明节放假安排通知如下：</p>
<p><strong>放假时间：</strong>4月4日（周四）至4月6日（周六），共3天</p>
<p><strong>调休安排：</strong>4月7日（周日）正常上班</p>
<p><strong>值班安排：</strong></p>
<ul>
<li>IT运维值班：联系人李华 138****8888</li>
<li>RPA机器人监控：系统自动执行，无需人工干预</li>
</ul>
<p>放假期间如有问题，请联系IT服务热线：400-888-8888</p>
<p>祝大家清明节安康！</p>',
'normal', 'published', 3, '人事部', '人力资源部', DATE_SUB(NOW(), INTERVAL 2 DAY), 156, 'all');

-- 插入第二条普通公告
INSERT INTO `announcement` (`title`, `content`, `priority`, `status`, `publisher_id`, `publisher_name`, `department`, `publish_time`, `read_count`, `scope`) VALUES
('RPA机器人运行月度报告（2024年3月）',
'<p>各位领导、同事：</p>
<p>现将2024年3月份RPA机器人运行情况报告如下：</p>
<p><strong>一、总体运行情况</strong></p>
<ul>
<li>本月累计执行任务：1,258次</li>
<li>任务成功率：98.6%</li>
<li>节省人力工时：约3,200小时</li>
<li>错误重试次数：18次</li>
</ul>
<p><strong>二、重点流程执行情况</strong></p>
<table border="1" cellpadding="5" cellspacing="0">
<tr><td><strong>流程名称</strong></td><td><strong>执行次数</strong></td><td><strong>成功率</strong></td></tr>
<tr><td>财务对账自动化</td><td>186次</td><td>99.5%</td></tr>
<tr><td>合同信息提取</td><td>432次</td><td>97.8%</td></tr>
<tr><td>报表自动生成</td><td>620次</td><td>99.2%</td></tr>
<tr><td>邮件自动处理</td><td>20次</td><td>95.0%</td></tr>
</table>
<p><strong>三、下月工作计划</strong></p>
<ul>
<li>完成采购流程自动化改造</li>
<li>上线新客服工单处理流程</li>
<li>开展第二轮RPA培训</li>
</ul>
<p>详细报告请查看附件。如有疑问请联系信息技术部。</p>',
'normal', 'published', 1, '信息技术部', '信息技术部', DATE_SUB(NOW(), INTERVAL 5 DAY), 89, 'all');

-- 插入一条草稿
INSERT INTO `announcement` (`title`, `content`, `priority`, `status`, `publisher_id`, `publisher_name`, `department`, `create_time`) VALUES
('【草稿】五一劳动节放假通知',
'<p>各位同事：</p>
<p>五一劳动节放假安排如下：</p>
<p>放假时间：5月1日至5月5日，共5天</p>
<p>5月6日（周一）正常上班</p>
<p>（草稿内容，待完善）</p>',
'normal', 'draft', 1, '系统管理员', '人力资源部', NOW());

-- ================================================
-- 查询验证
-- ================================================
SELECT '公告总数' as 项目, COUNT(*) as 数量 FROM announcement
UNION ALL
SELECT '已发布', COUNT(*) FROM announcement WHERE status = 'published'
UNION ALL
SELECT '紧急公告', COUNT(*) FROM announcement WHERE priority = 'urgent'
UNION ALL
SELECT '重要公告', COUNT(*) FROM announcement WHERE priority = 'important'
UNION ALL
SELECT '普通公告', COUNT(*) FROM announcement WHERE priority = 'normal';
