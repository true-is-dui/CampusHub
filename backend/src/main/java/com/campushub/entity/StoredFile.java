package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.entity.enums.FileUsage;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件元数据实体，映射 stored_files 表。
 *
 * <p>只保存文件元数据和上传溯源信息，不负责判断业务访问权限。
 * uploaderId、fileUsage、businessType、businessId 仅用于上传溯源、
 * 审计排查和孤立文件清理，不表示最终业务归属，也不据此判断谁能读文件。
 * 业务归属由对应业务对象通过 fileId 引用维护；访问权限由业务模块校验。
 */
@Data
@TableName("stored_files")
public class StoredFile {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上传者用户 ID，仅用于上传溯源和审计 */
    private Long uploaderId;

    /** 文件用途标记 */
    private FileUsage fileUsage;

    /** 原始文件名 */
    private String originalFilename;

    /** 本地存储路径或对象存储 key */
    private String storagePath;

    /** MIME 类型，例如 image/jpeg、image/png */
    private String mimeType;

    /** 文件大小，单位字节 */
    private Long fileSize;

    /** 文件内容 SHA-256 哈希，用于排查重复和完整性校验 */
    private String sha256;

    /** 上传溯源业务类型，非业务外键 */
    private String businessType;

    /** 上传溯源业务 ID，上传时可能为空，业务创建后回填，非业务外键 */
    private Long businessId;

    /** 上传时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
