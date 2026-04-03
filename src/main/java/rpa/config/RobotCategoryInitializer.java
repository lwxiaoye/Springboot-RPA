package rpa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rpa.entity.RobotCategory;
import rpa.repository.RobotCategoryRepository;

import java.util.Arrays;
import java.util.List;

/**
 * 机器人分类初始化器
 * <p>
 * 在应用启动时自动创建默认的机器人分类。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RobotCategoryInitializer implements CommandLineRunner {

    private final RobotCategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        // 默认的4种机器人分类
        List<RobotCategory> defaultCategories = Arrays.asList(
            createCategory("DATA_COLLECT", "数据采集"),
            createCategory("DATA_PARSE", "数据解析"),
            createCategory("DATA_PROCESS", "数据加工"),
            createCategory("DATA_OUTPUT", "数据落库")
        );

        for (RobotCategory category : defaultCategories) {
            // 检查是否已存在
            if (categoryRepository.findByCode(category.getCode()).isEmpty()) {
                categoryRepository.save(category);
                log.info("创建机器人分类: {} - {}", category.getCode(), category.getName());
            }
        }
        
        log.info("机器人分类初始化完成");
    }

    private RobotCategory createCategory(String code, String name) {
        RobotCategory category = new RobotCategory();
        category.setCode(code);
        category.setName(name);
        return category;
    }
}