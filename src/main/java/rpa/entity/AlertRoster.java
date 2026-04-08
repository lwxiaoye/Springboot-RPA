package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alert_roster")
public class AlertRoster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "alert_rule_ids", columnDefinition = "TEXT")
    private String alertRuleIds; // JSON数组

    @Column(name = "severity_levels", columnDefinition = "TEXT")
    private String severityLevels; // JSON数组

    @Column(name = "schedule_config", columnDefinition = "TEXT", nullable = false)
    private String scheduleConfig; // JSON

    @Column(name = "roster_members", columnDefinition = "TEXT", nullable = false)
    private String rosterMembers; // JSON数组

    @Column(name = "escalation_config", columnDefinition = "TEXT")
    private String escalationConfig; // JSON

    @Column
    private Boolean enabled = true;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "creator", length = 100)
    private String creator;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
