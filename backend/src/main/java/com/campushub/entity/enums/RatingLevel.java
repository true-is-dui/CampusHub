package com.campushub.entity.enums;

/**
 * 评价等级，对应 evaluations.rating_level。
 */
public enum RatingLevel {
    /** 好评：计入好评数 */
    GOOD,
    /** 中评：计入中评数 */
    NEUTRAL,
    /** 差评：计入差评数，业务层要求填写评价内容 */
    BAD
}
