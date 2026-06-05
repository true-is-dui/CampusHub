package com.campushub.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;

import java.util.List;

/**
 * 分页结果体，字段与 {@code api_design.yaml} 的 {@code PageResult} 契约一致：
 * {@code total} / {@code page} / {@code pageSize} / {@code list}。
 *
 * <p>注意字段名是 {@code list}，与 MyBatis Plus {@code IPage} 内部的
 * {@code records} 不同；本类负责把框架分页对象转换为对外契约形状，
 * 从而避免直接把框架类型暴露到 API 响应里。
 *
 * @param <T> 列表元素类型（通常是对外 DTO，而非数据库实体）
 */
@Getter
public class PageResult<T> {

    /** 满足条件的总记录数。 */
    private final long total;

    /** 当前页码。 */
    private final int page;

    /** 每页条数。 */
    private final int pageSize;

    /** 当前页数据。 */
    private final List<T> list;

    private PageResult(long total, int page, int pageSize, List<T> list) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
    }

    /**
     * 直接使用分页对象自带的记录构建结果，适用于查询结果本身就是对外类型的场景。
     */
    public static <T> PageResult<T> of(IPage<T> source) {
        return new PageResult<>(source.getTotal(), (int) source.getCurrent(),
                (int) source.getSize(), source.getRecords());
    }

    /**
     * 复用分页对象的 total/page/pageSize 元信息，但用调用方映射好的列表替换记录，
     * 适用于"查实体再转 DTO"的常见场景。
     *
     * @param source     携带分页元信息的查询结果（其 records 已被映射为 mappedList）
     * @param mappedList 已映射为对外类型的列表
     */
    public static <T> PageResult<T> of(IPage<?> source, List<T> mappedList) {
        return new PageResult<>(source.getTotal(), (int) source.getCurrent(),
                (int) source.getSize(), mappedList);
    }
}
