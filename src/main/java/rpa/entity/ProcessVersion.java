package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "process_version")
public class ProcessVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "version_type", length = 20)
    private String versionType = "release"; // release-正式版，gray-灰度版，test-测试版

    @Column(name = "gray_robot_ids", columnDefinition = "TEXT")
    private String grayRobotIds; // JSON数组

    @Column(name = "status", length = 20)
    private String status = "draft"; // draft, pending_approval, approved, published, deprecated

    @Column(name = "design_config", columnDefinition = "TEXT")
    private String designConfig; // JSON

    @Column(name = "release_notes", length = 1000)
    private String releaseNotes;

    @Column(name = "approval_flow_id")
    private Long approvalFlowId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by", length = 100)
    private String publishedBy;

    @Column(name = "rollback_from_version", length = 20)
    private String rollbackFromVersion;

    @Column(name = "compliance_check_result", length = 20)
    private String complianceCheckResult = "pass";

    @Column(name = "compliance_check_details", columnDefinition = "TEXT")
    private String complianceCheckDetails;

    @Column(name = "creator", length = 100)
    private String creator;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
