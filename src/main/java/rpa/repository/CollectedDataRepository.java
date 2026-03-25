package rpa.repository;

import rpa.entity.CollectedData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CollectedDataRepository extends JpaRepository<CollectedData, Long> {
    List<CollectedData> findByCollectId(Long collectId);
    List<CollectedData> findByParseStatus(Integer parseStatus);
}
