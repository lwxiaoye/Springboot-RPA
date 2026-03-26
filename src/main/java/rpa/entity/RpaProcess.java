package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rpa_process")
public class RpaProcess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String code;

    @Column(length = 2000)
    private String description;

    private String status = "active";

    @Column(length = 5000)
    private String steps;

    private Long creatorId;
    private String creatorName;

    private String version = "1.0.0";

    private Integer taskCount = 0;

    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime = LocalDateTime.now();
}
