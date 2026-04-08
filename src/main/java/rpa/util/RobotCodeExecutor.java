package rpa.util;

import javax.script.*;
import java.util.concurrent.*;

/**
 * Java 代码执行引擎
 * <p>
 * 用于动态执行机器人中存储的 Java 代码
 * 支持 Groovy、Rhino 和 JavaScript 脚本引擎执行
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
public class RobotCodeExecutor {
    
    private static final ScriptEngineManager manager = new ScriptEngineManager();
    private static final ScriptEngine groovyEngine = manager.getEngineByName("groovy");
    private static final ScriptEngine jsEngine = manager.getEngineByName("JavaScript");
    private static final ScriptEngine nashornEngine = manager.getEngineByName("nashorn");
    private static final ScriptEngine rhinoEngine = manager.getEngineByName("rhino");

    static {
        System.out.println("[RobotCodeExecutor] 初始化脚本引擎...");
        System.out.println("[RobotCodeExecutor] - Groovy: " + (groovyEngine != null ? "✅" : "❌"));
        System.out.println("[RobotCodeExecutor] - Nashorn: " + (nashornEngine != null ? "✅" : "❌"));
        System.out.println("[RobotCodeExecutor] - Rhino: " + (rhinoEngine != null ? "✅" : "❌"));
        System.out.println("[RobotCodeExecutor] - JavaScript: " + (jsEngine != null ? "✅" : "❌"));
    }

    /**
     * 执行机器人代码
     * 
     * @param robotCode 机器人执行代码 (Groovy/Java/JavaScript 语法)
     * @param params 参数
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public static Object execute(String robotCode, Object... params) throws Exception {
        if (robotCode == null || robotCode.trim().isEmpty()) {
            throw new RuntimeException("机器人代码为空");
        }
        
        // 选择可用的脚本引擎 (优先级：Groovy > Nashorn > Rhino > JavaScript)
        final ScriptEngine targetEngine;
        final String engineName;
        
        if (groovyEngine != null) {
            targetEngine = groovyEngine;
            engineName = "Groovy";
        } else if (nashornEngine != null) {
            targetEngine = nashornEngine;
            engineName = "Nashorn";
        } else if (rhinoEngine != null) {
            targetEngine = rhinoEngine;
            engineName = "Rhino";
        } else if (jsEngine != null) {
            targetEngine = jsEngine;
            engineName = "JavaScript";
        } else {
            // 如果都没有，使用系统默认的 JavaScript 引擎作为 fallback
            System.out.println("[RobotCodeExecutor] 使用系统默认 JavaScript 引擎...");
            ScriptEngineManager fallbackManager = new ScriptEngineManager();
            ScriptEngine js = fallbackManager.getEngineByName("JavaScript");
            if (js != null) {
                targetEngine = js;
                engineName = "JavaScript (fallback)";
                System.out.println("[RobotCodeExecutor] ✅ 使用默认 JavaScript 引擎");
            } else {
                System.out.println("[RobotCodeExecutor] ❌ 无法获取任何脚本引擎");
                throw new RuntimeException("未找到可用的脚本引擎");
            }
        }
        
        System.out.println("[RobotCodeExecutor] 使用脚本引擎：" + engineName);
        
        // 创建执行上下文
        final Bindings bindings = targetEngine.createBindings();
        bindings.put("params", params);
        bindings.put("robotName", params.length > 0 ? params[0] : "Unknown");
        bindings.put("processName", params.length > 1 ? params[1] : "Unknown");
        bindings.put("steps", params.length > 2 ? params[2] : null);
        
        // 设置超时时间 (30 秒)
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Object> future = executor.submit(() -> {
            try {
                return targetEngine.eval(robotCode, bindings);
            } catch (ScriptException e) {
                throw new RuntimeException("脚本执行错误：" + e.getMessage() + "\n在行号：" + e.getLineNumber(), e);
            }
        });
        
        try {
            Object result = future.get(30, TimeUnit.SECONDS);
            System.out.println("[RobotCodeExecutor] 执行完成，结果：" + (result != null ? result.toString() : "null"));
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("代码执行超时 (30 秒)");
        } catch (ExecutionException e) {
            throw new RuntimeException("代码执行异常：" + e.getCause().getMessage(), e.getCause());
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * 执行机器人代码 (带返回值类型)
     * 
     * @param robotCode 机器人执行代码
     * @param returnType 返回值类型
     * @param params 参数
     * @return 执行结果
     * @throws Exception 执行异常
     */
    @SuppressWarnings("unchecked")
    public static <T> T execute(String robotCode, Class<T> returnType, Object... params) throws Exception {
        Object result = execute(robotCode, params);
        if (result == null) {
            return null;
        }
        return returnType.cast(result);
    }
}
