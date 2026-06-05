package com.campushub.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 时间字段自动填充处理器。
 *
 * <p>配合实体上的 {@code @TableField(fill = ...)} 注解工作：
 * <ul>
 *   <li>insert 时：填充 createdAt 和 updatedAt 为当前时间</li>
 *   <li>update 时：填充 updatedAt 为当前时间</li>
 * </ul>
 *
 * <p>这样 Service 层执行 insert/update 时无需手动 set 时间字段。
 * 数据库列另有 DEFAULT CURRENT_TIMESTAMP 作为兜底，两层共同保证时间字段始终有值。
 *
 * <p>采用 strict 填充：仅当实体确实声明了该字段且当前为 null 时才填充，
 * 因此可统一处理所有实体——缺少某个时间字段的实体会自动跳过。
 */
@Component
public class TimeFieldFillHandler implements MetaObjectHandler {

    /** 插入时触发：填充标注了 FieldFill.INSERT / INSERT_UPDATE 的字段 */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // strictInsertFill：仅当实体确实有该字段、且字段为 null 时才填充
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    /** 更新时触发：填充标注了 FieldFill.INSERT_UPDATE 的字段 */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
