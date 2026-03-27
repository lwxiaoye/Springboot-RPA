package rpa.controller;

/**
 * 机器人管理控制器
 * <p>
 * 提供机器人相关的RESTful API接口，包括：
 * <ul>
 *   <li>机器人管理：CRUD操作</li>
 *   <li>状态管理：更新机器人状态（idle/busy/offline）</li>
 *   <li>空闲机器人查询：获取所有空闲状态的机器人</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

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
            String ip = (String) request.get("ip");
            String hostname = (String) request.get("hostname");
            Integer port = request.get("port") != null ? Integer.valueOf(request.get("port").toString()) : 8080;
            String description = (String) request.get("description");

            Robot robot = service.create(name, type, capabilities, ip, hostname, port, description);
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
            String ip = (String) request.get("ip");
            String hostname = (String) request.get("hostname");
            Integer port = request.get("port") != null ? Integer.valueOf(request.get("port").toString()) : 8080;
            String description = (String) request.get("description");

            Robot robot = service.update(id, name, type, capabilities, ip, hostname, port, description);
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
