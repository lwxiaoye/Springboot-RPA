package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rpa.config.JwtUtils;
import rpa.entity.User;
import rpa.repository.UserRepository;
import rpa.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户控制器 - 提供用户管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        try {
            String username = loginData.get("username");
            String password = loginData.get("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "用户名和密码不能为空"));
            }

            Optional<User> userOpt = userService.login(username, password);

            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "用户名或密码错误"));
            }

            User user = userOpt.get();

            // 生成 JWT token
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole());

            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "登录成功");
            result.put("data", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "realName", user.getRealName() != null ? user.getRealName() : user.getUsername(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "role", user.getRole() != null ? user.getRole() : 0,
                "status", user.getStatus() != null ? user.getStatus() : 1,
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "token", token
            ));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("登录失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 获取所有用户列表
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            // 按创建时间降序排列
            List<Map<String, Object>> userList = users.stream()
                    .sorted((a, b) -> {
                        if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                        if (a.getCreateTime() == null) return 1;
                        if (b.getCreateTime() == null) return -1;
                        return b.getCreateTime().compareTo(a.getCreateTime());
                    })
                    .map(u -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getId());
                        map.put("username", u.getUsername());
                        map.put("realName", u.getRealName());
                        map.put("email", u.getEmail());
                        map.put("phone", u.getPhone());
                        map.put("role", u.getRole());
                        map.put("status", u.getStatus());
                        map.put("avatar", u.getAvatar());
                        map.put("createTime", u.getCreateTime());
                        map.put("updateTime", u.getUpdateTime());
                        return map;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("code", 0, "data", userList));
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            return userRepository.findById(id)
                    .map(user -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", user.getId());
                        map.put("username", user.getUsername());
                        map.put("realName", user.getRealName());
                        map.put("email", user.getEmail());
                        map.put("phone", user.getPhone());
                        map.put("role", user.getRole());
                        map.put("status", user.getStatus());
                        map.put("avatar", user.getAvatar());
                        return ResponseEntity.ok(Map.of("code", 0, "data", map));
                    })
                    .orElse(ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在")));
        } catch (Exception e) {
            log.error("获取用户失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 注册新用户
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> userData) {
        try {
            String username = (String) userData.get("username");
            String password = (String) userData.get("password");

            if (userRepository.existsByUsername(username)) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "用户名已存在"));
            }

            User user = new User();
            user.setUsername(username);
            user.setPassword(password); // 实际应该加密
            user.setRealName((String) userData.get("realName"));
            user.setEmail((String) userData.get("email"));
            user.setPhone((String) userData.get("phone"));
            user.setRole(userData.get("role") != null ? ((Number) userData.get("role")).intValue() : 0);
            user.setStatus(1);
            user.setCreateTime(java.time.LocalDateTime.now());

            user = userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "注册成功",
                "data", Map.of("id", user.getId(), "username", user.getUsername())
            ));
        } catch (Exception e) {
            log.error("注册用户失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> userData) {
        try {
            return userRepository.findById(id)
                    .map(user -> {
                        if (userData.containsKey("realName")) {
                            user.setRealName((String) userData.get("realName"));
                        }
                        if (userData.containsKey("email")) {
                            user.setEmail((String) userData.get("email"));
                        }
                        if (userData.containsKey("phone")) {
                            user.setPhone((String) userData.get("phone"));
                        }
                        if (userData.containsKey("role")) {
                            user.setRole(((Number) userData.get("role")).intValue());
                        }
                        user.setUpdateTime(java.time.LocalDateTime.now());
                        userRepository.save(user);
                        return ResponseEntity.ok(Map.of("code", 0, "message", "更新成功"));
                    })
                    .orElse(ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在")));
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 重置密码
     */
    @PutMapping("/reset-password/{id}")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            return userRepository.findById(id)
                    .map(user -> {
                        String newPassword = (String) data.get("newPassword");
                        user.setPassword(newPassword); // 实际应该加密
                        user.setPasswordChangeTime(java.time.LocalDateTime.now());
                        userRepository.save(user);
                        return ResponseEntity.ok(Map.of("code", 0, "message", "密码重置成功"));
                    })
                    .orElse(ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在")));
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            return userRepository.findById(id)
                    .map(user -> {
                        user.setStatus(((Number) data.get("status")).intValue());
                        userRepository.save(user);
                        return ResponseEntity.ok(Map.of("code", 0, "message", "状态更新成功"));
                    })
                    .orElse(ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在")));
        } catch (Exception e) {
            log.error("更新状态失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                return ResponseEntity.ok(Map.of("code", 0, "message", "删除成功"));
            }
            return ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在"));
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 搜索用户（供聊天功能使用）
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        try {
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> result = users.stream()
                    .filter(u -> u.getStatus() == 1)
                    .filter(u ->
                        (u.getUsername() != null && u.getUsername().contains(keyword)) ||
                        (u.getRealName() != null && u.getRealName().contains(keyword))
                    )
                    .map(u -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getId());
                        map.put("username", u.getUsername());
                        map.put("realName", u.getRealName() != null ? u.getRealName() : u.getUsername());
                        map.put("avatar", u.getAvatar());
                        map.put("email", u.getEmail());
                        map.put("phone", u.getPhone());
                        map.put("role", u.getRole());
                        return map;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("code", 0, "data", result));
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }
}
