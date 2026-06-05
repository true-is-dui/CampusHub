package com.campushub.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置。
 *
 * <p>当前只注册分页插件，使 BaseMapper.selectPage(...) 能够真正执行
 * 物理分页（自动追加 LIMIT 并执行 count 查询）。不注册分页插件时，
 * selectPage 不会真正分页。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis Plus 拦截器，并加入分页内部拦截器。
     * DbType.MYSQL 让分页 SQL 按 MySQL 方言生成。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
