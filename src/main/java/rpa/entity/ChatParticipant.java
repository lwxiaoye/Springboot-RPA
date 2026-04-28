package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 会话参与者实体
 */
@Data
@Entity
@Table(name = "chat_participant")
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话ID */
    private Long conversationId;

    /** 用户ID */
    private Long userId;

    /** 群昵称 */
    @Column(length = 100)
    private String nickname;

    /** 角色: owner-群主, admin-管理员, member-成员 */
    @Column(length = 20)
    private String role = "member";

    /** 是否被禁言 */
    private Integer isMuted = 0;

    /** 会话是否置顶 */
    private Integer isPinned = 0;

    /** 会话是否归档 */
    private Integer isArchived = 0;

    /** 最后已读时间 */
    private LocalDateTime lastReadTime;

    /** 未读消息数 */
    private Integer unreadCount = 0;

    /** 加入时间 */
    private LocalDateTime joinedAt = LocalDateTime.now();

    /** 离开时间 */
    private LocalDateTime leftAt;

    // 非持久化字段
    @Transient
    private String userName;

    @Transient
    private String userAvatar;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
        if (role == null) role = "member";
        if (isMuted == null) isMuted = 0;
        if (isPinned == null) isPinned = 0;
        if (isArchived == null) isArchived = 0;
        if (unreadCount == null) unreadCount = 0;
    }
}
