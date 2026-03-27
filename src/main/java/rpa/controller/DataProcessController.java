package rpa.controller;

/**
 * 数据加工控制器
 * <p>
 * 提供数据加工相关的RESTful API接口，包括：
 * <ul>
 *   <li>加工配置管理：CRUD操作</li>
 *   <li>执行加工：触发加工任务执行</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.DataProcess;
import rpa.service.DataProcessService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dataProcess")
@RequiredArgsConstructor
@CrossOrigin
public class DataProcessController {

    private final DataProcessService service;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> response = new HashMap<>();
        List<DataProcess> list = service.findAll();
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
                    response.put("message", "加工配置不存在");
                }
        );
        return response;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            DataProcess process = service.create(request);
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
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            DataProcess process = service.update(id, request);
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

    @PostMapping("/{id}/execute")
    public Map<String, Object> execute(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = service.executeProcess(id);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
