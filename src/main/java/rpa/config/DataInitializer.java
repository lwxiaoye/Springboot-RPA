package rpa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import rpa.entity.Permission;
import rpa.entity.Role;
import rpa.entity.RolePermission;
import rpa.repository.PermissionRepository;
import rpa.repository.RolePermissionRepository;
import rpa.repository.RoleRepository;
import rpa.service.SystemConfigService;

/**
 * 系统数据初始化器
 * <p>
 * 在应用启动时初始化默认配置数据和权限。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SystemConfigService systemConfigService;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化系统数据...");

        // 初始化系统配置
        try {
            systemConfigService.initDefaultConfigs();
            log.info("系统配置初始化完成");
        } catch (Exception e) {
            log.error("系统配置初始化失败", e);
        }

        // 初始化报表相关权限
        try {
            initReportPermissions();
            log.info("报表权限初始化完成");
        } catch (Exception e) {
            log.error("报表权限初始化失败", e);
        }
    }

    /**
     * 初始化报表相关权限
     * <p>
     * 添加报表导出权限、报表查看权限、报表订阅权限等。
     * 并自动将这些权限分配给管理员角色。
     * </p>
     */
    private void initReportPermissions() {
        // 检查是否已存在报表权限
        if (permissionRepository.findByCode("report:export").isPresent()) {
            log.info("报表权限已存在，跳过初始化");
            return;
        }

        log.info("开始初始化报表权限...");

        // 1. 创建报表权限
        Permission reportMenu = createPermissionIfNotExists(
                "报表中心", "report", "menu", "/rpa/reports", null, 8, 0L
        );

        Permission reportView = createPermissionIfNotExists(
                "报表查看", "report:view", "button", null, null, 81, reportMenu.getId()
        );

        Permission reportExport = createPermissionIfNotExists(
                "报表导出", "report:export", "button", null, null, 82, reportMenu.getId()
        );

        Permission reportSubscription = createPermissionIfNotExists(
                "报表订阅", "report:subscription", "button", null, null, 83, reportMenu.getId()
        );

        // 2. 创建任务权限
        Permission taskMenu = permissionRepository.findByCode("task").orElse(null);
        if (taskMenu == null) {
            taskMenu = createPermissionIfNotExists(
                    "任务中心", "task", "menu", "/rpa/tasks", null, 3, 0L
            );
        }

        Permission taskView = createPermissionIfNotExists(
                "任务查看", "task:view", "button", null, null, 31, taskMenu.getId()
        );

        Permission taskExport = createPermissionIfNotExists(
                "任务导出", "task:export", "button", null, null, 32, taskMenu.getId()
        );

        // 3. 创建执行日志权限
        Permission logMenu = permissionRepository.findByCode("log").orElse(null);
        if (logMenu == null) {
            logMenu = createPermissionIfNotExists(
                    "日志中心", "log", "menu", "/rpa/logs", null, 9, 0L
            );
        }
        final Permission finalLogMenu = logMenu;
        createPermissionIfNotExists(
                "日志导出", "log:export", "button", null, null, 91, finalLogMenu.getId()
        );

        // 4. 将报表权限分配给管理员角色
        roleRepository.findByCode("admin").ifPresent(adminRole -> {
            // 检查是否已分配
            boolean alreadyAssigned = rolePermissionRepository
                    .findByRoleId(adminRole.getId()).stream()
                    .anyMatch(rp -> rp.getPermissionId().equals(reportExport.getId()));

            if (!alreadyAssigned) {
                RolePermission rp1 = new RolePermission();
                rp1.setRoleId(adminRole.getId());
                rp1.setPermissionId(reportView.getId());
                rolePermissionRepository.save(rp1);

                RolePermission rp2 = new RolePermission();
                rp2.setRoleId(adminRole.getId());
                rp2.setPermissionId(reportExport.getId());
                rolePermissionRepository.save(rp2);

                RolePermission rp3 = new RolePermission();
                rp3.setRoleId(adminRole.getId());
                rp3.setPermissionId(reportSubscription.getId());
                rolePermissionRepository.save(rp3);

                RolePermission rp4 = new RolePermission();
                rp4.setRoleId(adminRole.getId());
                rp4.setPermissionId(taskExport.getId());
                rolePermissionRepository.save(rp4);

                RolePermission rp5 = new RolePermission();
                rp5.setRoleId(adminRole.getId());
                rp5.setPermissionId(finalLogMenu.getId());
                rolePermissionRepository.save(rp5);

                log.info("已为管理员角色分配报表相关权限");
            }
        });

        log.info("报表权限初始化完成");
    }

    /**
     * 创建权限（如果不存在）
     */
    private Permission createPermissionIfNotExists(String name, String code, String type,
                                                   String url, String method,
                                                   Integer sort, Long parentId) {
        return permissionRepository.findByCode(code).orElseGet(() -> {
            Permission permission = new Permission();
            permission.setName(name);
            permission.setCode(code);
            permission.setType(type);
            permission.setUrl(url);
            permission.setMethod(method);
            permission.setSort(sort);
            permission.setParentId(parentId);
            permission.setStatus(1); // 1-启用
            return permissionRepository.save(permission);
        });
    }
}
