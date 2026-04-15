package rpa.controller;

import rpa.ai.DeepSeekService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private final DeepSeekService deepSeekService;

    public AiController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

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

            String generatedCode = deepSeekService.generateRobotCode(prompt, category);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "success");
            result.put("data", Map.of("code", generatedCode));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
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

            String optimizedPrompt = deepSeekService.optimizePrompt(prompt, category);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "success");
            result.put("data", Map.of("optimizedPrompt", optimizedPrompt));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            result.put("data", null);

            return ResponseEntity.ok(result);
        }
    }
}
