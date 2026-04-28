package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rpa.entity.AiChatConversation;

import java.util.List;

/**
 * AI聊天记录Repository
 */
@Repository
public interface AiChatConversationRepository extends JpaRepository<AiChatConversation, Long> {

    /**
     * 获取用户的AI聊天记录
     */
    List<AiChatConversation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 获取指定会话的聊天记录
     */
    List<AiChatConversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 获取最近的聊天记录
     */
    List<AiChatConversation> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
