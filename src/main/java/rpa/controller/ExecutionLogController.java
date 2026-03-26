package rpa.controller;

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
}
