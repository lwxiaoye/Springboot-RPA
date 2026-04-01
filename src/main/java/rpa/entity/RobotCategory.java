package rpa.entity;

import lombok.Data;
import javax.persistence.*;

/**
 * 机器人分类实体类
 */
@Data
@Entity
@Table(name = "robot_category")
public class RobotCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 分类名称 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 分类编码，唯一 */
    @Column(unique = true, nullable = false, length = 50)
    private String code;
}
