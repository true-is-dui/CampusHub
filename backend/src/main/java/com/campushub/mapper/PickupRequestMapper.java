package com.campushub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.entity.PickupRequest;
import org.apache.ibatis.annotations.Mapper;

/** 代取请求主表数据访问接口。基础 CRUD 由 BaseMapper 提供。 */
@Mapper
public interface PickupRequestMapper extends BaseMapper<PickupRequest> {
}
