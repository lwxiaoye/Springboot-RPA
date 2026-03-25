package rpa.repository;

import rpa.entity.DataParse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataParseRepository extends JpaRepository<DataParse, Long> {
    List<DataParse> findByStatus(Integer status);
    List<DataParse> findByCollectId(String collectId);
}
