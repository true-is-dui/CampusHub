package com.campushub.service;

import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.evaluation.EvaluationCreateRequest;
import com.campushub.dto.evaluation.EvaluationEligibility;
import com.campushub.dto.evaluation.EvaluationHistorySummary;
import com.campushub.dto.evaluation.EvaluationSubmitResult;
import com.campushub.dto.evaluation.PickupEvaluationItem;
import com.campushub.dto.evaluation.RatingSummary;
import com.campushub.dto.evaluation.ReceivedEvaluationDetail;

import java.util.List;

/**
 * 评价服务（Should 级能力），对应 {@code class_design.md} 的 {@code EvaluationService}：
 * 代取服务完成后双方互评，并按被评价人在服务中的角色（发布方 / 接单方）动态聚合好评率。
 *
 * <p>方法签名显式收 {@code currentUserId}（由 {@code @CurrentUser} 注入），不信任前端传入身份；
 * 被评价人不由前端提交，统一由 {@code pickupId} + 当前用户推导。评价上下文经
 * {@link PickupService#queryPickupEvaluationContext}（owner service，不传 entity），
 * 昵称经 {@link UserService#getUserBriefs}。
 */
public interface EvaluationService {

    /**
     * 查询当前用户对某代取服务的评价资格（供前端决定是否展示评价入口）。
     *
     * <p>仅作展示辅助：可评价时返回被评价人摘要；不可评价时返回原因。不抛业务冲突异常，
     * 代取服务不存在时抛 404。
     */
    EvaluationEligibility queryEvaluationEligibility(Long pickupId, Long currentUserId);

    /**
     * 提交评价。后端重新校验：服务存在且已完成、当前用户为参与者、未重复评价、
     * BAD 内容已填（DTO 已声明式拦截）。被评价人与角色由 {@code pickupId} + 当前用户推导。
     *
     * <p>非参与者 → 403；服务未完成 / 已评价 → 409。提交成功后向被评价人发评价通知。
     */
    EvaluationSubmitResult submitEvaluation(Long pickupId, Long currentUserId,
                                            EvaluationCreateRequest request);

    /**
     * 查询用户好评率摘要：按被评价人作为发布方 / 接单方分别动态聚合，不缓存。
     * 用户不存在不报错，按无评价返回各计数为 0。公开访问。
     */
    RatingSummary queryUserRatingSummary(Long userId);

    /** 分页查询用户收到的历史评价总览（按创建时间倒序）。公开访问。 */
    PageResult<EvaluationHistorySummary> queryUserEvaluations(Long userId, PageQuery pageQuery);

    /** 查询当前用户在指定代取服务中收到的评价详情（供评价通知落地页使用）。 */
    ReceivedEvaluationDetail queryReceivedEvaluation(Long pickupId, Long currentUserId);

    /** 查询指定代取服务下的双方评价列表（仅服务参与者可见，含评价者身份与完整内容）。 */
    List<PickupEvaluationItem> queryPickupEvaluations(Long pickupId, Long currentUserId);
}
