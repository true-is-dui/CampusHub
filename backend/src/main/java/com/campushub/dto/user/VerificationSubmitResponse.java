package com.campushub.dto.user;

import com.campushub.entity.enums.AuthStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 实名认证提交成功响应数据。 */
@Getter
@RequiredArgsConstructor
public class VerificationSubmitResponse {

    private final AuthStatus authStatus;
}
