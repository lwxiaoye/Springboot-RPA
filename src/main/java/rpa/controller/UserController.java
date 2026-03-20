package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.User;
import rpa.service.UserService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String username = request.get("username");
        String password = request.get("password");
        
        Optional<User> userOpt = userService.login(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            response.put("code", 0);
            response.put("message", "登录成功");
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("email", user.getEmail());
            data.put("phone", user.getPhone());
            data.put("role", user.getRole());
            data.put("status", user.getStatus());
            response.put("data", data);
        } else {
            response.put("code", -1);
            response.put("message", "用户名或密码错误");
        }
        return response;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = request.get("username");
            String password = request.get("password");
            String realName = request.get("realName");
            String email = request.get("email");
            String phone = request.get("phone");
            String roleStr = request.get("role");
            Integer role = roleStr != null ? Integer.parseInt(roleStr) : null;

            User user = userService.register(username, password, realName, email, phone, role);
            response.put("code", 0);
            response.put("message", "注册成功");
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            response.put("data", data);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            response.put("code", 0);
            response.put("data", userOpt.get());
        } else {
            response.put("code", -1);
            response.put("message", "用户不存在");
        }
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateProfile(@PathVariable Long id,
                                             @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String realName = (String) request.get("realName");
            String email = (String) request.get("email");
            String phone = (String) request.get("phone");
            Integer role = request.get("role") != null ? ((Number) request.get("role")).intValue() : null;
            Integer status = request.get("status") != null ? ((Number) request.get("status")).intValue() : null;
            
            User user = userService.updateProfile(id, realName, email, phone);
            
            // 更新角色
            if (role != null) {
                user = userService.updateUserRole(id, role);
            }
            
            // 更新状态
            if (status != null) {
                user = userService.updateUserStatus(id, status);
            }
            
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", user);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/password/{id}")
    public Map<String, Object> updatePassword(@PathVariable Long id,
                                              @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            
            userService.updatePassword(id, oldPassword, newPassword);
            response.put("code", 0);
            response.put("message", "密码修改成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping
    public Map<String, Object> listUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("data", userService.findAll());
        return response;
    }

    @PutMapping("/reset-password/{id}")
    public Map<String, Object> resetPassword(@PathVariable Long id,
                                             @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String newPassword = request.get("newPassword");
            userService.resetPassword(id, newPassword);
            response.put("code", 0);
            response.put("message", "密码重置成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateUserStatus(@PathVariable Long id,
                                                 @RequestBody Map<String, Integer> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer status = request.get("status");
            userService.updateUserStatus(id, status);
            response.put("code", 0);
            response.put("message", "状态更新成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            userService.deleteUser(id);
            response.put("code", 0);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
