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

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    @Transactional
    public Role create(Role role) {
        if (roleRepository.existsByCode(role.getCode())) {
            throw new RuntimeException("角色编码已存在");
        }
        return roleRepository.save(role);
    }

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

    @Transactional
    public void delete(Long id) {
        if (id == 1) {
            throw new RuntimeException("不能删除超级管理员角色");
        }
        roleRepository.deleteById(id);
    }

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
