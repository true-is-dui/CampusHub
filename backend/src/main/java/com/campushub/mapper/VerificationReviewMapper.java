package com.campushub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.entity.VerificationReview;
import org.apache.ibatis.annotations.Mapper;

/** 实名认证审核记录表数据访问接口。基础 CRUD 由 BaseMapper 提供。 */
@Mapper
public interface VerificationReviewMapper extends BaseMapper<VerificationReview> {
}
