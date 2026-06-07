package com.campushub.dto.pickup;

import lombok.Builder;
import lombok.Getter;

/**
 * 公开用户摘要，对应 {@code api_design.yaml} 的 {@code UserSummary}，用于代取大厅/详情/
 * 我的代取列表中展示发布方与接单方。
 *
 * <p>只携带 {@code userId} 与 {@code nickname}（第四批准则：跨模块展示用户只暴露公开字段，
 * 不泄露学号/姓名/认证材料）。契约的 {@code ratingSummary} 为 nullable 且普通列表默认不返回，
 * 评价模块（第七批 Should）尚未实现，故本批不带该字段，待评价模块落地后再补。
 */
@Getter
@Builder
public class UserSummary {

    private final Long userId;
    private final String nickname;
}
