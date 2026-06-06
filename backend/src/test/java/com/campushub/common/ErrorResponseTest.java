package com.campushub.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证错误响应 {@link ErrorResponse} 的序列化形状符合 {@code api_design.yaml} 的
 * {@code ErrorResponse} schema：code/message/data/errors 四个键都为 required，
 * 故 data 固定输出 null、errors 即使无详情也必须出现（值为 null）。
 *
 * <p>纯 Jackson 序列化测试，不加载 Spring 上下文。这里用 JsonNode 断言键是否存在，
 * 因为 {@code "errors":null} 用字符串 contains 难以与 "不含该键" 区分。
 */
class ErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void errorWithoutDetails_keepsDataNull_andKeepsErrorsKeyAsNull() throws Exception {
        JsonNode node = objectMapper.valueToTree(
                ErrorResponse.of(ErrorCode.UNAUTHENTICATED, "未登录或登录已过期"));

        assertThat(node.get("code").asInt()).isEqualTo(40101);
        // data 必填且为 null
        assertThat(node.has("data")).isTrue();
        assertThat(node.get("data").isNull()).isTrue();
        // errors 必填：无详情时键仍须出现，值为 null
        assertThat(node.has("errors")).isTrue();
        assertThat(node.get("errors").isNull()).isTrue();
    }

    @Test
    void errorWithFieldDetails_includesErrors() throws Exception {
        JsonNode node = objectMapper.valueToTree(
                ErrorResponse.of(ErrorCode.INVALID_PARAM, "请求参数错误",
                        Map.of("password", "密码至少 8 位")));

        assertThat(node.get("code").asInt()).isEqualTo(40001);
        assertThat(node.get("data").isNull()).isTrue();
        assertThat(node.get("errors").get("password").asText()).isEqualTo("密码至少 8 位");
    }
}
