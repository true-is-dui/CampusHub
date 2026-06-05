package com.campushub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表数据访问接口。
 *
 * <p>继承 MyBatis Plus 的 BaseMapper&lt;User&gt; 后，自动拥有
 * selectById / insert / updateById / deleteById / selectList /
 * selectPage / selectCount 等约 17 个基础 CRUD 方法，无需编写 XML。
 *
 * <p>若有 BaseMapper 无法表达的复杂查询，可在此声明方法并配合
 * @Select 注解或 XML 手写 SQL（当前 MVP 暂不需要）。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
