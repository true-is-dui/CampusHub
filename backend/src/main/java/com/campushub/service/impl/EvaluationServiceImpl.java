package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.evaluation.EvaluationCreateRequest;
import com.campushub.dto.evaluation.EvaluationEligibility;
import com.campushub.dto.evaluation.EvaluationEligibilityReason;
import com.campushub.dto.evaluation.EvaluationHistorySummary;
import com.campushub.dto.evaluation.EvaluationSubmitResult;
import com.campushub.dto.evaluation.RatingRoleSummary;
import com.campushub.dto.evaluation.RatingSummary;
import com.campushub.dto.pickup.UserSummary;
import com.campushub.entity.Evaluation;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.NotificationType;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RatingLevel;
import com.campushub.mapper.EvaluationMapper;
import com.campushub.service.EvaluationService;
import com.campushub.service.NotificationService;
import com.campushub.service.PickupService;
import com.campushub.service.UserService;
import com.campushub.service.dto.PickupEvaluationContext;
import com.campushub.service.dto.UserBrief;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * {@link EvaluationService} 实现。
 *
 * <p>评价用 {@code businessType=PICKUP_REQUEST + businessId=pickupId} 定位业务对象（不写死代取外键）；
 * 唯一约束 {@code (business_type, business_id, reviewer_id)} 保证同一服务中同一评价者只评一次，
 * 应用层先 {@code selectCount} 预检 + 兜底 {@link DuplicateKeyException}（并发下两请求同时通过预检时）。
 *
 * <p>被评价人不信任前端传入：由 {@link PickupService#queryPickupEvaluationContext} 取发布方/接单方/状态，
 * 据当前用户推导（我是发布方→评接单方，反之亦然）。好评率动态聚合、不缓存（DB 设计 §5.1）。
 */
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private static final String BUSINESS_TYPE = BusinessType.PICKUP_REQUEST.name();

    private final EvaluationMapper evaluationMapper;
    private final PickupService pickupService;
    private final UserService userService;
    private final NotificationService notificationService;

    // ---------------- 评价资格 ----------------

    @Override
    public EvaluationEligibility queryEvaluationEligibility(Long pickupId, Long currentUserId) {
        PickupEvaluationContext ctx = pickupService.queryPickupEvaluationContext(pickupId);

        Long revieweeId = resolveRevieweeId(ctx, currentUserId);
        if (revieweeId == null) {
            return ineligible(EvaluationEligibilityReason.NOT_PARTICIPANT);
        }
        if (ctx.getStatus() != PickupStatus.COMPLETED) {
            return ineligible(EvaluationEligibilityReason.NOT_COMPLETED);
        }
        if (alreadyEvaluated(pickupId, currentUserId)) {
            return ineligible(EvaluationEligibilityReason.ALREADY_EVALUATED);
        }

        return EvaluationEligibility.builder()
                .canEvaluate(true)
                .reviewee(loadUserSummary(revieweeId))
                .reason(null)
                .build();
    }

    // ---------------- 提交评价 ----------------

    @Override
    @Transactional
    public EvaluationSubmitResult submitEvaluation(Long pickupId, Long currentUserId,
                                                   EvaluationCreateRequest request) {
        PickupEvaluationContext ctx = pickupService.queryPickupEvaluationContext(pickupId);

        // 推导被评价人与其在本服务中的角色（我评对手方）。
        Long revieweeId = resolveRevieweeId(ctx, currentUserId);
        if (revieweeId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorReason.AUTH_STATUS_NOT_ALLOWED,
                    "非该代取服务的参与者，不能评价");
        }
        if (ctx.getStatus() != PickupStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.PICKUP_EVALUATION_NOT_ALLOWED,
                    "代取服务未完成，不能评价");
        }
        if (alreadyEvaluated(pickupId, currentUserId)) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.PICKUP_EVALUATION_NOT_ALLOWED,
                    "已对该代取服务评价过");
        }

        // 被评价人是发布方还是接单方，决定其角色统计口径。
        PickupParticipantRole revieweeRole = ctx.getPublisherId().equals(revieweeId)
                ? PickupParticipantRole.PUBLISHER
                : PickupParticipantRole.ACCEPTOR;

        Evaluation evaluation = new Evaluation();
        evaluation.setBusinessType(BusinessType.PICKUP_REQUEST);
        evaluation.setBusinessId(pickupId);
        evaluation.setReviewerId(currentUserId);
        evaluation.setRevieweeId(revieweeId);
        evaluation.setRevieweeRole(revieweeRole);
        evaluation.setRatingLevel(request.getRatingLevel());
        evaluation.setContent(normalizeContent(request.getContent()));
        try {
            evaluationMapper.insert(evaluation);
        } catch (DuplicateKeyException ex) {
            // 并发下两请求同时通过预检；唯一约束兜底，归同一 409。
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.PICKUP_EVALUATION_NOT_ALLOWED,
                    "已对该代取服务评价过");
        }

        notificationService.createNotice(revieweeId, NotificationType.EVALUATION,
                "收到一条新评价", "您在一次代取服务中收到了新的评价。",
                BUSINESS_TYPE, pickupId);

        return EvaluationSubmitResult.builder().evaluationId(evaluation.getId()).build();
    }

    // ---------------- 好评率摘要 ----------------

    @Override
    public RatingSummary queryUserRatingSummary(Long userId) {
        // 查出该用户收到的全部评价，在内存按角色 + 等级聚合，避免多次分组查询。
        // 不做 lambda 列投影：lambda select 会在构建期触发 MP TableInfo 缓存，纯单元测试下不可用；
        // 评价行很窄（无大字段），全列读取代价可忽略，聚合只读 reviewee_role / rating_level 两列。
        List<Evaluation> received = evaluationMapper.selectList(Wrappers.<Evaluation>lambdaQuery()
                .eq(Evaluation::getRevieweeId, userId));

        return RatingSummary.builder()
                .userId(userId)
                .publisherRoleSummary(aggregate(received, PickupParticipantRole.PUBLISHER))
                .acceptorRoleSummary(aggregate(received, PickupParticipantRole.ACCEPTOR))
                .build();
    }

    // ---------------- 收到的评价列表 ----------------

    @Override
    public PageResult<EvaluationHistorySummary> queryUserEvaluations(Long userId, PageQuery pageQuery) {
        Page<Evaluation> page = evaluationMapper.selectPage(pageQuery.toMpPage(),
                Wrappers.<Evaluation>lambdaQuery()
                        .eq(Evaluation::getRevieweeId, userId)
                        .orderByDesc(Evaluation::getCreatedAt));
        List<EvaluationHistorySummary> list = page.getRecords().stream()
                .map(EvaluationHistorySummary::from)
                .toList();
        return PageResult.of(page, list);
    }

    // ---------------- 私有辅助 ----------------

    /** 推导被评价人：当前用户是发布方→接单方；是接单方→发布方；非参与者→null。 */
    private Long resolveRevieweeId(PickupEvaluationContext ctx, Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        if (currentUserId.equals(ctx.getPublisherId())) {
            return ctx.getAcceptorId(); // 接单方可能为 null（未接单），但 COMPLETED 必有接单方
        }
        if (currentUserId.equals(ctx.getAcceptorId())) {
            return ctx.getPublisherId();
        }
        return null;
    }

    private boolean alreadyEvaluated(Long pickupId, Long reviewerId) {
        Long count = evaluationMapper.selectCount(Wrappers.<Evaluation>lambdaQuery()
                .eq(Evaluation::getBusinessType, BusinessType.PICKUP_REQUEST)
                .eq(Evaluation::getBusinessId, pickupId)
                .eq(Evaluation::getReviewerId, reviewerId));
        return count != null && count > 0;
    }

    private EvaluationEligibility ineligible(EvaluationEligibilityReason reason) {
        return EvaluationEligibility.builder()
                .canEvaluate(false)
                .reviewee(null)
                .reason(reason)
                .build();
    }

    private UserSummary loadUserSummary(Long userId) {
        Optional<UserBrief> brief = userService.getUserBriefs(List.of(userId)).stream().findFirst();
        return brief.map(b -> UserSummary.builder()
                        .userId(b.getUserId())
                        .nickname(b.getNickname())
                        .build())
                .orElse(UserSummary.builder().userId(userId).build());
    }

    /** content 归一化：trim 后空串转 null（BAD 必填已由 DTO @AssertTrue 拦截）。 */
    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 按角色聚合好评率：分母为 0 时各计数为 0、positiveRate 为 0.0。 */
    private RatingRoleSummary aggregate(List<Evaluation> received, PickupParticipantRole role) {
        int good = 0;
        int neutral = 0;
        int bad = 0;
        for (Evaluation e : received) {
            if (e.getRevieweeRole() != role) {
                continue;
            }
            if (e.getRatingLevel() == RatingLevel.GOOD) {
                good++;
            } else if (e.getRatingLevel() == RatingLevel.NEUTRAL) {
                neutral++;
            } else if (e.getRatingLevel() == RatingLevel.BAD) {
                bad++;
            }
        }
        int total = good + neutral + bad;
        double positiveRate = total == 0 ? 0.0 : (double) good / total;
        return RatingRoleSummary.builder()
                .revieweeRoleInBusiness(role)
                .positiveCount(good)
                .neutralCount(neutral)
                .negativeCount(bad)
                .totalCount(total)
                .positiveRate(positiveRate)
                .build();
    }
}
