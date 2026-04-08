package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "robot_backup")
public class RobotBackup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "primary_robot_id", nullable = false)
    private Long primaryRobotId;

    @Column(name = "primary_robot_name", length = 100)
    private String primaryRobotName;

    @Column(name = "backup_robot_id", nullable = false)
    private Long backupRobotId;

    @Column(name = "backup_robot_name", length = 100)
    private String backupRobotName;

    @Column(name = "relation_type", length = 20)
    private String relationType = "hot"; // hot-热备, cold-冷备

    @Column(name = "bound_process_id")
    private Long boundProcessId;

    @Column(name = "bound_process_name", length = 200)
    private String boundProcessName;

    @Column(name = "auto_switch")
    private Boolean autoSwitch = true;

    @Column(name = "switch_condition", length = 100)
    private String switchCondition = "heartbeat_timeout";

    @Column(name = "switch_threshold")
    private Integer switchThreshold = 60;

    @Column(name = "status", length = 20)
    private String status = "active"; // active, inactive, switched

    @Column(name = "last_switch_time")
    private LocalDateTime lastSwitchTime;

    @Column(name = "switch_reason", length = 200)
    private String switchReason;

    @Column(name = "switch_count")
    private Integer switchCount = 0;

    @Column(name = "creator", length = 100)
    private String creator;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
