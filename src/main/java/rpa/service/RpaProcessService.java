package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.RpaProcess;
import rpa.repository.RpaProcessRepository;
import java.util.List;
import java.util.Optional;

/**
 * RPA流程服务类
 * <p>
 * 提供RPA流程相关的业务逻辑处理，包括流程CRUD。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class RpaProcessService {

    private final RpaProcessRepository repository;

    /**
     * 查询所有流程
     */
    public List<RpaProcess> findAll() {
        return repository.findAll();
    }

    /**
     * 根据创建者查询流程
     */
    public List<RpaProcess> findByCreatorId(Long creatorId) {
        return repository.findByCreatorId(creatorId);
    }

    /**
     * 根据ID查询流程
     */
    public Optional<RpaProcess> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 创建流程
     *
     * @param name 名称
     * @param code 编码
     * @param description 描述
     * @param version 版本
     * @param status 状态
     * @param creatorId 创建者ID
     * @param creatorName 创建者名称
     * @return 创建的流程
     */
    public RpaProcess create(String name, String code, String description, String version, String status, Long creatorId, String creatorName) {
        RpaProcess process = new RpaProcess();
        process.setName(name);
        process.setCode(code);
        process.setDescription(description);
        process.setVersion(version != null ? version : "1.0.0");
        process.setStatus(status != null ? status : "draft");
        process.setCreatorId(creatorId);
        process.setCreatorName(creatorName);
        process.setTaskCount(0);
        return repository.save(process);
    }

    /**
     * 更新流程
     *
     * @param id 流程ID
     * @param name 名称
     * @param code 编码
     * @param description 描述
     * @param version 版本
     * @param status 状态
     * @return 更新后的流程
     */
    public RpaProcess update(Long id, String name, String code, String description, String version, String status) {
        return repository.findById(id).map(process -> {
            if (name != null) {
                process.setName(name);
            }
            if (code != null) {
                process.setCode(code);
            }
            if (description != null) {
                process.setDescription(description);
            }
            if (version != null) {
                process.setVersion(version);
            }
            if (status != null) {
                process.setStatus(status);
            }
            return repository.save(process);
        }).orElseThrow(() -> new RuntimeException("流程不存在"));
    }

    /**
     * 删除流程
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * 保存流程设计
     *
     * @param id 流程 ID
     * @param steps 流程步骤（JSON 格式）
     * @return 更新后的流程
     */
    public RpaProcess saveDesign(Long id, String steps) {
        return repository.findById(id).map(process -> {
            process.setSteps(steps);
            return repository.save(process);
        }).orElseThrow(() -> new RuntimeException("流程不存在"));
    }
}
