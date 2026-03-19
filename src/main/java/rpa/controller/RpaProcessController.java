package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.RpaProcess;
import rpa.service.RpaProcessService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
@CrossOrigin
public class RpaProcessController {

    private final RpaProcessService service;

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
            Object creatorIdObj = request.get("creatorId");
            Long creatorId = null;
            if (creatorIdObj != null) {
                creatorId = Long.valueOf(creatorIdObj.toString());
            }
            String creatorName = (String) request.get("creatorName");
            
            RpaProcess process = service.create(name, code, description, "", creatorId, creatorName);
            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", process);
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
            
            RpaProcess process = service.updateWithCode(id, name, code, description);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", process);
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
