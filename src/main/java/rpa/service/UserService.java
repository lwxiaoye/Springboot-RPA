package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rpa.entity.User;
import rpa.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * 用户服务类
 * <p>
 * 提供用户相关的业务逻辑处理，包括登录认证、密码管理、用户CRUD等。
 * </p>
 * <p>
 * 功能列表：
 * <ul>
 *   <li>登录认证：支持用户名/手机号登录</li>
 *   <li>密码管理：修改密码、重置密码、密码复杂度验证</li>
 *   <li>用户CRUD：创建、查询、更新、删除用户</li>
 *   <li>头像管理：上传和更新用户头像</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     * <p>
     * 支持用户名或手机号登录，自动识别登录方式。
     * 密码验证支持BCrypt加密和明文（兼容旧数据）。
     * </p>
     *
     * @param account 用户名或手机号
     * @param password 密码
     * @return Optional<User> 登录成功返回用户信息
     */
    public Optional<User> login(String account, String password) {
        return userRepository.findByUsername(account)
                .or(() -> userRepository.findByPhone(account))
                .filter(user -> {
                    // 检查用户状态，禁用的用户无法登录
                    if (user.getStatus() != null && user.getStatus() == 0) {
                        return false;
                    }
                        
                    String storedPassword = user.getPassword();
                    // 兼容处理：如果密码是 BCrypt 加密则验证加密，否则直接比较明文
                    if (storedPassword.startsWith("$2")) {
                        return passwordEncoder.matches(password, storedPassword);
                    } else {
                        // 明文密码比较（兼容旧数据）
                        return storedPassword.equals(password);
                    }
                });
    }

    /**
     * 根据用户名查询用户
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据手机号查询用户
     */
    public Optional<User> getUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    /**
     * 根据邮箱查询用户
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 根据ID查询用户
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 查询所有用户
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * 更新用户资料
     *
     * @param id 用户ID
     * @param realName 真实姓名
     * @param email 邮箱
     * @param phone 手机号
     * @return 更新后的用户
     */
    public User updateProfile(Long id, String realName, String email, String phone) {
        return userRepository.findById(id).map(user -> {
            user.setRealName(realName);
            user.setEmail(email);
            user.setPhone(phone);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    /**
     * 更新用户角色
     *
     * @param id 用户ID
     * @param role 角色值
     * @return 更新后的用户
     */
    public User updateUserRole(Long id, Integer role) {
        return userRepository.findById(id).map(user -> {
            user.setRole(role);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 修改密码（需验证旧密码）
     *
     * @param id 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 更新后的用户
     */
    public User updatePassword(Long id, String oldPassword, String newPassword) {
        return userRepository.findById(id).map(user -> {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("原密码错误");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 重置密码（管理员操作）
     *
     * @param id 用户ID
     * @param newPassword 新密码
     * @return 更新后的用户
     */
    public User resetPassword(Long id, String newPassword) {
        return userRepository.findById(id).map(user -> {
            LocalDateTime now = LocalDateTime.now();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordChangeTime(now);
            user.setUpdateTime(now);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 检查密码是否过期（已禁用，永远返回false）
     */
    public boolean isPasswordExpired(User user) {
        return false;
    }

    /**
     * 验证密码复杂度
     * <p>
     * 要求：长度8-24位，必须包含字母、数字和特殊字符
     * </p>
     *
     * @param password 密码
     * @return boolean 是否符合要求
     */
    public boolean validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8 || password.length() > 24) {
            return false;
        }
        
        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecialChar = true;
            }
        }
        
        return hasLetter && hasDigit && hasSpecialChar;
    }

    /**
     * 修改密码（统一方法）
     *
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 更新后的用户
     */
    public User changePassword(Long userId, String oldPassword, String newPassword) {
        return userRepository.findById(userId).map(user -> {
            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("原密码错误");
            }
            
            // 验证新密码复杂度
            if (!validatePasswordComplexity(newPassword)) {
                throw new RuntimeException("密码不符合复杂度要求：必须包含字母、数字和特殊字符，长度 8-24 位");
            }
            
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordChangeTime(LocalDateTime.now());
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param status 状态（1-启用，0-禁用）
     * @return 更新后的用户
     */
    public User updateUserStatus(Long id, Integer status) {
        return userRepository.findById(id).map(user -> {
            user.setStatus(status);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * 更新用户头像
     *
     * @param id 用户ID
     * @param avatarUrl 头像URL
     * @return 更新后的用户
     */
    public User updateAvatar(Long id, String avatarUrl) {
        return userRepository.findById(id).map(user -> {
            user.setAvatar(avatarUrl);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 创建新用户
     *
     * @param user 用户信息
     * @return 创建的用户
     */
    public User createUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setPasswordChangeTime(LocalDateTime.now());
        if (user.getRole() == null) {
            user.setRole(0);
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        // 密码加密存储
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
}
