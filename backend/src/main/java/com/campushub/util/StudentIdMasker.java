package com.campushub.util;

import org.springframework.util.StringUtils;

/**
 * 学号脱敏工具：对外展示时保留前 2 位与后 2 位，中间以等长 {@code *} 替换。
 *
 * <p>无状态纯函数，供用户资料与实名审核等多处复用，避免脱敏规则在各服务里重复实现导致漂移。
 */
public final class StudentIdMasker {

    private StudentIdMasker() {
    }

    /**
     * 脱敏学号。学号为空（未认证）返回 {@code null}；长度不足 5 位时保守地整体打码。
     *
     * @param studentId 明文学号
     * @return 脱敏后的学号，或 {@code null}
     */
    public static String mask(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            return null;
        }
        int len = studentId.length();
        if (len <= 4) {
            return "*".repeat(len);
        }
        return studentId.substring(0, 2) + "*".repeat(len - 4) + studentId.substring(len - 2);
    }
}
