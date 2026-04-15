package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * <p>
 * 系统用户实体，对应数据库中的users表。
 * 存储用户的登录信息、角色、状态和个人资料。
 * </p>
 * <p>
 * 字段说明：
 * <ul>
 *   <li>username - 用户名，唯一且必填</li>
 *   <li>password - 密码，已加密存储</li>
 *   <li>realName - 真实姓名</li>
 *   <li>email - 邮箱地址</li>
 *   <li>phone - 手机号码</li>
 *   <li>role - 角色（0-普通用户，1-管理员）</li>
 *   <li>status - 状态（1-启用，0-禁用）</li>
 *   <li>avatar - 头像URL</li>
 *   <li>passwordChangeTime - 密码最后修改时间</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名，唯一且必填 */
    @Column(unique = true, nullable = false)
    private String username;

    /** 密码（已BCrypt加密） */
    @Column(nullable = false)
    private String password;

    /** 真实姓名 */
    private String realName;
    
    /** 邮箱地址 */
    private String email;
    
    /** 手机号码 */
    private String phone;
    
    /** 角色（0-普通用户，1-管理员） */
    @Column(nullable = false)
    private Integer role = 0;

    /** 状态（1-启用，0-禁用） */
    private Integer status = 1;
    
    /** 头像URL */
    private String avatar;
    
    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();
    
    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();

    /** 密码最后修改时间 */
    private LocalDateTime passwordChangeTime;

    /** 更新时自动设置更新时间 */
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}