package com.campushub.service.impl;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.user.LoginSession;
import com.campushub.entity.User;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.NotificationType;
import com.campushub.entity.enums.UserRole;
import com.campushub.mapper.UserMapper;
import com.campushub.security.JwtUtil;
import com.campushub.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthServiceImpl} 单元测试：mock 掉 mapper / 密码编码器 / JWT 工具，
 * 只验证服务层的业务分支与状态组装，不连数据库。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_persistsHashedUser_withDefaults() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("Passw0rd")).thenReturn("HASHED");

        authService.register("alice", "Passw0rd");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPasswordHash()).isEqualTo("HASHED");
        // 默认值：昵称取用户名，未认证，普通用户
        assertThat(saved.getNickname()).isEqualTo("alice");
        assertThat(saved.getAuthStatus()).isEqualTo(AuthStatus.UNVERIFIED);
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        // 绝不存明文
        assertThat(saved.getPasswordHash()).isNotEqualTo("Passw0rd");
        // 注册成功发欢迎通知（SYSTEM，无业务关联）。
        verify(notificationService).createNotice(any(), eq(NotificationType.SYSTEM),
                any(), any(), isNull(), isNull());
    }

    @Test
    void register_rejectsDuplicateUsername_withConflict() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register("alice", "Passw0rd"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        // 预检命中即拒绝，不应再尝试插入
        verify(userMapper, never()).insert(any(User.class));
        // 注册失败不发欢迎通知
        verify(notificationService, never()).createNotice(any(), any(), any(), any(), any(), any());
    }

    @Test
    void login_returnsToken_whenCredentialsValid() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setPasswordHash("HASHED");
        user.setRole(UserRole.USER);
        user.setAuthStatus(AuthStatus.APPROVED);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("Passw0rd", "HASHED")).thenReturn(true);
        when(jwtUtil.generateToken(7L, UserRole.USER, AuthStatus.APPROVED)).thenReturn("JWT");

        LoginSession session = authService.login("alice", "Passw0rd");

        assertThat(session.getToken()).isEqualTo("JWT");
    }

    @Test
    void login_failsWithUnauthenticated_whenPasswordWrong() {
        User user = new User();
        user.setPasswordHash("HASHED");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice", "wrongpass"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_failsWithUnauthenticated_whenUserNotFound() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login("ghost", "Passw0rd"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
        // 用户不存在时不应调用密码比对（短路），也不签发令牌
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }
}
