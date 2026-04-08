package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task_dependency")
public class TaskDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "dag_config", columnDefinition = "TEXT", nullable = false)
    private String dagConfig; // JSON，包含节点和边

    @Column(name = "status", length = 20)
    private String status = "active"; // active, paused, completed, failed

    @Column(name = "root_task_id")
    private Long rootTaskId;

    @Column(name = "parent_task_id")
    private Long parentTaskId;

    @Column(name = "current_execution_id")
    private Long currentExecutionId;

    @Column(name = "execution_status", length = 20)
    private String executionStatus;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_duration", length = 50)
    private String totalDuration;

    @Column(name = "success_nodes")
    private Integer successNodes = 0;

    @Column(name = "failed_nodes")
    private Integer failedNodes = 0;

    @Column(name = "total_nodes")
    private Integer totalNodes = 0;

    @Column(name = "creator", length = 100)
    private String creator;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
