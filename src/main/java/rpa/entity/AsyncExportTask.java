package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "async_export_task")
public class AsyncExportTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no", nullable = false, unique = true, length = 50)
    private String taskNo;

    @Column(name = "export_type", nullable = false, length = 30)
    private String exportType; // data_query, report, audit_log

    @Column(name = "export_name", nullable = false, length = 200)
    private String exportName;

    @Column(name = "query_params", columnDefinition = "TEXT")
    private String queryParams; // JSON

    @Column(name = "export_fields", columnDefinition = "TEXT")
    private String exportFields; // JSON数组

    @Column(name = "file_format", length = 10)
    private String fileFormat = "xlsx"; // xlsx, csv, pdf

    @Column(name = "total_records")
    private Integer totalRecords = 0;

    @Column(name = "exported_records")
    private Integer exportedRecords = 0;

    @Column(name = "status", length = 20)
    private String status = "pending"; // pending, processing, completed, failed

    @Column(name = "progress")
    private Integer progress = 0;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_md5", length = 64)
    private String fileMd5;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "download_expire_at")
    private LocalDateTime downloadExpireAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
