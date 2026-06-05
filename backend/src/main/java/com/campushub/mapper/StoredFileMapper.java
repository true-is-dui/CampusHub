package com.campushub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.entity.StoredFile;
import org.apache.ibatis.annotations.Mapper;

/** 文件元数据表数据访问接口。基础 CRUD 由 BaseMapper 提供。 */
@Mapper
public interface StoredFileMapper extends BaseMapper<StoredFile> {
}
