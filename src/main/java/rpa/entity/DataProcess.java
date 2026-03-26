package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_process")
public class DataProcess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String sourceIds;

    private String processType;

    @Column(columnDefinition = "TEXT")
    private String processRules;

    private String outputTable;

    private Integer status = 0;

    private Integer processedCount = 0;

    private Long lastProcessTime;

    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime = LocalDateTime.now();
}
