package rpa.controller;

/**
 * 权限管理控制器
 * <p>
 * 提供权限相关的RESTful API接口，包括：
 * <ul>
 *   <li>权限管理：CRUD操作</li>
 *   <li>权限树：获取树形结构的权限列表</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.Permission;
import rpa.service.PermissionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
@CrossOrigin
public class PermissionController {

    private final PermissionService service;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("data", service.findAll());
        return response;
    }

    @GetMapping("/tree")
    public Map<String, Object> tree() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("data", service.findTree());
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        return service.findById(id).map(p -> {
            response.put("code", 0);
            response.put("data", p);
            return response;
        }).orElseGet(() -> {
            response.put("code", -1);
            response.put("message", "权限不存在");
            return response;
        });
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Permission permission) {
        Map<String, Object> response = new HashMap<>();
        try {
            Permission created = service.create(permission);
            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", created);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Permission permission) {
        Map<String, Object> response = new HashMap<>();
        try {
            Permission updated = service.update(id, permission);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", updated);
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
