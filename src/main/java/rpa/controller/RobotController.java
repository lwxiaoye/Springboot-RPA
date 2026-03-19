package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.Robot;
import rpa.service.RobotService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/robot")
@RequiredArgsConstructor
@CrossOrigin
public class RobotController {

    private final RobotService service;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> response = new HashMap<>();
        List<Robot> list = service.findAll();
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    @GetMapping("/idle")
    public Map<String, Object> listIdleRobots() {
        Map<String, Object> response = new HashMap<>();
        List<Robot> list = service.findIdleRobots();
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        service.findById(id).ifPresentOrElse(
                robot -> {
                    response.put("code", 0);
                    response.put("data", robot);
                },
                () -> {
                    response.put("code", -1);
                    response.put("message", "机器人不存在");
                }
        );
        return response;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) request.get("name");
            String type = (String) request.get("type");
            String capabilities = (String) request.get("capabilities");
            
            Robot robot = service.create(name, type, capabilities);
            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", robot);
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
            Robot robot = service.updateStatus(id, status);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", robot);
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
            String type = (String) request.get("type");
            String capabilities = (String) request.get("capabilities");
            
            Robot robot = service.update(id, name, type, capabilities);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", robot);
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
