package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.Permission;
import rpa.repository.PermissionRepository;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository repository;

    public List<Permission> findAll() {
        return repository.findAll();
    }

    public Optional<Permission> findById(Long id) {
        return repository.findById(id);
    }

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

    public List<Permission> findMenus() {
        return repository.findByType("menu");
    }

    public Permission create(Permission permission) {
        return repository.save(permission);
    }

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

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
