package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * <p>
 * 用于存储系统全局配置，包括：
 * <ul>
 *   <li>通用设置（系统名称、会话超时等）</li>
 *   <li>消息服务（SMTP、Webhook等）</li>
 *   <li>存储配置（本地/NFS/S3）</li>
 *   <li>OCR服务配置</li>
 *   <li>AI大模型配置</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "system_config")
public class SystemConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置分类（general/message/storage/ocr/llm/license） */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** 配置键名 */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /** 配置值 */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /** 配置描述 */
    @Column(length = 200)
    private String description;

    /** 是否加密存储（用于密码、密钥等） */
    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();
}
