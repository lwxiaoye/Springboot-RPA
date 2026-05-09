package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import rpa.entity.User;
import rpa.entity.WatermarkAuditLog;
import rpa.entity.WatermarkConfig;
import rpa.entity.WatermarkTemporaryDisable;
import rpa.repository.UserRepository;
import rpa.service.WatermarkService;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 水印控制器
 * <p>
 * 提供水印配置、临时关闭、审计日志查询等API接口
 * </p>
 *
 * @author RPA Security Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@Slf4j
@RestController
@RequestMapping("/api/watermark")
@RequiredArgsConstructor
public class WatermarkController {

    private final WatermarkService watermarkService;
    private final UserRepository userRepository;

    /**
     * 获取当前用户信息
     */
    private Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole());
        return userInfo;
    }

    /**
     * 获取所有水印配置
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        List<WatermarkConfig> configs = watermarkService.getAllConfigs();
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", configs
        ));
    }

    /**
     * 获取指定敏感级别的配置
     */
    @GetMapping("/config/{sensitivity}")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String sensitivity) {
        WatermarkConfig config = watermarkService.getConfigBySensitivity(sensitivity);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", config
        ));
    }

    /**
     * 更新水印配置（仅管理员）
     */
    @PutMapping("/config/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable Long id,
            @RequestBody WatermarkConfig newConfig) {
        Map<String, Object> userInfo = getCurrentUser();
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "code", 401,
                "message", "未登录或登录已过期"
            ));
        }

        try {
            WatermarkConfig updated = watermarkService.updateConfig(id, newConfig,
                (Long) userInfo.get("id"),
                (String) userInfo.get("realName"));
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "配置更新成功",
                "data", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 检查用户是否有生效中的关闭记录
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWatermarkStatus() {
        Map<String, Object> userInfo = getCurrentUser();
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "code", 401,
                "message", "未登录或登录已过期"
            ));
        }

        boolean hasActiveDisable = watermarkService.hasActiveDisableRecord((Long) userInfo.get("id"));
        WatermarkConfig config = watermarkService.getConfigBySensitivity("low");

        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", Map.of(
                "watermarkEnabled", !hasActiveDisable,
                "activeDisableRecord", hasActiveDisable,
                "defaultConfig", config
            )
        ));
    }

    /**
     * 临时关闭水印（仅管理员）
     */
    @PostMapping("/temporary-disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> temporaryDisable(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Map<String, Object> userInfo = getCurrentUser();
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "code", 401,
                "message", "未登录或登录已过期"
            ));
        }

        String reason = (String) request.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", "关闭原因不能为空"
            ));
        }

        int duration = request.containsKey("duration") ?
            ((Number) request.get("duration")).intValue() : 60;

        try {
            WatermarkTemporaryDisable record = watermarkService.temporaryDisable(
                (Long) userInfo.get("id"),
                (String) userInfo.get("realName"),
                reason,
                duration,
                httpRequest
            );

            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "水印已临时关闭",
                "data", Map.of(
                    "recordId", record.getId(),
                    "scheduledRestoreTime", record.getScheduledRestoreTime(),
                    "durationMinutes", record.getDurationMinutes()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 手动恢复水印
     */
    @PostMapping("/restore")
    public ResponseEntity<Map<String, Object>> restoreWatermark(HttpServletRequest httpRequest) {
        Map<String, Object> userInfo = getCurrentUser();
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "code", 401,
                "message", "未登录或登录已过期"
            ));
        }

        try {
            watermarkService.restoreWatermark(
                (Long) userInfo.get("id"),
                (String) userInfo.get("realName"),
                httpRequest
            );

            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "水印已恢复"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取关闭记录列表
     */
    @GetMapping("/disable-records")
    public ResponseEntity<Map<String, Object>> getDisableRecords() {
        Map<String, Object> userInfo = getCurrentUser();
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "code", 401,
                "message", "未登录或登录已过期"
            ));
        }

        List<WatermarkTemporaryDisable> records =
            watermarkService.getDisableRecordsByUser((Long) userInfo.get("id"));

        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", records
        ));
    }
}

/**
 * 水印审计控制器
 * <p>
 * 处理水印相关事件的API接口
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
class WatermarkAuditController {

    private final WatermarkService watermarkService;
    private final UserRepository userRepository;

    /**
     * 获取当前用户信息
     */
    private Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole());
        return userInfo;
    }

    /**
     * 记录水印篡改事件
     */
    @PostMapping("/watermark-tampering")
    public ResponseEntity<Map<String, Object>> logTamperingEvent(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Map<String, Object> userInfo = getCurrentUser();
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "code", 401,
                "message", "未登录或登录已过期"
            ));
        }

        try {
            String reason = (String) request.get("reason");
            String details = request.containsKey("details") ?
                request.get("details").toString() : null;

            watermarkService.logTamperingEvent(
                (Long) userInfo.get("id"),
                (String) userInfo.get("realName"),
                reason,
                details,
                httpRequest
            );

            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "篡改事件已记录"
            ));
        } catch (Exception e) {
            log.error("记录篡改事件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 记录截图事件
     */
    @PostMapping("/screenshot")
    public ResponseEntity<Map<String, Object>> logScreenshotEvent(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Map<String, Object> userInfo = getCurrentUser();
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "code", 401,
                "message", "未登录或登录已过期"
            ));
        }

        try {
            String pageUrl = (String) request.getOrDefault("pageUrl", httpRequest.getRequestURI());

            watermarkService.logScreenshotEvent(
                (Long) userInfo.get("id"),
                (String) userInfo.get("realName"),
                pageUrl,
                httpRequest
            );

            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "截图事件已记录"
            ));
        } catch (Exception e) {
            log.error("记录截图事件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 记录打印事件
     */
    @PostMapping("/print")
    public ResponseEntity<Map<String, Object>> logPrintEvent(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Map<String, Object> userInfo = getCurrentUser();
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "code", 401,
                "message", "未登录或登录已过期"
            ));
        }

        try {
            String pageUrl = (String) request.getOrDefault("pageUrl", httpRequest.getRequestURI());

            watermarkService.logPrintEvent(
                (Long) userInfo.get("id"),
                (String) userInfo.get("realName"),
                pageUrl,
                httpRequest
            );

            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "打印事件已记录"
            ));
        } catch (Exception e) {
            log.error("记录打印事件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 记录导出文件事件
     */
    @PostMapping("/export")
    public ResponseEntity<Map<String, Object>> logExportEvent(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Map<String, Object> userInfo = getCurrentUser();
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                "code", 401,
                "message", "未登录或登录已过期"
            ));
        }

        try {
            String fileType = (String) request.get("fileType");
            String fileName = (String) request.get("fileName");

            watermarkService.logExportEvent(
                (Long) userInfo.get("id"),
                (String) userInfo.get("realName"),
                fileType,
                fileName,
                httpRequest
            );

            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "导出事件已记录"
            ));
        } catch (Exception e) {
            log.error("记录导出事件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 查询水印审计日志
     */
    @GetMapping("/watermark-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> queryWatermarkLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime endTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        Page<WatermarkAuditLog> logs = watermarkService.queryAuditLogs(
            userId, eventType, riskLevel, status, startTime, endTime, page, size);

        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", Map.of(
                "content", logs.getContent(),
                "totalElements", logs.getTotalElements(),
                "totalPages", logs.getTotalPages(),
                "currentPage", logs.getNumber()
            )
        ));
    }

    /**
     * 获取水印审计统计数据
     */
    @GetMapping("/watermark-statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getWatermarkStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime endTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        Map<String, Object> stats = watermarkService.getStatistics(startTime, endTime);

        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", stats
        ));
    }

    /**
     * 导出水印操作日志
     */
    @GetMapping("/watermark-export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> exportWatermarkLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        List<WatermarkAuditLog> logs = watermarkService.exportLogs(startTime, endTime);

        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", Map.of(
                "total", logs.size(),
                "logs", logs
            )
        ));
    }

    /**
     * 获取客户端IP地址
     */
    @GetMapping("/ip")
    public ResponseEntity<Map<String, Object>> getClientIp(HttpServletRequest request) {
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

        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "ip", ip
        ));
    }
}
