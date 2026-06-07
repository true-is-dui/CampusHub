package com.campushub.service.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 用户公开摘要，供其他业务模块（如实名审核列表）展示用户时使用。
 *
 * <p>对齐 {@code class_design.md} §315 / FR-UM-03 的约束：跨模块展示用户只携带公开字段，
 * <b>不得暴露学号、真实姓名、认证材料或文件标识</b>。因此这里只含 userId/username/nickname，
 * 由用户模块产出，避免其他模块直接拿 {@code User} 实体而越界读到敏感列。
 */
@Getter
@Builder
public class UserBrief {

    private final Long userId;
    private final String username;
    private final String nickname;
}
