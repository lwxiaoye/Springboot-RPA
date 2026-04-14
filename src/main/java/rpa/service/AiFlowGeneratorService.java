package rpa.service;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rpa.ai.AiService;
import rpa.ai.LlmService;

import java.util.*;

/**
 * AI流程生成服务
 *
 * 核心功能：
 * 1. 理解用户自然语言描述
 * 2. 分解任务步骤
 * 3. 生成流程节点配置
 * 4. 输出可执行的流程JSON
 *
 * @author RPA System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFlowGeneratorService {

    private final LlmService llmService;

    @Value("${rpa.flow-generator.max-steps:50}")
    private int maxSteps;

    @Value("${rpa.flow-generator.confidence-threshold:0.7}")
    private double confidenceThreshold;

    /**
     * 根据描述生成流程
     *
     * @param description 用户描述（如："帮我自动抓取淘宝店铺的月销售额"）
     * @param context 上下文信息
     * @return 生成的流程
     */
    public FlowGenerationResult generateFromDescription(String description, Map<String, Object> context) {
        FlowGenerationResult result = new FlowGenerationResult();

        try {
            log.info("开始生成流程: {}", description);

            // 1. 理解用户意图
            String intentPrompt = buildIntentPrompt(description);
            AiService.AiResult intentResult = llmService.chat(intentPrompt, null);
            String intent = intentResult.isSuccess() ? intentResult.getText() : "未知意图";
            result.setIntent(intent);

            // 2. 分解任务步骤
            String stepsPrompt = buildStepsPrompt(description, intent);
            AiService.AiResult stepsResult = llmService.chat(stepsPrompt, null);
            String stepsJson = stepsResult.isSuccess() ? stepsResult.getText() : "[]";
            List<TaskStep> steps = parseSteps(stepsJson);

            result.setSteps(steps);

            // 3. 生成流程节点
            List<FlowNode> nodes = new ArrayList<>();
            List<FlowEdge> edges = new ArrayList<>();

            int nodeId = 1;
            String prevNodeId = null;

            for (TaskStep step : steps) {
                // 创建节点
                FlowNode node = createNodeFromStep(step, nodeId);
                nodes.add(node);

                // 创建连接
                if (prevNodeId != null) {
                    FlowEdge edge = new FlowEdge();
                    edge.setId("edge_" + (nodeId - 1));
                    edge.setSource(prevNodeId);
                    edge.setTarget(String.valueOf(nodeId));
                    edges.add(edge);
                }

                prevNodeId = String.valueOf(nodeId);
                nodeId++;
            }

            // 4. 构建流程配置
            FlowConfig flowConfig = new FlowConfig();
            flowConfig.setName(extractFlowName(description));
            flowConfig.setDescription(description);
            flowConfig.setNodes(nodes);
            flowConfig.setEdges(edges);
            flowConfig.setIntent(intent);

            result.setConfig(flowConfig);
            result.setSuccess(true);
            result.setMessage("流程生成成功，共 " + steps.size() + " 个步骤");

            log.info("流程生成完成: {} 个节点", nodes.size());

        } catch (Exception e) {
            log.error("流程生成失败", e);
            result.setSuccess(false);
            result.setError(e.getMessage());
            result.setMessage("流程生成失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 优化现有流程
     */
    public FlowGenerationResult optimizeFlow(FlowConfig existingFlow, String optimizationGoal) {
        FlowGenerationResult result = new FlowGenerationResult();

        try {
            log.info("优化流程: {}", optimizationGoal);

            // 构建优化提示
            String prompt = String.format("""
                请优化以下RPA流程，目标：%s

                现有流程：
                %s

                请输出优化后的流程配置，保持JSON格式。
                """, optimizationGoal, JSON.toJSONString(existingFlow));

            String response = llmService.chat(prompt, null).getText();
            FlowConfig optimized = JSON.parseObject(response, FlowConfig.class);

            result.setConfig(optimized);
            result.setSuccess(true);
            result.setMessage("流程优化完成");

        } catch (Exception e) {
            log.error("流程优化失败", e);
            result.setSuccess(false);
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * 分析流程并提供建议
     */
    public List<String> analyzeAndSuggest(FlowConfig flow) {
        try {
            String prompt = String.format("""
                请分析以下RPA流程并提供改进建议：

                流程名称：%s
                流程描述：%s
                节点数量：%d

                请输出3-5条具体的改进建议，每条建议不超过50字。
                """, flow.getName(), flow.getDescription(), flow.getNodes().size());

            String response = llmService.chat(prompt, null).getText();
            return parseSuggestions(response);

        } catch (Exception e) {
            log.error("流程分析失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 从对话中继续完善流程
     */
    public FlowGenerationResult continueFromContext(String context, String userInput) {
        FlowGenerationResult result = new FlowGenerationResult();

        try {
            String prompt = String.format("""
                用户正在设计RPA流程，对话上下文：
                %s

                用户说：%s

                请理解用户意图，继续完善流程配置。输出完整的流程JSON配置。
                """, context, userInput);

            String response = llmService.chat(prompt, null).getText();
            FlowConfig flow = JSON.parseObject(response, FlowConfig.class);

            result.setConfig(flow);
            result.setSuccess(true);

        } catch (Exception e) {
            log.error("继续生成失败", e);
            result.setSuccess(false);
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * 导出为流程设计器格式
     */
    public Map<String, Object> exportToDesigner(FlowConfig flow) {
        Map<String, Object> designerData = new LinkedHashMap<>();

        designerData.put("name", flow.getName());
        designerData.put("description", flow.getDescription());
        designerData.put("version", "1.0");
        designerData.put("createdBy", "AI Flow Generator");

        // 转换为Vue Flow格式
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (FlowNode node : flow.getNodes()) {
            nodes.add(nodeToDesignerNode(node));
        }

        for (FlowEdge edge : flow.getEdges()) {
            edges.add(edgeToDesignerEdge(edge));
        }

        designerData.put("nodes", nodes);
        designerData.put("edges", edges);

        return designerData;
    }

    // ==================== 私有方法 ====================

    private String buildIntentPrompt(String description) {
        return String.format("""
            请分析以下RPA任务的意图，只输出意图描述（不超过20字）：

            任务：%s

            示例输出：网页数据抓取
            """, description);
    }

    private String buildStepsPrompt(String description, String intent) {
        return String.format("""
            请将以下RPA任务分解为具体的执行步骤，并输出JSON数组格式：

            任务：%s
            意图：%s

            每个步骤需要包含：
            - name: 步骤名称
            - type: 步骤类型（browser/database/file/excel/condition/loop/script/email等）
            - action: 具体动作（open/navigate/click/input/extract/query/read/write/send等）
            - params: 参数配置（JSON对象）
            - description: 步骤描述

            请输出不超过%d个步骤，返回JSON数组。
            """, description, intent, maxSteps);
    }

    private List<TaskStep> parseSteps(String jsonStr) {
        try {
            return JSON.parseArray(jsonStr, TaskStep.class);
        } catch (Exception e) {
            log.warn("解析步骤失败，尝试修复格式: {}", e.getMessage());
            return parseStepsWithFix(jsonStr);
        }
    }

    private List<TaskStep> parseStepsWithFix(String jsonStr) {
        List<TaskStep> steps = new ArrayList<>();

        // 尝试提取JSON数组部分
        int start = jsonStr.indexOf('[');
        int end = jsonStr.lastIndexOf(']');

        if (start >= 0 && end > start) {
            String arrayStr = jsonStr.substring(start, end + 1);
            try {
                steps = JSON.parseArray(arrayStr, TaskStep.class);
            } catch (Exception e) {
                log.error("无法解析步骤JSON");
            }
        }

        return steps;
    }

    private String extractFlowName(String description) {
        // 简单���取前10个字作为名称
        if (description.length() <= 10) {
            return description;
        }
        return description.substring(0, 10) + "...";
    }

    private FlowNode createNodeFromStep(TaskStep step, int nodeId) {
        FlowNode node = new FlowNode();
        node.setId(String.valueOf(nodeId));
        node.setName(step.getName());
        node.setType(step.getType());
        node.setAction(step.getAction());
        node.setParams(step.getParams());
        node.setDescription(step.getDescription());

        // 设置默认位置
        node.setX(250 + (nodeId - 1) % 3 * 300);
        node.setY(100 + (nodeId - 1) / 3 * 150);

        return node;
    }

    private List<String> parseSuggestions(String response) {
        List<String> suggestions = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.length() > 5 && (line.startsWith("-") || line.startsWith("•") || line.matches("^\\d+[.、].*"))) {
                suggestions.add(line.replaceAll("^[\\d\\.、\\-•\\s]+", "").trim());
            }
        }

        return suggestions;
    }

    private Map<String, Object> nodeToDesignerNode(FlowNode node) {
        Map<String, Object> designerNode = new LinkedHashMap<>();
        designerNode.put("id", node.getId());
        designerNode.put("type", node.getType());
        designerNode.put("position", Map.of("x", node.getX(), "y", node.getY()));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("label", node.getName());
        data.put("nodeType", node.getType());
        data.put("action", node.getAction());
        data.put("params", node.getParams());
        data.put("description", node.getDescription());

        designerNode.put("data", data);

        return designerNode;
    }

    private Map<String, Object> edgeToDesignerEdge(FlowEdge edge) {
        Map<String, Object> designerEdge = new LinkedHashMap<>();
        designerEdge.put("id", edge.getId());
        designerEdge.put("source", edge.getSource());
        designerEdge.put("target", edge.getTarget());
        designerEdge.put("type", "smoothstep");

        return designerEdge;
    }

    // ==================== 内部类 ====================

    @lombok.Data
    public static class FlowGenerationResult {
        private boolean success;
        private String message;
        private String error;
        private String intent;
        private List<TaskStep> steps;
        private FlowConfig config;
    }

    @lombok.Data
    public static class TaskStep {
        private String name;
        private String type;
        private String action;
        private Map<String, Object> params;
        private String description;
    }

    @lombok.Data
    public static class FlowConfig {
        private String name;
        private String description;
        private String intent;
        private List<FlowNode> nodes;
        private List<FlowEdge> edges;
    }

    @lombok.Data
    public static class FlowNode {
        private String id;
        private String name;
        private String type;
        private String action;
        private Map<String, Object> params;
        private String description;
        private int x;
        private int y;
    }

    @lombok.Data
    public static class FlowEdge {
        private String id;
        private String source;
        private String target;
    }
}