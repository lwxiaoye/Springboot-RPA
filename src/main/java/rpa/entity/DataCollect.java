package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_collect")
public class DataCollect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sourceUrl;

    private String sourceType;

    @Column(columnDefinition = "TEXT")
    private String selectorRules;

    @Column(columnDefinition = "TEXT")
    private String headers;

    @Column(columnDefinition = "TEXT")
    private String cookies;

    private String cronExpression;

    private Integer status = 0;

    private Long lastCollectTime;

    private Integer dataCount = 0;

    private Long creatorId;
    private String creatorName;

    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime = LocalDateTime.now();
}
