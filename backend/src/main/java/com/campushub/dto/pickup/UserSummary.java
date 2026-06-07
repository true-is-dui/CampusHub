package com.campushub.dto.pickup;

import com.campushub.dto.evaluation.RatingSummary;
import lombok.Builder;
import lombok.Getter;

/**
 * 公开用户摘要，对应 {@code api_design.yaml} 的 {@code UserSummary}，用于代取大厅/详情/
 * 我的代取列表、评价资格中展示发布方与接单方。
 *
 * <p>只携带 {@code userId} 与 {@code nickname}（第四批准则：跨模块展示用户只暴露公开字段，
 * 不泄露学号/姓名/认证材料）。{@code ratingSummary} 为 nullable，普通列表（大厅/我的代取）
 * 默认不返回，仅在明确需要评分摘要的场景填充；评价模块落地后由调用方按需注入。
 */
@Getter
@Builder
public class UserSummary {

    private final Long userId;
    private final String nickname;
    /** 评价统计摘要；普通列表默认 null，仅需要评分摘要的场景填充。 */
    private final RatingSummary ratingSummary;
}
