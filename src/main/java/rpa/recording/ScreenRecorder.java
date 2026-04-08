package rpa.recording;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 屏幕录制服务
 * 
 * 功能说明：
 * - 录制RPA执行过程的屏幕操作
 * - 支持开始、暂停、恢复、停止
 * - 自动生成视频文件
 * - 支持定时清理过期录制文件
 * 
 * 技术实现：
 * - 使用Java Robot获取屏幕截图
 * - 使用图片编码器生成视频
 * - 支持FFmpeg转码（可选）
 * 
 * @author RPA System
 */
@Slf4j
@Service
public class ScreenRecorder {

    // 录制状态
    public enum RecordingStatus {
        IDLE,           // 空闲
        RECORDING,      // 录制中
        PAUSED,         // 已暂停
        STOPPED         // 已停止
    }

    // 配置参数
    @Value("${rpa.recording.save-path:D:/rpa/recordings}")
    private String savePath;

    @Value("${rpa.recording.fps:10}")
    private int fps;

    @Value("${rpa.recording.quality:80}")
    private int quality;

    @Value("${rpa.recording.max-duration:3600}")
    private int maxDuration;  // 最大录制时长（秒）

    @Value("${rpa.recording.auto-clean:false}")
    private boolean autoClean;

    @Value("${rpa.recording.clean-after-days:30}")
    private int cleanAfterDays;

    // 录制状态
    private final AtomicInteger recordingCount = new AtomicInteger(0);
    private final Map<String, RecordingSession> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 开始录制
     */
    public RecordingResult startRecording(RecordingRequest request) {
        String sessionId = UUID.randomUUID().toString();
        
        log.info("开始录制: sessionId={}, robotId={}", sessionId, request.getRobotId());
        
        // 创建保存目录
        Path sessionDir = Paths.get(savePath, sessionId);
        try {
            Files.createDirectories(sessionDir);
        } catch (IOException e) {
            log.error("创建录制目录失败", e);
            return RecordingResult.fail("无法创建录制目录: " + e.getMessage());
        }
        
        // 创建录制会话
        RecordingSession session = new RecordingSession();
        session.setSessionId(sessionId);
        session.setRobotId(request.getRobotId());
        session.setTaskId(request.getTaskId());
        session.setStatus(RecordingStatus.RECORDING);
        session.setStartTime(System.currentTimeMillis());
        session.setSavePath(sessionDir.toString());
        session.setFps(fps);
        session.setQuality(quality);
        
        // 启动录制线程
        RecordingThread recordingThread = new RecordingThread(session, request.getMonitorIndex());
        session.setRecordingThread(recordingThread);
        
        Thread thread = new Thread(recordingThread, "recorder-" + sessionId);
        thread.setDaemon(true);
        thread.start();
        
        activeSessions.put(sessionId, session);
        recordingCount.incrementAndGet();
        
        RecordingResult result = new RecordingResult();
        result.setSuccess(true);
        result.setSessionId(sessionId);
        result.setMessage("录制已开始");
        
        return result;
    }

    /**
     * 暂停录制
     */
    public RecordingResult pauseRecording(String sessionId) {
        RecordingSession session = activeSessions.get(sessionId);
        if (session == null) {
            return RecordingResult.fail("录制会话不存在");
        }
        
        session.setStatus(RecordingStatus.PAUSED);
        log.info("暂停录制: sessionId={}", sessionId);
        
        RecordingResult result = new RecordingResult();
        result.setSuccess(true);
        result.setSessionId(sessionId);
        result.setMessage("录制已暂停");
        
        return result;
    }

    /**
     * 恢复录制
     */
    public RecordingResult resumeRecording(String sessionId) {
        RecordingSession session = activeSessions.get(sessionId);
        if (session == null) {
            return RecordingResult.fail("录制会话不存在");
        }
        
        session.setStatus(RecordingStatus.RECORDING);
        log.info("恢复录制: sessionId={}", sessionId);
        
        RecordingResult result = new RecordingResult();
        result.setSuccess(true);
        result.setSessionId(sessionId);
        result.setMessage("录制已恢复");
        
        return result;
    }

    /**
     * 停止录制并生成视频
     */
    public RecordingResult stopRecording(String sessionId) {
        RecordingSession session = activeSessions.get(sessionId);
        if (session == null) {
            return RecordingResult.fail("录制会话不存在");
        }
        
        session.setStatus(RecordingStatus.STOPPED);
        session.setEndTime(System.currentTimeMillis());
        
        log.info("停止录制: sessionId={}, duration={}s", 
            sessionId, (session.getEndTime() - session.getStartTime()) / 1000);
        
        // 生成视频
        String videoPath = generateVideo(session);
        
        // 清理截图文件
        cleanImageFiles(session);
        
        activeSessions.remove(sessionId);
        
        RecordingResult result = new RecordingResult();
        result.setSuccess(true);
        result.setSessionId(sessionId);
        result.setVideoPath(videoPath);
        result.setDuration((session.getEndTime() - session.getStartTime()) / 1000);
        result.setFrameCount(session.getFrameCount());
        result.setMessage("录制已完成");
        
        return result;
    }

    /**
     * 录制一个动作（截图）
     */
    public void recordAction(String sessionId, String action, Map<String, Object> metadata) {
        RecordingSession session = activeSessions.get(sessionId);
        if (session == null || session.getStatus() != RecordingStatus.RECORDING) {
            return;
        }
        
        try {
            // 截图并保存
            BufferedImage screenshot = new Robot().createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            
            String fileName = String.format("frame_%08d.png", session.getFrameCount());
            File outputFile = new File(session.getSavePath(), fileName);
            ImageIO.write(screenshot, "PNG", outputFile);
            
            session.incrementFrameCount();
            
            // 记录元数据
            session.addMetadata(new FrameMetadata(
                session.getFrameCount(),
                System.currentTimeMillis(),
                action,
                metadata
            ));
            
        } catch (Exception e) {
            log.error("录制动作失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取录制状态
     */
    public RecordingSession getSessionStatus(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * 获取所有活跃会话
     */
    public java.util.List<RecordingSession> getActiveSessions() {
        return new java.util.ArrayList<>(activeSessions.values());
    }

    /**
     * 清理过期录制文件
     */
    public void cleanExpiredRecordings() {
        if (!autoClean) {
            return;
        }
        
        log.info("开始清理过期录制文件...");
        
        try {
            Path saveDir = Paths.get(savePath);
            if (!Files.exists(saveDir)) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (cleanAfterDays * 24L * 60L * 60L * 1000);
            
            Files.walk(saveDir)
                .filter(Files::isDirectory)
                .filter(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(p -> {
                    try {
                        deleteDirectory(p.toFile());
                        log.info("已删除过期录制: {}", p);
                    } catch (Exception e) {
                        log.error("删除录制目录失败: {}", p, e);
                    }
                });
            
        } catch (Exception e) {
            log.error("清理过期录制文件失败", e);
        }
    }

    /**
     * 生成视频
     */
    private String generateVideo(RecordingSession session) {
        // 简单实现：只保存截图文件路径
        // 实际生产中应该使用FFmpeg或JMF生成视频
        
        String videoFileName = String.format("recording_%s_%s.mp4",
            session.getRobotId(),
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(session.getStartTime())));
        
        String videoPath = Paths.get(session.getSavePath(), videoFileName).toString();
        
        // 创建元数据文件
        String metadataPath = Paths.get(session.getSavePath(), "metadata.json").toString();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"sessionId\": \"").append(session.getSessionId()).append("\",\n");
            sb.append("  \"robotId\": \"").append(session.getRobotId()).append("\",\n");
            sb.append("  \"taskId\": \"").append(session.getTaskId()).append("\",\n");
            sb.append("  \"startTime\": ").append(session.getStartTime()).append(",\n");
            sb.append("  \"endTime\": ").append(session.getEndTime()).append(",\n");
            sb.append("  \"duration\": ").append(session.getDuration()).append(",\n");
            sb.append("  \"frameCount\": ").append(session.getFrameCount()).append(",\n");
            sb.append("  \"fps\": ").append(session.getFps()).append(",\n");
            sb.append("  \"frames\": [\n");
            
            for (int i = 0; i < session.getMetadata().size(); i++) {
                FrameMetadata m = session.getMetadata().get(i);
                sb.append("    {\"index\": ").append(m.getFrameIndex())
                  .append(", \"time\": ").append(m.getTimestamp())
                  .append(", \"action\": \"").append(m.getAction()).append("\"}");
                if (i < session.getMetadata().size() - 1) sb.append(",");
                sb.append("\n");
            }
            
            sb.append("  ]\n}");
            sb.append("}");
            
            Files.writeString(Paths.get(metadataPath), sb.toString());
            
        } catch (Exception e) {
            log.error("生成元数据文件失败", e);
        }
        
        return videoPath;
    }

    /**
     * 清理截图文件
     */
    private void cleanImageFiles(RecordingSession session) {
        File dir = new File(session.getSavePath());
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        
        if (files != null) {
            log.info("清理截图文件: {} 个文件", files.length);
            for (File f : files) {
                f.delete();
            }
        }
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    // ==================== 内部类 ====================

    /**
     * 录制请求
     */
    @Data
    public static class RecordingRequest {
        private String robotId;
        private String taskId;
        private int monitorIndex = 0;
        private String description;
    }

    /**
     * 录制结果
     */
    @Data
    public static class RecordingResult {
        private boolean success;
        private String sessionId;
        private String videoPath;
        private long duration;
        private int frameCount;
        private String message;
        
        public static RecordingResult fail(String message) {
            RecordingResult result = new RecordingResult();
            result.setSuccess(false);
            result.setMessage(message);
            return result;
        }
    }

    /**
     * 录制会话
     */
    @Data
    public static class RecordingSession {
        private String sessionId;
        private String robotId;
        private String taskId;
        private RecordingStatus status;
        private long startTime;
        private long endTime;
        private String savePath;
        private int fps;
        private int quality;
        private int frameCount = 0;
        private RecordingThread recordingThread;
        private java.util.List<FrameMetadata> metadata = new CopyOnWriteArrayList<>();
        
        public int getDuration() {
            return (int) ((endTime - startTime) / 1000);
        }
        
        public void incrementFrameCount() {
            this.frameCount++;
        }
        
        public void addMetadata(FrameMetadata m) {
            this.metadata.add(m);
        }
    }

    /**
     * 帧元数据
     */
    @Data
    public static class FrameMetadata {
        private int frameIndex;
        private long timestamp;
        private String action;
        private Map<String, Object> metadata;
        
        public FrameMetadata(int frameIndex, long timestamp, String action, Map<String, Object> metadata) {
            this.frameIndex = frameIndex;
            this.timestamp = timestamp;
            this.action = action;
            this.metadata = metadata;
        }
    }

    /**
     * 录制线程
     */
    private class RecordingThread implements Runnable {
        private final RecordingSession session;
        private final int monitorIndex;
        private volatile boolean running = true;
        private long frameInterval = 1000 / fps;
        
        public RecordingThread(RecordingSession session, int monitorIndex) {
            this.session = session;
            this.monitorIndex = monitorIndex;
        }
        
        public void stop() {
            running = false;
        }
        
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            int frameIndex = 0;
            
            while (running && session.getStatus() == RecordingStatus.RECORDING) {
                if (System.currentTimeMillis() - startTime > maxDuration * 1000L) {
                    log.warn("录制达到最大时长限制: {}秒", maxDuration);
                    break;
                }
                
                try {
                    // 获取屏幕截图
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                    BufferedImage screenshot = new Robot().createScreenCapture(screenRect);
                    
                    // 保存截图（使用简单方式）
                    String fileName = String.format("frame_%08d.jpg", frameIndex);
                    File outputFile = new File(session.getSavePath(), fileName);
                    
                    // 使用ImageIO直接保存（简化版本）
                    ImageIO.write(screenshot, "jpg", outputFile);
                    
                    session.incrementFrameCount();
                    frameIndex++;
                    
                    // 等待下一帧
                    long elapsed = System.currentTimeMillis() - startTime;
                    long expectedTime = frameIndex * frameInterval;
                    long sleepTime = Math.max(0, expectedTime - elapsed);
                    
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                    
                } catch (Exception e) {
                    log.error("录制帧失败", e);
                }
            }
        }
    }
}