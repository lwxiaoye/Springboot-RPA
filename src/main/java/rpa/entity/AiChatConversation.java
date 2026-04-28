package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI聊天记录实体
 */
@Data
@Entity
@Table(name = "chat_ai_conversation")
public class AiChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户消息 */
    @Column(columnDefinition = "TEXT")
    private String userMessage;

    /** AI回复 */
    @Column(columnDefinition = "TEXT")
    private String aiResponse;

    /** 识别的意图 */
    @Column(length = 100)
    private String intent;

    /** 识别的实体 */
    @Column(columnDefinition = "JSON")
    private String entities;

    /** 建议的操作 */
    @Column(columnDefinition = "JSON")
    private String suggestions;

    /** 会话ID */
    @Column(length = 100)
    private String sessionId;

    /** 创建时间 */
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
