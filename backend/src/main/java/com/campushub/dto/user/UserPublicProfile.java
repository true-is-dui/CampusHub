package com.campushub.dto.user;

import com.campushub.dto.evaluation.RatingSummary;
import lombok.Builder;
import lombok.Getter;

/**
 * 用户公开主页，对应 {@code api_design.yaml} 的 {@code UserPublicProfile}
 * （{@code GET /users/{userId}/profile}，公开访问）。
 *
 * <p>只暴露公开资料（昵称/学院/联系方式）+ 可选评价摘要，绝不含学号、真实姓名、认证材料等
 * 隐私字段（FR-UM-03）。{@code ratingSummary} 仅当查询参数 {@code includeRating=true} 时填充，
 * 否则为 null（契约要求）。
 */
@Getter
@Builder
public class UserPublicProfile {

    private final String nickname;
    /** 学院，选填；未填写为 null。 */
    private final String college;
    /** 公开联系方式，选填；未填写为 null。 */
    private final String contact;
    /** 评价统计摘要；仅 includeRating=true 时返回，否则为 null。 */
    private final RatingSummary ratingSummary;
}
