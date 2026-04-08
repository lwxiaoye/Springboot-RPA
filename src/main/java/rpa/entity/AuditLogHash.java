package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_log_hash")
public class AuditLogHash {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_log_id", nullable = false)
    private Long auditLogId;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(name = "current_hash", nullable = false, length = 64)
    private String currentHash;

    @Column(name = "hash_algorithm", length = 20)
    private String hashAlgorithm = "SHA-256";

    @Column(length = 500)
    private String signature;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "chain_id", length = 50)
    private String chainId;

    @Column(name = "verify_status", length = 20)
    private String verifyStatus = "verified";

    @Column(name = "verify_time")
    private LocalDateTime verifyTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
