package com.campushub.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交实名认证请求的文本字段，对应 {@code api_design.yaml} 的 {@code VerificationSubmitRequest}。
 *
 * <p>仅承载文本字段并做声明式格式校验；认证材料图片作为独立 {@code @RequestPart} 接收，
 * 不放进本 DTO。学号/姓名为必填，约束逐字对齐契约。
 */
@Data
public class VerificationSubmitRequest {

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^\\d{6,20}$", message = "学号必须为 6-20 位数字")
    private String studentId;

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 20, message = "真实姓名长度需为 2-20 个字符")
    private String realName;
}
