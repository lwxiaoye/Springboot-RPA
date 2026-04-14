package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "approval_flow")
public class ApprovalFlow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "flow_type", nullable = false, length = 30)
    private String flowType; // process_publish, robot_register, data_export

    @Column(name = "approval_stages", columnDefinition = "TEXT", nullable = false)
    private String approvalStages; // JSON数组

    @Column(name = "current_stage")
    private Integer currentStage = 1;

    @Column(name = "total_stages")
    private Integer totalStages = 1;

    @Column(name = "status", length = 20)
    private String status = "pending"; // pending, approved, rejected, cancelled

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Column(name = "current_approver_id")
    private Long currentApproverId;

    @Column(name = "current_approver_name", length = 100)
    private String currentApproverName;

    @Column(name = "apply_user_id")
    private Long applyUserId;

    @Column(name = "apply_user_name", length = 100)
    private String applyUserName;

    @Column(name = "apply_reason", length = 500)
    private String applyReason;

    @Column(name = "approval_comments", columnDefinition = "TEXT")
    private String approvalComments; // JSON数组

    @Column(name = "result_comments", columnDefinition = "TEXT")
    private String resultComments;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
