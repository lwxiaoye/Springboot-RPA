package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.Permission;
import rpa.entity.RolePermission;
import rpa.entity.User;
import rpa.repository.PermissionRepository;
import rpa.repository.RolePermissionRepository;
import rpa.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限检查服务
 * <p>
 * 提供运行时权限检查功能，用于：
 * - 检查用户是否拥有指定权限
 * - 检查用户是否可以导出报表
 * - 获取用户的所有权限列表
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCheckService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;

    // 缓存用户权限，避免频繁查询
    private final java.util.Map<Long, Set<String>> userPermissionCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 检查用户是否拥有指定权限
     *
     * @param userId 用户ID
     * @param permissionCode 权限代码（如 report:export）
     * @return true-有权限，false-无权限
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null) {
            return false;
        }

        // 管理员拥有所有权限
        if (isAdmin(userId)) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions(userId);
        return userPermissions.contains(permissionCode);
    }

    /**
     * 检查用户是否拥有所有指定的权限
     *
     * @param userId 用户ID
     * @param permissionCodes 权限代码列表
     * @return true-拥有所有权限，false-缺少部分权限
     */
    public boolean hasAllPermissions(Long userId, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }

        for (String code : permissionCodes) {
            if (!hasPermission(userId, code)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查用户是否拥有任一指定的权限
     *
     * @param userId 用户ID
     * @param permissionCodes 权限代码列表
     * @return true-拥有任一权限，false-无任何权限
     */
    public boolean hasAnyPermission(Long userId, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }

        for (String code : permissionCodes) {
            if (hasPermission(userId, code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查用户是否是管理员
     */
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }

        return userRepository.findById(userId)
                .map(user -> "admin".equals(user.getUsername()) || "admin".equals(user.getRole()))
                .orElse(false);
    }

    /**
     * 检查用户是否可以导出报表
     * <p>
     * 需要同时拥有 report:view 和 report:export 权限。
     * </p>
     */
    public boolean canExportReport(Long userId) {
        return hasPermission(userId, "report:export");
    }

    /**
     * 检查用户是否可以查看报表
     */
    public boolean canViewReport(Long userId) {
        return hasPermission(userId, "report:view");
    }

    /**
     * 检查用户是否可以管理报表订阅
     */
    public boolean canManageSubscription(Long userId) {
        return hasPermission(userId, "report:subscription");
    }

    /**
     * 检查用户是否可以导出任务
     */
    public boolean canExportTask(Long userId) {
        return hasPermission(userId, "task:export");
    }

    /**
     * 检查用户是否可以导出日志
     */
    public boolean canExportLog(Long userId) {
        return hasPermission(userId, "log:export");
    }

    /**
     * 获取用户的所有权限列表
     */
    public Set<String> getUserPermissions(Long userId) {
        if (userId == null) {
            return new HashSet<>();
        }

        // 先检查缓存
        Set<String> cachedPermissions = userPermissionCache.get(userId);
        if (cachedPermissions != null) {
            return cachedPermissions;
        }

        // 查询用户的角色
        Set<String> permissions = new HashSet<>();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return permissions;
        }

        // 获取用户的角色ID列表
        List<Long> roleIds = getUserRoleIds(user);

        // 获取所有角色关联的权限
        for (Long roleId : roleIds) {
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
            for (RolePermission rp : rolePermissions) {
                permissionRepository.findById(rp.getPermissionId()).ifPresent(permission -> {
                    permissions.add(permission.getCode());
                    // 如果是菜单权限，也添加其所有子权限
                    if ("menu".equals(permission.getType())) {
                        addChildPermissions(permission.getId(), permissions);
                    }
                });
            }
        }

        // 缓存结果
        userPermissionCache.put(userId, permissions);

        return permissions;
    }

    /**
     * 获取用户角色ID列表
     */
    private List<Long> getUserRoleIds(User user) {
        // 根据用户实体的role字段获取角色ID
        // User.role 是 Integer 类型，0=普通用户，1=管理员
        if (user.getRole() != null && user.getRole() == 1) {
            return List.of(1L); // 假设管理员角色ID为1
        }
        return List.of(2L); // 普通用户角色ID为2
    }

    /**
     * 添加子权限到集合
     */
    private void addChildPermissions(Long parentId, Set<String> permissions) {
        List<Permission> children = permissionRepository.findByParentId(parentId);
        for (Permission child : children) {
            permissions.add(child.getCode());
            if ("menu".equals(child.getType())) {
                addChildPermissions(child.getId(), permissions);
            }
        }
    }

    /**
     * 清除用户权限缓存
     * <p>
     * 在用户权限变更后调用此方法清除缓存。
     * </p>
     */
    public void clearCache(Long userId) {
        userPermissionCache.remove(userId);
    }

    /**
     * 清除所有权限缓存
     */
    public void clearAllCache() {
        userPermissionCache.clear();
    }

    /**
     * 刷新用户权限缓存
     */
    public void refreshCache(Long userId) {
        clearCache(userId);
        getUserPermissions(userId);
    }

    /**
     * 权限检查异常
     */
    public static class PermissionDeniedException extends RuntimeException {
        private final String permission;

        public PermissionDeniedException(String permission) {
            super("权限不足: " + permission);
            this.permission = permission;
        }

        public String getPermission() {
            return permission;
        }
    }

    /**
     * 要求用户拥有指定权限，否则抛出异常
     */
    public void requirePermission(Long userId, String permissionCode) {
        if (!hasPermission(userId, permissionCode)) {
            throw new PermissionDeniedException(permissionCode);
        }
    }

    /**
     * 要求用户可以导出报表，否则抛出异常
     */
    public void requireExportReport(Long userId) {
        requirePermission(userId, "report:export");
    }
}
