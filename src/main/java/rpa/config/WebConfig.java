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
 *   <li>根路径"/"转发到 index.html (Vue SPA)</li>
 *   <li>静态资源服务</li>
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
     *   <li>/ -> index.html (Vue SPA入口)</li>
     * </ul>
     * </p>
     *
     * @param registry 视图控制器注册表
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径转发到 Vue SPA 的 index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射头像资源路径
        registry.addResourceHandler("/user/avatar/image/**")
                .addResourceLocations("file:uploads/avatars/");
        registry.addResourceHandler("/api/user/avatar/image/**")
                .addResourceLocations("file:uploads/avatars/");

        // Vue SPA 静态资源 - 关键配置
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/frontend/");
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/frontend/");
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");
    }
}
