package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.DataCollect;
import rpa.repository.DataCollectRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCollectService {

    private final DataCollectRepository repository;
    private final WebScraperService scraperService;

    public List<DataCollect> findAll() {
        return repository.findAll();
    }

    public Optional<DataCollect> findById(Long id) {
        return repository.findById(id);
    }

    public DataCollect create(Map<String, Object> request) {
        DataCollect collect = new DataCollect();
        collect.setName((String) request.get("name"));
        collect.setSourceUrl((String) request.get("sourceUrl"));
        collect.setSourceType((String) request.getOrDefault("sourceType", "web"));
        collect.setSelectorRules((String) request.get("selectorRules"));
        collect.setHeaders((String) request.get("headers"));
        collect.setCookies((String) request.get("cookies"));
        collect.setCronExpression((String) request.get("cronExpression"));
        collect.setStatus(0);
        collect.setDataCount(0);
        collect.setCreateTime(LocalDateTime.now());
        collect.setUpdateTime(LocalDateTime.now());

        Object creatorIdObj = request.get("creatorId");
        if (creatorIdObj != null) {
            collect.setCreatorId(Long.valueOf(creatorIdObj.toString()));
        }
        collect.setCreatorName((String) request.get("creatorName"));

        return repository.save(collect);
    }

    public DataCollect update(Long id, Map<String, Object> request) {
        return repository.findById(id).map(collect -> {
            if (request.containsKey("name")) collect.setName((String) request.get("name"));
            if (request.containsKey("sourceUrl")) collect.setSourceUrl((String) request.get("sourceUrl"));
            if (request.containsKey("sourceType")) collect.setSourceType((String) request.get("sourceType"));
            if (request.containsKey("selectorRules")) collect.setSelectorRules((String) request.get("selectorRules"));
            if (request.containsKey("headers")) collect.setHeaders((String) request.get("headers"));
            if (request.containsKey("cookies")) collect.setCookies((String) request.get("cookies"));
            if (request.containsKey("cronExpression")) collect.setCronExpression((String) request.get("cronExpression"));
            if (request.containsKey("status")) collect.setStatus((Integer) request.get("status"));
            collect.setUpdateTime(LocalDateTime.now());
            return repository.save(collect);
        }).orElseThrow(() -> new RuntimeException("采集配置不存在"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Map<String, Object> executeCollect(Long id) {
        return repository.findById(id).map(collect -> {
            Map<String, Object> result = scraperService.executeCollect(collect);
            if ((boolean) result.getOrDefault("success", false)) {
                collect.setStatus(1);
                collect.setLastCollectTime(System.currentTimeMillis());
                collect.setDataCount(collect.getDataCount() + (Integer) result.getOrDefault("count", 0));
            } else {
                collect.setStatus(2);
            }
            repository.save(collect);
            return result;
        }).orElseThrow(() -> new RuntimeException("采集配置不存在"));
    }
}
