package rpa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rpa.entity.Resource;
import rpa.repository.ResourceRepository;
import java.util.List;

/**
 * 资源服务类
 * <p>
 * 提供资源相关的业务逻辑处理，包括资源CRUD。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    /**
     * 查询所有资源
     */
    public List<Resource> findAll() {
        return resourceRepository.findAll();
    }

    /**
     * 根据ID查询资源
     *
     * @param id 资源ID
     * @return 资源信息
     */
    public Resource findById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
    }

    /**
     * 创建资源
     *
     * @param name 资源名称
     * @param code 资源编码
     * @param type 资源类型
     * @param url URL路径
     * @param icon 图标
     * @param sort 排序号
     * @return 创建的资源
     */
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

    /**
     * 更新资源
     *
     * @param id 资源ID
     * @param name 资源名称
     * @param code 资源编码
     * @param type 资源类型
     * @param url URL路径
     * @param icon 图标
     * @param sort 排序号
     * @param status 状态
     * @return 更新后的资源
     */
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

    /**
     * 删除资源
     */
    public void delete(Long id) {
        resourceRepository.deleteById(id);
    }
}
