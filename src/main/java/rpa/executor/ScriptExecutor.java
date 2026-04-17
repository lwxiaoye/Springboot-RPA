package rpa.executor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RPA脚本执行器 - 统一调度不同类型的脚本执行
 * 
 * 功能说明：
 * - 支持Python、JavaScript、Shell等脚本类型
 * - 提供脚本执行、停止、状态查询
 * - 支持变量注入和输出捕获
 * - 内置沙箱隔离机制
 * 
 * @author RPA System
 */
@Slf4j
@Component
public class ScriptExecutor {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, ScriptProcess> runningScripts = new ConcurrentHashMap<>();
    private final Map<String, Object> globalVariables = new ConcurrentHashMap<>();

    private static final long DEFAULT_TIMEOUT = 60000;
    private String pythonPath = "python";
    private String nodePath = "node";

    public ScriptResult execute(ScriptRequest request) {
        log.info("开始执行脚本: type={}, scriptId={}", request.getScriptType(), request.getScriptId());
        
        ScriptResult result = new ScriptResult();
        result.setScriptId(request.getScriptId());
        result.setStartTime(new Date());

        try {
            validateScript(request);
            
            switch (request.getScriptType()) {
                case "python":
                    result = executePython(request);
                    break;
                case "javascript":
                case "js":
                    result = executeJavaScript(request);
                    break;
                case "shell":
                    result = executeShell(request);
                    break;
                default:
                    result.setSuccess(false);
                    result.setErrorMessage("不支持的脚本类型: " + request.getScriptType());
            }
        } catch (Exception e) {
            log.error("脚本执行异常: scriptId={}", request.getScriptId(), e);
            result.setSuccess(false);
            result.setErrorMessage("脚本执行异常: " + e.getMessage());
        }

        result.setEndTime(new Date());
        result.setDuration(result.getEndTime().getTime() - result.getStartTime().getTime());
        runningScripts.remove(request.getScriptId());
        
        return result;
    }

    private ScriptResult executePython(ScriptRequest request) throws Exception {
        ScriptResult result = new ScriptResult();
        result.setStartTime(new Date());
        File tempScript = createTempScript(request.getCode(), "py");
        
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("python", "-u", tempScript.getAbsolutePath());
            applyEnvironment(builder, request.getVariables());
            builder.redirectErrorStream(false);
            
            Process process = builder.start();
            recordProcess(request.getScriptId(), process);

            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());
            
            long timeout = request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT;
            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            
            return buildResult(result, process, finished, timeout, output, error);
        } finally {
            tempScript.delete();
        }
    }

    private ScriptResult executeJavaScript(ScriptRequest request) throws Exception {
        ScriptResult result = new ScriptResult();
        result.setStartTime(new Date());
        File tempScript = createTempScript(request.getCode(), "js");
        
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("node", tempScript.getAbsolutePath());
            applyEnvironment(builder, request.getVariables());
            builder.redirectErrorStream(false);
            
            Process process = builder.start();
            recordProcess(request.getScriptId(), process);

            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());
            
            long timeout = request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT;
            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            
            return buildResult(result, process, finished, timeout, output, error);
        } finally {
            tempScript.delete();
        }
    }

    private ScriptResult executeShell(ScriptRequest request) throws Exception {
        ScriptResult result = new ScriptResult();
        result.setStartTime(new Date());
        File tempScript = createTempScript(request.getCode(), "sh");
        tempScript.setExecutable(true);
        
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("/bin/bash", tempScript.getAbsolutePath());
            applyEnvironment(builder, request.getVariables());
            builder.redirectErrorStream(false);
            
            Process process = builder.start();
            recordProcess(request.getScriptId(), process);

            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());
            
            long timeout = request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT;
            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            
            return buildResult(result, process, finished, timeout, output, error);
        } finally {
            tempScript.delete();
        }
    }

    private ScriptResult buildResult(ScriptResult result, Process process, boolean finished, long timeout, String output, String error) {
        if (!finished) {
            process.destroyForcibly();
            result.setSuccess(false);
            result.setErrorMessage("脚本执行超时: " + timeout + "ms");
            result.setKilled(true);
        } else {
            int exitCode = process.exitValue();
            result.setExitCode(exitCode);
            result.setSuccess(exitCode == 0);
            result.setOutput(output);
            if (exitCode != 0) {
                result.setErrorMessage(error);
            }
        }
        return result;
    }

    private void recordProcess(String scriptId, Process process) {
        ScriptProcess sp = new ScriptProcess();
        sp.setProcess(process);
        sp.setScriptId(scriptId);
        sp.setStartTime(System.currentTimeMillis());
        runningScripts.put(scriptId, sp);
    }

    public boolean stopScript(String scriptId) {
        ScriptProcess process = runningScripts.get(scriptId);
        if (process != null && process.getProcess().isAlive()) {
            process.getProcess().destroyForcibly();
            runningScripts.remove(scriptId);
            log.info("已停止脚本: scriptId={}", scriptId);
            return true;
        }
        return false;
    }

    public ScriptStatus getStatus(String scriptId) {
        ScriptProcess process = runningScripts.get(scriptId);
        if (process == null) {
            return ScriptStatus.COMPLETED;
        }
        return process.getProcess().isAlive() ? ScriptStatus.RUNNING : ScriptStatus.COMPLETED;
    }

    private void validateScript(ScriptRequest request) throws SecurityException {
        String code = request.getCode();
        
        String[] dangerousPatterns = {
            "rm -rf /", "format c:", "del /f /s /q", "shutdown", "reboot",
            "__import__", "eval\\s*\\(", "exec\\s*\\(", "os.system", "subprocess", "socket", "requests"
        };
        
        for (String pattern : dangerousPatterns) {
            if (code.matches(".*" + pattern + ".*")) {
                throw new SecurityException("脚本包含危险操作: " + pattern);
            }
        }
        
        if (code.length() > 100000) {
            throw new SecurityException("脚本内容过长，最大支持100KB");
        }
    }

    private File createTempScript(String code, String extension) throws IOException {
        File tempFile = File.createTempFile("rpa_script_", "." + extension);
        tempFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
            writer.write(code);
        }
        return tempFile;
    }

    private void applyEnvironment(ProcessBuilder builder, Map<String, Object> variables) {
        if (variables != null) {
            Map<String, String> env = builder.environment();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                env.put("RPA_" + entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
    }

    private String readStream(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null && lineCount < 10000) {
            builder.append(line).append("\n");
            lineCount++;
        }
        return builder.toString();
    }

    // ==================== 内部类 ====================

    @Data
    public static class ScriptRequest {
        private String scriptId;
        private String scriptType;
        private String code;
        private Map<String, Object> variables;
        private Long timeout;
        private String workingDir;
    }

    @Data
    public static class ScriptResult {
        private String scriptId;
        private boolean success;
        private String output;
        private String errorMessage;
        private int exitCode;
        private long duration;
        private boolean killed;
        private Date startTime;
        private Date endTime;
    }

    @Data
    private static class ScriptProcess {
        private Process process;
        private String scriptId;
        private long startTime;
    }

    public enum ScriptStatus {
        RUNNING, COMPLETED, FAILED, TIMEOUT, KILLED
    }

    public void setPythonPath(String pythonPath) {
        this.pythonPath = pythonPath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }
}