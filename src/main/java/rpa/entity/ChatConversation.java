package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话实体
 */
@Data
@Entity
@Table(name = "chat_conversation")
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话类型: private-单聊, group-群聊, temporary-临时群 */
    @Column(length = 20)
    private String type = "private";

    /** 群名称(群聊时) */
    @Column(length = 200)
    private String name;

    /** 群头像 */
    @Column(length = 500)
    private String avatar;

    /** 群主ID */
    private Long ownerId;

    /** 群描述 */
    @Column(length = 500)
    private String description;

    /** 是否系统会话 */
    private Integer isSystem = 0;

    /** 是否已归档(临时群) */
    private Integer isArchived = 0;

    /** 归档时间 */
    private LocalDateTime archivedAt;

    /** 成员数量 */
    private Integer memberCount = 0;

    /** 消息数量 */
    private Integer messageCount = 0;

    /** 最后消息时间 */
    private LocalDateTime lastMessageTime;

    /** 最后消息摘要 */
    @Column(length = 500)
    private String lastMessageContent;

    /** 创建人 */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 关联类型: task/robot/flow（用于临时群关联RPA资源） */
    @Column(length = 50)
    private String relatedType;

    /** 关联ID */
    private Long relatedId;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (type == null) type = "private";
        if (isSystem == null) isSystem = 0;
        if (isArchived == null) isArchived = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 非持久化字段
    @Transient
    private List<ChatParticipant> participants;

    @Transient
    private ChatMessage lastMessage;

    @Transient
    private Integer unreadCount;
}
