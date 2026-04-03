package rpa.controller;

/**
 * 角色管理控制器
 * <p>
 * 提供角色相关的RESTful API接口，包括：
 * <ul>
 *   <li>角色管理：CRUD操作</li>
 *   <li>权限配置：为角色分配权限</li>
 *   <li>角色用户查询：查看某角色的所有用户</li>
 *   <li>默认角色初始化：创建系统默认角色</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.Role;
import rpa.entity.User;
import rpa.service.RoleService;
import rpa.service.UserService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
@CrossOrigin
public class RoleController {

    private final RoleService service;
    private final UserService userService;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> response = new HashMap<>();
        List<Role> roles = service.findAll();
        List<User> allUsers = userService.findAll();

        List<Map<String, Object>> data = roles.stream().map(role -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", role.getId());
            map.put("name", role.getName());
            map.put("code", role.getCode());
            map.put("description", role.getDescription());
            map.put("status", role.getStatus());
            map.put("createTime", role.getCreateTime());
            map.put("permissionCount", role.getPermissions() != null ? role.getPermissions().size() : 0);
            map.put("permissionIds", role.getPermissions() != null ?
                role.getPermissions().stream().map(p -> p.getId()).collect(Collectors.toSet()) : new HashSet<Long>());

            // 统计该角色的用户数量
            int userCount = countUsersByRoleCode(allUsers, role.getCode());
            map.put("userCount", userCount);

            return map;
        }).collect(Collectors.toList());
        response.put("code", 0);
        response.put("data", data);
        return response;
    }

    private int countUsersByRoleCode(List<User> users, String roleCode) {
        if (users == null) return 0;
        // 根据角色编码映射到role值
        Integer roleValue;
        if ("ROLE_ADMIN".equals(roleCode)) {
            roleValue = 1;
        } else if ("ROLE_OPERATOR".equals(roleCode)) {
            roleValue = 2;
        } else if ("ROLE_USER".equals(roleCode)) {
            roleValue = 0;
        } else {
            // 对于未知角色，尝试解析数字
            roleValue = null;
            try {
                if (roleCode != null && roleCode.startsWith("ROLE_")) {
                    String numStr = roleCode.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        roleValue = Integer.parseInt(numStr);
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        if (roleValue == null) return 0;

        final Integer finalRoleValue = roleValue;
        return (int) users.stream().filter(u -> u.getRole() != null && u.getRole().equals(finalRoleValue)).count();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        return service.findById(id).map(role -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", role.getId());
            data.put("name", role.getName());
            data.put("code", role.getCode());
            data.put("description", role.getDescription());
            data.put("status", role.getStatus());
            data.put("permissionIds", role.getPermissions() != null ?
                role.getPermissions().stream().map(p -> p.getId()).collect(Collectors.toSet()) : new HashSet<Long>());
            response.put("code", 0);
            response.put("data", data);
            return response;
        }).orElseGet(() -> {
            response.put("code", -1);
            response.put("message", "角色不存在");
            return response;
        });
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Role role = new Role();
            role.setName((String) request.get("name"));
            role.setCode((String) request.get("code"));
            role.setDescription((String) request.getOrDefault("description", ""));
            role.setStatus((Integer) request.getOrDefault("status", 1));
            Role created = service.create(role);
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
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Role role = new Role();
            role.setName((String) request.get("name"));
            role.setCode((String) request.get("code"));
            role.setDescription((String) request.getOrDefault("description", ""));
            role.setStatus((Integer) request.getOrDefault("status", 1));
            Role updated = service.update(id, role);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", updated);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}/permissions")
    public Map<String, Object> updatePermissions(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Long> permissionIdList = (List<Long>) request.get("permissionIds");
            Set<Long> permissionIds = permissionIdList != null ?
                permissionIdList.stream().collect(Collectors.toSet()) : new HashSet<>();
            service.updatePermissions(id, permissionIds);
            response.put("code", 0);
            response.put("message", "权限配置成功");
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

    @PostMapping("/init")
    public Map<String, Object> init() {
        Map<String, Object> response = new HashMap<>();
        try {
            service.initDefaultRoles();
            response.put("code", 0);
            response.put("message", "初始化成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/{id}/users")
    public Map<String, Object> getRoleUsers(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Role role = service.findById(id).orElse(null);
            if (role == null) {
                response.put("code", -1);
                response.put("message", "角色不存在");
                return response;
            }

            // 根据角色编码获取对应角色的用户
            List<User> allUsers = userService.findAll();
            List<User> roleUsers;

            if ("ROLE_ADMIN".equals(role.getCode())) {
                // 管理员角色对应 role = 1
                roleUsers = allUsers.stream().filter(u -> u.getRole() != null && u.getRole() == 1).collect(Collectors.toList());
            } else if ("ROLE_OPERATOR".equals(role.getCode())) {
                // 运营人员角色对应 role = 2
                roleUsers = allUsers.stream().filter(u -> u.getRole() != null && u.getRole() == 2).collect(Collectors.toList());
            } else if ("ROLE_USER".equals(role.getCode())) {
                // 普通用户角色对应 role = 0
                roleUsers = allUsers.stream().filter(u -> u.getRole() != null && u.getRole() == 0).collect(Collectors.toList());
            } else {
                roleUsers = allUsers;
            }

            List<Map<String, Object>> userList = roleUsers.stream().map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("username", user.getUsername());
                map.put("realName", user.getRealName());
                map.put("email", user.getEmail());
                map.put("phone", user.getPhone());
                map.put("role", user.getRole());
                map.put("status", user.getStatus());
                map.put("createTime", user.getCreateTime());
                return map;
            }).collect(Collectors.toList());

            response.put("code", 0);
            response.put("data", userList);
            response.put("count", userList.size());
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
