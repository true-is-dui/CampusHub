package com.campushub.dto.pickup;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消代取服务的可选请求体，对应 {@code api_design.yaml} 的 {@code ReasonRequest}。
 *
 * <p>取消接口 requestBody 整体可省略（Controller 以 {@code @RequestBody(required=false)}
 * 接收）；若提供 body，则 reason 为发布方主动取消的补充说明，落到
 * {@code pickup_requests.cancel_detail}。这里只约束长度上限（契约 1-200），
 * 不强制非空——整体 body 缺省时本对象为 null。
 */
@Data
public class PickupCancelRequest {

    /** 发布方主动取消的补充说明，可空；提供时不超过 200 字。 */
    @Size(max = 200, message = "取消说明不能超过 200 字")
    private String reason;
}
