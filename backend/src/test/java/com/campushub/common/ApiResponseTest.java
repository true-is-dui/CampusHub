package com.campushub.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证成功响应 {@link ApiResponse} 的序列化形状符合 {@code api_design.yaml} 的
 * {@code ApiResponse} schema：data 始终出现（无数据时为 null），不含 errors 字段。
 *
 * <p>纯 Jackson 序列化测试，不加载 Spring 上下文，故不依赖数据库。
 */
class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void successWithoutData_keepsDataNull_andOmitsErrors() throws Exception {
        String json = objectMapper.writeValueAsString(ApiResponse.ok());

        // data 必须保留且为 null（契约 required: data）
        assertThat(json).contains("\"data\":null");
        // 成功响应 schema 不含 errors 字段
        assertThat(json).doesNotContain("errors");
        assertThat(json).contains("\"code\":0");
    }

    @Test
    void successWithData_serializesData_andOmitsErrors() throws Exception {
        String json = objectMapper.writeValueAsString(ApiResponse.ok(Map.of("token", "abc")));

        assertThat(json).contains("\"token\":\"abc\"");
        assertThat(json).doesNotContain("errors");
    }
}
