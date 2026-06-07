package com.campushub;

import com.campushub.config.JwtProperties;
import com.campushub.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class BackendApplicationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of())
      .withUserConfiguration(JwtTestConfiguration.class)
      .withPropertyValues(
          "jwt.secret=test-jwt-secret-for-phase4-coverage-123456",
          "jwt.expiration=24h");

  @Test
  void jwtInfrastructureLoadsWithTestSecret() {
    contextRunner.run(context -> {
      org.assertj.core.api.Assertions.assertThat(context).hasSingleBean(JwtUtil.class);
      org.assertj.core.api.Assertions.assertThat(context).hasSingleBean(JwtProperties.class);
    });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(JwtProperties.class)
  static class JwtTestConfiguration {
    @Bean
    JwtUtil jwtUtil(JwtProperties properties) {
      return new JwtUtil(properties);
    }
  }
}
