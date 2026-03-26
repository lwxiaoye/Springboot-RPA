package rpa.repository;

import rpa.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    List<ExecutionLog> findByTaskId(Long taskId);
    List<ExecutionLog> findByProcessId(Long processId);
    List<ExecutionLog> findByRobotId(Long robotId);

    @Query("SELECT e FROM ExecutionLog e ORDER BY e.createTime DESC")
    List<ExecutionLog> findAllOrderByCreateTimeDesc();
}
