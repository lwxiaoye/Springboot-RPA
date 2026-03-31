package rpa.controller;

/**
 * 用户管理控制器
 * <p>
 * 提供用户相关的RESTful API接口，包括：
 * <ul>
 *   <li>登录认证：支持用户名/手机号登录，返回JWT Token</li>
 *   <li>用户CRUD：创建、查询、更新、删除用户</li>
 *   <li>密码管理：修改密码、重置密码、密码重置验证码</li>
 *   <li>头像管理：上传和获取用户头像</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rpa.config.JwtUtils;
import rpa.entity.User;
import rpa.service.UserService;
import rpa.repository.UserRepository;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String account = request.get("account"); // 支持用户名或手机号
        if (account == null || account.trim().isEmpty()) {
            account = request.get("username"); // 兼容前端发送的username参数
        }
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
                    
            // 处理头像路径：如果数据库中的路径包含 /api/前缀，去掉它
            String avatar = user.getAvatar();
            if (avatar != null && avatar.startsWith("/api/")) {
                avatar = avatar.replace("/api/", "/");
            }
            data.put("avatar", avatar); // 返回处理后的头像路径
        
            // 生成 JWT token
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole());
            data.put("token", token);
        
            data.put("status", user.getStatus()); // 返回用户状态
            response.put("data", data);
        } else {
            // 检查用户是否存在但被禁用
            Optional<User> disabledUserOpt = userRepository.findByUsername(account);
            if (disabledUserOpt.isEmpty()) {
                disabledUserOpt = userRepository.findByPhone(account);
            }

            if (disabledUserOpt.isPresent() && disabledUserOpt.get().getStatus() == 0) {
                response.put("code", -1);
                response.put("message", "该账号已被禁用，请联系管理员");
            } else {
                response.put("code", -1);
                response.put("message", "用户名/手机号或密码错误");
            }
        }
        return response;
    }


    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // 处理头像路径
            String avatar = user.getAvatar();
            if (avatar != null && avatar.startsWith("/api/")) {
                avatar = avatar.replace("/api/", "/");
            }
            user.setAvatar(avatar);
            
            response.put("code", 0);
            response.put("data", user);
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
            String avatar = request.containsKey("avatar") ? (String) request.get("avatar") : null; // 支持清除头像

            User user = userService.updateProfile(id, realName, email, phone);
            
            // 更新头像（包括设置为 null）
            if (request.containsKey("avatar")) {
                userService.updateAvatar(id, avatar);
            }

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
                response.put("code", 0);
                response.put("data", false); // 密码永不过期
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
        List<User> users = userService.findAll();
        
        // 处理所有用户的头像路径
        for (User user : users) {
            String avatar = user.getAvatar();
            if (avatar != null && avatar.startsWith("/api/")) {
                user.setAvatar(avatar.replace("/api/", "/"));
            }
        }
        
        response.put("code", 0);
        response.put("data", users);
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
            
            // 如果是手机号，优先使用手机号验证
            boolean isPhone = account.matches("^[0-9]{11}$");
            if (isPhone && user.getPhone() != null && !user.getPhone().equals(account)) {
                response.put("code", -1);
                response.put("message", "手机号不匹配");
                return response;
            }
            
            // TODO: 实际应用中应该发送邮件或短信
            System.out.println("【RPA 系统】验证码：" + verificationCode + "，用于重置密码。5 分钟内有效。");
            System.out.println("测试用固定验证码：123456");
            
            // 将验证码保存到缓存（实际应该用 Redis，这里简单处理）
            // 这里为了演示，直接返回给前端
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("code", "123456"); // 测试期间固定返回 123456
            
            response.put("code", 0);
            response.put("message", "验证码已发送，有效期 5 分钟");
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
            String account = (String) request.get("account");
            String code = (String) request.get("code");
            String newPassword = (String) request.get("newPassword");
            
            if (account == null || code == null || newPassword == null) {
                response.put("code", -1);
                response.put("message", "参数错误");
                return response;
            }
            
            // 验证验证码（测试期间固定验证 123456）
            if (!"123456".equals(code)) {
                response.put("code", -1);
                response.put("message", "验证码错误");
                return response;
            }
            
            // 验证新密码长度（至少 6 位）
            if (newPassword.length() < 6 || newPassword.length() > 20) {
                response.put("code", -1);
                response.put("message", "密码长度必须在 6-20 位之间");
                return response;
            }
            
            // 根据账号查找用户
            Optional<User> userOpt = findUserByAccount(account);
            if (!userOpt.isPresent()) {
                response.put("code", -1);
                response.put("message", "用户不存在");
                return response;
            }
            
            User user = userOpt.get();
            
            // 重置密码
            userService.resetPassword(user.getId(), newPassword);
            
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

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            String realName = (String) request.get("realName");
            String email = (String) request.get("email");
            String phone = (String) request.get("phone");
            Integer role = request.get("role") != null ? ((Number) request.get("role")).intValue() : 0;

            if (username == null || username.trim().isEmpty()) {
                response.put("code", -1);
                response.put("message", "请输入用户名");
                return response;
            }

            // 若未提供密码，使用默认密码 123456
            if (password == null || password.isEmpty()) {
                password = "123456";
            }

            if (password.length() < 6 || password.length() > 20) {
                response.put("code", -1);
                response.put("message", "密码长度必须在 6-20 位之间");
                return response;
            }

            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRealName(realName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole(role);
            user.setStatus(1);

            User createdUser = userService.createUser(user);
            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", createdUser);
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

    /**
     * 上传头像
     */
    @PostMapping("/avatar/{id}")
    public Map<String, Object> uploadAvatar(@PathVariable Long id,
                                            @RequestParam("avatar") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("📥 收到头像上传请求：userId=" + id + ", fileName=" + file.getOriginalFilename());
        
        if (file.isEmpty()) {
            System.err.println("❌ 上传的文件为空");
            response.put("code", -1);
            response.put("message", "请选择文件");
            return response;
        }
        
        // 检查文件大小（2MB）
        if (file.getSize() > 2 * 1024 * 1024) {
            System.err.println("❌ 文件大小超过限制：" + file.getSize() + " bytes");
            response.put("code", -1);
            response.put("message", "文件大小不能超过 2MB");
            return response;
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            System.err.println("❌ 文件类型不支持：" + contentType);
            response.put("code", -1);
            response.put("message", "只支持图片文件");
            return response;
        }
        
        try {
            // 保存到本地文件系统
            String fileName = saveAvatarFile(file, id);
            System.out.println("✅ 头像文件已保存：" + fileName);
            
            // 更新用户信息
            String avatarPath = "/user/avatar/image/" + fileName;
            userService.updateAvatar(id, avatarPath);
            System.out.println("✅ 用户头像已更新：userId=" + id + ", path=" + avatarPath);
            
            response.put("code", 0);
            response.put("message", "头像上传成功");
            Map<String, String> data = new HashMap<>();
            data.put("imageUrl", "/user/avatar/image/" + fileName);  // 返回相对路径
            response.put("data", data);
            
        } catch (IOException e) {
            System.err.println("❌ 上传失败：" + e.getMessage());
            e.printStackTrace();
            response.put("code", -1);
            response.put("message", "上传失败：" + e.getMessage());
        }
        
        return response;
    }

    /**
     * 获取头像图片
     */
    @GetMapping("/avatar/image/{filename}")
    public byte[] getAvatarImage(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get("uploads/avatars/" + filename);
        return Files.readAllBytes(filePath);
    }

    /**
     * 保存头像文件到本地
     */
    private String saveAvatarFile(MultipartFile file, Long userId) throws IOException {
        // 使用绝对路径，避免相对路径问题
        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "avatars";
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("✅ 创建上传目录：" + uploadDir);
        }
        
        // 生成文件名：userId_timestamp.ext
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "avatar_" + userId + "_" + timestamp + extension;
        
        // 保存文件
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        return filename;
    }
}
