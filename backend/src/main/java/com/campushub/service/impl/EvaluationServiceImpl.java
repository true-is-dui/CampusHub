package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.dto.*;
import com.campushub.entity.Evaluation;
import com.campushub.entity.PickupRequest;
import com.campushub.entity.User;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RatingLevel;
import com.campushub.mapper.EvaluationMapper;
import com.campushub.mapper.PickupRequestMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final PickupRequestMapper pickupRequestMapper;
    private final UserMapper userMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EvaluationEligibilityDTO queryEvaluationEligibility(Long pickupId, Long reviewerId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null) {
            throw new BusinessException(40401, "代拿请求不存在");
        }

        EvaluationEligibilityDTO dto = new EvaluationEligibilityDTO();

        if (pickup.getStatus() != PickupStatus.COMPLETED) {
            dto.setCanEvaluate(false);
            dto.setReason("代拿请求尚未完成");
            return dto;
        }

        boolean isPublisher = reviewerId.equals(pickup.getPublisherId());
        boolean isAcceptor = reviewerId.equals(pickup.getAcceptorId());
        if (!isPublisher && !isAcceptor) {
            dto.setCanEvaluate(false);
            dto.setReason("您不是该代拿请求的参与者");
            return dto;
        }

        QueryWrapper<Evaluation> wrapper = new QueryWrapper<>();
        wrapper.eq("business_type", BusinessType.PICKUP_REQUEST)
                .eq("business_id", pickupId)
                .eq("reviewer_id", reviewerId);
        if (evaluationMapper.selectCount(wrapper) > 0) {
            dto.setCanEvaluate(false);
            dto.setReason("您已经评价过该代拿请求");
            return dto;
        }

        dto.setCanEvaluate(true);
        Long revieweeId = isPublisher ? pickup.getAcceptorId() : pickup.getPublisherId();
        User reviewee = userMapper.selectById(revieweeId);
        if (reviewee != null) {
            UserSummaryDTO revieweeDTO = new UserSummaryDTO();
            revieweeDTO.setUserId(reviewee.getId());
            revieweeDTO.setNickname(reviewee.getNickname());
            dto.setReviewee(revieweeDTO);
        }

        return dto;
    }

    public Long submitEvaluation(Long pickupId, Long reviewerId, String ratingLevel, String content) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null) {
            throw new BusinessException(40401, "代拿请求不存在");
        }
        if (pickup.getStatus() != PickupStatus.COMPLETED) {
            throw new BusinessException(40001, "代拿请求尚未完成，无法评价");
        }

        boolean isPublisher = reviewerId.equals(pickup.getPublisherId());
        boolean isAcceptor = reviewerId.equals(pickup.getAcceptorId());
        if (!isPublisher && !isAcceptor) {
            throw new BusinessException(40301, "您不是该代拿请求的参与者");
        }

        QueryWrapper<Evaluation> existWrapper = new QueryWrapper<>();
        existWrapper.eq("business_type", BusinessType.PICKUP_REQUEST)
                .eq("business_id", pickupId)
                .eq("reviewer_id", reviewerId);
        if (evaluationMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException(40002, "您已经评价过该代拿请求");
        }

        Long revieweeId = isPublisher ? pickup.getAcceptorId() : pickup.getPublisherId();
        PickupParticipantRole revieweeRole = isPublisher ? PickupParticipantRole.ACCEPTOR : PickupParticipantRole.PUBLISHER;

        Evaluation evaluation = new Evaluation();
        evaluation.setReviewerId(reviewerId);
        evaluation.setRevieweeId(revieweeId);
        evaluation.setBusinessType(BusinessType.PICKUP_REQUEST);
        evaluation.setBusinessId(pickupId);
        evaluation.setRevieweeRoleInBusiness(revieweeRole);
        evaluation.setRatingLevel(RatingLevel.valueOf(ratingLevel));
        evaluation.setContent(content);
        evaluationMapper.insert(evaluation);

        return evaluation.getId();
    }

    public RatingSummaryDTO queryUserRatingSummary(Long userId) {
        QueryWrapper<Evaluation> wrapper = new QueryWrapper<>();
        wrapper.eq("reviewee_id", userId);
        List<Evaluation> evaluations = evaluationMapper.selectList(wrapper);

        Map<PickupParticipantRole, List<Evaluation>> grouped = evaluations.stream()
                .collect(Collectors.groupingBy(Evaluation::getRevieweeRoleInBusiness));

        RatingSummaryDTO summary = new RatingSummaryDTO();
        summary.setUserId(userId);
        summary.setPublisherRoleSummary(buildRoleSummary(grouped.get(PickupParticipantRole.PUBLISHER)));
        summary.setAcceptorRoleSummary(buildRoleSummary(grouped.get(PickupParticipantRole.ACCEPTOR)));
        return summary;
    }

    public Map<String, Object> queryUserEvaluations(Long userId, int page, int pageSize) {
        Page<Evaluation> pageObj = new Page<>(page, pageSize);
        QueryWrapper<Evaluation> wrapper = new QueryWrapper<>();
        wrapper.eq("reviewee_id", userId).orderByDesc("created_at");

        IPage<Evaluation> result = evaluationMapper.selectPage(pageObj, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", result.getRecords().stream().map(this::toEvaluationHistorySummaryDTO).toList());
        return data;
    }

    private RatingRoleSummaryDTO buildRoleSummary(List<Evaluation> evaluations) {
        RatingRoleSummaryDTO dto = new RatingRoleSummaryDTO();
        if (evaluations == null || evaluations.isEmpty()) {
            dto.setPositiveCount(0);
            dto.setNeutralCount(0);
            dto.setNegativeCount(0);
            dto.setTotalCount(0);
            dto.setPositiveRate(0.0);
            return dto;
        }

        int positive = 0, neutral = 0, negative = 0;
        for (Evaluation e : evaluations) {
            if (e.getRatingLevel() == RatingLevel.GOOD) positive++;
            else if (e.getRatingLevel() == RatingLevel.NEUTRAL) neutral++;
            else if (e.getRatingLevel() == RatingLevel.BAD) negative++;
        }

        int total = evaluations.size();
        dto.setRevieweeRoleInBusiness(evaluations.get(0).getRevieweeRoleInBusiness().name());
        dto.setPositiveCount(positive);
        dto.setNeutralCount(neutral);
        dto.setNegativeCount(negative);
        dto.setTotalCount(total);
        dto.setPositiveRate(total > 0 ? (double) positive / total * 100 : 0.0);
        return dto;
    }

    private EvaluationHistorySummaryDTO toEvaluationHistorySummaryDTO(Evaluation evaluation) {
        EvaluationHistorySummaryDTO dto = new EvaluationHistorySummaryDTO();
        dto.setEvaluationId(evaluation.getId());
        dto.setRevieweeRoleInBusiness(evaluation.getRevieweeRoleInBusiness() != null
                ? evaluation.getRevieweeRoleInBusiness().name() : null);
        dto.setRatingLevel(evaluation.getRatingLevel() != null ? evaluation.getRatingLevel().name() : null);

        String content = evaluation.getContent();
        dto.setContentPreview(content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content);

        dto.setCreatedAt(evaluation.getCreatedAt() != null ? evaluation.getCreatedAt().format(DT_FMT) : null);
        return dto;
    }
}
