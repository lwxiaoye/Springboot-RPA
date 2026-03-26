package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rpa.entity.Permission;
import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByType(String type);
    List<Permission> findByStatus(Integer status);
    List<Permission> findByParentId(Long parentId);
    List<Permission> findByParentIdOrderBySort(Long parentId);
}
