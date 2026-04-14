package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dead_letter_queue")
public class DeadLetterQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_queue_id")
    private Long originalQueueId;

    @Column(name = "original_queue_name", length = 200)
    private String originalQueueName;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "task_name", length = 200)
    private String taskName;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "process_name", length = 200)
    private String processName;

    @Column(name = "robot_id")
    private Long robotId;

    @Column(name = "robot_name", length = 100)
    private String robotName;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack", columnDefinition = "TEXT")
    private String errorStack;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retry")
    private Integer maxRetry = 3;

    @Column(name = "last_retry_time")
    private LocalDateTime lastRetryTime;

    @Column(name = "original_params", columnDefinition = "TEXT")
    private String originalParams;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "status", length = 20)
    private String status = "pending"; // pending, analysing, resolved, manually_closed

    @Column(name = "resolution_type", length = 30)
    private String resolutionType; // retry, skip, manual_fix

    @Column(name = "resolution_comment", columnDefinition = "TEXT")
    private String resolutionComment;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
