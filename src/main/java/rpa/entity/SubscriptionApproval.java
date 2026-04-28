package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 订阅审批记录实体
 * <p>
 * 存储报表订阅的审批记录，对应数据库中的 subscription_approval 表。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "subscription_approval")
public class SubscriptionApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订阅ID */
    private Long subscriptionId;

    /** 订阅名称 */
    @Column(length = 200)
    private String subscriptionName;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人姓名 */
    @Column(length = 100)
    private String applicantName;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    @Column(length = 100)
    private String approverName;

    /** 审批状态（pending-待审批，approved-已批准，rejected-已拒绝） */
    @Column(length = 20)
    private String status = "pending";

    /** 审批备注 */
    @Column(length = 500)
    private String remark;

    /** 申请原因 */
    @Column(length = 500)
    private String reason;

    /** 审批时间 */
    private LocalDateTime approvalTime;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();

    /** 订阅内容摘要 */
    @Column(length = 500)
    private String subscriptionSummary;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "pending";
        }
    }
}
