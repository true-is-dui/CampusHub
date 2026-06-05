package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，映射 users 表。
 *
 * <p>保存登录信息、公开资料、当前有效实名信息、认证状态和角色。
 * 密码只保存 BCrypt 哈希，不保存明文。好评率等评价统计不在本表缓存，
 * 由评价模块从 evaluations 动态聚合。
 *
 * <p>本类是富领域模型：除了字段，还提供维护自身状态的领域方法
 * （markAuthSubmitted / markAuthApproved 等）。这些方法只表达
 * "对象自己的状态规则"，供 Service 层调用；权限校验、跨模块协作
 * 仍由 Service 负责。
 */
@Data
@TableName("users")
public class User {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号，平台内唯一 */
    private String username;

    /** BCrypt 密码哈希，禁止明文存储 */
    private String passwordHash;

    /** 公开展示昵称，可重复 */
    private String nickname;

    /** 头像文件 ID，关联 stored_files.id */
    private Long avatarFileId;

    /** 手机号，选填资料字段；MVP 不用于短信登录 */
    private String phone;

    /** 当前有效学号，认证通过后从审核快照同步；不对普通用户公开 */
    private String studentId;

    /** 当前有效真实姓名，仅实名认证和管理员审核可见 */
    private String realName;

    /** 学院，选填公开资料 */
    private String college;

    /** 用户主动填写的公开联系方式，选填 */
    private String contact;

    /** 实名认证状态 */
    private AuthStatus authStatus;

    /** 用户角色 */
    private UserRole role;

    /** 注册时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ---------------- 领域方法（仅供 Service 层内部调用） ----------------

    /**
     * 提交实名认证：保存实名信息和认证材料，进入审核中。
     * 实名材料文件由 verification_reviews 维护，本表只保存当前有效实名信息。
     */
    public void markAuthSubmitted(String studentId, String realName) {
        this.studentId = studentId;
        this.realName = realName;
        this.authStatus = AuthStatus.REVIEWING;
    }

    /** 实名认证通过 */
    public void markAuthApproved() {
        this.authStatus = AuthStatus.APPROVED;
    }

    /** 实名认证驳回 */
    public void markAuthRejected() {
        this.authStatus = AuthStatus.REJECTED;
    }

    /** 更新公开资料；传入 null 表示该项不修改由 Service 决定，本方法直接赋值 */
    public void updateProfile(String nickname, Long avatarFileId, String college, String contact) {
        this.nickname = nickname;
        this.avatarFileId = avatarFileId;
        this.college = college;
        this.contact = contact;
    }

    /** 是否已认证通过、可参与代取业务 */
    public boolean canParticipatePickup() {
        return this.authStatus == AuthStatus.APPROVED;
    }

    /** 是否为管理员 */
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
}
