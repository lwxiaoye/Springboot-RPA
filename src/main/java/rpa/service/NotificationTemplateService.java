package rpa.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.NotificationTemplate;
import rpa.repository.NotificationTemplateRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知模板服务
 * <p>
 * 提供通知模板的CRUD操作和变量替换功能。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;

    // 变量替换正则表达式：${变量名}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * 查询所有模板
     */
    public List<NotificationTemplate> findAll() {
        return templateRepository.findAll();
    }

    /**
     * 根据ID查询模板
     */
    public Optional<NotificationTemplate> findById(Long id) {
        return templateRepository.findById(id);
    }

    /**
     * 根据编码查询模板
     */
    public Optional<NotificationTemplate> findByCode(String code) {
        return templateRepository.findByCode(code);
    }

    /**
     * 根据类型查询模板列表
     */
    public List<NotificationTemplate> findByType(String type) {
        return templateRepository.findByTypeOrderByCreateTimeDesc(type);
    }

    /**
     * 获取启用的模板
     */
    public List<NotificationTemplate> findEnabled() {
        return templateRepository.findByEnabled(1);
    }

    /**
     * 获取指定类型和渠道的模板
     */
    public Optional<NotificationTemplate> findByTypeAndChannel(String type, String channel) {
        return templateRepository.findByTypeAndChannelsContainingAndEnabled(type, channel, 1);
    }

    /**
     * 创建模板
     */
    @Transactional
    public NotificationTemplate create(NotificationTemplate template) {
        if (templateRepository.existsByCode(template.getCode())) {
            throw new RuntimeException("模板编码已存在: " + template.getCode());
        }
        template.setCreateTime(LocalDateTime.now());
        template.setUseCount(0);
        return templateRepository.save(template);
    }

    /**
     * 更新模板
     */
    @Transactional
    public NotificationTemplate update(Long id, NotificationTemplate template) {
        return templateRepository.findById(id).map(existing -> {
            existing.setName(template.getName());
            existing.setContent(template.getContent());
            existing.setDescription(template.getDescription());
            existing.setEnabled(template.getEnabled());
            existing.setIsDefault(template.getIsDefault());
            existing.setChannels(template.getChannels());
            existing.setSubjectTemplate(template.getSubjectTemplate());
            existing.setUpdateTime(LocalDateTime.now());
            return templateRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("模板不存在: " + id));
    }

    /**
     * 删除模板
     */
    @Transactional
    public void delete(Long id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + id));

        // 不允许删除系统默认模板
        if (template.getIsDefault() == 1 && "system".equals(template.getCreatorName())) {
            throw new RuntimeException("系统默认模板不允许删除");
        }

        templateRepository.deleteById(id);
    }

    /**
     * 切换模板状态
     */
    @Transactional
    public NotificationTemplate toggleEnabled(Long id) {
        return templateRepository.findById(id).map(template -> {
            template.setEnabled(template.getEnabled() == 1 ? 0 : 1);
            template.setUpdateTime(LocalDateTime.now());
            return templateRepository.save(template);
        }).orElseThrow(() -> new RuntimeException("模板不存在: " + id));
    }

    /**
     * 设置默认模板
     */
    @Transactional
    public void setDefault(Long id, String type) {
        // 取消该类型的其他默认模板
        templateRepository.findByTypeAndIsDefault(type, 1).ifPresent(existing -> {
            existing.setIsDefault(0);
            templateRepository.save(existing);
        });

        // 设置新的默认模板
        templateRepository.findById(id).ifPresent(template -> {
            template.setIsDefault(1);
            templateRepository.save(template);
        });
    }

    /**
     * 使用变量替换处理模板
     * <p>
     * 支持的变量格式：${变量名}
     * </p>
     *
     * @param templateCode 模板编码
     * @param variables   变量Map
     * @return 替换后的内容
     */
    public String processTemplate(String templateCode, Map<String, Object> variables) {
        NotificationTemplate template = templateRepository.findByCode(templateCode)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateCode));

        return processTemplateContent(template.getContent(), variables);
    }

    /**
     * 处理模板内容
     */
    public String processTemplateContent(String content, Map<String, Object> variables) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = value != null ? value.toString() : "";
            // 处理特殊字符
            replacement = replacement.replace("\\", "\\\\")
                    .replace("$", "\\$");
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 处理邮件主题
     */
    public String processSubject(String subjectTemplate, Map<String, Object> variables) {
        if (subjectTemplate == null || subjectTemplate.isEmpty()) {
            return "RPA系统通知";
        }
        return processTemplateContent(subjectTemplate, variables);
    }

    /**
     * 获取模板中使用的所有变量
     */
    public java.util.Set<String> extractVariables(String content) {
        java.util.Set<String> variables = new java.util.HashSet<>();
        if (content == null) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    /**
     * 验证模板语法
     */
    public ValidationResult validateTemplate(String content) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        if (content == null || content.isEmpty()) {
            result.setValid(false);
            result.setMessage("模板内容不能为空");
            return result;
        }

        // 检查未闭合的变量
        long openCount = content.chars().filter(c -> c == '{').count();
        long closeCount = content.chars().filter(c -> c == '}').count();
        if (openCount != closeCount) {
            result.setValid(false);
            result.setMessage("变量格式错误：花括号不匹配");
            return result;
        }

        // 检查变量格式
        java.util.Set<String> variables = extractVariables(content);
        result.setVariables(variables);

        return result;
    }

    /**
     * 初始化默认模板
     */
    @Transactional
    public void initDefaultTemplates() {
        initTaskCompleteTemplate();
        initTaskFailedTemplate();
        initSubscriptionTemplate();
        initAlertTemplate();
        log.info("默认通知模板初始化完成");
    }

    private void initTaskCompleteTemplate() {
        if (templateRepository.findByCode("task_complete").isPresent()) {
            return;
        }

        NotificationTemplate template = new NotificationTemplate();
        template.setName("任务完成通知");
        template.setCode("task_complete");
        template.setType("task_complete");
        template.setChannels("email,dingtalk,wecom");
        template.setSubjectTemplate("【RPA通知】任务已完成：${taskName}");
        template.setContent(getDefaultTaskCompleteTemplate());
        template.setDescription("任务成功完成时发送的通知模板");
        template.setEnabled(1);
        template.setIsDefault(1);
        template.setCreatorName("system");
        templateRepository.save(template);
    }

    private void initTaskFailedTemplate() {
        if (templateRepository.findByCode("task_failed").isPresent()) {
            return;
        }

        NotificationTemplate template = new NotificationTemplate();
        template.setName("任务失败通知");
        template.setCode("task_failed");
        template.setType("task_failed");
        template.setChannels("email,dingtalk,wecom");
        template.setSubjectTemplate("【RPA告警】任务执行失败：${taskName}");
        template.setContent(getDefaultTaskFailedTemplate());
        template.setDescription("任务执行失败时发送的通知模板");
        template.setEnabled(1);
        template.setIsDefault(1);
        template.setCreatorName("system");
        templateRepository.save(template);
    }

    private void initSubscriptionTemplate() {
        if (templateRepository.findByCode("subscription").isPresent()) {
            return;
        }

        NotificationTemplate template = new NotificationTemplate();
        template.setName("报表订阅通知");
        template.setCode("subscription");
        template.setType("subscription");
        template.setChannels("email,dingtalk,wecom");
        template.setSubjectTemplate("【RPA报表】${period} - ${subscriptionName}");
        template.setContent(getDefaultSubscriptionTemplate());
        template.setDescription("报表订阅定时发送的通知模板");
        template.setEnabled(1);
        template.setIsDefault(1);
        template.setCreatorName("system");
        templateRepository.save(template);
    }

    private void initAlertTemplate() {
        if (templateRepository.findByCode("alert").isPresent()) {
            return;
        }

        NotificationTemplate template = new NotificationTemplate();
        template.setName("系统告警通知");
        template.setCode("alert");
        template.setType("alert");
        template.setChannels("email,dingtalk,wecom");
        template.setSubjectTemplate("【RPA告警-${level}】${alertTitle}");
        template.setContent(getDefaultAlertTemplate());
        template.setDescription("系统告警时发送的通知模板");
        template.setEnabled(1);
        template.setIsDefault(1);
        template.setCreatorName("system");
        templateRepository.save(template);
    }

    // ==================== 默认模板内容 ====================

    private String getDefaultTaskCompleteTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        padding: 20px; border-radius: 10px 10px 0 0; color: white; }
                    .content { background: #f8f9fa; padding: 20px; border-radius: 0 0 10px 10px; }
                    .info-table { width: 100%%; border-collapse: collapse; }
                    .info-table td { padding: 10px; border-bottom: 1px solid #ddd; }
                    .success { color: #28a745; font-weight: bold; }
                    .btn { display: inline-block; padding: 12px 30px; background: #667eea;
                        color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd;
                        color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2 style="margin:0;">✅ 任务执行完成</h2>
                    </div>
                    <div class="content">
                        <p>尊敬的用户，您的RPA任务已成功完成。</p>
                        <table class="info-table">
                            <tr><td><strong>任务名称：</strong></td><td>${taskName}</td></tr>
                            <tr><td><strong>任务ID：</strong></td><td>${taskId}</td></tr>
                            <tr><td><strong>执行状态：</strong></td><td class="success">✅ 成功</td></tr>
                            <tr><td><strong>开始时间：</strong></td><td>${startTime}</td></tr>
                            <tr><td><strong>完成时间：</strong></td><td>${endTime}</td></tr>
                            <tr><td><strong>执行耗时：</strong></td><td>${duration}</td></tr>
                            ${attachmentSection}
                        </table>
                        <div style="text-align: center;">
                            <a href="${reportUrl}" class="btn">查看报表详情 →</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>此邮件由RPA系统自动发送，请勿回复。</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getDefaultTaskFailedTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; }
                    .header { background: linear-gradient(135deg, #f56c6c 0%, #e64a19 100%);
                        padding: 20px; border-radius: 10px 10px 0 0; color: white; }
                    .content { background: #fff3e0; padding: 20px; border-radius: 0 0 10px 10px; }
                    .info-table { width: 100%%; border-collapse: collapse; }
                    .info-table td { padding: 10px; border-bottom: 1px solid #ddd; }
                    .error-box { background: #ffebee; padding: 15px; border-radius: 5px;
                        border-left: 4px solid #f56c6c; margin: 15px 0; }
                    .btn { display: inline-block; padding: 12px 30px; background: #f56c6c;
                        color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd;
                        color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2 style="margin:0;">⚠️ 任务执行失败</h2>
                    </div>
                    <div class="content">
                        <p>尊敬的用户，您的RPA任务执行失败，请及时处理。</p>
                        <table class="info-table">
                            <tr><td><strong>任务名称：</strong></td><td>${taskName}</td></tr>
                            <tr><td><strong>任务ID：</strong></td><td>${taskId}</td></tr>
                            <tr><td><strong>执行状态：</strong></td><td style="color:#dc3545;"><strong>❌ 失败</strong></td></tr>
                            <tr><td><strong>开始时间：</strong></td><td>${startTime}</td></tr>
                        </table>
                        <div class="error-box">
                            <strong style="color:#dc3545;">错误信息：</strong>
                            <p style="margin:10px 0 0 0;">${errorMessage}</p>
                        </div>
                        <div style="text-align: center;">
                            <a href="${reportUrl}" class="btn">查看任务详情 →</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>此邮件由RPA系统自动发送，请勿回复。如有问题，请联系管理员。</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getDefaultSubscriptionTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                    .container { max-width: 800px; margin: 0 auto; }
                    .header { background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
                        padding: 20px; border-radius: 10px 10px 0 0; color: white; }
                    .content { background: #f8f9fa; padding: 20px; border-radius: 0 0 10px 10px; }
                    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin-bottom: 20px; }
                    .stat-card { background: white; padding: 20px; border-radius: 8px; text-align: center; }
                    .stat-value { font-size: 24px; font-weight: bold; }
                    .stat-label { color: #666; font-size: 12px; }
                    .btn { display: inline-block; padding: 12px 30px; background: #409eff;
                        color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd;
                        color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2 style="margin:0;">📊 RPA报表订阅 - ${period}</h2>
                        <p style="margin:10px 0 0 0;">${subscriptionName}</p>
                    </div>
                    <div class="content">
                        <p><strong>报表类型：</strong>${reportType} | <strong>生成时间：</strong>${generateTime}</p>
                        <div class="stats-grid">
                            <div class="stat-card">
                                <div class="stat-value" style="color:#409eff;">${totalExecutions}</div>
                                <div class="stat-label">总执行次数</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-value" style="color:#67c23a;">${successRate}%%</div>
                                <div class="stat-label">成功率</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-value" style="color:#e6a23c;">${totalData}</div>
                                <div class="stat-label">数据采集量</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-value" style="color:#f56c6c;">${failedCount}</div>
                                <div class="stat-label">失败次数</div>
                            </div>
                        </div>
                        <div style="text-align: center;">
                            <a href="${reportUrl}" class="btn">查看完整报表 →</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>此报表由RPA系统自动生成，如需修改订阅设置，请联系管理员。</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getDefaultAlertTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; }
                    .header { background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
                        padding: 20px; border-radius: 10px 10px 0 0; color: white; }
                    .header.critical { background: linear-gradient(135deg, #dc3545 0%, #c82333 100%); }
                    .content { background: #fff3cd; padding: 20px; border-radius: 0 0 10px 10px; }
                    .level-badge { display: inline-block; padding: 4px 12px; border-radius: 4px;
                        font-size: 12px; font-weight: bold; margin-bottom: 10px; }
                    .level-badge.info { background: #17a2b8; color: white; }
                    .level-badge.warning { background: #ffc107; color: #333; }
                    .level-badge.error { background: #dc3545; color: white; }
                    .level-badge.critical { background: #721c24; color: white; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd;
                        color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header ${level == 'critical' || level == 'error' ? 'critical' : ''}">
                        <h2 style="margin:0;">🚨 系统告警</h2>
                    </div>
                    <div class="content">
                        <span class="level-badge ${level}">${level.toUpperCase()}</span>
                        <h3 style="margin:10px 0;">${alertTitle}</h3>
                        <p>${alertContent}</p>
                        <p style="color:#666;font-size:12px;">发送时间：${alertTime}</p>
                    </div>
                    <div class="footer">
                        <p>此告警由RPA系统自动发送，请及时处理。</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    /**
     * 模板验证结果
     */
    @Data
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private java.util.Set<String> variables;
    }
}
