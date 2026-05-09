package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 公告阅读记录实体类
 * <p>
 * 记录用户对公告的阅读状态，用于统计公告的阅读情况。
 * 一个用户对一条公告只有一条阅读记录。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 */
@Data
@Entity
@Table(name = "announcement_read_record")
public class AnnouncementReadRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 公告ID */
    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 用户名 */
    @Column(name = "user_name", nullable = false)
    private String userName;

    /** 用户部门 */
    @Column(name = "department")
    private String department;

    /** 是否已读 */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /** 阅读时间 */
    @Column(name = "read_time")
    private LocalDateTime readTime;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
