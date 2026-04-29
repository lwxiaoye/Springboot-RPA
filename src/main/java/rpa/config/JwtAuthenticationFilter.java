package rpa.config;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * <p>
 * 继承OncePerRequestFilter，确保每个HTTP请求只执行一次过滤逻辑。
 * 从请求头的Authorization字段中提取Bearer Token，验证Token有效性，
 * 并将认证信息存入Spring Security的SecurityContext中供后续使用。
 * </p>
 * <p>
 * 角色映射规则：
 * <ul>
 *   <li>role = 1 -> ROLE_ADMIN (系统管理员)</li>
 *   <li>其他 -> ROLE_USER (普通用户)</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see OncePerRequestFilter
 * @see JwtUtils
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    /**
     * 执行JWT认证过滤逻辑
     * <p>
     * 处理流程：
     * <ol>
     *   <li>从请求头中获取Authorization字段</li>
     *   <li>检查是否以"Bearer "开头</li>
     *   <li>提取Token字符串</li>
     *   <li>验证Token有效性和过期时间</li>
     *   <li>提取用户名和角色信息</li>
     *   <li>创建认证Token并设置到SecurityContext</li>
     * </ol>
     * </p>
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 允许 OPTIONS 预检请求直接通过
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtils.validateToken(token)) {
                    String username = jwtUtils.extractUsername(token);
                    var claims = jwtUtils.extractAllClaims(token);
                    Integer role = claims.get("role", Integer.class);

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                        role != null && role == 1 ? "ROLE_ADMIN" : "ROLE_USER"
                    );

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(authority));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Set authentication for user: {}", username);
                }
            } catch (Exception e) {
                log.error("JWT authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
