package rpa.repository;

import rpa.entity.DataQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataQueryRepository extends JpaRepository<DataQuery, Long> {
    List<DataQuery> findByStatus(Integer status);
    List<DataQuery> findBySourceTable(String sourceTable);
}
