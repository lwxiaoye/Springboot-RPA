package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_query")
public class DataQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String sourceTable;

    @Column(columnDefinition = "TEXT")
    private String queryCondition;

    @Column(columnDefinition = "TEXT")
    private String queryColumns;

    @Column(columnDefinition = "TEXT")
    private String resultData;

    private Integer resultCount = 0;

    private Integer status = 0;

    private Long lastQueryTime;

    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime = LocalDateTime.now();
}
