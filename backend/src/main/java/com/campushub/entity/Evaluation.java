package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.RatingLevel;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体，映射 evaluations 表（Should 级能力）。
 *
 * <p>使用 businessType + businessId 定位被评价的业务对象，
 * 当前 MVP 固定 businessType=PICKUP_REQUEST、businessId 对应代取请求 ID，
 * 避免在评价表写死代取请求外键，便于后续扩展其他业务。
 *
 * <p>唯一约束 (businessType, businessId, reviewerId) 保证同一业务对象中
 * 同一评价者只能评价一次。
 */
@Data
@TableName("evaluations")
public class Evaluation {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型，当前仅 PICKUP_REQUEST */
    private BusinessType businessType;

    /** 业务对象 ID，当前对应代取请求 ID */
    private Long businessId;

    /** 评价者 ID */
    private Long reviewerId;

    /** 被评价者 ID */
    private Long revieweeId;

    /** 被评价者在该代取服务中的角色 */
    private PickupParticipantRole revieweeRole;

    /** 评价等级 */
    private RatingLevel ratingLevel;

    /** 评价内容；差评（BAD）时业务层要求非空 */
    private String content;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
