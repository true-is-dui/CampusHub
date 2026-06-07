package com.campushub.entity.enums;

/**
 * 文件上传溯源的业务类型，写入 stored_files.business_type。
 *
 * <p>仅用于把已上传文件溯源到某类业务记录（审计排查、孤立文件清理），不表示文件
 * 最终业务归属，也不据此判断访问权限。与 {@link BusinessType}（评价/支付定位的业务
 * 对象类型）语义不同，故单独定义。
 *
 * <p>持久层字段为 VARCHAR，存储本枚举的 {@code name()}；当前仅按实际用到的取值收录，
 * 新增业务文件时再补。
 */
public enum FileBusinessType {
    /** 头像，溯源到用户 */
    USER_AVATAR,
    /** 实名认证材料，溯源到实名审核记录 */
    VERIFICATION_REVIEW
}
