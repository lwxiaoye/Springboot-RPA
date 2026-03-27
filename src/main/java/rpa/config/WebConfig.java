package rpa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/dashboard.html");
        registry.addViewController("/login.html").setViewName("login");
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
