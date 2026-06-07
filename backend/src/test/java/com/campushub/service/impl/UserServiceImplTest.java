package com.campushub.service.impl;

import com.campushub.common.BusinessException;
import com.campushub.entity.User;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.FileBusinessType;
import com.campushub.entity.enums.FileUsage;
import com.campushub.entity.enums.UserRole;
import com.campushub.mapper.UserMapper;
import com.campushub.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final UserServiceImpl service = new UserServiceImpl(userMapper, fileStorageService);

    @Test
    void updateProfile_partiallyUpdatesFieldsAndAvatar() {
        User user = user(7L, AuthStatus.UNVERIFIED);
        user.setCollege("old");
        user.setContact("old-contact");
        when(userMapper.selectOne(any())).thenReturn(user);
        MockMultipartFile file = new MockMultipartFile("avatar", "a.png", "image/png", new byte[]{1});
        when(fileStorageService.uploadImage(eq(file), eq(7L), eq(FileUsage.AVATAR),
                eq(FileBusinessType.USER_AVATAR), eq(7L))).thenReturn(99L);

        service.updateProfile(7L, "新昵称", file, "", "wx123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User updated = captor.getValue();
        assertThat(updated.getNickname()).isEqualTo("新昵称");
        assertThat(updated.getAvatarFileId()).isEqualTo(99L);
        assertThat(updated.getCollege()).isNull();
        assertThat(updated.getContact()).isEqualTo("wx123");
    }

    @Test
    void markVerificationSubmitted_setsReviewing() {
        User user = user(7L, AuthStatus.REJECTED);
        when(userMapper.selectOne(any())).thenReturn(user);

        service.markVerificationSubmitted(7L);

        verify(userMapper).updateById(user);
        assertThat(user.getAuthStatus()).isEqualTo(AuthStatus.REVIEWING);
    }

    @Test
    void markVerificationSubmitted_rejectsWhenAlreadyApproved() {
        User user = user(7L, AuthStatus.APPROVED);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> service.markVerificationSubmitted(7L))
                .isInstanceOf(BusinessException.class);
        verify(userMapper, org.mockito.Mockito.never()).updateById(any(User.class));
    }

    @Test
    void markVerificationApproved_syncsIdentityAndStatus() {
        User user = user(7L, AuthStatus.REVIEWING);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(0L);

        service.markVerificationApproved(7L, "20260001", "张三");

        verify(userMapper).updateById(user);
        assertThat(user.getAuthStatus()).isEqualTo(AuthStatus.APPROVED);
        assertThat(user.getStudentId()).isEqualTo("20260001");
        assertThat(user.getRealName()).isEqualTo("张三");
    }

    @Test
    void markVerificationApproved_rejectsWhenStudentIdCertifiedByOther() {
        User user = user(7L, AuthStatus.REVIEWING);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.markVerificationApproved(7L, "20260001", "张三"))
                .isInstanceOf(BusinessException.class);
        verify(userMapper, org.mockito.Mockito.never()).updateById(any(User.class));
    }

    @Test
    void markVerificationRejected_setsRejected() {
        User user = user(7L, AuthStatus.REVIEWING);
        when(userMapper.selectOne(any())).thenReturn(user);

        service.markVerificationRejected(7L);

        verify(userMapper).updateById(user);
        assertThat(user.getAuthStatus()).isEqualTo(AuthStatus.REJECTED);
    }

    private User user(Long id, AuthStatus authStatus) {
        User user = new User();
        user.setId(id);
        user.setUsername("alice");
        user.setNickname("alice");
        user.setAuthStatus(authStatus);
        user.setRole(UserRole.USER);
        return user;
    }
}
