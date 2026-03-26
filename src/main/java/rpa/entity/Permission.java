package rpa.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_permission")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    private String type = "menu"; // menu, button, api

    private String url;

    private String method; // GET, POST, PUT, DELETE

    private String icon;

    private Integer sort = 0;

    private Long parentId = 0L;

    private Integer status = 1;

    private LocalDateTime createTime = LocalDateTime.now();

    private LocalDateTime updateTime = LocalDateTime.now();

    @Transient
    private List<Permission> children = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
