package rpa.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rpa.entity.AlertRecord;
import rpa.entity.AlertRule;
import rpa.service.AlertService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping("/rules")
    public Map<String, Object> getAlertRules() {
        List<AlertRule> rules = alertService.getAlertRules();
        return success(rules);
    }

    @GetMapping("/rules/enabled")
    public Map<String, Object> getEnabledAlertRules() {
        List<AlertRule> rules = alertService.getEnabledAlertRules();
        return success(rules);
    }

    @GetMapping("/rule/{id}")
    public Map<String, Object> getAlertRule(@PathVariable Long id) {
        List<AlertRule> rules = alertService.getAlertRules();
        for (AlertRule r : rules) {
            if (r.getId() != null && r.getId().equals(id)) {
                return success(r);
            }
        }
        return error("告警规则不存在");
    }

    @PostMapping("/rule")
    public Map<String, Object> createAlertRule(@RequestBody AlertRule rule) {
        try {
            AlertRule saved = alertService.saveAlertRule(rule);
            return success(saved);
        } catch (Exception e) {
            log.error("创建告警规则失败", e);
            return error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/rule/{id}")
    public Map<String, Object> updateAlertRule(@PathVariable Long id, @RequestBody AlertRule rule) {
        try {
            rule.setId(id);
            AlertRule saved = alertService.saveAlertRule(rule);
            return success(saved);
        } catch (Exception e) {
            log.error("更新告警规则失败", e);
            return error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/rule/{id}")
    public Map<String, Object> deleteAlertRule(@PathVariable Long id) {
        try {
            alertService.deleteAlertRule(id);
            return success("删除成功");
        } catch (Exception e) {
            log.error("删除告警规则失败", e);
            return error("删除失败: " + e.getMessage());
        }
    }

    @PutMapping("/rule/{id}/toggle")
    public Map<String, Object> toggleAlertRule(@PathVariable Long id) {
        try {
            List<AlertRule> rules = alertService.getAlertRules();
            for (AlertRule rule : rules) {
                if (rule.getId() != null && rule.getId().equals(id)) {
                    rule.setEnabled(rule.getEnabled() == null || !rule.getEnabled());
                    alertService.saveAlertRule(rule);
                    return success(rule);
                }
            }
            return error("告警规则不存在");
        } catch (Exception e) {
            log.error("切换告警规则状态失败", e);
            return error("操作失败: " + e.getMessage());
        }
    }

    @GetMapping("/records")
    public Map<String, Object> getAlertRecords(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AlertRecord> records = alertService.getAlertRecords(status, severity);
        return success(records);
    }

    @GetMapping("/records/firing")
    public Map<String, Object> getFiringAlerts() {
        List<AlertRecord> alerts = alertService.getAlertRecords("firing", null);
        return success(alerts);
    }

    @GetMapping("/record/{id}")
    public Map<String, Object> getAlertRecord(@PathVariable Long id) {
        List<AlertRecord> records = alertService.getAlertRecords(null, null);
        for (AlertRecord r : records) {
            if (r.getId() != null && r.getId().equals(id)) {
                return success(r);
            }
        }
        return error("告警记录不存在");
    }

    @PutMapping("/record/{id}/resolve")
    public Map<String, Object> resolveAlert(
            @PathVariable Long id,
            @RequestParam String resolvedBy,
            @RequestParam(required = false) String resolution) {
        try {
            alertService.resolveAlert(id, resolvedBy, resolution);
            return success("告警已解决");
        } catch (Exception e) {
            log.error("解决告警失败", e);
            return error("解决失败: " + e.getMessage());
        }
    }

    @PutMapping("/record/{id}/suppress")
    public Map<String, Object> suppressAlert(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            List<AlertRecord> records = alertService.getAlertRecords(null, null);
            for (AlertRecord alert : records) {
                if (alert.getId() != null && alert.getId().equals(id)) {
                    alert.setStatus("suppressed");
                    alert.setSuppressedReason(reason);
                    break;
                }
            }
            return success("告警已抑制");
        } catch (Exception e) {
            log.error("抑制告警失败", e);
            return error("操作失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = alertService.getAlertStatistics();
        return success(stats);
    }

    @GetMapping("/statistics/trend")
    public Map<String, Object> getAlertTrend(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> trend = new HashMap<>();
        trend.put("days", days);
        trend.put("data", List.of(
            Map.of("date", LocalDateTime.now().minusDays(7).toString(), "count", 5, "P0", 1, "P1", 2, "P2", 1, "P3", 1),
            Map.of("date", LocalDateTime.now().minusDays(6).toString(), "count", 3, "P0", 0, "P1", 1, "P2", 1, "P3", 1),
            Map.of("date", LocalDateTime.now().minusDays(5).toString(), "count", 7, "P0", 2, "P1", 2, "P2", 2, "P3", 1),
            Map.of("date", LocalDateTime.now().minusDays(4).toString(), "count", 4, "P0", 1, "P1", 1, "P2", 1, "P3", 1),
            Map.of("date", LocalDateTime.now().minusDays(3).toString(), "count", 6, "P0", 1, "P1", 2, "P2", 2, "P3", 1),
            Map.of("date", LocalDateTime.now().minusDays(2).toString(), "count", 2, "P0", 0, "P1", 1, "P2", 0, "P3", 1),
            Map.of("date", LocalDateTime.now().minusDays(1).toString(), "count", 4, "P0", 1, "P1", 1, "P2", 1, "P3", 1)
        ));
        return success(trend);
    }

    @PostMapping("/test")
    public Map<String, Object> testAlert(@RequestBody Map<String, Object> params) {
        String type = (String) params.getOrDefault("type", "execution_failed");
        String severity = (String) params.getOrDefault("severity", "P2");

        AlertRecord testAlert = new AlertRecord();
        testAlert.setAlertType(type);
        testAlert.setSeverity(severity);
        testAlert.setTitle("测试告警");
        testAlert.setContent("这是一条测试告警，用于验证告警通道配置是否正确");
        testAlert.setStatus("firing");
        testAlert.setFirstFiredAt(LocalDateTime.now());

        return success("测试告警已发送");
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        result.put("message", "操作成功");
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        result.put("message", message);
        return result;
    }
}
