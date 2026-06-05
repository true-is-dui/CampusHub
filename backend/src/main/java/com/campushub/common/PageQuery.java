package com.campushub.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询参数，绑定接口的 {@code page} / {@code pageSize} 查询参数。
 * 取值约束与 {@code api_design.yaml} 的 {@code parameters.Page} /
 * {@code parameters.PageSize} 一致：page 从 1 起，pageSize 默认 20、上限 50。
 *
 * <p>用普通可变 POJO（Lombok {@code @Data} 生成 getter/setter）以便 Spring MVC
 * 按查询参数名反射注入；字段初始值即为契约默认值，调用方不传时生效。
 */
@Data
public class PageQuery {

    /** 页码，从 1 开始。 */
    @Min(value = 1, message = "页码必须大于等于 1")
    private int page = 1;

    /** 每页条数，1~50。 */
    @Min(value = 1, message = "每页条数必须大于等于 1")
    @Max(value = 50, message = "每页条数不能超过 50")
    private int pageSize = 20;

    /**
     * 转成 MyBatis Plus 的分页对象，供 {@code BaseMapper.selectPage(...)} 使用。
     * 把"框架分页类型"的构造集中在此，Service 只需 {@code query.toMpPage()}。
     *
     * @param <T> 分页查询的实体类型
     */
    public <T> Page<T> toMpPage() {
        return Page.of(page, pageSize);
    }
}
