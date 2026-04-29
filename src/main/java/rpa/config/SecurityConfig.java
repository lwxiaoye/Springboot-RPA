package rpa.config;

import lombok.RequiredArgsConstructor;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Spring Security安全配置类
 * <p>
 * 配置Spring Security的各项安全策略，包括：
 * <ul>
 *   <li>CSRF防护：已禁用（适用于前后端分离项目，JWT无状态认证不需要）</li>
 *   <li>CORS跨域：允许指定的开发端口进行跨域请求</li>
 *   <li>会话管理：采用无状态会话策略（STATELESS），配合JWT Token使用</li>
 *   <li>接口权限：白名单接口无需认证，其他API接口需要JWT认证</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置安全过滤器链
     * <p>
     * 定义URL的访问权限规则：
     * <ul>
     *   <li>白名单路径：/api/user/login, /api/user/register, /login 等无需认证</li>
     *   <li>API路径：/api/** 需要JWT认证</li>
     *   <li>其他路径：允许访问</li>
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
            .authorizeRequests()
                // 登录注册相关 - 公开
                .antMatchers("/api/user/login", "/api/user/register", "/login",
                    "/api/user/send-reset-code", "/api/user/reset-password-by-code",
                    "/api/user/avatar/**").permitAll()
                // 公告相关 - 公开
                .antMatchers("/api/announcement/**").permitAll()
                // 水印相关 - 公开
                .antMatchers("/api/watermark/**").permitAll()
                // 聊天相关 - 公开
                .antMatchers("/api/chat/**").permitAll()
                // 其他 RPA API - 公开（开发阶段）
                .antMatchers("/api/robot/**", "/api/process/**", "/api/task/**",
                    "/api/log/**", "/api/notification/**", "/api/user/**",
                    "/api/dataCollect/**", "/api/dataParse/**", "/api/dataProcess/**", "/api/dataQuery/**",
                    "/api/invoice/**", "/api/enterprise/**", "/api/ai/**", "/credential/**").permitAll()
                // WebSocket - 公开
                .antMatchers("/ws/**").permitAll()
                // 其他请求需要认证
                .antMatchers("/api/**").authenticated()
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
     * 允许以下源进行跨域访问：
     * <ul>
     *   <li>http://localhost:5173 (Vite开发服务器)</li>
     *   <li>http://localhost:5174 (备用Vite端口)</li>
     *   <li>http://localhost:3000 (备用开发端口)</li>
     *   <li>http://10.7.54.138:5173 (局域网访问)</li>
     * </ul>
     * 支持GET、POST、PUT、DELETE、OPTIONS方法，允许携带凭证。
     * </p>
     *
     * @return CorsConfigurationSource CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有来源（开发环境使用 *，生产环境建议改为具体域名）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        // 允许所有常用方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        // 允许所有请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 允许携带凭证（Cookie、Authorization header等）
        configuration.setAllowCredentials(true);
        // 暴露响应头，让前端可以访问
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
