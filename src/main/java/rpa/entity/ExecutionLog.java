package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "execution_log")
public class ExecutionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;
    private Long processId;
    private Long robotId;

    private String taskName;
    private String robotName;

    private String action;
    private String status;

    @Column(length = 2000)
    private String message;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String duration;

    @Column(columnDefinition = "TEXT")
    private String steps;

    private LocalDateTime createTime = LocalDateTime.now();
}
