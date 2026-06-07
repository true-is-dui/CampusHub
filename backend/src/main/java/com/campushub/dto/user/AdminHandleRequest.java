package com.campushub.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 管理员处理实名认证审核请求体，对应 {@code api_design.yaml} 的 {@code AdminHandleRequest}。
 *
 * <p>契约规定：{@code result=REJECT} 时 {@code reason} 必填；{@code result=APPROVE} 时可为空。
 * 该跨字段约束用 {@link AssertTrue} 在 DTO 层声明，与 {@code @Valid} 入口校验一致，
 * 不下沉到服务层手写。
 */
@Data
public class AdminHandleRequest {

    @NotNull(message = "审核结果不能为空")
    private AdminHandleResult result;

    @Size(max = 200, message = "驳回原因不能超过 200 个字符")
    private String reason;

    /** 驳回必须填写原因；其余情况不约束。校验失败时字段名定位到 {@code reason}。 */
    @JsonIgnore
    @AssertTrue(message = "驳回时必须填写驳回原因")
    public boolean isReasonValidForReject() {
        if (result != AdminHandleResult.REJECT) {
            return true;
        }
        return StringUtils.hasText(reason);
    }
}
