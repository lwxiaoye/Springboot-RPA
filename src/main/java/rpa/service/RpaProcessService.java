package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.RpaProcess;
import rpa.repository.RpaProcessRepository;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RpaProcessService {

    private final RpaProcessRepository repository;

    public List<RpaProcess> findAll() {
        return repository.findAll();
    }

    public List<RpaProcess> findByCreatorId(Long creatorId) {
        return repository.findByCreatorId(creatorId);
    }

    public Optional<RpaProcess> findById(Long id) {
        return repository.findById(id);
    }

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

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
