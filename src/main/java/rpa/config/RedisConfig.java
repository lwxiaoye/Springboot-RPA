package rpa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis缓存配置类
 * <p>
 * 配置RedisTemplate的序列化方式，支持复杂对象的存储和读取。
 * 使用String作为Key的序列化器，保证Redis key的可读性；
 * 使用JSON作为Value的序列化器，支持任意Java对象的自动序列化/反序列化。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate Bean
     * <p>
     * 使用StringRedisSerializer作为Key序列化器，保证key的可读性；
     * 使用GenericJackson2JsonRedisSerializer作为Value序列化器，
     * 支持复杂对象（如实体类、Map等）的自动JSON序列化。
     * </p>
     *
     * @param connectionFactory Redis连接工厂，由Spring自动注入
     * @return RedisTemplate<String, Object> 配置好的Redis模板实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
