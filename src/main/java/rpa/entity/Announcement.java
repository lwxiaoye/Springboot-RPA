package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统公告实体类
 *
 * @author RPA System
 * @version 1.0.0
 */
@Data
@Entity
@Table(name = "announcement")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 公告标题 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 公告内容（支持HTML） */
    @Column(name = "content", nullable = false, length = 5000)
    private String content;

    /** 优先级：normal-普通，important-重要，urgent-紧急 */
    @Column(name = "priority", nullable = false, length = 20)
    private String priority = "normal";

    /** 状态：draft-草稿，published-已发布，expired-已过期 */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "published";

    /** 发布者ID */
    @Column(name = "publisher_id")
    private Long publisherId;

    /** 发布者名称 */
    @Column(name = "publisher_name", length = 100)
    private String publisherName;

    /** 发布者部门 */
    @Column(name = "department", length = 100)
    private String department;

    /** 发布时间 */
    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    /** 过期时间 */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /** 阅读次数 */
    @Column(name = "read_count")
    private Integer readCount = 0;

    /** 发布范围：all-全平台，enterprise-本企业，specific-指定用户 */
    @Column(name = "scope", length = 20)
    private String scope = "all";

    /** 目标部门（逗号分隔，如：人事部,运维部,开发部） */
    @Column(name = "target_departments", length = 500)
    private String targetDepartments;

    /** 指定用户ID列表（逗号分隔） */
    @Column(name = "target_users", length = 1000)
    private String targetUsers;

    /** 附件JSON字符串 */
    @Column(name = "attachments", columnDefinition = "text")
    private String attachments;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    /** 更新时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();
}
