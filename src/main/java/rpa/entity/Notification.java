package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知实体类
 * <p>
 * 系统通知实体，对应数据库中的notification表。
 * 用于向用户推送采集、加工等任务的结果通知。
 * </p>
 * <p>
 * 通知类型：
 * <ul>
 *   <li>collect - 采集任务通知</li>
 *   <li>temp - 临时通知</li>
 *   <li>user - 用户操作通知</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 通知类型（collect-采集通知，temp-临时通知，user-用户操作通知） */
    @Column(nullable = false, length = 20)
    private String type;

    /** 通知标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 通知内容 */
    @Column(length = 2000)
    private String content;

    /** 状态（unread-未读，read-已读） */
    @Column(nullable = false, length = 20)
    private String status = "unread";

    /** 关联业务ID（如采集任务ID） */
    private Long relatedId;

    /** 关联业务类型（如 collect, process, task, robot） */
    private String relatedType;

    /** 创建者ID */
    private Long creatorId;
    
    /** 创建者名称 */
    private String creatorName;

    /** 接收者ID（留空表示全员通知） */
    private Long receiverId;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();
    
    /** 阅读时间 */
    private LocalDateTime readTime;
}
