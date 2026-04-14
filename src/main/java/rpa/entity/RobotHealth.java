package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "robot_health")
public class RobotHealth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "robot_id", nullable = false)
    private Long robotId;

    @Column(name = "robot_name", length = 100)
    private String robotName;

    @Column(name = "check_time")
    private LocalDateTime checkTime;

    @Column(name = "cpu_usage")
    private Integer cpuUsage = 0;

    @Column(name = "memory_usage")
    private Integer memoryUsage = 0;

    @Column(name = "disk_usage")
    private Integer diskUsage = 0;

    @Column(name = "network_status", length = 20)
    private String networkStatus = "normal"; // normal, slow, disconnected

    @Column(name = "disk_io_wait")
    private Integer diskIoWait = 0;

    @Column(name = "process_count")
    private Integer processCount = 0;

    @Column(name = "open_file_count")
    private Integer openFileCount = 0;

    @Column(name = "network_latency")
    private Integer networkLatency = 0;

    @Column(name = "health_score")
    private Integer healthScore = 100;

    @Column(name = "health_status", length = 20)
    private String healthStatus = "healthy"; // healthy, warning, critical, offline

    @Column(name = "warning_items", columnDefinition = "TEXT")
    private String warningItems; // JSON数组

    @Column(name = "critical_items", columnDefinition = "TEXT")
    private String criticalItems; // JSON数组

    @Column(name = "heartbeat_timeout")
    private Boolean heartbeatTimeout = false;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
