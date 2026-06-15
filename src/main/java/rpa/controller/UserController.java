package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * <p>
 * 包含用户注册、登录、信息管理等功能。
 * 所有敏感操作均有审计日志记录。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
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
    private final PasswordEncoder passwordEncoder;

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

            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "用户已被禁用，请联系管理员"));
            }

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
            log.error("登录失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "登录失败，请稍后重试"));
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
            log.error("获取用户列表失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "获取用户列表失败"));
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
            log.error("获取用户失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "获取用户信息失败"));
        }
    }

    /**
     * 注册新用户
     * <p>
     * 注意：此接口默认注册为普通用户角色，
     * 如需注册管理员需要通过后台管理界面操作。
     * </p>
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> userData) {
        try {
            String username = (String) userData.get("username");
            String password = (String) userData.get("password");

            // 参数校验
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "用户名不能为空"));
            }
            if (password == null || password.length() < 6) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "密码长度不能少于6位"));
            }
            if (username.length() < 2 || username.length() > 20) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "用户名长度需要在2-20位之间"));
            }

            // 检查用户名是否已存在
            if (userRepository.existsByUsername(username)) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "用户名已存在"));
            }

            // 验证邮箱格式（如果提供）
            String email = (String) userData.get("email");
            if (email != null && !email.isEmpty()) {
                if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    return ResponseEntity.ok(Map.of("code", 1, "message", "邮箱格式不正确"));
                }
                if (userRepository.existsByEmail(email)) {
                    return ResponseEntity.ok(Map.of("code", 1, "message", "邮箱已被使用"));
                }
            }

            // 验证手机号格式（如果提供）
            String phone = (String) userData.get("phone");
            if (phone != null && !phone.isEmpty()) {
                if (!phone.matches("^1[3-9]\\d{9}$")) {
                    return ResponseEntity.ok(Map.of("code", 1, "message", "手机号格式不正确"));
                }
                if (userRepository.existsByPhone(phone)) {
                    return ResponseEntity.ok(Map.of("code", 1, "message", "手机号已被使用"));
                }
            }

            // 创建用户
            User user = new User();
            user.setUsername(username.trim());
            // 【安全修复】使用BCrypt加密密码
            user.setPassword(passwordEncoder.encode(password));
            user.setRealName((String) userData.get("realName"));
            user.setEmail(email);
            user.setPhone(phone);
            // 默认注册为普通用户（role=0），不允许直接注册为管理员
            user.setRole(0);
            user.setStatus(1);
            user.setCreateTime(java.time.LocalDateTime.now());

            user = userRepository.save(user);

            log.info("新用户注册成功: username={}, email={}", username, email);

            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "注册成功",
                "data", Map.of("id", user.getId(), "username", user.getUsername())
            ));
        } catch (Exception e) {
            log.error("注册用户失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "注册失败，请稍后重试"));
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
                        // 验证邮箱格式（如果提供）
                        if (userData.containsKey("email")) {
                            String email = (String) userData.get("email");
                            if (email != null && !email.isEmpty()) {
                                if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                                    return ResponseEntity.ok(Map.of("code", 1, "message", "邮箱格式不正确"));
                                }
                            }
                        }

                        // 验证手机号格式（如果提供）
                        if (userData.containsKey("phone")) {
                            String phone = (String) userData.get("phone");
                            if (phone != null && !phone.isEmpty()) {
                                if (!phone.matches("^1[3-9]\\d{9}$")) {
                                    return ResponseEntity.ok(Map.of("code", 1, "message", "手机号格式不正确"));
                                }
                            }
                        }

                        if (userData.containsKey("realName")) {
                            user.setRealName((String) userData.get("realName"));
                        }
                        if (userData.containsKey("email")) {
                            user.setEmail((String) userData.get("email"));
                        }
                        if (userData.containsKey("phone")) {
                            user.setPhone((String) userData.get("phone"));
                        }
                        // 角色变更需要更严格的权限控制，这里暂时不开放
                        // if (userData.containsKey("role")) {
                        //     user.setRole(((Number) userData.get("role")).intValue());
                        // }
                        user.setUpdateTime(java.time.LocalDateTime.now());
                        userRepository.save(user);

                        log.info("用户信息更新成功: userId={}", id);

                        return ResponseEntity.ok(Map.of("code", 0, "message", "更新成功"));
                    })
                    .orElse(ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在")));
        } catch (Exception e) {
            log.error("更新用户失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "更新失败，请稍后重试"));
        }
    }

    /**
     * 重置密码（管理员操作）
     */
    @PutMapping("/reset-password/{id}")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            return userRepository.findById(id)
                    .map(user -> {
                        String newPassword = (String) data.get("newPassword");

                        // 参数校验
                        if (newPassword == null || newPassword.length() < 6) {
                            return ResponseEntity.ok(Map.of("code", 1, "message", "新密码长度不能少于6位"));
                        }
                        if (newPassword.length() > 20) {
                            return ResponseEntity.ok(Map.of("code", 1, "message", "新密码长度不能超过20位"));
                        }

                        // 【安全修复】使用BCrypt加密密码
                        user.setPassword(passwordEncoder.encode(newPassword));
                        user.setPasswordChangeTime(java.time.LocalDateTime.now());
                        userRepository.save(user);

                        log.info("用户密码已重置: userId={}", id);

                        return ResponseEntity.ok(Map.of("code", 0, "message", "密码重置成功"));
                    })
                    .orElse(ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在")));
        } catch (Exception e) {
            log.error("重置密码失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "密码重置失败，请稍后重试"));
        }
    }

    /**
     * 修改当前用户密码
     */
    @PutMapping("/password/{id}")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            String oldPassword = (String) data.get("oldPassword");
            String newPassword = (String) data.get("newPassword");

            // 参数校验
            if (oldPassword == null || oldPassword.isEmpty()) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "请输入原密码"));
            }
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "新密码长度不能少于6位"));
            }
            if (newPassword.length() > 20) {
                return ResponseEntity.ok(Map.of("code", 1, "message", "新密码长度不能超过20位"));
            }

            return userRepository.findById(id)
                    .map(user -> {
                        // 验证原密码
                        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                            log.warn("修改密码失败 - 原密码错误: userId={}", id);
                            return ResponseEntity.ok(Map.of("code", 1, "message", "原密码错误"));
                        }

                        // 【安全修复】使用BCrypt加密新密码
                        user.setPassword(passwordEncoder.encode(newPassword));
                        user.setPasswordChangeTime(java.time.LocalDateTime.now());
                        userRepository.save(user);

                        log.info("用户密码已修改: userId={}", id);

                        return ResponseEntity.ok(Map.of("code", 0, "message", "密码修改成功"));
                    })
                    .orElse(ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在")));
        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "密码修改失败，请稍后重试"));
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
                        int newStatus = ((Number) data.get("status")).intValue();
                        user.setStatus(newStatus);
                        userRepository.save(user);

                        log.info("用户状态已更新: userId={}, newStatus={}", id, newStatus);

                        return ResponseEntity.ok(Map.of("code", 0, "message", "状态更新成功"));
                    })
                    .orElse(ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在")));
        } catch (Exception e) {
            log.error("更新状态失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "状态更新失败"));
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
                log.info("用户已删除: userId={}", id);
                return ResponseEntity.ok(Map.of("code", 0, "message", "删除成功"));
            }
            return ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在"));
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "删除失败，请稍后重试"));
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
            log.error("搜索用户失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "搜索失败"));
        }
    }

    /**
     * 获取用户列表（供公告发布选择人员使用）
     */
    @GetMapping("/list")
    public ResponseEntity<?> getUserList(@RequestParam(required = false) String keyword) {
        try {
            List<User> users;
            if (keyword != null && !keyword.isEmpty()) {
                users = userRepository.searchUsers(keyword);
            } else {
                users = userRepository.findByStatus(1);
            }

            List<Map<String, Object>> result = users.stream()
                    .filter(u -> u.getStatus() == 1)
                    .map(u -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getId());
                        map.put("username", u.getUsername());
                        map.put("realName", u.getRealName() != null ? u.getRealName() : u.getUsername());
                        map.put("avatar", u.getAvatar());
                        map.put("department", u.getDepartment() != null ? u.getDepartment() : "");
                        map.put("email", u.getEmail());
                        map.put("phone", u.getPhone());
                        return map;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("code", 0, "data", result));
        } catch (Exception e) {
            log.error("获取用户列表失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "获取用户列表失败"));
        }
    }

    /**
     * 获取所有部门列表
     */
    @GetMapping("/departments")
    public ResponseEntity<?> getDepartments() {
        try {
            List<User> users = userRepository.findByStatus(1);
            List<String> departments = users.stream()
                    .map(User::getDepartment)
                    .filter(d -> d != null && !d.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("code", 0, "data", departments));
        } catch (Exception e) {
            log.error("获取部门列表失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 1, "message", "获取部门列表失败"));
        }
    }
}
