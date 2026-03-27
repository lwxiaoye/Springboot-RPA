package rpa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.Permission;
import rpa.entity.Role;
import rpa.entity.User;
import rpa.repository.PermissionRepository;
import rpa.repository.RoleRepository;
import rpa.repository.UserRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 数据初始化配置类
 * <p>
 * 实现CommandLineRunner接口，在Spring Boot应用启动时执行初始化操作。
 * 负责初始化系统的基础数据，包括：
 * <ul>
 *   <li>权限数据：系统菜单、按钮等权限定义</li>
 *   <li>角色数据：管理员、运营人员、普通用户等角色</li>
 *   <li>管理员账户：默认创建admin用户（密码：123456）</li>
 * </ul>
 * </p>
 * <p>
 * 初始化策略：
 * <ul>
 *   <li>幂等性检查：每次启动只初始化一次，已存在则跳过</li>
 *   <li>异常处理：初始化失败不影响应用启动</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see CommandLineRunner
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 应用启动时执行的初始化方法
     * <p>
     * 按顺序执行：
     * <ol>
     *   <li>initPermissions - 初始化权限数据</li>
     *   <li>initRoles - 初始化角色数据</li>
     *   <li>initAdminUser - 初始化管理员用户</li>
     * </ol>
     * </p>
     *
     * @param args 命令行参数
     */
    @Override
    @Transactional
    public void run(String... args) {
        initPermissions();
        initRoles();
        initAdminUser();
    }

    /**
     * 初始化权限数据
     * <p>
     * 创建系统所需的权限列表，包括：
     * <ul>
     *   <li>系统管理模块：用户管理、角色管理、资源管理</li>
     *   <li>RPA运营模块：任务管理、机器人管理、流程管理、执行日志</li>
     *   <li>数据管理模块：数据采集、数据解析、数据加工、数据查询</li>
     * </ul>
     * 每个权限包含名称、编码、类型、URL、排序和父子关系。
     * </p>
     */
    private void initPermissions() {
        // 检查是否已存在SYSTEM权限，如果存在则跳过
        Optional<Permission> existingSystem = permissionRepository.findById(1L);
        if (existingSystem.isPresent()) {
            log.info("权限数据已存在，跳过初始化");
            return;
        }

        List<Permission> permissions = Arrays.asList(
            // 系统管理
            createPermission("系统管理", "SYSTEM", "menu", "/system", 1, 0L),
            createPermission("个人信息", "SYSTEM_PROFILE", "menu", "/profile", 1, 1L),
            createPermission("用户管理", "USER_MANAGE", "menu", "/user", 2, 1L),
            createPermission("用户新增", "USER_ADD", "button", null, 3, 3L),
            createPermission("用户编辑", "USER_EDIT", "button", null, 4, 3L),
            createPermission("用户删除", "USER_DELETE", "button", null, 5, 3L),
            createPermission("角色管理", "ROLE_MANAGE", "menu", "/role", 6, 1L),
            createPermission("角色新增", "ROLE_ADD", "button", null, 7, 7L),
            createPermission("角色编辑", "ROLE_EDIT", "button", null, 8, 7L),
            createPermission("角色删除", "ROLE_DELETE", "button", null, 9, 7L),
            createPermission("资源管理", "RESOURCE_MANAGE", "menu", "/resource", 10, 1L),

            // RPA运营
            createPermission("RPA运营", "RPA", "menu", "/rpa", 11, 0L),
            createPermission("任务管理", "TASK_MANAGE", "menu", "/tasks", 12, 12L),
            createPermission("任务新增", "TASK_ADD", "button", null, 13, 12L),
            createPermission("任务编辑", "TASK_EDIT", "button", null, 14, 12L),
            createPermission("任务删除", "TASK_DELETE", "button", null, 15, 12L),
            createPermission("任务执行", "TASK_EXECUTE", "button", null, 16, 12L),
            createPermission("机器人管理", "ROBOT_MANAGE", "menu", "/robots", 17, 12L),
            createPermission("流程管理", "PROCESS_MANAGE", "menu", "/processes", 18, 12L),
            createPermission("执行日志", "LOG_MANAGE", "menu", "/logs", 19, 12L),

            // 数据管理
            createPermission("数据管理", "DATA", "menu", "/data", 20, 0L),
            createPermission("数据采集", "DATA_COLLECT", "menu", "/dataCollect", 21, 21L),
            createPermission("数据解析", "DATA_PARSE", "menu", "/dataParse", 22, 21L),
            createPermission("数据加工", "DATA_PROCESS", "menu", "/dataProcess", 23, 21L),
            createPermission("数据查询", "DATA_QUERY", "menu", "/dataQuery", 24, 21L)
        );

        try {
            permissionRepository.saveAll(permissions);
            log.info("初始化了 {} 个权限", permissions.size());
        } catch (Exception e) {
            log.warn("权限初始化跳过（可能已存在）: {}", e.getMessage());
        }
    }

    /**
     * 创建权限实体对象
     *
     * @param name 权限名称
     * @param code 权限编码（唯一）
     * @param type 权限类型（menu/button/api）
     * @param url 权限对应的URL路径
     * @param sort 排序号
     * @param parentId 父权限ID
     * @return Permission 权限实体
     */
    private Permission createPermission(String name, String code, String type, String url, Integer sort, Long parentId) {
        Permission p = new Permission();
        p.setName(name);
        p.setCode(code);
        p.setType(type);
        p.setUrl(url);
        p.setSort(sort);
        p.setParentId(parentId);
        p.setStatus(1);
        return p;
    }

    /**
     * 初始化角色数据
     * <p>
     * 创建系统默认的三个角色：
     * <ul>
     *   <li>ROLE_ADMIN (系统管理员)：拥有所有权限</li>
     *   <li>ROLE_OPERATOR (运营人员)：拥有除用户、角色、资源管理外的所有权限</li>
     *   <li>ROLE_USER (普通用户)：只拥有菜单访问权限</li>
     * </ul>
     * </p>
     */
    private void initRoles() {
        // 检查是否已存在管理员角色
        Optional<Role> existingAdmin = roleRepository.findByCode("ROLE_ADMIN");
        if (existingAdmin.isPresent()) {
            log.info("角色数据已存在，跳过初始化");
            return;
        }

        try {
            Role admin = new Role();
            admin.setName("系统管理员");
            admin.setCode("ROLE_ADMIN");
            admin.setDescription("拥有系统所有权限");
            admin.setStatus(1);
            admin = roleRepository.save(admin);

            Role operator = new Role();
            operator.setName("运营人员");
            operator.setCode("ROLE_OPERATOR");
            operator.setDescription("负责日常运营操作");
            operator.setStatus(1);
            operator = roleRepository.save(operator);

            Role user = new Role();
            user.setName("普通用户");
            user.setCode("ROLE_USER");
            user.setDescription("基础功能使用权限");
            user.setStatus(1);
            user = roleRepository.save(user);

            List<Permission> allPerms = permissionRepository.findAll();
            if (!allPerms.isEmpty()) {
                admin.setPermissions(new HashSet<>(allPerms));
                roleRepository.save(admin);

                Set<Permission> operatorPerms = new HashSet<>();
                for (Permission p : allPerms) {
                    if (!p.getCode().startsWith("USER_") && !p.getCode().startsWith("ROLE_") && !p.getCode().startsWith("RESOURCE_")) {
                        operatorPerms.add(p);
                    }
                }
                operator.setPermissions(operatorPerms);
                roleRepository.save(operator);

                Set<Permission> userPerms = new HashSet<>();
                for (Permission p : allPerms) {
                    if (p.getType().equals("menu") && !p.getCode().startsWith("USER_") && !p.getCode().startsWith("ROLE_") && !p.getCode().startsWith("RESOURCE_")) {
                        userPerms.add(p);
                    }
                }
                user.setPermissions(userPerms);
                roleRepository.save(user);
            }

            log.info("初始化了 3 个角色及其权限");
        } catch (Exception e) {
            log.warn("角色初始化跳过（可能已存在）: {}", e.getMessage());
        }
    }

    /**
     * 初始化管理员用户
     * <p>
     * 创建或更新系统管理员账户：
     * <ul>
     *   <li>用户名：admin</li>
     *   <li>密码：123456</li>
     *   <li>角色：ROLE_ADMIN</li>
     *   <li>状态：启用</li>
     * </ul>
     * 如果用户已存在，会检查密码是否已加密，未加密则更新为加密格式。
     * </p>
     */
    private void initAdminUser() {
        try {
            Optional<User> existingAdmin = userRepository.findByUsername("admin");
            if (existingAdmin.isPresent()) {
                User admin = existingAdmin.get();
                // 检查密码是否已加密，如果没有加密则加密存储
                String rawPassword = "123456";
                if (!admin.getPassword().startsWith("$2")) {
                    // 密码是明文，更新为加密密码
                    admin.setPassword(passwordEncoder.encode(rawPassword));
                    userRepository.save(admin);
                    log.info("管理员密码已更新为加密格式");
                }
                // 确保角色和状态正确
                if (admin.getRole() == null || admin.getRole() != 1) {
                    admin.setRole(1);
                    userRepository.save(admin);
                }
                if (admin.getStatus() == null || admin.getStatus() != 1) {
                    admin.setStatus(1);
                    userRepository.save(admin);
                }
                log.info("管理员用户已存在: admin/{}", rawPassword);
            } else {
                // 创建新管理员
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("123456"));
                admin.setRealName("系统管理员");
                admin.setEmail("admin@rpa.com");
                admin.setPhone("13800138000");
                admin.setRole(1);
                admin.setStatus(1);
                userRepository.save(admin);
                log.info("创建了管理员用户: admin/123456");
            }
        } catch (Exception e) {
            log.warn("用户初始化失败: {}", e.getMessage());
        }
    }
}
