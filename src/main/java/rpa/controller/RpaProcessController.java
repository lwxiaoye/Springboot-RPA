package rpa.controller;

/**
 * RPA流程管理控制器
 * <p>
 * 提供RPA流程相关的RESTful API接口，包括流程的CRUD操作。
 * 支持按创建者筛选流程列表。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.RpaProcess;
import rpa.service.RpaProcessService;
import rpa.service.AuditLogService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
@CrossOrigin
public class RpaProcessController {

    private final RpaProcessService service;
    private final AuditLogService auditLogService;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) Long creatorId) {
        Map<String, Object> response = new HashMap<>();
        List<RpaProcess> list;
        if (creatorId != null) {
            list = service.findByCreatorId(creatorId);
        } else {
            list = service.findAll();
        }
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        service.findById(id).ifPresentOrElse(
                process -> {
                    response.put("code", 0);
                    response.put("data", process);
                },
                () -> {
                    response.put("code", -1);
                    response.put("message", "流程不存在");
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
            String version = (String) request.get("version");
            String status = (String) request.get("status");
            Object creatorIdObj = request.get("creatorId");
            Long creatorId = null;
            if (creatorIdObj != null) {
                creatorId = Long.valueOf(creatorIdObj.toString());
            }
            String creatorName = (String) request.get("creatorName");

            RpaProcess process = service.create(name, code, description, version, status, creatorId, creatorName);
            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", process);

            // 审计日志
            auditLogService.log("PROCESS", "PROCESS_CREATE", "RpaProcess", process.getId(), name,
                "创建流程: " + name, "medium", "success",
                Map.of("version", version, "status", status));
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
                                      @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) request.get("name");
            String code = (String) request.get("code");
            String description = (String) request.get("description");
            String version = (String) request.get("version");
            String status = (String) request.get("status");

            RpaProcess process = service.update(id, name, code, description, version, status);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", process);

            // 审计日志
            auditLogService.log("PROCESS", "PROCESS_UPDATE", "RpaProcess", id, name,
                "更新流程: " + name, "medium", "success",
                Map.of("version", version, "status", status));
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
            // 先获取流程信息用于审计日志
            String processName = service.findById(id).map(RpaProcess::getName).orElse("未知流程");

            service.delete(id);
            response.put("code", 0);
            response.put("message", "删除成功");

            // 审计日志
            auditLogService.log("PROCESS", "PROCESS_DELETE", "RpaProcess", id, processName,
                "删除流程: " + processName, "high", "success", null);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 保存流程设计
     */
    @PostMapping("/{id}/design")
    public Map<String, Object> saveDesign(@PathVariable Long id,
                                          @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String steps = (String) request.get("steps");
            RpaProcess process = service.saveDesign(id, steps);
            response.put("code", 0);
            response.put("message", "保存成功");
            response.put("data", process);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取流程设计
     */
    @GetMapping("/{id}/design")
    public Map<String, Object> getDesign(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            RpaProcess process = service.findById(id).orElse(null);
            if (process != null) {
                response.put("code", 0);
                response.put("data", process.getSteps());
            } else {
                response.put("code", -1);
                response.put("message", "流程不存在");
            }
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 执行流程
     */
    @PostMapping("/{id}/execute")
    public Map<String, Object> execute(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = service.execute(id);
            response.put("code", 0);
            response.put("message", "执行成功");
            response.put("data", result);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
    
    /**
     * 获取执行状态
     */
    @GetMapping("/{id}/status")
    public Map<String, Object> getExecutionStatus(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = service.getExecutionStatus(id);
            response.put("code", 0);
            response.put("data", result);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
