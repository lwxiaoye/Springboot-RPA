package rpa.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.service.SystemConfigService;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * 大模型服务（调用本地 DeepSeek）
 *
 * @author RPA System
 */
@Slf4j
@Service
public class LlmService {

    private final SystemConfigService systemConfigService;

    // 可用的模型列表
    public static final Map<String, String> AVAILABLE_MODELS = new LinkedHashMap<>();
    static {
        AVAILABLE_MODELS.put("deepseek-r1:7b", "DeepSeek R1 7B (本地部署)");
        AVAILABLE_MODELS.put("deepseek-r1:14b", "DeepSeek R1 14B (本地部署)");
        AVAILABLE_MODELS.put("qwen-turbo", "通义千问 Turbo");
        AVAILABLE_MODELS.put("qwen-plus", "通义千问 Plus");
        AVAILABLE_MODELS.put("glm-4-flash", "智谱 GLM-4-Flash");
    };

    public LlmService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * 获取LLM配置
     */
    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("provider", systemConfigService.getConfig(SystemConfigService.KEY_LLM_PROVIDER, "local"));
        config.put("apiUrl", systemConfigService.getConfig(SystemConfigService.KEY_LLM_API_URL, ""));
        config.put("apiKey", systemConfigService.getConfig(SystemConfigService.KEY_LLM_API_KEY, ""));
        config.put("model", systemConfigService.getConfig(SystemConfigService.KEY_LLM_MODEL, "deepseek-r1:7b"));
        return config;
    }

    /**
     * 检查LLM是否已配置
     */
    public boolean isConfigured() {
        Map<String, String> config = getConfig();
        String provider = config.get("provider");

        // 本地部署模式不需要API Key
        if ("local".equals(provider)) {
            return config.get("apiUrl") != null && !config.get("apiUrl").isEmpty();
        }

        // 云端模式需要API Key
        return config.get("apiKey") != null && !config.get("apiKey").isEmpty();
    }

    /**
     * 调用大模型进行对话
     */
    public AiService.AiResult chat(String prompt, Map<String, Object> options) {
        AiService.AiResult result = new AiService.AiResult();

        try {
            Map<String, String> config = getConfig();
            String provider = config.get("provider");
            String apiUrl = config.get("apiUrl");
            String apiKey = config.get("apiKey");
            String model = config.get("model");

            if (apiUrl == null || apiUrl.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("LLM API地址未配置，请在系统设置中配置");
                return result;
            }

            // 根据提供商调用不同的API
            String responseText;
            if ("local".equals(provider) || apiUrl.contains("localhost") || apiUrl.contains("127.0.0.1")) {
                // 本地部署（Ollama）
                responseText = callOllama(apiUrl, prompt, model, options);
            } else if ("qwen".equals(provider)) {
                // 通义千问
                responseText = callQwen(apiUrl, apiKey, prompt, model);
            } else if ("zhipu".equals(provider)) {
                // 智谱AI
                responseText = callZhipu(apiUrl, apiKey, prompt, model);
            } else if ("openai".equals(provider)) {
                // OpenAI兼容API
                responseText = callOpenAICompatible(apiUrl, apiKey, prompt, model);
            } else {
                // 默认使用本地Ollama格式
                responseText = callOllama(apiUrl, prompt, model, options);
            }

            result.setSuccess(true);
            result.setText(responseText);

            Map<String, Object> data = new HashMap<>();
            data.put("response", responseText);
            data.put("model", model);
            data.put("provider", provider);
            result.setData(data);

        } catch (Exception e) {
            log.error("LLM调用失败", e);
            result.setSuccess(false);
            result.setErrorMessage("LLM调用失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 调用本地 Ollama API（支持 DeepSeek 等）
     */
    private String callOllama(String apiUrl, String prompt, String model, Map<String, Object> options) throws Exception {
        // 清理API URL末尾的斜杠
        if (apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        }

        // 构建请求
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model != null ? model : "deepseek-r1:7b");
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        // 添加可选参数
        if (options != null) {
            if (options.containsKey("temperature")) {
                requestBody.put("temperature", options.get("temperature"));
            }
            if (options.containsKey("max_tokens")) {
                requestBody.put("options", Map.of("num_predict", options.get("max_tokens")));
            }
            if (options.containsKey("system")) {
                requestBody.put("system", options.get("system"));
            }
        }

        // 设置超时时间为3分钟（LLM响应可能较慢）
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl + "/api/generate");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(180000);
            conn.setDoOutput(true);

            // 发送请求
            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);
            conn.getOutputStream().write(jsonBody.getBytes());

            // 读取响应
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("Ollama API返回错误码: " + responseCode);
            }

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 解析JSON响应
            Map<String, Object> responseMap = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(response.toString(), Map.class);

            return (String) responseMap.getOrDefault("response", "");

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 调用通义千问 API
     */
    private String callQwen(String apiUrl, String apiKey, String prompt, String model) throws Exception {
        // 清理API URL
        if (apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        }
        if (!apiUrl.contains("/v1")) {
            apiUrl = apiUrl + "/services/aigc/text-generation/generation";
        }

        java.net.HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(180000);
            conn.setDoOutput(true);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model != null ? model : "qwen-turbo");
            requestBody.put("input", Map.of("prompt", prompt));

            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);
            conn.getOutputStream().write(jsonBody.getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("通义千问API返回错误码: " + responseCode);
            }

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            Map<String, Object> responseMap = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(response.toString(), Map.class);

            // 解析通义千问响应格式
            Map<String, Object> output = (Map<String, Object>) responseMap.get("output");
            return output != null ? (String) output.getOrDefault("text", "") : "";

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 调用智谱AI API
     */
    private String callZhipu(String apiUrl, String apiKey, String prompt, String model) throws Exception {
        if (apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        }
        if (!apiUrl.contains("/v4")) {
            apiUrl = apiUrl + "/api/paas/v4/chat/completions";
        }

        java.net.HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(180000);
            conn.setDoOutput(true);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model != null ? model : "glm-4-flash");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));

            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);
            conn.getOutputStream().write(jsonBody.getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("智谱AI API返回错误码: " + responseCode);
            }

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            Map<String, Object> responseMap = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(response.toString(), Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return message != null ? (String) message.getOrDefault("content", "") : "";
            }
            return "";

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 调用 OpenAI 兼容 API
     */
    private String callOpenAICompatible(String apiUrl, String apiKey, String prompt, String model) throws Exception {
        if (apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        }
        if (!apiUrl.contains("/v1")) {
            apiUrl = apiUrl + "/v1/chat/completions";
        }

        java.net.HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(180000);
            conn.setDoOutput(true);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model != null ? model : "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));

            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);
            conn.getOutputStream().write(jsonBody.getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("API返回错误码: " + responseCode);
            }

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            Map<String, Object> responseMap = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(response.toString(), Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return message != null ? (String) message.getOrDefault("content", "") : "";
            }
            return "";

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 测试LLM连接
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, String> config = getConfig();
            String provider = config.get("provider");
            String apiUrl = config.get("apiUrl");
            String model = config.get("model");

            // 发送一个简单��测试请求
            AiService.AiResult chatResult = chat("你好，请回复'连接测试成功'", null);

            result.put("success", chatResult.isSuccess());
            result.put("provider", provider);
            result.put("apiUrl", apiUrl);
            result.put("model", model);
            result.put("message", chatResult.isSuccess() ? "连接成功" : chatResult.getErrorMessage());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
        }

        return result;
    }
}
