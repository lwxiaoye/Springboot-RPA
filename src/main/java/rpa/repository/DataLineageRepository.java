package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.DataLineage;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DataLineageRepository extends JpaRepository<DataLineage, Long> {

    List<DataLineage> findByTraceId(String traceId);

    List<DataLineage> findByProcessId(Long processId);

    List<DataLineage> findByTaskId(Long taskId);

    List<DataLineage> findByBusinessKey(String businessKey);

    @Query("SELECT dl FROM DataLineage dl WHERE dl.parentLineageId = :parentId")
    List<DataLineage> findByParentLineageId(@Param("parentId") Long parentId);

    @Query("SELECT dl FROM DataLineage dl WHERE dl.dataRecordId = :recordId ORDER BY dl.createTime DESC")
    List<DataLineage> findByDataRecordId(@Param("recordId") String recordId);

    @Query("SELECT dl FROM DataLineage dl WHERE dl.lineageType = :type AND dl.createTime BETWEEN :start AND :end")
    List<DataLineage> findByLineageTypeAndTimeRange(@Param("type") String type,
                                                     @Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);
}
