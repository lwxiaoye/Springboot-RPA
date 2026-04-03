package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发票数据实体类
 * <p>
 * 存储解析后的发票明细数据，对应数据库中的invoice_data表。
 * 用于存储从网页采集并解析的企业发票数据。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "invoice_data")
public class InvoiceData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 纳税人识别号 */
    @Column(name = "tax_no", length = 50)
    private String taxNo;

    /** 统一社会信用代码 */
    @Column(name = "credit_code", length = 50)
    private String creditCode;

    /** 企业名称 */
    @Column(name = "company_name", length = 200)
    private String companyName;

    /** 企业类型 */
    @Column(name = "company_type", length = 50)
    private String companyType;

    /** 申请日期 */
    @Column(name = "apply_date", length = 20)
    private String applyDate;

    /** 发票序号 */
    @Column(name = "invoice_index")
    private Integer invoiceIndex;

    /** 发票类型（销项/进项） */
    @Column(name = "invoice_type", length = 20)
    private String invoiceType;

    /** 发票状态（正常/作废/红冲） */
    @Column(name = "invoice_status", length = 20)
    private String invoiceStatus;

    /** 发票日期 */
    @Column(name = "invoice_date", length = 20)
    private String invoiceDate;

    /** 发票号码 */
    @Column(name = "invoice_no", length = 50)
    private String invoiceNo;

    /** 不含税金额 */
    @Column(name = "tax_exclusive_amount", precision = 15, scale = 2)
    private BigDecimal taxExclusiveAmount;

    /** 税额 */
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount;

    /** 价税合计 */
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /** 关联的采集任务ID */
    @Column(name = "collect_id")
    private Long collectId;

    /** 关联的采集任务名称 */
    @Column(name = "collect_name", length = 100)
    private String collectName;

    /** 数据来源URL */
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    /** 采集时间 */
    @Column(name = "collect_time")
    private LocalDateTime collectTime = LocalDateTime.now();

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
