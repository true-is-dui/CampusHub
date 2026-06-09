package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import lombok.Data;

import java.time.LocalDate;
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

    /** 平台积分余额，纯虚拟、不可充值提现；认证赠送、签到、代取流转时由 PointService 维护 */
    private Long pointBalance;

    /** 最近一次签到日期，用于每日签到去重 */
    private LocalDate lastCheckInDate;

    /** 注册时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ---------------- 领域方法（仅供 Service 层内部调用） ----------------

    /**
     * 提交实名认证：仅将认证状态置为审核中。
     * 当次提交的学号、姓名快照和材料文件由 verification_reviews 保存；
     * users 表的 student_id/real_name 表示当前有效实名信息，审核通过后才同步，
     * 提交阶段不写入（避免审核中提前占用 uk_users_student_id 唯一约束）。
     */
    public void markAuthSubmitted() {
        this.authStatus = AuthStatus.REVIEWING;
    }

    /**
     * 实名认证通过：由 Service 从审核快照取通过的学号、姓名同步为当前有效实名信息。
     */
    public void markAuthApproved(String studentId, String realName) {
        this.studentId = studentId;
        this.realName = realName;
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

    /**
     * 从积分余额中扣减，返回扣减后的余额。余额不足由 Service 层在调用前拦截
     * （本方法仍做一次防御性校验，不允许扣成负数）。供发布有报酬代取使用。
     */
    public long deductPoints(long amount) {
        long current = currentBalance();
        if (amount < 0 || current < amount) {
            throw new IllegalStateException("积分余额不足");
        }
        this.pointBalance = current - amount;
        return this.pointBalance;
    }

    /**
     * 向积分余额中增加，返回增加后的余额。供签到、认证赠送、取消退回、完成入账使用。
     */
    public long addPoints(long amount) {
        if (amount < 0) {
            throw new IllegalStateException("增加积分不能为负");
        }
        this.pointBalance = currentBalance() + amount;
        return this.pointBalance;
    }

    /** 余额初值兜底：历史数据或未初始化时按 0 处理（数据库列有 NOT NULL DEFAULT 0）。 */
    private long currentBalance() {
        return this.pointBalance == null ? 0L : this.pointBalance;
    }
}
