package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.TaskQueue;
import rpa.service.TaskQueueService;
import rpa.service.AuditLogService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务队列管理控制器
 * <p>
 * 提供任务队列的RESTful API接口，包括：
 * <ul>
 *   <li>队列管理：CRUD操作</li>
 *   <li>队列状态：启用、暂停、停止</li>
 *   <li>队列统计：任务数、并发数等</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
@CrossOrigin
public class TaskQueueController {

    private final TaskQueueService service;
    private final AuditLogService auditLogService;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String status) {
        Map<String, Object> response = new HashMap<>();
        List<TaskQueue> list;
        if (status != null && !status.isEmpty()) {
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
        List<TaskQueue> list = service.findActiveQueues();
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        service.findById(id).ifPresentOrElse(
            queue -> {
                response.put("code", 0);
                response.put("data", queue);
            },
            () -> {
                response.put("code", -1);
                response.put("message", "队列不存在");
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
            String status = (String) request.get("status");
            Integer priorityLevel = request.get("priorityLevel") != null ?
                Integer.valueOf(request.get("priorityLevel").toString()) : 2;
            Integer maxConcurrentTasks = request.get("maxConcurrentTasks") != null ?
                Integer.valueOf(request.get("maxConcurrentTasks").toString()) : 5;

            Object processIdsObj = request.get("processIds");
            String processIds = null;
            if (processIdsObj instanceof List) {
                processIds = ((List<?>) processIdsObj).toString();
            }
            Object processNamesObj = request.get("processNames");
            String processNames = null;
            if (processNamesObj instanceof List) {
                processNames = ((List<?>) processNamesObj).toString();
            }
            Object requiredCategoriesObj = request.get("requiredCategories");
            String requiredCategories = null;
            if (requiredCategoriesObj instanceof List) {
                requiredCategories = ((List<?>) requiredCategoriesObj).toString();
            }

            String department = (String) request.get("department");
            String creator = (String) request.get("creator");
            String remark = (String) request.get("remark");

            TaskQueue queue = service.create(name, code, description, status,
                priorityLevel, maxConcurrentTasks, processIds, processNames,
                requiredCategories, department, creator, remark);

            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", queue);

            auditLogService.logQueue(null, creator, null, "创建", name);

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
            String name = (String) request.get("name");
            String code = (String) request.get("code");
            String description = (String) request.get("description");
            String status = (String) request.get("status");
            Integer priorityLevel = request.get("priorityLevel") != null ?
                Integer.valueOf(request.get("priorityLevel").toString()) : null;
            Integer maxConcurrentTasks = request.get("maxConcurrentTasks") != null ?
                Integer.valueOf(request.get("maxConcurrentTasks").toString()) : null;

            Object processIdsObj = request.get("processIds");
            String processIds = null;
            if (processIdsObj instanceof List) {
                processIds = ((List<?>) processIdsObj).toString();
            }
            Object processNamesObj = request.get("processNames");
            String processNames = null;
            if (processNamesObj instanceof List) {
                processNames = ((List<?>) processNamesObj).toString();
            }
            Object requiredCategoriesObj = request.get("requiredCategories");
            String requiredCategories = null;
            if (requiredCategoriesObj instanceof List) {
                requiredCategories = ((List<?>) requiredCategoriesObj).toString();
            }

            String department = (String) request.get("department");
            String remark = (String) request.get("remark");
            Boolean enabled = (Boolean) request.get("enabled");

            TaskQueue queue = service.update(id, name, code, description, status,
                priorityLevel, maxConcurrentTasks, processIds, processNames,
                requiredCategories, department, remark, enabled);

            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", queue);

            auditLogService.logQueue(null, null, null, "更新", queue.getName());

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
            TaskQueue queue = service.updateStatus(id, status);
            response.put("code", 0);
            response.put("message", "状态更新成功");
            response.put("data", queue);

            auditLogService.logQueue(null, null, null, status, queue.getName());

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
            service.findById(id).ifPresent(queue -> {
                auditLogService.logQueue(null, null, null, "删除", queue.getName());
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
