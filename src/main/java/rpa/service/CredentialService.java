package rpa.service;

import rpa.entity.Credential;
import rpa.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 凭据服务
 * <p>
 * 提供凭据的CRUD操作、加密解密、与任务调度的集成。
 * </p>
 */
@Slf4j
@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final AuditLogService auditLogService;

    // 加密配置
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;

    // 主密钥
    private final SecretKeySpec masterKey;

    public CredentialService(CredentialRepository credentialRepository, AuditLogService auditLogService) {
        this.credentialRepository = credentialRepository;
        this.auditLogService = auditLogService;

        // 初始化主密钥
        try {
            String keyString = "RPA-Credential-Master-Key-2024-Secure!";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] key = digest.digest(keyString.getBytes(StandardCharsets.UTF_8));
            this.masterKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new RuntimeException("初始化密钥失败", e);
        }
    }

    /**
     * 创建凭据
     */
    @Transactional
    public Credential createCredential(Credential credential) {
        // 检查名称唯一性
        if (credentialRepository.existsByName(credential.getName())) {
            throw new RuntimeException("凭据名称已存在: " + credential.getName());
        }

        // 加密凭据值
        if (credential.getSecretValue() != null) {
            credential.setSecretValue(encrypt(credential.getSecretValue()));
        }
        if (credential.getSecretKey() != null) {
            credential.setSecretKey(encrypt(credential.getSecretKey()));
        }

        credential.setCreateTime(LocalDateTime.now());
        credential.setUpdateTime(LocalDateTime.now());
        credential.setUseCount(0);

        Credential saved = credentialRepository.save(credential);

        // 记录审计日志
        auditLogService.logCredentialCreate(saved.getId(), saved.getName(), saved.getType());

        return saved;
    }

    /**
     * 更新凭据
     */
    @Transactional
    public Credential updateCredential(Long id, Credential credential) {
        Credential existing = credentialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("凭据不存在: " + id));

        // 更新字段
        if (credential.getUsername() != null) {
            existing.setUsername(credential.getUsername());
        }
        if (credential.getUrl() != null) {
            existing.setUrl(credential.getUrl());
        }
        if (credential.getDescription() != null) {
            existing.setDescription(credential.getDescription());
        }
        if (credential.getTags() != null) {
            existing.setTags(credential.getTags());
        }
        if (credential.getType() != null) {
            existing.setType(credential.getType());
        }
        if (credential.getName() != null) {
            existing.setName(credential.getName());
        }
        // 处理过期时间
        existing.setExpireTime(credential.getExpireTime());

        // 重新加密凭据值
        if (credential.getSecretValue() != null) {
            existing.setSecretValue(encrypt(credential.getSecretValue()));
        }
        if (credential.getSecretKey() != null) {
            existing.setSecretKey(encrypt(credential.getSecretKey()));
        }

        existing.setUpdateTime(LocalDateTime.now());
        return credentialRepository.save(existing);
    }

    /**
     * 删除凭据
     */
    @Transactional
    public void deleteCredential(Long id) {
        Credential credential = credentialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("凭据不存在: " + id));

        credentialRepository.delete(credential);
        auditLogService.logCredentialDelete(id, credential.getName());
    }

    /**
     * 获取凭据（解密）- 用于任务执行
     */
    public Credential getDecryptedCredential(Long id) {
        Credential credential = credentialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("凭据不存在: " + id));

        return decryptCredential(credential);
    }

    /**
     * 获取凭据（解密）- 按名称
     */
    public Credential getDecryptedCredentialByName(String name) {
        Credential credential = credentialRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("凭据不存在: " + name));

        return decryptCredential(credential);
    }

    /**
     * 获取凭据（不解密）- 用于列表展示
     */
    public List<Credential> listCredentials() {
        return credentialRepository.findAll();
    }

    /**
     * 获取凭据（不解密）- 按ID
     */
    public Optional<Credential> getCredentialById(Long id) {
        return credentialRepository.findById(id);
    }

    /**
     * 搜索凭据
     */
    public List<Credential> searchCredentials(String keyword) {
        return credentialRepository.searchByKeyword(keyword);
    }

    /**
     * 根据类型获取凭据
     */
    public List<Credential> getCredentialsByType(String type) {
        return credentialRepository.findByType(type);
    }

    /**
     * 获取凭据供任务使用（记录访问日志）
     */
    public Map<String, String> getCredentialForTask(Long credentialId, String taskName) {
        if (credentialId == null) {
            return null;
        }

        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new RuntimeException("任务关联的凭据不存在: " + credentialId));

        // 记录访问日志
        auditLogService.logCredentialAccess(credentialId, credential.getName(), "任务执行: " + taskName);

        // 更新使用统计
        credential.setLastUsedTime(LocalDateTime.now());
        credential.setUseCount(credential.getUseCount() + 1);
        credentialRepository.save(credential);

        // 返回解密后的凭据
        Map<String, String> result = new HashMap<>();
        result.put("name", credential.getName());
        result.put("username", credential.getUsername());
        result.put("password", decrypt(credential.getSecretValue()));
        result.put("secretKey", credential.getSecretKey() != null ? decrypt(credential.getSecretKey()) : null);
        result.put("url", credential.getUrl());
        result.put("type", credential.getType());

        return result;
    }

    /**
     * 解密凭据
     */
    private Credential decryptCredential(Credential credential) {
        Credential decrypted = new Credential();
        // 复制所有字段
        decrypted.setId(credential.getId());
        decrypted.setName(credential.getName());
        decrypted.setType(credential.getType());
        decrypted.setUsername(credential.getUsername());
        decrypted.setUrl(credential.getUrl());
        decrypted.setDescription(credential.getDescription());
        decrypted.setTags(credential.getTags());
        decrypted.setStatus(credential.getStatus());
        decrypted.setExpireTime(credential.getExpireTime());
        decrypted.setCreateTime(credential.getCreateTime());
        decrypted.setUpdateTime(credential.getUpdateTime());
        decrypted.setUseCount(credential.getUseCount());
        decrypted.setLastUsedTime(credential.getLastUsedTime());

        // 解密敏感字段
        if (credential.getSecretValue() != null) {
            decrypted.setSecretValue(decrypt(credential.getSecretValue()));
        }
        if (credential.getSecretKey() != null) {
            decrypted.setSecretKey(decrypt(credential.getSecretKey()));
        }

        return decrypted;
    }

    /**
     * 加密
     */
    private String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密
     */
    private String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return null;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            byte[] iv = new byte[IV_SIZE];
            byte[] cipherBytes = new byte[combined.length - IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(cipherBytes);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }
}