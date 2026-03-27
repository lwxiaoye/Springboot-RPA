package rpa.controller;

/**
 * 资源管理控制器
 * <p>
 * 提供资源相关的RESTful API接口，包括资源的CRUD操作。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rpa.service.ResourceService;
import rpa.entity.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resource")
@CrossOrigin
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @GetMapping
    public Map<String, Object> getAllResources() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Resource> resources = resourceService.findAll();
            response.put("code", 0);
            response.put("message", "success");
            response.put("data", resources);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getResource(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Resource resource = resourceService.findById(id);
            response.put("code", 0);
            response.put("message", "success");
            response.put("data", resource);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping
    public Map<String, Object> createResource(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) request.get("name");
            String code = (String) request.get("code");
            String type = (String) request.get("type");
            String url = (String) request.get("url");
            String icon = (String) request.get("icon");
            Integer sort = request.get("sort") != null ? ((Number) request.get("sort")).intValue() : 0;

            resourceService.create(name, code, type, url, icon, sort);
            response.put("code", 0);
            response.put("message", "添加成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateResource(@PathVariable Long id,
                                               @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) request.get("name");
            String code = (String) request.get("code");
            String type = (String) request.get("type");
            String url = (String) request.get("url");
            String icon = (String) request.get("icon");
            Integer sort = request.get("sort") != null ? ((Number) request.get("sort")).intValue() : 0;
            Integer status = request.get("status") != null ? ((Number) request.get("status")).intValue() : 1;

            resourceService.update(id, name, code, type, url, icon, sort, status);
            response.put("code", 0);
            response.put("message", "更新成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteResource(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            resourceService.delete(id);
            response.put("code", 0);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
