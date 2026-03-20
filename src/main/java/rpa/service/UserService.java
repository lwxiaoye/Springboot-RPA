package rpa.service;

import lombok.RequiredArgsConstructor;
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

    public Optional<User> login(String account, String password) {
        // 尝试通过用户名或手机号登录
        return userRepository.findByUsername(account)
                .or(() -> userRepository.findByPhone(account))
                .filter(user -> user.getPassword().equals(password));
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
            if (!user.getPassword().equals(oldPassword)) {
                throw new RuntimeException("原密码错误");
            }
            user.setPassword(newPassword);
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public User resetPassword(Long id, String newPassword) {
        return userRepository.findById(id).map(user -> {
            LocalDateTime now = LocalDateTime.now();
            user.setPassword(newPassword);
            user.setPasswordChangeTime(now);
            user.setUpdateTime(now); // 使用本地时间直接替换原有的更新时间
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public boolean isPasswordExpired(User user) {
        if (user.getPasswordChangeTime() == null) {
            return true; // 从未修改过密码，需要强制修改
        }
        LocalDateTime now = LocalDateTime.now();
        long daysBetween = java.time.Duration.between(user.getPasswordChangeTime(), now).toDays();
        return daysBetween >= 90; // 90 天强制修改
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
            if (!user.getPassword().equals(oldPassword)) {
                throw new RuntimeException("原密码错误");
            }
            
            // 验证新密码复杂度
            if (!validatePasswordComplexity(newPassword)) {
                throw new RuntimeException("密码不符合复杂度要求：必须包含字母、数字和特殊字符，长度 8-24 位");
            }
            
            user.setPassword(newPassword);
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
}
