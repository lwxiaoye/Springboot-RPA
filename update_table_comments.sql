-- ============================================
-- RPA系统数据库表注释更新脚本
-- 用于Navicat中执行
-- ============================================

-- 1. alert_record - 告警记录表
ALTER TABLE `alert_record` COMMENT = '告警记录表';

-- 2. alert_roster - 告警排班表
ALTER TABLE `alert_roster` COMMENT = '告警排班表';

-- 3. alert_rule - 告警规则表
ALTER TABLE `alert_rule` COMMENT = '告警规则表';

-- 4. approval_flow - 审批流程表
ALTER TABLE `approval_flow` COMMENT = '审批流程表';

-- 5. async_export_task - 异步导出任务表
ALTER TABLE `async_export_task` COMMENT = '异步导出任务表';

-- 6. audit_log - 审计日志表
ALTER TABLE `audit_log` COMMENT = '审计日志表';

-- 7. audit_log_hash - 审计日志哈希存证表
ALTER TABLE `audit_log_hash` COMMENT = '审计日志哈希存证表';

-- 8. chat_ai_conversation - AI聊天记录表
ALTER TABLE `chat_ai_conversation` COMMENT = 'AI聊天记录表';

-- 9. chat_attachment - 消息附件表
ALTER TABLE `chat_attachment` COMMENT = '消息附件表';

-- 10. chat_audit_log - 聊天审计日志表
ALTER TABLE `chat_audit_log` COMMENT = '聊天审计日志表';

-- 11. chat_contact_favorite - 收藏联系人表
ALTER TABLE `chat_contact_favorite` COMMENT = '收藏联系人表';

-- 12. chat_conversation - 聊天会话表
ALTER TABLE `chat_conversation` COMMENT = '聊天会话表';

-- 13. chat_conversation_pin - 会话置顶表
ALTER TABLE `chat_conversation_pin` COMMENT = '会话置顶表';

-- 14. chat_message - 聊天消息表
ALTER TABLE `chat_message` COMMENT = '聊天消息表';

-- 15. chat_participant - 会话参与者表
ALTER TABLE `chat_participant` COMMENT = '会话参与者表';

-- 16. chat_read_status - 消息已读状态表
ALTER TABLE `chat_read_status` COMMENT = '消息已读状态表';

-- 17. chat_sensitive_word - 敏感词表
ALTER TABLE `chat_sensitive_word` COMMENT = '敏感词表';

-- 18. collected_data - 采集数据表
ALTER TABLE `collected_data` COMMENT = '采集数据表';

-- 19. credential - 凭证表
ALTER TABLE `credential` COMMENT = '凭证表';

-- 20. credential_usage_log - 凭证使用记录表
ALTER TABLE `credential_usage_log` COMMENT = '凭证使用记录表';

-- 21. custom_report - 自定义报表表
ALTER TABLE `custom_report` COMMENT = '自定义报表表';

-- 22. data_analysis - 数据分析表
ALTER TABLE `data_analysis` COMMENT = '数据分析表';

-- 23. data_collect - 数据采集配置表
ALTER TABLE `data_collect` COMMENT = '数据采集配置表';

-- 24. data_collection - 数据采集记录表
ALTER TABLE `data_collection` COMMENT = '数据采集记录表';

-- 25. data_lineage - 数据血缘表
ALTER TABLE `data_lineage` COMMENT = '数据血缘表';

-- 26. data_masking_rule - 数据脱敏规则表
ALTER TABLE `data_masking_rule` COMMENT = '数据脱敏规则表';

-- 27. data_parse - 数据解析配置表
ALTER TABLE `data_parse` COMMENT = '数据解析配置表';

-- 28. data_process - 数据处理配置表
ALTER TABLE `data_process` COMMENT = '数据处理配置表';

-- 29. data_processing - 数据处理记录表
ALTER TABLE `data_processing` COMMENT = '数据处理记录表';

-- 30. data_query - 数据查询配置表
ALTER TABLE `data_query` COMMENT = '数据查询配置表';

-- 31. dead_letter_queue - 死信队列表
ALTER TABLE `dead_letter_queue` COMMENT = '死信队列表';

-- 32. execution_log - 执行日志表
ALTER TABLE `execution_log` COMMENT = 'RPA执行日志表';

-- 33. execution_recording - 执行录像表
ALTER TABLE `execution_recording` COMMENT = '执行录像表';

-- 34. invoice_data - 发票数据表
ALTER TABLE `invoice_data` COMMENT = '发票数据表';

-- 35. notification - 通知表
ALTER TABLE `notification` COMMENT = '通知表';

-- 36. notification_channel - 通知渠道配置表
ALTER TABLE `notification_channel` COMMENT = '通知渠道配置表';

-- 37. notification_template - 通知模板表
ALTER TABLE `notification_template` COMMENT = '通知模板表';

-- 38. process_step - 流程步骤表
ALTER TABLE `process_step` COMMENT = '流程步骤表';

-- 39. process_version - 流程版本管理表
ALTER TABLE `process_version` COMMENT = '流程版本管理表';

-- 40. processed_data - 已处理数据表
ALTER TABLE `processed_data` COMMENT = '已处理数据表';

-- 41. recording_record - 录制记录表
ALTER TABLE `recording_record` COMMENT = 'RPA录制记录表';

-- 42. report_subscription - 报表订阅表
ALTER TABLE `report_subscription` COMMENT = '报表订阅表';

-- 43. report_subscription_log - 报表订阅发送记录表
ALTER TABLE `report_subscription_log` COMMENT = '报表订阅发送记录表';

-- 44. resource - 资源表
ALTER TABLE `resource` COMMENT = '系统资源表';

-- 45. robot - 机器人表
ALTER TABLE `robot` COMMENT = 'RPA机器人表';

-- 46. robot_backup - 机器人主备关系表
ALTER TABLE `robot_backup` COMMENT = '机器人主备关系表';

-- 47. robot_category - 机器人分类表
ALTER TABLE `robot_category` COMMENT = '机器人分类表';

-- 48. robot_health - 机器人健康检查表
ALTER TABLE `robot_health` COMMENT = '机器人健康检查表';

-- 49. roles - 角色表
ALTER TABLE `roles` COMMENT = '角色表';

-- 50. rpa_process - RPA流程表
ALTER TABLE `rpa_process` COMMENT = 'RPA流程表';

-- 51. subscription_approval - 订阅审批表
ALTER TABLE `subscription_approval` COMMENT = '订阅审批表';

-- 52. sys_permission - 系统权限表
ALTER TABLE `sys_permission` COMMENT = '系统权限表';

-- 53. sys_role - 系统角色表
ALTER TABLE `sys_role` COMMENT = '系统角色表';

-- 54. sys_role_permission - 系统角色权限关联表
ALTER TABLE `sys_role_permission` COMMENT = '系统角色权限关联表';

-- 55. system_config - 系统配置表
ALTER TABLE `system_config` COMMENT = '系统配置表';

-- 56. task - 任务表
ALTER TABLE `task` COMMENT = 'RPA任务表';

-- 57. task_dependency - 任务依赖编排表（DAG）
ALTER TABLE `task_dependency` COMMENT = '任务依赖编排表(DAG)';

-- 58. task_queue - 任务队列表
ALTER TABLE `task_queue` COMMENT = '任务队列表';

-- 59. trace_log - 链路追踪表
ALTER TABLE `trace_log` COMMENT = '全链路追踪表';

-- 60. trigger_rule - 触发规则表
ALTER TABLE `trigger_rule` COMMENT = 'RPA触发规则表';

-- 61. users - 用户表
ALTER TABLE `users` COMMENT = '用户表';

-- ============================================
-- 执行完成
-- ============================================
