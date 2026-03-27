package rpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RPA系统主入口
 * <p>
 * Spring Boot应用程序的主类，负责启动整个RPA系统。
 * </p>
 * <p>
 * 系统功能模块：
 * <ul>
 *   <li>用户认证 - JWT Token认证，支持登录注册</li>
 *   <li>权限管理 - RBAC基于角色的权限控制</li>
 *   <li>机器人管理 - RPA机器人注册和状态监控</li>
 *   <li>流程管理 - RPA自动化流程定义</li>
 *   <li>任务调度 - 任务分配和执行</li>
 *   <li>数据采集 - 网页数据自动化采集</li>
 *   <li>数据解析 - 采集数据的解析处理</li>
 *   <li>数据加工 - 数据的过滤转换映射</li>
 *   <li>数据查询 - 自定义数据查询配置</li>
 *   <li>通知系统 - 任务结果通知推送</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see SpringBootApplication
 */
@SpringBootApplication
public class Main {
    
    /**
     * 应用启动入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
