package com.campushub.dto.user;

import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

/**
 * 当前用户信息响应，对应 {@code api_design.yaml} 的 {@code UserMe}。
 *
 * <p>由 {@code UserService} 读库后组装：学号以脱敏形式（{@code studentIdMasked}）
 * 返回而非明文；未认证时学号、真实姓名相关字段为 null。设计为不可变对象，
 * 用 Lombok {@code @Builder} 装配（字段多且部分可空，Builder 比长构造器更清晰）。
 *
 * <p>不直接暴露实体 {@code User}：实体含 {@code passwordHash}、明文 {@code studentId}
 * 等敏感字段，响应对象只挑选契约规定的可见字段。
 */
@Getter
@Builder
public class UserMeResponse {

    /** 当前用户标识，可用于读取头像等当前用户资源。 */
    private final Long userId;

    /** 登录账号，仅当前用户本人可见。 */
    private final String username;

    /** 脱敏学号（前 2 + 后 2，中间打码）；未认证时为 null。 */
    private final String studentIdMasked;

    /** 真实姓名，仅本人可见；未认证时为 null。 */
    private final String realName;

    /** 公开展示昵称。 */
    private final String nickname;

    /** 学院，选填；未填写为 null。 */
    private final String college;

    /** 公开联系方式，选填；未填写为 null。 */
    private final String contact;

    /** 当前认证状态。 */
    private final AuthStatus authStatus;

    /** 用户角色（USER / ADMIN）。 */
    private final UserRole role;

    /** 当前用户的平台积分余额（>=0），积分为平台内虚拟资产、不可充值提现。 */
    private final Long pointBalance;
}
