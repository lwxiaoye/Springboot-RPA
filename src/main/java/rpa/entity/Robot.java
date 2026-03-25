package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "robot")
public class Robot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String type;
    private String status = "idle";

    @Column(length = 1000)
    private String capabilities;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "hostname", length = 100)
    private String hostname;

    private Integer port = 8080;

    @Column(name = "cpu_usage")
    private Integer cpuUsage = 0;

    @Column(name = "memory_usage")
    private Integer memoryUsage = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime lastHeartbeat = LocalDateTime.now();
    private LocalDateTime createTime = LocalDateTime.now();
}
