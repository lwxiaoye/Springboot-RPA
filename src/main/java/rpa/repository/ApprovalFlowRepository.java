package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.ApprovalFlow;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long> {

    Optional<ApprovalFlow> findByCode(String code);

    List<ApprovalFlow> findByStatus(String status);

    List<ApprovalFlow> findByFlowType(String flowType);

    List<ApprovalFlow> findByCurrentApproverId(Long approverId);

    @Query("SELECT af FROM ApprovalFlow af WHERE af.status = 'pending' AND af.currentApproverId = :approverId")
    List<ApprovalFlow> findPendingApprovals(@Param("approverId") Long approverId);

    Page<ApprovalFlow> findByFlowTypeAndStatus(String flowType, String status, Pageable pageable);

    @Query("SELECT af FROM ApprovalFlow af WHERE af.applyUserId = :userId ORDER BY af.createTime DESC")
    List<ApprovalFlow> findByApplyUserId(@Param("userId") Long userId);

    @Query("SELECT af FROM ApprovalFlow af WHERE af.targetType = :targetType AND af.targetId = :targetId")
    List<ApprovalFlow> findByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    long countByStatusAndFlowType(String status, String flowType);
}
