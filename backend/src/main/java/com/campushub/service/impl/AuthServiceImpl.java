package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campushub.common.BusinessException;
import com.campushub.config.JwtUtil;
import com.campushub.service.AuthService;
import com.campushub.dto.LoginRequest;
import com.campushub.dto.LoginSession;
import com.campushub.dto.RegisterRequest;
import com.campushub.entity.User;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import com.campushub.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginSession register(RegisterRequest request) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(40001, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAuthStatus(AuthStatus.UNVERIFIED);
        user.setRole(UserRole.USER);
        userMapper.insert(user);

        LoginSession session = new LoginSession();
        session.setToken(jwtUtil.generateToken(user.getId(), user.getRole().name()));
        return session;
    }

    public LoginSession login(LoginRequest request) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(40002, "用户名或密码错误");
        }

        LoginSession session = new LoginSession();
        session.setToken(jwtUtil.generateToken(user.getId(), user.getRole().name()));
        return session;
    }
}
