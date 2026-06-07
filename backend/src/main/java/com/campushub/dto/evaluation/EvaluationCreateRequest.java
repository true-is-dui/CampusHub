package com.campushub.dto.evaluation;

import com.campushub.entity.enums.RatingLevel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交代取服务评价的请求体，对应 {@code api_design.yaml} 的 {@code EvaluationCreateRequest}。
 *
 * <p>被评价人<b>不由前端提交</b>：后端根据 {@code pickupId} 和当前登录用户自动推导
 * （我是发布方则评接单方，反之亦然）。本 DTO 只收评分等级与内容。
 *
 * <p>格式校验声明式（第四批准则：格式归 DTO）：{@code ratingLevel} 必填、{@code content}
 * 上限 300 字；跨字段约束「BAD 时 content 必填」用 {@link AssertTrue} 表达，DTO 内即可判定。
 */
@Data
public class EvaluationCreateRequest {

    /** 评分等级；非法值由绑定阶段拒绝。 */
    @NotNull(message = "评分等级不能为空")
    private RatingLevel ratingLevel;

    /** 评价内容；GOOD/NEUTRAL 可空，BAD 必填（见 {@link #isContentConsistentForBad()}）。 */
    @Size(max = 300, message = "评价内容不能超过 300 字")
    private String content;

    /**
     * 跨字段约束：差评（BAD）时评价内容必填，用于承载差评原因/说明（DB 设计 §5.6）。
     * 校验失败时 errors 以字段名 {@code content} 为 key（契约参数校验用字段名作 key）。
     */
    @AssertTrue(message = "差评必须填写评价内容")
    public boolean isContentConsistentForBad() {
        if (ratingLevel != RatingLevel.BAD) {
            return true;
        }
        return content != null && !content.isBlank();
    }
}
