package com.campushub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.entity.PointTransaction;
import org.apache.ibatis.annotations.Mapper;

/** 积分流水表数据访问接口。基础 CRUD 由 BaseMapper 提供。 */
@Mapper
public interface PointTransactionMapper extends BaseMapper<PointTransaction> {
}
