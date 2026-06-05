package com.campushub.entity.enums;

/**
 * 文件用途，对应 stored_files.file_usage。
 * 文件模块只记录用途用于溯源，不据此判断业务访问权限；
 * 访问权限由对应业务模块校验（见 P3 数据库设计 §7.6）。
 */
public enum FileUsage {
    /** 头像 */
    AVATAR,
    /** 实名认证材料：实名审核模块/管理员可读 */
    VERIFICATION_MATERIAL,
    /** 取件凭证：接单前不向非发布方展示 */
    PICKUP_CREDENTIAL,
    /** 完成凭证：仅服务参与者可读 */
    COMPLETION_PROOF
}
