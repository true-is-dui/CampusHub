package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
import com.campushub.dto.user.UserMeResponse;
import com.campushub.dto.user.UserPublicProfile;
import com.campushub.entity.User;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.FileBusinessType;
import com.campushub.entity.enums.FileUsage;
import com.campushub.mapper.UserMapper;
import com.campushub.service.FileStorageService;
import com.campushub.service.dto.StoredFileContent;
import com.campushub.service.dto.UserBrief;
import com.campushub.service.UserService;
import com.campushub.util.StudentIdMasker;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link UserService} 实现。
 *
 * <p>{@code getCurrentUser} 读库返回最新资料（不依赖 JWT 快照），并把明文学号脱敏后
 * 组装为 {@link UserMeResponse}——绝不直接返回实体 {@link User}，以免泄露密码哈希、
 * 明文学号等敏感字段。
 *
 * <p>字段格式校验（长度/正则/必填）由 Controller 入口的请求 DTO 声明式完成；
 * 本层只做业务归一化（trim、空串转 null）与业务规则校验。实名认证的提交与审核流转由
 * {@code VerificationReviewService} 编排，本服务只提供用户侧的状态变更原语
 * （{@code markVerificationSubmitted/Approved/Rejected}）。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;

    @Override
    public UserMeResponse getCurrentUser(Long userId) {
        User user = requireUser(userId);
        return UserMeResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .studentIdMasked(StudentIdMasker.mask(user.getStudentId()))
                .realName(user.getRealName())
                .nickname(user.getNickname())
                .college(user.getCollege())
                .contact(user.getContact())
                .authStatus(user.getAuthStatus())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, String nickname, MultipartFile avatar,
                              String college, String contact) {
        if (nickname == null && avatar == null && college == null && contact == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "至少提交一个要修改的字段",
                    Map.of("request", "至少提交一个要修改的字段"));
        }

        User user = requireUser(userId);
        String nextNickname = user.getNickname();
        Long nextAvatarFileId = user.getAvatarFileId();
        String nextCollege = user.getCollege();
        String nextContact = user.getContact();

        if (nickname != null) {
            nextNickname = nickname.trim();
        }
        if (college != null) {
            nextCollege = normalizeOptionalText(college);
        }
        if (contact != null) {
            nextContact = normalizeOptionalText(contact);
        }
        if (avatar != null) {
            nextAvatarFileId = fileStorageService.uploadImage(
                    avatar, userId, FileUsage.AVATAR, FileBusinessType.USER_AVATAR, userId);
        }

        user.updateProfile(nextNickname, nextAvatarFileId, nextCollege, nextContact);
        userMapper.updateById(user);
    }

    @Override
    public UserPublicProfile getPublicProfile(Long userId) {
        User user = requireUser(userId);
        // 只暴露公开资料；ratingSummary 由 Controller 按 includeRating 决定是否经评价服务填充。
        return UserPublicProfile.builder()
                .nickname(user.getNickname())
                .college(user.getCollege())
                .contact(user.getContact())
                .ratingSummary(null)
                .build();
    }

    @Override
    public StoredFileContent loadAvatar(Long userId) {
        User user = requireUser(userId);
        if (user.getAvatarFileId() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorReason.RESOURCE_NOT_FOUND,
                    "头像不存在");
        }
        return fileStorageService.loadFile(user.getAvatarFileId());
    }

    @Override
    public List<UserBrief> getUserBriefs(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 只查摘要需要的 3 列，避免拉取 passwordHash/明文 studentId 等敏感字段；一次 IN 查询避 N+1。
        return userMapper.selectList(Wrappers.<User>lambdaQuery()
                        .select(User::getId, User::getUsername, User::getNickname)
                        .in(User::getId, userIds)).stream()
                .map(u -> UserBrief.builder()
                        .userId(u.getId())
                        .username(u.getUsername())
                        .nickname(u.getNickname())
                        .build())
                .toList();
    }

    @Override
    public boolean isStudentIdCertified(String studentId, Long excludeUserId) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getStudentId, studentId)
                .ne(excludeUserId != null, User::getId, excludeUserId));
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void markVerificationSubmitted(Long userId) {
        User user = requireUser(userId);
        if (user.getAuthStatus() == AuthStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.DUPLICATE_OR_CONFLICTED_OPERATION, "实名认证已通过，不能重复提交");
        }
        user.markAuthSubmitted();
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void markVerificationApproved(Long userId, String studentId, String realName) {
        User user = requireUser(userId);
        if (isStudentIdCertified(studentId, userId)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.DUPLICATE_OR_CONFLICTED_OPERATION, "该学号已被其他用户认证");
        }
        user.markAuthApproved(studentId, realName);
        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.DUPLICATE_OR_CONFLICTED_OPERATION, "该学号已被其他用户认证");
        }
    }

    @Override
    @Transactional
    public void markVerificationRejected(Long userId) {
        User user = requireUser(userId);
        user.markAuthRejected();
        userMapper.updateById(user);
    }

    @Override
    public void ensureCertified(Long userId) {
        User user = requireUser(userId);
        if (!user.canParticipatePickup()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorReason.AUTH_STATUS_NOT_ALLOWED,
                    "需实名认证通过后才能参与代取服务");
        }
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getId, userId));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorReason.RESOURCE_NOT_FOUND,
                    "用户不存在");
        }
        return user;
    }

    /** 可选文本字段归一化：trim 后空串视为「清空」转 null。长度约束由请求 DTO 保证。 */
    private String normalizeOptionalText(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
