package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.Task;
import rpa.service.TaskService;
import rpa.service.ExecutionLogService;
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
            Object assigneeIdObj = request.get("assigneeId");
            Long assigneeId = null;
            if (assigneeIdObj != null) {
                assigneeId = Long.valueOf(assigneeIdObj.toString());
            }
            String assigneeName = (String) request.get("assigneeName");
            
            Task task = taskService.create(name, category, priority, null, null, assigneeId, assigneeName);
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
            
            Task task = taskService.update(id, name, category, priority);
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
}
