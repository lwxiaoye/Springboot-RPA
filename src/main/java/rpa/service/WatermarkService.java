package rpa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.config.JwtUtils;
import rpa.entity.User;
import rpa.entity.WatermarkAuditLog;
import rpa.entity.WatermarkConfig;
import rpa.entity.WatermarkTemporaryDisable;
import rpa.repository.WatermarkAuditLogRepository;
import rpa.repository.WatermarkConfigRepository;
import rpa.repository.WatermarkTemporaryDisableRepository;
import rpa.repository.UserRepository;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 水印服务类
 * <p>
 * 提供水印配置管理、临时关闭、审计日志记录等功能
 * </p>
 *
 * @author RPA Security Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkService {

    private final WatermarkConfigRepository watermarkConfigRepository;
    private final WatermarkTemporaryDisableRepository disableRepository;
    private final WatermarkAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // 默认配置
    private static final Map<String, WatermarkConfig> DEFAULT_CONFIGS = new HashMap<>();

    static {
        // 极高敏感配置
        WatermarkConfig extreme = new WatermarkConfig();
        extreme.setConfigType("LEVEL_EXTREME");
        extreme.setSensitivityLevel("extreme");
        extreme.setOpacity(0.12);
        extreme.setFontSize(14);
        extreme.setTileWidth(150);
        extreme.setTileHeight(150);
        extreme.setRotation(-45);
        extreme.setRandomOffset(true);
        extreme.setShowTimestamp(true);
        extreme.setColor("#000000");
        extreme.setDescription("极高敏感页面：执行日志、审计日志、凭证中心、数据查询");
        DEFAULT_CONFIGS.put("extreme", extreme);

        // 高敏感配置
        WatermarkConfig high = new WatermarkConfig();
        high.setConfigType("LEVEL_HIGH");
        high.setSensitivityLevel("high");
        high.setOpacity(0.10);
        high.setFontSize(14);
        high.setTileWidth(200);
        high.setTileHeight(200);
        high.setRotation(-45);
        high.setRandomOffset(true);
        high.setShowTimestamp(true);
        high.setColor("#000000");
        high.setDescription("高敏感页面：任务调度中心、机器人管理、队列管理、触发器管理、数据脱敏");
        DEFAULT_CONFIGS.put("high", high);

        // 中敏感配置
        WatermarkConfig medium = new WatermarkConfig();
        medium.setConfigType("LEVEL_MEDIUM");
        medium.setSensitivityLevel("medium");
        medium.setOpacity(0.07);
        medium.setFontSize(12);
        medium.setTileWidth(250);
        medium.setTileHeight(250);
        medium.setRotation(-45);
        medium.setRandomOffset(true);
        medium.setShowTimestamp(true);
        medium.setColor("#000000");
        medium.setDescription("中敏感页面：流程仓库、脚本执行、分布式锁、报表分析");
        DEFAULT_CONFIGS.put("medium", medium);

        // 低敏感配置
        WatermarkConfig low = new WatermarkConfig();
        low.setConfigType("LEVEL_LOW");
        low.setSensitivityLevel("low");
        low.setOpacity(0.05);
        low.setFontSize(12);
        low.setTileWidth(300);
        low.setTileHeight(300);
        low.setRotation(-45);
        low.setRandomOffset(true);
        low.setShowTimestamp(true);
        low.setColor("#000000");
        low.setDescription("低敏感页面：系统设置、通知管理");
        DEFAULT_CONFIGS.put("low", low);
    }

    /**
     * 初始化默认配置
     */
    @Transactional
    public void initDefaultConfigs() {
        for (Map.Entry<String, WatermarkConfig> entry : DEFAULT_CONFIGS.entrySet()) {
            String level = entry.getKey();
            WatermarkConfig config = entry.getValue();

            if (!watermarkConfigRepository.findBySensitivityLevel(level).isPresent()) {
                config.setCreateTime(LocalDateTime.now());
                watermarkConfigRepository.save(config);
                log.info("已初始化水印配置: {}", level);
            }
        }
    }

    /**
     * 获取指定敏感级别的配置
     */
    public WatermarkConfig getConfigBySensitivity(String sensitivityLevel) {
        return watermarkConfigRepository.findBySensitivityLevel(sensitivityLevel)
            .orElse(DEFAULT_CONFIGS.getOrDefault(sensitivityLevel, DEFAULT_CONFIGS.get("low")));
    }

    /**
     * 获取所有配置
     */
    public List<WatermarkConfig> getAllConfigs() {
        List<WatermarkConfig> configs = watermarkConfigRepository.findAllByOrderByConfigTypeAsc();
        if (configs.isEmpty()) {
            initDefaultConfigs();
            configs = watermarkConfigRepository.findAllByOrderByConfigTypeAsc();
        }
        return configs;
    }

    /**
     * 更新配置
     */
    @Transactional
    public WatermarkConfig updateConfig(Long configId, WatermarkConfig newConfig, Long userId, String userName) {
        WatermarkConfig config = watermarkConfigRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("配置不存在"));

        config.setOpacity(newConfig.getOpacity());
        config.setFontSize(newConfig.getFontSize());
        config.setTileWidth(newConfig.getTileWidth());
        config.setTileHeight(newConfig.getTileHeight());
        config.setRotation(newConfig.getRotation());
        config.setRandomOffset(newConfig.getRandomOffset());
        config.setShowTimestamp(newConfig.getShowTimestamp());
        config.setColor(newConfig.getColor());
        config.setEnabled(newConfig.getEnabled());
        config.setUpdateBy(userId);
        config.setUpdateByName(userName);
        config.setUpdateTime(LocalDateTime.now());

        WatermarkConfig saved = watermarkConfigRepository.save(config);

        // 记录审计日志
        auditLogService.log("WATERMARK", "CONFIG_UPDATE", "WatermarkConfig", configId,
            config.getSensitivityLevel(), "更新水印配置: " + config.getSensitivityLevel(),
            "medium", "success", null);

        return saved;
    }

    /**
     * 检查用户是否有生效中的水印关闭记录
     */
    public boolean hasActiveDisableRecord(Long userId) {
        return disableRepository.existsByUserIdAndStatus(userId, "ACTIVE");
    }

    /**
     * 临时关闭水印（仅管理员）
     */
    @Transactional
    public WatermarkTemporaryDisable temporaryDisable(Long userId, String userName, String reason,
                                                      int durationMinutes, HttpServletRequest request) {
        // 验证是否为管理员
        if (!isAdmin(userId)) {
            throw new RuntimeException("权限不足：仅超级管理员可临时关闭水印");
        }

        // 检查是否已有生效中的关闭记录
        if (hasActiveDisableRecord(userId)) {
            throw new RuntimeException("已存在生效中的关闭记录，请先恢复或等待自动恢复");
        }

        // 限制关闭时长（最大24小时）
        if (durationMinutes > 1440) {
            durationMinutes = 1440;
        }

        LocalDateTime now = LocalDateTime.now();
        WatermarkTemporaryDisable disable = new WatermarkTemporaryDisable();
        disable.setUserId(userId);
        disable.setUserName(userName);
        disable.setReason(reason);
        disable.setStartTime(now);
        disable.setScheduledRestoreTime(now.plusMinutes(durationMinutes));
        disable.setDurationMinutes(durationMinutes);
        disable.setIpAddress(getClientIp(request));
        disable.setUserAgent(request.getHeader("User-Agent"));
        disable.setStatus("ACTIVE");
        disable.setManualRestore(false);

        WatermarkTemporaryDisable saved = disableRepository.save(disable);

        // 记录审计日志
        auditLogService.log("WATERMARK", "WATERMARK_DISABLED", "WatermarkTemporaryDisable", saved.getId(),
            userName, "临时关闭水印: " + reason, "high", "success",
            Map.of("duration", durationMinutes, "reason", reason));

        // 记录到水印专用审计日志
        logWatermarkEvent("WATERMARK_DISABLED", userId, userName, request,
            "临时关闭水印，原因：" + reason + "，时长：" + durationMinutes + "分钟",
            null, null, "HIGH");

        return saved;
    }

    /**
     * 手动恢复水印
     */
    @Transactional
    public void restoreWatermark(Long userId, String userName, HttpServletRequest request) {
        Optional<WatermarkTemporaryDisable> optDisable =
            disableRepository.findByUserIdAndStatus(userId, "ACTIVE");

        if (optDisable.isEmpty()) {
            throw new RuntimeException("没有找到生效中的关闭记录");
        }

        WatermarkTemporaryDisable disable = optDisable.get();
        disable.setStatus("RESTORED");
        disable.setActualRestoreTime(LocalDateTime.now());
        disable.setManualRestore(true);
        disableRepository.save(disable);

        // 记录审计日志
        auditLogService.log("WATERMARK", "WATERMARK_ENABLED", "WatermarkTemporaryDisable", disable.getId(),
            userName, "手动恢复水印", "medium", "success", null);

        // 记录到水印专用审计日志
        logWatermarkEvent("WATERMARK_ENABLED", userId, userName, request,
            "手动恢复水印", null, null, "MEDIUM");
    }

    /**
     * 记录水印篡改检测事件
     */
    public void logTamperingEvent(Long userId, String userName, String tamperReason,
                                   String tamperDetails, HttpServletRequest request) {
        WatermarkAuditLog auditLog = new WatermarkAuditLog();
        auditLog.setEventType("WATERMARK_TAMPERING");
        auditLog.setUserId(userId);
        auditLog.setUserName(userName);
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLog.setPageUrl(request.getRequestURI());
        auditLog.setTamperReason(tamperReason);
        auditLog.setTamperDetails(tamperDetails);
        auditLog.setRiskLevel("CRITICAL");
        auditLog.setStatus("DETECTED");
        auditLog.setEventTime(LocalDateTime.now());

        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("detectedAt", LocalDateTime.now().toString());
        additionalData.put("sessionId", request.getSession(false) != null ? request.getSession().getId() : null);
        try {
            auditLog.setAdditionalData(objectMapper.writeValueAsString(additionalData));
        } catch (Exception e) {
            log.error("序列化附加数据失败", e);
        }

        auditLogRepository.save(auditLog);

        // 记录主审计日志
        auditLogService.log("WATERMARK", "WATERMARK_TAMPERING", "WatermarkAuditLog", auditLog.getId(),
            userName, "检测到水印篡改: " + tamperReason, "critical", "success",
            Map.of("reason", tamperReason, "details", tamperDetails));
    }

    /**
     * 记录截图事件
     */
    public void logScreenshotEvent(Long userId, String userName, String pageUrl, HttpServletRequest request) {
        WatermarkAuditLog auditLog = new WatermarkAuditLog();
        auditLog.setEventType("SCREENSHOT_CAPTURED");
        auditLog.setUserId(userId);
        auditLog.setUserName(userName);
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLog.setPageUrl(pageUrl);
        auditLog.setRiskLevel("MEDIUM");
        auditLog.setStatus("DETECTED");
        auditLog.setEventTime(LocalDateTime.now());

        auditLogRepository.save(auditLog);

        // 记录主审计日志
        auditLogService.log("WATERMARK", "SCREENSHOT", "WatermarkAuditLog", auditLog.getId(),
            userName, "用户截图操作", "medium", "success",
            Map.of("pageUrl", pageUrl));
    }

    /**
     * 记录打印事件
     */
    public void logPrintEvent(Long userId, String userName, String pageUrl, HttpServletRequest request) {
        WatermarkAuditLog auditLog = new WatermarkAuditLog();
        auditLog.setEventType("PRINT_ACTION");
        auditLog.setUserId(userId);
        auditLog.setUserName(userName);
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLog.setPageUrl(pageUrl);
        auditLog.setRiskLevel("MEDIUM");
        auditLog.setStatus("DETECTED");
        auditLog.setEventTime(LocalDateTime.now());

        auditLogRepository.save(auditLog);

        // 记录主审计日志
        auditLogService.log("WATERMARK", "PRINT", "WatermarkAuditLog", auditLog.getId(),
            userName, "用户打印操作", "low", "success",
            Map.of("pageUrl", pageUrl));
    }

    /**
     * 记录导出文件事件
     */
    public void logExportEvent(Long userId, String userName, String fileType, String fileName,
                               HttpServletRequest request) {
        WatermarkAuditLog auditLog = new WatermarkAuditLog();
        auditLog.setEventType("EXPORT_WITH_WATERMARK");
        auditLog.setUserId(userId);
        auditLog.setUserName(userName);
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLog.setExportFileType(fileType);
        auditLog.setExportFileName(fileName);
        auditLog.setRiskLevel("LOW");
        auditLog.setStatus("DETECTED");
        auditLog.setEventTime(LocalDateTime.now());

        auditLogRepository.save(auditLog);

        // 记录主审计日志
        auditLogService.log("WATERMARK", "EXPORT", "WatermarkAuditLog", auditLog.getId(),
            userName, "导出文件已添加水印: " + fileName, "low", "success",
            Map.of("fileType", fileType, "fileName", fileName));
    }

    /**
     * 查询水印审计日志
     */
    public Page<WatermarkAuditLog> queryAuditLogs(Long userId, String eventType, String riskLevel,
                                                   String status, LocalDateTime startTime,
                                                   LocalDateTime endTime, int page, int size) {
        return auditLogRepository.findByConditions(userId, eventType, riskLevel, status,
            startTime, endTime, PageRequest.of(page, size));
    }

    /**
     * 获取水印审计统计数据
     */
    public Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("tamperingCount", auditLogRepository.countByEventTypeAndCreateTimeAfter(
            "WATERMARK_TAMPERING", startTime));
        stats.put("screenshotCount", auditLogRepository.countByEventTypeAndCreateTimeAfter(
            "SCREENSHOT_CAPTURED", startTime));
        stats.put("printCount", auditLogRepository.countByEventTypeAndCreateTimeAfter(
            "PRINT_ACTION", startTime));
        stats.put("exportCount", auditLogRepository.countByEventTypeAndCreateTimeAfter(
            "EXPORT_WITH_WATERMARK", startTime));
        stats.put("criticalCount", auditLogRepository.countByRiskLevelAndCreateTimeAfter(
            "CRITICAL", startTime));
        stats.put("highCount", auditLogRepository.countByRiskLevelAndCreateTimeAfter(
            "HIGH", startTime));

        return stats;
    }

    /**
     * 导出水印操作日志
     */
    public List<WatermarkAuditLog> exportLogs(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findAllByTimeRange(startTime, endTime);
    }

    /**
     * 自动恢复过期的关闭记录（定时任务）
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Transactional
    public void autoRestoreExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        List<WatermarkTemporaryDisable> expiredRecords = disableRepository.findExpiredRecords(now);

        for (WatermarkTemporaryDisable record : expiredRecords) {
            record.setStatus("RESTORED");
            record.setActualRestoreTime(now);
            record.setManualRestore(false);
            disableRepository.save(record);

            log.info("自动恢复水印: userId={}", record.getUserId());

            // 记录审计日志
            auditLogService.log("WATERMARK", "WATERMARK_AUTO_RESTORED",
                "WatermarkTemporaryDisable", record.getId(),
                record.getUserName(), "水印自动恢复（临时关闭时长到期）",
                "low", "success", null);
        }
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 检查是否为管理员
     */
    private boolean isAdmin(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        return userOpt.map(user -> user.getRole() == 1).orElse(false);
    }

    /**
     * 获取用户的关闭记录列表
     */
    public List<WatermarkTemporaryDisable> getDisableRecordsByUser(Long userId) {
        return disableRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 记录水印事件（通用方法）
     */
    private void logWatermarkEvent(String eventType, Long userId, String userName,
                                    HttpServletRequest request, String description,
                                    String screenshotMethod, String printContentType,
                                    String riskLevel) {
        WatermarkAuditLog auditLog = new WatermarkAuditLog();
        auditLog.setEventType(eventType);
        auditLog.setUserId(userId);
        auditLog.setUserName(userName);
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLog.setPageUrl(request.getRequestURI());
        auditLog.setDescription(description);
        auditLog.setRiskLevel(riskLevel);
        auditLog.setStatus("DETECTED");
        auditLog.setEventTime(LocalDateTime.now());

        if (screenshotMethod != null) {
            auditLog.setScreenshotMethod(screenshotMethod);
        }
        if (printContentType != null) {
            auditLog.setPrintContentType(printContentType);
        }

        auditLogRepository.save(auditLog);
    }

    /**
     * 计算哈希值
     */
    private String calculateHash(Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
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
}
