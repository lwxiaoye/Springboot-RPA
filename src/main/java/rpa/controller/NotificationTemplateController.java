package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.entity.NotificationTemplate;
import rpa.service.NotificationTemplateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知模板控制器
 * <p>
 * 提供通知模板的CRUD操作和模板编辑功能。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/notification-template")
@RequiredArgsConstructor
@CrossOrigin
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    /**
     * 查询所有模板
     */
    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<NotificationTemplate> templates = templateService.findAll();
            response.put("code", 0);
            response.put("data", templates);
        } catch (Exception e) {
            log.error("查询模板列表失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 根据ID查询模板
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            return templateService.findById(id)
                    .map(template -> {
                        response.put("code", 0);
                        response.put("data", template);
                        return response;
                    })
                    .orElseGet(() -> {
                        response.put("code", -1);
                        response.put("message", "模板不存在");
                        return response;
                    });
        } catch (Exception e) {
            log.error("查询模板失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 根据类型查询模板
     */
    @GetMapping("/type/{type}")
    public Map<String, Object> listByType(@PathVariable String type) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<NotificationTemplate> templates = templateService.findByType(type);
            response.put("code", 0);
            response.put("data", templates);
        } catch (Exception e) {
            log.error("查询模板列表失败, type={}", type, e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取启用的模板
     */
    @GetMapping("/enabled")
    public Map<String, Object> listEnabled() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<NotificationTemplate> templates = templateService.findEnabled();
            response.put("code", 0);
            response.put("data", templates);
        } catch (Exception e) {
            log.error("查询启用模板失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 创建模板
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody NotificationTemplate template) {
        Map<String, Object> response = new HashMap<>();
        try {
            NotificationTemplate saved = templateService.create(template);
            response.put("code", 0);
            response.put("data", saved);
            response.put("message", "模板创建成功");
        } catch (Exception e) {
            log.error("创建模板失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody NotificationTemplate template) {
        Map<String, Object> response = new HashMap<>();
        try {
            NotificationTemplate updated = templateService.update(id, template);
            response.put("code", 0);
            response.put("data", updated);
            response.put("message", "模板更新成功");
        } catch (Exception e) {
            log.error("更新模板失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            templateService.delete(id);
            response.put("code", 0);
            response.put("message", "模板删除成功");
        } catch (Exception e) {
            log.error("删除模板失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 切换模板状态
     */
    @PutMapping("/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            NotificationTemplate template = templateService.toggleEnabled(id);
            response.put("code", 0);
            response.put("data", template);
            response.put("message", template.getEnabled() != null && template.getEnabled() == 1 ? "模板已启用" : "模板已禁用");
        } catch (Exception e) {
            log.error("切换模板状态失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 设置默认模板
     */
    @PutMapping("/{id}/set-default")
    public Map<String, Object> setDefault(@PathVariable Long id, @RequestParam String type) {
        Map<String, Object> response = new HashMap<>();
        try {
            templateService.setDefault(id, type);
            response.put("code", 0);
            response.put("message", "默认模板设置成功");
        } catch (Exception e) {
            log.error("设置默认模板失败, id={}, type={}", id, type, e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 预览模板
     */
    @PostMapping("/preview")
    public Map<String, Object> preview(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String content = (String) request.get("content");
            Map<String, Object> variables = (Map<String, Object>) request.get("variables");

            // 验证模板
            NotificationTemplateService.ValidationResult validation = templateService.validateTemplate(content);
            if (!validation.isValid()) {
                response.put("code", -1);
                response.put("message", validation.getMessage());
                return response;
            }

            // 替换变量
            String processed = templateService.processTemplateContent(content, variables);

            response.put("code", 0);
            response.put("data", Map.of(
                    "processed", processed,
                    "variables", validation.getVariables()
            ));
        } catch (Exception e) {
            log.error("预览模板失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 验证模板语法
     */
    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String content = (String) request.get("content");
            NotificationTemplateService.ValidationResult result = templateService.validateTemplate(content);

            response.put("code", 0);
            response.put("data", result);
        } catch (Exception e) {
            log.error("验证模板失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取模板变量列表
     */
    @GetMapping("/variables/{type}")
    public Map<String, Object> getVariables(@PathVariable String type) {
        Map<String, Object> response = new HashMap<>();
        try {
            Set<String> variables = getVariablesByType(type);
            response.put("code", 0);
            response.put("data", variables);
        } catch (Exception e) {
            log.error("获取变量列表失败, type={}", type, e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 根据类型获取可用变量
     */
    private Set<String> getVariablesByType(String type) {
        return switch (type) {
            case "task_complete", "task_failed" -> Set.of(
                    "taskId", "taskName", "taskStatus", "startTime", "endTime",
                    "duration", "errorMessage", "robotName", "processName",
                    "reportUrl", "creatorName"
            );
            case "subscription" -> Set.of(
                    "subscriptionName", "reportType", "period", "generateTime",
                    "totalExecutions", "successCount", "failedCount", "successRate",
                    "totalData", "dailyAvg", "peakData", "reportUrl"
            );
            case "alert" -> Set.of(
                    "alertId", "alertTitle", "alertContent", "alertTime",
                    "level", "source", "metrics", "recommendation"
            );
            default -> Set.of();
        };
    }
}
