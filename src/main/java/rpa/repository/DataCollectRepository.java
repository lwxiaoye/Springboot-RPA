package rpa.repository;

import rpa.entity.DataCollect;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataCollectRepository extends JpaRepository<DataCollect, Long> {
    List<DataCollect> findByStatus(Integer status);
    List<DataCollect> findByCreatorId(Long creatorId);
}
