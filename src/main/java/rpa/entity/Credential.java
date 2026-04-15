package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 凭据实体类
 * <p>
 * 用于存储系统中需要使用的敏感凭据信息。
 * 凭据值在存储时加密，使用时解密。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 */
@Data
@Entity
@Table(name = "credential")
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 凭据名称（唯一标识） */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** 凭据类型 */
    @Column(length = 50)
    private String type;

    /** 用户名/账号 */
    @Column(length = 100)
    private String username;

    /** 加密后的凭据值 */
    @Column(columnDefinition = "TEXT")
    private String secretValue;

    /** 额外密钥（加密） */
    @Column(columnDefinition = "TEXT")
    private String secretKey;

    /** 关联URL */
    @Column(length = 500)
    private String url;

    /** 描述 */
    @Column(length = 500)
    private String description;

    /** 标签（JSON格式） */
    @Column(length = 500)
    private String tags;

    /** 创建人ID */
    private Long createBy;

    /** 创建人名称 */
    @Column(length = 100)
    private String createByName;

    /** 最后使用时间 */
    private LocalDateTime lastUsedTime;

    /** 使用次数 */
    private Integer useCount = 0;

    /** 状态（active-激活，inactive-停用） */
    @Column(length = 20)
    private String status = "active";

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();
}