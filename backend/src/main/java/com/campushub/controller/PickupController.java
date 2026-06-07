package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.CurrentUserContext;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.evaluation.EvaluationCreateRequest;
import com.campushub.dto.evaluation.EvaluationEligibility;
import com.campushub.dto.evaluation.EvaluationSubmitResult;
import com.campushub.dto.pickup.CompletionConfirmResult;
import com.campushub.dto.pickup.PickupAcceptResult;
import com.campushub.dto.pickup.PickupCancelRequest;
import com.campushub.dto.pickup.PickupCancelResult;
import com.campushub.dto.pickup.PickupCreateRequest;
import com.campushub.dto.pickup.PickupCreateResult;
import com.campushub.dto.pickup.PickupRequestDetail;
import com.campushub.dto.pickup.PickupRequestSummary;
import com.campushub.entity.enums.RewardType;
import com.campushub.security.CurrentUser;
import com.campushub.service.PickupService;
import com.campushub.service.EvaluationService;
import com.campushub.service.dto.StoredFileContent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 代取服务接口，实现 {@code api_design.yaml} 的 PickupRequests 分组（{@code /pickup-requests/**}）。
 *
 * <p>controller 按 URL 路径前缀划分（与 Auth/User/Admin 一致）：本类只承载 {@code /pickup-requests}
 * 下的接口；「我的发布 / 我的接单」路径在 {@code /users/me/pickup-requests}，按路径归
 * {@code UserController}，由其委托回 {@link PickupService}（与 {@code /users/me/verification}
 * 委托给实名审核服务同模式）。业务逻辑统一在 {@link PickupService}。
 *
 * <p>大厅浏览与详情为公开 GET（{@code security: []}），不带 {@code @CurrentUser}；其余接口需登录，
 * 身份由 {@code @CurrentUser} 从 JWT 注入。multipart 表单文本字段封 DTO 用
 * {@code @Valid @ModelAttribute} 入口校验，文件单独 {@code @RequestPart}。
 */
@RestController
@RequestMapping("/pickup-requests")
@RequiredArgsConstructor
public class PickupController {

    private final PickupService pickupService;
    private final EvaluationService evaluationService;

    // ---------------- 发布 / 浏览 / 详情 ----------------

    /** 发布代取服务（取件凭证随表单上传）。 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PickupCreateResult> publish(@CurrentUser CurrentUserContext me,
                                                   @Valid @ModelAttribute PickupCreateRequest request,
                                                   @RequestPart MultipartFile pickupCredential) {
        return ApiResponse.ok(pickupService.publishPickup(me.getCurrentUserId(), request, pickupCredential));
    }

    /** 代取大厅公开列表：仅待接单，按校区 / 报酬类型筛选。 */
    @GetMapping
    public ApiResponse<PageResult<PickupRequestSummary>> hall(
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) RewardType rewardType,
            @Valid PageQuery pageQuery) {
        return ApiResponse.ok(pickupService.queryHall(campus, rewardType, pageQuery));
    }

    /** 代取详情，公开访问。 */
    @GetMapping("/{pickupId}")
    public ApiResponse<PickupRequestDetail> detail(@PathVariable Long pickupId) {
        return ApiResponse.ok(pickupService.queryDetail(pickupId));
    }

    /** 读取取件凭证图片（仅参与者）。 */
    @GetMapping("/{pickupId}/credential")
    public ResponseEntity<Resource> credential(@CurrentUser CurrentUserContext me,
                                               @PathVariable Long pickupId) {
        StoredFileContent content = pickupService.loadPickupCredential(pickupId, me.getCurrentUserId());
        return imageResponse(content);
    }

    // ---------------- 接单 / 完成 / 取消 ----------------

    /** 接单。 */
    @PostMapping("/{pickupId}/accept")
    public ApiResponse<PickupAcceptResult> accept(@CurrentUser CurrentUserContext me,
                                                  @PathVariable Long pickupId) {
        return ApiResponse.ok(pickupService.acceptPickup(pickupId, me.getCurrentUserId()));
    }

    /** 上传完成凭证（仅接单方）。 */
    @PostMapping(value = "/{pickupId}/completion-proof",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> uploadCompletionProof(@CurrentUser CurrentUserContext me,
                                                    @PathVariable Long pickupId,
                                                    @RequestPart MultipartFile proofImage) {
        pickupService.uploadCompletionProof(pickupId, me.getCurrentUserId(), proofImage);
        return ApiResponse.ok();
    }

    /** 读取完成凭证图片（仅参与者）。 */
    @GetMapping("/{pickupId}/completion-proof")
    public ResponseEntity<Resource> completionProof(@CurrentUser CurrentUserContext me,
                                                    @PathVariable Long pickupId) {
        StoredFileContent content = pickupService.loadCompletionProof(pickupId, me.getCurrentUserId());
        return imageResponse(content);
    }

    /** 发布方确认完成。 */
    @PostMapping("/{pickupId}/completion-confirmation")
    public ApiResponse<CompletionConfirmResult> confirmComplete(@CurrentUser CurrentUserContext me,
                                                                @PathVariable Long pickupId) {
        return ApiResponse.ok(pickupService.confirmComplete(pickupId, me.getCurrentUserId()));
    }

    /** 发布方取消（请求体可选）。 */
    @PostMapping("/{pickupId}/cancel")
    public ApiResponse<PickupCancelResult> cancel(@CurrentUser CurrentUserContext me,
                                                  @PathVariable Long pickupId,
                                                  @Valid @RequestBody(required = false) PickupCancelRequest request) {
        String detail = request == null ? null : request.getReason();
        return ApiResponse.ok(pickupService.cancelPickup(pickupId, me.getCurrentUserId(), detail));
    }

    // ---------------- 评价（按路径前缀归本 controller，委托评价服务） ----------------

    /**
     * 提交代取服务评价。被评价人不由前端提交，后端按 pickupId + 当前用户推导。
     *
     * <p>路径在 {@code /pickup-requests/{id}} 下，归本 controller；业务委托 {@link EvaluationService}。
     */
    @PostMapping("/{pickupId}/evaluations")
    public ApiResponse<EvaluationSubmitResult> submitEvaluation(
            @CurrentUser CurrentUserContext me,
            @PathVariable Long pickupId,
            @Valid @RequestBody EvaluationCreateRequest request) {
        return ApiResponse.ok(
                evaluationService.submitEvaluation(pickupId, me.getCurrentUserId(), request));
    }

    /** 查询当前用户对该代取服务的评价资格（供前端决定是否展示评价入口）。 */
    @GetMapping("/{pickupId}/evaluation-eligibility")
    public ApiResponse<EvaluationEligibility> evaluationEligibility(
            @CurrentUser CurrentUserContext me,
            @PathVariable Long pickupId) {
        return ApiResponse.ok(
                evaluationService.queryEvaluationEligibility(pickupId, me.getCurrentUserId()));
    }

    private ResponseEntity<Resource> imageResponse(StoredFileContent content) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.getMimeType()))
                .contentLength(content.getFileSize())
                .body(content.getResource());
    }
}
