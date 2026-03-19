package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "robot")
public class Robot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String type;
    private String status = "idle";
    
    @Column(length = 1000)
    private String capabilities;

    private LocalDateTime lastHeartbeat = LocalDateTime.now();
    private LocalDateTime createTime = LocalDateTime.now();
}
