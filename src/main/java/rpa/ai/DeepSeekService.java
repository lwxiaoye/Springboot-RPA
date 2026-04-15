package rpa.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;

@Service
public class DeepSeekService {

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.model}")
    private String model;

    @Value("${deepseek.max-tokens}")
    private int maxTokens;

    @Value("${deepseek.temperature}")
    private double temperature;

    private final RestTemplate restTemplate;

    public DeepSeekService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 生成RPA机器人代码
     */
    public String generateRobotCode(String prompt, String category) {
        String systemPrompt = buildSystemPrompt(category);
        return callDeepSeekApi(systemPrompt, prompt);
    }

    /**
     * 优化提示词
     */
    public String optimizePrompt(String prompt, String category) {
        String systemPrompt = "你是一个专业的RPA机器人代码助手。" +
                "请根据用户的需求描述，优化和补充细节，使其更加清晰和具体。" +
                "返回优化后的需求描述，保持简洁。";

        String userPrompt = "原始需求：" + prompt + "\n机器人分类：" + getCategoryName(category);
        return callDeepSeekApi(systemPrompt, userPrompt);
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String category) {
        String categoryDesc = getCategoryName(category);

        return String.format("""
            你是一个专业的RPA机器人代码生成助手。

            请根据用户的需求生成RPA机器人执行代码。

            机器人分类：%s

            支持的命令格式：
            - @collect URL          - 采集网页内容
            - @parse                - 解析HTML/JSON/XML
            - @parse_rule 规则      - 解析规则
            - @table_selector 选择器 - 表格CSS选择器
            - @columns 列名列表      - 列配置
            - @process clean,transform,validate,dedup - 数据处理步骤
            - @validate 校验规则     - 数据校验规则
            - @transform 转换规则   - 数据转换规则
            - @store 表名           - 存储到数据库
            - @log 消息             - 日志输出

            请生成完整、可执行的机器人代码注释和命令。
            只返回代码，不需要解释。
            """, categoryDesc);
    }

    /**
     * 获取分类名称
     */
    private String getCategoryName(String category) {
        Map<String, String> categories = Map.of(
            "DATA_COLLECT", "数据采集",
            "DATA_PARSE", "数据解析",
            "DATA_PROCESS", "数据加工",
            "GENERAL", "通用执行"
        );
        return categories.getOrDefault(category, "通用执行");
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekApi(String systemPrompt, String userPrompt) {
        try {
            // 检查API配置
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_DEEPSEEK_API_KEY")) {
                return generateFallbackCode(userPrompt);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            return generateFallbackCode(userPrompt);

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("DeepSeek API认证失败：" + e.getMessage());
        } catch (ResourceAccessException e) {
            throw new RuntimeException("无法连接DeepSeek API：" + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("调用DeepSeek API失败：" + e.getMessage());
        }
    }

    /**
     * 生成备用代码（当API不可用时）
     */
    private String generateFallbackCode(String prompt) {
        return "// AI生成的RPA机器人\n" +
               "// 描述: " + prompt + "\n\n" +
               "@collect http://example.com/data\n" +
               "@table_selector table.data-list tbody tr\n" +
               "@columns 列1,列2,列3\n" +
               "@process clean,transform\n" +
               "@store report_data\n" +
               "@log 任务执行完成";
    }
}
