package rpa.service;

import rpa.entity.AuditLog;
import rpa.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审计日志服务
 * <p>
 * 提供统一的审计日志记录功能，支持：
 * - 操作记录自动采集（通过AOP切面）
 * - 手动记录特定操作
 * - 日志查询和统计
 * </p>
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录审计日志
     */
    public AuditLog log(String module, String action, String description, String riskLevel) {
        return log(module, action, null, null, null, description, riskLevel, null, null);
    }

    public AuditLog log(String module, String action, String targetType, Long targetId,
                        String targetName, String description, String riskLevel, String status) {
        return log(module, action, targetType, targetId, targetName, description, riskLevel, status, null);
    }

    public AuditLog log(String module, String action, String targetType, Long targetId,
                        String targetName, String description, String riskLevel, String status,
                        Map<String, Object> requestParams) {
        AuditLog auditLog = new AuditLog();
        auditLog.setModule(module);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setTargetName(targetName);
        auditLog.setDescription(description);
        auditLog.setRiskLevel(riskLevel != null ? riskLevel : "low");
        auditLog.setStatus(status != null ? status : "success");
        auditLog.setCreateTime(LocalDateTime.now());

        // 获取当前请求上下文
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIp(getClientIp(request));

                // 从请求中获取用户信息（需要结合实际认证机制）
                String userId = request.getHeader("X-User-Id");
                String userName = request.getHeader("X-User-Name");
                if (userId != null) auditLog.setUserId(Long.parseLong(userId));
                if (userName != null) auditLog.setUserName(userName);
            }
        } catch (Exception e) {
            // 忽略获取请求上下文的异常
        }

        // 存储请求参数
        if (requestParams != null && !requestParams.isEmpty()) {
            try {
                auditLog.setRequestParams(objectMapper.writeValueAsString(requestParams));
            } catch (Exception e) {
                auditLog.setRequestParams(requestParams.toString());
            }
        }

        // 计算哈希值（用于完整性验证）
        auditLog.setHash(calculateHash(auditLog));

        return auditLogRepository.save(auditLog);
    }

    /**
     * 记录登录日志
     */
    public void logLogin(Long userId, String userName, boolean success, String reason) {
        String action = success ? "USER_LOGIN" : "USER_LOGIN_FAILED";
        String description = success ? "用户登录成功" : "用户登录失败: " + reason;
        String riskLevel = success ? "low" : "medium";

        AuditLog log = log("AUTH", action, "User", userId, userName, description, riskLevel,
            success ? "success" : "failed", Map.of("reason", reason));
    }

    /**
     * 记录机器人操作
     */
    public void logRobotCreate(Long robotId, String robotName, String robotCode) {
        log("ROBOT", "ROBOT_CREATE", "Robot", robotId, robotName,
            "创建机器人: " + robotName, "medium", "success",
            Map.of("category", extractCategory(robotCode)));
    }

    public void logRobotUpdate(Long robotId, String robotName, Map<String, Object> changes) {
        log("ROBOT", "ROBOT_UPDATE", "Robot", robotId, robotName,
            "更新机器人: " + robotName, "medium", "success", changes);
    }

    public void logRobotDelete(Long robotId, String robotName) {
        log("ROBOT", "ROBOT_DELETE", "Robot", robotId, robotName,
            "删除机器人: " + robotName, "high", "success", null);
    }

    public void logRobotExecute(Long robotId, String robotName, boolean success, String message) {
        String action = success ? "ROBOT_EXECUTE_SUCCESS" : "ROBOT_EXECUTE_FAILED";
        String riskLevel = success ? "low" : "medium";
        log("ROBOT", action, "Robot", robotId, robotName,
            "执行机器人: " + robotName + (success ? " - 成功" : " - 失败: " + message),
            riskLevel, success ? "success" : "failed",
            Map.of("message", message));
    }

    /**
     * 记录任务操作
     */
    public void logTaskCreate(Long taskId, String taskName, String cron) {
        log("TASK", "TASK_CREATE", "Task", taskId, taskName,
            "创建任务: " + taskName + " (调度: " + cron + ")", "medium", "success",
            Map.of("cron", cron));
    }

    public void logTaskExecute(Long taskId, String taskName, boolean success, String message) {
        String action = success ? "TASK_EXECUTE_SUCCESS" : "TASK_EXECUTE_FAILED";
        String riskLevel = success ? "low" : "medium";
        log("TASK", action, "Task", taskId, taskName,
            (success ? "任务执行成功" : "任务执行失败") + ": " + taskName,
            riskLevel, success ? "success" : "failed",
            Map.of("message", message));
    }

    /**
     * 记录凭据操作（注意：不记录凭据内容）
     */
    public void logCredentialCreate(Long credentialId, String credentialName, String type) {
        log("CREDENTIAL", "CREDENTIAL_CREATE", "Credential", credentialId, credentialName,
            "创建凭据: " + credentialName + " (类型: " + type + ")", "high", "success",
            Map.of("type", type));
    }

    public void logCredentialAccess(Long credentialId, String credentialName, String reason) {
        log("CREDENTIAL", "CREDENTIAL_ACCESS", "Credential", credentialId, credentialName,
            "访问凭据: " + credentialName, "medium", "success",
            Map.of("reason", reason));
    }

    public void logCredentialDelete(Long credentialId, String credentialName) {
        log("CREDENTIAL", "CREDENTIAL_DELETE", "Credential", credentialId, credentialName,
            "删除凭据: " + credentialName, "high", "success", null);
    }

    /**
     * 记录系统配置变更
     */
    public void logConfigChange(String configKey, String oldValue, String newValue) {
        log("SYSTEM", "CONFIG_CHANGE", "SystemConfig", null, configKey,
            "修改系统配置: " + configKey, "high", "success",
            Map.of("oldValue", oldValue, "newValue", newValue));
    }

    /**
     * 保存审计日志（直接保存实体）
     */
    public AuditLog log(AuditLog auditLog) {
        auditLog.setHash(calculateHash(auditLog));
        return auditLogRepository.save(auditLog);
    }

    /**
     * 根据用户ID查询日志
     */
    public List<AuditLog> findByUserId(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByUserIdAndCreateTimeBetween(userId, startTime, endTime);
    }

    /**
     * 记录队列操作
     */
    public void logQueue(Long queueId, String userName, String queueName, String action, String description) {
        log("QUEUE", "QUEUE_" + action, "Queue", queueId, queueName,
            description, "medium", "success", null);
    }

    /**
     * 记录触发器操作
     */
    public void logTrigger(Long triggerId, String userName, String triggerName, String action, String description) {
        log("TRIGGER", "TRIGGER_" + action, "Trigger", triggerId, triggerName,
            description, "medium", "success", null);
    }

    /**
     * 查询审计日志
     */
    public List<AuditLog> queryLogs(String module, String action, LocalDateTime startTime,
                                     LocalDateTime endTime, Long userId, String riskLevel,
                                     int page, int size) {
        return auditLogRepository.findAuditLogs(module, action, riskLevel, startTime, endTime,
            org.springframework.data.domain.PageRequest.of(page, size)).getContent();
    }

    /**
     * 获取统计数据
     */
    public Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", auditLogRepository.countByModuleAndCreateTimeAfter(null, startTime));
        stats.put("today", auditLogRepository.countByModuleAndCreateTimeAfter(null,
            LocalDateTime.now().withHour(0).withMinute(0)));
        stats.put("highRisk", auditLogRepository.countByRiskLevelAndCreateTimeAfter("high",
            LocalDateTime.now().minusDays(30)));

        return stats;
    }

    /**
     * 导出审计日志
     */
    public List<AuditLog> exportLogs(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByCreateTimeBetween(startTime, endTime);
    }

    /**
     * 验证日志完整性
     */
    public boolean verifyLogIntegrity(AuditLog auditLog) {
        String expectedHash = calculateHash(auditLog);
        return expectedHash.equals(auditLog.getHash());
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 计算哈希值
     */
    private String calculateHash(AuditLog log) {
        try {
            String data = String.join("|",
                String.valueOf(log.getModule()),
                String.valueOf(log.getAction()),
                String.valueOf(log.getUserId()),
                String.valueOf(log.getTargetId()),
                String.valueOf(log.getCreateTime()),
                String.valueOf(log.getDescription())
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 从机器人代码中提取分类
     */
    private String extractCategory(String code) {
        if (code == null) return "GENERAL";
        if (code.contains("@collect")) return "DATA_COLLECT";
        if (code.contains("@parse")) return "DATA_PARSE";
        if (code.contains("@process")) return "DATA_PROCESS";
        return "GENERAL";
    }
}