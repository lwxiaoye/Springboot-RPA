package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.Permission;
import rpa.entity.Role;
import rpa.repository.PermissionRepository;
import rpa.repository.RoleRepository;
import java.util.*;

/**
 * 角色服务类
 * <p>
 * 提供角色相关的业务逻辑处理，包括角色CRUD和权限分配。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * 查询所有角色
     */
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    /**
     * 根据ID查询角色
     */
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    /**
     * 创建角色
     *
     * @param role 角色信息
     * @return 创建的角色
     */
    @Transactional
    public Role create(Role role) {
        if (roleRepository.existsByCode(role.getCode())) {
            throw new RuntimeException("角色编码已存在");
        }
        return roleRepository.save(role);
    }

    /**
     * 更新角色
     *
     * @param id 角色ID
     * @param request 更新信息
     * @return 更新后的角色
     */
    @Transactional
    public Role update(Long id, Role request) {
        return roleRepository.findById(id).map(role -> {
            if (roleRepository.existsByCodeAndIdNot(request.getCode(), id)) {
                throw new RuntimeException("角色编码已存在");
            }
            role.setName(request.getName());
            role.setCode(request.getCode());
            role.setDescription(request.getDescription());
            role.setStatus(request.getStatus());
            return roleRepository.save(role);
        }).orElseThrow(() -> new RuntimeException("角色不存在"));
    }

    /**
     * 更新角色权限
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID集合
     * @return 更新后的角色
     */
    @Transactional
    public Role updatePermissions(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        Set<Permission> permissions = new HashSet<>();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long pid : permissionIds) {
                permissionRepository.findById(pid).ifPresent(permissions::add);
            }
        }
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    /**
     * 删除角色
     * <p>
     * 系统管理员角色(id=1)不允许删除
     * </p>
     *
     * @param id 角色ID
     */
    @Transactional
    public void delete(Long id) {
        if (id == 1) {
            throw new RuntimeException("不能删除超级管理员角色");
        }
        roleRepository.deleteById(id);
    }

    /**
     * 初始化默认角色
     * <p>
     * 如果系统中还没有角色，则创建三个默认角色：
     * <ul>
     *   <li>系统管理员 - 拥有所有权限</li>
     *   <li>运营人员 - 拥有运营相关权限</li>
     *   <li>普通用户 - 拥有基础菜单权限</li>
     * </ul>
     * </p>
     */
    @Transactional
    public void initDefaultRoles() {
        if (roleRepository.count() == 0) {
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
            roleRepository.save(user);

            List<Permission> allPermissions = permissionRepository.findAll();
            admin.setPermissions(new HashSet<>(allPermissions));
            roleRepository.save(admin);

            Set<Permission> operatorPerms = new HashSet<>();
            for (Permission p : allPermissions) {
                if (!p.getCode().startsWith("USER_") && !p.getCode().startsWith("ROLE_")) {
                    operatorPerms.add(p);
                }
            }
            operator.setPermissions(operatorPerms);
            roleRepository.save(operator);

            log.info("默认角色初始化完成");
        }
    }
}
