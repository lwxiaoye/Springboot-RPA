package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天消息实体
 */
@Data
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话ID */
    private Long conversationId;

    /** 发送人ID */
    private Long senderId;

    /** 发送人姓名 */
    @Column(length = 100)
    private String senderName;

    /** 发送人头像 */
    @Column(length = 500)
    private String senderAvatar;

    /** 消息类型: text/image/file/link/rpa_card/notice */
    @Column(length = 30)
    private String type = "text";

    /** 消息内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 卡片类型: TASK/ROBOT/FLOW/LOG/APPROVAL/ALERT */
    @Column(length = 50)
    private String cardType;

    /** 卡片数据(JSON) */
    @Column(columnDefinition = "JSON")
    private String cardData;

    /** 是否已撤回 */
    private Integer isRecalled = 0;

    /** 撤回时间 */
    private LocalDateTime recalledAt;

    /** 撤回人 */
    private Long recalledBy;

    /** 是否置顶(群公告) */
    private Integer isPinned = 0;

    /** 回复的消息ID */
    private Long replyToId;

    /** @提及的用户ID列表 */
    @Column(length = 500)
    private String mentionedUsers;

    /** 创建时间 */
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // 非持久化字段
    @Transient
    private Boolean isEdited = false;

    @Transient
    private ChatMessage replyToMessage;

    @Transient
    private Integer likeCount = 0;

    @Transient
    private Map<String, Object> parsedCardData;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (type == null) type = "text";
        if (isRecalled == null) isRecalled = 0;
        if (isPinned == null) isPinned = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
