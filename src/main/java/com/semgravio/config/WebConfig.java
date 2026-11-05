package com.semgravio.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Web 配置类
 *
 * <p>配置跨域资源共享（CORS）支持
 */
@Configuration
public class WebConfig {

    private final List<String> allowedOrigins;

    WebConfig(@Value("${admin.cors.allowed-origins}") String allowedOrigins) {
        // 逗号分隔的显式名单（allowCredentials(true) 下不可用 *），正本在 application.yml，
        // 生产经服务器 .env.production 的 CORS_ALLOWED_ORIGINS 覆盖（#121）
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
    }

    /**
     * 配置 CORS 过滤器
     *
     * <p>允许 {@code admin.cors.allowed-origins} 名单内来源的跨域请求
     * （默认 localhost:3001 管理后台前端开发服务器；生产注入 https://admin.semgravio.com）
     *
     * @return CORS 过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许携带认证信息（cookies, authorization headers）
        config.setAllowCredentials(true);

        // 允许的前端域名（配置驱动，逗号分隔多个）
        allowedOrigins.forEach(config::addAllowedOrigin);

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许所有 HTTP 方法
        config.addAllowedMethod("*");

        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
