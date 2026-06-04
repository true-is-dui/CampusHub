package com.campushub.service;

import com.campushub.dto.UpdateProfileRequest;
import com.campushub.dto.UserMeDTO;
import com.campushub.dto.UserSummaryDTO;

import java.util.Map;

public interface UserService {
    UserMeDTO getCurrentUser(Long userId);
    UserSummaryDTO getUserSummary(Long userId);
    Map<String, Object> loadAvatar(Long userId);
    void updateProfile(Long userId, UpdateProfileRequest request);
    String getAvatarMimeType(Long userId);
    void submitVerification(Long userId, String studentId, String realName, Long verificationFileId);
    void updateAvatarFileId(Long userId, Long avatarFileId);
}
