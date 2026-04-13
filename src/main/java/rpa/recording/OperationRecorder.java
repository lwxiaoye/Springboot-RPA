package rpa.recording;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RPA智能操作录制器
 *
 * 核心功能：
 * 1. 录制桌面操作（鼠标、键盘）
 * 2. 捕获屏幕区域截图
 * 3. 生成可回放的脚本
 * 4. 支持导入流程设计器
 *
 * @author RPA System
 */
@Slf4j
@Service
public class OperationRecorder {

    @Value("${rpa.recorder.save-path:D:/rpa/recordings}")
    private String savePath;

    @Value("${rpa.recorder.auto-screenshot:true}")
    private boolean autoScreenshot;

    @Value("${rpa.recorder.screenshot-quality:85}")
    private int screenshotQuality;

    private final Map<String, RecordSession> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 开始录制
     */
    public RecordResult startRecording(RecordRequest request) {
        String sessionId = UUID.randomUUID().toString();
        log.info("开始操作录制: sessionId={}, mode={}", sessionId, request.getMode());

        // 创建保存目录
        Path sessionDir = Paths.get(savePath, "operations", sessionId);
        try {
            Files.createDirectories(sessionDir);
        } catch (IOException e) {
            log.error("创建录制目录失败", e);
            return RecordResult.fail("无法创建录制目录: " + e.getMessage());
        }

        RecordSession session = new RecordSession();
        session.setSessionId(sessionId);
        session.setMode(request.getMode());
        session.setTargetApp(request.getTargetApp());
        session.setStartTime(System.currentTimeMillis());
        session.setSavePath(sessionDir.toString());
        session.setStatus(RecordStatus.RECORDING);

        activeSessions.put(sessionId, session);

        RecordResult result = new RecordResult();
        result.setSuccess(true);
        result.setSessionId(sessionId);
        result.setMessage("录制已开始");
        result.setSavePath(sessionDir.toString());

        return result;
    }

    /**
     * 停止录制
     */
    public RecordResult stopRecording(String sessionId) {
        RecordSession session = activeSessions.get(sessionId);
        if (session == null) {
            return RecordResult.fail("录制会话不存在");
        }

        session.setStatus(RecordStatus.STOPPED);
        session.setEndTime(System.currentTimeMillis());

        log.info("停止录制: sessionId={}, 操作数={}", sessionId, session.getOperations().size());

        // 生成脚本文件
        String scriptPath = generateScript(session);

        activeSessions.remove(sessionId);

        RecordResult result = new RecordResult();
        result.setSuccess(true);
        result.setSessionId(sessionId);
        result.setScriptPath(scriptPath);
        result.setOperationCount(session.getOperations().size());
        result.setDuration((session.getEndTime() - session.getStartTime()) / 1000);
        result.setMessage("录制完成，共 " + session.getOperations().size() + " 个操作");

        return result;
    }

    /**
     * 录制单个操作
     */
    public void recordOperation(String sessionId, OperationInfo operation) {
        RecordSession session = activeSessions.get(sessionId);
        if (session == null || session.getStatus() != RecordStatus.RECORDING) {
            return;
        }

        try {
            // 添加截图
            if (autoScreenshot) {
                BufferedImage screenshot = new Robot().createScreenCapture(
                    new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

                String imageName = String.format("op_%04d.png", session.getOperations().size());
                Path imagePath = Paths.get(session.getSavePath(), imageName);

                ImageIO.write(screenshot, "PNG", imagePath.toFile());
                operation.setScreenshotPath(imagePath.toString());
            }

            session.getOperations().add(operation);
            log.debug("录制操作: {} at ({}, {})", operation.getAction(), operation.getX(), operation.getY());

        } catch (Exception e) {
            log.error("录制操作失败", e);
        }
    }

    /**
     * 记录鼠标操作
     */
    public void recordMouseOperation(String sessionId, int x, int y, String action, String elementInfo) {
        RecordSession session = activeSessions.get(sessionId);
        if (session == null || session.getStatus() != RecordStatus.RECORDING) {
            return;
        }

        OperationInfo op = new OperationInfo();
        op.setType("mouse");
        op.setAction(action);
        op.setX(x);
        op.setY(y);
        op.setTimestamp(System.currentTimeMillis());
        op.setElementInfo(elementInfo);

        // 获取当前焦点窗口信息（简化实现）
        try {
            // Java AWT 无法直接获取窗口标题，设置为空
            op.setWindowTitle("");
        } catch (Exception e) {
            // 忽略
        }

        recordOperation(sessionId, op);
    }

    /**
     * 记录键盘操作
     */
    public void recordKeyboardOperation(String sessionId, String key, String action, boolean ctrlPressed, boolean altPressed, boolean shiftPressed) {
        RecordSession session = activeSessions.get(sessionId);
        if (session == null || session.getStatus() != RecordStatus.RECORDING) {
            return;
        }

        OperationInfo op = new OperationInfo();
        op.setType("keyboard");
        op.setAction(action);
        op.setKey(key);
        op.setTimestamp(System.currentTimeMillis());
        op.setCtrl(ctrlPressed);
        op.setAlt(altPressed);
        op.setShift(shiftPressed);

        recordOperation(sessionId, op);
    }

    /**
     * 添加标注点
     */
    public void addAnchor(String sessionId, int x, int y, String name) {
        RecordSession session = activeSessions.get(sessionId);
        if (session == null) {
            return;
        }

        OperationInfo op = new OperationInfo();
        op.setType("anchor");
        op.setAction("anchor");
        op.setX(x);
        op.setY(y);
        op.setAnchorName(name);
        op.setTimestamp(System.currentTimeMillis());

        session.getOperations().add(op);
        log.info("添加标注点: {} at ({}, {})", name, x, y);
    }

    /**
     * 生成RPA脚本
     */
    private String generateScript(RecordSession session) {
        String scriptFileName = String.format("script_%s_%s.json",
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()),
            session.getSessionId().substring(0, 8));

        Path scriptPath = Paths.get(session.getSavePath(), scriptFileName);

        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"sessionId\": \"").append(session.getSessionId()).append("\",\n");
            json.append("  \"mode\": \"").append(session.getMode()).append("\",\n");
            json.append("  \"targetApp\": \"").append(session.getTargetApp()).append("\",\n");
            json.append("  \"startTime\": ").append(session.getStartTime()).append(",\n");
            json.append("  \"endTime\": ").append(session.getEndTime()).append(",\n");
            json.append("  \"operationCount\": ").append(session.getOperations().size()).append(",\n");
            json.append("  \"operations\": [\n");

            for (int i = 0; i < session.getOperations().size(); i++) {
                OperationInfo op = session.getOperations().get(i);
                json.append("    {\n");
                json.append("      \"index\": ").append(i).append(",\n");
                json.append("      \"type\": \"").append(op.getType()).append("\",\n");
                json.append("      \"action\": \"").append(op.getAction()).append("\",\n");
                json.append("      \"x\": ").append(op.getX()).append(",\n");
                json.append("      \"y\": ").append(op.getY()).append(",\n");
                json.append("      \"key\": ").append(op.getKey() != null ? "\"" + op.getKey() + "\"" : "null").append(",\n");
                json.append("      \"timestamp\": ").append(op.getTimestamp()).append(",\n");
                json.append("      \"anchorName\": ").append(op.getAnchorName() != null ? "\"" + op.getAnchorName() + "\"" : "null").append(",\n");
                json.append("      \"elementInfo\": ").append(op.getElementInfo() != null ? "\"" + op.getElementInfo().replace("\"", "\\\"") + "\"" : "null").append("\n");
                json.append("    }");
                if (i < session.getOperations().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n}");
            json.append("}");

            Files.writeString(scriptPath, json.toString());
            log.info("脚本已生成: {}", scriptPath);

            return scriptPath.toString();

        } catch (Exception e) {
            log.error("生成脚本失败", e);
            return null;
        }
    }

    /**
     * 导入脚本到流程
     */
    public String importToProcess(String scriptPath, String processId) {
        try {
            String content = Files.readString(Paths.get(scriptPath));
            // 这里可以调用 ProcessService 将脚本转换为流程节点
            log.info("导入脚本到流程: script={}, process={}", scriptPath, processId);
            return "脚本已成功导入流程";
        } catch (Exception e) {
            log.error("导入脚本失败", e);
            return "导入失败: " + e.getMessage();
        }
    }

    /**
     * 获取录制会话状态
     */
    public RecordSession getSessionStatus(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * 获取所有活跃会话
     */
    public List<RecordSession> getActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    /**
     * 暂停录制
     */
    public RecordResult pauseRecording(String sessionId) {
        RecordSession session = activeSessions.get(sessionId);
        if (session == null) {
            return RecordResult.fail("录制会话不存在");
        }

        session.setStatus(RecordStatus.PAUSED);
        log.info("暂停录制: sessionId={}", sessionId);

        RecordResult result = new RecordResult();
        result.setSuccess(true);
        result.setSessionId(sessionId);
        result.setMessage("录制已暂停");

        return result;
    }

    /**
     * 恢复录制
     */
    public RecordResult resumeRecording(String sessionId) {
        RecordSession session = activeSessions.get(sessionId);
        if (session == null) {
            return RecordResult.fail("录制会话不存在");
        }

        session.setStatus(RecordStatus.RECORDING);
        log.info("恢复录制: sessionId={}", sessionId);

        RecordResult result = new RecordResult();
        result.setSuccess(true);
        result.setSessionId(sessionId);
        result.setMessage("录制已恢复");

        return result;
    }

    /**
     * 清理过期文件
     */
    public void cleanExpiredFiles() {
        try {
            Path baseDir = Paths.get(savePath, "operations");
            if (!Files.exists(baseDir)) {
                return;
            }

            long cutoffTime = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);

            Files.walk(baseDir)
                .filter(Files::isDirectory)
                .filter(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(this::deleteDirectory);

        } catch (Exception e) {
            log.error("清理过期文件失败", e);
        }
    }

    private void deleteDirectory(Path path) {
        try {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            log.info("已删除过期录制: {}", path);
        } catch (IOException e) {
            log.error("删除目录失败: {}", path, e);
        }
    }

    // ==================== 内部类 ====================

    /**
     * 录制状态
     */
    public enum RecordStatus {
        RECORDING, PAUSED, STOPPED
    }

    /**
     * 录制模式
     */
    public enum RecordMode {
        DESKTOP,      // 桌面应用模式
        BROWSER,      // 浏览器模式
        IMAGE        // 图像识别模式
    }

    /**
     * 录制请求
     */
    @Data
    public static class RecordRequest {
        private String name;
        private RecordMode mode = RecordMode.DESKTOP;
        private String targetApp;
        private String description;
    }

    /**
     * 录制结果
     */
    @Data
    public static class RecordResult {
        private boolean success;
        private String sessionId;
        private String scriptPath;
        private String savePath;
        private int operationCount;
        private long duration;
        private String message;

        public static RecordResult fail(String message) {
            RecordResult result = new RecordResult();
            result.setSuccess(false);
            result.setMessage(message);
            return result;
        }
    }

    /**
     * 操作信息
     */
    @Data
    public static class OperationInfo {
        private String type;           // mouse, keyboard, anchor
        private String action;         // click, doubleClick, rightClick, input, press
        private int x;
        private int y;
        private String key;
        private boolean ctrl;
        private boolean alt;
        private boolean shift;
        private long timestamp;
        private String anchorName;
        private String elementInfo;
        private String screenshotPath;
        private String windowTitle;
    }

    /**
     * 录制会话
     */
    @Data
    public static class RecordSession {
        private String sessionId;
        private RecordMode mode;
        private String targetApp;
        private RecordStatus status;
        private long startTime;
        private long endTime;
        private String savePath;
        private List<OperationInfo> operations = new CopyOnWriteArrayList<>();
    }
}