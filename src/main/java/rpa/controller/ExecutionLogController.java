package rpa.controller;

/**
 * 执行日志控制器
 * <p>
 * 提供执行日志相关的RESTful API接口。
 * 支持按任务、流程、机器人多维度查询日志。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.ExecutionLog;
import rpa.service.ExecutionLogService;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
@CrossOrigin
public class ExecutionLogController {

    private final ExecutionLogService service;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) Long taskId,
                                    @RequestParam(required = false) Long processId,
                                    @RequestParam(required = false) Long robotId) {
        Map<String, Object> response = new HashMap<>();
        List<ExecutionLog> list;
        if (taskId != null) {
            list = service.findByTaskId(taskId);
        } else if (processId != null) {
            list = service.findByProcessId(processId);
        } else if (robotId != null) {
            list = service.findByRobotId(robotId);
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
                log -> {
                    response.put("code", 0);
                    response.put("data", log);
                },
                () -> {
                    response.put("code", -1);
                    response.put("message", "日志不存在");
                }
        );
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
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
