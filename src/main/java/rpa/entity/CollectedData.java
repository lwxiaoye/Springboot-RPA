package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "collected_data")
public class CollectedData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long collectId;
    private String collectName;

    @Column(columnDefinition = "TEXT")
    private String rawData;

    @Column(columnDefinition = "TEXT")
    private String parsedData;

    private String dataType;

    private String sourceUrl;

    private Integer parseStatus = 0;

    private LocalDateTime collectTime = LocalDateTime.now();
    private LocalDateTime parseTime;
}
