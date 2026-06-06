package com.campushub.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体，对应 {@code api_design.yaml} 的 {@code LoginRequest}。
 *
 * <p>登录只校验"是否填写"，不复用注册的长度/字符集格式校验：登录的职责是核对
 * 凭证对不对，而非凭证是否合规；格式不符的输入按"用户名或密码错误"处理即可，
 * 避免把校验规则变更暴露给历史用户。实际比对在 {@code AuthService.login} 内完成。
 */
@Data
public class LoginRequest {

    /** 登录账号。 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 原始密码。 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
