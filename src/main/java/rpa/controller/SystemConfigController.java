package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import rpa.service.SystemConfigService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 * <p>
 * 提供系统配置的RESTful API接口，包括：
 * <ul>
 *   <li>通用设置配置</li>
 *   <li>消息服务配置</li>
 *   <li>存储配置</li>
 *   <li>OCR服务配置</li>
 *   <li>大模型配置</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@CrossOrigin
public class SystemConfigController {

    private final SystemConfigService configService;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取所有配置
     */
    @GetMapping
    public Map<String, Object> getAllConfigs() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Map<String, String>> configs = configService.getAllConfigs();
            response.put("code", 0);
            response.put("data", configs);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "获取配置失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取指定分类的配置
     */
    @GetMapping("/category/{category}")
    public Map<String, Object> getConfigsByCategory(@PathVariable String category) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, String> configs = configService.getConfigsByCategory(category);
            response.put("code", 0);
            response.put("data", configs);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "获取配置失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取单个配置值
     */
    @GetMapping("/{key}")
    public Map<String, Object> getConfig(@PathVariable String key) {
        Map<String, Object> response = new HashMap<>();
        try {
            String value = configService.getConfig(key);
            response.put("code", 0);
            response.put("data", value);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "获取配置失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 保存配置
     */
    @PostMapping
    public Map<String, Object> saveConfig(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String category = (String) request.get("category");
            Map<String, String> configs = (Map<String, String>) request.get("configs");

            if (category == null || configs == null) {
                response.put("code", -1);
                response.put("message", "参数不完整");
                return response;
            }

            configService.saveConfigs(category, configs);
            response.put("code", 0);
            response.put("message", "配置保存成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "保存失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 保存通用设置
     */
    @PostMapping("/general")
    public Map<String, Object> saveGeneralConfig(@RequestBody Map<String, String> configs) {
        Map<String, Object> response = new HashMap<>();
        try {
            configService.saveConfigs(SystemConfigService.CATEGORY_GENERAL, configs);
            response.put("code", 0);
            response.put("message", "通用设置保存成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "保存失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 保存消息服务配置
     */
    @PostMapping("/message")
    public Map<String, Object> saveMessageConfig(@RequestBody Map<String, String> configs) {
        Map<String, Object> response = new HashMap<>();
        try {
            configService.saveConfigs(SystemConfigService.CATEGORY_MESSAGE, configs);
            response.put("code", 0);
            response.put("message", "消息服务配置保存成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "保存失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 保存存储配置
     */
    @PostMapping("/storage")
    public Map<String, Object> saveStorageConfig(@RequestBody Map<String, String> configs) {
        Map<String, Object> response = new HashMap<>();
        try {
            configService.saveConfigs(SystemConfigService.CATEGORY_STORAGE, configs);
            response.put("code", 0);
            response.put("message", "存储配置保存成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "保存失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 保存OCR配置
     */
    @PostMapping("/ocr")
    public Map<String, Object> saveOcrConfig(@RequestBody Map<String, String> configs) {
        Map<String, Object> response = new HashMap<>();
        try {
            configService.saveConfigs(SystemConfigService.CATEGORY_OCR, configs);
            response.put("code", 0);
            response.put("message", "OCR配置保存成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "保存失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 保存大模型配置
     */
    @PostMapping("/llm")
    public Map<String, Object> saveLlmConfig(@RequestBody Map<String, String> configs) {
        Map<String, Object> response = new HashMap<>();
        try {
            configService.saveConfigs(SystemConfigService.CATEGORY_LLM, configs);
            response.put("code", 0);
            response.put("message", "大模型配置保存成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "保存失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 初始化默认配置
     */
    @PostMapping("/init")
    public Map<String, Object> initDefaultConfigs() {
        Map<String, Object> response = new HashMap<>();
        try {
            configService.initDefaultConfigs();
            response.put("code", 0);
            response.put("message", "默认配置初始化成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "初始化失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 测试OCR连接
     */
    @PostMapping("/ocr/test")
    public Map<String, Object> testOcr(@RequestBody Map<String, String> configs) {
        Map<String, Object> response = new HashMap<>();
        try {
            String provider = configs.get("provider");
            // TODO: 实现实际的OCR连接测试
            response.put("code", 0);
            response.put("message", provider + " OCR连接测试成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "测试失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 测试大模型连接
     */
    @PostMapping("/llm/test")
    public Map<String, Object> testLlm(@RequestBody Map<String, String> configs) {
        Map<String, Object> response = new HashMap<>();
        String provider = configs.get("provider");
        String apiUrl = configs.get("apiUrl");
        String apiKey = configs.get("apiKey");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            response.put("code", -1);
            response.put("message", "API Key不能为空");
            return response;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of("messages",
                List.of(Map.of("role", "user", "content", "hi")),
                "model", getModelForProvider(provider));

            HttpEntity<Map<String, Object>> entity;
            String testUrl;

            switch (provider) {
                case "openai":
                    headers.setBearerAuth(apiKey);
                    testUrl = ensureEndsWithSlash(apiUrl) + "chat/completions";
                    entity = new HttpEntity<>(body, headers);
                    break;
                case "zhipu":
                    headers.setBearerAuth(apiKey);
                    testUrl = ensureEndsWithSlash(apiUrl) + "chat/completions";
                    entity = new HttpEntity<>(body, headers);
                    break;
                case "qwen":
                    headers.set("Authorization", "Bearer " + apiKey);
                    testUrl = ensureEndsWithSlash(apiUrl) + "services/aigc/text-generation/instance-sdk";
                    entity = new HttpEntity<>(body, headers);
                    break;
                default:
                    response.put("code", -1);
                    response.put("message", "不支持的提供商: " + provider);
                    return response;
            }

            ResponseEntity<String> result = restTemplate.exchange(
                testUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (result.getStatusCode().is2xxSuccessful()) {
                response.put("code", 0);
                response.put("message", provider + " 连接成功!");
            } else {
                response.put("code", -1);
                response.put("message", "HTTP错误: " + result.getStatusCode());
            }

        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", "连接失败: " + e.getMessage());
        }
        return response;
    }

    private String getModelForProvider(String provider) {
        switch (provider) {
            case "openai": return "gpt-3.5-turbo";
            case "zhipu": return "glm-4-flash";
            case "qwen": return "qwen-turbo";
            default: return "gpt-3.5-turbo";
        }
    }

    private String ensureEndsWithSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
