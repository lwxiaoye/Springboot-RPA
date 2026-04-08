package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notification_channel")
public class NotificationChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_type", nullable = false, length = 30)
    private String channelType; // wechat, dingtalk, feishu, email, sms, webhook

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "config", columnDefinition = "TEXT", nullable = false)
    private String config; // JSON

    @Column
    private Boolean enabled = true;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column
    private Integer priority = 100;

    @Column(name = "rate_limit")
    private Integer rateLimit = 100;

    @Column(name = "success_rate")
    private Double successRate = 100.0;

    @Column(name = "last_test_at")
    private LocalDateTime lastTestAt;

    @Column(name = "last_test_result", length = 20)
    private String lastTestResult;

    @Column(name = "creator", length = 100)
    private String creator;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
