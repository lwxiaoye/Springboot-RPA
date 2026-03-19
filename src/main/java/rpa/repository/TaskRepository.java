package rpa.repository;

import rpa.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssigneeId(Long assigneeId);
    List<Task> findByRobotId(Long robotId);
    List<Task> findByStatus(String status);
    List<Task> findByCategory(String category);
}
