package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
import com.campushub.dto.user.LoginSession;
import com.campushub.entity.User;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import com.campushub.mapper.UserMapper;
import com.campushub.security.JwtUtil;
import com.campushub.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AuthService} 实现：注册与登录。
 *
 * <p>注册：先查用户名是否占用（{@code selectCount} 只问存在性，不取整行），命中即抛
 * 409；并发下两个请求同时通过预检时，由 users 表唯一约束 {@code uk_users_username}
 * 兜底，捕获 {@link DuplicateKeyException} 同样翻成 409。密码以 BCrypt 加盐哈希存储。
 *
 * <p>登录：用户名查用户，再用 {@link PasswordEncoder#matches} 比对密码哈希。无论
 * "用户不存在"还是"密码不符"，都返回同一个 401 + {@code INVALID_CREDENTIALS}，
 * 不区分两者以避免账号枚举攻击。校验通过后由 {@link JwtUtil} 签发携带身份快照的令牌。
 *
 * <p>依赖通过 Lombok {@code @RequiredArgsConstructor} 生成的构造器注入（final 字段），
 * 注入的是接口/Bean 类型，便于测试替换为 mock。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public void register(String username, String rawPassword) {
        // 预检：用户名是否已被占用
        Long existing = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.DUPLICATE_OR_CONFLICTED_OPERATION, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        // 注册即拥有公开展示名，默认用用户名，后续可在资料编辑中修改
        user.setNickname(username);
        user.setAuthStatus(AuthStatus.UNVERIFIED);
        user.setRole(UserRole.USER);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发兜底：预检与插入之间另一个请求抢先插入了同名用户
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.DUPLICATE_OR_CONFLICTED_OPERATION, "用户名已被占用");
        }
    }

    @Override
    public LoginSession login(String username, String rawPassword) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        // 用户不存在与密码错误返回一致的错误，避免账号枚举
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, ErrorReason.INVALID_CREDENTIALS);
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getAuthStatus());
        return new LoginSession(token);
    }
}
