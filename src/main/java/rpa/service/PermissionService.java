package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.Permission;
import rpa.repository.PermissionRepository;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务类
 * <p>
 * 提供权限相关的业务逻辑处理，包括权限CRUD和权限树构建。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository repository;

    /**
     * 查询所有权限
     */
    public List<Permission> findAll() {
        return repository.findAll();
    }

    /**
     * 根据ID查询权限
     */
    public Optional<Permission> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 构建权限树
     * <p>
     * 将扁平化的权限列表构建成树形结构，
     * 根据parentId关联父子关系。
     * </p>
     *
     * @return List<Permission> 权限树
     */
    public List<Permission> findTree() {
        List<Permission> all = repository.findAll();
        List<Permission> roots = all.stream()
                .filter(p -> p.getParentId() == null || p.getParentId() == 0)
                .sorted(Comparator.comparing(Permission::getSort))
                .collect(Collectors.toList());

        for (Permission root : roots) {
            buildTree(root, all);
        }
        return roots;
    }

    /**
     * 递归构建子树
     *
     * @param parent 父权限
     * @param all 所有权限列表
     */
    private void buildTree(Permission parent, List<Permission> all) {
        List<Permission> children = all.stream()
                .filter(p -> parent.getId().equals(p.getParentId()))
                .sorted(Comparator.comparing(Permission::getSort))
                .collect(Collectors.toList());
        parent.setChildren(children);
        for (Permission child : children) {
            buildTree(child, all);
        }
    }

    /**
     * 查询所有菜单权限
     */
    public List<Permission> findMenus() {
        return repository.findByType("menu");
    }

    /**
     * 创建权限
     */
    public Permission create(Permission permission) {
        return repository.save(permission);
    }

    /**
     * 更新权限
     *
     * @param id 权限ID
     * @param request 更新信息
     * @return 更新后的权限
     */
    public Permission update(Long id, Permission request) {
        return repository.findById(id).map(p -> {
            p.setName(request.getName());
            p.setCode(request.getCode());
            p.setType(request.getType());
            p.setUrl(request.getUrl());
            p.setMethod(request.getMethod());
            p.setIcon(request.getIcon());
            p.setSort(request.getSort());
            p.setParentId(request.getParentId());
            p.setStatus(request.getStatus());
            return repository.save(p);
        }).orElseThrow(() -> new RuntimeException("权限不存在"));
    }

    /**
     * 删除权限
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
