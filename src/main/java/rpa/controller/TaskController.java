package rpa.controller;

/**
 * 任务管理控制器
 * <p>
 * 提供任务相关的RESTful API接口，包括：
 * <ul>
 *   <li>任务管理：CRUD操作</li>
 *   <li>任务分配：分配机器人执行任务</li>
 *   <li>状态管理：更新任务执行状态</li>
 *   <li>执行日志：自动记录任务状态变更日志</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.Task;
import rpa.service.TaskService;
import rpa.service.ExecutionLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
@CrossOrigin
public class TaskController {

    private final TaskService taskService;
    private final ExecutionLogService executionLogService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) Long assigneeId,
                                    @RequestParam(required = false) String status) {
        Map<String, Object> response = new HashMap<>();
        List<Task> list;
        if (assigneeId != null) {
            list = taskService.findByAssigneeId(assigneeId);
        } else if (status != null) {
            list = taskService.findByStatus(status);
        } else {
            list = taskService.findAll();
        }
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    /**
     * 根据队列查询任务列表
     */
    @GetMapping("/queue/{queueId}")
    public Map<String, Object> getByQueue(@PathVariable Long queueId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Task> tasks = taskService.findByQueueId(queueId);
            response.put("code", 0);
            response.put("data", tasks);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        taskService.findById(id).ifPresentOrElse(
                task -> {
                    response.put("code", 0);
                    response.put("data", task);
                },
                () -> {
                    response.put("code", -1);
                    response.put("message", "任务不存在");
                }
        );
        return response;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) request.get("name");
            String category = (String) request.get("category");
            String priority = (String) request.get("priority");
            String remark = (String) request.get("remark");
            
            Object assigneeIdObj = request.get("assigneeId");
            Long assigneeId = null;
            if (assigneeIdObj != null) {
                assigneeId = Long.valueOf(assigneeIdObj.toString());
            }
            String assigneeName = (String) request.get("assigneeName");
            
            // 处理 processIds（支持 List 和字符串）
            Object processIdsObj = request.get("processIds");
            String processIds = null;
            if (processIdsObj instanceof List) {
                processIds = objectMapper.writeValueAsString(processIdsObj);
            } else if (processIdsObj != null) {
                processIds = processIdsObj.toString();
            }
            Object processNamesObj = request.get("processNames");
            String processNames = null;
            if (processNamesObj instanceof List) {
                processNames = objectMapper.writeValueAsString(processNamesObj);
            } else if (processNamesObj != null) {
                processNames = processNamesObj.toString();
            }
            
            // 处理单个 processId（兼容）
            Object processIdObj = request.get("processId");
            Long processId = null;
            if (processIdObj != null) {
                processId = Long.valueOf(processIdObj.toString());
            }
            String processName = (String) request.get("processName");
            
            Task task = taskService.create(name, category, priority, processId, processName, processIds, processNames, assigneeId, assigneeName, remark);
            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", task);
            
            executionLogService.create(task.getId(), null, null, 
                    "任务创建", "pending", "任务已创建等待分配");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/{id}/assign")
    public Map<String, Object> assignRobot(@PathVariable Long id,
                                           @RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long robotId = request.get("robotId");
            Task task = taskService.assignRobot(id, robotId);
            response.put("code", 0);
            response.put("message", "分配成功");
            response.put("data", task);
            
            executionLogService.create(id, task.getProcessId(), robotId, 
                    "任务分配", "assigned", "任务已分配给机器人");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id,
                                            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String status = request.get("status");
            String resultData = request.get("resultData");
            String errorMessage = request.get("errorMessage");
            
            Task task = taskService.updateStatus(id, status, resultData, errorMessage);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", task);
            
            executionLogService.create(id, task.getProcessId(), task.getRobotId(), 
                    "状态更新", status, "任务状态更新为：" + status);
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
            String category = (String) request.get("category");
            String priority = (String) request.get("priority");
            String remark = (String) request.get("remark");
            
            // 处理 processId
            Object processIdObj = request.get("processId");
            Long processId = null;
            if (processIdObj != null) {
                processId = Long.valueOf(processIdObj.toString());
            }
            String processName = (String) request.get("processName");
            
            // 处理 processIds（多流程，支持 List 和字符串）
            Object processIdsObj = request.get("processIds");
            String processIds = null;
            if (processIdsObj instanceof List) {
                processIds = objectMapper.writeValueAsString(processIdsObj);
            } else if (processIdsObj != null) {
                processIds = processIdsObj.toString();
            }
            Object processNamesObj = request.get("processNames");
            String processNames = null;
            if (processNamesObj instanceof List) {
                processNames = objectMapper.writeValueAsString(processNamesObj);
            } else if (processNamesObj != null) {
                processNames = processNamesObj.toString();
            }
            
            Task task = taskService.update(id, name, category, priority, processId, processName, processIds, processNames, remark);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", task);
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
            taskService.delete(id);
            response.put("code", 0);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
    
    /**
     * 检查任务名称是否重复
     */
    @PostMapping("/check-name")
    public Map<String, Object> checkNameDuplicate(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) request.get("name");
            Long excludeId = null;
            Object excludeIdObj = request.get("excludeId");
            if (excludeIdObj != null) {
                excludeId = Long.valueOf(excludeIdObj.toString());
            }
            boolean duplicate = taskService.isNameDuplicate(name, excludeId);
            response.put("code", 0);
            response.put("data", duplicate);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
