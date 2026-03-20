package rpa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rpa.entity.Resource;
import rpa.repository.ResourceRepository;
import java.util.List;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    public List<Resource> findAll() {
        return resourceRepository.findAll();
    }

    public Resource findById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
    }

    public Resource create(String name, String code, String type, String url, String icon, Integer sort) {
        Resource resource = new Resource();
        resource.setName(name);
        resource.setCode(code);
        resource.setType(type);
        resource.setUrl(url);
        resource.setIcon(icon);
        resource.setSort(sort);
        resource.setStatus(1);
        return resourceRepository.save(resource);
    }

    public Resource update(Long id, String name, String code, String type, String url, String icon, Integer sort, Integer status) {
        return resourceRepository.findById(id).map(resource -> {
            resource.setName(name);
            resource.setCode(code);
            resource.setType(type);
            resource.setUrl(url);
            resource.setIcon(icon);
            resource.setSort(sort);
            resource.setStatus(status);
            return resourceRepository.save(resource);
        }).orElseThrow(() -> new RuntimeException("资源不存在"));
    }

    public void delete(Long id) {
        resourceRepository.deleteById(id);
    }
}
