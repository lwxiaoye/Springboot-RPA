package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.ChatConversation;
import rpa.entity.ChatMessage;

import java.util.List;
import java.util.Optional;

/**
 * 聊天会话Repository
 */
@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    /**
     * 获取用户参与的所有会话（按最后消息时间排序）
     */
    @Query(value = """
        SELECT c.* FROM chat_conversation c
        INNER JOIN chat_participant p ON c.id = p.conversation_id
        WHERE p.user_id = :userId AND p.is_archived = 0
        ORDER BY c.last_message_time DESC
        """, nativeQuery = true)
    List<ChatConversation> findUserConversations(@Param("userId") Long userId);

    /**
     * 获取用户参与的所有会话（分页）
     */
    @Query(value = """
        SELECT c.* FROM chat_conversation c
        INNER JOIN chat_participant p ON c.id = p.conversation_id
        WHERE p.user_id = :userId AND p.is_archived = 0
        ORDER BY c.last_message_time DESC
        """, countQuery = """
        SELECT COUNT(*) FROM chat_conversation c
        INNER JOIN chat_participant p ON c.id = p.conversation_id
        WHERE p.user_id = :userId AND p.is_archived = 0
        """, nativeQuery = true)
    Page<ChatConversation> findUserConversations(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取用户的单聊会话（与指定用户）
     */
    @Query(value = """
        SELECT c.* FROM chat_conversation c
        INNER JOIN chat_participant p1 ON c.id = p1.conversation_id AND p1.user_id = :userId
        INNER JOIN chat_participant p2 ON c.id = p2.conversation_id AND p2.user_id = :otherUserId
        WHERE c.type = 'private'
        LIMIT 1
        """, nativeQuery = true)
    Optional<ChatConversation> findPrivateConversation(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    /**
     * 搜索会话
     */
    @Query("""
        SELECT c FROM ChatConversation c
        WHERE c.name LIKE %:keyword% OR c.description LIKE %:keyword%
        ORDER BY c.lastMessageTime DESC
        """)
    List<ChatConversation> searchConversations(@Param("keyword") String keyword);

    /**
     * 获取群聊会话
     */
    List<ChatConversation> findByTypeAndIsArchivedOrderByLastMessageTimeDesc(String type, Integer isArchived);

    /**
     * 获取未归档的群聊
     */
    @Query("SELECT c FROM ChatConversation c WHERE c.type = 'group' AND c.isArchived = 0 ORDER BY c.lastMessageTime DESC")
    List<ChatConversation> findActiveGroups();

    /**
     * 更新会话最后消息
     */
    @Query("""
        UPDATE ChatConversation c SET 
        c.lastMessageTime = :lastMessageTime,
        c.lastMessageContent = :lastMessageContent,
        c.messageCount = c.messageCount + 1
        WHERE c.id = :conversationId
        """)
    void updateLastMessage(@Param("conversationId") Long conversationId,
                           @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime,
                           @Param("lastMessageContent") String lastMessageContent);
}
