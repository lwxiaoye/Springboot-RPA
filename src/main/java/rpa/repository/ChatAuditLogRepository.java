package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.ChatAuditLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天审计日志Repository
 */
@Repository
public interface ChatAuditLogRepository extends JpaRepository<ChatAuditLog, Long> {

    /**
     * 查询会话审计日志
     */
    Page<ChatAuditLog> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * 查询用户操作日志
     */
    Page<ChatAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询关联RPA的操作日志
     */
    List<ChatAuditLog> findByRelatedTypeAndRelatedIdOrderByCreatedAtDesc(String relatedType, Long relatedId);

    /**
     * 按时间范围查询
     */
    @Query("""
        SELECT a FROM ChatAuditLog a 
        WHERE a.createdAt BETWEEN :startTime AND :endTime
        ORDER BY a.createdAt DESC
        """)
    List<ChatAuditLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 搜索操作日志
     */
    @Query("""
        SELECT a FROM ChatAuditLog a 
        WHERE a.actionDetail LIKE %:keyword% OR a.userName LIKE %:keyword%
        ORDER BY a.createdAt DESC
        """)
    List<ChatAuditLog> searchAuditLogs(@Param("keyword") String keyword);

    /**
     * 获取用户的操作统计
     */
    @Query("""
        SELECT a.action, COUNT(a) 
        FROM ChatAuditLog a 
        WHERE a.userId = :userId 
        AND a.createdAt BETWEEN :startTime AND :endTime
        GROUP BY a.action
        """)
    List<Object[]> getUserActionStats(@Param("userId") Long userId, 
                                    @Param("startTime") LocalDateTime startTime, 
                                    @Param("endTime") LocalDateTime endTime);
}
