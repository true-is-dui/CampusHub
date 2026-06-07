package com.campushub.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑个人资料请求的文本字段，对应 {@code api_design.yaml} 的 {@code UpdateProfileRequest}。
 *
 * <p>仅承载文本字段并做声明式格式校验；头像文件作为独立 {@code @RequestPart} 接收，
 * 不放进本 DTO（{@code @ModelAttribute} 无法绑定 multipart 文件）。
 *
 * <p>契约的 {@code minProperties: 1}（至少修改一项）需同时考虑头像文件，
 * 跨越了本 DTO 与文件参数，故该判断保留在服务层，不在此处声明。
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 20, message = "昵称长度需为 1-20 个字符")
    private String nickname;

    @Size(max = 50, message = "学院不能超过 50 个字符")
    private String college;

    @Size(max = 100, message = "联系方式不能超过 100 个字符")
    private String contact;
}
