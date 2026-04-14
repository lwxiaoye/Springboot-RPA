package rpa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rpa.entity.AuditLog;
import rpa.repository.AuditLogRepository;
import rpa.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) Long userId,
                                    @RequestParam(required = false) String module,
                                    @RequestParam(required = false) String riskLevel,
                                    @RequestParam(required = false) String startTime,
                                    @RequestParam(required = false) String endTime) {
        List<AuditLog> list;

        if (userId != null) {
            LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime) : LocalDateTime.now().minusMonths(1);
            LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime) : LocalDateTime.now();
            list = auditLogService.findByUserId(userId, start, end);
        } else if (module != null && !module.isEmpty()) {
            list = auditLogRepository.findByModule(module);
        } else if (riskLevel != null && !riskLevel.isEmpty()) {
            list = auditLogRepository.findByRiskLevel(riskLevel);
        } else {
            list = auditLogRepository.findAll();
        }

        return success(list);
    }

    @GetMapping("/high-risk")
    public Map<String, Object> listHighRisk() {
        LocalDateTime since = LocalDateTime.now().minusMonths(1);
        List<AuditLog> list = auditLogRepository.findHighRiskSince(since);
        return success(list);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<AuditLog> logs = auditLogRepository.findByCreateTimeBetween(since, LocalDateTime.now());

        stats.put("totalCount", logs.size());
        stats.put("highRiskCount", logs.stream().filter(l -> "high".equals(l.getRiskLevel())).count());
        stats.put("byModule", logs.stream().collect(Collectors.groupingBy(AuditLog::getModule, Collectors.counting())));
        stats.put("byAction", logs.stream().collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting())));

        return success(stats);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody AuditLog auditLog) {
        AuditLog saved = auditLogService.log(auditLog);
        return success(saved);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        return auditLogRepository.findById(id)
            .map(this::success)
            .orElse(error("记录不存在"));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        result.put("message", message);
        return result;
    }
}
