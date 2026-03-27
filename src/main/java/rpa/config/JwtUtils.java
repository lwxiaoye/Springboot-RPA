package rpa.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 * <p>
 * 提供JWT Token的生成、验证和解析功能。
 * 使用HMAC-SHA256算法进行签名，安全性较高。
 * Token中包含用户信息和角色信息，支持自定义声明扩展。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>生成Token：包含用户名、角色、签发时间、过期时间</li>
 *   <li>验证Token：检查签名和过期时间</li>
 *   <li>提取Claims：获取用户名、过期时间、自定义声明</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class JwtUtils {

    /**
     * JWT签名密钥，默认值用于开发环境，生产环境应通过环境变量或配置文件注入
     */
    @Value("${jwt.secret:mySecretKeyForJwtTokenGenerationMustBeLongEnough12345}")
    private String secret;

    /**
     * Token过期时间，默认24小时（86400000毫秒）
     */
    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 获取签名密钥
     * <p>
     * 使用HMAC-SHA256算法，需要足够长度的密钥。
     * </p>
     *
     * @return SecretKey HMAC签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 从Token中提取用户名
     *
     * @param token JWT Token字符串
     * @return String 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从Token中提取过期时间
     *
     * @param token JWT Token字符串
     * @return Date 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从Token中提取指定声明
     *
     * @param token JWT Token字符串
     * @param claimsResolver 声明提取函数
     * @param <T> 声明类型
     * @return T 声明值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 解析Token获取所有声明
     *
     * @param token JWT Token字符串
     * @return Claims Token声明集合
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查Token是否已过期
     *
     * @param token JWT Token字符串
     * @return Boolean 是否过期
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 使用UserDetails生成Token
     *
     * @param userDetails Spring Security用户详情
     * @return String JWT Token
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * 使用用户名和角色生成Token
     *
     * @param username 用户名
     * @param role 用户角色（1-管理员，其他-普通用户）
     * @return String JWT Token
     */
    public String generateToken(String username, Integer role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    /**
     * 创建Token
     *
     * @param claims 声明Map
     * @param subject 主题（通常为用户名）
     * @return String JWT Token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证Token（带UserDetails）
     *
     * @param token JWT Token
     * @param userDetails Spring Security用户详情
     * @return Boolean 验证结果
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * 验证Token（仅检查签名和过期）
     *
     * @param token JWT Token
     * @return Boolean 验证结果
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
