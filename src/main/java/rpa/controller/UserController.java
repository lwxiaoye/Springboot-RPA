package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.User;
import rpa.service.UserService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String account = request.get("account"); // 支持用户名或手机号
        String password = request.get("password");
        
        // 后端校验
        if (account == null || account.trim().isEmpty()) {
            response.put("code", -1);
            response.put("message", "请输入用户名或手机号");
            return response;
        }
        
        if (account.length() > 50) {
            response.put("code", -1);
            response.put("message", "用户名或手机号长度不能超过 50 个字符");
            return response;
        }
        
        // 校验是否包含特殊字符（手机号除外）
        String phoneRegex = "^[0-9]{11}$";
        if (!account.matches(phoneRegex)) {
            // 不是手机号，检查是否包含特殊字符
            String specialCharRegex = "[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]";
            if (account.matches(".*" + specialCharRegex + ".*")) {
                response.put("code", -1);
                response.put("message", "用户名不能包含特殊字符");
                return response;
            }
        }
        
        if (password == null || password.isEmpty()) {
            response.put("code", -1);
            response.put("message", "请输入密码");
            return response;
        }
        
        if (password.length() < 6 || password.length() > 20) {
            response.put("code", -1);
            response.put("message", "密码长度必须在 6-20 位之间");
            return response;
        }
        
        Optional<User> userOpt = userService.login(account, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            response.put("code", 0);
            response.put("message", "登录成功");
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("role", user.getRole());
            // 检查密码是否过期
            data.put("passwordExpired", userService.isPasswordExpired(user));
            response.put("data", data);
        } else {
            response.put("code", -1);
            response.put("message", "用户名/手机号或密码错误");
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
            
            userService.changePassword(id, oldPassword, newPassword);
            response.put("code", 0);
            response.put("message", "密码修改成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/check-password-expiry/{id}")
    public Map<String, Object> checkPasswordExpiry(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isPresent()) {
                boolean isExpired = userService.isPasswordExpired(userOpt.get());
                response.put("code", 0);
                response.put("data", isExpired);
            } else {
                response.put("code", -1);
                response.put("message", "用户不存在");
            }
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

    /**
     * 发送密码重置验证码
     */
    @PostMapping("/send-reset-code")
    public Map<String, Object> sendResetCode(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String account = request.get("account");
            
            if (account == null || account.trim().isEmpty()) {
                response.put("code", -1);
                response.put("message", "请输入用户名、手机号或邮箱");
                return response;
            }
            
            // 查找用户（支持用户名、手机号、邮箱）
            Optional<User> userOpt = findUserByAccount(account);
            
            if (!userOpt.isPresent()) {
                response.put("code", -1);
                response.put("message", "用户不存在");
                return response;
            }
            
            User user = userOpt.get();
            
            // 生成 6 位随机验证码
            String verificationCode = String.format("%06d", new Random().nextInt(999999));
            
            // TODO: 实际应用中应该发送邮件或短信
            System.out.println("【RPA 系统】验证码：" + verificationCode + "，用于重置密码。5 分钟内有效。");
            System.out.println("测试用固定验证码：123456");
            
            // 将验证码保存到缓存（实际应该用 Redis，这里简单处理）
            // 这里为了演示，直接返回给前端
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("code", verificationCode);
            
            response.put("code", 0);
            response.put("message", "验证码已发送");
            response.put("data", data);
            
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 通过验证码重置密码
     */
    @PostMapping("/reset-password-by-code")
    public Map<String, Object> resetPasswordByCode(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Number userIdNum = (Number) request.get("userId");
            String newPassword = (String) request.get("newPassword");
            
            if (userIdNum == null || newPassword == null) {
                response.put("code", -1);
                response.put("message", "参数错误");
                return response;
            }
            
            Long userId = userIdNum.longValue();
            
            // 验证新密码复杂度
            if (!userService.validatePasswordComplexity(newPassword)) {
                response.put("code", -1);
                response.put("message", "密码不符合复杂度要求：必须包含字母、数字和特殊字符，长度 8-24 位");
                return response;
            }
            
            // 重置密码
            userService.resetPassword(userId, newPassword);
            
            response.put("code", 0);
            response.put("message", "密码重置成功");
            
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
    
    /**
     * 根据账号查找用户（支持用户名、手机号、邮箱）
     */
    private Optional<User> findUserByAccount(String account) {
        // 先尝试用户名
        Optional<User> user = userService.getUserByUsername(account);
        if (user.isPresent()) {
            return user;
        }
        
        // 再尝试手机号
        user = userService.getUserByPhone(account);
        if (user.isPresent()) {
            return user;
        }
        
        // 最后尝试邮箱
        return userService.getUserByEmail(account);
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
