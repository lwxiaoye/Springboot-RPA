package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_parse")
public class DataParse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String collectId;

    private String parseType;

    @Column(columnDefinition = "TEXT")
    private String parseRules;

    private String outputFormat;

    private Integer status = 0;

    private Integer successCount = 0;
    private Integer failCount = 0;

    private Long lastParseTime;

    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime = LocalDateTime.now();
}
