package rpa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import rpa.service.SystemConfigService;

/**
 * 系统数据初始化器
 * <p>
 * 在应用启动时初始化默认配置数据。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SystemConfigService systemConfigService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化系统配置...");
        try {
            systemConfigService.initDefaultConfigs();
            log.info("系统配置初始化完成");
        } catch (Exception e) {
            log.error("系统配置初始化失败", e);
        }
    }
}
