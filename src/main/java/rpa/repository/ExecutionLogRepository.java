package rpa.repository;

import rpa.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    List<ExecutionLog> findByTaskId(Long taskId);
    List<ExecutionLog> findByProcessId(Long processId);
}
