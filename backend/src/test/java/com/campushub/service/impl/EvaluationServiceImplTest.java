package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.evaluation.EvaluationCreateRequest;
import com.campushub.dto.evaluation.EvaluationEligibility;
import com.campushub.dto.evaluation.EvaluationEligibilityReason;
import com.campushub.dto.evaluation.EvaluationSubmitResult;
import com.campushub.dto.evaluation.RatingSummary;
import com.campushub.entity.Evaluation;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.NotificationType;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RatingLevel;
import com.campushub.mapper.EvaluationMapper;
import com.campushub.service.NotificationService;
import com.campushub.service.PickupService;
import com.campushub.service.UserService;
import com.campushub.service.dto.PickupEvaluationContext;
import com.campushub.service.dto.UserBrief;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EvaluationServiceImpl} 单元测试：mock mapper / PickupService / UserService / NotificationService，
 * 验证评价资格分支、提交校验（参与者 / 已完成 / 重复）、被评价人推导、唯一约束兜底与好评率聚合，
 * 不连数据库。
 *
 * <p>约定：发布方 = 7L，接单方 = 8L，pickupId = 5L；COMPLETED 状态为可评价基线。
 */
@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvaluationMapper evaluationMapper;
    @Mock
    private PickupService pickupService;
    @Mock
    private UserService userService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private PickupEvaluationContext completedCtx() {
        return PickupEvaluationContext.builder()
                .pickupId(5L).publisherId(7L).acceptorId(8L)
                .status(PickupStatus.COMPLETED).build();
    }

    // ---------------- 评价资格 ----------------

    @Test
    void eligibility_publisher_canEvaluateAcceptor() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(completedCtx());
        when(evaluationMapper.selectCount(any())).thenReturn(0L);
        when(userService.getUserBriefs(any())).thenReturn(List.of(
                UserBrief.builder().userId(8L).username("acc").nickname("接单侠").build()));

        EvaluationEligibility result = evaluationService.queryEvaluationEligibility(5L, 7L);

        assertThat(result.isCanEvaluate()).isTrue();
        assertThat(result.getReason()).isNull();
        assertThat(result.getReviewee().getUserId()).isEqualTo(8L);
        assertThat(result.getReviewee().getNickname()).isEqualTo("接单侠");
    }

    @Test
    void eligibility_nonParticipant_returnsNotParticipant() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(completedCtx());

        EvaluationEligibility result = evaluationService.queryEvaluationEligibility(5L, 99L);

        assertThat(result.isCanEvaluate()).isFalse();
        assertThat(result.getReason()).isEqualTo(EvaluationEligibilityReason.NOT_PARTICIPANT);
        assertThat(result.getReviewee()).isNull();
    }

    @Test
    void eligibility_notCompleted_returnsNotCompleted() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(
                PickupEvaluationContext.builder()
                        .pickupId(5L).publisherId(7L).acceptorId(8L)
                        .status(PickupStatus.IN_PROGRESS).build());

        EvaluationEligibility result = evaluationService.queryEvaluationEligibility(5L, 7L);

        assertThat(result.isCanEvaluate()).isFalse();
        assertThat(result.getReason()).isEqualTo(EvaluationEligibilityReason.NOT_COMPLETED);
    }

    @Test
    void eligibility_alreadyEvaluated_returnsAlreadyEvaluated() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(completedCtx());
        when(evaluationMapper.selectCount(any())).thenReturn(1L);

        EvaluationEligibility result = evaluationService.queryEvaluationEligibility(5L, 7L);

        assertThat(result.isCanEvaluate()).isFalse();
        assertThat(result.getReason()).isEqualTo(EvaluationEligibilityReason.ALREADY_EVALUATED);
    }

    // ---------------- 提交评价 ----------------

    @Test
    void submit_publisherRatesAcceptor_insertsAndNotifies() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(completedCtx());
        when(evaluationMapper.selectCount(any())).thenReturn(0L);

        EvaluationCreateRequest req = new EvaluationCreateRequest();
        req.setRatingLevel(RatingLevel.GOOD);
        req.setContent("很快很靠谱");

        EvaluationSubmitResult result = evaluationService.submitEvaluation(5L, 7L, req);

        ArgumentCaptor<Evaluation> captor = ArgumentCaptor.forClass(Evaluation.class);
        verify(evaluationMapper).insert(captor.capture());
        Evaluation saved = captor.getValue();
        // 发布方(7L)评接单方(8L)，被评价角色应为 ACCEPTOR。
        assertThat(saved.getReviewerId()).isEqualTo(7L);
        assertThat(saved.getRevieweeId()).isEqualTo(8L);
        assertThat(saved.getRevieweeRole()).isEqualTo(PickupParticipantRole.ACCEPTOR);
        assertThat(saved.getBusinessType()).isEqualTo(BusinessType.PICKUP_REQUEST);
        assertThat(saved.getBusinessId()).isEqualTo(5L);
        assertThat(saved.getRatingLevel()).isEqualTo(RatingLevel.GOOD);
        // 通知被评价人(8L)，类型 EVALUATION。
        verify(notificationService).createNotice(eq(8L), eq(NotificationType.EVALUATION),
                any(), any(), eq("PICKUP_REQUEST"), eq(5L));
        assertThat(result).isNotNull();
    }

    @Test
    void submit_acceptorRatesPublisher_revieweeRoleIsPublisher() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(completedCtx());
        when(evaluationMapper.selectCount(any())).thenReturn(0L);

        EvaluationCreateRequest req = new EvaluationCreateRequest();
        req.setRatingLevel(RatingLevel.GOOD);

        evaluationService.submitEvaluation(5L, 8L, req);

        ArgumentCaptor<Evaluation> captor = ArgumentCaptor.forClass(Evaluation.class);
        verify(evaluationMapper).insert(captor.capture());
        // 接单方(8L)评发布方(7L)，被评价角色应为 PUBLISHER。
        assertThat(captor.getValue().getRevieweeId()).isEqualTo(7L);
        assertThat(captor.getValue().getRevieweeRole()).isEqualTo(PickupParticipantRole.PUBLISHER);
    }

    @Test
    void submit_nonParticipant_throws403() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(completedCtx());

        EvaluationCreateRequest req = new EvaluationCreateRequest();
        req.setRatingLevel(RatingLevel.GOOD);

        assertThatThrownBy(() -> evaluationService.submitEvaluation(5L, 99L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(evaluationMapper, never()).insert(any(Evaluation.class));
    }

    @Test
    void submit_notCompleted_throws409() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(
                PickupEvaluationContext.builder()
                        .pickupId(5L).publisherId(7L).acceptorId(8L)
                        .status(PickupStatus.IN_PROGRESS).build());

        EvaluationCreateRequest req = new EvaluationCreateRequest();
        req.setRatingLevel(RatingLevel.GOOD);

        assertThatThrownBy(() -> evaluationService.submitEvaluation(5L, 7L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
        verify(evaluationMapper, never()).insert(any(Evaluation.class));
    }

    @Test
    void submit_alreadyEvaluated_throws409_byPrecheck() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(completedCtx());
        when(evaluationMapper.selectCount(any())).thenReturn(1L);

        EvaluationCreateRequest req = new EvaluationCreateRequest();
        req.setRatingLevel(RatingLevel.GOOD);

        assertThatThrownBy(() -> evaluationService.submitEvaluation(5L, 7L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
        verify(evaluationMapper, never()).insert(any(Evaluation.class));
    }

    @Test
    void submit_duplicateKeyRace_throws409() {
        when(pickupService.queryPickupEvaluationContext(5L)).thenReturn(completedCtx());
        when(evaluationMapper.selectCount(any())).thenReturn(0L);
        // 预检通过但并发插入撞唯一约束，兜底翻译为 409。
        when(evaluationMapper.insert(any(Evaluation.class)))
                .thenThrow(new DuplicateKeyException("uk_evaluations_once"));

        EvaluationCreateRequest req = new EvaluationCreateRequest();
        req.setRatingLevel(RatingLevel.GOOD);

        assertThatThrownBy(() -> evaluationService.submitEvaluation(5L, 7L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
        verify(notificationService, never()).createNotice(any(), any(), any(), any(), any(), any());
    }

    // ---------------- 好评率聚合 ----------------

    @Test
    void ratingSummary_aggregatesByRoleAndLevel() {
        // 用户 9L：作为接单方收到 2 好评 + 1 差评；作为发布方收到 1 中评。
        when(evaluationMapper.selectList(any())).thenReturn(List.of(
                eval(PickupParticipantRole.ACCEPTOR, RatingLevel.GOOD),
                eval(PickupParticipantRole.ACCEPTOR, RatingLevel.GOOD),
                eval(PickupParticipantRole.ACCEPTOR, RatingLevel.BAD),
                eval(PickupParticipantRole.PUBLISHER, RatingLevel.NEUTRAL)));

        RatingSummary summary = evaluationService.queryUserRatingSummary(9L);

        assertThat(summary.getUserId()).isEqualTo(9L);
        assertThat(summary.getAcceptorRoleSummary().getTotalCount()).isEqualTo(3);
        assertThat(summary.getAcceptorRoleSummary().getPositiveCount()).isEqualTo(2);
        assertThat(summary.getAcceptorRoleSummary().getNegativeCount()).isEqualTo(1);
        assertThat(summary.getAcceptorRoleSummary().getPositiveRate())
                .isEqualTo(2.0 / 3.0);
        assertThat(summary.getPublisherRoleSummary().getTotalCount()).isEqualTo(1);
        assertThat(summary.getPublisherRoleSummary().getNeutralCount()).isEqualTo(1);
        assertThat(summary.getPublisherRoleSummary().getPositiveRate()).isEqualTo(0.0);
    }

    @Test
    void ratingSummary_noEvaluations_zeroRate() {
        when(evaluationMapper.selectList(any())).thenReturn(List.of());

        RatingSummary summary = evaluationService.queryUserRatingSummary(9L);

        assertThat(summary.getPublisherRoleSummary().getTotalCount()).isZero();
        assertThat(summary.getPublisherRoleSummary().getPositiveRate()).isEqualTo(0.0);
        assertThat(summary.getAcceptorRoleSummary().getTotalCount()).isZero();
    }

    private Evaluation eval(PickupParticipantRole role, RatingLevel level) {
        Evaluation e = new Evaluation();
        e.setRevieweeRole(role);
        e.setRatingLevel(level);
        return e;
    }
}
