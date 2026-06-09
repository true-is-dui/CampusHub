package com.campushub.service;

import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.pickup.CompletionConfirmResult;
import com.campushub.dto.pickup.PickupAcceptResult;
import com.campushub.dto.pickup.PickupCancelResult;
import com.campushub.dto.pickup.PickupCreateRequest;
import com.campushub.dto.pickup.PickupCreateResult;
import com.campushub.dto.pickup.PickupRequestDetail;
import com.campushub.dto.pickup.PickupRequestSummary;
import com.campushub.dto.pickup.PickupSummary;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RewardType;
import com.campushub.service.dto.PickupEvaluationContext;
import com.campushub.service.dto.StoredFileContent;
import org.springframework.web.multipart.MultipartFile;

/**
 * 代取服务，对应 {@code class_design.md} 的 {@code PickupService}：负责身份、权限、状态校验，
 * 以及与积分 / 文件 / 通知模块的协作。代取业务状态以 {@code PickupRequest.status} 为准，
 * 各方法内部按 {@code PickupStatus} 做前置校验。
 *
 * <p>方法签名显式收 {@code currentUserId}（由 {@code @CurrentUser} 注入），不信任前端传入身份。
 * 有报酬服务的报酬以平台积分计价：发布即扣减发布方积分、完成转入接单方、待接单阶段取消退回，
 * 积分变动经 {@code PointService}（同一事务内完成）。
 */
public interface PickupService {

    /**
     * 发布代取服务（取件凭证图片随表单上传）。需实名认证通过。
     * 无报酬与有报酬服务发布成功后均直接进入 WAITING_ACCEPT；有报酬服务发布时从发布方账户
     * 扣减报酬积分，余额不足则发布失败（409 INSUFFICIENT_POINTS）。
     */
    PickupCreateResult publishPickup(Long currentUserId, PickupCreateRequest request,
                                     MultipartFile pickupCredential);

    /** 代取大厅：仅 WAITING_ACCEPT，按校区 / 报酬类型筛选，发布时间倒序。公开访问。 */
    PageResult<PickupRequestSummary> queryHall(String campus, RewardType rewardType, PageQuery pageQuery);

    /** 查看代取详情。公开访问，不返回取件 / 完成凭证图片。 */
    PickupRequestDetail queryDetail(Long pickupId);

    /** 读取取件凭证图片：仅参与者（发布方 / 接单方）可读；调用方先鉴权再读文件。 */
    StoredFileContent loadPickupCredential(Long pickupId, Long currentUserId);

    /**
     * 接单。需实名认证通过；接单方不得是发布方；服务须为 WAITING_ACCEPT。
     * 若已超过 acceptDeadline，先把服务流转为 CANCELLED(ACCEPT_DEADLINE_EXPIRED) 再返回 409。
     */
    PickupAcceptResult acceptPickup(Long pickupId, Long currentUserId);

    /** 接单方上传单张完成凭证；服务须为 IN_PROGRESS。 */
    void uploadCompletionProof(Long pickupId, Long currentUserId, MultipartFile proofImage);

    /** 读取完成凭证图片：仅参与者可读，且须已上传完成凭证。 */
    StoredFileContent loadCompletionProof(Long pickupId, Long currentUserId);

    /**
     * 发布方确认完成：服务须为 IN_PROGRESS 且已上传完成凭证。
     * 流转为 COMPLETED；有报酬服务把发布时扣减的报酬积分转入接单方账户。
     */
    CompletionConfirmResult confirmComplete(Long pickupId, Long currentUserId);

    /**
     * 发布方取消：仅 WAITING_ACCEPT 可取消。无报酬直接取消；有报酬把发布时扣减的积分退回发布方。
     * IN_PROGRESS / COMPLETED / CANCELLED 不可取消。
     */
    PickupCancelResult cancelPickup(Long pickupId, Long currentUserId, String cancelDetail);

    /** 我的发布：按状态可选筛选，返回 PickupSummary 分页。 */
    PageResult<PickupSummary> queryMyPublished(Long currentUserId, PickupStatus status, PageQuery pageQuery);

    /** 我的接单：按状态可选筛选，返回 PickupSummary 分页。 */
    PageResult<PickupSummary> queryMyAccepted(Long currentUserId, PickupStatus status, PageQuery pageQuery);

    /**
     * 向评价模块提供代取服务的评价上下文（发布方 / 接单方 / 状态）。
     *
     * <p>跨模块只回业务层 DTO、不暴露代取实体（第四批准则）；评价模块据此判断参与者、
     * 推导被评价人与角色、校验服务是否已完成。代取服务不存在抛 404。
     */
    PickupEvaluationContext queryPickupEvaluationContext(Long pickupId);
}
