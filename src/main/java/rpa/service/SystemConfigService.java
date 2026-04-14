package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.SystemConfig;
import rpa.repository.SystemConfigRepository;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统配置服务类
 * <p>
 * 提供配置的CRUD操作，支持配置的分类管理和加密存储。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository repository;

    // ==================== 配置分类常量 ====================
    public static final String CATEGORY_GENERAL = "general";      // 通用设置
    public static final String CATEGORY_MESSAGE = "message";      // 消息服务
    public static final String CATEGORY_STORAGE = "storage";      // 存储配置
    public static final String CATEGORY_OCR = "ocr";              // OCR服务
    public static final String CATEGORY_LLM = "llm";              // 大模型
    public static final String CATEGORY_LICENSE = "license";      // 许可证

    // ==================== 通用配置键 ====================
    public static final String KEY_SYSTEM_NAME = "system_name";
    public static final String KEY_SESSION_TIMEOUT = "session_timeout";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_TIMEZONE = "timezone";

    // ==================== 消息服务配置键 ====================
    public static final String KEY_SMTP_HOST = "smtp_host";
    public static final String KEY_SMTP_PORT = "smtp_port";
    public static final String KEY_SMTP_USERNAME = "smtp_username";
    public static final String KEY_SMTP_PASSWORD = "smtp_password";
    public static final String KEY_SMTP_SSL = "smtp_ssl";
    public static final String KEY_WEBHOOK_TYPE = "webhook_type";
    public static final String KEY_WEBHOOK_URL = "webhook_url";

    // ==================== 存储配置键 ====================
    public static final String KEY_STORAGE_TYPE = "storage_type";
    public static final String KEY_STORAGE_PATH = "storage_path";
    public static final String KEY_NFS_HOST = "nfs_host";
    public static final String KEY_S3_ENDPOINT = "s3_endpoint";
    public static final String KEY_S3_ACCESS_KEY = "s3_access_key";
    public static final String KEY_S3_SECRET_KEY = "s3_secret_key";
    public static final String KEY_STORAGE_QUOTA = "storage_quota";

    // ==================== OCR配置键 ====================
    public static final String KEY_OCR_PROVIDER = "ocr_provider";
    public static final String KEY_OCR_APP_ID = "ocr_app_id";
    public static final String KEY_OCR_API_KEY = "ocr_api_key";
    public static final String KEY_OCR_SECRET_KEY = "ocr_secret_key";

    // ==================== 大模型配置键 ====================
    public static final String KEY_LLM_PROVIDER = "llm_provider";
    public static final String KEY_LLM_API_URL = "llm_api_url";
    public static final String KEY_LLM_API_KEY = "llm_api_key";
    public static final String KEY_LLM_MODEL = "llm_model";

    /**
     * 获取指定分类的所有配置
     */
    public Map<String, String> getConfigsByCategory(String category) {
        List<SystemConfig> configs = repository.findByCategory(category);
        return configs.stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        config -> decryptIfNeeded(config)
                ));
    }

    /**
     * 获取所有配置（按分类组织）
     */
    public Map<String, Map<String, String>> getAllConfigs() {
        List<SystemConfig> configs = repository.findAll();
        Map<String, Map<String, String>> result = new HashMap<>();

        for (SystemConfig config : configs) {
            result.computeIfAbsent(config.getCategory(), k -> new HashMap<>())
                  .put(config.getConfigKey(), decryptIfNeeded(config));
        }

        return result;
    }

    /**
     * 保存单个配置
     */
    @Transactional
    public SystemConfig saveConfig(String category, String key, String value, boolean isEncrypted) {
        Optional<SystemConfig> existing = repository.findByConfigKey(key);

        SystemConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setConfigValue(isEncrypted ? encrypt(value) : value);
            config.setUpdateTime(java.time.LocalDateTime.now());
        } else {
            config = new SystemConfig();
            config.setCategory(category);
            config.setConfigKey(key);
            config.setConfigValue(isEncrypted ? encrypt(value) : value);
            config.setIsEncrypted(isEncrypted);
            config.setCreateTime(java.time.LocalDateTime.now());
            config.setUpdateTime(java.time.LocalDateTime.now());
        }

        return repository.save(config);
    }

    /**
     * 批量保存配置
     */
    @Transactional
    public void saveConfigs(String category, Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            boolean isEncrypted = isSensitiveKey(key);
            saveConfig(category, key, value, isEncrypted);
        }
    }

    /**
     * 获取单个配置值
     */
    public String getConfig(String key) {
        return repository.findByConfigKey(key)
                .map(this::decryptIfNeeded)
                .orElse(null);
    }

    /**
     * 获取配置值（带默认值）
     */
    public String getConfig(String key, String defaultValue) {
        return repository.findByConfigKey(key)
                .map(this::decryptIfNeeded)
                .orElse(defaultValue);
    }

    /**
     * 删除指定配置
     */
    @Transactional
    public void deleteConfig(String key) {
        repository.deleteByConfigKey(key);
    }

    /**
     * 检查是否为敏感配置键
     */
    private boolean isSensitiveKey(String key) {
        return key.contains("password") ||
               key.contains("secret") ||
               key.contains("api_key") ||
               key.contains("credential") ||
               key.contains("token");
    }

    /**
     * 简单加密（生产环境应使用更安全的加密方式）
     */
    private String encrypt(String value) {
        if (value == null || value.isEmpty()) return value;
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    /**
     * 解密（如果需要）
     */
    private String decryptIfNeeded(SystemConfig config) {
        if (Boolean.TRUE.equals(config.getIsEncrypted())) {
            try {
                return new String(Base64.getDecoder().decode(config.getConfigValue()));
            } catch (Exception e) {
                log.warn("配置解密失败: {}", config.getConfigKey());
                return config.getConfigValue();
            }
        }
        return config.getConfigValue();
    }

    /**
     * 初始化默认配置
     */
    @Transactional
    public void initDefaultConfigs() {
        // 通用设置默认值
        saveConfigIfNotExists(CATEGORY_GENERAL, KEY_SYSTEM_NAME, "RPA运营管理系统", false);
        saveConfigIfNotExists(CATEGORY_GENERAL, KEY_SESSION_TIMEOUT, "30", false);
        saveConfigIfNotExists(CATEGORY_GENERAL, KEY_LANGUAGE, "zh-CN", false);
        saveConfigIfNotExists(CATEGORY_GENERAL, KEY_TIMEZONE, "Asia/Shanghai", false);

        // 存储默认值
        saveConfigIfNotExists(CATEGORY_STORAGE, KEY_STORAGE_TYPE, "local", false);
        saveConfigIfNotExists(CATEGORY_STORAGE, KEY_STORAGE_PATH, "/data/rpa/storage", false);
        saveConfigIfNotExists(CATEGORY_STORAGE, KEY_STORAGE_QUOTA, "500", false);

        // OCR默认值
        saveConfigIfNotExists(CATEGORY_OCR, KEY_OCR_PROVIDER, "baidu", false);

        // LLM默认值
        saveConfigIfNotExists(CATEGORY_LLM, KEY_LLM_PROVIDER, "openai", false);
        saveConfigIfNotExists(CATEGORY_LLM, KEY_LLM_API_URL, "https://api.openai.com/v1", false);
        saveConfigIfNotExists(CATEGORY_LLM, KEY_LLM_MODEL, "gpt-4", false);
    }

    private void saveConfigIfNotExists(String category, String key, String value, boolean isEncrypted) {
        if (!repository.existsByConfigKey(key)) {
            saveConfig(category, key, value, isEncrypted);
        }
    }
}
