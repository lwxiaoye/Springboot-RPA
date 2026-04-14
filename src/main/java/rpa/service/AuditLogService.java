package rpa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.AuditLog;
import rpa.entity.AuditLogHash;
import rpa.repository.AuditLogRepository;
import rpa.repository.AuditLogHashRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogHashRepository auditLogHashRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository,
                          AuditLogHashRepository auditLogHashRepository,
                          ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogHashRepository = auditLogHashRepository;
        this.objectMapper = objectMapper;
    }

    public AuditLog log(AuditLog auditLog) {
        auditLog.setCreateTime(LocalDateTime.now());

        String previousHash = auditLogRepository.findTopByOrderByIdDesc()
            .map(AuditLog::getHash)
            .orElse("0");

        String currentHash = calculateHash(auditLog, previousHash);
        auditLog.setHash(currentHash);

        AuditLog saved = auditLogRepository.save(auditLog);

        AuditLogHash hashRecord = new AuditLogHash();
        hashRecord.setAuditLogId(saved.getId());
        hashRecord.setPreviousHash(previousHash);
        hashRecord.setCurrentHash(currentHash);
        hashRecord.setHashAlgorithm("SHA-256");
        hashRecord.setTimestamp(LocalDateTime.now());
        auditLogHashRepository.save(hashRecord);

        log.info("审计日志记录: {} - {}", auditLog.getUserName(), auditLog.getDescription());
        return saved;
    }

    public void logQueue(Long queueId, String userName, Long userId, String action, String queueName) {
        AuditLog auditLog = new AuditLog();
        auditLog.setModule("queue");
        auditLog.setAction(action);
        auditLog.setTargetType("task_queue");
        auditLog.setTargetId(queueId);
        auditLog.setTargetName(queueName);
        auditLog.setDescription(String.format("队列%s: %s", action, queueName));
        auditLog.setUserName(userName);
        auditLog.setUserId(userId);
        auditLog.setStatus("success");
        log(auditLog);
    }

    public void logTrigger(Long triggerId, String userName, Long userId, String action, String triggerName) {
        AuditLog auditLog = new AuditLog();
        auditLog.setModule("trigger");
        auditLog.setAction(action);
        auditLog.setTargetType("trigger_rule");
        auditLog.setTargetId(triggerId);
        auditLog.setTargetName(triggerName);
        auditLog.setDescription(String.format("触发器%s: %s", action, triggerName));
        auditLog.setUserName(userName);
        auditLog.setUserId(userId);
        auditLog.setStatus("success");
        log(auditLog);
    }

    public void logRobot(Long robotId, String userName, Long userId, String action, String robotName) {
        AuditLog auditLog = new AuditLog();
        auditLog.setModule("robot");
        auditLog.setAction(action);
        auditLog.setTargetType("robot");
        auditLog.setTargetId(robotId);
        auditLog.setTargetName(robotName);
        auditLog.setDescription(String.format("机器人%s: %s", action, robotName));
        auditLog.setUserName(userName);
        auditLog.setUserId(userId);
        auditLog.setStatus("success");
        log(auditLog);
    }

    private String calculateHash(AuditLog auditLog, String previousHash) {
        try {
            String data = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                auditLog.getId(),
                auditLog.getModule(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getUserId(),
                auditLog.getUserName(),
                auditLog.getCreateTime()
            );
            String content = previousHash + "|" + data;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(content.getBytes("UTF-8"));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            log.error("计算哈希值失败", e);
            return UUID.randomUUID().toString();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public boolean verifyIntegrity(Long auditLogId) {
        AuditLog auditLog = auditLogRepository.findById(auditLogId).orElse(null);
        if (auditLog == null) {
            return false;
        }

        AuditLogHash hashRecord = auditLogHashRepository.findByAuditLogId(auditLogId).orElse(null);
        if (hashRecord == null) {
            return false;
        }

        Optional<AuditLog> prevLog = auditLogHashRepository
            .findTopByAuditLogIdLessThanOrderByAuditLogIdDesc(auditLogId)
            .flatMap(h -> auditLogRepository.findById(h.getAuditLogId()));

        String previousHash = prevLog.map(AuditLog::getHash).orElse("0");
        String calculatedHash = calculateHash(auditLog, previousHash);

        return calculatedHash.equals(hashRecord.getCurrentHash());
    }

    public List<AuditLog> findByUserId(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByUserIdAndCreateTimeBetween(userId, startTime, endTime);
    }

    public List<AuditLog> findAll() {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> findByModule(String module) {
        return auditLogRepository.findByModule(module);
    }

    public List<AuditLog> findByRiskLevel(String riskLevel) {
        return auditLogRepository.findByRiskLevel(riskLevel);
    }

    public Map<String, Object> getAuditStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<AuditLog> logs = auditLogRepository.findByCreateTimeBetween(since, LocalDateTime.now());

        stats.put("totalCount", logs.size());
        stats.put("highRiskCount", logs.stream().filter(l -> "high".equals(l.getRiskLevel())).count());
        stats.put("byModule", logs.stream().collect(Collectors.groupingBy(AuditLog::getModule, Collectors.counting())));
        stats.put("byAction", logs.stream().collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting())));

        return stats;
    }
}
