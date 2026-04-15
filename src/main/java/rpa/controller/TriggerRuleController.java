package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.TriggerRule;
import rpa.service.TriggerRuleService;
import rpa.service.AuditLogService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 触发器管理控制器
 * <p>
 * 提供触发器的RESTful API接口，包括：
 * <ul>
 *   <li>触发器管理：CRUD操作</li>
 *   <li>触发器配置：定时、文件、API、Webhook</li>
 *   <li>触发执行：手动触发流程</li>
 *   <li>触发统计：触发次数、成功率</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/trigger")
@RequiredArgsConstructor
@CrossOrigin
public class TriggerRuleController {

    private final TriggerRuleService service;
    private final AuditLogService auditLogService;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String triggerType,
                                     @RequestParam(required = false) String status) {
        Map<String, Object> response = new HashMap<>();
        List<TriggerRule> list;
        if (triggerType != null && !triggerType.isEmpty()) {
            list = service.findByTriggerType(triggerType);
        } else if (status != null && !status.isEmpty()) {
            list = service.findByStatus(status);
        } else {
            list = service.findAll();
        }
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    @GetMapping("/active")
    public Map<String, Object> listActive() {
        Map<String, Object> response = new HashMap<>();
        List<TriggerRule> list = service.findActiveTriggers();
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    @GetMapping("/active/schedule")
    public Map<String, Object> listActiveSchedule() {
        Map<String, Object> response = new HashMap<>();
        List<TriggerRule> list = service.findActiveScheduleTriggers();
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        service.findById(id).ifPresentOrElse(
            trigger -> {
                response.put("code", 0);
                response.put("data", trigger);
            },
            () -> {
                response.put("code", -1);
                response.put("message", "触发器不存在");
            }
        );
        return response;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) request.get("name");
            String code = (String) request.get("code");
            String description = (String) request.get("description");
            String triggerType = (String) request.get("triggerType");

            Long processId = request.get("processId") != null ?
                Long.valueOf(request.get("processId").toString()) : null;
            String processName = (String) request.get("processName");
            String processCode = (String) request.get("processCode");

            Long queueId = request.get("queueId") != null ?
                Long.valueOf(request.get("queueId").toString()) : null;
            String queueName = (String) request.get("queueName");

            String cron = (String) request.get("cron");
            String scheduleType = (String) request.get("scheduleType");
            String scheduleTime = (String) request.get("scheduleTime");
            String scheduleDays = (String) request.get("scheduleDays");

            String watchPath = (String) request.get("watchPath");
            String filePattern = (String) request.get("filePattern");
            Boolean watchSubdirs = (Boolean) request.get("watchSubdirs");

            String apiKey = (String) request.get("apiKey");
            String webhookUrl = (String) request.get("webhookUrl");
            String httpMethod = (String) request.get("httpMethod");

            String triggerCondition = (String) request.get("triggerCondition");
            String triggerParams = (String) request.get("triggerParams");
            Boolean autoStart = (Boolean) request.get("autoStart");
            Integer maxConcurrent = request.get("maxConcurrent") != null ?
                Integer.valueOf(request.get("maxConcurrent").toString()) : 1;

            String creator = (String) request.get("creator");
            String remark = (String) request.get("remark");

            TriggerRule trigger = service.create(name, code, description, triggerType,
                processId, processName, processCode, queueId, queueName,
                cron, scheduleType, scheduleTime, scheduleDays,
                watchPath, filePattern, watchSubdirs,
                apiKey, webhookUrl, httpMethod,
                triggerCondition, triggerParams, autoStart, maxConcurrent,
                creator, remark);

            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", trigger);

            auditLogService.logTrigger(null, creator, null, "创建", name);

        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            TriggerRule oldTrigger = service.findById(id)
                .orElseThrow(() -> new RuntimeException("触发器不存在"));
            
            TriggerRule trigger = service.update(id, request);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", trigger);

            auditLogService.logTrigger(null, null, null, "更新", trigger.getName());

        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String status = request.get("status");
            TriggerRule trigger = service.updateStatus(id, status);
            response.put("code", 0);
            response.put("message", "状态更新成功");
            response.put("data", trigger);

            auditLogService.logTrigger(null, null, null, status, trigger.getName());

        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/{id}/trigger")
    public Map<String, Object> trigger(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = service.triggerProcess(id);
            response.put("code", 0);
            response.put("message", "触发成功");
            response.put("data", result);

            auditLogService.logTrigger(null, null, null, "手动触发", service.findById(id).map(t -> t.getName()).orElse("未知"));

        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            service.findById(id).ifPresent(trigger -> {
                auditLogService.logTrigger(null, null, null, "删除", trigger.getName());
            });
            service.delete(id);
            response.put("code", 0);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
