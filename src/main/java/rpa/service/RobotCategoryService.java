package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.RobotCategory;
import rpa.repository.RobotCategoryRepository;
import java.util.List;

/**
 * 机器人分类服务类
 */
@Service
@RequiredArgsConstructor
public class RobotCategoryService {

    private final RobotCategoryRepository repository;

    public List<RobotCategory> findAll() {
        return repository.findAll();
    }

    public RobotCategory create(String name, String code) {
        if (repository.existsByCode(code)) {
            throw new RuntimeException("分类编码已存在，请使用其他编码");
        }
        RobotCategory category = new RobotCategory();
        category.setName(name);
        category.setCode(code);
        return repository.save(category);
    }

    public RobotCategory update(String code, String name) {
        RobotCategory category = repository.findByCode(code)
            .orElseThrow(() -> new RuntimeException("分类不存在"));
        category.setName(name);
        return repository.save(category);
    }

    public void delete(String code) {
        // 不允许删除系统默认分类
        if (isSystemCategory(code)) {
            throw new RuntimeException("系统默认分类不能删除");
        }
        repository.findByCode(code).ifPresent(repository::delete);
    }

    private boolean isSystemCategory(String code) {
        return "DATA_COLLECT".equals(code) ||
               "DATA_PARSE".equals(code) ||
               "DATA_PROCESS".equals(code) ||
               "GENERAL".equals(code);
    }
}
