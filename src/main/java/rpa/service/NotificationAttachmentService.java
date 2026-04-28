package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rpa.entity.Task;
import rpa.entity.ExecutionLog;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 通知附件服务
 * <p>
 * 生成并管理通知邮件的附件，包括：
 * - 执行日志文件
 * - 屏幕截图
 * - 执行汇总报告
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAttachmentService {

    @Value("${rpa.notification.attachment.enabled:true}")
    private boolean attachmentEnabled;

    @Value("${rpa.notification.attachment.max-size:10485760}") // 10MB
    private long maxAttachmentSize;

    @Value("${rpa.notification.attachment.directory:./attachments}")
    private String attachmentDirectory;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 附件类型枚举
     */
    public enum AttachmentType {
        EXECUTION_LOG("执行日志", "log"),
        SUMMARY("执行汇总", "summary"),
        ERROR_LOG("错误日志", "error"),
        SCREENSHOT("截图", "png"),
        FULL_REPORT("完整报告", "zip");

        private final String description;
        private final String extension;

        AttachmentType(String description, String extension) {
            this.description = description;
            this.extension = extension;
        }

        public String getDescription() { return description; }
        public String getExtension() { return extension; }
    }

    /**
     * 附件信息
     */
    @lombok.Data
    public static class Attachment {
        private String fileName;
        private String filePath;
        private long fileSize;
        private String contentType;
        private byte[] content;

        public Attachment() {}

        public Attachment(String fileName, long fileSize, String contentType, byte[] content) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.content = content;
        }
    }

    /**
     * 生成任务执行日志附件
     */
    public Attachment generateExecutionLogAttachment(Task task) {
        if (!attachmentEnabled) {
            return null;
        }

        try {
            StringBuilder logContent = new StringBuilder();
            logContent.append("=".repeat(60)).append("\n");
            logContent.append("RPA任务执行日志\n");
            logContent.append("=".repeat(60)).append("\n\n");

            // 任务基本信息
            logContent.append("【任务信息】\n");
            logContent.append(String.format("任务ID：%d\n", task.getId()));
            logContent.append(String.format("任务名称：%s\n", task.getName()));
            logContent.append(String.format("任务状态：%s\n", task.getStatus()));
            logContent.append(String.format("创建时间：%s\n", task.getCreateTime()));
            logContent.append(String.format("开始时间：%s\n", task.getStartTime()));
            logContent.append(String.format("结束时间：%s\n", task.getEndTime()));

            if (task.getStartTime() != null && task.getEndTime() != null) {
                long durationMinutes = java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
                logContent.append(String.format("执行耗时：%d 分钟\n", durationMinutes));
            }

            logContent.append("\n");

            // 执行结果
            logContent.append("【执行结果】\n");
            if (task.getResultData() != null) {
                logContent.append(task.getResultData());
            } else {
                logContent.append("无执行结果数据\n");
            }
            logContent.append("\n");

            // 错误信息
            if ("failed".equals(task.getStatus()) && task.getErrorMessage() != null) {
                logContent.append("【错误信息】\n");
                logContent.append(task.getErrorMessage()).append("\n\n");
            }

            // 页脚
            logContent.append("=".repeat(60)).append("\n");
            logContent.append(String.format("报告生成时间：%s\n", java.time.LocalDateTime.now()));
            logContent.append("RPA System - 自动化运维平台\n");

            // 生成文件
            String fileName = String.format("task_%d_%s_%s.txt",
                    task.getId(),
                    task.getName().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_"),
                    LocalDate.now().format(DATE_FORMATTER));

            byte[] content = logContent.toString().getBytes("UTF-8");

            Attachment attachment = new Attachment();
            attachment.setFileName(fileName);
            attachment.setContent(content);
            attachment.setFileSize(content.length);
            attachment.setContentType("text/plain; charset=UTF-8");

            log.info("生成执行日志附件成功: taskId={}, fileName={}", task.getId(), fileName);

            return attachment;

        } catch (Exception e) {
            log.error("生成执行日志附件失败: taskId={}", task.getId(), e);
            return null;
        }
    }

    /**
     * 生成错误日志附件（仅当任务失败时）
     */
    public Attachment generateErrorLogAttachment(Task task) {
        if (!attachmentEnabled || !"failed".equals(task.getStatus())) {
            return null;
        }

        try {
            StringBuilder errorContent = new StringBuilder();
            errorContent.append("=".repeat(60)).append("\n");
            errorContent.append("RPA任务错误报告\n");
            errorContent.append("=".repeat(60)).append("\n\n");

            errorContent.append("【错误摘要】\n");
            errorContent.append(String.format("任务ID：%d\n", task.getId()));
            errorContent.append(String.format("任务名称：%s\n", task.getName()));
            errorContent.append(String.format("错误时间：%s\n", task.getEndTime()));
            errorContent.append("\n");

            errorContent.append("【错误详情】\n");
            errorContent.append(task.getErrorMessage()).append("\n\n");

            errorContent.append("【完整执行日志】\n");
            if (task.getResultData() != null) {
                errorContent.append(task.getResultData());
            }

            errorContent.append("\n").append("=".repeat(60)).append("\n");
            errorContent.append(String.format("报告生成时间：%s\n", java.time.LocalDateTime.now()));

            String fileName = String.format("error_%d_%s_%s.txt",
                    task.getId(),
                    task.getName().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_"),
                    LocalDate.now().format(DATE_FORMATTER));

            byte[] content = errorContent.toString().getBytes("UTF-8");

            Attachment attachment = new Attachment();
            attachment.setFileName(fileName);
            attachment.setContent(content);
            attachment.setFileSize(content.length);
            attachment.setContentType("text/plain; charset=UTF-8");

            log.info("生成错误日志附件成功: taskId={}", task.getId());

            return attachment;

        } catch (Exception e) {
            log.error("生成错误日志附件失败: taskId={}", task.getId(), e);
            return null;
        }
    }

    /**
     * 生成执行汇总附件（CSV格式）
     */
    public Attachment generateSummaryAttachment(Task task) {
        if (!attachmentEnabled) {
            return null;
        }

        try {
            StringBuilder csv = new StringBuilder();

            // BOM for Excel
            csv.append("\uFEFF");

            // Header
            csv.append("任务ID,任务名称,任务状态,开始时间,结束时间,执行耗时(分钟),错误信息\n");

            // Data
            csv.append(task.getId()).append(",");
            csv.append(escapeCsv(task.getName())).append(",");
            csv.append(escapeCsv(task.getStatus())).append(",");

            if (task.getStartTime() != null) {
                csv.append(task.getStartTime().toString()).append(",");
            } else {
                csv.append(",");
            }

            if (task.getEndTime() != null) {
                csv.append(task.getEndTime().toString()).append(",");
            } else {
                csv.append(",");
            }

            if (task.getStartTime() != null && task.getEndTime() != null) {
                long duration = java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
                csv.append(duration).append(",");
            } else {
                csv.append(",");
            }

            csv.append(escapeCsv(task.getErrorMessage() != null ? task.getErrorMessage() : "")).append("\n");

            String fileName = String.format("task_summary_%d_%s.csv",
                    task.getId(),
                    LocalDate.now().format(DATE_FORMATTER));

            byte[] content = csv.toString().getBytes("UTF-8");

            Attachment attachment = new Attachment();
            attachment.setFileName(fileName);
            attachment.setContent(content);
            attachment.setFileSize(content.length);
            attachment.setContentType("text/csv; charset=UTF-8");

            return attachment;

        } catch (Exception e) {
            log.error("生成汇总附件失败: taskId={}", task.getId(), e);
            return null;
        }
    }

    /**
     * 生成完整报告（ZIP格式，包含日志和错误信息）
     */
    public Attachment generateFullReport(Task task) {
        if (!attachmentEnabled) {
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            // 添加执行日志
            Attachment logAttachment = generateExecutionLogAttachment(task);
            if (logAttachment != null) {
                addToZip(zos, logAttachment.getFileName(), logAttachment.getContent());
            }

            // 添加错误日志（如果有）
            if ("failed".equals(task.getStatus())) {
                Attachment errorAttachment = generateErrorLogAttachment(task);
                if (errorAttachment != null) {
                    addToZip(zos, errorAttachment.getFileName(), errorAttachment.getContent());
                }
            }

            // 添加汇总CSV
            Attachment summaryAttachment = generateSummaryAttachment(task);
            if (summaryAttachment != null) {
                addToZip(zos, summaryAttachment.getFileName(), summaryAttachment.getContent());
            }

            zos.finish();

            String fileName = String.format("task_report_%d_%s.zip",
                    task.getId(),
                    LocalDate.now().format(DATE_FORMATTER));

            byte[] content = baos.toByteArray();

            Attachment attachment = new Attachment();
            attachment.setFileName(fileName);
            attachment.setContent(content);
            attachment.setFileSize(content.length);
            attachment.setContentType("application/zip");

            log.info("生成完整报告附件成功: taskId={}, size={}", task.getId(), content.length);

            return attachment;

        } catch (Exception e) {
            log.error("生成完整报告附件失败: taskId={}", task.getId(), e);
            return null;
        }
    }

    /**
     * 为任务生成所有需要的附件
     */
    public List<Attachment> generateAttachmentsForTask(Task task) {
        List<Attachment> attachments = new ArrayList<>();

        // 根据任务状态决定附件类型
        if ("failed".equals(task.getStatus())) {
            // 失败任务：添加执行日志和错误日志
            Attachment log = generateExecutionLogAttachment(task);
            if (log != null) attachments.add(log);

            Attachment error = generateErrorLogAttachment(task);
            if (error != null) attachments.add(error);
        } else if ("completed".equals(task.getStatus())) {
            // 成功任务：添加执行日志
            Attachment log = generateExecutionLogAttachment(task);
            if (log != null) attachments.add(log);
        }

        // 如果附件总大小超过限制，只保留日志
        long totalSize = attachments.stream().mapToLong(Attachment::getFileSize).sum();
        if (totalSize > maxAttachmentSize) {
            attachments.clear();
            Attachment log = generateExecutionLogAttachment(task);
            if (log != null) attachments.add(log);
        }

        return attachments;
    }

    /**
     * 添加文件到ZIP
     */
    private void addToZip(ZipOutputStream zos, String fileName, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        entry.setSize(content.length);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }

    /**
     * 转义CSV特殊字符
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // 如果包含逗号、引号或换行符，需要用引号包裹
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 保存附件到磁盘（可选）
     */
    public String saveToDisk(Attachment attachment) {
        try {
            Path directory = Paths.get(attachmentDirectory);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(attachment.getFileName());
            Files.write(filePath, attachment.getContent());

            log.info("附件已保存到磁盘: {}", filePath);

            return filePath.toString();

        } catch (Exception e) {
            log.error("保存附件到磁盘失败", e);
            return null;
        }
    }

    /**
     * 检查附件功能是否启用
     */
    public boolean isAttachmentEnabled() {
        return attachmentEnabled;
    }
}
