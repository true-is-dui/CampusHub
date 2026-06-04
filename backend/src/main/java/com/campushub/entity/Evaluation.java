package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.RatingLevel;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evaluations")
public class Evaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewerId;
    private Long revieweeId;
    private BusinessType businessType;
    private Long businessId;
    private PickupParticipantRole revieweeRoleInBusiness;
    private RatingLevel ratingLevel;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
