package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
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
import com.campushub.dto.pickup.UserSummary;
import com.campushub.entity.PickupRequest;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.FileBusinessType;
import com.campushub.entity.enums.FileUsage;
import com.campushub.entity.enums.NotificationType;
import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RewardType;
import com.campushub.mapper.PickupRequestMapper;
import com.campushub.service.FileStorageService;
import com.campushub.service.NotificationService;
import com.campushub.service.PointService;
import com.campushub.service.PickupService;
import com.campushub.service.UserService;
import com.campushub.service.dto.PickupEvaluationContext;
import com.campushub.service.dto.StoredFileContent;
import com.campushub.service.dto.UserBrief;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link PickupService} 实现。
 *
 * <p>状态流转用<b>条件更新</b>（{@code WHERE id=? AND status=?}）保证并发安全：先读取做业务校验，
 * 落库时再带状态条件，update 影响行数为 0 视为状态已被并发改变，按业务冲突处理（第四批准则：
 * 状态条件更新，不引乐观锁 version 列）。
 *
 * <p>跨模块协作：认证门槛与用户摘要经 {@link UserService}（owner service），凭证文件经
 * {@link FileStorageService}（只回 fileId、受信读取），有报酬服务的积分扣减/退回/转入经
 * {@link PointService} 接口。关键状态变更（接单/上传凭证/确认完成/取消）后经
 * {@link NotificationService} 通知对手方。
 */
@Service
@RequiredArgsConstructor
public class PickupServiceImpl implements PickupService {

    /** 物品说明预览长度上限，超出截断（列表项用）。 */
    private static final int DESC_PREVIEW_LEN = 100;

    private final PickupRequestMapper pickupRequestMapper;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final PointService pointService;
    private final NotificationService notificationService;

    // ---------------- 发布 ----------------

    @Override
    @Transactional
    public PickupCreateResult publishPickup(Long currentUserId, PickupCreateRequest request,
                                            MultipartFile pickupCredential) {
        userService.ensureCertified(currentUserId);

        // 先上传取件凭证拿 fileId（文件模块只回 id、不判权限）。
        Long credentialFileId = fileStorageService.uploadImage(
                pickupCredential, currentUserId, FileUsage.PICKUP_CREDENTIAL,
                FileBusinessType.PICKUP_REQUEST, null);

        PickupRequest pickup = new PickupRequest();
        pickup.setPublisherId(currentUserId);
        pickup.setCampus(request.getCampus());
        pickup.setPickupLocation(request.getPickupLocation());
        pickup.setDeliveryLocation(request.getDeliveryLocation());
        pickup.setItemDescription(request.getItemDescription());
        pickup.setRewardType(request.getRewardType());
        pickup.setRewardAmount(request.getRewardAmount());
        pickup.setPickupCredentialFileId(credentialFileId);
        pickup.setAcceptDeadline(request.getAcceptDeadline());

        // 有报酬与无报酬发布成功后均直接进入待接单；有报酬先落库拿到 pickupId 再扣积分
        // （积分流水需 relatedPickupId 溯源），扣减失败（余额不足）抛 409、事务整体回滚。
        pickup.markWaitingAccept();
        pickupRequestMapper.insert(pickup);
        // 回填取件凭证文件溯源到代取请求 ID。
        fileStorageService.updateBusinessTrace(credentialFileId,
                FileBusinessType.PICKUP_REQUEST, pickup.getId());

        if (request.getRewardType() == RewardType.PAID) {
            pointService.spendForPublish(currentUserId,
                    toPoints(request.getRewardAmount()), pickup.getId());
        }

        notificationService.createNotice(currentUserId, NotificationType.PICKUP,
                "代取服务发布成功", "您的代取服务已发布，正在等待其他用户接单。",
                BusinessType.PICKUP_REQUEST.name(), pickup.getId());

        return PickupCreateResult.builder()
                .pickupId(pickup.getId())
                .status(pickup.getStatus())
                .build();
    }

    // ---------------- 浏览 / 详情 ----------------

    @Override
    public PageResult<PickupRequestSummary> queryHall(String campus, RewardType rewardType,
                                                      PageQuery pageQuery) {
        LambdaQueryWrapper<PickupRequest> wrapper = Wrappers.<PickupRequest>lambdaQuery()
                .eq(PickupRequest::getStatus, PickupStatus.WAITING_ACCEPT)
                .eq(campus != null, PickupRequest::getCampus, campus)
                .eq(rewardType != null, PickupRequest::getRewardType, rewardType)
                .orderByDesc(PickupRequest::getCreatedAt);

        Page<PickupRequest> page = pickupRequestMapper.selectPage(pageQuery.toMpPage(), wrapper);
        Map<Long, UserSummary> publishers = loadUserSummaries(
                page.getRecords().stream().map(PickupRequest::getPublisherId).toList());

        List<PickupRequestSummary> list = page.getRecords().stream()
                .map(p -> toHallSummary(p, publishers.get(p.getPublisherId())))
                .toList();
        return PageResult.of(page, list);
    }

    @Override
    public PickupRequestDetail queryDetail(Long pickupId) {
        PickupRequest pickup = requirePickup(pickupId);
        UserSummary publisher = loadUserSummaries(List.of(pickup.getPublisherId()))
                .get(pickup.getPublisherId());
        UserSummary acceptor = pickup.getAcceptorId() == null ? null
                : loadUserSummaries(List.of(pickup.getAcceptorId())).get(pickup.getAcceptorId());

        return PickupRequestDetail.builder()
                .pickupId(pickup.getId())
                .campus(pickup.getCampus())
                .pickupLocation(pickup.getPickupLocation())
                .deliveryLocation(pickup.getDeliveryLocation())
                .itemDescriptionPreview(preview(pickup.getItemDescription()))
                .rewardType(pickup.getRewardType())
                .rewardAmount(pickup.getRewardAmount())
                .status(pickup.getStatus())
                .cancelReason(pickup.getCancelReason())
                .publisher(publisher)
                .acceptDeadline(pickup.getAcceptDeadline())
                .createdAt(pickup.getCreatedAt())
                .itemDescription(pickup.getItemDescription())
                .acceptor(acceptor)
                .acceptedAt(pickup.getAcceptedAt())
                .completedAt(pickup.getCompletedAt())
                .build();
    }

    // ---------------- 取件凭证 ----------------

    @Override
    public StoredFileContent loadPickupCredential(Long pickupId, Long currentUserId) {
        PickupRequest pickup = requirePickup(pickupId);
        if (!pickup.canViewPickupCredential(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorReason.NOT_PICKUP_PARTICIPANT);
        }
        return fileStorageService.loadFile(pickup.getPickupCredentialFileId());
    }

    // ---------------- 接单 ----------------

    @Override
    @Transactional
    public PickupAcceptResult acceptPickup(Long pickupId, Long currentUserId) {
        userService.ensureCertified(currentUserId);
        PickupRequest pickup = requirePickup(pickupId);

        if (pickup.isPublisher(currentUserId)) {
            // 发布方不能接自己单；契约无专门 reason，归状态不允许语义。
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.PICKUP_STATUS_NOT_ALLOWED,
                    "不能接取自己发布的代取服务");
        }
        if (pickup.getStatus() != PickupStatus.WAITING_ACCEPT) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.PICKUP_STATUS_NOT_ALLOWED);
        }
        // 接单时超时：先流转为取消，再以 409 拒绝本次接单。
        if (pickup.getAcceptDeadline() != null
                && pickup.getAcceptDeadline().isBefore(LocalDateTime.now())) {
            pickup.markCancelled(PickupCancelReason.ACCEPT_DEADLINE_EXPIRED, null);
            updateOnStatus(pickup, PickupStatus.WAITING_ACCEPT);
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.ACCEPT_DEADLINE_EXPIRED);
        }

        pickup.markAccepted(currentUserId);
        requireAffected(updateOnStatus(pickup, PickupStatus.WAITING_ACCEPT));
        notificationService.createNotice(pickup.getPublisherId(), NotificationType.PICKUP,
                "代取服务已被接单", "您发布的代取服务已被接单，接单方将尽快取件。",
                BusinessType.PICKUP_REQUEST.name(), pickup.getId());
        return PickupAcceptResult.builder()
                .status(pickup.getStatus())
                .acceptedAt(pickup.getAcceptedAt())
                .build();
    }

    // ---------------- 完成凭证 ----------------

    @Override
    @Transactional
    public void uploadCompletionProof(Long pickupId, Long currentUserId, MultipartFile proofImage) {
        PickupRequest pickup = requirePickup(pickupId);
        if (!pickup.isAcceptor(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorReason.NOT_PICKUP_PARTICIPANT);
        }
        if (pickup.getStatus() != PickupStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.PICKUP_STATUS_NOT_ALLOWED);
        }

        Long proofFileId = fileStorageService.uploadImage(
                proofImage, currentUserId, FileUsage.COMPLETION_PROOF,
                FileBusinessType.PICKUP_REQUEST, pickupId);
        pickup.recordCompletionProof(proofFileId);
        requireAffected(updateOnStatus(pickup, PickupStatus.IN_PROGRESS));
        notificationService.createNotice(pickup.getPublisherId(), NotificationType.PICKUP,
                "完成凭证已上传", "接单方已上传完成凭证，请查看并确认完成。",
                BusinessType.PICKUP_REQUEST.name(), pickup.getId());
    }

    @Override
    public StoredFileContent loadCompletionProof(Long pickupId, Long currentUserId) {
        PickupRequest pickup = requirePickup(pickupId);
        if (!pickup.isPublisher(currentUserId) && !pickup.isAcceptor(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorReason.NOT_PICKUP_PARTICIPANT);
        }
        if (pickup.getCompletionProofFileId() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.COMPLETION_PROOF_NOT_AVAILABLE);
        }
        return fileStorageService.loadFile(pickup.getCompletionProofFileId());
    }

    // ---------------- 确认完成 ----------------

    @Override
    @Transactional
    public CompletionConfirmResult confirmComplete(Long pickupId, Long currentUserId) {
        PickupRequest pickup = requirePickup(pickupId);
        if (!pickup.isPublisher(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorReason.NOT_PICKUP_PARTICIPANT);
        }
        if (pickup.getStatus() != PickupStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.PICKUP_STATUS_NOT_ALLOWED);
        }
        if (pickup.getCompletionProofFileId() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.COMPLETION_PROOF_NOT_AVAILABLE);
        }

        pickup.markCompleted();
        requireAffected(updateOnStatus(pickup, PickupStatus.IN_PROGRESS));

        // 有报酬服务：把发布时扣减的报酬积分转入接单方账户（发布方扣减已在发布时记账）。
        if (pickup.isPaid()) {
            pointService.transferOnComplete(pickup.getPublisherId(), pickup.getAcceptorId(),
                    toPoints(pickup.getRewardAmount()), pickup.getId());
            // 报酬入账提醒（独立于下方的「确认完成」业务通知，一业务一账务）。
            notificationService.createNotice(pickup.getAcceptorId(), NotificationType.PAYMENT,
                    "报酬积分已到账", "本次代取服务的报酬积分已转入您的账户。",
                    BusinessType.PICKUP_REQUEST.name(), pickup.getId());
        }
        notificationService.createNotice(pickup.getAcceptorId(), NotificationType.PICKUP,
                "代取服务已确认完成", "发布方已确认完成本次代取服务，感谢您的帮助。",
                BusinessType.PICKUP_REQUEST.name(), pickup.getId());
        return CompletionConfirmResult.builder()
                .status(pickup.getStatus())
                .completedAt(pickup.getCompletedAt())
                .build();
    }

    // ---------------- 取消 ----------------

    @Override
    @Transactional
    public PickupCancelResult cancelPickup(Long pickupId, Long currentUserId, String cancelDetail) {
        PickupRequest pickup = requirePickup(pickupId);
        if (!pickup.isPublisher(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorReason.NOT_PICKUP_PARTICIPANT);
        }

        PickupStatus from = pickup.getStatus();
        // 仅 WAITING_ACCEPT 可由发布方主动取消；IN_PROGRESS / COMPLETED / CANCELLED 不可取消。
        if (from != PickupStatus.WAITING_ACCEPT) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.PICKUP_STATUS_NOT_ALLOWED);
        }

        pickup.markCancelled(PickupCancelReason.USER_CANCELLED, cancelDetail);
        requireAffected(updateOnStatus(pickup, from));

        // 有报酬服务：把发布时扣减的报酬积分退回发布方账户（无报酬无需处理积分）。
        if (pickup.isPaid()) {
            pointService.refundForCancel(pickup.getPublisherId(),
                    toPoints(pickup.getRewardAmount()), pickup.getId());
        }
        return PickupCancelResult.builder()
                .status(pickup.getStatus())
                .cancelReason(pickup.getCancelReason())
                .build();
    }

    // ---------------- 我的发布 / 我的接单 ----------------

    @Override
    public PageResult<PickupSummary> queryMyPublished(Long currentUserId, PickupStatus status,
                                                      PageQuery pageQuery) {
        return queryMine(PickupRequest::getPublisherId, currentUserId, status, pageQuery);
    }

    @Override
    public PageResult<PickupSummary> queryMyAccepted(Long currentUserId, PickupStatus status,
                                                     PageQuery pageQuery) {
        return queryMine(PickupRequest::getAcceptorId, currentUserId, status, pageQuery);
    }

    private PageResult<PickupSummary> queryMine(
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<PickupRequest, ?> ownerColumn,
            Long userId, PickupStatus status, PageQuery pageQuery) {
        LambdaQueryWrapper<PickupRequest> wrapper = Wrappers.<PickupRequest>lambdaQuery()
                .eq(ownerColumn, userId)
                .eq(status != null, PickupRequest::getStatus, status)
                .orderByDesc(PickupRequest::getCreatedAt);
        Page<PickupRequest> page = pickupRequestMapper.selectPage(pageQuery.toMpPage(), wrapper);
        List<PickupSummary> list = page.getRecords().stream().map(this::toSummary).toList();
        return PageResult.of(page, list);
    }

    // ---------------- 评价上下文（供评价模块） ----------------

    @Override
    public PickupEvaluationContext queryPickupEvaluationContext(Long pickupId) {
        PickupRequest pickup = requirePickup(pickupId);
        return PickupEvaluationContext.builder()
                .pickupId(pickup.getId())
                .publisherId(pickup.getPublisherId())
                .acceptorId(pickup.getAcceptorId())
                .status(pickup.getStatus())
                .build();
    }

    // ---------------- 私有辅助 ----------------

    /** 条件更新：仅当库中状态仍为 expected 时落库，返回影响行数（0 表示并发改变）。 */
    private int updateOnStatus(PickupRequest pickup, PickupStatus expected) {
        return pickupRequestMapper.update(pickup, Wrappers.<PickupRequest>lambdaUpdate()
                .eq(PickupRequest::getId, pickup.getId())
                .eq(PickupRequest::getStatus, expected));
    }

    private void requireAffected(int affected) {
        if (affected == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.PICKUP_STATUS_NOT_ALLOWED);
        }
    }

    private PickupRequest requirePickup(Long pickupId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorReason.RESOURCE_NOT_FOUND,
                    "代取服务不存在");
        }
        return pickup;
    }

    /** 批量查公开摘要，构成 userId→UserSummary 映射（复用 UserService.getUserBriefs，避 N+1）。 */
    private Map<Long, UserSummary> loadUserSummaries(Collection<Long> userIds) {
        List<UserBrief> briefs = userService.getUserBriefs(userIds);
        return briefs.stream().collect(Collectors.toMap(
                UserBrief::getUserId,
                b -> UserSummary.builder().userId(b.getUserId()).nickname(b.getNickname()).build()));
    }

    private PickupRequestSummary toHallSummary(PickupRequest p, UserSummary publisher) {
        return PickupRequestSummary.builder()
                .pickupId(p.getId())
                .campus(p.getCampus())
                .pickupLocation(p.getPickupLocation())
                .deliveryLocation(p.getDeliveryLocation())
                .itemDescriptionPreview(preview(p.getItemDescription()))
                .rewardType(p.getRewardType())
                .rewardAmount(p.getRewardAmount())
                .status(p.getStatus())
                .cancelReason(p.getCancelReason())
                .publisher(publisher)
                .acceptDeadline(p.getAcceptDeadline())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PickupSummary toSummary(PickupRequest p) {
        return PickupSummary.builder()
                .pickupId(p.getId())
                .campus(p.getCampus())
                .pickupLocation(p.getPickupLocation())
                .deliveryLocation(p.getDeliveryLocation())
                .itemDescriptionPreview(preview(p.getItemDescription()))
                .rewardType(p.getRewardType())
                .rewardAmount(p.getRewardAmount())
                .status(p.getStatus())
                .cancelReason(p.getCancelReason())
                .createdAt(p.getCreatedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }

    private String preview(String desc) {
        if (desc == null) {
            return null;
        }
        return desc.length() <= DESC_PREVIEW_LEN ? desc : desc.substring(0, DESC_PREVIEW_LEN);
    }

    /**
     * 报酬金额（DECIMAL，1-200 整数元）→ 积分量（long）。
     * 金额由 DTO 校验保证为 1-200 且发布时为整数元；{@code longValueExact} 对含非零小数的值抛异常，
     * 作为防御性兜底（库表 CHECK 也限定 PAID 金额范围）。
     */
    private long toPoints(java.math.BigDecimal rewardAmount) {
        return rewardAmount.longValueExact();
    }
}
