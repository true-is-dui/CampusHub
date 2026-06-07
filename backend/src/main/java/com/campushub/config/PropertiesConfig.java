package com.campushub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 集中注册项目的 {@code @ConfigurationProperties} 绑定类。
 *
 * <p>统一用 {@code @EnableConfigurationProperties} 注册（而非在各 Properties 上加
 * {@code @Component}），让外部化配置的注册点集中、可一眼概览，也不污染组件扫描。
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, FileStorageProperties.class})
public class PropertiesConfig {
}
