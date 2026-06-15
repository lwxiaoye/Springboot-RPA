package rpa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security安全配置类
 * <p>
 * 配置Spring Security的各项安全策略，包括：
 * <ul>
 *   <li>CSRF防护：已禁用（适用于前后端分离项目，JWT无状态认证不需要）</li>
 *   <li>CORS跨域：从配置读取允许的域名列表</li>
 *   <li>会话管理：采用无状态会话策略（STATELESS），配合JWT Token使用</li>
 *   <li>接口权限：白名单接口无需认证，其他API接口需要JWT认证</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 未认证时返回JSON错误信息（前后端分离架构）
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("Unauthorized request to: {} - {}", request.getRequestURI(), authException.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期，请重新登录\"}");
        };
    }

    /**
     * CORS允许的来源列表，从配置文件读取
     * 生产环境应配置为具体的前端域名
     */
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    /**
     * 配置安全过滤器链
     * <p>
     * 定义URL的访问权限规则：
     * <ul>
     *   <li>白名单路径：/api/user/login, /api/user/register 等无需认证</li>
     *   <li>公告和水印：公开访问，供未登录用户查看</li>
     *   <li>其他API路径：需要JWT认证</li>
     *   <li>WebSocket：允许连接（认证在连接时处理）</li>
     * </ul>
     * </p>
     *
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint())
            .and()
            .authorizeRequests()
                // ========== 公开接口白名单 ==========
                // 登录注册相关 - 公开（无需认证）
                .antMatchers("/api/user/login", "/api/user/register", "/login",
                    "/api/user/send-reset-code", "/api/user/reset-password-by-code",
                    "/api/user/avatar/**").permitAll()
                // 公告相关 - 公开（供未登录用户查看系统公告）
                .antMatchers("/api/announcement/list", "/api/announcement/{id}").permitAll()
                // 水印配置 - 公开（用于页面水印展示）
                .antMatchers("/api/watermark/config/**", "/api/watermark/status").permitAll()
                // WebSocket - 允许连接（认证在WebSocket握手时处理）
                .antMatchers("/ws/**").permitAll()
                // Actuator健康检查 - 公开
                .antMatchers("/actuator/health").permitAll()
                // 错误页面 - 公开
                .antMatchers("/error").permitAll()
                // ========== 其他所有API接口需要认证 ==========
                .antMatchers("/api/**").authenticated()
                // 其他静态资源等允许访问
                .anyRequest().permitAll()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置密码加密器
     * <p>
     * 使用BCryptPasswordEncoder进行密码加密和验证，
     * BCrypt是一种基于Blowfish加密算法的单向哈希函数，
     * 具有自动加盐和防彩虹表攻击的特性。
     * </p>
     *
     * @return PasswordEncoder BCrypt密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置认证管理器
     * <p>
     * 从Spring Security配置中获取AuthenticationManager实例，
     * 用于自定义认证逻辑。
     * </p>
     *
     * @param config 认证配置
     * @return AuthenticationManager 认证管理器
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 配置CORS跨域资源共享
     * <p>
     * 从配置文件读取允许的跨域来源：
     * <ul>
     *   <li>生产环境：配置具体的前端域名，如 https://rpa.yourcompany.com</li>
     *   <li>开发环境：可以配置多个本地开发端口</li>
     * </ul>
     * </p>
     *
     * @return CorsConfigurationSource CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 从配置读取允许的来源
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            // 支持配置多个域名，用逗号分隔
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            origins = origins.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
            configuration.setAllowedOriginPatterns(origins);
        } else {
            // 未配置时只允许localhost（安全默认值）
            configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*"
            ));
        }

        // 允许常用的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        // 允许常用的请求头
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Cache-Control"
        ));
        // 允许携带凭证
        configuration.setAllowCredentials(true);
        // 暴露响应头
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        // 缓存预检请求结果时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
