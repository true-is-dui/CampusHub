package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.user.UserMeResponse;
import com.campushub.entity.User;
import com.campushub.mapper.UserMapper;
import com.campushub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * {@link UserService} 实现。
 *
 * <p>{@code getCurrentUser} 读库返回最新资料（不依赖 JWT 快照），并把明文学号脱敏后
 * 组装为 {@link UserMeResponse}——绝不直接返回实体 {@link User}，以免泄露密码哈希、
 * 明文学号等敏感字段。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserMeResponse getCurrentUser(Long userId) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getId, userId));
        if (user == null) {
            // 令牌有效但用户已被删除等极端情况
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return UserMeResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .studentIdMasked(maskStudentId(user.getStudentId()))
                .realName(user.getRealName())
                .nickname(user.getNickname())
                .college(user.getCollege())
                .contact(user.getContact())
                .authStatus(user.getAuthStatus())
                .role(user.getRole())
                .build();
    }

    /**
     * 学号脱敏：保留前 2 位与后 2 位，中间以等长 {@code *} 替换；
     * 未认证（学号为空）返回 null，长度不足 5 位时保守地整体打码。
     */
    private String maskStudentId(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            return null;
        }
        int len = studentId.length();
        if (len <= 4) {
            return "*".repeat(len);
        }
        String prefix = studentId.substring(0, 2);
        String suffix = studentId.substring(len - 2);
        return prefix + "*".repeat(len - 4) + suffix;
    }
}
