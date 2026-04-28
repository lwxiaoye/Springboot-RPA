package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.ChatParticipant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话参与者Repository
 */
@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    /**
     * 检查用户是否在会话中
     */
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * 获取会话所有参与者
     */
    List<ChatParticipant> findByConversationId(Long conversationId);

    /**
     * 获取用户在会话中的信息
     */
    Optional<ChatParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * 获取用户参与的所有会话ID
     */
    @Query("SELECT p.conversationId FROM ChatParticipant p WHERE p.userId = :userId AND p.leftAt IS NULL")
    List<Long> findConversationIdsByUserId(@Param("userId") Long userId);

    /**
     * 获取用户未归档的会话
     */
    @Query("""
        SELECT p FROM ChatParticipant p 
        WHERE p.userId = :userId 
        AND p.isArchived = 0 
        AND p.leftAt IS NULL
        ORDER BY p.isPinned DESC, p.lastReadTime DESC
        """)
    List<ChatParticipant> findActiveByUserId(@Param("userId") Long userId);

    /**
     * 获取群主
     */
    @Query("SELECT p FROM ChatParticipant p WHERE p.conversationId = :conversationId AND p.role = 'owner'")
    Optional<ChatParticipant> findOwnerByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 获取管理员
     */
    @Query("SELECT p FROM ChatParticipant p WHERE p.conversationId = :conversationId AND p.role IN ('owner', 'admin')")
    List<ChatParticipant> findAdminsByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 统计会话成员数
     */
    long countByConversationIdAndLeftAtIsNull(Long conversationId);

    /**
     * 禁言用户
     */
    @Modifying
    @Query("UPDATE ChatParticipant p SET p.isMuted = :isMuted WHERE p.conversationId = :conversationId AND p.userId = :userId")
    void setMuted(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("isMuted") Integer isMuted);

    /**
     * 更新已读时间
     */
    @Modifying
    @Query("""
        UPDATE ChatParticipant p SET 
        p.lastReadTime = :readTime,
        p.unreadCount = 0
        WHERE p.conversationId = :conversationId AND p.userId = :userId
        """)
    void updateLastReadTime(@Param("conversationId") Long conversationId, 
                           @Param("userId") Long userId, 
                           @Param("readTime") LocalDateTime readTime);

    /**
     * 增加未读数
     */
    @Modifying
    @Query("UPDATE ChatParticipant p SET p.unreadCount = p.unreadCount + 1 WHERE p.conversationId = :conversationId AND p.userId != :excludeUserId AND p.leftAt IS NULL")
    void incrementUnreadCount(@Param("conversationId") Long conversationId, @Param("excludeUserId") Long excludeUserId);

    /**
     * 退出会话
     */
    @Modifying
    @Query("UPDATE ChatParticipant p SET p.leftAt = :leftAt WHERE p.conversationId = :conversationId AND p.userId = :userId")
    void leaveConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("leftAt") LocalDateTime leftAt);
}
