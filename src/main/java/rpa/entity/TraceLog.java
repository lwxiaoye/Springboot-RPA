package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trace_log")
public class TraceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "span_id", length = 32)
    private String spanId;

    @Column(name = "parent_span_id", length = 32)
    private String parentSpanId;

    @Column(name = "operation_name", nullable = false, length = 200)
    private String operationName;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "module", length = 50)
    private String module; // trigger, scheduler, robot, process

    @Column(name = "status", length = 20)
    private String status = "success"; // success, failed, running

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON

    @Column(name = "logs", columnDefinition = "TEXT")
    private String logs; // JSON数组

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
