package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.recording.ImageLocatorService;
import rpa.recording.ImageLocatorService.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * 图像识别定位控制器
 *
 * 提供图像定位的REST API：
 * - 屏幕查找
 * - 区域查找
 * - 等待图像
 * - 截图保存
 *
 * @author RPA System
 */
@Slf4j
@RestController
@RequestMapping("/api/locator")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class ImageLocatorController {

    private final ImageLocatorService imageLocatorService;

    /**
     * 在屏幕上查找图像
     */
    @PostMapping("/find")
    public Result findOnScreen(@RequestBody FindRequest request) {
        try {
            List<MatchResult> results = imageLocatorService.findOnScreen(request.getTemplatePath());
            return Result.success(Map.of(
                "found", !results.isEmpty(),
                "count", results.size(),
                "results", results
            ));
        } catch (Exception e) {
            log.error("图像查找失败", e);
            return Result.fail("查找失败: " + e.getMessage());
        }
    }

    /**
     * 在区域中查找图像
     */
    @PostMapping("/find-in-region")
    public Result findInRegion(@RequestBody FindInRegionRequest request) {
        try {
            List<MatchResult> results = imageLocatorService.findInRegion(
                request.getTemplatePath(),
                request.getX(), request.getY(),
                request.getWidth(), request.getHeight()
            );
            return Result.success(Map.of(
                "found", !results.isEmpty(),
                "results", results
            ));
        } catch (Exception e) {
            return Result.fail("区域查找失败: " + e.getMessage());
        }
    }

    /**
     * 等待图像出现
     */
    @PostMapping("/wait")
    public Result waitForImage(@RequestBody WaitRequest request) {
        try {
            int timeout = request.getTimeout() > 0 ? request.getTimeout() : 5000;
            MatchResult result = imageLocatorService.waitForImage(request.getTemplatePath(), timeout);

            if (result != null) {
                return Result.success(Map.of(
                    "found", true,
                    "x", result.getX(),
                    "y", result.getY(),
                    "width", result.getWidth(),
                    "height", result.getHeight(),
                    "confidence", result.getConfidence()
                ));
            } else {
                return Result.success(Map.of("found", false));
            }
        } catch (Exception e) {
            return Result.fail("等待失败: " + e.getMessage());
        }
    }

    /**
     * 等待图像消失
     */
    @PostMapping("/wait-disappear")
    public Result waitForImageDisappear(@RequestBody WaitRequest request) {
        try {
            int timeout = request.getTimeout() > 0 ? request.getTimeout() : 5000;
            boolean disappeared = imageLocatorService.waitForImageDisappear(request.getTemplatePath(), timeout);
            return Result.success(Map.of("disappeared", disappeared));
        } catch (Exception e) {
            return Result.fail("等待失败: " + e.getMessage());
        }
    }

    /**
     * 查找并点击
     */
    @PostMapping("/find-and-click")
    public Result findAndClick(@RequestBody FindClickRequest request) {
        try {
            boolean success = imageLocatorService.findAndClick(request.getTemplatePath());
            return Result.success(Map.of("success", success));
        } catch (Exception e) {
            return Result.fail("点击失败: " + e.getMessage());
        }
    }

    /**
     * 查找并双击
     */
    @PostMapping("/find-and-double-click")
    public Result findAndDoubleClick(@RequestBody FindClickRequest request) {
        try {
            boolean success = imageLocatorService.findAndDoubleClick(request.getTemplatePath());
            return Result.success(Map.of("success", success));
        } catch (Exception e) {
            return Result.fail("双击失败: " + e.getMessage());
        }
    }

    /**
     * 查找并悬停
     */
    @PostMapping("/find-and-hover")
    public Result findAndHover(@RequestBody FindClickRequest request) {
        try {
            boolean success = imageLocatorService.findAndHover(request.getTemplatePath());
            return Result.success(Map.of("success", success));
        } catch (Exception e) {
            return Result.fail("悬停失败: " + e.getMessage());
        }
    }

    /**
     * 保存全屏截图
     */
    @PostMapping("/screenshot")
    public Result saveScreenshot(@RequestBody ScreenshotRequest request) {
        try {
            String path = imageLocatorService.saveScreenshot(request.getName());
            if (path != null) {
                return Result.success(Map.of("path", path));
            } else {
                return Result.fail("截图失败");
            }
        } catch (Exception e) {
            return Result.fail("截图失败: " + e.getMessage());
        }
    }

    /**
     * 截取区域
     */
    @PostMapping("/capture")
    public Result captureTemplate(@RequestBody CaptureRequest request) {
        try {
            String path = imageLocatorService.captureTemplate(
                request.getX(), request.getY(),
                request.getWidth(), request.getHeight(),
                request.getName()
            );
            if (path != null) {
                return Result.success(Map.of("path", path));
            } else {
                return Result.fail("截取失败");
            }
        } catch (Exception e) {
            return Result.fail("截取失败: " + e.getMessage());
        }
    }

    /**
     * 设置匹配阈值
     */
    @PostMapping("/threshold")
    public Result setThreshold(@RequestBody ThresholdRequest request) {
        try {
            imageLocatorService.setMatchThreshold(request.getThreshold());
            return Result.success(Map.of("threshold", request.getThreshold()));
        } catch (Exception e) {
            return Result.fail("设置阈值失败: " + e.getMessage());
        }
    }

    /**
     * 获取模板列表
     */
    @GetMapping("/templates")
    public Result getTemplateList() {
        try {
            List<String> templates = imageLocatorService.getTemplateList();
            return Result.success(Map.of("templates", templates));
        } catch (Exception e) {
            return Result.fail("获取模板列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/template")
    public Result deleteTemplate(@RequestParam String path) {
        try {
            boolean success = imageLocatorService.deleteTemplate(path);
            return Result.success(Map.of("deleted", success));
        } catch (Exception e) {
            return Result.fail("删除模板失败: " + e.getMessage());
        }
    }

    // ==================== 请求类 ====================

    @lombok.Data
    public static class FindRequest {
        private String templatePath;
    }

    @lombok.Data
    public static class FindInRegionRequest {
        private String templatePath;
        private int x, y, width, height;
    }

    @lombok.Data
    public static class WaitRequest {
        private String templatePath;
        private int timeout = 5000;
    }

    @lombok.Data
    public static class FindClickRequest {
        private String templatePath;
    }

    @lombok.Data
    public static class ScreenshotRequest {
        private String name;
    }

    @lombok.Data
    public static class CaptureRequest {
        private int x, y, width, height;
        private String name;
    }

    @lombok.Data
    public static class ThresholdRequest {
        private double threshold;
    }

    /**
     * 通用结果类
     */
    @lombok.Data
    public static class Result {
        private boolean success;
        private String message;
        private Object data;

        public static Result success(Object data) {
            Result r = new Result();
            r.setSuccess(true);
            r.setData(data);
            return r;
        }

        public static Result fail(String message) {
            Result r = new Result();
            r.setSuccess(false);
            r.setMessage(message);
            return r;
        }
    }
}