package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rpa.entity.User;
import rpa.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> login(String account, String password) {
        // 尝试通过用户名或手机号登录
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

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User updateProfile(Long id, String realName, String email, String phone) {
        return userRepository.findById(id).map(user -> {
            user.setRealName(realName);
            user.setEmail(email);
            user.setPhone(phone);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    public User updateUserRole(Long id, Integer role) {
        return userRepository.findById(id).map(user -> {
            user.setRole(role);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public User updatePassword(Long id, String oldPassword, String newPassword) {
        return userRepository.findById(id).map(user -> {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("原密码错误");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public User resetPassword(Long id, String newPassword) {
        return userRepository.findById(id).map(user -> {
            LocalDateTime now = LocalDateTime.now();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordChangeTime(now);
            user.setUpdateTime(now);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public boolean isPasswordExpired(User user) {
        // 移除 90 天密码过期机制
        return false;
    }

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

    public User updateUserStatus(Long id, Integer status) {
        return userRepository.findById(id).map(user -> {
            user.setStatus(status);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * 更新用户头像
     * @param id 用户 ID
     * @param avatarUrl 头像 URL
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
