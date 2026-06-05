package com.campushub.entity.enums;

/**
 * 业务类型，对应 payment_records.business_type、evaluations.business_type。
 * 当前 MVP 仅有代取请求一种业务，预留通用关联以便后续扩展。
 */
public enum BusinessType {
    /** 代取请求 */
    PICKUP_REQUEST
}
