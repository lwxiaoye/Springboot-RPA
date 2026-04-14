package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.AsyncExportTask;
import java.util.Optional;

@Repository
public interface AsyncExportTaskRepository extends JpaRepository<AsyncExportTask, Long> {

    Optional<AsyncExportTask> findByTaskNo(String taskNo);

    Page<AsyncExportTask> findByUserId(Long userId, Pageable pageable);

    Page<AsyncExportTask> findByStatus(String status, Pageable pageable);

    @Query("SELECT t FROM AsyncExportTask t WHERE t.userId = :userId ORDER BY t.createTime DESC")
    Page<AsyncExportTask> findUserTasks(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM AsyncExportTask t WHERE t.status = 'pending' OR t.status = 'processing'")
    long countProcessingTasks();
}
