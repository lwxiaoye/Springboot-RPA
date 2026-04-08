package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_lineage")
public class DataLineage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lineage_type", nullable = false, length = 30)
    private String lineageType; // source, process, storage, output

    @Column(name = "source_type", length = 50)
    private String sourceType; // execution_log, invoice_data, custom

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "process_name", length = 200)
    private String processName;

    @Column(name = "process_version", length = 20)
    private String processVersion;

    @Column(name = "robot_id")
    private Long robotId;

    @Column(name = "robot_name", length = 100)
    private String robotName;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "data_table", length = 100)
    private String dataTable;

    @Column(name = "data_field", length = 100)
    private String dataField;

    @Column(name = "data_record_id", length = 100)
    private String dataRecordId;

    @Column(name = "transformation_rule", length = 500)
    private String transformationRule;

    @Column(name = "parent_lineage_id")
    private Long parentLineageId;

    @Column(name = "child_lineage_ids", columnDefinition = "TEXT")
    private String childLineageIds; // JSON数组

    @Column(name = "business_key", length = 200)
    private String businessKey;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
