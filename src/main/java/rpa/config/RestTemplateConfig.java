package rpa.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 * <p>
 * 配置HTTP客户端用于调用外部API和采集网页内容。
 * </p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建RestTemplate Bean
     * <p>
     * 配置连接超时和读取超时时间，用于HTTP请求。
     * </p>
     *
     * @param builder RestTemplate构建器
     * @return 配置好的RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10秒
        factory.setReadTimeout(30000);    // 30秒

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
