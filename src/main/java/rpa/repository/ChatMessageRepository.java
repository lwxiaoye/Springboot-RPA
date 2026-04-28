package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 聊天消息Repository
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 获取会话消息（按时间倒序）
     */
    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * 获取会话消息（按时间正序）
     */
    List<ChatMessage> findByConversationIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long conversationId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取会话最新消息
     */
    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);

    /**
     * 获取未读消息数量
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.createdAt > :lastReadTime AND m.senderId != :userId")
    long countUnreadMessages(@Param("conversationId") Long conversationId, @Param("lastReadTime") LocalDateTime lastReadTime, @Param("userId") Long userId);

    /**
     * 搜索消息内容
     */
    @Query("""
        SELECT m FROM ChatMessage m 
        WHERE m.conversationId = :conversationId 
        AND m.content LIKE %:keyword% 
        AND m.isRecalled = 0
        ORDER BY m.createdAt DESC
        """)
    List<ChatMessage> searchMessages(@Param("conversationId") Long conversationId, @Param("keyword") String keyword);

    /**
     * 获取RPA卡片消息
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.cardType IS NOT NULL AND m.isRecalled = 0 ORDER BY m.createdAt DESC")
    List<ChatMessage> findRPACards(@Param("conversationId") Long conversationId);

    /**
     * 撤回消息
     */
    @Modifying
    @Query("""
        UPDATE ChatMessage m SET 
        m.isRecalled = 1, 
        m.recalledAt = :recalledAt, 
        m.recalledBy = :recalledBy,
        m.content = '[此消息已被撤回]'
        WHERE m.id = :messageId
        """)
    void recallMessage(@Param("messageId") Long messageId, 
                       @Param("recalledAt") LocalDateTime recalledAt, 
                       @Param("recalledBy") Long recalledBy);

    /**
     * 根据卡片类型获取消息
     */
    List<ChatMessage> findByConversationIdAndCardTypeAndIsRecalledFalseOrderByCreatedAtDesc(Long conversationId, String cardType);

    /**
     * 获取关联RPA资源的分享消息
     */
    @Query("""
        SELECT m FROM ChatMessage m 
        WHERE m.type = 'file' 
        AND m.createdAt BETWEEN :startTime AND :endTime
        ORDER BY m.createdAt DESC
        """)
    List<ChatMessage> findSharedFiles(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
