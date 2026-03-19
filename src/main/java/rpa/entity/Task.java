package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;
    private String priority = "normal";
    
    private Long processId;
    private String processName;
    
    private Long robotId;
    private String robotName;
    
    private Long assigneeId;
    private String assigneeName;
    
    private String status = "pending";
    
    @Column(length = 3000)
    private String inputData;
    
    @Column(length = 5000)
    private String resultData;
    
    private String errorMessage;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime = LocalDateTime.now();
}
