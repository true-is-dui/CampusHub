package com.campushub.service.impl;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    // ---------------- 积分原语：getPointBalance / applyPointChange ----------------

    @Test
    void getPointBalance_returnsValue() {
        User user = user(7L, AuthStatus.APPROVED);
        user.setPointBalance(120L);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThat(service.getPointBalance(7L)).isEqualTo(120L);
    }

    @Test
    void getPointBalance_nullAsZero() {
        User user = user(7L, AuthStatus.APPROVED);
        user.setPointBalance(null);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThat(service.getPointBalance(7L)).isEqualTo(0L);
    }

    @Test
    void applyPointChange_add_returnsBalanceAfter_conditionalUpdate() {
        User user = user(7L, AuthStatus.APPROVED);
        user.setPointBalance(40L);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.update(any(), any())).thenReturn(1);

        long after = service.applyPointChange(7L, 10L, null);

        assertThat(after).isEqualTo(50L);
        assertThat(user.getPointBalance()).isEqualTo(50L);
        verify(userMapper).update(eq(user), any());
    }

    @Test
    void applyPointChange_deduct_insufficient_throwsInsufficientPoints_noUpdate() {
        User user = user(7L, AuthStatus.APPROVED);
        user.setPointBalance(5L);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> service.applyPointChange(7L, -10L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        // 余额不足在落库前拦截，不写库。
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void applyPointChange_concurrentBalance_throwsConflict() {
        User user = user(7L, AuthStatus.APPROVED);
        user.setPointBalance(40L);
        when(userMapper.selectOne(any())).thenReturn(user);
        // 余额条件更新影响行数 0 → 并发冲突。
        when(userMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.applyPointChange(7L, 10L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void applyPointChange_checkIn_conflict_throwsAlreadyChecked() {
        User user = user(7L, AuthStatus.APPROVED);
        user.setPointBalance(10L);
        when(userMapper.selectOne(any())).thenReturn(user);
        // 带签到日期条件的更新影响行数 0 → 当日已签到。
        when(userMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.applyPointChange(7L, 5L, LocalDate.now()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrors())
                .isEqualTo(java.util.Map.of("reason", ErrorReason.ALREADY_CHECKED_IN_TODAY.name()));
    }

    @Test
    void applyPointChange_checkIn_success_setsDateAndBalance() {
        User user = user(7L, AuthStatus.APPROVED);
        user.setPointBalance(10L);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.update(any(), any())).thenReturn(1);

        LocalDate today = LocalDate.now();
        long after = service.applyPointChange(7L, 5L, today);

        assertThat(after).isEqualTo(15L);
        assertThat(user.getLastCheckInDate()).isEqualTo(today);
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
