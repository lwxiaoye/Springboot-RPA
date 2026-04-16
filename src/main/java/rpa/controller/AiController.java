package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI代码生成控制器
 * 支持智谱AI调用生成RPA机器人代码
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiController {

    @Value("${zhipu.api-url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private String apiUrl;

    @Value("${zhipu.api-key:}")
    private String apiKey;

    @Value("${zhipu.model:glm-4-flash}")
    private String model;

    @Value("${zhipu.max-tokens:2000}")
    private int maxTokens;

    @Value("${zhipu.temperature:0.7}")
    private double temperature;

    private final RestTemplate restTemplate;

    /**
     * 生成RPA机器人代码
     */
    @PostMapping("/generate-robot-code")
    public ResponseEntity<Map<String, Object>> generateRobotCode(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            String category = request.getOrDefault("category", "GENERAL");

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "提示词不能为空"
                ));
            }

            // 检查API配置
            if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("YOUR_")) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", -1);
                result.put("message", "请先在系统设置 - 集成中心配置智谱AI API Key");
                result.put("data", Map.of("code", getFallbackCode(prompt, category)));
                return ResponseEntity.ok(result);
            }

            // 调用智谱AI生成代码
            String generatedCode = callZhipuApi(prompt, category);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "success");
            result.put("data", Map.of("code", generatedCode));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("AI代码生成失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", -1);
            result.put("message", "AI生成失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 优化提示词
     */
    @PostMapping("/optimize-prompt")
    public ResponseEntity<Map<String, Object>> optimizePrompt(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            String category = request.getOrDefault("category", "GENERAL");

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "提示词不能为空"
                ));
            }

            // 检查API配置
            if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("YOUR_")) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", -1);
                result.put("message", "请先在系统设置 - 集成中心配置智谱AI API Key");
                return ResponseEntity.ok(result);
            }

            // 调用智谱AI优化提示词
            String systemPrompt = "你是一个专业的RPA机器人代码助手。请根据用户的需求描述，优化和补充细节，使其更加清晰和具体。返回优化后的需求描述，保持简洁。";
            String userPrompt = "原始需求：" + prompt + "\n机器人分类：" + getCategoryName(category);

            String optimizedPrompt = callZhipuApiDirect(systemPrompt, userPrompt);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "success");
            result.put("data", Map.of("optimizedPrompt", optimizedPrompt));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("AI提示词优化失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", -1);
            result.put("message", "优化失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 调用智谱AI API生成代码
     */
    private String callZhipuApi(String userPrompt, String category) {
        String systemPrompt = buildSystemPrompt(category);
        return callZhipuApiDirect(systemPrompt, userPrompt);
    }

    /**
     * 直接调用智谱AI API
     */
    private String callZhipuApiDirect(String systemPrompt, String userPrompt) {
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

        log.info("调用智谱AI API: {}, 模型: {}", apiUrl, model);

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
                String content = (String) message.get("content");
                log.info("智谱AI返回内容长度: {}", content != null ? content.length() : 0);
                return content;
            }
        }

        throw new RuntimeException("智谱AI返回内容为空");
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String category) {
        String categoryDesc = getCategoryName(category);

        return "你是一个专业的RPA机器人代码生成助手。\n\n" +
               "请根据用户的需求生成RPA机器人执行代码。\n\n" +
               "机器人分类：" + categoryDesc + "\n\n" +
               "支持的命令格式：\n" +
               "- @collect URL          - 采集网页内容\n" +
               "- @parse                - 解析HTML/JSON/XML\n" +
               "- @parse_rule 规则      - 解析规则\n" +
               "- @table_selector 选择器 - 表格CSS选择器\n" +
               "- @columns 列名列表      - 列配置\n" +
               "- @process clean,transform,validate,dedup - 数据处理步骤\n" +
               "- @validate 校验规则     - 数据校验规则\n" +
               "- @transform 转换规则   - 数据转换规则\n" +
               "- @store 表名           - 存储到数据库\n" +
               "- @log 消息             - 日志输出\n\n" +
               "请生成完整、可执行的机器人代码注释和命令。\n" +
               "只返回代码，不需要解释。";
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
     * 备用代码（当API不可用时）
     */
    private String getFallbackCode(String prompt, String category) {
        String categoryDesc = getCategoryName(category);
        return "// AI生成的RPA机器人\n" +
               "// 分类: " + categoryDesc + "\n" +
               "// 需求: " + prompt + "\n\n" +
               "@collect http://example.com/data\n" +
               "@table_selector table.data-list tbody tr\n" +
               "@columns 列1,列2,列3\n" +
               "@process clean,transform\n" +
               "@store report_data\n" +
               "@log 任务执行完成\n\n" +
               "// 注意：请先在系统设置 - 集成中心配置智谱AI API Key以获得AI生成的代码";
    }
}
