package rpa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * <p>
 * 配置Spring MVC的视图控制器和静态资源处理。
 * 主要功能：
 * <ul>
 *   <li>根路径"/"转发到登录页面</li>
 *   <li>"/dashboard.html"映射到dashboard视图</li>
 *   <li>"/login.html"映射到login视图</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see WebMvcConfigurer
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 添加视图控制器
     * <p>
     * 配置URL到视图的映射关系：
     * <ul>
     *   <li>/ -> 转发到登录页面</li>
     *   <li>/dashboard.html -> dashboard视图</li>
     *   <li>/login.html -> login视图</li>
     * </ul>
     * </p>
     *
     * @param registry 视图控制器注册表
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/login.html");
        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/dashboard.html").setViewName("dashboard");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射头像资源路径 - 支持两种路径格式，确保前后端一致
        registry.addResourceHandler("/user/avatar/image/**")
                .addResourceLocations("file:uploads/avatars/");
        // 兼容带 /api 前缀的访问
        registry.addResourceHandler("/api/user/avatar/image/**")
                .addResourceLocations("file:uploads/avatars/");
    }
}
