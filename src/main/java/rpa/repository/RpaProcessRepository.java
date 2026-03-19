package rpa.repository;

import rpa.entity.RpaProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RpaProcessRepository extends JpaRepository<RpaProcess, Long> {
    List<RpaProcess> findByCreatorId(Long creatorId);
    List<RpaProcess> findByStatus(String status);
}
