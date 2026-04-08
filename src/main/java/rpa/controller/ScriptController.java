package rpa.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.executor.ScriptExecutor;

import java.util.HashMap;
import java.util.Map;

/**
 * 脚本执行控制器
 * 
 * 提供脚本执行的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/script")
@RequiredArgsConstructor
@CrossOrigin
public class ScriptController {

    private final ScriptExecutor scriptExecutor;

    /**
     * 执行脚本
     */
    @PostMapping("/execute")
    public Map<String, Object> executeScript(@RequestBody ScriptExecuteRequest request) {
        log.info("收到脚本执行请求: type={}, codeLength={}", 
            request.getScriptType(), request.getCode() != null ? request.getCode().length() : 0);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ScriptExecutor.ScriptRequest scriptRequest = new ScriptExecutor.ScriptRequest();
            scriptRequest.setScriptId(generateScriptId());
            scriptRequest.setScriptType(request.getScriptType());
            scriptRequest.setCode(request.getCode());
            scriptRequest.setVariables(request.getVariables());
            scriptRequest.setTimeout(request.getTimeout());
            
            ScriptExecutor.ScriptResult result = scriptExecutor.execute(scriptRequest);
            
            response.put("code", 0);
            response.put("data", convertScriptResult(result));
            return response;
        } catch (SecurityException e) {
            log.warn("脚本安全检查失败: {}", e.getMessage());
            response.put("code", 400);
            response.put("message", e.getMessage());
            return response;
        } catch (Exception e) {
            log.error("脚本执行异常", e);
            response.put("code", 500);
            response.put("message", "脚本执行失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 停止脚本
     */
    @PostMapping("/stop/{scriptId}")
    public Map<String, Object> stopScript(@PathVariable String scriptId) {
        Map<String, Object> response = new HashMap<>();
        boolean stopped = scriptExecutor.stopScript(scriptId);
        if (stopped) {
            response.put("code", 0);
            response.put("message", "脚本已停止");
        } else {
            response.put("code", -1);
            response.put("message", "脚本不存在或已结束");
        }
        return response;
    }

    /**
     * 获取脚本状态
     */
    @GetMapping("/status/{scriptId}")
    public Map<String, Object> getStatus(@PathVariable String scriptId) {
        Map<String, Object> response = new HashMap<>();
        ScriptExecutor.ScriptStatus status = scriptExecutor.getStatus(scriptId);
        response.put("code", 0);
        response.put("data", status.name());
        return response;
    }

    /**
     * 验证脚本安全性
     */
    @PostMapping("/validate")
    public Map<String, Object> validateScript(@RequestBody ScriptValidateRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("scriptType", request.getScriptType());
        result.put("codeLength", request.getCode() != null ? request.getCode().length() : 0);
        
        Map<String, Object> response = new HashMap<>();
        
        // 基本验证
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            result.put("valid", false);
            result.put("reason", "脚本内容为空");
            response.put("code", 0);
            response.put("data", result);
            return response;
        }
        
        if (request.getCode().length() > 100000) {
            result.put("valid", false);
            result.put("reason", "脚本内容过长，最大支持100KB");
            response.put("code", 0);
            response.put("data", result);
            return response;
        }
        
        // 危险命令检测
        String[] dangerPatterns = {"rm -rf /", "format c:", "del /f /s /q"};
        for (String pattern : dangerPatterns) {
            if (request.getCode().contains(pattern)) {
                result.put("valid", false);
                result.put("reason", "包含危险命令: " + pattern);
                response.put("code", 0);
                response.put("data", result);
                return response;
            }
        }
        
        result.put("valid", true);
        result.put("reason", "脚本验证通过");
        response.put("code", 0);
        response.put("data", result);
        return response;
    }

    /**
     * 获取脚本模板
     */
    @GetMapping("/templates")
    public Map<String, Object> getTemplates() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> templates = new HashMap<>();
        
        // Python数据处理模板
        templates.put("python_data_process", 
            "import json\n" +
            "\n# 获取输入数据\ninput_data = '${inputData}'\ndata = json.loads(input_data)\n" +
            "\n# 数据处理逻辑\nresult = []\nfor item in data:\n    processed = item\n    result.append(processed)\n" +
            "\n# 输出结果\nprint(json.dumps(result, ensure_ascii=False))"
        );
        
        // Python HTTP请求模板
        templates.put("python_http",
            "import json\n" +
            "# Python HTTP请求示例\nprint(json.dumps({'status': 'success', 'data': []}))"
        );
        
        // JavaScript数据处理模板
        templates.put("js_data_process",
            "// 获取输入数据\nconst inputData = JSON.parse('${inputData}');\n" +
            "\n// 数据处理逻辑\nconst result = inputData.map(item => ({\n    ...item,\n    processed: true\n}));\n" +
            "\n// 输出结果\nconsole.log(JSON.stringify(result));"
        );
        
        // JavaScript HTTP请求模板
        templates.put("js_http",
            "// JavaScript HTTP请求示例\nconst result = { status: 'success', data: [] };\n" +
            "console.log(JSON.stringify(result));"
        );
        
        // Python Excel处理模板
        templates.put("python_excel",
            "import json\n" +
            "\n# Excel数据处理示例\n# 读取数据: ${excelData}\n" +
            "data = json.loads('${excelData}')\n" +
            "\n# 处理数据\nresult = []\nfor row in data:\n    result.append(row)\n" +
            "\nprint(json.dumps(result, ensure_ascii=False))"
        );
        
        response.put("code", 0);
        response.put("data", templates);
        return response;
    }

    // ==================== 请求类 ====================

    @Data
    public static class ScriptExecuteRequest {
        private String scriptType;  // python, js, shell
        private String code;
        private Map<String, Object> variables;
        private Long timeout;  // 超时时间(ms)
    }

    @Data
    public static class ScriptValidateRequest {
        private String scriptType;
        private String code;
    }

    /**
     * 生成唯一脚本ID
     */
    private String generateScriptId() {
        return "script_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    /**
     * 转换脚本执行结果
     */
    private Map<String, Object> convertScriptResult(ScriptExecutor.ScriptResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("scriptId", result.getScriptId());
        map.put("success", result.isSuccess());
        map.put("output", result.getOutput());
        map.put("errorMessage", result.getErrorMessage());
        map.put("exitCode", result.getExitCode());
        map.put("duration", result.getDuration());
        map.put("killed", result.isKilled());
        return map;
    }
}