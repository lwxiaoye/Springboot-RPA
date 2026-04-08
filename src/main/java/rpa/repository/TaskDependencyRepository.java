package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.TaskDependency;
import java.util.List;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    List<TaskDependency> findByStatus(String status);

    List<TaskDependency> findByParentTaskId(Long parentTaskId);

    @Query("SELECT td FROM TaskDependency td WHERE td.parentTaskId = :parentTaskId")
    List<TaskDependency> findByParentTask(@Param("parentTaskId") Long parentTaskId);

    @Query("SELECT td FROM TaskDependency td WHERE td.rootTaskId = :rootTaskId")
    List<TaskDependency> findByRootTask(@Param("rootTaskId") Long rootTaskId);

    long countByStatus(String status);
}
