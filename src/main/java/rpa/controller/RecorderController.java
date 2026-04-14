package rpa.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.recording.OperationRecorder;
import rpa.recording.OperationRecorder.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * RPA操作录制控制器
 *
 * 提供录制器的REST API接口：
 * - 启动/停止/暂停录制
 * - 获取录制状态
 * - 捕获屏幕截图
 * - 生成流程脚本
 *
 * @author RPA System
 */
@Slf4j
@RestController
@RequestMapping("/api/recorder")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class RecorderController {

    private final OperationRecorder operationRecorder;

    // ==================== 录制控制接口 ====================

    /**
     * 开始录制
     */
    @PostMapping("/start")
    public Result startRecording(@RequestBody RecordRequest request) {
        try {
            log.info("开始录制: mode={}, targetApp={}", request.getMode(), request.getTargetApp());
            RecordResult result = operationRecorder.startRecording(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("开始录制失败", e);
            return Result.fail("开始录制失败: " + e.getMessage());
        }
    }

    /**
     * 停止录制
     */
    @PostMapping("/stop")
    public Result stopRecording(@RequestParam String sessionId) {
        try {
            log.info("停止录制: sessionId={}", sessionId);
            RecordResult result = operationRecorder.stopRecording(sessionId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("停止录制失败", e);
            return Result.fail("停止录制失败: " + e.getMessage());
        }
    }

    /**
     * 暂停录制
     */
    @PostMapping("/pause")
    public Result pauseRecording(@RequestParam String sessionId) {
        try {
            RecordResult result = operationRecorder.pauseRecording(sessionId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail("暂停录制失败: " + e.getMessage());
        }
    }

    /**
     * 恢复录制
     */
    @PostMapping("/resume")
    public Result resumeRecording(@RequestParam String sessionId) {
        try {
            RecordResult result = operationRecorder.resumeRecording(sessionId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail("恢复录制失败: " + e.getMessage());
        }
    }

    /**
     * 获取录制状态
     */
    @GetMapping("/status")
    public Result getStatus(@RequestParam String sessionId) {
        try {
            RecordSession session = operationRecorder.getSessionStatus(sessionId);
            return Result.success(session);
        } catch (Exception e) {
            return Result.fail("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有活跃会话
     */
    @GetMapping("/sessions")
    public Result getActiveSessions() {
        try {
            List<RecordSession> sessions = operationRecorder.getActiveSessions();
            return Result.success(sessions);
        } catch (Exception e) {
            return Result.fail("获取会话列表失败: " + e.getMessage());
        }
    }

    // ==================== 屏幕捕获接口 ====================

    /**
     * 捕获全屏截图
     */
    @GetMapping("/screenshot/full")
    public Result captureFullScreen() {
        try {
            BufferedImage image = new Robot().createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

            // 保存到临时文件
            String fileName = "screenshot_" + System.currentTimeMillis() + ".png";
            Path savePath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
            ImageIO.write(image, "PNG", savePath.toFile());

            return Result.success(Map.of(
                "path", savePath.toString(),
                "width", image.getWidth(),
                "height", image.getHeight()
            ));
        } catch (Exception e) {
            log.error("截图失败", e);
            return Result.fail("截图失败: " + e.getMessage());
        }
    }

    /**
     * 捕获区域截图
     */
    @PostMapping("/screenshot/region")
    public Result captureRegion(@RequestBody RegionRequest request) {
        try {
            Rectangle rect = new Rectangle(
                request.getX(), request.getY(),
                request.getWidth(), request.getHeight()
            );

            BufferedImage image = new Robot().createScreenCapture(rect);

            String fileName = "region_" + System.currentTimeMillis() + ".png";
            Path savePath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
            ImageIO.write(image, "PNG", savePath.toFile());

            return Result.success(Map.of(
                "path", savePath.toString(),
                "width", image.getWidth(),
                "height", image.getHeight()
            ));
        } catch (Exception e) {
            log.error("区域截图失败", e);
            return Result.fail("区域截图失败: " + e.getMessage());
        }
    }

    /**
     * 获取屏幕尺寸
     */
    @GetMapping("/screen/size")
    public Result getScreenSize() {
        try {
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            return Result.success(Map.of(
                "width", size.getWidth(),
                "height", size.getHeight()
            ));
        } catch (Exception e) {
            return Result.fail("获取屏幕尺寸失败: " + e.getMessage());
        }
    }

    /**
     * 获取屏幕列表（多显示器）
     */
    @GetMapping("/screen/monitors")
    public Result getMonitors() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();

            List<Map<String, Object>> monitors = new ArrayList<>();
            for (int i = 0; i < screens.length; i++) {
                GraphicsDevice screen = screens[i];
                GraphicsConfiguration[] configs = screen.getConfigurations();
                for (GraphicsConfiguration config : configs) {
                    Rectangle bounds = config.getBounds();
                    monitors.add(Map.of(
                        "index", i,
                        "width", bounds.width,
                        "height", bounds.height,
                        "x", bounds.x,
                        "y", bounds.y
                    ));
                }
            }

            return Result.success(monitors);
        } catch (Exception e) {
            return Result.fail("获取显示器列表失败: " + e.getMessage());
        }
    }

    // ==================== 操作记录接口 ====================

    /**
     * 记录鼠标操作
     */
    @PostMapping("/operation/mouse")
    public Result recordMouseOperation(@RequestBody MouseOperationRequest request) {
        try {
            operationRecorder.recordMouseOperation(
                request.getSessionId(),
                request.getX(),
                request.getY(),
                request.getAction(),
                request.getElementInfo()
            );
            return Result.success(Map.of("recorded", true));
        } catch (Exception e) {
            return Result.fail("记录鼠标操作失败: " + e.getMessage());
        }
    }

    /**
     * 记录键盘操作
     */
    @PostMapping("/operation/keyboard")
    public Result recordKeyboardOperation(@RequestBody KeyboardOperationRequest request) {
        try {
            operationRecorder.recordKeyboardOperation(
                request.getSessionId(),
                request.getKey(),
                request.getAction(),
                request.isCtrl(),
                request.isAlt(),
                request.isShift()
            );
            return Result.success(Map.of("recorded", true));
        } catch (Exception e) {
            return Result.fail("记录键盘操作失败: " + e.getMessage());
        }
    }

    /**
     * 添加标注点
     */
    @PostMapping("/anchor")
    public Result addAnchor(@RequestBody AnchorRequest request) {
        try {
            operationRecorder.addAnchor(
                request.getSessionId(),
                request.getX(),
                request.getY(),
                request.getName()
            );
            return Result.success(Map.of("added", true));
        } catch (Exception e) {
            return Result.fail("添加标注点失败: " + e.getMessage());
        }
    }

    /**
     * 导入脚本到流程
     */
    @PostMapping("/import")
    public Result importToProcess(@RequestBody ImportRequest request) {
        try {
            String result = operationRecorder.importToProcess(
                request.getScriptPath(),
                request.getProcessId()
            );
            return Result.success(Map.of("message", result));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    // ==================== 清理接口 ====================

    /**
     * 清理过期文件
     */
    @PostMapping("/clean")
    public Result cleanExpiredFiles() {
        try {
            operationRecorder.cleanExpiredFiles();
            return Result.success(Map.of("cleaned", true));
        } catch (Exception e) {
            return Result.fail("清理失败: " + e.getMessage());
        }
    }

    // ==================== 内部类 ====================

    @Data
    public static class RegionRequest {
        private int x;
        private int y;
        private int width;
        private int height;
    }

    @Data
    public static class MouseOperationRequest {
        private String sessionId;
        private int x;
        private int y;
        private String action;      // click, doubleClick, rightClick
        private String elementInfo; // 元素信息
    }

    @Data
    public static class KeyboardOperationRequest {
        private String sessionId;
        private String key;
        private String action;      // keyDown, keyUp, type
        private boolean ctrl;
        private boolean alt;
        private boolean shift;
    }

    @Data
    public static class AnchorRequest {
        private String sessionId;
        private int x;
        private int y;
        private String name;
    }

    @Data
    public static class ImportRequest {
        private String scriptPath;
        private String processId;
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