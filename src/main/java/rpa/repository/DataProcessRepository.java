package rpa.repository;

import rpa.entity.DataProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataProcessRepository extends JpaRepository<DataProcess, Long> {
    List<DataProcess> findByStatus(Integer status);
}
