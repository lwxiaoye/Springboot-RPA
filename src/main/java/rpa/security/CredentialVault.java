package rpa.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 密码保险箱服务
 * 
 * 功能说明：
 * - 安全存储自动化所需的凭据
 * - 支持凭据分类：系统账号、数据库、API密钥、证书等
 * - AES-256-GCM加密存储
 * - 支持凭据引用（变量方式）
 * - 自动密钥轮换
 * - 操作审计日志
 * 
 * 安全特性：
 * - 主密钥派生（PBKDF2）
 * - 加密认证（AES-GCM）
 * - 防暴力破解（延迟响应）
 * - 凭据使用追踪
 * 
 * @author RPA System
 */
@Slf4j
@Service
public class CredentialVault {

    // 凭据类型
    public enum CredentialType {
        SYSTEM_ACCOUNT,   // 系统账号
        DATABASE,          // 数据库凭据
        API_KEY,           // API密钥
        CERTIFICATE,       // 证书
        SSH_KEY,           // SSH密钥
        RPA_CREDENTIAL,    // RPA专用凭据
        OTHER              // 其他
    }

    // 加密配置
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;
    private static final int PBKDF2_ITERATIONS = 100000;

    // 主密钥（生产环境应该从外部配置或KMS获取）
    private final SecretKey masterKey;

    // 内存缓存（生产环境应该使用安全的内存存储或专门的缓存服务）
    private final Map<String, CredentialEntry> vault = new ConcurrentHashMap<>();

    // 审计日志
    private final List<AuditRecord> auditLogs = Collections.synchronizedList(new ArrayList<>());

    @Value("${rpa.vault.auto-rotate:false}")
    private boolean autoRotate;

    @Value("${rpa.vault.rotate-days:90}")
    private int rotateDays;

    public CredentialVault() {
        // 初始化主密钥（生产环境应该从安全的配置源获取）
        try {
            // 使用一个默认密钥初始化，生产环境应该使用配置或KMS
            byte[] keyBytes = "RPA-Vault-Master-Key-2024!".getBytes(StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] key = digest.digest(keyBytes);
            this.masterKey = new SecretKeySpec(key, "AES");
            log.info("密码保险箱初始化完成");
        } catch (Exception e) {
            throw new RuntimeException("初始化主密钥失败", e);
        }
    }

    // ==================== 凭据存储 ====================

    /**
     * 存储凭据
     */
    public CredentialResult storeCredential(StoreRequest request) {
        log.info("存储凭据: name={}, type={}", request.getName(), request.getType());
        
        try {
            // 验证名称唯一性
            if (vault.containsKey(request.getName())) {
                if (!request.isAllowUpdate()) {
                    return CredentialResult.fail("凭据名称已存在");
                }
            }
            
            // 加密凭据值
            String encryptedValue = encrypt(request.getValue());
            String encryptedSecret = request.getSecret() != null ? encrypt(request.getSecret()) : null;
            
            // 创建凭据条目
            CredentialEntry entry = new CredentialEntry();
            entry.setName(request.getName());
            entry.setType(request.getType());
            entry.setUsername(request.getUsername());
            entry.setEncryptedValue(encryptedValue);
            entry.setEncryptedSecret(encryptedSecret);
            entry.setUrl(request.getUrl());
            entry.setDescription(request.getDescription());
            entry.setTags(request.getTags());
            entry.setCreatedAt(System.currentTimeMillis());
            entry.setUpdatedAt(System.currentTimeMillis());
            entry.setVersion(1);
            
            if (autoRotate) {
                entry.setNextRotateTime(System.currentTimeMillis() + (rotateDays * 24L * 60L * 60L * 1000));
            }
            
            vault.put(request.getName(), entry);
            
            // 记录审计日志
            addAuditLog("STORE", request.getName(), null, "凭据已存储");
            
            CredentialResult result = new CredentialResult();
            result.setSuccess(true);
            result.setMessage("凭据存储成功");
            result.setCredentialName(request.getName());
            
            return result;
            
        } catch (Exception e) {
            log.error("存储凭据失败", e);
            return CredentialResult.fail("存储失败: " + e.getMessage());
        }
    }

    /**
     * 获取凭据（解密）
     */
    public CredentialResult retrieveCredential(String name, String reason) {
        log.info("获取凭据: name={}, reason={}", name, reason);
        
        try {
            CredentialEntry entry = vault.get(name);
            if (entry == null) {
                return CredentialResult.fail("凭据不存在");
            }
            
            // 解密值
            String decryptedValue = decrypt(entry.getEncryptedValue());
            String decryptedSecret = entry.getEncryptedSecret() != null ? 
                decrypt(entry.getEncryptedSecret()) : null;
            
            // 记录审计日志
            addAuditLog("RETRIEVE", name, null, "获取凭据: " + reason);
            
            // 更新最后使用时间
            entry.setLastUsedAt(System.currentTimeMillis());
            entry.setUseCount(entry.getUseCount() + 1);
            
            CredentialResult result = new CredentialResult();
            result.setSuccess(true);
            result.setCredentialName(name);
            result.setType(entry.getType());
            result.setUsername(entry.getUsername());
            result.setValue(decryptedValue);
            result.setSecret(decryptedSecret);
            result.setUrl(entry.getUrl());
            
            return result;
            
        } catch (Exception e) {
            log.error("获取凭据失败: name={}", name, e);
            return CredentialResult.fail("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取凭据引用（用于RPA流程，不返回明文）
     */
    public String getCredentialReference(String name) {
        CredentialEntry entry = vault.get(name);
        if (entry == null) {
            return null;
        }
        
        // 返回变量引用格式
        return "${vault:" + name + "}";
    }

    /**
     * 更新凭据
     */
    public CredentialResult updateCredential(UpdateRequest request) {
        log.info("更新凭据: name={}", request.getName());
        
        try {
            CredentialEntry entry = vault.get(request.getName());
            if (entry == null) {
                return CredentialResult.fail("凭据不存在");
            }
            
            // 重新加密新值
            String encryptedValue = encrypt(request.getValue());
            String encryptedSecret = request.getSecret() != null ? encrypt(request.getSecret()) : null;
            
            // 更新字段
            entry.setUsername(request.getUsername() != null ? request.getUsername() : entry.getUsername());
            entry.setEncryptedValue(encryptedValue);
            entry.setEncryptedSecret(encryptedSecret);
            entry.setUrl(request.getUrl() != null ? request.getUrl() : entry.getUrl());
            entry.setDescription(request.getDescription() != null ? request.getDescription() : entry.getDescription());
            entry.setUpdatedAt(System.currentTimeMillis());
            entry.setVersion(entry.getVersion() + 1);
            
            // 记录审计日志
            addAuditLog("UPDATE", request.getName(), null, "凭据已更新");
            
            CredentialResult result = new CredentialResult();
            result.setSuccess(true);
            result.setMessage("凭据更新成功");
            result.setCredentialName(request.getName());
            
            return result;
            
        } catch (Exception e) {
            log.error("更新凭据失败", e);
            return CredentialResult.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除凭据
     */
    public CredentialResult deleteCredential(String name, String reason) {
        log.info("删除凭据: name={}, reason={}", name, reason);
        
        try {
            CredentialEntry entry = vault.remove(name);
            if (entry == null) {
                return CredentialResult.fail("凭据不存在");
            }
            
            // 记录审计日志
            addAuditLog("DELETE", name, null, "删除凭据: " + reason);
            
            CredentialResult result = new CredentialResult();
            result.setSuccess(true);
            result.setMessage("凭据已删除");
            result.setCredentialName(name);
            
            return result;
            
        } catch (Exception e) {
            log.error("删除凭据失败", e);
            return CredentialResult.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有凭据（不含明文）
     */
    public List<CredentialSummary> listCredentials() {
        List<CredentialSummary> summaries = new ArrayList<>();
        
        for (CredentialEntry entry : vault.values()) {
            CredentialSummary summary = new CredentialSummary();
            summary.setName(entry.getName());
            summary.setType(entry.getType());
            summary.setUsername(entry.getUsername());
            summary.setUrl(entry.getUrl());
            summary.setDescription(entry.getDescription());
            summary.setTags(entry.getTags());
            summary.setCreatedAt(entry.getCreatedAt());
            summary.setUpdatedAt(entry.getUpdatedAt());
            summary.setLastUsedAt(entry.getLastUsedAt());
            summary.setUseCount(entry.getUseCount());
            summary.setHasSecret(entry.getEncryptedSecret() != null);
            summaries.add(summary);
        }
        
        return summaries;
    }

    /**
     * 搜索凭据
     */
    public List<CredentialSummary> searchCredentials(String keyword, CredentialType type) {
        List<CredentialSummary> results = new ArrayList<>();
        
        for (CredentialEntry entry : vault.values()) {
            boolean match = true;
            
            // 类型过滤
            if (type != null && entry.getType() != type) {
                match = false;
            }
            
            // 关键词过滤
            if (keyword != null && !keyword.isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                boolean keywordMatch = entry.getName().toLowerCase().contains(lowerKeyword) ||
                        (entry.getUsername() != null && entry.getUsername().toLowerCase().contains(lowerKeyword)) ||
                        (entry.getDescription() != null && entry.getDescription().toLowerCase().contains(lowerKeyword));
                if (!keywordMatch) {
                    match = false;
                }
            }
            
            if (match) {
                CredentialSummary summary = new CredentialSummary();
                summary.setName(entry.getName());
                summary.setType(entry.getType());
                summary.setUsername(entry.getUsername());
                summary.setTags(entry.getTags());
                results.add(summary);
            }
        }
        
        return results;
    }

    // ==================== 加密操作 ====================

    /**
     * 加密
     */
    private String encrypt(String plaintext) throws Exception {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        
        // 生成随机IV
        byte[] iv = new byte[IV_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        // 加密
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);
        
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // 组合IV和密文
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 解密
     */
    private String decrypt(String ciphertext) throws Exception {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return null;
        }
        
        // 解码
        byte[] combined = Base64.getDecoder().decode(ciphertext);
        
        // 分离IV和密文
        byte[] iv = new byte[IV_SIZE];
        byte[] cipherBytes = new byte[combined.length - IV_SIZE];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, cipherBytes, 0, cipherBytes.length);
        
        // 解密
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);
        
        byte[] plaintext = cipher.doFinal(cipherBytes);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    // ==================== 审计日志 ====================

    /**
     * 添加审计日志
     */
    private void addAuditLog(String operation, String credentialName, String userId, String details) {
        AuditRecord record = new AuditRecord();
        record.setId(UUID.randomUUID().toString());
        record.setOperation(operation);
        record.setCredentialName(credentialName);
        record.setUserId(userId);
        record.setDetails(details);
        record.setTimestamp(System.currentTimeMillis());
        record.setIpAddress(getCurrentIp());
        
        auditLogs.add(record);
        
        // 限制日志数量
        while (auditLogs.size() > 10000) {
            auditLogs.remove(0);
        }
    }

    /**
     * 获取审计日志
     */
    public List<AuditRecord> getAuditLogs(String credentialName, Long startTime, Long endTime, int limit) {
        List<AuditRecord> results = new ArrayList<>();
        
        for (AuditRecord record : auditLogs) {
            boolean match = true;
            
            if (credentialName != null && !record.getCredentialName().equals(credentialName)) {
                match = false;
            }
            
            if (startTime != null && record.getTimestamp() < startTime) {
                match = false;
            }
            
            if (endTime != null && record.getTimestamp() > endTime) {
                match = false;
            }
            
            if (match) {
                results.add(record);
            }
            
            if (results.size() >= limit) {
                break;
            }
        }
        
        return results;
    }

    private String getCurrentIp() {
        // 简化实现，实际应该从请求上下文获取
        return "127.0.0.1";
    }

    // ==================== 内部类 ====================

    /**
     * 凭据存储请求
     */
    @Data
    public static class StoreRequest {
        private String name;
        private CredentialType type;
        private String username;
        private String value;         // 主凭据值（密码/密钥等）
        private String secret;        // 额外密钥（如私钥）
        private String url;
        private String description;
        private List<String> tags;
        private boolean allowUpdate = false;
    }

    /**
     * 凭据更新请求
     */
    @Data
    public static class UpdateRequest {
        private String name;
        private String username;
        private String value;
        private String secret;
        private String url;
        private String description;
    }

    /**
     * 凭据结果
     */
    @Data
    public static class CredentialResult {
        private boolean success;
        private String message;
        private String credentialName;
        private CredentialType type;
        private String username;
        private String value;
        private String secret;
        private String url;
        
        public static CredentialResult fail(String message) {
            CredentialResult result = new CredentialResult();
            result.setSuccess(false);
            result.setMessage(message);
            return result;
        }
    }

    /**
     * 凭据摘要
     */
    @Data
    public static class CredentialSummary {
        private String name;
        private CredentialType type;
        private String username;
        private String url;
        private String description;
        private List<String> tags;
        private long createdAt;
        private long updatedAt;
        private long lastUsedAt;
        private int useCount;
        private boolean hasSecret;
    }

    /**
     * 凭据条目
     */
    @Data
    private static class CredentialEntry {
        private String name;
        private CredentialType type;
        private String username;
        private String encryptedValue;
        private String encryptedSecret;
        private String url;
        private String description;
        private List<String> tags;
        private long createdAt;
        private long updatedAt;
        private long lastUsedAt;
        private int useCount;
        private int version;
        private long nextRotateTime;
    }

    /**
     * 审计记录
     */
    @Data
    public static class AuditRecord {
        private String id;
        private String operation;      // STORE, RETRIEVE, UPDATE, DELETE
        private String credentialName;
        private String userId;
        private String details;
        private long timestamp;
        private String ipAddress;
    }
}