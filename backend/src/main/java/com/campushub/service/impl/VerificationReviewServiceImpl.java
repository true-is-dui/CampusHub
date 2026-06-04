package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.dto.VerificationReviewSummaryDTO;
import com.campushub.entity.User;
import com.campushub.entity.VerificationReview;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.NotificationType;
import com.campushub.entity.enums.ReviewStatus;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.VerificationReviewMapper;
import com.campushub.service.VerificationReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerificationReviewServiceImpl implements VerificationReviewService {

    private final VerificationReviewMapper verificationReviewMapper;
    private final UserMapper userMapper;
    private final NotificationServiceImpl notificationService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> queryReviews(String status, int page, int pageSize) {
        Page<VerificationReview> pageObj = new Page<>(page, pageSize);
        QueryWrapper<VerificationReview> wrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", ReviewStatus.valueOf(status));
        }
        wrapper.orderByDesc("created_at");

        IPage<VerificationReview> result = verificationReviewMapper.selectPage(pageObj, wrapper);
        List<VerificationReviewSummaryDTO> list = result.getRecords().stream()
                .map(this::toVerificationReviewSummaryDTO)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", list);
        return data;
    }

    @Override
    @Transactional
    public void approveReview(Long reviewId, Long adminId) {
        VerificationReview review = verificationReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(40401, "审核记录不存在");
        }
        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new BusinessException(40001, "该审核记录已处理");
        }

        review.setStatus(ReviewStatus.APPROVED);
        review.setReviewerId(adminId);
        review.setReviewedAt(LocalDateTime.now());
        verificationReviewMapper.updateById(review);

        User user = userMapper.selectById(review.getUserId());
        if (user != null) {
            user.setAuthStatus(AuthStatus.APPROVED);
            userMapper.updateById(user);

            notificationService.createNotice(
                    user.getId(),
                    NotificationType.SYSTEM,
                    "实名认证通过",
                    "您的实名认证申请已通过审核。",
                    null,
                    null
            );
        }
    }

    @Override
    @Transactional
    public void rejectReview(Long reviewId, Long adminId, String reason) {
        VerificationReview review = verificationReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(40401, "审核记录不存在");
        }
        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new BusinessException(40001, "该审核记录已处理");
        }

        review.setStatus(ReviewStatus.REJECTED);
        review.setReviewerId(adminId);
        review.setRejectReason(reason);
        review.setReviewedAt(LocalDateTime.now());
        verificationReviewMapper.updateById(review);

        User user = userMapper.selectById(review.getUserId());
        if (user != null) {
            user.setAuthStatus(AuthStatus.REJECTED);
            userMapper.updateById(user);

            notificationService.createNotice(
                    user.getId(),
                    NotificationType.SYSTEM,
                    "实名认证未通过",
                    "您的实名认证申请未通过审核。原因：" + (reason != null ? reason : "未说明"),
                    null,
                    null
            );
        }
    }

    private VerificationReviewSummaryDTO toVerificationReviewSummaryDTO(VerificationReview review) {
        VerificationReviewSummaryDTO dto = new VerificationReviewSummaryDTO();
        dto.setId(review.getId());
        dto.setUserId(review.getUserId());
        dto.setStatus(review.getStatus() != null ? review.getStatus().name() : null);
        dto.setRejectReason(review.getRejectReason());
        dto.setReviewerId(review.getReviewerId());
        dto.setCreatedAt(review.getCreatedAt() != null ? review.getCreatedAt().format(DT_FMT) : null);
        dto.setReviewedAt(review.getReviewedAt() != null ? review.getReviewedAt().format(DT_FMT) : null);

        User user = userMapper.selectById(review.getUserId());
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setStudentIdMasked(maskStudentId(user.getStudentId()));
            dto.setRealName(user.getRealName());
        }

        return dto;
    }

    private String maskStudentId(String studentId) {
        if (studentId == null || studentId.isEmpty()) {
            return null;
        }
        if (studentId.length() <= 4) {
            return studentId;
        }
        return "****" + studentId.substring(studentId.length() - 4);
    }
}
