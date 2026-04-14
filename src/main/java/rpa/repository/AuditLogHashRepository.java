package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rpa.entity.AuditLogHash;
import java.util.Optional;

@Repository
public interface AuditLogHashRepository extends JpaRepository<AuditLogHash, Long> {
    Optional<AuditLogHash> findByAuditLogId(Long auditLogId);
    Optional<AuditLogHash> findTopByAuditLogIdLessThanOrderByAuditLogIdDesc(Long auditLogId);
}
