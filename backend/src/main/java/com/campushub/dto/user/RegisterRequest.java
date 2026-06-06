package com.campushub.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求体，对应 {@code api_design.yaml} 的 {@code RegisterRequest}。
 *
 * <p>字段约束逐字映射契约：用户名 3-30 位、仅字母数字下划线；密码 8-32 位、
 * 至少含字母和数字，允许 {@code _@#$%^&*.-} 等常见符号。校验注解由 Controller
 * 入口的 {@code @Valid} 触发，不通过时统一交 {@code GlobalExceptionHandler} 出 40001。
 *
 * <p>用 Lombok {@code @Data} 生成 getter/setter，供 Jackson 反序列化请求 JSON。
 */
@Data
public class RegisterRequest {

    /** 登录账号，平台内唯一，不作为公开展示名。 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度需为 3-30 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /** 原始密码，后端保存时加盐哈希，禁止明文存储。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需为 8-32 位")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d_@#$%^&*.-]+$",
            message = "密码至少包含字母和数字，可使用 _@#$%^&*.- 等符号")
    private String password;
}
