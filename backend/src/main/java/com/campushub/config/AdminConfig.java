package com.campushub.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campushub.entity.User;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import com.campushub.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminConfig implements CommandLineRunner {

    private final UserMapper userMapper;

    @Override
    public void run(String... args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Create admin if not exists
        QueryWrapper<User> adminWrapper = new QueryWrapper<>();
        adminWrapper.eq("role", UserRole.ADMIN);
        if (userMapper.selectCount(adminWrapper) == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(encoder.encode("admin123"));
            admin.setRole(UserRole.ADMIN);
            admin.setAuthStatus(AuthStatus.APPROVED);
            userMapper.insert(admin);
        }

        // Create test user if not exists
        QueryWrapper<User> testWrapper = new QueryWrapper<>();
        testWrapper.eq("username", "testuser");
        if (userMapper.selectCount(testWrapper) == 0) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setPasswordHash(encoder.encode("123456"));
            testUser.setNickname("testuser");
            testUser.setRole(UserRole.USER);
            testUser.setAuthStatus(AuthStatus.APPROVED);
            userMapper.insert(testUser);
        }
    }
}
