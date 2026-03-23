package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rpa.entity.Resource;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByStatus(Integer status);
    List<Resource> findByType(String type);
}
