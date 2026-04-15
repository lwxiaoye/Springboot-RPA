package rpa.config;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * <p>
 * 标注需要记录审计日志的方法，自动采集操作信息。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作模块
     */
    String module() default "SYSTEM";

    /**
     * 操作类型
     */
    String action() default "OPERATION";

    /**
     * 操作描述（支持SpEL表达式）
     */
    String description() default "";

    /**
     * 风险等级：low-低，medium-中，high-高
     */
    String riskLevel() default "low";

    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean logResult() default false;
}