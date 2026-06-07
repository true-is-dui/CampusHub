package com.campushub.service;

import com.campushub.common.CurrentUserContext;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.user.AdminHandleRequest;
import com.campushub.dto.user.VerificationReviewSummary;
import com.campushub.dto.user.VerificationSubmitResponse;
import com.campushub.entity.enums.ReviewStatus;
import com.campushub.service.dto.StoredFileContent;
import org.springframework.web.multipart.MultipartFile;

/**
 * 实名认证审核服务，编排实名认证的提交与审核流转。
 *
 * <p>提交（用户侧）与审核（管理员侧）都涉及 verification_reviews 记录，归口本服务；
 * 用户认证状态的变更通过 {@code UserService} 的状态变更原语完成（依赖方向单向：
 * 本服务 → UserService，无循环依赖）。
 */
public interface VerificationReviewService {

    /**
     * 用户提交实名认证申请：校验、保存材料、写审核记录、置用户为审核中。
     *
     * @param userId            当前登录用户 ID
     * @param studentId         学号
     * @param realName          真实姓名
     * @param verificationImage 认证材料图片
     * @return 提交后的认证状态（REVIEWING）
     */
    VerificationSubmitResponse submitVerification(Long userId, String studentId, String realName,
                                                  MultipartFile verificationImage);

    /** 管理员分页查询实名认证审核记录；status 为空时查询全部。 */
    PageResult<VerificationReviewSummary> queryReviews(CurrentUserContext admin,
                                                       ReviewStatus status,
                                                       PageQuery pageQuery);

    /** 管理员处理待审核记录。 */
    void handleReview(CurrentUserContext admin, Long reviewId, AdminHandleRequest request);

    /** 管理员读取认证材料图片。 */
    StoredFileContent loadReviewImage(CurrentUserContext admin, Long reviewId);
}

