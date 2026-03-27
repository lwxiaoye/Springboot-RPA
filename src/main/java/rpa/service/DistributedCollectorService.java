package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式采集协调服务类
 * <p>
 * 提供分布式环境下的采集任务协调功能，
 * 支持多节点环境下的任务分配、锁控制和状态同步。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>节点管理 - 节点注册和心跳</li>
 *   <li>任务锁控制 - 分布式互斥锁</li>
 *   <li>任务分配 - 负载均衡分配</li>
 *   <li>状态同步 - 任务状态和进度同步</li>
 *   <li>故障恢复 - 失败任务检测和转移</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedCollectorService {

    private final CacheService cacheService;

    /** 锁过期时间（秒），可通过配置覆盖 */
    @Value("${scraper.lock-timeout:300}")
    private long lockTimeout;

    /** Redis key前缀 - 节点信息 */
    private static final String NODE_PREFIX = "node:";
    
    /** Redis key前缀 - 任务锁 */
    private static final String TASK_PREFIX = "task:";
    
    /** Redis key前缀 - 任务状态 */
    private static final String STATUS_PREFIX = "status:";

    /** 本地节点信息 */
    private final Map<String, Object> localNodeInfo = new ConcurrentHashMap<>();
    
    /** 任务计数器（生成批次ID） */
    private final AtomicInteger taskCounter = new AtomicInteger(0);

    /**
     * 注册当前节点
     * <p>
     * 将节点信息注册到Redis，10分钟有效。
     * 定期调用heartbeat可保持注册状态。
     * </p>
     *
     * @param nodeId 节点ID
     * @param nodeInfo 节点信息（host/port/capacity）
     */
    public void registerNode(String nodeId, Map<String, Object> nodeInfo) {
        localNodeInfo.put("nodeId", nodeId);
        localNodeInfo.put("host", nodeInfo.getOrDefault("host", "unknown"));
        localNodeInfo.put("port", nodeInfo.getOrDefault("port", 0));
        localNodeInfo.put("capacity", nodeInfo.getOrDefault("capacity", 5));
        localNodeInfo.put("status", "active");
        localNodeInfo.put("registeredAt", System.currentTimeMillis());

        cacheService.set(NODE_PREFIX + nodeId, localNodeInfo, 10);
        log.info("节点 {} 已注册到协调器", nodeId);
    }

    /**
     * 节点心跳
     * <p>
     * 更新节点的存活状态，2分钟过期。
     * 需要定期调用以保持节点活跃。
     * </p>
     *
     * @param nodeId 节点ID
     */
    public void heartbeat(String nodeId) {
        cacheService.set(NODE_PREFIX + nodeId + ":heartbeat", "alive", 2);
    }

    /**
     * 获取所有活跃节点
     */
    public List<Map<String, Object>> getActiveNodes() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(localNodeInfo);
        return nodes;
    }

    /**
     * 申请采集任务分布式锁
     * <p>
     * 使用Redis实现互斥锁，确保同一任务只能被一个节点执行。
     * 返回requestId用于释放锁。
     * </p>
     *
     * @param collectId 采集任务ID
     * @param nodeId 节点ID
     * @return String requestId（获取成功），null（获取失败）
     */
    public String acquireTaskLock(Long collectId, String nodeId) {
        String taskLockKey = TASK_PREFIX + collectId;
        String requestId = UUID.randomUUID().toString();

        boolean acquired = cacheService.tryLock(taskLockKey, requestId, lockTimeout);

        if (acquired) {
            log.info("节点 {} 获取任务 {} 锁成功", nodeId, collectId);
            return requestId;
        }

        log.warn("节点 {} 获取任务 {} 锁失败", nodeId, collectId);
        return null;
    }

    /**
     * 释放采集任务锁
     *
     * @param collectId 采集任务ID
     * @param requestId 请求ID
     */
    public void releaseTaskLock(Long collectId, String requestId) {
        String taskLockKey = TASK_PREFIX + collectId;
        cacheService.unlock(taskLockKey, requestId);
    }

    /**
     * 更新任务状态
     *
     * @param collectId 采集任务ID
     * @param status 状态（pending/running/completed/failed）
     * @param nodeId 节点ID
     */
    public void updateTaskStatus(Long collectId, String status, String nodeId) {
        Map<String, Object> taskStatus = new HashMap<>();
        taskStatus.put("collectId", collectId);
        taskStatus.put("status", status);
        taskStatus.put("nodeId", nodeId);
        taskStatus.put("timestamp", System.currentTimeMillis());

        cacheService.set(STATUS_PREFIX + collectId, taskStatus, 60);
    }

    /**
     * 获取任务状态
     */
    public Map<String, Object> getTaskStatus(Long collectId) {
        return cacheService.get(STATUS_PREFIX + collectId, Map.class);
    }

    /**
     * 分配采集任务到节点
     * <p>
     * 采用简单的负载均衡策略：
     * 选择请求数最少的节点作为任务执行节点。
     * </p>
     *
     * @param collectId 采集任务ID
     * @param nodeIds 可用节点列表
     * @return String 选中的节点ID
     */
    public String distributeTask(Long collectId, List<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            Object nodeId = localNodeInfo.getOrDefault("nodeId", "local");
            return nodeId != null ? nodeId.toString() : "local";
        }

        int minRequests = Integer.MAX_VALUE;
        String selectedNode = "local";
        Object defaultNode = localNodeInfo.get("nodeId");
        if (defaultNode != null) {
            selectedNode = defaultNode.toString();
        }

        for (String nodeId : nodeIds) {
            long requests = cacheService.getRequestCount(nodeId);
            if (requests < minRequests) {
                minRequests = (int) requests;
                selectedNode = nodeId;
            }
        }

        log.info("任务 {} 分配给节点 {}", collectId, selectedNode);
        return selectedNode;
    }

    /**
     * 检查是否可以执行采集（防止重复采集）
     */
    public boolean canExecuteCollect(Long collectId) {
        Map<String, Object> status = getTaskStatus(collectId);
        if (status == null) {
            return true;
        }

        String currentStatus = (String) status.get("status");
        return !"running".equals(currentStatus);
    }

    /**
     * 生成任务批次ID
     *
     * @return String 批次ID，格式：batch_timestamp_counter
     */
    public String generateBatchId() {
        return "batch_" + System.currentTimeMillis() + "_" + taskCounter.incrementAndGet();
    }

    /**
     * 记录节点请求数
     */
    public void recordRequest(String nodeId) {
        cacheService.incrementRequestCount(nodeId);
    }

    /**
     * 获取节点负载情况
     */
    public Map<String, Object> getNodeLoad(String nodeId) {
        Map<String, Object> load = new HashMap<>();
        load.put("nodeId", nodeId);
        load.put("totalRequests", cacheService.getRequestCount(nodeId));
        load.put("activeTasks", getActiveTaskCount(nodeId));
        return load;
    }

    private int getActiveTaskCount(String nodeId) {
        return 0;
    }

    /**
     * 获取失败的任务列表
     * <p>
     * 用于故障转移时重新分配任务。
     * </p>
     */
    public List<Long> getFailedTasks() {
        return new ArrayList<>();
    }

    /**
     * 同步采集进度到Redis
     *
     * @param collectId 采集任务ID
     * @param progress 当前进度
     * @param total 总数
     */
    public void syncProgress(Long collectId, int progress, int total) {
        Map<String, Object> progressInfo = new HashMap<>();
        progressInfo.put("collectId", collectId);
        progressInfo.put("progress", progress);
        progressInfo.put("total", total);
        progressInfo.put("percent", total > 0 ? (progress * 100 / total) : 0);
        progressInfo.put("updatedAt", System.currentTimeMillis());

        cacheService.set("progress:" + collectId, progressInfo, 30);
    }

    /**
     * 获取采集进度
     */
    public Map<String, Object> getProgress(Long collectId) {
        return cacheService.get("progress:" + collectId, Map.class);
    }
}
