package com.campushub.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link ApiResponse} 的序列化形状符合 {@code api_design.yaml} 契约：
 * data 始终出现（无数据时为 null），errors 仅在非空时出现。
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
        // 成功响应不应出现 errors 字段
        assertThat(json).doesNotContain("errors");
        assertThat(json).contains("\"code\":0");
    }

    @Test
    void successWithData_serializesData_andOmitsErrors() throws Exception {
        String json = objectMapper.writeValueAsString(ApiResponse.ok(Map.of("token", "abc")));

        assertThat(json).contains("\"token\":\"abc\"");
        assertThat(json).doesNotContain("errors");
    }

    @Test
    void errorWithoutDetails_keepsDataNull_andOmitsErrors() throws Exception {
        String json = objectMapper.writeValueAsString(
                ApiResponse.error(ErrorCode.UNAUTHENTICATED, "未登录或登录已过期"));

        assertThat(json).contains("\"code\":40101");
        assertThat(json).contains("\"data\":null");
        assertThat(json).doesNotContain("errors");
    }

    @Test
    void errorWithFieldDetails_includesErrors() throws Exception {
        String json = objectMapper.writeValueAsString(
                ApiResponse.error(ErrorCode.INVALID_PARAM, "请求参数错误",
                        Map.of("password", "密码至少 8 位")));

        assertThat(json).contains("\"code\":40001");
        assertThat(json).contains("\"errors\"");
        assertThat(json).contains("\"password\":\"密码至少 8 位\"");
        assertThat(json).contains("\"data\":null");
    }
}
