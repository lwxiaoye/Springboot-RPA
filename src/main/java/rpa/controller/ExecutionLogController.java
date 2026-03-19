package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.ExecutionLog;
import rpa.service.ExecutionLogService;
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
                                    @RequestParam(required = false) Long processId) {
        Map<String, Object> response = new HashMap<>();
        List<ExecutionLog> list;
        if (taskId != null) {
            list = service.findByTaskId(taskId);
        } else if (processId != null) {
            list = service.findByProcessId(processId);
        } else {
            list = service.findAll();
        }
        response.put("code", 0);
        response.put("data", list);
        return response;
    }
}
