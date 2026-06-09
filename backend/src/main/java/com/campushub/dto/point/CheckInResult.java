package com.campushub.dto.point;

import lombok.Builder;
import lombok.Getter;

/**
 * 签到结果，对应 {@code api_design.yaml} 的 {@code CheckInResult}。
 *
 * <p>每日首次签到返回本次获得积分（当前 MVP 固定 5）与签到后余额；当日重复签到由 Service
 * 返回 409，不返回此结构。
 */
@Getter
@Builder
public class CheckInResult {

    /** 本次签到获得的积分，当前 MVP 固定为 5。 */
    private final Integer earnedPoints;

    /** 签到后的当前积分余额（>=0）。 */
    private final Long pointBalance;
}
