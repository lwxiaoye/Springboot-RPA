package rpa.config;

import rpa.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 审计日志切面
 * <p>
 * 自动拦截带有 @AuditLog 注解的方法，记录审计日志。
 * </p>
 */
@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    public AuditLogAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(rpa.config.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        rpa.config.AuditLog auditAnnotation = method.getAnnotation(rpa.config.AuditLog.class);

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        // 记录开始时间
        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;

        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            // 记录审计日志
            try {
                String description = auditAnnotation.description();
                // 简单的参数替换（实际可以使用SpEL更复杂的功能）
                if (description.contains("{0}") && args.length > 0) {
                    description = String.format(description, args);
                }

                auditLogService.log(
                    auditAnnotation.module(),
                    auditAnnotation.action(),
                    description,
                    auditAnnotation.riskLevel()
                );
            } catch (Exception e) {
                // 忽略审计日志记录失败，不要影响主业务
            }
        }
    }
}