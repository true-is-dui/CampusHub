package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campushub.common.BusinessException;
import com.campushub.dto.UpdateProfileRequest;
import com.campushub.dto.UserMeDTO;
import com.campushub.dto.UserSummaryDTO;
import com.campushub.entity.User;
import com.campushub.entity.VerificationReview;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.ReviewStatus;
import com.campushub.mapper.UserMapper;
import com.campushub.mapper.VerificationReviewMapper;
import com.campushub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final VerificationReviewMapper verificationReviewMapper;
    private final FileStorageServiceImpl fileStorageService;

    public UserMeDTO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40401, "用户不存在");
        }

        UserMeDTO dto = new UserMeDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setStudentIdMasked(maskStudentId(user.getStudentId()));
        dto.setRealName(user.getRealName());
        dto.setNickname(user.getNickname());
        dto.setCollege(user.getCollege());
        dto.setContact(user.getContact());
        dto.setAuthStatus(user.getAuthStatus() != null ? user.getAuthStatus().name() : null);
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        return dto;
    }

    public UserSummaryDTO getUserSummary(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40401, "用户不存在");
        }

        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setUserId(user.getId());
        dto.setNickname(user.getNickname());
        return dto;
    }

    public Map<String, Object> loadAvatar(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40401, "用户不存在");
        }
        if (user.getAvatarFileId() == null) {
            throw new BusinessException(40402, "用户未设置头像");
        }
        return fileStorageService.loadFile(user.getAvatarFileId());
    }

    public String getAvatarMimeType(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getAvatarFileId() == null) {
            return null;
        }
        Map<String, Object> fileData = fileStorageService.loadFile(user.getAvatarFileId());
        return (String) fileData.get("mimeType");
    }

    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40401, "用户不存在");
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getCollege() != null) {
            user.setCollege(request.getCollege());
        }
        if (request.getContact() != null) {
            user.setContact(request.getContact());
        }
        userMapper.updateById(user);
    }

    @Transactional
    public void submitVerification(Long userId, String studentId, String realName, Long verificationFileId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40401, "用户不存在");
        }

        user.setStudentId(studentId);
        user.setRealName(realName);
        user.setVerificationFileId(verificationFileId);
        user.setAuthStatus(AuthStatus.REVIEWING);
        userMapper.updateById(user);

        VerificationReview review = new VerificationReview();
        review.setUserId(userId);
        review.setStatus(ReviewStatus.PENDING);
        verificationReviewMapper.insert(review);
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
